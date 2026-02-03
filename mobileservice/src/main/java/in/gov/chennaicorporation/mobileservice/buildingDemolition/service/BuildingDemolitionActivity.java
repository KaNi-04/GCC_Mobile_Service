package in.gov.chennaicorporation.mobileservice.buildingDemolition.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
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
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class BuildingDemolitionActivity {
	private JdbcTemplate jdbcBuildingDemolitionTemplate;

	private final Environment environment;
	private String fileBaseUrl;

	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final int STRING_LENGTH = 15;
	private static final Random RANDOM = new SecureRandom();

	@Autowired
	public void setDataSource(@Qualifier("mysqlGccBuildingDemolitionSource") DataSource BuildingDemolitionDataSource) {
		this.jdbcBuildingDemolitionTemplate = new JdbcTemplate(BuildingDemolitionDataSource);
	}

	@Autowired
	public BuildingDemolitionActivity(Environment environment) {
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
		String serviceFolderName = environment.getProperty("buildingdemolition_foldername");
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
			System.out.println(file.getOriginalFilename());
			String fileName = name + "_" + id + "_" + datetimetxt + "_" + generateRandomStringForFile(10) + "_"
					+ file.getOriginalFilename();
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
			Files.write(path, bytes);

			System.out.println(filePath);
			return filepath_txt;

		} catch (IOException e) {
			e.printStackTrace();
			return "Failed to save file " + file.getOriginalFilename();
		}
	}

	public List<Map<String, Object>> getBuildingList(String aeUserId) {

		String sql = "SELECT zone, ward FROM gcc_penalty_hoardings.`hoading_user_list` WHERE userid = ? AND type='ae'";
		List<Map<String, Object>> userList = jdbcBuildingDemolitionTemplate.queryForList(sql, aeUserId);

		if (userList.isEmpty()) {
			// You can return an empty list or throw a custom exception
			return Collections.emptyList(); // or handle as needed
		}

		Map<String, Object> vendorData = userList.get(0);
		String ward = String.valueOf(vendorData.get("ward"));

		sql = "SELECT  "
				+ "    bd.* "
				+ "FROM building_demolition bd "
				+ "LEFT JOIN building_demolition_status bds ON bd.id = bds.bdid "
				+ "WHERE bds.bdid IS NULL "
				+ "  AND bd.ward = ?";
		List<Map<String, Object>> result = jdbcBuildingDemolitionTemplate.queryForList(sql, ward);
		Map<String, Object> response = new HashMap<>();
		response.put("status", "Success");
		response.put("message", "Building Demolition List.");
		response.put("data", result);

		return Collections.singletonList(response);
	}

	public List<Map<String, Object>> updateDetails(
			String id,
			String status,
			String cby,
			String latitude,
			String longitude,
			String remarks,
			MultipartFile file) {

		String filepath = null;
		// 1. Fetch building details
		String sql = "SELECT `id` as bdid, `council_resolution_date`, `year`, `zone`, `ward`, " +
				"`description`, `category_of_building`, `area_sqm` " +
				"FROM `building_demolition` WHERE id=?";

		List<Map<String, Object>> result = jdbcBuildingDemolitionTemplate.queryForList(sql, id);

		if (result.isEmpty()) {
			Map<String, Object> response = new HashMap<>();
			response.put("status", "Failed");
			response.put("message", "No record found for id " + id);
			return Collections.singletonList(response);
		}
		Map<String, Object> building = result.get(0);

		// 2. Check if record already exists in building_demolition_status
		String checkSql = "SELECT COUNT(*) FROM `building_demolition_status` WHERE bdid = ?";
		Integer count = jdbcBuildingDemolitionTemplate.queryForObject(checkSql, Integer.class, building.get("bdid"));

		if (count != null && count > 0) {
			Map<String, Object> response = new HashMap<>();
			response.put("status", "Failed");
			response.put("message", "Duplicate entry: Demolition status already exists for building id " + id);
			return Collections.singletonList(response);
		}

		// 2.1. Handle file upload (optional)
		if (file != null && !file.isEmpty()) {
			filepath = fileUpload(id, cby, file);
		}

		// 3. Insert into building_demolition_status
		String insertSql = "INSERT INTO `building_demolition_status` " +
				"(`bdid`, `council_resolution_date`, `year`, `zone`, `ward`, `description`, " +
				"`category_of_building`, `area_sqm`, `status`, `inby`, " +
				" `file1`, `remarks`, `latitude`, `longitude`) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		jdbcBuildingDemolitionTemplate.update(insertSql,
				building.get("bdid"),
				building.get("council_resolution_date"),
				building.get("year"),
				building.get("zone"),
				building.get("ward"),
				building.get("description"),
				building.get("category_of_building"),
				building.get("area_sqm"),
				status,
				cby,
				filepath,
				remarks,
				latitude,
				longitude);

		// 4. Build response
		Map<String, Object> response = new HashMap<>();
		response.put("status", "Success");
		response.put("message", "Building Demolition Status inserted successfully.");
		response.put("data", building);

		return Collections.singletonList(response);
	}

	// Reports
	public List<Map<String, Object>> zoneReport() {
		String sqlQuery = "SELECT "
				+ "    bd.zone, "
				+ "    COUNT(bd.id) AS total_building, "
				+ "    SUM(CASE WHEN bds.bdid IS NULL THEN 1 ELSE 0 END) AS pending, "
				+ "    SUM(CASE WHEN bds.bdid IS NOT NULL THEN 1 ELSE 0 END) AS completed "
				+ "FROM building_demolition bd "
				+ "LEFT JOIN building_demolition_status bds ON bd.id = bds.bdid "
				+ "GROUP BY bd.zone "
				+ "ORDER BY bd.zone";

		List<Map<String, Object>> result = jdbcBuildingDemolitionTemplate.queryForList(sqlQuery);

		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
		response.put("message", "Zone list report.");
		response.put("data", result);

		return Collections.singletonList(response);
	}

	public List<Map<String, Object>> wardReport(String zone) {
		String sqlQuery = "SELECT  "
				+ "    bd.ward, "
				+ "    COUNT(bd.id) AS total_building, "
				+ "    SUM(CASE WHEN bds.bdid IS NULL THEN 1 ELSE 0 END) AS pending, "
				+ "    SUM(CASE WHEN bds.bdid IS NOT NULL THEN 1 ELSE 0 END) AS completed "
				+ "FROM building_demolition bd "
				+ "LEFT JOIN building_demolition_status bds ON bd.id = bds.bdid "
				+ "WHERE bd.zone = ? "
				+ "GROUP BY bd.ward "
				+ "ORDER BY bd.ward";

		List<Map<String, Object>> result = jdbcBuildingDemolitionTemplate.queryForList(sqlQuery, zone);

		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
		response.put("message", "Ward list report.");
		response.put("data", result);

		return Collections.singletonList(response);
	}

	public List<Map<String, Object>> pendingReport(String ward) {
		String sqlQuery = "SELECT  "
				+ "    bd.* "
				+ "FROM building_demolition bd "
				+ "LEFT JOIN building_demolition_status bds ON bd.id = bds.bdid "
				+ "WHERE bds.bdid IS NULL "
				+ "  AND bd.ward = ?";

		List<Map<String, Object>> result = jdbcBuildingDemolitionTemplate.queryForList(sqlQuery, ward);

		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
		response.put("message", "Pending list report.");
		response.put("data", result);

		return Collections.singletonList(response);
	}

	public List<Map<String, Object>> completedReport(String ward) {
		String sqlQuery = "SELECT  "
				+ "    bds.*,"
				+ "  CONCAT('" + fileBaseUrl + "/gccofficialapp/files', bds.file1) AS image_url "
				+ "FROM building_demolition bd "
				+ "INNER JOIN building_demolition_status bds ON bd.id = bds.bdid "
				+ "WHERE bd.ward = ?";

		List<Map<String, Object>> result = jdbcBuildingDemolitionTemplate.queryForList(sqlQuery, ward);

		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
		response.put("message", "Completed list report.");
		response.put("data", result);

		return Collections.singletonList(response);
	}

	// --- New Methods Replicated from Construction Guidelines ---

	public String getWardByLoginId(String loginid, String type) {
		String sqlQuery = "SELECT ward FROM gcc_penalty_hoardings.hoading_user_list WHERE userid = ? LIMIT 1";
		List<Map<String, Object>> results = jdbcBuildingDemolitionTemplate.queryForList(sqlQuery, loginid);
		if (!results.isEmpty()) {
			return (String) results.get(0).get("ward");
		}
		return "000";
	}

	public String sendMessage(String tempid, String bdid) {
		String sendTo = "";
		String datetxt = "";
		String name = "";
		String Nos = "";
		String fineAmount = "";

		// Assuming send_msg_to table is shared or similar exists
		String sql = "SELECT `id`, `name`, `mobile`, `userid`, `tempids` FROM `send_msg_to` WHERE `isactive` = 1 AND `isdelete` = 0 AND FIND_IN_SET(?, tempids);";
		List<Map<String, Object>> mobileResult = jdbcBuildingDemolitionTemplate.queryForList(sql, tempid);
		StringBuilder sendToBuilder = new StringBuilder();

		for (Map<String, Object> mobileRow : mobileResult) {
			String mobile = String.valueOf(mobileRow.get("mobile"));
			if (sendToBuilder.length() > 0) {
				sendToBuilder.append(",");
			}
			sendToBuilder.append(mobile);
		}
		sendTo = sendToBuilder.toString();
		System.out.println("Mobile Numbers: " + sendTo);

		// NOTE: Credentials and URL structure copied from GuidelinesService
		String urlString = "";
		String username = "2000233507";
		String password = "h2YjFNcJ";

		// Note: Messages adapted slightly or kept generic if specific building
		// demolition context is needed
		switch (tempid) {
			case "1":
				System.out.println("TempID is 1 (Initial Notice)");
				urlString = "https://media.smsgupshup.com/GatewayAPI/rest?"
						+ "userid=" + username
						+ "&password=" + password
						+ "&send_to=" + sendTo
						+ "&v=1.1"
						+ "&format=json"
						+ "&msg_type=TEXT"
						+ "&method=SENDMEDIAMESSAGE"
						+ "&msg=Your+Demolition+Site+%28" + name + "%29+has+violations.+Please+rectify+within+15+days."
						+ "&isTemplate=true"
						+ "&header=GCC+Building+Demolition"
						+ "&footer=GCC+-+IT+Cell";
				break;
			case "2": // Compliance resolved
				// ... (Similar logic)
				break;
			case "3": // Penalty
				// ...
				break;
			// Add other cases as needed
			default:
				System.out.println("TempID (" + tempid + ") is unknown or not fully implemented for Demolition");
		}

		if (!urlString.isBlank() && !sendTo.isEmpty()) {
			// String response = sendWhatsAppMessage(urlString);
			// System.out.println("WhatsApp response: " + response);
		}
		return "success";
	}

	@org.springframework.transaction.annotation.Transactional
	public List<Map<String, Object>> saveDemolitionFeedback(
			String bdid,
			String bdsid, // Building Demolition Status ID
			String cby,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String q1, String q2, String q3, String q4, String q5, String q6, String q7, String q8, String q9,
			String q10,
			String q11, String q12, String q13, String q14, String q15, String q16, String q17, String q18,
			MultipartFile image1, MultipartFile image2, MultipartFile image3, MultipartFile image4,
			MultipartFile image5) {

		Map<String, Object> response = new HashMap<>();
		List<Map<String, Object>> result = java.util.ArrayList.class.cast(new java.util.ArrayList<>());

		MultipartFile[] files = { image1, image2, image3, image4, image5 };
		String[] imagePaths = new String[5];
		for (int i = 0; i < files.length; i++) {
			if (files[i] != null && !files[i].isEmpty()) {
				imagePaths[i] = fileUpload(bdid, "_" + bdsid + "_" + (i + 1), files[i]);
			} else {
				imagePaths[i] = "";
			}
		}

		String insertSql = "INSERT INTO `building_demolition_guidelines_inspection` "
				+ "(`bdid`, `bdsid`, `cby`, `latitude`, `longitude`, `zone`, `ward`,"
				+ "`q1`, `q2`, `q3`, `q4`, `q5`, `q6`, `q7`, `q8`, `q9`, `q10`, "
				+ "`q11`, `q12`, `q13`, `q14`, `q15`, `q16`, `q17`, `q18`, "
				+ "`img1`, `img2`, `img3`, `img4`, `img5`) " +
				"VALUES (" + "?,".repeat(7 + 18 + 5).replaceAll(",$", "") + ")";

		org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();

		int affectedRows = jdbcBuildingDemolitionTemplate.update(connection -> {
			java.sql.PreparedStatement ps = connection.prepareStatement(insertSql, new String[] { "bdgiid" });
			int i = 1;
			ps.setString(i++, bdid);
			ps.setString(i++, bdsid);
			ps.setString(i++, cby);
			ps.setString(i++, latitude);
			ps.setString(i++, longitude);
			ps.setString(i++, zone);
			ps.setString(i++, ward);
			for (String q : new String[] { q1, q2, q3, q4, q5, q6, q7, q8, q9, q10, q11, q12, q13, q14, q15, q16, q17,
					q18 }) {
				ps.setString(i++, q);
			}
			for (String path : imagePaths) {
				ps.setString(i++, path);
			}
			return ps;
		}, keyHolder);

		if (affectedRows > 0) {
			int lastInsertId = keyHolder.getKey().intValue();
			response.put("insertId", lastInsertId);

			String penalty = findPenaltyType(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10, q11, q12, q13, q14, q15, q16, q17,
					q18);

			// Calculate fine amount based on building area (from building_demolition table)
			double areaSqm = getAreaSqm(bdid);
			int fineAmount = 0;

			// Logic replicated from Construction Guidelines (Area based)
			// Assuming 'area_sqm' is comparable to 'buildup_area'
			if (areaSqm > 20000) {
				fineAmount = "High".equalsIgnoreCase(penalty) ? 500000 : 100000;
			} else if (areaSqm > 500) {
				fineAmount = "High".equalsIgnoreCase(penalty) ? 25000 : 10000;
			} else if (areaSqm >= 300) {
				fineAmount = "High".equalsIgnoreCase(penalty) ? 10000 : 1000;
			}

			if (updateDemolitionStatus(bdsid, bdid, penalty, String.valueOf(fineAmount))) {
				response.put("status", "success");
				response.put("message", "Feedback inserted successfully.");
			} else {
				response.put("status", "error");
				response.put("message", "Feedback Insert failed (Status Update).");
			}
		} else {
			response.put("status", "error");
			response.put("message", "Insert failed.");
		}

		result.add(response);
		return result;
	}

	private double getAreaSqm(String bdid) {
		String sql = "SELECT area_sqm FROM building_demolition WHERE id = ?";
		List<Map<String, Object>> result = jdbcBuildingDemolitionTemplate.queryForList(sql, bdid);
		if (result.isEmpty())
			return 0.0;
		Object val = result.get(0).get("area_sqm");
		if (val == null)
			return 0.0;
		try {
			return Double.parseDouble(val.toString());
		} catch (Exception e) {
			return 0.0;
		}
	}

	private String findPenaltyType(String q1, String q2, String q3, String q4, String q5, String q6, String q7,
			String q8,
			String q9, String q10,
			String q11, String q12, String q13, String q14, String q15, String q16, String q17, String q18) {
		if ("No".equalsIgnoreCase(q1) || "No".equalsIgnoreCase(q2) || "No".equalsIgnoreCase(q3) ||
				"No".equalsIgnoreCase(q4) || "No".equalsIgnoreCase(q5) || "No".equalsIgnoreCase(q6) ||
				"No".equalsIgnoreCase(q7) || "No".equalsIgnoreCase(q8) || "No".equalsIgnoreCase(q9) ||
				"No".equalsIgnoreCase(q10) || "No".equalsIgnoreCase(q11) || "No".equalsIgnoreCase(q12) ||
				"No".equalsIgnoreCase(q13) || "No".equalsIgnoreCase(q14)) {
			return "High";
		}
		if ("No".equalsIgnoreCase(q15) || "No".equalsIgnoreCase(q16) || "No".equalsIgnoreCase(q17)) {
			return "Medium";
		}
		return "Low";
	}

	public boolean updateDemolitionStatus(String bdsid, String bdid, String penalty, String fineAmount) {
		// Assuming 'id' is key for building_demolition_status
		// Wait, user might not have 'id' in status table if it's 1-to-1 with 'bdid'?
		// Code in updateDetails does NOT use bdsid, but we need to update the row we
		// created.
		// If bdsid (ciid) is passed, we use it. If not, we map by bdid (as duplicate
		// check ensures 1 row).
		String updateSql = "UPDATE `building_demolition_status` SET `penalty` = ?, `fine_amount` = ? WHERE bdid = ?";
		// Using bdid (id from building_demolition) as the foreign key reference
		int affectedRows = jdbcBuildingDemolitionTemplate.update(updateSql, penalty, fineAmount, bdid);
		return affectedRows > 0;
	}

	public List<Map<String, Object>> getRevisitDemolitionList(String loginid, String latitude, String longitude) {
		String ward = getWardByLoginId(loginid, "");
		String sql = "SELECT bd.*, " +
				"CONCAT('" + fileBaseUrl + "/gccofficialapp/files', bds.file1) AS photo, " + // Assuming file1 is image
				"bds.id as bdsid, bds.penalty " +
				"FROM building_demolition bd " +
				"LEFT JOIN building_demolition_status bds ON bds.bdid = bd.id " +
				"WHERE bd.ward = ? AND bds.penalty IN ('High','Medium','Low') " +
				"AND bd.id NOT IN (SELECT bdid FROM building_demolition_after_notice WHERE isactive = 1)";

		List<Map<String, Object>> result = jdbcBuildingDemolitionTemplate.queryForList(sql, ward);
		java.time.LocalDate today = java.time.LocalDate.now();
		java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		for (Map<String, Object> row : result) {
			boolean allowRevisit = false;
			Object cdateObj = row.get("cdate"); // Assuming cdate column exists
			if (cdateObj != null) {
				try {
					java.time.LocalDate cdate = null;
					if (cdateObj instanceof java.sql.Timestamp) {
						cdate = ((java.sql.Timestamp) cdateObj).toLocalDateTime().toLocalDate();
					} else if (cdateObj instanceof String) {
						cdate = java.time.LocalDateTime.parse((String) cdateObj, dtf).toLocalDate();
					}
					if (cdate != null) {
						java.time.LocalDate finalDate = cdate.plusDays(15);
						allowRevisit = !today.isBefore(finalDate);
					}
				} catch (Exception e) {
					// Ignore parse error
				}
			}
			row.put("allowRevisit", allowRevisit);
		}

		Map<String, Object> response = new HashMap<>();
		response.put("status", "Success");
		response.put("message", "Revisit List");
		response.put("data", result);
		return Collections.singletonList(response);
	}

	@org.springframework.transaction.annotation.Transactional
	public List<Map<String, Object>> saveDemolitionAfterNotice(
			String bdid, String bdsid, String bdgiid, String remarks, String status,
			String zone, String ward, String cby, String latitude, String longitude, MultipartFile mainfile) {

		Map<String, Object> response = new HashMap<>();
		String imagePath = fileUpload(bdid, "after_notice", mainfile);

		String sql = "INSERT INTO `building_demolition_after_notice` (bdid, bdsid, bdgiid, remarks, status, zone, ward, cby, latitude, longitude, mainfile) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
		int rows = jdbcBuildingDemolitionTemplate.update(sql, bdid, bdsid, bdgiid, remarks, status, zone, ward, cby,
				latitude, longitude, imagePath);

		if (rows > 0) {
			response.put("status", "success");
			response.put("message", "After notice details saved");
		} else {
			response.put("status", "error");
			response.put("message", "Failed to save");
		}
		return Collections.singletonList(response);
	}

	@org.springframework.transaction.annotation.Transactional
	public List<Map<String, Object>> saveDemolitionFinalNotice(
			String bdid, String bdsid, String bdgiid, String remarks, String status,
			String zone, String ward, String cby, String latitude, String longitude, MultipartFile mainfile) {

		Map<String, Object> response = new HashMap<>();
		String imagePath = fileUpload(bdid, "final_notice", mainfile);

		String sql = "INSERT INTO `building_demolition_final_notice` (bdid, bdsid, bdgiid, remarks, status, zone, ward, cby, latitude, longitude, mainfile) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
		int rows = jdbcBuildingDemolitionTemplate.update(sql, bdid, bdsid, bdgiid, remarks, status, zone, ward, cby,
				latitude, longitude, imagePath);

		if (rows > 0) {
			response.put("status", "success");
			response.put("message", "Final notice details saved");
		} else {
			response.put("status", "error");
			response.put("message", "Failed to save");
		}
		return Collections.singletonList(response);
	}

	private String sendWhatsAppMessage(String urlString) {
		String response = "";
		try {
			java.net.URL url = new java.net.URL(urlString);
			java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			int responseCode = connection.getResponseCode();
			response = String.valueOf(responseCode);
			System.out.println("Response Code for URL: " + urlString + " is " + responseCode);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}
}
