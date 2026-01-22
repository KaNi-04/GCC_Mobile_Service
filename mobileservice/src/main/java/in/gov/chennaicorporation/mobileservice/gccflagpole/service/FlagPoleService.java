package in.gov.chennaicorporation.mobileservice.gccflagpole.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class FlagPoleService {

    @Autowired
    JdbcTemplate jdbcFlagPoleTemplate;

    private final Environment environment;

    private String fileBaseUrl;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final int STRING_LENGTH = 15;

    private static final Random RANDOM = new SecureRandom();

    @Autowired
    public void setDataSource(@Qualifier("mysqlGccFlagPoleDataSource") DataSource flagPoleDataSource) {
        this.jdbcFlagPoleTemplate = new JdbcTemplate(flagPoleDataSource);
    }

    public FlagPoleService(Environment environment) {
        this.environment = environment;
        this.fileBaseUrl = environment.getProperty("fileBaseUrl");
    }

    public static String generateRandomString() {
        StringBuilder result = new StringBuilder(STRING_LENGTH);
        for (int i = 0; i < STRING_LENGTH; i++) {
            result.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return result.toString();
    }

    private byte[] compressImage(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(byteArrayOutputStream);

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        writer.setOutput(imageOutputStream);

        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }

        writer.write(null, new javax.imageio.IIOImage(image, null, null), param);

        writer.dispose();
        imageOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    public String fileUpload(MultipartFile file, String name) {

        // Set the file path where you want to save it
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("flagpole_foldername");
        var year = DateTimeUtil.getCurrentYear();
        var month = DateTimeUtil.getCurrentMonth();

        uploadDirectory = uploadDirectory + serviceFolderName + year + "/" + month;

        try {
            // Create directory if it doesn't exist
            Path directoryPath = Paths.get(uploadDirectory);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Datetime string
            String datetimetxt = DateTimeUtil.getCurrentDateTime();
            // File name
            String fileName = name + "_" + datetimetxt + "_" + file.getOriginalFilename();
            fileName = fileName.replaceAll("\s+", ""); // Remove space on filename

            String filePath = uploadDirectory + "/" + fileName;

            String filepath_txt = "/" + serviceFolderName + year + "/" + month + "/" + fileName;

            // Create a new Path object
            Path path = Paths.get(filePath);

            // Get the bytes of the file
            byte[] bytes = file.getBytes();

            // Compress the image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50% quality

            // Write the bytes to the file
            Files.write(path, compressedBytes);

            System.out.println(filePath);
            return filepath_txt;

        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to save file " + file.getOriginalFilename();
        }
    }

    public List<Map<String, Object>> getEventDetails() {
        try{
            String sql = "select * from event_type where is_active = 1 and is_delete = 0";
            List<Map<String, Object>> eventList = jdbcFlagPoleTemplate.queryForList(sql);
            return eventList;

        }catch(Exception ex){
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getAllFlagPoleRequest(String loginid) {

        String ward = getWardByLoginId(loginid, "ae");

        try {
            String sql = "select urd.id, urd.refid, urd.applicant_name, urd.mobile_number, urd.total_poles, " +
                    "date_format(urd.cdate,'%d-%m-%Y %l:%i %p') as req_rec_date, count(sd.id) as streetCount " +
                    "from user_request_details urd " +
                    "left join street_details sd on urd.refid = sd.refid " +
                    "where urd.payment_status = 'COMPLETED' and sd.ward =? and urd.ae_status IS NOT NULL " +
                    " AND sd.is_active =1 and sd.is_delete =0 "+
                    "group by urd.id, urd.refid, urd.applicant_name, urd.mobile_number,urd.total_poles, urd.cdate ";
            List<Map<String, Object>> requestList = jdbcFlagPoleTemplate.queryForList(sql, new Object[] { ward });
            return requestList;
        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "No data found");
            return Collections.singletonList(response);

        }

    }

    public Map<String, Object> getRequestDetailsById(String reqId) {
        if (reqId == null || reqId.isEmpty()) {
            return Collections.singletonMap("status", "Incorrect Request ID");

        }
        try {
            String applicantsql = "select urd.id, urd.refid, urd.applicant_name, urd.mobile_number, urd.total_poles,  "+
                    "date_format(urd.cdate,'%d-%m-%Y %l:%i %p') as req_rec_date, count(sd.id) as streetCount, " +
                    // " case when urd.ae_status is null and urd.ae_status !='APPROVED' THEN
                    // 'PENDING' ELSE 'APPROVED' END AS status, "+
                    "urd.applicant_address as address, urd.event_desc as eventDetails " +
                    "from user_request_details urd " +
                    "left join street_details sd on urd.refid = sd.refid " +
                    "where urd.payment_status = 'COMPLETED' and urd.id = ? " +
                    "group by urd.id, urd.refid, urd.applicant_name, urd.mobile_number,urd.total_poles, urd.cdate";
            Map<String, Object> requestDetails = jdbcFlagPoleTemplate.queryForMap(applicantsql, new Object[] { reqId });

            String streetSql = "select sd.id as streetDetailsId, urd.id as reqid, sd.street_name, sd.zone, sd.ward, sd.no_of_poles, "+
                    "sd.height, pm.pole_material_name, fm.flag_material_name, sd.pole_space, sd.no_of_days " +
                    "from street_details sd " +
                    "left join user_request_details urd on urd.refid = sd.refid " +
                    "left join pole_material pm on sd.pole_material_id = pm.id " +
                    "left join flag_material fm on sd.flag_material_id = fm.id " +
                    "where urd.id = ? AND sd.is_active =1 and sd.is_delete =0 ";
            List<Map<String, Object>> streetDetails = jdbcFlagPoleTemplate.queryForList(streetSql,
                    new Object[] { reqId });

            requestDetails.put("streetDetails", streetDetails);

            return requestDetails;
        } catch (Exception ex) {
            ex.printStackTrace();
            return Collections.singletonMap("status", "No data found");

        }
    }

    public String getWardByLoginId(String loginid, String type) {
        String sqlQuery = "SELECT ward FROM gcc_penalty_hoardings.hoading_user_list WHERE userid = ? AND type = ? LIMIT 1";

        List<Map<String, Object>> results = jdbcFlagPoleTemplate.queryForList(sqlQuery, loginid, type);

        if (!results.isEmpty()) {
            System.out.println("Ward....." + results);
            // Extract the ward value from the first result
            return (String) results.get(0).get("ward");
        }

        return "000";
    }

    public Map<String, Object> approveFlagPoleRequestByAE(String reqId, String approvedBy) {
        if (reqId == null || reqId.isEmpty()) {
            return Collections.singletonMap("status", "Incorrect Request ID");

        }
        try {

            String detailsSql = "select applicant_name,mobile_number, applicant_address, event_id, event_date, event_desc, no_of_days, "+
                    "total_poles, deposit, total_cost, approved_by, approved_date, ae_status, rdo_status, rdo_remarks, cdate, refid, "+
                    "payment_status, refund_status, booking_status from user_request_details where id = ?";
            Map<String, Object> requestDetails = jdbcFlagPoleTemplate.queryForMap(detailsSql, new Object[] { reqId });

            String historyInsertSql = "INSERT INTO user_request_details_history ( applicant_name, mobile_number, applicant_address, "+
                    " event_id, event_date, event_desc, no_of_days, total_poles, deposit, total_cost, approved_by, " +
                    " approved_date, ae_status, rdo_status, rdo_remarks, cdate, refid, payment_status, refund_status, booking_status) "+
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            jdbcFlagPoleTemplate.update(historyInsertSql, requestDetails.get("applicant_name"),
                    requestDetails.get("mobile_number"), requestDetails.get("applicant_address"),
                    requestDetails.get("event_id"), requestDetails.get("event_date"), requestDetails.get("event_desc"),
                    requestDetails.get("no_of_days"), requestDetails.get("total_poles"), requestDetails.get("deposit"),
                    requestDetails.get("total_cost"), requestDetails.get("approved_by"),
                    requestDetails.get("approved_date"),
                    requestDetails.get("ae_status"), requestDetails.get("rdo_status"),
                    requestDetails.get("rdo_remarks"), requestDetails.get("cdate"),
                    requestDetails.get("refid"), requestDetails.get("payment_status"),
                    requestDetails.get("refund_status"),
                    requestDetails.get("booking_status"));

            String updateSql = "UPDATE user_request_details SET ae_status = 'APPROVED', approved_by = ?, approved_date = NOW() WHERE id = ?";
            int rowsAffected = jdbcFlagPoleTemplate.update(updateSql, approvedBy, reqId);

            if (rowsAffected > 0) {
                return Collections.singletonMap("status", "Request approved successfully");
            } else {
                return Collections.singletonMap("status", "No request found with the given ID");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return Collections.singletonMap("status", "Error occurred while approving the request");

        }
    }

    public List<Map<String, Object>> getRDOApprovedRequests(String userid) {
        String ward = getWardByLoginId(userid, "ae");

        try {
            String sql = "select urd.id, urd.refid, urd.applicant_name, urd.mobile_number, urd.total_poles, " +
                    "date_format(urd.cdate,'%d-%m-%Y %l:%i %p') as req_rec_date, count(sd.id) as streetCount, " +
                    "DATE_FORMAT(urd.event_date,'%d-%m-%Y') AS eventStartDate, DATE_FORMAT(DATE_ADD(urd.event_date, INTERVAL urd.no_of_days DAY),'%d-%m-%Y') AS eventEndDate "+
                    "from user_request_details urd " +
                    "left join street_details sd on urd.refid = sd.refid " +
                    "where urd.payment_status = 'COMPLETED' and sd.ward =? and urd.rdo_status = 'APPROVED' "+ 
                    " AND sd.is_active =1 and sd.is_delete =0 "+
                    "and urd.ae_status IS NOT NULL and urd.id not in (select ref_id from restoration) "+
                    "group by urd.id, urd.refid, urd.applicant_name, urd.mobile_number,urd.total_poles, urd.cdate ";
            List<Map<String, Object>> requestList = jdbcFlagPoleTemplate.queryForList(sql, new Object[] { ward });
            return requestList;
        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "No data found");
            return Collections.singletonList(response);

        }
    }

    public Map<String, Object> getRDOApprovedRequestsDetails(String reqId) {

        try {
            String sql = "select urd.id, urd.refid, urd.applicant_name, urd.mobile_number, urd.total_poles, " +
                    "date_format(urd.cdate,'%d-%m-%Y %l:%i %p') as req_rec_date, urd.applicant_address, urd.event_desc, "
                    +
                    "count(sd.id) as streetCount, DATE_FORMAT(urd.event_date,'%d-%m-%Y') AS eventStartDate, " +
                    "DATE_FORMAT(DATE_ADD(urd.event_date, INTERVAL urd.no_of_days DAY),'%d-%m-%Y') AS eventEndDate, " +
                    "ifnull (urd.rdo_status,'') as rdo_status, ifnull(urd.rdo_remarks,'') as rdo_remarks " +
                    "from user_request_details urd " +
                    "left join street_details sd on urd.refid = sd.refid " +
                    "where urd.payment_status = 'COMPLETED' and urd.id = ? and urd.rdo_status = 'APPROVED' and urd.ae_status IS NOT NULL "+
                    "group by urd.id, urd.refid, urd.applicant_name, urd.mobile_number,urd.total_poles, urd.cdate ";
            Map<String, Object> requestList = jdbcFlagPoleTemplate.queryForMap(sql, reqId);

            String streetSql = "select sd.id as streetDetailsId, urd.id as reqid, sd.street_name, sd.zone, sd.ward, sd.no_of_poles, "+
                    "sd.height, pm.pole_material_name, fm.flag_material_name, sd.pole_space, sd.no_of_days " +
                    "from street_details sd " +
                    "left join user_request_details urd on urd.refid = sd.refid " +
                    "left join pole_material pm on sd.pole_material_id = pm.id " +
                    "left join flag_material fm on sd.flag_material_id = fm.id " +
                    "where urd.id = ? AND sd.is_active =1 and sd.is_delete =0 ";
            List<Map<String, Object>> streetDetails = jdbcFlagPoleTemplate.queryForList(streetSql,
                    new Object[] { reqId });

            requestList.put("streetDetails", streetDetails);

            return requestList;
        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "No data found");
            return Collections.singletonMap("status", "No data found");

        }
    }

    public String submitRestorationDetails(String reqId, String amount, String restorImgPath, String remarks,
            String userid) {
        try {
            String sql = "Insert into restoration (ref_id, amount, image_path, remarks, created_by) values(?,?,?,?,?)";
            int rows = jdbcFlagPoleTemplate.update(sql, reqId, amount, restorImgPath, remarks, userid);
            if (rows > 0) {
                return "Restoration details submitted successfully";
            } else {
                return "Failed to submit restoration details";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error occurred while submitting restoration details";
        }

    }

    public List<Map<String, Object>> getRestorationList(String userid) {

        String ward = getWardByLoginId(userid, "ae");

        try {
            String sql = "select urd.id, urd.refid, urd.applicant_name, urd.mobile_number, urd.total_poles, " +
                    "date_format(urd.cdate,'%d-%m-%Y %l:%i %p') as req_rec_date, count(sd.id) as streetCount, " +
                    "DATE_FORMAT(urd.event_date,'%d-%m-%Y') AS eventStartDate, DATE_FORMAT(DATE_ADD(urd.event_date, INTERVAL urd.no_of_days DAY),'%d-%m-%Y') AS eventEndDate, "+
                    "r.amount as restorationAmount "+
                    "from user_request_details urd " +
                    "left join street_details sd on urd.refid = sd.refid " +
                    "left join restoration r on urd.id = r.ref_id  "+
                    "where urd.payment_status = 'COMPLETED' and sd.ward =? and urd.rdo_status = 'APPROVED' "+ 
                    "and urd.ae_status IS NOT NULL and urd.id in (select ref_id from restoration) "+
                    " AND sd.is_active =1 and sd.is_delete =0 AND r.is_active = 1 "+
                    "group by urd.id, urd.refid, urd.applicant_name, urd.mobile_number,urd.total_poles, urd.cdate, r.amount ";
            List<Map<String, Object>> requestList = jdbcFlagPoleTemplate.queryForList(sql, new Object[] { ward });
            return requestList;
        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "No data found");
            return Collections.singletonList(response);

        }


    }

    public Map<String, Object> getRestorationDetailsById(String reqId) {

        try {
            String sql = "select urd.id, urd.refid, urd.applicant_name, urd.mobile_number, urd.total_poles, " +
                    "date_format(urd.cdate,'%d-%m-%Y %l:%i %p') as req_rec_date, urd.applicant_address, urd.event_desc, "+
                    "count(sd.id) as streetCount, DATE_FORMAT(urd.event_date,'%d-%m-%Y') AS eventStartDate, " +
                    "DATE_FORMAT(DATE_ADD(urd.event_date, INTERVAL urd.no_of_days DAY),'%d-%m-%Y') AS eventEndDate, " +
                    "ifnull (urd.rdo_status,'') as rdo_status, ifnull(urd.rdo_remarks,'') as rdo_remarks, " +
                    "r.amount as restorationAmount, r.payment_status as restoration_pay_status, r.remarks as restorationRemarks, "+
                    "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', r.image_path) AS restoration_image_url " +
                    "from user_request_details urd " +
                    "left join street_details sd on urd.refid = sd.refid " +
                    "left join restoration r on urd.id = r.ref_id  "+
                    "where urd.payment_status = 'COMPLETED' and urd.id = ? and urd.rdo_status = 'APPROVED' and "+
                    "urd.ae_status IS NOT NULL AND r.is_active = 1 "+
                    "group by urd.id, urd.refid, urd.applicant_name, urd.mobile_number,urd.total_poles, urd.cdate, r.amount, "+
                    "r.payment_status, r.remarks, r.image_path ";
            Map<String, Object> requestList = jdbcFlagPoleTemplate.queryForMap(sql, reqId);

            String streetSql = "select sd.id as streetDetailsId, urd.id as reqid, sd.street_name, sd.zone, sd.ward, sd.no_of_poles, "+
                    "sd.height, pm.pole_material_name, fm.flag_material_name, sd.pole_space, sd.no_of_days " +
                    "from street_details sd " +
                    "left join user_request_details urd on urd.refid = sd.refid " +
                    "left join pole_material pm on sd.pole_material_id = pm.id " +
                    "left join flag_material fm on sd.flag_material_id = fm.id " +
                    "where urd.id = ? AND sd.is_active =1 and sd.is_delete =0 ";
            List<Map<String, Object>> streetDetails = jdbcFlagPoleTemplate.queryForList(streetSql,
                    new Object[] { reqId });

            requestList.put("streetDetails", streetDetails);

            return requestList;
        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "No data found");
            return Collections.singletonMap("status", "No data found");

        }


    }

    @Transactional
    public String saveFineDetails(String vName, String mobNo, String fineImgPath, String address, String zone,
            String ward, String latitude, String longitude, String noofflagpoles, String fineAmount, String remarks,
            String userid, String eventType) {
        try {

            String sql = "Insert into fine_collection (user_name, mob_no, image_path, street_name, zone, ward, latitude, longitude, "+
                    "no_of_poles, fine_amount, total_amount, remarks, created_by, event_id) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcFlagPoleTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, vName);
                ps.setLong(2, Long.parseLong(mobNo));
                ps.setString(3, fineImgPath);
                ps.setString(4, address);
                ps.setString(5, zone);
                ps.setString(6, ward);
                ps.setString(7, latitude);
                ps.setString(8, longitude);
                ps.setInt(9, Integer.parseInt(noofflagpoles));
                ps.setString(10, fineAmount);
                ps.setString(11, fineAmount);
                ps.setString(12, remarks);
                ps.setString(13, userid);
                ps.setString(14, eventType);
                return ps;
            }, keyHolder);

            int generatedId = keyHolder.getKey().intValue();
            String requestId = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + generatedId;

            jdbcFlagPoleTemplate.update("UPDATE fine_collection SET ref_id = ? WHERE id = ?", requestId, generatedId);

            return "Fine details saved successfully with Request ID: " + requestId;

        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error occurred while saving fine details"+ex.getMessage();
        }
    }

    public List<Map<String, Object>> getFineList(String userid) {
        String ward = getWardByLoginId(userid, "ae");

        try {
            String sql = "select fc.id, fc.ref_id, fc.user_name, fc.mob_no, fc.no_of_poles, " +
                    "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', fc.image_path) AS fine_image_url, " +
                    "ifnull(CONCAT('"+fileBaseUrl+"/gccofficialapp/files', fc.fir_image_path),'N/A') AS FIR_image_url, " +
                    "date_format(fc.cdate,'%d-%m-%Y %l:%i %p') as fine_rec_date, fc.fine_amount, fc.total_amount, "+
                    "fc.street_name, fc.zone, fc.ward, fc.remarks, fc.payment_status "+
                    "from fine_collection fc " +
                    "where fc.ward =? and isactive =1 and isdelete =0 "+
                    "group by fc.id, fc.ref_id, fc.user_name, fc.mob_no,fc.no_of_poles, fc.cdate, fc.fine_amount, fc.total_amount ";
            List<Map<String, Object>> fineList = jdbcFlagPoleTemplate.queryForList(sql, new Object[] { ward });
            return fineList;
        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "No data found");
            return Collections.singletonList(response);

        }
    }

    public String submitFIRDetails(String refid, String firImgPath, String userid, String fir_remarks) {
        try {
            String sql = "Update fine_collection set fir_image_path = ?, fir_by = ?, fir_date = NOW(), fir_remarks = ? "+
                    "where id = ?";
            int rows = jdbcFlagPoleTemplate.update(sql, firImgPath, userid, refid, fir_remarks);
            System.out.println("rows..."+rows);
            if (rows > 0) {
                return "FIR details submitted successfully";
            } else {
                return "Failed to submit FIR details";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error occurred while submitting FIR details";
        }
    }

}
