package in.gov.chennaicorporation.mobileservice.busshelters.service;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class BusShelterRenovationActivity {
	private JdbcTemplate jdbcBusShelterTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlBusShelterDataSource") DataSource BusShelterDataSource) {
		this.jdbcBusShelterTemplate = new JdbcTemplate(BusShelterDataSource);
	}
    
    @Autowired
	public BusShelterRenovationActivity(Environment environment) {
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
        String serviceFolderName = environment.getProperty("busshelter_renovation_foldername");
        var year =DateTimeUtil.getCurrentYear();
        var month =DateTimeUtil.getCurrentMonth();
        
        uploadDirectory = uploadDirectory + serviceFolderName + year +"/"+month;
        
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
	        
	        String filepath_txt = "/"+serviceFolderName + year + "/" + month + "/" + fileName;
	        
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
	
	//////////////////////////////
	public String getConfigValue() {
	    String sqlQuery = "SELECT `id`, `lat_long_radius` FROM `config` LIMIT 1";
	    String value = "50"; // default fallback value
	    List<Map<String, Object>> results = jdbcBusShelterTemplate.queryForList(sqlQuery);

	    if (!results.isEmpty()) {
	        Map<String, Object> row = results.get(0);
	        value = String.valueOf(row.get("lat_long_radius")); // corrected key name
	    }

	    return value;
	}
	
	
	public String getWardByLoginId(String loginid, String type) {
	    String sqlQuery = "SELECT `ward` FROM gcc_penalty_hoardings.`hoading_user_list` WHERE `userid` = ? AND `type` = ? LIMIT 1";
	    
	    // Query the database using queryForList
	    List<Map<String, Object>> results = jdbcBusShelterTemplate.queryForList(sqlQuery, loginid, type);
	    
	    // Check if results is not empty and extract the ward value
	    if (!results.isEmpty()) {
	        // Extract the ward value from the first result
	        return (String) results.get(0).get("ward");
	    }
	    
	    // Handle the case where no result is found
	    return "000";  // or return null based on your needs
	}
	
	//////////////////////////////
    
    public List<Map<String, Object>> getBusShelterList(
    		MultiValueMap<String, String> formData,
			String latitude,
			String longitude,
			String loginid
    		) {
    	
    	String sqlWhere = "";
    	
    	String radius = getConfigValue();
    	/*
    	if (latitude != null && longitude != null && !latitude.isBlank() && !latitude.isEmpty() && !longitude.isBlank()
		        && !longitude.isEmpty()) {
		    sqlWhere += " AND ((6371008.8 * acos((cos(radians(" + latitude
		            + ")) * cos(radians(bsl.latitude)) * cos(radians(bsl.longitude) - radians(" + longitude
		            + ")) + sin(radians(" + latitude + ")) * sin(radians(bsl.latitude))))) < "+radius+")"
		            + " ORDER BY"
					+ "    bsl.`id` DESC";
		}
    	*/
    	
    	String ward = getWardByLoginId(loginid,"ae");
    	
    	sqlWhere = " AND (bsr.`stage_status` <> 'Completed' OR bsr.`stage_status` is NULL) ORDER BY bsl.`id` DESC";
    	
        String sql = "SELECT "
        		+ "    bsl.`id`, "
        		+ "    bsl.`zone`, "
        		+ "    bsl.`ward`, "
        		+ "    bsl.`latitude`, "
        		+ "    bsl.`longitude`, "
        		+ "    bsl.`road`, "
        		+ "    bsl.`category`, "
        		+ "    bsr.renovation_required, "
        		+ "    IF(bsr.shelter_image IS NOT NULL AND bsr.shelter_image != '', "
        		+ "       CONCAT('" + fileBaseUrl + "/gccofficialapp/files', bsr.shelter_image), "
        		+ "       NULL) AS shelter_image, "
        		+ "	   COALESCE(bsr.stage_status,'Pending') AS stage_status "
        		+ "FROM `bus_shelter_list` bsl "
        		+ "LEFT JOIN `bus_shelter_renovation` bsr "
        		+ "    ON bsr.bsid = bsl.id AND bsr.isactive = 1 "
        		+ "WHERE bsl.isactive = 1 AND bsl.ward = ? "+sqlWhere;
        
        //System.out.println(sql);
        
        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql,ward);
        
        ObjectMapper mapper = new ObjectMapper();
        for (Map<String, Object> row : result) {
        	
            Object renovation_required = row.get("renovation_required");
            
            if (renovation_required != null) {
            	sql = "SELECT COALESCE(SUM(`weightage`), 0) AS total_weightage "
            		+ "FROM `bus_shelter_renovation_data` "
            		+ "WHERE bsid = ?";

            	List<Map<String, Object>> presentage = jdbcBusShelterTemplate.queryForList(sql, row.get("id"));

            	// Safely get the first result
            	if (!presentage.isEmpty()) {
            		row.put("stage_presentage", presentage.get(0).get("total_weightage"));
            	} else {
            		row.put("stage_presentage", 0);
            	}
            } else {
            	row.put("stage_presentage", 0);
            }
          
        }
        
        
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Bus Shelter List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getBusCompletedShelterList(
    		MultiValueMap<String, String> formData,
			String latitude,
			String longitude,
			String loginid
    		) {
    	
    	String sqlWhere = "";
    	String radius = getConfigValue();
    	/*
    	if (latitude != null && longitude != null && !latitude.isBlank() && !latitude.isEmpty() && !longitude.isBlank()
		        && !longitude.isEmpty()) {
		    sqlWhere += " AND ((6371008.8 * acos((cos(radians(" + latitude
		            + ")) * cos(radians(bsl.latitude)) * cos(radians(bsl.longitude) - radians(" + longitude
		            + ")) + sin(radians(" + latitude + ")) * sin(radians(bsl.latitude))))) < "+radius+")"
		            + " ORDER BY"
					+ "    bsl.`id` DESC";
		}
    	*/
        String sql = "SELECT "
        		+ "    bsl.`id`, "
        		+ "    bsl.`zone`, "
        		+ "    bsl.`ward`, "
        		+ "    bsl.`latitude`, "
        		+ "    bsl.`longitude`, "
        		+ "    bsl.`road`, "
        		+ "    bsl.`category`, "
        		+ "    bsr.renovation_required, "
        		+ "    IF(bsr.shelter_image IS NOT NULL AND bsr.shelter_image != '', "
        		+ "       CONCAT('" + fileBaseUrl + "/gccofficialapp/files', bsr.shelter_image), "
        		+ "       NULL) AS shelter_image, "
        		+ "	   COALESCE(bsr.stage_status,'Pending') AS stage_status "
        		+ "FROM `bus_shelter_list` bsl "
        		+ "LEFT JOIN `bus_shelter_renovation` bsr "
        		+ "    ON bsr.bsid = bsl.id AND bsr.isactive = 1 "
        		+ "WHERE bsl.isactive = 1 AND bsr.stage_status='Completed'"; // +sqlWhere;
        
        System.out.println(sql);
        
        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql);
        
        ObjectMapper mapper = new ObjectMapper();
        for (Map<String, Object> row : result) {
        	
            Object renovation_required = row.get("renovation_required");
            
            if (renovation_required != null) {
            	sql = "SELECT COALESCE(SUM(`weightage`), 0) AS total_weightage "
            		+ "FROM `bus_shelter_renovation_data` "
            		+ "WHERE bsid = ?";

            	List<Map<String, Object>> presentage = jdbcBusShelterTemplate.queryForList(sql, row.get("id"));

            	// Safely get the first result
            	if (!presentage.isEmpty()) {
            		row.put("stage_presentage", presentage.get(0).get("total_weightage"));
            	} else {
            		row.put("stage_presentage", 0);
            	}
            } else {
            	row.put("stage_presentage", 0);
            }
          
        }
        
        
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Bus Shelter Completed List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getStageList() {
        String sql = "SELECT * FROM `renovation_statge` WHERE isactive=1 ORDER BY orderby";
        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Bus Shelter Stages.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getBusShelterDetails(
    		MultiValueMap<String, String> formData,
			String id,
			String loginid,
			String latitude, 
			String longitude,
			String renovation_required_condition
    		) {
    	
    	String sqlWhere = "";
    	//String radius = getConfigValue();
    	
        String sql = "SELECT "
        		+ "    bsl.`id`, "
        		+ "    bsl.`zone`, "
        		+ "    bsl.`ward`, "
        		+ "    bsl.`latitude`, "
        		+ "    bsl.`longitude`, "
        		+ "    bsl.`road`, "
        		+ "    bsl.`category`, "
        		+ "    bsr.renovation_required, "
        		+ "    IF(bsr.shelter_image IS NOT NULL AND bsr.shelter_image != '', "
        		+ "       CONCAT('" + fileBaseUrl + "/gccofficialapp/files', bsr.shelter_image), "
        		+ "       NULL) AS shelter_image, "
        		+ "	   COALESCE(bsr.stage_status,'Pending') AS stage_status "
        		+ "FROM `bus_shelter_list` bsl "
        		+ "LEFT JOIN `bus_shelter_renovation` bsr "
        		+ "    ON bsr.bsid = bsl.id AND bsr.isactive = 1 "
        		+ "WHERE bsl.id = ? ";
        
        System.out.println(sql);
        
        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql, id);
        
        ObjectMapper mapper = new ObjectMapper();
        for (Map<String, Object> row : result) {
        	
            Object renovation_required = row.get("renovation_required");
            
            if (renovation_required != null) {
            	sql = "SELECT COALESCE(SUM(`weightage`), 0) AS total_weightage "
            		+ "FROM `bus_shelter_renovation_data` "
            		+ "WHERE bsid = ?";

            	List<Map<String, Object>> presentage = jdbcBusShelterTemplate.queryForList(sql, row.get("id"));

            	// Safely get the first result
            	if (!presentage.isEmpty()) {
            		row.put("stage_presentage", presentage.get(0).get("total_weightage"));
            	} else {
            		row.put("stage_presentage", 0);
            	}
            } else {
            	row.put("stage_presentage", 0);
            }
            
            // Location Check 
            double shelterLat = Double.parseDouble(row.get("latitude").toString());
        	double shelterLng = Double.parseDouble(row.get("longitude").toString());
        	double userLat = Double.parseDouble(latitude);
        	double userLng = Double.parseDouble(longitude);

        	// Calculate distance using Haversine formula
        	double earthRadius = 6371008.8; // in meters
        	double dLat = Math.toRadians(userLat - shelterLat);
        	double dLng = Math.toRadians(userLng - shelterLng);
        	double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        	         + Math.cos(Math.toRadians(shelterLat)) * Math.cos(Math.toRadians(userLat))
        	         * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        	double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        	double distance = earthRadius * c;

        	double radiusInMeters = Double.parseDouble(getConfigValue());
        	row.put("user_on_correct_location", distance <= radiusInMeters); // true if within radius meters
        	////////
        	
        	if(!"0".equals(renovation_required_condition)) {
        	
	            // Stages
	            List<Map<String, Object>> stageListRaw = getStageList();
	            List<Map<String, Object>> stageList = (List<Map<String, Object>>) stageListRaw.get(0).get("data");
	            List<Map<String, Object>> stagedata = new ArrayList<>();
	
	            for (Map<String, Object> stage : stageList) {
	                Map<String, Object> stageEntry = new HashMap<>(stage); // Clone base stage info
	                
	                stageEntry.put("stageid", stage.get("sid"));
	                stageEntry.put("stage_name", stage.get("name"));
	                stageEntry.put("weightage", stage.get("weightage"));
	                stageEntry.put("orderby", stage.get("orderby"));
	                
	                sql = "SELECT `dataid`, `bsid`, `stageid`, "
	                		+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `image`) AS `image`, "
	                		+ "`remarks`, `weightage`, "
	                		+ "`zone`, `ward`, `latitude`, `longitude`, `streetname`, `streetid`, "
	                		+ " DATE_FORMAT(`cdate`, '%d-%m-%Y') AS `cdate`, "
	                		+ "`cby`, `isactive` FROM `bus_shelter_renovation_data` "
	                		+ "WHERE `bsid` = ? AND `stageid` = ? AND isactive = 1";
	
	                List<Map<String, Object>> resultStageData = jdbcBusShelterTemplate.queryForList(sql, row.get("id"), stage.get("sid"));
	
	                if (!resultStageData.isEmpty()) {
	                    Map<String, Object> stageDataRow = resultStageData.get(0);
	                    stageEntry.put("stage_image", stageDataRow.get("image"));
	                    stageEntry.put("stage_remarks", stageDataRow.get("remarks"));
	                    stageEntry.put("stage_date", stageDataRow.get("cdate"));
	                    stageEntry.put("stage_status", "Completed");
	                    stageEntry.put("stage_presentage", stageDataRow.get("weightage"));
	                } else {
	                    // Defaults if no data available
	                    stageEntry.put("stage_image", null);
	                    stageEntry.put("stage_remarks", null);
	                    stageEntry.put("stage_date", null);
	                    stageEntry.put("stage_status", "Pending");
	                    stageEntry.put("stage_presentage", 0);
	                }
	
	                stagedata.add(stageEntry);
	            }
	            row.put("stages", stagedata);
        	}
            
        }
        
        
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Bus Shelter With Stage Details.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
    
    @Transactional
    public String addRenovationBaseData(
    		String shelter_id, String renovation_required, String image, 
    		String latitude, String longitude, String zone, String ward, 
    		String streetname, String streetid, String stage_status
    		) {
    	String sql ="INSERT INTO `bus_shelter_renovation`("
    			+ "`bsid`, `renovation_required`, `shelter_image`, "
    			+ "`latitude`, `longitude`, `zone`, `ward`, `streetname`, `streetid`, "
    			+ "`stage_status`) "
    			+ "VALUES (?,?,?,?,?,?,?,?,?,?)";
    	
    	jdbcBusShelterTemplate.update(sql, shelter_id, renovation_required, image, latitude, longitude , zone, ward, streetname, streetid, stage_status);
    	return "success";
    }
    
    public String updateStageStatus(String shelter_id, String stage_status, String datetxt) {
        
		String sql = "UPDATE `bus_shelter_renovation` SET `stage_status`=?,`lastcdate` =? WHERE `bsid`=?";
		jdbcBusShelterTemplate.update(sql,stage_status, datetxt, shelter_id);
		
		return "sussess";
    }
	
    @Transactional
	public List<Map<String, Object>> saveStageActivityData(
			String shelter_id,
			String cby,
			String latitude,
			String longitude, 
			String zone, 
			String ward,
			String streetname,
			String streetid,
			String stageid,
			String remarks,
			String weightage,
			MultipartFile file,
			
			MultipartFile stage_file,
			String renovation_required) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;

		String image = "";
		String stage_image = "";
		
		var year = DateTimeUtil.getCurrentYear();
		var month = DateTimeUtil.getCurrentMonth();
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			image = fileUpload("bushshelter", shelter_id, file);
		}
		
		// Question Images
		if (stage_file != null && !stage_file.isEmpty()) {
			stage_image = fileUpload("stage", stageid, stage_file);
		}
		
		String baseimg = image;
		
		String stage_image_txt = stage_image;
		
		//String type = // Get before & after
		
		// Get today's date
        LocalDate today = LocalDate.now();

        // Format it in the desired format (yyyy-MM-dd)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String todayDate = today.format(formatter);
        
        String stage_status = "Pending";
        
        if("0".equals(renovation_required) || "1".equals(renovation_required)) {
        	
        	addRenovationBaseData(shelter_id, renovation_required, image, latitude, longitude , zone, ward, streetname, streetid, stage_status);
        }
        if("0".equals(renovation_required)) {
        	stage_status = "Completed";
        	updateStageStatus(shelter_id, stage_status, todayDate);
        }
        if(!"0".equals(renovation_required)) {
			String sqlQuery = "INSERT INTO `bus_shelter_renovation_data`("
					+ "`bsid`, `stageid`, `image`, `remarks`, `weightage`, `zone`, "
					+ "`ward`, `latitude`, `longitude`, `streetname`, `streetid`, `cby`"
					+ ") "
					+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
	
			KeyHolder keyHolder = new GeneratedKeyHolder();
			
			try {
				int affectedRows = jdbcBusShelterTemplate.update(new PreparedStatementCreator() {
					@Override
					public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
						PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "dataid" });
						ps.setString(1, shelter_id);
						ps.setString(2, stageid);
						ps.setString(3, stage_image_txt);
						ps.setString(4, remarks);
						ps.setString(5, weightage);
						ps.setString(6, zone);
						ps.setString(7, ward);
						ps.setString(8, latitude);
						ps.setString(9, longitude);
						ps.setString(10, streetname);
						ps.setString(11, streetid);
						ps.setString(12, cby);
						
						return ps;
					}
				}, keyHolder);
	
				if (affectedRows > 0) {
					Number generatedId = keyHolder.getKey();
					lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
					response.put("insertId", lastInsertId);
					response.put("status", "success");
					response.put("message", "A Bus Shelter stage data was inserted successfully!");
					System.out.println("A Bus Shelter stage data was inserted successfully! Bus Shelter ID: " + generatedId);
					
					String sql = "SELECT COALESCE(SUM(`weightage`), 0) AS total_weightage "
		            		+ "FROM `bus_shelter_renovation_data` "
		            		+ "WHERE bsid = ?";
					List<Map<String, Object>> resultStageData = jdbcBusShelterTemplate.queryForList(sql, shelter_id );
					
					Object totalWeightageObj = resultStageData.get(0).get("total_weightage");
					double totalWeightage = totalWeightageObj != null ? Double.parseDouble(totalWeightageObj.toString()) : 0;
	
					if (totalWeightage >= 100) {
					    stage_status = "Completed";
					}
					
					updateStageStatus(shelter_id, stage_status, todayDate);
					
				} else {
					response.put("status", "error");
					response.put("message", "Failed to insert a Bus Shelter stage data. Bus Shelter ID:" + shelter_id);
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
        	response.put("insertId", 0);
			response.put("status", "success");
			response.put("message", "0 - A Bus Shelter stage data was inserted successfully!");
        }
		return Collections.singletonList(response);
	}
    
    // Reports
    
    public List<Map<String, Object>> getZoneList() {
        String sql = "SELECT "
        		+ "    bsl.zone, "
        		+ "    COUNT(DISTINCT bsl.id) AS total_shelters, "
        		+ "    COUNT(DISTINCT CASE WHEN bsr.bsid IS NULL THEN bsl.id END) AS total_not_visited, "
        		+ "    COUNT(DISTINCT CASE WHEN bsr.renovation_required = 1 THEN bsr.bsid END) AS total_need_improvement, "
        		+ "    COUNT(DISTINCT CASE WHEN bsr.renovation_required = 0 THEN bsr.bsid END) AS total_no_improvement, "
        		+ "    COUNT(DISTINCT CASE WHEN bsr.stage_status != 'Completed' AND bsr.stage_status IS NOT NULL THEN bsr.bsid END) AS total_pending, "
        		+ "    COUNT(DISTINCT CASE WHEN bsr.stage_status = 'Completed' THEN bsr.bsid END) AS total_completed "
        		+ "FROM "
        		+ "    bus_shelter_list bsl "
        		+ "LEFT JOIN "
        		+ "    bus_shelter_renovation bsr ON bsr.bsid = bsl.id AND bsr.isactive = 1 "
        		+ "WHERE "
        		+ "    bsl.isactive = 1 "
        		+ "GROUP BY "
        		+ "    bsl.zone "
        		+ "ORDER BY "
        		+ "    bsl.zone";
        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql);
        // Compute totals
        Map<String, Object> totals = new HashMap<>();
        totals.put("zone", "Total");

        String[] keys = {
            "total_shelters", "total_not_visited",
            "total_need_improvement", "total_no_improvement",
            "total_pending", "total_completed"
        };

        for (String key : keys) {
            int sum = result.stream()
                .mapToInt(row -> row.get(key) != null ? ((Number) row.get(key)).intValue() : 0)
                .sum();
            totals.put(key, sum);
        }
        // Add totals row at the end
        result.add(totals);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Bus Shelter Zone Report.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getWardList(String zone) {
        String sql = "SELECT "
        		+ "    bsl.ward,"
        		+ "    COUNT(DISTINCT bsl.id) AS total_shelters,"
        		+ "    COUNT(DISTINCT CASE WHEN bsr.bsid IS NULL THEN bsl.id END) AS total_not_visited,"
        		+ "    COUNT(DISTINCT CASE WHEN bsr.renovation_required = 1 THEN bsr.bsid END) AS total_need_improvement,"
        		+ "    COUNT(DISTINCT CASE WHEN bsr.renovation_required = 0 THEN bsr.bsid END) AS total_no_improvement,"
        		+ "    COUNT(DISTINCT CASE WHEN bsr.stage_status != 'Completed' AND bsr.stage_status IS NOT NULL THEN bsr.bsid END) AS total_pending,"
        		+ "    COUNT(DISTINCT CASE WHEN bsr.stage_status = 'Completed' THEN bsr.bsid END) AS total_completed "
        		+ "FROM "
        		+ "    bus_shelter_list bsl "
        		+ "LEFT JOIN "
        		+ "    bus_shelter_renovation bsr ON bsr.bsid = bsl.id AND bsr.isactive = 1 "
        		+ "WHERE "
        		+ "    bsl.isactive = 1 "
        		+ "    AND bsl.zone = ? "
        		+ "GROUP BY "
        		+ "    bsl.ward "
        		+ "ORDER BY "
        		+ "    bsl.ward";
        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql,zone);
        // Compute totals
        Map<String, Object> totals = new HashMap<>();
        totals.put("ward", "Total");

        String[] keys = {
            "total_shelters", "total_not_visited",
            "total_need_improvement", "total_no_improvement",
            "total_pending", "total_completed"
        };

        for (String key : keys) {
            int sum = result.stream()
                .mapToInt(row -> row.get(key) != null ? ((Number) row.get(key)).intValue() : 0)
                .sum();
            totals.put(key, sum);
        }

        // Add totals row at the end
        result.add(totals);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Bus Shelter Ward Report.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getVistPendingList(String ward) {
        String sql = "SELECT bsl.id, bsl.zone, bsl.ward, bsl.road, bsl.latitude, bsl.longitude " +
                     "FROM bus_shelter_list bsl " +
                     "LEFT JOIN bus_shelter_renovation bsr ON bsr.bsid = bsl.id AND bsr.isactive = 1 " +
                     "WHERE bsl.isactive = 1 " +
                     "AND bsl.ward = ? AND bsr.bsid IS NULL";

        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql, ward);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "Success");
        response.put("message", "Bus Shelters not yet visited (pending) in ward " + ward);
        response.put("data", result);

        return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getRenovationRequiredList(String ward) {
        String sql = "SELECT bsl.id, bsl.zone, bsl.ward, bsl.road, bsl.latitude, bsl.longitude, bsr.stage_status " +
                     "FROM bus_shelter_list bsl " +
                     "LEFT JOIN bus_shelter_renovation bsr ON bsr.bsid = bsl.id AND bsr.isactive = 1 " +
                     "WHERE bsl.isactive = 1 " +
                     "AND bsl.ward = ? AND bsr.renovation_required=1";

        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql, ward);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "Success");
        response.put("message", "Bus Shelters not yet visited (pending) in ward " + ward);
        response.put("data", result);

        return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getRenovationNotRequiredList(String ward) {
        String sql = "SELECT bsl.id, bsl.zone, bsl.ward, bsl.road, bsl.latitude, bsl.longitude, bsr.stage_status " +
                     "FROM bus_shelter_list bsl " +
                     "LEFT JOIN bus_shelter_renovation bsr ON bsr.bsid = bsl.id AND bsr.isactive = 1 " +
                     "WHERE bsl.isactive = 1 " +
                     "AND bsl.ward = ? AND bsr.renovation_required=0";

        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql, ward);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "Success");
        response.put("message", "Bus Shelters improvement Needed List in ward " + ward);
        response.put("data", result);

        return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getWorkPendingList(String ward) {
        String sql = "SELECT bsl.id, bsl.zone, bsl.ward, bsl.road, bsl.latitude, bsl.longitude, bsr.stage_status " +
                     "FROM bus_shelter_list bsl " +
                     "INNER JOIN bus_shelter_renovation bsr ON bsr.bsid = bsl.id AND bsr.isactive = 1 " +
                     "WHERE bsl.isactive = 1 " +
                     "AND bsl.ward = ? AND COALESCE(bsr.stage_status, 'Pending') != 'Completed'";

        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql, ward);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "Success");
        response.put("message", "Bus Shelter improvement Not Needed List for ward " + ward);
        response.put("data", result);

        return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getWorkCompletedList(String ward) {
        String sql = "SELECT bsl.id, bsl.zone, bsl.ward, bsl.road, bsl.latitude, bsl.longitude, bsr.stage_status " +
                     "FROM bus_shelter_list bsl " +
                     "INNER JOIN bus_shelter_renovation bsr ON bsr.bsid = bsl.id AND bsr.isactive = 1 " +
                     "WHERE bsl.isactive = 1 " +
                     "AND bsl.ward = ? AND COALESCE(bsr.stage_status, 'Pending') = 'Completed'";

        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql, ward);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "Success");
        response.put("message", "Bus Shelter work pending list for ward " + ward);
        response.put("data", result);

        return Collections.singletonList(response);
    }
    
    /*
     Zone wise:
     SELECT 
    bsl.zone,
    COUNT(DISTINCT bsl.id) AS total_shelters,
    COUNT(DISTINCT CASE WHEN bsr.bsid IS NULL THEN bsl.id END) AS total_not_visited,
    COUNT(DISTINCT CASE WHEN bsr.renovation_required = 1 THEN bsr.bsid END) AS total_need_improvement,
    COUNT(DISTINCT CASE WHEN bsr.renovation_required = 0 THEN bsr.bsid END) AS total_no_improvement,
    COUNT(DISTINCT CASE WHEN bsr.stage_status != 'Completed' AND bsr.stage_status IS NOT NULL THEN bsr.bsid END) AS total_pending,
    COUNT(DISTINCT CASE WHEN bsr.stage_status = 'Completed' THEN bsr.bsid END) AS total_completed
FROM 
    bus_shelter_list bsl
LEFT JOIN 
    bus_shelter_renovation bsr ON bsr.bsid = bsl.id AND bsr.isactive = 1
WHERE 
    bsl.isactive = 1
GROUP BY 
    bsl.zone
ORDER BY 
    bsl.zone;
    
    // ward wise
      SELECT 
    bsl.ward,
    COUNT(DISTINCT bsl.id) AS total_shelters,
    COUNT(DISTINCT CASE WHEN bsr.bsid IS NULL THEN bsl.id END) AS total_not_visited,
    COUNT(DISTINCT CASE WHEN bsr.renovation_required = 1 THEN bsr.bsid END) AS total_need_improvement,
    COUNT(DISTINCT CASE WHEN bsr.renovation_required = 0 THEN bsr.bsid END) AS total_no_improvement,
    COUNT(DISTINCT CASE WHEN bsr.stage_status != 'Completed' AND bsr.stage_status IS NOT NULL THEN bsr.bsid END) AS total_pending,
    COUNT(DISTINCT CASE WHEN bsr.stage_status = 'Completed' THEN bsr.bsid END) AS total_completed
FROM 
    bus_shelter_list bsl
LEFT JOIN 
    bus_shelter_renovation bsr ON bsr.bsid = bsl.id AND bsr.isactive = 1
WHERE 
    bsl.isactive = 1 AND bsl.isdelete = 0
    AND bsl.zone = '01'
GROUP BY 
    bsl.ward
ORDER BY 
    bsl.ward;
     */  
}
