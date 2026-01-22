package in.gov.chennaicorporation.mobileservice.gccstraydogs.service;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;

import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

@Service
public class UserService {
	@PersistenceContext
	private EntityManager entityManager;
	private JdbcTemplate jdbcStraydogsTemplate;
	private final Environment environment;
	private String fileBaseUrl;

	@Autowired
	public void setDataSource(@Qualifier("mysqlGccStraydogsDataSource") DataSource straydogsDataSource) {
		this.jdbcStraydogsTemplate = new JdbcTemplate(straydogsDataSource);
	}

	@Autowired
	public UserService(Environment environment) {
		this.environment = environment;
		this.fileBaseUrl = environment.getProperty("fileBaseUrl");
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
		String serviceFolderName = environment.getProperty("straydogs_foldername");
		var year = DateTimeUtil.getCurrentYear();
		var month = DateTimeUtil.getCurrentMonth();

		uploadDirectory = uploadDirectory + serviceFolderName + year + "/" + month + "/";

		try {
			// Create directory if it doesn't exist
			Path directoryPath = Paths.get(uploadDirectory);
			if (!Files.exists(directoryPath)) {
				Files.createDirectories(directoryPath);
			}

			// Datetime string
			String datetimetxt = DateTimeUtil.getCurrentDateTime();
			// File name
			String fileName = name + "" + id + "" + datetimetxt + "_" + file.getOriginalFilename();
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

	@Transactional
	public List<Map<String, Object>> getABCCenter(String latitude, String longitude) {
		String sqlQuery = "";

		sqlQuery = "SELECT * " + "FROM abc_centers " + "WHERE (isactive=1 AND isdelete=0) AND "
				+ " ((6371008.8 * acos(  ROUND(cos(radians(?)) * cos(radians(latitude)) * cos(radians(longitude) - radians(?)) +   sin(radians(?)) * sin(radians(latitude)), 9)   )) < 500)";
		List<Map<String, Object>> result = jdbcStraydogsTemplate.queryForList(sqlQuery,
				new Object[] { latitude, longitude, latitude });
		Map<String, Object> response = new HashMap<>();

		response.put("status", "success");
		response.put("message", "Request Information");
		response.put("Data", result);

		return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> getABCCenterList(){
		String sqlQuery ="";
		
		sqlQuery="SELECT * "
				+ "FROM abc_centers ";
		List<Map<String, Object>> result = jdbcStraydogsTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
	    
	    response.put("status", "success");
        response.put("message", "Request Information");
        response.put("Data", result);
        
        return Collections.singletonList(response);
	}

	@Transactional
	public String saveStep1(String qr_id, String userid_1, Timestamp date_1, String lat_1, String long_1,
			String location_1, String zone_1, String ward_1, String streetid_1, String streetname_1, MultipartFile file, String sex_1,
			boolean contraception_1, int stage, boolean flow_location) {

		String filename = fileUpload("straydogs", "step1", file);

		String sql = "INSERT INTO stray_dog_logs (qr_id, userid_1, date_1, lat_1, long_1, location_1, zone_1, ward_1, streetid_1, streetname_1, photo_1, sex_1, contraception_1, stage, flow_location) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try {

			int rowsAffected = jdbcStraydogsTemplate.update(sql, qr_id, userid_1, date_1, lat_1, long_1, location_1,
					zone_1, ward_1, streetid_1, streetname_1, filename, sex_1, contraception_1, stage, flow_location);

			if (rowsAffected > 0) {
				return Integer.toString(stage);
			} else {
				return "Error: No rows inserted";
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "Error in Saving Step_1 give proper input";
		}
	}

	@Transactional
	public String saveStep2(String qr_id, String userid_2, Timestamp date_2, String lat_2, String long_2,
			String location_2, String zone_2, String ward_2, String streetid_2, String streetname_2, String abc_center_2, MultipartFile file,
			String sex_2, boolean contraception_2, int stage) {
		try {
			String filename = fileUpload("straydogs", "step2", file);
			if (filename == null || filename.isEmpty()) {
				throw new IllegalArgumentException("File upload failed");
			}

			String sql = "UPDATE stray_dog_logs SET userid_2 = ?, date_2 = ?, lat_2 = ?, long_2 = ?, location_2 = ?, zone_2 = ?, ward_2 = ?, streetid_2 = ?, streetname_2 = ?, abc_center_2 = ?, photo_2 = ?, sex_2 = ?, contraception_2 = ?, stage = ? WHERE qr_id = ?";

			jdbcStraydogsTemplate.update(sql, userid_2, date_2, lat_2, long_2, location_2, zone_2, ward_2, streetid_2, streetname_2,
					abc_center_2, filename, sex_2, contraception_2, stage, qr_id);

			return Integer.toString(stage);

		} catch (Exception e) {
			e.printStackTrace();
			return "Database error while saving Step 2: " + e.getMessage();
		}
	}

	@Transactional
	public void saverfid(String qr_id, String rf_id) {

		String sql = "UPDATE stray_dog_logs SET rf_id = ? WHERE qr_id = ? ";
		try {
			jdbcStraydogsTemplate.update(sql, rf_id, qr_id);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Transactional
	public String saveStep3(String qr_id, String rf_id, String userid_3, String sex_3, String body_weight_3,
			String temperature_3, boolean surgery_fitness_3, String surgery_type_3, Timestamp surgery_date) {

		String sql = "INSERT INTO stray_dog_master (qr_id, rf_id, userid_3, sex_3, body_weight_3, temperature_3, surgery_fitness_3, surgery_type_3, surgery_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try {
			jdbcStraydogsTemplate.update(sql, qr_id, rf_id, userid_3, sex_3, body_weight_3, temperature_3,
					surgery_fitness_3, surgery_type_3, surgery_date);

			return qr_id;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Error in Saving Step_3 give proper input";
	}

	@Transactional
	public String saveStep4(String qr_id, String rf_id, String userid_4, boolean surgery_complication_4,
			String complication_type_4, boolean ready_to_release_4) {
		String sql = "UPDATE stray_dog_master SET userid_4 = ?, surgery_complication_4 = ?, complication_type_4 = ?, ready_to_release_4 = ? WHERE qr_id = ? AND rf_id = ?";
		try {
			jdbcStraydogsTemplate.update(sql, userid_4, surgery_complication_4, complication_type_4, ready_to_release_4,
					qr_id, rf_id);
			return qr_id;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Error in Saving Step_4 give proper input";
	}

	@Transactional
	public String saveNewStep5(String rf_id, String qr_id, String userid_5, boolean arv_5, boolean dhppi_5,
			MultipartFile file, String lat_5, String long_5, String location_5, String zone_5, String ward_5,
			String streetid_5, Timestamp date_5) {

		String filename = fileUpload("straydogs", "step5", file);

		String sql = "INSERT INTO stray_dog_master (rf_id, qr_id, userid_5, arv_5, dhppi_5, photo_5, lat_5, long_5, location_5, zone_5, ward_5, streetid_5, date_5) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?, ?)";
		try {
			jdbcStraydogsTemplate.update(sql, rf_id, qr_id, userid_5, arv_5, dhppi_5, filename, lat_5, long_5,
					location_5, zone_5, ward_5, streetid_5, date_5);
			return qr_id;
		} catch (Exception e) {
			e.printStackTrace();
			return "Error in Saving Step_5 give proper input";
		}
	}

	@Transactional
	public String saveUpdateStep5(String rf_id, String qr_id, String userid_5, boolean arv_5, boolean dhppi_5,
			MultipartFile file, String lat_5, String long_5, String location_5, String zone_5, String ward_5,
			String streetid_5, String streetname_5, Timestamp date_5) {

		String filename = fileUpload("straydogs", "step5", file);

		String sql = "UPDATE stray_dog_master SET userid_5 = ?, arv_5 = ?, dhppi_5 = ?, photo_5 = ?, lat_5 = ?, long_5 = ?, location_5 = ?, zone_5 = ?, ward_5 = ?, streetid_5 = ?, streetname_5 = ?, date_5 = ? WHERE rf_id = ? AND qr_id = ?";
		try {
			jdbcStraydogsTemplate.update(sql, userid_5, arv_5, dhppi_5, filename, lat_5, long_5, location_5, zone_5,
					ward_5, streetid_5, streetname_5, date_5, rf_id, qr_id);
			return qr_id;
		} catch (Exception e) {
			e.printStackTrace();
			return "Error in Saving Step_5 give proper input";
		}
	}

	@Transactional
	public String saveStep6(String qr_id, String userid_6, String lat_6, String long_6, String location_6,
			String zone_6, String ward_6, String streetid_6, String streetname_6, Timestamp date_6, MultipartFile file, int stage) {

		String filename = fileUpload("straydogs", "step6", file);

		String sql = "UPDATE stray_dog_logs SET userid_6 = ?, lat_6 = ?, long_6 = ?, location_6 = ?, zone_6 = ?, ward_6 = ?, streetid_6 = ?, streetname_6 = ?, date_6 = ?, photo_6 = ?, stage = ? WHERE qr_id = ?";
		try {
			jdbcStraydogsTemplate.update(sql, userid_6, lat_6, long_6, location_6, zone_6, ward_6, streetid_6, streetname_6, date_6,
					filename, stage, qr_id);
			return Integer.toString(stage);

		} catch (Exception e) {
			e.printStackTrace();
			return "Error in Saving Step_6 give proper input";
		}
	}

	@Transactional
	public List<Map<String, Object>> getMasterData(String rf_id) {
		String sql = "SELECT *, " + "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_5) AS step5_imageUrl, "

				+ "CASE WHEN surgery_fitness_3 = 1 THEN DATE_FORMAT(surgery_date, '%d-%m-%Y %r') ELSE '-' END AS formatted_surgery_datetime, "
				+ "CASE WHEN surgery_fitness_3 = 1 THEN DATE_FORMAT(surgery_date, '%d-%m-%Y') ELSE '-' END AS formatted_surgery_date, "
				+ "CASE WHEN surgery_fitness_3 = 1 THEN DATE_FORMAT(surgery_date, '%r') ELSE '-' END AS formatted_surgery_time, "

				+ "CASE WHEN arv_5 = 1 THEN DATE_FORMAT(date_5, '%d-%m-%Y %r') ELSE '-' END AS formatted_arv_datetime, "
				+ "CASE WHEN arv_5 = 1 THEN DATE_FORMAT(date_5, '%d-%m-%Y') ELSE '-' END AS formatted_arv_date, "
				+ "CASE WHEN arv_5 = 1 THEN DATE_FORMAT(date_5, '%r') ELSE '-' END AS formatted_arv_time "
				+ " FROM stray_dog_master WHERE rf_id = ?";

		// String sql = "SELECT * FROM stray_dog_master WHERE rf_id = ?";
		return jdbcStraydogsTemplate.queryForList(sql, rf_id);
	}

	@Transactional
	public List<Map<String, Object>> getLogData(String qr_id) {
		String sql = "SELECT *, " + "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_1) AS step1_imageUrl, "
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_2) AS step2_imageUrl, " + "CONCAT('"
				+ fileBaseUrl + "/gccofficialapp/files', photo_6) AS step6_imageUrl, "

				+ "CASE WHEN userid_1 IS NOT NULL THEN DATE_FORMAT(date_1, '%d-%m-%Y %r') ELSE '-' END AS formatted_stage_1_datetime, "
				+ "CASE WHEN userid_1 IS NOT NULL THEN DATE_FORMAT(date_1, '%d-%m-%Y') ELSE '-' END AS formatted_stage_1_date, "
				+ "CASE WHEN userid_1 IS NOT NULL THEN DATE_FORMAT(date_1, '%r') ELSE '-' END AS formatted_stage_1_time, "

				+ "CASE WHEN userid_2 IS NOT NULL THEN DATE_FORMAT(date_2, '%d-%m-%Y %r') ELSE '-' END AS formatted_stage_2_datetime, "
				+ "CASE WHEN userid_2 IS NOT NULL THEN DATE_FORMAT(date_2, '%d-%m-%Y') ELSE '-' END AS formatted_stage_2_date, "
				+ "CASE WHEN userid_2 IS NOT NULL THEN DATE_FORMAT(date_2, '%r') ELSE '-' END AS formatted_stage_2_time, "

				+ "CASE WHEN userid_6 IS NOT NULL THEN DATE_FORMAT(date_6, '%d-%m-%Y %r') ELSE '-' END AS formatted_stage_6_datetime, "
				+ "CASE WHEN userid_6 IS NOT NULL THEN DATE_FORMAT(date_6, '%d-%m-%Y') ELSE '-' END AS formatted_stage_6_date, "
				+ "CASE WHEN userid_6 IS NOT NULL THEN DATE_FORMAT(date_6, '%r') ELSE '-' END AS formatted_stage_6_time "

				+ " FROM stray_dog_logs WHERE qr_id = ?";
		return jdbcStraydogsTemplate.queryForList(sql, qr_id);
	}

	@Transactional
	public List<Map<String, Object>> getlogsrfid(String rf_id) {

		String sql = "SELECT *, " + "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_5) AS step5_imageUrl, "

				+ "CASE WHEN surgery_fitness_3 = 1 THEN DATE_FORMAT(surgery_date, '%d-%m-%Y %r') ELSE '-' END AS formatted_surgery_datetime, "
				+ "CASE WHEN surgery_fitness_3 = 1 THEN DATE_FORMAT(surgery_date, '%d-%m-%Y') ELSE '-' END AS formatted_surgery_date, "
				+ "CASE WHEN surgery_fitness_3 = 1 THEN DATE_FORMAT(surgery_date, '%r') ELSE '-' END AS formatted_surgery_time, "

				+ "CASE WHEN arv_5 = 1 THEN DATE_FORMAT(date_5, '%d-%m-%Y %r') ELSE '-' END AS formatted_arv_datetime, "
				+ "CASE WHEN arv_5 = 1 THEN DATE_FORMAT(date_5, '%d-%m-%Y') ELSE '-' END AS formatted_arv_date, "
				+ "CASE WHEN arv_5 = 1 THEN DATE_FORMAT(date_5, '%r') ELSE '-' END AS formatted_arv_time "

				+ "FROM stray_dog_master WHERE rf_id = ? ORDER BY rf_id DESC LIMIT 1";

		return jdbcStraydogsTemplate.queryForList(sql, rf_id);
	}

	@Transactional
	public String updateStage(String rf_id, String qr_id, int stage) {

		String sql = "UPDATE stray_dog_logs SET stage = ?, rf_id = ? WHERE qr_id = ?";
		jdbcStraydogsTemplate.update(sql, stage, rf_id, qr_id);

		return Integer.toString(stage);
	}

	@Transactional
	public int getVaccineDue(String rfId, String formattedDate) {

		String sql = "SELECT COALESCE(DATEDIFF(DATE_ADD(date_5, INTERVAL 1 YEAR), ?), 365) AS remaining_days\n"
				+ "FROM stray_dog_master\n" + "WHERE  rf_id = ? ORDER BY date_5  DESC LIMIT 1";

		List<Integer> days = jdbcStraydogsTemplate.queryForList(sql, new Object[] { formattedDate, rfId },
				Integer.class);
		if (days != null && !days.isEmpty()) {
			int day = days.get(0);

			return day;
		}
		return 99999;

	}

	public String updateStage3(String rf_id, String qr_id) {

		String sql = "UPDATE stray_dog_master SET qr_id = ? WHERE rf_id = ?";

		jdbcStraydogsTemplate.update(sql, qr_id, rf_id);

		return qr_id;
	}

	public String checkQrId(String qr_id) {

		String sql = "SELECT CASE " + "WHEN EXISTS(SELECT 1 FROM stray_dog_logs WHERE qr_id = ?) " + "THEN 'true' "
				+ "ELSE 'false' " + "END AS result";

		return jdbcStraydogsTemplate.queryForObject(sql, new Object[] { qr_id }, String.class);

	}

	public List<Map<String, Object>> getOverallData(String fromDate, String toDate) {
		String sql = "SELECT "
				+ "SUM(CASE WHEN date(date_1) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d') AND sex_1 = 'Male' THEN 1 ELSE 0 END) AS Male_Catched, "
				+ "SUM(CASE WHEN date(date_1) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d') AND sex_1 = 'Female' THEN 1 ELSE 0 END) AS Female_Catched, "
				+ "SUM(CASE WHEN date(date_1) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d') THEN 1 ELSE 0 END) AS Total_Catched, "
				+ "SUM(CASE WHEN date(date_6) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d') AND sex_2 = 'Male' THEN 1 ELSE 0 END) AS Male_Released, "
				+ "SUM(CASE WHEN date(date_6) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d') AND sex_2 = 'Female' THEN 1 ELSE 0 END) AS Female_Released, "
				+ "SUM(CASE WHEN date(date_6) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d') THEN 1 ELSE 0 END) AS Total_Released "
				+ "FROM stray_dog_logs";

		Map<String, Object> resultMap = jdbcStraydogsTemplate.queryForMap(sql, fromDate, toDate, fromDate, toDate,
				fromDate, toDate, fromDate, toDate, fromDate, toDate, fromDate, toDate);

		Map<String, Integer> catchedData = new HashMap<>();
		Map<String, Integer> releasedData = new HashMap<>();

		resultMap.forEach((key, value) -> {
			int intValue = (value instanceof BigDecimal) ? ((BigDecimal) value).intValue() : (Integer) value;
			if (key.contains("Catched")) {
				catchedData.put(key, intValue);
			} else if (key.contains("Released")) {
				releasedData.put(key, intValue);
			}
		});

		Map<String, Object> catchedResult = new HashMap<>();
		catchedResult.put("flag", "Catched");
		catchedResult.put("data", catchedData);

		Map<String, Object> releasedResult = new HashMap<>();
		releasedResult.put("flag", "Released");
		releasedResult.put("data", releasedData);

		List<Map<String, Object>> resultList = new ArrayList<>();
		resultList.add(catchedResult);
		resultList.add(releasedResult);

		return resultList;
	}

	public List<Map<String, Object>> getoverallcatcheddata(String fromDate, String toDate) {
		String sql = "SELECT \r\n" + "    z.zone_name AS Zone,\r\n"
				+ "    COALESCE(SUM(CASE WHEN s.sex_1 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Catched,\r\n"
				+ "    COALESCE(SUM(CASE WHEN s.sex_1 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Catched,\r\n"
				+ "    COALESCE(COUNT(s.sex_1), 0) AS Total_Catched\r\n" + "FROM \r\n" + "    zone z\r\n"
				+ "LEFT JOIN \r\n" + "    stray_dog_logs s ON z.zone_name = s.zone_1 \r\n"
				+ "    AND date(s.date_1) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "GROUP BY \r\n" + "    z.zone_name";
		return jdbcStraydogsTemplate.queryForList(sql, fromDate, toDate);
	}

	public List<Map<String, Object>> getoverallreleaseddata(String fromDate, String toDate) {
		String sql = "SELECT \r\n" + "    z.zone_name AS Zone,\r\n"
				+ "    COALESCE(SUM(CASE WHEN s.sex_2 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Released,\r\n"
				+ "    COALESCE(SUM(CASE WHEN s.sex_2 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Released,\r\n"
				+ "    COALESCE(COUNT(s.sex_2), 0) AS Total_Released\r\n" + "FROM \r\n" + "    zone z\r\n"
				+ "LEFT JOIN \r\n" + "    stray_dog_logs s ON z.zone_name = s.zone_6 \r\n"
				+ "    AND date(s.date_6) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "GROUP BY \r\n" + "    z.zone_name";
		return jdbcStraydogsTemplate.queryForList(sql, fromDate, toDate);
	}

	public List<Map<String, Object>> getAbcCenterwiseData(String fromDate, String toDate, String abcCenter) {

		Map<String, Object> unsterilized = getunsterilizeddata(fromDate, toDate, abcCenter);
		Map<String, Object> sterilized = getsterilizeddata(fromDate, toDate, abcCenter);
		Map<String, Object> unfit = getunfitdata(fromDate, toDate, abcCenter);
		Map<String, Object> surgery = getsurgerydata(fromDate, toDate, abcCenter);
		Map<String, Object> died = getdieddata(fromDate, toDate, abcCenter);
		Map<String, Object> released = getreleaseddata(fromDate, toDate, abcCenter);

		Map<String, Object> unsterilizedResult = new HashMap<>();
		unsterilizedResult.put("flag", "Unsterilized");
		unsterilizedResult.put("data", unsterilized);

		Map<String, Object> sterilizedResult = new HashMap<>();
		sterilizedResult.put("flag", "Sterilized");
		sterilizedResult.put("data", sterilized);

		Map<String, Object> unfitResult = new HashMap<>();
		unfitResult.put("flag", "Unfit");
		unfitResult.put("data", unfit);

		Map<String, Object> surgeryResult = new HashMap<>();
		surgeryResult.put("flag", "Surgery");
		surgeryResult.put("data", surgery);

		Map<String, Object> diedResult = new HashMap<>();
		diedResult.put("flag", "Died");
		diedResult.put("data", died);

		Map<String, Object> releasedResult = new HashMap<>();
		releasedResult.put("flag", "Released");
		releasedResult.put("data", released);

		List<Map<String, Object>> resultList = new ArrayList<>();
		resultList.add(unsterilizedResult);
		resultList.add(sterilizedResult);
		resultList.add(unfitResult);
		resultList.add(surgeryResult);
		resultList.add(diedResult);
		resultList.add(releasedResult);

		return resultList;
	}

	private Map<String, Object> getreleaseddata(String fromDate, String toDate, String abcCenter) {
		String sql = "SELECT \r\n"
				+ "    IFNULL(SUM(CASE WHEN sl.sex_2 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_released,\r\n"
				+ "    IFNULL(SUM(CASE WHEN sl.sex_2 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_released,\r\n"
				+ "    COUNT(*) AS Total_released\r\n" + "FROM \r\n" + "    stray_dog_logs sl\r\n" + "WHERE \r\n"
				+ "    date(sl.date_6) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.abc_center_2 = ?";
		return jdbcStraydogsTemplate.queryForMap(sql, fromDate, toDate, abcCenter);
	}

	private Map<String, Object> getdieddata(String fromDate, String toDate, String abcCenter) {
		String sql = "SELECT \r\n"
				+ "    IFNULL(SUM(CASE WHEN sm.sex_3 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Died,\r\n"
				+ "    IFNULL(SUM(CASE WHEN sm.sex_3 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Died,\r\n"
				+ "    COUNT(*) AS Total_Died\r\n" + "FROM \r\n" + "    stray_dog_master sm\r\n"
				+ "    Left join stray_dog_logs sl on sm.rf_id = sl.rf_id\r\n" + "WHERE \r\n"
				+ "    date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sm.ready_to_release_4 = false\r\n" + "    AND sl.abc_center_2 = ?";
		return jdbcStraydogsTemplate.queryForMap(sql, fromDate, toDate, abcCenter);
	}

	private Map<String, Object> getsurgerydata(String fromDate, String toDate, String abcCenter) {
		String sql = " SELECT \r\n"
				+ "    IFNULL(SUM(CASE WHEN sm.sex_3 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Surgery,\r\n"
				+ "    IFNULL(SUM(CASE WHEN sm.sex_3 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Surgery,\r\n"
				+ "    COUNT(*) AS Total_Surgery\r\n" + "FROM \r\n" + "    stray_dog_master sm\r\n"
				+ "    Left join stray_dog_logs sl on sm.rf_id = sl.rf_id\r\n" + "WHERE \r\n"
				+ "    date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sm.userid_4 is not null\r\n" + "    AND sl.abc_center_2 = ?";
		return jdbcStraydogsTemplate.queryForMap(sql, fromDate, toDate, abcCenter);
	}

	private Map<String, Object> getunfitdata(String fromDate, String toDate, String abcCenter) {
		String sql = " SELECT \r\n"
				+ "    IFNULL(SUM(CASE WHEN sm.sex_3 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Unfit,\r\n"
				+ "    IFNULL(SUM(CASE WHEN sm.sex_3 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Unfit,\r\n"
				+ "    COUNT(*) AS Total_Unfit\r\n" + "FROM \r\n" + "    stray_dog_master sm\r\n"
				+ "    Left join stray_dog_logs sl on sm.rf_id = sl.rf_id\r\n" + "WHERE \r\n"
				+ "    date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sm.surgery_fitness_3 = false\r\n" + "    AND sl.abc_center_2 = ?";
		return jdbcStraydogsTemplate.queryForMap(sql, fromDate, toDate, abcCenter);
	}

	private Map<String, Object> getsterilizeddata(String fromDate, String toDate, String abcCenter) {
		String sql = "  SELECT \r\n"
				+ "    IFNULL(SUM(CASE WHEN sl.sex_2 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Sterilized,\r\n"
				+ "    IFNULL(SUM(CASE WHEN sl.sex_2 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Sterilized,\r\n"
				+ "    COUNT(*) AS Total_Sterilized\r\n" + "FROM \r\n" + "    stray_dog_logs sl\r\n" + "WHERE \r\n"
				+ "    date(sl.date_2) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.contraception_2 = true\r\n" + "    AND sl.abc_center_2 = ?";
		return jdbcStraydogsTemplate.queryForMap(sql, fromDate, toDate, abcCenter);
	}

	private Map<String, Object> getunsterilizeddata(String fromDate, String toDate, String abcCenter) {
		String sql = "SELECT \r\n"
				+ "    IFNULL(SUM(CASE WHEN sl.sex_2 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Unsterilized,\r\n"
				+ "    IFNULL(SUM(CASE WHEN sl.sex_2 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Unsterilized,\r\n"
				+ "    COUNT(*) AS Total_Unsterilized\r\n" + "FROM \r\n" + "    stray_dog_logs sl\r\n" + "WHERE \r\n"
				+ "   date(sl.date_2) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.contraception_2 = false\r\n" + "    AND sl.abc_center_2 = ?";
		return jdbcStraydogsTemplate.queryForMap(sql, fromDate, toDate, abcCenter);
	}

	public List<Map<String, Object>> getUnstrelizedZoneBreakup(String fromDate, String toDate, String abcCenter) {
		String sql = "SELECT \r\n" + "    z.zone_name AS Zone,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Unsterilized,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Unsterilized,\r\n"
				+ "    COALESCE(COUNT(sl.sex_2), 0) AS Total_Unsterilized\r\n" + "FROM \r\n" + "    zone z\r\n"
				+ "LEFT JOIN \r\n" + "    stray_dog_logs sl ON sl.zone_1 =  z.zone_name\r\n"
				+ "    AND  date(sl.date_2) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND  sl.contraception_2 = false\r\n" + "     AND sl.abc_center_2 = ?\r\n" + "GROUP BY \r\n"
				+ "    z.zone_name";
		return jdbcStraydogsTemplate.queryForList(sql, fromDate, toDate, abcCenter);
	}

	public List<Map<String, Object>> getUnstrelizedDivisionBreakup(String fromDate, String toDate, String abcCenter,
			String zone) {
		String sql = "SELECT \r\n" + "    d.div_name AS Division,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Unsterilized,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Unsterilized,\r\n"
				+ "    COALESCE(COUNT(sl.sex_2), 0) AS Total_Unsterilized\r\n" + "FROM \r\n" + "    division d \r\n"
				+ "LEFT JOIN \r\n" + "    stray_dog_logs sl ON d.div_name = sl.ward_1 \r\n" + "LEFT JOIN \r\n"
				+ "    zone z ON d.zone_id = z.zone_id \r\n"
				+ "    where date(sl.date_2) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.contraception_2 = false\r\n" + "    AND sl.abc_center_2 = ?\r\n"
				+ "    AND z.zone_name = ?\r\n" + "GROUP BY \r\n" + "    d.div_name";
		return jdbcStraydogsTemplate.queryForList(sql, fromDate, toDate, abcCenter, zone);
	}

	public List<Map<String, Object>> getUnstrelizedStreetBreakup(String ward, String fromDate, String toDate,
			String abcCenter, String zone) {
		String sql = "SELECT \r\n" + "    sl.streetid_1 as Street_ID,  sl.streetname_1 as Street_Name,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Unsterilized,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Unsterilized,\r\n"
				+ "    COALESCE(COUNT(sl.sex_2), 0) AS Total_Unsterilized\r\n" + "FROM \r\n"
				+ "    stray_dog_logs sl\r\n" + "    where\r\n" + "    sl.ward_1 = ? \r\n"
				+ "    AND date(sl.date_2) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.abc_center_2 = ?\r\n" + "    AND sl.zone_1 = ?\r\n"
				+ "     AND sl.contraception_2 = false\r\n" + "GROUP BY \r\n" + "    sl.streetid_1, sl.streetname_1";
		return jdbcStraydogsTemplate.queryForList(sql, ward, fromDate, toDate, abcCenter, zone);
	}

	public List<Map<String, Object>> getStrelizedZoneBreakup(String fromDate, String toDate, String abcCenter) {
		String sql = " SELECT \r\n" + "    z.zone_name AS Zone,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Sterilized,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Sterilized,\r\n"
				+ "    COALESCE(COUNT(sl.sex_2), 0) AS Total_sterilized\r\n" + "FROM \r\n" + "    zone z\r\n"
				+ "LEFT JOIN \r\n" + "    stray_dog_logs sl ON sl.zone_1 =  z.zone_name\r\n"
				+ "    AND  date(sl.date_2) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND  sl.contraception_2 = true\r\n" + "     AND sl.abc_center_2 = ?\r\n" + "GROUP BY \r\n"
				+ "    z.zone_name";
		return jdbcStraydogsTemplate.queryForList(sql, fromDate, toDate, abcCenter);
	}

	public List<Map<String, Object>> getStrelizedDivisionBreakup(String fromDate, String toDate, String abcCenter,
			String zone) {
		String sql = "SELECT \r\n" + "    d.div_name AS Division,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Sterilized,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Sterilized,\r\n"
				+ "    COALESCE(COUNT(sl.sex_2), 0) AS Total_Sterilized\r\n" + "FROM \r\n" + "    division d \r\n"
				+ "LEFT JOIN \r\n" + "    stray_dog_logs sl ON d.div_name = sl.ward_1 \r\n" + "LEFT JOIN \r\n"
				+ "    zone z ON d.zone_id = z.zone_id \r\n"
				+ "    where date(sl.date_2) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.contraception_2 = true\r\n" + "    AND sl.abc_center_2 = ?\r\n"
				+ "    AND z.zone_name = ?\r\n" + "GROUP BY \r\n" + "    d.div_name";
		return jdbcStraydogsTemplate.queryForList(sql, fromDate, toDate, abcCenter, zone);
	}

	public List<Map<String, Object>> getStrelizedStreetBreakup(String ward, String fromDate, String toDate,
			String abcCenter, String zone) {
		String sql = "SELECT \r\n" + "    sl.streetid_1 as Street_ID,  sl.streetname_1 as Street_Name,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Sterilized,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Sterilized,\r\n"
				+ "    COALESCE(COUNT(sl.sex_2), 0) AS Total_Sterilized\r\n" + "FROM \r\n" + "    stray_dog_logs sl\r\n"
				+ "    where\r\n" + "    sl.ward_1 = ? \r\n"
				+ "    AND date(sl.date_2) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.abc_center_2 = ?\r\n" + "    AND sl.zone_1 = ?\r\n"
				+ "     AND sl.contraception_2 = true\r\n" + "GROUP BY \r\n" + "    sl.streetid_1, sl.streetname_1";
		return jdbcStraydogsTemplate.queryForList(sql, ward, fromDate, toDate, abcCenter, zone);
	}

	public List<Map<String, Object>> getUnfitZoneBreakup(String fromDate, String toDate, String abcCenter) {
		String sql = " SELECT  \r\n" + "     z.zone_name as Zone,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Unfit,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Unfit,\r\n"
				+ "    COALESCE(COUNT(sm.sex_3), 0) AS Total_Unfit\r\n" + "FROM \r\n" + "   zone z \r\n"
				+ "left JOIN stray_dog_logs sl ON z.zone_name =  sl.zone_1\r\n"
				+ "LEFT JOIN stray_dog_master sm ON  sl.rf_id =  sm.rf_id\r\n"
				+ "     AND  date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND  sm.surgery_fitness_3 = false\r\n" + "	AND  sl.abc_center_2 = ?\r\n"
				+ "GROUP BY z.zone_name";
		return jdbcStraydogsTemplate.queryForList(sql, fromDate, toDate, abcCenter);
	}

	public List<Map<String, Object>> getUnfitDivisionBreakup(String fromDate, String toDate, String abcCenter,
			String zone) {
		String sql = "SELECT \r\n" + "    sl.ward_1 AS Division,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Unfit,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Unfit,\r\n"
				+ "    COALESCE(COUNT(sm.sex_3), 0) AS Total_Unfit\r\n" + "FROM \r\n" + "    division d \r\n"
				+ "left JOIN stray_dog_logs sl ON d.div_name = sl.ward_1    \r\n"
				+ "left JOIN stray_dog_master sm ON  sl.rf_id =  sm.rf_id\r\n"
				+ "left JOIN zone z ON d.zone_id = z.zone_id\r\n"
				+ "   where date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "   AND sm.surgery_fitness_3 = false\r\n" + "   AND sl.abc_center_2 = ?\r\n"
				+ "   AND sl.zone_1 = ?\r\n" + "GROUP BY \r\n" + "    sl.ward_1";
		return jdbcStraydogsTemplate.queryForList(sql, fromDate, toDate, abcCenter, zone);
	}

	public List<Map<String, Object>> getUnfitStreetBreakup(String ward, String zone, String fromDate, String toDate,
			String abcCenter) {
		String sql = "  SELECT \r\n" + "    sl.streetid_1 as Street_ID,  sl.streetname_1 as Street_Name,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Unfit,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Unfit,\r\n"
				+ "    COALESCE(COUNT(sm.sex_3), 0) AS Total_Unfit\r\n" + "FROM \r\n" + "    stray_dog_logs sl\r\n"
				+ "    LEFT JOIN stray_dog_master sm ON  sm.rf_id =  sl.rf_id\r\n" + "     where\r\n"
				+ "    sl.ward_1 = ? \r\n" + "    AND sl.zone_1 = ?\r\n"
				+ "    AND date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.abc_center_2 = ?\r\n" + "      AND  sm.surgery_fitness_3 = false\r\n" + "GROUP BY \r\n"
				+ "    sl.streetid_1, sl.streetname_1";
		return jdbcStraydogsTemplate.queryForList(sql, ward, zone, fromDate, toDate, abcCenter);
	}

	public List<Map<String, Object>> getSurgeryZoneBreakup(String fromDate, String toDate, String abcCenter) {
		String sql = " SELECT  \r\n" + "     z.zone_name as Zone,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Surgery,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Surgery,\r\n"
				+ "    COALESCE(COUNT(sm.sex_3), 0) AS Total_Surgery\r\n" + "FROM \r\n" + "   zone z \r\n"
				+ "left JOIN stray_dog_logs sl ON z.zone_name =  sl.zone_1\r\n"
				+ "LEFT JOIN stray_dog_master sm ON  sl.rf_id =  sm.rf_id\r\n"
				+ "     AND  date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "     AND sm.userid_4 is not null\r\n" + "	AND  sl.abc_center_2 = ?\r\n" + "GROUP BY z.zone_name";
		return jdbcStraydogsTemplate.queryForList(sql, fromDate, toDate, abcCenter);
	}

	public List<Map<String, Object>> getSurgeryDivisionBreakup(String fromDate, String toDate, String abcCenter,
			String zone) {
		String sql = "    SELECT \r\n" + "    sl.ward_1 AS Division,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Surgery,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Female' THEN 1 ELSE 0 END), 0) AS female_Surgery,\r\n"
				+ "    COALESCE(COUNT(sm.sex_3), 0) AS total_Surgery\r\n" + "FROM \r\n" + "    division d \r\n"
				+ "left JOIN stray_dog_logs sl ON d.div_name = sl.ward_1    \r\n"
				+ "left JOIN stray_dog_master sm ON  sl.rf_id =  sm.rf_id\r\n"
				+ "left JOIN zone z ON d.zone_id = z.zone_id\r\n"
				+ "   where date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sm.userid_4 is not null\r\n" + "   AND sl.abc_center_2 = ?\r\n" + "   AND sl.zone_1 = ?\r\n"
				+ "GROUP BY \r\n" + "    sl.ward_1";
		return jdbcStraydogsTemplate.queryForList(sql, fromDate, toDate, abcCenter, zone);
	}

	public List<Map<String, Object>> getSurgeryStreetBreakup(String ward, String zone, String fromDate, String toDate,
			String abcCenter) {
		String sql = " SELECT \r\n" + "    sl.streetid_1 as Street_ID,  sl.streetname_1 as Street_Name,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Surgery,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Surgery,\r\n"
				+ "    COALESCE(COUNT(sm.sex_3), 0) AS Total_Surgery\r\n" + "FROM \r\n" + "    stray_dog_logs sl\r\n"
				+ "    LEFT JOIN stray_dog_master sm ON  sl.rf_id =  sm.rf_id\r\n" + "     where\r\n"
				+ "    sl.ward_1 = ? \r\n" + "    AND sl.zone_1 = ?\r\n"
				+ "    AND date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.abc_center_2 = ?\r\n" + "      AND sm.userid_4 is not null\r\n" + "GROUP BY \r\n"
				+ "    sl.streetid_1, sl.streetname_1";
		return jdbcStraydogsTemplate.queryForList(sql, ward, zone, fromDate, toDate, abcCenter);
	}

	public List<Map<String, Object>> getDiedZoneBreakup(String fromDate, String toDate, String abcCenter) {
		String sql = " SELECT  \r\n" + "         z.zone_name as Zone,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Died,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Died,\r\n"
				+ "    COALESCE(COUNT(sm.sex_3), 0) AS Total_Died\r\n" + "FROM \r\n" + "   zone z \r\n"
				+ "left JOIN stray_dog_logs sl ON z.zone_name =  sl.zone_1\r\n"
				+ "LEFT JOIN stray_dog_master sm ON  sl.rf_id =  sm.rf_id\r\n"
				+ "     AND  date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "      AND sm.ready_to_release_4 = false\r\n" + "	AND  sl.abc_center_2 = ?\r\n"
				+ "GROUP BY z.zone_name";
		return jdbcStraydogsTemplate.queryForList(sql, fromDate, toDate, abcCenter);
	}

	public List<Map<String, Object>> getDiedDivisionBreakup(String fromDate, String toDate, String abcCenter,
			String zone) {
		String sql = " SELECT \r\n" + "    sl.ward_1 AS Division,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Died,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Died,\r\n"
				+ "    COALESCE(COUNT(sm.sex_3), 0) AS Total_Died\r\n" + "FROM \r\n" + "    division d \r\n"
				+ "left JOIN stray_dog_logs sl ON d.div_name = sl.ward_1    \r\n"
				+ "left JOIN stray_dog_master sm ON  sl.rf_id =  sm.rf_id\r\n"
				+ "left JOIN zone z ON d.zone_id = z.zone_id\r\n"
				+ "   where date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sm.ready_to_release_4 = false\r\n" + "   AND sl.abc_center_2 = ?\r\n"
				+ "   AND sl.zone_1 = ?\r\n" + "GROUP BY \r\n" + "    sl.ward_1";
		return jdbcStraydogsTemplate.queryForList(sql, fromDate, toDate, abcCenter, zone);
	}

	public List<Map<String, Object>> getDiedStreetBreakup(String ward, String zone, String fromDate, String toDate,
			String abcCenter) {
		String sql = " SELECT \r\n" + "    sl.streetid_1 as Street_ID,  sl.streetname_1 as Street_Name,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Died,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sm.sex_3 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Died,\r\n"
				+ "    COALESCE(COUNT(sm.sex_3), 0) AS Total_Died\r\n" + "FROM \r\n" + "    stray_dog_logs sl\r\n"
				+ "    LEFT JOIN stray_dog_master sm ON  sl.rf_id =  sm.rf_id\r\n" + "     where\r\n"
				+ "    sl.ward_1 = ? \r\n" + "    AND sl.zone_1 = ?\r\n"
				+ "    AND date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.abc_center_2 = ?\r\n" + "     AND sm.ready_to_release_4 = false\r\n" + "GROUP BY \r\n"
				+ "    sl.streetid_1, sl.streetname_1";
		return jdbcStraydogsTemplate.queryForList(sql, ward, zone, fromDate, toDate, abcCenter);
	}

	public List<Map<String, Object>> getReleasedZoneBreakup(String fromDate, String toDate, String abcCenter) {
		String sql = "SELECT \r\n" + "    z.zone_name AS Zone,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Released,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Released,\r\n"
				+ "    COALESCE(COUNT(sl.sex_2), 0) AS Total_Released\r\n" + "FROM \r\n" + "    zone z\r\n"
				+ "LEFT JOIN \r\n" + "    stray_dog_logs sl ON z.zone_name = sl.zone_6 \r\n"
				+ "    AND date(sl.date_6) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.abc_center_2 = ?\r\n" + "GROUP BY \r\n" + "z.zone_name";
		return jdbcStraydogsTemplate.queryForList(sql, fromDate, toDate, abcCenter);
	}

	public List<Map<String, Object>> getReleasedDivisionBreakup(String fromDate, String toDate, String abcCenter,
			String zone) {
		String sql = " SELECT \r\n" + "    d.div_name AS Division,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Released,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Released,\r\n"
				+ "    COALESCE(COUNT(sl.sex_2), 0) AS Total_Released\r\n" + "FROM \r\n" + "    division d \r\n"
				+ "LEFT JOIN \r\n" + "    stray_dog_logs sl ON d.div_name = sl.ward_6\r\n" + "LEFT JOIN \r\n"
				+ "    zone z ON d.zone_id = z.zone_id \r\n"
				+ "    where date(sl.date_6) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.abc_center_2 = ?\r\n" + "    AND sl.zone_6 = ?\r\n" + "GROUP BY \r\n" + "    d.div_name";
		return jdbcStraydogsTemplate.queryForList(sql, fromDate, toDate, abcCenter, zone);
	}

	public List<Map<String, Object>> getReleasedStreetBreakup(String ward, String zone, String fromDate, String toDate,
			String abcCenter) {
		String sql = " SELECT \r\n" + "    sl.streetid_6 AS Street_ID,  sl.streetname_6 as Street_Name,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Male' THEN 1 ELSE 0 END), 0) AS Male_Released,\r\n"
				+ "    COALESCE(SUM(CASE WHEN sl.sex_2 = 'Female' THEN 1 ELSE 0 END), 0) AS Female_Released,\r\n"
				+ "    COALESCE(COUNT(sl.sex_2), 0) AS Total_Released\r\n" + "FROM \r\n" + "    stray_dog_logs sl\r\n"
				+ "WHERE\r\n" + "    sl.ward_6 = ?\r\n" + "    AND sl.zone_6 = ?\r\n"
				+ "    AND DATE(sl.date_6) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.abc_center_2 = ?\r\n" + "GROUP BY \r\n" + "    sl.streetid_6, sl.streetname_6";
		return jdbcStraydogsTemplate.queryForList(sql, ward, zone, fromDate, toDate, abcCenter);
	}

	public ByteArrayInputStream overallExcelReport(String fromDate, String toDate) {
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			List<Map<String, Object>> catchedData = getoverallcatcheddata(fromDate, toDate);
			List<Map<String, Object>> releasedData = getoverallreleaseddata(fromDate, toDate);

			
			if (catchedData.isEmpty() || releasedData.isEmpty()) {
				throw new RuntimeException("No data found for the report. Please check the database.");
			}

			Sheet sheet = workbook.createSheet("Dog Catching Report");

			// Define header and cell styles
			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerStyle.setFont(headerFont);
			headerStyle.setAlignment(HorizontalAlignment.CENTER);
			headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			headerStyle.setBorderBottom(BorderStyle.THIN);
			headerStyle.setBorderTop(BorderStyle.THIN);
			headerStyle.setBorderLeft(BorderStyle.THIN);
			headerStyle.setBorderRight(BorderStyle.THIN);

			// First header row with the report title
			Row titleRow = sheet.createRow(0);
			Cell titleCell = titleRow.createCell(0);
			titleCell.setCellValue("Zonewise Overall Report (From: " + fromDate + " to To: " + toDate + ")");
			titleCell.setCellStyle(headerStyle);
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

			// Group header row
			Row groupHeaderRow = sheet.createRow(1);

			// Merging cells (0,1) in groupHeaderRow with headerRow1
			sheet.addMergedRegion(new CellRangeAddress(1, 2, 0, 0)); // Merge Sl. No column
			sheet.addMergedRegion(new CellRangeAddress(1, 2, 1, 1)); // Merge Zone column

			// Set merged header cells
			Cell slNoHeader = groupHeaderRow.createCell(0);
			slNoHeader.setCellValue("Sl. No");
			slNoHeader.setCellStyle(headerStyle);

			Cell zoneHeader = groupHeaderRow.createCell(1);
			zoneHeader.setCellValue("Zone");
			zoneHeader.setCellStyle(headerStyle);

			// Create merged regions and set only the main cell style for catches and
			// released headers
			Cell catchesHeader = groupHeaderRow.createCell(2);
			catchesHeader.setCellValue("No. of Dogs Cought");
			catchesHeader.setCellStyle(headerStyle);
			sheet.addMergedRegion(new CellRangeAddress(1, 1, 2, 4));

			Cell releasedHeader = groupHeaderRow.createCell(5);
			releasedHeader.setCellValue("No. of Dogs Released");
			releasedHeader.setCellStyle(headerStyle);
			sheet.addMergedRegion(new CellRangeAddress(1, 1, 5, 7));

			// Column headers row (second row with individual column headers)
			Row headerRow1 = sheet.createRow(2);
			headerRow1.createCell(0).setCellValue("Sl. No");
			headerRow1.createCell(1).setCellValue("Zone");
			headerRow1.createCell(2).setCellValue("Male");
			headerRow1.createCell(3).setCellValue("Female");
			headerRow1.createCell(4).setCellValue("Total");
			headerRow1.createCell(5).setCellValue("Male");
			headerRow1.createCell(6).setCellValue("Female");
			headerRow1.createCell(7).setCellValue("Total");

			// Apply header styles to the individual columns in headerRow1
			for (int i = 0; i <= 7; i++) {
				headerRow1.getCell(i).setCellStyle(headerStyle);
			}

			// Populate data rows
			CellStyle regularStyle = workbook.createCellStyle();
			regularStyle.setBorderBottom(BorderStyle.THIN);
			regularStyle.setBorderTop(BorderStyle.THIN);
			regularStyle.setBorderLeft(BorderStyle.THIN);
			regularStyle.setBorderRight(BorderStyle.THIN);
			regularStyle.setAlignment(HorizontalAlignment.CENTER);

			int rowIndex = 3; // Start below the header rows
			int slNo = 1;
			for (Map<String, Object> catched : catchedData) {

				Row excelRow = sheet.createRow(rowIndex++);
				excelRow.createCell(0).setCellValue(slNo++); // Sl. No
				excelRow.createCell(1).setCellValue((String) catched.get("Zone")); // Zone
				excelRow.createCell(2).setCellValue(((Number) catched.get("Male_Catched")).doubleValue()); // Male
																											// Catches
				excelRow.createCell(3).setCellValue(((Number) catched.get("Female_Catched")).doubleValue()); // Female
																												// Catches
				excelRow.createCell(4).setCellValue(((Number) catched.get("Total_Catched")).doubleValue()); // Total
																											// Catches

				for (Map<String, Object> released : releasedData) {
					if ((((String) released.get("Zone")).equalsIgnoreCase((String) catched.get("Zone")))) {
						excelRow.createCell(5).setCellValue(((Number) released.get("Male_Released")).doubleValue()); // Male
																														// Released
						excelRow.createCell(6).setCellValue(((Number) released.get("Female_Released")).doubleValue()); // Female
																														// Released
						excelRow.createCell(7).setCellValue(((Number) released.get("Total_Released")).doubleValue()); // Total
																														// Released
					}
				}
				for (int col = 0; col <= 7; col++) {
					excelRow.getCell(col).setCellStyle(regularStyle);
				}
			}

			// Auto-size columns
			for (int i = 0; i < 8; i++) {
				sheet.autoSizeColumn(i);
			}

			workbook.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException("Failed to generate Excel report", e);
		} catch (Exception e) {
			throw new RuntimeException("An error occurred while generating the report: " + e.getMessage(), e);
		}
	}

	public ByteArrayInputStream abcCenterExcelReport(String fromDate, String toDate, String abcCenter) {
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			List<Map<String, Object>> unsterilizedData = getUnstrelizedZoneBreakup(fromDate, toDate, abcCenter);
			List<Map<String, Object>> sterilizedData = getStrelizedZoneBreakup(fromDate, toDate, abcCenter);
			List<Map<String, Object>> unfitData = getUnfitZoneBreakup(fromDate, toDate, abcCenter);
			List<Map<String, Object>> surgeryData = getSurgeryZoneBreakup(fromDate, toDate, abcCenter);
			List<Map<String, Object>> diedData = getDiedZoneBreakup(fromDate, toDate, abcCenter);
			List<Map<String, Object>> releasedData = getReleasedZoneBreakup(fromDate, toDate, abcCenter);

			if (unsterilizedData.isEmpty() || sterilizedData.isEmpty() || unfitData.isEmpty() || surgeryData.isEmpty()
					|| diedData.isEmpty() || releasedData.isEmpty()) {
				throw new RuntimeException("No data found for the report. Please check the database.");
			}

			Sheet sheet = workbook.createSheet("Dog Catching Report");

			// Define header and cell styles
			CellStyle headerStyle = createHeaderStyle(workbook);
			CellStyle regularStyle = createRegularCellStyle(workbook);

			// First header row with the report title
			Row titleRow = sheet.createRow(0);
			for (int col = 0; col <= 19; col++) {
				Cell cell = titleRow.createCell(col);
				cell.setCellStyle(headerStyle);
			}
			Cell titleCell = titleRow.createCell(0);
			titleCell.setCellValue("Daily Report GCC " + abcCenter + " ABC Center Data (From: " + fromDate + " to To: "
					+ toDate + ")");
			titleCell.setCellStyle(headerStyle);
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 19));

			// Re-apply bottom border for merged title row
			for (int col = 0; col <= 19; col++) {
				Cell cell = titleRow.getCell(col);
				CellStyle titleStyle = workbook.createCellStyle();
				titleStyle.cloneStyleFrom(headerStyle);
				titleStyle.setBorderBottom(BorderStyle.THIN);
				cell.setCellStyle(titleStyle);
			}

			// Group header row
			Row groupHeaderRow = sheet.createRow(1);
			sheet.addMergedRegion(new CellRangeAddress(1, 2, 0, 0)); // Merge Sl. No column
			sheet.addMergedRegion(new CellRangeAddress(1, 2, 1, 1)); // Merge Zone column

			// Set merged header cells
			Cell slNoHeader = groupHeaderRow.createCell(0);
			slNoHeader.setCellValue("Sl. No");
			slNoHeader.setCellStyle(headerStyle);

			Cell zoneHeader = groupHeaderRow.createCell(1);
			zoneHeader.setCellValue("Zone");
			zoneHeader.setCellStyle(headerStyle);

			// Set merged header cells
			createMergedHeaderCell(groupHeaderRow, 2, "No. of Unsterilized Dogs Received from each zone", headerStyle,
					sheet, 2, 4);
			createMergedHeaderCell(groupHeaderRow, 5, "No. of Sterilized Dogs Received from each zone", headerStyle,
					sheet, 5, 7);
			createMergedHeaderCell(groupHeaderRow, 8, "No. of Dogs Unfit for Surgery", headerStyle, sheet, 8, 10);
			createMergedHeaderCell(groupHeaderRow, 11, "No. of ABC Surgery Conducted", headerStyle, sheet, 11, 13);
			createMergedHeaderCell(groupHeaderRow, 14, "No. of Dogs Died", headerStyle, sheet, 14, 16);
			createMergedHeaderCell(groupHeaderRow, 17, "No. of Dog Released", headerStyle, sheet, 17, 19);

			// Column headers row (second row with individual column headers)
			Row headerRow1 = sheet.createRow(2);
			String[] headers = { "Sl. No", "Zone", "Male", "Female", "Total", "Male", "Female", "Total", "Male",
					"Female", "Total", "Male", "Female", "Total", "Male", "Female", "Total", "Male", "Female",
					"Total" };

			for (int col = 0; col < headers.length; col++) {
				Cell cell = headerRow1.createCell(col);
				cell.setCellValue(headers[col]);
				cell.setCellStyle(headerStyle);
			}

			// Populate data rows
			int rowIndex = 3; // Start below the header rows
			int slNo = 1;
			for (Map<String, Object> unsterilized : unsterilizedData) {
				Row excelRow = sheet.createRow(rowIndex++);
				excelRow.createCell(0).setCellValue(slNo++); // Sl. No
				excelRow.createCell(1).setCellValue((String) unsterilized.get("Zone")); // Zone
				excelRow.createCell(2).setCellValue(((Number) unsterilized.get("Male_Unsterilized")).doubleValue());
				excelRow.createCell(3).setCellValue(((Number) unsterilized.get("Female_Unsterilized")).doubleValue());
				excelRow.createCell(4).setCellValue(((Number) unsterilized.get("Total_Unsterilized")).doubleValue());

				for (Map<String, Object> sterilized : sterilizedData) {
					if (((String) sterilized.get("Zone")).equalsIgnoreCase((String) unsterilized.get("Zone"))) {
						excelRow.createCell(5).setCellValue(((Number) sterilized.get("Male_Sterilized")).doubleValue());
						excelRow.createCell(6)
								.setCellValue(((Number) sterilized.get("Female_Sterilized")).doubleValue());
						excelRow.createCell(7)
								.setCellValue(((Number) sterilized.get("Total_sterilized")).doubleValue());
						break;
					}
				}

				for (Map<String, Object> unfit : unfitData) {
					if (((String) unfit.get("Zone")).equalsIgnoreCase((String) unsterilized.get("Zone"))) {
						excelRow.createCell(8).setCellValue(((Number) unfit.get("Male_Unfit")).doubleValue());
						excelRow.createCell(9).setCellValue(((Number) unfit.get("Female_Unfit")).doubleValue());
						excelRow.createCell(10).setCellValue(((Number) unfit.get("Total_Unfit")).doubleValue());
						break;
					}
				}

				for (Map<String, Object> surgery : surgeryData) {
					if (((String) surgery.get("Zone")).equalsIgnoreCase((String) unsterilized.get("Zone"))) {
						excelRow.createCell(11).setCellValue(((Number) surgery.get("Male_Surgery")).doubleValue());
						excelRow.createCell(12).setCellValue(((Number) surgery.get("Female_Surgery")).doubleValue());
						excelRow.createCell(13).setCellValue(((Number) surgery.get("Total_Surgery")).doubleValue());
						break;
					}
				}

				for (Map<String, Object> died : diedData) {
					if (((String) died.get("Zone")).equalsIgnoreCase((String) unsterilized.get("Zone"))) {
						excelRow.createCell(14).setCellValue(((Number) died.get("Male_Died")).doubleValue());
						excelRow.createCell(15).setCellValue(((Number) died.get("Female_Died")).doubleValue());
						excelRow.createCell(16).setCellValue(((Number) died.get("Total_Died")).doubleValue());
						break;
					}
				}

				for (Map<String, Object> released : releasedData) {
					if (((String) released.get("Zone")).equalsIgnoreCase((String) unsterilized.get("Zone"))) {
						excelRow.createCell(17).setCellValue(((Number) released.get("Male_Released")).doubleValue());
						excelRow.createCell(18).setCellValue(((Number) released.get("Female_Released")).doubleValue());
						excelRow.createCell(19).setCellValue(((Number) released.get("Total_Released")).doubleValue());
						break;
					}
				}

				// Apply the regularStyle to all populated cells in the row
				for (int col = 0; col <= 19; col++) {
					excelRow.getCell(col).setCellStyle(regularStyle);
				}
			}

			// Auto-size columns
			for (int i = 0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);
			}

			workbook.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException("Failed to generate Excel report", e);
		} catch (Exception e) {
			throw new RuntimeException("An error occurred while generating the report: " + e.getMessage(), e);
		}
	}

	// Helper method for setting merged header cells with style and applying border
	// to all cells in the range
	private void createMergedHeaderCell(Row row, int colIndex, String value, CellStyle style, Sheet sheet, int fromCol,
			int toCol) {
		// Set value in the main cell and apply style
		Cell mainCell = row.createCell(colIndex);
		mainCell.setCellValue(value);
		mainCell.setCellStyle(style);

		// Merge cells
		sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), fromCol, toCol));

		// Apply style to each cell in the merged region to ensure borders are visible
		for (int col = fromCol; col <= toCol; col++) {
			Cell cell = row.getCell(col);
			if (cell == null) { // Create cells if they don’t exist
				cell = row.createCell(col);
			}
			cell.setCellStyle(style);
		}
	}

	// Define cell styles for headers and regular cells
	private CellStyle createHeaderStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setBold(true);
		style.setFont(font);
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		return style;
	}

	private CellStyle createRegularCellStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		return style;
	}

	public List<Map<String, Object>> getZoneList() {
		String sql = "SELECT NAME FROM abc_centers as Name";
		List<Map<String, Object>> resultMapList = jdbcStraydogsTemplate.queryForList(sql);
		return resultMapList;
	}

	public List<Map<String, Object>> getUnstrelizedDogsBreakup(String ward, String fromDate, String toDate,
			String abcCenter, String zone, String streetID, String sex) {
		String sql = "SELECT *,"
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_1) AS step1_imageUrl, "
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_2) AS step2_imageUrl, " 
				+ "CONCAT('"+ fileBaseUrl + "/gccofficialapp/files', photo_5) AS step5_imageUrl, "
				+ "CONCAT('"+ fileBaseUrl + "/gccofficialapp/files', photo_6) AS step6_imageUrl "
			+" FROM \r\n" + "    stray_dog_master sm\r\n"
				+ "    LEFT JOIN stray_dog_logs sl ON sm.rf_id = sl.rf_id\r\n" + "    WHERE\r\n"
				+ "    sl.ward_1 = ? \r\n"
				+ "    AND date(sl.date_2) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.abc_center_2 = ?\r\n" + "    AND sl.zone_1 = ?  \r\n" + "    AND sl.streetid_1 = ?\r\n"
				+ "	AND sl.contraception_2 = false\r\n" + "	AND sl.sex_2 = ?";

		List<Map<String, Object>> data = jdbcStraydogsTemplate.queryForList(sql, ward, fromDate, toDate, abcCenter,
				zone, streetID, sex);

		return data;
	}

	public List<Map<String, Object>> getStrelizedDogsBreakup(String ward, String fromDate, String toDate,
			String abcCenter, String zone, String streetID, String sex) {

		String sql = "SELECT *,"
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_1) AS step1_imageUrl, "
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_2) AS step2_imageUrl, " 
				+ "CONCAT('"+ fileBaseUrl + "/gccofficialapp/files', photo_5) AS step5_imageUrl, "
				+ "CONCAT('"+ fileBaseUrl + "/gccofficialapp/files', photo_6) AS step6_imageUrl "
		+ " FROM \r\n" + "    stray_dog_master sm\r\n"
				+ "    left join stray_dog_logs sl on sm.rf_id = sl.rf_id\r\n" + "    where\r\n"
				+ "    sl.ward_1 = ? \r\n"
				+ "    AND date(sl.date_2) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "   AND sl.abc_center_2 = ?\r\n" + "    AND sl.zone_1 = ?  \r\n" + "    and sl.streetid_1 = ?\r\n"
				+ "     AND sl.contraception_2 = true\r\n" + "     and sl.sex_2 = ?";

		List<Map<String, Object>> data = jdbcStraydogsTemplate.queryForList(sql, ward, fromDate, toDate, abcCenter,
				zone, streetID, sex);

		return data;

	}

	public List<Map<String, Object>> getUnfitDogsBreakup(String ward, String fromDate, String toDate, String abcCenter,
			String zone, String streetID, String sex) {

		String sql = " SELECT *," 
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_1) AS step1_imageUrl, "
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_2) AS step2_imageUrl, " 
				+ "CONCAT('"+ fileBaseUrl + "/gccofficialapp/files', photo_5) AS step5_imageUrl, "
				+ "CONCAT('"+ fileBaseUrl + "/gccofficialapp/files', photo_6) AS step6_imageUrl "
		+ " FROM \r\n" + "    stray_dog_master sm\r\n"
				+ "    left join stray_dog_logs sl on sm.rf_id = sl.rf_id\r\n" + "    where\r\n"
				+ "    sl.ward_1 = ? \r\n"
				+ "    AND date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "   AND sl.abc_center_2 = ?\r\n" + "    AND sl.zone_1 = ?  \r\n" + "    and sl.streetid_1 = ?\r\n"
				+ "     AND sm.surgery_fitness_3 = false\r\n" + "     and sl.sex_2 = ?";
		List<Map<String, Object>> data = jdbcStraydogsTemplate.queryForList(sql, ward, fromDate, toDate, abcCenter,
				zone, streetID, sex);

		return data;
	}

	public List<Map<String, Object>> getSurgeryDogsBreakup(String ward, String fromDate, String toDate,
			String abcCenter, String zone, String streetID, String sex) {
		String sql = "SELECT *, "
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_1) AS step1_imageUrl, "
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_2) AS step2_imageUrl, " 
				+ "CONCAT('"+ fileBaseUrl + "/gccofficialapp/files', photo_5) AS step5_imageUrl, "
				+ "CONCAT('"+ fileBaseUrl + "/gccofficialapp/files', photo_6) AS step6_imageUrl "
				+ " FROM \r\n" + "    stray_dog_master sm\r\n"
				+ "    left join stray_dog_logs sl on sm.rf_id = sl.rf_id\r\n" + "    where\r\n"
				+ "    sl.ward_1 = ? \r\n"
				+ "    AND date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "   AND sl.abc_center_2 = ?\r\n" + "    AND sl.zone_1 = ?  \r\n" + "    and sl.streetid_1 = ?\r\n"
				+ "     AND  sm.userid_4 is not null\r\n" + "     and sl.sex_2 = ?";

		List<Map<String, Object>> data = jdbcStraydogsTemplate.queryForList(sql, ward, fromDate, toDate, abcCenter,
				zone, streetID, sex);

		return data;
	}

	public List<Map<String, Object>> getDiedDogsBreakup(String ward, String fromDate, String toDate, String abcCenter,
			String zone, String streetID, String sex) {
		String sql = " SELECT *,"
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_1) AS step1_imageUrl, "
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_2) AS step2_imageUrl, " 
				+ "CONCAT('"+ fileBaseUrl + "/gccofficialapp/files', photo_5) AS step5_imageUrl, "
				+ "CONCAT('"+ fileBaseUrl + "/gccofficialapp/files', photo_6) AS step6_imageUrl "
				+ " FROM \r\n" + "    stray_dog_master sm\r\n"
				+ "    left join stray_dog_logs sl on sm.rf_id = sl.rf_id\r\n" + "    where\r\n"
				+ "    sl.ward_1 = ? \r\n"
				+ "    AND date(sm.surgery_date) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "   AND sl.abc_center_2 = ?\r\n" + "    AND sl.zone_1 = ?  \r\n" + "    and sl.streetid_1 = ?\r\n"
				+ "	AND  sm.ready_to_release_4 = false\r\n" + "     and sl.sex_2 = ?";

		List<Map<String, Object>> data = jdbcStraydogsTemplate.queryForList(sql, ward, fromDate, toDate, abcCenter,
				zone, streetID, sex);

		return data;
	}

	public List<Map<String, Object>> getReleasedDogsBreakup(String ward, String fromDate, String toDate,
			String abcCenter, String zone, String streetID, String sex) {
		String sql = "SELECT *," 
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_1) AS step1_imageUrl, "
				+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', photo_2) AS step2_imageUrl, " 
				+ "CONCAT('"+ fileBaseUrl + "/gccofficialapp/files', photo_5) AS step5_imageUrl, "
				+ "CONCAT('"+ fileBaseUrl + "/gccofficialapp/files', photo_6) AS step6_imageUrl "
				+ " FROM \r\n" + "    stray_dog_master sm\r\n"
				+ "    left join stray_dog_logs sl on sm.rf_id = sl.rf_id\r\n" + "    where\r\n"
				+ "     sl.ward_6 = ? \r\n"
				+ "     AND date(sl.date_6) BETWEEN STR_TO_DATE(?, '%Y-%m-%d') AND STR_TO_DATE(?, '%Y-%m-%d')\r\n"
				+ "    AND sl.abc_center_2 = ?\r\n" + "    AND sl.zone_6 = ?  \r\n" + "     and sl.streetid_6 = ?\r\n"
				+ "    and sl.sex_2 = ?";

		List<Map<String, Object>> data = jdbcStraydogsTemplate.queryForList(sql, ward, fromDate, toDate, abcCenter,
				zone, streetID, sex);

		return data;
	}

	@Transactional
	public String saveBuildings(String location, String latitude, String longitude, String street_id, String street_name) {

		
		String sql = "INSERT INTO buildings (location, latitude, longitude, street_id, street_name) VALUES (?, ?, ?, ?, ?)";
		try {

			int rowsAffected = jdbcStraydogsTemplate.update(sql, location, latitude, longitude, street_id, street_name);
          return "Saved Sucessfuly";
		} catch (Exception e) {
			e.printStackTrace();
			return "Error in Saving Step_1 give proper input";
		}
	}
	
	@Transactional
	public List<Map<String, Object>> getBuildings(String latitude, String longitude) {
		String sqlQuery = "";

		sqlQuery = "SELECT * " + "FROM buildings " + "WHERE (is_active=1 AND is_delete=0) AND "
				+ " ((6371008.8 * acos(  ROUND(cos(radians(?)) * cos(radians(latitude)) * cos(radians(longitude) - radians(?)) +   sin(radians(?)) * sin(radians(latitude)), 9)   )) < 500)";
		List<Map<String, Object>> result = jdbcStraydogsTemplate.queryForList(sqlQuery,
				new Object[] { latitude, longitude, latitude });
		Map<String, Object> response = new HashMap<>();

		response.put("status", "success");
		response.put("message", "Request Information");
		response.put("Data", result);

		return Collections.singletonList(response);
	}


}