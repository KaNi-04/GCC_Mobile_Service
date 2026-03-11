package in.gov.chennaicorporation.mobileservice.ie_complaints.service;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.DataSource;
import org.slf4j.Logger;

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
import java.util.stream.Collectors;

import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.jdbc.core.PreparedStatementCreator;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;
import in.gov.chennaicorporation.mobileservice.nulm.service.DocumentUploadService;

@Service("iecomplaintservice")
public class ieComplaintService {

	private static final Logger logger = LoggerFactory.getLogger(ieComplaintService.class);

	private JdbcTemplate jdbcTemplate;

	private final Environment environment;
	private String fileBaseUrl;

	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final int STRING_LENGTH = 15;
	private static final Random RANDOM = new SecureRandom();

	@Autowired
	public void setDataSource(@Qualifier("mysqlIEComplaintsDataSource") DataSource IEComplaintsDataSource) {
		this.jdbcTemplate = new JdbcTemplate(IEComplaintsDataSource);
	}

	@Autowired
	public ieComplaintService(Environment environment) {
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
		String serviceFolderName = environment.getProperty("ie_complaint_foldername");
		var year = DateTimeUtil.getCurrentYear();
		var month = DateTimeUtil.getCurrentMonth();
		var date = DateTimeUtil.getCurrentDay();

		uploadDirectory = uploadDirectory + serviceFolderName + year +
				"/" + month;

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
			String fileName = name + "_" + id + "_" + datetimetxt + "_" + file.getOriginalFilename();
			fileName = fileName.replaceAll("\\s+", "");

			String filePath = uploadDirectory + "/" + fileName;

			String filepath_txt = "/" + serviceFolderName + year + "/" +
					month + "/"
					+ fileName;

			// Create a new Path object
			Path path = Paths.get(filePath);

			// Get the bytes of the file
			byte[] bytes = file.getBytes();

			// Compress the image
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50%quality

			// Write the bytes to the file
			Files.write(path, bytes);

			System.out.println(filePath);
			return filepath_txt;

		} catch (IOException e) {
			e.printStackTrace();
			return "Failed to save file " + file.getOriginalFilename();
		}
	}

	/*
	 * @Transactional
	 * public List<Map<String, Object>> bovbinscomplaint(
	 * String zone,
	 * String ward,
	 * String street_name,
	 * String latitude,
	 * String longitude,
	 * String cby,
	 * String vechileno,
	 * MultipartFile bovimage,
	 * String remarks) {
	 * List<Map<String, Object>> result = null;
	 * Map<String, Object> response = new HashMap<>();
	 * // int lastInsertId = 0;
	 * 
	 * String image = "";
	 * 
	 * // Handle file upload if a file is provided
	 * if (bovimage != null && !bovimage.isEmpty()) {
	 * var year = DateTimeUtil.getCurrentYear();
	 * var month = DateTimeUtil.getCurrentMonth();
	 * image = fileUpload("bov bins", "0", bovimage);
	 * }
	 * 
	 * String storeIMG = image;
	 * 
	 * String sqlQuery = "INSERT INTO `bov_bins_complaint`"
	 * +
	 * "(`zone`, `ward`, `street_name`, `latitude`, `longitude`, `cby`, `vechile_no`, `image_path`, `remarks`) "
	 * + "VALUES (?,?,?,?,?,?,?,?,?)";
	 * 
	 * KeyHolder keyHolder = new GeneratedKeyHolder();
	 * 
	 * try {
	 * 
	 * int affectedRows = jdbcTemplate.update(new PreparedStatementCreator() {
	 * 
	 * @Override
	 * public PreparedStatement createPreparedStatement(Connection connection)
	 * throws SQLException {
	 * PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] {
	 * "id" });
	 * ps.setString(1, zone);
	 * ps.setString(2, ward);
	 * ps.setString(3, street_name);
	 * ps.setString(4, latitude);
	 * ps.setString(5, longitude);
	 * ps.setString(6, cby);
	 * ps.setString(7, vechileno);
	 * ps.setString(8, storeIMG);
	 * ps.setString(9, remarks);
	 * return ps;
	 * }
	 * }, keyHolder);
	 * 
	 * if (affectedRows > 0) {
	 * int generatedId = keyHolder.getKey().intValue();
	 * String prefix =
	 * LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
	 * String refId = prefix + generatedId;
	 * 
	 * String updateSql = "UPDATE bov_bins_complaint SET ref_id = ? WHERE id = ?";
	 * jdbcTemplate.update(updateSql, refId, generatedId);
	 * 
	 * response.put("insertId", generatedId);
	 * response.put("status", "success");
	 * response.put("message",
	 * "A new Bov Bins Complaint was inserted successfully!");
	 * System.out.
	 * println("A new Bov Bins Complaint was inserted successfully! Insert ID: " +
	 * generatedId);
	 * } else {
	 * response.put("status", "error");
	 * response.put("message", "Failed to insert a new Bov Bins Complaint.");
	 * }
	 * } catch (DataAccessException e) {
	 * System.out.println("Data Access Exception:");
	 * Throwable rootCause = e.getMostSpecificCause();
	 * if (rootCause instanceof SQLException) {
	 * SQLException sqlException = (SQLException) rootCause;
	 * System.out.println("SQL State: " + sqlException.getSQLState());
	 * System.out.println("Error Code: " + sqlException.getErrorCode());
	 * System.out.println("Message: " + sqlException.getMessage());
	 * response.put("status", "error");
	 * response.put("message", sqlException.getMessage());
	 * response.put("sqlState", sqlException.getSQLState());
	 * response.put("errorCode", sqlException.getErrorCode());
	 * } else {
	 * System.out.println("Message: " + rootCause.getMessage());
	 * response.put("status", "error");
	 * response.put("message", rootCause.getMessage());
	 * }
	 * }
	 * 
	 * return Collections.singletonList(response);
	 * }
	 */

	// equipmentmaster
	/*
	 * public List<Map<String, Object>> getEquipmentList() {
	 * 
	 * String sql =
	 * " SELECT equipment_id,equipment_name FROM equipmentmaster WHERE is_delete = 0 AND is_active = 1 "
	 * ;
	 * 
	 * return jdbcTemplate.queryForList(sql);
	 * }
	 */

	public List<Map<String, Object>> getEquipmentList() {

		String methodName = "getEquipmentList";

		try {

			String sql = "SELECT equipment_id, equipment_name FROM equipmentmaster WHERE is_delete = 0 AND is_active = 1";

			return jdbcTemplate.queryForList(sql);

		} catch (Exception e) {

			System.err.println("Exception in Method : " + methodName);
			System.err.println("Date & Time        : " + LocalDateTime.now());
			System.err.println("Error Message      : " + e.getMessage());

			e.printStackTrace(); // full stack trace

			throw new RuntimeException("Failed to fetch Equipment List", e);
		}
	}

	// save singleapi

	/*
	 * @Transactional
	 * public List<Map<String, Object>> saveComplaint(
	 * 
	 * Integer complaint_id,
	 * String zone,
	 * String ward,
	 * String street_name,
	 * String latitude,
	 * String longitude,
	 * String cby,
	 * String vechileno,
	 * // Long equipment_id,
	 * List<Long> equipment_id,
	 * List<Long> sweeping_id,
	 * // Integer area_id,
	 * // List<Long> equipment_check_id,
	 * String cleaningDetails,
	 * MultipartFile image,
	 * String remarks,
	 * String q_id) {
	 * 
	 * Map<String, Object> response = new HashMap<>();
	 * String imagePath = "";
	 * 
	 * try {
	 * 
	 * if (image != null && !image.isEmpty()) {
	 * imagePath = fileUpload("complaints", "0", image);
	 * }
	 * 
	 * String sqlQuery;
	 * String updateSql;
	 * String equipmentIds = null;
	 * String sweepingIds = null;
	 * String equipmentCheckIds = null;
	 * Integer areaId = null;
	 * 
	 * if (complaint_id == 1) {
	 * 
	 * if (vechileno == null || vechileno.isBlank()) {
	 * response.put("status", "error");
	 * response.put("message", "Vehicle number is required");
	 * return Collections.singletonList(response);
	 * }
	 * 
	 * sqlQuery = "INSERT INTO bov_bins_complaint " +
	 * "(zone, ward, street_name, latitude, longitude, cby, vechile_no, image_path, remarks,q_id) "
	 * +
	 * "VALUES (?,?,?,?,?,?,?,?,?,?)";
	 * 
	 * updateSql = "UPDATE bov_bins_complaint SET ref_id = ? WHERE id = ?";
	 * 
	 * } else if (complaint_id == 2) {
	 * 
	 * if (equipment_id == null) {
	 * response.put("status", "error");
	 * response.put("message", "Equipment ID is required");
	 * return Collections.singletonList(response);
	 * }
	 * 
	 * if (equipment_id != null && !equipment_id.isEmpty()) {
	 * equipmentIds = equipment_id.stream()
	 * .map(String::valueOf)
	 * .collect(Collectors.joining(","));
	 * }
	 * 
	 * sqlQuery = "INSERT INTO deployment_of_equipment " +
	 * "(zone, ward, street_name, latitude, longitude, cby, equipment_id, image_path, remarks,q_id) "
	 * +
	 * "VALUES (?,?,?,?,?,?,?,?,?,?)";
	 * 
	 * updateSql = "UPDATE deployment_of_equipment SET ref_id = ? WHERE id = ?";
	 * 
	 * } else if (complaint_id == 3) {
	 * 
	 * if (sweeping_id == null) {
	 * response.put("status", "error");
	 * response.put("message", "Sweeping ID is required");
	 * return Collections.singletonList(response);
	 * }
	 * 
	 * if (sweeping_id != null && !sweeping_id.isEmpty()) {
	 * sweepingIds = sweeping_id.stream()
	 * .map(String::valueOf)
	 * .collect(Collectors.joining(","));
	 * }
	 * 
	 * sqlQuery = "INSERT INTO sweeping_complaint " +
	 * "(zone, ward, street_name, latitude, longitude, cby, image_path, remarks,q_id) "
	 * +
	 * "VALUES (?,?,?,?,?,?,?,?,?)";
	 * 
	 * updateSql = "UPDATE sweeping_complaint SET ref_id = ? WHERE id = ?";
	 * 
	 * } else if (complaint_id == 4) {
	 * 
	 * sqlQuery = "INSERT INTO hotspot_complaint " +
	 * "(zone, ward, street_name, latitude, longitude, cby, image_path, remarks) " +
	 * "VALUES (?,?,?,?,?,?,?,?)";
	 * 
	 * updateSql = "UPDATE hotspot_complaint SET ref_id = ? WHERE id = ?";
	 * 
	 * } else if (complaint_id == 5) {
	 * 
	 * if (cleaningDetails == null || cleaningDetails.isBlank()) {
	 * response.put("status", "error");
	 * response.put("message", "Cleaning Details are required");
	 * return Collections.singletonList(response);
	 * }
	 * 
	 * ObjectMapper mapper = new ObjectMapper();
	 * List<Map<String, Object>> cleaningList;
	 * 
	 * try {
	 * cleaningList = mapper.readValue(
	 * cleaningDetails,
	 * new TypeReference<List<Map<String, Object>>>() {
	 * });
	 * } catch (Exception e) {
	 * response.put("status", "error");
	 * response.put("message", "Invalid cleaningDetails JSON format");
	 * return Collections.singletonList(response);
	 * }
	 * 
	 * // Since your table supports one row,
	 * // take first object
	 * Map<String, Object> item = cleaningList.get(0);
	 * 
	 * areaId = ((Number) item.get("areaId")).intValue();
	 * 
	 * List<?> equipmentList = (List<?>) item.get("equipmentCheckId");
	 * 
	 * equipmentCheckIds = equipmentList.stream()
	 * .map(obj -> String.valueOf(((Number) obj).intValue()))
	 * .collect(Collectors.joining(","));
	 * 
	 * sqlQuery = "INSERT INTO public_areas_cleaning " +
	 * "(zone, ward, street_name, latitude, longitude, cby, area_id, equipment_check_id, image_path, remarks, q_id) "
	 * +
	 * "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
	 * 
	 * updateSql = "UPDATE public_areas_cleaning SET ref_id = ? WHERE id = ?";
	 * } else {
	 * response.put("status", "error");
	 * response.put("message", "Invalid complaint type");
	 * return Collections.singletonList(response);
	 * }
	 * 
	 * // ✅ MAKE FINAL COPIES (Important Fix)
	 * final String finalSqlQuery = sqlQuery;
	 * final String finalUpdateSql = updateSql;
	 * final String finalImagePath = imagePath;
	 * final int finalComplaintType = complaint_id;
	 * final String finalEquipmentIds = equipmentIds;
	 * final String finalSweepingIds = sweepingIds;
	 * final Integer finalAreaId = areaId;
	 * final String finalEquipmentCheckIds = equipmentCheckIds;
	 * 
	 * KeyHolder keyHolder = new GeneratedKeyHolder();
	 * 
	 * int affectedRows = jdbcTemplate.update(new PreparedStatementCreator() {
	 * 
	 * @Override
	 * public PreparedStatement createPreparedStatement(Connection connection)
	 * throws SQLException {
	 * 
	 * PreparedStatement ps = connection.prepareStatement(finalSqlQuery, new
	 * String[] { "id" });
	 * 
	 * 
	 * 
	 * if (finalComplaintType == 4) {
	 * 
	 * ps.setString(1, zone);
	 * ps.setString(2, ward);
	 * ps.setString(3, street_name);
	 * ps.setString(4, latitude);
	 * ps.setString(5, longitude);
	 * ps.setString(6, cby);
	 * ps.setString(7, finalImagePath);
	 * ps.setString(8, remarks);
	 * 
	 * } else if (finalComplaintType == 5) {
	 * 
	 * ps.setString(1, zone);
	 * ps.setString(2, ward);
	 * ps.setString(3, street_name);
	 * ps.setString(4, latitude);
	 * 
	 * ps.setString(5, longitude);
	 * ps.setString(6, cby);
	 * 
	 * ps.setInt(7, finalAreaId);
	 * ps.setString(8, finalEquipmentCheckIds);
	 * ps.setString(9, finalImagePath);
	 * ps.setString(10, remarks);
	 * ps.setString(11, q_id);
	 * 
	 * } else {
	 * 
	 * ps.setString(1, zone);
	 * ps.setString(2, ward);
	 * ps.setString(3, street_name);
	 * ps.setString(4, latitude);
	 * ps.setString(5, longitude);
	 * ps.setString(6, cby);
	 * 
	 * if (finalComplaintType == 1) {
	 * ps.setString(7, vechileno);
	 * } else if (finalComplaintType == 2) {
	 * ps.setString(7, finalEquipmentIds);
	 * } else if (finalComplaintType == 3) {
	 * ps.setString(7, finalSweepingIds);
	 * }
	 * 
	 * ps.setString(8, finalImagePath);
	 * ps.setString(9, remarks);
	 * ps.setString(10, q_id);
	 * }
	 * 
	 * return ps;
	 * }
	 * 
	 * }, keyHolder);
	 * 
	 * if (affectedRows > 0) {
	 * 
	 * int generatedId = keyHolder.getKey().intValue();
	 * 
	 * String prefix = LocalDate.now()
	 * .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
	 * String refId = prefix + generatedId;
	 * 
	 * jdbcTemplate.update(finalUpdateSql, refId, generatedId);
	 * 
	 * response.put("insertId", generatedId);
	 * response.put("complaint_id", complaint_id);
	 * // response.put("refId", refId);
	 * response.put("status", "success");
	 * response.put("message", "Complaint saved successfully");
	 * 
	 * } else {
	 * response.put("status", "error");
	 * response.put("message", "Insert failed");
	 * }
	 * 
	 * } catch (DataAccessException e) {
	 * 
	 * Throwable rootCause = e.getMostSpecificCause();
	 * 
	 * System.out.println("Method: saveComplaint");
	 * System.out.println("Date: " + LocalDateTime.now());
	 * System.out.println("Error: " + rootCause.getMessage());
	 * 
	 * e.printStackTrace();
	 * 
	 * response.put("status", "error");
	 * response.put("message", rootCause.getMessage());
	 * }
	 * 
	 * return Collections.singletonList(response);
	 * }
	 */

	// get Sweeping
	public List<Map<String, Object>> getChecklistList() {

		String methodName = "getChecklistList";

		try {

			String sql = "SELECT checklist_id, checklist_name FROM check_list_master WHERE is_delete = 0 AND is_active = 1";

			return jdbcTemplate.queryForList(sql);

		} catch (Exception e) {

			System.err.println("Exception in Method : " + methodName);
			System.err.println("Date & Time        : " + LocalDateTime.now());
			System.err.println("Error Message      : " + e.getMessage());

			e.printStackTrace(); // full stack trace

			throw new RuntimeException("Failed to fetch Checklist List", e);
		}
	}

	public List<Map<String, Object>> getParentQuestionsList(String flow_ref_id, String complaint_id, String loginId) {

		Map<String, Object> response = new HashMap<>();

		try {
			String sql1 = "SELECT complaint_id FROM complaint_master WHERE flow_ref_id = ? and is_active = 1 and is_delete = 0  limit 1";
			int complaintId = jdbcTemplate.queryForObject(sql1, new Object[] { flow_ref_id }, Integer.class);

			if (complaintId > 0) {
				String sql = "SELECT "
						+ "    ql.*, "
						+ "    CASE   "
						+ "        WHEN (ql.input_type = 'text' OR ql.input_type = 'checkbox' OR ql.input_type = 'list') AND COUNT(qov.answer_id) > 0 THEN JSON_ARRAYAGG( "
						+ "            JSON_OBJECT( "
						+ "                'option_id', qov.answer_id, "
						+ "                'value', qov.answer_id, "
						+ "                'q_english', qov.english_name, "
						+ "                'q_tamil', IFNULL(qov.tamil_name, ''), "
						+ "                'orderby', qov.orderby "
						+ "            ) "
						+ "        ) "
						+ "        ELSE JSON_ARRAY()  "
						+ "    END AS options "
						+ "FROM questionmaster ql "
						+ "LEFT JOIN answer qov  "
						+ "    ON qov.question_id = ql.question_id  "
						+ "    AND qov.is_active = 1  "
						+ "    AND qov.is_delete = 0 "
						+ "WHERE ql.is_active = 1 AND ql.group_id= ?  "
						+ "GROUP BY ql.question_id";

				List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, complaintId);
				Iterator<Map<String, Object>> iterator = result.iterator();
				ObjectMapper mapper = new ObjectMapper();
				while (iterator.hasNext()) {
					Map<String, Object> row = iterator.next();
					// 🔹 Convert null q_tamil to empty string
					if (row.get("q_tamil") == null) {
						row.put("q_tamil", "");
					}
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

				response.put("status", "Success");
				response.put("message", "IE Question List.");
				response.put("complaint_id", complaintId);
				response.put("data", result);

				return Collections.singletonList(response);

			}

			response.put("status", "Failed");
			response.put("message", "Invalid flow ref id");

			return Collections.singletonList(response);

		} catch (Exception e) {
			logger.error("Error in getParentQuestionsList", e);
			response.put("status", "Failed");
			response.put("message", "Invalid Flow ref id");
			return Collections.singletonList(response);
		}

	}

	// update

	@Transactional
	public List<Map<String, Object>> saveComplaint(

			Integer complaint_id,
			String zone,
			String ward,
			String street_name,
			Integer street_id,
			String latitude,
			String longitude,
			String cby,
			MultipartFile image,
			String questionAnswers,
			String remarks) {

		Map<String, Object> response = new HashMap<>();
		String imagePath = "";

		try {

			if (image != null && !image.isEmpty()) {
				imagePath = fileUpload("complaints", "0", image);

			}

			// =========================
			// 1️⃣ INSERT INTO complaint_details
			// =========================

			String insertComplaint = "INSERT INTO complaint_details " +
					"(complaint_id, zone, ward, street_name,street_id, latitude, longitude, image_path, remarks, cby) "
					+
					"VALUES (?,?,?,?,?,?,?,?,?,?)";

			KeyHolder keyHolder = new GeneratedKeyHolder();
			final String finalImagePath = imagePath;

			jdbcTemplate.update(connection -> {

				PreparedStatement ps = connection.prepareStatement(insertComplaint, new String[] { "id" });

				ps.setInt(1, complaint_id);
				ps.setString(2, zone);
				ps.setString(3, ward);
				ps.setString(4, street_name);
				ps.setInt(5, street_id);
				ps.setString(6, latitude);
				ps.setString(7, longitude);
				ps.setString(8, finalImagePath);
				ps.setString(9, remarks);
				ps.setString(10, cby);

				return ps;

			}, keyHolder);

			int generatedId = keyHolder.getKey().intValue();

			// generate ref_id
			String refId = LocalDate.now()
					.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + generatedId;

			jdbcTemplate.update(
					"UPDATE complaint_details SET ref_id=? WHERE id=?",
					refId,
					generatedId);

			// =========================
			// 2️⃣ INSERT QUESTION ANSWERS
			// =========================

			/*
			 * ObjectMapper mapper = new ObjectMapper();
			 * 
			 * List<Map<String, Object>> qaList = mapper.readValue(questionAnswers,
			 * new TypeReference<List<Map<String, Object>>>() {
			 * });
			 * 
			 * for (Map<String, Object> item : qaList) {
			 * 
			 * Integer qid = ((Number) item.get("qid")).intValue();
			 * 
			 * Object answerObj = item.get("answerid");
			 * 
			 * String answerIds = null;
			 * String answerText = null;
			 * 
			 * // 🔹 If multi-select (array)
			 * if (answerObj instanceof List<?>) {
			 * 
			 * List<?> answerList = (List<?>) answerObj;
			 * 
			 * answerIds = answerList.stream()
			 * .map(obj -> String.valueOf(((Number) obj).intValue()))
			 * .collect(Collectors.joining(","));
			 * }
			 * // 🔹 If single select
			 * else if (answerObj != null) {
			 * 
			 * answerIds = String.valueOf(((Number) answerObj).intValue());
			 * }
			 * 
			 * // 🔹 If text answer
			 * if (item.get("answertext") != null) {
			 * answerText = item.get("answertext").toString();
			 * }
			 * 
			 * jdbcTemplate.update(
			 * "INSERT INTO response (ref_id, complaint_id, q_id, answer_id, answer_text) VALUES (?,?,?,?,?)"
			 * ,
			 * refId,
			 * complaint_id,
			 * qid,
			 * answerIds,
			 * answerText);
			 * }
			 */
			if (questionAnswers != null && !questionAnswers.isBlank()) {

				ObjectMapper mapper = new ObjectMapper();

				List<Map<String, Object>> qaList = mapper.readValue(questionAnswers,
						new TypeReference<List<Map<String, Object>>>() {
						});

				for (Map<String, Object> item : qaList) {

					Integer qid = ((Number) item.get("qid")).intValue();

					Object answerObj = item.get("answerid");

					String answerIds = null;
					String answerText = null;
					// multiplechecbox id separation

					if (answerObj instanceof List<?>) {
						List<?> answerList = (List<?>) answerObj;
						answerIds = answerList.stream()
								.map(obj -> String.valueOf(((Number) obj).intValue()))
								.collect(Collectors.joining(","));
					}
					// single answerid for dropdown
					else if (answerObj != null) {

						answerIds = String.valueOf(((Number) answerObj).intValue());
					}
					// answer text

					if (item.get("answertext") != null) {
						answerText = item.get("answertext").toString();
					}

					jdbcTemplate.update(
							"INSERT INTO response (ref_id, complaint_id, q_id, answer_id, answer_text) VALUES (?,?,?,?,?)",
							refId,
							complaint_id,
							qid,
							answerIds,
							answerText);
				}
			}
			response.put("status", "success");
			response.put("insert_id", generatedId);
			// response.put("ref_id", refId);
			response.put("message", "Complaint saved successfully");

		} catch (Exception e) {

			e.printStackTrace();

			response.put("status", "error");
			response.put("message", e.getMessage());
		}

		return Collections.singletonList(response);
	}

	public Map<String, Object> getZoneComplaintListforFeedback(String loginid) {

		Map<String, Object> response = new HashMap<>();

		try {

			// String ward = getWardByLoginId(loginid, "si_so");
			String ward = getWardByLoginId(loginid);

			String sqlQuery = "SELECT " +

					" cd.zone, " +
					" cd.ward, " +
					" cd.street_name, " +
					" cd.latitude, " +
					" cd.longitude, " +
					// " cd.image_path, " +
					"CASE WHEN cd.image_path IS NOT NULL " +
					"THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', cd.image_path) " +
					"ELSE '' " +
					" END AS image_path, " +
					"cd.remarks, " +
					"cd.street_id, " +

					" cd.cby, " +
					"cm.complaint_name, " +

					" cd.ref_id, " +
					" cd.complaint_id, " +

					" q.q_english AS question_name, " +
					" CASE " +
					"   WHEN q.input_type = 'text' " +
					"       THEN r.answer_text " +
					"   ELSE GROUP_CONCAT(DISTINCT a.english_name " +
					"       ORDER BY a.answer_id SEPARATOR ', ') " +
					" END AS answer_value " +
					"FROM complaint_details cd " +
					"LEFT JOIN response r ON r.ref_id = cd.ref_id " +
					"LEFT JOIN questionmaster q ON q.question_id = r.q_id " +
					"LEFT JOIN answer a ON FIND_IN_SET(a.answer_id, r.answer_id) " +
					" JOIN complaint_master cm on cm.complaint_id = cd.complaint_id " +
					"WHERE cd.ward = ? " +
					"AND cd.ref_id NOT IN (SELECT ref_id FROM completion) " +
					"GROUP BY " +
					"  cd.zone, cd.ward, cd.street_name,cd.street_id, " +
					" cd.latitude, cd.longitude, cd.image_path, " +
					" cd.remarks,  cd.cby, " +

					" cd.ref_id, cd.complaint_id, " +
					" q.question_id, r.answer_text " +
					"ORDER BY cd.ref_id, q.question_id";

			List<Map<String, Object>> rows = jdbcTemplate.queryForList(sqlQuery, ward);

			// ✅ NO DATA CASE
			if (rows == null || rows.isEmpty()) {
				response.put("status", "Failed");
				response.put("message", "No data available");
				response.put("data", new ArrayList<>());
				return response;
			}

			// ✅ GROUPING
			Map<String, Map<String, Object>> complaintMap = new LinkedHashMap<>();

			for (Map<String, Object> row : rows) {

				String refId = String.valueOf(row.get("ref_id"));

				if (!complaintMap.containsKey(refId)) {

					Map<String, Object> complaint = new LinkedHashMap<>();

					complaint.put("zone", row.get("zone"));
					complaint.put("ward", row.get("ward"));
					complaint.put("street_name", row.get("street_name"));
					complaint.put("street_id", row.get("street_id"));

					complaint.put("latitude", row.get("latitude"));
					complaint.put("longitude", row.get("longitude"));
					complaint.put("image_path", row.get("image_path"));
					complaint.put("remarks", row.get("remarks"));
					complaint.put("complaint_name", row.get("complaint_name"));

					complaint.put("cby", row.get("cby"));

					complaint.put("ref_id", refId);

					complaint.put("questions", new ArrayList<>());

					complaintMap.put(refId, complaint);
				}

				Map<String, Object> question = new LinkedHashMap<>();
				question.put("question_name",
						row.get("question_name") == null ? "" : row.get("question_name").toString());

				question.put("answer_value",
						row.get("answer_value") == null ? "" : row.get("answer_value").toString());
				/*
				 * question.put("question_name", row.get("question_name"));
				 * question.put("answer_value", row.get("answer_value"));
				 */

				List<Map<String, Object>> questions = (List<Map<String, Object>>) complaintMap
						.get(refId)
						.get("questions");

				questions.add(question);
			}

			List<Map<String, Object>> result = new ArrayList<>(complaintMap.values());

			// ✅ SUCCESS RESPONSE
			response.put("status", "Success");
			response.put("message", "IE SiSO List.");
			response.put("data", result);

		} catch (Exception e) {

			e.printStackTrace();

			response.put("status", "Error");
			response.put("message", "Something went wrong");
			response.put("data", null);
		}

		return response;
	}

	// public String getWardByLoginId(String loginid, String type) {
	public String getWardByLoginId(String loginid) {
		// String sqlQuery = "SELECT `ward` FROM gcc_penalty_hoardings.hoading_user_list
		// WHERE userid = ? AND type = ? LIMIT 1";
		String sqlQuery = "SELECT `ward` FROM gcc_penalty_hoardings.hoading_user_list WHERE userid = ? and isactive=1  LIMIT 1";

		// Query the database using queryForList
		List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery, loginid);

		// Check if results is not empty and extract the ward value
		if (!results.isEmpty()) {
			System.out.println("ward....." + results);
			// Extract the ward value from the first result
			return (String) results.get(0).get("ward");
		}

		// Handle the case where no result is found
		return "00"; // or return null based on your needs
	}

	public Map<String, Object> saveCompletion(
			String refId,
			String remarks,
			MultipartFile image,
			String cby,
			String zone,
			String ward,
			String street_name,
			Integer street_id,
			String latitude,
			String longitude) {

		Map<String, Object> response = new HashMap<>();

		try {

			// =========================
			// 1️⃣ CHECK ALREADY COMPLETED
			// =========================
			Integer count = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM completion WHERE ref_id=?",
					Integer.class,
					refId);

			if (count != null && count > 0) {
				response.put("status", "Failed");
				response.put("message", "Completion already exists");
				return response;
			}

			// =========================
			// 2️⃣ FILE UPLOAD
			// =========================
			String imagePath = "";

			if (image != null && !image.isEmpty()) {
				imagePath = fileUpload("completion", "0", image);
			}

			// =========================
			// 3️⃣ INSERT COMPLETION
			// =========================
			String insertSql = "INSERT INTO completion " +
					"(ref_id, remarks, image_path, cby,street_id,street_name,zone,ward,latitude,longitude) " +
					"VALUES (?,?,?,?,?,?,?,?,?,?)";

			int insertRows = jdbcTemplate.update(
					insertSql,
					refId,
					remarks,
					imagePath,
					cby, street_id, street_name, zone, ward, latitude, longitude);

			if (insertRows == 0) {
				response.put("status", "Failed");
				response.put("message", "Completion insert failed");
				return response;
			}

			// =========================
			// 4️⃣ UPDATE STATUS
			// =========================
			int updateRows = jdbcTemplate.update(
					"UPDATE complaint_details SET status=? WHERE ref_id=?",
					"completed",
					refId);

			// =========================
			// 5️⃣ RESPONSE
			// =========================
			if (updateRows > 0) {
				response.put("status", "Success");
				response.put("message", "Completion saved and status updated");
			} else {
				response.put("status", "Partial Success");
				response.put("message",
						"Completion saved but status update failed");
			}

			response.put("ref_id", refId);

		} catch (Exception e) {

			e.printStackTrace();

			response.put("status", "Error");
			response.put("message", e.getMessage());
		}

		return response;
	}

	// aee
	public Map<String, Object> getZoneComplaintListForAEE(String loginid) {

		Map<String, Object> response = new HashMap<>();

		try {

			// ✅ get zone for AEE
			String zone = getZoneByLoginId(loginid);
			// String zone = getZoneByLoginId(loginid, "aee");

			String sqlQuery = "SELECT " +
					" cd.zone, " +
					" cd.ward, " +
					" cd.street_name, " +
					" cd.latitude, " +
					" cd.longitude, " +
					// " cd.image_path, " +
					"CASE WHEN cd.image_path IS NOT NULL " +
					"THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', cd.image_path) " +
					"ELSE '' " +
					" END AS image_path, " +

					" cd.remarks, " +
					" cd.cby, " +
					"cd.street_id, " +
					" cd.ref_id, " +
					" cd.complaint_id, " +
					"cm.complaint_name, " +
					" q.q_english AS question_name, " +
					" CASE " +
					"   WHEN q.input_type = 'text' " +
					"       THEN r.answer_text " +
					"   ELSE GROUP_CONCAT(DISTINCT a.english_name " +
					"       ORDER BY a.answer_id SEPARATOR ', ') " +
					" END AS answer_value " +

					"FROM complaint_details cd " +
					"JOIN response r ON r.ref_id = cd.ref_id " +
					"JOIN questionmaster q ON q.question_id = r.q_id " +
					"LEFT JOIN answer a ON FIND_IN_SET(a.answer_id, r.answer_id) " +
					"Join complaint_master cm on cm.complaint_id=cd.complaint_id " +

					// ✅ NEW CONDITIONS
					"WHERE cd.zone = ? " +
					"AND cd.status = 'pending' " +
					"AND cd.esclation_flag = 1 " +

					"GROUP BY " +
					" cd.zone, cd.ward, cd.street_name,cd.street_id, " +
					" cd.latitude, cd.longitude, cd.image_path, " +
					" cd.remarks, cd.cby, " +
					" cd.ref_id, cd.complaint_id, " +
					" q.question_id, r.answer_text " +

					"ORDER BY cd.ref_id, q.question_id";

			List<Map<String, Object>> rows = jdbcTemplate.queryForList(sqlQuery, zone);

			// ✅ NO DATA
			if (rows == null || rows.isEmpty()) {
				response.put("status", "Failed");
				response.put("message", "No data available");
				response.put("data", new ArrayList<>());
				return response;
			}

			// ✅ GROUPING LOGIC (same as yours)
			Map<String, Map<String, Object>> complaintMap = new LinkedHashMap<>();

			for (Map<String, Object> row : rows) {

				String refId = String.valueOf(row.get("ref_id"));

				if (!complaintMap.containsKey(refId)) {

					Map<String, Object> complaint = new LinkedHashMap<>();

					complaint.put("zone", row.get("zone"));
					complaint.put("ward", row.get("ward"));
					complaint.put("street_name", row.get("street_name"));
					// complaint.put("street_id", row.get("street_id"));
					complaint.put("street_id",
							row.get("street_id") == null ? "" : row.get("street_id").toString());

					complaint.put("latitude", row.get("latitude"));
					complaint.put("longitude", row.get("longitude"));
					complaint.put("image_path", row.get("image_path"));
					complaint.put("remarks", row.get("remarks"));
					complaint.put("complaint_name", row.get("complaint_name"));

					complaint.put("cby", row.get("cby"));
					complaint.put("ref_id", refId);

					complaint.put("questions", new ArrayList<>());

					complaintMap.put(refId, complaint);
				}

				Map<String, Object> question = new LinkedHashMap<>();
				/*
				 * question.put("question_name", row.get("question_name"));
				 * question.put("answer_value", row.get("answer_value"));
				 */

				question.put("question_name",
						row.get("question_name") == null ? "" : row.get("question_name").toString());

				question.put("answer_value",
						row.get("answer_value") == null ? "" : row.get("answer_value").toString());

				((List<Map<String, Object>>) complaintMap
						.get(refId)
						.get("questions"))
						.add(question);
			}

			response.put("status", "Success");
			response.put("message", "IE AEE List.");
			response.put("data",
					new ArrayList<>(complaintMap.values()));

		} catch (Exception e) {

			e.printStackTrace();

			response.put("status", "Error");
			response.put("message", "Something went wrong");
			response.put("data", null);
		}

		return response;
	}
	// public String getZoneByLoginId(String loginid, String type) {

	public String getZoneByLoginId(String loginid) {
		// String sqlQuery = "SELECT `zone` FROM gcc_penalty_hoardings.hoading_user_list
		// WHERE userid = ? AND type = ? LIMIT 1";
		String sqlQuery = "SELECT `zone` FROM gcc_penalty_hoardings.hoading_user_list WHERE userid = ?  and isactive=1 LIMIT 1";
		List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery, loginid);

		// Query the database using queryForList
		// List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery,
		// loginid, type);

		// Check if results is not empty and extract the ward value
		if (!results.isEmpty()) {
			System.out.println("zone....." + results);
			// Extract the ward value from the first result
			return (String) results.get(0).get("zone");
		}

		// Handle the case where no result is found
		return "00"; // or return null based on your needs
	}

	public Map<String, Object> updateIEEscalationStatus(String escalationFlag) {

		try {

			// ===============================
			// 1️⃣ 24 HOURS ESCALATION
			// complaint_id → 1,2,7
			// ===============================
			String sql24Hours = "UPDATE complaint_details " +
					"SET esclation_flag = ? " +
					"WHERE esclation_flag = 0 " +
					"AND status = 'pending' " +
					"AND is_active=1 " +
					"AND complaint_id IN (1,2,7) " +
					"AND cdate < NOW() - INTERVAL 24 HOUR";

			int updated24 = jdbcTemplate.update(sql24Hours, escalationFlag);

			// ===============================
			// 2️⃣ 6 HOURS ESCALATION
			// complaint_id → 3,4,5,6,8,9,10
			// ===============================
			String sql6Hours = "UPDATE complaint_details " +
					"SET esclation_flag = ? " +
					"WHERE esclation_flag = 0 " +
					"AND status = 'pending' " +
					"AND is_active=1 " +
					"AND complaint_id IN (3,4,5,6,8,9,10) " +
					"AND cdate < NOW() - INTERVAL 6 HOUR";

			int updated6 = jdbcTemplate.update(sql6Hours, escalationFlag);

			int totalUpdated = updated24 + updated6;

			return Map.of(
					"status", true,
					"message", "IE Escalation Updated Successfully",
					"rowsAffected", totalUpdated,
					"updated24Hours", updated24,
					"updated6Hours", updated6);

		} catch (Exception e) {

			e.printStackTrace();

			return Map.of(
					"status", false,
					"message", "Error: " + e.getMessage());
		}
	}

	// report
	// esisting before totalcount
	/*
	 * public Map<String, Object> getZoneComplaintReport(
	 * Integer complaintId,
	 * String fromDate,
	 * String toDate) {
	 * 
	 * Map<String, Object> response = new HashMap<>();
	 * 
	 * try {
	 * 
	 * String sql = "SELECT zone, " +
	 * "COUNT(CASE WHEN status='pending' THEN 1 END) AS opened_count, " +
	 * "COUNT(CASE WHEN status='completed' THEN 1 END) AS closed_count " +
	 * "FROM complaint_details " +
	 * "WHERE complaint_id = ? " +
	 * "AND DATE(cdate) BETWEEN ? AND ? " +
	 * "GROUP BY zone " +
	 * "ORDER BY zone";
	 * 
	 * List<Map<String, Object>> result = jdbcTemplate.queryForList(sql,
	 * complaintId,
	 * fromDate,
	 * toDate);
	 * 
	 * if (result.isEmpty()) {
	 * response.put("status", "Failed");
	 * response.put("message", "No data available");
	 * response.put("data", new ArrayList<>());
	 * return response;
	 * }
	 * 
	 * response.put("status", "Success");
	 * response.put("message", "Zone Complaint Report");
	 * response.put("data", result);
	 * 
	 * } catch (Exception e) {
	 * 
	 * e.printStackTrace();
	 * 
	 * response.put("status", "Error");
	 * response.put("message", e.getMessage());
	 * response.put("data", null);
	 * }
	 * 
	 * return response;
	 * }
	 */
	public Map<String, Object> getZoneComplaintReport(
			Integer complaintId,
			String fromDate,
			String toDate) {

		Map<String, Object> response = new HashMap<>();

		try {

			String sql = "SELECT cd.zone, " +

			// OPENED count (complaint_details date)
					"COUNT(DISTINCT CASE " +
					"   WHEN cd.status = 'pending' " +
					"   AND DATE(cd.cdate) BETWEEN ? AND ? " +
					"   THEN cd.ref_id END) AS opened_count, " +

					// CLOSED count (completion date)
					"COUNT(DISTINCT CASE " +
					"   WHEN comp.ref_id IS NOT NULL " +
					"   AND DATE(comp.cdate) BETWEEN ? AND ? " +
					"   THEN comp.ref_id END) AS closed_count, " +

					// TOTAL = OPENED + CLOSED
					"(" +
					"COUNT(DISTINCT CASE " +
					"   WHEN cd.status = 'pending' " +
					"   AND DATE(cd.cdate) BETWEEN ? AND ? " +
					"   THEN cd.ref_id END) " +
					"+" +
					"COUNT(DISTINCT CASE " +
					"   WHEN comp.ref_id IS NOT NULL " +
					"   AND DATE(comp.cdate) BETWEEN ? AND ? " +
					"   THEN comp.ref_id END)" +
					") AS total_count " +

					"FROM complaint_details cd " +
					"LEFT JOIN completion comp ON comp.ref_id = cd.ref_id " +
					"WHERE cd.complaint_id = ? " +
					"GROUP BY cd.zone " +
					"ORDER BY cd.zone";

			List<Map<String, Object>> result = jdbcTemplate.queryForList(
					sql,
					fromDate, toDate, // opened
					fromDate, toDate, // closed
					fromDate, toDate, // total opened part
					fromDate, toDate, // total closed part
					complaintId);

			if (result == null || result.isEmpty()) {
				response.put("status", "Failed");
				response.put("message", "No data available");
				response.put("data", new ArrayList<>());
				return response;
			}

			response.put("status", "Success");
			response.put("message", "Zone Complaint Report");
			response.put("data", result);

		} catch (Exception e) {

			e.printStackTrace();

			response.put("status", "Error");
			response.put("message", e.getMessage());
			response.put("data", null);
		}

		return response;
	}
	// esisting before totalcount

	/*
	 * public Map<String, Object> getWardComplaintReport(
	 * String zone,
	 * String fromDate,
	 * String toDate, Integer complaintId) {
	 * 
	 * Map<String, Object> response = new HashMap<>();
	 * 
	 * try {
	 * 
	 * String sql = "SELECT " +
	 * " ward, " +
	 * 
	 * " COUNT(CASE WHEN status='pending' THEN 1 END) AS opened_count, " +
	 * " COUNT(CASE WHEN status='completed' THEN 1 END) AS closed_count " +
	 * 
	 * "FROM complaint_details " +
	 * "WHERE zone = ? " +
	 * "AND complaint_id = ?  " +
	 * "AND DATE(cdate) BETWEEN ? AND ? " +
	 * "GROUP BY ward";
	 * 
	 * List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, zone,
	 * complaintId, fromDate, toDate);
	 * 
	 * // ✅ No Data
	 * if (result.isEmpty()) {
	 * response.put("status", "Failed");
	 * response.put("message", "No data available");
	 * response.put("data", new ArrayList<>());
	 * return response;
	 * }
	 * 
	 * // ✅ Success
	 * response.put("status", "Success");
	 * response.put("message", "Ward Complaint Report");
	 * response.put("data", result);
	 * 
	 * } catch (Exception e) {
	 * 
	 * e.printStackTrace();
	 * 
	 * response.put("status", "Error");
	 * response.put("message", "Something went wrong");
	 * response.put("data", null);
	 * }
	 * 
	 * return response;
	 * }
	 */
	public Map<String, Object> getWardComplaintReport(
			String zone,
			String fromDate,
			String toDate,
			Integer complaintId) {

		Map<String, Object> response = new HashMap<>();

		try {

			String sql = "SELECT cd.ward, " +

			// OPENED (complaint_details date)
					"COUNT(DISTINCT CASE " +
					"   WHEN cd.status = 'pending' " +
					"   AND DATE(cd.cdate) BETWEEN ? AND ? " +
					"   THEN cd.ref_id END) AS opened_count, " +

					// CLOSED (completion date)
					"COUNT(DISTINCT CASE " +
					"   WHEN comp.ref_id IS NOT NULL " +
					"   AND DATE(comp.cdate) BETWEEN ? AND ? " +
					"   THEN comp.ref_id END) AS closed_count, " +

					// TOTAL = opened + closed
					"(" +
					"COUNT(DISTINCT CASE " +
					"   WHEN cd.status = 'pending' " +
					"   AND DATE(cd.cdate) BETWEEN ? AND ? " +
					"   THEN cd.ref_id END) " +
					"+" +
					"COUNT(DISTINCT CASE " +
					"   WHEN comp.ref_id IS NOT NULL " +
					"   AND DATE(comp.cdate) BETWEEN ? AND ? " +
					"   THEN comp.ref_id END)" +
					") AS total_count " +

					"FROM complaint_details cd " +
					"LEFT JOIN completion comp ON comp.ref_id = cd.ref_id " +

					"WHERE cd.zone = ? " +
					"AND cd.complaint_id = ? " +

					"GROUP BY cd.ward " +
					"ORDER BY cd.ward";

			List<Map<String, Object>> result = jdbcTemplate.queryForList(
					sql,
					fromDate, toDate, // opened
					fromDate, toDate, // closed
					fromDate, toDate, // total opened part
					fromDate, toDate, // total closed part
					zone,
					complaintId);

			if (result == null || result.isEmpty()) {
				response.put("status", "Failed");
				response.put("message", "No data available");
				response.put("data", new ArrayList<>());
				return response;
			}

			response.put("status", "Success");
			response.put("message", "Ward Complaint Report");
			response.put("data", result);

		} catch (Exception e) {

			e.printStackTrace();

			response.put("status", "Error");
			response.put("message", e.getMessage());
			response.put("data", null);
		}

		return response;
	}

	public Map<String, Object> getWardComplaintReportDetails(
			String ward,
			String status,
			String fromDate,
			String toDate,
			Integer complaintId) {

		Map<String, Object> response = new HashMap<>();

		try {

			String sql = "SELECT " +
					" cd.id, cd.zone, cd.ward, cd.street_name, " +
					" cd.latitude, cd.longitude, " +
					"CASE WHEN cd.image_path IS NOT NULL " +
					"THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', cd.image_path) " +
					"ELSE '' " +
					" END AS complaint_image, " +

					// " cd.image_path AS complaint_image, " +
					" cd.remarks AS complaint_remarks, " +
					" cd.cdate, cd.cby, cd.ref_id, cd.status, " +
					" cm.complaint_name AS complaint_name, " +

					" q.q_english AS question_name, " +

					" CASE " +
					"   WHEN q.input_type='text' THEN r.answer_text " +
					"   ELSE GROUP_CONCAT(DISTINCT a.english_name " +
					"        ORDER BY a.answer_id SEPARATOR ', ') " +
					" END AS answer_value, " +

					"comp.zone As completion_zone, " +
					"comp.ward As completion_ward, " +
					"comp.latitude As completion_latitude, " +
					"comp.longitude As completion_longitude, " +
					"comp.street_id As completion_street_id , " +
					"comp.street_name As completion_street_name, " +

					" comp.remarks AS completion_remarks, " +
					"CASE WHEN comp.image_path IS NOT NULL " +
					"THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', comp.image_path) " +
					"ELSE '' " +
					" END AS completion_image " +
					// " comp.image_path AS completion_image " +

					"FROM complaint_details cd " +
					"LEFT JOIN response r ON r.ref_id = cd.ref_id " +
					"LEFT JOIN questionmaster q ON q.question_id = r.q_id " +
					"LEFT JOIN answer a ON FIND_IN_SET(a.answer_id, r.answer_id) " +
					"LEFT JOIN completion comp ON comp.ref_id = cd.ref_id " +
					" JOIN complaint_master cm ON cm.complaint_id = cd.complaint_id " +

					"WHERE cd.ward = ? " +
					"AND cd.status = ? " +
					"AND cd.complaint_id =? " +
					"AND DATE(cd.cdate) BETWEEN ? AND ? " +

					"GROUP BY cd.id, cd.zone, cd.ward, cd.street_name, " +
					"cd.latitude, cd.longitude, cd.image_path, cd.remarks, " +
					"cd.cdate, cd.cby, cd.ref_id, cd.status, " +
					"q.question_id, r.answer_text, comp.remarks, comp.image_path, " +
					"comp.zone,comp.ward,comp.latitude,comp.longitude,comp.street_id,comp.street_name " +

					"ORDER BY cd.ref_id, q.question_id";

			List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, ward, status, complaintId, fromDate,
					toDate);

			// ✅ NO DATA
			if (rows.isEmpty()) {
				response.put("status", "Failed");
				response.put("message", "No data available");
				response.put("data", new ArrayList<>());
				return response;
			}

			// ✅ GROUP BY ref_id
			Map<String, Map<String, Object>> complaintMap = new LinkedHashMap<>();

			for (Map<String, Object> row : rows) {

				String refId = String.valueOf(row.get("ref_id"));

				if (!complaintMap.containsKey(refId)) {

					Map<String, Object> complaint = new LinkedHashMap<>();

					complaint.put("zone", row.get("zone"));
					complaint.put("ward", row.get("ward"));
					complaint.put("street_name", row.get("street_name"));

					complaint.put("latitude", row.get("latitude"));
					complaint.put("longitude", row.get("longitude"));
					complaint.put("status", row.get("status"));
					complaint.put("complaint_image", row.get("complaint_image"));
					complaint.put("complaint_remarks", row.get("complaint_remarks"));
					complaint.put("complaint_name", row.get("complaint_name"));

					// completion fields auto-null for pending
					complaint.put("completion_remarks",
							row.get("completion_remarks") == null ? "" : row.get("completion_remarks"));

					complaint.put("completion_image",
							row.get("completion_image") == null ? "" : row.get("completion_image"));
					complaint.put("completion_zone",
							row.get("completion_zone") == null ? "" : row.get("completion_zone").toString());

					complaint.put("completion_ward",
							row.get("completion_ward") == null ? "" : row.get("completion_ward").toString());

					complaint.put("completion_latitude",
							row.get("completion_latitude") == null ? "" : row.get("completion_latitude").toString());

					complaint.put("completion_longitude",
							row.get("completion_longitude") == null ? "" : row.get("completion_longitude").toString());

					complaint.put("completion_street_id",
							row.get("completion_street_id") == null ? "" : row.get("completion_street_id").toString());

					complaint.put("completion_street_name",
							row.get("completion_street_name") == null ? ""
									: row.get("completion_street_name").toString());
					/*
					 * complaint.put("completion_zone", row.get("completion_zone"));
					 * complaint.put("completion_ward", row.get("completion_ward"));
					 * complaint.put("completion_latitude", row.get("completion_latitude"));
					 * complaint.put("completion_longitude", row.get("completion_longitude"));
					 * complaint.put("completion_street_id", row.get("completion_street_id"));
					 * complaint.put("completion_street_name", row.get("completion_street_name"));
					 */
					/*
					 * complaint.put("completion_remarks", row.get("completion_remarks") );
					 * complaint.put("completion_image", row.get("completion_image"));
					 */

					complaint.put("questions", new ArrayList<>());

					complaintMap.put(refId, complaint);
				}

				Map<String, Object> question = new LinkedHashMap<>();
				/*
				 * question.put("question_name", row.get("question_name"));
				 * question.put("answer_value", row.get("answer_value"));
				 */
				question.put("question_name",
						row.get("question_name") == null ? "" : row.get("question_name").toString());

				question.put("answer_value",
						row.get("answer_value") == null ? "" : row.get("answer_value").toString());

				((List<Map<String, Object>>) complaintMap
						.get(refId)
						.get("questions"))
						.add(question);
			}

			response.put("status", "Success");
			response.put("message", "Ward Complaint Drilldown Details");
			response.put("data", new ArrayList<>(complaintMap.values()));

		} catch (Exception e) {

			e.printStackTrace();

			response.put("status", "Error");
			response.put("message", "Something went wrong");
			response.put("data", null);
		}

		return response;
	}

	public Map<String, Object> getComplaintMaster() {

		Map<String, Object> response = new HashMap<>();

		try {

			String sql = "SELECT complaint_id, complaint_name " +
					"FROM complaint_master " +
					"WHERE is_active = 1 AND is_delete = 0 " +
					"ORDER BY complaint_id";

			List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

			if (result.isEmpty()) {
				response.put("status", "Failed");
				response.put("message", "No data available");
				response.put("data", new ArrayList<>());
				return response;
			}

			response.put("status", "Success");
			response.put("message", "Complaint Master List");
			response.put("data", result);

		} catch (Exception e) {

			e.printStackTrace();

			response.put("status", "Error");
			response.put("message", "Something went wrong");
			response.put("data", null);
		}

		return response;
	}

}
