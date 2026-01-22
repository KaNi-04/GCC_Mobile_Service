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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class DistributionService {
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
	public DistributionService(Environment environment) {
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
	
	public List<Map<String, Object>> getConfig(String loginid) {
		
		// String ward = getWardByLoginId(loginid,"ae");
		
	    String sql = "SELECT * FROM `shift_master` WHERE `isactive`=1 AND `isdelete`=0 ORDER BY orderby";
	    List<Map<String, Object>> configDetails = jdbcFoodTemplate.queryForList(sql);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Configuration Details.");
	    response.put("data", configDetails);

	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getShiftCount(String loginid, String shiftid) {
		String ward = getWardByLoginId(loginid);
		
	    String sql = "SELECT * FROM `count_master` WHERE (`isactive`=1 AND `isdelete`=0) AND (`ward`=? AND `shiftid`=?)";
	    List<Map<String, Object>> shiftcount = jdbcFoodTemplate.queryForList(sql, ward, shiftid);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Count Details.");
	    response.put("data", shiftcount);

	    return Collections.singletonList(response);
	}
	
	
	@Transactional
	public List<Map<String, Object>> saveRequest(
	        String ward,
	        String required_date,
	        String permanent,
	        String nulm,
	        String private_,
	        String others,
	        String nmr,
	        String weeklyoff_permanent,
	        String weeklyoff_nulm,
	        String weeklyoff_private,
	        String weeklyoff_others,
	        String weeklyoff_nmr,
	        String absentees_permanent,
	        String absentees_nulm,
	        String absentees_private,
	        String absentees_others,
	        String absentees_nmr,
	        String shiftid,
	        String request_by
	) {


	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();
	    
		// -----------------------------------------------
	    // 1️⃣ CHECK IF Request ALREADY EXISTS
	    // -----------------------------------------------
	    String chkSql = "SELECT requestid FROM daily_request WHERE ward=? AND shiftid=? AND required_date=? AND isactive=1 AND isdelete=0";

	    List<Map<String, Object>> exists = jdbcFoodTemplate.queryForList(chkSql, ward, shiftid, convertDateFormat(required_date,0));
	    if (!exists.isEmpty()) {
	        response.put("status", "error");
	        response.put("message", "Food Request exists for this ward:"+ ward+" Shift ID:"+shiftid+" Required Date:"+required_date+".");
	        result.add(response);
	        return result;
	    }
		
		String action = "New Request";
		
	    String insertSqltxt = "INSERT INTO `daily_request`(`ward`, `required_date`, `permanent`, `nulm`, `private`, "
	    		+ "	`others`, `weeklyoff_permanent`, `weeklyoff_nulm`, `weeklyoff_private`, `weeklyoff_others`, "
	    		+ "	`absentees_permanent`, `absentees_nulm`, `absentees_private`, `absentees_others`, `shiftid`, "
	    		+ "	`request_by`,`nmr`,`weeklyoff_nmr`,`absentees_nmr`) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	    String insertSql = insertSqltxt;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcFoodTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"requestid"});
	        int i = 1;
	        ps.setString(i++, ward);
	        ps.setString(i++, convertDateFormat(required_date,0));
	        ps.setString(i++, permanent);
	        ps.setString(i++, nulm);
	        ps.setString(i++, private_);
	        ps.setString(i++, others);
	        ps.setString(i++, weeklyoff_permanent);
	        ps.setString(i++, weeklyoff_nulm);
	        ps.setString(i++, weeklyoff_private);
	        ps.setString(i++, weeklyoff_others);
	        ps.setString(i++, absentees_permanent);
	        ps.setString(i++, absentees_nulm);
	        ps.setString(i++, absentees_private);
	        ps.setString(i++, absentees_others);
	        ps.setString(i++, shiftid);
	        ps.setString(i++, request_by);
	        ps.setString(i++, nmr);
	        ps.setString(i++, weeklyoff_nmr);
	        ps.setString(i++, absentees_nmr);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("Sluice Point", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", action + " data inserted successfully.");
	    } else {
	    	response.put("Sluice Point", action + " insertId error!");
	        response.put("status", "error");
	        response.put("message", action + " data insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
	
	public List<Map<String, Object>> getPending(String loginid) {

	    String ward = getWardByLoginId(loginid);
	 
	    String sql = "SELECT " +
	            "dr.*, " +
	            "sm.Code AS shift_code, " +
	            "sm.name AS shift_name, " +
	            "sm.hint AS shift_hint, " +
	            "sm.time AS shift_time, " +
	            "sm.enable_time AS shift_enable_time, " +
	            "sm.disable_time AS shift_disable_time, " +
	            "DATE_FORMAT(dr.required_date, '%d-%m-%Y') AS required_date_fmt, " +
	            "DATE_FORMAT(dr.request_date, '%d-%m-%Y %H:%i:%s') AS request_date_fmt, " +
	            "(dr.permanent-(weeklyoff_permanent+absentees_permanent)) as current_permanent, " +
	            "(dr.nulm-(weeklyoff_nulm+absentees_nulm)) as current_nulm, " +
	            "(dr.private-(weeklyoff_private+absentees_private)) as current_private, " +
	            "(dr.others-(weeklyoff_others+absentees_others)) as current_others, " +
	            "(dr.nmr-(weeklyoff_nmr+absentees_nmr)) as current_nmr " +
	            "FROM daily_request dr " +
	            "LEFT JOIN shift_master sm ON dr.shiftid = sm.shiftid " +
	            "WHERE dr.isactive = 1 AND dr.isdelete = 0 AND dr.feedbackid = 0 AND dr.required_date <= CURDATE() " +
	            "AND dr.ward = ?";

	    List<Map<String, Object>> pendingList = jdbcFoodTemplate.queryForList(sql, ward);

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Pending Details.");
	    response.put("data", pendingList);

	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getPendingDetails(String loginid, String requestid, String shiftid) {

	    // 1. Get shortfall details
	    String sql = "SELECT SUM(total_food_received) - SUM(return_today) AS total_shortfall "
	               + "FROM gcc_received_update WHERE shiftid=? AND (isactive = 1 AND isdelete = 0) ";

	    List<Map<String, Object>> shortfallDetails = jdbcFoodTemplate.queryForList(sql, shiftid);

	    // 2. Get requested food details
	    sql = "SELECT (permanent + nulm + private + others+ nmr) - "
	            + "((weeklyoff_permanent + weeklyoff_nulm + weeklyoff_private + weeklyoff_others + weeklyoff_nmr) + "
	            + "(absentees_permanent + absentees_nulm + absentees_private + absentees_others + absentees_nmr)) "
	            + "AS total_food_requested "
	            + "FROM daily_request WHERE requestid=? AND (isactive = 1 AND isdelete = 0)";

	    List<Map<String, Object>> requestedInfo = jdbcFoodTemplate.queryForList(sql, requestid);
	    
	    // 3. Get Feedback Questions
	    //sql = "SELECT * FROM `feedback_questions` WHERE isactive=1 and isdelete=0 ORDER BY orderby";
	    sql = "SELECT "
	    		+ "    ql.*, "
	    		+ "    CASE "
	    		+ "        WHEN ql.qtype IN ('select', 'radio') THEN "
	    		+ "            JSON_ARRAYAGG( "
	    		+ "                JSON_OBJECT( "
	    		+ "                    'option_id', qov.id, "
	    		+ "                    'qid', qov.qid, "
	    		+ "                    'english_name', qov.name, "
	    		+ "                    'value', qov.id, "
	    		+ "                    'orderby', qov.orderby "
	    		+ "                ) "
	    		+ "            ) "
	    		+ "        ELSE NULL "
	    		+ "    END AS options "
	    		+ "FROM feedback_questions ql "
	    		+ "LEFT JOIN feedback_option_values qov "
	    		+ "       ON qov.qid = ql.qid "
	    		+ "      AND qov.isactive = 1 "
	    		+ "      AND qov.isdelete = 0 "
	    		+ "WHERE ql.isactive = 1 "
	    		+ "GROUP BY ql.qid";

	    //List<Map<String, Object>> questionInfo = jdbcFoodTemplate.queryForList(sql);

	    List<Map<String, Object>> questionInfo = jdbcFoodTemplate.queryForList(sql);
        Iterator<Map<String, Object>> iterator = questionInfo.iterator();
        ObjectMapper mapper = new ObjectMapper();
        while (iterator.hasNext()) {
            Map<String, Object> row = iterator.next();
            Object optionsRaw = row.get("options");
            if (optionsRaw != null && optionsRaw instanceof String) {
                try {
                    List<Map<String, Object>> optionsParsed = mapper.readValue((String) optionsRaw, List.class);
                    
                    // Sort options by 'orderby'
                    optionsParsed.sort(Comparator.comparing(opt -> {
                        Object order = opt.get("orderby");
                        return (order instanceof Number) ? ((Number) order).intValue() : 0;
                    }));
                    
                    row.put("options", optionsParsed);
                } catch (Exception e) {
                    row.put("options", null); // fallback if malformed
                }
            }
        }
        
	    // 3. Combine both list results
	    List<Map<String, Object>> pendingDetails = new ArrayList<>();
	    pendingDetails.addAll(shortfallDetails);
	    pendingDetails.addAll(requestedInfo);
	    pendingDetails.addAll(questionInfo);
	    
	    // Combine request info (shortfall + requested)
	    List<Map<String, Object>> requestInfo = new ArrayList<>();
	    requestInfo.addAll(shortfallDetails);
	    requestInfo.addAll(requestedInfo);

	    // Final response structure
	    Map<String, Object> data = new HashMap<>();
	    data.put("request_info", requestInfo);
	    data.put("feedback_question", questionInfo);
	    
	    // 4. Create final response
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Pending Details.");
	    response.put("data", data);
	    
	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveRecivedUpdate(
	        String requestid,
	        String shiftid,
	        String total_box_shortfall,
	        String return_today,
	        String total_food_requested,
	        String total_food_received,
	        String today_absentees_permanent,
	        String today_absentees_nulm,
	        String today_absentees_private,
	        String today_absentees_others,
	        String received_by,
	        String today_absentees_nmr
	){

		String action = "New Request";
		
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();
	    
	    String insertSqltxt = "INSERT INTO `gcc_received_update`(`requestid`, `shiftid`, `total_box_shortfall`, `return_today`, "
	    		+ "	`total_food_requested`, `total_food_received`, `today_absentees_permanent`, "
	    		+ "	`today_absentees_nulm`, `today_absentees_private`, `today_absentees_others`, `received_by`, `today_absentees_nmr`) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

	    String insertSql = insertSqltxt;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcFoodTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"receivedid"});
	        int i = 1;
	        ps.setString(i++, requestid);
	        ps.setString(i++, shiftid);
	        ps.setString(i++, total_box_shortfall);
	        ps.setString(i++, return_today);
	        ps.setString(i++, total_food_requested);
	        ps.setString(i++, total_food_received);
	        ps.setString(i++, today_absentees_permanent);
	        ps.setString(i++, today_absentees_nulm);
	        ps.setString(i++, today_absentees_private);
	        ps.setString(i++, today_absentees_others);
	        ps.setString(i++, received_by);
	        ps.setString(i++, today_absentees_nmr);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("Sluice Point", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", action + " data inserted successfully.");
	    } else {
	    	response.put("Sluice Point", action + " insertId error!");
	        response.put("status", "error");
	        response.put("message", action + " data insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}
	
	@Transactional
	public List<Map<String, Object>> saveFullFeedbackAndReceived(
	        String requestid, String shiftid,

	        // Feedback
	        String q1, MultipartFile q1_img,
	        String q2, MultipartFile q2_img,
	        String q3, MultipartFile q3_img,
	        String feedback_by,

	        // Received Update
	        String total_box_shortfall,
	        String return_today,
	        String total_food_requested,
	        String total_food_received,
	        String today_absentees_permanent,
	        String today_absentees_nulm,
	        String today_absentees_private,
	        String today_absentees_others,
	        String received_by,
	        String today_absentees_nmr,
	        String remarks
	) {

	    List<Map<String, Object>> result = new ArrayList<>();
	    Map<String, Object> response = new HashMap<>();


	    // -----------------------------------------------
	    // 1️⃣ CHECK IF FEEDBACK ALREADY EXISTS
	    // -----------------------------------------------
	    String chkSql = "SELECT feedbackid FROM feedback_data WHERE requestid=? AND isactive=1 AND isdelete=0";

	    List<Map<String, Object>> exists = jdbcFoodTemplate.queryForList(chkSql, requestid);
	    if (!exists.isEmpty()) {
	        response.put("status", "error");
	        response.put("message", "Feedback already exists for this request.");
	        result.add(response);
	        return result;
	    }

	    String filetype = "image";

		 // Q1 Image
		 final String[] q1_img_path = { null };
		 if (q1_img != null && !q1_img.isEmpty()) {
			 q1_img_path[0] = fileUpload(requestid, shiftid, q1_img, filetype);
		     if ("error".equalsIgnoreCase(q1_img_path[0])) {
		         response.put("status", "error");
		         response.put("message", "Q1 feedback image upload failed.");
		         result.add(response);
		         return result;
		     }
		 }
	
		 // Q2 Image
		 final String[] q2_img_path = { null };
		 if (q2_img != null && !q2_img.isEmpty()) {
		     q2_img_path[0] = fileUpload(requestid, shiftid, q2_img, filetype);
		     if ("error".equalsIgnoreCase(q2_img_path[0])) {
		         response.put("status", "error");
		         response.put("message", "Q2 feedback image upload failed.");
		         result.add(response);
		         return result;
		     }
		 }
	
		 // Q3 Image
		 final String[] q3_img_path = { null };
		 if (q3_img != null && !q3_img.isEmpty()) {
		     q3_img_path[0] = fileUpload(requestid, shiftid, q3_img, filetype);
		     if ("error".equalsIgnoreCase(q3_img_path[0])) {
		         response.put("status", "error");
		         response.put("message", "Q3 feedback image upload failed.");
		         result.add(response);
		         return result;
		     }
		 }
	    
	    // -----------------------------------------------
	    // 2️⃣ INSERT FEEDBACK FIRST
	    // -----------------------------------------------
	    String feedbackSql = """
	        INSERT INTO feedback_data
	        (requestid, q1, q1_img, q2, q2_img, q3, q3_img, feedback_by,remarks)
	        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
	    """;

	    KeyHolder feedbackKeyHolder = new GeneratedKeyHolder();

	    jdbcFoodTemplate.update(conn -> {
	        PreparedStatement ps = conn.prepareStatement(feedbackSql, new String[]{"feedbackid"});
	        int i = 1;
	        ps.setString(i++, requestid);
	        ps.setString(i++, q1);
	        ps.setString(i++, q1_img_path[0]);
	        ps.setString(i++, q2);
	        ps.setString(i++, q2_img_path[0]);
	        ps.setString(i++, q3);
	        ps.setString(i++, q3_img_path[0]);
	        ps.setString(i++, feedback_by);
	        ps.setString(i++, remarks);
	        return ps;
	    }, feedbackKeyHolder);

	    int feedbackId = feedbackKeyHolder.getKey().intValue();

		 try {
		    // -----------------------------------------------
		    // 3️⃣ SAVE RECEIVED UPDATE (Existing Code Reused)
		    // -----------------------------------------------
		    String receivedSql = """
		        INSERT INTO gcc_received_update
		        (requestid, shiftid, total_box_shortfall, return_today, total_food_requested,
		         total_food_received, today_absentees_permanent, today_absentees_nulm,
		         today_absentees_private, today_absentees_others, received_by, today_absentees_nmr)
		        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		    """;
	
		    KeyHolder receivedKeyHolder = new GeneratedKeyHolder();
	
		    jdbcFoodTemplate.update(conn -> {
		        PreparedStatement ps = conn.prepareStatement(receivedSql, new String[]{"receivedid"});
		        int i = 1;
		        ps.setString(i++, requestid);
		        ps.setString(i++, shiftid);
		        ps.setString(i++, total_box_shortfall);
		        ps.setString(i++, return_today);
		        ps.setString(i++, total_food_requested);
		        ps.setString(i++, total_food_received);
		        ps.setString(i++, today_absentees_permanent);
		        ps.setString(i++, today_absentees_nulm);
		        ps.setString(i++, today_absentees_private);
		        ps.setString(i++, today_absentees_others);
		        ps.setString(i++, received_by);
		        ps.setString(i++, today_absentees_nmr);
		        return ps;
		    }, receivedKeyHolder);
	
		    int receivedId = receivedKeyHolder.getKey().intValue();
		    
		    // ------------------------------
		    // UPDATE DAILY_REQUEST
		    // ------------------------------
		    String updateDailyRequestSql = """
		        UPDATE daily_request
		        SET feedbackid = ?
		        WHERE requestid = ?
		    """;
	
		    int updated = jdbcFoodTemplate.update(
		        updateDailyRequestSql,
		        feedbackId,
		        requestid
		    );
	
		    if (updated == 0) {
		        throw new RuntimeException("Failed to update daily_request");
		    }
	
		    // ------------------------------
		    // SUCCESS RESPONSE
		    // ------------------------------
		    response.put("status", "success");
		    response.put("feedbackid", feedbackId);
		    response.put("receivedid", receivedId);
		    response.put("message", "Feedback and received update saved successfully.");
	
		    result.add(response);
		    return result;
	
		} catch (Exception e) {
			chkSql = "UPDATE feedback_data SET isactive=0, isdelete=1 WHERE requestid=?";
			int updated = jdbcFoodTemplate.update(chkSql,requestid);
		    // ❌ VERY IMPORTANT → rethrow exception
		    // This forces Spring to rollback feedbackSql
		    throw new RuntimeException("Transaction failed. Rolling back ("+requestid+").", e);
		}
	}
	
	// For Reports
	public List<Map<String, Object>> getZoneSummary(String fromDate, String toDate) {
		
		fromDate = convertDateFormat(fromDate,0);
    	toDate = convertDateFormat(toDate,0);
    	
	    // SQL: count completed days per Disposal point
	    String sql = "SELECT * "
	    		+ "FROM ( "
	    		+ " "
	    		+ "    /* ================= ZONE-WISE SUMMARY ================= */ "
	    		+ "    SELECT "
	    		+ "        z.zone AS zone, "
	    		+ " "
	    		+ "        /* TOTAL COMPLEMENT */ "
	    		+ "        ( "
	    		+ "            SELECT IFNULL(SUM(dr.permanent + dr.nulm + dr.private + dr.nmr + dr.others), 0) "
	    		+ "            FROM daily_request dr "
	    		+ "            WHERE dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "              AND dr.shiftid = 1 "
	    		+ "              AND dr.isactive = 1 "
	    		+ "              AND dr.isdelete = 0 "
	    		+ "              AND dr.request_by IN ( "
	    		+ "                    SELECT lm.siloginid "
	    		+ "                    FROM location_mapping lm "
	    		+ "                    WHERE lm.zone = z.zone "
	    		+ "                      AND lm.isactive = 1 "
	    		+ "                      AND lm.isdelete = 0 "
	    		+ "              ) "
	    		+ "        ) AS total_complement, "
	    		+ " "
	    		+ "        /* WEEKLY OFF */ "
	    		+ "        ( "
	    		+ "            SELECT IFNULL(SUM(dr.weeklyoff_permanent + dr.weeklyoff_private + "
	    		+ "                              dr.weeklyoff_nulm + dr.weeklyoff_others + dr.weeklyoff_nmr), 0) "
	    		+ "            FROM daily_request dr "
	    		+ "            WHERE dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "              AND dr.shiftid = 1 "
	    		+ "              AND dr.isactive = 1 "
	    		+ "              AND dr.isdelete = 0 "
	    		+ "              AND dr.request_by IN ( "
	    		+ "                    SELECT lm.siloginid "
	    		+ "                    FROM location_mapping lm "
	    		+ "                    WHERE lm.zone = z.zone "
	    		+ "                      AND lm.isactive = 1 "
	    		+ "                      AND lm.isdelete = 0 "
	    		+ "              ) "
	    		+ "        ) AS weekly_off, "
	    		+ " "
	    		+ "        /* PLANNED LEAVE */ "
	    		+ "        ( "
	    		+ "            SELECT IFNULL(SUM(dr.absentees_permanent + dr.absentees_private + "
	    		+ "                              dr.absentees_nulm + dr.absentees_others + dr.absentees_nmr), 0) "
	    		+ "            FROM daily_request dr "
	    		+ "            WHERE dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "              AND dr.shiftid = 1 "
	    		+ "              AND dr.isactive = 1 "
	    		+ "              AND dr.isdelete = 0 "
	    		+ "              AND dr.request_by IN ( "
	    		+ "                    SELECT lm.siloginid "
	    		+ "                    FROM location_mapping lm "
	    		+ "                    WHERE lm.zone = z.zone "
	    		+ "                      AND lm.isactive = 1 "
	    		+ "                      AND lm.isdelete = 0 "
	    		+ "              ) "
	    		+ "        ) AS planned_leave, "
	    		+ " "
	    		+ "        /* PRESENT COUNT */ "
	    		+ "        ( "
	    		+ "            ( "
	    		+ "                SELECT IFNULL(SUM(dr.permanent + dr.nulm + dr.private + dr.nmr + dr.others), 0) "
	    		+ "                FROM daily_request dr "
	    		+ "            WHERE dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "                  AND dr.shiftid = 1 "
	    		+ "                  AND dr.isactive = 1 "
	    		+ "                  AND dr.isdelete = 0 "
	    		+ "                  AND dr.request_by IN ( "
	    		+ "                        SELECT lm.siloginid "
	    		+ "                        FROM location_mapping lm "
	    		+ "                        WHERE lm.zone = z.zone "
	    		+ "                          AND lm.isactive = 1 "
	    		+ "                          AND lm.isdelete = 0 "
	    		+ "                  ) "
	    		+ "            ) "
	    		+ "            - "
	    		+ "            ( "
	    		+ "                SELECT IFNULL(SUM(dr.weeklyoff_permanent + dr.weeklyoff_private + "
	    		+ "                                  dr.weeklyoff_nulm + dr.weeklyoff_others + dr.weeklyoff_nmr), 0) "
	    		+ "                FROM daily_request dr "
	    		+ "            WHERE dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "                  AND dr.shiftid = 1 "
	    		+ "                  AND dr.isactive = 1 "
	    		+ "                  AND dr.isdelete = 0 "
	    		+ "                  AND dr.request_by IN ( "
	    		+ "                        SELECT lm.siloginid "
	    		+ "                        FROM location_mapping lm "
	    		+ "                        WHERE lm.zone = z.zone "
	    		+ "                          AND lm.isactive = 1 "
	    		+ "                          AND lm.isdelete = 0 "
	    		+ "                  ) "
	    		+ "            ) "
	    		+ "            - "
	    		+ "            ( "
	    		+ "                SELECT IFNULL(SUM(dr.absentees_permanent + dr.absentees_private + "
	    		+ "                                  dr.absentees_nulm + dr.absentees_others + dr.absentees_nmr), 0) "
	    		+ "                FROM daily_request dr "
	    		+ "            WHERE dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "                  AND dr.shiftid = 1 "
	    		+ "                  AND dr.isactive = 1 "
	    		+ "                  AND dr.isdelete = 0 "
	    		+ "                  AND dr.request_by IN ( "
	    		+ "                        SELECT lm.siloginid "
	    		+ "                        FROM location_mapping lm "
	    		+ "                        WHERE lm.zone = z.zone "
	    		+ "                          AND lm.isactive = 1 "
	    		+ "                          AND lm.isdelete = 0 "
	    		+ "                  ) "
	    		+ "            ) "
	    		+ "        ) AS present_count, "
	    		+ " "
	    		+ "        /* FOOD REQUESTED (same as present) */ "
	    		+ "        ( "
	    		+ "            ( "
	    		+ "                SELECT IFNULL(SUM(dr.permanent + dr.nulm + dr.private + dr.nmr + dr.others), 0) "
	    		+ "                FROM daily_request dr "
	    		+ "            WHERE dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "                  AND dr.shiftid = 1 "
	    		+ "                  AND dr.isactive = 1 "
	    		+ "                  AND dr.isdelete = 0 "
	    		+ "                  AND dr.request_by IN ( "
	    		+ "                        SELECT lm.siloginid "
	    		+ "                        FROM location_mapping lm "
	    		+ "                        WHERE lm.zone = z.zone "
	    		+ "                          AND lm.isactive = 1 "
	    		+ "                          AND lm.isdelete = 0 "
	    		+ "                  ) "
	    		+ "            ) "
	    		+ "            - "
	    		+ "            ( "
	    		+ "                SELECT IFNULL(SUM(dr.weeklyoff_permanent + dr.weeklyoff_private + "
	    		+ "                                  dr.weeklyoff_nulm + dr.weeklyoff_others + dr.weeklyoff_nmr), 0) "
	    		+ "                FROM daily_request dr "
	    		+ "            WHERE dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "                  AND dr.shiftid = 1 "
	    		+ "                  AND dr.isactive = 1 "
	    		+ "                  AND dr.isdelete = 0 "
	    		+ "                  AND dr.request_by IN ( "
	    		+ "                        SELECT lm.siloginid "
	    		+ "                        FROM location_mapping lm "
	    		+ "                        WHERE lm.zone = z.zone "
	    		+ "                          AND lm.isactive = 1 "
	    		+ "                          AND lm.isdelete = 0 "
	    		+ "                  ) "
	    		+ "            ) "
	    		+ "            - "
	    		+ "            ( "
	    		+ "                SELECT IFNULL(SUM(dr.absentees_permanent + dr.absentees_private + "
	    		+ "                                  dr.absentees_nulm + dr.absentees_others + dr.absentees_nmr), 0) "
	    		+ "                FROM daily_request dr "
	    		+ "            WHERE dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "                  AND dr.shiftid = 1 "
	    		+ "                  AND dr.isactive = 1 "
	    		+ "                  AND dr.isdelete = 0 "
	    		+ "                  AND dr.request_by IN ( "
	    		+ "                        SELECT lm.siloginid "
	    		+ "                        FROM location_mapping lm "
	    		+ "                        WHERE lm.zone = z.zone "
	    		+ "                          AND lm.isactive = 1 "
	    		+ "                          AND lm.isdelete = 0 "
	    		+ "                  ) "
	    		+ "            ) "
	    		+ "        ) AS food_requested, "
	    		+ " "
	    		+ "        /* FOOD DELIVERED */ "
	    		+ "        ( "
	    		+ "            SELECT IFNULL(SUM(gru.total_food_received), 0) "
	    		+ "            FROM gcc_received_update gru "
	    		+ "            WHERE gru.shiftid = 1 "
	    		+ "              AND gru.isactive = 1 "
	    		+ "              AND gru.isdelete = 0 "
	    		+ "              AND gru.requestid IN ( "
	    		+ "                    SELECT dr.requestid "
	    		+ "                    FROM daily_request dr "
	    		+ "            WHERE dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "                      AND dr.shiftid = 1 "
	    		+ "                      AND dr.isactive = 1 "
	    		+ "                      AND dr.isdelete = 0 "
	    		+ "                      AND dr.request_by IN ( "
	    		+ "                            SELECT lm.siloginid "
	    		+ "                            FROM location_mapping lm "
	    		+ "                            WHERE lm.zone = z.zone "
	    		+ "                              AND lm.isactive = 1 "
	    		+ "                              AND lm.isdelete = 0 "
	    		+ "                      ) "
	    		+ "              ) "
	    		+ "        ) AS food_delivered, "
	    		+ " "
	    		+ "        /* FOOD SHORTFALL */ "
	    		+ "        ( "
	    		+ "            ( "
	    		+ "                ( "
	    		+ "                    SELECT IFNULL(SUM(dr.permanent + dr.nulm + dr.private + dr.nmr + dr.others), 0) "
	    		+ "                    FROM daily_request dr "
	    		+ "            WHERE dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "                      AND dr.shiftid = 1 "
	    		+ "                      AND dr.isactive = 1 "
	    		+ "                      AND dr.isdelete = 0 "
	    		+ "                      AND dr.request_by IN ( "
	    		+ "                            SELECT lm.siloginid "
	    		+ "                            FROM location_mapping lm "
	    		+ "                            WHERE lm.zone = z.zone "
	    		+ "                              AND lm.isactive = 1 "
	    		+ "                              AND lm.isdelete = 0 "
	    		+ "                      ) "
	    		+ "                ) "
	    		+ "                - "
	    		+ "                ( "
	    		+ "                    SELECT IFNULL(SUM(dr.weeklyoff_permanent + dr.weeklyoff_private + "
	    		+ "                                      dr.weeklyoff_nulm + dr.weeklyoff_others + dr.weeklyoff_nmr), 0) "
	    		+ "                    FROM daily_request dr "
	    		+ "            WHERE dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "                      AND dr.shiftid = 1 "
	    		+ "                      AND dr.isactive = 1 "
	    		+ "                      AND dr.isdelete = 0 "
	    		+ "                      AND dr.request_by IN ( "
	    		+ "                            SELECT lm.siloginid "
	    		+ "                            FROM location_mapping lm "
	    		+ "                            WHERE lm.zone = z.zone "
	    		+ "                              AND lm.isactive = 1 "
	    		+ "                              AND lm.isdelete = 0 "
	    		+ "                      ) "
	    		+ "                ) "
	    		+ "                - "
	    		+ "                ( "
	    		+ "                    SELECT IFNULL(SUM(dr.absentees_permanent + dr.absentees_private + "
	    		+ "                                      dr.absentees_nulm + dr.absentees_others + dr.absentees_nmr), 0) "
	    		+ "                    FROM daily_request dr "
	    		+ "            WHERE dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "                      AND dr.shiftid = 1 "
	    		+ "                      AND dr.isactive = 1 "
	    		+ "                      AND dr.isdelete = 0 "
	    		+ "                      AND dr.request_by IN ( "
	    		+ "                            SELECT lm.siloginid "
	    		+ "                            FROM location_mapping lm "
	    		+ "                            WHERE lm.zone = z.zone "
	    		+ "                              AND lm.isactive = 1 "
	    		+ "                              AND lm.isdelete = 0 "
	    		+ "                      ) "
	    		+ "                ) "
	    		+ "            ) "
	    		+ "            - "
	    		+ "            ( "
	    		+ "                SELECT IFNULL(SUM(gru.total_food_received), 0) "
	    		+ "                FROM gcc_received_update gru "
	    		+ "                WHERE gru.shiftid = 1 "
	    		+ "                  AND gru.isactive = 1 "
	    		+ "                  AND gru.isdelete = 0 "
	    		+ "                  AND gru.requestid IN ( "
	    		+ "                        SELECT dr.requestid "
	    		+ "                        FROM daily_request dr "
	    		+ "            WHERE dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "                          AND dr.shiftid = 1 "
	    		+ "                          AND dr.isactive = 1 "
	    		+ "                          AND dr.isdelete = 0 "
	    		+ "                          AND dr.request_by IN ( "
	    		+ "                                SELECT lm.siloginid "
	    		+ "                                FROM location_mapping lm "
	    		+ "                                WHERE lm.zone = z.zone "
	    		+ "                                  AND lm.isactive = 1 "
	    		+ "                                  AND lm.isdelete = 0 "
	    		+ "                          ) "
	    		+ "                  ) "
	    		+ "            ) "
	    		+ "        ) AS food_shortfall "
	    		+ " "
	    		+ "    FROM ( "
	    		+ "        SELECT DISTINCT zone "
	    		+ "        FROM location_mapping "
	    		+ "        WHERE zone IS NOT NULL "
	    		+ "          AND isactive = 1 "
	    		+ "          AND isdelete = 0 "
	    		+ "        ORDER BY CAST(zone AS UNSIGNED) "
	    		+ "    ) z "
	    		+ " "
	    		+ "    UNION ALL "
	    		+ " "
	    		+ "    /* ================= GRAND TOTAL ================= */ "
	    		+ "    SELECT "
	    		+ "        'TOTAL' AS zone, "
	    		+ " "
	    		+ "        IFNULL(SUM(dr.permanent + dr.nulm + dr.private + dr.nmr + dr.others), 0), "
	    		+ "        IFNULL(SUM(dr.weeklyoff_permanent + dr.weeklyoff_private + dr.weeklyoff_nulm + "
	    		+ "                    dr.weeklyoff_others + dr.weeklyoff_nmr), 0), "
	    		+ "        IFNULL(SUM(dr.absentees_permanent + dr.absentees_private + dr.absentees_nulm + "
	    		+ "                    dr.absentees_others + dr.absentees_nmr), 0), "
	    		+ " "
	    		+ "        ( "
	    		+ "            IFNULL(SUM(dr.permanent + dr.nulm + dr.private + dr.nmr + dr.others), 0) "
	    		+ "            - "
	    		+ "            IFNULL(SUM(dr.weeklyoff_permanent + dr.weeklyoff_private + dr.weeklyoff_nulm + "
	    		+ "                        dr.weeklyoff_others + dr.weeklyoff_nmr), 0) "
	    		+ "            - "
	    		+ "            IFNULL(SUM(dr.absentees_permanent + dr.absentees_private + dr.absentees_nulm + "
	    		+ "                        dr.absentees_others + dr.absentees_nmr), 0) "
	    		+ "        ), "
	    		+ " "
	    		+ "        ( "
	    		+ "            IFNULL(SUM(dr.permanent + dr.nulm + dr.private + dr.nmr + dr.others), 0) "
	    		+ "            - "
	    		+ "            IFNULL(SUM(dr.weeklyoff_permanent + dr.weeklyoff_private + dr.weeklyoff_nulm + "
	    		+ "                        dr.weeklyoff_others + dr.weeklyoff_nmr), 0) "
	    		+ "            - "
	    		+ "            IFNULL(SUM(dr.absentees_permanent + dr.absentees_private + dr.absentees_nulm + "
	    		+ "                        dr.absentees_others + dr.absentees_nmr), 0) "
	    		+ "        ), "
	    		+ " "
	    		+ "        IFNULL(SUM(gru.total_food_received), 0), "
	    		+ " "
	    		+ "        ( "
	    		+ "            ( "
	    		+ "                IFNULL(SUM(dr.permanent + dr.nulm + dr.private + dr.nmr + dr.others), 0) "
	    		+ "                - "
	    		+ "                IFNULL(SUM(dr.weeklyoff_permanent + dr.weeklyoff_private + dr.weeklyoff_nulm + "
	    		+ "                            dr.weeklyoff_others + dr.weeklyoff_nmr), 0) "
	    		+ "                - "
	    		+ "                IFNULL(SUM(dr.absentees_permanent + dr.absentees_private + dr.absentees_nulm + "
	    		+ "                            dr.absentees_others + dr.absentees_nmr), 0) "
	    		+ "            ) "
	    		+ "            - "
	    		+ "            IFNULL(SUM(gru.total_food_received), 0) "
	    		+ "        ) "
	    		+ " "
	    		+ "    FROM daily_request dr "
	    		+ "    LEFT JOIN gcc_received_update gru "
	    		+ "           ON gru.requestid = dr.requestid "
	    		+ "          AND gru.isactive = 1 "
	    		+ "          AND gru.isdelete = 0 "
	    		+ "          AND dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "      AND dr.shiftid = 1 "
	    		+ "      AND dr.isactive = 1 "
	    		+ "      AND dr.isdelete = 0 "
	    		+ " "
	    		+ ") final_output "
	    		+ " "
	    		+ "ORDER BY "
	    		+ "    CASE WHEN zone = 'TOTAL' THEN 1 ELSE 0 END, "
	    		+ "    CAST(zone AS UNSIGNED)";

	    List<Map<String, Object>> results = jdbcFoodTemplate.queryForList(sql);

	    return results;
	}
	public List<Map<String, Object>> getWardSummary(String fromDate, String toDate, String zone) {
		
		fromDate = convertDateFormat(fromDate,0);
    	toDate = convertDateFormat(toDate,0);
    	
	    // SQL: count completed days per Disposal point
	    String sql = "WITH ward_data AS ( "
	    		+ "    SELECT "
	    		+ "        w.ward, "
	    		+ " "
	    		+ "        /* TOTAL COMPLEMENT */ "
	    		+ "        IFNULL(SUM(dr.permanent + dr.nulm + dr.private + dr.nmr + dr.others), 0) AS total_complement, "
	    		+ " "
	    		+ "        /* WEEKLY OFF */ "
	    		+ "        IFNULL(SUM( "
	    		+ "            dr.weeklyoff_permanent + dr.weeklyoff_private + "
	    		+ "            dr.weeklyoff_nulm + dr.weeklyoff_others + dr.weeklyoff_nmr "
	    		+ "        ), 0) AS weekly_off, "
	    		+ " "
	    		+ "        /* PLANNED LEAVE */ "
	    		+ "        IFNULL(SUM( "
	    		+ "            dr.absentees_permanent + dr.absentees_private + "
	    		+ "            dr.absentees_nulm + dr.absentees_others + dr.absentees_nmr "
	    		+ "        ), 0) AS planned_leave, "
	    		+ " "
	    		+ "        /* PRESENT COUNT */ "
	    		+ "        ( "
	    		+ "            IFNULL(SUM(dr.permanent + dr.nulm + dr.private + dr.nmr + dr.others), 0) "
	    		+ "            - "
	    		+ "            IFNULL(SUM( "
	    		+ "                dr.weeklyoff_permanent + dr.weeklyoff_private + "
	    		+ "                dr.weeklyoff_nulm + dr.weeklyoff_others + dr.weeklyoff_nmr "
	    		+ "            ), 0) "
	    		+ "            - "
	    		+ "            IFNULL(SUM( "
	    		+ "                dr.absentees_permanent + dr.absentees_private + "
	    		+ "                dr.absentees_nulm + dr.absentees_others + dr.absentees_nmr "
	    		+ "            ), 0) "
	    		+ "        ) AS present_count, "
	    		+ " "
	    		+ "        /* FOOD REQUESTED */ "
	    		+ "        ( "
	    		+ "            IFNULL(SUM(dr.permanent + dr.nulm + dr.private + dr.nmr + dr.others), 0) "
	    		+ "            - "
	    		+ "            IFNULL(SUM( "
	    		+ "                dr.weeklyoff_permanent + dr.weeklyoff_private + "
	    		+ "                dr.weeklyoff_nulm + dr.weeklyoff_others + dr.weeklyoff_nmr "
	    		+ "            ), 0) "
	    		+ "            - "
	    		+ "            IFNULL(SUM( "
	    		+ "                dr.absentees_permanent + dr.absentees_private + "
	    		+ "                dr.absentees_nulm + dr.absentees_others + dr.absentees_nmr "
	    		+ "            ), 0) "
	    		+ "        ) AS food_requested, "
	    		+ " "
	    		+ "        /* FOOD DELIVERED */ "
	    		+ "        IFNULL(SUM(gru.total_food_received), 0) AS food_delivered, "
	    		+ " "
	    		+ "        /* FOOD SHORTFALL (NO NEGATIVE) */ "
	    		+ "        CASE "
	    		+ "            WHEN IFNULL(SUM(gru.total_food_received), 0) >= "
	    		+ "                 ( "
	    		+ "                    IFNULL(SUM(dr.permanent + dr.nulm + dr.private + dr.nmr + dr.others), 0) "
	    		+ "                    - "
	    		+ "                    IFNULL(SUM( "
	    		+ "                        dr.weeklyoff_permanent + dr.weeklyoff_private + "
	    		+ "                        dr.weeklyoff_nulm + dr.weeklyoff_others + dr.weeklyoff_nmr "
	    		+ "                    ), 0) "
	    		+ "                    - "
	    		+ "                    IFNULL(SUM( "
	    		+ "                        dr.absentees_permanent + dr.absentees_private + "
	    		+ "                        dr.absentees_nulm + dr.absentees_others + dr.absentees_nmr "
	    		+ "                    ), 0) "
	    		+ "                 ) "
	    		+ "            THEN 0 "
	    		+ "            ELSE "
	    		+ "                ( "
	    		+ "                    ( "
	    		+ "                        IFNULL(SUM(dr.permanent + dr.nulm + dr.private + dr.nmr + dr.others), 0) "
	    		+ "                        - "
	    		+ "                        IFNULL(SUM( "
	    		+ "                            dr.weeklyoff_permanent + dr.weeklyoff_private + "
	    		+ "                            dr.weeklyoff_nulm + dr.weeklyoff_others + dr.weeklyoff_nmr "
	    		+ "                        ), 0) "
	    		+ "                        - "
	    		+ "                        IFNULL(SUM( "
	    		+ "                            dr.absentees_permanent + dr.absentees_private + "
	    		+ "                            dr.absentees_nulm + dr.absentees_others + dr.absentees_nmr "
	    		+ "                        ), 0) "
	    		+ "                    ) "
	    		+ "                    - "
	    		+ "                    IFNULL(SUM(gru.total_food_received), 0) "
	    		+ "                ) "
	    		+ "        END AS food_shortfall "
	    		+ " "
	    		+ "    FROM ( "
	    		+ "        SELECT DISTINCT ward "
	    		+ "        FROM location_mapping "
	    		+ "        WHERE zone = '"+zone+"' "
	    		+ "          AND ward IS NOT NULL "
	    		+ "          AND isactive = 1 "
	    		+ "          AND isdelete = 0 "
	    		+ "    ) w "
	    		+ " "
	    		+ "    LEFT JOIN location_mapping lm "
	    		+ "           ON lm.ward = w.ward "
	    		+ "          AND lm.zone = '"+zone+"' "
	    		+ "          AND lm.isactive = 1 "
	    		+ "          AND lm.isdelete = 0 "
	    		+ " "
	    		+ "    LEFT JOIN daily_request dr "
	    		+ "           ON dr.request_by = lm.siloginid "
	    		+ "          AND dr.required_date >= '"+fromDate+"' AND dr.required_date<='"+toDate+"'"
	    		+ "          AND dr.shiftid = 1 "
	    		+ "          AND dr.isactive = 1 "
	    		+ "          AND dr.isdelete = 0 "
	    		+ " "
	    		+ "    LEFT JOIN gcc_received_update gru "
	    		+ "           ON gru.requestid = dr.requestid "
	    		+ "          AND gru.isactive = 1 "
	    		+ "          AND gru.isdelete = 0 "
	    		+ " "
	    		+ "    GROUP BY w.ward "
	    		+ ") "
	    		+ " "
	    		+ "SELECT * FROM ward_data "
	    		+ " "
	    		+ "UNION ALL "
	    		+ " "
	    		+ "SELECT "
	    		+ "    'TOTAL' AS ward, "
	    		+ "    SUM(total_complement), "
	    		+ "    SUM(weekly_off), "
	    		+ "    SUM(planned_leave), "
	    		+ "    SUM(present_count), "
	    		+ "    SUM(food_requested), "
	    		+ "    SUM(food_delivered), "
	    		+ "    SUM(food_shortfall) "
	    		+ "FROM ward_data "
	    		+ " "
	    		+ "ORDER BY "
	    		+ "    CASE WHEN ward = 'TOTAL' THEN 1 ELSE 0 END, "
	    		+ "    ward";

	    List<Map<String, Object>> results = jdbcFoodTemplate.queryForList(sql);

	    return results;
	}
}