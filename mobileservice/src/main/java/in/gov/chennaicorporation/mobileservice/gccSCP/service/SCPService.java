package in.gov.chennaicorporation.mobileservice.gccSCP.service;

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
public class SCPService {
	
	private JdbcTemplate jdbcSCPTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlActivityDataSource") DataSource SCPDataSource) {
		this.jdbcSCPTemplate = new JdbcTemplate(SCPDataSource);
	}
    
    @Autowired
	public SCPService(Environment environment) {
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
		String serviceFolderName = environment.getProperty("scp_foldername");
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
				System.out.println("Activity: Pump");
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
	    List<Map<String, Object>> results = jdbcSCPTemplate.queryForList(sqlQuery, loginid, type);
	    
	    // Check if results is not empty and extract the ward value
	    if (!results.isEmpty()) {
	        // Extract the ward value from the first result
	        return (String) results.get(0).get("ward");
	    }
	    
	    // Handle the case where no result is found
	    return "000";  // or return null based on your needs
	}

	public List<Map<String, Object>> getSCPListForMap(String loginid, String streetid) {
		
		//String ward = getWardByLoginId(loginid,"ae");
		/*
		String sqlQuery ="SELECT "
				+ "    al.*, "
				+ "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files/asset_images/2/', al.image) AS imageUrl, "
				+ "    EXISTS ("
				+ "        SELECT 1 FROM `scp_grating_activity` aa "
				+ "        WHERE aa.asset_id = al.id "
				+ "    ) AS `activity_completed` "
				+ "FROM asset_list al "
				+ "WHERE al.category_id = 2 AND al.streetid = ? AND al.isactive=1 AND isdelete=0 ";
		*/
		String sqlQuery = 
			    "SELECT " +
			    "    al.*, " +
			    "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files/asset_images/2/', al.image) AS imageUrl, " +
			    "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', aa.file) AS fileUrl, " +
			    "    aa.activity_id, " +
			    "    aa.grating, " +
			    "    aa.inby AS activity_by, " +
			    "    DATE_FORMAT(aa.indate, '%d-%m-%Y') AS activity_date, " +
			    "    aa.road_type, " +
			    "    CASE WHEN aa.activity_id IS NOT NULL THEN 1 ELSE 0 END AS activity_completed " +
			    "FROM asset_list al " +
			    "LEFT JOIN ( " +
			    "    SELECT a1.* " +
			    "    FROM scp_grating_activity a1 " +
			    "    INNER JOIN ( " +
			    "        SELECT asset_id, MAX(indate) AS latest_indate " +
			    "        FROM scp_grating_activity " +
			    "        WHERE isactive=1 AND isdelete=0 " +
			    "        GROUP BY asset_id " +
			    "    ) a2 ON a1.asset_id = a2.asset_id AND a1.indate = a2.latest_indate " +
			    "    WHERE a1.isactive=1 AND a1.isdelete=0 " +
			    ") aa ON aa.asset_id = al.id " +
			    "WHERE al.category_id = 2 " +
			    "  AND al.streetid = ? " +
			    "  AND al.isactive=1 " +
			    "  AND al.isdelete=0";
		
	    List<Map<String, Object>> Pumps = jdbcSCPTemplate.queryForList(sqlQuery, streetid);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Pump & Sump Map List.");
	    response.put("data", Pumps);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveActivity(
	        String asset_id,
	        String grating,
	        MultipartFile file,
	        String zone,
	        String ward,
	        String street_name,
	        String street_id,
	        String latitude,
	        String longitude,
	        String inby,
	        String road_type
	) {

		String filetxt =asset_id;
		
	    
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    // For Image
	    String filetype = "image";
	    String activityimg = fileUpload(grating, filetxt, file, filetype);

	    if ("error".equalsIgnoreCase(activityimg)) {
	        response.put("status", "error");
	        response.put("message", "SCP activity image insert failed.");
	        result.add(response);
	        return result;
	    }

	    
	    String insertBeforeSql = "INSERT INTO `scp_grating_activity` (asset_id, grating, file, zone, ward, street_name, street_id, latitude, longitude, inby, road_type) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

	    String insertSql = insertBeforeSql;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcSCPTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"activity_id"});
	        int i = 1;
	        ps.setString(i++, asset_id);
	        ps.setString(i++, grating);
	        ps.setString(i++, activityimg);
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, street_name);
	        ps.setString(i++, street_id);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, inby);
	        ps.setString(i++, road_type);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("SCP", " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", "SCP activity inserted successfully.");
	    } else {
	    	response.put("SCP", " insertId error!");
	        response.put("status", "error");
	        response.put("message", "SCP activity insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
	
	public List<Map<String, Object>> getZoneReport(String loginid) {
		
		String sqlQuery ="SELECT "
				+ "    al.zone,"
				+ "    COUNT(DISTINCT al.id) AS total_scp,"
				+ "    COUNT(DISTINCT CASE WHEN aa.activity_id IS NOT NULL THEN al.id END) AS Completed,"
				+ "    COUNT(DISTINCT CASE WHEN aa.activity_id IS NULL THEN al.id END) AS Pending,"
				+ "    COUNT(DISTINCT CASE WHEN aa.grating = 'yes' THEN al.id END) AS yes,"
				+ "    COUNT(DISTINCT CASE WHEN aa.grating = 'no' THEN al.id END) AS no"
				+ " "
				+ "FROM asset_list al"
				+ " "
				+ "LEFT JOIN ("
				+ "    SELECT a1.*"
				+ "    FROM scp_grating_activity a1"
				+ "    INNER JOIN ("
				
				+ "        SELECT asset_id, MAX(indate) AS latest_indate"
				+ "        FROM scp_grating_activity"
				+ "        WHERE isactive=1 AND isdelete=0"
				+ "        GROUP BY asset_id"
				+ "    ) a2 ON a1.asset_id = a2.asset_id AND a1.indate = a2.latest_indate"
				+ "    WHERE a1.isactive=1 AND a1.isdelete=0"
				+ ") aa ON aa.asset_id = al.id"
				+ " "
				+ "WHERE al.category_id = 2 AND al.isactive=1 AND al.isdelete=0"
				+ " "
				+ "GROUP BY al.zone "
				+ "ORDER BY al.zone";
		
	    List<Map<String, Object>> Pumps = jdbcSCPTemplate.queryForList(sqlQuery);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Zone SCP List.");
	    response.put("data", Pumps);

	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getWardReport(String loginid, String zone) {
		
		String sqlQuery ="SELECT "
				+ "    al.zone,al.ward,"
				+ "    COUNT(DISTINCT al.id) AS total_scp,"
				+ "    COUNT(DISTINCT CASE WHEN aa.activity_id IS NOT NULL THEN al.id END) AS Completed,"
				+ "    COUNT(DISTINCT CASE WHEN aa.activity_id IS NULL THEN al.id END) AS Pending,"
				+ "    COUNT(DISTINCT CASE WHEN aa.grating = 'yes' THEN al.id END) AS yes,"
				+ "    COUNT(DISTINCT CASE WHEN aa.grating = 'no' THEN al.id END) AS no"
				+ " "
				+ "FROM asset_list al"
				+ " "
				+ "LEFT JOIN ("
				+ "    SELECT a1.*"
				+ "    FROM scp_grating_activity a1"
				+ "    INNER JOIN ("
				
				+ "        SELECT asset_id, MAX(indate) AS latest_indate"
				+ "        FROM scp_grating_activity"
				+ "        WHERE isactive=1 AND isdelete=0"
				+ "        GROUP BY asset_id"
				+ "    ) a2 ON a1.asset_id = a2.asset_id AND a1.indate = a2.latest_indate"
				+ "    WHERE a1.isactive=1 AND a1.isdelete=0"
				+ ") aa ON aa.asset_id = al.id"
				+ " "
				+ "WHERE al.category_id = 2 AND al.isactive=1 AND al.isdelete=0 AND al.zone=?"
				+ " "
				+ "GROUP BY al.zone,al.ward "
				+ "ORDER BY al.zone,al.ward";
		
	    List<Map<String, Object>> Pumps = jdbcSCPTemplate.queryForList(sqlQuery,zone);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Ward SCP List.");
	    response.put("data", Pumps);

	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getStreetReport(String loginid, String ward) {

	    String sqlQuery = 
	        "SELECT " +
	        "    al.streetid, " +
	        "    al.streetname, " +
	        "    COUNT(DISTINCT al.id) AS total_scp, " +
	        "    COUNT(DISTINCT CASE WHEN aa.activity_id IS NOT NULL THEN al.id END) AS Completed, " +
	        "    COUNT(DISTINCT CASE WHEN aa.activity_id IS NULL THEN al.id END) AS Pending, " +
	        "    COUNT(DISTINCT CASE WHEN aa.grating = 'yes' THEN al.id END) AS yes, " +
	        "    COUNT(DISTINCT CASE WHEN aa.grating = 'no' THEN al.id END) AS no " +
	        "FROM asset_list al " +
	        "LEFT JOIN ( " +
	        "    SELECT a1.* " +
	        "    FROM scp_grating_activity a1 " +
	        "    INNER JOIN ( " +
	        "        SELECT asset_id, MAX(indate) AS latest_indate " +
	        "        FROM scp_grating_activity " +
	        "        WHERE isactive=1 AND isdelete=0 " +
	        "        GROUP BY asset_id " +
	        "    ) a2 ON a1.asset_id = a2.asset_id AND a1.indate = a2.latest_indate " +
	        "    WHERE a1.isactive=1 AND a1.isdelete=0 " +
	        ") aa ON aa.asset_id = al.id " +
	        "WHERE al.category_id = 2 AND al.isactive=1 AND al.isdelete=0 AND al.ward=? " +
	        "GROUP BY al.streetid, al.streetname " +
	        "ORDER BY al.streetname";

	    List<Map<String, Object>> streetReport = jdbcSCPTemplate.queryForList(sqlQuery, ward);

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Street-level SCP List.");
	    response.put("data", streetReport);

	    return Collections.singletonList(response);
	}
}
