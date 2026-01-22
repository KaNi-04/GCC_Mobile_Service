package in.gov.chennaicorporation.mobileservice.gccpothole.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class PotholeService {


    @Autowired
    private JdbcTemplate jdbcPotHoleTemplate;

    private final Environment environment;

    private String fileBaseUrl;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final int STRING_LENGTH = 15;

    private static final Random RANDOM = new SecureRandom();


    @Autowired
    public void setDataSource(@Qualifier("mysqlGccPotHoleDataSource") DataSource potHoleDataSource) {
        this.jdbcPotHoleTemplate = new JdbcTemplate(potHoleDataSource);
    }


    public PotholeService(Environment environment) {
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
        String serviceFolderName = environment.getProperty("pothole_foldername");
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
            fileName = fileName.replaceAll("\\s+", ""); // Remove space on filename

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

    public String saveComplaintDetails(String zone, String ward, String latitudeStr, String longitudeStr, String length,
                                       String width, String area, String streetId, String streetName, String statusId,
                                       String complaintPhotoPath, String loginId, String caseId, String userLength, String userWidth,
                                       String userHeight, String riskLevel, String damage_type) {

    	double latitude = Double.parseDouble(latitudeStr);
	    double longitude = Double.parseDouble(longitudeStr);
	    /*
        String checkSql = "SELECT count(*) FROM pothole_complaint_details pcd " +
                "left join status_master sm on pcd.status_id = sm.id " +
                "WHERE pcd.street_id =? and sm.status!='Completed' and (6371008.8 * ACOS(ROUND(COS(RADIANS(?)) * COS(RADIANS(pcd.latitude)) * COS(RADIANS(pcd.longitude) - RADIANS(?)) + SIN(RADIANS(?)) * SIN(RADIANS(pcd.latitude))))) < 3";
	     */
        String checkSql = "SELECT count(*) FROM pothole_complaint_details pcd " +
        	    "LEFT JOIN status_master sm ON pcd.status_id = sm.id " +
        	    "WHERE pcd.street_id = ? " +
        	    "AND sm.status != 'Completed' " +
        	    "AND (6371008.8 * ACOS(" +
        	    "COS(RADIANS(?)) * COS(RADIANS(pcd.latitude)) * " +
        	    "COS(RADIANS(pcd.longitude) - RADIANS(?)) + " +
        	    "SIN(RADIANS(?)) * SIN(RADIANS(pcd.latitude))" +
        	    ")) < 3";
        
        Integer count = jdbcPotHoleTemplate.queryForObject(checkSql, Integer.class, new Object[]{streetId, latitude, longitude, latitude});

        if (count != null && count > 0) {
            return "duplicate";  // ✅ Return "duplicate" if at least one match found
        }


        String sql = "INSERT INTO pothole_complaint_details ("
                + "zone, ward, latitude,longitude, length, width, area,  "
                + "street_id, street_name,status_id, login_id,case_id,user_length,user_width,user_height, risk_level,damage_type)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            int result = jdbcPotHoleTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setString(1, zone);
                ps.setString(2, ward);
                ps.setString(3, latitudeStr);
                ps.setString(4, longitudeStr);
                ps.setString(5, length);
                ps.setString(6, width);
                ps.setString(7, area);
                ps.setString(8, streetId);
                ps.setString(9, streetName);
                ps.setString(10, statusId);
                //ps.setString(11, complaintPhotoPath);
                ps.setString(11, loginId);
                ps.setString(12, caseId);
                ps.setString(13, userLength);
                ps.setString(14, userWidth);
                ps.setString(15, userHeight);
                ps.setString(16, riskLevel);
                ps.setString(17, damage_type);
                return ps;
            }, keyHolder);


            //return keyHolder.getKey().intValue();
            if (result > 0) {
                int generatedId = keyHolder.getKey().intValue();
                String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String refId = prefix + generatedId;
                // update vendor table with reference ID
                String updateVendorSql = "UPDATE pothole_complaint_details SET complaint_no = ? WHERE id = ?";

                jdbcPotHoleTemplate.update(updateVendorSql, refId, generatedId);


                // ✅ insert image path into image table
                if (complaintPhotoPath != null && !complaintPhotoPath.isBlank()) {
                    String insertImageSql = "INSERT INTO complaint_images (complaint_no, complaint_image_url,image_flag) VALUES (?, ?,?	)";
                    jdbcPotHoleTemplate.update(insertImageSql, refId, complaintPhotoPath, "before");
                }
                return refId;
            }
            //return "error";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }


    //To fetch all status
    public List<Map<String, Object>> getAllStatus() {
        String sql = "SELECT id, status FROM status_master WHERE is_active = 1 AND is_delete = 0 ";
        return jdbcPotHoleTemplate.queryForList(sql);
    }


    //kani akka
//    public List<Map<String, Object>> getComplaintsByWard(String aeUserId) {
//        // Step 1: Get zone and ward
//        String Sql = "SELECT zone, ward FROM ae_login WHERE ae_userid = ?";
//        Map<String, Object> vendorData = jdbcPotHoleTemplate.queryForMap(Sql, aeUserId);
//
//
//        // String zone = String.valueOf(vendorData.get("zone"));
//        String ward = String.valueOf(vendorData.get("ward"));
//
//        // Step 2: Get complaints using zone and ward
//        String complaintsql = "SELECT cd.* ,DATE_FORMAT(cd.cdate, '%d-%m-%Y') as complaint_date,sm.status,"
//                + " CONCAT('" + fileBaseUrl + "/gccofficialapp/files',ci.complaint_image_url) as complaint_image_url "
//                + "	FROM pothole_complaint_details cd "
//                + "	 Left JOIN complaint_images ci ON ci.complaint_no = cd.complaint_no "
//                + "	 Left JOIN status_master sm ON sm.id = cd.status_id "
//                + "	 WHERE  cd.ward = ? AND cd.status_id IN (2,4)";
//
//        return jdbcPotHoleTemplate.queryForList(complaintsql, ward);
//    }


    //yogi

    public List<Map<String, Object>> getComplaintsByWard(String aeUserId) {
        String sql = "SELECT zone, ward FROM gcc_penalty_hoardings.`hoading_user_list` WHERE userid = ? AND type='ae'";
        List<Map<String, Object>> userList = jdbcPotHoleTemplate.queryForList(sql, aeUserId);

        if (userList.isEmpty()) {
            // You can return an empty list or throw a custom exception
            return Collections.emptyList(); // or handle as needed
        }

        Map<String, Object> vendorData = userList.get(0);
        String ward = String.valueOf(vendorData.get("ward"));

        String complaintSql = 
                "SELECT cd.*, " +
                        "       DATE_FORMAT(cd.cdate, '%d-%m-%Y') AS complaint_date, " +
                        "       sm.status, " +
                        "       CONCAT('" + fileBaseUrl + "/gccofficialapp/files', ci.complaint_image_url) AS complaint_image_url, " +
                        "       CONCAT('" + fileBaseUrl + "/gccofficialapp/files', vi.complaint_image_url) AS vendor_image_url " +
                        "FROM pothole_complaint_details cd " +
                        "LEFT JOIN status_master sm ON sm.id = cd.status_id " +
                        "LEFT JOIN ( " +
                        "    SELECT complaint_no, MIN(complaint_image_url) AS complaint_image_url " +
                        "    FROM complaint_images " +
                        "    GROUP BY complaint_no " +
                        ") ci ON ci.complaint_no = cd.complaint_no " +
                        "LEFT JOIN ( " +
                        "    SELECT complaint_no, MIN(complaint_image_url) AS complaint_image_url " +
                        "    FROM complaint_images " +
                        "    WHERE image_flag = 'vendor' " +
                        "    GROUP BY complaint_no " +
                        ") vi ON vi.complaint_no = cd.complaint_no " +
                        "WHERE cd.ward = ? AND cd.status_id IN (1, 2, 4, 6) AND cd.isactive=1";

        return jdbcPotHoleTemplate.queryForList(complaintSql, ward);
    }

    // yogi modified

    public List<Map<String, Object>> getComplaintsByVendorId(String vendorUserId) {
        // Step 1: Get zone and ward
        String vendorSql = "SELECT zone, ward FROM vendor_login WHERE vendor_userid = ?";
        List<Map<String, Object>> vendorList = jdbcPotHoleTemplate.queryForList(vendorSql, vendorUserId);

        // If no vendor found, return empty list
        if (vendorList.isEmpty()) {
            return Collections.emptyList();
        }
        
        Map<String, Object> vendorData = vendorList.get(0);
        String ward = String.valueOf(vendorData.get("ward"));

        // Step 2: Get complaints using ward
        String complaintSql = "SELECT cd.*, DATE_FORMAT(cd.cdate, '%d-%m-%Y') AS complaint_date, " +
                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', ci.complaint_image_url) AS complaint_image_url, " +
                "sm.status " +
                "FROM pothole_complaint_details cd " +
                "LEFT JOIN complaint_images ci ON ci.complaint_no = cd.complaint_no " +
                "LEFT JOIN status_master sm ON sm.id = cd.status_id " +
                "WHERE cd.ward = ? AND cd.status_id IN (1,3,7) AND cd.isactive=1";

        return jdbcPotHoleTemplate.queryForList(complaintSql, ward);
    }


    public List<Map<String, Object>> getFlatRequestList(String loginId) {

        String sql = "SELECT " +
                "    pcd.complaint_no, " +
                "    pcd.*, " +
                "   DATE_FORMAT(pcd.cdate, '%d-%m-%Y') as complaint_date, " +
                "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', ci.complaint_image_url) AS complaint_image_url, " +
                "    ci.image_flag, " +
                "    sm.status " +
                "FROM pothole_complaint_details pcd " +
                "JOIN complaint_images ci ON pcd.complaint_no = ci.complaint_no " +
                "LEFT JOIN status_master sm ON pcd.status_id = sm.id " +
                "WHERE pcd.login_id = ? AND pcd.isactive=1";

        return jdbcPotHoleTemplate.queryForList(sql, loginId);
    }


    public List<Map<String, Object>> getRequestList(String loginId) {
        List<Map<String, Object>> flatList = getFlatRequestList(loginId);

        Map<String, Map<String, Object>> groupedMap = new LinkedHashMap<>();

        for (Map<String, Object> row : flatList) {
            String complaintNo = (String) row.get("complaint_no");
            String imageFlag = (String) row.get("image_flag");
            String imageUrl = (String) row.get("complaint_image_url");

            Map<String, Object> existing = groupedMap.get(complaintNo);

            if (existing == null) {
                // Initialize map with all complaint fields except image fields
                Map<String, Object> base = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    String key = entry.getKey();
                    if (!key.equals("complaint_image_url") && !key.equals("image_flag")) {
                        base.put(key, entry.getValue());
                    }
                }

                // Always add image fields with null (default)
                base.put("before_image_url", null);
                base.put("vendor_image_url", null);
                base.put("reopen_image_url", null);
                base.put("rectified_image_url", null);
                base.put("completed_image_url", null);

                groupedMap.put(complaintNo, base);
                existing = base;
            }

            // Conditionally add image URL to the correct field
            if ("Before".equalsIgnoreCase(imageFlag)) {
                existing.put("before_image_url", imageUrl);
            } else if ("Vendor".equalsIgnoreCase(imageFlag)) {
                existing.put("vendor_image_url", imageUrl);
            } else if ("Reopen".equalsIgnoreCase(imageFlag)) {
                existing.put("reopen_image_url", imageUrl);
            } else if ("Rectified".equalsIgnoreCase(imageFlag)) {
                existing.put("rectified_image_url", imageUrl);
            } else if ("Completed".equalsIgnoreCase(imageFlag)) {
                existing.put("completed_image_url", imageUrl);
            }
        }

        return new ArrayList<>(groupedMap.values());
    }


    public boolean checkLocation(String latitude, String longitude) {
        String sql = "select count(*) from pothole_complaint_details pcd " +
                "where pcd.isactive=1 AND ((6371008.8 * acos(ROUND(cos(radians(?)) * cos(radians(pcd.latitude)) * cos(radians(pcd.longitude) - radians(?)) +" +
                "sin(radians(?)) * sin(radians(pcd.latitude)), 9))) )< 3 ";

        int count = jdbcPotHoleTemplate.queryForObject(sql, Integer.class, Double.parseDouble(latitude), Double.parseDouble(longitude), Double.parseDouble(latitude));

        return count > 0;
    }

    public String updateComplaintbyVendor(String statusId, 
    		String loginId, 
    		LocalDateTime updatedDate,
    		String vendorLength, 
    		String vendorWidth, 
    		String vendorHeight,
    		String vendorPhotoPath,
    		String aeLength, 
    		String aeWidth, 
    		String aeHeight,
    		String complaint_no) {
        String status = insertIntoHistory(complaint_no);
        //System.err.println("updateComplaintbyVendor - status : "+ status);
        int count = 0;
        try {
	        if ("Success".equalsIgnoreCase(status.trim())) {
	        	
	        	System.err.println("Inside Success Attempting update with: " +
	            	    "statusId=" + statusId + ", loginId=" + loginId + ", updatedDate=" + updatedDate +
	            	    ", vendorLength=" + vendorLength + ", vendorWidth=" + vendorWidth + ", vendorHeight=" + vendorHeight +
	            	    ", complaint_no=" + complaint_no);
	        	
	            if ("2".equalsIgnoreCase(statusId.trim()) || "4".equalsIgnoreCase(statusId.trim())) {
	                String updateSql = "update pothole_complaint_details set status_id =?, updated_login_id =?, updated_date =? , vendor_length = ? , vendor_width = ? , vendor_height = ? where complaint_no = ? ";
	                //System.err.println("Vendor Executing SQL: " + updateSql);
	                //System.err.printf("Params: [%s, %s, %s, %s, %s, %s, %s]%n", statusId, loginId, updatedDate, vendorLength, vendorWidth, vendorHeight, complaint_no);
	                count = jdbcPotHoleTemplate.update(updateSql, statusId, loginId, updatedDate, vendorLength, vendorWidth, vendorHeight, complaint_no);
	            } else if ("3".equalsIgnoreCase(statusId.trim()) || "5".equalsIgnoreCase(statusId.trim())) {
	                String updateSql = "update pothole_complaint_details set status_id =?, updated_login_id =?, updated_date =? , ae_length = ? , ae_width = ? , ae_height = ? where complaint_no = ? ";
	               // System.err.println("AE Executing SQL: " + updateSql);
	                //System.err.printf("Params: [%s, %s, %s, %s, %s, %s, %s]%n", statusId, loginId, updatedDate, aeLength, aeWidth, aeHeight, complaint_no);
	                count = jdbcPotHoleTemplate.update(updateSql, statusId, loginId, updatedDate, aeLength, aeWidth, aeHeight, complaint_no);
	            }
	            
	            if (count > 0) {
	
	                if ("2".equalsIgnoreCase(statusId.trim())) {
	
	                    // ✅ insert image path into image table
	                    if (vendorPhotoPath != null && !vendorPhotoPath.isBlank()) {
	                        String insertImageSql = "INSERT INTO complaint_images (complaint_no, complaint_image_url,image_flag) VALUES (?, ?,?	)";
	                        jdbcPotHoleTemplate.update(insertImageSql, complaint_no, vendorPhotoPath, "vendor");
	                    }
	
	                } else if ("4".equalsIgnoreCase(statusId.trim())) {
	
	                    if (vendorPhotoPath != null && !vendorPhotoPath.isBlank()) {
	                        String insertImageSql = "INSERT INTO complaint_images (complaint_no, complaint_image_url,image_flag) VALUES (?, ?,?	)";
	                        jdbcPotHoleTemplate.update(insertImageSql, complaint_no, vendorPhotoPath, "rectified");
	                    }
	
	                } else if ("3".equalsIgnoreCase(statusId.trim())) {
	
	                    if (vendorPhotoPath != null && !vendorPhotoPath.isBlank()) {
	                        String insertImageSql = "INSERT INTO complaint_images (complaint_no, complaint_image_url,image_flag) VALUES (?, ?,?	)";
	                        jdbcPotHoleTemplate.update(insertImageSql, complaint_no, vendorPhotoPath, "reopen");
	                    }
	                } else if ("5".equalsIgnoreCase(statusId.trim())) {
	
	                    if (vendorPhotoPath != null && !vendorPhotoPath.isBlank()) {
	                        String insertImageSql = "INSERT INTO complaint_images (complaint_no, complaint_image_url,image_flag) VALUES (?, ?,?	)";
	                        //System.err.println("complaint_images Executing SQL: " + insertImageSql);
	    	                //System.err.printf("Params: [%s, %s, %s]%n", complaint_no, vendorPhotoPath, "completed");
	                        jdbcPotHoleTemplate.update(insertImageSql, complaint_no, vendorPhotoPath, "completed");
	                    }
	
	                } else {
	                    return "Invalid Status Id";
	                }
	
	                return "Success";
	            }
	        }
        } catch (Exception e) {
            //System.err.println("Error inserting into Update: " + e.getMessage());
            e.printStackTrace(); // Shows full stack trace
            //return "Failed: " + e.getMessage();
        }
        /*
        System.err.println("Attempting update with: " +
        	    "statusId=" + statusId + ", loginId=" + loginId + ", updatedDate=" + updatedDate +
        	    ", vendorLength=" + vendorLength + ", vendorWidth=" + vendorWidth + ", vendorHeight=" + vendorHeight +
        	    ", complaint_no=" + complaint_no);
        */
        return "Failed to save the data";
    }

    private String insertIntoHistory(String complaintNo) {
        if (complaintNo == null || complaintNo.trim().isEmpty()) {
            return "Complaint No is Empty";
        }

        try {
            String selectSql = "SELECT * FROM pothole_complaint_details WHERE complaint_no = ? LIMIT 1";
            Map<String, Object> complaintDetails = jdbcPotHoleTemplate.queryForMap(selectSql, complaintNo);

            if (complaintDetails != null && !complaintDetails.isEmpty()) {
                String insertSql = "INSERT INTO pothole_complaint_details_history (" +
                        "complaint_no, zone, ward, latitude, longitude, length, width, area, street_id, street_name, status_id, cdate, updated_date, remarks, " +
                        "login_id, case_id, updated_login_id, user_length, user_width, user_height, vendor_length, vendor_width, vendor_height, ae_length, ae_width, ae_height, risk_level, " +
                        "request_by, approved_by, approved_date, approved_remarks, location_status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                jdbcPotHoleTemplate.update(insertSql,
                        complaintDetails.get("complaint_no"),
                        complaintDetails.get("zone"),
                        complaintDetails.get("ward"),
                        complaintDetails.get("latitude"),
                        complaintDetails.get("longitude"),
                        complaintDetails.get("length"),
                        complaintDetails.get("width"),
                        complaintDetails.get("area"),
                        complaintDetails.get("street_id"),
                        complaintDetails.get("street_name"),
                        complaintDetails.get("status_id"),
                        complaintDetails.get("cdate"),
                        complaintDetails.get("updated_date"),
                        complaintDetails.get("remarks"),
                        complaintDetails.get("login_id"),
                        complaintDetails.get("case_id"),
                        complaintDetails.get("updated_login_id"),
                        complaintDetails.get("user_length"),
                        complaintDetails.get("user_width"),
                        complaintDetails.get("user_height"),
                        complaintDetails.get("vendor_length"),
                        complaintDetails.get("vendor_width"),
                        complaintDetails.get("vendor_height"),
                        complaintDetails.get("ae_length"),
                        complaintDetails.get("ae_width"),
                        complaintDetails.get("ae_height"),
                        complaintDetails.get("risk_level"),
                        complaintDetails.get("request_by"),
                        complaintDetails.get("approved_by"),
                        complaintDetails.get("approved_date"),
                        complaintDetails.get("approved_remarks"),
                        complaintDetails.get("location_status")
                );

                return "Success";
            } else {
                return "Complaint Details Not Found: " + complaintNo;
            }
        } catch (Exception e) {
            System.err.println("Error inserting into history: " + e.getMessage());
            return "Failed: " + e.getMessage();
        }
    }

    public List<Map<String, Object>> getZoneWiseAEReport(String fromDate, String toDate, String damage_type) {

        String formattedFrom = formatFromDate(fromDate);
        String formattedTo = formatToDate(toDate);
        String where="";
        if(damage_type!=null)
        {
        	where=" AND pcd.`damage_type`='"+damage_type+"' ";
        }
        String sql = "SELECT pcd.zone, " +
                "COUNT(pcd.complaint_no) AS total_mapped, " +
                "SUM(CASE WHEN pcd.status_id IN (1, 2, 4, 6) THEN 1 ELSE 0 END) AS ae_pending, " +
                "SUM(CASE WHEN pcd.status_id = '5' THEN 1 ELSE 0 END) AS ae_completed " +
                "FROM pothole_complaint_details pcd " +
                "LEFT JOIN status_master st ON st.id = pcd.status_id " +
                "WHERE pcd.isactive=1 AND DATE(pcd.cdate) BETWEEN ? AND ? " + where +
                "GROUP BY pcd.zone " +
                "ORDER BY pcd.zone ";

        return jdbcPotHoleTemplate.queryForList(sql, formattedFrom, formattedTo);
    }


    public List<Map<String, Object>> getWardWiseAEReport(String fromDate, String toDate, String zone, String damage_type) {

        String formattedFrom = formatFromDate(fromDate);
        String formattedTo = formatToDate(toDate);
        String where="";
        if(damage_type!=null)
        {
        	where=" AND pcd.`damage_type`='"+damage_type+"' ";
        }
        String sql = "SELECT "
                + "pcd.zone,pcd.ward,count(pcd.complaint_no) AS total_mapped,"
                + "SUM(CASE WHEN pcd.status_id IN (1, 2, 4, 6) THEN 1 ELSE 0 END) AS ae_pending, "
                + "SUM(CASE WHEN pcd.status_id = '5' THEN 1 ELSE 0 END) AS ae_completed "
                + "FROM pothole_complaint_details pcd "
                + "LEFT JOIN status_master st ON st.id = pcd.status_id "
                + "WHERE pcd.isactive=1 AND DATE(pcd.cdate) BETWEEN ? AND ? AND pcd.zone= ? " + where 
                + "GROUP BY pcd.ward "
                + "ORDER BY pcd.ward ";

        return jdbcPotHoleTemplate.queryForList(sql, formattedFrom, formattedTo, zone);
    }

    public List<Map<String, Object>> getZoneWiseVendorReport(String fromDate, String toDate, String damage_type) {

        String formattedFrom = formatFromDate(fromDate);
        String formattedTo = formatToDate(toDate);
        String where="";
        if(damage_type!=null)
        {
        	where=" AND pcd.`damage_type`='"+damage_type+"' ";
        }
        String sql = "SELECT "
                + "pcd.zone,count(pcd.complaint_no) AS total_mapped,"
                + "SUM(CASE WHEN pcd.status_id IN ('1','3')  THEN 1 ELSE 0 END) AS vendor_pending,"
                + "SUM(CASE WHEN pcd.status_id IN ('2','4') THEN 1 ELSE 0 END) AS vendor_completed "
                + "FROM pothole_complaint_details pcd "
                + "LEFT JOIN status_master st ON st.id = pcd.status_id "
                + "WHERE pcd.isactive=1 AND DATE(pcd.cdate) BETWEEN ? AND ? " + where
                + "GROUP BY pcd.zone "
                + "ORDER BY pcd.zone ";

        return jdbcPotHoleTemplate.queryForList(sql, formattedFrom, formattedTo);
    }


    public List<Map<String, Object>> getWardWiseVendorReport(String fromDate, String toDate, String zone, String damage_type) {

        String formattedFrom = formatFromDate(fromDate);
        String formattedTo = formatToDate(toDate);
        String where="";
        if(damage_type!=null)
        {
        	where=" AND pcd.`damage_type`='"+damage_type+"' ";
        }
        String sql = "SELECT "
                + "pcd.zone,pcd.ward,count(pcd.complaint_no) AS total_mapped,"
                + "SUM(CASE WHEN pcd.status_id IN ('1','3')  THEN 1 ELSE 0 END) AS vendor_pending,"
                + "SUM(CASE WHEN pcd.status_id IN ('2','4') THEN 1 ELSE 0 END) AS vendor_completed "
                + "FROM pothole_complaint_details pcd "
                + "LEFT JOIN status_master st ON st.id = pcd.status_id "
                + "WHERE pcd.isactive=1 AND DATE(pcd.cdate) BETWEEN ? AND ? AND pcd.zone=? " + where
                + "GROUP BY pcd.ward "
                + "ORDER BY pcd.ward ";

        return jdbcPotHoleTemplate.queryForList(sql, formattedFrom, formattedTo, zone);
    }


    // get the details by complaint_no


    public List<Map<String, Object>> getComplaintList(String complaintNo) {

        String sql = "select pcd.complaint_no,pcd.zone,pcd.ward,pcd.latitude,pcd.longitude, "
                + "pcd.street_id,pcd.street_name,pcd.case_id,pcd.user_length,pcd.user_width,pcd.user_height, "
                + "pcd.vendor_length,pcd.vendor_width,pcd.vendor_height,pcd.ae_length,pcd.ae_width,pcd.ae_height, "
                + "st.status, "
                + "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', ci.complaint_image_url) AS complaint_image_url, "
                + "ci.image_flag "
                + "from pothole_complaint_details pcd "
                + "LEFT JOIN complaint_images ci on ci.complaint_no=pcd.complaint_no "
                + "LEFT JOIN status_master st on st.id=pcd.status_id "
                + "WHERE pcd.complaint_no= ? AND pcd.isactive=1 ";

        return jdbcPotHoleTemplate.queryForList(sql, complaintNo);
    }


    public List<Map<String, Object>> getDetailsByComplaintNo(String complaintNo) {
        List<Map<String, Object>> complaintList = getComplaintList(complaintNo);

        Map<String, Map<String, Object>> groupedMap = new LinkedHashMap<>();

        for (Map<String, Object> row : complaintList) {
            String complaintnumber = (String) row.get("complaint_no");
            String imageFlag = (String) row.get("image_flag");
            String imageUrl = (String) row.get("complaint_image_url");

            Map<String, Object> existing = groupedMap.get(complaintnumber);

            if (existing == null) {
                // Initialize map with all complaint fields except image fields
                Map<String, Object> base = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    String key = entry.getKey();
                    if (!key.equals("complaint_image_url") && !key.equals("image_flag")) {
                        base.put(key, entry.getValue());
                    }
                }

                // Always add image fields with null (default)
                base.put("before_image_url", null);
                base.put("vendor_image_url", null);
                base.put("reopen_image_url", null);
                base.put("rectified_image_url", null);
                base.put("completed_image_url", null);

                groupedMap.put(complaintnumber, base);
                existing = base;
            }

            // Conditionally add image URL to the correct field
            if ("Before".equalsIgnoreCase(imageFlag)) {
                existing.put("before_image_url", imageUrl);
            } else if ("Vendor".equalsIgnoreCase(imageFlag)) {
                existing.put("vendor_image_url", imageUrl);
            } else if ("Reopen".equalsIgnoreCase(imageFlag)) {
                existing.put("reopen_image_url", imageUrl);
            } else if ("Rectified".equalsIgnoreCase(imageFlag)) {
                existing.put("rectified_image_url", imageUrl);
            } else if ("Completed".equalsIgnoreCase(imageFlag)) {
                existing.put("completed_image_url", imageUrl);
            }
        }

        return new ArrayList<>(groupedMap.values());
    }


    // get the details by between date and zone,ward

    public List<Map<String, Object>> getAllComplaints(String fromDate, String toDate, String zone, String ward, String streetId, String damage_type, String status) {

        String formattedFrom = formatFromDate(fromDate);
        String formattedTo = formatToDate(toDate);
        String where="";
        if(damage_type!=null)
        {
        	where +=" AND pcd.`damage_type`='"+damage_type+"' ";
        }
        if(status!=null)
        {
        	if (status.equalsIgnoreCase("pending")) {
        		where+=" AND pcd.status_id IN (1, 2, 4, 6) ";
        	}
        	if (status.equalsIgnoreCase("completed")) {
        		where+=" AND pcd.status_id IN (5) ";
        	}
        }
        
        String sql = "select pcd.complaint_no,pcd.zone,pcd.ward,pcd.latitude,pcd.longitude, "
        		+ "pcd.length,pcd.width,pcd.area, "
                + "pcd.street_id,pcd.street_name,pcd.case_id,pcd.user_length,pcd.user_width,pcd.user_height, "
                + "pcd.vendor_length,pcd.vendor_width,pcd.vendor_height,pcd.ae_length,pcd.ae_width,pcd.ae_height, "
                + "st.status, date_format(pcd.cdate, '%d-%m-%Y') as createdDate, "
                + "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', ci.complaint_image_url) AS complaint_image_url, "
                + "ci.image_flag, date_format(ci.cdate, '%d-%m-%Y') as imgDate  "
                + "from pothole_complaint_details pcd "
                + "LEFT JOIN complaint_images ci on ci.complaint_no=pcd.complaint_no "
                + "LEFT JOIN status_master st on st.id=pcd.status_id "
                + "WHERE DATE(pcd.cdate) BETWEEN ? AND ? AND pcd.zone = ? AND pcd.ward= ? and pcd.street_id = ? AND pcd.isactive=1"+ where;

        return jdbcPotHoleTemplate.queryForList(sql, formattedFrom, formattedTo, zone, ward, streetId);
    }


    public List<Map<String, Object>> getAllComplaintNo(String fromDate, String toDate, String zone, String ward, String streetId, String damage_type, String status) {
        List<Map<String, Object>> complaintList = getAllComplaints(fromDate, toDate, zone, ward, streetId, damage_type, status);

        Map<String, Map<String, Object>> groupedMap = new LinkedHashMap<>();

        for (Map<String, Object> row : complaintList) {
            String complaintnumber = (String) row.get("complaint_no");
            String imageFlag = (String) row.get("image_flag");
            String imageUrl = (String) row.get("complaint_image_url");
            String imageDate = (String) row.get("imgDate");

            Map<String, Object> existing = groupedMap.get(complaintnumber);

            if (existing == null) {
                // Initialize map with all complaint fields except image fields
                Map<String, Object> base = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    String key = entry.getKey();
                    if (!key.equals("complaint_image_url") && !key.equals("image_flag")) {
                        base.put(key, entry.getValue());
                    }
                }

                // Always add image fields with null (default)
                base.put("before_image_url", null);
                base.put("vendor_image_url", null);
                base.put("reopen_image_url", null);
                base.put("rectified_image_url", null);
                base.put("completed_image_url", null);

                groupedMap.put(complaintnumber, base);
                existing = base;
            }

            // Conditionally add image URL to the correct field
            if ("Before".equalsIgnoreCase(imageFlag)) {
                existing.put("before_image_url", imageUrl);
            } else if ("Vendor".equalsIgnoreCase(imageDate)) {
                existing.put("vendor_image_url", imageUrl);
            } else if ("Reopen".equalsIgnoreCase(imageFlag)) {
                existing.put("reopen_image_url", imageUrl);
            } else if ("Rectified".equalsIgnoreCase(imageFlag)) {
                existing.put("rectified_image_url", imageUrl);
            } else if ("Completed".equalsIgnoreCase(imageFlag)) {
                existing.put("completed_image_url", imageUrl);
            }

            if ("Vendor".equalsIgnoreCase(imageDate)) {
                existing.put("vendor_completed_date", imageDate);
            } else if ("Reopen".equalsIgnoreCase(imageDate)) {
                existing.put("reopen_date", imageDate);
            } else if ("Rectified".equalsIgnoreCase(imageDate)) {
                existing.put("rectified_date", imageDate);
            } else if ("Completed".equalsIgnoreCase(imageDate)) {
                existing.put("completed_date", imageDate);
            }
        }

        return new ArrayList<>(groupedMap.values());
    }

    public String formatFromDate(String fromDATE) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
        return fromDate.format(outputFormatter);
    }

    public String formatToDate(String toDATE) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);
        return toDate.format(outputFormatter);
    }

    public List<Map<String, Object>> getStreetDetailsAE(String fromDate, String toDate, String zone, String ward, String damage_type) {

        String formattedFrom = formatFromDate(fromDate);
        String formattedTo = formatToDate(toDate);
        String where="";
        if(damage_type!=null)
        {
        	where=" AND pcd.`damage_type`='"+damage_type+"' ";
        }
        String sql = "SELECT "
                + "pcd.street_id, pcd.street_name, pcd.zone,pcd.ward, "
                + "COUNT(*) AS total_complaints, "
                + "SUM(CASE WHEN pcd.status_id IN (1, 2, 4, 6) THEN 1 ELSE 0 END) AS ae_pending, "
                + "SUM(CASE WHEN pcd.status_id = '5' THEN 1 ELSE 0 END) AS ae_completed "
                + "FROM pothole_complaint_details pcd "
                + "WHERE pcd.isactive=1 AND DATE(pcd.cdate) BETWEEN ? AND ? AND pcd.zone = ? AND pcd.ward= ? " + where
                + "GROUP BY pcd.street_id,pcd.zone,pcd.ward "
                + "ORDER BY pcd.street_id ";
        System.err.println(sql);
        return jdbcPotHoleTemplate.queryForList(sql, formattedFrom, formattedTo, zone, ward);
    }

    public List<Map<String, Object>> getStreetDetailsVE(String fromDate, String toDate, String zone, String ward, String damage_type) {

        String formattedFrom = formatFromDate(fromDate);
        String formattedTo = formatToDate(toDate);
        String where="";
        if(damage_type!=null)
        {
        	where=" AND pcd.`damage_type`='"+damage_type+"' ";
        }

        String sql = "SELECT "
                + "pcd.street_id, pcd.street_name, pcd.zone,pcd.ward,"
                + "COUNT(*) AS total_complaints, "
                + "SUM(CASE WHEN pcd.status_id IN ('1','3')  THEN 1 ELSE 0 END) AS vendor_pending,"
                + "SUM(CASE WHEN pcd.status_id IN ('2','4') THEN 1 ELSE 0 END) AS vendor_completed "
                + "FROM pothole_complaint_details pcd "
                + "WHERE pcd.isactive=1 AND DATE(pcd.cdate) BETWEEN ? AND ? AND pcd.zone = ? AND pcd.ward= ? " + where
                + "GROUP BY pcd.street_id,pcd.zone,pcd.ward "
                + "ORDER BY pcd.street_id ";
        return jdbcPotHoleTemplate.queryForList(sql, formattedFrom, formattedTo, zone, ward);
    }

    public String saveUserComplaintDetails(String zone, String ward, String latitude, String longitude, String length,
                                           String width, String area, String streetId, String streetName, String statusId,
                                           String complaintPhotoPath, String caseId, String userLength, String userWidth,
                                           String userHeight, String riskLevel) {

        String checkSql = "SELECT count(*) FROM pothole_complaint_details pcd " +
                "left join status_master sm on pcd.status_id = sm.id " +
                "WHERE pcd.isactive=1 AND pcd.street_id =? and sm.status!='Completed' and (6371008.8 * ACOS(ROUND(COS(RADIANS(?)) * COS(RADIANS(pcd.latitude)) * COS(RADIANS(pcd.longitude) - RADIANS(?)) + SIN(RADIANS(?)) * SIN(RADIANS(pcd.latitude)), 9))) <50";

        Integer count = jdbcPotHoleTemplate.queryForObject(checkSql, new Object[]{streetId, latitude, longitude, latitude}, Integer.class);

        if (count != null && count > 0) {
            return "duplicate";  // ✅ Return "duplicate" if at least one match found
        }


        String sql = "INSERT INTO pothole_complaint_details ("
                + "zone, ward, latitude,longitude, length, width, area,  "
                + "street_id, street_name,status_id, login_id,case_id,user_length,user_width,user_height, risk_level, request_by)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            int result = jdbcPotHoleTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setString(1, zone);
                ps.setString(2, ward);
                ps.setString(3, latitude);
                ps.setString(4, longitude);
                ps.setString(5, length);
                ps.setString(6, width);
                ps.setString(7, area);
                ps.setString(8, streetId);
                ps.setString(9, streetName);
                ps.setString(10, statusId);
                //ps.setString(11, complaintPhotoPath);
                ps.setInt(11, 000);
                ps.setString(12, caseId);
                ps.setString(13, userLength);
                ps.setString(14, userWidth);
                ps.setString(15, userHeight);
                ps.setString(16, riskLevel);
                ps.setString(17, "Others");

                return ps;
            }, keyHolder);


            //return keyHolder.getKey().intValue();
            if (result > 0) {
                int generatedId = keyHolder.getKey().intValue();
                String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String refId = prefix + generatedId;
                // update vendor table with reference ID
                String updateVendorSql = "UPDATE pothole_complaint_details SET complaint_no = ? WHERE id = ?";

                jdbcPotHoleTemplate.update(updateVendorSql, refId, generatedId);


                // ✅ insert image path into image table
                if (complaintPhotoPath != null && !complaintPhotoPath.isBlank()) {
                    String insertImageSql = "INSERT INTO complaint_images (complaint_no, complaint_image_url,image_flag) VALUES (?, ?,?	)";
                    jdbcPotHoleTemplate.update(insertImageSql, refId, complaintPhotoPath, "before");
                }
                return refId;
            }
            //return "error";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }


    public List<Map<String, Object>> getPendingUserRequest(String loginId, String fromDate, String toDate) {
        try {
            String sql = "SELECT zone, ward FROM gcc_penalty_hoardings.`hoading_user_list` WHERE userid = ? AND type='ae'";
            List<Map<String, Object>> aeList = jdbcPotHoleTemplate.queryForList(sql, loginId);

            if (aeList.isEmpty()) {
                // You can return an empty list or throw a custom exception
                return Collections.emptyList(); // or handle as needed
            }

            Map<String, Object> vendorData = aeList.get(0);
            String ward = String.valueOf(vendorData.get("ward"));

            StringBuilder sqlQuery = new StringBuilder();

            sqlQuery =
                    new StringBuilder("SELECT cd.*, " +
                            "       DATE_FORMAT(cd.cdate, '%d-%m-%Y') AS complaint_date, " +
                            "       sm.status, " +
                            "       CONCAT('" + fileBaseUrl + "/gccofficialapp/files', ci.complaint_image_url) AS complaint_image_url, " +
                            "       CONCAT('" + fileBaseUrl + "/gccofficialapp/files', vi.complaint_image_url) AS vendor_image_url " +
                            "FROM pothole_complaint_details cd " +
                            "LEFT JOIN status_master sm ON sm.id = cd.status_id " +
                            "LEFT JOIN ( " +
                            "    SELECT complaint_no, MIN(complaint_image_url) AS complaint_image_url " +
                            "    FROM complaint_images " +
                            "    GROUP BY complaint_no " +
                            ") ci ON ci.complaint_no = cd.complaint_no " +
                            "LEFT JOIN ( " +
                            "    SELECT complaint_no, MIN(complaint_image_url) AS complaint_image_url " +
                            "    FROM complaint_images " +
                            "    WHERE image_flag = 'vendor' " +
                            "    GROUP BY complaint_no " +
                            ") vi ON vi.complaint_no = cd.complaint_no " +
                            "WHERE cd.isactive=1 AND cd.ward = ? AND cd.request_by ='Others' and status_id not in (7,8) ");

            List<Object> params = new ArrayList<>();
            params.add(ward);

            if (fromDate != null && !fromDate.trim().isEmpty() &&
                    toDate != null && !toDate.trim().isEmpty()) {
                sqlQuery.append(" AND DATE(cd.cdate) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') ");
                params.add(fromDate);
                params.add(toDate);
            }

            return jdbcPotHoleTemplate.queryForList(sqlQuery.toString(), params.toArray());

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList(); // or handle as needed
        }
    }

    public Map<String, Object> approveUserRequest(String loginId, String complaintNo, String remarks, String status, String locationStatus) {

        String history = insertIntoHistory(complaintNo);

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        String updateSql = "UPDATE pothole_complaint_details " +
                "SET approved_by = ?, approved_date = ?, approved_remarks = ?, status_id = ?, location_status=?  " +
                "WHERE complaint_no = ?";

        List<Object> params = new ArrayList<>();
        params.add(loginId);
        params.add(formattedDateTime);
        params.add(remarks);
        params.add(status);
        params.add(locationStatus);
        params.add(complaintNo);

        // Execute update
        int result = jdbcPotHoleTemplate.update(updateSql, params.toArray());

        if (result > 0) {
            // Fetch and return updated row
            String selectSql = "SELECT * FROM pothole_complaint_details WHERE complaint_no = ?";
            return jdbcPotHoleTemplate.queryForMap(selectSql, complaintNo);
        } else {
            return Collections.emptyMap(); // or throw an exception
        }
    }

}
