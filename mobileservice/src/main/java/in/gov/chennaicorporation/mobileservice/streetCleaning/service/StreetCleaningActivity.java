package in.gov.chennaicorporation.mobileservice.streetCleaning.service;

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
import java.util.LinkedHashMap;
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
public class StreetCleaningActivity {
	private JdbcTemplate jdbcStreetCleaningTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlGccStreetCleaningSource") DataSource StreetCleaningDataSource) {
		this.jdbcStreetCleaningTemplate = new JdbcTemplate(StreetCleaningDataSource);
	}
    
    @Autowired
	public StreetCleaningActivity(Environment environment) {
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
		String serviceFolderName = environment.getProperty("StreetCleaning_foldername");
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
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		        
				System.out.println("Date: "+ now.format(formatter));
				System.out.println("Activity: StreetCleaning");
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
	    List<Map<String, Object>> results = jdbcStreetCleaningTemplate.queryForList(sqlQuery, loginid, type);
	    
	    // Check if results is not empty and extract the ward value
	    if (!results.isEmpty()) {
	        // Extract the ward value from the first result
	        return (String) results.get(0).get("ward");
	    }
	    
	    // Handle the case where no result is found
	    return "000";  // or return null based on your needs
	}
	
 	@Transactional
	public String checkStreet_id(String inby, String gis_street_id, String ward) {
		String checkSql = "Select count(sid) FROM `street_list_master` WHERE `gis_street_id`=? AND `ward`=?";
		Integer count = jdbcStreetCleaningTemplate.queryForObject(
                checkSql,
                Integer.class,
                gis_street_id,
                ward
        );
		
		if(count != null && count > 0) {
			return "false";
		}
		return "true";
	}
 	
 	@Transactional
 	public List<Map<String, Object>> getStreetPendingList(String loginid, String ward) {
 		
 	    String sqlStreet = "SELECT sid, zone, ward, gis_street_id, street_name, road_type, " +
 	            "street_length, isactive, isdelete, inby, cdate " +
 	            "FROM street_list_master " +
 	            "WHERE ward = ? AND isactive=1 AND isdelete=0";

 	    List<Map<String, Object>> streetList = jdbcStreetCleaningTemplate.queryForList(sqlStreet, ward);

 	    List<Map<String, Object>> finalList = new ArrayList<>();

 	    for (Map<String, Object> street : streetList) {
 	        Map<String, Object> streetResponse = new HashMap<>(street);

 	        // ✅ latest activity for that street
 	        String sqlActivity = "SELECT activity_id, gis_street_id, work_status " +
 	                "FROM cleaning_activity " +
 	                "WHERE gis_street_id = ? AND ward = ? AND isactive=1 AND isdelete=0 " +
 	                "ORDER BY indate DESC LIMIT 1";

 	        List<Map<String, Object>> activities = jdbcStreetCleaningTemplate.queryForList(
 	                sqlActivity, street.get("gis_street_id"), street.get("ward")
 	        );

 	        String overallStatus = "Pending"; // default
 	        if (!activities.isEmpty()) {
 	            Map<String, Object> lastActivity = activities.get(0);
 	            String workStatus = (String) lastActivity.get("work_status");

 	            if (workStatus != null && !workStatus.equalsIgnoreCase("Partially")
 	                    && !workStatus.equalsIgnoreCase("pending")) {
 	                overallStatus = "Completed";
 	            }
 	        }

 	        // ✅ only keep Pending streets
 	        if ("Pending".equalsIgnoreCase(overallStatus)) {
 	            streetResponse.put("status", "Pending");
 	            finalList.add(streetResponse);
 	        }
 	    }

 	    Map<String, Object> response = new HashMap<>();
 	    response.put("status", "Success");
 	    response.put("message", "Pending Street List.");
 	    response.put("data", finalList);

 	    return Collections.singletonList(response);
 	}
	
 	@Transactional
	public List<Map<String, Object>> getStreetList(String loginid, String street_id, String ward) {
	    
	    //String ward = getWardByLoginId(loginid, "ae");

	    // 1. Fetch all streets for the ward
	    String sqlStreet = "SELECT sid, zone, ward, gis_street_id, street_name, road_type, " +
	            "street_length, isactive, isdelete, inby, cdate " +
	            "FROM street_list_master " +
	            "WHERE gis_street_id = ? AND ward =? AND isactive=1 AND isdelete=0";
	    List<Map<String, Object>> streetList = jdbcStreetCleaningTemplate.queryForList(sqlStreet, street_id, ward);

	    List<Map<String, Object>> finalList = new ArrayList<>();

	    for (Map<String, Object> street : streetList) {
	        Map<String, Object> streetResponse = new HashMap<>();
	        streetResponse.putAll(street);

	        // 2. Fetch activities based on street_id
	        String sqlActivity = "SELECT activity_id, gis_street_id, video_file, left_file, right_file, " +
	                "work_status, cleaned_meter, inby, indate, isactive, isdelete, latitude, longitude, " +
	                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', video_file) AS video_url, " +
	                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', left_file) AS left_file_url, " +
	                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', right_file) AS right_file_url " +
	                "FROM cleaning_activity " +
	                "WHERE gis_street_id = ? AND ward = ? AND isactive=1 AND isdelete=0 " +
	                "ORDER BY indate DESC LIMIT 1";

	        System.out.println(sqlActivity);
	        
	        List<Map<String, Object>> activities = jdbcStreetCleaningTemplate.queryForList(sqlActivity, street.get("gis_street_id"), street.get("ward"));

	        Map<String, Object> activityData = new HashMap<>();
	        String overallStatus = "Pending";

	        if (!activities.isEmpty()) {
	            Map<String, Object> lastActivity = activities.get(0); // latest one
	            String workStatus = (String) lastActivity.get("work_status");

	            if (workStatus != null && workStatus.equalsIgnoreCase("Partially")) {
	                overallStatus = "Pending";
	            } else {
	                overallStatus = "Completed";
	            }

	            activityData.put("status", overallStatus);
	            activityData.put("details", activities);
	        } else {
	            activityData.put("status", "Pending");
	            activityData.put("details", Collections.emptyList());
	        }

	        // 3. Add status + activity info to street response
	        streetResponse.put("status", overallStatus);
	        streetResponse.put("activities", activityData);

	        finalList.add(streetResponse);
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Street List.");
	    response.put("data", finalList);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public boolean getStreet_id(String gis_street_id, String ward) {
		String checkSql = "Select count(sid) FROM `street_list_master` WHERE `gis_street_id`=? AND ward=?";
		Integer count = jdbcStreetCleaningTemplate.queryForObject(
                checkSql,
                Integer.class,
                gis_street_id,
                ward
        );
		return (count != null && count > 0);
	}
	
	@Transactional
	public Boolean saveStreet(
	        String zone,
	        String ward,
	        String gis_street_id,
	        String street_name,
	        String road_type,
	        String street_length,
	        String latitude,
	        String longitude,
	        String inby
	) {
	    // 1. Duplicate check
	    if (getStreet_id(gis_street_id, ward)) {
	        return true; // stop execution, street already exists
	    }

	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    String insertSql = "INSERT INTO street_list_master " +
	            "(zone, ward, gis_street_id, street_name, road_type, street_length, inby) " +
	            "VALUES (?,?,?,?,?,?,?)";

	    int affectedRows = jdbcStreetCleaningTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"street_id"});
	        int i = 1;
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, gis_street_id);
	        ps.setString(i++, street_name);
	        ps.setString(i++, road_type);
	        ps.setString(i++, street_length);
	        ps.setString(i++, inby);
	        return ps;
	    }, keyHolder);

	    return affectedRows > 0;
	}
	
	@Transactional
	public List<Map<String, Object>> saveActivity(
	        MultipartFile video_file,
	        MultipartFile left_file,
	        MultipartFile right_file,
	        String work_status,
	        String cleaned_meter,
	        String zone,
	        String ward,
	        String gis_street_id,
	        String street_name,
	        String road_type,
	        String street_length,
	        String latitude,
	        String longitude,
	        String inby
	) {
		
		String filetxt =gis_street_id;
	    String action = inby;
	    
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    // 1. Street Check and add
	    if (!saveStreet(zone, ward, gis_street_id, street_name, road_type, street_length, latitude, longitude, inby)) {
	    	response.put("status", "error");
	        response.put("message", "Failed to add current street!");
	        result.add(response);
	        return result; // stop execution, don’t insert
	    }
	    
	    // For Left Image
	    String filetype = "image";
	    String leftIMG = fileUpload(action, filetxt, left_file, filetype);

	    if ("error".equalsIgnoreCase(leftIMG)) {
	        response.put("status", "error");
	        response.put("message", "Street cleaning activity left image insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    // For Right Image	    
	    String rightIMG = fileUpload(action, filetxt, right_file, filetype);

	    if ("error".equalsIgnoreCase(rightIMG)) {
	        response.put("status", "error");
	        response.put("message", "Street cleaning activity right image insert failed.");
	        result.add(response);
	    }    
	    
	    // For Video
	    filetype = "video";
	    String video = fileUpload(action, filetxt, video_file, filetype);
	    
	    if ("error".equalsIgnoreCase(video)) {
	        response.put("status", "error");
	        response.put("message", "Street cleaning activity video insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    String insertBeforeSql = "INSERT INTO `cleaning_activity`"
	    		+ "(`gis_street_id`, `video_file`, `left_file`, `right_file`, `work_status`, "
	    		+ "`cleaned_meter`, `inby`, `latitude`, `longitude`, `zone`, `ward`) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

	    String insertSql = insertBeforeSql;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcStreetCleaningTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"activity_id"});
	        int i = 1;
	        ps.setString(i++, gis_street_id);
	        ps.setString(i++, video);
	        ps.setString(i++, leftIMG);
	        ps.setString(i++, rightIMG);
	        ps.setString(i++, work_status);
	        ps.setString(i++, cleaned_meter);
	        ps.setString(i++, inby);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("Street Cleaning", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", "Street Cleaning activity inserted successfully.");
	    } else {
	    	response.put("Street Cleaning", action + " insertId error!");
	        response.put("status", "error");
	        response.put("message", "Street Cleaning activity insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
	
	/*
	@Transactional
	public List<Map<String, Object>> getZoneReport(String loginid) {
	    // 1. Fetch all active streets for the ward (with zone info)
	    String sqlStreet = "SELECT sid, zone, ward, gis_street_id, street_name, road_type, street_length " +
	            "FROM street_list_master " +
	            "WHERE isactive = 1 AND isdelete = 0 ORDER BY zone ASC";

	    List<Map<String, Object>> streetList = jdbcStreetCleaningTemplate.queryForList(sqlStreet);

	    // Map<Zone, Map<status, count>>
	    Map<String, Map<String, Integer>> zoneSummary = new LinkedHashMap<>();

	    for (Map<String, Object> street : streetList) {
	        String zone = String.valueOf(street.get("zone"));
	        String gisStreetId = String.valueOf(street.get("gis_street_id"));
	        String wardNo = String.valueOf(street.get("ward"));

	        // Fetch latest activity for this street
	        String sqlActivity = "SELECT work_status FROM cleaning_activity " +
	                "WHERE gis_street_id = ? AND zone = ? AND isactive = 1 AND isdelete = 0 " +
	                "ORDER BY indate DESC LIMIT 1";

	        List<Map<String, Object>> activities = jdbcStreetCleaningTemplate.queryForList(sqlActivity, gisStreetId, zone);

	        String status = "Pending"; // default
	        if (!activities.isEmpty()) {
	            Map<String, Object> last = activities.get(0);
	            String workStatus = (String) last.get("work_status");
	            if (workStatus != null && !workStatus.equalsIgnoreCase("Partially")) {
	                status = "Completed";
	            }
	        }

	        // Initialize zone counters
	        zoneSummary.putIfAbsent(zone, new HashMap<>(Map.of(
	                "Total Street", 0,
	                "Completed", 0,
	                "Pending", 0
	        )));

	        Map<String, Integer> counts = zoneSummary.get(zone);
	        counts.put("Total Street", counts.get("Total Street") + 1);
	        if (status.equalsIgnoreCase("Completed")) {
	            counts.put("Completed", counts.get("Completed") + 1);
	        } else {
	            counts.put("Pending", counts.get("Pending") + 1);
	        }
	    }

	    // Build final list
	    List<Map<String, Object>> reportList = new ArrayList<>();
	    for (var entry : zoneSummary.entrySet()) {
	        Map<String, Object> zoneReport = new LinkedHashMap<>();
	        zoneReport.put("Zone", entry.getKey());
	        zoneReport.putAll(entry.getValue());
	        reportList.add(zoneReport);
	    }

	    Map<String, Object> response = new LinkedHashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Zone-wise Street Cleaning Report");
	    response.put("data", reportList);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> getWardReportByZone(String loginid, String zone) {

	    // 1️⃣ Fetch all active streets under the given zone
	    String sqlStreet = "SELECT sid, zone, ward, gis_street_id, street_name, road_type, street_length " +
	            "FROM street_list_master " +
	            "WHERE zone = ? AND isactive = 1 AND isdelete = 0";

	    List<Map<String, Object>> streetList = jdbcStreetCleaningTemplate.queryForList(sqlStreet, zone);

	    // Map<Ward, Map<status, count>>
	    Map<String, Map<String, Integer>> wardSummary = new LinkedHashMap<>();

	    for (Map<String, Object> street : streetList) {
	        String ward = String.valueOf(street.get("ward"));
	        String gisStreetId = String.valueOf(street.get("gis_street_id"));

	        // 2️⃣ Fetch the latest cleaning activity for this street
	        String sqlActivity = "SELECT work_status FROM cleaning_activity " +
	                "WHERE gis_street_id = ? AND ward = ? AND isactive = 1 AND isdelete = 0 " +
	                "ORDER BY indate DESC LIMIT 1";

	        List<Map<String, Object>> activities =
	                jdbcStreetCleaningTemplate.queryForList(sqlActivity, gisStreetId, ward);

	        String status = "Pending"; // Default

	        if (!activities.isEmpty()) {
	            Map<String, Object> last = activities.get(0);
	            String workStatus = (String) last.get("work_status");
	            if (workStatus != null && !workStatus.equalsIgnoreCase("Partially")) {
	                status = "Completed";
	            }
	        }

	        // 3️⃣ Initialize ward counters
	        wardSummary.putIfAbsent(ward, new HashMap<>(Map.of(
	                "Total Street", 0,
	                "Completed", 0,
	                "Pending", 0
	        )));

	        Map<String, Integer> counts = wardSummary.get(ward);
	        counts.put("Total Street", counts.get("Total Street") + 1);
	        if (status.equalsIgnoreCase("Completed")) {
	            counts.put("Completed", counts.get("Completed") + 1);
	        } else {
	            counts.put("Pending", counts.get("Pending") + 1);
	        }
	    }

	    // 4️⃣ Build the final report list
	    List<Map<String, Object>> reportList = new ArrayList<>();
	    for (var entry : wardSummary.entrySet()) {
	        Map<String, Object> wardReport = new LinkedHashMap<>();
	        wardReport.put("Ward", entry.getKey());
	        wardReport.putAll(entry.getValue());
	        reportList.add(wardReport);
	    }

	    // 5️⃣ Prepare response
	    Map<String, Object> response = new LinkedHashMap<>();
	    response.put("status", "Success");
	    response.put("zone", zone);
	    response.put("message", "Ward-wise Street Cleaning Report for Zone " + zone);
	    response.put("data", reportList);

	    return Collections.singletonList(response);
	}
	*/

	@Transactional
	public List<Map<String, Object>> getZoneReport(String loginid) {
	    // 1. Fetch all active streets for the ward (with zone info)
	    String sqlStreet = "SELECT"
	    		+ "    CASE WHEN zone IS NULL THEN 'TOTAL' ELSE zone END AS Zone,"
	    		+ "    SUM(total_road_count) AS `Total Street`,"
	    		+ "    SUM(total_partially_completed) AS Pending,"
	    		+ "    SUM(total_fully_completed) AS Completed,"
	    		+ "	SUM(total_road_count) - (SUM(total_partially_completed) + SUM(total_fully_completed)) AS total_pending "
	    		+ "FROM ("
	    		+ "    SELECT"
	    		+ "        zone,"
	    		+ "        COUNT(*) AS total_road_count,"
	    		+ "        0 AS total_partially_completed,"
	    		+ "        0 AS total_fully_completed"
	    		+ "    FROM gis_street_list"
	    		+ "    WHERE isactive = 1 AND isdelete = 0"
	    		+ "    GROUP BY zone"
	    		+ ""
	    		+ "    UNION ALL"
	    		+ ""
	    		+ "    SELECT"
	    		+ "        sl.zone,"
	    		+ "		"
	    		+ "        0 AS total_road_count,"
	    		+ "        SUM(CASE WHEN ca.work_status = 'partially' THEN 1 ELSE 0 END) AS total_partially_completed,"
	    		+ "        SUM(CASE WHEN ca.work_status = 'completed' THEN 1 ELSE 0 END) AS total_fully_completed"
	    		+ "    FROM street_list_master sl"
	    		+ "    LEFT JOIN ("
	    		+ "        SELECT ca1.*"
	    		+ "        FROM cleaning_activity ca1"
	    		+ "        INNER JOIN ("
	    		+ "            SELECT gis_street_id, MAX(indate) AS last_indate"
	    		+ "            FROM cleaning_activity"
	    		+ "            WHERE isactive = 1 AND isdelete = 0"
	    		+ "            GROUP BY gis_street_id"
	    		+ "        ) ca2 "
	    		+ "        ON ca1.gis_street_id = ca2.gis_street_id "
	    		+ "        AND ca1.indate = ca2.last_indate"
	    		+ "        WHERE ca1.isactive = 1 AND ca1.isdelete = 0"
	    		+ "    ) ca ON sl.gis_street_id = ca.gis_street_id"
	    		+ "    WHERE sl.isactive = 1 AND sl.isdelete = 0"
	    		+ "    GROUP BY sl.zone"
	    		+ ") AS combined "
	    		+ "GROUP BY zone WITH ROLLUP "
	    		+ "ORDER BY "
	    		+ "    CASE WHEN zone IS NULL THEN 999 ELSE CAST(zone AS UNSIGNED) END ";

	    List<Map<String, Object>> reportList = jdbcStreetCleaningTemplate.queryForList(sqlStreet);

	    Map<String, Object> response = new LinkedHashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Zone-wise Street Cleaning Report");
	    response.put("data", reportList);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> getWardReportByZone(String loginid, String zone) {

	    // 1️⃣ Fetch all active streets under the given zone
	    String sqlStreet = "SELECT"
	    		+ "    CASE WHEN ward IS NULL THEN 'TOTAL' ELSE ward END AS Ward,"
	    		+ "    SUM(total_road_count) AS `Total Street`,"
	    		+ "    SUM(total_partially_completed) AS Pending,"
	    		+ "    SUM(total_fully_completed) AS Completed,"
	    		+ "	SUM(total_road_count) - (SUM(total_partially_completed) + SUM(total_fully_completed)) AS total_pending "
	    		+ "FROM ("
	    		+ "    SELECT"
	    		+ "        ward,"
	    		+ "        COUNT(*) AS total_road_count,"
	    		+ "        0 AS total_partially_completed,"
	    		+ "        0 AS total_fully_completed"
	    		+ "    FROM gis_street_list"
	    		+ "    WHERE isactive = 1 AND isdelete = 0 AND zone = ?"
	    		+ "    GROUP BY ward"
	    		+ ""
	    		+ "    UNION ALL"
	    		+ ""
	    		+ "    SELECT"
	    		+ "        sl.ward,"
	    		+ "        0 AS total_road_count,"
	    		+ "        SUM(CASE WHEN ca.work_status = 'partially' THEN 1 ELSE 0 END) AS total_partially_completed,"
	    		+ "        SUM(CASE WHEN ca.work_status = 'completed' THEN 1 ELSE 0 END) AS total_fully_completed"
	    		+ "    FROM street_list_master sl"
	    		+ "    LEFT JOIN ("
	    		+ "        SELECT ca1.*"
	    		+ "        FROM cleaning_activity ca1"
	    		+ "        INNER JOIN ("
	    		+ "            SELECT gis_street_id, MAX(indate) AS last_indate"
	    		+ "            FROM cleaning_activity"
	    		+ "            WHERE isactive = 1 AND isdelete = 0"
	    		+ "            GROUP BY gis_street_id"
	    		+ "        ) ca2 ON ca1.gis_street_id = ca2.gis_street_id AND ca1.indate = ca2.last_indate"
	    		+ "        WHERE ca1.isactive = 1 AND ca1.isdelete = 0"
	    		+ "    ) ca ON sl.gis_street_id = ca.gis_street_id"
	    		+ "    WHERE sl.isactive = 1 AND sl.isdelete = 0 AND sl.zone = ?"
	    		+ "    GROUP BY sl.ward"
	    		+ ") AS combined "
	    		+ "GROUP BY ward WITH ROLLUP "
	    		+ "ORDER BY "
	    		+ "    CASE WHEN ward IS NULL THEN 999 ELSE CAST(ward AS UNSIGNED) END ";

	    List<Map<String, Object>> reportList = jdbcStreetCleaningTemplate.queryForList(sqlStreet, zone, zone);

	    // 5️⃣ Prepare response
	    Map<String, Object> response = new LinkedHashMap<>();
	    response.put("status", "Success");
	    response.put("zone", zone);
	    response.put("message", "Ward-wise Street Cleaning Report for Zone " + zone);
	    response.put("data", reportList);

	    return Collections.singletonList(response);
	}
	@Transactional
	public List<Map<String, Object>> getStreetListByWardAndStatus(String loginid, String ward, String status) {

	    // 1️⃣ Fetch all streets for the ward
	    String sqlStreet = "SELECT sid, zone, ward, gis_street_id, street_name, road_type, " +
	            "street_length, isactive, isdelete, inby, cdate " +
	            "FROM street_list_master " +
	            "WHERE ward = ? AND isactive = 1 AND isdelete = 0";

	    List<Map<String, Object>> streetList = jdbcStreetCleaningTemplate.queryForList(sqlStreet, ward);

	    List<Map<String, Object>> finalList = new ArrayList<>();

	    for (Map<String, Object> street : streetList) {
	        Map<String, Object> streetResponse = new HashMap<>(street);

	        // 2️⃣ Get latest cleaning activity for the street
	        String sqlActivity = "SELECT activity_id, gis_street_id, video_file, left_file, right_file, " +
	                "work_status, cleaned_meter, inby, indate, isactive, isdelete, latitude, longitude, " +
	                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', video_file) AS video_url, " +
	                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', left_file) AS left_file_url, " +
	                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', right_file) AS right_file_url " +
	                "FROM cleaning_activity " +
	                "WHERE gis_street_id = ? AND ward = ? AND isactive = 1 AND isdelete = 0 " +
	                "ORDER BY indate DESC LIMIT 1";

	        List<Map<String, Object>> activities = jdbcStreetCleaningTemplate.queryForList(
	                sqlActivity,
	                street.get("gis_street_id"),
	                street.get("ward")
	        );

	        String overallStatus = "Pending"; // default
	        Map<String, Object> activityData = new HashMap<>();

	        if (!activities.isEmpty()) {
	            Map<String, Object> lastActivity = activities.get(0);
	            String workStatus = (String) lastActivity.get("work_status");

	            // logic: treat 'Partially' as 'Pending'
	            if (workStatus != null && !workStatus.equalsIgnoreCase("Partially")) {
	                overallStatus = "Completed";
	            }

	            activityData.put("status", overallStatus);
	            activityData.put("details", activities);
	        } else {
	            activityData.put("status", "Pending");
	            activityData.put("details", Collections.emptyList());
	        }

	        // 3️⃣ Add computed status and activity info
	        streetResponse.put("status", overallStatus);
	        streetResponse.put("activities", activityData);

	        // 4️⃣ Filter based on the requested status (pending/completed)
	        if (status == null || status.isEmpty() || overallStatus.equalsIgnoreCase(status)) {
	            finalList.add(streetResponse);
	        }
	    }

	    // 5️⃣ Build response
	    Map<String, Object> response = new LinkedHashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Street List for Ward " + ward + " (" + status + ")");
	    response.put("ward", ward);
	    response.put("filter_status", status);
	    response.put("total", finalList.size());
	    response.put("data", finalList);

	    return Collections.singletonList(response);
	}
	
	// For Rating
	@Transactional
	public List<Map<String, Object>> getStreetListForRating(String loginid, String street_id, String ward) {

	    String sqlStreet = "SELECT sid, zone, ward, gis_street_id, street_name, road_type, " +
	            "street_length, isactive, isdelete, inby, cdate " +
	            "FROM street_list_master " +
	            "WHERE gis_street_id = ? AND ward = ? AND isactive=1 AND isdelete=0";

	    List<Map<String, Object>> streetList = jdbcStreetCleaningTemplate.queryForList(sqlStreet, street_id, ward);
	    List<Map<String, Object>> finalList = new ArrayList<>();

	    for (Map<String, Object> street : streetList) {
	        Map<String, Object> streetResponse = new HashMap<>();
	        streetResponse.putAll(street);

	        // 1️⃣ Fetch latest cleaning activity
	        String sqlActivity = "SELECT activity_id, gis_street_id, video_file, left_file, right_file, " +
	                "work_status, cleaned_meter, inby, indate, isactive, isdelete, latitude, longitude, " +
	                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', video_file) AS video_url, " +
	                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', left_file) AS left_file_url, " +
	                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', right_file) AS right_file_url " +
	                "FROM cleaning_activity " +
	                "WHERE gis_street_id = ? AND ward = ? AND isactive=1 AND isdelete=0 " +
	                "ORDER BY indate DESC LIMIT 1";

	        List<Map<String, Object>> activities = jdbcStreetCleaningTemplate.queryForList(
	                sqlActivity, street.get("gis_street_id"), street.get("ward")
	        );

	        Map<String, Object> activityData = new HashMap<>();
	        String overallStatus = "Pending";

	        if (!activities.isEmpty()) {
	            Map<String, Object> lastActivity = activities.get(0);
	            String workStatus = (String) lastActivity.get("work_status");

	            if ("Completed".equalsIgnoreCase(workStatus)) {
	                overallStatus = "Completed";
	            } else {
	                overallStatus = "Pending";
	            }

	            activityData.put("status", overallStatus);
	            activityData.put("details", activities);
	        } else {
	            activityData.put("status", "Pending");
	            activityData.put("details", Collections.emptyList());
	        }

	        // ❗️Skip if not completed
	        if (!"Completed".equalsIgnoreCase(overallStatus)) {
	            continue;
	        }

	        // 2️⃣ Fetch rating info (only for Completed)
	        Map<String, Object> ratingData = new HashMap<>();
	        String sqlRating = "SELECT rating_id, gis_street_id, video_file, left_file, right_file, " +
	                "work_rating, inby, indate, isactive, isdelete, latitude, longitude, " +
	                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', video_file) AS video_url, " +
	                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', left_file) AS left_file_url, " +
	                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', right_file) AS right_file_url " +
	                "FROM cleaning_activity_rating " +
	                "WHERE gis_street_id = ? AND ward = ? AND isactive=1 AND isdelete=0 " +
	                "ORDER BY indate DESC LIMIT 1";

	        List<Map<String, Object>> ratings = jdbcStreetCleaningTemplate.queryForList(
	                sqlRating, street.get("gis_street_id"), street.get("ward")
	        );

	        if (!ratings.isEmpty()) {
	            ratingData.put("Rating_status", "Completed");
	            ratingData.put("Rating_details", ratings);
	        } else {
	            ratingData.put("Rating_status", "Pending");
	            ratingData.put("Rating_details", Collections.emptyList());
	        }

	        // 3️⃣ Merge into final response
	        streetResponse.put("status", overallStatus);
	        streetResponse.put("activities", activityData);
	        streetResponse.put("rating", ratingData);

	        finalList.add(streetResponse);
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Completed Street List with Ratings.");
	    response.put("data", finalList);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveRatingActivity(
	        MultipartFile video_file,
	        MultipartFile left_file,
	        MultipartFile right_file,
	        String work_rating,
	        String zone,
	        String ward,
	        String gis_street_id,
	        String street_name,
	        String road_type,
	        String latitude,
	        String longitude,
	        String inby
	) {
		
		String filetxt =gis_street_id;
	    String action = "rating_"+inby;
	    
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    // For Left Image
	    String filetype = "image";
	    String leftIMG = fileUpload(action, filetxt, left_file, filetype);

	    if ("error".equalsIgnoreCase(leftIMG)) {
	        response.put("status", "error");
	        response.put("message", "Street cleaning rating activity left image insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    // For Right Image	    
	    String rightIMG = fileUpload(action, filetxt, right_file, filetype);

	    if ("error".equalsIgnoreCase(rightIMG)) {
	        response.put("status", "error");
	        response.put("message", "Street cleaning rating activity right image insert failed.");
	        result.add(response);
	    }    
	    
	    // For Video
	    filetype = "video";
	    String video = fileUpload(action, filetxt, video_file, filetype);
	    
	    if ("error".equalsIgnoreCase(video)) {
	        response.put("status", "error");
	        response.put("message", "Street cleaning rating activity video insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    String insertBeforeSql = "INSERT INTO `cleaning_activity_rating`"
	    		+ "(`gis_street_id`, `video_file`, `left_file`, `right_file`, `work_rating`, "
	    		+ " `inby`, `latitude`, `longitude`, `zone`, `ward`) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?)";

	    String insertSql = insertBeforeSql;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcStreetCleaningTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"rating_id"});
	        int i = 1;
	        ps.setString(i++, gis_street_id);
	        ps.setString(i++, video);
	        ps.setString(i++, leftIMG);
	        ps.setString(i++, rightIMG);
	        ps.setString(i++, work_rating);
	        ps.setString(i++, inby);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("Street Cleaning rating", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", "Street Cleaning rating activity inserted successfully.");
	    } else {
	    	response.put("Street Cleaning rataing", action + " insertId error!");
	        response.put("status", "error");
	        response.put("message", "Street Cleaning rating activity insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
}
