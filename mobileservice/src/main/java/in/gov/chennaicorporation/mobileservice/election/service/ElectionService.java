package in.gov.chennaicorporation.mobileservice.election.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class ElectionService {

    @Autowired
    JdbcTemplate jdbcElectionTemplate;

    private Environment environment;

    private String fileBaseUrl;

    @Autowired
    public void setDataSource(@Qualifier("mysqlElectionDataSource") DataSource electionDataSource) {
        this.jdbcElectionTemplate = new JdbcTemplate(electionDataSource);
    }

    public ElectionService(Environment environment) {
        this.environment = environment;
        this.fileBaseUrl = environment.getProperty("fileBaseUrl");
    }

    @Transactional
    public List<Map<String, Object>> getApplicantsByMobile(String slno) {

        try {

            String sql = "SELECT " +

            /* Person Details */
                    "ppn.id AS person_id, " +
                    "ppn.slno, " +
                    "UPPER(ppn.name) AS name, " +
                    "ppn.age, " +
                    "ppn.sex, " +
                    "ppn.r_mobile_no, " +
                    "ppn.r_phone_no, " +
                    "ppn.r_address1, " +
                    "ppn.r_address2, " +
                    "ppn.r_address3, " +
                    "ppn.r_pincode, " +
                    "ppn.batch_no," +

                    /* Department */
                    "ppn.dept AS dept_id, " +
                    "dm.dept_name, " +

                    /* Designation */
                    "ppn.designation_id AS designation_id, " +
                    "d.new_designation AS designation_name, " +

                    /* Office */
                    "ppn.office_id AS office_id, " +
                    "od.office_name, " +

                    /* Image */
                    "CONCAT('" + fileBaseUrl + "/election/files', ppm.image_path) AS img_full_path, " +

                    /* Office Full Address */
                    "CONCAT( " +
                    "COALESCE(od.address1, ''), ' ', " +
                    "COALESCE(od.address2, ''), ' ', " +
                    "COALESCE(od.address3, ''), ' ', " +
                    "COALESCE(od.pincode, '') " +
                    ") AS office_address, " +

                    /* Constituencies */
                    "ppn.native_ac_no, ac_native.ac_name AS native_constituency, " +
                    "ppn.reside_ac_no, ac_reside.ac_name AS residential_constituency, " +
                    "ppn.elector_ac_no, ac_elector.ac_name AS elector_constituency, " +
                    "od.work_ac_no, ac_work.ac_name AS working_constituency, " +
                    "ppn.elector_epic_no AS elector_epic_no, " +
                    "ppn.elector_part_no AS elector_part_no, " +
                    "ppn.elector_sl_no AS elector_sl_no, " +
                    "ppn.elector_ac_no AS elector_ac_no " +

                    "FROM poll_person_new ppn " +

                    "LEFT JOIN poll_person_images ppm ON ppm.ppn_id = ppn.id " +
                    "LEFT JOIN dept_master dm ON ppn.dept = dm.dept_id " +
                    "LEFT JOIN designation d ON ppn.designation_id = d.desig_id " +
                    "LEFT JOIN office_details od ON ppn.office_id = od.id " +
                    "LEFT JOIN ac_list_all ac_native ON ac_native.ac_no = ppn.native_ac_no " +
                    "LEFT JOIN ac_list_all ac_reside ON ac_reside.ac_no = ppn.reside_ac_no " +
                    "LEFT JOIN ac_list_all ac_elector ON ac_elector.ac_no = ppn.elector_ac_no " +
                    "LEFT JOIN ac_list_all ac_work ON ac_work.ac_no = od.work_ac_no " +

                    "WHERE ppn.slno = ? " +
                    "AND ppn.is_active = 1 AND ppn.is_delete = 0";

            List<Map<String, Object>> data = jdbcElectionTemplate.queryForList(sql, slno);

            // if (data.isEmpty()) {
            // throw new RuntimeException("Your details are not registered");
            // }

            /* 🔥 Convert all NULL values to empty string */
            for (Map<String, Object> row : data) {
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (entry.getValue() == null) {
                        entry.setValue("");
                    }
                }
            }

            return data;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching applicant details: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getBatchDetails(String mobileNo) {

        try {

            String sql = "SELECT r_mobile_no AS mobile_number, slno " +
                    "FROM poll_person_new " +
                    "WHERE r_mobile_no = ?";

            List<Map<String, Object>> data = jdbcElectionTemplate.queryForList(sql, mobileNo);

            for (Map<String, Object> row : data) {

                Object slnoObj = row.get("slno");

                // Convert null → ""
                String slno = (slnoObj == null) ? "" : slnoObj.toString().trim();
                row.put("slno", slno);

                // hasLogin flag
                boolean hasLogin = !slno.isEmpty();
                row.put("hasLogin", hasLogin);

                // Convert mobile_number null → ""
                Object mobileObj = row.get("mobile_number");
                row.put("mobile_number",
                        mobileObj == null ? "" : mobileObj.toString());
            }

            return data;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching batch details: " + e.getMessage());
        }
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

    private String saveimage(MultipartFile file, String slno) {
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("polling_person_images_foldername");
        var year = DateTimeUtil.getCurrentYear();
        // var month = DateTimeUtil.getCurrentMonth();

        uploadDirectory = uploadDirectory + serviceFolderName + year + "/" + "photo";
        try {
            // Create directory if it doesn't exist
            Path directoryPath = Paths.get(uploadDirectory);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Datetime string
            String datetimetxt = DateTimeUtil.getCurrentDateTime();

            // File name
            String fileName = slno + ".jpg";
            fileName = fileName.replaceAll("\s+", ""); // Remove space on filename

            String filePath = uploadDirectory + "/" + fileName;

            String filepath_txt = "/" + serviceFolderName + year + "/" + "photo" + "/" + fileName;

            // Create a new Path object
            Path path = Paths.get(filePath);

            // Get the bytes of the file
            byte[] bytes = file.getBytes();

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50% quality

            Files.write(path, compressedBytes);

            System.out.println(filePath);
            // System.out.println(" filepath_txt=="+ filepath_txt);
            return filepath_txt;

        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to save file " + file.getOriginalFilename();
        }

    }

    public Map<String, Object> savePollPersonUpdate(
            Integer slno,
            String name,
            String designationId,
            String sex,
            String address1,
            String address2,
            String address3,
            String pincode,
            String nearPolicesta,
            String phoneNo,
            String mobileNo,
            Integer homeDist,
            Integer nativeAcNo,
            Integer resideAcNo,
            Integer electorAcNo,
            Integer electorPartNo,
            Integer electorSlNo,
            String electorEpicNo,
            String updatedBy,
            MultipartFile photo) {

        Map<String, Object> response = new HashMap<>();

        try {

            // Validate SLNO exists in main table
            String checkSql = "SELECT COUNT(*) FROM poll_person_new WHERE slno=?";
            Integer count = jdbcElectionTemplate.queryForObject(checkSql, Integer.class, slno);

            if (count == null || count == 0) {
                throw new RuntimeException("Invalid SLNO. No record found.");
            }

            // Prevent duplicate request (optional)
            String checkReqSql = "SELECT COUNT(*) FROM poll_person_updation_request WHERE slno=? AND is_delete=0";
            Integer existing = jdbcElectionTemplate.queryForObject(checkReqSql, Integer.class, slno);

            if (existing != null && existing > 0) {
                throw new RuntimeException("Update request already exists for this person");
            }

            // Save Image
            String photoUrl = null;
            if (photo != null && !photo.isEmpty()) {
                photoUrl = saveimage(photo, String.valueOf(slno));

                if ("error".equals(photoUrl)) {
                    throw new RuntimeException("Image upload failed");
                }
            }

            String insertSql = """
                        INSERT INTO poll_person_updation_request (
                            slno, name, designation_id, sex,
                            address1, address2, address3,
                            pincode, near_policesta, phone_no, mobile_no,
                            home_dist, native_ac_no, reside_ac_no,
                            elector_ac_no, elector_part_no, elector_sl_no,
                            elector_epic_no, photo_upload_url,
                            cdate, updated_date, updated_by,
                            is_active, is_delete
                        ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW(),?,1,0)
                    """;

            jdbcElectionTemplate.update(insertSql,
                    slno, name, designationId, sex,
                    address1, address2, address3,
                    pincode, nearPolicesta, phoneNo, mobileNo,
                    homeDist, nativeAcNo, resideAcNo,
                    electorAcNo, electorPartNo, electorSlNo,
                    electorEpicNo, photoUrl,
                    updatedBy);

            response.put("status", "success");
            response.put("message", "Poll person update request submitted successfully");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while saving: " + e.getMessage());
        }

        return response;
    }

}
