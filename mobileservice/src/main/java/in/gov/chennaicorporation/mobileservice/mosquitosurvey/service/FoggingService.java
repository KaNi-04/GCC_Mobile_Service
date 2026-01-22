package in.gov.chennaicorporation.mobileservice.mosquitosurvey.service;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class FoggingService {
	private JdbcTemplate jdbcMosquitoTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
	@Autowired
	public void setDataSource(@Qualifier("mysqlMosquitoDataSource") DataSource mosquitoDataSource) {
		this.jdbcMosquitoTemplate = new JdbcTemplate(mosquitoDataSource);
	}
	
	@Autowired
	public FoggingService(Environment environment) {
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
	
	public static String generateRandomStringForFile(int String_Lenth) {
        StringBuilder result = new StringBuilder(String_Lenth);
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
	
	public String fileUpload(String name, String id, MultipartFile file) {
		
		int lastInsertId = 0;
		// Set the file path where you want to save it
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("mosquito_foldername");
        var year =DateTimeUtil.getCurrentYear();
        var month =DateTimeUtil.getCurrentMonth();
        var date =DateTimeUtil.getCurrentDay();
        
        uploadDirectory = uploadDirectory + serviceFolderName + year +"/"+month+"/"+date;
        
        try {
	        // Create directory if it doesn't exist
	        Path directoryPath = Paths.get(uploadDirectory);
	        if (!Files.exists(directoryPath)) {
	            Files.createDirectories(directoryPath);
	        }
	        
	        // Datetime string
	        String datetimetxt = DateTimeUtil.getCurrentDateTime();
	        // File name 
	        System.out.println(file.getOriginalFilename());
            String fileName = name+ "_" +id + "_" + datetimetxt + "_" + generateRandomStringForFile(10) + "_" + file.getOriginalFilename();
            fileName = fileName.replaceAll("\\s+", ""); // Remove space on filename
            
	        String filePath = uploadDirectory + "/" + fileName;
	        
	        String filepath_txt = "/"+serviceFolderName + year + "/" + month + "/" + date + "/" + fileName;
	        
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
	
	public List<Map<String, Object>> getMaintainByList() {
        String sql = "SELECT * FROM `maintained_by` WHERE isactive=1";
        return jdbcMosquitoTemplate.queryForList(sql);
    }
	
	// ************************************************************************************************************************* //
	// For Canal Activity
	
	public List<Map<String, Object>> getCanalsList(String maintainedby, String zone) {
        String sql = "SELECT * FROM `canals_list` WHERE `isactive`=1 AND (`maintainedby`=? AND `zone`=?) ORDER BY `name`";
        return jdbcMosquitoTemplate.queryForList(sql,maintainedby,zone );
    }
	
	@Transactional
	public List<Map<String, Object>> saveCanalFlow(
			String cid,
			String scid,
			String maintainedby,
			String canalid,
			String q4,
			String q5,
			MultipartFile file,
			String remarks,
			String cby,
			String zone,
			String ward,
			String latitude,
			String longitude,
			String status,
			String address) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		
		int lastInsertId = 0;

		String filetxt = "";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			filetxt = fileUpload("canal_"+cid+"_"+scid, cby, file);
		}
		
		String finalfile = filetxt;
		
		String sqlQuery = "INSERT INTO `fogging_of_canal` "
				+ "(`cid`, `scid`, `maintainedby`, `canalid`, `q4`, `q5`, `treatedfile`, "
				+ "`remarks`, `cby`, `zone`, `ward`, `latitude`, `longitude`, `status`,`address`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		try {
			int affectedRows = jdbcMosquitoTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, cid);
					ps.setString(2, scid);
					ps.setString(3, maintainedby);
					ps.setString(4, canalid);
					ps.setString(5, q4);
					ps.setString(6, q5);
					ps.setString(7, finalfile);
					ps.setString(8, remarks);
					ps.setString(9, cby);
					ps.setString(10, zone);
					ps.setString(11, ward);
					ps.setString(12, latitude);
					ps.setString(13, longitude);
					ps.setString(14, status);
					ps.setString(15, address);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				
				String surveyid = lastInsertId+"";
				
				addCanalActivityData(surveyid,finalfile,"Start Image",cby,zone,ward,latitude,longitude);
				
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "New Survey inserted successfully!");
				System.out.println("A new Survey was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new House Survey.");
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
	public List<Map<String, Object>> addCanalActivityData(
			String canalactivityid,
			String activityfile,
			String remarks,
			String cby,
			String zone,
			String ward,
			String latitude,
			String longitude) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		
		int lastInsertId = 0;
	
		String sqlQuery = "INSERT INTO `fogging_of_canal_data` "
				+ "(`canalactivityid`, `activityfile`, "
				+ "`remarks`, `cby`, `zone`, `ward`, `latitude`, `longitude`) "
				+ "VALUES (?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		try {
			int affectedRows = jdbcMosquitoTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, canalactivityid);
					ps.setString(2, activityfile);
					ps.setString(3, remarks);
					ps.setString(4, cby);
					ps.setString(5, zone);
					ps.setString(6, ward);
					ps.setString(7, latitude);
					ps.setString(8, longitude);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "New canal data inserted successfully!");
				System.out.println("A new canal data was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new canal data.");
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
	
	
	public List<Map<String, Object>> checkHasCanalPending(String cby) {
		
		String pending = "true";
		String message = "Pending found";
		
		String sql = "SELECT `id` FROM `fogging_of_canal` WHERE (isactive=1 AND `status`='pending') AND `cby`=?";
		List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sql, cby);
		
		// Check if result is empty or not
	    if (result.isEmpty()) {
	        pending = "false";
	        message = "No Pending";
	    }
	    
		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", message);
		response.put("pending", pending);
		
		return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getCanalPendingList(String cby) {
	    
	    String sql = "SELECT "
	            + "foc.`id` AS activityid, "
	            + "foc.`maintainedby`, "
	            + "foc.`canalid`, "
	            + "cl.`name`, "
	            + "cl.`maintainedby`, "
	            + "mby.`name`, "
	            + "mby.`code`, "
	            + "foc.`remarks`, "
	            + "foc.`cby`, "
	            + "DATE_FORMAT(foc.`cdate`, '%d-%m-%Y %r') AS createddate, "
	            + "foc.`zone`, "
	            + "foc.`ward`, "
	            + "foc.`latitude`, "
	            + "foc.`longitude`, "
	            + "foc.`status` "
	            + "FROM `fogging_of_canal` foc "
	            + "JOIN `canals_list` AS cl ON foc.canalid = cl.id "
	            + "JOIN `maintained_by` AS mby ON cl.maintainedby = mby.id "
	            + "WHERE foc.`isactive` = 1 AND foc.`status`='pending' AND foc.cby = ?";

	    List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sql, cby);
	    
	    List<Map<String, Object>> canalData = new ArrayList<>();
	  
	    for (Map<String, Object> row : result) {
	        
	        Object activityidObj = row.get("activityid");

	        // Check if the value is an Integer and convert it to String
	        String activityid = "";
	        if (activityidObj instanceof Integer) {
	            activityid = String.valueOf(activityidObj);  // Converts Integer to String
	        } else if (activityidObj instanceof String) {
	            activityid = (String) activityidObj;  // Already a String, so cast
	        }

	        // Query for the images related to the current activity
	        String sqlForImage = "SELECT `did`, "
	                             + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', activityfile) AS activityfile,  "
	                             + "`remarks`, "
	                             + "DATE_FORMAT(`cdate`, '%d-%m-%Y %r') AS createddate, "
	                             + "`zone`, "
	                             + "`ward`, "
	                             + "`latitude`, "
	                             + "`longitude` "
	                             + "FROM `fogging_of_canal_data` WHERE `canalactivityid`=?";
	        
	        List<Map<String, Object>> imageresult = jdbcMosquitoTemplate.queryForList(sqlForImage, activityid);

	        
	        if (!imageresult.isEmpty()) {
	            row.put("fileData", imageresult); 
	        }

	        // Add the row to canalData
	        canalData.add(row);
	    }
	    
	    // Prepare the response
	    Map<String, Object> response = new HashMap<>();
	    response.put("CanalData", canalData);
	    response.put("message", "Canal Activity Data List.");
	    
	    // Return the response wrapped in a singleton list
	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> checkCanalPendingUpdateLocation(String canalactivityid, String latitude, String longitude) {
	    String sql = "SELECT `did`, `canalactivityid`, `cdate`, `latitude`, `longitude` "
	            + "FROM `fogging_of_canal_data` "
	            + "WHERE `isactive` = 1 "
	            + "AND `canalactivityid` = ? "
	            + "ORDER BY `cdate` DESC "
	            + "LIMIT 1";

	    // Retrieve the most recent row based on cdate
	    List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sql, canalactivityid);

	    // If result is empty, return early
	    if (result.isEmpty()) {
	        Map<String, Object> response = new HashMap<>();
	        response.put("status", false);
	        response.put("distance", 0);
	        return Collections.singletonList(response);
	    }

	    // Extract latitude and longitude from the result
	    Map<String, Object> lastRow = result.get(0);
	    String latitudedataStr = (String) lastRow.get("latitude");
	    String longitudedataStr = (String) lastRow.get("longitude");

	    // Convert the String latitude and longitude to Double
	    Double latitudedata = Double.parseDouble(latitudedataStr);
	    Double longitudedata = Double.parseDouble(longitudedataStr);

	    // Convert input latitude and longitude from String to Double
	    double inputLatitude = Double.parseDouble(latitude);
	    double inputLongitude = Double.parseDouble(longitude);

	    // Fixed coordinates (for comparison)
	    double fixedLatitude = inputLatitude;
	    double fixedLongitude = inputLongitude;

	    // Haversine formula to calculate distance
	    double radius = 6371008.8;  // Earth's radius in meters
	    double latDistance = Math.toRadians(latitudedata - fixedLatitude);
	    double lonDistance = Math.toRadians(longitudedata - fixedLongitude);
	    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
	               Math.cos(Math.toRadians(fixedLatitude)) * Math.cos(Math.toRadians(latitudedata)) *
	               Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	    double distance = radius * c; // Distance in meters

	    // Check if distance is between 30 and 50 meters
	    //boolean isWithinRange = (distance >= 30 && distance <= 50);
	    
	    // Check if distance above 30 meters
	    boolean isWithinRange = (distance > 30);

	    // Response based on whether the distance is within the range
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", isWithinRange);
	    response.put("distance", distance);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveCanalActivityData(
			String canalactivityid,
			String isfinal,
			MultipartFile file,
			String remarks,
			String cby,
			String zone,
			String ward,
			String latitude,
			String longitude) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		
		int lastInsertId = 0;
	
		String filetxt = "";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			filetxt = fileUpload(canalactivityid, cby, file);
		}
		String activityfile = filetxt;
		
		String sqlQuery = "INSERT INTO `fogging_of_canal_data` "
				+ "(`canalactivityid`, `activityfile`, "
				+ "`remarks`, `cby`, `zone`, `ward`, `latitude`, `longitude`) "
				+ "VALUES (?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		try {
			int affectedRows = jdbcMosquitoTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, canalactivityid);
					ps.setString(2, activityfile);
					ps.setString(3, remarks);
					ps.setString(4, cby);
					ps.setString(5, zone);
					ps.setString(6, ward);
					ps.setString(7, latitude);
					ps.setString(8, longitude);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "New canal data inserted successfully!");
				System.out.println("A new canal data was inserted successfully! Insert ID: " + generatedId);
				
				if(isfinal.equals("yes")){
					if(updateCanalActivityStatus(canalactivityid)) {
						System.out.println("Canal final status updated successfully!. Activity ID: " + canalactivityid);
					}else {
						System.out.println("Canal final status update failed!. Activity ID: " + canalactivityid);
					}
				}
				
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new canal data.");
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
	
	public Boolean updateCanalActivityStatus(String id) {
		
		String sqlQuery = "UPDATE `fogging_of_canal` SET `status`='close' WHERE `id`=?";
	    int rowsAffected = jdbcMosquitoTemplate.update(sqlQuery, new Object[]{ id });
	    
		return rowsAffected>0;
	}
	
	// ************************************************************************************************************************* //
	// For SWD Activity
	
	@Transactional
	public List<Map<String, Object>> saveSWDFlow(
			String cid,
			String scid,
			String streetname,
			String streetid,
			String q2,
			String q4,
			MultipartFile file,
			String remarks,
			String cby,
			String zone,
			String ward,
			String latitude,
			String longitude,
			String status,
			String address) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		
		int lastInsertId = 0;

		String filetxt = "";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			filetxt = fileUpload("SWD_"+cid+"_"+scid, cby, file);
		}
		
		String finalfile = filetxt;
		
		String sqlQuery = "INSERT INTO `fogging_of_swd` "
				+ "(`cid`, `scid`, `streetname`, `streetid`, `q2`, `q4`, `treatedfile`, "
				+ "`remarks`, `cby`, `zone`, `ward`, `latitude`, `longitude`, `status`,`address`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		try {
			int affectedRows = jdbcMosquitoTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, cid);
					ps.setString(2, scid);
					ps.setString(3, streetname);
					ps.setString(4, streetid);
					ps.setString(5, q2);
					ps.setString(6, q4);
					ps.setString(7, finalfile);
					ps.setString(8, remarks);
					ps.setString(9, cby);
					ps.setString(10, zone);
					ps.setString(11, ward);
					ps.setString(12, latitude);
					ps.setString(13, longitude);
					ps.setString(14, status);
					ps.setString(15, address);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				
				String surveyid = lastInsertId+"";
				
				//addSWDActivityData(surveyid,finalfile,streetname,streetid,q2,q4,remarks,cby,zone,ward,latitude,longitude,"close");
				
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "New Survey inserted successfully!");
				System.out.println("A new Survey was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new House Survey.");
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
	public List<Map<String, Object>> addSWDActivityData(
			String swdactivityid,
			String activityfile,
			String streetname,
			String streetid, 
			String q2,
			String q4,
			String remarks,
			String cby,
			String zone,
			String ward,
			String latitude,
			String longitude,
			String status) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		
		int lastInsertId = 0;
	
		String sqlQuery = "INSERT INTO `fogging_of_swd_data` "
				+ "(`swdactivityid`, `activityfile`, `streetname`, `streetid`, `q2`, `q4`, "
				+ "`remarks`, `cby`, `zone`, `ward`, `latitude`, `longitude`,`status`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		try {
			int affectedRows = jdbcMosquitoTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "did" });
					ps.setString(1, swdactivityid);
					ps.setString(2, activityfile);
					ps.setString(3, streetname);
					ps.setString(4, streetid);
					ps.setString(5, q2);
					ps.setString(6, q4);
					ps.setString(7, remarks);
					ps.setString(8, cby);
					ps.setString(9, zone);
					ps.setString(10, ward);
					ps.setString(11, latitude);
					ps.setString(12, longitude);
					ps.setString(13, status);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "New SWD data inserted successfully!");
				System.out.println("A new SWD data was inserted successfully! Insert ID: " + generatedId);
				
				addSWDExtraActivityData(2,swdactivityid,"","","","","","",cby,zone,ward,"","","pending");
				
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new SWD data.");
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
	public List<Map<String, Object>> addSWDExtraActivityData(
			int totaltaskrequired,
			String swdactivityid,
			String activityfile,
			String streetname,
			String streetid, 
			String q2,
			String q4,
			String remarks,
			String cby,
			String zone,
			String ward,
			String latitude,
			String longitude,
			String status) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		
		int lastInsertId = 0;
	
		String sqlQuery = "INSERT INTO `fogging_of_swd_data` "
				+ "(`swdactivityid`, `activityfile`, `streetname`, `streetid`, `q2`, `q4`, "
				+ "`remarks`, `cby`, `zone`, `ward`, `latitude`, `longitude`,`status`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		try {
			
			// Insert the data the required number of times
			for (int i = 0; i < totaltaskrequired; i++) {
				int affectedRows = jdbcMosquitoTemplate.update(new PreparedStatementCreator() {
					@Override
					public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
						PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "did" });
						ps.setString(1, swdactivityid);
						ps.setString(2, activityfile);
						ps.setString(3, streetname);
						ps.setString(4, streetid);
						ps.setString(5, q2);
						ps.setString(6, q4);
						ps.setString(7, remarks);
						ps.setString(8, cby);
						ps.setString(9, zone);
						ps.setString(10, ward);
						ps.setString(11, latitude);
						ps.setString(12, longitude);
						ps.setString(13, status);
						return ps;
					}
				}, keyHolder);
			
				if (affectedRows > 0) {
					Number generatedId = keyHolder.getKey();
					lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
					response.put("insertId", lastInsertId);
					response.put("status", "success");
					response.put("message", "New SWD data inserted successfully!");
					System.out.println("A new SWD data was inserted successfully! Insert ID: " + generatedId);
				} else {
					response.put("status", "error");
					response.put("message", "Failed to insert a new SWD data.");
				}
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
	
	public List<Map<String, Object>> checkHasSWDPending(String cby) {
		
		String pending = "true";
		String message = "Pending found";
		
		String sql = "SELECT `id` FROM `fogging_of_swd` WHERE (isactive=1 AND `status`='pending') AND `cby`=?";
		List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sql, cby);
		
		// Check if result is empty or not
	    if (result.isEmpty()) {
	        pending = "false";
	        message = "No Pending";
	    }
	    
	    pending = "false";
        message = "No Pending";
        
		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", message);
		response.put("pending", pending);
		
		return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getSWDPendingList(String cby) {
	    
	    String sql = "SELECT "
	            + "swd.`id` AS activityid, "
	            + "swd.`streetname`, "
	            + "swd.`streetid`, "
	            + "swd.`cby`, "
	            //+ "DATE_FORMAT(foc.`cdate`, '%d-%m-%Y %r') AS createddate, "
	            + "swd.`zone`, "
	            + "swd.`ward`, "
	            + "swd.`latitude`, "
	            + "swd.`longitude`, "
	            + "swd.`address`, "
	            + "swd.`status` "
	            + "FROM `fogging_of_swd` swd "
	            + "WHERE swd.`isactive` = 1 AND swd.`status`='pending' AND swd.cby = ?";

	    List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sql, cby);
	    
	    List<Map<String, Object>> SWDData = new ArrayList<>();
	  
	    for (Map<String, Object> row : result) {
	        
	        Object activityidObj = row.get("activityid");

	        // Check if the value is an Integer and convert it to String
	        String activityid = "";
	        if (activityidObj instanceof Integer) {
	            activityid = String.valueOf(activityidObj);  // Converts Integer to String
	        } else if (activityidObj instanceof String) {
	            activityid = (String) activityidObj;  // Already a String, so cast
	        }

	        // Query for the task related to the current activity
	        String sqlForTask = "SELECT `did`, `swdactivityid`, "
	                             + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', activityfile) AS activityfile,  "
	                             + "`streetname`, "
	                             + "`streetid`, "
	                             + "`q2`, "
	                             + "`q4`, "
	                             + "`remarks`, "
	                             + "DATE_FORMAT(`cdate`, '%d-%m-%Y %r') AS createddate, "
	                             + "`zone`, "
	                             + "`ward`, "
	                             + "`latitude`, "
	                             + "`longitude`, "
	                             + "`status` "
	                             + "FROM `fogging_of_swd_data` WHERE `swdactivityid`=?";
	        
	        List<Map<String, Object>> taskresult = jdbcMosquitoTemplate.queryForList(sqlForTask, activityid);

	        
	        if (!taskresult.isEmpty()) {
	            row.put("tasklist", taskresult); 
	        }

	        // Add the row to SWDData
	        SWDData.add(row);
	    }
	    
	    // Prepare the response
	    Map<String, Object> response = new HashMap<>();
	    response.put("SWDData", SWDData);
	    response.put("message", "SWD Activity Data List.");
	    
	    // Return the response wrapped in a singleton list
	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> checkSWDPendingUpdateLocation(String swdactivityid, String streetid) {
	    String sql = "SELECT `did`, `swdactivityid` "
	            + "FROM `fogging_of_swd_data` "
	            + "WHERE `isactive` = 1 "
	            + "AND `swdactivityid` = ? AND `streetid`=? "
	            + "ORDER BY `cdate` DESC "
	            + "LIMIT 1";

	    // Retrieve the most recent row based on cdate
	    List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sql, swdactivityid, streetid);

	    Map<String, Object> response = new HashMap<>();
	    // If result is empty, return early
	    if (result.isEmpty()) {
	        response.put("status", true);
	    }
	    else {
	    	response.put("status", false);
	    }
	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> updateSWDPendingStatus(
			String swdactivityid,
			MultipartFile file,
			String streetname,
			String streetid, 
			String q2,
			String q4,
			String remarks,
			String cby,
			String zone,
			String ward,
			String latitude,
			String longitude,
			String status,
			String did
			) {
	    
		// Create a response map
	    Map<String, Object> response = new HashMap<>();
	    
		if(!checkupdateSWDPendingStreetid(streetid,swdactivityid)) {
			// Wrap the response in a list as required
			response.put("status", false);
		    return Collections.singletonList(response);
		}
		
		String filetxt ="";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			filetxt = fileUpload("SWD_"+"_"+did+"_"+swdactivityid, cby, file);
		}
		
		String finalactivityfile = filetxt;
		
		LocalDateTime updateDate = LocalDateTime.now();

		String formattedDate = updateDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		String sqlQuery = "UPDATE `fogging_of_swd_data` SET "
				+ "`activityfile`=?,`streetname`=?,`streetid`=?,`q2`=?,`q4`=?,`remarks`=?,"
				+ "`cdate`=?,`latitude`=?,`longitude`=?,`status`=?"
				+ "WHERE `did`=? AND `swdactivityid`=? AND `zone`=? AND `ward`=? AND cby=?";
		
		// Execute the update query
	    int rowsAffected = jdbcMosquitoTemplate.update(sqlQuery, new Object[]{
	    		finalactivityfile, streetname, streetid, q2, q4, remarks, formattedDate, latitude, longitude, status,
	    		did, swdactivityid, zone, ward, cby
	    });
	    
	    // If rows are affected, it means the update was successful
	    response.put("status", rowsAffected > 0);
	    
	    if(updateSWDStatus(swdactivityid)) {
	    	System.out.println("SWD PARENT Status Updated from `pending` -> `close` (swdactivityid (id):"+ swdactivityid + ")");
	    }
	    
	    // Wrap the response in a list as required
	    return Collections.singletonList(response);
	}
	
	public boolean checkupdateSWDPendingStreetid(String swdactivityid,String streetid) {
		String sql = "SELECT `did` FROM `fogging_of_swd_data` WHERE (`isactive`=1 AND `streetid`=?) AND `swdactivityid`=?";
		List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sql, streetid, swdactivityid);
	    return result.isEmpty();
	}
	
	public boolean updateSWDStatus(String swdactivityid) {
		String sql = "SELECT `did` FROM `fogging_of_swd_data` WHERE (`isactive`=1 AND `status`='pending') AND `swdactivityid`=?";
		List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sql, swdactivityid);
		
		int rowsAffected = 0;
		// Check if result is empty or not
	    if (result.isEmpty()) {
	    	String sqlQuery = "UPDATE `fogging_of_swd` SET `status`=? WHERE `id`=?";
		    rowsAffected = jdbcMosquitoTemplate.update(sqlQuery, new Object[]{"close",swdactivityid});
	    }
	    return rowsAffected > 0;
	}
	
	@Transactional
	public List<Map<String, Object>> saveStreetData(
			String cid,
			String scid,
			String streetid,
			String streetname,
			MultipartFile file,
			String remarks,
			String cby,
			String zone,
			String ward,
			String latitude,
			String longitude,
			String status,
			String address) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		
		int lastInsertId = 0;

		String treatedfile = "";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			treatedfile = fileUpload(cid+"_"+scid, cby, file);
		}
		
		String finaltreatedfile = treatedfile;
		
		String sqlQuery = "INSERT INTO `fogging_of_street_data` "
				+ "(`cid`, `scid`, `streetid`, `streetname`, `treatedfile`, "
				+ "`remarks`, `cby`, `zone`, `ward`, `latitude`, `longitude`, `status`,`address`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		try {
			int affectedRows = jdbcMosquitoTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, cid);
					ps.setString(2, scid);
					ps.setString(3, streetid);
					ps.setString(4,streetname);
					ps.setString(5, finaltreatedfile);
					ps.setString(6, remarks);
					ps.setString(7, cby);
					ps.setString(8, zone);
					ps.setString(9, ward);
					ps.setString(10, latitude);
					ps.setString(11, longitude);
					ps.setString(12, status);
					ps.setString(13, address);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				
				String surveyid = lastInsertId+"";
				
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "New Survey inserted successfully!");
				System.out.println("A new Survey was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new House Survey.");
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
	public List<Map<String, Object>> saveVacantLandData(
			String cid,
			String scid,
			String name,
			String q2,
			String q3,
			MultipartFile file,
			String remarks,
			String cby,
			String zone,
			String ward,
			String latitude,
			String longitude,
			String status,
			String address) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		
		int lastInsertId = 0;

		String treatedfile = "";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			treatedfile = fileUpload(cid+"_"+scid, cby, file);
		}
		
		String finaltreatedfile = treatedfile;
		
		String sqlQuery = "INSERT INTO `fogging_of_vacantland_data` "
				+ "(`cid`, `scid`, `name`, `q2`, `q3`, `treatedfile`, "
				+ "`remarks`, `cby`, `zone`, `ward`, `latitude`, `longitude`, `status`,`address`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		try {
			int affectedRows = jdbcMosquitoTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, cid);
					ps.setString(2, scid);
					ps.setString(3, name);
					ps.setString(4, q2);
					ps.setString(5, q3);
					ps.setString(6, finaltreatedfile);
					ps.setString(7, remarks);
					ps.setString(8, cby);
					ps.setString(9, zone);
					ps.setString(10, ward);
					ps.setString(11, latitude);
					ps.setString(12, longitude);
					ps.setString(13, status);
					ps.setString(14, address);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				
				String surveyid = lastInsertId+"";
				
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "New Survey inserted successfully!");
				System.out.println("A new Survey was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new House Survey.");
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
	
	
	// SAMPLE COLLECTION 
	
	@Transactional
	public List<Map<String, Object>> saveLarvalAdultCollectionFlow(
			String cid,
			String maintainedby,
			String canalid,
			String q2,
			String q6,
			String q7,
			MultipartFile file,
			String remarks,
			String cby,
			String zone,
			String ward,
			String latitude,
			String longitude,
			String status,
			String address) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		
		int lastInsertId = 0;

		String filetxt = "";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			filetxt = fileUpload("lavasample_"+cid+"_"+canalid, cby, file);
		}
		
		String finalfile = filetxt;
		
		String sqlQuery = "INSERT INTO `larval_adult_collection_data` "
				+ "(`cid`, `maintainedby`, `canalid`, `q2`, `q6`, `q7`, `treatedfile`, "
				+ "`remarks`, `cby`, `zone`, `ward`, `latitude`, `longitude`, `status`,`address`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		try {
			int affectedRows = jdbcMosquitoTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, cid);
					ps.setString(2, maintainedby);
					ps.setString(3, canalid);
					ps.setString(4, q2);
					ps.setString(5, q6);
					ps.setString(6, q7);
					ps.setString(7, finalfile);
					ps.setString(8, remarks);
					ps.setString(9, cby);
					ps.setString(10, zone);
					ps.setString(11, ward);
					ps.setString(12, latitude);
					ps.setString(13, longitude);
					ps.setString(14, status);
					ps.setString(15, address);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				
				String surveyid = lastInsertId+"";
				
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "New Survey inserted successfully!");
				System.out.println("A new Survey was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new House Survey.");
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
	
	// Reports 
	/*
	public List<Map<String, Object>> summaryReport(String cby) {
	    
	    String sql = "";

	    List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sql, cby);
	    
	    List<Map<String, Object>> reportData = new ArrayList<>();
	  
	    	row.put("tasklist", taskresult); 
	        // Add the row to SWDData
	        reportData.add(row);
	    
	    // Prepare the response
	    Map<String, Object> response = new HashMap<>();
	    response.put("ReportData", reportData);
	    response.put("message", "Summary Data.");
	    
	    // Return the response wrapped in a singleton list
	    return Collections.singletonList(response);
	}
	*/
}
