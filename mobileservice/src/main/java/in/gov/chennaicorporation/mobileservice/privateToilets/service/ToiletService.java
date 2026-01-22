package in.gov.chennaicorporation.mobileservice.privateToilets.service;

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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class ToiletService {
	
	private JdbcTemplate jdbcToiletsTemplate;
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final int STRING_LENGTH = 15;
	private static final Random RANDOM = new SecureRandom();

	@Autowired
	public void setDataSource(@Qualifier("mysqlPrivateToiletsDataSource") DataSource toiletsDataSource) {
		this.jdbcToiletsTemplate = new JdbcTemplate(toiletsDataSource);
	}
	
	@Autowired
	public ToiletService(Environment environment) {
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

	public String fileUpload(String name, String id, MultipartFile file) {

		int lastInsertId = 0;
		// Set the file path where you want to save it
		String uploadDirectory = environment.getProperty("file.upload.directory");
		String serviceFolderName = environment.getProperty("private_toilets_foldername");
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

			// Compress the image
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50% quality

			// Write the bytes to the file
			Files.write(path, compressedBytes);

			System.out.println(filePath);
			return filepath_txt;

		} catch (IOException e) {
			e.printStackTrace();
			return "Failed to save file " + file.getOriginalFilename();
		}
	}
	
	public List<Map<String, Object>> getToiletsList(String loginid) {
        String sql = "SELECT *"
        		//+ ",CONCAT('"+fileBaseUrl+"/gccofficialapp/files',image) AS photo "
        		+ " FROM `gcc_toilet_list` WHERE `isactive`=1";
        List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sql,loginid);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Toilets List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> toiletlistRest(
			MultiValueMap<String, String> formData,
			String latitude,
			String longitude) {
			
		String sqlWhere = "";

		 Map<String, Object> response = new HashMap<>();
		 
		if (latitude != null && longitude != null && !latitude.isBlank() && !latitude.isEmpty() && !longitude.isBlank()
		        && !longitude.isEmpty()) {
		    String sqlQuery = "SELECT gl.*, tf.last_feedback_date " +
		            "FROM gcc_toilet_list gl " +
		            "LEFT JOIN ( " +
		            "    SELECT toilet_id, DATE_FORMAT(MAX(cdate), '%d-%m-%Y') AS last_feedback_date " +
		            "    FROM toilet_feedback WHERE isactive = 1 " +
		            "    GROUP BY toilet_id " +
		            ") tf ON tf.toilet_id = gl.id " +
		            "WHERE gl.isactive = 1 " +
		            "  AND ( " +
		            "    (6371008.8 * ACOS(ROUND( " +
		            "        COS(RADIANS(" + latitude + ")) * COS(RADIANS(gl.latitude)) * " +
		            "        COS(RADIANS(gl.longitude) - RADIANS(" + longitude + ")) + " +
		            "        SIN(RADIANS(" + latitude + ")) * SIN(RADIANS(gl.latitude)), " +
		            "    9))) < 200 " +
		            "  ) " +
		            "ORDER BY gl.id DESC";
		    
		    List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sqlQuery);
		   
	        response.put("status", 200);
	        response.put("message", "Nearby toilet list.");
	        if(result.isEmpty()) {
	        	response.put("message", "No toilet found nearby.");
	        }
	        response.put("data", result);
		}
		else {
			response.put("status", 200);
			response.put("message", "No toilet found nearby.");
		}
		return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> toiletlistbylatlong(
			MultiValueMap<String, String> formData,
			String latitude,
			String longitude,
			//String id,
			String loginid) {
			
		String sqlWhere = "";

		 Map<String, Object> response = new HashMap<>();
		 
		if (latitude != null && longitude != null && !latitude.isBlank() && !latitude.isEmpty() && !longitude.isBlank()
		        && !longitude.isEmpty()) {
		   /*
			sqlWhere += " AND ((6371008.8 * acos(ROUND(cos(radians(" + latitude
		            + ")) * cos(radians(latitude)) * cos(radians(longitude) - radians(" + longitude
		            + ")) + sin(radians(" + latitude + ")) * sin(radians(latitude)), 9))) < 200)"
		            + " ORDER BY `id` DESC";
		    */
			/*
		    String sqlQuery = "SELECT * "
					//+ ",       CONCAT('https://gccservices.in/gccofficialapp/files', image) AS photo\n"
					+ "FROM gcc_toilet_list "
					+ "WHERE isactive = 1 "
					+ "  AND ( "
					+ "    (6371008.8 * ACOS(ROUND( "
					+ "        COS(RADIANS("+ latitude +")) * COS(RADIANS(latitude)) * "
					+ "        COS(RADIANS(longitude) - RADIANS("+longitude+")) + "
					+ "        SIN(RADIANS("+latitude+")) * SIN(RADIANS(latitude)), "
					+ "    9))) < 200 "
					+ "  ) "
					+ "ORDER BY id DESC";
		    */
		    String sqlQuery = "SELECT gl.*, tf.last_feedback_date " +
		            "FROM gcc_toilet_list gl " +
		            "LEFT JOIN ( " +
		            "    SELECT toilet_id, DATE_FORMAT(MAX(cdate), '%d-%m-%Y') AS last_feedback_date " +
		            "    FROM toilet_feedback WHERE isactive = 1 " +
		            "    GROUP BY toilet_id " +
		            ") tf ON tf.toilet_id = gl.id " +
		            "WHERE gl.isactive = 1 " +
		            "  AND ( " +
		            "    (6371008.8 * ACOS(ROUND( " +
		            "        COS(RADIANS(" + latitude + ")) * COS(RADIANS(gl.latitude)) * " +
		            "        COS(RADIANS(gl.longitude) - RADIANS(" + longitude + ")) + " +
		            "        SIN(RADIANS(" + latitude + ")) * SIN(RADIANS(gl.latitude)), " +
		            "    9))) < 200 " +
		            "  ) " +
		            "ORDER BY gl.id DESC";
		    
		    List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sqlQuery);
		   
	        response.put("status", 200);
	        response.put("message", "Nearby toilet list.");
	        if(result.isEmpty()) {
	        	response.put("message", "No toilet found nearby.");
	        }
	        response.put("data", result);
		}
		else {
			response.put("status", 200);
			response.put("message", "No toilet found nearby.");
		}
		return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getParentQuestionsList() {
        String sql = "SELECT "
        		+ "    ql.*, "
        		+ "    CASE   "
        		+ "        WHEN (ql.question_type = 'select' OR ql.question_type = 'radio') THEN JSON_ARRAYAGG( "
        		+ "            JSON_OBJECT( "
        		+ "                'option_id', qov.id, "
        		+ "                'english_name', qov.name, "
        		+ "                'value', qov.id, "
        		+ "                'orderby', qov.orderby "
        		+ "            ) "
        		+ "        ) "
        		+ "        ELSE NULL  "
        		+ "    END AS options "
        		+ "FROM question_list ql "
        		+ "LEFT JOIN question_option_values qov  "
        		+ "    ON qov.qid = ql.id  "
        		+ "    AND qov.isactive = 1  "
        		+ "    AND qov.isdelete = 0 "
        		+ "WHERE ql.isactive = 1  "
        		+ "  AND ql.pid = 0 "
        		+ "GROUP BY ql.id";
        
        List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sql);
        Iterator<Map<String, Object>> iterator = result.iterator();
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
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Toilet Parent Question List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getChildQuestionsList(String pid) {
        String sql = "SELECT * FROM question_list WHERE isactive=1 AND `pid`=?";
        List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sql,pid);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Toilet Child Question List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> saveFeedback(
	        String toilet_id,
	        String cby,
	        String latitude,
	        String longitude,
	        String zone,
	        String ward,
	        String q1, String q2, String q3, String q4, String q5, String q6, String q7, String q8, String q9, String q10,
	        String q11, String q12, String q13, String q14, String q15, String q16, String q17, String q18, String q19, String q20,
	        String q21, String q22, String q23, String q24, String q25, String q26, String q27,
	        String q28, String q29, String q30, String q31, String q32, String q33, String q34, String q35,
	        String remarks, MultipartFile filedata,
	        MultipartFile q1_image, MultipartFile q2_image, MultipartFile q3_image, MultipartFile q4_image, MultipartFile q5_image,
	        MultipartFile q6_image, MultipartFile q7_image, MultipartFile q8_image, MultipartFile q9_image, MultipartFile q10_image,
	        MultipartFile q11_image, MultipartFile q12_image, MultipartFile q13_image, MultipartFile q14_image, MultipartFile q15_image,
	        MultipartFile q16_image, MultipartFile q17_image, MultipartFile q18_image, MultipartFile q19_image, MultipartFile q20_image,
	        MultipartFile q21_image, MultipartFile q22_image, MultipartFile q23_image, MultipartFile q24_image, MultipartFile q25_image,
	        MultipartFile q26_image, MultipartFile q27_image, MultipartFile q28_image, MultipartFile q29_image, MultipartFile q30_image, 
	        MultipartFile q31_image, MultipartFile q32_image, MultipartFile q33_image, MultipartFile q34_image, MultipartFile q35_image
	) {
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    String feedbackimg = fileUpload("feedback", "main", filedata);
	    
	    String type = "";
	    
	    // Prepare question image uploads
	    MultipartFile[] files = {
	        q1_image, q2_image, q3_image, q4_image, q5_image, q6_image, q7_image, q8_image, q9_image, q10_image,
	        q11_image, q12_image, q13_image, q14_image, q15_image, q16_image, q17_image, q18_image, q19_image, q20_image,
	        q21_image, q22_image, q23_image, q24_image, q25_image, q26_image, q27_image, q28_image, q29_image, q30_image,
	        q31_image, q32_image, q33_image, q34_image, q35_image 
	    };
	    String[] imagePaths = new String[35];
	    for (int i = 0; i < files.length; i++) {
	        if (files[i] != null && !files[i].isEmpty()) {
	            imagePaths[i] = fileUpload(String.valueOf(i + 1), "q" + (i + 1), files[i]);
	        } else {
	            imagePaths[i] = "";
	        }
	    }

	    // Insert query
	    String insertSql = "INSERT INTO toilet_feedback (toilet_id, cby, latitude, longitude, zone, ward, image, remarks, type, " +
	            "q1, q2, q3, q4, q5, q6, q7, q8, q9, q10, q11, q12, q13, q14, q15, q16, q17, q18, q19, q20, q21, q22, q23, q24, "
	            + "q25, q26, q27, q28, q29, q30, q31, q32, q33, q34, q35," +
	            "q1_image, q2_image, q3_image, q4_image, q5_image, q6_image, q7_image, q8_image, q9_image, q10_image, q11_image, q12_image, " +
	            "q13_image, q14_image, q15_image, q16_image, q17_image, q18_image, q19_image, q20_image, q21_image, q22_image, q23_image, " +
	            "q24_image, q25_image, q26_image, q27_image, q28_image, q29_image, q30_image, q31_image, q32_image, q33_image, q34_image, q35_image) " +
	            "VALUES (" + "?,".repeat(9 + 35 + 35).replaceAll(",$", "") + ")";

	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcToiletsTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"id"});
	        int i = 1;
	        ps.setString(i++, toilet_id);
	        ps.setString(i++, cby);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, feedbackimg);
	        ps.setString(i++, remarks);
	        ps.setString(i++, type);

	        // 27 question answers
	        for (String q : new String[]{q1, q2, q3, q4, q5, q6, q7, q8, q9, q10,
	                q11, q12, q13, q14, q15, q16, q17, q18, q19, q20,
	                q21, q22, q23, q24, q25, q26, q27, q28, q29, q30
	                , q31, q32, q33, q34, q35}) {
	            ps.setString(i++, q);
	        }

	        // 27 question image paths
	        for (String path : imagePaths) {
	            ps.setString(i++, path);
	        }

	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("insertId", lastInsertId);
	        response.put("status", "success");
	        response.put("message", "Feedback inserted successfully.");
	    } else {
	        response.put("status", "error");
	        response.put("message", "Insert failed.");
	    }

	    result.add(response);
	    return result;
	}

	// Report
	
	@Transactional
	public List<Map<String, Object>> zoneReport(
			MultiValueMap<String, String> formData,
			String fromDate,
			String toDate,
			String loginid) {

		String sqlQuery = "SELECT "
				+ "    `gcc_toilet_list`.`zone`, "
				+ "    COUNT(DISTINCT `gcc_toilet_list`.`id`) AS Total_Toilets, "
				+ "    COUNT(DISTINCT `toilet_feedback`.`toilet_id`) AS Completed, "
				+ "    (COUNT(DISTINCT gcc_toilet_list.id) - COUNT(DISTINCT toilet_feedback.toilet_id)) AS Pending "
				+ "FROM "
				+ "    `gcc_toilet_list` "
				+ "LEFT JOIN "
				+ "    `toilet_feedback` "
				+ "    ON `toilet_feedback`.`toilet_id` = `gcc_toilet_list`.`id` "
				+ "		AND `toilet_feedback`.`isactive` = 1 "
				+ "    AND DATE(`toilet_feedback`.`cdate`) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
				+ "WHERE "
				//+ "    `gcc_toilet_list`.`user_id` = ? AND "
				+ "    `gcc_toilet_list`.`isactive` = 1 "
				+ "GROUP BY "
				+ "    `gcc_toilet_list`.`zone` "
				+ "ORDER BY "
				+ "    `gcc_toilet_list`.`zone`";
		
		List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sqlQuery, fromDate, toDate);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Zone report.");
        if(result.isEmpty()) {
        	response.put("message", "No data found.");
        }
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> wardReport(
			MultiValueMap<String, String> formData,
			String fromDate,
			String toDate,
			String zone,
			String loginid) {

		String sqlQuery = "SELECT "
				+ "    `gcc_toilet_list`.`ward`, "
				+ "    COUNT(DISTINCT `gcc_toilet_list`.`id`) AS Total_Toilets, "
				+ "    COUNT(DISTINCT `toilet_feedback`.`toilet_id`) AS Completed, "
				+ "    (COUNT(DISTINCT gcc_toilet_list.id) - COUNT(DISTINCT toilet_feedback.toilet_id)) AS Pending "
				+ "FROM "
				+ "    `gcc_toilet_list` "
				+ "LEFT JOIN "
				+ "    `toilet_feedback` "
				+ "    ON `toilet_feedback`.`toilet_id` = `gcc_toilet_list`.`id` "
				+ "		AND `toilet_feedback`.`isactive` = 1 "
				+ "    AND DATE(`toilet_feedback`.`cdate`) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
				+ "WHERE "
				+ "    `gcc_toilet_list`.`zone` = ? AND "
				+ "    `gcc_toilet_list`.`isactive` = 1 "
				+ "GROUP BY "
				+ "    `gcc_toilet_list`.`ward` "
				+ "ORDER BY "
				+ "    `gcc_toilet_list`.`ward`";
		
		List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sqlQuery, fromDate, toDate, zone);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Ward report.");
        if(result.isEmpty()) {
        	response.put("message", "No data found.");
        }
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	public static List<String> getDateRange(String start, String end) {
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	    LocalDate startDate = LocalDate.parse(start, formatter);
	    LocalDate endDate = LocalDate.parse(end, formatter);
	    List<String> dates = new ArrayList<>();

	    while (!startDate.isAfter(endDate)) {
	        dates.add(startDate.format(formatter));
	        startDate = startDate.plusDays(1);
	    }

	    return dates;
	}
	
	@Transactional
	public List<Map<String, Object>> toiletReport(
			MultiValueMap<String, String> formData,
			String fromDate,
			String toDate,
			String ward,
			String loginid) {
			
		String sqlQuery = "SELECT  "
				+ "    gtl.id AS toilet_id, "
				+ "    gtl.pct_name AS name, "
				+ "    gtl.zone, "
				+ "    gtl.ward, "
				+ "     "
				+ "    DATEDIFF( "
				+ "        STR_TO_DATE(?, '%d-%m-%Y'), "
				+ "        STR_TO_DATE(?, '%d-%m-%Y') "
				+ "    ) + 1 AS total_inspection_need_to_be_done, "
				+ " "
				+ "    COUNT(DISTINCT DATE(tf.cdate)) AS total_inspection_done, "
				+ "     "
				+ "    ( "
				+ "        DATEDIFF( "
				+ "            STR_TO_DATE(?, '%d-%m-%Y'), "
				+ "            STR_TO_DATE(?, '%d-%m-%Y') "
				+ "        ) + 1 "
				+ "        - COUNT(DISTINCT DATE(tf.cdate)) "
				+ "    ) AS total_inspection_pending "
				+ " "
				+ "FROM  "
				+ "    gcc_toilet_list gtl "
				+ "LEFT JOIN  "
				+ "    toilet_feedback tf ON tf.toilet_id = gtl.id "
				+ "    AND tf.isactive = 1 "
				+ "    AND DATE(tf.cdate) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
				+ " "
				+ "WHERE  "
				+ "    gtl.isactive = 1 "
				+ "    AND gtl.ward = ? "
				+ " "
				+ "GROUP BY  "
				+ "    gtl.id "
				+ "ORDER BY  "
				+ "    gtl.id";
		
		List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(
			    sqlQuery,
			    toDate, fromDate,
			    toDate, fromDate,
			    fromDate, toDate, 
			    ward
			);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Toilet list report.");
        if(result.isEmpty()) {
        	response.put("message", "No data found.");
        }
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getToiletDailyFeedbackReport(MultiValueMap<String, String> formData,
			String fromDate,
			String toDate,
			String toilet_id,
			String type,
			String loginid) {
	    List<Map<String, Object>> finalResult = new ArrayList<>();

	    // Step 1: Get all dates between fromDate and toDate
	    List<String> allDates = getDateRange(fromDate, toDate);

	    // Step 2: Execute the SQL
	    String sql = "SELECT gtl.id AS toilet_id, gtl.pct_name, gtl.zone, gtl.ward, " +
	    			 "gtl.latitude, gtl.longitude, " +
	                 "MAX(tf.image) AS image, " +
	                 "GROUP_CONCAT(DATE(tf.cdate) ORDER BY tf.cdate ASC) AS feedback_dates " +
	                 "FROM gcc_toilet_list gtl " +
	                 "LEFT JOIN toilet_feedback tf ON tf.toilet_id = gtl.id AND tf.isactive = 1 " +
	                 "AND DATE(tf.cdate) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') " +
	                 "WHERE gtl.id = ? AND gtl.isactive = 1 " +
	                 "GROUP BY gtl.id";

	    List<Map<String, Object>> toiletData = jdbcToiletsTemplate.queryForList(sql, fromDate, toDate, toilet_id);

	    for (Map<String, Object> toilet : toiletData) {
	        Map<String, Object> report = new LinkedHashMap<>();
	        String toiletId = toilet.get("toilet_id").toString();
	        String image = toilet.get("image") != null ? toilet.get("image").toString() : "";
	        String feedbackImage = image.isEmpty() ? "" : fileBaseUrl+"/gccofficialapp/files" + image;

	        report.put("id", toiletId);
	        report.put("name", toilet.get("pct_name"));
	        report.put("zone", toilet.get("zone"));
	        report.put("ward", toilet.get("ward"));
	        report.put("latitude", toilet.get("latitude"));
	        report.put("longitude", toilet.get("longitude"));
	        report.put("photo", feedbackImage);

	        // Step 3: Build Completed Dates
	        List<Map<String, String>> completedDates = new ArrayList<>();
	        List<Map<String, String>> pendingDates = new ArrayList<>();
	        
	       // if(toilet.get("toilet_id").equals("59")) {
	        	System.out.println("Toilet"+toilet.get("toilet_id")+" :" + toilet.get("feedback_dates"));
	        //}
	        
	        String[] completed = Optional.ofNullable(toilet.get("feedback_dates"))
	                                     .map(Object::toString)
	                                     .map(d -> d.split(","))
	                                     .orElse(new String[0]);

	       // Set<String> completedSet = new HashSet<>(Arrays.asList(completed));

	        DateTimeFormatter fromFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	        DateTimeFormatter toFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

	        String[] completedRaw = Optional.ofNullable(toilet.get("feedback_dates"))
	            .map(Object::toString)
	            .map(d -> d.split(","))
	            .orElse(new String[0]);

	        Set<String> completedSet = Arrays.stream(completedRaw)
	            .map(date -> {
	                try {
	                    return LocalDate.parse(date.trim(), fromFormatter).format(toFormatter);
	                } catch (Exception e) {
	                    return ""; // skip malformed
	                }
	            })
	            .filter(s -> !s.isEmpty())
	            .collect(Collectors.toSet());
	        
	        for (String date : allDates) {
	            Map<String, String> statusMap = new HashMap<>();
	            statusMap.put("Date", date);
	            if (completedSet.contains(date)) {
	                statusMap.put("Status", "Done");
	                completedDates.add(statusMap);
	            } else {
	                statusMap.put("Status", "NotDone");
	                pendingDates.add(statusMap);
	            }
	        }

	        if("completed".equals(type)) {
	        	report.put("Dates", completedDates);
	        }
	        else {
	        	report.put("Dates", pendingDates);
	        }
	        
	        
	        finalResult.add(report);
	    }

	    return finalResult;
	}
	
	@Transactional
	public List<Map<String, Object>> toiletReport2(
			MultiValueMap<String, String> formData,
			String fromDate,
			String toDate,
			String ward,
			String loginid) {
			
		String sqlQuery = "SELECT "
				+ "    `gcc_toilet_list`.`id`,`gcc_toilet_list`.`name`,`gcc_toilet_list`.`zone`,`gcc_toilet_list`.`ward`, "
				+ "	   CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `toilet_feedback`.`image`) AS photo,"
				+ "    COUNT(`toilet_feedback`.`toilet_id`) AS Completed "
				+ "FROM "
				+ "    `gcc_toilet_list` "
				+ "LEFT JOIN "
				+ "    `toilet_feedback` "
				+ "    ON `toilet_feedback`.`toilet_id` = `gcc_toilet_list`.`id` "
				+ "		AND `toilet_feedback`.`isactive` = 1 "
				+ "    AND DATE(`toilet_feedback`.`cdate`) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
				+ "WHERE "
				+ "    `gcc_toilet_list`.`ward` = ? AND "
				+ "    `gcc_toilet_list`.`isactive` = 1 "
				+ "GROUP BY "
				+ "    `gcc_toilet_list`.`id` "
				+ "ORDER BY "
				+ "    `gcc_toilet_list`.`id`";
		
		List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sqlQuery, fromDate, toDate, ward);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Toilet list report.");
        if(result.isEmpty()) {
        	response.put("message", "No data found.");
        }
        response.put("data", result);
		
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> feedbackReport(
	        MultiValueMap<String, String> formData,
	        String filterDate,
	        String toiletid,
	        String loginid) {

	    Map<String, Object> response = new HashMap<>();

	    String sqlQuery = "SELECT "
	            + "    gtl.id AS assetid, "
	            + "    gtl.pct_name AS name, "
	            + "    gtl.zone, "
	            + "    gtl.ward, "
	            + "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', tf.image) AS toiletPhoto, "
	            + "    DATE_FORMAT(tf.cdate, '%d-%m-%Y %I:%i %p') AS feedbackDate, "
	            + "    tf.*, "
	            + "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', tf.image) AS feedbackPhoto "
	            + "FROM gcc_toilet_list gtl "
	            + "RIGHT JOIN toilet_feedback tf ON tf.toilet_id = gtl.id AND tf.isactive = 1 "
	            + "    AND DATE(tf.cdate) = STR_TO_DATE(?, '%d-%m-%Y') "
	            + "WHERE gtl.id = ? AND gtl.isactive = 1 "
	            + "ORDER BY feedbackDate LIMIT 1";

	    List<Map<String, Object>> result = jdbcToiletsTemplate.queryForList(sqlQuery, filterDate, toiletid);
	    List<Map<String, Object>> finalQA = new ArrayList<>();

	    if (!result.isEmpty()) {
	        Map<String, Object> feedback = result.get(0);

	        // 1. Load all questions
	        String questionSql = "SELECT ql.*, "
	                + "CASE WHEN (ql.question_type IN ('select', 'radio')) THEN JSON_ARRAYAGG(JSON_OBJECT( "
	                + "    'option_id', qov.id, "
	                + "    'english_name', qov.name, "
	                + "    'value', qov.id, "
	                + "    'orderby', qov.orderby "
	                + ")) ELSE NULL END AS options "
	                + "FROM question_list ql "
	                + "LEFT JOIN question_option_values qov ON qov.qid = ql.id "
	                + "AND qov.isactive = 1 AND qov.isdelete = 0 "
	                + "WHERE (ql.isactive = 1 OR ql.showonreport = 1) "
	                + "GROUP BY ql.id";
	        List<Map<String, Object>> questions = jdbcToiletsTemplate.queryForList(questionSql);

	        // 2. Build answer map from feedback row
	        Map<String, String> answerMap = new HashMap<>();
	        for (int i = 1; i <= 35; i++) {
	            String key = "q" + i;
	            Object val = feedback.get(key);
	            answerMap.put(String.valueOf(i), val != null ? val.toString() : "");
	        }

	        // 3. Combine Questions + Answers + Optional Images
	        for (Map<String, Object> q : questions) {
	            Map<String, Object> entry = new HashMap<>();
	            String qid = q.get("id") != null ? q.get("id").toString() : "";

	            entry.put("qid", qid);
	            entry.put("question", q.get("q_english"));
	            entry.put("question_type", q.get("question_type"));
	            String answerValue = answerMap.getOrDefault(qid, "");

	            if (q.get("question_type") != null && 
	                (q.get("question_type").toString().equalsIgnoreCase("select") || 
	                 q.get("question_type").toString().equalsIgnoreCase("radio"))) {

	                // Resolve the label from options
	                if (q.get("options") != null && !answerValue.isBlank()) {
	                    try {
	                        List<Map<String, Object>> options = new ObjectMapper().readValue(q.get("options").toString(), List.class);
	                        for (Map<String, Object> opt : options) {
	                            if (opt.get("option_id").toString().equals(answerValue)) {
	                                answerValue = opt.get("english_name").toString();
	                                break;
	                            }
	                        }
	                    } catch (Exception e) {
	                        // fallback: leave answerValue as-is
	                    }
	                }
	            }

	            entry.put("answer", answerValue);
	            entry.put("img_required", q.get("img_required"));

	            // Optional image
	            String imageField = "q" + qid + "_image";
	            if (feedback.containsKey(imageField)) {
	                Object imgPath = feedback.get(imageField);
	                if (imgPath != null && !imgPath.toString().isBlank()) {
	                    entry.put("image", fileBaseUrl + "/gccofficialapp/files" + imgPath.toString());
	                }
	            }

	            finalQA.add(entry);
	        }

	        response.put("status", 200);
	        response.put("message", "Feedback list report.");
	        response.put("data", result);
	        response.put("question", finalQA);
	    } else {
	        response.put("status", 200);
	        response.put("message", "No data found.");
	        response.put("data", Collections.emptyList());
	        response.put("question", Collections.emptyList());
	    }

	    return Collections.singletonList(response);
	}
}
