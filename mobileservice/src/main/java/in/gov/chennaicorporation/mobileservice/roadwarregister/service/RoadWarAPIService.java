package in.gov.chennaicorporation.mobileservice.roadwarregister.service;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class RoadWarAPIService {
	private JdbcTemplate jdbcRoadWar;
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final int STRING_LENGTH = 15;
	private static final Random RANDOM = new SecureRandom();

	@Autowired
	public void setDataSource(@Qualifier("mysqlGccRoadWarSource") DataSource RoadWarDataSource) {
		this.jdbcRoadWar = new JdbcTemplate(RoadWarDataSource);
	}
	
	@Autowired
	public RoadWarAPIService(Environment environment) {
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
		String serviceFolderName = environment.getProperty("roadwar_foldername");
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
	
	public List<Map<String, Object>> getRoadTypes(){
        String sql = "SELECT `roadtype_id`, `name` FROM `road_type` WHERE `isactive`=1 AND `isdelete`=0 ORDER BY `orderby`";
        List<Map<String, Object>> result = jdbcRoadWar.queryForList(sql);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Road Type List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getRoadLayTypes(){
        String sql = "SELECT `lay_type_id`, `name` FROM `road_lay_type` WHERE `isactive`=1 AND `isdelete`=0 ORDER BY `orderby`";
        List<Map<String, Object>> result = jdbcRoadWar.queryForList(sql);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Road Lay Type List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> checkAssetExists(String latitudeStr, String longitudeStr) {
		double latitude = Double.parseDouble(latitudeStr);
	    double longitude = Double.parseDouble(longitudeStr);
	    
	    String checkSql = "SELECT COUNT(*) FROM road_list list " +
	            "WHERE (6371008.8 * ACOS(COS(RADIANS(?)) * COS(RADIANS(list.latitude)) * " +
	            "COS(RADIANS(list.longitude) - RADIANS(?)) + SIN(RADIANS(?)) * SIN(RADIANS(list.latitude)))) < 50";
	    
	    Integer count = jdbcRoadWar.queryForObject(
	    	    checkSql,
	    	    Integer.class,
	    	    latitude, longitude, latitude
	    	);
	    
	    Map<String, Object> response = new HashMap<>();
	    if (count != null && count > 0) {
	    	response.put("count", count);
	        response.put("status", "error");
	        response.put("message", "Duplicate: Road already exists.");
	    } else {
	    	response.put("count", count);
	        response.put("status", "success");
	        response.put("message", "No existing Road found.");
	    }
	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getStartRoadList(String inby){
        /* 
         	String sql = "SELECT *, "
        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', start_img) AS start_img_url "
        		+ " FROM `start_street_details` WHERE (`isactive`=1 AND `isdelete`=0) AND (`inby`=?) "
        		+ "AND s.strat_id NOT IN ( SELECT e.start_id FROM end_street_details e )";
        */
        String sql ="SELECT "
        		+ "    s.*, "
        		+ "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', start_img) AS start_img_url "
        		+ "FROM "
        		+ "    `start_street_details` s "
        		+ "WHERE "
        		+ "    s.`isactive` = 1"
        		+ "    AND s.`isdelete` = 0 AND (`inby`=?)"
        		+ "    AND s.`strat_id` NOT IN ("
        		+ "        SELECT e.`start_id` "
        		+ "        FROM `end_street_details` e"
        		+ "  )";
        List<Map<String, Object>> result = jdbcRoadWar.queryForList(sql,inby);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Road Start List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	@Transactional
	public Map<String, Object> saveStreetDetails(
	        String type,                // "START" or "END"
	        String roadName,
	        String roadZone,
	        String roadWard,
	        String roadId,
	        String roadType,
	        String manualZone,
	        String manualWard,
	        String manualroadType,
	        String roadLayType,
	        String lastLayOn,           // yyyy-MM-dd or yyyy-MM format
	        String roadLength,
	        String carriagewayWidth,
	        String walltowallWidth,
	        String footpath,
	        String median,
	        String swd,
	        String inby,
	        String latitude,
	        String longitude,
	        String streetboard,
	        MultipartFile roadImage,
	        Integer startId             // only for END street (foreign key to start_street_details)
	) {
	    Map<String, Object> response = new HashMap<>();

	    // ✅ Handle file upload safely
	    final String uploadedImg = (roadImage != null && !roadImage.isEmpty())
	            ? fileUpload("RoadRegister", type.toLowerCase(), roadImage)
	            : null;
	    /*
	    String uploadedImg = null;
	    if (roadImage != null && !roadImage.isEmpty()) {
	        uploadedImg = fileUpload("RoadRegister", type.toLowerCase(), roadImage);
	    }
		*/
	    // ✅ Choose table & generated key column
	    String sql;
	    String[] generatedKey = new String[1];

	    if ("START".equalsIgnoreCase(type)) {
	        sql = "INSERT INTO start_street_details " +
	                "(road_name, road_zone, road_ward, road_id, road_type, " +
	                "manual_zone, manual_ward, manual_roadType, road_lay_type, last_lay_on, " +
	                "road_length, carriageway_width, walltowall_width, footpath, median, " +
	                "swd, inby, start_latitude, start_longitude, streetboard, start_img) " +
	                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	        generatedKey[0] = "strat_id";
	    } else {
	        sql = "INSERT INTO end_street_details " +
	                "(road_name, road_zone, road_ward, road_id, road_type, " +
	                "manual_zone, manual_ward, manual_roadType, road_lay_type, last_lay_on, " +
	                "road_length, carriageway_width, walltowall_width, footpath, median, " +
	                "swd, inby, start_latitude, start_longitude, streetboard, start_id, end_img) " +
	                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	        generatedKey[0] = "end_id";
	    }

	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affected = jdbcRoadWar.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(sql, generatedKey);
	        int i = 1;
	        ps.setString(i++, roadName);
	        ps.setString(i++, roadZone);
	        ps.setString(i++, roadWard);
	        ps.setString(i++, roadId);
	        ps.setString(i++, roadType);

	        // ✅ Manual fields
	        ps.setString(i++, manualZone);
	        ps.setString(i++, manualWard);
	        ps.setString(i++, manualroadType);
	        ps.setString(i++, roadLayType);
	        ps.setString(i++, lastLayOn);

	        // ✅ Road dimensions
	        ps.setString(i++, roadLength);
	        ps.setString(i++, carriagewayWidth);
	        ps.setString(i++, walltowallWidth);
	        ps.setString(i++, footpath);
	        ps.setString(i++, median);
	        ps.setString(i++, swd);
	        ps.setString(i++, inby);

	        // ✅ Location + Board
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, streetboard);

	        // ✅ Image & FK handling
	        if ("START".equalsIgnoreCase(type)) {
	            ps.setString(i++, uploadedImg);
	        } else {
	            ps.setObject(i++, startId);  // foreign key
	            ps.setString(i++, uploadedImg);
	        }
	        return ps;
	    }, keyHolder);

	    if (affected > 0) {
	        response.put("status", "success");
	        response.put("insertId", keyHolder.getKey().intValue());
	        response.put("message", type + " Street details saved successfully");
	    } else {
	        response.put("status", "error");
	        response.put("message", type + " Street details insert failed");
	    }

	    return response;
	}
	
	public List<Map<String, Object>> getCompletedRoadLists() {
	    String sql = "SELECT "
	            + "    s.strat_id AS UnicID, "
	            + "    s.road_name AS StreetName, "
	            + "    JSON_OBJECT("
	            + "        'strat_id', s.strat_id, "
	            + "        'road_name', s.road_name, "
	            + "        'road_zone', s.road_zone, "
	            + "        'road_ward', s.road_ward, "
	            + "        'road_id', s.road_id, "
	            + "        'road_type', s.road_type, "
	            + "        'manual_zone', s.manual_zone, "
	            + "        'manual_ward', s.manual_ward, "
	            + "        'manual_roadType', s.manual_roadType, "
	            + "        'road_lay_type', s.road_lay_type, "
	            + "        'last_lay_on', s.last_lay_on, "
	            + "        'road_length', s.road_length, "
	            + "        'carriageway_width', s.carriageway_width, "
	            + "        'walltowall_width', s.walltowall_width, "
	            + "        'footpath', s.footpath, "
	            + "        'median', s.median, "
	            + "        'swd', s.swd, "
	            + "        'inby', s.inby, "
	            + "        'indate', s.indate, "
	            + "        'start_latitude', s.start_latitude, "
	            + "        'start_longitude', s.start_longitude, "
	            + "        'isactive', s.isactive, "
	            + "        'isdelete', s.isdelete, "
	            + "        'streetboard', s.streetboard, "
	            + "        'start_img', s.start_img, "
	            + "        'start_img_url', CONCAT('" + fileBaseUrl + "/gccofficialapp/files', IFNULL(s.start_img, '/nostreetboard.png'))"
	            + "    ) AS `start`, "
	            + "    JSON_OBJECT("
	            + "        'end_id', e.end_id, "
	            + "        'road_name', e.road_name, "
	            + "        'road_zone', e.road_zone, "
	            + "        'road_ward', e.road_ward, "
	            + "        'road_id', e.road_id, "
	            + "        'road_type', e.road_type, "
	            + "        'manual_zone', e.manual_zone, "
	            + "        'manual_ward', e.manual_ward, "
	            + "        'manual_roadType', e.manual_roadType, "
	            + "        'road_lay_type', e.road_lay_type, "
	            + "        'last_lay_on', e.last_lay_on, "
	            + "        'road_length', e.road_length, "
	            + "        'carriageway_width', e.carriageway_width, "
	            + "        'walltowall_width', e.walltowall_width, "
	            + "        'footpath', e.footpath, "
	            + "        'median', e.median, "
	            + "        'swd', e.swd, "
	            + "        'inby', e.inby, "
	            + "        'indate', e.indate, "
	            + "        'start_latitude', e.start_latitude, "
	            + "        'start_longitude', e.start_longitude, "
	            + "        'isactive', e.isactive, "
	            + "        'isdelete', e.isdelete, "
	            + "        'streetboard', e.streetboard, "
	            + "        'end_img', e.end_img, "
	            + "        'end_img_url', CONCAT('" + fileBaseUrl + "/gccofficialapp/files', IFNULL(e.end_img, '/nostreetboard.png'))"
	            + "    ) AS `end` "
	            + "FROM start_street_details s "
	            + "INNER JOIN end_street_details e "
	            + "    ON e.start_id = s.strat_id "
	            + "WHERE s.isactive = 1 "
	            + "  AND s.isdelete = 0 "
	            + "  AND e.isactive = 1 "
	            + "  AND e.isdelete = 0";

	    List<Map<String, Object>> rawResult = jdbcRoadWar.queryForList(sql);

	    ObjectMapper mapper = new ObjectMapper();
	    List<Map<String, Object>> formattedResult = new ArrayList<>();

	    for (Map<String, Object> row : rawResult) {
	        Map<String, Object> formattedRow = new HashMap<>();
	        formattedRow.put("UnicID", row.get("UnicID"));
	        formattedRow.put("StreetName", row.get("StreetName"));

	        // Convert JSON strings to proper objects
	        try {
	            Map<String, Object> startObj = mapper.readValue(row.get("start").toString(), new TypeReference<Map<String, Object>>() {});
	            Map<String, Object> endObj = mapper.readValue(row.get("end").toString(), new TypeReference<Map<String, Object>>() {});
	            formattedRow.put("start", startObj);
	            formattedRow.put("end", endObj);
	        } catch (Exception ex) {
	            ex.printStackTrace();
	        }

	        formattedResult.add(formattedRow);
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Completed road list.");
	    response.put("data", formattedResult);

	    return Collections.singletonList(response);
	}
}
