package in.gov.chennaicorporation.mobileservice.mosquitosurvey.service;

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
import java.time.LocalDateTime;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class MosquitoService {
	private JdbcTemplate jdbcMosquitoTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
	@Autowired
	public void setDataSource(@Qualifier("mysqlMosquitoDataSource") DataSource mosquitoDataSource) {
		this.jdbcMosquitoTemplate = new JdbcTemplate(mosquitoDataSource);
	}
	
	@Autowired
	public MosquitoService(Environment environment) {
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
        String serviceFolderName = environment.getProperty("mosquito_foldername");
        var year =DateTimeUtil.getCurrentYear();
        var month =DateTimeUtil.getCurrentMonth();
        var date =DateTimeUtil.getCurrentDay();
        
        uploadDirectory = uploadDirectory + serviceFolderName + year +"/"+month+"/"+date;
        
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
	        
	        String filepath_txt = "/"+serviceFolderName + year + "/" + month + "/" + date + "/" + fileName;
	        
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
	
	public List<Map<String, Object>> getCategoryList() {
        String sql = "SELECT * FROM `category` WHERE isactive=1 ORDER BY `orderby`";
        return jdbcMosquitoTemplate.queryForList(sql);
    }
	
	public List<Map<String, Object>> getSubCategoryList(String cid) {
        String sql = "SELECT * FROM `subcategory` WHERE `cid`=? AND isactive=1 ORDER BY `orderby`";
        return jdbcMosquitoTemplate.queryForList(sql, cid);
    }
	
	public List<Map<String, Object>> getTreatmentList() {
        String sql = "SELECT * FROM `treatment` WHERE isactive=1 ORDER BY `orderby`";
        return jdbcMosquitoTemplate.queryForList(sql);
    }
	
	public List<Map<String, Object>> getBreadingSourceCategoryList() {
        String sql = "SELECT * FROM `breadingsource_category` WHERE isactive=1 ORDER BY `orderby`";
        return jdbcMosquitoTemplate.queryForList(sql);
    }
	
	public List<Map<String, Object>> getBreadingSourceList() {
        String sql = "SELECT * FROM `breadingsource` WHERE isactive=1 ORDER BY `orderby`";
        return jdbcMosquitoTemplate.queryForList(sql);
    }

	public List<Map<String, Object>> getChronicDiseases() {
        String sql = "SELECT * FROM `chronic_diseases` WHERE isactive=1 ORDER BY `orderby`";
        return jdbcMosquitoTemplate.queryForList(sql);
    }

	// ************************************************************************************************************************* //
	// For House, Companies & Education Institution
	
	@Transactional
	public List<Map<String, Object>> saveSurveyFlow1(
			String cid,
			String scid,
			String q1,
			String treatment,
			String q2,
			String sourcetype,
			String cby,
			String name,
			String zone,
			String ward,
			String latitude,
			String longitude,
			String visitstatus,
			String noofsource,
			String address,
			String coughcold,
			String ischronic,
			String chronicdisease) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		
		int lastInsertId = 0;
/*
		String image = "";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			var year = DateTimeUtil.getCurrentYear();
			var month = DateTimeUtil.getCurrentMonth();
			image = fileUpload("Created", cby, file);
		}
		
		String storeIMG = image;
*/		
		String sqlQuery = "INSERT INTO `survey_of_house`"
				+ "(`cid`, `scid`, `q1`, `treatment`, `q2`, `sourcetype`, `cby`, "
				+ "`name`, `zone`, `ward`, `latitude`, `longitude`,`visitstatus`,`noofsource`,`address`,`q3`,`q4`,`chronicdisease`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		try {
			int affectedRows = jdbcMosquitoTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, cid);
					ps.setString(2, scid);
					ps.setString(3, q1);
					ps.setString(4, treatment);
					ps.setString(5, q2);
					ps.setString(6, sourcetype);
					ps.setString(7, cby);
					ps.setString(8, name);
					ps.setString(9, zone);
					ps.setString(10, ward);
					ps.setString(11, latitude);
					ps.setString(12, longitude);
					ps.setString(13, visitstatus);
					ps.setString(14, noofsource);
					ps.setString(15, address);
					ps.setString(16, coughcold);
					ps.setString(17, ischronic);
					ps.setString(18, chronicdisease);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				
				String surveyid = lastInsertId+"";
				
				// Split the sourcetype string by comma
		        String[] sourcetypeArray = sourcetype.split(",");
		        
		        // Loop through the array and process each value
		        for (String type : sourcetypeArray) {
		            System.out.println("Sourcetype: " + type);
		            createSurveyPending(surveyid, type, "pending",cby);
		        }
				
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "New House Survey inserted successfully!");
				System.out.println("A new House Survey was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new House Survey.");
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
	
	@Transactional
	public List<Map<String, Object>> createSurveyPending(
			String surveyid,
			String sourceid,
			String status,
			String cby) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		
		int lastInsertId = 0;
	
		String sqlQuery = "INSERT INTO `survey_of_house_data`"
				+ "(`surveyid`, `sourceid`, `status`, `q3`, `treatedfile`, `remarks`, `cby`, "
				+ "`zone`, `ward`, `latitude`, `longitude`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		String q3 = "";
		String treatedfile = "";
		String remarks = "";
		
		String zone = "";
		String ward = "";
		String latitude = "";
		String longitude = "";
		
		try {
			int affectedRows = jdbcMosquitoTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "sid" });
					ps.setString(1, surveyid);
					ps.setString(2, sourceid);
					ps.setString(3, status);
					ps.setString(4, q3);
					ps.setString(5, treatedfile);
					ps.setString(6, remarks);
					ps.setString(7, cby);
					ps.setString(8, zone);
					ps.setString(9, ward);
					ps.setString(10, latitude);
					ps.setString(11, longitude);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "New House Survey inserted successfully!");
				System.out.println("A new House Survey was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new House Survey.");
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
	
	public List<Map<String, Object>> checkHasPending(String cby) {
		
		String pending = "true";
		
		String sql = "SELECT `sid` FROM `survey_of_house_data` WHERE (isactive=1 AND `status`='pending') AND `cby`=?";
		List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sql, cby);
		
		// Check if result is empty or not
	    if (result.isEmpty()) {
	        pending = "false";  // If no rows are found, set pending to "true"
	    }
	    
		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "Pending found");
		response.put("pending", pending);
		
		return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getHousePendingList(String cby) {
	    
	    String sql = "SELECT shd.sid, sh.id, sh.name, "
	    		+ "bs.name AS sourcetype, bs.display_name AS sourcetype_tamil, "
	    		+ "sc.name AS sitetype, sc.display_name AS sitetype_tamil, "
	               + "sh.zone, sh.ward, sh.latitude, sh.longitude, "
	               + "bsc.actiontype AS actionrequired, "
	               + "DATE_FORMAT(sh.`cdate`, '%d-%m-%Y %r') AS createddate "
	               + "FROM survey_of_house_data AS shd "
	               + "JOIN survey_of_house AS sh ON shd.surveyid = sh.id "
	               + "JOIN breadingsource AS bs ON shd.sourceid = bs.sid "
	               + "JOIN breadingsource_category AS bsc ON bs.scid = bsc.scid "
	               + "JOIN subcategory AS sc ON sc.scid = sh.scid "
	               + "WHERE shd.isactive = 1 AND shd.status='pending' AND sh.cby = ?";
	    
	    return jdbcMosquitoTemplate.queryForList(sql, cby);
	}
	
	public List<Map<String, Object>> checkPendingUpdateLocation(String id, String latitude, String longitude) {
    	
		String sql = "SELECT `id` FROM `survey_of_house` WHERE "
				+ "`isactive` = 1 AND `id` = ? "
				+ "AND ((6371008.8 * acos(ROUND(cos(radians(?)) * cos(radians(latitude)) * cos(radians(longitude) - radians(?)) + sin(radians(?)) * sin(radians(latitude)), 9))) < 50) "
	            + "";
	    List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sql, new Object[]{id, latitude, longitude, latitude});

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", !result.isEmpty());

	    return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> updateFlow1PendingStatus(
			String status, String q3, 
			MultipartFile file, String remarks, 
			String cby, String zone, String ward,
			String latitude, String longitude,
			String sid,
			MultipartFile file_before
			) {
	    	
		String treatedfile ="";
		String beforefile ="";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			treatedfile = fileUpload(sid, cby, file);
		}
		
		String finaltreatedfile = treatedfile;
		
		// Handle file upload if a file is provided
		if (file_before != null && !file_before.isEmpty()) {
			beforefile = fileUpload(sid+"_before", cby, file_before);
		}
		String finaltbeforefile = beforefile;
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		String sqlQuery = "UPDATE `survey_of_house_data` SET "
				+ "`status`=?, `q3`=?, `treatedfile`=?, `remarks`=?, `cby`=?, "
				+ "`zone`=?, `ward`=?, `latitude`=?, `longitude`=? ,`updatedate`=? ,`file_before`=?"
				+ "WHERE `sid`=?";
		
		// Execute the update query
	    int rowsAffected = jdbcMosquitoTemplate.update(sqlQuery, new Object[]{
	        status, q3, finaltreatedfile, remarks, cby, zone, ward, latitude, longitude,formattedDate, finaltbeforefile, sid
	    });

	    // Create a response map
	    Map<String, Object> response = new HashMap<>();
	    
	    // If rows are affected, it means the update was successful
	    response.put("status", rowsAffected > 0);

	    // Wrap the response in a list as required
	    return Collections.singletonList(response);
	}
	
	// ************************************************************************************************************************* //
	
	// For Construction Site & Vacant Land
	
	@Transactional
	public List<Map<String, Object>> saveSurveyFlow2(
			String cid,
			String scid,
			String name,
			String q2,
			String q3,
			MultipartFile file,
			String remarks,
			String cby,
			String zone,
			String ward,
			String latitude,
			String longitude,
			String status,
			String address) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		
		int lastInsertId = 0;

		String treatedfile = "";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			treatedfile = fileUpload(cid+"_"+scid, cby, file);
		}
		
		String finaltreatedfile = treatedfile;
		
		String sqlQuery = "INSERT INTO `survey_of_construction_land_data` "
				+ "(`cid`, `scid`, `name`, `q2`, `q3`, `treatedfile`, "
				+ "`remarks`, `cby`, `zone`, `ward`, `latitude`, `longitude`, `status`,`address`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		try {
			int affectedRows = jdbcMosquitoTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, cid);
					ps.setString(2, scid);
					ps.setString(3, name);
					ps.setString(4, q2);
					ps.setString(5, q3);
					ps.setString(6, finaltreatedfile);
					ps.setString(7, remarks);
					ps.setString(8, cby);
					ps.setString(9, zone);
					ps.setString(10, ward);
					ps.setString(11, latitude);
					ps.setString(12, longitude);
					ps.setString(13, status);
					ps.setString(14, address);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				
				String surveyid = lastInsertId+"";
				
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "New Survey inserted successfully!");
				System.out.println("A new Survey was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new House Survey.");
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
	
	
	// For Reports
	
	@Transactional
	public List<Map<String, Object>> breedingZoneReports(String cby, String fromDate, String toDate){
		
		DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		DateTimeFormatter dbFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		String fromDateDB = LocalDate.parse(fromDate, inputFmt).format(dbFmt);
		String toDateDB   = LocalDate.parse(toDate, inputFmt).format(dbFmt);
		
		Map<String, Object> response = new HashMap<>();
		
		String sqlQuery = "SELECT "
				+ "    zone, "
				+ "    COUNT(*) AS Inspected, "
				+ "    SUM( LENGTH(sourcetype) - LENGTH(REPLACE(sourcetype, ',', '')) + 1 ) AS NoOfBreedingSourceIdentified "
				+ "FROM survey_of_house "
				+ "WHERE isactive = 1 AND zone BETWEEN '01' AND '15' "
				+ "AND DATE(`cdate`) BETWEEN ? AND ? "
				+ "GROUP BY zone "
				+ "ORDER BY zone";
		
		List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sqlQuery, fromDateDB, toDateDB);
		response.put("status", 200);
        response.put("message", "Zone report.");
        if(result.isEmpty()) {
        	response.put("message", "No data found.");
        }
        response.put("data", result);
		return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> breedingWardReports(String cby, String fromDate, String toDate, String zone){
		
		DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		DateTimeFormatter dbFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		String fromDateDB = LocalDate.parse(fromDate, inputFmt).format(dbFmt);
		String toDateDB   = LocalDate.parse(toDate, inputFmt).format(dbFmt);
		
		Map<String, Object> response = new HashMap<>();
		
		String sqlQuery = "SELECT "
				+ "    ward, "
				+ "    COUNT(*) AS Inspected, "
				+ "    SUM( LENGTH(sourcetype) - LENGTH(REPLACE(sourcetype, ',', '')) + 1 ) AS NoOfBreedingSourceIdentified "
				+ "FROM survey_of_house "
				+ "WHERE isactive = 1 "
				+ "AND DATE(`cdate`) BETWEEN ? AND ? "
				+ "AND `zone` = ? "
				+ "GROUP BY ward "
				+ "ORDER BY ward";
		
		List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sqlQuery, fromDateDB, toDateDB, zone);
		response.put("status", 200);
        response.put("message", "Ward report.");
        if(result.isEmpty()) {
        	response.put("message", "No data found.");
        }
        response.put("data", result);
		return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> breedinglistReports(String cby, String fromDate, String toDate, String ward){
		
		Map<String, Object> response = new HashMap<>();
		DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		DateTimeFormatter dbFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		String fromDateDB = LocalDate.parse(fromDate, inputFmt).format(dbFmt);
		String toDateDB   = LocalDate.parse(toDate, inputFmt).format(dbFmt);
		
		String sqlQuery = "SELECT "
				+ "    sh.*, "
				+ "    SUM( LENGTH(sh.sourcetype) - LENGTH(REPLACE(sh.sourcetype, ',', '')) + 1 ) AS NoOfBreedingSourceIdentified, "
				+ "CASE "
				+ "     WHEN COALESCE(SUM(CASE WHEN shd.status <> 'close' THEN 1 ELSE 0 END),0) = 0  "
				+ "            THEN 'Completed'"
				+ "        ELSE 'Pending'"
				+ "    END AS final_status "
				+ "FROM survey_of_house sh "
				+ "LEFT JOIN survey_of_house_data shd ON sh.id = shd.surveyid "
				+ "WHERE sh.isactive = 1 "
				+ "AND DATE(sh.`cdate`) BETWEEN ? AND ? "
				+ "AND sh.`ward` = ? "
				+ "GROUP BY sh.id "
				+ "ORDER BY id";
		
		List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sqlQuery, fromDateDB, toDateDB, ward);
		response.put("status", 200);
        response.put("message", "Ward based list.");
        if(result.isEmpty()) {
        	response.put("message", "No data found.");
        }
        response.put("data", result);
		return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> getBreedingDetails(String surveyId) {
		Map<String, Object> response = new HashMap<>();
	    String sql = "SELECT "
	            + "shd.sid, sh.id, sh.name, "
	            + "bs.name AS sourcetype, bs.display_name AS sourcetype_tamil, "
	            + "sc.name AS sitetype, sc.display_name AS sitetype_tamil, "
	            + "sh.zone, sh.ward, sh.latitude, sh.longitude, sh.address,"
	            + "bsc.actiontype AS actionrequired, "
	            + "CONCAT(?, '/gccofficialapp/files', shd.file_before) AS file_before, "
	            + "CONCAT(?, '/gccofficialapp/files', shd.treatedfile) AS treatedfile, "
	            + "shd.status, shd.remarks, "
	            + "DATE_FORMAT(sh.cdate, '%d-%m-%Y %r') AS createddate "
	            + "FROM survey_of_house_data AS shd "
	            + "JOIN survey_of_house AS sh ON shd.surveyid = sh.id "
	            + "JOIN breadingsource AS bs ON shd.sourceid = bs.sid "
	            + "JOIN breadingsource_category AS bsc ON bs.scid = bsc.scid "
	            + "JOIN subcategory AS sc ON sc.scid = sh.scid "
	            + "WHERE shd.isactive = 1 "
	            + "AND sh.id = ? "
	            + "ORDER BY shd.sid";

	    List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sql, fileBaseUrl, fileBaseUrl, surveyId);
		response.put("status", 200);
        response.put("message", "Ward based list.");
        if(result.isEmpty()) {
        	response.put("message", "No data found.");
        }
        response.put("data", result);
		return Collections.singletonList(response);
	}
	
	// USER Based Report
	
	@Transactional
	public List<Map<String, Object>> userReports(String cby, String fromDate, String toDate, String ward){
		
		Map<String, Object> response = new HashMap<>();
		DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		DateTimeFormatter dbFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		String fromDateDB = LocalDate.parse(fromDate, inputFmt).format(dbFmt);
		String toDateDB   = LocalDate.parse(toDate, inputFmt).format(dbFmt);
		
		String sqlQuery = "SELECT "
				+ "	   sh.cby,"
				+ "    eu.FIRST_NAME,"
				+ "    eu.USER_NAME,"
				+ "    eu.EXTRAFIELD1,"
				+ "    eu.EXTRAFIELD2,"
				+ "    eu.EXTRAFIELD3,"
				+ "    eu.EXTRAFIELD4,"
				+ "    COUNT(*) AS Inspected,"
				+ "    SUM( LENGTH(sh.sourcetype) - LENGTH(REPLACE(sh.sourcetype, ',', '')) + 1 ) AS NoOfBreedingSourceIdentified "
				+ " FROM survey_of_house sh "
				+ " LEFT JOIN erp_pgr.EG_USER eu ON sh.cby = eu.id "
				+ " WHERE sh.isactive = 1 "
				+ " AND DATE(sh.cdate) BETWEEN ? AND ? "
				+ " AND sh.ward = ? "
				+ " GROUP BY sh.cby, eu.FIRST_NAME, eu.USER_NAME, eu.EXTRAFIELD1, eu.EXTRAFIELD2, eu.EXTRAFIELD3, eu.EXTRAFIELD4 "
				+ " ORDER BY sh.cby";
		
		List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sqlQuery, fromDateDB, toDateDB, ward);
		response.put("status", 200);
        response.put("message", "Ward based list.");
        if(result.isEmpty()) {
        	response.put("message", "No data found.");
        }
        response.put("data", result);
		return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> userDayReports(String cby, String fromDate, String toDate, String ward){
		
		Map<String, Object> response = new HashMap<>();
		DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		DateTimeFormatter dbFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		String fromDateDB = LocalDate.parse(fromDate, inputFmt).format(dbFmt);
		String toDateDB   = LocalDate.parse(toDate, inputFmt).format(dbFmt);
		
		String sqlQuery = "SELECT"
				+ "	DATE_FORMAT(sh.cdate, '%d-%m-%Y') as inspectedDate,"
				+ "	   sh.cby,"
				+ "    eu.FIRST_NAME,"
				+ "    eu.USER_NAME,"
				+ "    eu.EXTRAFIELD1,"
				+ "    eu.EXTRAFIELD2,"
				+ "    eu.EXTRAFIELD3,"
				+ "    eu.EXTRAFIELD4,"
				+ "    COUNT(*) AS Inspected,"
				+ "    SUM( LENGTH(sh.sourcetype) - LENGTH(REPLACE(sh.sourcetype, ',', '')) + 1 ) AS NoOfBreedingSourceIdentified"
				+ " FROM survey_of_house sh"
				+ " LEFT JOIN erp_pgr.EG_USER eu ON sh.cby = eu.id"
				+ " WHERE sh.isactive = 1"
				+ " AND DATE(sh.cdate) BETWEEN ? AND ?"
				+ " AND sh.ward = ?"
				+ " AND sh.cby = ?"
				+ " GROUP BY DATE_FORMAT(sh.cdate, '%d-%m-%Y'), sh.cby, eu.FIRST_NAME, eu.USER_NAME, eu.EXTRAFIELD1, eu.EXTRAFIELD2, eu.EXTRAFIELD3, eu.EXTRAFIELD4"
				+ " ORDER BY sh.cby;";
		
		List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sqlQuery, fromDateDB, toDateDB, ward, cby);
		response.put("status", 200);
        response.put("message", "Ward based list.");
        if(result.isEmpty()) {
        	response.put("message", "No data found.");
        }
        response.put("data", result);
		return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> userBreedinglistReports(String cby, String selectedDate, String ward){
		
		Map<String, Object> response = new HashMap<>();
		DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		DateTimeFormatter dbFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		String DateDB = LocalDate.parse(selectedDate, inputFmt).format(dbFmt);
		//String toDateDB   = LocalDate.parse(toDate, inputFmt).format(dbFmt);
		
		String sqlQuery = "SELECT "
				+ "    sh.*, "
				+ "    SUM( LENGTH(sh.sourcetype) - LENGTH(REPLACE(sh.sourcetype, ',', '')) + 1 ) AS NoOfBreedingSourceIdentified, "
				+ "CASE "
				+ "     WHEN COALESCE(SUM(CASE WHEN shd.status <> 'close' THEN 1 ELSE 0 END),0) = 0  "
				+ "            THEN 'Completed'"
				+ "        ELSE 'Pending'"
				+ "    END AS final_status "
				+ "FROM survey_of_house sh "
				+ "LEFT JOIN survey_of_house_data shd ON sh.id = shd.surveyid "
				+ "WHERE sh.isactive = 1 "
				+ "AND DATE(sh.`cdate`) = ? "
				+ "AND sh.`ward` = ? "
				+ "AND sh.`cby` =  ? "
				+ "GROUP BY sh.id "
				+ "ORDER BY id";
		
		List<Map<String, Object>> result = jdbcMosquitoTemplate.queryForList(sqlQuery, DateDB, ward, cby);
		response.put("status", 200);
        response.put("message", "User based list.");
        if(result.isEmpty()) {
        	response.put("message", "No data found.");
        }
        response.put("data", result);
		return Collections.singletonList(response);
	}
}
