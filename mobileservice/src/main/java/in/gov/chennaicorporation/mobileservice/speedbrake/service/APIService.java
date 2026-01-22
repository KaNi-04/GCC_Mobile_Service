package in.gov.chennaicorporation.mobileservice.speedbrake.service;

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

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class APIService {

	private JdbcTemplate jdbcSpeedBrake;
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final int STRING_LENGTH = 15;
	private static final Random RANDOM = new SecureRandom();

	@Autowired
	public void setDataSource(@Qualifier("mysqlGccSpeedBrakeSource") DataSource speedBrakeDataSource) {
		this.jdbcSpeedBrake = new JdbcTemplate(speedBrakeDataSource);
	}
	
	@Autowired
	public APIService(Environment environment) {
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
		String serviceFolderName = environment.getProperty("speedbrake_foldername");
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
	
	public List<Map<String, Object>> getQuestions(){
        String sql = "SELECT * FROM `Questions` WHERE `isactive`=1 ORDER BY `orderby`";
        List<Map<String, Object>> result = jdbcSpeedBrake.queryForList(sql);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Question List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> checkAssetExists(String latitudeStr, String longitudeStr) {
		double latitude = Double.parseDouble(latitudeStr);
	    double longitude = Double.parseDouble(longitudeStr);
	    
	    String checkSql = "SELECT COUNT(*) FROM speedbrake_list sbl " +
	            "WHERE (6371008.8 * ACOS(COS(RADIANS(?)) * COS(RADIANS(sbl.latitude)) * " +
	            "COS(RADIANS(sbl.longitude) - RADIANS(?)) + SIN(RADIANS(?)) * SIN(RADIANS(sbl.latitude)))) < 2";
	    //Integer count = jdbcSpeedBrake.queryForObject(checkSql,new Object[]{latitude, longitude, latitude}, Integer.class);
	    
	    Integer count = jdbcSpeedBrake.queryForObject(
	    	    checkSql,
	    	    Integer.class,
	    	    latitude, longitude, latitude
	    	);
	    
	    Map<String, Object> response = new HashMap<>();
	    if (count != null && count > 0) {
	    	response.put("count", count);
	        response.put("status", "error");
	        response.put("message", "Duplicate: Speed breaker already exists.");
	    } else {
	    	response.put("count", count);
	        response.put("status", "success");
	        response.put("message", "No existing speed breaker found.");
	    }
	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveSpeedBrakeDetails(
	        String zone,
	        String ward,
	        String streetid,
	        String streetname,
	        String streettype,
	        String roadtype,
	        String marktype,
	        String q1,
	        String q2,
	        String q3,
	        String cby,
	        String latitude,
	        String longitude,
	        String sb_condition,
	        MultipartFile mainfile
	) {
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    String roadimg = fileUpload("Construction", "main", mainfile);
	    
	    String roadcategorytxt = "GCC";
	    
	    if("HY".equalsIgnoreCase(roadtype)) {
	    	roadcategorytxt = "Highways";
	    }
	    
	    String roadcategory = roadcategorytxt;
	    
	    // Insert query
	    String insertSql = "INSERT INTO `speedbrake_list`(`zone`, `ward`, `streetid`, `streetname`, `streettype`, `roadcategory`, "
	    		+ "`roadtype`, `marktype`, `file`, "
	    		+ "`q1`, `q2`, `q3`, `cby`,`latitude`, `longitude`,`sb_condition`) VALUES "
	    		+ "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcSpeedBrake.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"cdid"});
	        int i = 1;
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, streetid);
	        ps.setString(i++, streetname);
	        ps.setString(i++, streettype);
	        ps.setString(i++, roadcategory);
	        ps.setString(i++, roadtype);
	        ps.setString(i++, marktype);
	        ps.setString(i++, roadimg);
	        ps.setString(i++, q1);
	        ps.setString(i++, q2);
	        ps.setString(i++, q3);
	        ps.setString(i++, cby);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, sb_condition);
	        
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        
	        	response.put("insertId", lastInsertId);
		        response.put("status", "success");
		        response.put("message", "Speed Brake details inserted successfully.");
	       
	        
	    } else {
	        response.put("status", "error");
	        response.put("message", "Speed Brake details insert failed.");
	    }

	    result.add(response);
	    return result;
	}
	
	public List<Map<String, Object>> getZoneWiseReport() {
		String sql = "SELECT  "
				+ "    zone, "
				+ "    COUNT(*) AS total_mapping, "
				+ "    SUM(CASE WHEN marktype = 'New' THEN 1 ELSE 0 END) AS new_count, "
				+ "    SUM(CASE WHEN marktype = 'Existing' THEN 1 ELSE 0 END) AS existing_count "
				+ "FROM  "
				+ "    speedbrake_list "
				+ "WHERE  "
				+ "    isactive = 1 AND isdelete = 0 "
				+ "GROUP BY  "
				+ "    zone "
				+ " "
				+ "UNION ALL "
				+ " "
				+ "SELECT  "
				+ "    'Total' AS zone, "
				+ "    COUNT(*) AS total_mapping, "
				+ "    SUM(CASE WHEN marktype = 'New' THEN 1 ELSE 0 END), "
				+ "    SUM(CASE WHEN marktype = 'Existing' THEN 1 ELSE 0 END) "
				+ "FROM  "
				+ "    speedbrake_list "
				+ "WHERE  "
				+ "    isactive = 1 AND isdelete = 0 "
				+ " "
				+ "ORDER BY "
				+ "    CASE  "
				+ "        WHEN zone = 'Total' THEN 1  "
				+ "        ELSE 0  "
				+ "    END, "
				+ "    zone";
		
		return jdbcSpeedBrake.queryForList(sql);
	}
	
	public List<Map<String, Object>> getWardeWiseReport(String zone) {
		String sql = "SELECT  "
				+ "    ward, "
				+ "    COUNT(*) AS total_mapping, "
				+ "    SUM(CASE WHEN marktype = 'New' THEN 1 ELSE 0 END) AS new_count, "
				+ "    SUM(CASE WHEN marktype = 'Existing' THEN 1 ELSE 0 END) AS existing_count "
				+ "FROM  "
				+ "    speedbrake_list "
				+ "WHERE  "
				+ "    isactive = 1 AND isdelete = 0 AND zone = ? "
				+ "GROUP BY  "
				+ "    ward "
				+ " "
				+ "UNION ALL "
				+ " "
				+ "SELECT  "
				+ "    'Total' AS ward, "
				+ "    COUNT(*) AS total_mapping, "
				+ "    SUM(CASE WHEN marktype = 'New' THEN 1 ELSE 0 END), "
				+ "    SUM(CASE WHEN marktype = 'Existing' THEN 1 ELSE 0 END) "
				+ "FROM  "
				+ "    speedbrake_list "
				+ "WHERE  "
				+ "    isactive = 1 AND isdelete = 0 AND zone = ? "
				+ "ORDER BY "
				+ "    CASE  "
				+ "        WHEN ward = 'Total' THEN 1  "
				+ "        ELSE 0  "
				+ "    END, "
				+ "    ward";
		
		return jdbcSpeedBrake.queryForList(sql,zone,zone);
	}
	
	public List<Map<String, Object>> getNewStreetWiseReport(String ward) {
		String sql = "SELECT  "
				+ "    streetname, "
				+ "    COUNT(*) AS count "
				+ "FROM  "
				+ "    speedbrake_list "
				+ "WHERE  "
				+ "    isactive = 1  "
				+ "    AND isdelete = 0  "
				+ "    AND marktype = 'New' "
				+ "    AND ward = ? "
				+ "GROUP BY  "
				+ "    streetname "
				+ " "
				+ "UNION ALL "
				+ " "
				+ "SELECT  "
				+ "    'Total' AS streetname, "
				+ "    COUNT(*) AS count "
				+ "FROM  "
				+ "    speedbrake_list "
				+ "WHERE  "
				+ "    isactive = 1  "
				+ "    AND isdelete = 0  "
				+ "    AND marktype = 'New' "
				+ "    AND ward = ?";
		
		return jdbcSpeedBrake.queryForList(sql,ward,ward);
	}
	
	public List<Map<String, Object>> getExistingStreetWiseReport(String ward) {
		String sql = "SELECT  "
				+ "    streetname, "
				+ "    SUM(CASE WHEN sb_condition = 'Good' THEN 1 ELSE 0 END) AS good_count, "
				+ "    SUM(CASE WHEN sb_condition = 'Complete reconstruction' THEN 1 ELSE 0 END) AS complete_reconstruction_count, "
				+ "    SUM(CASE WHEN sb_condition = 'Improvement to the existing' THEN 1 ELSE 0 END) AS improvement_count "
				+ "FROM  "
				+ "    speedbrake_list "
				+ "WHERE  "
				+ "    isactive = 1  "
				+ "    AND isdelete = 0  "
				+ "    AND marktype = 'Existing' "
				+ "    AND ward = ? "
				+ "GROUP BY  "
				+ "    streetname "
				+ " "
				+ "UNION ALL "
				+ " "
				+ "SELECT  "
				+ "    'Total' AS streetname, "
				+ "    SUM(CASE WHEN sb_condition = 'Good' THEN 1 ELSE 0 END), "
				+ "    SUM(CASE WHEN sb_condition = 'Complete reconstruction' THEN 1 ELSE 0 END), "
				+ "    SUM(CASE WHEN sb_condition = 'Improvement to the existing' THEN 1 ELSE 0 END) "
				+ "FROM  "
				+ "    speedbrake_list "
				+ "WHERE  "
				+ "    isactive = 1  "
				+ "    AND isdelete = 0  "
				+ "    AND marktype = 'Existing' "
				+ "    AND ward = ?";
		
		return jdbcSpeedBrake.queryForList(sql,ward,ward);
	}
	
	public List<Map<String, Object>> getSpeedbrakeDetailsWithQuestions(String ward, String marktype, String sb_condition, String streetname) {
	    String where = "";
	    List<Object> params = new ArrayList<>();
	    params.add(marktype);
	    params.add(ward);
	    params.add(streetname);
	    
	    if (sb_condition != null) {
	        where = " AND sb_condition = ?";
	        params.add(sb_condition);
	    }

	    // 1. Fetch questions list
	    String qSql = "SELECT orderby, english FROM Questions ORDER BY orderby";
	    List<Map<String, Object>> questions = jdbcSpeedBrake.queryForList(qSql);

	    // 2. Fetch speedbrake entries
	    String sql = "SELECT id, zone, ward, streetid, streetname, streettype, "
	               + "roadcategory, roadtype, marktype, q1, q2, q3, q4, cby, cddate, "
	               + "latitude, longitude, sb_condition, "
	               + "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', file) AS complaint_image_url "
	               + "FROM speedbrake_list "
	               + "WHERE isactive = 1 AND isdelete = 0 AND marktype = ? AND ward = ? AND streetname = ? " + where;

	    List<Map<String, Object>> entries = jdbcSpeedBrake.queryForList(sql, params.toArray());

	    // 3. Merge Q&A into each entry
	    for (Map<String, Object> row : entries) {
	        List<Map<String, String>> qnaList = new ArrayList<>();

	        for (int i = 1; i <= 4; i++) {
	            Object qVal = row.get("q" + i);
	            if (qVal != null && i <= questions.size()) {
	                String questionText = String.valueOf(questions.get(i - 1).get("english"));
	                Map<String, String> qna = new HashMap<>();
	                qna.put("q", questionText);
	                qna.put("ans", String.valueOf(qVal));
	                qnaList.add(qna);
	            }
	        }

	        row.put("questions", qnaList);

	        // Remove q1–q4 from main record
	        row.remove("q1");
	        row.remove("q2");
	        row.remove("q3");
	        row.remove("q4");
	    }
	    return entries;
	}
}
