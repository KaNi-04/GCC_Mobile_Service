package in.gov.chennaicorporation.mobileservice.gccstreetvendor.service;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.DataSource;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class StreetVendorService {

    @Autowired
    private JdbcTemplate jdbcStreetVendorTemplate;

    private final Environment environment;
    private String fileBaseUrl;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();

    @Autowired
    public void setDataSource(@Qualifier("mysqlGccStreetVendorDataSource") DataSource streetVendorDataSource) {
        this.jdbcStreetVendorTemplate = new JdbcTemplate(streetVendorDataSource);
    }

    public StreetVendorService(Environment environment) {
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
        String serviceFolderName = environment.getProperty("street_vendor_foldername");
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
    
    
    
    public String getLiWardByLoginId(String loginid, String type) {
	    String sqlQuery = "SELECT `ward` FROM `officer_login_mapping` WHERE `userid` = ? AND `type` = ? AND isactive=1 LIMIT 1";
	    
	    // Query the database using queryForList
	    List<Map<String, Object>> results = jdbcStreetVendorTemplate.queryForList(sqlQuery, loginid, type);
	    
	    // Check if results is not empty and extract the ward value
	    if (!results.isEmpty()) {
	        // Extract the ward value from the first result
	        return (String) results.get(0).get("ward");
	    }
	    
	    // Handle the case where no result is found
	    return "000";  
	}
    
    public String getEEZoneByLoginId(String loginid, String type) {
	    String sqlQuery = "SELECT `zone` FROM `officer_login_mapping` WHERE `userid` = ? AND `type` = ? AND isactive=1 LIMIT 1";
	    
	    // Query the database using queryForList
	    List<Map<String, Object>> results = jdbcStreetVendorTemplate.queryForList(sqlQuery, loginid, type);
	    
	    // Check if results is not empty and extract the ward value
	    if (!results.isEmpty()) {
	        // Extract the ward value from the first result
	        return (String) results.get(0).get("zone");
	    }
	    
	    // Handle the case where no result is found
	    return "000";  
	}
    
    

    public String getWardByLoginId(String loginid, String type) {
	    String sqlQuery = "SELECT `ward` FROM `gcc_penalty_hoardings`.`hoading_user_list` WHERE `userid` = ? AND `type` = ? LIMIT 1";
	    
	    // Query the database using queryForList
	    List<Map<String, Object>> results = jdbcStreetVendorTemplate.queryForList(sqlQuery, loginid, type);
	    
	    // Check if results is not empty and extract the ward value
	    if (!results.isEmpty()) {
	        // Extract the ward value from the first result
	        return (String) results.get(0).get("ward");
	    }
	    
	    // Handle the case where no result is found
	    return "000";  // or return null based on your needs
	}
    
    //To fetch all social category
    public List<Map<String,Object>> getAllSocialCategories() {
        String sql = "SELECT id, category_name FROM social_category_master WHERE is_active = true AND is_delete = false";
        return jdbcStreetVendorTemplate.queryForList(sql);
    }

    //To fetch all vending Type
    public List<Map<String,Object>> getAllVendingType() {
        String sql = "SELECT typeid, vending_type_name FROM vending_type_master WHERE is_active = true AND is_delete = false";
        return jdbcStreetVendorTemplate.queryForList(sql);
    }
    
    //To fetch all vending Type Sub
    public List<Map<String,Object>> getAllVendingTypeSub(String typeid) {
        String sql = "SELECT subtypeid, vending_sub_type_name FROM vending_sub_type_master WHERE typeid = ? AND is_active = true AND is_delete = false";
        return jdbcStreetVendorTemplate.queryForList(sql, typeid);
    }
    
    //To fetch all vending category
    public List<Map<String,Object>> getAllVendingCategories() {
        String sql = "SELECT id, vending_category_name FROM vending_category_master WHERE is_active = true AND is_delete = false";
        return jdbcStreetVendorTemplate.queryForList(sql);
    }

    // To save Vendor details
    public String saveStreetVendorDetails(String name, String fhname, String gender, String dob, String socialCategory,
			boolean diffAbled, String mobNo, String aadharNo, String presentAddress, String presentDistrict,
			String presentPincode, String vendingAddress, String vendingDistrict, String vendingPincode,
			String vendingCategory, String vendingSpace, boolean bankAccStatus, String zone, String ward,
			boolean pmSvanidhiLoan, String bankPassbookPath, String rationCardPath, String streetVendorPhotoPath,
			String aadharFrontPhotoPath, String aadharBackPhotoPath,
			int educationStatus, int maritalStatus, int noOfFamMem,boolean areAnyFamMemInvolStrVen) {

    	
		String sql = "INSERT INTO vendor_details ("
				+ "name, f_h_name, gender, dob, social_category, diff_abled, mob_no, aadhar_no, "
				+ "present_address, present_district, present_pincode, vending_address, vending_district, vending_pincode, "
				+ "vending_category, vending_space, bank_acc_status, zone, ward, pm_svanidhi_loan, "
				+ "bank_passbook, ration_card_photo, street_vendor_photo, aadhar_front_photo, aadhar_back_photo, "
				+ "education_status, marital_status, no_of_fam_mem, are_any_fam_mem_invol_str_ven"
				+ ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			int result = jdbcStreetVendorTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, new String[] { "id" });
				ps.setString(1, name);
				ps.setString(2, fhname);
				ps.setString(3, gender);
				ps.setString(4, dob);
				ps.setString(5, socialCategory);
				ps.setBoolean(6, diffAbled);
				ps.setString(7, mobNo);
				ps.setString(8, aadharNo);
				ps.setString(9, presentAddress);
				ps.setString(10, presentDistrict);
				ps.setString(11, presentPincode);
				ps.setString(12, vendingAddress);
				ps.setString(13, vendingDistrict);
				ps.setString(14, vendingPincode);
				ps.setString(15, vendingCategory);
				ps.setString(16, vendingSpace);
				ps.setBoolean(17, bankAccStatus);
				ps.setString(18, zone);
				ps.setString(19, ward);
				ps.setBoolean(20, pmSvanidhiLoan);
				ps.setString(21, bankPassbookPath);
				ps.setString(22, rationCardPath);
				ps.setString(23, streetVendorPhotoPath);
				ps.setString(24, aadharFrontPhotoPath);
				ps.setString(25, aadharBackPhotoPath);
				ps.setInt(26, educationStatus);
	            ps.setInt(27, maritalStatus);
	            ps.setInt(28, noOfFamMem);
	            ps.setBoolean(29, areAnyFamMemInvolStrVen);
				return ps;
			}, keyHolder);
			//return keyHolder.getKey().intValue();
			if (result > 0) {
	            int generatedId = keyHolder.getKey().intValue();
	            String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
	            String refId = prefix + generatedId;

	            String updateSql = " UPDATE vendor_details SET vendor_req_id = ? WHERE id = ? ";
	            jdbcStreetVendorTemplate.update(updateSql,refId, generatedId);
	            return refId;
	        }
	        //return "error";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "error";
	}

    // To fetch street vendor details
    @Transactional
    public List<Map<String, Object>> getAllVendorDetails_l1(String loginid) {
    	
    	String wardString = getLiWardByLoginId(loginid,"li");
    	   	
        //System.out.println("wardString=="+wardString);
        
        List<String> wardList = Arrays.stream(wardString.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        String placeholders = wardList.stream()
                .map(w -> "?")
                .collect(Collectors.joining(","));
    	
        StringBuilder query = new StringBuilder(
                                    "   SELECT vd.id, vd.name, vd.f_h_name, vd.gender, vd.zone, vd.ward," +
                                    "   vd.dob,vd.mob_no, vd.vending_space, vd.pm_svanidhi_loan, vd.bank_acc_status, " +
                                    " 	vd.diff_abled, scm.category_name, vcm.vending_category_name," +
                                    " 	em.education, msm.marital_status, vd.no_of_fam_mem, CASE "
                                    + "    WHEN vd.are_any_fam_mem_invol_str_ven = TRUE THEN 'Yes' "
                                    + "    ELSE 'No' "
                                    + "  END AS family_member_involved, " +
                                    "   CONCAT(vd.present_district, ', ', vd.present_pincode, ', ', vd.present_address) AS residental_address," +
                                    "   CONCAT(vd.vending_district, ', ', vd.vending_pincode, ', ', vd.vending_address) AS business_address," +
                                    
                                    "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files',vd.street_vendor_photo) as street_vendor_photo," +
                                    "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files',vd.aadhar_front_photo) as aadhar_front_photo," +
                                    "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files',vd.aadhar_back_photo) as aadhar_back_photo," +
                                    "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files',vd.ration_card_photo) as ration_card_photo," +
                                    "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files',vd.bank_passbook) as bank_passbook," +
                                    
                                    "   DATE_FORMAT(vd.cdate, '%d-%m-%Y') as request_date" +
                                    "   FROM vendor_details vd" +
                                    "   LEFT JOIN social_category_master scm ON vd.social_category=scm.id" +
                                    "   LEFT JOIN vending_category_master vcm ON vd.vending_category=vcm.id " +
                                    "   LEFT JOIN `education_master` em ON vd.education_status=em.id " +
                                    "   LEFT JOIN `marital_status_master` msm ON vd.marital_status=msm.id " +
                                    " WHERE vd.ward IN (" + placeholders + ") "
                                    + "AND vd.id NOT IN (SELECT vdid FROM vendor_approval_level_1)");

        return jdbcStreetVendorTemplate.queryForList(query.toString(),wardList.toArray());
    }


    public List<Map<String, Object>> getAllVendingTimes() {
        String sql = " SELECT id, vending_time FROM vending_time_master WHERE is_active = 1 AND is_delete = 0 ";
        return jdbcStreetVendorTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getAllMaritalStatus() {
        String sql = " SELECT id, marital_status FROM marital_status_master WHERE is_active = 1 AND is_delete = 0 ";
        return jdbcStreetVendorTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getAllTransactionModes() {
        String sql = "SELECT id, transaction_mode FROM transaction_mode_master WHERE is_active = 1 AND is_delete = 0";
        return jdbcStreetVendorTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getAllEducationSatus() {
        String sql = "SELECT id, education FROM education_master WHERE is_active = 1 AND is_delete = 0";
        return jdbcStreetVendorTemplate.queryForList(sql);
    }
    
    public String saveFirstApproval(String loginid, String vdid, String vendor_type, String vendor_sub_type, String latitude, String longitude,
        boolean vendor_id_card, String vending_nature, String vending_time, 
        String transaction_mode,String svanidhi_loan_amount,
        String status, String remarks, String vendor_space_path) {
	
    	String sql = "INSERT INTO vendor_approval_level_1 ("
    	           + "vendor_type, latitude, longitude, vendor_id_card, vending_nature, vendor_timing_id, transaction_mode_id, swanidi_loan_amount, status, remarks, "
    	           + "vendor_space_photo, vdid, cby, vendor_sub_type"
    	           + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			KeyHolder keyHolder = new GeneratedKeyHolder();
			
			
			int result = jdbcStreetVendorTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, vendor_type);
				ps.setString(2, latitude);
				ps.setString(3, longitude);
				ps.setBoolean(4, vendor_id_card);
				ps.setString(5, vending_nature);
				ps.setString(6, vending_time);
				
				ps.setString(7, transaction_mode);
				ps.setString(8, svanidhi_loan_amount);
				ps.setString(9, status);
				ps.setString(10, remarks);
				ps.setString(11, vendor_space_path);
				ps.setString(12, vdid);
				ps.setString(13, loginid);
				ps.setString(14, vendor_sub_type);
				
				return ps;
			}, keyHolder);
			
			if (result > 0) {
				int generatedId = keyHolder.getKey().intValue();
				String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
				String refId = prefix + generatedId;
				
				String updateSql = " UPDATE vendor_details SET editable = 0 WHERE id = ? ";
				jdbcStreetVendorTemplate.update(updateSql,vdid);
				return refId;
			}
			
		return "error";
	}
    
    // To fetch street vendor details
    @Transactional
    public List<Map<String, Object>> getAllVendorDetails_l2(String loginid) {

        String wardString  = getLiWardByLoginId(loginid, "aro");
        //System.out.println("wardString=="+wardString);
        
        List<String> wardList = Arrays.stream(wardString.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        String placeholders = wardList.stream()
                .map(w -> "?")
                .collect(Collectors.joining(","));
        
        

        StringBuilder query = new StringBuilder(
            "SELECT " +
            "   vd.id AS vendor_id, vd.name, vd.f_h_name, vd.gender, vd.zone, vd.ward, " +
            "   vd.dob, vd.mob_no, vd.vending_space, vd.pm_svanidhi_loan, vd.bank_acc_status, " +
            "   vd.diff_abled, scm.category_name, vcm.vending_category_name, " +
            "   em.education, msm.marital_status, vd.no_of_fam_mem, " +
            "   CASE WHEN vd.are_any_fam_mem_invol_str_ven = TRUE THEN 'Yes' ELSE 'No' END AS family_member_involved, " +
            "   CONCAT(vd.present_district, ', ', vd.present_pincode, ', ', vd.present_address) AS residental_address, " +
            "   CONCAT(vd.vending_district, ', ', vd.vending_pincode, ', ', vd.vending_address) AS business_address, " +
            "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files', vd.street_vendor_photo) AS street_vendor_photo, " +
            "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files', vd.aadhar_front_photo) AS aadhar_front_photo, " +
            "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files', vd.aadhar_back_photo) AS aadhar_back_photo, " +
            "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files', vd.ration_card_photo) AS ration_card_photo, " +
            "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files', vd.bank_passbook) AS bank_passbook, " +
            "   DATE_FORMAT(vd.cdate, '%d-%m-%Y') AS request_date, " +

            // Inline L1 fields
            "   l1.id AS l1_id, l1.vendor_space_photo, l1.vendor_type,l1.vendor_sub_type, l1.latitude AS l1_latitude, l1.longitude AS l1_longitude, " +
            "   l1.vendor_id_card, l1.vending_nature, l1.vendor_timing_id, l1.transaction_mode_id, l1.swanidi_loan_amount, " +
            "   l1.status AS l1_status, l1.remarks AS l1_remarks, l1.cby AS l1_cby, " +
            "   DATE_FORMAT(l1.cdate, '%d-%m-%Y %h:%i %p') AS l1_cdate, " +
            "   DATE_FORMAT(l1.updated_date, '%d-%m-%Y %h:%i %p') AS l1_updated_date " +

            "FROM vendor_details vd " +
            "LEFT JOIN social_category_master scm ON vd.social_category = scm.id " +
            "LEFT JOIN vending_category_master vcm ON vd.vending_category = vcm.id " +
            "LEFT JOIN education_master em ON vd.education_status = em.id " +
            "LEFT JOIN marital_status_master msm ON vd.marital_status = msm.id " +
            "LEFT JOIN vendor_approval_level_1 l1 ON vd.id = l1.vdid " +
            "WHERE vd.ward IN (" + placeholders + ") " +
            "AND vd.id IN (SELECT vdid FROM vendor_approval_level_1) " +
            "AND vd.id NOT IN (SELECT vdid FROM vendor_approval_level_2) " +
            ""
        );

        List<Map<String, Object>> flatList = jdbcStreetVendorTemplate.queryForList(query.toString(), wardList.toArray());
        List<Map<String, Object>> finalList = new ArrayList<>();

        for (Map<String, Object> row : flatList) {
            Map<String, Object> vendor = new HashMap<>(row);
            Map<String, Object> l1Data = new LinkedHashMap<>();

            // Extract L1 data into sub-object
            l1Data.put("id", row.get("l1_id"));
            l1Data.put("vendor_space_photo", row.get("vendor_space_photo"));
            l1Data.put("vendor_type", row.get("vendor_type"));
            l1Data.put("vendor_sub_type", row.get("vendor_sub_type"));
            l1Data.put("latitude", row.get("l1_latitude"));
            l1Data.put("longitude", row.get("l1_longitude"));
            l1Data.put("vendor_id_card", row.get("vendor_id_card"));
            l1Data.put("vending_nature", row.get("vending_nature"));
            l1Data.put("vendor_timing_id", row.get("vendor_timing_id"));
            l1Data.put("transaction_mode_id", row.get("transaction_mode_id"));
            l1Data.put("swanidi_loan_amount", row.get("swanidi_loan_amount"));
            l1Data.put("status", row.get("l1_status"));
            l1Data.put("remarks", row.get("l1_remarks"));
            l1Data.put("cby", row.get("l1_cby"));
            l1Data.put("cdate", row.get("l1_cdate"));
            l1Data.put("updated_date", row.get("l1_updated_date"));

            // Remove L1 fields from main object
            // Explicitly remove L1 fields from parent row
            row.remove("vendor_space_photo");
            row.remove("vendor_type");
            row.remove("vendor_sub_type");
            row.remove("l1_latitude");
            row.remove("l1_longitude");
            row.remove("vendor_id_card");
            row.remove("vending_nature");
            row.remove("vendor_timing_id");
            row.remove("transaction_mode_id");
            row.remove("swanidi_loan_amount");
            row.remove("l1_status");
            row.remove("l1_remarks");
            row.remove("l1_cby");
            row.remove("l1_cdate");
            row.remove("l1_updated_date");
            row.remove("l1_id");

            // Add as sub-object
            vendor.put("vendor_approval_level_1", l1Data);

            finalList.add(vendor);
        }

        return finalList;
    }
    
    public String saveSecondApproval(String loginid, String vdid, String status, String remarks) {
	
    	String sql = "INSERT INTO vendor_approval_level_2 ("
    	           + "vdid, status, cby, remarks"
    	           + ") VALUES (?, ?, ?, ?)";
			
			KeyHolder keyHolder = new GeneratedKeyHolder();
			
			
			int result = jdbcStreetVendorTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				
				ps.setString(1, vdid);
				ps.setString(2, status);
				ps.setString(3, loginid);
				ps.setString(4, remarks);
				
				return ps;
			}, keyHolder);
			
			if (result > 0) {
				int generatedId = keyHolder.getKey().intValue();
				String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
				String refId = prefix + generatedId;
				
				//String updateSql = " UPDATE vendor_details SET vendor_req_id = ? WHERE id = ? ";
				//jdbcStreetVendorTemplate.update(updateSql,refId, generatedId);
				return refId;
			}
			
		return "error";
	}

    
    // To fetch street vendor details
    @Transactional
    public List<Map<String, Object>> getAllVendorDetails_l3(String loginid) {

        String zone = getEEZoneByLoginId(loginid, "exeeng");
        
        //System.out.println("zone="+zone);

        StringBuilder query = new StringBuilder(
            "SELECT " +
            "   vd.id AS vendor_id, vd.name, vd.f_h_name, vd.gender, vd.zone, vd.ward, " +
            "   vd.dob, vd.mob_no, vd.vending_space, vd.pm_svanidhi_loan, vd.bank_acc_status, " +
            "   vd.diff_abled, scm.category_name, vcm.vending_category_name, " +
            "   em.education, msm.marital_status, vd.no_of_fam_mem, " +
            "   CASE WHEN vd.are_any_fam_mem_invol_str_ven = TRUE THEN 'Yes' ELSE 'No' END AS family_member_involved, " +
            "   CONCAT(vd.present_district, ', ', vd.present_pincode, ', ', vd.present_address) AS residental_address, " +
            "   CONCAT(vd.vending_district, ', ', vd.vending_pincode, ', ', vd.vending_address) AS business_address, " +
            "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files', vd.street_vendor_photo) AS street_vendor_photo, " +
            "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files', vd.aadhar_front_photo) AS aadhar_front_photo, " +
            "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files', vd.aadhar_back_photo) AS aadhar_back_photo, " +
            "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files', vd.ration_card_photo) AS ration_card_photo, " +
            "   CONCAT('" + fileBaseUrl + "/gccofficialapp/files', vd.bank_passbook) AS bank_passbook, " +
            "   DATE_FORMAT(vd.cdate, '%d-%m-%Y') AS request_date, " +

            // Inline L1 fields
            "   l1.id AS l1_id, l1.vendor_space_photo, l1.vendor_type,l1.vendor_sub_type, l1.latitude AS l1_latitude, l1.longitude AS l1_longitude, " +
            "   l1.vendor_id_card, l1.vending_nature, l1.vendor_timing_id, l1.transaction_mode_id, l1.swanidi_loan_amount, " +
            "   l1.status AS l1_status, l1.remarks AS l1_remarks, l1.cby AS l1_cby, " +
            "   DATE_FORMAT(l1.cdate, '%d-%m-%Y %h:%i %p') AS l1_cdate, " +
            "   DATE_FORMAT(l1.updated_date, '%d-%m-%Y %h:%i %p') AS l1_updated_date, " +

			//Inline L2 fields
			"   l2.id AS l2_id, l2.remarks AS l2_remarks," +
			"   DATE_FORMAT(l2.cdate, '%d-%m-%Y %h:%i %p') AS l2_cdate " +
			
            "FROM vendor_details vd " +
            "LEFT JOIN social_category_master scm ON vd.social_category = scm.id " +
            "LEFT JOIN vending_category_master vcm ON vd.vending_category = vcm.id " +
            "LEFT JOIN education_master em ON vd.education_status = em.id " +
            "LEFT JOIN marital_status_master msm ON vd.marital_status = msm.id " +
            "LEFT JOIN vendor_approval_level_1 l1 ON vd.id = l1.vdid " +
            "LEFT JOIN vendor_approval_level_2 l2 ON vd.id = l2.vdid " +
            "WHERE vd.zone IN (?) " +
            "AND vd.id IN (SELECT vdid FROM vendor_approval_level_2) " +
            "AND vd.id NOT IN (SELECT vdid FROM vendor_approval_level_3) " +
            ""
            //"AND vd.id NOT IN (SELECT vdid FROM vendor_approval_level_2)"
        );

        List<Map<String, Object>> flatList = jdbcStreetVendorTemplate.queryForList(query.toString(), zone);
        List<Map<String, Object>> finalList = new ArrayList<>();

        for (Map<String, Object> row : flatList) {
            Map<String, Object> vendor = new HashMap<>(row);
            Map<String, Object> l1Data = new LinkedHashMap<>();
            Map<String, Object> l2Data = new LinkedHashMap<>();

            // Extract L1 data into sub-object
            l1Data.put("id", row.get("l1_id"));
            l1Data.put("vendor_space_photo", row.get("vendor_space_photo"));
            l1Data.put("vendor_type", row.get("vendor_type"));
            l1Data.put("vendor_sub_type", row.get("vendor_sub_type"));
            l1Data.put("latitude", row.get("l1_latitude"));
            l1Data.put("longitude", row.get("l1_longitude"));
            l1Data.put("vendor_id_card", row.get("vendor_id_card"));
            l1Data.put("vending_nature", row.get("vending_nature"));
            l1Data.put("vendor_timing_id", row.get("vendor_timing_id"));
            l1Data.put("transaction_mode_id", row.get("transaction_mode_id"));
            l1Data.put("swanidi_loan_amount", row.get("swanidi_loan_amount"));
            l1Data.put("status", row.get("l1_status"));
            l1Data.put("remarks", row.get("l1_remarks"));
            l1Data.put("cby", row.get("l1_cby"));
            l1Data.put("cdate", row.get("l1_cdate"));
            l1Data.put("updated_date", row.get("l1_updated_date"));

            
            // Remove L1 fields from main object
            // Explicitly remove L1 fields from parent row
            row.remove("vendor_space_photo");
            row.remove("vendor_type");
            row.remove("vendor_sub_type");
            row.remove("l1_latitude");
            row.remove("l1_longitude");
            row.remove("vendor_id_card");
            row.remove("vending_nature");
            row.remove("vendor_timing_id");
            row.remove("transaction_mode_id");
            row.remove("swanidi_loan_amount");
            row.remove("l1_status");
            row.remove("l1_remarks");
            row.remove("l1_cby");
            row.remove("l1_cdate");
            row.remove("l1_updated_date");
            row.remove("l1_id");

            // Extract L2 data into sub-object
            l2Data.put("l2_cdate", row.get("l2_cdate"));
            l2Data.put("l2_remarks", row.get("l2_remarks"));
            l2Data.put("l2_id", row.get("l2_id"));
            // Remove l2
            row.remove("l2_cdate");
            row.remove("l2_remarks");
            row.remove("l2_id");
            
            // Add as sub-object
            vendor.put("vendor_approval_level_1", l1Data);
            vendor.put("vendor_approval_level_2", l2Data);

            finalList.add(vendor);
        }

        return finalList;
    }
    
    public String savethirdApproval(String loginid, String vdid, String status, String remarks) {
	
    	String sql = "INSERT INTO vendor_approval_level_3 ("
    	           + "vdid, status, cby, remarks"
    	           + ") VALUES (?, ?, ?, ?)";
			
			KeyHolder keyHolder = new GeneratedKeyHolder();
			
			
			int result = jdbcStreetVendorTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				
				ps.setString(1, vdid);
				ps.setString(2, status);
				ps.setString(3, loginid);
				ps.setString(4, remarks);
				
				return ps;
			}, keyHolder);
			
			if (result > 0) {
				int generatedId = keyHolder.getKey().intValue();
				String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
				String refId = prefix + generatedId;
				
				//String updateSql = " UPDATE vendor_details SET vendor_req_id = ? WHERE id = ? ";
				//jdbcStreetVendorTemplate.update(updateSql,refId, generatedId);
				return refId;
			}
			
		return "error";
	}
    
}
