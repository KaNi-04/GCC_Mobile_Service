package in.gov.chennaicorporation.mobileservice.manhole.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class ManHoleActivity {
	private JdbcTemplate jdbcManHoleTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlGccManHoleSource") DataSource ManHoleDataSource) {
		this.jdbcManHoleTemplate = new JdbcTemplate(ManHoleDataSource);
	}
    
    @Autowired
	public ManHoleActivity(Environment environment) {
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

	public String fileUpload(String name, String id, MultipartFile file, String filetype) {

		int lastInsertId = 0;
		// Set the file path where you want to save it
		String uploadDirectory = environment.getProperty("file.upload.directory");
		String serviceFolderName = environment.getProperty("manhole_foldername");
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
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		        
				System.out.println("Date: "+ now.format(formatter));
				System.out.println("Activity: ManHole");
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
	    String sqlQuery = "SELECT `ward` FROM gcc_penalty_hoardings.`hoading_user_list` WHERE `userid` = ? AND `type` = ? LIMIT 1";
	    
	    // Query the database using queryForList
	    List<Map<String, Object>> results = jdbcManHoleTemplate.queryForList(sqlQuery, loginid, type);
	    
	    // Check if results is not empty and extract the ward value
	    if (!results.isEmpty()) {
	        // Extract the ward value from the first result
	        return (String) results.get(0).get("ward");
	    }
	    
	    // Handle the case where no result is found
	    return "000";  // or return null based on your needs
	}
	
	/*
	public List<Map<String, Object>> getDrainList(String loginid) {
		
		String ward = getWardByLoginId(loginid,"ae");
		
        String sql = "SELECT *, "
        		+ "	CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `file`) AS file_url "
        		+ "FROM `drain_master` WHERE `ward`= ? AND isactive = 1 AND isdelete = 0 ";
        List<Map<String, Object>> result = jdbcManHoleTemplate.queryForList(sql, ward);
        
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Drain List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	*/
	
	public List<Map<String, Object>> getDrainList(String loginid) {
	    String ward = getWardByLoginId(loginid, "ae");

	    // 1. Fetch all drains for this ward
	    String sqlDrain = "SELECT *, "
	            + " CONCAT('" + fileBaseUrl + "/gccofficialapp/files', `file`) AS file_url "
	            + " FROM `drain_master` WHERE `ward`= ? AND isactive = 1 AND isdelete = 0";

	    List<Map<String, Object>> drains = jdbcManHoleTemplate.queryForList(sqlDrain, ward);
	    List<Map<String, Object>> finalList = new ArrayList<>();

	    for (Map<String, Object> drain : drains) {
	        Map<String, Object> drainResponse = new HashMap<>();
	        drainResponse.putAll(drain); // add drain fields

	        String drainId = drain.get("drain_id").toString();

	        // 2. Fetch manholes for this drain
	        String sqlManhole = "SELECT manhole_id FROM manhole_master WHERE drain_id = ? AND isactive=1 AND isdelete=0";
	        List<Map<String, Object>> manholes = jdbcManHoleTemplate.queryForList(sqlManhole, drainId);

	        int totalManholes = manholes.size();
	        int completedCount = 0;
	        int pendingCount = 0;

	        // 3. Check before & after activities for each manhole
	        for (Map<String, Object> manhole : manholes) {
	            Object manholeId = manhole.get("manhole_id");

	            // Before
	            String sqlBefore = "SELECT COUNT(*) FROM before_activity "
	                    + "WHERE manhole_id = ? AND drain_id = ? AND isactive=1 AND isdelete=0";
	            int beforeCount = jdbcManHoleTemplate.queryForObject(sqlBefore, Integer.class, manholeId, drainId);

	            // After
	            String sqlAfter = "SELECT COUNT(*) FROM after_activity "
	                    + "WHERE manhole_id = ? AND drain_id = ? AND isactive=1 AND isdelete=0";
	            int afterCount = jdbcManHoleTemplate.queryForObject(sqlAfter, Integer.class, manholeId, drainId);

	            if (beforeCount > 0 && afterCount > 0) {
	                completedCount++;
	            } else {
	                pendingCount++;
	            }
	        }

	        // 4. Percentages
	        double percentCompleted = (totalManholes > 0) ? (completedCount * 100.0 / totalManholes) : 0.0;
	        double percentPending = (totalManholes > 0) ? (pendingCount * 100.0 / totalManholes) : 0.0;

	        // 5. Drain status
	        String drainStatus = (completedCount == totalManholes && totalManholes > 0) ? "Completed" : "Pending";

	        // 6. Add stats
	        drainResponse.put("total_manholes", totalManholes);
	        drainResponse.put("completed", completedCount);
	        drainResponse.put("pending", pendingCount);
	        drainResponse.put("percent_completed", String.format("%.2f", percentCompleted));
	        drainResponse.put("percent_pending", String.format("%.2f", percentPending));
	        drainResponse.put("status", drainStatus);

	        finalList.add(drainResponse);
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Drain List.");
	    response.put("data", finalList);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public String duplicateDrainCheck(String street_id, String drain_side) {
	    try {

	        String checkSql = "SELECT COUNT(*) FROM drain_master WHERE street_id = ? AND `drain_side`= ? AND isactive = 1 AND isdelete = 0 ";

	        Integer count = jdbcManHoleTemplate.queryForObject(
	                checkSql,
	                Integer.class,
	                new Object[]{street_id, drain_side} 
	        );

	        return (count != null && count > 0) ? "true" : "false";

	    } catch (NumberFormatException e) {
	        return "false";
	    }
	}
	
	public List<Map<String, Object>> saveDrain(
			String drain_side,
			String drain_type,
			String drain_length,
			String drain_width,
			String drain_depth,
			String manhole_count,
	        String zone,
	        String ward,
	        String street_name,
	        String street_id,
	        String road_type,
	        String latitude,
	        String longitude,
	        String inby,
	        MultipartFile file
			) {
		
		String action = "New Drain";
		
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();
	    
		// Check Duplicate
		if(duplicateDrainCheck(street_id,drain_side).equalsIgnoreCase("true")) {
			response.put("status", "error");
	        response.put("message", "Duplicate drain identified!");
	        result.add(response);
	        return result; // stop execution, don’t insert
		}
        		
		String filetxt = street_id+"_SWD";
		
	    String filetype = "image";
	    
	    String drainIMG = fileUpload("Drain", filetxt, file, filetype);

	    if ("error".equalsIgnoreCase(drainIMG)) {
	    	response.put("Drain", "upload image");
	        response.put("status", "error");
	        response.put("message", "Drain image upload failed.");
	        result.add(response);
	        return result;
	    }
	    
	    KeyHolder keyHolder = new GeneratedKeyHolder();
	    
	    String insertSql = "INSERT INTO `drain_master` (`drain_side`,`drain_type`,`drain_length`,`drain_width`,`drain_depth`,"
	    		+ "`manhole_count`,`zone`,`ward`,`street_name`,`street_id`,`road_type`,`latitude`,`longitude`,`inby`,`file`) "
	    		+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	    	
	    int affectedRows = jdbcManHoleTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"drain_id"});
	        int i = 1;
	        ps.setString(i++, drain_side);
	        ps.setString(i++, drain_type);
	        ps.setString(i++, drain_length);
	        ps.setString(i++, drain_width);
	        ps.setString(i++, drain_depth);
	        ps.setInt(i++, (manhole_count == null || manhole_count.trim().isEmpty())
	                ? 1
	                : Integer.parseInt(manhole_count));
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, street_name);
	        ps.setString(i++, street_id);
	        ps.setString(i++, road_type);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, inby);
	        ps.setString(i++, drainIMG);
	        
	        return ps;
	    }, keyHolder);
	    
	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("ManHole", action + " insertId: " + lastInsertId);
	        response.put("status", "success");
	        response.put("message", action + " inserted successfully.");
	    } else {
	    	response.put("ManHole", action + " insertId error!");
	        response.put("status", "error");
	        response.put("message", action + " insert failed.");
	    }
	    
	    result.add(response);
	    return result;
    }
	
	/*
	public List<Map<String, Object>> getManHoleList(String loginid, String swdid) {
		
        String sql = "SELECT * FROM `manhole_master` WHERE `swdid`= ?";
        List<Map<String, Object>> result = jdbcManHoleTemplate.queryForList(sql, swdid);
        
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "ManHole List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getManHoleBeforeStatus(String manhole_id) {
		
        String sql = "SELECT *,"
        		+ "	CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `file`) AS file_url "
        		+ " FROM `before_activity` WHERE `manhole_id`= ?";
        List<Map<String, Object>> result = jdbcManHoleTemplate.queryForList(sql, manhole_id);
        
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "ManHole before activity list.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getManHoleAfterStatus(String manhole_id) {
		
        String sql = "SELECT *,"
        		+ "	CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `file`) AS file_url "
        		+ " FROM `after_activity` WHERE `manhole_id`= ?";
        List<Map<String, Object>> result = jdbcManHoleTemplate.queryForList(sql, manhole_id);
        
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "ManHole before activity list.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
    */
	
	@Transactional
	public boolean manHoleDuplicateCheck(String latitudeStr, String longitudeStr) {
	    try {
	        double latitude = Double.parseDouble(latitudeStr);
	        double longitude = Double.parseDouble(longitudeStr);

	        System.out.println("Duplicate check latlong:"+ latitude + " | " + longitude);
	        
	        String checkSql = "SELECT COUNT(*) FROM manhole_master " +
	                "WHERE isactive = 1 AND isdelete = 0 " + // fixed delete flag
	                "AND (6371008.8 * ACOS(" +
	                "COS(RADIANS(?)) * COS(RADIANS(latitude)) * " +
	                "COS(RADIANS(longitude) - RADIANS(?)) + " +
	                "SIN(RADIANS(?)) * SIN(RADIANS(latitude))" +
	                ")) < 0.5";

	        Integer count = jdbcManHoleTemplate.queryForObject(
	                checkSql,
	                Integer.class,
	                latitude, longitude, latitude
	        );
	        /*
	        String checkSqlstr = "SELECT COUNT(*) FROM manhole_master " +
	                "WHERE isactive = 1 AND isdelete = 0 " + // fixed delete flag
	                "AND (6371008.8 * ACOS(" +
	                "COS(RADIANS("+latitude+")) * COS(RADIANS(latitude)) * " +
	                "COS(RADIANS(longitude) - RADIANS("+longitude+")) + " +
	                "SIN(RADIANS("+latitude+")) * SIN(RADIANS(latitude))" +
	                ")) < 0.5";
	        
	        System.out.println("Duplicate check sql:"+ checkSqlstr);
	        */
	        return (count != null && count > 0);

	    } catch (NumberFormatException e) {
	        return false; // invalid lat/lon input
	    }
	}
	
	@Transactional
	public boolean manHoleStreetCheck(String drain_id, String street_id) {
	    try {
	       String checkSql = "SELECT COUNT(*) FROM `drain_master` " +
	                "WHERE isactive = 1 AND isdelete = 0 " + // fixed delete flag
	                "AND drain_id = ?  AND street_id = ? ";

	        Integer count = jdbcManHoleTemplate.queryForObject(
	                checkSql,
	                Integer.class,
	                drain_id,
	                street_id
	        );

	        return (count != null && count == 1);

	    } catch (NumberFormatException e) {
	        return false; // invalid lat/lon input
	    }
	}

	@Transactional
	public List<Map<String, Object>> saveManHole(
	        String drain_id,
	        String zone,
	        String ward,
	        String street_name,
	        String street_id,
	        String latitude,
	        String longitude,
	        String inby
	) {
	    String action = "Save";

	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    // 1. Duplicate check
	    if (manHoleDuplicateCheck(latitude, longitude)) {
	        response.put("status", "error");
	        response.put("message", "Duplicate ManHole identified within 3 meters!");
	        result.add(response);
	        return result; // stop execution, don’t insert
	    }
	    
	    // 2. Drain Street Check
	    if (!manHoleStreetCheck(drain_id, street_id)) {
	    	response.put("status", "error");
	        response.put("message", "Drain Street not mached with current Street!");
	        result.add(response);
	        return result; // stop execution, don’t insert
	    }
	    
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    String insertSql = "INSERT INTO manhole_master " +
	            "(drain_id, zone, ward, street_name, street_id, latitude, longitude, inby) " +
	            "VALUES (?,?,?,?,?,?,?,?)";

	    int affectedRows = jdbcManHoleTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"manhole_id"});
	        int i = 1;
	        ps.setString(i++, drain_id);
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, street_name);
	        ps.setString(i++, street_id);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, inby);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("ManHole", action + " insertId: " + lastInsertId);
	        response.put("status", "success");
	        response.put("message", action + " inserted successfully.");
	    } else {
	        response.put("ManHole", action + " insertId error!");
	        response.put("status", "error");
	        response.put("message", action + " insert failed.");
	    }

	    result.add(response);
	    return result;
	}
	
	public List<Map<String, Object>> getManHoleList(String loginid, String drain_id) {
	    // 1. Fetch all manholes for the given drain_id
	    String sqlManhole = "SELECT * FROM manhole_master WHERE drain_id = ?";
	    List<Map<String, Object>> manholes = jdbcManHoleTemplate.queryForList(sqlManhole, drain_id);

	    List<Map<String, Object>> finalList = new ArrayList<>();

	    for (Map<String, Object> manhole : manholes) {
	        Map<String, Object> manholeResponse = new HashMap<>();
	        manholeResponse.putAll(manhole); // add all manhole columns

	        // 2. Fetch before activity
	        String sqlBefore = "SELECT *, "
	        		+ "	CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `file`) AS file_url "
	        		+ "FROM before_activity WHERE manhole_id = ? AND drain_id = ? AND isactive=1 AND isdelete=0";
	        List<Map<String, Object>> beforeActivities = jdbcManHoleTemplate.queryForList(sqlBefore,
	                manhole.get("manhole_id"), drain_id);

	        Map<String, Object> beforeData = new HashMap<>();
	        if (!beforeActivities.isEmpty()) {
	            beforeData.put("status", "Completed");
	            beforeData.put("details", beforeActivities);
	        } else {
	            beforeData.put("status", "Pending");
	            beforeData.put("details", Collections.emptyList());
	        }

	        // 3. Fetch after activity
	        String sqlAfter = "SELECT *,"
	        		+ "	CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `file`) AS file_url "
	        		+ " FROM after_activity WHERE manhole_id = ? AND drain_id = ? AND isactive=1 AND isdelete=0";
	        List<Map<String, Object>> afterActivities = jdbcManHoleTemplate.queryForList(sqlAfter,
	                manhole.get("manhole_id"), drain_id);

	        Map<String, Object> afterData = new HashMap<>();
	        if (!afterActivities.isEmpty()) {
	            afterData.put("status", "Completed");
	            afterData.put("details", afterActivities);
	        } else {
	            afterData.put("status", "Pending");
	            afterData.put("details", Collections.emptyList());
	        }

	        // 4. Overall manhole status
	        String overallStatus = (!beforeActivities.isEmpty() && !afterActivities.isEmpty()) ? "Completed" : "Pending";
	        manholeResponse.put("status", overallStatus);
	        manholeResponse.put("before", beforeData);
	        manholeResponse.put("after", afterData);

	        finalList.add(manholeResponse);
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "ManHole List.");
	    response.put("data", finalList);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public String manHoleLocationCheck(String cby, String manhole_id, String latitudeStr, String longitudeStr) {
	    try {
	        double latitude = Double.parseDouble(latitudeStr);
	        double longitude = Double.parseDouble(longitudeStr);

	        String checkSql = "SELECT COUNT(*) FROM manhole_master " +
	                "WHERE manhole_id = ? " +
	                "AND (6371008.8 * ACOS(" +
	                "COS(RADIANS(?)) * COS(RADIANS(latitude)) * " +
	                "COS(RADIANS(longitude) - RADIANS(?)) + " +
	                "SIN(RADIANS(?)) * SIN(RADIANS(latitude))" +
	                ")) < 1";

	        Integer count = jdbcManHoleTemplate.queryForObject(
	                checkSql,
	                Integer.class,
	                new Object[]{manhole_id, latitude, longitude, latitude}
	        );

	        return (count != null && count > 0) ? "true" : "false";

	    } catch (NumberFormatException e) {
	        return "false"; // invalid lat/lon input
	    }
	}
	
	@Transactional
	public List<Map<String, Object>> saveActivity(
	        String manhole_id,
	        String drain_id,
	        MultipartFile file,
	        String zone,
	        String ward,
	        String street_name,
	        String street_id,
	        String latitude,
	        String longitude,
	        String inby,
	        String action,
	        String beforeActivityId,
	        String clean_type,
	        String case_id
	) {
		
		String filetxt =manhole_id+"_"+drain_id;
		
	    String filetype = "image";
	    
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    // 1. Drain Street Check
	    if (!manHoleStreetCheck(drain_id, street_id)) {
	    	response.put("status", "error");
	        response.put("message", "Drain Street not mached with current Street!");
	        result.add(response);
	        return result; // stop execution, don’t insert
	    }
	    
	    String activityimg = fileUpload(action, filetxt, file, filetype);
	    // 2. Check file uploaded
	    if ("error".equalsIgnoreCase(activityimg)) {
	        response.put("status", "error");
	        response.put("message", "ManHole activity insert failed.");
	        result.add(response);
	        return result;
	    }

	    String insertBeforeSql = "INSERT INTO before_activity(manhole_id, drain_id, file, zone, ward, street_name, street_id, latitude, longitude, inby, case_id) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

		String insertAfterSql = "INSERT INTO after_activity(manhole_id, drain_id, file, zone, ward, street_name, street_id, latitude, longitude, inby, case_id, before_activity_id, clean_type) "
		               + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		String insertSql = "before".equalsIgnoreCase(action) ? insertBeforeSql : insertAfterSql;

	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcManHoleTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"activity_id"});
	        int i = 1;
	        ps.setString(i++, manhole_id);
	        ps.setString(i++, drain_id);
	        ps.setString(i++, activityimg);
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, street_name);
	        ps.setString(i++, street_id);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, inby);
	        ps.setString(i++, case_id);
	        
	        if ("after".equalsIgnoreCase(action)) {
	            ps.setString(i++, beforeActivityId); 
	            ps.setString(i++, clean_type); 
	        }

	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("ManHole", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", "ManHole " + action + " activity inserted successfully.");
	    } else {
	    	response.put("ManHole", action + " insertId error!");
	        response.put("status", "error");
	        response.put("message", "ManHole " + action + " activity insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
	
	public List<Map<String, Object>> getZoneReport() {
	    String sql = 
	        "SELECT "
	        + "    z.zone,"
	        + "    COUNT(t.drain_id) AS total_drain,"
	        + "    SUM(CASE WHEN t.total_manhole = t.completed_manhole THEN 1 ELSE 0 END) AS total_drain_completed,"
	        + "    SUM(CASE WHEN t.total_manhole > t.completed_manhole THEN 1 ELSE 0 END) AS total_drain_pending,"
	        + "    ROUND(SUM(CASE WHEN t.total_manhole = t.completed_manhole THEN 1 ELSE 0 END) * 100.0 / COUNT(t.drain_id), 2) AS drain_completed_percentage,"
	        + "    ROUND(SUM(CASE WHEN t.total_manhole > t.completed_manhole THEN 1 ELSE 0 END) * 100.0 / COUNT(t.drain_id), 2) AS drain_pending_percentage "
	        + "FROM ("
	        + "    SELECT '01' AS zone UNION ALL"
	        + "    SELECT '02' UNION ALL"
	        + "    SELECT '03' UNION ALL"
	        + "    SELECT '04' UNION ALL"
	        + "    SELECT '05' UNION ALL"
	        + "    SELECT '06' UNION ALL"
	        + "    SELECT '07' UNION ALL"
	        + "    SELECT '08' UNION ALL"
	        + "    SELECT '09' UNION ALL"
	        + "    SELECT '10' UNION ALL"
	        + "    SELECT '11' UNION ALL"
	        + "    SELECT '12' UNION ALL"
	        + "    SELECT '13' UNION ALL"
	        + "    SELECT '14' UNION ALL"
	        + "    SELECT '15'"
	        + ") z "
	        + "LEFT JOIN ("
	        + "    SELECT "
	        + "        d.drain_id,"
	        + "        d.zone,"
	        + "        SUM(d.manhole_count) AS total_manhole,"
	        + "        COUNT(DISTINCT CASE "
	        + "            WHEN ba.activity_id IS NOT NULL AND aa.activity_id IS NOT NULL THEN m.manhole_id "
	        + "        END) AS completed_manhole"
	        + "    FROM drain_master d"
	        + "    LEFT JOIN manhole_master m ON d.drain_id = m.drain_id AND m.isactive=1 AND m.isdelete=0"
	        + "    LEFT JOIN before_activity ba ON m.manhole_id = ba.manhole_id AND ba.isactive=1 AND ba.isdelete=0"
	        + "    LEFT JOIN after_activity aa  ON m.manhole_id = aa.manhole_id AND aa.isactive=1 AND aa.isdelete=0"
	        + "    WHERE d.isactive = 1 AND d.isdelete = 0"
	        + "    GROUP BY d.drain_id, d.zone"
	        + ") t ON z.zone = t.zone "
	        + "GROUP BY z.zone "
	        + "ORDER BY z.zone";
	    
	    return jdbcManHoleTemplate.queryForList(sql);
	}
	
	public List<Map<String, Object>> getWardReport(String zone) {
	    String sql = 
	        "SELECT "
	        + "    t.zone,"
	        + "    t.ward,"
	        + "    COUNT(t.drain_id) AS total_drain,"
	        + "    SUM(CASE WHEN t.total_manhole = t.completed_manhole THEN 1 ELSE 0 END) AS total_drain_completed,"
	        + "    SUM(CASE WHEN t.total_manhole > t.completed_manhole THEN 1 ELSE 0 END) AS total_drain_pending,"
	        + "    ROUND(SUM(CASE WHEN t.total_manhole = t.completed_manhole THEN 1 ELSE 0 END) * 100.0 / COUNT(t.drain_id), 2) AS drain_completed_percentage,"
	        + "    ROUND(SUM(CASE WHEN t.total_manhole > t.completed_manhole THEN 1 ELSE 0 END) * 100.0 / COUNT(t.drain_id), 2) AS drain_pending_percentage "
	        + "FROM ("
	        + "    SELECT "
	        + "        d.drain_id,"
	        + "        d.zone,"
	        + "        d.ward,"
	        + "        SUM(d.manhole_count) AS total_manhole,"
	        + "        COUNT(DISTINCT CASE "
	        + "            WHEN ba.activity_id IS NOT NULL AND aa.activity_id IS NOT NULL THEN m.manhole_id "
	        + "        END) AS completed_manhole"
	        + "    FROM drain_master d"
	        + "    LEFT JOIN manhole_master m ON d.drain_id = m.drain_id AND m.isactive=1 AND m.isdelete=0"
	        + "    LEFT JOIN before_activity ba ON m.manhole_id = ba.manhole_id AND ba.isactive=1 AND ba.isdelete=0"
	        + "    LEFT JOIN after_activity aa  ON m.manhole_id = aa.manhole_id AND aa.isactive=1 AND aa.isdelete=0"
	        + "    WHERE d.isactive = 1 AND d.isdelete = 0"
	        + "      AND d.zone = ?"
	        + "    GROUP BY d.drain_id, d.zone, d.ward"
	        + ") t "
	        + "GROUP BY t.zone, t.ward "
	        + "ORDER BY t.ward";
	    
	    return jdbcManHoleTemplate.queryForList(sql,zone);
	}
	
	public List<Map<String, Object>> getDrainReport(String ward) {
	    String sql = 
	        "SELECT "
	        + "    t.drain_name AS drain, "
	        + "    t.total_manhole AS total_manhole, "
	        + "    t.completed_manhole AS manhole_completed, "
	        + "    (t.total_manhole - t.completed_manhole) AS manhole_pending, "
	        + "    ROUND(t.completed_manhole * 100.0 / t.total_manhole, 2) AS completed_percentage, "
	        + "    ROUND((t.total_manhole - t.completed_manhole) * 100.0 / t.total_manhole, 2) AS pending_percentage "
	        + "FROM ( "
	        + "    SELECT "
	        + "        CONCAT(d.street_name, '-', d.drain_side) AS drain_name, "
	        + "        d.drain_id, "
	        + "        SUM(d.manhole_count) AS total_manhole, "
	        + "        COUNT(DISTINCT CASE "
	        + "            WHEN ba.activity_id IS NOT NULL AND aa.activity_id IS NOT NULL THEN m.manhole_id "
	        + "        END) AS completed_manhole "
	        + "    FROM drain_master d "
	        + "    LEFT JOIN manhole_master m ON d.drain_id = m.drain_id AND m.isactive=1 AND m.isdelete=0 "
	        + "    LEFT JOIN before_activity ba ON m.manhole_id = ba.manhole_id AND ba.isactive=1 AND ba.isdelete=0 "
	        + "    LEFT JOIN after_activity aa ON m.manhole_id = aa.manhole_id AND aa.isactive=1 AND aa.isdelete=0 "
	        + "    WHERE d.isactive = 1 AND d.isdelete = 0 "
	        + "      AND d.ward = ? "
	        + "    GROUP BY drain_name, d.drain_id "
	        + ") t "
	        + "ORDER BY t.drain_name";

	    return jdbcManHoleTemplate.queryForList(sql, ward);
	}
}
