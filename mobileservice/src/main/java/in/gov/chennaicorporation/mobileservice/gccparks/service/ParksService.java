package in.gov.chennaicorporation.mobileservice.gccparks.service;

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
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.DataSource;

import org.hibernate.internal.build.AllowSysOut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class ParksService {

	private JdbcTemplate jdbcParkseTemplate;

	private final Environment environment;
	private String fileBaseUrl;

	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final int STRING_LENGTH = 15;
	private static final Random RANDOM = new SecureRandom();

	@Autowired
	public void setDataSource(@Qualifier("mysqlGccParksDataSource") DataSource parksDataSource) {
		this.jdbcParkseTemplate = new JdbcTemplate(parksDataSource);
	}

	@Autowired
	public ParksService(Environment environment) {
		this.environment = environment;
		this.fileBaseUrl = environment.getProperty("fileBaseUrl");
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
		String serviceFolderName = environment.getProperty("parks_playground_foldername");
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

	@Transactional
	public List<Map<String, Object>> saveAsset(MultiValueMap<String, String> formData, String categoryId,
			String assetTypeId, String latitude, String longitude, String zone, String ward, String streeId,
			String streetName, String loginId, String name, MultipartFile file) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;

		String image = fileUpload(categoryId, assetTypeId, file);

		String sqlQuery = "INSERT INTO `asset_list`(`category_id`, `assettype_id`, `name`, `image`, `latitude`, `longitude`, "
				+ "`zone`, `ward`, `streetid`, `streetname`, `cby`) " + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			int affectedRows = jdbcParkseTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, categoryId);
					ps.setString(2, assetTypeId);
					ps.setString(3, name);
					ps.setString(4, image);
					ps.setString(5, latitude);
					ps.setString(6, longitude);
					ps.setString(7, zone);
					ps.setString(8, ward);
					ps.setString(9, streeId);
					ps.setString(10, streetName);
					ps.setString(11, loginId);
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

	@Transactional
	public List<Map<String, Object>> loadAssetByFilter(String categoryId, String assetTypeId, String latitude,
			String longitude) {

		String sqlWhere = "1=1";

		if (categoryId != null && !categoryId.isBlank() && !categoryId.isEmpty()) {
			sqlWhere += " AND ai.category_id='" + categoryId + "'";
		}

		if (categoryId != null && !assetTypeId.isBlank() && !assetTypeId.isEmpty()) {
			sqlWhere += " AND ai.assettype_id='" + assetTypeId + "'";
		}
		if (latitude != null && longitude != null && !latitude.isBlank() && !latitude.isEmpty() && !longitude.isBlank()
				&& !longitude.isEmpty()) {
			sqlWhere += " AND ((6371008.8 * acos(ROUND(cos(radians(" + latitude
					+ ")) * cos(radians(al.latitude)) * cos(radians(al.longitude) - radians(" + longitude
					+ ")) + sin(radians(" + latitude + ")) * sin(radians(al.latitude)), 9))) < 500) ";
		}

		String sqlQuery = "SELECT al.`id`, al.`category_id`, al.`assettype_id`, al.`name`, al.`latitude`, al.`longitude`, al.`zone`, "
				+ "al.`ward`, al.`streetid`, al.`streetname`, " + "CONCAT('" + fileBaseUrl
				+ "/gccofficialapp/files', al.image) AS imageUrl " + "FROM asset_list al WHERE " + sqlWhere;

		List<Map<String, Object>> result = jdbcParkseTemplate.queryForList(sqlQuery);
		return result;
	}

	// ----------------------------------------------Enumeration
	// API------------------------------------------------

	public int centerMedianEnumeration(String enterParkName, int maintained_By, Double enu_Area_Meters,
			int typeofCenterMedian, String latitude, String longitude, String zone, String ward, String location,
			String userId, MultipartFile photo, String enumeration_type) throws Exception {
		int enumerationImg_Id_master = centerMedianEnum(enterParkName, maintained_By, enu_Area_Meters,
				typeofCenterMedian, latitude, longitude, zone, ward, location, userId, enumeration_type);
		String name = "centerMedian " + enumerationImg_Id_master;
		String imgId = Integer.toString(enumerationImg_Id_master);
		try {
			String ImageUrl = fileUpload(name, imgId, photo);
			// System.out.println("===========................" + ImageUrl);
			saveImageForEnumeration(enumerationImg_Id_master, ImageUrl);
			return enumerationImg_Id_master;
		} catch (Exception e) {
			throw new Exception("Error in image saving" + e);
		}
	}

	private String saveImageForEnumeration(int enumerationImg_Id_master, String imageUrl) {
		String sql = "INSERT INTO enumertion_images (photos_enu, enumerationImg_Id_master)" + "VALUES (?, ?)";
		// Execute SQL query to save feedback for the provided question
		jdbcParkseTemplate.update(sql, imageUrl, enumerationImg_Id_master);

		return "success";
	}

	private int centerMedianEnum(String enterParkName, int maintained_By, Double enu_Area_Meters,
			int typeofCenterMedian, String latitude, String longitude, String zone, String ward, String location,
			String userId, String enumeration_type) {

		String sql = "INSERT INTO enumeration_master (park_name, maintained_By, Enu_Area_Meters, Cen_tra_Median_Type, latitude, longitude, zone, ward, location, userId, enumeration_type, cdate, is_Active, is_Delete) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, TRUE, FALSE)";

		KeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			jdbcParkseTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, new String[] { "enumeration_Id" });
				ps.setString(1, enterParkName); // park_name
				ps.setInt(2, maintained_By); // maintained_by_id
				ps.setDouble(3, enu_Area_Meters); // Enu_Area_Meters
				ps.setInt(4, typeofCenterMedian); // center_median_type_id
				ps.setString(5, latitude); // latitude
				ps.setString(6, longitude); // longitude
				ps.setString(7, zone); // zone
				ps.setString(8, ward); // ward
				ps.setString(9, location); // loction
				ps.setString(10, userId); // userId
				ps.setString(11, enumeration_type); // enumeration_type
				return ps;
			}, keyHolder);
			System.out.println("img id master:" + keyHolder.getKey().intValue());
			// Retrieve the auto-generated ID and return it
			return keyHolder.getKey().intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1; // Return error indicator
	}

	public int trafficIslandEnumeration(int maintainedById, Double enu_Area_Meters, int Type_of_Traffic_Island,
			String latitude, String longitude, String zone, String ward, String location, String userId,
			MultipartFile photo, String enumeration_type) throws Exception {
		int enumerationImg_Id_master = trafficIslandEnum(maintainedById, enu_Area_Meters, Type_of_Traffic_Island,
				latitude, longitude, zone, ward, location, userId, enumeration_type);
		String name = "trafficIsland " + enumerationImg_Id_master;
		String imgId = Integer.toString(enumerationImg_Id_master);
		try {
			String ImageUrl = fileUpload(name, imgId, photo);
			// System.out.println("===========................" + ImageUrl);
			saveImageForEnumeration(enumerationImg_Id_master, ImageUrl);
			return enumerationImg_Id_master;
		} catch (Exception e) {
			throw new Exception("Error in image saving " + e);
		}

	}

	private int trafficIslandEnum(int maintainedById, Double enu_Area_Meters, int type_of_Traffic_Island,
			String latitude, String longitude, String zone, String ward, String location, String userId,
			String enumeration_type) {

		String sql = "INSERT INTO enumeration_master (maintained_By, Enu_Area_Meters, Cen_tra_Median_Type, latitude, longitude, zone, ward, location, userId, enumeration_type, cdate, is_Active, is_Delete) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, TRUE, FALSE)";

		KeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			jdbcParkseTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, new String[] { "enumeration_Id" });
				ps.setInt(1, maintainedById); // maintained_by_id
				ps.setDouble(2, enu_Area_Meters); // Enu_Area_Meters
				ps.setInt(3, type_of_Traffic_Island); // center_median_type_id
				ps.setString(4, latitude); // latitude
				ps.setString(5, longitude); // longitude
				ps.setString(6, zone); // zone
				ps.setString(7, ward); // ward
				ps.setString(8, location); // loction
				ps.setString(9, userId); // userId
				ps.setString(10, enumeration_type); // enumeration_type
				return ps;

			}, keyHolder);
			// System.out.println("img id master:"+keyHolder.getKey().intValue());
			// Retrieve the auto-generated ID and return it
			return keyHolder.getKey().intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1; // Return error indicator
	}

	public int parkEnumeration(String enterParkName, int maintained_By, String latitude, String longitude, String zone,
			String ward, String location, String userId, MultipartFile photo, String zone_Available,
			String enumeration_type) throws Exception {

		int enumerationImg_Id_master = parkEnum(enterParkName, maintained_By, latitude, longitude, zone, ward, location,
				userId, photo, zone_Available, enumeration_type);
		// System.out.println("enumerationImg_Id_master"+enumerationImg_Id_master);
		String name = "parkEnumeration " + enumerationImg_Id_master;
		String imgId = Integer.toString(enumerationImg_Id_master);
		// String imgId = enumerationImg_Id_master+"";

		try {
			String ImageUrl = fileUpload(name, imgId, photo);
			// System.out.println("===========................" + ImageUrl);
			saveImageForEnumeration(enumerationImg_Id_master, ImageUrl);
			return enumerationImg_Id_master;
		} catch (Exception e) {
			throw new Exception("Error in image saving" + e);
		}

	}

	private int parkEnum(String enterParkName, int maintained_By, String latitude, String longitude, String zone,
			String ward, String location, String userId, MultipartFile photo, String zone_Available,
			String enumeration_type) {
		// System.out.println("Check1");
		String sql = "INSERT INTO enumeration_master (maintained_By,  latitude, longitude, zone, ward, location, userId,park_name, Zone_Available,enumeration_type, cdate, is_Active, is_Delete) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?,?,?, CURRENT_TIMESTAMP, TRUE, FALSE)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		System.out.println("Check2");

		try {

			jdbcParkseTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, new String[] { "enumeration_Id" }); // Primary
																											// key
																											// column

				// Set the parameters in the correct order (index starts at 1)
				ps.setInt(1, maintained_By); // maintained_By
				ps.setString(2, latitude); // latitude
				ps.setString(3, longitude); // longitude
				ps.setString(4, zone); // zone
				ps.setString(5, ward); // ward
				ps.setString(6, location); // loction
				ps.setString(7, userId); // userId
				ps.setString(8, enterParkName);
				ps.setString(9, zone_Available);
				ps.setString(10, enumeration_type);// park_name
				return ps;
			}, keyHolder);

			// Retrieve the auto-generated ID (primary key) and return it
			return keyHolder.getKey().intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1; // Return error indicator
	}

	public int beautificationRoad(String latitude, String longitude, String zone, String ward, String location,
			String userId, MultipartFile photo, String enumeration_type) throws Exception {

		int enumerationImg_Id_master = beautifiRoad(latitude, longitude, zone, ward, location, userId, photo,
				enumeration_type);
		String name = "beautificationRoad " + enumerationImg_Id_master;
		String imgId = Integer.toString(enumerationImg_Id_master);
		try {
			String ImageUrl = fileUpload(name, imgId, photo);
			// System.out.println("===========................" + ImageUrl);
			saveImageForEnumeration(enumerationImg_Id_master, ImageUrl);
			return enumerationImg_Id_master;
		} catch (Exception e) {
			throw new Exception("Error in image saving");
		}
	}

	private int beautifiRoad(String latitude, String longitude, String zone, String ward, String location,
			String userId, MultipartFile photo, String enumeration_type) {

		// SQL query with placeholders for parameters
		String sql = "INSERT INTO enumeration_master ( latitude, longitude, zone, ward, location, userId,enumeration_type, cdate, is_Active, is_Delete) "
				+ "VALUES ( ?, ?, ?, ?, ?, ?,?, CURRENT_TIMESTAMP, TRUE, FALSE)";

		KeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			jdbcParkseTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, new String[] { "enumeration_Id" }); // Primary
																											// key
																											// column

				// Set the parameters in the correct order (index starts at 1)
//	            ps.setString(1, area);                 // Area
				ps.setString(1, latitude); // latitude
				ps.setString(2, longitude); // longitude
				ps.setString(3, zone); // zone
				ps.setString(4, ward); // ward
				ps.setString(5, location); // loction
				ps.setString(6, userId);
				ps.setString(7, enumeration_type); // userId
				return ps;
			}, keyHolder);

			// Retrieve the auto-generated ID (primary key) and return it
			return keyHolder.getKey().intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1; // Return error indicator
	}

	public int readingZone(String latitude, String longitude, String zone, String ward, String location, String userId,
			MultipartFile photo, String enumeration_type, String park_name) throws Exception {

		int enumerationImg_Id_master = parkEnumReadingZone(latitude, longitude, zone, ward, location, userId, photo,
				enumeration_type, park_name);
		String name = "readingZone " + enumerationImg_Id_master;
		String imgId = Integer.toString(enumerationImg_Id_master);
		try {
			String ImageUrl = fileUpload(name, imgId, photo);
			// System.out.println("===========................" + ImageUrl);
			saveImageForEnumeration(enumerationImg_Id_master, ImageUrl);
			return enumerationImg_Id_master;
		} catch (Exception e) {
			throw new Exception("Error in image saving");
		}

	}

	private int parkEnumReadingZone(String latitude, String longitude, String zone, String ward, String location,
			String userId, MultipartFile photo, String enumeration_type, String park_name) {

		String sql = "INSERT INTO enumeration_master ( latitude, longitude, zone, ward, location, userId,enumeration_type,park_name, cdate, is_Active, is_Delete) "
				+ "VALUES ( ?, ?, ?, ?, ?, ?,?,?, CURRENT_TIMESTAMP, TRUE, FALSE)";

		KeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			jdbcParkseTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, new String[] { "enumeration_Id" }); // Primary
																											// key
																											// column

				// Set the parameters in the correct order (index starts at 1)
//	            ps.setString(1, zone_Available_name);                 // Area
				ps.setString(1, latitude); // latitude
				ps.setString(2, longitude); // longitude
				ps.setString(3, zone); // zone
				ps.setString(4, ward); // ward
				ps.setString(5, location); // loction
				ps.setString(6, userId); // userId
				ps.setString(7, enumeration_type);
				ps.setString(8, park_name);
				return ps;
			}, keyHolder);

			// Retrieve the auto-generated ID (primary key) and return it
			return keyHolder.getKey().intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1; // Return error indicator

	}

	public int miyawakiNew(String type_of_place, String latitude, String longitude, String zone, String ward,
			String location, String userId, MultipartFile photo, String enumeration_type) throws Exception {

		int enumerationImg_Id_master = miyawakiNeww(type_of_place, latitude, longitude, zone, ward, location, userId,
				photo, enumeration_type);
		String name = "miyawakiNew " + enumerationImg_Id_master;
		String imgId = Integer.toString(enumerationImg_Id_master);
		try {
			String ImageUrl = fileUpload(name, imgId, photo);
			// System.out.println("===========................" + ImageUrl);
			saveImageForEnumeration(enumerationImg_Id_master, ImageUrl);
			return enumerationImg_Id_master;
		} catch (Exception e) {
			throw new Exception("Error in image saving");
		}
	}

	private int miyawakiNeww(String type_of_place, String latitude, String longitude, String zone, String ward,
			String location, String userId, MultipartFile photo, String enumeration_type) {
		String sql = "INSERT INTO enumeration_master ( Type_of_place, latitude, longitude, zone, ward, location, userId,enumeration_type, cdate, is_Active, is_Delete) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?,?, CURRENT_TIMESTAMP, TRUE, FALSE)";

		KeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			jdbcParkseTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, new String[] { "enumeration_Id" }); // Primary
																											// key
																											// column

				// Set the parameters in the correct order (index starts at 1)
				ps.setString(1, type_of_place); // Area
				ps.setString(2, latitude); // latitude
				ps.setString(3, longitude); // longitude
				ps.setString(4, zone); // zone
				ps.setString(5, ward); // ward
				ps.setString(6, location); // loction
				ps.setString(7, userId);
				ps.setString(8, enumeration_type);// userId
				return ps;
			}, keyHolder);

			// Retrieve the auto-generated ID (primary key) and return it
			return keyHolder.getKey().intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1; // Return error indicator
	}

	public int miyawakiExisting(int maintained_By, String latitude, String longitude, String zone, String ward,
			String location, String userId, MultipartFile photo, String enumeration_type) throws Exception {
		int enumerationImg_Id_master = miyawakiExistingSer(maintained_By, latitude, longitude, zone, ward, location,
				userId, photo, enumeration_type);
		String name = "miyawakiExisting " + enumerationImg_Id_master;
		String imgId = Integer.toString(enumerationImg_Id_master);
		try {
			String ImageUrl = fileUpload(name, imgId, photo);
			// System.out.println("===========................" + ImageUrl);
			saveImageForEnumeration(enumerationImg_Id_master, ImageUrl);
			return enumerationImg_Id_master;
		} catch (Exception e) {
			throw new Exception("Error in image saving");
		}
	}

	private int miyawakiExistingSer(int maintained_By, String latitude, String longitude, String zone, String ward,
			String location, String userId, MultipartFile photo, String enumeration_type) {
		String sql = "INSERT INTO enumeration_master ( maintained_By, latitude, longitude, zone, ward, location, userId,enumeration_type, cdate, is_Active, is_Delete) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?,?, CURRENT_TIMESTAMP, TRUE, FALSE)";

		KeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			jdbcParkseTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, new String[] { "enumeration_Id" }); // Primary
																											// key
																											// column

				// Set the parameters in the correct order (index starts at 1)
				ps.setInt(1, maintained_By); // Area
				ps.setString(2, latitude); // latitude
				ps.setString(3, longitude); // longitude
				ps.setString(4, zone); // zone
				ps.setString(5, ward); // ward
				ps.setString(6, location); // loction
				ps.setString(7, userId); // userId
				ps.setString(8, enumeration_type);
				return ps;
			}, keyHolder);

			// Retrieve the auto-generated ID (primary key) and return it
			return keyHolder.getKey().intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1; // Return error indicator
	}

	// -----------------------------------------------Feedback
	// API-----------------------------------------------------------------

	public int centerMedian(String irrigationProperlyQa, String irrigationProperlyAns, String trimmingProperlyQa,
			String trimmingProperlyAns, String weedingQa, String weedingAns, String handRailingQa,
			String handRailingAns, String latitude, String longitude, String zone, String ward, String location,
			String enumeration_type, String userId, String enumeration_id, MultipartFile photo) throws Exception {
		String saveImage;
		try {
			saveImage = fileUpload("CenterMedianFeedback", enumeration_id, photo);
		} catch (Exception e) {
			throw new Exception(" check photo");
		}
		centerMedian(saveImage, enumeration_id);
		centerMedian("test_cen", enumeration_id);
		int activity_Id = saveActivity(zone, location, latitude, longitude, ward, userId, enumeration_type,
				enumeration_id);
		// System.out.println(activity_Id);
		SaveFeedBack(irrigationProperlyQa, irrigationProperlyAns, activity_Id);
		//SaveFeedBack(irrigationProperlyQa, irrigationProperlyAns, activity_Id);
		SaveFeedBack(trimmingProperlyQa, trimmingProperlyAns, activity_Id);
		SaveFeedBack(weedingQa, weedingAns, activity_Id);
		int saveFeedBackId = SaveFeedBack(handRailingQa, handRailingAns, activity_Id);
		
		saveComplaintFeedback(irrigationProperlyQa, irrigationProperlyAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(trimmingProperlyQa, trimmingProperlyAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(weedingQa, weedingAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(handRailingQa, handRailingAns, activity_Id, zone, ward, latitude, longitude, userId);

		return saveFeedBackId;

	}

	private int saveActivity(String zone, String division, String latitude, String longitude, String ward,
			String userId, String enumeration_type, String enumeration_id) {

		// SQL query to insert activity details into the activity table
		String sql = "INSERT INTO activity (feedback_zone, feedback_location, feedback_latitude, feedback_longitude, cdate, ward,user_Id, feedback_enumeration_Type, feedback_enumeration_Id) "
				+ "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			// Use JdbcTemplate's update method to execute the insert operation
			jdbcParkseTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, new String[] { "activity_id" }); // Assuming
																											// "activity_id"
																											// is the
																											// primary
																											// key

				// Set the parameters for the activity table in the correct order (index starts
				// at 1)
				ps.setString(1, zone); // feedback_zone
				ps.setString(2, division); // feedback_division
				ps.setString(3, latitude); // feedback_latitude
				ps.setString(4, longitude);// feedback_longitude
				ps.setString(5, ward); // user_Id
				ps.setString(6, userId);
				ps.setString(7, enumeration_type);
				ps.setString(8, enumeration_id);// feedback_enumeration_Type

				return ps;
			}, keyHolder);

			// Retrieve the auto-generated activity_id and return it
			return keyHolder.getKey().intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1; // Return -1 in case of any error
	}

	private int SaveFeedBack(String qaId, String qaName, int activityId) {

		String sql = "INSERT INTO feedback (feedback_Name, feedback_qa_id, activity_id) " + "VALUES (?, ?, ?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			jdbcParkseTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, new String[] { "feed_id" }); // Primary key
																										// column

				// Set the parameters in the correct order (index starts at 1)
				ps.setString(1, qaName); // feedback_Name
				ps.setString(2, qaId); // feedback_qa_id
				ps.setInt(3, activityId); // feedback_enumeration_Id
				return ps;
			}, keyHolder);

			// Retrieve the auto-generated ID (primary key) and return it
			return keyHolder.getKey().intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1; // Return error indicator
	}

	private List<Map<String, Object>> centerMedian(String saveImage, String enumeration_id) {
		// Update SQL query
//		String updateSql = "UPDATE enumeration_master SET centerMedian_image = ? WHERE enumeration_Id = ?";

		try {
			// Perform the update operation
//			jdbcParkseTemplate.update( saveImage, enumeration_id);

			// After the update, fetch the updated row
			String selectSql = "SELECT * FROM enumeration_master WHERE enumeration_Id = ?"; // Added '='

			// Return the updated data as a list of maps (each map corresponds to a row)
			return jdbcParkseTemplate.queryForList(selectSql, enumeration_id);

		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList(); // Return an empty list in case of an error
		}
	}
/* Patch 23-11-2024
	public int trafficIsland(String irrigationProperlyQa, String irrigationProperlyAns, String trimmingProperlyQa,
			String trimmingProperlyAns, String weedingQa, String weedingans, String handRailingQa,
			String handRailingAns, String latitude, String longitude, String zone, String ward, String location,
			String userId, String enumeration_type, String enumeration_id, String gapFillingQa, String gapFilingAns,
			String improvementQa, String improvementAns, MultipartFile photo) throws Exception {

		String saveImage;
		try {
			saveImage = fileUpload("TrafficIslandFeedback", enumeration_id, photo);
		} catch (Exception e) {
			throw new Exception(" check photo");
		}

		trafficIslandSave(saveImage, enumeration_id);
		int activity_Id = saveActivity(zone, location, latitude, longitude, ward, userId, enumeration_type,
				enumeration_id);

		SaveFeedBack(improvementQa, improvementAns, activity_Id);
		SaveFeedBack(irrigationProperlyQa, irrigationProperlyAns, activity_Id);
		SaveFeedBack(trimmingProperlyQa, trimmingProperlyAns, activity_Id);
		SaveFeedBack(weedingQa, weedingans, activity_Id);

		int saveFeedBackId = SaveFeedBack(handRailingQa, handRailingAns, activity_Id);
		return saveFeedBackId;

	}
*/
	
	public int trafficIsland(String irrigationProperlyQa, String irrigationProperlyAns, String trimmingProperlyQa,
			String trimmingProperlyAns, String weedingQa, String weedingans, String handRailingQa,
			String handRailingAns, String latitude, String longitude, String zone, String ward, String location,
			String userId, String enumeration_type, String enumeration_id, String gapFillingQa, String gapFilingAns,
			String improvementQa, String improvementAns,String fountainAvailableQa, String fountainAvailableAns, 
			String fountainWorkingQa, String fountainWorkingAns,MultipartFile photo) throws Exception {

		String saveImage;
		try {
			saveImage = fileUpload("TrafficIslandFeedback", enumeration_id, photo);
		} catch (Exception e) {
			throw new Exception(" check photo");
		}

		trafficIslandSave(saveImage, enumeration_id);
		int activity_Id = saveActivity(zone, location, latitude, longitude, ward, userId, enumeration_type,enumeration_id);
		
		SaveFeedBack(irrigationProperlyQa, irrigationProperlyAns, activity_Id);
		SaveFeedBack(trimmingProperlyQa, trimmingProperlyAns, activity_Id);
		SaveFeedBack(weedingQa, weedingans, activity_Id);		
		SaveFeedBack(improvementQa, improvementAns, activity_Id);
		SaveFeedBack(fountainAvailableQa, fountainAvailableAns, activity_Id);
		SaveFeedBack(fountainWorkingQa, fountainWorkingAns, activity_Id);

		int saveFeedBackId = SaveFeedBack(handRailingQa, handRailingAns, activity_Id);
		
		saveComplaintFeedback(irrigationProperlyQa, irrigationProperlyAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(trimmingProperlyQa, trimmingProperlyAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(weedingQa, weedingans, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(improvementQa, improvementAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(fountainAvailableQa, fountainAvailableAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(fountainWorkingQa, fountainWorkingAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(gapFillingQa, gapFilingAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(handRailingQa, handRailingAns, activity_Id, zone, ward, latitude, longitude, userId);

        
		return saveFeedBackId;
	}
	
	private List<Map<String, Object>> trafficIslandSave(String saveImage, String enumeration_id) {
		// Update SQL query
//		 String updateSql = "UPDATE enumeration_master SET traffic_image = ? WHERE enumeration_Id = ?";

		try {
			// Perform the update operation
//			jdbcParkseTemplate.update(saveImage, enumeration_id);

			// After the update, fetch the updated row
			String selectSql = "SELECT * FROM enumeration_master WHERE enumeration_Id = ?"; // Added '='

			// Return the updated data as a list of maps (each map corresponds to a row)
			return jdbcParkseTemplate.queryForList(selectSql, enumeration_id);

		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList(); // Return an empty list in case of an error
		}
	}

	public int beautification(String irrigationProperlyQa, String irrigationProperlyAns, String trimmingProperlyQa,
			String trimmingProperlyAns, String weedingQa, String weedingans, String gapFillingQa, String gapFillingAns,
			String improvementQa, String improvementAns, String latitude, String longitude, String zone, String ward,
			String location, String enumeration_id, String userId, String enumeration_type, MultipartFile photo)
			throws Exception {
		String saveImage;
		try {
			saveImage = fileUpload("beautificationFeedback", enumeration_id, photo);
		} catch (Exception e) {
			throw new Exception(" check photo");
		}

		saveBeautification(saveImage, enumeration_id);
		int activity_Id = saveActivity(zone, location, latitude, longitude, ward, userId, enumeration_type,
				enumeration_id);

		SaveFeedBack(irrigationProperlyQa, irrigationProperlyAns, activity_Id);
		SaveFeedBack(trimmingProperlyQa, trimmingProperlyAns, activity_Id);
		SaveFeedBack(weedingQa, weedingans, activity_Id);
		SaveFeedBack(improvementQa, improvementAns, activity_Id);
		
		int saveFeedBackId = SaveFeedBack(gapFillingQa, gapFillingAns, activity_Id);
		
		saveComplaintFeedback(irrigationProperlyQa, irrigationProperlyAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(trimmingProperlyQa, trimmingProperlyAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(weedingQa, weedingans, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(improvementQa, improvementAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(gapFillingQa, gapFillingAns, activity_Id, zone, ward, latitude, longitude, userId);

        
		return saveFeedBackId;

	}

	private List<Map<String, Object>> saveBeautification(String saveImage, String enumeration_id) {
//		 String updateSql = "UPDATE enumeration_master SET feed_beautificat = ? WHERE enumeration_Id = ?";

		try {
			// Perform the update operation
//			jdbcParkseTemplate.update(saveImage, enumeration_id);

			// After the update, fetch the updated row
			String selectSql = "SELECT * FROM enumeration_master WHERE enumeration_Id = ?"; // Added '='

			// Return the updated data as a list of maps (each map corresponds to a row)
			return jdbcParkseTemplate.queryForList(selectSql, enumeration_id);

		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList(); // Return an empty list in case of an error
		}
	}

	public int parkInspection(String openedQa, String openedAns, String irrigationProperlyQa,
			String irrigationProperlyAns, String trimmingProperlyQa, String trimmingProperlyAns, String weedingQa,
			String weedingans, String gapFillingQa, String gapFillingAns, String playandGymQa, String playandGymAns,
			String overAllQa, String overAllAns, String properLightQa, String properLightAns, String debrisRemovalQa,
			String debrisRemovalAns, String toiletAQA, String toiletAns, String latitude, String longitude, String zone,
			String ward, String location, String userId, String enumeration_type, String enumeration_id,
			String civilWorkQa, String civilWorkAns, String noWatchmenQa, String noWatchmenAns, String noSweeperQa,
			String noSweeperAns, MultipartFile photo) throws Exception {

		String saveImage;
		try {
			saveImage = fileUpload("ParkInspectionFeedback", enumeration_id, photo);
		} catch (Exception e) {
			throw new Exception(" check photo");
		}

		saveParkInspection(saveImage, enumeration_id);
		int activity_Id = saveActivity(zone, location, latitude, longitude, ward, userId, enumeration_type,
				enumeration_id);

		SaveFeedBack(civilWorkQa, civilWorkAns, activity_Id);
		SaveFeedBack(noWatchmenQa, noWatchmenAns, activity_Id);
		SaveFeedBack(noSweeperQa, noSweeperAns, activity_Id);

		SaveFeedBack(openedQa, openedAns, activity_Id);
		SaveFeedBack(irrigationProperlyQa, irrigationProperlyAns, activity_Id);
		SaveFeedBack(trimmingProperlyQa, trimmingProperlyAns, activity_Id);
		SaveFeedBack(weedingQa, weedingans, activity_Id);
		SaveFeedBack(gapFillingQa, gapFillingAns, activity_Id);

		SaveFeedBack(playandGymQa, playandGymAns, activity_Id);
		SaveFeedBack(overAllQa, overAllAns, activity_Id);
		SaveFeedBack(properLightQa, properLightAns, activity_Id);
		SaveFeedBack(debrisRemovalQa, debrisRemovalAns, activity_Id);

		int saveFeedBackId = SaveFeedBack(toiletAQA, toiletAns, activity_Id);
		
		saveComplaintFeedback(civilWorkQa, civilWorkAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(noWatchmenQa, noWatchmenAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(noSweeperQa, noSweeperAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(openedQa, openedAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(irrigationProperlyQa, irrigationProperlyAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(trimmingProperlyQa, trimmingProperlyAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(weedingQa, weedingans, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(gapFillingQa, gapFillingAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(playandGymQa, playandGymAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(overAllQa, overAllAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(properLightQa, properLightAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(debrisRemovalQa, debrisRemovalAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(toiletAQA, toiletAns, activity_Id, zone, ward, latitude, longitude, userId);

		return saveFeedBackId;

	}

	private List<Map<String, Object>> saveParkInspection(String saveImage, String enumeration_id) {
//		 String updateSql = "UPDATE enumeration_master SET feed_prak_Inspe_image = ? WHERE enumeration_id = ?";
		try {
			// Perform the update operation
//			jdbcParkseTemplate.update(saveImage, enumeration_id);

			// After the update, fetch the updated row
			String selectSql = "SELECT * FROM enumeration_master WHERE enumeration_Id = ?"; // Added '='

			// Return the updated data as a list of maps (each map corresponds to a row)
			return jdbcParkseTemplate.queryForList(selectSql, enumeration_id);

		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList(); // Return an empty list in case of an error
		}
	}

	public int readingZone(String openedOrNotQa, String openedOrNotAns, String bookAvailableQa, String bookAvailableAns,
			String newsapaperAvailableQa, String newsapaperAvailableAns, String latitude, String longitude, String zone,
			String ward, String location, String userId, String enumeration_type, String enumeration_id,
			String improvementQa, String improvementAns, MultipartFile photo) throws Exception {
		// TODO Auto-generated method

		String saveImage;
		try {
			saveImage = fileUpload("readinZoneFeedback", enumeration_id, photo);
		} catch (Exception e) {
			throw new Exception(" check photo");
		}

		saveReadingZone(saveImage, enumeration_id);
		int activity_Id = saveActivity(zone, location, latitude, longitude, ward, userId, enumeration_type,
				enumeration_id);

		SaveFeedBack(openedOrNotQa, openedOrNotAns, activity_Id);
		SaveFeedBack(bookAvailableQa, bookAvailableAns, activity_Id);
		SaveFeedBack(improvementQa, improvementAns, activity_Id);
		
		int saveFeedBackId = SaveFeedBack(newsapaperAvailableQa, newsapaperAvailableAns, activity_Id);
		
		saveComplaintFeedback(openedOrNotQa, openedOrNotAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(bookAvailableQa, bookAvailableAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(improvementQa, improvementAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(newsapaperAvailableQa, newsapaperAvailableAns, activity_Id, zone, ward, latitude, longitude, userId);


		return saveFeedBackId;

	}

	private List<Map<String, Object>> saveReadingZone(String saveImage, String enumeration_id) {
//		 String updateSql = "UPDATE enumeration_master SET feed_readingZone = ? WHERE enumeration_id = ?";
		try {
			// Perform the update operation
//			jdbcParkseTemplate.update(saveImage, enumeration_id);

			// After the update, fetch the updated row
			String selectSql = "SELECT * FROM enumeration_master WHERE enumeration_Id = ?"; // Added '='

			// Return the updated data as a list of maps (each map corresponds to a row)
			return jdbcParkseTemplate.queryForList(selectSql, enumeration_id);

		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList(); // Return an empty list in case of an error
		}
	}

	public int miyawakiNew(String debrisCleaningQa, String debrisCleaningAns, String landpreprarationQa,
			String landpreprarationAns, String plantingDoneQa, String plantingDoneAns, String cctvQa, String cctvAns,
			String latitude, String longitude, String zone, String ward, String location, String userId,
			String enumeration_type, String enumeration_id, String saplingAvailQa, String saplingAvailAns,
			String fencingAvailbilityQa, String fencingAvailbilityAns, String sourcaOfIrrigationQa,
			String sourcaOfIrrigationAns, MultipartFile photo) throws Exception {

		String saveImage;
		try {
			saveImage = fileUpload("miyawakiNewFeedback", enumeration_id, photo);
		} catch (Exception e) {
			throw new Exception(" check photo");
		}

		saveMiyawakiNew(saveImage, enumeration_id);
		int activity_Id = saveActivity(zone, location, latitude, longitude, ward, userId, enumeration_type,
				enumeration_id);

		SaveFeedBack(saplingAvailQa, saplingAvailAns, activity_Id);
		SaveFeedBack(fencingAvailbilityQa, fencingAvailbilityAns, activity_Id);
		SaveFeedBack(sourcaOfIrrigationQa, sourcaOfIrrigationAns, activity_Id);

		SaveFeedBack(debrisCleaningQa, debrisCleaningAns, activity_Id);
		SaveFeedBack(landpreprarationQa, landpreprarationAns, activity_Id);
		SaveFeedBack(plantingDoneQa, plantingDoneAns, activity_Id);

		int saveFeedBackId = SaveFeedBack(cctvQa, cctvAns, activity_Id);
		
		saveComplaintFeedback(saplingAvailQa, saplingAvailAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(fencingAvailbilityQa, fencingAvailbilityAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(sourcaOfIrrigationQa, sourcaOfIrrigationAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(debrisCleaningQa, debrisCleaningAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(landpreprarationQa, landpreprarationAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(plantingDoneQa, plantingDoneAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(cctvQa, cctvAns, activity_Id, zone, ward, latitude, longitude, userId);

		return saveFeedBackId;
	}

	private List<Map<String, Object>> saveMiyawakiNew(String saveImage, String enumeration_id) {
//		 String updateSql = "UPDATE enumeration_master SET feed_miyawakiNew = ? WHERE enumeration_id = ?";

		try {
			// Perform the update operation
//			jdbcParkseTemplate.update(saveImage, enumeration_id);
			// After the update, fetch the updated row
			String selectSql = "SELECT * FROM enumeration_master WHERE enumeration_Id = ?"; // Added '='

			// Return the updated data as a list of maps (each map corresponds to a row)
			return jdbcParkseTemplate.queryForList(selectSql, enumeration_id);

		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList(); // Return an empty list in case of an error
		}
	}

	public int miyawakiExisiting(String weedingQa, String weedingAns, String purningQa, String purningans,
			String gapFillingQa, String gapFillingAns, String latitude, String longitude, String zone, String ward,
			String location, String userId, String enumeration_type, String enumeration_id, String improvmetReqQa,
			String improvmetReqAns, String sourceOfirrigationQa, String sourceOfirrigationAns, MultipartFile photo)
			throws Exception {

		String saveImage;
		try {
			saveImage = fileUpload("miywakiExistingFeedback", enumeration_id, photo);
		} catch (Exception e) {
			throw new Exception(" check photo");
		}

		saveMiyawakiExisiting(saveImage, enumeration_id);
		int activity_Id = saveActivity(zone, location, latitude, longitude, ward, userId, enumeration_type,
				enumeration_id);

		SaveFeedBack(improvmetReqQa, improvmetReqAns, activity_Id);
		SaveFeedBack(sourceOfirrigationQa, sourceOfirrigationAns, activity_Id);

		SaveFeedBack(weedingQa, weedingAns, activity_Id);
		SaveFeedBack(purningQa, purningans, activity_Id);
		
		int saveFeedBackId = SaveFeedBack(gapFillingQa, gapFillingAns, activity_Id);
		
		saveComplaintFeedback(improvmetReqQa, improvmetReqAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(sourceOfirrigationQa, sourceOfirrigationAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(weedingQa, weedingAns, activity_Id, zone, ward, latitude, longitude, userId);
        saveComplaintFeedback(purningQa, purningans, activity_Id, zone, ward, latitude, longitude, userId);

		return saveFeedBackId;
	}

	private List<Map<String, Object>> saveMiyawakiExisiting(String saveImage, String enumeration_id) {

//		 String updateSql = "UPDATE enumeration_master SET feed_miyawaki_Exit = ? WHERE enumeration_id = ?";

		try {
			// Perform the update operation
//			jdbcParkseTemplate.update(saveImage, enumeration_id);
			// After the update, fetch the updated row
			String selectSql = "SELECT * FROM enumeration_master WHERE enumeration_Id = ?"; // Added '='

			// Return the updated data as a list of maps (each map corresponds to a row)
			return jdbcParkseTemplate.queryForList(selectSql, enumeration_id);

		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList(); // Return an empty list in case of an error
		}
	}

	public List<Map<String, Object>> categoryType() {
		String sql = "SELECT enumeration_type_id, enumerationName FROM enumerationtype where is_Active = 1 and is_Delete =0 ";

		try {
			// Use query to get a list of park names
			List<Map<String, Object>> parkDetails = jdbcParkseTemplate.queryForList(sql);
			return parkDetails;
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList(); // Return an empty list in case of an error
		}
	}

	public List<Map<String, Object>> masterCategoryType(String cat_enumeration_id) {
		String sql = "SELECT master_category_id, categoryName FROM mastercategory WHERE Cat_enumeration_id = ? and is_Active = 1 and is_Delete = 0";

		try {
			// Execute the query and pass the parameter
			List<Map<String, Object>> categoryDetails = jdbcParkseTemplate.queryForList(sql,
					new Object[] { cat_enumeration_id });
			return categoryDetails;
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList(); // Return an empty list in case of an error
		}
	}

	public List<Map<String, Object>> subCategoryType(String masterCategory) {
		String sql = "SELECT sub_category_id, categoryName FROM subcategory WHERE masterCategory = ? and is_Active = 1 and is_Delete = 0";
		try {
			// Execute the query and pass the parameter
			List<Map<String, Object>> categoryDetails = jdbcParkseTemplate.queryForList(sql,
					new Object[] { masterCategory });
			return categoryDetails;
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList(); // Return an empty list in case of an error
		}
	}

	public List<Map<String, Object>> getDateByLocation(String latitude, String longitude, String enumeration_type) throws Exception {
		String latitude1 = latitude;

		List<Map<String, Object>> parkByLocation = getParkByLocation(latitude, longitude, latitude1, enumeration_type);
		if (parkByLocation == null) {
			throw new NoDataException("No data in database");
		}
		return parkByLocation;
	}

	public List<Map<String, Object>> getReadingZoneParkByLocation(String latitude, String longitude, String latitude1)
			throws Exception {
//		String sql = "SELECT enutype.enumeration_type_id , enutype.enumerationName, COALESCE(ac.park_name, ac.location) AS park_name, ac.enumeration_Id "
//				+ "FROM  enumeration_master ac left join enumerationtype enutype on  enutype.enumeration_type_id = ac.enumeration_type WHERE "
//				+ " ((6371008.8 * acos(  ROUND(cos(radians(?)) * cos(radians(ac.latitude)) * cos(radians(ac.longitude) - radians(?)) +   sin(radians(?)) * sin(radians(ac.latitude)), 9)   )) < 500)"
//				+ " AND Zone_Available='yes' "
//				+ " GROUP BY enutype.enumeration_type_id,enutype.enumerationName, ac.park_name, ac.location ";
		String sql = "SELECT  enutype.enumeration_type_id , enutype.enumerationName, COALESCE(ac.park_name, ac.location) AS park_name,ac.enumeration_Id "
				+ "FROM  enumeration_master ac left join enumerationtype enutype on  enutype.enumeration_type_id = ac.enumeration_type   WHERE   "
				+ " ((6371008.8 * acos(  ROUND(cos(radians(?)) * cos(radians(ac.latitude)) * cos(radians(ac.longitude) - radians(?)) +   sin(radians(?)) * sin(radians(ac.latitude)), 9)   )) < 500)"
				+ " AND ac.Zone_Available='yes' "
				+ " GROUP BY enutype.enumeration_type_id,enutype.enumerationName, ac.park_name, ac.location,ac.enumeration_Id ";

		// Use latitude, longitude, and schoolType as positional parameters
		List<Map<String, Object>> parkByLocation = jdbcParkseTemplate.queryForList(sql,
				new Object[] { latitude, longitude, latitude1 });

		if (parkByLocation == null) {
			throw new NoDataException("No data in database");
		}
		return parkByLocation;
	}

	private List<Map<String, Object>> getParkByLocation(String latitude, String longitude, String latitude1, String enumeration_type) {
		String sql = "SELECT  ac.enumeration_Id,enutype.enumeration_type_id , enutype.enumerationName, COALESCE(ac.park_name, ac.location) AS park_name "
				+ "FROM  enumeration_master ac left join enumerationtype enutype on  enutype.enumeration_type_id = ac.enumeration_type   WHERE   "
				+ " ((6371008.8 * acos(  ROUND(cos(radians(?)) * cos(radians(ac.latitude)) * cos(radians(ac.longitude) - radians(?)) +   sin(radians(?)) * sin(radians(ac.latitude)), 9)   )) < 500) "
				+ "and (ac.park_name is not null or ac.location is not null) and (ac.enumeration_type is not null and ac.enumeration_type =?) "
				+ " GROUP BY ac.enumeration_Id, enutype.enumeration_type_id,enutype.enumerationName, ac.park_name, ac.location ";

		return jdbcParkseTemplate.queryForList(sql, new Object[] { latitude, longitude, latitude1, enumeration_type });
	}

	public List<Map<String, Object>> parkName() throws Exception {

		List<Map<String, Object>> parkName = getParkName();

		if (parkName == null) {
			throw new NoDataException("The is no park");
		}
		return parkName;

	}

	public List<Map<String, Object>> getParkName() {
		String sql = "SELECT enumeration_Id, COALESCE(park_name, location) AS park_name FROM enumeration_master WHERE Zone_Available = 'yes'";

		try {

			List<Map<String, Object>> parkDetails = jdbcParkseTemplate.queryForList(sql);
			return parkDetails;
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList(); // Return an empty list in case of an error
		}
	}

	// ------------------------------------------Tree Feedback
	// API-----------------------------------------------------

	public Map<String, Object> SaveTreeDetailes(int No_of_trees_required, String enumeration_type, String latitude,
			String longitude, String zone, String ward, String location, String parkname, String userId,
			MultipartFile tree_Image) {

		int No_of_trees = 0;
		String tree_Status;
		int treeId;
		int treeflag = 0;

		if (No_of_trees_required == No_of_trees) {
			tree_Status = "completed";
		}
		tree_Status = "pending";
		Map<String, Object> saveTreeDetailes = SaveTreeDetails(No_of_trees_required, enumeration_type, No_of_trees,
				tree_Status, latitude, longitude, zone, ward, location, parkname, userId);

		if (saveTreeDetailes.containsKey("tree_Id")) {

			treeId = (int) saveTreeDetailes.get("tree_Id");

			try {
				Random random = new Random();
				// Generate a random number between 1000 and 9999
				int treeId1 = 1000 + random.nextInt(9000);

				String saveImage = fileUpload("TreeFeedback", Integer.toString(treeId1), tree_Image); // FileUpload
				SaveTreeImage(treeId, saveImage, userId, zone, ward, latitude, longitude, treeflag);
			} catch (Exception e) {
				throw new NoDataException("check the tree_id ");
			}

		} else {
			// Handle error case if tree_Id is missing
			System.out.println("Error: tree_Id not found in saved data.");
		}
		return saveTreeDetailes;

	}

	private int SaveTreeImage(int treeId, String saveImage, String userId, String zone, String ward, String latitude,
			String longitude, int treeflag) {

		String sql = "INSERT INTO tree_image (tr_image, tree_enumeration_id, tree_user_id, "
				+ "tree_zone, tree_ward, tree_latitude, tree_longitude, tree_cdate,tree_flag) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			jdbcParkseTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, new String[] { "tree_Image_Id" }); // Auto-generated
																											// primary
																											// key
				ps.setString(1, saveImage); // tr_image
				ps.setInt(2, treeId); // tree_enumeration_id
				ps.setString(3, userId); // tree_user_id
				ps.setString(4, zone); // tree_zone
				ps.setString(5, ward); // tree_ward
				ps.setString(6, latitude); // tree_latitude
				ps.setString(7, longitude);
				ps.setInt(8, treeflag);
//				ps.setString(8, treeFlag);
				// tree_longitude
				return ps;
			}, keyHolder);

			// Retrieve and return the auto-generated ID
			return keyHolder.getKey().intValue();
		} catch (Exception e) {
			e.printStackTrace();
			return -1; // Return error code
		}

	}

	private Map<String, Object> SaveTreeDetails(int no_of_trees_required, String enumeration_type, int no_of_trees,
			String tree_Status, String latitude, String longitude, String zone, String ward, String location,
			String parkname, String userId) {
		String locationtxt = location.isEmpty() ? null : location;
		String parknametxt = parkname.isEmpty() ? null : parkname;
		
		String sql = "INSERT INTO treeplaintandpruning (No_of_trees, No_of_trees_required,  tree_Status, "
				+ "tree_enumeration_id, tree_user_id,tree_location, tr_parkname, tree_zone, tree_ward, tree_latitude, tree_longitude, "
				+ "tree_cdate, tree_is_Active, tree_is_Delete) "
				+ "VALUES (?, ?, ?, ?,?,?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, TRUE, FALSE)";

		KeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			jdbcParkseTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, new String[] { "tree_Id" }); // Auto-generated
																										// primary key
				ps.setInt(1, no_of_trees);
				ps.setInt(2, no_of_trees_required);
				ps.setString(3, tree_Status);
				ps.setString(4, enumeration_type);
				ps.setString(5, userId);
				ps.setString(6, locationtxt);
				ps.setString(7, parknametxt);
				ps.setString(8, zone);
				ps.setString(9, ward);
				ps.setString(10, latitude);
				ps.setString(11, longitude);
				return ps;
			}, keyHolder);

			int treeId = keyHolder.getKey().intValue();

			String selectSql = "SELECT * FROM treeplaintandpruning WHERE tree_Id = ?";
			return jdbcParkseTemplate.queryForMap(selectSql, treeId);

		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyMap(); // Return an empty map if there's an error
		}
	}

	public Map<String, Object> SaveimageDetailes(String enumeration_type, String latitude, String longitude,
			String userId, MultipartFile tree_Image, int tree_id) throws Exception {

		List<Map<String, Object>> treeDetailes = getTreeDetails(tree_id);

		if (treeDetailes == null || treeDetailes.isEmpty()) {
			throw new NoDataException("check the tree_id ");
		}

		Map<String, Object> mapTReeDetailes = treeDetailes.get(0);

		if (mapTReeDetailes.containsKey("No_of_trees_required")) {
			int treeId1 = (int) mapTReeDetailes.get("tree_Id");
			int treeNumber = (int) mapTReeDetailes.get("No_of_trees_required"); // total
			int trerequired = (int) mapTReeDetailes.get("No_of_trees"); // add tree count
			String zone = (String) mapTReeDetailes.get("tree_zone");
			String ward = (String) mapTReeDetailes.get("tree_ward");
			int treeflag = 1;

			// System.out.println("Tree ID: " + treeId1);
			// System.out.println("Number of Trees Planted: " + treeNumber);
			// System.out.println("Number of Trees Required: " + trerequired);

			int trerequired1 = ((Number) mapTReeDetailes.get("No_of_trees")).intValue();
			String treeStatus = null;

			if (treeNumber > trerequired) {

				try {
					Random random = new Random();
					// Generate a random number between 1000 and 9999
					int treeId2 = 1000 + random.nextInt(9000);
					String saveImage = fileUpload("TreeImage", Integer.toString(treeId2), tree_Image);
					SaveTreeImage(treeId1, saveImage, userId, zone, ward, latitude, longitude, treeflag);
					updateTreeRequired(treeId1, trerequired1 + 1);

					if (treeNumber - 1 == trerequired) {
						treeStatus = "completed";
					} else {
						treeStatus = "pending";
					}
					return updateTreeStatusAndGetDetails(treeId1, treeStatus);
				} catch (Exception e) {
					throw new NoDataException("Error in Saving image");
				}

			}
//			if (treeNumber == trerequired -1 ) {
//
//				treeStatus = "completed";
//				return updateTreeStatusAndGetDetails(treeId1, treeStatus);
//			}

		}
		return null;
	}

	private Map<String, Object> updateTreeStatusAndGetDetails(int treeId1, String treeStatus) {
		String updateSql = "UPDATE treeplaintandpruning SET tree_Status = ? WHERE tree_Id = ?";
		String selectSql = "SELECT * FROM treeplaintandpruning WHERE tree_Id = ?";

		try {
			// Update the tree status
			int rowsAffected = jdbcParkseTemplate.update(updateSql, treeStatus, treeId1);

			if (rowsAffected > 0) {
				// Fetch the updated tree details after the update
				return jdbcParkseTemplate.queryForMap(selectSql, treeId1);
			} else {
				return null; // Return null if no row was updated
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null; // Return null in case of an error
		}
	}

	private Map<String, Object> updateTreeRequired(int treeId1, int i) {
		String updateSql = "UPDATE treeplaintandpruning SET No_of_trees = ? WHERE tree_Id = ?";
		String selectSql = "SELECT * FROM treeplaintandpruning WHERE tree_Id = ?";

		try {
			// Update the tree status
			int rowsAffected = jdbcParkseTemplate.update(updateSql, i, treeId1);

			if (rowsAffected > 0) {
				// Fetch the updated tree details after the update
				return jdbcParkseTemplate.queryForMap(selectSql, treeId1);
			} else {
				return null; // Return null if no row was updated
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null; // Return null in case of an error
		}
	}

	private List<Map<String, Object>> getTreeDetails(int tree_id) {
		String sql = "SELECT * FROM treeplaintandpruning WHERE tree_Id = ?";

		try {
			// Query for list with parameter treeId
			return jdbcParkseTemplate.queryForList(sql, tree_id);
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList(); // Return an empty list in case of an error
		}
	}

	// ------------------------------------------Enumeration Report
	// API-----------------------------------------------------
	/* 23-11-2024 Patch
	public List<Map<String, Object>> getZoneCount(String fromdate, String todate, String enumeration_type)
			throws Exception {
		String sql = "SELECT zone,count(enumeration_Id) as count FROM enumeration_master"
				+ "	where enumeration_type=(?) and DATE(cdate) BETWEEN STR_TO_DATE('" + fromdate  + "', '%d-%m-%Y') "
				+ "AND STR_TO_DATE('" + todate + "', '%d-%m-%Y') group by zone ";
		
		
		return jdbcParkseTemplate.queryForList(sql, new Object[] { enumeration_type});

	}
	*/
	public List<Map<String, Object>> getZoneCount(String fromdate, String todate, String enumeration_type, String userId )
			throws Exception {
		/*
		StringBuilder sql = new StringBuilder("SELECT zone,count(enumeration_Id) as count FROM enumeration_master"
				+ "	where enumeration_type=(?) and DATE(cdate) BETWEEN STR_TO_DATE(? , '%d-%m-%Y') "
				+ "AND STR_TO_DATE(? , '%d-%m-%Y') ");
		*/
		StringBuilder sql = new StringBuilder("SELECT `feedback_zone` as zone ,count(activity_id) as count FROM activity"
				+ "	where feedback_enumeration_Type=(?) and DATE(cdate) BETWEEN STR_TO_DATE(? , '%d-%m-%Y') "
				+ "AND STR_TO_DATE(? , '%d-%m-%Y') ");
		
		List<Object> params = new ArrayList<>();
	    params.add(enumeration_type);
	    params.add(fromdate);
	    params.add(todate);
	    
//	    if (userId != null) {
//	        sql.append(" AND UserId = ?");
//	        params.add(userId);
//	    }
	    
	    sql.append(" GROUP BY zone");
				
		return jdbcParkseTemplate.queryForList(sql.toString(), params.toArray());

	}

	public List<Map<String, Object>> getwardCount(String zone, String fromdate, String todate, String enumeration_type, 
			String userId) {
		
		StringBuilder sql = new StringBuilder(" SELECT ward,count(activity_id) as count  FROM `activity` "
				+ "where feedback_zone=(?) and date(cdate) between STR_TO_DATE(? , '%d-%m-%Y') "
				+ "and STR_TO_DATE(? , '%d-%m-%Y') And `feedback_enumeration_Type` =(?)");
		
		List<Object> params = new ArrayList<>();
		params.add(zone);
		params.add(fromdate);
		params.add(todate);
		params.add(enumeration_type);
		
//		if (userId != null) {
//	        sql.append(" AND UserId = ?");
//	        params.add(userId);
//	    }
		
		sql.append(" group by ward");
		
		return jdbcParkseTemplate.queryForList(sql.toString(), params.toArray());

	}


	public List<Map<String, Object>> getActivityCount(String ward, String fromdate, String todate,
			String enumeration_type, String zone, String userId) {		
		
		
		StringBuilder sql = new StringBuilder("SELECT "
				+ "    act.feedback_enumeration_Id AS feedback_enumeration_Id, "
				+ "    act.ward, "
				+ "    COALESCE(em.park_name, em.location) AS location_Park, "
				+ "    COUNT(act.feedback_enumeration_Id) AS count "
				+ "FROM "
				+ "    activity act "
				+ "LEFT JOIN "
				+ "    enumeration_master em "
				+ "    ON em.enumeration_Id = act.feedback_enumeration_Id "
				+ "WHERE "
				+ "    act.ward = ? "
				+ "    AND act.feedback_zone = ? "
				+ "    AND DATE(act.cdate) BETWEEN STR_TO_DATE('"+fromdate+"', '%d-%m-%Y') "
				+ "    AND STR_TO_DATE('"+todate+"', '%d-%m-%Y') "
				+ "    AND act.`feedback_enumeration_Type` = ? "
				+ "GROUP BY "
				+ "    act.feedback_enumeration_Id, act.ward, em.location");
		
		List<Object> params = new ArrayList<>();
		params.add(ward);
		params.add(zone);
		params.add(enumeration_type);
		
		return jdbcParkseTemplate.queryForList(sql.toString(), params.toArray());

	}

	public List<Map<String, Object>> getActivityDetails(String ward, String fromdate, String todate,
			String enumeration_type,  String enumerationId, String zone, String userId ) {
		
		StringBuilder sql = new StringBuilder( "SELECT act.feedback_enumeration_Id,act.activity_id, DATE_FORMAT(act.cdate, '%d-%m-%Y %l:%i %p') AS date, "
				+ "act.feedback_latitude, act.feedback_longitude, et.enumerationName FROM activity act "
				+ "LEFT JOIN enumerationtype et ON et.enumeration_type_id = act.feedback_enumeration_Type "
				+ "WHERE act.ward = ?  and act.feedback_zone = ? AND act.feedback_enumeration_Type = ? "
				+ "AND DATE(act.cdate) BETWEEN STR_TO_DATE(? , '%d-%m-%Y') AND STR_TO_DATE(? , '%d-%m-%Y') "
				+ "and act.feedback_enumeration_Id = ?");
		
		List<Object> params = new ArrayList<>();
		params.add(ward);
		params.add(zone);
		params.add(enumeration_type);
		params.add(fromdate);
		params.add(todate);
		params.add(enumerationId);
		
		
//		if (userId != null) {
//	        sql.append(" and act.user_Id = ?");
//	        params.add(userId);
//	    }
		
		sql.append(" GROUP BY act.feedback_enumeration_Id,act.activity_id, act.feedback_latitude, act.feedback_longitude, et.enumerationName");

		return jdbcParkseTemplate.queryForList(sql.toString(),params.toArray());

	}

	public List<List<Map<String, Object>>> getInspectionDetails(String activityId, String fromdate, String todate,
			String enumerationId) {

		// Initialize the list that will hold all park details
		List<List<Map<String, Object>>> getEnumDetails1 = new ArrayList<>();

		// Initialize a list to hold individual park details
		List<Map<String, Object>> getParkDetails = new ArrayList<>();

		// Fetch images and feedback
		List<Map<String, Object>> feedback = getEnumfeedback(activityId);
		List<Map<String, Object>> image = getEnumImage(enumerationId);

		// Prepare maps for images and feedback
		Map<String, Object> imageMap = new HashMap<>();
		imageMap.put("images", image);

		Map<String, Object> feedbackMap = new HashMap<>();
		feedbackMap.put("feedbacks", feedback);

		// Add ward details, images, and feedback to the park details list
		getParkDetails.add(feedbackMap);
		getParkDetails.add(imageMap);

		// Add this park's details to the final list
		getEnumDetails1.add(getParkDetails);

		return getEnumDetails1;
	}

	private List<Map<String, Object>> getEnumfeedback(String activityId) {
		String sql = "select eqm.qa_name, fb.feedback_Name from feedback fb "
				+ "left join enu_question_master eqm on eqm.qa_id = fb.feedback_qa_id "
				+ "left join activity act on act.activity_id = fb.activity_id "
				+ "where fb.activity_id =?";

		return jdbcParkseTemplate.queryForList(sql, new Object[] { activityId });
	}

	private List<Map<String, Object>> getEnumImage(String enumerationId) {

		String sql = "select *,coalesce(em.park_name,em.location) as parkName, " + "CONCAT('" + fileBaseUrl
				+ "/gccofficialapp/files', eimg.photos_enu) AS imageUrl, em.latitude, em.longitude from enumertion_images eimg "
				+ "left join enumeration_master em on em.enumeration_Id = eimg.enumerationImg_Id_master "
				+ "where eimg.enumerationImg_Id_master = ?";
		return jdbcParkseTemplate.queryForList(sql, new Object[] { enumerationId });
	}

	public List<Map<String, Object>> getParkCount(String ward, String fromdate, String todate,
			String enumeration_type) {
		String sql = "SELECT CASE WHEN park_name IS NULL THEN location ELSE park_name END AS named, "
				+ "COUNT(enumeration_Id) AS count, " + "DATE_FORMAT(cdate, '%d-%m-%Y') AS formatted_date "
				+ "FROM enumeration_master "
				+ "WHERE ward = ? AND DATE(cdate) BETWEEN STR_TO_DATE('" + fromdate + "', '%d-%m-%Y') AND "
				+ "STR_TO_DATE('" + todate + "', '%d-%m-%Y') And enumeration_type =(?) GROUP BY named, cdate, ward";

		return jdbcParkseTemplate.queryForList(sql, new Object[] { ward, enumeration_type });
	}

	public List<Map<String, Object>> getWishtreeZoneCount(String fromdate, String todate, String enumeration_type) {

		String sql = "SELECT "
				+ "    tree_zone,"
				+ "    COUNT(tree_id) AS count,"
				+ "    SUM(No_of_trees_required) AS assigned,"
				+ "    SUM(No_of_trees) AS completed,"
				+ "    (SUM(No_of_trees_required) - SUM(No_of_trees)) AS pending "
				+ "FROM "
				+ "    treeplaintandpruning "
				+ "	where tree_enumeration_id=(?) and DATE(tree_cdate) between STR_TO_DATE('" + fromdate + "', '%d-%m-%Y') "
				+ "and STR_TO_DATE('" + todate + "', '%d-%m-%Y') group by tree_zone Order By tree_zone";
		return jdbcParkseTemplate.queryForList(sql, new Object[] { enumeration_type});

	}

	public List<Map<String, Object>> getTreewardCount(String zone, String fromdate, String todate,
			String enumeration_type) {
		String sql = "SELECT "
				+ "    tree_ward,"
				+ "    COUNT(tree_id) AS count,"
				+ "    SUM(No_of_trees_required) AS assigned,"
				+ "    SUM(No_of_trees) AS completed,"
				+ "    (SUM(No_of_trees_required) - SUM(No_of_trees)) AS pending "
				+ "FROM "
				+ "    treeplaintandpruning "
				+ "WHERE tree_zone = ? AND DATE(tree_cdate) BETWEEN STR_TO_DATE('" + fromdate + "', '%d-%m-%Y') AND "
				+ "STR_TO_DATE('" + todate + "', '%d-%m-%Y')and tree_enumeration_id=(?) GROUP BY tree_ward Order By tree_ward";

		return jdbcParkseTemplate.queryForList(sql, new Object[] { zone, enumeration_type });
	}

	public List<Map<String, Object>> getwardWishTreedata(String ward) {
		String sql = "SELECT tree.tree_Id,tree.No_of_trees_required,tree.No_of_trees,tree.tree_enumeration_id,tree.tree_zone,tree.tree_ward,"
				+ "CONCAT('" + fileBaseUrl
				+ "/gccofficialapp/files', image.tr_image) AS imageUrl, "
				+ "    SUM(tree.No_of_trees_required) AS assigned,"
				+ "    SUM(tree.No_of_trees) AS completed,"
				+ "    (SUM(tree.No_of_trees_required) - SUM(tree.No_of_trees)) AS pending "
				+ "FROM treeplaintandpruning AS tree "
				+ "LEFT JOIN tree_image AS image ON image.tree_enumeration_id = tree.tree_Id WHERE tree.tree_ward =? ";
		return jdbcParkseTemplate.queryForList(sql, new Object[] { ward });
	}
	
	public List<Map<String, Object>> getTreeWardWiseCountDetails(String ward, String enumeration_type, String fromdate,String todate) {
		String sql = "select  tree_Id, tree_ward, coalesce(tr_parkname,tree_location) as location, count(tree_Id) as count, "
				+ "    SUM(No_of_trees_required) AS assigned,"
				+ "    SUM(No_of_trees) AS completed,"
				+ "    (SUM(No_of_trees_required) - SUM(No_of_trees)) AS pending "
				+ "FROM treeplaintandpruning "
				+ "where tree_ward=? and tree_enumeration_id = ? and date(tree_cdate) between STR_TO_DATE('" + fromdate + "', '%d-%m-%Y') and "
				+ "STR_TO_DATE('" + todate + "', '%d-%m-%Y') group by tree_Id, tree_ward, location";

		return jdbcParkseTemplate.queryForList(sql, new Object[] { ward, enumeration_type, });
	}
	
	public List<Map<String, Object>> getTreeImageDetails(String tree_Id, String enumeration_type, String fromdate, String todate) {
		String sql = "SELECT timg.tree_latitude, timg.tree_longitude, DATE_FORMAT(timg.tree_cdate, '%d-%m-%Y %l:%i %p') AS Date, "
				+ "tpp.No_of_trees_required AS total, tpp.No_of_trees AS Completed_Count, "
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', timg.tr_image) AS imageUrl "
				+ "FROM tree_image timg LEFT JOIN treeplaintandpruning tpp ON tpp.tree_id = timg.tree_enumeration_id "
				+ "WHERE tpp.tree_Id = ? AND tpp.tree_enumeration_id = ? AND DATE(timg.tree_cdate) BETWEEN STR_TO_DATE('" + fromdate + "', '%d-%m-%Y') "
				+ "AND STR_TO_DATE('" + todate + "', '%d-%m-%Y') and timg.tree_flag = 1";
		return jdbcParkseTemplate.queryForList(sql, new Object[] { tree_Id, enumeration_type });
	}
	
	
	public List<Map<String, Object>> getTreeDetails(String tree_Id) {
		String sql = "SELECT * FROM treeplaintandpruning WHERE tree_Id = ?";
		return jdbcParkseTemplate.queryForList(sql, new Object[] { tree_Id });
	}

	public List<Map<String, Object>> getTreeDetailsBasedonLocation(String parkName, String location, String ward) {
		String sql = "SELECT  *," + "CONCAT('" + fileBaseUrl + "/gccofficialapp/files/', image.tr_image) AS imageUrl "
				+ "FROM  treeplaintandpruning treep left join tree_image image on image.tree_enumeration_id=treep.tree_Id  "
				+ "WHERE treep.tree_ward=? and treep.tr_parkname=? or treep.tree_location =? ";

		try {
			return jdbcParkseTemplate.queryForList(sql, new Object[] { ward, parkName, location });
		} catch (Exception e) {
			throw new NoDataException("Exception" + e);
		}

	}

	public List<Map<String, Object>> getEnumeration_feedback(String enumerationId, String enumeration_type) {
		String sql = "SELECT  qa.qa_name,feed.feedback_Name, feed.activity_id, act.feedback_enumeration_Id, act.feedback_enumeration_Type "
				+ "FROM feedback feed " + "LEFT JOIN activity act ON act.activity_id = feed.activity_id "
				+ "LEFT JOIN enu_question_master qa ON feed.feedback_qa_id = qa.qa_id "
				+ "WHERE act.feedback_enumeration_Id = ? AND act.feedback_enumeration_Type = ?";

		try {
			return jdbcParkseTemplate.queryForList(sql, enumerationId, enumeration_type);
		} catch (Exception e) {
			throw new NoDataException("No enumeration feedback found for enumeration_Id: " + enumerationId);
		}
	}

	/* 23-11-2024 Patch
	public List<List<Map<String, Object>>> getParkDetails(String enumeration_type, String fromdate, String todate)
			throws Exception {
		// Fetch park data based on the given date range
		List<Map<String, Object>> parkData = getParkData(fromdate, todate, enumeration_type);

		// Initialize the list that will hold all park details
		List<List<Map<String, Object>>> getParkDetails1 = new ArrayList<>();

		// Loop through the parkData list (each map represents a row)
		for (Map<String, Object> row : parkData) {
			// Extract the enumeration_Id from the row
			Object enumId = row.get("enumeration_Id"); // Use the correct key from parkData
			System.out.println("enumId: " + enumId);
			if (enumId == null) {
				System.out.println("No enumeration_Id found in row.");
				continue;
			}

			// Initialize a list to hold individual park details
			List<Map<String, Object>> getParkDetails = new ArrayList<>();

			// Fetch ward count based on the enumeration_Id
			Map<String, Object> getwardCount = getEnumeration_master(enumId.toString(), enumeration_type);
			getwardCount.replaceAll((key, value) -> value == null ? "" : value);
			// System.out.println(getwardCount);

			// Fetch images and feedback
			List<Map<String, Object>> image = getEnumeration_image(enumId.toString(), enumeration_type);
			List<Map<String, Object>> feedback = getEnumeration_feedback(enumId.toString(), enumeration_type);

			// Prepare maps for images and feedback
			Map<String, Object> imageMap = new HashMap<>();
			imageMap.put("images", image);

			Map<String, Object> feedbackMap = new HashMap<>();
			feedbackMap.put("feedbacks", feedback);

			// Add ward details, images, and feedback to the park details list
			getParkDetails.add(getwardCount);
			getParkDetails.add(imageMap);
			getParkDetails.add(feedbackMap);

			// Add this park's details to the final list
			getParkDetails1.add(getParkDetails);
		}

		return getParkDetails1;
	}
*/
	public List<List<Map<String, Object>>> getParkDetails(String enumeration_type, String fromdate, String todate, String userId)
			throws Exception {
		// Fetch park data based on the given date range
		Map<String, Object> parkData = getParkData(fromdate, todate, enumeration_type, userId);

		// Initialize the list that will hold all park details
		List<List<Map<String, Object>>> getParkDetails1 = new ArrayList<>();

		// Loop through the parkData list (each map represents a row)
		
		// Extract the enumeration_Id from the row
		Object enumId = parkData.get("enumeration_Id"); // Use the correct key from parkData
		//System.out.println("enumId: " + enumId);
		if (enumId == null) {
			System.out.println("No enumeration_Id found in row.");
			//continue;
		}

		// Initialize a list to hold individual park details
		List<Map<String, Object>> getParkDetails = new ArrayList<>();

		// Fetch ward count based on the enumeration_Id
		Map<String, Object> getwardCount = getEnumeration_master(enumId.toString(), enumeration_type);
		getwardCount.replaceAll((key, value) -> value == null ? "" : value);
		// System.out.println(getwardCount);

		// Fetch images and feedback
		List<Map<String, Object>> image = getEnumeration_image(enumId.toString(), enumeration_type);
		//System.out.println("image: "+image.size());
		List<Map<String, Object>> feedback = getEnumeration_feedback(enumId.toString(), enumeration_type);
		//System.out.println("feedback: "+feedback.get(0));

		// Prepare maps for images and feedback
		Map<String, Object> imageMap = new HashMap<>();
		imageMap.put("images", image);

		Map<String, Object> feedbackMap = new HashMap<>();
		feedbackMap.put("feedbacks", feedback);

		// Add ward details, images, and feedback to the park details list
		getParkDetails.add(getwardCount);
		getParkDetails.add(imageMap);
		getParkDetails.add(feedbackMap);

		// Add this park's details to the final list
		getParkDetails1.add(getParkDetails);
		
		return getParkDetails1;
	}
	
	private Map<String, Object> getEnumeration_master(Object enumeration_Id, String enumeration_type) throws Exception {
		String sql = "SELECT * FROM enumeration_master WHERE enumeration_Id = ? and enumeration_type =?";

		try {
			return jdbcParkseTemplate.queryForMap(sql, new Object[] { enumeration_Id, enumeration_type });
		} catch (Exception e) {
			throw new NoDataException("No enumeration data found for enumeration_Id: " + enumeration_Id);
		}
	}

	public List<Map<String, Object>> getEnumeration_image(Object enumeration_Id, String enumeration_type)
			throws Exception {
		String sql = "SELECT *, CONCAT('" + fileBaseUrl + "/gccofficialapp/files/', photos_enu) AS imageUrl "
				+ "FROM enumertion_images WHERE enumerationImg_Id_master = ?";
		try {
			return jdbcParkseTemplate.queryForList(sql, new Object[] { enumeration_Id });
		} catch (Exception e) {
			throw new NoDataException("No enumeration image found for enumeration_Id: " + enumeration_Id);
		}
	}
	/* 23-11-2024 Patch
	private List<Map<String, Object>> getParkData(String fromdate, String todate, String enumeration_type) {
		String sql = "SELECT enumeration_Id "
				+ "FROM enumeration_master WHERE enumeration_type=? and DATE(cdate) BETWEEN STR_TO_DATE('" + fromdate + "', '%d-%m-%Y') AND "
				+ "STR_TO_DATE('" + todate + "', '%d-%m-%Y') and park_name IS NOT NULL";

		return jdbcParkseTemplate.queryForList(sql, new Object[] { enumeration_type});
	}
	*/
	private Map<String, Object> getParkData(String fromdate, String todate, String enumeration_type, String userId) {
		String sql = "SELECT enumeration_Id "
				+ "FROM enumeration_master WHERE enumeration_type=?  and UserId =? "
				+ "and DATE(cdate) BETWEEN STR_TO_DATE(? , '%d-%m-%Y') AND "
				+ "STR_TO_DATE(? , '%d-%m-%Y') and park_name IS NOT NULL";

		return jdbcParkseTemplate.queryForMap(sql, enumeration_type, userId, fromdate, todate);
	}

	public List<Map<String, Object>> getBriefReport(String fromdate, String todate, String category, String divison,
			String zone, String park_name) {
		String sql = "select park_name, latitude, longitude, location, zone from enumeration_master em "
				+ "where DATE(em.cdate) between STR_TO_DATE('" + fromdate + "', '%d-%m-%Y') and STR_TO_DATE('" + todate + "', '%d-%m-%Y') "
				+ "and enumeration_type =? and ward = ? and zone = ? and park_name = ? order by em.cdate ";

		return jdbcParkseTemplate.queryForList(sql, category, divison, zone, park_name);
	}

	public List<Map<String, Object>> getUserBasedEnum(String tree_user_id, String tree_latitude, String tree_longitude,
			String tree_Status, String enumeration_type) {
		String sql = "SELECT  *,date_format(treep.tree_cdate, '%d-%m-%Y %l:%i %p')as tree_date, COALESCE(treep.tr_parkname,treep.tree_location) AS tr_location,"
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', image.tr_image) AS imageUrl "
				+ "			FROM  treeplaintandpruning treep left join tree_image image on image.tree_enumeration_id=treep.tree_Id  WHERE tree_flag=0 and  tree_Status = ? and "
				+ "		((6371008.8 * acos(  ROUND(cos(radians(?)) * cos(radians(treep.tree_latitude)) * cos(radians(treep.tree_longitude) - radians(?)) + sin(radians(?)) * sin(radians(treep.tree_latitude)), 9)   )) < 500) "
				+ "      and treep.tree_user_id = ? and treep.tree_enumeration_id=?";

		return jdbcParkseTemplate.queryForList(sql,
				new Object[] { tree_Status, tree_latitude, tree_longitude, tree_latitude, tree_user_id, enumeration_type });

	}

	private List<Map<String, Object>> getByLocation(String latitude, String longitude, String latitude1) {
		String sql = "SELECT  enutype.enumeration_type_id , enutype.enumerationName, COALESCE(ac.park_name, ac.location) AS park_name "
				+ "FROM  treeplaintandpruning    WHERE  tree_Status = 'pending' and "
				+ " ((6371008.8 * acos(  ROUND(cos(radians(?)) * cos(radians(ac.latitude)) * cos(radians(ac.longitude) - radians(?)) +   sin(radians(?)) * sin(radians(ac.latitude)), 9)   )) < 500)"
				+ " GROUP BY enutype.enumeration_type_id,enutype.enumerationName, ac.park_name, ac.location ";

		// Use latitude, longitude, and schoolType as positional parameters
		return jdbcParkseTemplate.queryForList(sql, new Object[] { latitude, longitude, latitude1 });
	}

	public List<Map<String, Object>> getUserBasedEnum(String tree_latitude, String tree_longitude, String tree_Status) {
		String sql = "SELECT  * "
				+ "			FROM  treeplaintandpruning treep left join tree_image image on image.tree_enumeration_id = treep.tree_Id  WHERE tree_flag = 0 and  tree_Status = ? and "
				+ "		((6371008.8 * acos(  ROUND(cos(radians(?)) * cos(radians(treep.tree_latitude)) * cos(radians(treep.tree_longitude) - radians(?)) + sin(radians(?)) * sin(radians(treep.tree_latitude)), 9)   )) < 500) ";

		// Use latitude, longitude, and schoolType as positional parameters
		return jdbcParkseTemplate.queryForList(sql,
				new Object[] { tree_Status, tree_latitude, tree_longitude, tree_latitude });

	}

	public List<Map<String, Object>> getparkNameByRadius(String tree_latitude, String tree_longitude) {
		String sql = " 	SELECT  `enumeration_Id`, `park_name`" + " FROM enumeration_master   WHERE "
				+ "		((6371008.8 * acos(  ROUND(cos(radians(?)) * cos(radians(latitude)) * cos(radians(longitude) - radians(?)) + "
				+ "        sin(radians(?)) * sin(radians(latitude)), 9)   )) < 500) and  park_name is not null";
		return jdbcParkseTemplate.queryForList(sql, new Object[] { tree_latitude, tree_longitude, tree_latitude });

	}
	
	// Added for Flowering and Non-Flowering enumeration
	
	public List<Map<String, Object>> getparkNameByEnumType(int EnumType, String tree_latitude, String tree_longitude) {
		String sql = " 	SELECT  enumeration_Id, COALESCE(park_name, location) AS park_name FROM enumeration_master WHERE enumeration_type = ? and "
				+ "		((6371008.8 * acos(  ROUND(cos(radians(?)) * cos(radians(latitude)) * cos(radians(longitude) - radians(?)) +\n"
				+ "        sin(radians(?)) * sin(radians(latitude)), 9)   )) < 500)";
		return jdbcParkseTemplate.queryForList(sql, EnumType, tree_latitude, tree_longitude, tree_latitude);

	}

	public int saveFlowringandNonFlowringDetails(int enumerationId, String enumType, String parkname, int floweringPlantCount, 
			MultipartFile floweringImg, int nonFloweringPlantCount, MultipartFile nonFloweringImg, String userId) throws Exception {
		
		int floweringImg_Id = saveFloweringandNonFloweringData(enumerationId, enumType,parkname, floweringPlantCount, nonFloweringPlantCount, userId);
		
		String name = "floweringImages " + floweringImg_Id;
		String imgId = Integer.toString(floweringImg_Id);
		
		
		try {
			if(floweringImg != null && !floweringImg.isEmpty())
			{
				String floweringImageUrl = fileUpload(name, imgId, floweringImg);
				saveFloweringandNonFloweringImage(floweringImg_Id, floweringImageUrl, "Flowering", userId);	
				System.out.println("===========................" + floweringImageUrl);
			}  
			if(nonFloweringImg != null && !nonFloweringImg.isEmpty())
			{
				String nonFloweringImageUrl = fileUpload(name, imgId, nonFloweringImg);
				saveFloweringandNonFloweringImage(floweringImg_Id, nonFloweringImageUrl, "NonFlowering", userId );
				System.out.println("===========................" + nonFloweringImageUrl );				
			}				
			return floweringImg_Id;
			
		} catch (Exception e) {
			throw new Exception("Error in image saving " + e);
		}		
		
	}

	private int saveFloweringandNonFloweringData(int enumerationId, String enumType, String parkname,int floweringPlantCount, 
			int nonFloweringPlantCount, String userId) {
		
		String sql ="Insert into floweringandnonflowering (enumeration_Id, flowering_enumtype, enumeration_type, park_name, flowering_plant_count, "
				+ "nonflowering_plant_count, userId) values(?, ?, ?, ?, ?, ?, ?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			jdbcParkseTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, new String[] { "Id" });
				ps.setInt(1, enumerationId); 
				ps.setInt(2, 12); 
				ps.setString(3, enumType); 
				ps.setString(4, parkname); 
				ps.setInt(5, floweringPlantCount); 
				ps.setInt(6, nonFloweringPlantCount); 
				ps.setString(7, userId); 
				return ps;

			}, keyHolder);
			// System.out.println("img id master:"+keyHolder.getKey().intValue());
			// Retrieve the auto-generated ID and return it
			return keyHolder.getKey().intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1; // Return error indicator
	}
	
	private String saveFloweringandNonFloweringImage(int floweringImg_Id, String floweringImageUrl, String status, String userId ) {
		
		try {
			String sqlQuery = "insert into flower_image (flowering_id, flowering_img, flower_status, userid) values (?, ?, ?, ?)";
			jdbcParkseTemplate.update(sqlQuery,floweringImg_Id, floweringImageUrl, status, userId);
			
			return "Success";			
		}catch(Exception e) {
			System.out.println("Exception:"+e);			
			return null;
		}	
	}


	public List<Map<String, Object>> getFloweringZoneCount(String enumeration_type, String fromdate, String todate,
	        String userId) {

	    StringBuilder sql = new StringBuilder("SELECT en.zone,COUNT(distinct(ff.enumeration_Id)) AS count, sum(ff.flowering_plant_count) as floweringCount, "
	    		+ "sum(ff.nonflowering_plant_count) as non_floweringCount FROM floweringandnonflowering ff "
	            + "LEFT JOIN enumeration_master en ON ff.enumeration_Id = en.enumeration_Id "
	            + "WHERE ff.enumeration_type = ? "
	            + "AND DATE(ff.created_date) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y')");
	    
	    if (userId != null && !userId.isEmpty()) {
	        sql.append("AND ff.userId = ? ");
	    }
	    sql.append("GROUP BY en.zone");
	    
	    List<Object> params = new ArrayList<>();
	    params.add(enumeration_type);
	    params.add(fromdate);
	    params.add(todate);

	    if (userId != null && !userId.isEmpty()) {
	        params.add(userId);
	    }

	    return jdbcParkseTemplate.queryForList(sql.toString(),params.toArray());
	}

	public List<Map<String, Object>> getFloweringWardCount(String zone, String fromdate, String todate,
			String enumeration_type, String userId) {
		
		StringBuilder sql = new StringBuilder(" SELECT en.zone, en.ward, count(distinct(ff.enumeration_Id)) as count, sum(ff.flowering_plant_count) as floweringCount, "
				+ "sum(ff.nonflowering_plant_count) as non_floweringCount FROM floweringandnonflowering ff "
				+ "left join enumeration_master en on ff.enumeration_Id = en.enumeration_Id "
				+ "where en.zone=? and date(ff.created_date) between STR_TO_DATE(? , '%d-%m-%Y') "
				+ "and STR_TO_DATE(? , '%d-%m-%Y') And ff.enumeration_type =? ");
				
				if (userId != null && !userId.isEmpty()) {
			        sql.append("AND ff.userId = ? ");
			    }
			    sql.append("GROUP BY en.ward");
			    
			    List<Object> params = new ArrayList<>();
			    params.add(zone);
			    params.add(fromdate);
			    params.add(todate);
			    params.add(enumeration_type);

			    if (userId != null && !userId.isEmpty()) {
			        params.add(userId);
			    }
		
		return jdbcParkseTemplate.queryForList(sql.toString(), params.toArray());
		
	}


	public List<Map<String, Object>> getFloweringDetails(String enumeration_type, String zone, String ward,
			String fromdate, String todate, String userId, String parkname) {
		
		StringBuilder sql = new StringBuilder("select et.enumerationName, em.zone, em.ward, ff.park_name as location, em.latitude, em.longitude, ff.flowering_plant_count, "
				+ "ff.nonflowering_plant_count, date_format(ff.created_date,'%d-%m-%Y %l:%i %p') as date, "
				+ "COALESCE(GROUP_CONCAT(case when fi.flower_status ='Flowering' then CONCAT('" + fileBaseUrl + "/gccofficialapp/files', fi.flowering_img) else null end),'') as floweringImg,"
				+ "COALESCE(GROUP_CONCAT(case when fi.flower_status ='NonFlowering' then CONCAT('" + fileBaseUrl + "/gccofficialapp/files', fi.flowering_img) else null end),'') as nonFloweringImg"
				+ " from floweringandnonflowering ff "
				+ "left join enumeration_master em on ff.enumeration_Id = em.enumeration_Id "
				+ "left join enumerationtype et on ff.enumeration_type = et.enumeration_type_id "
				+ "left join flower_image fi on ff.flowering_id = fi.flowering_id "
				+ "where ff.enumeration_type = ? and em.zone = ? and em.ward = ? and date(ff.created_date) between "
				+ "STR_TO_DATE(? , '%d-%m-%Y') and STR_TO_DATE(? , '%d-%m-%Y') and ff.park_name = ? ");
				
				if (userId != null && !userId.isEmpty()) {
			        sql.append("AND ff.userId = ? ");
			    }
				
			    sql.append("group by em.latitude,em.longitude, ff.flowering_plant_count, ff.nonflowering_plant_count, ff.created_date");
			    
			    List<Object> params = new ArrayList<>();
			    params.add(enumeration_type);
			    params.add(zone);
			    params.add(ward);
			    params.add(fromdate);
			    params.add(todate);
			    params.add(parkname);			    

			    if (userId != null && !userId.isEmpty()) {
			        params.add(userId);
			    }
		
		return jdbcParkseTemplate.queryForList(sql.toString(), params.toArray());
	}
	
	public List<Map<String, Object>> getParkwiseFloweringCount(String enumeration_type, String zone, String ward,String fromdate, String todate, 
			String userId) {
		
		StringBuilder sql = new StringBuilder("select ff.park_name, sum(ff.flowering_plant_count) as floweringCount, sum(ff.nonflowering_plant_count) as non_floweringCount "
				+ "from floweringandnonflowering ff "
				+ "left join enumeration_master em on ff.enumeration_Id = em.enumeration_Id "
				+ "where ff.enumeration_type = ? and em.zone = ? and em.ward = ? and "
				+ "date(ff.created_date) between STR_TO_DATE(? , '%d-%m-%Y') and STR_TO_DATE(? , '%d-%m-%Y') ");
				
				if (userId != null && !userId.isEmpty()) {
			        sql.append("AND ff.userId = ? ");
			    }
				
			    sql.append("group by ff.park_name");
			    
			    List<Object> params = new ArrayList<>();
			    params.add(enumeration_type);
			    params.add(zone);
			    params.add(ward);
			    params.add(fromdate);
			    params.add(todate);			    

			    if (userId != null && !userId.isEmpty()) {
			        params.add(userId);
			    }
		
		return jdbcParkseTemplate.queryForList(sql.toString(), params.toArray());
	}
	
    private void saveComplaintFeedback(String QuestionId, String Ans, int activityId, String zone, String ward, String latitude, String longitude, String userId) {
       
    	if (QuestionId != null && !QuestionId.isEmpty()) {

            String sql = "SELECT qa_name, raise_compliance, positive_ans "
            		+ "FROM enu_question_master "
            		+ "WHERE qa_id = ? "
            		+ "AND raise_compliance = 1 "
            		+ "AND positive_ans <> ?";

            try {
                Map<String, Object> result = jdbcParkseTemplate.queryForMap(sql, QuestionId,Ans);

                String qaName = (String) result.get("qa_name");
                
                String positiveAns = (String) result.get("positive_ans");

                String feedComplaint = qaName + ":" + Ans;

                if (qaName != null && !qaName.isEmpty() && !Ans.equals(positiveAns)) {

                    String saveComplaintQuery = "INSERT INTO feed_complaint (`feed_question`, `⁠feed_ans`, `activity_id`, `zone`, `ward`, `⁠latitude`, "
                    		+ "`⁠longitude`, `userId`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                    jdbcParkseTemplate.update(saveComplaintQuery, feedComplaint, Ans, activityId, zone, ward, latitude, longitude, userId);
                }
            } catch (EmptyResultDataAccessException e) {
                System.out.println("No record found for QA_ID: " + QuestionId);
            }
        }
    }

    public List<Map<String, Object>> getComplaintListforFeedback(String userid) {
    	String sqlQuery = "select fc.comp_id, fc.activity_id, et.enumerationName, fc.zone, fc.ward, "
    			+ "fc.`⁠latitude` AS latitude, fc.`⁠longitude` AS longitude, coalesce(em.park_name, em.location) as parkName, " +
                "fc.feed_question as complaintName, " +
                "date_format(fc.cdate,'%d-%m-%Y %l:%i %p') as complaintDate, " +
                "ifnull(fs.status,'Pending') as ReplyStatus, " +
                "ifnull(fs.remarks,'yet to be Resolved') as remarks, " +
                "ifnull(CONCAT('" + fileBaseUrl + "/gccofficialapp/files',fs.comp_img_path),'Image Not Uploaded') as ReplyImg, " +
                "ifnull(date_format(fs.cdate,'%d-%m-%Y %l:%i %p'),'Complaint yet to be resolved') as complaintClosedDate, " +
                "ifnull(fs.userId,'yet to be resolved') as closedBy "+
                "from feed_complaint fc " +
                "left join feed_complaint_status fs on fc.comp_id = fs.comp_id " +
                "left join activity ac on fc.activity_id = ac.activity_id " +
                "left join enumeration_master em on ac.feedback_enumeration_Id = em.enumeration_Id " +
                "left join enumerationtype et on em.enumeration_type = et.enumeration_type_id "+
                "where fc.userId =? and fc.is_active = 1 order by fc.cdate asc";

        return jdbcParkseTemplate.queryForList(sqlQuery, userid);
    }

    public List<Map<String, Object>> updateComplaintDetails(String compId, String remarks, MultipartFile compImg, String latitude, String longitude, String userId) throws Exception {

        String name = "feedback_Complaint";
        int comp_status_id = 0;

        try {
            if (compImg != null && !compImg.isEmpty()) {
                String imagePath = fileUpload(name, compId, compImg);
                System.out.println("Image Path: " + imagePath);
                comp_status_id = saveComplaintDetails(compId, remarks, imagePath, latitude, longitude, userId);
            } /*else {
                comp_status_id = saveComplaintDetails(compId, remarks, null, userId);
            }*/

            if (comp_status_id > 0) {
                String fetchQuery = "select fc.comp_id, fc.activity_id, fc.feed_question as complaintName, date_format(fc.cdate,'%d-%m-%Y %l:%i %p') as complaintDate, " +
                        "ifnull(fs.status,'Pending') as ReplyStatus, " +
                        "ifnull(fs.remarks,'yet to be Resolved') as remarks, " +
                        "ifnull(CONCAT('" +fileBaseUrl+ "/gccofficialapp/files',fs.comp_img_path),'Image Not Uploaded') as ReplyImg, " +
                        "ifnull(date_format(fs.cdate,'%d-%m-%Y %l:%i %p'),'Complaint yet to be resolved') as complaintClosedDate, " +
                        "ifnull(fs.userId,'yet to be resolved') as closedBy " +
                        "from feed_complaint fc " +
                        "left join feed_complaint_status fs on fc.comp_id = fs.comp_id " +
                        "where fc.comp_id =? and fc.is_active = 1";
                return jdbcParkseTemplate.queryForList(fetchQuery, compId);
            }

        } catch (Exception e) {
            throw new Exception("Error in image saving: " + e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    
    private int saveComplaintDetails(String compId, String remarks, String imagePath, String latitude, String longitude, String userId) {
    	
    	System.out.println("saveComplaintDetails: \n"
    			+ "compId:"+compId
    			+ "\nremarks:"+remarks
    			+ "\nimagePath:"+imagePath
    			+ "\nlatitude:"+latitude
    			+ "\nlongitude:"+longitude
    			+ "\nuserId:"+userId
    			);
    	
        String sqlQuery = "INSERT INTO feed_complaint_status "
        		+ "(comp_id, status, remarks, comp_img_path, ⁠latitude, "
        		+ "longitude, userId) "
        		+ "VALUES (?, ?, ?, ?, ?, ?, ?)";

        String status = "Completed";

        return jdbcParkseTemplate.update(sqlQuery, compId, status, remarks, imagePath, latitude, longitude, userId);
    }

    public boolean confirmLocationforComplaint(String compId, String latitude, String longitude) {

        String sqlQuery ="select `comp_id` from `feed_complaint` where "
        		+ "`comp_id` = ? "
        		+ "AND ((6371008.8 * acos(ROUND(cos(radians(?)) * cos(radians(`⁠latitude`)) * cos(radians(`⁠longitude`) - radians(?)) + sin(radians(?)) * sin(radians(`⁠latitude`)), 9))) < 500)";
        Integer count = jdbcParkseTemplate.queryForObject(sqlQuery, Integer.class, compId, latitude, longitude, latitude);

        return count != null && count > 0;
    }
    
    public String getZoneByLoginId(String loginid, String type) {
        String sqlQuery = "SELECT `zone` FROM gcc_penalty_hoardings.hoading_user_list WHERE userid = ? AND type = ? LIMIT 1";

        // Query the database using queryForList
        List<Map<String, Object>> results = jdbcParkseTemplate.queryForList(sqlQuery, loginid, type);


        // Check if results is not empty and extract the ward value
        if (!results.isEmpty()) {
            System.out.println("zone....."+results);
            // Extract the ward value from the first result
            return (String) results.get(0).get("zone");
        }

        // Handle the case where no result is found
        return "00";  // or return null based on your needs
    }

    public List<Map<String, Object>> getZoneComplaintListforFeedback(String loginid) {
    	
    	String zone = getZoneByLoginId(loginid,"osr");

        String sqlQuery = "select fc.comp_id, fc.activity_id, et.enumerationName, fc.zone, "
        		+ "fc.ward, fc.`⁠latitude` AS latitude, fc.`⁠longitude` AS longitude, coalesce(em.park_name, em.location) as parkName, "
        		+ "fc.feed_question as complaintName, date_format(fc.cdate,'%d-%m-%Y %l:%i %p') as complaintDate, "
        		+ "ifnull(fs.status,'Pending') as ReplyStatus, ifnull(fs.remarks,'yet to be Resolved') as remarks, "
        		+ "ifnull(CONCAT('https://gccservices.in/gccofficialapp/files',fs.comp_img_path),'Image Not Uploaded') as ReplyImg, "
        		+ "ifnull(date_format(fs.cdate,'%d-%m-%Y %l:%i %p'),'Complaint yet to be resolved') as complaintClosedDate, "
        		+ "ifnull(fs.userId,'yet to be resolved') as closedBy "
        		+ "from feed_complaint fc "
        		+ "left join feed_complaint_status fs on fc.comp_id = fs.comp_id "
        		+ "left join activity ac on fc.activity_id = ac.activity_id "
        		+ "left join enumeration_master em on ac.feedback_enumeration_Id = em.enumeration_Id "
        		+ "left join enumerationtype et on em.enumeration_type = et.enumeration_type_id "
        		+ "where fc.zone =? and fc.is_active = 1 and fs.status is null order by fc.cdate asc";

        return jdbcParkseTemplate.queryForList(sqlQuery, zone);
    }

}
