package in.gov.chennaicorporation.mobileservice.gccSpecialVehicle.service;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class VehicleService {

private JdbcTemplate jdbcVehicleTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlGccSpecialVehicleSource") DataSource VehicleDataSource) {
		this.jdbcVehicleTemplate = new JdbcTemplate(VehicleDataSource);
	}
    
    @Autowired
	public VehicleService(Environment environment) {
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
		String serviceFolderName = environment.getProperty("Vehicle_foldername");
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
				System.out.println("Activity: Special Vehicle");
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
	    List<Map<String, Object>> results = jdbcVehicleTemplate.queryForList(sqlQuery, loginid, type);
	    
	    // Check if results is not empty and extract the ward value
	    if (!results.isEmpty()) {
	        // Extract the ward value from the first result
	        return (String) results.get(0).get("ward");
	    }
	    
	    // Handle the case where no result is found
	    return "000";  // or return null based on your needs
	}
	
	public List<Map<String, Object>> getVehicleList(String loginid) {
	    String sqlVehicle = "SELECT * FROM vehicle_master WHERE isactive = 1 AND isdelete=0";
	    List<Map<String, Object>> vehicles = jdbcVehicleTemplate.queryForList(sqlVehicle);

	    List<Map<String, Object>> finalList = new ArrayList<>();

	    for (Map<String, Object> vehicle : vehicles) {
	        Map<String, Object> vehicleResponse = new HashMap<>(vehicle);

	        String vehicleId = vehicle.get("vehicle_id").toString();
	        String beforeActivityId = "0";

	        // === 1. BEFORE ACTIVITY ===
	        String sqlBefore = "SELECT *, "
	        		+ "DATE_FORMAT(indate, '%d-%m-%Y') AS formatted_indate,"
	        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', `img_file`) AS img_file_url, "
	        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', `video_file`) AS video_file_url "
	                + "FROM before_activity WHERE vehicle_id = ? AND isactive=1 AND isdelete=0 "
	                + "ORDER BY activity_id DESC LIMIT 1";

	        List<Map<String, Object>> beforeActivities = jdbcVehicleTemplate.queryForList(sqlBefore, vehicleId);

	        Map<String, Object> beforeData = new HashMap<>();
	        if (!beforeActivities.isEmpty()) {
	            beforeActivityId = beforeActivities.get(0).get("activity_id").toString();
	            beforeData.put("status", "Completed");
	            beforeData.put("details", beforeActivities);
	        } else {
	            beforeData.put("status", "Pending");
	            beforeData.put("details", Collections.emptyList());
	        }

	        // === 2. AFTER ACTIVITY ===
	        String sqlAfter = "SELECT *, "
	        		+ "DATE_FORMAT(indate, '%d-%m-%Y') AS formatted_indate, "
	        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', `img_file`) AS img_file_url, "
	        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', `video_file`) AS video_file_url "
	                + "FROM after_activity WHERE vehicle_id = ? AND before_activity_id = ? AND isactive=1 AND isdelete=0 "
	                + "ORDER BY activity_id DESC LIMIT 1";

	        List<Map<String, Object>> afterActivities = jdbcVehicleTemplate.queryForList(sqlAfter, vehicleId, beforeActivityId);

	        Map<String, Object> afterData = new HashMap<>();
	        if (!afterActivities.isEmpty()) {
	            afterData.put("status", "Completed");
	            afterData.put("details", afterActivities);
	        } else {
	            afterData.put("status", "Pending");
	            afterData.put("details", Collections.emptyList());
	        }

	        // === 3. OVERALL STATUS ===
	        String overallStatus = (!beforeActivities.isEmpty() && !afterActivities.isEmpty())
	                ? "Completed"
	                : "Pending";

	        vehicleResponse.put("status", overallStatus);
	        vehicleResponse.put("before", beforeData);
	        vehicleResponse.put("after", afterData);

	        finalList.add(vehicleResponse);
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Vehicle List.");
	    response.put("data", finalList);

	    return Collections.singletonList(response);
	}
	
	
	@Transactional
	public List<Map<String, Object>> saveActivity(
	        String vehicle_id,
	        MultipartFile file,
	        MultipartFile video,
	        String zone,
	        String ward,
	        String street_name,
	        String street_id,
	        String latitude,
	        String longitude,
	        String inby,
	        String action,
	        String beforeActivityId,
	        String clean_weight
	) {

		String filetxt =vehicle_id;
		
	    
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    // For Image
	    String filetype = "image";
	    String activityimg = fileUpload(action, filetxt, file, filetype);

	    if ("error".equalsIgnoreCase(activityimg)) {
	        response.put("status", "error");
	        response.put("message", "Special vehicle activity image insert failed.");
	        result.add(response);
	        return result;
	    }

	    // For Video
	    filetype = "video";
	    String activityvideo = fileUpload(action, filetxt, video, filetype);
	    
	    if ("error".equalsIgnoreCase(activityvideo)) {
	        response.put("status", "error");
	        response.put("message", "Special vehicle activity video insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    String insertBeforeSql = "INSERT INTO before_activity(vehicle_id, img_file, video_file, zone, ward, street_name, street_id, latitude, longitude, inby) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?)";

	    
		String insertAfterSql = "INSERT INTO after_activity(vehicle_id, img_file, video_file, zone, ward, street_name, street_id, latitude, longitude, inby, before_activity_id, clean_weight) "
		               + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
		
		String insertSql = "before".equalsIgnoreCase(action) ? insertBeforeSql : insertAfterSql;
		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcVehicleTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"activity_id"});
	        int i = 1;
	        ps.setString(i++, vehicle_id);
	        ps.setString(i++, activityimg);
	        ps.setString(i++, activityvideo);
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, street_name);
	        ps.setString(i++, street_id);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, inby);

	        if ("after".equalsIgnoreCase(action)) {
	            ps.setString(i++, beforeActivityId); 
	            ps.setString(i++, clean_weight); 
	        }

	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("Special vehicle", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", "Pump & Sump " + action + " activity inserted successfully.");
	    } else {
	    	response.put("Special vehicle", action + " insertId error!");
	        response.put("status", "error");
	        response.put("message", "Special vehicle " + action + " activity insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
	
	public List<Map<String, Object>> getVehicleReport(String loginid) {

	    String sqlQuery = 
	        "SELECT "
	        + "    vm.vehicle_id,"
	        + "    vm.vehicle_type,"
	        + "    vm.vehicle_no,"
	        + "    vm.vehicle_zone,"
	        + "    vm.vehicle_ward,"
	        + "    "
	       
	        + "    COALESCE(COUNT(DISTINCT ba.activity_id), 0) AS Started_Count,"
	        + "    "
	       
	        + "    COALESCE(COUNT(DISTINCT aa.activity_id), 0) AS End_Count,"
	        + "    "
	        
	        + "    COALESCE(SUM(aa.clean_weight), 0) AS Total_Clean_Weight"
	        + " "
	        + "FROM vehicle_master vm "
	        + "LEFT JOIN before_activity ba "
	        + "    ON ba.vehicle_id = vm.vehicle_id "
	        + "    AND ba.isactive = 1 "
	        + "    AND ba.isdelete = 0 "
	        + "LEFT JOIN after_activity aa "
	        + "    ON aa.vehicle_id = vm.vehicle_id "
	        + "    AND aa.isactive = 1 "
	        + "    AND aa.isdelete = 0"
	        + " "
	        + "WHERE vm.isactive = 1"
	        + "  AND vm.isdelete = 0"
	        + " "
	        + "GROUP BY "
	        + "    vm.vehicle_id,"
	        + "    vm.vehicle_type,"
	        + "    vm.vehicle_no,"
	        + "    vm.vehicle_zone,"
	        + "    vm.vehicle_ward "
	        + "ORDER BY "
	        + "    vm.vehicle_zone, vm.vehicle_ward, vm.vehicle_no";

	    List<Map<String, Object>> vechicleReport = jdbcVehicleTemplate.queryForList(sqlQuery);

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Special Vehicle List.");
	    response.put("data", vechicleReport);

	    return Collections.singletonList(response);
	}
			
	public List<Map<String, Object>> getVehicleSummaryReport(String loginid, String vehicle_id) {

	    String sqlQuery = 
	        "SELECT " +
	        "    vm.vehicle_id, " +
	        "    vm.vehicle_type, " +
	        "    vm.vehicle_no, " +
	        "    vm.vehicle_zone, " +
	        "    vm.vehicle_ward, " +

	        "    ba.activity_id AS start_activity_id, " +
	        "    ba.indate AS start_date, " +
	        "    ba.img_file AS start_img, " +
	        "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', ba.img_file) AS start_img_file_url, " +
	        "    ba.video_file AS start_video, " +
	        "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', ba.video_file) AS start_video_file_url, " +

	        "    aa.activity_id AS end_activity_id, " +
	        "    aa.indate AS end_date, " +
	        "    aa.img_file AS end_img, " +
	        "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', aa.img_file) AS end_img_file_url, " +
	        "    aa.video_file AS end_video, " +
	        "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', aa.video_file) AS end_video_file_url, " +
	        "    COALESCE(aa.clean_weight, 0) AS clean_weight " +

	        "FROM vehicle_master vm " +
	        "LEFT JOIN before_activity ba " +
	        "    ON ba.vehicle_id = vm.vehicle_id " +
	        "    AND ba.isactive = 1 " +
	        "    AND ba.isdelete = 0 " +
	        "LEFT JOIN after_activity aa " +
	        "    ON aa.before_activity_id = ba.activity_id " +
	        "    AND aa.vehicle_id = vm.vehicle_id " +
	        "    AND aa.isactive = 1 " +
	        "    AND aa.isdelete = 0 " +
	        "WHERE vm.isactive = 1 " +
	        "  AND vm.isdelete = 0 " +
	        (vehicle_id != null && !vehicle_id.isBlank() ? "  AND vm.vehicle_id = ? " : "") +
	        "ORDER BY vm.vehicle_zone, vm.vehicle_ward, vm.vehicle_no, ba.indate";

	    List<Map<String, Object>> vehicleReport;

	    if (vehicle_id != null && !vehicle_id.isBlank()) {
	        vehicleReport = jdbcVehicleTemplate.queryForList(sqlQuery, vehicle_id);
	    } else {
	        vehicleReport = jdbcVehicleTemplate.queryForList(sqlQuery);
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Vehicle Start-End Activity Report");
	    response.put("data", vehicleReport);

	    return Collections.singletonList(response);
	}
	
	
	
}
