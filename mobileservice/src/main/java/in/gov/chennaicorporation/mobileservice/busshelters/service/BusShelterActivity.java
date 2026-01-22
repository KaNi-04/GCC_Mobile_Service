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
public class BusShelterActivity {
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
	public BusShelterActivity(Environment environment) {
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
        String serviceFolderName = environment.getProperty("busshelter_foldername");
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
	
	
	//////////////////////////////
    
    public List<Map<String, Object>> getBusShelterList(
    		MultiValueMap<String, String> formData,
			String latitude,
			String longitude,
			String loginid
    		) {
    	
    	String sqlWhere = "";
    	String radius = getConfigValue();
    	
    	if (latitude != null && longitude != null && !latitude.isBlank() && !latitude.isEmpty() && !longitude.isBlank()
		        && !longitude.isEmpty()) {
		    sqlWhere += " AND ((6371008.8 * acos(ROUND(cos(radians(" + latitude
		            + ")) * cos(radians(bsl.latitude)) * cos(radians(bsl.longitude) - radians(" + longitude
		            + ")) + sin(radians(" + latitude + ")) * sin(radians(bsl.latitude)), 9))) < "+radius+")"
		            + " ORDER BY"
					+ "    bsl.`id` DESC";
		}
    	
        String sql = "SELECT  "
        		+ "    bsl.*,  "
        		+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', bsl.image) AS photo, "
        		+ "    bf.type AS qtype, "
        		+ "    DATE_FORMAT(bf.latest_feedback_date, '%d-%m-%Y') AS latestfeedbackdate "
        		+ "FROM  "
        		+ "    bus_shelter_list bsl "
        		+ "LEFT JOIN ( "
        		+ "    SELECT  "
        		+ "        bsf.shelter_id,  "
        		+ "        bsf.type, "
        		+ "        bsf.cdate AS latest_feedback_date "
        		+ "    FROM  "
        		+ "        bus_shelter_feedback bsf "
        		+ "    INNER JOIN ( "
        		+ "        SELECT  "
        		+ "            shelter_id,  "
        		+ "            MAX(id) AS max_id "
        		+ "        FROM  "
        		+ "            bus_shelter_feedback "
        		+ "        GROUP BY shelter_id "
        		+ "    ) latest ON bsf.shelter_id = latest.shelter_id AND bsf.id = latest.max_id "
        		+ ") bf ON bsl.id = bf.shelter_id "
        		+ "WHERE  "
        		+ "    bsl.isactive = 1  "
        		+ "    AND bsl.gcc_app_updated = 0 "+sqlWhere;
        System.out.println(sql);
        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql);
        
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Bus Shelter List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getQuestionsList() {
        String sql = "SELECT * FROM question_list WHERE isactive=1";
        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Bus Shelter Question List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getParentQuestionsList(String qtype) {
        //String sql = "SELECT * FROM question_list WHERE isactive=1 AND `pid`=0 AND `qtype` LIKE ?";
        String sql = "SELECT ql.id AS question_id, ql.q_english, ql.q_tamil, "
        		+ "ql.isactive, ql.img_required, ql.pid, ql.qtype, ql.feedbacktype, "
        		+ "CASE WHEN ql.feedbacktype = 'select' THEN JSON_ARRAYAGG"
        		+ "( JSON_OBJECT( 'option_id', qov.id, 'english_name', qov.english_name, 'tamil_name', "
        		+ "qov.tamil_name, 'value', qov.value, 'isactive', qov.isactive, 'isdelete', qov.isdelete ) ) "
        		+ "ELSE NULL END AS options FROM question_list ql "
        		+ "LEFT JOIN q_option_value qov ON qov.qid = ql.id "
        		+ "AND ql.feedbacktype = 'select' WHERE ql.isactive = 1 AND `pid`=0 AND `qtype` LIKE ? GROUP BY ql.id";
        
        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql,"%" + qtype + "%");
        
        ObjectMapper mapper = new ObjectMapper();
        for (Map<String, Object> row : result) {
            Object optionsRaw = row.get("options");
            if (optionsRaw != null && optionsRaw instanceof String) {
                try {
                    List<Map<String, Object>> optionsParsed = mapper.readValue((String) optionsRaw, List.class);
                    row.put("options", optionsParsed);
                } catch (Exception e) {
                    row.put("options", null); // fallback if malformed
                }
            }
        }
        
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Bus Shelter Parent Question List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getChildQuestionsList(String pid) {
        String sql = "SELECT * FROM question_list WHERE isactive=1 AND `pid`=?";
        List<Map<String, Object>> result = jdbcBusShelterTemplate.queryForList(sql,pid);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Bus Shelter Child Question List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public String inactiveFeedBack(String type, String toilet_id) {
        
		String sql = "UPDATE `bus_shelter_feedback` SET `isactive`=0 WHERE DATE(`bus_shelter_feedback`.`cdate`) = CURDATE() AND `type`= ? AND `shelter_id`=?";
		jdbcBusShelterTemplate.update(sql,type,toilet_id);
		
		return "sussess";
    }

	@Transactional
	public List<Map<String, Object>> saveFeedback(
			String shelter_id,
			String cby,
			String latitude,
			String longitude, 
			String zone, 
			String ward,
			String q_1,
			String q_2,
			String q_3,
			String q_4,
			String q_5,
			String q_6,
			String q_7,
			String q_8,
			String q_9,
			String q_10,
			String q_11,
			String remarks,
			MultipartFile file,
			
			MultipartFile file_1, 
			MultipartFile file_2, 
			MultipartFile file_3, 
			MultipartFile file_4, 
			MultipartFile file_5, 
			MultipartFile file_6, 
			MultipartFile file_7, 
			MultipartFile file_8, 
			MultipartFile file_9, 
			MultipartFile file_10, 
			MultipartFile file_11,
			String type) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;

		String image = "";
		String image_q1 = "";
		String image_q2 = "";
		String image_q3 = "";
		String image_q4 = "";
		String image_q5 = "";
		String image_q6 = "";
		String image_q7 = "";
		String image_q8 = "";
		String image_q9 = "";
		String image_q10 = "";
		String image_q11 = "";
		
		var year = DateTimeUtil.getCurrentYear();
		var month = DateTimeUtil.getCurrentMonth();
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			image = fileUpload("feedback", "0", file);
		}
		
		// Question Images
		if (file_1 != null && !file_1.isEmpty()) {
			image_q1 = fileUpload("1", "q1", file_1);
		}
		
		if (file_2 != null && !file_2.isEmpty()) {
			image_q2 = fileUpload("2", "q2", file_2);
		}
		
		if (file_3 != null && !file_3.isEmpty()) {
			image_q3 = fileUpload("3", "q3", file_3);
		}
		
		if (file_4 != null && !file_4.isEmpty()) {
			image_q4 = fileUpload("4", "q4", file_4);
		}
		
		if (file_5 != null && !file_5.isEmpty()) {
			image_q5 = fileUpload("5", "q5", file_5);
		}
		
		if (file_6 != null && !file_6.isEmpty()) {
			image_q6 = fileUpload("6", "q6", file_6);
		}
		
		if (file_7 != null && !file_7.isEmpty()) {
			image_q7 = fileUpload("7", "q7", file_7);
		}
		
		if (file_8 != null && !file_8.isEmpty()) {
			image_q8 = fileUpload("8", "q8", file_8);
		}
		
		if (file_9 != null && !file_9.isEmpty()) {
			image_q9 = fileUpload("9", "q9", file_9);
		}
		
		if (file_10 != null && !file_10.isEmpty()) {
			image_q10 = fileUpload("10", "q10", file_10);
		}
		
		if (file_11 != null && !file_11.isEmpty()) {
			image_q11 = fileUpload("11", "q11", file_11);
		}
		
		String feedbackimg = image;
		
		String q1_image = image_q1;
		String q2_image = image_q2;
		String q3_image = image_q3;
		String q4_image = image_q4;
		String q5_image = image_q5;
		String q6_image = image_q6;
		String q7_image = image_q7;
		String q8_image = image_q8;
		String q9_image = image_q9;
		String q10_image = image_q10;
		String q11_image = image_q11;
		
		//String type = // Get before & after
		
		// Get today's date
        LocalDate today = LocalDate.now();

        // Format it in the desired format (yyyy-MM-dd)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String todayDate = today.format(formatter);
        
		inactiveFeedBack(type, shelter_id); // Upadte already insert data.
		
		String sqlQuery = "INSERT INTO `bus_shelter_feedback`("
				+ "`shelter_id`, `cby`, "
				+ "`q1`, `q2`, `q3`, `q4`, `q5`, `q6`, `q7`, `q8`, `q9`, `q10`, `q11`, "
				+ "`remarks`,`latitude`, `longitude`,`zone`,`ward`,`type`,`image`,"
				+ "`q1_image`, `q2_image`, `q3_image`, `q4_image`, `q5_image`, `q6_image`, "
				+ "`q7_image`, `q8_image`, `q9_image`, `q10_image`, `q11_image`"
				+ ") "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
			int affectedRows = jdbcBusShelterTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, shelter_id);
					ps.setString(2, cby);
					ps.setString(3, q_1);
					ps.setString(4, q_2);
					ps.setString(5, q_3);
					ps.setString(6, q_4);
					ps.setString(7, q_5);
					ps.setString(8, q_6);
					ps.setString(9, q_7);
					ps.setString(10, q_8);
					ps.setString(11, q_9);
					ps.setString(12, q_10);
					ps.setString(13, q_11);
					ps.setString(14, remarks);
					ps.setString(15, latitude);
					ps.setString(16, longitude);
					ps.setString(17, zone);
					ps.setString(18, ward);
					ps.setString(19, type);
					ps.setString(20, feedbackimg);
					
					ps.setString(21, q1_image);
					ps.setString(22, q2_image);
					ps.setString(23, q3_image);
					ps.setString(24, q4_image);
					ps.setString(25, q5_image);
					ps.setString(26, q6_image);
					ps.setString(27, q7_image);
					ps.setString(28, q8_image);
					ps.setString(29, q9_image);
					ps.setString(30, q10_image);
					ps.setString(31, q11_image);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "A new feedback was inserted successfully!");
				System.out.println("A new feedback was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new feedback. Asset ID:" + shelter_id);
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

		return Collections.singletonList(response);
	}
	
	
}
