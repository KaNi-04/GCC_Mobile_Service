package in.gov.chennaicorporation.mobileservice.foodDistribution.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class DriverService {
	private JdbcTemplate jdbcFoodTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlGccFoodDistributionSource") DataSource FoodDistributionSource) {
		this.jdbcFoodTemplate = new JdbcTemplate(FoodDistributionSource);
	}
    
    @Autowired
	public DriverService(Environment environment) {
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
		String serviceFolderName = environment.getProperty("fooddistribution_foldername");
		var year = DateTimeUtil.getCurrentYear();
		var month = DateTimeUtil.getCurrentMonth();
		var date = DateTimeUtil.getCurrentDay();

		uploadDirectory = uploadDirectory + serviceFolderName + "driver/" + filetype + "/" + year + "/" + month + "/" + date;

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

			String filepath_txt = "/" + serviceFolderName + "driver/" + filetype + "/" + year + "/" + month + "/" + date + "/" + fileName;

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
				System.out.println("Activity: SluicePoint");
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
	
	@Transactional
	public List<Map<String, Object>> versionCheck() {
		String sqlQuery = "SELECT * FROM `app_version` WHERE `isactive`=1 LIMIT 1"; 
		List<Map<String, Object>> result = jdbcFoodTemplate.queryForList(sqlQuery);
		return result;
	}
	
	public String getWardByLoginId(String loginid) {
	    String sqlQuery = "SELECT `ward` FROM `location_mapping` WHERE `siloginid` = ? AND `isactive`=1 AND `isdelete`=0 LIMIT 1";
	    
	    // Query the database using queryForList
	    List<Map<String, Object>> results = jdbcFoodTemplate.queryForList(sqlQuery, loginid);
	    
	    // Check if results is not empty and extract the ward value
	    if (!results.isEmpty()) {
	        // Extract the ward value from the first result
	        return (String) results.get(0).get("ward");
	    }
	    
	    // Handle the case where no result is found
	    return "000";  // or return null based on your needs
	}
	
	@Transactional
	public List<Map<String, Object>> getloginList(String username, String password, String usertype) {
		
		String sqlQuery = ""; 
		List<Map<String, Object>> result=null;
		try {
            switch (usertype) {
                case "driver":
                	sqlQuery = "SELECT `loginid`, `name`, `username`  FROM `driver_login` WHERE (`username`=? AND `password`=?) AND (`isactive`=1 AND `isdelete`=0) LIMIT 1";
                	result = jdbcFoodTemplate.queryForList(sqlQuery, username, password);
                    break;
                case "vendor":
                	sqlQuery = "SELECT `loginid`, `name`, `username`, `hub_id`  FROM `driver_login` WHERE (`username`=? AND `password`=?) AND (`isactive`=1 AND `isdelete`=0) LIMIT 1";
                    result = jdbcFoodTemplate.queryForList(sqlQuery, username, password);
                    break;
                // additional cases can be added here
                default:
                	sqlQuery = "SELECT `loginid`, `name`, `username`  FROM `driver_login` WHERE (`username`=? AND `password`=?) AND (`isactive`=1 AND `isdelete`=0) LIMIT 1";
                    result = jdbcFoodTemplate.queryForList(sqlQuery, username, password);
                    //result = Collections.emptyList();
                    break;
            }
        } catch (Exception e) {
            // Log the error
            e.printStackTrace();
            // Return an error message as part of the result list
            return Collections.singletonList(Collections.singletonMap("error", "An error occurred while fetching data."));
        }
        
        // If no records found, return a status and message indicating no user found
        if (result == null || result.isEmpty()) {
            Map<String, Object> response = Map.of(
                "status", 404,
                "message", "User not found."
            );
            return Collections.singletonList(response);
        }
        
        // Add a success status and message to the result
        Map<String, Object> successResponse = Map.of(
            "status", 200,
            "message", "login successfuly.",
            "data", result
        );
        return Collections.singletonList(successResponse);
    }
	
	public List<Map<String, Object>> getDeliveryLocation(String loginid) {
		
		//String searchDate = convertDateFormat(date,0);
		
	    String sql = "SELECT * FROM `location_mapping` WHERE `driverid`=? AND `isactive`=1 AND `isdelete`=0";
	    List<Map<String, Object>> DeliveryLocation = jdbcFoodTemplate.queryForList(sql, loginid);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Delivery Location Details.");
	    response.put("data", DeliveryLocation);

	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getDriverStatus(String siloginid) {

	    Map<String, Object> response = new HashMap<>();

	    // 1. Get driverid using siloginid
	    String sql1 = "SELECT driverid FROM location_mapping " +
	                  "WHERE siloginid = ? AND isactive = 1 AND isdelete = 0";

	    List<Map<String, Object>> driverList = jdbcFoodTemplate.queryForList(sql1, siloginid);

	    if (driverList.isEmpty()) {
	        response.put("status", "Failed");
	        response.put("message", "No driver found");
	        response.put("data", Collections.emptyList());
	        return Collections.singletonList(response);
	    }

	    // Extract driverid
	    Object driverIdObj = driverList.get(0).get("driverid");

	    if (driverIdObj == null) {
	        response.put("status", "Failed");
	        response.put("message", "DriverID is null");
	        response.put("data", Collections.emptyList());
	        return Collections.singletonList(response);
	    }

	    String driverId = driverIdObj.toString();

	    // 2. Get latest start/stop entry
	    String sql = 
	    	    "SELECT dss.*, " +
	    	    "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', dss.image) AS photourl, " +
	    	    "lm.id AS mapping_id, lm.zone, lm.zone_name, lm.ward, lm.location, " +
	    	    "lm.latitude, lm.logitude, lm.si_name, lm.si_number " +
	    	    "FROM driver_start_stop dss " +
	    	    "JOIN location_mapping lm ON lm.driverid = dss.loginid " +
	    	    "WHERE dss.loginid = ? AND dss.isactive = 1 AND dss.isdelete = 0 " +
	    	    "ORDER BY dss.locationid DESC LIMIT 1";

	    	List<Map<String, Object>> driverStatus = jdbcFoodTemplate.queryForList(sql, driverId);

	    response.put("status", "Success");
	    response.put("message", "Driver Status & Last Delivery Location Details");
	    response.put("data", driverStatus);

	    return Collections.singletonList(response);
	}

	public List<Map<String, Object>> getLiveTrack(String siloginid, String date) {

	    Map<String, Object> response = new HashMap<>();

	    try {
	        // 1. Get driverid using siloginid
	        String sql1 = "SELECT driverid FROM location_mapping " +
	                      "WHERE siloginid = ? AND isactive = 1 AND isdelete = 0";

	        List<Map<String, Object>> deliveryLocation = jdbcFoodTemplate.queryForList(sql1, siloginid);

	        if (deliveryLocation.isEmpty()) {
	            response.put("status", "Failed");
	            response.put("message", "No driver found");
	            response.put("data", Collections.emptyList());
	            return Collections.singletonList(response);
	        }

	        // Extract driverid
	        Object driverIdObj = deliveryLocation.get(0).get("driverid");

	        if (driverIdObj == null) {
	            response.put("status", "Failed");
	            response.put("message", "DriverID is null");
	            response.put("data", Collections.emptyList());
	            return Collections.singletonList(response);
	        }

	        String loginid = driverIdObj.toString();

	        // 2. Convert Date
	        String searchDate = convertDateFormat(date, 0);

	        // 3. Fetch Live Track
	        String sql2 = "SELECT * FROM driver_live_track " +
	                      "WHERE loginid = ? AND DATE(datetime) = ? AND isactive = 1 AND isdelete = 0 " +
	                      "ORDER BY datetime";

	        List<Map<String, Object>> liveTrack =
	                jdbcFoodTemplate.queryForList(sql2, loginid, searchDate);

	        // 4. Prepare Response
	        response.put("status", "Success");
	        response.put("message", "Live Track Details.");
	        response.put("data", liveTrack);

	    } catch (Exception e) {
	        response.put("status", "Failed");
	        response.put("message", "Error: " + e.getMessage());
	        response.put("data", Collections.emptyList());
	    }

	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getDriverLastStartStop(String loginid) {
		
	    String sql = "SELECT * FROM `driver_start_stop` WHERE `loginid`=? ORDER BY locationid DESC LIMIT 1";
	    List<Map<String, Object>> liveTrack = jdbcFoodTemplate.queryForList(sql, loginid);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Driver Status Details.");
	    response.put("data", liveTrack);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveStartStop(
	        String loginid,
	        String latitude,
	        String longitude,
	        String action,
	        String startid,
	        String driver_live_mobile,
	        MultipartFile image,
	        String address,
	        String driver_name
	) {	
		
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();
	    
	    String filetype = "image";

		 // Q1 Image
		 final String[] image_path = { null };
		 if (image != null && !image.isEmpty()) {
			 image_path[0] = fileUpload(loginid, action, image, filetype);
		     if ("error".equalsIgnoreCase(image_path[0])) {
		         response.put("status", "error");
		         response.put("message", "Feedback image upload failed.");
		         result.add(response);
		         return result;
		     }
		 }
		 
	    String insertSqltxt = "INSERT INTO `driver_start_stop`(`latitude`, `longitude`, `image`,`loginid`,`action`,`startid`,`driver_live_mobile`,`address`,`driver_name`) "
                + "VALUES (?,?,?,?,?,?,?,?,?)";

	    String insertSql = insertSqltxt;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcFoodTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"requestid"});
	        int i = 1;
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, image_path[0]);
	        ps.setString(i++, loginid);
	        ps.setString(i++, action);
	        ps.setString(i++, startid);
	        ps.setString(i++, driver_live_mobile);
	        ps.setString(i++, address);
	        ps.setString(i++, driver_name);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("Driver Point", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", action + " data inserted successfully.");
	        
	        saveLiveLocation(loginid,latitude,longitude,address); // Save location In Live Track on start and stop
	    
	    } else {
	    	response.put("Driver Point", action + " insertId error!");
	        response.put("status", "error");
	        response.put("message", action + " data insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
	
	@Transactional
	public List<Map<String, Object>> saveLiveLocation(
	        String loginid,
	        String latitude,
	        String longitude,
	        String address
	) {	
		String action = "Live Location";
		
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();
		 
	    String insertSqltxt = "INSERT INTO `driver_live_track`(`loginid`, `latitude`, `longitude`, `address`) "
                + "VALUES (?,?,?,?)";

	    String insertSql = insertSqltxt;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcFoodTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"requestid"});
	        int i = 1;
	        ps.setString(i++, loginid);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, address);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("Driver", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", action + " data inserted successfully.");
	    } else {
	    	response.put("Driver", action + " insertId error!");
	        response.put("status", "error");
	        response.put("message", action + " data insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
	
	// Vendor
	public List<Map<String, Object>> getPendingDetails(String loginid, String shiftid, String date, String hub_id) {

		String searchDate = convertDateFormat(date, 0);
		int hubIdInt = hub_id.isEmpty() ? 0 : Integer.parseInt(hub_id);
		/*
	
	    // 1. Get shortfall details
		String sql = "SELECT "
				+ "    dr.required_date, "
				+ "    dr.shiftid, "
				+ "    sm.Code, "
				+ "    sm.name, "
				+ "    sm.hint, "
				+ "    sm.time, "
				+ "    sm.enable_time, "
				+ "    sm.disable_time, "
				+ "    (dr.permanent + dr.nulm + dr.private + dr.others + dr.nmr) - "
				+ "    ("
				+ "        (dr.weeklyoff_permanent + dr.weeklyoff_nulm + dr.weeklyoff_private + dr.weeklyoff_others + dr.weeklyoff_nmr) + "
				+ "        (dr.absentees_permanent + dr.absentees_nulm + dr.absentees_private + dr.absentees_others + dr.absentees_nmr) "
				+ "    ) AS sum_total_food_requested "
				+ "FROM daily_request dr "
				+ "JOIN shift_master sm "
				+ "      ON sm.shiftid = dr.shiftid "
				+ "WHERE dr.required_date = ? "
				+ "  AND dr.shiftid = ? "
				+ "  AND dr.isactive = 1 "
				+ "  AND dr.isdelete = 0"
				+ " GROUP BY dr.required_date, dr.shiftid";
		*/
		// 1. Get shortfall details
		String sql =
			    "SELECT " +
			    "    dr.required_date, " +
			    "    dr.shiftid, " +
			    "    sm.Code, " +
			    "    sm.name, " +
			    "    sm.hint, " +
			    "    sm.time, " +
			    "    sm.enable_time, " +
			    "    sm.disable_time, " +
			    "    SUM( " +
			    "        (dr.permanent + dr.nulm + dr.private + dr.others + dr.nmr) - " +
			    "        ( " +
			    "            (dr.weeklyoff_permanent + dr.weeklyoff_nulm + dr.weeklyoff_private + dr.weeklyoff_others + dr.weeklyoff_nmr) + " +
			    "            (dr.absentees_permanent + dr.absentees_nulm + dr.absentees_private + dr.absentees_others + dr.absentees_nmr) " +
			    "        ) " +
			    "    ) AS sum_total_food_requested " +
			    "FROM daily_request dr " +
			    "JOIN shift_master sm " +
			    "      ON sm.shiftid = dr.shiftid " +
			    "WHERE dr.required_date = ? " +
			    "  AND dr.shiftid = ? " +
				"  AND dr.hub_id = ? " +
			    "  AND dr.isactive = 1 " +
			    "  AND dr.isdelete = 0 " +
			    "GROUP BY dr.required_date, dr.shiftid";

	    List<Map<String, Object>> shortfallDetails = jdbcFoodTemplate.queryForList(sql, searchDate, shiftid, hubIdInt);

	    // 2. Get requested food details
	    sql = "SELECT "
	    		+ "    dr.requestid, "
	    		+ "	dr.request_by, "
	    		+ "    dr.request_date, "
	    		+ "    (dr.permanent + dr.nulm + dr.private + dr.others + dr.nmr) - "
	    		+ "    ( "
	    		+ "        (dr.weeklyoff_permanent + dr.weeklyoff_nulm + dr.weeklyoff_private + dr.weeklyoff_others + dr.weeklyoff_nmr) + "
	    		+ "        (dr.absentees_permanent + dr.absentees_nulm + dr.absentees_private + dr.absentees_others + dr.absentees_nmr) "
	    		+ "    ) AS total_food_requested, "
	    		+ "    lm.id AS mapping_id, "
	    		+ "    lm.zone, "
	    		+ "    lm.zone_name, "
	    		+ "    lm.ward, "
	    		+ "    lm.location, "
	    		+ "    lm.Latitude, "
	    		+ "    lm.logitude, "
	    		+ "    lm.si_name, "
	    		+ "    lm.si_number, "
	    		+ "    lm.driverid, "
	    		+ "    lm.siloginid "
	    		+ "FROM daily_request dr "
	    		+ "JOIN location_mapping lm "
	    		+ "      ON dr.request_by = lm.siloginid "
	    		+ "WHERE "
	    		+ "    dr.required_date = ? "
	    		+ "    AND dr.shiftid = ? "
				+ "    AND dr.hub_id = ? "
	    		+ "    AND dr.isactive = 1 "
	    		+ "    AND dr.isdelete = 0 "
	    		+ "    AND lm.isactive = 1 "
	    		+ "    AND lm.isdelete = 0";

	    List<Map<String, Object>> requestedInfo = jdbcFoodTemplate.queryForList(sql, searchDate, shiftid, hubIdInt);
	    
	    
	    /*
		// 2. Get requested food details
	    sql = "SELECT `foodid`, `type`, `name`, `isactive`, `isdelete` FROM `food_list_master` WHERE `isactive`=1 AND `isdelete`=0";

	    List<Map<String, Object>> foodInfo = jdbcFoodTemplate.queryForList(sql);
	    
	    // 2. Combine both list results
	    // Combine request info (shortfall + requested)
	    //List<Map<String, Object>> requestInfo = new ArrayList<>();
	    //requestInfo.addAll(shortfallDetails);
	    //requestInfo.addAll(requestedInfo);
	    */
	    // Final response structure
	    Map<String, Object> data = new HashMap<>();
	    data.put("request_info", requestedInfo);
	    data.put("summary_info", shortfallDetails);

	    // 3. Create final response
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Pending Details.");
	    response.put("data", data);
	    
	    return Collections.singletonList(response);
	}
	
	
	public List<Map<String, Object>> getMenuDetails(String loginid) {

		// Get requested food details
	    String sql = "SELECT `foodid`, `type`, `name`, `isactive`, `isdelete` FROM `food_list_master` WHERE `isactive`=1 AND `isdelete`=0";

	    List<Map<String, Object>> foodInfo = jdbcFoodTemplate.queryForList(sql);
	    
	    // 3. Create final response
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Food Menu Details.");
	    response.put("data", foodInfo);
	    
	    return Collections.singletonList(response);
	}
	
	
	public List<Map<String, Object>> getRequestUpdated(String loginid, String shiftid, String date) {

		String searchDate = convertDateFormat(date, 0);
		
		// Get requested food details
	    String sql = "SELECT `uid`, `shiftid`, `total_food_request_recived`, `total_food_transported`, `foodid`, `indate`, `inby`"
	    		+ " FROM `vendor_food_update` WHERE `shiftid`=? AND DATE(`indate`)=? AND `isactive`=1 AND `isdelete`=0";

	    List<Map<String, Object>> requestUpadteInfo = jdbcFoodTemplate.queryForList(sql,shiftid, searchDate);
	    
	    // 3. Create final response
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Request Update Details.");
	    response.put("data", requestUpadteInfo);
	    
	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveRequestUpadte(
	        String loginid,
	        String shiftid,
	        String total_food_request_recived,
	        String total_food_transported,
	        String foodid
	) {	
		String action = "Food Request Status update";
		
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();
		 
	    String insertSqltxt = "INSERT INTO `vendor_food_update`(`shiftid`, `total_food_request_recived`, `total_food_transported`, `foodid`,`inby`) "
                + "VALUES (?,?,?,?,?)";

	    String insertSql = insertSqltxt;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcFoodTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"requestid"});
	        int i = 1;
	        
	        ps.setString(i++, shiftid);
	        ps.setString(i++, total_food_request_recived);
	        ps.setString(i++, total_food_transported);
	        ps.setString(i++, foodid);
	        ps.setString(i++, loginid);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("Driver", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", action + " data inserted successfully.");
	    } else {
	    	response.put("Driver", action + " insertId error!");
	        response.put("status", "error");
	        response.put("message", action + " data insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
}
