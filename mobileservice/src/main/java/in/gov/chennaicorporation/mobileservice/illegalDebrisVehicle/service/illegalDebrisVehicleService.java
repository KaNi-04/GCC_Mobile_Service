package in.gov.chennaicorporation.mobileservice.illegalDebrisVehicle.service;

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
import java.time.LocalDateTime;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

public class illegalDebrisVehicleService {
private JdbcTemplate jdbcTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlillegalDebrisVehicleSource") DataSource IllegalDebrisVehicleDataSource) {
		this.jdbcTemplate = new JdbcTemplate(IllegalDebrisVehicleDataSource);
	}
    
    @Autowired
	public illegalDebrisVehicleService(Environment environment) {
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
	
	public static String generateRandomFileString(int lenthval) {
		StringBuilder result = new StringBuilder(lenthval);
		for (int i = 0; i < lenthval; i++) {
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
		
	public String fileUpload(String name, String id, MultipartFile file, String filetype) {

		int lastInsertId = 0;
		// Set the file path where you want to save it
		String uploadDirectory = environment.getProperty("file.upload.directory");
		String serviceFolderName = environment.getProperty("illegalDebrisVehicle_foldername");
		var year = DateTimeUtil.getCurrentYear();
		var month = DateTimeUtil.getCurrentMonth();
		var date = DateTimeUtil.getCurrentDay();

		uploadDirectory = uploadDirectory + serviceFolderName + filetype + "/" + year + "/" + month + "/" + date;

		try {
			// Create directory if it doesn't exist
			Path directoryPath = Paths.get(uploadDirectory);
			if (!Files.exists(directoryPath)) {
				Files.createDirectories(directoryPath);
			}

			// Datetime string
			String datetimetxt = DateTimeUtil.getCurrentDateTime();
			
			datetimetxt = datetimetxt + "_"+ generateRandomFileString(6); // Attached Random text
			
			// File name
			String fileName = name + "_" + id + "_" + datetimetxt + "_" + file.getOriginalFilename();
			fileName = fileName.replaceAll("\\s+", ""); // Remove space on filename

			String filePath = uploadDirectory + "/" + fileName;

			String filepath_txt = "/" + serviceFolderName + filetype + "/" + year + "/" + month + "/" + date + "/" + fileName;

			// Create a new Path object
			Path path = Paths.get(filePath);

			// Get the bytes of the file
			byte[] bytes = file.getBytes();
			
			if(filetype.equalsIgnoreCase("image"))
			{
				// Compress the image
				BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
				byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50% quality
				// Write the bytes to the file
				Files.write(path, compressedBytes);
			}
			else {
				// Write the bytes to the file
				Files.write(path, bytes);
			}
				// Get current date & time
		        LocalDateTime now = LocalDateTime.now();
	
		        // Format date-time (optional)
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		        
				System.out.println("Date: "+ now.format(formatter));
				System.out.println("Activity: " + serviceFolderName);
				System.out.println("File Type: " + filetype);
				System.out.println("File Upload Path: " + filePath);
				System.out.println("File Path: " + filepath_txt);
				
			return filepath_txt;

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to save file " + file.getOriginalFilename());
			return "error";
		}
	}
	
	public String getWardByLoginId(String loginid, String type) {
        String sqlQuery = "SELECT ward FROM gcc_penalty_hoardings.hoading_user_list WHERE userid = ? LIMIT 1";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery, loginid);

        if (!results.isEmpty()) {
            //System.out.println("Ward....." + results);
            // Extract the ward value from the first result
            return (String) results.get(0).get("ward");
        }

        return "000";
    }
	
	public List<Map<String, Object>> getVehicleTypeList() {
        String sql = "SELECT * FROM `vehicle_type` WHERE isactive=1 ORDER BY `name`";
        return jdbcTemplate.queryForList(sql);
    }
	
	public List<Map<String, Object>> getWasteTypeList() {
        String sql = "SELECT * FROM `waste_type` WHERE isactive=1 ORDER BY `name`";
        return jdbcTemplate.queryForList(sql);
    }
	
	@Transactional
	public List<Map<String, Object>> saveCatchVehicleData(
	        String zone,
	        String ward,
	        String cby,
	        String latitude,
	        String longitude,
	        String vehicle_no,
	        String place_name,
	        String tonage,
	        //String type,
	        String remarks,
	        MultipartFile file_1
	) {

		String filetxt = cby;
		
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    // For Image
	    String filetype = "image";
	    String action = "identified_waste";
	    
	    // for Image 1
	    String file1 = fileUpload(action, filetxt, file_1, filetype);
	    
	    if ("error".equalsIgnoreCase(file1)) {
	        response.put("status", "error");
	        response.put("message", "Image-1 insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    String insertSqltxt = "INSERT INTO `vehicle_catch_location_mapping` "
	    		+ "(`zone`, `ward`, `cby`, `latitude`, `longitude`, "
	    		+ "	`vehicle_no`, `place_name`,`tonage`, `remarks`, `file`,`status`) "
                + "VALUES "
                + "(?,?,?,?,?,?,?,?,?,?,?)";

	    String insertSql = insertSqltxt;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"vclid"});
	        int i = 1;
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, cby);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        
	        ps.setString(i++, vehicle_no);
	        ps.setString(i++, place_name);
	        ps.setString(i++, tonage);
	       // ps.setString(i++, type);
	        ps.setString(i++, remarks);
	        
	        ps.setString(i++, file1);
	        ps.setString(i++, "Pending");
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("illegal_debris_vehicle", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", "Data inserted successfully.");
	    } else {
	    	response.put("illegal_debris_vehicle", action + " error!");
	        response.put("status", "error");
	        response.put("message", "Data insert failed.");
	    }
	
	    result.add(response);
	    return result;
	}
	
	public List<Map<String, Object>> getComplaintList(String loginId) {
		
		String ward = getWardByLoginId(loginId,"");
		
        String sql = "SELECT "
        		+ "    vclm.vclid, "
        		+ "    vclm.zone AS comp_zone, "
        		+ "    vclm.ward AS comp_ward, "
        		+ "    eu.EXTRAFIELD2 AS comp_contact, "
        		+ "    eu.FIRST_NAME AS comp_name, "
        		+ "    DATE_FORMAT(vclm.cdate, '%d-%m-%Y %r') AS comp_date, "
        		+ "    vclm.latitude AS comp_latitude, "
        		+ "    vclm.longitude AS comp_longitude, "
        		+ "    vclm.vehicle_no AS comp_vehicle_no, "
        		+ "    vclm.place_name AS comp_place_name, "
        		+ "    vclm.tonage AS appx_tonage, "
        		+ "    wt.name AS comp_type, "
        		+ "    CONCAT('https://gccservices.in/gccofficialapp/files', vclm.file) AS comp_image "
        		+ "FROM vehicle_catch_location_mapping vclm "
        		+ "JOIN erp_pgr.EG_USER eu "
        		+ "    ON vclm.cby = eu.id "
        		+ "LEFT JOIN waste_type wt "
        		+ "    ON vclm.typeid = wt.typeid "
        		+ "WHERE vclm.isactive = 1 "
        		+ "  AND vclm.status !='Close' "
        		+ "  AND vclm.ward = ? ";
        
        return jdbcTemplate.queryForList(sql, ward);
    }
	
	private void inactiveTheReply(String vclid) {
	    String sql = "UPDATE `waste_reply` SET `isactive`=0 WHERE `vclid`=?";
	    jdbcTemplate.update(sql,vclid);
	}
	
	private void updateStatus(String vclid, int reply_id, String status) {
	    String sql = "UPDATE `vehicle_catch_location_mapping` SET `status`=?, reply_id=? WHERE `vclid`=?";
	    jdbcTemplate.update(sql, status, reply_id, vclid);
	}
	
	private boolean isAlreadyClosed(String vclid) {
	    String sql = "SELECT COUNT(*) FROM vehicle_catch_location_mapping " +
	                 "WHERE vclid = ? AND status = 'Close'";
	    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, vclid);
	    return count != null && count > 0;
	}
	
	@Transactional
	public List<Map<String, Object>> step3( // Create Challan
			MultiValueMap<String, String> formData,
			String vclid,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String streeId,
			String streetName,
			String loginId,
			String remarks,
			String violatorName,
			String violatorPhone,
			String fineAmount,
			String vehicle_no,
			MultipartFile file) {
		
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		String image = ""; 
		/*fileUpload("cc_step2",cinfoid,file);*/
		String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		
		String orderid = "VEH_"+today+"_"+generateRandomString();
		
		String sqlQuery = "INSERT INTO `vehicle_catch_challan`(`vclid`, `image`, `latitude`, `longitude`, "
				+ "`zone`, `ward`, `streetid`, `streetname`, `challan_by`,`remarks`, "
				+ " `violator_name`, `violator_phone`,`fine_amount`,`orderid`,`vehicle_no`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
            int affectedRows = jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1,  vclid);
                    ps.setString(2, image);
                    ps.setString(3, latitude);
                    ps.setString(4, longitude);
                    ps.setString(5, zone);
                    ps.setString(6, ward);
                    ps.setString(7, streeId);
                    ps.setString(8, streetName);
                    ps.setString(9, loginId);
                    ps.setString(10, remarks);
                    ps.setString(11, violatorName);
                    ps.setString(12, violatorPhone);
                    ps.setString(13, fineAmount);
                    ps.setString(14, orderid);
                    ps.setString(15, vehicle_no);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "A new vehicle Penalty (Chellan) was generated successfully!");
                System.out.println("A new vehicle Penalty (Chellan) was generated successfully! Insert ID: " + generatedId);
                
                updateStep2(vclid,"challan");
                
            } else {
                response.put("status", "error");
                response.put("message", "Failed to insert a new vehicle Penalty (Chellan).");
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

	public String updateStep2(String vclid, String status) {
	    String sqlQuery = "UPDATE vehicle_catch_location_mapping SET status = ? WHERE vclid IN ("+vclid+")";
	    //System.out.println(sqlQuery);
	    jdbcTemplate.update(sqlQuery, status);
	    return "success";
	}
	
	@Transactional
	public List<Map<String, Object>> getChallanPOSData(
			MultiValueMap<String, String> formData,
			String orderid,
			String loginid) {
		
		String sqlQuery = "SELECT `vclid`,"
						+ "`latitude`, `longitude`, `zone`, `ward`, "
        		+ "`challan_by`, `violator_name`, `violator_phone`, `vehicle_no`,"
        		+ "`fine_amount`,`orderid`,`status` From `vehicle_catch_challan` WHERE (`isactive`=1 AND `isdelete`=0) AND (`status` <> 'Transaction success' AND `status` <> 'out') ";
		
		if(orderid!=null && !orderid.isEmpty() && !orderid.isBlank()) {
			sqlQuery= sqlQuery +" AND `orderid`='"+orderid+"'";
		}
		if(loginid!=null && !loginid.isEmpty() && !loginid.isBlank()) {
			sqlQuery= sqlQuery +" AND `challan_by`='"+loginid+"'";
		}
		
		//sqlQuery = sqlQuery + " LIMIT 1";
		
		//System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcTemplate.queryForList(sqlQuery);
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
	    String sqlQuery = "UPDATE vehicle_catch_challan SET tid = ?, mid = ?, serial_number = ? WHERE orderid = ? LIMIT 1";
	    jdbcTemplate.update(sqlQuery, tid, mid, serialNumber, orderid);
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
	            int affectedRows = jdbcTemplate.update(new PreparedStatementCreator() {
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
	                
	                // Fetch inserted data for response, joining `penalty_challan` and `bank_transactions`
	                String sqlQuery_joined = "SELECT"
	                		+ "    pc.`id`, "
	                		+ "    pc.`vclid`, "
	                		+ "    pc.`challan_by`,  "
	                		+ "    DATE_FORMAT(pc.`challan_date`, '%d-%m-%Y %H:%i') AS challan_date, "
	                		+ "    pc.`latitude`, "
	                		+ "    pc.`longitude`, "
	                		+ "    pc.`zone`, "
	                		+ "    pc.`ward`, "
	                		+ "    pc.`streetid`, "
	                		+ "    pc.`streetname`, "
	                		+ "    pc.`image`, "
	                		+ "    pc.`remarks`, "
	                		+ "    pc.`status`, "
	                		+ "    pc.`isactive`, "
	                		+ "    pc.`isdelete`, "
	                		+ "    pc.`violator_name`, "
	                		+ "    pc.`violator_phone`, "
	                		+ "    pc.`fine_amount`, "
	                		+ "    pc.`orderid`, "
	                		+ "    pc.`vehicle_no`, "
	                		+ "    LENGTH(pc.vclid) - LENGTH(REPLACE(pc.vclid, ',', '')) + 1 AS cattlecount, "
	                		+ "    CONCAT('https://gccservices.in/gccofficialapp/files', pc.`image`) AS imageurl, "
	                		+ "    bt.mid, "
	                		+ "    bt.tid, "
	                		+ "    bt.txnId, "
	                		+ "    bt.bankName, "
	                		+ "    bt.batchNumber, "
	                		+ "    bt.transactionAmount, "
	                		+ "    bt.transactionTitle, "
	                		+ "    bt.txnCardNo, "
	                		+ "    bt.txnDate, "
	                		+ "    bt.txnInvoice, "
	                		+ "    bt.txnMode, "
	                		+ "    bt.txnRefNo  "
	                		+ "FROM "
	                		+ "    `vehicle_catch_challan` pc "
	                		+ "LEFT JOIN "
	                		+ "    `bank_transactions` bt ON pc.orderid = bt.txnId "
	                		+ "WHERE "
	                		+ "    (pc.`isactive` = 1 AND pc.`isdelete` = 0) "
	                		+ "    AND (bt.id = ?)";
	                
	                System.out.println(sqlQuery_joined);
	                // Execute the query
	                List<Map<String, Object>> result = jdbcTemplate.queryForList(sqlQuery_joined, lastInsertId);
	                
	                response.put("status", 200);
	                response.put("message", "Transaction stored successfully!");
	                response.put("data", result);
	                
	                updatePaymentStatus(txnId, transactionResponseStatus);
	                
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
	            int affectedRows = jdbcTemplate.update(new PreparedStatementCreator() {
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
	                String sqlQuery_joined = "SELECT"
	                		+ "    pc.`id`, "
	                		+ "    pc.`vclid`, "
	                		+ "    pc.`challan_by`,  "
	                		+ "    DATE_FORMAT(pc.`challan_date`, '%d-%m-%Y %H:%i') AS challan_date, "
	                		+ "    pc.`latitude`, "
	                		+ "    pc.`longitude`, "
	                		+ "    pc.`zone`, "
	                		+ "    pc.`ward`, "
	                		+ "    pc.`streetid`, "
	                		+ "    pc.`streetname`, "
	                		+ "    pc.`image`, "
	                		+ "    pc.`remarks`, "
	                		+ "    pc.`status`, "
	                		+ "    pc.`isactive`, "
	                		+ "    pc.`isdelete`, "
	                		+ "    pc.`violator_name`, "
	                		+ "    pc.`violator_phone`, "
	                		+ "    pc.`fine_amount`, "
	                		+ "    pc.`orderid`, "
	                		+ "    pc.`vehicle_no`, "
	                		+ "    LENGTH(pc.vclid) - LENGTH(REPLACE(pc.vclid, ',', '')) + 1 AS cattlecount, "
	                		+ "    CONCAT('https://gccservices.in/gccofficialapp/files', pc.`image`) AS imageurl, "
	                		+ "    bt.mid, "
	                		+ "    bt.tid, "
	                		+ "    bt.txnId, "
	                		+ "    bt.bankName, "
	                		+ "    bt.batchNumber, "
	                		+ "    bt.transactionAmount, "
	                		+ "    bt.transactionTitle, "
	                		+ "    bt.txnCardNo, "
	                		+ "    bt.txnDate, "
	                		+ "    bt.txnInvoice, "
	                		+ "    bt.txnMode, "
	                		+ "    bt.txnRefNo  "
	                		+ "FROM "
	                		+ "    `vehicle_catch_challan` pc "
	                		+ "LEFT JOIN "
	                		+ "    `bank_failed_transactions` bt ON pc.orderid = bt.txnId "
	                		+ "WHERE "
	                		+ "    (pc.`isactive` = 1 AND pc.`isdelete` = 0) "
	                		+ "    AND (bt.id = ?)";
	                
	                System.out.println(sqlQuery_joined);
	                // Execute the query
	                List<Map<String, Object>> result = jdbcTemplate.queryForList(sqlQuery_joined, lastInsertId);
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
	    String sqlQuery = "UPDATE vehicle_catch_challan SET status = ? WHERE orderid = ? LIMIT 1";
	    jdbcTemplate.update(sqlQuery, status, orderid);
	    return "success";
	}
	
	@Transactional
	public List<Map<String, Object>> step3list(
			MultiValueMap<String, String> formData,
			String loginid) {
		
		String sqlQuery = "SELECT `id`, `cinfoids`, `challan_by`,  "
				+ "DATE_FORMAT(`challan_date`, '%d-%m-%Y %H:%i') AS challan_date, "
				+ "`latitude`, `longitude`, `zone`, `ward`, `streetid`, `streetname`, "
				+ "`image`, `remarks`, `status`, `isactive`, `isdelete`, `violator_name`, "
				+ "`violator_phone`, `fine_amount`, `orderid`, `depotid`, "
				+ "LENGTH(cinfoids) - LENGTH(REPLACE(cinfoids, ',', '')) + 1 AS cattlecount, "
				+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files',`image`) as imageurl "
				+ " From `vehicle_catch_challan` WHERE (`isactive`=1 AND `isdelete`=0) AND (`status`='Transaction success')";
		
		if(loginid!=null && !loginid.isEmpty() && !loginid.isBlank()) {
			sqlQuery= sqlQuery +" AND `challan_by`='"+loginid+"'";
		}
		
		//sqlQuery = sqlQuery + " LIMIT 1";
		
		//System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Challan Details");
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> saveClose(
			String vclid,
			String zone,
			String ward,
			String latitude,
			String longitude, 
			MultipartFile file, 
			String remarks,
			List<Map<String, String>> wastetype,
			String cby) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		// Check if already closed
		if (isAlreadyClosed(vclid)) {
		    response.put("status", "error");
		    response.put("message", "This complaint is already closed.");
		    result.add(response);
		    return result;
		}
		
		String filetxt = cby;
		
		// Call fileUpload to download and save the file locally
		// For Image
	    String filetype = "image";
	    String action = "identified_waste";
	    
	    // for Image 1
	    String file1 = fileUpload(action, filetxt, file, filetype);
	    
	    if ("error".equalsIgnoreCase(file1)) {
	        response.put("status", "error");
	        response.put("message", "Image-1 insert failed.");
	        result.add(response);
	        return result;
	    }
	    
		inactiveTheReply(vclid); // Set Old reply inactive 
		
		String sqlQuery = "INSERT INTO `waste_reply`(`vclid`, `zone`, `ward`, `latitude`, `longitude`, `file`, `remarks`,`cby`) "
				+ "VALUES (?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
			int affectedRows = jdbcTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "vclid" });
					int i = 1;
			        ps.setString(i++, vclid);
			        ps.setString(i++, zone);
			        ps.setString(i++, ward);
			        ps.setString(i++, latitude);
			        ps.setString(i++, longitude);
			        //ps.setString(i++, type);
					ps.setString(i++, file1);
					ps.setString(i++, remarks);
					//ps.setString(i++, tonage);
					ps.setString(i++, cby);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				
				// 2️⃣ Insert waste_info (MULTIPLE ROWS)
			    String insertWasteInfoSql =
			        "INSERT INTO waste_info (typeid, kg, vclid, wrid) " +
			        "VALUES (?,?,?,?)";

			    for (Map<String, String> wt : wastetype) {
			        jdbcTemplate.update(
			            insertWasteInfoSql,
			            wt.get("id"),
			            wt.get("kg"),
			            vclid,
			            lastInsertId
			        );
			    }
			    
				updateStatus(vclid, lastInsertId, "Close"); // Update the complaint status
				
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "A complaint reply added successfully!");
				//System.out.println("A new complaint reply added successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to add the complaint reply!.");
			}
		} catch (DataAccessException e) {
			System.out.println("Data Access Exception:");
			Throwable rootCause = e.getMostSpecificCause();
			if (rootCause instanceof SQLException) {
				SQLException sqlException = (SQLException) rootCause;
				
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
	
	
	// For Reports
	public List<Map<String, Object>> getZoneSummary(String fromDate, String toDate) {
		
		fromDate = convertDateFormat(fromDate,0);
    	toDate = convertDateFormat(toDate,1);
    	
    	String sql = "SELECT "
    			+ "    vclm.zone, "
    			+ "    COUNT(vclm.vclid) AS identified, "
    			+ "    SUM(CASE WHEN wr.vclid IS NULL THEN 1 ELSE 0 END) AS pending, "
    			+ "    SUM(CASE WHEN wr.vclid IS NOT NULL THEN 1 ELSE 0 END) AS `close` "
    			+ "FROM vehicle_catch_location_mapping vclm "
    			+ "LEFT JOIN waste_reply wr "
    			+ "    ON vclm.vclid = wr.vclid"
    			+ "    AND wr.isactive = 1 "
    			+ "WHERE vclm.isactive = 1 "
    			+ "  AND vclm.isdelete = 0 "
    			+ "  AND vclm.cdate >= ? "
    			+ "  AND vclm.cdate < ? "
    			+ "GROUP BY vclm.zone "
    			
    			+ " UNION ALL "
    			
    			+ "SELECT "
    			+ "    'TOTAL' AS zone, "
    			+ "    COUNT(vclm.vclid), "
    			+ "    COALESCE(SUM(CASE WHEN wr.vclid IS NULL THEN 1 ELSE 0 END), 0), " 
    	        + "    COALESCE(SUM(CASE WHEN wr.vclid IS NOT NULL THEN 1 ELSE 0 END), 0) " 
    			+ "FROM vehicle_catch_location_mapping vclm "
    			+ "LEFT JOIN waste_reply wr "
    			+ "    ON vclm.vclid = wr.vclid "
    			+ "    AND wr.isactive = 1 "
    			+ "WHERE vclm.isactive = 1 "
    			+ "  AND vclm.isdelete = 0 "
    			+ "  AND vclm.cdate >= ? "
    			+ "  AND vclm.cdate < ? ";
        
        return jdbcTemplate.queryForList(sql, fromDate, toDate, fromDate,toDate);
	}
	
	public List<Map<String, Object>> getWardSummary(String zone, String fromDate, String toDate) {
		
		fromDate = convertDateFormat(fromDate,0);
    	toDate = convertDateFormat(toDate,1);
    	
    	String sql = "SELECT  "
    			+ "    vclm.ward, "
    			+ "    COUNT(vclm.vclid) AS identified, "
    			+ "    SUM(CASE WHEN wr.vclid IS NULL THEN 1 ELSE 0 END) AS pending, "
    			+ "    SUM(CASE WHEN wr.vclid IS NOT NULL THEN 1 ELSE 0 END) AS `close` "
    			+ "FROM vehicle_catch_location_mapping vclm "
    			+ "LEFT JOIN waste_reply wr  "
    			+ "    ON vclm.vclid = wr.vclid "
    			+ "    AND wr.isactive = 1 "
    			+ "WHERE vclm.isactive = 1 "
    			+ "  AND vclm.isdelete = 0 "
    			+ "  AND vclm.zone = ? "
    			+ "  AND vclm.cdate >= ? "
    			+ "  AND vclm.cdate < ? "
    			+ "GROUP BY vclm.ward "
    			+ " "
    			+ "UNION ALL "
    			+ " "
    			+ "SELECT  "
    			+ "    'TOTAL' AS ward, "
    			+ "    COUNT(vclm.vclid), "
    	        + "    COALESCE(SUM(CASE WHEN wr.vclid IS NULL THEN 1 ELSE 0 END), 0), " 
    	        + "    COALESCE(SUM(CASE WHEN wr.vclid IS NOT NULL THEN 1 ELSE 0 END), 0) " 
    			+ "FROM vehicle_catch_location_mapping vclm "
    			+ "LEFT JOIN waste_reply wr  "
    			+ "    ON vclm.vclid = wr.vclid "
    			+ "    AND wr.isactive = 1 "
    			+ "WHERE vclm.isactive = 1 "
    			+ "  AND vclm.isdelete = 0 "
    			+ "  AND vclm.zone = ? "
    			+ "  AND vclm.cdate >= ? "
    			+ "  AND vclm.cdate < ? ";
        
        return jdbcTemplate.queryForList(sql, zone, fromDate, toDate, zone, fromDate, toDate);
	}
	
	public List<Map<String, Object>> getPendingSummary(String ward, String fromDate, String toDate) {
		
		fromDate = convertDateFormat(fromDate,0);
    	toDate = convertDateFormat(toDate,1);
    	
    	String sql = "SELECT "
    			+ "    vclm.vclid, "
    			+ "    vclm.zone, "
    			+ "    vclm.ward, "
    			+ "    DATE_FORMAT(vclm.cdate, '%d-%m-%Y %r') AS identified_date, "
    			+ "    vclm.latitude, "
    			+ "    vclm.longitude, "
    			+ "    vclm.vehicle_no, "
    			+ "    vclm.place_name, "
    			+ "    vclm.tonage, "
    			+ "    wt.name AS waste_type, "
    			+ "    vclm.remarks, "
    			+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', vclm.file) AS image "
    			+ "FROM vehicle_catch_location_mapping vclm "
    			+ "LEFT JOIN waste_reply wr "
    			+ "    ON vclm.vclid = wr.vclid "
    			+ "    AND wr.isactive = 1 "
    			+ "LEFT JOIN waste_type wt "
    			+ "    ON vclm.typeid = wt.typeid "
    			+ "WHERE vclm.isactive = 1 "
    			+ "  AND vclm.isdelete = 0 "
    			+ "  AND vclm.ward = ? "
    			+ "  AND wr.vclid IS NULL "
    			+ "  AND vclm.cdate >= ? "
    			+ "  AND vclm.cdate < ? "
    			+ "ORDER BY vclm.cdate DESC";
        
        return jdbcTemplate.queryForList(sql, ward, fromDate, toDate);
	}
	
public List<Map<String, Object>> getCloseSummary(String ward, String fromDate, String toDate) {
		
		fromDate = convertDateFormat(fromDate,0);
    	toDate = convertDateFormat(toDate,1);
    	
    	String sql =
    	        "SELECT "
    	      + "    vclm.vclid, "
    	      + "    vclm.zone, "
    	      + "    vclm.ward, "
    	      + "    DATE_FORMAT(vclm.cdate, '%d-%m-%Y %r') AS identified_date, "
    	      + "    DATE_FORMAT(wr.cdate, '%d-%m-%Y %r') AS closed_date, "
    	      + "    vclm.latitude, "
    	      + "    vclm.longitude, "
    	      + "    vclm.vehicle_no, "
    	      + "    vclm.place_name, "
    	      + "    vclm.tonage, "
    	      + "    wr.remarks AS closing_remarks, "
    	      + "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', vclm.file) AS identified_image, "
    	      + "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', wr.file) AS close_image, "
    	      + "    COALESCE( "
    	      + "        JSON_ARRAYAGG( "
    	      + "            JSON_OBJECT( "
    	      + "                'typeId', wi.typeid, "
    	      + "                'typeName', wt.name, "
    	      + "                'kg', wi.kg "
    	      + "            ) "
    	      + "        ), "
    	      + "        JSON_ARRAY() "
    	      + "    ) AS waste_types "
    	      + "FROM vehicle_catch_location_mapping vclm "
    	      + "INNER JOIN waste_reply wr "
    	      + "    ON vclm.vclid = wr.vclid "
    	      + "    AND wr.isactive = 1 "
    	      + "LEFT JOIN waste_info wi "
    	      + "    ON wi.vclid = vclm.vclid "
    	      + "    AND wi.isactive = 1 "
    	      + "    AND wi.isdelete = 0 "
    	      + "LEFT JOIN waste_type wt "
    	      + "    ON wi.typeid = wt.typeid "
    	      + "WHERE vclm.isactive = 1 "
    	      + "  AND vclm.isdelete = 0 "
    	      + "  AND vclm.ward = ? "
    	      + "  AND vclm.cdate >= ? "
    	      + "  AND vclm.cdate < ? "
    	      + "GROUP BY vclm.vclid "
    	      + "ORDER BY wr.cdate DESC";
        
    	ObjectMapper mapper = new ObjectMapper();

    	List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, ward, fromDate, toDate);

    	for (Map<String, Object> row : list) {
    	    Object wt = row.get("waste_types");

    	    if (wt != null && wt instanceof String) {
    	        try {
    	            List<Map<String, Object>> wasteTypes =
    	                    mapper.readValue(wt.toString(), new TypeReference<List<Map<String, Object>>>() {});
    	            row.put("waste_types", wasteTypes);
    	        } catch (JsonProcessingException e) {
    	            row.put("waste_types", Collections.emptyList()); // fallback
    	        }
    	    }
    	}

    	return list;
	}

}
