package in.gov.chennaicorporation.mobileservice.toilets.service;

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
import java.time.LocalTime;
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

@Service("gccofficialappstoiletsactivity")
public class activity {
	private JdbcTemplate jdbcToiletsTemplate;
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final int STRING_LENGTH = 15;
	private static final Random RANDOM = new SecureRandom();

	@Autowired
	public void setDataSource(@Qualifier("mysqlToiletsDataSource") DataSource toiletsDataSource) {
		this.jdbcToiletsTemplate = new JdbcTemplate(toiletsDataSource);
	}
	
	@Autowired
	public activity(Environment environment) {
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

	public String fileUpload(String name, String id, MultipartFile file) {

		int lastInsertId = 0;
		// Set the file path where you want to save it
		String uploadDirectory = environment.getProperty("file.upload.directory");
		String serviceFolderName = environment.getProperty("toilets_foldername");
		var year = DateTimeUtil.getCurrentYear();
		var month = DateTimeUtil.getCurrentMonth();
		var date = DateTimeUtil.getCurrentDay();

		uploadDirectory = uploadDirectory + serviceFolderName + year + "/" + month + "/" + date;

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

			String filepath_txt = "/" + serviceFolderName + year + "/" + month + "/" + date + "/" + fileName;

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
	
	public static String getTypeBasedOnTime() {
        // Get the current time
        LocalTime now = LocalTime.now();

        // Define morning and evening time ranges
        LocalTime morningStart = LocalTime.of(1, 0); // 1:00 AM
        LocalTime eveningStart = LocalTime.of(13, 0); // 1:00 PM

        // Check and return the appropriate type
        if (now.isAfter(morningStart) && now.isBefore(eveningStart)) {
            return "morning";
        } else {
            return "evening";
        }
    }
	
	public List<Map<String, Object>> getToiletsList(String loginid) {
        String sql = "SELECT *,"
        		+ " CONCAT('"+fileBaseUrl+"/gccofficialapp/files',image) AS photo "
        		+ " FROM `gcc_toilet_list` WHERE `user_id`=? AND `isactive`=1 AND `gcc_app_updated`= 0 ";
        List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sql,loginid);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Toilets List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> toiletlistRest(
			MultiValueMap<String, String> formData,
			String latitude,
			String longitude) {
			
		String sqlWhere = "";

		if (latitude != null && longitude != null && !latitude.isBlank() && !latitude.isEmpty() && !longitude.isBlank()
		        && !longitude.isEmpty()) {
		    sqlWhere += " AND ((6371008.8 * acos(ROUND(cos(radians(" + latitude
		            + ")) * cos(radians(latitude)) * cos(radians(longitude) - radians(" + longitude
		            + ")) + sin(radians(" + latitude + ")) * sin(radians(latitude)), 9))) < 200)"
		            + " ORDER BY `id` DESC";
		}

		String sqlQuery = "SELECT *,"
        				+ " CONCAT('"+fileBaseUrl+"/gccofficialapp/files',image) AS photo "
        				+ " FROM `gcc_toilet_list` WHERE `isactive`=1" // This is the starting part of WHERE clause
		                + sqlWhere; // Here the latitude and longitude condition gets appended if provided
		
		List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Nearby toilet list.");
        if(result.isEmpty()) {
        	response.put("message", "No toilet found nearby.");
        }
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> updateToiletLocation(String id,String latitude, String longitude, MultipartFile file) {
		Map<String, Object> response = new HashMap<>();
		String image = "";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			var year = DateTimeUtil.getCurrentYear();
			var month = DateTimeUtil.getCurrentMonth();
			image = fileUpload(year, month, file);
		}
		
		String toiletimg = image;
	    String sql = "UPDATE `gcc_toilet_list` SET `gcc_app_updated`=1, `latitude`=?, longitude=?, `image`=? WHERE `id`=?";
	    jdbcToiletsTemplate.update(sql, latitude, longitude, toiletimg, id);
	    
	    
		response.put("status", "success");
		response.put("message", "Toilet Data updated successfully!");
		System.out.println("Toilet Data updated successfully!! Update ID: " + id);
		
		return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getToiletsListWithButton(String loginid) {
		
        String sql ="SELECT "
        		+ "    `gcc_toilet_list`.*, "
        		+ "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', "
        		+ "           CASE "
        		+ "               WHEN `gcc_toilet_list`.`image` IS NULL OR `gcc_toilet_list`.`image` = '' "
        		+ "               THEN `toilet_feedback`.`image` "
        		+ "               ELSE `gcc_toilet_list`.`image` "
        		+ "           END "
        		+ "    ) AS photo, "
        		+ "    MAX(CASE WHEN `toilet_feedback`.`type` = 'morning' THEN true ELSE false END) AS morning_feedback, "
        		+ "    MAX(CASE WHEN `toilet_feedback`.`type` = 'evening' THEN true ELSE false END) AS evening_feedback "
        		+ "FROM "
        		+ "    `gcc_toilet_list` "
        		+ "LEFT JOIN "
        		+ "    `toilet_feedback` "
        		+ "    ON `toilet_feedback`.`toilet_id` = `gcc_toilet_list`.`id` "
        		+ "    AND `toilet_feedback`.`isactive` = 1 "
        		+ "    AND DATE(`toilet_feedback`.`cdate`) = CURDATE() "
        		+ "WHERE "
        		+ "    `gcc_toilet_list`.`user_id` = ? "
        		+ "    AND `gcc_toilet_list`.`isactive` = 1 AND `gcc_app_updated`= 1 "
        		+ "GROUP BY "
        		+ "    `gcc_toilet_list`.`id`";
        
        List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sql,loginid);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Toilets List with extra info.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getQuestionsList() {
        String sql = "SELECT * FROM question_list WHERE isactive=1";
        List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sql);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Toilet Question List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getParentQuestionsList() {
        String sql = "SELECT * FROM question_list WHERE isactive=1 AND `pid`=0";
        List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sql);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Toilet Parent Question List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getChildQuestionsList(String pid) {
        String sql = "SELECT * FROM question_list WHERE isactive=1 AND `pid`=?";
        List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sql,pid);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Toilet Child Question List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }

	@Transactional
	public List<Map<String, Object>> addNewToilet(String latitude,
			String longitude, String zone, String ward, String name, String locality, String cby, MultipartFile file) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;

		String image = "";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			var year = DateTimeUtil.getCurrentYear();
			var month = DateTimeUtil.getCurrentMonth();
			image = fileUpload(year, month, file);
		}
		
		String toiletimg = image;
		
		String sqlQuery = "INSERT INTO `gcc_toilet_list`("
				+ "`zone`, `zone_name`, `ward`, `user_id`, `list`, "
				+ "`name`, `locality`, `latitude`, `longitude`, `toilet_availability`, "
				+ "`toilet_category`, `toilet_type`, `working_condition`, `refurbishment_type`,`image`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
			int affectedRows = jdbcToiletsTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, zone);
					ps.setString(2, "");
					ps.setString(3, ward);
					ps.setString(4, cby);
					ps.setString(5, "");
					ps.setString(6, name);
					ps.setString(7, locality);
					ps.setString(8, latitude);
					ps.setString(9, longitude);
					ps.setString(10, "");
					ps.setString(11, "");
					ps.setString(12, "");
					ps.setString(13, "");
					ps.setString(14, "");
					ps.setString(15, toiletimg);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "A new asset was inserted successfully!");
				System.out.println("A new asset was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new asset.");
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
	public List<Map<String, Object>> toiletlistbylatlong(
			MultiValueMap<String, String> formData,
			String latitude,
			String longitude,
			String id,
			String loginid) {
			
		String sqlWhere = "";

		if (latitude != null && longitude != null && !latitude.isBlank() && !latitude.isEmpty() && !longitude.isBlank()
		        && !longitude.isEmpty()) {
		    sqlWhere += " AND ((6371008.8 * acos(ROUND(cos(radians(" + latitude
		            + ")) * cos(radians(latitude)) * cos(radians(longitude) - radians(" + longitude
		            + ")) + sin(radians(" + latitude + ")) * sin(radians(latitude)), 9))) < 200)"
		            + " ORDER BY `id` DESC";
		}

		String sqlQuery = "SELECT *,"
        				+ " CONCAT('"+fileBaseUrl+"/gccofficialapp/files',image) AS photo "
        				+ " FROM `gcc_toilet_list` WHERE `user_id`=? AND `isactive`=1 AND id = ?" // This is the starting part of WHERE clause
		                + sqlWhere; // Here the latitude and longitude condition gets appended if provided
		
		List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sqlQuery,loginid, id);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Nearby toilet list.");
        if(result.isEmpty()) {
        	response.put("message", "No toilet found nearby.");
        }
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	public String inactiveFeedBack(String type, String toilet_id) {
        
		String sql = "UPDATE `toilet_feedback` SET `isactive`=0 WHERE DATE(`toilet_feedback`.`cdate`) = CURDATE() AND `type`= ? AND `toilet_id`=?";
		jdbcToiletsTemplate.update(sql,type,toilet_id);
		
		return "sussess";
    }
	
	@Transactional
	public List<Map<String, Object>> saveFeedback(
			String toilet_id,
			String cby,
			String latitude,
			String longitude, 
			String zone, 
			String ward,
			String q_1,
			String q_2,
			String q_3,
			String q_4,
			String q_5,
			String q_6,
			String q_7,
			String q_8,
			String q_9,
			String q_10,
			String q_11,
			String remarks,
			MultipartFile file,
			
			MultipartFile file_1, 
			MultipartFile file_2, 
			MultipartFile file_3, 
			MultipartFile file_4, 
			MultipartFile file_5, 
			MultipartFile file_6, 
			MultipartFile file_7, 
			MultipartFile file_8, 
			MultipartFile file_9, 
			MultipartFile file_10, 
			MultipartFile file_11) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;

		String image = "";
		String image_q1 = "";
		String image_q2 = "";
		String image_q3 = "";
		String image_q4 = "";
		String image_q5 = "";
		String image_q6 = "";
		String image_q7 = "";
		String image_q8 = "";
		String image_q9 = "";
		String image_q10 = "";
		String image_q11 = "";
		
		var year = DateTimeUtil.getCurrentYear();
		var month = DateTimeUtil.getCurrentMonth();
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			image = fileUpload("feedback", "0", file);
		}
		
		// Question Images
		if (file_1 != null && !file_1.isEmpty()) {
			image_q1 = fileUpload("1", "q1", file_1);
		}
		
		if (file_2 != null && !file_2.isEmpty()) {
			image_q2 = fileUpload("2", "q2", file_2);
		}
		
		if (file_3 != null && !file_3.isEmpty()) {
			image_q3 = fileUpload("3", "q3", file_3);
		}
		
		if (file_4 != null && !file_4.isEmpty()) {
			image_q4 = fileUpload("4", "q4", file_4);
		}
		
		if (file_5 != null && !file_5.isEmpty()) {
			image_q5 = fileUpload("5", "q5", file_5);
		}
		
		if (file_6 != null && !file_6.isEmpty()) {
			image_q6 = fileUpload("6", "q6", file_6);
		}
		
		if (file_7 != null && !file_7.isEmpty()) {
			image_q7 = fileUpload("7", "q7", file_7);
		}
		
		if (file_8 != null && !file_8.isEmpty()) {
			image_q8 = fileUpload("8", "q8", file_8);
		}
		
		if (file_9 != null && !file_9.isEmpty()) {
			image_q9 = fileUpload("9", "q9", file_9);
		}
		
		if (file_10 != null && !file_10.isEmpty()) {
			image_q10 = fileUpload("10", "q10", file_10);
		}
		
		if (file_11 != null && !file_11.isEmpty()) {
			image_q11 = fileUpload("11", "q11", file_11);
		}
		
		String feedbackimg = image;
		
		String q1_image = image_q1;
		String q2_image = image_q2;
		String q3_image = image_q3;
		String q4_image = image_q4;
		String q5_image = image_q5;
		String q6_image = image_q6;
		String q7_image = image_q7;
		String q8_image = image_q8;
		String q9_image = image_q9;
		String q10_image = image_q10;
		String q11_image = image_q11;
		
		String type = getTypeBasedOnTime(); // Get Morning & Evening
		
		// Get today's date
        LocalDate today = LocalDate.now();

        // Format it in the desired format (yyyy-MM-dd)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String todayDate = today.format(formatter);
        
		inactiveFeedBack(type, toilet_id); // Upadte already insert data.
		
		String sqlQuery = "INSERT INTO `toilet_feedback`("
				+ "`toilet_id`, `cby`, "
				+ "`q1`, `q2`, `q3`, `q4`, `q5`, `q6`, `q7`, `q8`, `q9`, `q10`, `q11`, "
				+ "`remarks`,`latitude`, `longitude`,`zone`,`ward`,`type`,`image`,"
				+ "`q1_image`, `q2_image`, `q3_image`, `q4_image`, `q5_image`, `q6_image`, "
				+ "`q7_image`, `q8_image`, `q9_image`, `q10_image`, `q11_image`"
				+ ") "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
			int affectedRows = jdbcToiletsTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, toilet_id);
					ps.setString(2, cby);
					ps.setString(3, q_1);
					ps.setString(4, q_2);
					ps.setString(5, q_3);
					ps.setString(6, q_4);
					ps.setString(7, q_5);
					ps.setString(8, q_6);
					ps.setString(9, q_7);
					ps.setString(10, q_8);
					ps.setString(11, q_9);
					ps.setString(12, q_10);
					ps.setString(13, q_11);
					ps.setString(14, remarks);
					ps.setString(15, latitude);
					ps.setString(16, longitude);
					ps.setString(17, zone);
					ps.setString(18, ward);
					ps.setString(19, type);
					ps.setString(20, feedbackimg);
					
					ps.setString(21, q1_image);
					ps.setString(22, q2_image);
					ps.setString(23, q3_image);
					ps.setString(24, q4_image);
					ps.setString(25, q5_image);
					ps.setString(26, q6_image);
					ps.setString(27, q7_image);
					ps.setString(28, q8_image);
					ps.setString(29, q9_image);
					ps.setString(30, q10_image);
					ps.setString(31, q11_image);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "A new feedback was inserted successfully!");
				System.out.println("A new feedback was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new feedback. Asset ID:" + toilet_id);
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
	
	// Report
	
	@Transactional
	public List<Map<String, Object>> zoneReport(
			MultiValueMap<String, String> formData,
			String fromDate,
			String toDate,
			String loginid) {

		String sqlQuery = "SELECT "
				+ "    `gcc_toilet_list`.`zone`, "
				+ "    COUNT(DISTINCT `gcc_toilet_list`.`id`) AS Total_Toilets, "
				+ "    SUM(CASE "
				+ "        WHEN `gcc_toilet_list`.`gcc_app_updated` = '0' THEN 1 "
				+ "        ELSE 0 "
				+ "    END) AS Enumeration_Pending, "
				+ "    SUM(CASE "
				+ "        WHEN `toilet_feedback`.`type` = 'morning' THEN 1 "
				+ "        ELSE 0 "
				+ "    END) AS Morning_Count, "
				+ "    SUM(CASE "
				+ "        WHEN `toilet_feedback`.`type` = 'evening' THEN 1 "
				+ "        ELSE 0 "
				+ "    END) AS Evening_Count "
				+ "FROM "
				+ "    `gcc_toilet_list` "
				+ "LEFT JOIN "
				+ "    `toilet_feedback` "
				+ "    ON `toilet_feedback`.`toilet_id` = `gcc_toilet_list`.`id` "
				+ "		AND `toilet_feedback`.`isactive` = 1 "
				+ "    AND DATE(`toilet_feedback`.`cdate`) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
				+ "WHERE "
				//+ "    `gcc_toilet_list`.`user_id` = ? AND "
				+ "    `gcc_toilet_list`.`isactive` = 1 "
				+ "GROUP BY "
				+ "    `gcc_toilet_list`.`zone` "
				+ "ORDER BY "
				+ "    `gcc_toilet_list`.`zone`";
		
		List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sqlQuery, fromDate, toDate);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Zone report.");
        if(result.isEmpty()) {
        	response.put("message", "No data found.");
        }
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> wardReport(
			MultiValueMap<String, String> formData,
			String fromDate,
			String toDate,
			String zone,
			String loginid) {

		String sqlQuery = "SELECT "
				+ "    `gcc_toilet_list`.`ward`, "
				+ "    COUNT(DISTINCT `gcc_toilet_list`.`id`) AS Total_Toilets, "
				+ "    SUM(CASE "
				+ "        WHEN `gcc_toilet_list`.`gcc_app_updated` = '0' THEN 1 "
				+ "        ELSE 0 "
				+ "    END) AS Enumeration_Pending, "
				+ "    SUM(CASE "
				+ "        WHEN `toilet_feedback`.`type` = 'morning' THEN 1 "
				+ "        ELSE 0 "
				+ "    END) AS Morning_Count, "
				+ "    SUM(CASE "
				+ "        WHEN `toilet_feedback`.`type` = 'evening' THEN 1 "
				+ "        ELSE 0 "
				+ "    END) AS Evening_Count "
				+ "FROM "
				+ "    `gcc_toilet_list` "
				+ "LEFT JOIN "
				+ "    `toilet_feedback` "
				+ "    ON `toilet_feedback`.`toilet_id` = `gcc_toilet_list`.`id` "
				+ "		AND `toilet_feedback`.`isactive` = 1 "
				+ "    AND DATE(`toilet_feedback`.`cdate`) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
				+ "WHERE "
				+ "    `gcc_toilet_list`.`zone` = ? AND "
				+ "    `gcc_toilet_list`.`isactive` = 1 "
				+ "GROUP BY "
				+ "    `gcc_toilet_list`.`ward` "
				+ "ORDER BY "
				+ "    `gcc_toilet_list`.`ward`";
		
		List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sqlQuery, fromDate, toDate, zone);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Ward report.");
        if(result.isEmpty()) {
        	response.put("message", "No data found.");
        }
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> toiletReport(
			MultiValueMap<String, String> formData,
			String fromDate,
			String toDate,
			String ward,
			String loginid) {
			
		

		String sqlQuery = "SELECT "
				+ "    `gcc_toilet_list`.`id`,`gcc_toilet_list`.`name`,`gcc_toilet_list`.`zone`,`gcc_toilet_list`.`ward`, "
				+ "	   CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `gcc_toilet_list`.`image`) AS photo,"
				+ "    SUM(CASE "
				+ "        WHEN `gcc_toilet_list`.`gcc_app_updated` = '0' THEN 1 "
				+ "        ELSE 0 "
				+ "    END) AS Enumeration_Pending, "
				+ "    SUM(CASE "
				+ "        WHEN `toilet_feedback`.`type` = 'morning' THEN 1 "
				+ "        ELSE 0 "
				+ "    END) AS Morning_Count, "
				+ "    SUM(CASE "
				+ "        WHEN `toilet_feedback`.`type` = 'evening' THEN 1 "
				+ "        ELSE 0 "
				+ "    END) AS Evening_Count "
				+ "FROM "
				+ "    `gcc_toilet_list` "
				+ "LEFT JOIN "
				+ "    `toilet_feedback` "
				+ "    ON `toilet_feedback`.`toilet_id` = `gcc_toilet_list`.`id` "
				+ "		AND `toilet_feedback`.`isactive` = 1 "
				+ "    AND DATE(`toilet_feedback`.`cdate`) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
				+ "WHERE "
				+ "    `gcc_toilet_list`.`ward` = ? AND "
				+ "    `gcc_toilet_list`.`isactive` = 1 "
				+ "GROUP BY "
				+ "    `gcc_toilet_list`.`id` "
				+ "ORDER BY "
				+ "    `gcc_toilet_list`.`id`";
		
		List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sqlQuery, fromDate, toDate, ward);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Toilet list report.");
        if(result.isEmpty()) {
        	response.put("message", "No data found.");
        }
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> feedbackReport(
	        MultiValueMap<String, String> formData,
	        String fromDate,
	        String toDate,
	        //String ward,
	        String type,
	        String toiletid,
	        String loginid) {

	    String sqlQuery = "SELECT "
	    		+ "    `gcc_toilet_list`.`id` as assetid, "
	    		+ "    `gcc_toilet_list`.`name`, "
	    		+ "    `gcc_toilet_list`.`zone`, "
	    		+ "    `gcc_toilet_list`.`ward`, "
	    		+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `gcc_toilet_list`.`image`) AS toiletPhoto, "
	    		+ "    DATE_FORMAT(`toilet_feedback`.`cdate`, '%d-%m-%Y %I:%i %p') as feedbackDate, "
	    		+ "    `toilet_feedback`.*, "
	    		+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `toilet_feedback`.`image`) AS feedbackPhoto "
	    		+ "FROM "
	    		+ "    `gcc_toilet_list` "
	    		+ "RIGHT JOIN "
	    		+ "    `toilet_feedback` "
	    		+ "    ON `toilet_feedback`.`toilet_id` = `gcc_toilet_list`.`id` "
	    		+ "		AND `toilet_feedback`.`isactive` = 1 "
	    		+ "	   AND `toilet_feedback`.`type` = ? "
	    		+ "    AND DATE(`toilet_feedback`.`cdate`) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
	    		+ "WHERE "
	    		+ "    `gcc_toilet_list`.`id` = ? "
	    		+ "    AND `gcc_toilet_list`.`isactive` = 1 "
	    		+ "ORDER BY "
	    		+ "    feedbackDate";

	    List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sqlQuery, type, fromDate, toDate, toiletid);
	    
	    // Questions
	    String sqlQuestion = "SELECT * FROM `question_list`";

	    List<Map<String, Object>> question = jdbcToiletsTemplate.queryForList(sqlQuestion);

	    // Prepare response
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", 200);
	    if (result.isEmpty()) {
	        response.put("message", "No data found.");
	        response.put("data", Collections.emptyList());
	        response.put("question", Collections.emptyList());
	    } else {
	        response.put("message", "Feedback list report.");
	        response.put("data", result);
	        response.put("question", question);
	    }

	    return Collections.singletonList(response);
	}
	/*
	@Transactional
	public List<Map<String, Object>> feedbackReport(
	        MultiValueMap<String, String> formData,
	        String fromDate,
	        String toDate,
	        String type,
	        String toiletid,
	        String loginid) {

	    // Fetch feedback data
	    String sqlQuery = "SELECT `gcc_toilet_list`.`id` as assetid,`gcc_toilet_list`.`name`,`gcc_toilet_list`.`zone`,"
	            + "`gcc_toilet_list`.`ward`, CONCAT('https://gccservices.in/gccofficialapp/files', `gcc_toilet_list`.`image`) AS toiletPhoto,"
	            + " DATE(`toilet_feedback`.`cdate`) as feedbackDate, "
	            + "`toilet_feedback`.*, CONCAT('https://gccservices.in/gccofficialapp/files', `toilet_feedback`.`image`) AS feedbackPhoto "
	            + "FROM `gcc_toilet_list` JOIN `toilet_feedback` ON `toilet_feedback`.`toilet_id` = `gcc_toilet_list`.`id` "
	            + "AND `toilet_feedback`.`type` = ? "
	            + "AND DATE(`toilet_feedback`.`cdate`) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') "
	            + "AND STR_TO_DATE(?, '%d-%m-%Y') WHERE `gcc_toilet_list`.`id` = ? "
	            + "AND `gcc_toilet_list`.`isactive` = 1 GROUP BY `gcc_toilet_list`.`id` ORDER BY feedbackDate";

	    List<Map<String, Object>> feedbackData = jdbcToiletsTemplate.queryForList(sqlQuery, type, fromDate, toDate, toiletid);

	    // Fetch question data
	    String sqlQuestion = "SELECT * FROM `question_list`";
	    List<Map<String, Object>> questionData = jdbcToiletsTemplate.queryForList(sqlQuestion);

	    // Map questions by ID for easy lookup
	    Map<String, Map<String, Object>> questionMap = new HashMap<>();
	    for (Map<String, Object> question : questionData) {
	        questionMap.put("q" + question.get("id"), question);
	    }

	    // Enrich feedback data with question English and Tamil fields
	    for (Map<String, Object> feedback : feedbackData) {
	        for (int i = 1; i <= 7; i++) { // Assuming there are 7 questions (q1 to q7)
	            String questionKey = "q" + i;
	            if (feedback.containsKey(questionKey)) {
	                Map<String, Object> question = questionMap.get(questionKey);
	                if (question != null) {
	                    feedback.put(questionKey + "_english", question.get("q_english"));
	                    feedback.put(questionKey + "_tamil", question.get("q_tamil"));
	                } else {
	                    feedback.put(questionKey + "_english", null);
	                    feedback.put(questionKey + "_tamil", null);
	                }
	            }
	        }
	    }

	    // Prepare response
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", 200);
	    if (feedbackData.isEmpty()) {
	        response.put("message", "No data found.");
	        response.put("data", Collections.emptyList());
	    } else {
	        response.put("message", "Feedback list report.");
	        response.put("data", feedbackData);
	    }

	    return Collections.singletonList(response);
	}
	*/
}
