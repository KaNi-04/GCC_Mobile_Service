package in.gov.chennaicorporation.mobileservice.abandonedVehicle.service;

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
import java.time.temporal.ChronoUnit;
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

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service("abandonedVehicleService")
public class VehicleService {
private JdbcTemplate jdbcTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlGccAbandonedVehicleSource") DataSource AbandonedVehicleDataSource) {
		this.jdbcTemplate = new JdbcTemplate(AbandonedVehicleDataSource);
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
		String serviceFolderName = environment.getProperty("abandonedvehicle_foldername");
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
	    String sqlQuery = "SELECT `ward` FROM `gcc_penalty_hoardings`.`hoading_user_list` WHERE `userid` = ? LIMIT 1";
	    
	    // Query the database using queryForList
	    List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery, loginid);
	    
	    // Check if results is not empty and extract the ward value
	    if (!results.isEmpty()) {
	        // Extract the ward value from the first result
	        return (String) results.get(0).get("ward");
	    }
	    
	    // Handle the case where no result is found
	    return "000";  // or return null based on your needs
	}
	
	@Transactional
	public List<Map<String, Object>> saveIdentfiedVehicle(
	        String zone,
	        String ward,
	        String street_name,
	        String street_id,
	        String latitude,
	        String longitude,
	        String inby,
	        MultipartFile frontFile,
	        MultipartFile backFile,
	        MultipartFile sideFile
	) {

		String filetxt = inby;
		
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    // For Image
	    String filetype = "image";
	    // for front Image
	    String action = "front";
	    String frontimg = fileUpload(action, filetxt, frontFile, filetype);
	    
	    if ("error".equalsIgnoreCase(frontimg)) {
	        response.put("status", "error");
	        response.put("message", "Front image insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    
	    // for side Image
	    action = "side";
	    String sideimg = fileUpload(action, filetxt, sideFile, filetype);
	    
	    if ("error".equalsIgnoreCase(sideimg)) {
	        response.put("status", "error");
	        response.put("message", "Side image insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    // for back Image
	    action = "back";
	    String backimg = fileUpload(action, filetxt, backFile, filetype);

	    if ("error".equalsIgnoreCase(backimg)) {
	        response.put("status", "error");
	        response.put("message", "Back image insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    String insertSqltxt = "INSERT INTO `vehicle_identified` (`zone`, `ward`, `street_name`, `streetid`, `latitude`, `longitude`, `inby`, `front_img`,`side_img`,`back_img`) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?)";

	    String insertSql = insertSqltxt;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"vid"});
	        int i = 1;
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, street_name);
	        ps.setString(i++, street_id);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, inby);
	        ps.setString(i++, frontimg);
	        ps.setString(i++, sideimg);
	        ps.setString(i++, backimg);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("Abandoned Vehicle", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", "Abandoned Vehicle data inserted successfully.");
	    } else {
	    	response.put("Abandoned Vehicle", action + " error!");
	        response.put("status", "error");
	        response.put("message", "Abandoned Vehicle data insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
	
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getRemovePendingList(String userid) {

		String ward = getWardByLoginId(userid, "ae");
		
		String sql =
	    	    "SELECT *, "
	    	    + "date_format(cdate,'%d-%m-%Y %l:%i %p') as created_date,"
	    	    + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `front_img`) AS front_img_url,"
	    	    + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `side_img`) AS side_img_url,"
	    	    + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `back_img`) AS back_img_url " 
	    	    + "FROM vehicle_identified "
	    	    + "WHERE isactive = 1 AND isdelete = 0 AND status_id = 1 "
	    	    + "AND `ward` = ? "
	    	    + "ORDER BY `ward`";

	    List<Map<String, Object>> results =
	    	    jdbcTemplate.queryForList(sql,ward);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Pending List");
	    response.put("data", results);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveRemovedVehicle(
			String vid,
	        String zone,
	        String ward,
	        String street_name,
	        String street_id,
	        String latitude,
	        String longitude,
	        String inby,
	        MultipartFile removeFile
	) {

		String filetxt = inby;
		
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    // For Image
	    String filetype = "image";
	    // for front Image
	    String action = "Remove";
	    String removeimg = fileUpload(action, filetxt, removeFile, filetype);
	    
	    if ("error".equalsIgnoreCase(removeimg)) {
	        response.put("status", "error");
	        response.put("message", "Remove image insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    
	    String insertSqltxt = "INSERT INTO `vehicle_removed` (`vid`, `zone`, `ward`, `street_name`, `streetid`, `latitude`, `longitude`, `inby`, `remove_img`) "
                + "VALUES (?,?,?,?,?,?,?,?,?)";

	    String insertSql = insertSqltxt;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"vrid"});
	        int i = 1;
	        ps.setString(i++, vid);
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, street_name);
	        ps.setString(i++, street_id);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, inby);
	        ps.setString(i++, removeimg);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	    	// Update on vehicle status
	    	String updateSQL = "UPDATE `vehicle_identified` SET status_id=2 WHERE vid=?";
	    	jdbcTemplate.update(updateSQL,vid);
	    	
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("Abandoned Vehicle", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", "Abandoned Vehicle removed data inserted successfully.");
	    } else {
	    	response.put("Abandoned Vehicle", action + " error!");
	        response.put("status", "error");
	        response.put("message", "Abandoned Vehicle removed data insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
	
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getYardPendingList(String userid) {

		String ward = getWardByLoginId(userid, "ae");
		
		String sql =
	    	    "SELECT *, "
	    	    + "date_format(cdate,'%d-%m-%Y %l:%i %p') as created_date,"
	    	    + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `front_img`) AS front_img_url,"
	    	    + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `side_img`) AS side_img_url,"
	    	    + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `back_img`) AS back_img_url " 
	    	    + "FROM vehicle_identified "
	    	    + "WHERE isactive = 1 AND isdelete = 0 AND status_id = 2 "
	    	    + "AND `ward` = ? "
	    	    + "ORDER BY `ward`";

	    List<Map<String, Object>> results =
	    	    jdbcTemplate.queryForList(sql,ward);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Pending List");
	    response.put("data", results);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveYardVehicle(
			String vid,
			String yardid,
	        String zone,
	        String ward,
	        String street_name,
	        String street_id,
	        String latitude,
	        String longitude,
	        String inby,
	        MultipartFile yardFile
	) {

		String filetxt = inby;
		
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    // For Image
	    String filetype = "image";
	    // for front Image
	    String action = "Yard";
	    String yardimg = fileUpload(action, filetxt, yardFile, filetype);
	    
	    if ("error".equalsIgnoreCase(yardimg)) {
	        response.put("status", "error");
	        response.put("message", "Yard drop image insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    
	    String insertSqltxt = "INSERT INTO `vehicle_yard` (`vid`, `zone`, `ward`, `street_name`, `streetid`, `latitude`, `longitude`, `inby`, `drop_img`, `yard`) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?)";

	    String insertSql = insertSqltxt;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"vyid"});
	        int i = 1;
	        ps.setString(i++, vid);
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, street_name);
	        ps.setString(i++, street_id);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, inby);
	        ps.setString(i++, yardimg);
	        ps.setString(i++, yardid);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	    	// Update on vehicle status
	    	String updateSQL = "UPDATE `vehicle_identified` SET status_id=3 WHERE vid=?";
	    	jdbcTemplate.update(updateSQL,vid);
	    	
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("Abandoned Vehicle", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", "Yard drop data inserted successfully.");
	    } else {
	    	response.put("Abandoned Vehicle", action + " error!");
	        response.put("status", "error");
	        response.put("message", "Yard drop data insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
	
	// Reports OLD
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getZoneSummary(String fromDate, String toDate) {

	    fromDate = convertDateFormat(fromDate, 0);
	    toDate = convertDateFormat(toDate, 1);

	    String sql =
	    	    "SELECT zone, COUNT(vid) AS total " +
	    	    "FROM vehicle_identified " +
	    	    "WHERE isactive = 1 AND isdelete = 0 " +
	    	    "AND cdate >= ? " +
	    	    "AND cdate < ? " +
	    	    "GROUP BY `zone` ORDER BY `zone` ";

	    String fromDateTime = fromDate + " 00:00:00";
	    String toDateTime   = toDate   + " 00:00:00";

	    List<Map<String, Object>> results =
	    	    jdbcTemplate.queryForList(sql, fromDateTime, toDateTime);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Zone List.");
	    response.put("data", results);

	    return Collections.singletonList(response);
	}
	
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getWardSummary(String fromDate, String toDate, String zone) {

	    fromDate = convertDateFormat(fromDate, 0);
	    toDate = convertDateFormat(toDate, 1);

	    String sql =
	    	    "SELECT ward, COUNT(vid) AS total " +
	    	    "FROM vehicle_identified " +
	    	    "WHERE isactive = 1 AND isdelete = 0 " +
	    	    "AND cdate >= ? " +
	    	    "AND cdate < ? " +
	    	    "AND `zone` = ? " +
	    	    "GROUP BY `ward`  ORDER BY `ward` ";

	    String fromDateTime = fromDate + " 00:00:00";
	    String toDateTime   = toDate   + " 00:00:00";

	    List<Map<String, Object>> results =
	    	    jdbcTemplate.queryForList(sql, fromDateTime, toDateTime, zone);
	    	
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Ward List.");
	    response.put("data", results);

	    return Collections.singletonList(response);
	}
	
	
	// Reports
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getZoneSummary_(String fromDate, String toDate) {

	    fromDate = convertDateFormat(fromDate, 0);
	    toDate = convertDateFormat(toDate, 1);

	    String sql =
	    	    "SELECT IFNULL(zone, 'TOTAL') AS zone, " +
	    	    "COUNT(*) AS total_identified, " +
	    	    "SUM(CASE WHEN status_id = 2 THEN 1 ELSE 0 END) AS total_removed, " +
	    	    "SUM(CASE WHEN status_id = 3 THEN 1 ELSE 0 END) AS total_yard, " +
	    	    "SUM(CASE WHEN status_id = 1 THEN 1 ELSE 0 END) AS removed_pending, " +
	    	    "SUM(CASE WHEN status_id = 2 THEN 1 ELSE 0 END) AS yard_pending " +
	    	    "FROM vehicle_identified " +
	    	    "WHERE isactive = 1 " +
	    	    "AND isdelete = 0 " +
	    	    "AND cdate >= ? " +
	    	    "AND cdate < ? " +
	    	    "GROUP BY zone WITH ROLLUP";

	    String fromDateTime = fromDate + " 00:00:00";
	    String toDateTime   = toDate   + " 00:00:00";

	    List<Map<String, Object>> results =
	    	    jdbcTemplate.queryForList(sql, fromDateTime, toDateTime);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Zone List.");
	    response.put("data", results);

	    return Collections.singletonList(response);
	}
	
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getWardSummary_(String fromDate, String toDate, String zone) {

	    fromDate = convertDateFormat(fromDate, 0);
	    toDate = convertDateFormat(toDate, 1);

	    String sql =
	    	    "SELECT IFNULL(ward, 'TOTAL') AS ward, " +
	    	    "COUNT(*) AS total_identified, " +
	    	    "SUM(CASE WHEN status_id = 2 THEN 1 ELSE 0 END) AS total_removed, " +
	    	    "SUM(CASE WHEN status_id = 3 THEN 1 ELSE 0 END) AS total_yard, " +
	    	    "SUM(CASE WHEN status_id = 1 THEN 1 ELSE 0 END) AS removed_pending, " +
	    	    "SUM(CASE WHEN status_id = 2 THEN 1 ELSE 0 END) AS yard_pending " +
	    	    "FROM vehicle_identified " +
	    	    "WHERE isactive = 1 " +
	    	    "AND isdelete = 0 " +
	    	    "AND cdate >= ? " +
	    	    "AND cdate < ? " +
	    	    "AND `zone` = ? " +
	    	    "GROUP BY ward WITH ROLLUP";
	    
	    String fromDateTime = fromDate + " 00:00:00";
	    String toDateTime   = toDate   + " 00:00:00";

	    List<Map<String, Object>> results =
	    	    jdbcTemplate.queryForList(sql, fromDateTime, toDateTime, zone);
	    	
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Ward List.");
	    response.put("data", results);

	    return Collections.singletonList(response);
	}
		
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getVehicleSummary(String fromDate, String toDate, String ward) {

	    fromDate = convertDateFormat(fromDate, 0);
	    toDate = convertDateFormat(toDate, 1);

	    String sql =
	    	    "SELECT *, "
	    	    + "date_format(cdate,'%d-%m-%Y %l:%i %p') as created_date,"
	    	    + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `front_img`) AS front_img_url,"
	    	    + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `side_img`) AS side_img_url,"
	    	    + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `back_img`) AS back_img_url " 
	    	    + "FROM vehicle_identified " +
	    	    "WHERE isactive = 1 AND isdelete = 0 " +
	    	    "AND cdate >= ? " +
	    	    "AND cdate < ? " + 
	    	    "AND `ward` = ? " +
	    	    "ORDER BY `ward` ";

	    String fromDateTime = fromDate + " 00:00:00";
	    String toDateTime   = toDate   + " 00:00:00";

	    List<Map<String, Object>> results =
	    	    jdbcTemplate.queryForList(sql, fromDateTime, toDateTime, ward);
	    	
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Vehicle List.");
	    response.put("data", results);

	    return Collections.singletonList(response);
	}
	
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getVehicleSummary_(
	        String fromDate,
	        String toDate,
	        String ward,
	        String filterType
	) {

	    fromDate = convertDateFormat(fromDate, 0);
	    toDate   = convertDateFormat(toDate, 1);

	    StringBuilder sql = new StringBuilder(
	        "SELECT vi.*, " +
	        "DATE_FORMAT(vi.cdate,'%d-%m-%Y %l:%i %p') AS created_date, " +
	        "CONCAT(?, vi.front_img) AS front_img_url, " +
	        "CONCAT(?, vi.side_img) AS side_img_url, " +
	        "CONCAT(?, vi.back_img) AS back_img_url "
	    );

	    // 🔹 Conditional extra columns
	    if ("REMOVED".equals(filterType) || "YARD_PENDING".equals(filterType)) {
	        sql.append(
	            ", DATE_FORMAT(vr.cdate,'%d-%m-%Y %l:%i %p') AS removed_date, " +
	            "CONCAT(?, vr.remove_img) AS remove_img_url "
	        );
	    }

	    if ("YARD".equals(filterType)) {
	        sql.append(
	            ", DATE_FORMAT(vy.cdate,'%d-%m-%Y %l:%i %p') AS yard_date, " +
	            "vy.yard, " +
	            "CONCAT(?, vy.drop_img) AS drop_img_url "
	        );
	    }

	    sql.append(" FROM vehicle_identified vi ");

	    // 🔹 Conditional JOINs
	    if ("REMOVED".equals(filterType) || "YARD_PENDING".equals(filterType)) {
	        sql.append(
	            " LEFT JOIN vehicle_removed vr " +
	            " ON vr.vid = vi.vid "
	        );
	    }

	    if ("YARD".equals(filterType)) {
	        sql.append(
	            " LEFT JOIN vehicle_yard vy " +
	            " ON vy.vid = vi.vid "
	        );
	    }

	    sql.append(
	        " WHERE vi.isactive = 1 AND vi.isdelete = 0 " +
	        " AND vi.cdate >= ? " +
	        " AND vi.cdate < ? " +
	        " AND vi.ward = ? "
	    );

	    // 🔹 Status filter
	    if (filterType != null) {
	        switch (filterType) {
	            case "IDENTIFIED":
	            case "REMOVED_PENDING":
	                sql.append(" AND vi.status_id = 1 ");
	                break;

	            case "REMOVED":
	            case "YARD_PENDING":
	                sql.append(" AND vi.status_id = 2 ");
	                break;

	            case "YARD":
	                sql.append(" AND vi.status_id = 3 ");
	                break;
	        }
	    }

	    sql.append(" ORDER BY vi.cdate DESC ");

	    // 🔹 Parameters
	    List<Object> params = new ArrayList<>();

	    // base images
	    params.add(fileBaseUrl + "/gccofficialapp/files");
	    params.add(fileBaseUrl + "/gccofficialapp/files");
	    params.add(fileBaseUrl + "/gccofficialapp/files");

	    // removed images
	    if ("REMOVED".equals(filterType) || "YARD_PENDING".equals(filterType)) {
	        params.add(fileBaseUrl + "/gccofficialapp/files");
	    }

	    // yard images
	    if ("YARD".equals(filterType)) {
	        params.add(fileBaseUrl + "/gccofficialapp/files");
	    }

	    params.add(fromDate + " 00:00:00");
	    params.add(toDate   + " 00:00:00");
	    params.add(ward);

	    List<Map<String, Object>> results =
	            jdbcTemplate.queryForList(sql.toString(), params.toArray());

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Vehicle List");
	    response.put("data", results);

	    return Collections.singletonList(response);
	}
	
	/*
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getVehicleSummary_(
	        String fromDate,
	        String toDate,
	        String ward,
	        String filterType
	) {

	    fromDate = convertDateFormat(fromDate, 0);
	    toDate   = convertDateFormat(toDate, 1);

	    StringBuilder sql = new StringBuilder(
	        "SELECT *, " +
	        "DATE_FORMAT(cdate,'%d-%m-%Y %l:%i %p') AS created_date, " +
	        "CONCAT(?, front_img) AS front_img_url, " +
	        "CONCAT(?, side_img) AS side_img_url, " +
	        "CONCAT(?, back_img) AS back_img_url " +
	        "FROM vehicle_identified " +
	        "WHERE isactive = 1 AND isdelete = 0 " +
	        "AND cdate >= ? " +
	        "AND cdate < ? " +
	        "AND ward = ? "
	    );

	    List<Object> params = new ArrayList<>();
	    params.add(fileBaseUrl + "/gccofficialapp/files");
	    params.add(fileBaseUrl + "/gccofficialapp/files");
	    params.add(fileBaseUrl + "/gccofficialapp/files");

	    params.add(fromDate + " 00:00:00");
	    params.add(toDate   + " 00:00:00");
	    params.add(ward);

	    // 🔹 Status filter based on click
	    if (filterType != null) {
	        switch (filterType) {
	            case "IDENTIFIED":
	            case "REMOVED_PENDING":
	                sql.append(" AND status_id = 1 ");
	                break;

	            case "REMOVED":
	            case "YARD_PENDING":
	                sql.append(" AND status_id = 2 ");
	                break;

	            case "YARD":
	                sql.append(" AND status_id = 3 ");
	                break;
	        }
	    }

	    sql.append(" ORDER BY cdate DESC ");

	    List<Map<String, Object>> results =
	            jdbcTemplate.queryForList(sql.toString(), params.toArray());

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Vehicle List");
	    response.put("data", results);

	    return Collections.singletonList(response);
	}
	*/
}
