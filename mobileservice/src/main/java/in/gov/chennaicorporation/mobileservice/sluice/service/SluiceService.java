package in.gov.chennaicorporation.mobileservice.sluice.service;

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

@Service
public class SluiceService {
	
	private JdbcTemplate jdbcSluiceTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlGccSluicePointSource") DataSource SluiceDataSource) {
		this.jdbcSluiceTemplate = new JdbcTemplate(SluiceDataSource);
	}
    
    @Autowired
	public SluiceService(Environment environment) {
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
		String serviceFolderName = environment.getProperty("sluicepoint_foldername");
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
	
	public String getWardByLoginId(String loginid, String type) {
	    String sqlQuery = "SELECT `ward` FROM gcc_penalty_hoardings.`hoading_user_list` WHERE `userid` = ? AND `type` = ? LIMIT 1";
	    
	    // Query the database using queryForList
	    List<Map<String, Object>> results = jdbcSluiceTemplate.queryForList(sqlQuery, loginid, type);
	    
	    // Check if results is not empty and extract the ward value
	    if (!results.isEmpty()) {
	        // Extract the ward value from the first result
	        return (String) results.get(0).get("ward");
	    }
	    
	    // Handle the case where no result is found
	    return "000";  // or return null based on your needs
	}

	@Transactional
	public List<Map<String, Object>> saveSluicePoint(
	        String zone,
	        String ward,
	        String street_name,
	        String street_id,
	        String latitude,
	        String longitude,
	        String inby,
	        MultipartFile file
	) {

		String filetxt = inby;
		
		String action = "new_mapping";
		
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    // For Image
	    String filetype = "image";
	    String activityimg = fileUpload(action, filetxt, file, filetype);

	    if ("error".equalsIgnoreCase(activityimg)) {
	        response.put("status", "error");
	        response.put("message", "Sluice Point new mapping image insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    String insertSqltxt = "INSERT INTO sluicepoint_mapped_master(`zone`, `ward`, `road_name`, `road_gisid`, `latitude`, `longitude`, `inby`, `file`) "
                + "VALUES (?,?,?,?,?,?,?,?)";

	    String insertSql = insertSqltxt;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcSluiceTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"sluicepoint_id"});
	        int i = 1;
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, street_name);
	        ps.setString(i++, street_id);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, inby);
	        ps.setString(i++, activityimg);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("Sluice Point", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", "Sluice Point " + action + " data inserted successfully.");
	    } else {
	    	response.put("Sluice Point", action + " insertId error!");
	        response.put("status", "error");
	        response.put("message", "Sluice Point " + action + " data insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
	
	public List<Map<String, Object>> getSluicePointListForMap(String loginid) {
		
		String ward = getWardByLoginId(loginid,"ae");
		
	    String sqlManhole = "SELECT "
	    		+ "    d.zone,"
	    		+ "    d.ward,"
	    		+ "    COUNT(*) AS total_sluice_points,"
	    		+ "    (SELECT COUNT(*) "
	    		+ "     FROM sluicepoint_mapped_master m"
	    		+ "     WHERE m.isactive = 1 AND m.isdelete = 0 AND m.ward = d.ward) AS total_mapped_points,"
	    		+ "    (COUNT(*) - "
	    		+ "     (SELECT COUNT(*) "
	    		+ "      FROM sluicepoint_mapped_master m"
	    		+ "      WHERE m.isactive = 1 AND m.isdelete = 0 AND m.ward = d.ward)"
	    		+ "    ) AS total_pending_points "
	    		+ "FROM sluicepoint_master d "
	    		+ "WHERE d.isactive = 1 AND d.isdelete = 0"
	    		+ "  AND d.ward = ? "
	    		+ "GROUP BY d.ward "
	    		+ "ORDER BY d.ward";
	    List<Map<String, Object>> sluicepoints = jdbcSluiceTemplate.queryForList(sqlManhole, ward);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Dispoasl Map List.");
	    response.put("data", sluicepoints);

	    return Collections.singletonList(response);
	}

	public List<Map<String, Object>> getSluicePointList(String loginid) {
		
		String ward = getWardByLoginId(loginid,"ae");
		
	    // 1. Fetch all Sluice Points for the given SWDID
	    String sqlManhole = "SELECT *, "
	    		+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `file`) AS img_file_url "
	    		+ " FROM sluicepoint_mapped_master WHERE ward = ? AND `isactive`=1 AND `isdelete`=0";
	    List<Map<String, Object>> sluicepoints = jdbcSluiceTemplate.queryForList(sqlManhole, ward);

	    List<Map<String, Object>> finalList = new ArrayList<>();

	    for (Map<String, Object> sluicepoint : sluicepoints) {
	        Map<String, Object> sluicepointResponse = new HashMap<>();
	        sluicepointResponse.putAll(sluicepoint); // add all Sluice point columns

	        // 2. Fetch before activity
	        String sqlBefore = "SELECT *,"
	        		+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `img_file`) AS img_file_url,"
	        		+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `video_file`) AS video_file_url "
	        		+ "FROM before_activity WHERE sluicepoint_id = ? "
	        		+ "AND indate >= CURDATE() "
	                + "AND indate < CURDATE() + INTERVAL 1 DAY "
	        		+ "AND isactive=1 AND isdelete=0";
	        List<Map<String, Object>> beforeActivities = jdbcSluiceTemplate.queryForList(sqlBefore,
	        		sluicepoint.get("sluicepoint_id"));

	        Map<String, Object> beforeData = new HashMap<>();
	        if (!beforeActivities.isEmpty()) {
	            beforeData.put("status", "Completed");
	            beforeData.put("details", beforeActivities);
	        } else {
	            beforeData.put("status", "Pending");
	            beforeData.put("details", Collections.emptyList());
	        }
	        
	        // 4. Overall Sluice Point status
	        String overallStatus = (!beforeActivities.isEmpty()) ? "Completed" : "Pending";
	        sluicepointResponse.put("status", overallStatus);
	        sluicepointResponse.put("before", beforeData);

	        finalList.add(sluicepointResponse);
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Dispoasl List.");
	    response.put("data", finalList);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveActivity(
	        String sluicepoint_id,
	        String rust_painting,
	        String greasing,
	        MultipartFile file,
	        MultipartFile video,
	        String zone,
	        String ward,
	        String street_name,
	        String street_id,
	        String latitude,
	        String longitude,
	        String inby,
	        String action
	) {

		String filetxt =sluicepoint_id;
		
	    
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    // For Image
	    String filetype = "image";
	    String activityimg = fileUpload(action, filetxt, file, filetype);

	    if ("error".equalsIgnoreCase(activityimg)) {
	        response.put("status", "error");
	        response.put("message", "Sluice Point activity image insert failed.");
	        result.add(response);
	        return result;
	    }

	    // For Video
	    filetype = "video";
	    String activityvideo = fileUpload(action, filetxt, video, filetype);
	    
	    if ("error".equalsIgnoreCase(activityvideo)) {
	        response.put("status", "error");
	        response.put("message", "Sluice Point activity video insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    String insertBeforeSql = "INSERT INTO before_activity(sluicepoint_id, img_file, video_file, zone, ward, "
	    		+ "street_name, street_id, latitude, longitude, inby, rust_painting, greasing) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

	    String insertSql = insertBeforeSql;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcSluiceTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"activity_id"});
	        int i = 1;
	        ps.setString(i++, sluicepoint_id);
	        ps.setString(i++, activityimg);
	        ps.setString(i++, activityvideo);
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, street_name);
	        ps.setString(i++, street_id);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, inby);
	        ps.setString(i++, rust_painting);
	        ps.setString(i++, greasing);

	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("Sluice Point", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", "Sluice Point " + action + " activity inserted successfully.");
	    } else {
	    	response.put("Sluice Point", action + " insertId error!");
	        response.put("status", "error");
	        response.put("message", "Sluice Point " + action + " activity insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
	
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getZoneSummary(String fromDate, String toDate) {

	    fromDate = convertDateFormat(fromDate, 0);
	    toDate = convertDateFormat(toDate, 0);

	    String sql =
	        "SELECT " +
	        "    z.zone, " +
	        "    COALESCE(dm.total_sluice_points, 0) AS total_sluice_points, " +
	        "    COALESCE(mm.total_mapped_points, 0) AS total_mapped_points, " +
	        "    COALESCE(COUNT(DISTINCT (b.sluicepoint_id), 0)) AS completed_days " +
	        "FROM (" +
	        "    SELECT DISTINCT zone FROM sluicepoint_master WHERE isactive=1 AND isdelete=0" +
	        ") z " +
	        "LEFT JOIN (" +
	        "    SELECT zone, COUNT(*) AS total_sluice_points " +
	        "    FROM sluicepoint_master " +
	        "    WHERE isactive=1 AND isdelete=0 " +
	        "    GROUP BY zone" +
	        ") dm ON z.zone = dm.zone " +
	        "LEFT JOIN (" +
	        "    SELECT zone, COUNT(*) AS total_mapped_points " +
	        "    FROM sluicepoint_mapped_master " +
	        "    WHERE isactive=1 AND isdelete=0 " +
	        "    GROUP BY zone" +
	        ") mm ON z.zone = mm.zone " +
	        "LEFT JOIN sluicepoint_mapped_master dmm ON dmm.zone = z.zone AND dmm.isactive=1 AND dmm.isdelete=0 " +
	        "LEFT JOIN before_activity b ON dmm.sluicepoint_id = b.sluicepoint_id " +
	        "    AND b.isactive=1 AND b.isdelete=0 " +
	        "    AND b.indate >= ? AND b.indate < DATE_ADD(?, INTERVAL 1 DAY) " +
	        "GROUP BY z.zone, dm.total_sluice_points, mm.total_mapped_points " +
	        "ORDER BY z.zone";

	    List<Map<String, Object>> results = jdbcSluiceTemplate.queryForList(sql, fromDate, toDate);

	    long totalDays = ChronoUnit.DAYS.between(
	            LocalDate.parse(fromDate),
	            LocalDate.parse(toDate)
	    ) + 1;

	    List<Map<String, Object>> finalList = new ArrayList<>();

	    long grandTotalSluicePoints = 0;
	    long grandTotalMappedPoints = 0;
	    long grandCompletedDays = 0;
	    long grandPendingDays = 0;

	    
	    for (Map<String, Object> row : results) {
	        Map<String, Object> report = new LinkedHashMap<>();
	        long completed = ((Number) row.get("completed_days")).longValue();
	        long totalSluicePoints = ((Number) row.get("total_sluice_points")).longValue();
	        long totalMappedPoints = ((Number) row.get("total_mapped_points")).longValue();

	        long totalDays_final = (totalDays*totalMappedPoints);
	        
	        report.put("zone", row.get("zone"));
	        report.put("totalSluicePoints", totalSluicePoints);
	        report.put("totalMappedPoints", totalMappedPoints);
	        report.put("totalDays", totalDays_final);
	        report.put("completedDays", completed);
	        report.put("pendingDays", (totalDays_final - completed));

	        finalList.add(report);
	        
	        // accumulate grand totals
	        grandTotalSluicePoints += totalSluicePoints;
	        grandTotalMappedPoints += totalMappedPoints;
	        grandCompletedDays += completed;
	        grandPendingDays +=(totalDays_final - completed);
	    }

	    // add grand total row
	    Map<String, Object> grandTotalRow = new LinkedHashMap<>();
	    grandTotalRow.put("zone", "TOTAL");
	    grandTotalRow.put("totalSluicePoints", grandTotalSluicePoints);
	    grandTotalRow.put("totalMappedPoints", grandTotalMappedPoints);
	    grandTotalRow.put("totalDays", totalDays);
	    grandTotalRow.put("completedDays", grandCompletedDays);
	    grandTotalRow.put("pendingDays", grandPendingDays); // total pending across all wards
	    
	    finalList.add(grandTotalRow);
	    
	    return finalList;
	}
	
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getWardSummary(String fromDate, String toDate, String zone) {

	    fromDate = convertDateFormat(fromDate, 0);
	    toDate = convertDateFormat(toDate, 0);

	    String sql =
	        "SELECT " +
	        "    w.ward, " +
	        "    COALESCE(dm.total_sluice_points, 0) AS total_sluice_points, " +
	        "    COALESCE(mm.total_mapped_points, 0) AS total_mapped_points, " +
	        "    COALESCE(COUNT(DISTINCT (b.sluicepoint_id), 0)) AS completed_days " +
	        "FROM (" +
	        "    SELECT DISTINCT ward FROM sluicepoint_master WHERE isactive=1 AND isdelete=0 AND zone=?" +
	        ") w " +
	        "LEFT JOIN (" +
	        "    SELECT ward, COUNT(*) AS total_sluice_points " +
	        "    FROM sluicepoint_master " +
	        "    WHERE isactive=1 AND isdelete=0 AND zone=? " +
	        "    GROUP BY ward" +
	        ") dm ON w.ward = dm.ward " +
	        "LEFT JOIN (" +
	        "    SELECT ward, COUNT(*) AS total_mapped_points " +
	        "    FROM sluicepoint_mapped_master " +
	        "    WHERE isactive=1 AND isdelete=0 AND zone=? " +
	        "    GROUP BY ward" +
	        ") mm ON w.ward = mm.ward " +
	        "LEFT JOIN sluicepoint_mapped_master dmm ON dmm.ward = w.ward AND dmm.isactive=1 AND dmm.isdelete=0 " +
	        "LEFT JOIN before_activity b ON dmm.sluicepoint_id = b.sluicepoint_id " +
	        "    AND b.isactive=1 AND b.isdelete=0 " +
	        "    AND b.indate >= ? AND b.indate < DATE_ADD(?, INTERVAL 1 DAY) " +
	        "GROUP BY w.ward, dm.total_sluice_points, mm.total_mapped_points " +
	        "ORDER BY w.ward";

	    List<Map<String, Object>> results = jdbcSluiceTemplate.queryForList(
	        sql, zone, zone, zone, fromDate, toDate
	    );

	    long totalDays = ChronoUnit.DAYS.between(
	            LocalDate.parse(fromDate),
	            LocalDate.parse(toDate)
	    ) + 1;

	    List<Map<String, Object>> finalList = new ArrayList<>();

	    long grandTotalSluicePoints = 0;
	    long grandTotalMappedPoints = 0;
	    long grandCompletedDays = 0;
	    long grandPendingDays = 0;
	    
	    for (Map<String, Object> row : results) {
	        Map<String, Object> report = new LinkedHashMap<>();
	        long completed = ((Number) row.get("completed_days")).longValue();
	        long totalSluicePoints = ((Number) row.get("total_sluice_points")).longValue();
	        long totalMappedPoints = ((Number) row.get("total_mapped_points")).longValue();
	        long totalDays_final = (totalDays*totalMappedPoints);
	        
	        report.put("ward", row.get("ward"));
	        report.put("totalSluicePoints", totalSluicePoints);
	        report.put("totalMappedPoints", totalMappedPoints);
	        report.put("totalDays", totalDays_final);
	        report.put("completedDays", completed);
	        report.put("pendingDays", (totalDays_final - completed));

	        finalList.add(report);
	        
	        // accumulate grand totals
	        grandTotalSluicePoints += totalSluicePoints;
	        grandTotalMappedPoints += totalMappedPoints;
	        grandCompletedDays += completed;
	        grandPendingDays +=(totalDays_final - completed);
	    }

	    // add grand total row
	    Map<String, Object> grandTotalRow = new LinkedHashMap<>();
	    grandTotalRow.put("ward", "TOTAL");
	    grandTotalRow.put("totalSluicePoints", grandTotalSluicePoints);
	    grandTotalRow.put("totalMappedPoints", grandTotalMappedPoints);
	    grandTotalRow.put("totalDays", totalDays);
	    grandTotalRow.put("completedDays", grandCompletedDays);
	    grandTotalRow.put("pendingDays", grandPendingDays); // total pending across all wards

	    finalList.add(grandTotalRow);
	    
	    return finalList;
	}
	
	public List<Map<String, Object>> getSluicePointSummary(String fromDate, String toDate, String ward) {
		
		fromDate = convertDateFormat(fromDate,0);
    	toDate = convertDateFormat(toDate,0);
    	
	    // SQL: count completed days per Sluice point
	    String sql = "SELECT d.road_name, d.sluicepoint_id, COUNT(DISTINCT DATE(b.indate)) AS completedDays "
	               + "FROM sluicepoint_mapped_master d "
	               + "LEFT JOIN before_activity b ON d.sluicepoint_id = b.sluicepoint_id "
	               + "AND b.indate >= ? AND b.indate < DATE_ADD(?, INTERVAL 1 DAY) "
	               + "AND b.isactive=1 AND b.isdelete=0 "
	               + "WHERE d.isactive=1 AND d.isdelete=0 AND d.ward=? "
	               + "GROUP BY d.road_name, d.sluicepoint_id ORDER BY d.road_name";

	    List<Map<String, Object>> results = jdbcSluiceTemplate.queryForList(sql, fromDate, toDate, ward);

	    long totalDays = ChronoUnit.DAYS.between(
	            LocalDate.parse(fromDate), 
	            LocalDate.parse(toDate)
	    ) + 1;

	    List<Map<String, Object>> finalList = new ArrayList<>();
	    
	    //long grandTotalSluicePoints = 0;
	    //long grandTotalMappedPoints = 0;
	    long grandCompletedDays = 0;
	    long grandPendingDays = 0;
	    
	    for (Map<String, Object> row : results) {
	        Map<String, Object> Report = new HashMap<>();
	        long completed = ((Number) row.get("completedDays")).longValue();
	        long totalDays_final = (totalDays*1);
	        
	        Report.put("road_name", row.get("road_name"));
	        Report.put("sluicepoint_id", row.get("sluicepoint_id"));
	        Report.put("totalDays", totalDays_final);
	        Report.put("completed", completed);
	        Report.put("pending", totalDays_final - completed);
	        finalList.add(Report);
	        
	        grandCompletedDays += completed;
	        grandPendingDays +=(totalDays_final - completed);
	    }

	    // add grand total row
	    Map<String, Object> grandTotalRow = new LinkedHashMap<>();
	    grandTotalRow.put("road_name", "TOTAL");
	    grandTotalRow.put("sluicepoint_id", 0);
	    grandTotalRow.put("totalDays", totalDays);
	    grandTotalRow.put("completed", grandCompletedDays);
	    grandTotalRow.put("pending", grandPendingDays); // total pending across all wards

	    //finalList.add(grandTotalRow);
	    
	    return finalList;
	}
}
