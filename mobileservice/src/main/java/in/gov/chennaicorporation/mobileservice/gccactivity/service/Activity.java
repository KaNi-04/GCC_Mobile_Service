package in.gov.chennaicorporation.mobileservice.gccactivity.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.*;

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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class Activity {
	private JdbcTemplate jdbcActivityTemplate;
	private DateTimeUtil dateTimeUtil;
	private final Environment environment;
	private String fileBaseUrl;
	@Autowired
	public void setDataSource(@Qualifier("mysqlActivityDataSource") DataSource activityDataSource) {
		this.jdbcActivityTemplate = new JdbcTemplate(activityDataSource);
	}
	
	@Autowired
	public Activity(Environment environment) {
		this.environment = environment;
		this.fileBaseUrl=environment.getProperty("fileBaseUrl");
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
	
	@Transactional
	public List<Map<String, Object>> getActivityCategory(String departmentId){
		String sqlQuery = "SELECT * FROM `activity_category` WHERE (`isactive`=1 AND `isdelete`=0) ";
		
		if(departmentId!=null && !departmentId.isEmpty() && !departmentId.isBlank()) {
			sqlQuery= sqlQuery +" AND `mappingid`='"+departmentId+"'";
		}
		sqlQuery = sqlQuery + " ORDER BY `display_order`, `name` ASC";
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery);
		
		return result;
	}
	
	@Transactional
	public int checkAssetExistsCount(String categoryId, String streetId, String latitude, String longitude) {
	    String sqlQuery = "SELECT COUNT(*) " +
	            "FROM ( " +
	            "   SELECT id, ST_Distance_Sphere( " +
	            " 		location, ST_GeomFromText(CONCAT('POINT(', ?, ' ', ?, ')'), 4326) " +
	            " 	) AS distance_meters " +
	            "    FROM asset_list " +
	            "    WHERE streetid = ? " +
	            "      AND category_id = ? " +
	            "      AND isactive = 1 " +
	            "      AND cby <> 0 " +
	            "    HAVING distance_meters <= 3 " +
	            ") AS nearby_assets";

	    return jdbcActivityTemplate.queryForObject(
	        sqlQuery, Integer.class,
	        longitude, latitude, 
	        streetId, categoryId
	    );
	}
	
	@Transactional
	public List<Map<String, Object>> saveAsset(
			MultiValueMap<String, String> formData,
			String assetType,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String streeId,
			String streetName,
			String loginId,
			String name,
			MultipartFile file) {
		List<Map<String, Object>> result=null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		String image = assetFileUpload(assetType,file);
		
		String sqlQuery = "INSERT INTO `asset_list`(`category_id`, `image`, `latitude`, `longitude`, `zone`, `ward`, `streetid`, `streetname`, `cby`, `name`,`location`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?, ST_GeomFromText(CONCAT('POINT(', ?, ' ', ?, ')'), 4326))";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
            int affectedRows = jdbcActivityTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, assetType);
                    ps.setString(2, image);
                    ps.setString(3, latitude);
                    ps.setString(4, longitude);
                    ps.setString(5, zone);
                    ps.setString(6, ward);
                    ps.setString(7, streeId);
                    ps.setString(8, streetName);
                    ps.setString(9, loginId);
                    ps.setString(10, name);
                    ps.setString(11, longitude);
                    ps.setString(12, latitude); 
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
	
	public String assetFileUpload(String catId, MultipartFile file) {
		
		int lastInsertId = 0;
		// Set the file path where you want to save it
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("activity_new_asset_foldername");
        uploadDirectory = uploadDirectory + serviceFolderName + catId;
        
        try {
        	System.out.println("file-size: "+file.getBytes().toString());
	        // Create directory if it doesn't exist
	        Path directoryPath = Paths.get(uploadDirectory);
	        if (!Files.exists(directoryPath)) {
	            Files.createDirectories(directoryPath);
	        }
	        
	        // Datetime string
	        String datetimetxt = DateTimeUtil.getCurrentDateTime();
	        // File name 
            String fileName = catId + "_" + datetimetxt + "_" + file.getOriginalFilename();
            fileName = fileName.replaceAll("\\s+", ""); // Remove space on filename
            
	        String filePath = uploadDirectory + "/" + fileName;
	        
	        // Create a new Path object
            Path path = Paths.get(filePath);
            
            // Get the bytes of the file
            byte[] bytes = file.getBytes();
            
            // Compress the image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50% quality
            
            // Write the bytes to the file
            Files.write(path, bytes);
            
            System.out.println(fileName);
            return fileName;
            
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to save file " + file.getOriginalFilename();
        } 
	}

	@Transactional
	public List<Map<String, Object>> loadAssetlistbyStreetId(String assetTypeId, String streetId){
		String serviceFolderName = environment.getProperty("activity_new_asset_foldername");
		String filePath=serviceFolderName + assetTypeId+"/";
		//String sqlQuery = "SELECT *,CONCAT('https://gccservices.chennaicorporation.gov.in/gccofficialapp/files/"+filePath+"', image) AS imageUrl FROM `asset_list` WHERE `category_id`=? AND `streetid`=?";
		
		/*
		String sqlQuery="SELECT al.*, CONCAT('"+fileBaseUrl+"/gccofficialapp/files/asset_images/2', al.image) AS imageUrl, "
				+ "af.feedback_date AS lastfeedbackdate FROM asset_list al "
				+ "LEFT JOIN ( SELECT af1.*, af2.latest_feedback_date FROM activity_feedback af1 "
				+ "INNER JOIN ( SELECT asset_id, MAX(feedback_date) AS latest_feedback_date FROM activity_feedback GROUP BY asset_id ) af2 "
				+ "ON af1.asset_id = af2.asset_id AND af1.feedback_date = af2.latest_feedback_date ) af "
				+ "ON al.id = af.asset_id WHERE al.category_id = ? AND al.streetid = ?";
		*/
		List<Map<String, Object>> result;
		
		if ("2".equals(assetTypeId)) {
		//String startdate = "2025-05-18";
		String startdate = "2025-10-22";
		
		String sqlQuery ="SELECT "
				+ "    al.*, "
				+ "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files/', al.image) AS imageUrl, "
				+ "    DATE_FORMAT(af.latest_feedback_date, '%d-%m-%Y') AS lastfeedbackdate, "
				+ "    EXISTS ("
				+ "        SELECT 1 FROM activity_feedback ab "
				+ "        WHERE ab.asset_id = al.id "
				+ "        AND ab.feedback_id = 0 "
				+ "        AND ab.feedback_date BETWEEN ? AND NOW() "
				+ "    ) AS is_before, "
				+ "    EXISTS ("
				+ "        SELECT 1 FROM activity_feedback aa "
				+ "        WHERE aa.asset_id = al.id "
				+ "        AND aa.feedback_id > 0 "
				+ "        AND aa.feedback_date BETWEEN ? AND NOW() "
				+ "    ) AS is_after "
				+ "FROM asset_list al "
				+ "LEFT JOIN ("
				+ "    SELECT asset_id, MAX(feedback_date) AS latest_feedback_date "
				+ "    FROM activity_feedback "
				+ "    GROUP BY asset_id "
				+ ") af ON al.id = af.asset_id "
				+ "WHERE al.category_id = ? AND al.streetid = ? AND al.isactive=1 AND isdelete=0 ";
		
		result = jdbcActivityTemplate.queryForList(sqlQuery, startdate, startdate, assetTypeId, streetId);
		
		} else {
		
			/*
			String sqlQuery = 
				    "SELECT " +
				    "    al.*, " +
				    "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files/', al.image) AS imageUrl, " +
				    
				    "    DATE_FORMAT(bf.latest_before_date, '%d-%m-%Y') AS lastfeedbackdate, " +
				    
				    "    DATE_FORMAT(bf.latest_before_date, '%d-%m-%Y') AS last_before_date, " +
				    "    CASE WHEN bf.asset_id IS NOT NULL THEN TRUE ELSE FALSE END AS has_before, " +

				    "    DATE_FORMAT(af.latest_after_date, '%d-%m-%Y') AS last_after_date, " +
				    "    CASE WHEN af.asset_id IS NOT NULL THEN TRUE ELSE FALSE END AS has_after " +

				    "FROM asset_list al " +

				    "LEFT JOIN ( " +
				    "    SELECT asset_id, MAX(feedback_date) AS latest_before_date " +
				    "    FROM activity_feedback " +
				    "    WHERE feedback_id = 0 " +
				    "    GROUP BY asset_id " +
				    ") bf ON al.id = bf.asset_id " +

				    "LEFT JOIN ( " +
				    "    SELECT af.asset_id, MAX(af.feedback_date) AS latest_after_date " +
				    "    FROM activity_feedback af " +
				    "    INNER JOIN ( " +
				    "        SELECT asset_id, MAX(feedback_date) AS latest_before_date " +
				    "        FROM activity_feedback " +
				    "        WHERE feedback_id = 0 " +
				    "        GROUP BY asset_id " +
				    "    ) bf_sub ON af.asset_id = bf_sub.asset_id " +
				    "    WHERE af.feedback_id > 0 AND af.feedback_date > bf_sub.latest_before_date " +
				    "    GROUP BY af.asset_id " +
				    ") af ON al.id = af.asset_id " +

				    "WHERE al.category_id = ? AND al.streetid = ?";
				*/
			
			String sqlQuery = 
				    "SELECT " +
				    "    al.*, " +
				    "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files/', al.image) AS imageUrl, " +

				    // Unified last feedback date (same as old query)
				    "    DATE_FORMAT(allf.latest_feedback_date, '%d-%m-%Y') AS lastfeedbackdate, " +

				    // Latest before feedback date
				    "    DATE_FORMAT(bf.latest_before_date, '%d-%m-%Y') AS last_before_date, " +
				    "    CASE WHEN bf.asset_id IS NOT NULL THEN TRUE ELSE FALSE END AS is_before, " +

				    // Latest after feedback date
				    "    DATE_FORMAT(af.latest_after_date, '%d-%m-%Y') AS last_after_date, " +
				    "    CASE WHEN af.asset_id IS NOT NULL THEN TRUE ELSE FALSE END AS is_after " +

				    "FROM asset_list al " +

				    // All feedback: for lastfeedbackdate
				    "LEFT JOIN ( " +
				    "    SELECT asset_id, MAX(feedback_date) AS latest_feedback_date " +
				    "    FROM activity_feedback " +
				    "    GROUP BY asset_id " +
				    ") allf ON al.id = allf.asset_id " +

				    // Latest before feedback (feedback_id = 0)
				    "LEFT JOIN ( " +
				    "    SELECT asset_id, MAX(feedback_date) AS latest_before_date " +
				    "    FROM activity_feedback " +
				    "    WHERE feedback_id = 0 " +
				    "    GROUP BY asset_id " +
				    ") bf ON al.id = bf.asset_id " +

				    // Latest after feedback (feedback_id > 0) after the before feedback
				    "LEFT JOIN ( " +
				    "    SELECT af.asset_id, MAX(af.feedback_date) AS latest_after_date " +
				    "    FROM activity_feedback af " +
				    "    INNER JOIN ( " +
				    "        SELECT asset_id, MAX(feedback_date) AS latest_before_date " +
				    "        FROM activity_feedback " +
				    "        WHERE feedback_id = 0 " +
				    "        GROUP BY asset_id " +
				    "    ) bf_sub ON af.asset_id = bf_sub.asset_id " +
				    "    WHERE af.feedback_id > 0 AND af.feedback_date > bf_sub.latest_before_date " +
				    "    GROUP BY af.asset_id " +
				    ") af ON al.id = af.asset_id " +

				    "WHERE al.category_id = ? AND al.streetid = ? AND al.isactive=1 AND isdelete=0 ";
				
				result = jdbcActivityTemplate.queryForList(sqlQuery, assetTypeId, streetId);
			/*
			String sqlQuery ="SELECT "
				+ "    al.*, "
				+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files/', al.image) AS imageUrl, "
				+ "    DATE_FORMAT(af.latest_feedback_date, '%d-%m-%Y') AS lastfeedbackdate, "
				+ "    CASE "
				+ "        WHEN ab.has_before = 1 THEN TRUE "
				+ "        ELSE FALSE "
				+ "    END AS is_before, "
				+ "    CASE "
				+ "        WHEN aa.has_after = 1 THEN TRUE "
				+ "        ELSE FALSE "
				+ "    END AS is_after "
				+ "FROM "
				+ "    asset_list al "
				+ "LEFT JOIN ( "
				+ "    SELECT "
				+ "        asset_id, "
				+ "        MAX(feedback_date) AS latest_feedback_date "
				+ "    FROM activity_feedback "
				+ "    GROUP BY asset_id "
				+ ") af ON al.id = af.asset_id "
				+ "LEFT JOIN ( "
				+ "    SELECT "
				+ "        id, asset_id, "
				+ "        1 AS has_before "
				+ "    FROM activity_feedback "
				+ "    WHERE feedback_id = 0 "
				+ "    GROUP BY asset_id "
				+ ") ab ON al.id = ab.asset_id "
				+ "LEFT JOIN ( "
				+ "    SELECT "
				+ "        asset_id, "
				+ "        1 AS has_after,"
				+ "		   feedback_id "
				+ "    FROM activity_feedback "
				+ "    WHERE feedback_id > 0 "
				+ "    GROUP BY asset_id "
				+ ") aa ON ab.id = aa.feedback_id "
				+ " WHERE al.category_id = ? AND al.streetid = ?";
				*/
		//result = jdbcActivityTemplate.queryForList(sqlQuery, assetTypeId,streetId);
	}
		
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> loadAssetQuestionlistbyAssetTypeId(String assetTypeId){
		String sqlQuery = "SELECT * FROM `activity_questions` WHERE (`isactive`=1 AND `isdelete`=0) AND `category_id`=? ORDER BY `display_order` ASC";
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery,assetTypeId);
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> saveAssetAction(
			MultiValueMap<String, String> formData,
			String catId,
			String assetId,
			String remarks,
			String latitude,
			String longitude,
			String loginId,
			String feedbackId,
			MultipartFile beforeFile,
			MultipartFile afterFile){
		
		String beforeFileName="";
		String afterFileName="";
		
		List<Map<String, Object>> result=null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		//if(feedbackId.equals("0")) {
			String sqlQuery = "INSERT INTO `activity_feedback`(`asset_id`, `remarks`, `feedback_by`,`latitude`, `longitude`, `feedback_id`) "
					+ "VALUES (?,?,?,?,?,?)";
			
			KeyHolder keyHolder = new GeneratedKeyHolder();
			
			try {
	            int affectedRows = jdbcActivityTemplate.update(new PreparedStatementCreator() {
	                @Override
	                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
	                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
	                    ps.setString(1, assetId);
	                    ps.setString(2, remarks);
	                    ps.setString(3, loginId);
	                    ps.setString(4, latitude);
	                    ps.setString(5, longitude);
	                    ps.setString(6, feedbackId);
	                    return ps;
	                }
	            }, keyHolder);
	
	            if (affectedRows > 0) {
	                Number generatedId = keyHolder.getKey();
	                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
	                System.out.println("Activity inserted successfully! Insert ID: " + generatedId);
	                
	                // Save User Feedback Data
	                saveFeedbackData(formData,lastInsertId);
	                
	                // File Upload call
	                if (beforeFile == null || beforeFile.isEmpty()) {
	                	System.out.println("Before File is empty ");
	                }
	                else {
	                	beforeFileName = fileUpload(catId, "before",lastInsertId, beforeFile);
	                }
	                
	                if (afterFile == null || afterFile.isEmpty()) {
	                	System.out.println("After File is empty ");
	                }
	                else {
	                	afterFileName = fileUpload(catId, "after", lastInsertId, afterFile);
	                }
	                
	                response.put("status", "Success");
	                response.put("message", "Your activity was recorded successfully.");
	                return Collections.singletonList(response);
	                
	            } else {
	                response.put("status", "error");
	                response.put("message", "Failed to insert the activity data.");
	                return Collections.singletonList(response);
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
	            return Collections.singletonList(response);
	        }
		//}
		//return Collections.singletonList(response);
	}
	
	public String saveFeedbackData(MultiValueMap<String, String> formData, int feedbackId) {
		// Process the form data here
        for (String key : formData.keySet()) {
            if (!key.equals("catId")
                    && !key.equals("assetId")
                    && !key.equals("remarks")
                    && !key.equals("latitude")
                    && !key.equals("longitude")
                    && !key.equals("loginId")
                    && !key.equals("feedbackId")
                    && !key.equals("beforeFile")
                    && !key.equals("afterFile")) {
                List<String> values = formData.get(key);
                // Remove empty values
                List<String> filteredValues = values.stream()
                        .filter(value -> !value.isEmpty())
                        .map(String::trim)
                        .collect(Collectors.toList());

                // Do something with the field values
                if (!filteredValues.isEmpty()) {
                    //String feedback = filteredValues.toString().trim();
                    String feedback = String.join(" ", filteredValues).trim();
                    String[] parts = key.split("_");
                    int lastInsertId = 0;
                    
                    if (parts.length == 2) {
                        String questionType = parts[0];
                        String questionId = parts[1];
                        if (questionId != null && !questionId.isEmpty() && !questionId.isBlank()) {
                            // Insert Data
                        	String sqlQuery = "INSERT INTO `activity_feedback_data`(`feedback_id`, `qid`, `feedback`) "
                        			+ "VALUES (?,?,?)";
                        	KeyHolder keyHolder = new GeneratedKeyHolder();
                 			
                 			try {
                 	            int affectedRows = jdbcActivityTemplate.update(new PreparedStatementCreator() {
                 	                @Override
                 	                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                 	                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                 	                    ps.setInt(1, feedbackId);
                 	                    ps.setString(2, questionId);
                 	                    ps.setString(3, feedback);
                 	                    return ps;
                 	                }
                 	            }, keyHolder);
                 	           
                 	            if (affectedRows > 0) {
                 	            	Number generatedId = keyHolder.getKey();
                	                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                 	                System.out.println("Activity Feedback inserted successfully! Insert Feedback Data ID: " + lastInsertId);
                 	            }
                 			} catch (DataAccessException e) {
                 	            System.out.println("File insert Feedback Data.");
                 	            Throwable rootCause = e.getMostSpecificCause();
                 	            if (rootCause instanceof SQLException) {
                 	                SQLException sqlException = (SQLException) rootCause;
                 	                System.out.println("SQL State: " + sqlException.getSQLState());
                 	                System.out.println("Error Code: " + sqlException.getErrorCode());
                 	                System.out.println("Message: " + sqlException.getMessage());
                 	            } else {
                 	                System.out.println("Message: " + rootCause.getMessage());
                 	            }
                 	        }
                        }
                    }
                }
            }
        }
		return "success";
	}
	
	public String fileUpload(String catId, String fileAction, int lastInsertfeedbackId, MultipartFile file) {
		
		int lastInsertId = 0;
		// Set the file path where you want to save it
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("activity_foldername");
        int feedbackId=lastInsertfeedbackId;
        uploadDirectory = uploadDirectory + serviceFolderName + catId;
        
        try {
	        // Create directory if it doesn't exist
	        Path directoryPath = Paths.get(uploadDirectory);
	        if (!Files.exists(directoryPath)) {
	            Files.createDirectories(directoryPath);
	        }
	        
	        // Datetime string
	        String datetimetxt = DateTimeUtil.getCurrentDateTime();
	        // File name 
            String OrgfileName = catId + "_" + fileAction + "_" + lastInsertfeedbackId + "_" + datetimetxt + "_" + file.getOriginalFilename();
            String fileName = OrgfileName.replaceAll("\\s+", ""); // Remove space on filename
            
	        String filePath = uploadDirectory + "/" + fileName;

            // Create a new Path object
            Path path = Paths.get(filePath);
            
            // Get the bytes of the file
            byte[] bytes = file.getBytes();
            

            // Compress the image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50% quality
            
            
            // Write the bytes to the file
            Files.write(path, bytes);
            
            //File Size
            Long fileSize = file.getSize();
            
            // File Type
            String fileType = file.getContentType();
            
            String sqlQuery="INSERT INTO `activity_feedback_file`(`feedback_id`, `fileaction`, `filepath`, `filename`, `filetype`, `filesize`) "
            		+ "VALUES (?,?,?,?,?,?)";
            
            KeyHolder keyHolder = new GeneratedKeyHolder();
			
			try {
	            int affectedRows = jdbcActivityTemplate.update(new PreparedStatementCreator() {
	                @Override
	                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
	                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
	                    ps.setInt(1, lastInsertfeedbackId);
	                    ps.setString(2, fileAction);
	                    ps.setString(3, filePath);
	                    ps.setString(4, fileName);
	                    ps.setString(5, fileType);
	                    ps.setLong(6, fileSize);
	                    return ps;
	                }
	            }, keyHolder);
	
	            if (affectedRows > 0) {
	                Number generatedId = keyHolder.getKey();
	                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
	                System.out.println("Activity File inserted successfully! Insert File ID: " + generatedId);
	            }
			} catch (DataAccessException e) {
	            System.out.println("File insert failed.");
	            Throwable rootCause = e.getMostSpecificCause();
	            if (rootCause instanceof SQLException) {
	                SQLException sqlException = (SQLException) rootCause;
	                System.out.println("SQL State: " + sqlException.getSQLState());
	                System.out.println("Error Code: " + sqlException.getErrorCode());
	                System.out.println("Message: " + sqlException.getMessage());
	            } else {
	                System.out.println("Message: " + rootCause.getMessage());
	            }
	        }
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to save file " + file.getOriginalFilename();
        }
		return "File Saved succussfully!";
	}
	
	@Transactional
	public List<Map<String, Object>> loadAssetLastFeedbackHistoryByUser(String catId, String assetId, String loginId) {
	    // Query to get the last feedback by user for the given asset
		List<Map<String, Object>> result;
	    if ("2".equals(catId)) {
		    String sqlQuery = "SELECT * FROM `activity_feedback` WHERE (`isactive`=1 AND `isdelete`=0) "
		                    + "AND (`asset_id`=? "
		                   // + "AND `feedback_by`=?"
		                    + ") "
		                    + "ORDER BY `id` DESC LIMIT 1";
		    
		    // Execute the query and get the result
		    result = jdbcActivityTemplate.queryForList(sqlQuery, assetId);
			}
	    else {
	    	String sqlQuery = "SELECT * FROM `activity_feedback` WHERE (`isactive`=1 AND `isdelete`=0) "
                    + "AND (`asset_id`=? AND `feedback_by`=?) ORDER BY `feedback_date` DESC LIMIT 1";
    
	    	// Execute the query and get the result
	    	result = jdbcActivityTemplate.queryForList(sqlQuery, assetId, loginId);
	    }
	    
	    String feedback_id = "0";
	    if (result.isEmpty()) {
	    	return result;
	    }else {
	    	// Get the feedback_id from the first result
		    feedback_id = result.get(0).get("id").toString();
		    String feedback_id_before = result.get(0).get("feedback_id").toString();
		    
		    // Query to get feedback data for the feedback_id
		    String sqlQuery2 = "SELECT *,`activity_questions`.english,`activity_questions`.tamil FROM `activity_feedback_data`, `activity_questions` WHERE feedback_id=? "
		    		+ "AND `activity_questions`.qid = `activity_feedback_data`.`qid`";
		    List<Map<String, Object>> result2 = jdbcActivityTemplate.queryForList(sqlQuery2, feedback_id);
		    
		    // Query to get feedback files for the feedback_id 
		    //uploadDirectory = uploadDirectory + serviceFolderName + catId;
		    String serviceFolderName = environment.getProperty("activity_foldername");
			String filePath=serviceFolderName + catId+"/";
		    
			String sqlQuery3 = "SELECT *,CONCAT('"+fileBaseUrl+"/gccofficialapp/files/"+filePath+"', filename) AS imageUrl "
					+ "FROM `activity_feedback_file` WHERE feedback_id=? or feedback_id=?";
		    List<Map<String, Object>> result3 = jdbcActivityTemplate.queryForList(sqlQuery3, feedback_id, feedback_id_before);
		   
		    // Add the result2 and result3 to the main result map
		    result.get(0).put("feedback_value", result2);
		    result.get(0).put("feedback_file", result3);
		    
		    return result;
	    }
	    
	}
	
	@Transactional
	public List<Map<String, Object>> loadAssetLastFeedbackByUser(String catId, String assetId, String loginId) {
	    // Query to get the last feedback by user for the given asset
		List<Map<String, Object>> result;
		if ("2".equals(catId)) {
	    String sqlQuery = "SELECT * FROM `activity_feedback` WHERE (`isactive`=1 AND `isdelete`=0) "
	                    + "AND (`asset_id`=? "
	                   // + "AND `feedback_by`=?"
	                    + ") "
	                    + "ORDER BY `id` DESC LIMIT 1";
	    
	    // Execute the query and get the result
	    result = jdbcActivityTemplate.queryForList(sqlQuery, assetId);
		}
		else {
			String sqlQuery = "SELECT * FROM `activity_feedback` WHERE (`isactive`=1 AND `isdelete`=0) "
                    + "AND (`asset_id`=? "
                    + "AND `feedback_by`=?"
                    + ") "
                    + "ORDER BY `id` DESC LIMIT 1";
    
			// Execute the query and get the result
			result = jdbcActivityTemplate.queryForList(sqlQuery, assetId, loginId);
		}
	    
	    String feedback_id = "0";
	    if (result.isEmpty()) {
	    	return result;
	    }else {
	    	// Get the feedback_id from the first result
		    feedback_id = result.get(0).get("id").toString();
	    }
	    // Query to get feedback data for the feedback_id
	    String sqlQuery2 = "SELECT * FROM `activity_feedback_data` WHERE feedback_id=?";
	    List<Map<String, Object>> result2 = jdbcActivityTemplate.queryForList(sqlQuery2, feedback_id);
	    
	    // Query to get feedback files for the feedback_id 
	    //uploadDirectory = uploadDirectory + serviceFolderName + catId;
	    String serviceFolderName = environment.getProperty("activity_foldername");
		String filePath=serviceFolderName + catId+"/";
	    String sqlQuery3 = "SELECT *,CONCAT('"+fileBaseUrl+"/gccofficialapp/files/"+filePath+"', filename) AS imageUrl FROM `activity_feedback_file` WHERE feedback_id=?";
	    List<Map<String, Object>> result3 = jdbcActivityTemplate.queryForList(sqlQuery3, feedback_id);
	    
	    // Add the result2 and result3 to the main result map
	    result.get(0).put("feedback_value", result2);
	    result.get(0).put("feedback_file", result3);
	    
	    return result;
	}
	
	@Transactional
	public List<Map<String, Object>> loadAssetFeedback(String catId, String assetId, String loginId) {
	    // Query to get the last feedback by user for the given asset
	    String sqlQuery = "SELECT * FROM `activity_feedback` WHERE (`isactive`=1 AND `isdelete`=0) "
	                    + "AND (`asset_id`=? AND `feedback_by`=?) ORDER BY `feedback_date` DESC LIMIT 1";
	    
	    // Execute the query and get the result
	    List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery, assetId, loginId);
	    
	    String feedback_id = "0";
	    if (result.isEmpty()) {
	    	return result;
	    }else {
	    	// Get the feedback_id from the first result
		    feedback_id = result.get(0).get("id").toString();
	    }
	    // Query to get feedback data for the feedback_id
	    String sqlQuery2 = "SELECT * FROM `activity_feedback_data` WHERE feedback_id=?";
	    List<Map<String, Object>> result2 = jdbcActivityTemplate.queryForList(sqlQuery2, feedback_id);
	    
	    // Query to get feedback files for the feedback_id 
	    //uploadDirectory = uploadDirectory + serviceFolderName + catId;
	    String serviceFolderName = environment.getProperty("activity_foldername");
		String filePath=serviceFolderName + catId+"/";
	    String sqlQuery3 = "SELECT *,CONCAT('"+fileBaseUrl+"/gccofficialapp/files/"+filePath+"', filename) AS imageUrl FROM `activity_feedback_file` WHERE feedback_id=?";
	    List<Map<String, Object>> result3 = jdbcActivityTemplate.queryForList(sqlQuery3, feedback_id);
	    
	    // Add the result2 and result3 to the main result map
	    result.get(0).put("feedback_value", result2);
	    result.get(0).put("feedback_file", result3);
	    
	    return result;
	}
	/*
	public int getZoneAssetCount(int catid, String zone) {
        String sqlQuery = "SELECT SUM(`totalcount`) as Total FROM `asset_count_master` WHERE catid=? and zone=?";
        
        List<Map<String, Object>> taskResult = jdbcActivityTemplate.queryForList(sqlQuery, catid, zone);
        
        if (!taskResult.isEmpty() && taskResult.get(0).get("Total") != null) {
            return ((Number) taskResult.get(0).get("Total")).intValue();
        } else {
            return 0;
        }
    }
    
    public int getZoneAssetCount(int catid, String zone, String ward) {
        String sqlQuery = "SELECT SUM(`totalcount`) as Total FROM `asset_count_master` WHERE catid=? AND zone=? AND ward=?";
        
        List<Map<String, Object>> taskResult = jdbcActivityTemplate.queryForList(sqlQuery, catid, zone, ward);
        
        if (!taskResult.isEmpty() && taskResult.get(0).get("Total") != null) {
            return ((Number) taskResult.get(0).get("Total")).intValue();
        } else {
            return 0;
        }
    }
	*/
    private int getInt(Object val) {
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }
    
	// For report and Dashboard
    


	@Transactional
	public Map<String, Object> filterReports(
	        String assetType, 
	        String fromDate, 
	        String toDate,
	        String zone,
	        String ward,
	        String streetid,
	        String loginId) {
		String groupQuery="al.zone";
		
		// Add additional filters if provided
	    if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
	    	groupQuery+=",al.ward";
	    }
	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
	    	groupQuery+=",al.streetid,al.streetname";
	    }
	    if (streetid != null && !streetid.isEmpty() && !streetid.isBlank()) {
	        //sqlQuery.append(" AND al.streetid = ?");
	    }
	    
	    String ExtraSQL="    COALESCE(SUM(CASE WHEN aff.fileaction = 'before' THEN 1 ELSE 0 END), 0) AS total_started, "
	            		+ "    COALESCE(SUM(CASE WHEN aff.fileaction = 'after' THEN 1 ELSE 0 END), 0) AS total_completed, ";
	   // if(assetType.equalsIgnoreCase("2")) {
	    if ("2".equals(assetType)) {
	    	ExtraSQL="    0 AS total_started, "
            		+ "    COALESCE(SUM(CASE WHEN aff.fileaction IN ('after', 'before') THEN 1 ELSE 0 END), 0) AS total_completed, ";
	    }
		
	    // Start constructing the SQL query
	    StringBuilder sqlQuery = new StringBuilder("SELECT "
	    		
	            + groupQuery
	            + "    ,COUNT( CONCAT(al.latitude, ',', al.longitude)) AS total_assets, "
	            //+ "    ,COUNT(DISTINCT CONCAT(al.latitude, ',', al.longitude)) AS total_assets, "    
	            //+ "    COALESCE(SUM(CASE WHEN aff.fileaction = 'before' THEN 1 ELSE 0 END), 0) AS total_started, "
	            //+ "    COALESCE(SUM(CASE WHEN aff.fileaction = 'after' THEN 1 ELSE 0 END), 0) AS total_completed, "
	            + ExtraSQL
	            + "    COALESCE(COUNT(DISTINCT al.id) - "
	            + "        SUM(CASE WHEN aff.fileaction = 'before' THEN 1 ELSE 0 END) - "
	            + "        SUM(CASE WHEN aff.fileaction = 'after' THEN 1 ELSE 0 END), 0) AS total_pending "
	            + "FROM "
	            + "    asset_list al "
	            + "LEFT JOIN "
	            + "    (SELECT "
	            + "         af.id, "
	            + "         af.asset_id "
	            + "     FROM "
	            + "         activity_feedback af "
	            + "     INNER JOIN "
	            + "         (SELECT asset_id, MAX(feedback_date) AS latest_feedback_date "
	            + "          FROM activity_feedback "
	            + "          WHERE isactive=1 ");
	    		
	    
	    // Add date range filter if both fromDate and toDate are provided
	    if (fromDate != null && !fromDate.trim().isEmpty() &&
	    	    toDate != null && !toDate.trim().isEmpty()) {
	        sqlQuery.append(" AND DATE(feedback_date) BETWEEN ? AND ?");
	    }
	    
	    sqlQuery.append("          GROUP BY asset_id) latest_feedback_dates "
	            + "     ON "
	            + "         af.asset_id = latest_feedback_dates.asset_id "
	            + "         AND af.feedback_date = latest_feedback_dates.latest_feedback_date "
	            + "    ) latest_feedback ON al.id = latest_feedback.asset_id "
	            + "LEFT JOIN "
	            + "    activity_feedback_file aff ON latest_feedback.id = aff.feedback_id "
	            + "WHERE 1=1 ");
	    
	    // Add additional filters if provided
	    if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
	        sqlQuery.append(" AND al.zone = ? ");
	    }
	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
	        sqlQuery.append(" AND al.ward = ? ");
	    }
	    if (streetid != null && !streetid.isEmpty() && !streetid.isBlank()) {
	        sqlQuery.append(" AND al.streetid = ? ");
	    }
	    if (assetType != null && !assetType.isEmpty() && !assetType.isBlank()) {
	        sqlQuery.append(" AND al.category_id = ? ");
	    }
	    
	    sqlQuery.append(" AND al.isactive = 1 AND al.isdelete = 0 ");
	    
	    /*
	    if(assetType != null && !assetType.isEmpty() && !assetType.isBlank() && "2".equals(assetType))
	    {
	    	sqlQuery.append(" AND ((DATE(al.cdate) > '2024-08-01' AND DATE(al.cdate) <= '2025-06-12' AND al.zone IN (1,2,3,4,5,6,7)) OR"
	    			+ "			    		    (DATE(al.cdate) > '2025-05-01' AND DATE(al.cdate) <= '2025-06-12' AND al.zone = 8) OR"
	    			+ "			    		    (DATE(al.cdate) > '2025-05-20' AND DATE(al.cdate) <= '2025-06-12' AND al.zone = 9) OR"
	    			+ "			    		    (DATE(al.cdate) > '2024-09-06' AND DATE(al.cdate) <= '2025-06-12' AND al.zone IN (10)) OR"
	    			+ "			    		    (DATE(al.cdate) > '2024-10-01' AND DATE(al.cdate) <= '2025-06-12' AND al.zone IN (11)) OR"
	    			+ "			    			(DATE(al.cdate) > '2024-09-01' AND DATE(al.cdate) <= '2025-06-12' AND al.zone IN (12)) OR"
	    			+ "			    		    (DATE(al.cdate) > '2024-05-03' AND DATE(al.cdate) <= '2025-06-12' AND al.zone IN (13,14,15))"
	    			+ "			    		  )");
	    }
	    */
	    
	    // Group by zone
	    sqlQuery.append(" GROUP BY "+groupQuery);
	    
	 // Add HAVING clause to filter out rows where total_started and total_completed are both zero
	    sqlQuery.append(" HAVING (COALESCE(SUM(CASE WHEN aff.fileaction = 'before' THEN 1 ELSE 0 END), 0) > 0 OR "
	                    + "COALESCE(SUM(CASE WHEN aff.fileaction = 'after' THEN 1 ELSE 0 END), 0) > 0)");
	    
	    // Prepare parameters
	    List<Object> params = new ArrayList<>();
	    if (fromDate != null && !fromDate.trim().isEmpty() &&
	    	    toDate != null && !toDate.trim().isEmpty()) {
	    	System.out.println("filterReports:");
	    	System.out.println("From Date:" + fromDate);
	    	System.out.println("To Date:" + toDate);
	    	
	    	fromDate = convertDateFormat(fromDate,0);
	    	toDate = convertDateFormat(toDate,0);
	    	
	    	//fromDate =  fromDate + " 00:00:00";
	    	//toDate =  toDate + " 23:59:59";
	    	
	        params.add(fromDate);
	        params.add(toDate);
	        System.out.println("After - filterReports:");
	    	System.out.println("After From Date:" + fromDate);
	    	System.out.println("After To Date:" + toDate);
	    }
	    if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
	        params.add(zone);
	    }
	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
	        params.add(ward);
	    }
	    if (streetid != null && !streetid.isEmpty() && !streetid.isBlank()) {
	        params.add(streetid);
	    }
	    if (assetType != null && !assetType.isEmpty() && !assetType.isBlank()) {
	        params.add(assetType);
	    }
	    
	    // Execute query
	    System.out.println(sqlQuery.toString()); // For debugging
	    
	    List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery.toString(), params.toArray());
	    
	    if ("2".equals(assetType)) {
	    	if ((zone == null || zone.isBlank()) && (ward == null || ward.isBlank())) {
	    	
		        String sqlMaster = "SELECT zone, SUM(totalcount) as master_total FROM asset_count_master WHERE catid = 2 GROUP BY zone";
		        List<Map<String, Object>> masterTotals = jdbcActivityTemplate.queryForList(sqlMaster);
		        
		        // Convert to Map for fast lookup by zone
		        Map<String, Integer> masterMap = new HashMap<>();
		        for (Map<String, Object> row : masterTotals) {
		            String zoneName = (String) row.get("zone");
		            Number count = (Number) row.get("master_total");
		            masterMap.put(zoneName, count != null ? count.intValue() : 0);
		        }

		        // Merge into zoneWise result
		        for (Map<String, Object> zoneEntry : result) {
		            String zoneName = (String) zoneEntry.get("zone");
		            Integer masterTotal = masterMap.getOrDefault(zoneName, 0);
		            zoneEntry.put("master_total", masterTotal);
		        }
		   
	    	}
		        
	    	if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
	    	
		        String sqlMaster2 = "SELECT division as ward, SUM(totalcount) as master_total FROM asset_count_master WHERE catid = 2 AND zone = ? GROUP BY division;";
		        List<Map<String, Object>> wardmasterTotals = jdbcActivityTemplate.queryForList(sqlMaster2,zone);
		        
		        // Convert to Map for fast lookup by ward
		        Map<String, Integer> masterMapWard = new HashMap<>();
		        for (Map<String, Object> row : wardmasterTotals) {
		            String wardName = (String) row.get("ward");
		            Number count = (Number) row.get("master_total");
		            masterMapWard.put(wardName, count != null ? count.intValue() : 0);
		        }

		        // Merge into zoneWise result
		        for (Map<String, Object> zoneEntry : result) {
		            String wardName = (String) zoneEntry.get("ward");
		            Integer masterTotal = masterMapWard.getOrDefault(wardName, 0);
		            zoneEntry.put("master_total", masterTotal);
		        }
		    }
	    }
	    
	    for (Map<String, Object> masterEntry : result) {
	        Number totalAssets = (Number) masterEntry.get("total_assets");
	        Number masterTotal = (Number) masterEntry.get("master_total");

	        int total = totalAssets != null ? totalAssets.intValue() : 0;
	        int master = masterTotal != null ? masterTotal.intValue() : 0;

	        int remainingAssets = master - total;
	        masterEntry.put("remaining_assets", remainingAssets);
	    }
	    if ((streetid == null || streetid.isBlank()) && (ward == null || ward.isBlank())) {
		    Map<String, Object> footerTotal = new HashMap<>();
		    footerTotal.put("zone", "Total");
		    footerTotal.put("ward", "Total");
	
		    int totalAssetsSum = 0;
		    int totalStartedSum = 0;
		    int totalCompletedSum = 0;
		    int totalPendingSum = 0;
		    int masterTotalSum = 0;
		    int remainingAssetsSum = 0;
	
		    for (Map<String, Object> entry : result) {
		        totalAssetsSum += getInt(entry.get("total_assets"));
		        totalStartedSum += getInt(entry.get("total_started"));
		        totalCompletedSum += getInt(entry.get("total_completed"));
		        totalPendingSum += getInt(entry.get("total_pending"));
		        masterTotalSum += getInt(entry.get("master_total"));
		        remainingAssetsSum += getInt(entry.get("remaining_assets"));
		    }
	
		    footerTotal.put("total_assets", totalAssetsSum);
		    footerTotal.put("total_started", totalStartedSum);
		    footerTotal.put("total_completed", totalCompletedSum);
		    footerTotal.put("total_pending", totalPendingSum);
		    footerTotal.put("master_total", masterTotalSum);
		    footerTotal.put("remaining_assets", remainingAssetsSum);
	
		    // Append to result
		    result.add(footerTotal);
		}
	 // --- Overall totals from zoneWise ---
	    int totalAssetsSum = 0;
	    int totalStartedSum = 0;
	    int totalCompletedSum = 0;
	    int totalPendingSum = 0;
	    int masterTotalSum = 0;
	    int remainingAssetsSum = 0;

	    for (Map<String, Object> entry : result) {
	        totalAssetsSum    += getInt(entry.get("total_assets"));
	        totalStartedSum   += getInt(entry.get("total_started"));
	        totalCompletedSum += getInt(entry.get("total_completed"));
	        totalPendingSum   += getInt(entry.get("total_pending"));
	        masterTotalSum    += getInt(entry.get("master_total"));
	        remainingAssetsSum+= getInt(entry.get("remaining_assets"));
	    }

	    Map<String, Object> overallTotals = new HashMap<>();
	    overallTotals.put("total_assets", totalAssetsSum);
	    overallTotals.put("total_started", totalStartedSum);
	    overallTotals.put("total_completed", totalCompletedSum);
	    overallTotals.put("total_pending", totalPendingSum);
	    overallTotals.put("master_total", masterTotalSum);
	    overallTotals.put("remaining_assets", remainingAssetsSum);

	    // --- Final result ---
	    Map<String, Object> result2 = new HashMap<>();
	    result2.put("zoneWise", result);
	    result2.put("overallTotals", overallTotals);

	    return result2;
	    /*
		 	// Combine results
		    Map<String, Object> result2 = new HashMap<>();
		    result2.put("zoneWise", result);
		    result2.put("overallTotals", overallResult.isEmpty() ? Collections.singletonMap("total_started", 0) : overallResult.get(0));
		    
		    
	    return result2;*/
	}
	
    
	
	@Transactional
	public List<Map<String, Object>> filterReportsByDate(
			String assetType, 
	        String fromDate, 
	        String toDate,
	        String zone,
	        String ward,
	        String streetid,
	        String loginId) {
		
	    // Step 1: Generate all dates within the specified month and year
	    String sqlQueryForTasks = "SELECT "
	    		+ "    `asset_list`.`id`, "
	    		+ "    `asset_list`.`category_id`, "
	    		+ "    `asset_list`.`image`, "
	    		+ "    `asset_list`.`latitude`, "
	    		+ "    `asset_list`.`longitude`, "
	    		+ "    `asset_list`.`zone`, "
	    		+ "    `asset_list`.`ward`, "
	    		+ "    `asset_list`.`streetid`, "
	    		+ "    `asset_list`.`streetname`, "
	    		+ "    `asset_list`.`cdate`, "
	    		+ "    `asset_list`.`cby`, "
	    		+ "    `asset_list`.`isactive`, "
	    		+ "    `asset_list`.`isdelete`, "
	    		+ "    `asset_list`.`gis_id`, "
	    		+ "    GROUP_CONCAT(`activity_feedback`.`id`) AS feedback_ids "
	    		+ "FROM "
	    		+ "    `activity_feedback` "
	    		+ "JOIN "
	    		+ "    `asset_list` "
	    		+ "    ON `asset_list`.`id` = `activity_feedback`.`asset_id` "
	    		+ "WHERE "
	    		+ "    DATE(`activity_feedback`.`feedback_date`) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
	    		+ "    AND `asset_list`.`category_id` = ? "
	    		+ "    AND `asset_list`.`streetid` = ? "
	    		+ "    AND `asset_list`.`isactive` = 1 "
	    		+ "    AND `asset_list`.`isdelete` = 0 "
	    		+ "    AND `activity_feedback`.`isactive` = 1 "
	    		+ "GROUP BY "
	    		+ "    `asset_list`.`id`";
	    
	 
	    System.out.println(sqlQueryForTasks);
	    // Execute the query to get the tasks
	    List<Map<String, Object>> taskResult = jdbcActivityTemplate.queryForList(sqlQueryForTasks, fromDate, toDate, assetType, streetid);

	    // Step 2: For each task, load the detailed feedback similar to loadAssetFeedback method
	    for (Map<String, Object> task : taskResult) {
	    	
	        String assetId = task.get("id").toString();
	        String catId = task.get("category_id").toString();
	        String feedbackIds = task.get("feedback_ids").toString();
	        
	        // Query to get the last feedback by user for the given asset
	        String sqlQueryForFeedback = "SELECT * FROM `activity_feedback` WHERE (`isactive`=1 AND `isdelete`=0) "
	                                   + "AND (`asset_id`="+assetId+" AND id IN ("+feedbackIds+")) ORDER BY `feedback_date` DESC LIMIT 1";
	        
	        // Execute the query and get the feedback result
	        List<Map<String, Object>> feedbackResult = jdbcActivityTemplate.queryForList(sqlQueryForFeedback);
	        if (!feedbackResult.isEmpty()) {
	            String feedback_id = feedbackResult.get(0).get("id").toString();
	            
	            // Query to get feedback data for the feedback_id
	            String sqlQueryForFeedbackData = "SELECT *,`activity_questions`.english,`activity_questions`.tamil FROM `activity_feedback_data`, `activity_questions` WHERE feedback_id=? "
	            		+ "AND `activity_questions`.qid = `activity_feedback_data`.`qid`";
	            List<Map<String, Object>> feedbackDataResult = jdbcActivityTemplate.queryForList(sqlQueryForFeedbackData, feedback_id);
	            
	            // Query to get feedback files for the feedback_id
	            String serviceFolderName = environment.getProperty("activity_foldername");
	            String filePath = serviceFolderName + catId + "/";
	            String sqlQueryForFeedbackFiles = "SELECT *,CONCAT('"+fileBaseUrl+"/gccofficialapp/files/" + filePath + "', filename) "
	            		+ "AS imageUrl FROM `activity_feedback_file` WHERE feedback_id IN (" + feedbackIds + ")";
	            List<Map<String, Object>> feedbackFileResult = jdbcActivityTemplate.queryForList(sqlQueryForFeedbackFiles);
	            System.out.println(sqlQueryForFeedbackFiles + feedbackIds);
	            // Add feedback data and files to the task result
	            task.put("feedback_value", feedbackDataResult);
	            task.put("feedback_file", feedbackFileResult);
	        }
	    }
	    
	    return taskResult;
	}
	
	
	@Transactional
	public List<Map<String, Object>> dashboardFiletReport(
			String assetType, 
	        String fromDate, 
	        String toDate,
	        String zone,
	        String ward,
	        String streetid,
	        String loginId) {
		
	    // Step 1: Generate all dates within the specified month and year
		StringBuilder sqlQuery = new StringBuilder("SELECT "
	    		+ "    `asset_list`.`id`,"
	    		+ "    `asset_list`.`category_id`, "
	    		+ "    `asset_list`.`image`, "
	    		+ "    `asset_list`.`latitude`, "
	    		+ "    `asset_list`.`longitude`, "
	    		+ "    `asset_list`.`zone`, "
	    		+ "    `asset_list`.`ward`, "
	    		+ "    `asset_list`.`streetid`, "
	    		+ "    `asset_list`.`streetname`, "
	    		+ "    `asset_list`.`cdate`, "
	    		+ "    `asset_list`.`cby`, "
	    		+ "    `asset_list`.`isactive`, "
	    		+ "    `asset_list`.`isdelete`, "
	    		+ "    `asset_list`.`gis_id`, "
	    		+ "    GROUP_CONCAT(`activity_feedback`.`id`) AS feedback_ids "
	    		+ "FROM "
	    		+ "    `activity_feedback` "
	    		+ "JOIN "
	    		+ "    `asset_list` ON `asset_list`.`id` = `activity_feedback`.`asset_id` "
	    		+ "WHERE activity_feedback.isactive=1 ");
	    		
	    		
	    
		// Add additional filters if provided
		if (fromDate != null && toDate != null) {
	        sqlQuery.append(" AND activity_feedback.feedback_date BETWEEN '"+fromDate+"' AND '"+toDate+"' ");
	    }
	    if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
	    	sqlQuery.append(" AND asset_list.zone = '"+zone+"'");
	    }
	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
	    	sqlQuery.append(" AND asset_list.ward = '"+zone+"'");
	    }
	    if (streetid != null && !streetid.isEmpty() && !streetid.isBlank()) {
	    	sqlQuery.append(" AND asset_list.streetid = '"+streetid+"'");
	    }
	    if (assetType != null && !assetType.isEmpty() && !assetType.isBlank()) {
	    	sqlQuery.append(" AND asset_list.category_id = '"+assetType+"'");
	    }
	    
	    sqlQuery.append(" AND (`asset_list`.`isactive`=1 AND `asset_list`.`isdelete`=0) GROUP BY `asset_list`.`id`");
	    
	    System.out.println(sqlQuery);
	    // Execute the query to get the tasks
	    List<Map<String, Object>> taskResult = jdbcActivityTemplate.queryForList(sqlQuery.toString());

	    // Step 2: For each task, load the detailed feedback similar to loadAssetFeedback method
	    for (Map<String, Object> task : taskResult) {
	    	
	        String assetId = task.get("id").toString();
	        String catId = task.get("category_id").toString();
	        String feedbackIds = task.get("feedback_ids").toString();
	        
	        // Query to get the last feedback by user for the given asset
	        String sqlQueryForFeedback = "SELECT * FROM `activity_feedback` WHERE (`isactive`=1 AND `isdelete`=0) "
	                                   + "AND (`asset_id`="+assetId+" AND id IN ("+feedbackIds+")) ORDER BY `feedback_date` DESC LIMIT 1";
	        
	        // Execute the query and get the feedback result
	        List<Map<String, Object>> feedbackResult = jdbcActivityTemplate.queryForList(sqlQueryForFeedback);
	        if (!feedbackResult.isEmpty()) {
	            String feedback_id = feedbackResult.get(0).get("id").toString();
	            
	            // Query to get feedback data for the feedback_id
	            String sqlQueryForFeedbackData = "SELECT *,`activity_questions`.english,`activity_questions`.tamil FROM `activity_feedback_data`, `activity_questions` WHERE feedback_id=? "
	            		+ "AND `activity_questions`.qid = `activity_feedback_data`.`qid`";
	            List<Map<String, Object>> feedbackDataResult = jdbcActivityTemplate.queryForList(sqlQueryForFeedbackData, feedback_id);
	            
	            // Query to get feedback files for the feedback_id
	            String serviceFolderName = environment.getProperty("activity_foldername");
	            String filePath = serviceFolderName + catId + "/";
	            String sqlQueryForFeedbackFiles = "SELECT *,CONCAT('"+fileBaseUrl+"/gccofficialapp/files/" + filePath + "', filename) "
	            		+ "AS imageUrl FROM `activity_feedback_file` WHERE feedback_id IN (" + feedbackIds + ")";
	            List<Map<String, Object>> feedbackFileResult = jdbcActivityTemplate.queryForList(sqlQueryForFeedbackFiles);
	            System.out.println(sqlQueryForFeedbackFiles + feedbackIds);
	            // Add feedback data and files to the task result
	            task.put("feedback_value", feedbackDataResult);
	            task.put("feedback_file", feedbackFileResult);
	        }
	    }
	    
	    return taskResult;
	}
	
	
	/*
	@Transactional
	public List<Map<String, Object>> filterReportsByDate(
			String assetType, 
	        String fromDate, 
	        String toDate,
	        String zone,
	        String ward,
	        String streetid,
	        String loginId) {
		
	    // Step 1: Generate all dates within the specified month and year
	    String sqlQueryForTasks = "SELECT `asset_list`.*, `activity_feedback`.`id` feedbackid "
	    		+ "FROM `activity_feedback`, `asset_list` "
	    		+ "WHERE DATE_FORMAT(`activity_feedback`.`feedback_date`, '%d-%m-%Y') "
	    		+ "      BETWEEN ? AND ? "
	    		+ "AND `asset_list`.`id` = `activity_feedback`.`asset_id` "
	    		+ "AND `asset_list`.`category_id`=? "
	    		+ "AND `asset_list`.`streetid` = ? ";
	   System.out.println(sqlQueryForTasks);
	    // Execute the query to get the tasks
	    List<Map<String, Object>> taskResult = jdbcActivityTemplate.queryForList(sqlQueryForTasks, fromDate, toDate, assetType, streetid);

	    // Step 2: For each task, load the detailed feedback similar to loadAssetFeedback method
	    for (Map<String, Object> task : taskResult) {
	        String assetId = task.get("id").toString();
	        String feedbackId = task.get("feedbackid").toString();
	        
	        // Query to get the last feedback by user for the given asset
	        String sqlQueryForFeedback = "SELECT * FROM `activity_feedback` WHERE (`isactive`=1 AND `isdelete`=0) "
	                                   + "AND (`asset_id`=? AND id=?) ORDER BY `feedback_date` DESC LIMIT 1";
	        
	        // Execute the query and get the feedback result
	        List<Map<String, Object>> feedbackResult = jdbcActivityTemplate.queryForList(sqlQueryForFeedback, assetId, feedbackId);
	        
	        if (!feedbackResult.isEmpty()) {
	            String feedback_id = feedbackResult.get(0).get("id").toString();
	            
	            // Query to get feedback data for the feedback_id
	            String sqlQueryForFeedbackData = "SELECT *,`activity_questions`.english,`activity_questions`.tamil FROM `activity_feedback_data`, `activity_questions` WHERE feedback_id=? "
	            		+ "AND `activity_questions`.qid = `activity_feedback_data`.`qid`";
	            List<Map<String, Object>> feedbackDataResult = jdbcActivityTemplate.queryForList(sqlQueryForFeedbackData, feedback_id);
	            
	            // Query to get feedback files for the feedback_id
	            String serviceFolderName = environment.getProperty("activity_foldername");
	            String filePath = serviceFolderName + assetId + "/";
	            String sqlQueryForFeedbackFiles = "SELECT *,CONCAT('https://gccservices.chennaicorporation.gov.in/gccofficialapp/files/" + filePath + "', filename) AS imageUrl FROM `activity_feedback_file` WHERE feedback_id=?";
	            List<Map<String, Object>> feedbackFileResult = jdbcActivityTemplate.queryForList(sqlQueryForFeedbackFiles, feedback_id);
	            
	            // Add feedback data and files to the task result
	            task.put("feedback_value", feedbackDataResult);
	            task.put("feedback_file", feedbackFileResult);
	        }
	    }
	    
	    return taskResult;
	}
	*/
	
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
    
    public String getApiResponse() {
    	//String url="https://media.smsgupshup.com/GatewayAPI/rest?userid=2000233507&password=h2YjFNcJ&send_to=9176617754&v=1.1&format=json&msg_type=TEXT&method=SENDMESSAGE&msg=Welcome+to+GCC%21+%0A%0AYour+OTP+for+GCC+Community+Centre+is+36837+.";
        String url="https://media.smsgupshup.com/GatewayAPI/rest?userid=2000233507&password={{PASSWORD}}&send_to=9176617754&v=1.1&format=json&msg_type=TEXT&method=SENDMESSAGE&msg=Welcome+to+GCC%2C+%0A%0AYour+User+Registration+request+for+booking+Community+Centre+submitted+successfully.";
    	RestTemplate restTemplate = new RestTemplate();
        //String response = restTemplate.getForObject(url, String.class);
        
        //String urlString = "https://tmegov.onex-aura.com/api/sms?key=pfTEYN6H&to=9176617754&from=GCCCRP&body=Welcome to GCC! Your OTP for GCC Community Centre booking is 00000&entityid=1401572690000011081&templateid=1407172346444550398";
        
        //String urlString = "https://tmegov.onex-aura.com/api/sms?key=pfTEYN6H&to=9176617754&from=GCCCRP&body=Welcome to GCC, Your User Registration for booking Community Centre is successful.&entityid=1401572690000011081&templateid=1407172346621223019";
        
        HttpURLConnection connection = null;
        
        URL url2;
		try {
			url2 = new URL(url);
        
        connection = (HttpURLConnection) url2.openConnection();

        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        System.out.println("Response Code: " + responseCode);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(urlString);
        //String Response = restTemplate.exchange(url2, String.class);
        
        return "";
    }
    
    // ONlY for SCP 
    /*
    @Transactional
	public Map<String, Object> scpfilterReports(
			String assetType, 
	        String fromDate, 
	        String toDate,
	        String zone,
	        String ward,
	        String streetid,
	        String loginId
			) {
    	
    	System.out.println("SCP REPORT SQL:IN");
    	
    	String groupQuery="al.zone";
    	
		
		// Add additional filters if provided
	    if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
	    	groupQuery="al.ward";
	    }
	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
	    	groupQuery="al.streetid";
	    }
	    
	    String groupQueryClass=groupQuery;
	    
	    String queryCon="";
	    if (assetType != null && !assetType.isEmpty() && !assetType.isBlank()) {
	    	queryCon="al.category_id = '"+assetType+"'";
	    }
	    if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
	    	queryCon+="AND al.zone = '"+zone+"'";
	    }
	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
	    	queryCon+="AND al.ward = '"+ward+"'";
	    	groupQueryClass="al.streetname";
	    }
	    if (streetid != null && !streetid.isEmpty() && !streetid.isBlank()) {
	    	queryCon+="AND al.streetid = '"+streetid+"'";
	    }
	    
	    queryCon+="AND al.isactive = 1 AND al.isdelete = 0";
	    
	    fromDate = convertDateFormat(fromDate,0);
    	toDate = convertDateFormat(toDate,0);
    	
	    
	    StringBuilder sqlQuery = new StringBuilder("SELECT "
	    		+ "		CASE WHEN GROUPING("+groupQuery+") = 1 THEN 'TOTAL' ELSE "+groupQueryClass+" END AS CLASS,"
	    		+ "    	CASE WHEN GROUPING("+groupQuery+") = 1 THEN 'TOTAL' ELSE "+groupQuery+" END AS VARIABLE,"
    			//+ "        COALESCE("+groupQueryClass+", 'TOTAL') AS CLASS,"
    			//+ "        COALESCE("+groupQuery+", 'TOTAL') AS VARIABLE,"
    			+ "        COUNT(al.id) AS Total,"
    			+ "        SUM("
    			+ "            CASE"
    			+ "                WHEN EXISTS ("
    			+ "                    SELECT 1"
    			+ "                    FROM activity_feedback ab"
    			+ "                    WHERE ab.asset_id = al.id"
    			//+ "                      AND ab.feedback_date BETWEEN '"+fromDate+"' AND '"+toDate+"'"
    			+ "               AND ab.feedback_date >= '" + fromDate + "'"
    			+ "               AND ab.feedback_date < DATE_ADD('" + toDate + "', INTERVAL 1 DAY)"
    			+ "                )"
    			+ "                THEN 1 ELSE 0"
    			+ "            END"
    			+ "        ) AS Cleaned,"
    			+ "        SUM("
    			+ "            CASE "
    			+ "                WHEN NOT EXISTS ("
    			+ "                    SELECT 1"
    			+ "                    FROM activity_feedback ab"
    			+ "                    WHERE ab.asset_id = al.id"
    			//+ "                      AND ab.feedback_date BETWEEN '"+fromDate+"' AND '"+toDate+"'"
    			+ "               AND ab.feedback_date >= '" + fromDate + "'"
    			+ "               AND ab.feedback_date < DATE_ADD('" + toDate + "', INTERVAL 1 DAY)"
    			+ "                )"
    			+ "                THEN 1 ELSE 0"
    			+ "            END"
    			+ "        ) AS Pending"
    			+ "    FROM asset_list al"
    			+ "    WHERE "+queryCon+" "
    			+ "    GROUP BY "+groupQuery+" WITH ROLLUP");
    	
	    System.out.println("SCP REPORT SQL:" + sqlQuery.toString());
	    
    	List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery.toString());
    	
    	// --- Final result ---
        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("scpreport", result);
        return finalResult;
    }
    */

    @Transactional
	public Map<String, Object> scpfilterReports(
			String assetType, 
	        String fromDate, 
	        String toDate,
	        String zone,
	        String ward,
	        String streetid,
	        String loginId
			) {
    	
    	System.out.println("SCP REPORT SQL:IN");
    	
    	String groupQuery="al.zone";
    	String having  ="";
		
		// Add additional filters if provided
	    if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
	    	groupQuery="al.ward";
	    }
	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
	    	groupQuery="al.streetid";
	    }
	    
	    String groupQueryClass=groupQuery;
	    String FgroupQuery = groupQuery;
	    String queryCon="";
	    if (assetType != null && !assetType.isEmpty() && !assetType.isBlank()) {
	    	queryCon="al.category_id = '"+assetType+"'";
	    }
	    if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
	    	queryCon+="AND al.zone = '"+zone+"'";
	    }
	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
	    	queryCon+="AND al.ward = '"+ward+"'";
	    	FgroupQuery += ",al.streetname";
	    	groupQueryClass="al.streetname";
	    	having =" HAVING NOT (GROUPING(al.streetid) = 0 AND GROUPING(al.streetname) = 1)";
	    }
	    if (streetid != null && !streetid.isEmpty() && !streetid.isBlank()) {
	    	queryCon+="AND al.streetid = '"+streetid+"'";
	    }
	    
	    queryCon+="AND al.isactive = 1 AND al.isdelete = 0";
	    
	    fromDate = convertDateFormat(fromDate,0);
    	toDate = convertDateFormat(toDate,0);
    	
	    
	    StringBuilder sqlQuery = new StringBuilder("SELECT "
	    		+ "		CASE WHEN GROUPING("+groupQuery+") = 1 THEN 'TOTAL' ELSE "+groupQueryClass+" END AS CLASS,"
	    		+ "    	CASE WHEN GROUPING("+groupQuery+") = 1 THEN 'TOTAL' ELSE "+groupQuery+" END AS VARIABLE,"
    			//+ "        COALESCE("+groupQueryClass+", 'TOTAL') AS CLASS,"
    			//+ "        COALESCE("+groupQuery+", 'TOTAL') AS VARIABLE,"
    			+ "        COUNT(al.id) AS Total,"
    			+ " COALESCE(SUM(CASE WHEN fc.asset_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS Cleaned,"
    			+ " COALESCE(SUM(CASE WHEN fc.asset_id IS NULL THEN 1 ELSE 0 END), 0) AS Pending,"
    			+ "    COALESCE(COUNT(DISTINCT CASE WHEN fc.category = 'Compressor' THEN al.id END), 0) AS Compressor, "
    			+ "    COALESCE(COUNT(DISTINCT CASE WHEN fc.category = 'Manual' THEN al.id END), 0)    AS Manual, "
    			+ "    COALESCE(COUNT(DISTINCT CASE WHEN fc.category = 'Recycle' THEN al.id END), 0)   AS Recycle "
    			+ "    FROM asset_list al "
    			+ "LEFT JOIN ("
    			+ "    SELECT "
    			+ "        af.asset_id,"
    			+ "        CASE "
    			+ "            WHEN SUM(CASE WHEN TRIM(LOWER(afd.feedback)) = 'compressor' THEN 1 ELSE 0 END) > 0 "
    			+ "                 THEN 'Compressor'"
    			+ "            WHEN SUM(CASE WHEN TRIM(LOWER(afd.feedback)) IN ('recycle','yes') THEN 1 ELSE 0 END) > 0"
    			+ "                 THEN 'Recycle'"
    			+ "            WHEN SUM(CASE WHEN TRIM(LOWER(afd.feedback)) IN ('manually','no') THEN 1 ELSE 0 END) > 0"
    			+ "                 THEN 'Manual'"
    			+ "            ELSE 'Manual'"
    			+ "        END AS category"
    			+ "    FROM activity_feedback af"
    			+ "    LEFT JOIN activity_feedback_data afd "
    			+ "        ON afd.feedback_id = af.id AND afd.qid = 220"
    			
    			+ "    WHERE af.feedback_date >= '" + fromDate + "'"
    			+ "      AND af.feedback_date < DATE_ADD('" + toDate + "', INTERVAL 1 DAY)"
    			+ "      AND af.isactive = 1"
    			+ "      AND af.isdelete = 0"
    			+ "    GROUP BY af.asset_id"
    			+ ") fc ON fc.asset_id = al.id"
    			
    			+ "    WHERE "+queryCon+" "
    			+ "    GROUP BY "+FgroupQuery+" WITH ROLLUP"
    					+ having
    			+ "");
    	
	    System.out.println("SCP REPORT SQL:" + sqlQuery.toString());
	    
    	List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery.toString());
    	
    	// --- Final result ---
        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("scpreport", result);
        return finalResult;
    }
    
    @Transactional
    public Map<String, Object> scpReportAssetList(
            String assetType,
            String status,
            String fromDate, 
            String toDate,
            String streetid,
            String loginId
    ) {
    	assetType = "2";
    	
        fromDate = convertDateFormat(fromDate, 0);
        toDate = convertDateFormat(toDate, 0);

        String serviceFolderName = environment.getProperty("activity_foldername");
        String filePath = serviceFolderName + assetType + "/";
        /*
        StringBuilder sqlQuery = new StringBuilder(
        	    "SELECT " +
        	    "    al.id, " +
        	    "    al.zone, " +
        	    "    al.ward, " +
        	    "    al.streetid, " +
        	    "    al.streetname, " +
        	    "    al.latitude, " +
        	    "    al.longitude, " +
        	    "    DATE_FORMAT(MAX(af.feedback_date), '%d-%m-%Y %H:%i') AS last_feedback_date, " +
        	    "    CASE WHEN MAX(af.feedback_date) IS NOT NULL THEN 'Cleaned' ELSE 'Pending' END AS Status, " +
        	    "    GROUP_CONCAT(DISTINCT CASE WHEN aff.fileaction = 'before' THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files/', '" + filePath + "', aff.filename) END SEPARATOR ',') AS before_filepath, " +
        	    "    GROUP_CONCAT(DISTINCT CASE WHEN aff.fileaction = 'after' THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files/', '" + filePath + "', aff.filename) END SEPARATOR ',') AS after_filepath " +
        	    "FROM asset_list al " +
        	    "LEFT JOIN activity_feedback af ON af.asset_id = al.id " +
        	    "    AND af.feedback_date >= ? " +
        	    "    AND af.feedback_date < DATE_ADD(?, INTERVAL 1 DAY) " +
        	    "LEFT JOIN activity_feedback_file aff ON aff.feedback_id = af.id " +
        	    "WHERE al.category_id = 2 " +
        	    "    AND al.streetid = ? " +
        	    "    AND al.isactive = 1 " +
        	    "    AND al.isdelete = 0 " +
        	    "GROUP BY al.id, al.zone, al.ward, al.streetid, al.streetname, al.latitude, al.longitude "
        	);
        */
        /*
        StringBuilder sqlQuery = new StringBuilder(
        		"SELECT "
        		+ "    al.id, "
        		+ "    al.zone, "
        		+ "    al.ward, "
        		+ "    al.streetid, "
        		+ "    al.streetname, "
        		+ "    al.latitude, "
        		+ "    al.longitude, "
        		+ "    DATE_FORMAT(MAX(af.feedback_date), '%d-%m-%Y %H:%i') AS last_feedback_date, "
        		+ "    CASE  "
        		+ "        WHEN  "
        		+ "            SUM(CASE WHEN aff.fileaction = 'before' THEN 1 ELSE 0 END) > 0 "
        		+ "            AND SUM(CASE WHEN aff.fileaction = 'after' THEN 1 ELSE 0 END) > 0 "
        		+ "        THEN 'Cleaned' "
        		+ "        ELSE 'Pending' "
        		+ "    END AS Status, "
        		+ "    GROUP_CONCAT(DISTINCT CASE WHEN aff.fileaction = 'before' THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files/', '" + filePath + "', aff.filename) END SEPARATOR ',') AS before_filepath, "
        	    + "    GROUP_CONCAT(DISTINCT CASE WHEN aff.fileaction = 'after' THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files/', '" + filePath + "', aff.filename) END SEPARATOR ',') AS after_filepath " 
        		
        		+ "FROM asset_list al "
        		+ "LEFT JOIN activity_feedback af  "
        		+ "    ON af.asset_id = al.id "
        		+ "    AND af.feedback_date >= ? "
        		+ "    AND af.feedback_date < DATE_ADD(?, INTERVAL 1 DAY) "
        		+ "LEFT JOIN activity_feedback_file aff  "
        		+ "    ON aff.feedback_id = af.id "
        		+ "WHERE al.category_id = 2 "
        		+ "  AND al.streetid = ? "
        		+ "  AND al.isactive = 1 "
        		+ "  AND al.isdelete = 0 "
        		+ "GROUP BY al.id, al.zone, al.ward, al.streetid, al.streetname, al.latitude, al.longitude "
        		);
        
        */
        StringBuilder sqlQuery = new StringBuilder(
        	    "SELECT " +
        	    "    al.id, " +
        	    "    al.zone, " +
        	    "    al.ward, " +
        	    "    al.streetid, " +
        	    "    al.streetname, " +
        	    "    al.latitude, " +
        	    "    al.longitude, " +
        	    "    DATE_FORMAT(MAX(af.feedback_date), '%d-%m-%Y %H:%i') AS last_feedback_date, " +
        	    "    CASE " +
        	    "        WHEN " +
        	    "            SUM(CASE WHEN aff.fileaction = 'before' THEN 1 ELSE 0 END) > 0 " +
        	    "            AND SUM(CASE WHEN aff.fileaction = 'after' THEN 1 ELSE 0 END) > 0 " +
        	    "        THEN 'Cleaned' " +
        	    "        ELSE 'Pending' " +
        	    "    END AS Status, " +

        	    "    (SELECT CONCAT('" + fileBaseUrl + "/gccofficialapp/files/', '" + filePath + "', aff2.filename) " +
        	    "     FROM activity_feedback_file aff2 " +
        	    "     INNER JOIN activity_feedback af2 ON af2.id = aff2.feedback_id " +
        	    "     WHERE af2.asset_id = al.id AND aff2.fileaction = 'before' " +
        	    "     AND af2.feedback_date >=? AND af2.feedback_date < DATE_ADD(?, INTERVAL 1 DAY) " +
        	    "     ORDER BY af2.feedback_date DESC LIMIT 1) AS before_filepath, " +

        	    "    (SELECT CONCAT('" + fileBaseUrl + "/gccofficialapp/files/', '" + filePath + "', aff3.filename) " +
        	    "     FROM activity_feedback_file aff3 " +
        	    "     INNER JOIN activity_feedback af3 ON af3.id = aff3.feedback_id " +
        	    "     WHERE af3.asset_id = al.id AND aff3.fileaction = 'after' " +
        	    "     AND af3.feedback_date >=? AND af3.feedback_date < DATE_ADD(?, INTERVAL 1 DAY) " +
        	    "     ORDER BY af3.feedback_date DESC LIMIT 1) AS after_filepath " +

        	    "FROM asset_list al " +
        	    "LEFT JOIN activity_feedback af " +
        	    "    ON af.asset_id = al.id " +
        	    "    AND af.feedback_date >= ? " +
        	    "    AND af.feedback_date < DATE_ADD(?, INTERVAL 1 DAY) " +
        	    "LEFT JOIN activity_feedback_file aff " +
        	    "    ON aff.feedback_id = af.id " +
        	    "WHERE al.category_id = 2 " +
        	    "  AND al.streetid = ? " +
        	    "  AND al.isactive = 1 " +
        	    "  AND al.isdelete = 0 " +
        	    "GROUP BY al.id, al.zone, al.ward, al.streetid, al.streetname, al.latitude, al.longitude "
        	);
        
        if (status != null && !status.isBlank()) {
            sqlQuery.append("HAVING Status = ? ");
        }

        List<Object> params = new ArrayList<>();
        params.add(fromDate);
        params.add(toDate);
        params.add(fromDate);
        params.add(toDate);
        params.add(fromDate);
        params.add(toDate);
        params.add(streetid);
        if (status != null && !status.isBlank()) {
            params.add(status);
        }
        
        String finalSql = sqlQuery.toString();
        for (Object param : params) {
            finalSql = finalSql.replaceFirst("\\?", "'" + String.valueOf(param) + "'");
        }
        
        System.out.println("Final SQL: " + finalSql);

        List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery.toString(), params.toArray());

        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("scpreport", result);
        return finalResult;
    }
    
    public String scpDeactivate(String assetId, String userId) {
        try {
            // 1. Log the deactivation
            String sqllog = "INSERT INTO asset_deactive_log(asset_id, deactive_by) VALUES (?, ?)";
            jdbcActivityTemplate.update(sqllog, assetId, userId);

            // 2. Update the asset to deactivate
            String sqlUpdate = "UPDATE asset_list SET isactive = 0, isdelete = 1 WHERE id = ?";
            jdbcActivityTemplate.update(sqlUpdate, assetId);

            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return "failure";
        }
    }
    
    // Encrochmanet 
    @Transactional
	public List<Map<String, Object>> encroachment_vendor_type(){
		String sqlQuery = "SELECT * FROM `encroachment_vendor_type` WHERE (`isactive`=1 AND `isdelete`=0) ORDER BY `name` ASC";
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery);
		return result;
	}
    
    // Encrochmanet 
    @Transactional
	public List<Map<String, Object>> encroachment_vendor_items(){
		String sqlQuery = "SELECT * FROM `encroachment_vendor_items` WHERE (`isactive`=1 AND `isdelete`=0) ORDER BY `name` ASC";
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery);
		return result;
	}
}
