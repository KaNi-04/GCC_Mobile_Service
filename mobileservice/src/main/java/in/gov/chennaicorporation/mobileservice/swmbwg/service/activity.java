package in.gov.chennaicorporation.mobileservice.swmbwg.service;

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

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service("gccofficialappswmbwgactivity")
public class activity {
	private JdbcTemplate jdbcBWGTemplate;
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final int STRING_LENGTH = 15;
	private static final Random RANDOM = new SecureRandom();

	@Autowired
	public void setDataSource(@Qualifier("mysqlActivityDataSource") DataSource bwgDataSource) {
		this.jdbcBWGTemplate = new JdbcTemplate(bwgDataSource);
	}
	
	@Autowired
	public activity(Environment environment) {
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
		String serviceFolderName = environment.getProperty("swm_bwg_foldername");
		var year = DateTimeUtil.getCurrentYear();
		var month = DateTimeUtil.getCurrentMonth();

		uploadDirectory = uploadDirectory + serviceFolderName + year + "/" + month;

		try {
			// Create directory if it doesn't exist
			Path directoryPath = Paths.get(uploadDirectory);
			if (!Files.exists(directoryPath)) {
				Files.createDirectories(directoryPath);
			}

			// Datetime string
			String datetimetxt = DateTimeUtil.getCurrentDateTime();
			// File name
			String fileName = name + "_" + id + "_" + datetimetxt + "_" + file.getOriginalFilename();
			fileName = fileName.replaceAll("\\s+", ""); // Remove space on filename

			String filePath = uploadDirectory + "/" + fileName;

			String filepath_txt = "/" + serviceFolderName + year + "/" + month + "/" + fileName;

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
	
	public List<Map<String, Object>> getAllClass(){
		
		String sqlQuery = "SELECT `cid`, `name` "
				+ "FROM `swm_bwg_class` "
				+ "WHERE `isactive`=1 AND `isdelete`=0";
		
		List<Map<String, Object>> result = jdbcBWGTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "SWM - BWG Class List");
        response.put("data", result);
	    
        return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getBWGTypes(){
		
		String sqlQuery = "SELECT `tid`, `name` "
				+ "FROM `swm_bwg_types` "
				+ "WHERE `isactive`=1 AND `isdelete`=0";
		
		List<Map<String, Object>> result = jdbcBWGTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "SWM - BWG Type List");
        response.put("data", result);
	    
        return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getServiceProviderList(){
		
		String sqlQuery = "SELECT `splid`, `name` "
				+ "FROM `swm_bwg_service_provider_list` "
				+ "WHERE `isactive`=1 AND `isdelete`=0";
		
		List<Map<String, Object>> result = jdbcBWGTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "SWM - BWG Service Provider List");
        response.put("data", result);
	    
        return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveRequest(String latitude,
			String longitude, String zone, String ward, String streetid, String streetname, String location,
			String classtxt, String type, String qtyofwaste, String onsiteprocessing, 
			String serviceprovider, String serviceproviderid, String cby, MultipartFile file, MultipartFile file2) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;

		String image = "";
		String image2 = "";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			var year = DateTimeUtil.getCurrentYear();
			var month = DateTimeUtil.getCurrentMonth();
			image = fileUpload(year, month, file);
		}
		
		// Handle file upload if a file is provided
		if (file2 != null && !file2.isEmpty()) {
			var year = DateTimeUtil.getCurrentYear();
			var month = DateTimeUtil.getCurrentMonth();
			image2 = fileUpload(year, month, file2);
		}

		String processingimg = image;
		String serviceproviderimg = image2;
		
		String sqlQuery = "INSERT INTO `swm_bwg_data`"
				+ "(`latitude`, `longitude`, `zone`, `ward`, `streetid`, "
				+ "`streetname`, `location`, `class`, `type`, `qtyofwaste`, `onsiteprocessing`, "
				+ "`processingimg`, `serviceprovider`, `serviceproviderid`, `cby`,`serviceproviderimg`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
			int affectedRows = jdbcBWGTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, latitude);
					ps.setString(2, longitude);
					ps.setString(3, zone);
					ps.setString(4, ward);
					ps.setString(5, streetid);
					ps.setString(6, streetname);
					ps.setString(7, location);
					ps.setString(8, classtxt);
					ps.setString(9, type);
					ps.setString(10, qtyofwaste);
					ps.setString(11, onsiteprocessing);
					ps.setString(12, processingimg);
					ps.setString(13, serviceprovider);
					ps.setString(14, serviceproviderid);
					ps.setString(15, cby);
					ps.setString(16, serviceproviderimg);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "A new asset was inserted successfully!");
				System.out.println("A new asset was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new asset.");
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
	
	public List<Map<String, Object>> getServiceProviderPendingList(String cby){
		
		String sqlQuery = "SELECT sbd.*,"
				+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', sbd.processingimg) AS photos, "
				+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', sbd.serviceproviderimg) AS providerphotos, "
				+ "sbc.name as classname, "
				+ "sbt.name as typename, "
				+ "spl.name as serviceprovidername "
				+ "FROM "
				+ "`swm_bwg_data` sbd, "
				+ "`swm_bwg_class` sbc, "
				+ "`swm_bwg_types` sbt, "
				+ "`swm_bwg_service_provider_list` spl "
				+ "WHERE (sbd.class=sbc.cid AND sbd.type=sbt.tid AND sbd.serviceproviderid=spl.splid) "
				+ "AND(`serviceprovider`='Engaged' AND `cby`= ?) AND `serviceproviderimg`=''";
		
		List<Map<String, Object>> result = jdbcBWGTemplate.queryForList(sqlQuery,cby);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "SWM - BWG Service Provider image upload pending list");
        response.put("data", result);
	    
        return Collections.singletonList(response);
	}
	
	
	public List<Map<String, Object>> updateServiceProviderImg(String did, MultipartFile file2) {
		String image2="";
		// Handle file upload if a file is provided
		if (file2 != null && !file2.isEmpty()) {
			var year = DateTimeUtil.getCurrentYear();
			var month = DateTimeUtil.getCurrentMonth();
			image2 = fileUpload(year, month, file2);
		}
				
	    String sqlQuery = "UPDATE `swm_bwg_data` SET `serviceproviderimg` = ? WHERE `did` = ? ";
	    jdbcBWGTemplate.update(sqlQuery, image2, did);
	    
	    String sqlQueryList = "SELECT sbd.*,"
				+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', sbd.processingimg) AS photos, "
				+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', sbd.serviceproviderimg) AS providerphotos, "
				+ "sbc.name as classname, "
				+ "sbt.name as typename, "
				+ "spl.name as serviceprovidername "
				+ "FROM "
				+ "`swm_bwg_data` sbd, "
				+ "`swm_bwg_class` sbc, "
				+ "`swm_bwg_types` sbt, "
				+ "`swm_bwg_service_provider_list` spl "
				+ "WHERE (sbd.class=sbc.cid AND sbd.type=sbt.tid AND sbd.serviceproviderid=spl.splid) "
				+ "AND(`serviceprovider`='Engaged' AND `did`= ?)";
	    
	    List<Map<String, Object>> result = jdbcBWGTemplate.queryForList(sqlQueryList,did);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "SWM - BWG Service Provider image uploaded successfuly");
        response.put("data", result);
        return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getZoneWiseCount(){
		
		String sqlQuery = "SELECT sbd.zone, count(sbd.did) totalcount FROM `swm_bwg_data` sbd "
				+ "WHERE isactive=1 and isdelete=0 GROUP BY sbd.zone ORDER BY sbd.zone";
		
		List<Map<String, Object>> result = jdbcBWGTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Zone wise count List");
        response.put("data", result);
	    
        return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getWardWiseCount(String zone){
		
		String sqlQuery = "SELECT sbd.ward, count(sbd.did) totalcount "
				+ "FROM "
				+ "`swm_bwg_data` sbd "
				+ "WHERE (isactive=1 and isdelete=0) and zone=? GROUP BY sbd.ward ORDER BY sbd.ward";
		
		List<Map<String, Object>> result = jdbcBWGTemplate.queryForList(sqlQuery,zone);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Ward wise count List");
        response.put("zone", zone);
        response.put("data", result);
	    
        return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getReport(String ward){
		
		String sqlQuery = "SELECT sbd.*,"
				+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', sbd.processingimg) AS photos, "
				+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', sbd.serviceproviderimg) AS providerphotos, "
				+ "sbc.name as classname, "
				+ "sbt.name as typename, "
				+ "spl.name as serviceprovidername "
				+ "FROM "
				+ "`swm_bwg_data` sbd, "
				+ "`swm_bwg_class` sbc, "
				+ "`swm_bwg_types` sbt, "
				+ "`swm_bwg_service_provider_list` spl "
				+ "WHERE (sbd.class=sbc.cid AND sbd.type=sbt.tid AND sbd.serviceproviderid=spl.splid) AND ward=?";
		
		List<Map<String, Object>> result = jdbcBWGTemplate.queryForList(sqlQuery,ward);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "SWM - BWG List");
        response.put("data", result);
	    
        return Collections.singletonList(response);
	}
}
