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
public class CattleCatchService {
	private JdbcTemplate jdbcPenaltyTemplate;
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
	@Autowired
	public void setDataSource(@Qualifier("mysqlCattleCatchDataSource") DataSource penaltyDataSource) {
		this.jdbcPenaltyTemplate = new JdbcTemplate(penaltyDataSource);
	}
	
	@Autowired
	public CattleCatchService(Environment environment) {
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
        String serviceFolderName = environment.getProperty("cattlecatch_foldername");
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
	
	@Transactional
	public List<Map<String, Object>> step1(
			MultiValueMap<String, String> formData,
			String category_id,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String streeId,
			String streetName,
			String loginId,
			String remarks,
			//String isviolatorAvailable,
			//String violatorName,
			//String violatorPhone,
			//String ton,
			MultipartFile file) {
		
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		String image = fileUpload("cc_step1_catch",category_id,"catch",file);
		
		String sqlQuery = "INSERT INTO `cc_step1`(`category_id`, `image`, `latitude`, `longitude`, "
				+ "`zone`, `ward`, `streetid`, `streetname`, `catch_by`,`remarks`) "
				//+ "`violator_info`, `violator_name`, `violator_phone`, `ton`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?)";
		
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
                    ps.setString(7, streeId);
                    ps.setString(8, streetName);
                    ps.setString(9, loginId);
                    ps.setString(10, remarks);
                    //ps.setString(11, isviolatorAvailable);
                    //ps.setString(12, violatorName);
                    //ps.setString(13, violatorPhone);
                    //ps.setString(14, ton);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "Cattle caught successfully!");
                System.out.println("Cattle caught successfully! Insert ID: " + generatedId);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to insert a new caught (Info).");
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
	
	@Transactional
	public List<Map<String, Object>> step1list(
			MultiValueMap<String, String> formData,
			String loginid) {
		
		String sqlQuery = "SELECT *, "
				+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files',`image`) as imageurl "
				+ " From `cc_step1` WHERE (`isactive`=1 AND `isdelete`=0 AND `checkin`=0)";
		
		if(loginid!=null && !loginid.isEmpty() && !loginid.isBlank()) {
			sqlQuery= sqlQuery +" AND `catch_by`='"+loginid+"'";
		}
		
		//sqlQuery = sqlQuery + " LIMIT 1";
		
		//System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "List for ChekIn");
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> getdepotlist(String latitude, String longitude) {
		String sqlQuery = "";

		sqlQuery = "SELECT * " + "FROM depot_list " + "WHERE (isactive=1 AND isdelete=0) AND "
				+ " ((6371008.8 * acos(  ROUND(cos(radians(?)) * cos(radians(latitude)) * cos(radians(longitude) - radians(?)) +   sin(radians(?)) * sin(radians(latitude)), 9)   )) < 500)";
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery,
				new Object[] { latitude, longitude, latitude });
		Map<String, Object> response = new HashMap<>();

		response.put("status", 200);
		response.put("message", "Request Information");
		response.put("Data", result);

		return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> step2(
			MultiValueMap<String, String> formData,
			String cinfoid,
			String depotid,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String streeId,
			String streetName,
			String loginId,
			String remarks,
			MultipartFile file) {
		
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		String image = fileUpload("cc_step2_in",cinfoid,"checkin",file);
		
		String sqlQuery = "INSERT INTO `cc_step2`(`cinfoid`,`image`, `latitude`, `longitude`, "
				+ "`zone`, `ward`, `streetid`, `streetname`, `in_by`,`remarks`,`depotid`) "
				//+ "`violator_info`, `violator_name`, `violator_phone`, `ton`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
            int affectedRows = jdbcPenaltyTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, cinfoid);
                    ps.setString(2, image);
                    ps.setString(3, latitude);
                    ps.setString(4, longitude);
                    ps.setString(5, zone);
                    ps.setString(6, ward);
                    ps.setString(7, streeId);
                    ps.setString(8, streetName);
                    ps.setString(9, loginId);
                    ps.setString(10, remarks);
                    ps.setString(11, depotid);
                    //ps.setString(11, isviolatorAvailable);
                    //ps.setString(12, violatorName);
                    //ps.setString(13, violatorPhone);
                    //ps.setString(14, ton);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "Cattle successfully checked into the depot!");
                System.out.println("Cattle successfully checked into the depot! Insert ID: " + generatedId);
                updateStep1(cinfoid);
                
            } else {
                response.put("status", "error");
                response.put("message", "Failed to checked into the depot");
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
	
	public String updateStep1(String cinfoid) {
	    String sqlQuery = "UPDATE `cc_step1` SET checkin = 1 WHERE id = ? LIMIT 1";
	    //System.out.println(sqlQuery);
	    jdbcPenaltyTemplate.update(sqlQuery, cinfoid);
	    return "success";
	}
	
	@Transactional
	public List<Map<String, Object>> step2list(
			MultiValueMap<String, String> formData,
			String depotid) {
		
		String sqlQuery = "SELECT s2.`id`, s2.`cinfoid`, s2.`depotid`, s2.`in_by`, "
				+ "DATE_FORMAT(s2.`in_date`, '%d-%m-%Y %H:%i') AS in_date, "
				+ "s2.`remarks` as in_remarks, s1.catch_date, s1.catch_by, "
				+ "s1.latitude, s1.longitude, s1.zone, s1.ward, s1.streetid, s1.streetname, s1.remarks as catch_remarks,"
				+ "DATEDIFF(CURDATE(), in_date) AS days_since_in_date, "
				+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files',s2.`image`) as in_imageurl, "
				+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files',s1.`image`) as catch_imageurl "
				+ "From `cc_step1` s1,`cc_step2` s2 WHERE (s1.`isactive`=1 AND s1.`isdelete`=0) AND (s1.id=s2.cinfoid) "
				+ "AND s2.`status`='in'";
		
		if(depotid!=null && !depotid.isEmpty() && !depotid.isBlank()) {
			sqlQuery= sqlQuery +" AND depotid=?";
		}
		
		System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery,depotid);
		/*
		//sqlQuery = sqlQuery + " LIMIT 1";
		
		System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery,depotid);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "CheckIn List");
        response.put("data", result);
		
        return result;
        */
        // Initialize response list
	    List<Map<String, Object>> detailedResult = new ArrayList<>();

	    // Iterate through the depots and get additional details
	    for (Map<String, Object> cattle : result) {
	        // Extract days from the current cattle entry
	    	Object noOfDayObject = cattle.get("days_since_in_date");
	        if (noOfDayObject == null) {
	            continue; // Skip if of days is missing
	        }
	        String noofdays = noOfDayObject.toString(); // Assuming noOfDayObject is your object containing the days
	        int days = Integer.parseInt(noofdays);      // Convert String to integer

	        
	        int baseAmount = 10000;
	        int extraChargePerDay = 1000;
	        int extraDays = 0;
	        int finalAmount;

	        if (days <= 2) {
	            finalAmount = baseAmount; // No extra charge
	        } else {
	            extraDays = days - 2; // Calculate extra days
	            finalAmount = baseAmount + (extraDays * extraChargePerDay); // Add extra charges
	        }

	        System.out.println("Final Amount: " + finalAmount);
	        
	        // Add counts to the current depot details
	        Map<String, Object> detailedDepot = new HashMap<>(cattle);
	        detailedDepot.put("fineAmount", finalAmount);

	        // Add detailed depot to the response list
	        detailedResult.add(detailedDepot);
	    }

	    // Prepare response
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", 200);
	    response.put("message", "CheckIn List");
	    response.put("data", detailedResult);

	    return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> step3(
			MultiValueMap<String, String> formData,
			String cinfoids,
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
			String depotid,
			MultipartFile file) {
		
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		String image = ""; 
		/*fileUpload("cc_step2",cinfoid,file);*/
		
		String orderid = generateRandomString();
		
		String sqlQuery = "INSERT INTO `cc_step3`(`cinfoids`, `image`, `latitude`, `longitude`, "
				+ "`zone`, `ward`, `streetid`, `streetname`, `challan_by`,`remarks`, "
				+ " `violator_name`, `violator_phone`,`fine_amount`,`orderid`,`depotid`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
            int affectedRows = jdbcPenaltyTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, String.join(",", cinfoids));
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
                    ps.setString(15, depotid);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "A new Penalty (Chellan) was generated successfully!");
                System.out.println("A new Penalty (Chellan) was generated successfully! Insert ID: " + generatedId);
                
                updateStep2(String.join(",", cinfoids),"challan");
                
            } else {
                response.put("status", "error");
                response.put("message", "Failed to insert a new Penalty (Chellan).");
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

	public String updateStep2(String cinfoid, String status) {
	    String sqlQuery = "UPDATE cc_step2 SET status = ? WHERE cinfoid IN ("+cinfoid+")";
	    //System.out.println(sqlQuery);
	    jdbcPenaltyTemplate.update(sqlQuery, status);
	    return "success";
	}
	
	@Transactional
	public List<Map<String, Object>> getChallanPOSData(
			MultiValueMap<String, String> formData,
			String orderid,
			String loginid) {
		
		String sqlQuery = "SELECT `cinfoids`,"
				//+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files',`image`) as imageurl, "
						+ "`latitude`, `longitude`, `zone`, `ward`, "
        		+ "`challan_by`, `violator_name`, `violator_phone`, "
        		+ "`fine_amount`,`orderid`,`status` From `cc_step3` WHERE (`isactive`=1 AND `isdelete`=0) AND (`status` <> 'Transaction success' AND `status` <> 'out') ";
		
		if(orderid!=null && !orderid.isEmpty() && !orderid.isBlank()) {
			sqlQuery= sqlQuery +" AND `orderid`='"+orderid+"'";
		}
		if(loginid!=null && !loginid.isEmpty() && !loginid.isBlank()) {
			sqlQuery= sqlQuery +" AND `challan_by`='"+loginid+"'";
		}
		
		//sqlQuery = sqlQuery + " LIMIT 1";
		
		//System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
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
	    String sqlQuery = "UPDATE cc_step3 SET tid = ?, mid = ?, serial_number = ? WHERE orderid = ? LIMIT 1";
	    jdbcPenaltyTemplate.update(sqlQuery, tid, mid, serialNumber, orderid);
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
	                
	                // Fetch inserted data for response, joining `penalty_challan` and `bank_transactions`
	                /*
	                String sqlQuery_joined = "SELECT pc.category_id, CONCAT('" + fileBaseUrl + "/gccofficialapp/files', pc.image) as imageurl, "
	                        + "pc.latitude, pc.longitude, pc.zone, pc.ward, pc.challan_by, "
	                        + "pc.violator_name, pc.violator_phone, pc.fine_amount, pc.orderid, pc.status, "
	                        + "bt.mid, bt.tid, bt.txnId, bt.bankName, bt.batchNumber, bt.transactionAmount, bt.transactionTitle, "
	                        + "bt.txnCardNo, bt.txnDate, bt.txnInvoice, bt.txnMode, bt.txnRefNo "
	                        + "FROM cc_step3 pc "
	                        + "LEFT JOIN bank_transactions bt ON pc.orderid = bt.txnId "
	                        + "WHERE pc.isactive = 1 AND pc.isdelete = 0 AND bt.id = ? LIMIT 1";
	                */
	                
	                String sqlQuery_joined = "SELECT"
	                		+ "    pc.`id`, "
	                		+ "    pc.`cinfoids`, "
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
	                		+ "    pc.`depotid`, "
	                		+ "    LENGTH(pc.cinfoids) - LENGTH(REPLACE(pc.cinfoids, ',', '')) + 1 AS cattlecount, "
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
	                		+ "    `cc_step3` pc "
	                		+ "LEFT JOIN "
	                		+ "    `bank_transactions` bt ON pc.orderid = bt.txnId "
	                		+ "WHERE "
	                		+ "    (pc.`isactive` = 1 AND pc.`isdelete` = 0) "
	                		+ "    AND (bt.id = ?)";
	                
	                System.out.println(sqlQuery_joined);
	                // Execute the query
	                List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery_joined, lastInsertId);
	                
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
	                /*
	                String sqlQuery_joined = "SELECT pc.category_id, CONCAT('" + fileBaseUrl + "/gccofficialapp/files', pc.image) as imageurl, "
	                        + "pc.latitude, pc.longitude, pc.zone, pc.ward, pc.challan_by, "
	                        + "pc.violator_name, pc.violator_phone, pc.fine_amount, pc.orderid, pc.status, "
	                        + "bt.mid, bt.tid, bt.txnId, bt.bankName, bt.batchNumber, bt.transactionAmount, bt.transactionTitle, "
	                        + "bt.txnCardNo, bt.txnDate, bt.txnInvoice, bt.txnMode, bt.txnRefNo "
	                        + "FROM cc_step3 pc "
	                        + "LEFT JOIN bank_transactions bt ON pc.orderid = bt.txnId "
	                        + "WHERE pc.isactive = 1 AND pc.isdelete = 0 AND bt.id = ? LIMIT 1";
	                */
	                
	                String sqlQuery_joined = "SELECT"
	                		+ "    pc.`id`, "
	                		+ "    pc.`cinfoids`, "
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
	                		+ "    pc.`depotid`, "
	                		+ "    LENGTH(pc.cinfoids) - LENGTH(REPLACE(pc.cinfoids, ',', '')) + 1 AS cattlecount, "
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
	                List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery_joined, lastInsertId);
	                response.put("status", 200);
	                response.put("message", "Failed Transaction stored successfully!");
	                response.put("data", result);
	            } else {
	                response.put("status", 201);
	                response.put("message", "Failed to store the failedtransaction.");
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
	    String sqlQuery = "UPDATE cc_step3 SET status = ? WHERE orderid = ? LIMIT 1";
	    jdbcPenaltyTemplate.update(sqlQuery, status, orderid);
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
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Challan Details");
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> step4(
			MultiValueMap<String, String> formData,
			String cinfoid,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String streeId,
			String streetName,
			String loginId,
			String remarks,
			MultipartFile file) {
		
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		String image = fileUpload("cc_step4_out",cinfoid,"out",file);
		
		String sqlQuery = "INSERT INTO `cc_step4`(`cinfoid`,`image`, `latitude`, `longitude`, "
				+ "`zone`, `ward`, `streetid`, `streetname`, `out_by`,`remarks`) "
				//+ "`violator_info`, `violator_name`, `violator_phone`, `ton`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
            int affectedRows = jdbcPenaltyTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, cinfoid);
                    ps.setString(2, image);
                    ps.setString(3, latitude);
                    ps.setString(4, longitude);
                    ps.setString(5, zone);
                    ps.setString(6, ward);
                    ps.setString(7, streeId);
                    ps.setString(8, streetName);
                    ps.setString(9, loginId);
                    ps.setString(10, remarks);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
            	updateOutStatus(cinfoid,"out");
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "Cattle successfully checked out from the depot!");
                System.out.println("Cattle successfully checked out from the depot! Insert ID: " + generatedId);
                
            } else {
                response.put("status", "error");
                response.put("message", "Failed to checked out from the depot");
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
	
	public String updateOutStatus(String id, String status) {
	    String sqlQuery = "UPDATE cc_step3 SET status = ? WHERE id = ? LIMIT 1";
	    jdbcPenaltyTemplate.update(sqlQuery, status, id);
	    return "success";
	}
	/*
	@Transactional
	public List<Map<String, Object>> DepotReport(
			MultiValueMap<String, String> formData,
			String fromDate,
			String toDate,
			String loginid) {
		
		String sqlQuery = "SELECT * FROM `depot_list` where `isactive`=1 AND `isdelete`=0";
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
		//System.out.println(sqlQuery);
		
		String depotid="1";
		
		String sqlQuery_total = "SELECT count(id) as Total FROM `depot_list` where (`isactive`=1 AND `isdelete`=0) AND (`depotid`=?)";
		List<Map<String, Object>> resultTotal = jdbcPenaltyTemplate.queryForList(sqlQuery_total,depotid);
		
		String sqlQuery_paid = "SELECT count(s3.id) as TotalPaid FROM `cc_step3` s3,"
				+ "where (`isactive`=1 AND `isdelete`=0) AND s3.`depotid`=?";
		List<Map<String, Object>> resultPaid = jdbcPenaltyTemplate.queryForList(sqlQuery_total,depotid);
		
		String sqlQuery_out = "SELECT count(s4.id) as TotalPaid FROM `cc_step4` s4,`cc_step3` s3,"
				+ "where (`isactive`=1 AND `isdelete`=0) AND s3.`id`= s4.cinfoid AND s3.`depotid`=?";
		List<Map<String, Object>> resultOut = jdbcPenaltyTemplate.queryForList(sqlQuery_total,depotid);
		
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Zone List Details");
        response.put("data", result);
		
        return result;
    }
	*/
	@Transactional
	public List<Map<String, Object>> depotReport(
	        MultiValueMap<String, String> formData,
	        String fromDate,
	        String toDate,
	        String loginid) {

	    // Fetch depot list
	    String sqlQuery = "SELECT * FROM `depot_list` WHERE (`isactive`=1 AND `isdelete`=0)";
	    List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);

	    // Initialize response list
	    List<Map<String, Object>> detailedResult = new ArrayList<>();

	    // Iterate through the depots and get additional details
	    for (Map<String, Object> depot : result) {
	        // Extract depotid from the current depot entry
	    	Object depotIdObject = depot.get("id");
	        if (depotIdObject == null) {
	            continue; // Skip if depotid is missing
	        }
	        String depotid = depotIdObject.toString();
	        System.out.println("depotid : "+depotid);
	        // Fetch total count for this depot
	        String sqlQuery_total = "SELECT count(id) as Total FROM `cc_step2` " +
	                "WHERE (`isactive`=1 AND `isdelete`=0) "
	                + "AND (`depotid`=? AND DATE(in_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y'))";
	        List<Map<String, Object>> resultTotal = jdbcPenaltyTemplate.queryForList(sqlQuery_total, depotid,fromDate,toDate);
	        /*
	        // Fetch total paid count for this depot
	        String sqlQuery_paid = "SELECT count(s3.id) as TotalPaid FROM `cc_step3` s3 " +
	                "WHERE (`isactive`=1 AND `isdelete`=0) AND (s3.`depotid`=? AND s3.`status`<>'unpaid')";
	        List<Map<String, Object>> resultPaid = jdbcPenaltyTemplate.queryForList(sqlQuery_paid, depotid);
*/
	        // Fetch total out count for this depot
	        String sqlQuery_out = "SELECT "
	                + "IFNULL(SUM(DISTINCT temp.cinfoid_count), 0) AS TotalOut "
	                + "FROM ( "
	                + "    SELECT "
	                + "        s2.cinfoid AS individual_cinfoid, "
	                + "        COUNT(*) AS cinfoid_count "
	                + "    FROM `cc_step2` s2 "
	                + "    JOIN `cc_step3` s3 ON FIND_IN_SET(s2.cinfoid, s3.cinfoids) "
	                + "    JOIN `cc_step4` s4 ON s3.`id` = s4.`cinfoid` "
	                + "    WHERE (s2.`isactive` = 1 AND s2.`isdelete` = 0) "
	                + "      AND (s2.`depotid` = ?) "
	                + "      AND (s4.`isactive` = 1 AND s4.`isdelete` = 0) "
	                + "      AND DATE(s4.out_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
	                + "     "
	                + ") temp";
	        List<Map<String, Object>> resultOut = jdbcPenaltyTemplate.queryForList(sqlQuery_out, depotid,fromDate,toDate);
	        /*
	        // Fetch total paid count for this depot
	        String sqlQuery_pending = "SELECT count(s2.id) as TotalPending FROM `cc_step2` s2 " +
	                "WHERE (s2.`isactive`=1 AND s2.`isdelete`=0) AND (s2.`depotid`=? AND s2.`status`='in')";
	        List<Map<String, Object>> resultPending = jdbcPenaltyTemplate.queryForList(sqlQuery_pending, depotid);
	        */
	        // Add counts to the current depot details
	        Map<String, Object> detailedDepot = new HashMap<>(depot);
	        detailedDepot.put("Total", resultTotal.isEmpty() ? 0 : resultTotal.get(0).get("Total"));
	        //detailedDepot.put("TotalPaid", resultPaid.isEmpty() ? 0 : resultPaid.get(0).get("TotalPaid"));
	        detailedDepot.put("TotalOut", resultOut.isEmpty() ? 0 : resultOut.get(0).get("TotalOut"));
	        //detailedDepot.put("TotalPending", resultPending.isEmpty() ? 0 : resultPending.get(0).get("TotalPending"));

	        // Add detailed depot to the response list
	        detailedResult.add(detailedDepot);
	    }

	    // Prepare response
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", 200);
	    response.put("message", "Depot List Details");
	    response.put("data", detailedResult);

	    return Collections.singletonList(response);
	}
	/*
	@Transactional
	public List<Map<String, Object>> zoneReport(
	        MultiValueMap<String, String> formData,
	        String fromDate,
	        String toDate,
	        String depotid,
	        String loginid) {

	    // Fetch depot list
	    String sqlQuery = "SELECT s1.`zone`, dl.name FROM `cc_step1` s1,`cc_step2` s2,`depot_list` dl "
	    		+ "WHERE (s2.`isactive`=1 AND s2.`isdelete`=0) "
	    		+ "AND (s1.id=s2.cinfoid)"
	    		+ "AND (s2.`depotid`=? and dl.id=s2.`depotid`) "
	    		+ "AND DATE(s2.in_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
	    		+ "GROUP BY s1.`zone`, dl.name";
	    List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery,depotid, fromDate, toDate);

	    // Initialize response list
	    List<Map<String, Object>> detailedResult = new ArrayList<>();

	    // Iterate through the depots and get additional details
	    for (Map<String, Object> depot : result) {
	        // Extract depotid from the current depot entry
	    	Object zoneObject = depot.get("zone");
	        if (zoneObject == null) {
	            continue; // Skip if depotid is missing
	        }
	        String zone = zoneObject.toString();
	        System.out.println("zone : "+zone);
	        System.out.println("depotid : "+depotid);
	        
	        // Fetch total count for this depot
	        String sqlQuery_total = "SELECT count(s2.id) as Total FROM `cc_step1` s1,`cc_step2` s2 "
	                + "WHERE (s1.`isactive`=1 AND s1.`isdelete`=0) AND (s2.`depotid`=?)"
	                + "AND (s1.id=s2.cinfoid AND s1.zone=?) "
	                + "AND DATE(s2.in_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y')";
	        List<Map<String, Object>> resultTotal = jdbcPenaltyTemplate.queryForList(sqlQuery_total, depotid, zone, fromDate, toDate);

	        // Fetch total out count for this depot
	        String sqlQuery_out = "SELECT IFNULL(SUM(unique_cattle_count), 0) AS cattlecount "
	        		+ "FROM ( SELECT COUNT(DISTINCT s1.id) AS unique_cattle_count "
	        		+ "FROM cc_step1 s1 JOIN cc_step3 s3 ON FIND_IN_SET(s1.id, s3.cinfoids) "
	        		+ "JOIN cc_step4 s4 ON s3.id = s4.cinfoid "
	        		+ "WHERE s1.isactive = 1 AND s1.isdelete = 0 AND s3.depotid = ? "
	        		+ "AND s1.zone = ? AND DATE(s4.out_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') "
	        		+ "AND STR_TO_DATE(?, '%d-%m-%Y') ) AS deduplicated_counts";
	        List<Map<String, Object>> resultOut = jdbcPenaltyTemplate.queryForList(sqlQuery_out, depotid,zone,fromDate,toDate);

	        System.out.println(sqlQuery_out);
	      	        
	        // Add counts to the current depot details
	        Map<String, Object> detailedDepot = new HashMap<>(depot);
	        detailedDepot.put("Total", resultTotal.isEmpty() ? 0 : resultTotal.get(0).get("Total"));
	        //detailedDepot.put("TotalPaid", resultPaid.isEmpty() ? 0 : resultPaid.get(0).get("TotalPaid"));
	        detailedDepot.put("TotalOut", resultOut.isEmpty() ? 0 : resultOut.get(0).get("cattlecount"));
	        //detailedDepot.put("TotalPending", resultPending.isEmpty() ? 0 : resultPending.get(0).get("TotalPending"));

	        // Add detailed depot to the response list
	        detailedResult.add(detailedDepot);
	    }

	    // Prepare response
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", 200);
	    response.put("message", "Zone List Details");
	    response.put("data", detailedResult);

	    return Collections.singletonList(response);
	}
	*/
	
	@Transactional
	public List<Map<String, Object>> zoneReport(
	        MultiValueMap<String, String> formData,
	        String fromDate,
	        String toDate,
	        String depotid,
	        String loginid) {

	    String sqlQuery = "SELECT zone, SUM(TotalIn) AS Total, SUM(TotalOut) AS TotalOut FROM ( " +
	        "SELECT s1.zone, COUNT(DISTINCT s2.id) AS TotalIn, 0 AS TotalOut " +
	        "FROM cc_step1 s1 " +
	        "JOIN cc_step2 s2 ON s1.id = s2.cinfoid " +
	        "WHERE s1.isactive = 1 AND s1.isdelete = 0 AND s2.isactive = 1 AND s2.isdelete = 0 " +
	        "AND s2.depotid = ? " +
	        "AND DATE(s2.in_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') " +
	        "GROUP BY s1.zone " +

	        "UNION ALL " +

	        "SELECT s1.zone, 0 AS TotalIn, COUNT(DISTINCT s1.id) AS TotalOut " +
	        "FROM cc_step1 s1 " +
	        "JOIN cc_step3 s3 ON FIND_IN_SET(s1.id, s3.cinfoids) " +
	        "JOIN cc_step4 s4 ON s3.id = s4.cinfoid " +
	        "WHERE s1.isactive = 1 AND s1.isdelete = 0 AND s3.depotid = ? " +
	        "AND DATE(s4.out_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') " +
	        "GROUP BY s1.zone " +
	    ") AS combined " +
	    "GROUP BY zone " +
	    "HAVING Total > 0 OR TotalOut > 0";

	    List<Map<String, Object>> detailedResult = jdbcPenaltyTemplate.queryForList(
	        sqlQuery,
	        depotid, fromDate, toDate,   // For IN part
	        depotid, fromDate, toDate    // For OUT part
	    );

	    // Prepare response
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", 200);
	    response.put("message", "Zone List Details");
	    response.put("data", detailedResult);

	    return Collections.singletonList(response);
	}
	
	
	@Transactional
	public List<Map<String, Object>> wardReport(
			 MultiValueMap<String, String> formData,
		        String fromDate,
		        String toDate,
		        String zone,
		        String depotid,
		        String loginid) {

	    String sqlQuery = "SELECT zone, ward, SUM(TotalIn) AS Total, SUM(TotalOut) AS TotalOut FROM ( " +
	        "SELECT s1.zone, s1.ward, COUNT(DISTINCT s2.id) AS TotalIn, 0 AS TotalOut " +
	        "FROM cc_step1 s1 " +
	        "JOIN cc_step2 s2 ON s1.id = s2.cinfoid " +
	        "WHERE s1.zone=? AND s1.isactive = 1 AND s1.isdelete = 0 AND s2.isactive = 1 AND s2.isdelete = 0 " +
	        "AND s2.depotid = ? " +
	        "AND DATE(s2.in_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') " +
	        "GROUP BY s1.zone, s1.ward " +

	        "UNION ALL " +

	        "SELECT s1.zone, s1.ward, 0 AS TotalIn, COUNT(DISTINCT s1.id) AS TotalOut " +
	        "FROM cc_step1 s1 " +
	        "JOIN cc_step3 s3 ON FIND_IN_SET(s1.id, s3.cinfoids) " +
	        "JOIN cc_step4 s4 ON s3.id = s4.cinfoid " +
	        "WHERE s1.zone=? AND s1.isactive = 1 AND s1.isdelete = 0 AND s3.depotid = ? " +
	        "AND DATE(s4.out_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') " +
	        "GROUP BY s1.zone, s1.ward " +
	    ") AS combined " +
	    "GROUP BY zone, ward " +
	    "HAVING Total > 0 OR TotalOut > 0";

	    List<Map<String, Object>> detailedResult = jdbcPenaltyTemplate.queryForList(
	        sqlQuery, 
	        zone, depotid, fromDate, toDate,    // For IN
	        zone, depotid, fromDate, toDate     // For OUT
	    );

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", 200);
	    response.put("message", "Zone-Ward Wise Report");
	    response.put("data", detailedResult);

	    return Collections.singletonList(response);
	}
	/*
	@Transactional
	public List<Map<String, Object>> wardReport(
	        MultiValueMap<String, String> formData,
	        String fromDate,
	        String toDate,
	        String zone,
	        String depotid,
	        String loginid) {

	    // Fetch depot list
	    String sqlQuery = "SELECT s1.`zone`,s1.`ward`,dl.name FROM `cc_step1` s1,`cc_step2` s2,`depot_list` dl "
	    		+ "WHERE (s2.`isactive`=1 AND s2.`isdelete`=0) "
	    		+ "AND (s1.id=s2.cinfoid AND s1.zone=?)"
	    		+ "AND (s2.`depotid`=? and dl.id=s2.`depotid`) "
	    		+ "AND DATE(s2.in_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
	    		+ "GROUP BY s1.`zone`, s1.`ward`, dl.name";
	   
	    List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery, zone, depotid, fromDate, toDate);

	    // Initialize response list
	    List<Map<String, Object>> detailedResult = new ArrayList<>();
	    System.out.println("zone : "+zone);
        System.out.println("depotid : "+depotid);
        System.out.println("fromDate : "+fromDate);
        System.out.println("toDate : "+toDate);
	    // Iterate through the depots and get additional details
	    for (Map<String, Object> depot : result) {
	        // Extract depotid from the current depot entry
	    	Object wardObject = depot.get("ward");
	        if (wardObject == null) {
	            continue; // Skip if depotid is missing
	        }
	        String ward = wardObject.toString();
	        System.out.println("zone : "+zone);
	        System.out.println("depotid : "+depotid);
	        
	        // Fetch total count for this depot
	        String sqlQuery_total = "SELECT count(s2.id) as Total FROM `cc_step1` s1,`cc_step2` s2 "
	                + "WHERE (s1.`isactive`=1 AND s1.`isdelete`=0) AND (s2.`depotid`=?)"
	                + "AND (s1.id=s2.cinfoid AND s1.zone=? AND s1.ward=?) "
	                + "AND DATE(s2.in_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y')";
	        List<Map<String, Object>> resultTotal = jdbcPenaltyTemplate.queryForList(sqlQuery_total, depotid, zone, ward,fromDate, toDate);

	        // Fetch total out count for this depot
	        String sqlQuery_out = "SELECT IFNULL(SUM(unique_cattle_count), 0) AS cattlecount "
	        		+ "FROM ( SELECT COUNT(DISTINCT s1.id) AS unique_cattle_count "
	        		+ "FROM cc_step1 s1 JOIN cc_step3 s3 ON FIND_IN_SET(s1.id, s3.cinfoids) "
	        		+ "JOIN cc_step4 s4 ON s3.id = s4.cinfoid "
	        		+ "WHERE s1.isactive = 1 AND s1.isdelete = 0 AND s3.depotid = ? "
	        		+ "AND s1.zone = ?  AND s1.ward=? "
	        		+ "AND DATE(s4.out_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') "
	        		+ "AND STR_TO_DATE(?, '%d-%m-%Y') ) AS deduplicated_counts";
	        List<Map<String, Object>> resultOut = jdbcPenaltyTemplate.queryForList(sqlQuery_out, depotid,zone,ward,fromDate,toDate);

	        System.out.println(sqlQuery_out);
	                
	        // Add counts to the current depot details
	        Map<String, Object> detailedDepot = new HashMap<>(depot);
	        detailedDepot.put("Total", resultTotal.isEmpty() ? 0 : resultTotal.get(0).get("Total"));
	        //detailedDepot.put("TotalPaid", resultPaid.isEmpty() ? 0 : resultPaid.get(0).get("TotalPaid"));
	        detailedDepot.put("TotalOut", resultOut.isEmpty() ? 0 : resultOut.get(0).get("cattlecount"));
	        //detailedDepot.put("TotalPending", resultPending.isEmpty() ? 0 : resultPending.get(0).get("TotalPending"));

	        // Add detailed depot to the response list
	        detailedResult.add(detailedDepot);
	    }

	    // Prepare response
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", 200);
	    response.put("message", "Ward List Details");
	    response.put("data", detailedResult);

	    return Collections.singletonList(response);
	}
	*/
	@Transactional
	public List<Map<String, Object>> inListReport(
			MultiValueMap<String, String> formData,
			String fromDate,
			String toDate,
			String zone,
			String ward,
			String depotid,
			String loginid) {
		
		String sqlQuery = "SELECT s1.`id`, s1.`category_id`,  s1.`catch_by`,  DATE_FORMAT(s1.`catch_date`, '%d-%m-%Y %H:%i') AS `catch_date`,  s1.`latitude`, "
				+ " s1.`longitude`,  s1.`zone`,  s1.`ward`,  s1.`streetid`,  s1.`streetname`,  s1.`image`,  s1.`remarks`, "
				+ " DATE_FORMAT(s2.in_date, '%d-%m-%Y %H:%i') AS `in_date` , s2.in_by, "
				+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files',s1.`image`) as imageurl "
				+ " From `cc_step1` s1, `cc_step2` s2 WHERE (s1.`isactive`=1 AND s1.`isdelete`=0) "
				+ "AND (s1.id=s2.cinfoid AND s1.zone=? AND s1.ward=? AND s2.depotid=?) "
				+ "AND DATE(s2.in_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y')";
		
		//System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery,zone,ward,depotid,fromDate,toDate);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Check IN List Details");
        response.put("data", result);
		
        return result;
    }
	
	@Transactional
	public List<Map<String, Object>> outListReport(
			MultiValueMap<String, String> formData,
			String fromDate,
			String toDate,
			String zone,
			String ward,
			String depotid,
			String loginid) {
		
		 String sqlQuery_out = "SELECT DISTINCT "
		 		+ "    s1.`id`, "
		 		+ "    s1.`category_id`, "
		 		+ "    s1.`catch_by`, "
		 		+ "    DATE_FORMAT(s1.`catch_date`, '%d-%m-%Y %H:%i') AS catch_date, "
		 		+ "    s1.`latitude`, "
		 		+ "    s1.`longitude`, "
		 		+ "    s1.`zone`, "
		 		+ "    s1.`ward`, "
		 		+ "    s1.`streetid`, "
		 		+ "    s1.`streetname`, "
		 		+ "    s1.`remarks`,  "
		 		+ "    DATE_FORMAT(s2.`in_date`, '%d-%m-%Y %H:%i') AS in_date, "
		 		+ "    s2.`in_by`, "
		 		+ "    DATE_FORMAT(s4.`out_date`, '%d-%m-%Y %H:%i') AS `out_date`, "
		 		+ "    s4.`out_by`, "
		 		+ "    DATEDIFF(s4.`out_date`, s2.`in_date`) AS days_since_in_date, "
		 		+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', s1.`image`) AS inImageurl, "
		 		+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', s4.`image`) AS outImageurl "
		 		+ "FROM "
		 		+ "    cc_step1 s1 "
		 		+ "JOIN "
		 		+ "    cc_step3 s3 ON FIND_IN_SET(s1.id, s3.cinfoids) "
		 		+ "JOIN "
		 		+ "    cc_step4 s4 ON s3.id = s4.cinfoid "
		 		+ "JOIN "
		 		+ "    cc_step2 s2 ON s1.id = s2.cinfoid "
		 		+ "WHERE "
		 		+ "    s1.isactive = 1 "
		 		+ "    AND s1.isdelete = 0 "
		 		+ "    AND s1.zone = ?  "
		 		+ "    AND s1.ward = ? "
		 		+ "    AND s3.depotid = ? "
		 		+ "    AND s4.isactive = 1 "
		 		+ "    AND s4.isdelete = 0 "
		 		+ "    AND DATE(s4.out_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') "
		 		+ "                             AND STR_TO_DATE(?, '%d-%m-%Y')";
		
		//System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery_out,zone,ward,depotid,fromDate,toDate);
		Map<String, Object> response = new HashMap<>();

		List<Map<String, Object>> detailedResult = new ArrayList<>();

	    for (Map<String, Object> cattle : result) {
	        Object noOfDayObject = cattle.get("days_since_in_date");

	        int days = (noOfDayObject != null) ? Integer.parseInt(noOfDayObject.toString()) : 0;

	        int baseAmount = 10000;
	        int extraChargePerDay = 1000;
	        int fineAmount = days <= 2 ? baseAmount : baseAmount + ((days - 2) * extraChargePerDay);

	        // Add fine amount to the current record
	        Map<String, Object> detailedCattle = new HashMap<>(cattle);
	        detailedCattle.put("fineAmount", fineAmount);

	        detailedResult.add(detailedCattle);
	    }
	    
        response.put("status", 200);
        response.put("message", "Check Out List Details");
        response.put("data", detailedResult);
		
        return Collections.singletonList(response);
    }
	/*
	@Transactional
	public List<Map<String, Object>> zoneReport(
			MultiValueMap<String, String> formData,
			String fromDate,
			String toDate,
			String loginid) {
		
		String sqlQuery = "SELECT ward, COUNT(*) AS count "
				+ "FROM `cc_step1` "
				+ "WHERE catch_date BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
				+ "GROUP BY ward";
		
		if(loginid!=null && !loginid.isEmpty() && !loginid.isBlank()) {
			sqlQuery= sqlQuery +" AND `challan_by`='"+loginid+"'";
		}
		
		//System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery,fromDate,toDate);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Zone List Details");
        response.put("data", result);
		
        return result;
    }
	
	@Transactional
	public List<Map<String, Object>> wardReport(
			MultiValueMap<String, String> formData,
			String zone,
			String fromDate,
			String toDate,
			String loginid) {
		
		String sqlQuery = "SELECT ward, COUNT(*) AS zone_count FROM `cc_step1` WHERE "
				+ "zone=? AND DATE(catch_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') GROUP BY ward ORDER BY ward";
		
		if(loginid!=null && !loginid.isEmpty() && !loginid.isBlank()) {
			sqlQuery= sqlQuery +" AND `challan_by`='"+loginid+"'";
		}
		
		//System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery,zone,fromDate,toDate);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Ward List Details");
        response.put("data", result);
		
        return result;
    }
	*/
}
