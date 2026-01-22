package in.gov.chennaicorporation.mobileservice.hoardings.services;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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

import org.hibernate.internal.build.AllowSysOut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class HoardingsService {
	private JdbcTemplate jdbcHoardingsTemplate;
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
	@Autowired
	public void setDataSource(@Qualifier("mysqlHoardingsDataSource") DataSource hoardings) {
		this.jdbcHoardingsTemplate = new JdbcTemplate(hoardings);
	}
	
	@Autowired
	public HoardingsService(Environment environment) {
		this.environment = environment;
		this.fileBaseUrl=environment.getProperty("fileBaseUrl");
	}
	
	public static String generateRandomString() {
        StringBuilder result = new StringBuilder(STRING_LENGTH);
        for (int i = 0; i < STRING_LENGTH; i++) {
            result.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return result.toString();
    }
	
	// Function to convert date string from dd-MM-yyyy to yyyy-MM-dd
	public static String convertDateFormat(String inputDate, int add) {
	    // Check if the inputDate is not null or blank
	    if (inputDate != null && !inputDate.isBlank()) {
	        try {
	            // Define the input and output date formats
	            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	            // Parse the input date string to LocalDate
	            LocalDate date = LocalDate.parse(inputDate, inputFormatter);

	            // Add the specified number of days to the date if 'add' is greater than 0
	            if (add > 0) {
	                date = date.plusDays(add);
	            }

	            // Format the date to the new format
	            return date.format(outputFormatter);

	        } catch (DateTimeParseException e) {
	            // Handle the case where the input date is invalid
	            System.out.println("Invalid date format: " + inputDate);
	            return "";
	        }
	    } else {
	        return "";
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
		
	public String fileUpload(String name, String id, String folder, MultipartFile file) {
		
		int lastInsertId = 0;
		// Set the file path where you want to save it
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("hoardings_foldername");
        var year =DateTimeUtil.getCurrentYear();
        var month =DateTimeUtil.getCurrentMonth();
        
        uploadDirectory = uploadDirectory + serviceFolderName + year +"/"+month+"/"+folder+"/";
        
        try {
	        // Create directory if it doesn't exist
	        Path directoryPath = Paths.get(uploadDirectory);
	        if (!Files.exists(directoryPath)) {
	            Files.createDirectories(directoryPath);
	        }
	        
	        // Datetime string
	        String datetimetxt = DateTimeUtil.getCurrentDateTime();
	        // File name 
            String fileName = name+ "_" +id + "_" + datetimetxt + "_" + file.getOriginalFilename();
            fileName = fileName.replaceAll("\\s+", ""); // Remove space on filename
            
	        String filePath = uploadDirectory + "/" + fileName;
	        
	        String filepath_txt = "/"+serviceFolderName + year +"/"+month+"/"+folder+"/"+fileName;
	        
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
	
	/*
	@Transactional
	public List<Map<String, Object>> licenseVerify(
			String license_no,
			String latitude,
			String longitude) {
		
		String sqlQuery = "SELECT `id`, `name`, `mobile`, `license_no`, `latitude`, `longitude`, `valid_from`, `valid_to`, "
				+ "CONCAT(DATE_FORMAT(`valid_from`, '%d-%m-%Y'), ' to ' , DATE_FORMAT(`valid_to`, '%d-%m-%Y')) AS valid_date "
				+ "FROM `hoarding_license` WHERE "
				+ "`license_no`=? AND ((6371008.8 * acos(ROUND(cos(radians(" + latitude
	            + ")) * cos(radians(latitude)) * cos(radians(longitude) - radians(" + longitude
	            + ")) + sin(radians(" + latitude + ")) * sin(radians(latitude)), 9))) < 500)";
		
		List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery, license_no);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "License Data");
        response.put("data", result);
    		
        return Collections.singletonList(response);
	}
	*/
	@Transactional
	public List<Map<String, Object>> licenseVerify(
			String license_no,
			String latitude,
			String longitude) {
		
		String sqlQuery = "SELECT `id`, `name`, `mobile`, `license_no`, `latitude`, `longitude`, `valid_from`, `valid_to`, "
				+ "CONCAT(DATE_FORMAT(`valid_from`, '%d-%m-%Y'), ' to ' , DATE_FORMAT(`valid_to`, '%d-%m-%Y')) AS valid_date "
				+ "FROM `hoarding_license` WHERE "
				+ "`license_no`=? AND `gcc_app_updated`=0";
				//+ "AND ((6371008.8 * acos(ROUND(cos(radians(" + latitude
	            //+ ")) * cos(radians(latitude)) * cos(radians(longitude) - radians(" + longitude
	            //+ ")) + sin(radians(" + latitude + ")) * sin(radians(latitude)), 9))) < 500)";
		
		List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery, license_no);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "License Data");
        response.put("data", result);
    		
        return Collections.singletonList(response);
	}
	
	private void updateHoardingLicenseInactive(String license_no, int asset_id) {
	    String sql = "UPDATE `hoarding_license` SET `gcc_app_updated`=? WHERE `license_no`=?";
	    jdbcHoardingsTemplate.update(sql, asset_id, license_no);
	}
	
	@Transactional
	public List<Map<String, Object>> saveHoardings(
			MultiValueMap<String, String> formData,
			String category_id,
			String assetName,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String streeId,
			String streetName,
			String loginId,
			String remarks,
			MultipartFile file,
			String hitid,
			String licenses_info,
			String licenses_no, 
			String validity_date,
			String case_no, 
			String ptax_no,
			String ptax_name, 
			String ptax_mobile,
			String agency_name, 
			String agency_mobile,
			String fine_to) {
		
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		if(hitid.equals('1')) {
			// Check if the licenses_no exists in hoardings_info table
		    String checkLicensesQuery = "SELECT COUNT(*) FROM hoardings_info WHERE licenses_no = ?";
		    int count = jdbcHoardingsTemplate.queryForObject(checkLicensesQuery, Integer.class, licenses_no);
		    
		    if (count > 0) {
		        // If the licenses_no exists, return an error message
		        response.put("status", "error");
		        response.put("message", "Licenses No already exists!.");
		        return Collections.singletonList(response);
		    }
		}
		
		String image = fileUpload("hoarding_asset",category_id,"asset",file);
		
		String sqlQuery = "INSERT INTO `asset_list`(`category_id`, `image`, `latitude`, `longitude`, "
				+ "`zone`, `ward`, `streetid`, `streetname`, `cby`,`remarks`,`name`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
            int affectedRows = jdbcHoardingsTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, category_id);
                    ps.setString(2, image);
                    ps.setString(3, latitude);
                    ps.setString(4, longitude);
                    ps.setString(5, zone);
                    ps.setString(6, ward);
                    ps.setString(7, streeId);
                    ps.setString(8, streetName);
                    ps.setString(9, loginId);
                    ps.setString(10, remarks);
                    ps.setString(11, assetName);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                int asset_id = lastInsertId;
                
                System.out.println("New Hoardings added successfully! Insert ID: " + generatedId);
                System.out.println("test:"+case_no);
                
                //updateHoardingLicenseInactive(licenses_no,asset_id);
                
                int getUpdateStatus=insertHoardingInfo(
                		asset_id, 
            			hitid,
            			remarks, 
            			licenses_info,
            			licenses_no, 
            			validity_date,
            			case_no, 
            			ptax_no,
            			ptax_name, 
            			ptax_mobile,
            			agency_name, 
            			agency_mobile,
            			fine_to, 
            			loginId);
                
                if(getUpdateStatus>0) {
                	updateHoardingLicenseInactive(licenses_no,asset_id);
                	
                	response.put("insertId", lastInsertId);
                    response.put("status", "success");
                    response.put("message", "New Hoardings added successfully!");
                }
                else {
                	String sqlQueryDelete= "DELETE FROM `asset_list` WHERE id = ?";
                	jdbcHoardingsTemplate.update(sqlQueryDelete,lastInsertId);
                	
                	response.put("status", "error");
                    response.put("message", "Failed to insert a new hoardings (Asset).");
                }
                
            } else {
                response.put("status", "error");
                response.put("message", "Failed to insert a new hoardings (Asset).");
            }
        } catch (DataAccessException e) {
            System.out.println("Data Access Exception:");
            Throwable rootCause = e.getMostSpecificCause();
            if (rootCause instanceof SQLException) {
                SQLException sqlException = (SQLException) rootCause;
                System.out.println("SQL State: " + sqlException.getSQLState());
                System.out.println("Error Code: " + sqlException.getErrorCode());
                System.out.println("Message: " + sqlException.getMessage());
                response.put("status", "error");
                response.put("message", sqlException.getMessage());
                response.put("sqlState", sqlException.getSQLState());
                response.put("errorCode", sqlException.getErrorCode());
            } else {
                System.out.println("Message: " + rootCause.getMessage());
                response.put("status", "error");
                response.put("message", rootCause.getMessage());
            }
        }
		
        return Collections.singletonList(response);
    }
	
	public Integer insertHoardingInfo(
			int asset_id, 
			String hitid,
			String remarks, 
			String licenses_info,
			String licenses_no, 
			String validity_date,
			String case_no, 
			String ptax_no,
			String ptax_name, 
			String ptax_mobile,
			String agency_name, 
			String agency_mobile,
			String fine_to, 
			String cby) {
	    String sqlQuery = "INSERT INTO `hoardings_info`(`asset_id`, `hitid`, `remarks`, `licenses_info`, "
	    		+ "`licenses_no`, `validity_date`, `case_no`, `ptax_no`, `ptax_name`, `ptax_mobile`, "
	    		+ "`agency_name`, `agency_mobile`, `fine_to`, `cby`) "
	    		+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	    
	    Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		String status="error"; 
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
	    try {
            int affectedRows = jdbcHoardingsTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setInt(1, asset_id);
                    ps.setString(2, hitid);
                    ps.setString(3, remarks);
                    ps.setString(4, licenses_info);
                    ps.setString(5, licenses_no);
                    ps.setString(6, validity_date);
                    ps.setString(7, case_no);
                    ps.setString(8, ptax_no);
                    ps.setString(9, ptax_name);
                    ps.setString(10, ptax_mobile);
                    ps.setString(11, agency_name);
                    ps.setString(12, agency_mobile);
                    ps.setString(13, fine_to);
                    ps.setString(14, cby);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "New Hoardings Info added successfully!");
                System.out.println("New Hoardings Info added successfully! Insert ID: " + generatedId);
                
                // Generate Challan and store (Update in info table) if hitid==3 (UnAuterized)
                if("3".equals(hitid)) {
                	createChallan(lastInsertId, "Challan created", hitid);
                }
                
            } else {
                response.put("status", "error");
                response.put("message", "Failed to insert a new hoardings Info (Info).");
            }
        } catch (DataAccessException e) {
            System.out.println("Data Access Exception:");
            Throwable rootCause = e.getMostSpecificCause();
            if (rootCause instanceof SQLException) {
                SQLException sqlException = (SQLException) rootCause;
                System.out.println("SQL State: " + sqlException.getSQLState());
                System.out.println("Error Code: " + sqlException.getErrorCode());
                System.out.println("Message: " + sqlException.getMessage());
                response.put("status", "error");
                response.put("message", sqlException.getMessage());
                response.put("sqlState", sqlException.getSQLState());
                response.put("errorCode", sqlException.getErrorCode());
            } else {
                System.out.println("Message: " + rootCause.getMessage());
                response.put("status", "error");
                response.put("message", rootCause.getMessage());
            }
        }
		
        return lastInsertId;
	}
	
	/* ============= Update Hoardings ============= */
	private String deleteOldHoardingInfo(String asset_id, String cby) {
		String sqlQuery ="INSERT INTO `hoardings_info_trash`  "
				+ "			( `id`, "	
				+ "			    `asset_id`,  "
				+ "			    `hitid`,  "
				+ "			    `remarks`,  "
				+ "			    `licenses_info`,  "
				+ "			    `licenses_no`,  "
				+ "			    `validity_date`,  "
				+ "			    `case_no`,  "
				+ "			    `ptax_no`,  "
				+ "			    `ptax_name`,  "
				+ "			    `ptax_mobile`,  "
				+ "			    `agency_name`,  "
				+ "			    `agency_mobile`,  "
				+ "			    `fine_to`,  "
				+ "			    `fine_amount`,  "
				+ "			    `fine_status`,  "
				+ "			    `orderid`,  "
				+ "			    `cdate`,  "
				+ "			    `cby`,  "
				+ "			    `tid`,  "
				+ "			    `mid`,  "
				+ "			    `serial_number`,  "
				+ "			    `isactive`,  "
				//+ "			    `trash_date`,  "
				+ "			    `trash_by` "
				+ "			) "
				+ "			SELECT `id`,"
				+ "			    `asset_id`,  "
				+ "			    `hitid`,  "
				+ "			    `remarks`,  "
				+ "			    `licenses_info`,  "
				+ "			    `licenses_no`,  "
				+ "			    `validity_date`,  "
				+ "			    `case_no`,  "
				+ "			    `ptax_no`,  "
				+ "			    `ptax_name`,  "
				+ "			    `ptax_mobile`,  "
				+ "			    `agency_name`,  "
				+ "			    `agency_mobile`,  "
				+ "			    `fine_to`,  "
				+ "			    `fine_amount`,  "
				+ "			    `fine_status`,  "
				+ "			    `orderid`,  "
				+ "			    `cdate`,  "
				+ "			    `cby`,  "
				+ "			    `tid`,  "
				+ "			    `mid`,  "
				+ "			    `serial_number`,  "
				+ "			    `isactive`,  "
				//+ "			    NOW() AS `trash_date`, "
				+ "			    ? AS `trash_by` "
				+ "			FROM  "
				+ "			    `hoardings_info`  "
				+ "			WHERE  "
				+ "			    `asset_id` = ?";
			int lastInsertId = 0;
			String status="error"; 
			KeyHolder keyHolder = new GeneratedKeyHolder();
		
			try {
	            int affectedRows = jdbcHoardingsTemplate.update(new PreparedStatementCreator() {
	                @Override
	                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
	                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
	                    ps.setString(1, cby);
	                    ps.setString(2, asset_id);
	                    return ps;
	                }
	            }, keyHolder);
	
	            if (affectedRows > 0) {
	                Number generatedId = keyHolder.getKey();
	                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
	                
	                String sqlDelete = "DELETE FROM `hoardings_info` WHERE `asset_id` = ?";
	                // Delete the original data
	                jdbcHoardingsTemplate.update(sqlDelete, asset_id);
	                
	                return "success";
	                
	            } else {
	            	return "error";
	            }
	        } catch (DataAccessException e) {
	            System.out.println("Data Access Exception:");
	            Throwable rootCause = e.getMostSpecificCause();
	            if (rootCause instanceof SQLException) {
	                SQLException sqlException = (SQLException) rootCause;
	                System.out.println("SQL State: " + sqlException.getSQLState());
	                System.out.println("Error Code: " + sqlException.getErrorCode());
	                System.out.println("Message: " + sqlException.getMessage());
	                return "error";
	            } else {
	            	return "error";
	            }
	        }
		//return "success";
	}
	
	public List<Map<String, Object>> updateHoardingInfo(
			MultiValueMap<String, String> formData,
			String asset_id, 
			String hitid,
			String remarks, 
			String licenses_info,
			String licenses_no, 
			String validity_date,
			String case_no, 
			String ptax_no,
			String ptax_name, 
			String ptax_mobile,
			String agency_name, 
			String agency_mobile,
			String fine_to, 
			String cby) {
		
		Map<String, Object> response = new HashMap<>();
		
		if(hitid.equals('1')) {
			// Check if the licenses_no exists in hoardings_info table
		    String checkLicensesQuery = "SELECT COUNT(*) FROM hoardings_info WHERE licenses_no = ? AND `asset_id` <> ?";
		    int count = jdbcHoardingsTemplate.queryForObject(checkLicensesQuery, Integer.class, licenses_no, asset_id);
		    
		    if (count > 0) {
		        // If the licenses_no exists, return an error message
		        response.put("status", "error");
		        response.put("message", "Licenses No already exists!.");
		        return Collections.singletonList(response);
		    }
		}
		
		String queryStatus = deleteOldHoardingInfo(asset_id, cby); // Delete old Hoarding Info data 
		
		if(queryStatus.equals("success")){
			String sqlQuery = "INSERT INTO `hoardings_info`(`asset_id`, `hitid`, `remarks`, `licenses_info`, "
		    		+ "`licenses_no`, `validity_date`, `case_no`, `ptax_no`, `ptax_name`, `ptax_mobile`, "
		    		+ "`agency_name`, `agency_mobile`, `fine_to`, `cby`) "
		    		+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		    
		   
			int lastInsertId = 0;
			String status="error"; 
			KeyHolder keyHolder = new GeneratedKeyHolder();
			
		    try {
	            int affectedRows = jdbcHoardingsTemplate.update(new PreparedStatementCreator() {
	                @Override
	                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
	                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
	                    ps.setString(1, asset_id);
	                    ps.setString(2, hitid);
	                    ps.setString(3, remarks);
	                    ps.setString(4, licenses_info);
	                    ps.setString(5, licenses_no);
	                    ps.setString(6, validity_date);
	                    ps.setString(7, case_no);
	                    ps.setString(8, ptax_no);
	                    ps.setString(9, ptax_name);
	                    ps.setString(10, ptax_mobile);
	                    ps.setString(11, agency_name);
	                    ps.setString(12, agency_mobile);
	                    ps.setString(13, fine_to);
	                    ps.setString(14, cby);
	                    return ps;
	                }
	            }, keyHolder);

	            if (affectedRows > 0) {
	                Number generatedId = keyHolder.getKey();
	                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
	                response.put("insertId", lastInsertId);
	                response.put("status", "success");
	                response.put("message", "New Hoardings Info added successfully!");
	                System.out.println("New Hoardings Info added successfully! Insert ID: " + generatedId);
	                
	                // Generate Challan and store (Update in info table) if hitid==3 (UnAuterized)
	                if("3".equals(hitid)) {
	                	createChallan(lastInsertId, "Challan created", hitid);
	                }
	                
	            } else {
	                response.put("status", "error");
	                response.put("message", "Failed to insert a new hoardings Info (Info).");
	            }
	        } catch (DataAccessException e) {
	            System.out.println("Data Access Exception:");
	            Throwable rootCause = e.getMostSpecificCause();
	            if (rootCause instanceof SQLException) {
	                SQLException sqlException = (SQLException) rootCause;
	                System.out.println("SQL State: " + sqlException.getSQLState());
	                System.out.println("Error Code: " + sqlException.getErrorCode());
	                System.out.println("Message: " + sqlException.getMessage());
	                response.put("status", "error");
	                response.put("message", sqlException.getMessage());
	                response.put("sqlState", sqlException.getSQLState());
	                response.put("errorCode", sqlException.getErrorCode());
	            } else {
	                System.out.println("Message: " + rootCause.getMessage());
	                response.put("status", "error");
	                response.put("message", rootCause.getMessage());
	            }
	        }
		}
		else {
			System.out.println("Message DELETE ID: " + asset_id + " "+ queryStatus);
			response.put("status", "error");
            response.put("message", "Hoding Info Delete failed :" + asset_id + " "+ queryStatus);
		}
		
		return Collections.singletonList(response);
	}
	
	/* ============= End Update ================ */
	
	public String createChallan(int id, String status, String hitid) {
	    // Fetch the fine details as a list of maps
	    List<Map<String, Object>> fineAmountList = getFineAmount("1");
	    
	    if (fineAmountList != null && !fineAmountList.isEmpty()) {
	        // Extract the first map from the list (it's wrapped in a 'data' key)
	        Map<String, Object> response = fineAmountList.get(0);
	        
	        // Extract the data list from the 'data' key
	        List<Map<String, Object>> fineAmountDataList = (List<Map<String, Object>>) response.get("data");
	        
	        if (fineAmountDataList != null && !fineAmountDataList.isEmpty()) {
	            // Extract the first record (since we expect only one record as per the query)
	            Map<String, Object> fineAmountData = fineAmountDataList.get(0);

	            // Safely get the fine amount, and check if it's null
	            Object fineAmountObj = fineAmountData.get("amount");
	            if (fineAmountObj != null) {
	                // Convert to String only if not null
	                String fineAmountValue = fineAmountObj.toString(); // Fetch the amount
	                String fineStatus = status; // Use the passed status for updating

	                // Update the hoardings_info table with fine amount and status
	                String sqlQuery = "UPDATE `hoardings_info` SET `fine_amount` = ?, `fine_status` = ?, `orderid` = ? WHERE id=? LIMIT 1";
	                String orderid = generateRandomString();
	                jdbcHoardingsTemplate.update(sqlQuery, fineAmountValue, fineStatus, orderid, id);
	                System.out.println("UPDATE `hoardings_info` SET `fine_amount` = "+fineAmountValue+", `fine_status` = "+fineStatus+", `orderid` = "+orderid+" WHERE id="+id+" LIMIT 1");
	                return "success"; // Return success after update
	            } else {
	                // Handle the case when amount is null
	            	System.out.println("failure: fine amount is null");
	                return "failure: fine amount is null";
	            }
	        } else {
	            // Handle case when data list is empty or not found
	        	System.out.println("failure: no fine amount data found");
	            return "failure: no fine amount data found";
	        }
	    } else {
	        // Handle case when fineAmountList is empty
	    	System.out.println("failure: no fine amount list found");
	        return "failure: no fine amount list found";
	    }
	}
	
	public String createChallanbyAssetId(int id, String status, String hitid) {
	    // Fetch the fine details as a list of maps
	    List<Map<String, Object>> fineAmountList = getFineAmount("1");
	    
	    if (fineAmountList != null && !fineAmountList.isEmpty()) {
	        // Extract the first map from the list (it's wrapped in a 'data' key)
	        Map<String, Object> response = fineAmountList.get(0);
	        
	        // Extract the data list from the 'data' key
	        List<Map<String, Object>> fineAmountDataList = (List<Map<String, Object>>) response.get("data");
	        
	        if (fineAmountDataList != null && !fineAmountDataList.isEmpty()) {
	            // Extract the first record (since we expect only one record as per the query)
	            Map<String, Object> fineAmountData = fineAmountDataList.get(0);

	            // Safely get the fine amount, and check if it's null
	            Object fineAmountObj = fineAmountData.get("amount");
	            if (fineAmountObj != null) {
	                // Convert to String only if not null
	                String fineAmountValue = fineAmountObj.toString(); // Fetch the amount
	                String fineStatus = status; // Use the passed status for updating

	                // Update the hoardings_info table with fine amount and status
	                String sqlQuery = "UPDATE `hoardings_info` SET `fine_amount` = ?, `fine_status` = ?, `orderid` = ? WHERE `asset_id`=? LIMIT 1";
	                String orderid = generateRandomString();
	                jdbcHoardingsTemplate.update(sqlQuery, fineAmountValue, fineStatus, orderid, id);
	                return "success"; // Return success after update
	            } else {
	                // Handle the case when amount is null
	                return "failure: fine amount is null";
	            }
	        } else {
	            // Handle case when data list is empty or not found
	            return "failure: no fine amount data found";
	        }
	    } else {
	        // Handle case when fineAmountList is empty
	        return "failure: no fine amount list found";
	    }
	}
	
	public List<Map<String, Object>>  getFineAmount(String category_id) {
	    // SQL query to fetch `id`, `category_id`, `ton`, and `amount`
	    String sqlQuery = "SELECT `id`, `category_id`, `ton`, `amount` FROM `penalty_amount` "
	    		+ "WHERE `category_id`= ? AND `isactive`=1 AND `isdelete`=0 LIMIT 1";
	   System.out.println(sqlQuery + category_id);
        // Fetching the data and returning as a simple array
    	List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery,category_id);
    	Map<String, Object> response = new HashMap<>();
    	response.put("status", 200);
        response.put("message", "Hoarding Fine Amount");
        response.put("data", result);
    	return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> assetlist(
			MultiValueMap<String, String> formData,
			String loginid) {
		
		String sqlQuery = "SELECT *, "
				+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files',`image`) as imageurl "
				+ " From `asset_list` WHERE (`isactive`=1 AND `isdelete`=0)";
		
		List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Hoarding list");
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> assetlistbylogin(
			MultiValueMap<String, String> formData,
			String loginid) {
		/*
		String sqlQuery = "SELECT al.`id`, al.`category_id`, ac.`name` category_name, al.`name`, al.`image`, al.`latitude`, "
                + "al.`longitude`, al.`zone`, al.`ward`, al.`streetid`, al.`streetname`, al.`cdate`, al.`cby`, al.`remarks`, "
                + "hi.`hitid`, hi.`licenses_info`, hi.`licenses_no`, hi.`validity_date`, hi.`case_no`, "
                + "hi.`ptax_no`, hi.`ptax_name`, hi.`ptax_mobile`, hi.`agency_name`, hi.`agency_mobile`, hi.`fine_to`, "
                + "hit.`name` hoarding_info_type, "
                + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', al.`image`) as imageurl "
                + "FROM `asset_list` al "
                + "INNER JOIN `hoardings_info` hi ON al.id = hi.asset_id "
                + "INNER JOIN `asset_category` ac ON ac.id = al.category_id "
                + "INNER JOIN `hoarding_info_type` hit ON hit.id = hi.hitid "
                + "WHERE al.`isactive` = 1 AND al.`isdelete` = 0 AND al.`cby` = ?";
		*/
		String sqlQuery = "SELECT "
				+ "    al.`id`, "
				+ "    al.`category_id`, "
				+ "    ac.`name` AS category_name, "
				+ "    al.`name`, "
				+ "    al.`image`, "
				+ "    al.`latitude`, "
				+ "    al.`longitude`, "
				+ "    al.`zone`, "
				+ "    al.`ward`, "
				+ "    al.`streetid`, "
				+ "    al.`streetname`, "
				+ "    al.`cdate`, "
				+ "    al.`cby`, "
				+ "    al.`remarks`, "
				+ "    hi.`hitid`, "
				+ "    hi.`licenses_info`, "
				+ "    hi.`licenses_no`, "
				+ "    hi.`validity_date`, "
				+ "    hi.`case_no`, "
				+ "    hi.`ptax_no`, "
				+ "    hi.`ptax_name`, "
				+ "    hi.`ptax_mobile`, "
				+ "    hi.`agency_name`, "
				+ "    hi.`agency_mobile`, "
				+ "    hi.`fine_to`, "
				+ "    hi.`fine_amount`, "
				+ "    hi.`fine_status`, "
				+ "    hi.`orderid`, "
				+ "    hi.`cdate` as ae_cdate, "
				+ "    hi.`cby` as ae_cby, "
				+ "    hi.`tid`, "
				+ "    hi.`mid`, "
				+ "    hi.`serial_number`, "
				+ "    hit.`name` AS hoarding_info_type, "
				+ "    CONCAT('https://gccservices.in/gccofficialapp/files', al.`image`) AS imageurl, "
				+ "    haeu.`update_image`, "
				+ "    haeu.`remarks`, "
				+ "    haeu.`removed_type`, "
				+ "    haeu.`fir`, "
				+ "    haeu.`fir_number`, "
				+ "    haeu.`fir_image`, "
				+ "    haeu.`material_collected`, "
				+ "    haeu.`material_receipt`, "
				+ "    haeu.`cdate` AS ae_cdate, "
				+ "    haeu.`cby` AS ae_cby, "
				+ "    haeu.`material_pending`, "
				+ "    haeu.`fir_pending`, "
				+ "    haeu.`id` AS ae_data_id, "
				+ "    CONCAT('https://gccservices.in/gccofficialapp/files', haeu.`update_image`) AS ae_update_imageurl, "
				+ "    CONCAT('https://gccservices.in/gccofficialapp/files', haeu.`material_receipt`) AS ae_material_imageurl, "
				+ "    CONCAT('https://gccservices.in/gccofficialapp/files', haeu.`fir_image`) AS ae_fir_imageurl "
				+ "FROM "
				+ "    `asset_list` al "
				+ "LEFT JOIN "
				+ "    `hoardings_info` hi ON al.id = hi.asset_id "
				+ "LEFT JOIN "
				+ "    `asset_category` ac ON ac.id = al.category_id "
				+ "LEFT JOIN "
				+ "    `hoarding_info_type` hit ON hit.id = hi.hitid "
				+ "LEFT JOIN "
				+ "    `hoarding_ae_update` haeu ON haeu.asset_id = hi.asset_id "
				+ "WHERE "
				+ "    al.`isactive` = 1 "
				+ "    AND al.`isdelete` = 0 "
				+ "    AND al.`cby` = ? "
				+ "GROUP BY "
				+ "    al.`id` "
				+ "ORDER BY"
				+ "    al.`id` DESC";
		
		List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery,loginid);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Hoarding list By Login");
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	
	@Transactional
	public List<Map<String, Object>> assetlistbylatlong(
			MultiValueMap<String, String> formData,
			String latitude,
			String longitude,
			String loginid) {
			
		String sqlWhere = "";

		if (latitude != null && longitude != null && !latitude.isBlank() && !latitude.isEmpty() && !longitude.isBlank()
		        && !longitude.isEmpty()) {
		    sqlWhere += " AND ((6371008.8 * acos(ROUND(cos(radians(" + latitude
		            + ")) * cos(radians(al.latitude)) * cos(radians(al.longitude) - radians(" + longitude
		            + ")) + sin(radians(" + latitude + ")) * sin(radians(al.latitude)), 9))) < 500)"
		            + " ORDER BY"
					+ "    al.`id` DESC";
		}

		String sqlQuery = "SELECT al.`id`, al.`category_id`, ac.`name` AS category_name, al.`name`, al.`image`, al.`latitude`, "
		                + "al.`longitude`, al.`zone`, al.`ward`, al.`streetid`, al.`streetname`, al.`cdate`, al.`cby`, al.`remarks`, "
		                + "hi.`hitid`, hi.`licenses_info`, hi.`licenses_no`, hi.`validity_date`, hi.`case_no`, "
		                + "hi.`ptax_no`, hi.`ptax_name`, hi.`ptax_mobile`, hi.`agency_name`, hi.`agency_mobile`, hi.`fine_to`, "
		                + "hit.`name` AS hoarding_info_type, "
		                + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', al.`image`) AS imageurl "
		                + "FROM `asset_list` al "
		                + "INNER JOIN `hoardings_info` hi ON al.id = hi.asset_id "
		                + "INNER JOIN `asset_category` ac ON ac.id = al.category_id "
		                + "INNER JOIN `hoarding_info_type` hit ON hit.id = hi.hitid "
		                + "WHERE al.`isactive` = 1 AND al.`isdelete` = 0" // This is the starting part of WHERE clause
		                + sqlWhere; // Here the latitude and longitude condition gets appended if provided
		
		List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Hoarding list by latlong");
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> getChallanPOSData(
	        MultiValueMap<String, String> formData,
	        String orderid,
	        String loginid) {

	    String sqlQuery = "SELECT al.`category_id`, al.`name`, al.`zone`, al.`ward`, "
	            + "CONCAT('https://gccservices.in/gccofficialapp/files', al.`image`) as imageurl, "
	            + "hi.`licenses_no`, hi.`fine_amount`, hi.`fine_status`, hi.`orderid`, "
	            + "CASE "
	            + "WHEN hi.`fine_to` = 1 THEN 'Ptax Owner' "
	            + "WHEN hi.`fine_to` = 2 THEN 'Agent' "
	            + "ELSE '' "
	            + "END AS fine_for_name, "
	            + "CASE "
	            + "WHEN hi.`fine_to` = 1 THEN COALESCE(hi.`ptax_name`, '') "
	            + "WHEN hi.`fine_to` = 2 THEN COALESCE(hi.`agency_name`, '') "
	            + "ELSE '' "
	            + "END AS fine_to_name, "
	            + "CASE "
	            + "WHEN hi.`fine_to` = 1 THEN COALESCE(hi.`ptax_mobile`, '') "
	            + "WHEN hi.`fine_to` = 2 THEN COALESCE(hi.`agency_mobile`, '') "
	            + "ELSE '' "
	            + "END AS fine_to_mobile, "
	            + "hit.`name` as status "
	            + "FROM `asset_list` as al "
	            + "INNER JOIN `hoardings_info` as hi ON hi.asset_id = al.id "
	            + "INNER JOIN `hoarding_info_type` as hit ON hit.id = hi.hitid "
	            + "WHERE al.`isactive` = 1 AND al.`isdelete` = 0 "
	            + "AND hi.`fine_status` <> 'Transaction success' "
	            + "AND hi.`fine_amount` > 0 ";

	    if (orderid != null && !orderid.isEmpty() && !orderid.isBlank()) {
	        sqlQuery = sqlQuery + " AND hi.`orderid` = '" + orderid + "'";
	    }
	    if (loginid != null && !loginid.isEmpty() && !loginid.isBlank()) {
	        sqlQuery = sqlQuery + " AND hi.`cby` = '" + loginid + "'";
	    }

	    // Debugging: Print the final SQL query for checking
	    System.out.println("Final SQL Query: " + sqlQuery);

	    List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery);
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", 200);
	    response.put("message", "Challan Details");
	    response.put("data", result);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> updateSerialNumber(
	        MultiValueMap<String, String> formData,
	        String tid,
	        String mid,
	        String serialNumber,
	        String orderid){
	    String sqlQuery = "UPDATE hoardings_info SET tid = ?, mid = ?, serial_number = ? WHERE orderid = ? LIMIT 1";
	    jdbcHoardingsTemplate.update(sqlQuery, tid, mid, serialNumber, orderid);
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", 200);
	    response.put("message", "Serial Number updated successfully");
	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> storeBankTransaction(Map<String, Object> transactionData) {

	    Map<String, Object> response = new HashMap<>();
	    int lastInsertId = 0;
	    
	    // Extract data from the JSON object
	    String transactionResponseStatus = (String) transactionData.get("transactionResponseStatus");

	    // Check if the transaction was successful
	    if ("Transaction success".equalsIgnoreCase(transactionResponseStatus)) {
	    	
	    	String mid = (String) transactionData.get("mid");
	        String tid = (String) transactionData.get("tid");
	        String txnId = (String) transactionData.get("txnId");

	        // Fetch nested transaction response data
	        Map<String, Object> txnResponseData = (Map<String, Object>) transactionData.get("txnResponseData");

	        String bankName = (String) txnResponseData.get("bankName");
	        String batchNumber = (String) txnResponseData.get("batchNumber");
	        String transactionTitle = (String) txnResponseData.get("transactionTitle");
	        String txnAID = (String) txnResponseData.get("txnAID");
	        String txnApprCode = (String) txnResponseData.get("txnApprCode");
	        String txnCardNo = (String) txnResponseData.get("txnCardNo");
	        String txnCardType = (String) txnResponseData.get("txnCardType");
	        String txnDate = (String) txnResponseData.get("txnDate");
	        String txnInvoice = (String) txnResponseData.get("txnInvoice");
	        String txnMode = (String) txnResponseData.get("txnMode");
	        String txnRefNo = (String) txnResponseData.get("txnRefNo");
	        String txnTC = (String) txnResponseData.get("txnTC");
	        String txnTSI = (String) txnResponseData.get("txnTSI");
	        String txnTVR = (String) txnResponseData.get("txnTVR");
	        String txnTime = (String) txnResponseData.get("txnTime");
            
	        // Convert transaction amount properly
	        Double transactionAmount = Double.valueOf((String) txnResponseData.get("transactionAmount"));

	        String sqlQuery = "INSERT INTO bank_transactions (mid, tid, txnId, bankName, batchNumber, transactionAmount, "
	                + "transactionTitle, txnAID, txnApprCode, txnCardNo, txnCardType, txnDate, txnInvoice, txnMode, txnRefNo, "
	                + "txnTC, txnTSI, txnTVR, txnTime) "
	                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	        
	        KeyHolder keyHolder = new GeneratedKeyHolder();

	        try {
	            int affectedRows = jdbcHoardingsTemplate.update(new PreparedStatementCreator() {
	                @Override
	                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
	                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
	                    ps.setString(1, mid);
	                    ps.setString(2, tid);
	                    ps.setString(3, txnId);
	                    ps.setString(4, bankName);
	                    ps.setString(5, batchNumber);
	                    ps.setDouble(6, transactionAmount);
	                    ps.setString(7, transactionTitle);
	                    ps.setString(8, txnAID);
	                    ps.setString(9, txnApprCode);
	                    ps.setString(10, txnCardNo);
	                    ps.setString(11, txnCardType);
	                    ps.setString(12, txnDate);
	                    ps.setString(13, txnInvoice);
	                    ps.setString(14, txnMode);
	                    ps.setString(15, txnRefNo);
	                    ps.setString(16, txnTC);
	                    ps.setString(17, txnTSI);
	                    ps.setString(18, txnTVR);
	                    ps.setString(19, txnTime);
	                    return ps;
	                }
	            }, keyHolder);

	            if (affectedRows > 0) {
	                Number generatedId = keyHolder.getKey();
	                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
	                
	                updatePaymentStatus(txnId, transactionResponseStatus);
	                
	                // Fetch inserted data for response, joining `penalty_challan` and `bank_transactions`
	                String sqlQuery_joined = "SELECT al.`category_id`, al.`name`, al.`zone`, al.`ward`, "
	        	            + "CONCAT('https://gccservices.in/gccofficialapp/files', al.`image`) as imageurl, "
	        	            + "hi.`licenses_no`, hi.`fine_amount`, hi.`fine_status`, hi.`orderid`, "
	        	            + "CASE "
	        	            + "WHEN hi.`fine_to` = 1 THEN 'Ptax Owner' "
	        	            + "WHEN hi.`fine_to` = 2 THEN 'Agent' "
	        	            + "ELSE '' "
	        	            + "END AS fine_for_name, "
	        	            + "CASE "
	        	            + "WHEN hi.`fine_to` = 1 THEN COALESCE(hi.`ptax_name`, '') "
	        	            + "WHEN hi.`fine_to` = 2 THEN COALESCE(hi.`agency_name`, '') "
	        	            + "ELSE '' "
	        	            + "END AS fine_to_name, "
	        	            + "CASE "
	        	            + "WHEN hi.`fine_to` = 1 THEN COALESCE(hi.`ptax_mobile`, '') "
	        	            + "WHEN hi.`fine_to` = 2 THEN COALESCE(hi.`agency_mobile`, '') "
	        	            + "ELSE '' "
	        	            + "END AS fine_to_mobile, "
	        	            + "hit.`name` as status "
	        	            + "FROM `asset_list` as al "
	        	            + "INNER JOIN `hoardings_info` as hi ON hi.asset_id = al.id "
	        	            + "INNER JOIN `hoarding_info_type` as hit ON hit.id = hi.hitid "
	        	            + "WHERE al.`isactive` = 1 AND al.`isdelete` = 0 "
	        	            + "AND hi.`orderid` = ?";
	                
	                System.out.println(sqlQuery_joined);
	                
	                // Execute the query
	                List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery_joined, txnId);
	                
	                response.put("status", 200);
	                response.put("message", "Transaction stored successfully!");
	                response.put("data", result);
	                
	            } else {
	                response.put("status", 201);
	                response.put("message", "Failed to store the transaction.");
	                response.put("data", "");
	            }
	        } catch (DataAccessException e) {
	            response.put("status", 500);
	            response.put("message", "Database error while storing transaction.");
	            response.put("error", e.getMessage());
	            e.printStackTrace(); // This will give you more detail in logs
	        }
	    } else {
	        response.put("status", 400);
	        response.put("message", "Transaction failed or invalid status.");
	    }

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> storeFailedBankTransaction(Map<String, Object> transactionData) {

	    Map<String, Object> response = new HashMap<>();
	    int lastInsertId = 0;
	    
	    // Extract data from the JSON object
	    String transactionResponseStatus = (String) transactionData.get("transactionResponseStatus");

	    // Check if the transaction was successful
	    if ("Transaction cancelled".equalsIgnoreCase(transactionResponseStatus)) {
	    	
	    	String mid = (String) transactionData.get("mid");
	        String tid = (String) transactionData.get("tid");
	        String txnId = (String) transactionData.get("txnId");

	        // Fetch nested transaction response data
	        Map<String, Object> txnResponseData = (Map<String, Object>) transactionData.get("txnResponseData");

	        String bankName = (String) txnResponseData.get("bankName");
	        String batchNumber = (String) txnResponseData.get("batchNumber");
	        String transactionTitle = (String) txnResponseData.get("transactionTitle");
	        String txnAID = (String) txnResponseData.get("txnAID");
	        String txnApprCode = (String) txnResponseData.get("txnApprCode");
	        String txnCardNo = (String) txnResponseData.get("txnCardNo");
	        String txnCardType = (String) txnResponseData.get("txnCardType");
	        String txnDate = (String) txnResponseData.get("txnDate");
	        String txnInvoice = (String) txnResponseData.get("txnInvoice");
	        String txnMode = (String) txnResponseData.get("txnMode");
	        String txnRefNo = (String) txnResponseData.get("txnRefNo");
	        String txnTC = (String) txnResponseData.get("txnTC");
	        String txnTSI = (String) txnResponseData.get("txnTSI");
	        String txnTVR = (String) txnResponseData.get("txnTVR");
	        String txnTime = (String) txnResponseData.get("txnTime");
            
	        // Convert transaction amount properly
	        //Double transactionAmount = Double.valueOf((String) txnResponseData.get("transactionAmount"));

	     // Get the transactionAmount from txnResponseData
	        String transactionAmountStr = (String) txnResponseData.get("transactionAmount");

	        // Check if the transactionAmount is not null and not empty before converting
	        Double transactionAmountTemp = null;

	        if (transactionAmountStr != null && !transactionAmountStr.trim().isEmpty()) {
	            try {
	                // Convert the valid string to a Double
	                transactionAmountTemp = Double.valueOf(transactionAmountStr);
	            } catch (NumberFormatException e) {
	                // Handle the case where the string cannot be parsed to a Double // Log the error or throw a custom exception
	                System.out.println("Error: Unable to parse transactionAmount to Double. " + e.getMessage());
	                transactionAmountTemp = 0.0; // Set to a default value or handle as needed
	            }
	        } else {
	            // Handle the case where transactionAmount is empty or null
	            transactionAmountTemp = 0.0; // Default value or handle as needed
	        }
	        
	        Double transactionAmount = transactionAmountTemp;
	        
	        String sqlQuery = "INSERT INTO bank_failed_transactions (mid, tid, txnId, bankName, batchNumber, transactionAmount, "
	                + "transactionTitle, txnAID, txnApprCode, txnCardNo, txnCardType, txnDate, txnInvoice, txnMode, txnRefNo, "
	                + "txnTC, txnTSI, txnTVR, txnTime) "
	                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	        
	        KeyHolder keyHolder = new GeneratedKeyHolder();

	        try {
	            int affectedRows = jdbcHoardingsTemplate.update(new PreparedStatementCreator() {
	                @Override
	                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
	                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
	                    ps.setString(1, mid);
	                    ps.setString(2, tid);
	                    ps.setString(3, txnId);
	                    ps.setString(4, bankName);
	                    ps.setString(5, batchNumber);
	                    ps.setDouble(6, transactionAmount);
	                    ps.setString(7, transactionTitle);
	                    ps.setString(8, txnAID);
	                    ps.setString(9, txnApprCode);
	                    ps.setString(10, txnCardNo);
	                    ps.setString(11, txnCardType);
	                    ps.setString(12, txnDate);
	                    ps.setString(13, txnInvoice);
	                    ps.setString(14, txnMode);
	                    ps.setString(15, txnRefNo);
	                    ps.setString(16, txnTC);
	                    ps.setString(17, txnTSI);
	                    ps.setString(18, txnTVR);
	                    ps.setString(19, txnTime);
	                    return ps;
	                }
	            }, keyHolder);

	            if (affectedRows > 0) {
	                Number generatedId = keyHolder.getKey();
	                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
	                
	                updatePaymentStatus(txnId, transactionResponseStatus);
	                
	             // Fetch inserted data for response, joining `penalty_challan` and `bank_transactions`
	             // Fetch inserted data for response, joining `penalty_challan` and `bank_transactions`
	                String sqlQuery_joined = "SELECT al.`category_id`, al.`name`, al.`zone`, al.`ward`, "
	        	            + "CONCAT('https://gccservices.in/gccofficialapp/files', al.`image`) as imageurl, "
	        	            + "hi.`licenses_no`, hi.`fine_amount`, hi.`fine_status`, hi.`orderid`, "
	        	            + "CASE "
	        	            + "WHEN hi.`fine_to` = 1 THEN 'Ptax Owner' "
	        	            + "WHEN hi.`fine_to` = 2 THEN 'Agent' "
	        	            + "ELSE '' "
	        	            + "END AS fine_for_name, "
	        	            + "CASE "
	        	            + "WHEN hi.`fine_to` = 1 THEN COALESCE(hi.`ptax_name`, '') "
	        	            + "WHEN hi.`fine_to` = 2 THEN COALESCE(hi.`agency_name`, '') "
	        	            + "ELSE '' "
	        	            + "END AS fine_to_name, "
	        	            + "CASE "
	        	            + "WHEN hi.`fine_to` = 1 THEN COALESCE(hi.`ptax_mobile`, '') "
	        	            + "WHEN hi.`fine_to` = 2 THEN COALESCE(hi.`agency_mobile`, '') "
	        	            + "ELSE '' "
	        	            + "END AS fine_to_mobile, "
	        	            + "hit.`name` as status "
	        	            + "FROM `asset_list` as al "
	        	            + "INNER JOIN `hoardings_info` as hi ON hi.asset_id = al.id "
	        	            + "INNER JOIN `hoarding_info_type` as hit ON hit.id = hi.hitid "
	        	            + "WHERE al.`isactive` = 1 AND al.`isdelete` = 0 "
	        	            + "AND hi.`orderid` = ?";
	                System.out.println(sqlQuery_joined);
	                // Execute the query
	                List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery_joined, txnId);
	                
	                response.put("status", 200);
	                response.put("message", "Transaction stored successfully!");
	                response.put("data", result);
	            } else {
	                response.put("status", 201);
	                response.put("message", "Failed to store the transaction.");
	                response.put("data", "");
	            }
	        } catch (DataAccessException e) {
	            response.put("status", 500);
	            response.put("message", "Database error while storing transaction.");
	            response.put("error", e.getMessage());
	            e.printStackTrace(); // This will give you more detail in logs
	        }
	    } else {
	        response.put("status", 400);
	        response.put("message", "Transaction failed or invalid status.");
	    }

	    return Collections.singletonList(response);
	}
	
	public String updatePaymentStatus(String orderid, String status) {
	    String sqlQuery = "UPDATE `hoardings_info` SET `fine_status` = ? WHERE orderid = ? LIMIT 1";
	    jdbcHoardingsTemplate.update(sqlQuery, status, orderid);
	    return "success";
	}
	
	public List<Map<String, Object>> insertHoardingUpdatedData(
			MultiValueMap<String, String> formData,
			String asset_id, 
			String update_type,
			String removed_type, 
			MultipartFile file,
			String licenses_no_visible, 
			String validity_date_visible,
			String not_as_per_size, 
			String licenses_no,
			String cby) {
		
		String update_image = fileUpload("hoarding_asset",asset_id,"asset_update",file);
		
	    String sqlQuery = "INSERT INTO `hoarding_update`"
	    		+ "(`asset_id`, `update_type`, `removed_type`, `update_image`, `licenses_no_visible`, `validity_date_visible`, "
	    		+ "`not_as_per_size`, `licenses_no`,`cby`) "
	    		+ "VALUES (?,?,?,?,?,?,?,?,?)";
	    
	    Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		String status="error"; 
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		//setHoadingInfoinactive(asset_id); // Update isactive as 0 for old records 
	    
		try {
            int affectedRows = jdbcHoardingsTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, asset_id);
                    ps.setString(2, update_type);
                    ps.setString(3, removed_type);
                    ps.setString(4, update_image);
                    ps.setString(5, licenses_no_visible);
                    ps.setString(6, validity_date_visible);
                    ps.setString(7, not_as_per_size);
                    ps.setString(8, licenses_no);
                    ps.setString(9, cby);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "Hoardings Info updated successfully!");
                System.out.println("Hoardings Info updated successfully! Insert ID: " + generatedId);
                //System.out.println("update_type :"+update_type);
                
                if ("Authorized".equals(update_type)) {
                    updateHoardingStatus(asset_id, "1"); // Update current status
                    updateHoardingLicense(asset_id, licenses_no); // Update new licenses number
                } else if ("Un-Authorized".equals(update_type)) {
                    updateHoardingStatus(asset_id, "3"); // Update current status
                } else if ("Removed by Own".equals(update_type)) {
                    updateHoardingStatus(asset_id, "4"); // Update current status
                }
                
            } else {
                response.put("status", "error");
                response.put("message", "Failed to update the hoardings Info (Update).");
            }
        } catch (DataAccessException e) {
            System.out.println("Data Access Exception:");
            Throwable rootCause = e.getMostSpecificCause();
            if (rootCause instanceof SQLException) {
                SQLException sqlException = (SQLException) rootCause;
                System.out.println("SQL State: " + sqlException.getSQLState());
                System.out.println("Error Code: " + sqlException.getErrorCode());
                System.out.println("Message: " + sqlException.getMessage());
                response.put("status", "error");
                response.put("message", sqlException.getMessage());
                response.put("sqlState", sqlException.getSQLState());
                response.put("errorCode", sqlException.getErrorCode());
            } else {
                System.out.println("Message: " + rootCause.getMessage());
                response.put("status", "error");
                response.put("message", rootCause.getMessage());
            }
        }
		
        return Collections.singletonList(response);
	}
	
	public String setHoadingInfoinactive(String asset_id) {
	    String sqlQuery = "UPDATE `hoarding_update` SET `isactive` = 0 WHERE `asset_id` = ?";
	    jdbcHoardingsTemplate.update(sqlQuery, asset_id);
	    return "success";
	}
	
	public String updateHoardingStatus(String asset_id, String status) {
		//System.out.println("update_type :"+ status + " & ID :"+asset_id);
	    String sqlQuery = "UPDATE `hoardings_info` SET `hitid` = ? WHERE `asset_id` = ? LIMIT 1";
	    jdbcHoardingsTemplate.update(sqlQuery, status, asset_id);
	    
	    // Generate Challan and store (Update in info table) if hitid==3 (UnAuterized)
        if("3".equals(status)) {
        	int assetIdInt = Integer.parseInt(asset_id);  // Convert the String asset_id to an int
        	createChallanbyAssetId(assetIdInt, "Challan created", status);
        }
        
	    return "success";
	}
	
	public String updateHoardingLicense(String asset_id, String licenses_no) {
		//System.out.println("update_type :"+ status + " & ID :"+asset_id);
	    String sqlQuery = "UPDATE `hoardings_info` SET `licenses_no` = ? WHERE `asset_id` = ? LIMIT 1";
	    jdbcHoardingsTemplate.update(sqlQuery, licenses_no, asset_id);
	    return "success";
	}
	
	public String getWardByLoginId(String loginid, String type) {
	    String sqlQuery = "SELECT `ward` FROM `hoading_user_list` WHERE `userid` = ? AND `type` = ? LIMIT 1";
	    
	    // Query the database using queryForList
	    List<Map<String, Object>> results = jdbcHoardingsTemplate.queryForList(sqlQuery, loginid, type);
	    
	    // Check if results is not empty and extract the ward value
	    if (!results.isEmpty()) {
	        // Extract the ward value from the first result
	        return (String) results.get(0).get("ward");
	    }
	    
	    // Handle the case where no result is found
	    return "000";  // or return null based on your needs
	}
	
	@Transactional
	public List<Map<String, Object>> unAuterrizedAssetlist(
			MultiValueMap<String, String> formData,
			String loginid
			) {
		
		String ward = getWardByLoginId(loginid,"ae");
		
		String sqlQuery = "SELECT al.`id`, al.`category_id`, ac.`name` AS category_name, al.`name`, al.`image`, al.`latitude`, "
                + "al.`longitude`, al.`zone`, al.`ward`, al.`streetid`, al.`streetname`, al.`cdate`, al.`cby`, al.`remarks`, "
                + "hi.`hitid`, hi.`licenses_info`, hi.`licenses_no`, hi.`validity_date`, hi.`case_no`, "
                + "hi.`ptax_no`, hi.`ptax_name`, hi.`ptax_mobile`, hi.`agency_name`, hi.`agency_mobile`, hi.`fine_to`, "
                + "hi.`fine_amount`, hi.`fine_status`, hi.`orderid`, hi.`cdate` as ae_cdate, hi.`cby` as ae_cby, "
                + "hi.`tid`, hi.`mid`, hi.`serial_number`, "
                + "hit.`name` AS hoarding_info_type, "
                + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', al.`image`) AS imageurl "
                + "FROM `asset_list` al "
                + "INNER JOIN `hoardings_info` hi ON al.id = hi.asset_id "
                + "INNER JOIN `asset_category` ac ON ac.id = al.category_id "
                + "INNER JOIN `hoarding_info_type` hit ON hit.id = hi.hitid "
                + "WHERE al.`isactive` = 1 AND al.`isdelete` = 0 AND hi.hitid = 3 "
                + "AND hi.cdate < (NOW() - INTERVAL 24 HOUR) AND al.ward = ?";  // This filters records within the last 24 hours
		
		List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery, ward);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Un-Auterrize Hoarding list By Login");
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> unauterrizedassetlistbylatlong(
			MultiValueMap<String, String> formData,
			String latitude,
			String longitude,
			String assetid,
			String loginid) {
			
		String sqlWhere = "";

		if (latitude != null && longitude != null && !latitude.isBlank() && !latitude.isEmpty() && !longitude.isBlank()
		        && !longitude.isEmpty()) {
		    sqlWhere += " AND ((6371008.8 * acos(ROUND(cos(radians(" + latitude
		            + ")) * cos(radians(al.latitude)) * cos(radians(al.longitude) - radians(" + longitude
		            + ")) + sin(radians(" + latitude + ")) * sin(radians(al.latitude)), 9))) < 500)";
		}

		String sqlQuery = "SELECT al.`id`, al.`category_id`, ac.`name` AS category_name, al.`name`, al.`image`, al.`latitude`, "
		                + "al.`longitude`, al.`zone`, al.`ward`, al.`streetid`, al.`streetname`, al.`cdate`, al.`cby`, al.`remarks`, "
		                + "hi.`hitid`, hi.`licenses_info`, hi.`licenses_no`, hi.`validity_date`, hi.`case_no`, "
		                + "hi.`ptax_no`, hi.`ptax_name`, hi.`ptax_mobile`, hi.`agency_name`, hi.`agency_mobile`, hi.`fine_to`, "
		                + "hi.`fine_amount`, hi.`fine_status`, hi.`orderid`, hi.`cdate` as ae_cdate, hi.`cby` as ae_cby, "
		                + "hi.`tid`, hi.`mid`, hi.`serial_number`, "
		                + "hit.`name` AS hoarding_info_type, "
		                + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', al.`image`) AS imageurl "
		                + "FROM `asset_list` al "
		                + "INNER JOIN `hoardings_info` hi ON al.id = hi.asset_id "
		                + "INNER JOIN `asset_category` ac ON ac.id = al.category_id "
		                + "INNER JOIN `hoarding_info_type` hit ON hit.id = hi.hitid "
		                + "WHERE al.`isactive` = 1 AND al.`isdelete` = 0 AND al.id = ? " // This is the starting part of WHERE clause
		                + sqlWhere; // Here the latitude and longitude condition gets appended if provided
		
		List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery,assetid);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Un-Auterrize Hoarding list by latlong");
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> insertHoardingUpdatedDataAE(
	        MultiValueMap<String, String> formData,
	        String asset_id,
	        String remarks,
	        String removed_type,
	        MultipartFile file,
	        //MultipartFile firfile,
	        //String fir,
	        //String fir_number,
	        //String material_collected,
	        //MultipartFile materialfile,
	        String loginId) {

		// Initialize the variables as final so they can be used in the anonymous class
	    final String update_image = (file != null && !file.isEmpty()) ? fileUpload("hoarding_asset", asset_id, "asset_ae_update", file) : "";
	    //final String materail_receipt = (materialfile != null && !materialfile.isEmpty()) ? fileUpload("hoarding_asset", asset_id, "asset_ae_materail_update", materialfile) : "";
	    //final String firfilepath = (firfile != null && !firfile.isEmpty()) ? fileUpload("hoarding_asset", asset_id, "asset_ae_fir_update", firfile) : "";

	    String sqlQuery = "INSERT INTO `hoarding_ae_update`( `asset_id`, `update_image`, `remarks`, `removed_type`, "
	            + "`fir`, `fir_number`, `fir_image`, `material_collected`, `material_receipt`, `cby`,`fir_remarks`,`material_remarks`) "
	            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

	    Map<String, Object> response = new HashMap<>();
	    int lastInsertId = 0;
	    String status = "error";
	    KeyHolder keyHolder = new GeneratedKeyHolder();
	    
	    setHoadingAEUpdateinactive(asset_id);
	    
	    // Dummy
	    String fir="";
	    String fir_number="";
	    String firfilepath="";
	    String fir_remarks="";
	    String material_collected="";
	    String materail_receipt="";
	    String material_remarks="";
	    
	    try {
	        int affectedRows = jdbcHoardingsTemplate.update(new PreparedStatementCreator() {
	            @Override
	            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
	                PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
	                ps.setString(1, asset_id);
	                ps.setString(2, update_image);  // Can be null if no file was uploaded
	                ps.setString(3, remarks);
	                ps.setString(4, removed_type);
	                ps.setString(5, fir);
	                ps.setString(6, fir_number);
	                ps.setString(7, firfilepath);  // Can be null if no FIR file was uploaded
	                ps.setString(8, material_collected);
	                ps.setString(9, materail_receipt);  // Can be null if no Material file was uploaded
	                ps.setString(10, loginId);
	                ps.setString(11, fir_remarks);
	                ps.setString(12, material_remarks);
	                return ps;
	            }
	        }, keyHolder);

	        if (affectedRows > 0) {
	            Number generatedId = keyHolder.getKey();
	            lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
	            response.put("insertId", lastInsertId);
	            response.put("status", "success");
	            response.put("message", "AE Hoardings Info updated successfully!");
	            System.out.println("AE Hoardings Info updated successfully! Insert ID: " + generatedId);
	            
	            // Update hoarding Status based on removed Type
	            if ("GCC".equals(removed_type)) {
                    updateHoardingStatus(asset_id, "4"); // Update current status
                    
                    // Weight
    	            if("".equals(material_collected) || "".equals(materail_receipt)) {
    	            	updateHoadingAEmaterialPendingStatus(lastInsertId,1);
    	            }
    	            else {
    	            	updateHoadingAEmaterialPendingStatus(lastInsertId,0);
    	            }
    	            
                } else if ("Violator".equals(removed_type)) {
                    updateHoardingStatus(asset_id, "4"); // Update current status
                    updateHoadingAEmaterialPendingStatus(lastInsertId,0);
                }
	            
	            // FIR
	            if("".equals(fir_number) || "".equals(firfilepath)) {
	            	updateHoadingAEFIRPendingStatus(lastInsertId,1);
	            }
	            else {
	            	updateHoadingAEFIRPendingStatus(lastInsertId,0);
	            }
	            
	        } else {
	            response.put("status", "error");
	            response.put("message", "Failed to update the AE hoardings Info (Update).");
	        }
	    } catch (DataAccessException e) {
	        System.out.println("Data Access Exception:");
	        Throwable rootCause = e.getMostSpecificCause();
	        if (rootCause instanceof SQLException) {
	            SQLException sqlException = (SQLException) rootCause;
	            System.out.println("SQL State: " + sqlException.getSQLState());
	            System.out.println("Error Code: " + sqlException.getErrorCode());
	            System.out.println("Message: " + sqlException.getMessage());
	            response.put("status", "error");
	            response.put("message", sqlException.getMessage());
	            response.put("sqlState", sqlException.getSQLState());
	            response.put("errorCode", sqlException.getErrorCode());
	        } else {
	            System.out.println("Message: " + rootCause.getMessage());
	            response.put("status", "error");
	            response.put("message", rootCause.getMessage());
	        }
	    }

	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> updateMaterialInfo(
			String id, 
			String asset_id,
			String material_collected,
			String material_remarks,
	        MultipartFile materialfile) {
		
		final String materail_receipt = (materialfile != null && !materialfile.isEmpty()) ? fileUpload("hoarding_asset", asset_id, "asset_ae_materail_update", materialfile) : "";
		
		Map<String, Object> response = new HashMap<>();
		 
	    String sqlQuery = "UPDATE `hoarding_ae_update` SET `material_collected`=?, `material_receipt`=?, `material_remarks`=? WHERE `id` = ?";
	    jdbcHoardingsTemplate.update(sqlQuery, material_collected, materail_receipt, material_remarks, id);
	    
	    response.put("status", "success");
        response.put("message", "AE Hoardings Material Info updated successfully!");
        
        int idInt = Integer.parseInt(id); // Convert String to int
        
        updateHoadingAEmaterialPendingStatus(idInt,0);
        
        return Collections.singletonList(response);
	}

	public List<Map<String, Object>> updateFirInfo(
			String id,
			String asset_id,
	        String fir_number,
	        String fir_remarks,
	        MultipartFile firfile) {
		
		final String firfilepath = (firfile != null && !firfile.isEmpty()) ? fileUpload("hoarding_asset", asset_id, "asset_ae_fir_update", firfile) : "";
		
		Map<String, Object> response = new HashMap<>();
		 
	    String sqlQuery = "UPDATE `hoarding_ae_update` SET `fir_number`=?, `fir_image`=?, `fir_remarks`=? WHERE `asset_id` = ?";
	    jdbcHoardingsTemplate.update(sqlQuery, fir_number, firfilepath, fir_remarks, asset_id);
	    
	    response.put("status", "success");
        response.put("message", "AE Hoardings FIR Info updated successfully!");
        
        int idInt = Integer.parseInt(id); // Convert String to int
        
        updateHoadingAEFIRPendingStatus(idInt,0);
        
        return Collections.singletonList(response);
	}
	
	public String setHoadingAEUpdateinactive(String asset_id) {
	    String sqlQuery = "UPDATE `hoarding_ae_update` SET `isactive` = 0 WHERE `asset_id` = ?";
	    jdbcHoardingsTemplate.update(sqlQuery, asset_id);
	    return "success";
	}
	
	public String updateHoadingAEmaterialPendingStatus(int id,int status) {
	    String sqlQuery = "UPDATE `hoarding_ae_update` SET `material_pending` = ? WHERE `id` = ?";
	    jdbcHoardingsTemplate.update(sqlQuery, status, id);
	    return "success";
	}
	
	public String updateHoadingAEFIRPendingStatus(int id,int status) {
	    String sqlQuery = "UPDATE `hoarding_ae_update` SET `fir_pending` = ? WHERE `id` = ?";
	    jdbcHoardingsTemplate.update(sqlQuery, status, id);
	    return "success";
	}
	
	@Transactional
	public List<Map<String, Object>> unAuterrizedAssetlistAEPending(
			MultiValueMap<String, String> formData,
			String pendingType,
			String loginid) {
		
		String sqlQuery = "SELECT "
				+ "    al.`id`, "
				+ "    al.`category_id`, "
				+ "    ac.`name` AS category_name, "
				+ "    al.`name`, "
				+ "    al.`image`, "
				+ "    al.`latitude`, "
				+ "    al.`longitude`, "
				+ "    al.`zone`, "
				+ "    al.`ward`, "
				+ "    al.`streetid`, "
				+ "    al.`streetname`, "
				+ "    al.`cdate`, "
				+ "    al.`cby`, "
				+ "    al.`remarks`, "
				+ "    hi.`hitid`, "
				+ "    hi.`licenses_info`, "
				+ "    hi.`licenses_no`, "
				+ "    hi.`validity_date`, "
				+ "    hi.`case_no`, "
				+ "    hi.`ptax_no`, "
				+ "    hi.`ptax_name`, "
				+ "    hi.`ptax_mobile`, "
				+ "    hi.`agency_name`, "
				+ "    hi.`agency_mobile`, "
				+ "    hi.`fine_to`, "
				+ "	   hi.`fine_amount`, hi.`fine_status`, hi.`orderid`, hi.`cdate` as ae_cdate, hi.`cby` as ae_cby, "
                + "    hi.`tid`, hi.`mid`, hi.`serial_number`, "
				+ "    hit.`name` AS hoarding_info_type, "
				+ "    CONCAT('https://gccservices.in/gccofficialapp/files', al.`image`) AS imageurl, "
				+ "    haeu.`update_image`, "
				+ "    haeu.`remarks`, "
				+ "    haeu.`removed_type`, "
				+ "    haeu.`fir`, "
				+ "    haeu.`fir_number`, "
				+ "    haeu.`fir_image`, "
				+ "    haeu.`material_collected`, "
				+ "    haeu.`material_receipt`, "
				+ "    haeu.`cdate` AS ae_cdate, "
				+ "    haeu.`cby` AS ae_cby, "
				+ "    haeu.`material_pending`, "
				+ "    haeu.`fir_pending`, "
				+ "    haeu.`id` AS ae_data_id, "
				+ "    CONCAT('https://gccservices.in/gccofficialapp/files', haeu.`update_image`) AS ae_update_imageurl, "
				+ "    CONCAT('https://gccservices.in/gccofficialapp/files', haeu.`material_receipt`) AS ae_material_imageurl, "
				+ "    CONCAT('https://gccservices.in/gccofficialapp/files', haeu.`fir_image`) AS ae_fir_imageurl "
				+ "FROM "
				+ "    `asset_list` al "
				+ "INNER JOIN "
				+ "    `hoardings_info` hi ON al.id = hi.asset_id "
				+ "INNER JOIN "
				+ "    `asset_category` ac ON ac.id = al.category_id "
				+ "INNER JOIN "
				+ "    `hoarding_info_type` hit ON hit.id = hi.hitid "
				+ "INNER JOIN "
				+ "    `hoarding_ae_update` haeu ON haeu.asset_id = hi.asset_id "
				+ "WHERE "
				+ "    (al.`isactive` = 1 AND al.`isdelete` = 0) ";
        /*        
		String sqlQuery_where=" AND 1=0 AND haeu.`cby`= ?";
		
				if("materail".equals(pendingType)) {
                	sqlQuery_where = "AND (haeu.material_pending = 1) AND (haeu.`cby` = ? AND haeu.isactive=1) AND haeu.`removed_type`='GCC'";
				}
				
				if("fir".equals(pendingType)) {
                	sqlQuery_where = "AND (haeu.`fir_pending` = 1) AND (haeu.`cby` = ? AND haeu.isactive=1)";
				}
				
				sqlQuery = sqlQuery + sqlQuery_where;
		*/
		List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery,loginid);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Un-Auterrize AE Pending Hoarding list By Login");
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> ReportTile(
			String fromDate,
			String toDate) {
		
		String sqlQuery = "SELECT "
				+ "    hit.`name` AS hoarding_info_type, "
				+ "    COUNT(DISTINCT al.id) AS total_count, "
				+ "    SUM(CASE WHEN hi.`fine_status` = 'Transaction success' THEN hi.`fine_amount` ELSE 0 END) AS total_fine_amount, "
				+ "    SUM(SUM(CASE WHEN hi.`fine_status` = 'Transaction success' THEN hi.`fine_amount` ELSE 0 END)) OVER() AS total_amount "
				+ "FROM "
				+ "    `asset_list` al "
				+ "LEFT JOIN "
				+ "    `hoardings_info` hi ON al.id = hi.asset_id "
				+ "LEFT JOIN "
				+ "    `asset_category` ac ON ac.id = al.category_id "
				+ "LEFT JOIN "
				+ "    `hoarding_info_type` hit ON hit.id = hi.hitid "
				+ "WHERE "
				+ "    al.`isactive` = 1 "
				+ "    AND al.`isdelete` = 0 "
				+ "    AND DATE(al.`cdate`) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
				+ "    AND hit.`name` IS NOT NULL "
				+ "GROUP BY "
				+ "    hit.`name` "
				+ "ORDER BY "
				+ "    hit.`id`";
		
		List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery, fromDate, toDate);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Main Report Data");
        response.put("data", result);
    		
        return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> reportZoneList(
			String fromDate,
			String toDate) {
		
		String sqlQuery = "SELECT "
				+ "    al.`zone`, "
				+ "    COUNT(DISTINCT CASE WHEN hit.`name` = 'Authorized' THEN al.id END) AS Authorized, "
				+ "    COUNT(DISTINCT CASE WHEN hit.`name` = 'Court Case' THEN al.id END) AS Court_Case, "
				+ "    COUNT(DISTINCT CASE WHEN hit.`name` = 'Unauthorized' THEN al.id END) AS Unauthorized, "
				+ "    COUNT(DISTINCT CASE WHEN hit.`name` = 'Removed' THEN al.id END) AS Removed, "
				+ "    SUM(CASE WHEN hit.`name` = 'Authorized' AND hi.`fine_status` = 'Transaction success' THEN hi.`fine_amount` ELSE 0 END) AS Authorized_Amount, "
				+ "    SUM(CASE WHEN hit.`name` = 'Court Case' AND hi.`fine_status` = 'Transaction success' THEN hi.`fine_amount` ELSE 0 END) AS Court_Case_Amount, "
				+ "    SUM(CASE WHEN hit.`name` = 'Unauthorized' AND hi.`fine_status` = 'Transaction success' THEN hi.`fine_amount` ELSE 0 END) AS Unauthorized_Amount, "
				+ "    SUM(CASE WHEN hit.`name` = 'Removed' AND hi.`fine_status` = 'Transaction success' THEN hi.`fine_amount` ELSE 0 END) AS Removed_Amount, "
				+ "    SUM(CASE WHEN hi.`fine_status` = 'Transaction success' THEN hi.`fine_amount` ELSE 0 END) AS Total_Amount "
				+ "FROM "
				+ "    `asset_list` al "
				+ "LEFT JOIN "
				+ "    `hoardings_info` hi ON al.id = hi.asset_id "
				+ "LEFT JOIN "
				+ "    `asset_category` ac ON ac.id = al.category_id "
				+ "LEFT JOIN "
				+ "    `hoarding_info_type` hit ON hit.id = hi.hitid "
				+ "WHERE "
				+ "    al.`isactive` = 1 "
				+ "    AND al.`isdelete` = 0 "
				+ "    AND DATE(al.`cdate`) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
				+ "    AND hit.`name` IS NOT NULL "
				+ "GROUP BY "
				+ "    al.`zone` "
				+ "ORDER BY "
				+ "    al.`zone` ";
		
		List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery, fromDate, toDate);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Main Report Data");
        response.put("data", result);
    		
        return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> reportWardList(
			String zone,
			String fromDate,
			String toDate) {
		
		String sqlQuery = "SELECT "
				+ "    al.`ward`,"
				+ "    COUNT(DISTINCT CASE WHEN hit.`name` = 'Authorized' THEN al.id END) AS Authorized, "
				+ "    COUNT(DISTINCT CASE WHEN hit.`name` = 'Court Case' THEN al.id END) AS Court_Case, "
				+ "    COUNT(DISTINCT CASE WHEN hit.`name` = 'Unauthorized' THEN al.id END) AS Unauthorized, "
				+ "    COUNT(DISTINCT CASE WHEN hit.`name` = 'Removed' THEN al.id END) AS Removed, "
				+ "    SUM(CASE WHEN hit.`name` = 'Authorized' AND hi.`fine_status` = 'Transaction success' THEN hi.`fine_amount` ELSE 0 END) AS Authorized_Amount, "
				+ "    SUM(CASE WHEN hit.`name` = 'Court Case' AND hi.`fine_status` = 'Transaction success' THEN hi.`fine_amount` ELSE 0 END) AS Court_Case_Amount, "
				+ "    SUM(CASE WHEN hit.`name` = 'Unauthorized' AND hi.`fine_status` = 'Transaction success' THEN hi.`fine_amount` ELSE 0 END) AS Unauthorized_Amount, "
				+ "    SUM(CASE WHEN hit.`name` = 'Removed' AND hi.`fine_status` = 'Transaction success' THEN hi.`fine_amount` ELSE 0 END) AS Removed_Amount, "
				+ "    SUM(CASE WHEN hi.`fine_status` = 'Transaction success' THEN hi.`fine_amount` ELSE 0 END) AS Total_Amount "
				+ "FROM "
				+ "    `asset_list` al "
				+ "LEFT JOIN "
				+ "    `hoardings_info` hi ON al.id = hi.asset_id "
				+ "LEFT JOIN "
				+ "    `asset_category` ac ON ac.id = al.category_id "
				+ "LEFT JOIN "
				+ "    `hoarding_info_type` hit ON hit.id = hi.hitid "
				+ "WHERE "
				+ "    al.`isactive` = 1 "
				+ "    AND al.`isdelete` = 0 "
				+ "    AND al.`zone` = ? "
				+ "    AND DATE(al.`cdate`) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
				+ "    AND hit.`name` IS NOT NULL "
				+ "GROUP BY "
				+ "    al.`ward` "
				+ "ORDER BY "
				+ "    al.`ward`";
		
		List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery, zone, fromDate, toDate);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Main Report Data");
        response.put("data", result);
    		
        return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> reportListBySelect(
			String ward,
			String hitid,
			String fromDate,
			String toDate) {
		
		String sqlQuery = "SELECT "
				+ "    al.`id`, "
				+ "    al.`category_id`, "
				+ "    ac.`name` AS category_name, "
				+ "    al.`name`, "
				+ "    al.`image`, "
				+ "    al.`latitude`, "
				+ "    al.`longitude`, "
				+ "    al.`zone`, "
				+ "    al.`ward`, "
				+ "    al.`streetid`, "
				+ "    al.`streetname`, "
				+ "    al.`cdate`, "
				+ "    al.`cby`, "
				+ "    al.`remarks`, "
				+ "    hi.`hitid`, "
				+ "    hi.`licenses_info`, "
				+ "    hi.`licenses_no`, "
				+ "    hi.`validity_date`, "
				+ "    hi.`case_no`, "
				+ "    hi.`ptax_no`, "
				+ "    hi.`ptax_name`, "
				+ "    hi.`ptax_mobile`, "
				+ "    hi.`agency_name`, "
				+ "    hi.`agency_mobile`, "
				+ "    hi.`fine_to`, "
				+ "    hi.`fine_amount`, "
				+ "    hi.`fine_status`, "
				+ "    hi.`orderid`, "
				+ "    hi.`cdate` as ae_cdate, "
				+ "    hi.`cby` as ae_cby, "
				+ "    hi.`tid`, "
				+ "    hi.`mid`, "
				+ "    hi.`serial_number`, "
				+ "    hit.`name` AS hoarding_info_type, "
				+ "    CONCAT('https://gccservices.in/gccofficialapp/files', al.`image`) AS imageurl, "
				+ "    haeu.`update_image`, "
				+ "    haeu.`remarks`, "
				+ "    haeu.`removed_type`, "
				+ "    haeu.`fir`, "
				+ "    haeu.`fir_number`, "
				+ "    haeu.`fir_image`, "
				+ "    haeu.`material_collected`, "
				+ "    haeu.`material_receipt`, "
				+ "    haeu.`cdate` AS ae_cdate, "
				+ "    haeu.`cby` AS ae_cby, "
				+ "    haeu.`material_pending`, "
				+ "    haeu.`fir_pending`, "
				+ "    haeu.`id` AS ae_data_id, "
				+ "    CONCAT('https://gccservices.in/gccofficialapp/files', haeu.`update_image`) AS ae_update_imageurl, "
				+ "    CONCAT('https://gccservices.in/gccofficialapp/files', haeu.`material_receipt`) AS ae_material_imageurl, "
				+ "    CONCAT('https://gccservices.in/gccofficialapp/files', haeu.`fir_image`) AS ae_fir_imageurl "
				+ "FROM "
				+ "    `asset_list` al "
				+ "LEFT JOIN "
				+ "    `hoardings_info` hi ON al.id = hi.asset_id "
				+ "LEFT JOIN "
				+ "    `asset_category` ac ON ac.id = al.category_id "
				+ "LEFT JOIN "
				+ "    `hoarding_info_type` hit ON hit.id = hi.hitid "
				+ "LEFT JOIN "
				+ "    `hoarding_ae_update` haeu ON haeu.asset_id = hi.asset_id "
				+ "WHERE "
				+ "    al.`isactive` = 1 "
				+ "    AND al.`isdelete` = 0 "
				+ "    AND al.`ward` = ? "
				+ "    AND hi.`hitid` = ? "
				+ "    AND DATE(al.`cdate`) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
				+ "    AND hit.`name` IS NOT NULL "
				+ "GROUP BY "
				+ "    al.`id` "
				+ "ORDER BY"
				+ "    al.`id` DESC";
		
		List<Map<String, Object>> result = jdbcHoardingsTemplate.queryForList(sqlQuery, ward, hitid, fromDate, toDate);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Main Report Data");
        response.put("data", result);
    		
        return Collections.singletonList(response);
	}
}
