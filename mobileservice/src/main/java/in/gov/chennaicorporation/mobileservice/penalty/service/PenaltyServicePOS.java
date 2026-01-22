package in.gov.chennaicorporation.mobileservice.penalty.service;

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
public class PenaltyServicePOS {
	private JdbcTemplate jdbcPenaltyTemplate;
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
	
	@Autowired
	public void setDataSource(@Qualifier("mysqlPenaltyDataSource") DataSource penaltyDataSource) {
		this.jdbcPenaltyTemplate = new JdbcTemplate(penaltyDataSource);
	}
	
	@Autowired
	public PenaltyServicePOS(Environment environment) {
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
	
	public String fileUpload(String name, String id, MultipartFile file) {
		
		int lastInsertId = 0;
		// Set the file path where you want to save it
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("penalty_foldername");
        var year =DateTimeUtil.getCurrentYear();
        var month =DateTimeUtil.getCurrentMonth();
        
        uploadDirectory = uploadDirectory + serviceFolderName +"pos/"+ year +"/"+month+"/";
        
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
	        
	        String filepath_txt = "/"+serviceFolderName +"pos/"+ year +"/"+month+"/"+fileName;
	        
	        // Create a new Path object
            Path path = Paths.get(filePath);
            
            // Get the bytes of the file
            byte[] bytes = file.getBytes();
            
            // Compress the image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50% quality
            
            // Write the bytes to the file
            Files.write(path, bytes);
            
            System.out.println(filePath);
            return filepath_txt;
            
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to save file " + file.getOriginalFilename();
        } 
	}
	
	//// POS ////
	@Transactional
	public List<Map<String, Object>> getPenaltyDepartment() {
		String sqlQuery = "SELECT * FROM `penalty_department` WHERE (`isactive`=1 AND `isdelete`=0) ";
		sqlQuery = sqlQuery + " ORDER BY `display_order`, `name` ASC";
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
		
		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
        response.put("message", "Department list");
        response.put("data", result);
        
		return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> getPenaltyTypeList(String departmentId) {
		String sqlQuery = "SELECT * FROM `penalty_category` WHERE (`isactive`=1 AND `isdelete`=0) ";
		
		if(departmentId!=null && !departmentId.isEmpty() && !departmentId.isBlank()) {
			sqlQuery= sqlQuery +" AND `mappingid`='"+departmentId+"'";
		}
		
		//sqlQuery = sqlQuery + " ORDER BY `display_order`, `name` ASC";
		sqlQuery = sqlQuery + " ORDER BY `display_order` ASC";
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
		
		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
        response.put("message", "Penalty category list");
        response.put("data", result);
        
		return Collections.singletonList(response);
	}
		
	@Transactional
	public List<Map<String, Object>> getPenaltyAmount(String categoryId, String ton) {
		String sqlQuery = "SELECT * FROM `penalty_amount` WHERE (`isactive`=1 AND `isdelete`=0) ";
		
		if(categoryId!=null && !categoryId.isEmpty() && !categoryId.isBlank()) {
			sqlQuery= sqlQuery +" AND `category_id`='"+categoryId+"'";
		}
		if(ton!=null && !ton.isEmpty() && !ton.isBlank()) {
			sqlQuery= sqlQuery +" AND `ton`='"+ton+"'";
		}
		
		sqlQuery = sqlQuery + "";
		
		System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
		
		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
        response.put("message", "Penalty amount details");
        response.put("data", result);
        
		return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> getBankList() {
		String sqlQuery = "SELECT `id`, `code`, `name`, `narration` FROM `bank_list` WHERE `isactive`=1 AND `isdelete`=0";
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
		
		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
        response.put("message", "Bank list");
        response.put("data", result);
        
		return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> getOfflineModeTypes() {
		String sqlQuery = "SELECT `id`, `name` FROM `offline_mode_type` WHERE `isactive`=1 AND `isdelete`=0";
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
		
		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
        response.put("message", "Offline mode types");
        response.put("data", result);
        
		return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> generateChallanPOS(
			MultiValueMap<String, String> formData,
			String category_id,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String loginId,
			String violatorName,
			String violatorPhone,
			String violatorCompany,
			String ton,
			String amount,
			String tid,
			String mid,
			String serialNumber,
			MultipartFile file) {
		
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		String orderid = generateRandomString();
		String image = fileUpload("challan",category_id,file);
		
		String sqlQuery = "INSERT INTO `penalty_challan`(`category_id`, `image`, `latitude`, `longitude`, "
				+ "`zone`, `ward`, `challan_by`, "
				+ "`violator_name`, `violator_phone`, `ton`, `fine_amount`,`orderid`,`violator_company`,"
				+ "`tid`, `mid`, `serial_number`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		try {
            int affectedRows = jdbcPenaltyTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, category_id);
                    ps.setString(2, image);
                    ps.setString(3, latitude);
                    ps.setString(4, longitude);
                    ps.setString(5, zone);
                    ps.setString(6, ward);
                    ps.setString(7, loginId);
                    ps.setString(8, violatorName);
                    ps.setString(9, violatorPhone);
                    ps.setString(10, ton);
                    ps.setString(11, amount);
                    ps.setString(12, orderid);
                    ps.setString(13, violatorCompany);
                    ps.setString(14, tid);
                    ps.setString(15, mid);
                    ps.setString(16, serialNumber);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                //CONCAT('"+fileBaseUrl+"/gccofficialapp/files', cp.photo_url) AS photos,
                String sqlQuery_txt = "SELECT `category_id`,CONCAT('"+fileBaseUrl+"/gccofficialapp/files',`image`) as imageurl, `latitude`, `longitude`, `zone`, `ward`, "
                		+ "`challan_by`, `violator_name`, `violator_phone`, `violator_company`, `ton`, "
                		+ "`fine_amount`,`orderid`,`tid`,`mid`,`serial_number` From `penalty_challan` WHERE id='"+lastInsertId+"' LIMIT 1";
                List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery_txt);
                
                //response.put("insertid", lastInsertId);
                response.put("status", 200);
                response.put("message", "A new Penalty (Chellan) was generated successfully!");
                //response.put("orderid", orderid);
                response.put("data", result);
                
                updateZoneWard(lastInsertId,loginId); // Update Zone and ward details 
                
                System.out.println("A new Penalty (Chellan) was generated successfully! Insert ID: " + generatedId);
            } else {
                response.put("status", 201);
                response.put("message", "Failed to insert a new Penalty (Chellan).");
                response.put("data", "");
            }
        } catch (DataAccessException e) {
            System.out.println("Data Access Exception:");
            Throwable rootCause = e.getMostSpecificCause();
            if (rootCause instanceof SQLException) {
                SQLException sqlException = (SQLException) rootCause;
                System.out.println("SQL State: " + sqlException.getSQLState());
                System.out.println("Error Code: " + sqlException.getErrorCode());
                System.out.println("Message: " + sqlException.getMessage());
                response.put("status", 500);
	            response.put("message", "Database error while storing new Penalty (Chellan).");
	            response.put("error", e.getMessage());
            } else {
                System.out.println("Message: " + rootCause.getMessage());
                response.put("status", 400);
    	        response.put("message", "Failed to insert a new Penalty (Chellan).");
            }
        }
		
        return Collections.singletonList(response);
    }
	
	public String updateZoneWard(int id, String loginid) {
	    // Select query to fetch data from the user_zone table
	    String sqluserLocation = "SELECT `SNO`, `ID`, `USER_NAME`, `FIRST_NAME`, `LOC_FLAG`, `LOC`, `ACTUAL_LOC`, `ZONE_HQ` "
	            + "FROM `user_zone` WHERE `ID`=?";
	    // Fetch the result from the query (assuming only one row is returned)
	    List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqluserLocation, loginid);

	    if (result.isEmpty()) {
	    	System.out.println( "Challan User Loaction update: Error: User not found.");
	        // If no result is found, return an error message
	        return "Error: User not found.";
	        
	    }

	    // Extract values from the result map (assuming the first record)
	    Map<String, Object> userZoneData = result.get(0);
	    String locFlag = (String) userZoneData.get("LOC_FLAG");
	    String loc = (String) userZoneData.get("LOC");
	    String actualLoc = (String) userZoneData.get("ACTUAL_LOC");
	    String zoneHq = (String) userZoneData.get("ZONE_HQ");

	    // Update query to update the penalty_challan table using the fetched values
	    String sqlQuery = "UPDATE penalty_challan SET `LOC_FLAG` = ?, `LOC` = ?, `ACTUAL_LOC` = ?, `ZONE_HQ` = ? WHERE id = ? LIMIT 1";
	    
	    // Execute the update query using the values fetched from user_zone table
	    jdbcPenaltyTemplate.update(sqlQuery, locFlag, loc, actualLoc, zoneHq, id);

	    System.out.println( "Challan User Loaction Updated (id, loginid): " + id +" , " + loginid);
	    return "success"; // Return success after the update
	}
	
	@Transactional
	public List<Map<String, Object>> getChallanPOSData(
			MultiValueMap<String, String> formData,
			String orderid,
			String id) {
		
		String sqlQuery = "SELECT `category_id`,CONCAT('"+fileBaseUrl+"/gccofficialapp/files',`image`) as imageurl, `latitude`, `longitude`, `zone`, `ward`, "
        		+ "`challan_by`, `violator_name`, `violator_phone`, `ton`, "
        		+ "`fine_amount`,`orderid`,`status` From `penalty_challan` WHERE (`isactive`=1 AND `isdelete`=0) ";
		
		if(orderid!=null && !orderid.isEmpty() && !orderid.isBlank()) {
			sqlQuery= sqlQuery +" AND `orderid`='"+orderid+"'";
		}
		if(id!=null && !id.isEmpty() && !id.isBlank()) {
			sqlQuery= sqlQuery +" AND `id`='"+id+"'";
		}
		
		sqlQuery = sqlQuery + " LIMIT 1";
		
		//System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Challan Details");
        response.put("data", result);
		
        return result;
    }
	
	/*
	@Transactional
	public List<Map<String, Object>> updateOrderDetails(
			MultiValueMap<String, String> formData,
			String orderid,
			String id) {
		
		String paymentStatus = "paid";
		 
		String sqlQuery = "UPDATE `penalty_challan` SET `status`='"+paymentStatus+"'";
		
		if(orderid!=null && !orderid.isEmpty() && !orderid.isBlank()) {
			sqlQuery= sqlQuery +" AND `orderid`='"+orderid+"'";
		}
		
		sqlQuery = sqlQuery + " LIMIT 1";
		
		System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
		
		Map<String, Object> response = new HashMap<>();
		//response.put("insertid", lastInsertId);
        response.put("status", 200);
        response.put("message", "Payment Status updated successfully!");
        response.put("orderid", orderid);
        response.put("data", result);
        
        return result;
    }
	*/
	
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
	            int affectedRows = jdbcPenaltyTemplate.update(new PreparedStatementCreator() {
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
	                String sqlQuery_joined = "SELECT pc.category_id, CONCAT('" + fileBaseUrl + "/gccofficialapp/files', pc.image) as imageurl, "
	                        + "pc.latitude, pc.longitude, pc.zone, pc.ward, pc.challan_by, "
	                        + "pc.violator_name, pc.violator_phone, pc.ton, pc.fine_amount, pc.orderid, pc.status, "
	                        + "bt.mid, bt.tid, bt.txnId, bt.bankName, bt.batchNumber, bt.transactionAmount, bt.transactionTitle, "
	                        + "bt.txnCardNo, bt.txnDate, bt.txnInvoice, bt.txnMode, bt.txnRefNo "
	                        + "FROM penalty_challan pc "
	                        + "LEFT JOIN bank_transactions bt ON pc.orderid = bt.txnId "
	                        + "WHERE pc.isactive = 1 AND pc.isdelete = 0 AND bt.id = ? LIMIT 1";
	                System.out.println(sqlQuery_joined);
	                // Execute the query
	                List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery_joined, lastInsertId);
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
	            int affectedRows = jdbcPenaltyTemplate.update(new PreparedStatementCreator() {
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
	                String sqlQuery_joined = "SELECT pc.category_id, CONCAT('" + fileBaseUrl + "/gccofficialapp/files', pc.image) as imageurl, "
	                        + "pc.latitude, pc.longitude, pc.zone, pc.ward, pc.challan_by, "
	                        + "pc.violator_name, pc.violator_phone, pc.ton, pc.fine_amount, pc.orderid, pc.status, "
	                        + "bt.mid, bt.tid, bt.txnId, bt.bankName, bt.batchNumber, bt.transactionAmount, bt.transactionTitle, "
	                        + "bt.txnCardNo, bt.txnDate, bt.txnInvoice, bt.txnMode, bt.txnRefNo "
	                        + "FROM penalty_challan pc "
	                        + "LEFT JOIN bank_transactions bt ON pc.orderid = bt.txnId "
	                        + "WHERE pc.isactive = 1 AND pc.isdelete = 0 AND bt.txnId = ? LIMIT 1";
	                System.out.println(sqlQuery_joined);
	                // Execute the query
	                List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery_joined, txnId);
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
	    String sqlQuery = "UPDATE penalty_challan SET status = ? WHERE orderid = ? LIMIT 1";
	    jdbcPenaltyTemplate.update(sqlQuery, status, orderid);
	    return "success";
	}
	
	@Transactional
	public List<Map<String, Object>> storeOfflineTransaction(
			MultiValueMap<String, String> formData,
			String transactionResponseStatus,
			String bankName,
			String transactionAmount,
			String txnDate,
			String txnTime,
			String branchname,
			String extraInfo,
			String extraInfo2,
			String extraInfo3,
			String txnMode,
			String txnId,
			MultipartFile file) {
		
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		String orderid = generateRandomString();
		
		String image = fileUpload("offline",extraInfo,file);
		
		// Convert transaction amount properly
	    Double transactionAmount_d = Double.valueOf(transactionAmount); // Corrected line
		
		String sqlQuery = "INSERT INTO `bank_transactions`(`transactionResponseStatus`, `bankName`, `transactionAmount`, "
				+ "`txnDate`, `txnTime`, `branchname`,`extraInfo`,`extraInfo2`, `extraInfo3`,`txnMode`, `txnId`,`file`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
            int affectedRows = jdbcPenaltyTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, transactionResponseStatus);
                    ps.setString(2, bankName);
                    ps.setString(3, transactionAmount);
                    ps.setString(4, txnDate);
                    ps.setString(5, txnTime);
                    ps.setString(6, branchname);
                    ps.setString(7, extraInfo);
                    ps.setString(8, extraInfo2);
                    ps.setString(9, extraInfo3);
                    ps.setString(10, txnMode);
                    ps.setString(11, txnId);
                    ps.setString(12, image);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
            	Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                
                updatePaymentStatus(txnId, transactionResponseStatus);
                
             // Fetch inserted data for response, joining `penalty_challan` and `bank_transactions`
                String sqlQuery_joined = "SELECT pc.category_id, CONCAT('" + fileBaseUrl + "/gccofficialapp/files', pc.image) as imageurl, "
                        + "pc.latitude, pc.longitude, pc.zone, pc.ward, pc.challan_by, "
                        + "pc.violator_name, pc.violator_phone, pc.ton, pc.fine_amount, pc.orderid, pc.status, "
                        + "bt.mid, bt.tid, bt.txnId, bt.bankName, bt.batchNumber, bt.transactionAmount, bt.transactionTitle, "
                        + "bt.txnCardNo, bt.txnDate, bt.txnInvoice, bt.txnMode, bt.txnRefNo, " 
                        + "bt.branchname, bt.extraInfo, bt.extraInfo2, bt.extraInfo3, CONCAT('" + fileBaseUrl + "/gccofficialapp/files', bt.file) as offlineimageurl " 
                        + "FROM penalty_challan pc "
                        + "LEFT JOIN bank_transactions bt ON pc.orderid = bt.txnId "
                        + "WHERE pc.isactive = 1 AND pc.isdelete = 0 AND bt.id = ? LIMIT 1";
                System.out.println(sqlQuery_joined);
                // Execute the query
                List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery_joined, lastInsertId);
                
                //response.put("insertid", lastInsertId);
                response.put("status", 200);
                response.put("message", "A new Penalty (Chellan) was generated successfully!");
                //response.put("orderid", orderid);
                response.put("data", result);
                
                System.out.println("Transaction stored successfully!");
            } else {
                response.put("status", 201);
                response.put("message", "Failed to store the transaction.");
                response.put("data", "");
            }
        } catch (DataAccessException e) {
            System.out.println("Data Access Exception:");
            Throwable rootCause = e.getMostSpecificCause();
            if (rootCause instanceof SQLException) {
                SQLException sqlException = (SQLException) rootCause;
                System.out.println("SQL State: " + sqlException.getSQLState());
                System.out.println("Error Code: " + sqlException.getErrorCode());
                System.out.println("Message: " + sqlException.getMessage());
                response.put("status", 500);
	            response.put("message", "Database error while storing transaction.");
	            response.put("error", e.getMessage());
            } else {
                System.out.println("Message: " + rootCause.getMessage());
                response.put("status", 400);
    	        response.put("message", "Transaction failed or invalid status.");
            }
        }
		
        return Collections.singletonList(response);
    }

}
