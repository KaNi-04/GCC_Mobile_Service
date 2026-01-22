package in.gov.chennaicorporation.mobileservice.gccHealthCamp.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class HealthCampService {

	@Autowired
	private JdbcTemplate jdbcHealthCampTemplate;

	private final Environment environment;
	private String fileBaseUrl;

	@Autowired
	public void setDataSource(@Qualifier("mysqlGccMedicalCampDataSource") DataSource healthCampDataSource) {
		this.jdbcHealthCampTemplate = new JdbcTemplate(healthCampDataSource);
	}

	public HealthCampService(Environment environment) {
		this.environment = environment;
		this.fileBaseUrl = environment.getProperty("fileBaseUrl");
	}

	public List<Map<String, Object>> getAllHealthCamp() {
		String sql = " select id,name from camp_type where is_active=1 AND is_delete=0 ";
		return jdbcHealthCampTemplate.queryForList(sql);
	}

	public List<Map<String, Object>> getAlldisease() {
		String sql = " select id,disease_name from disease_master where is_active=1 AND is_delete=0 ";
		return jdbcHealthCampTemplate.queryForList(sql);
	}

	public List<Map<String, Object>> getAllgender() {
		String sql = " select id,gender from gender_master where is_active=1 AND is_delete=0 ";
		return jdbcHealthCampTemplate.queryForList(sql);
	}

	public String getWardByLoginId(String loginid, String type) {
        String sqlQuery = "SELECT ward FROM gcc_penalty_hoardings.hoading_user_list WHERE userid = ? AND type = ? LIMIT 1";

        List<Map<String, Object>> results = jdbcHealthCampTemplate.queryForList(sqlQuery, loginid, type);

        if (!results.isEmpty()) {
            System.out.println("Ward....." + results);
            // Extract the ward value from the first result
            return (String) results.get(0).get("ward");
        }

        return "000";
    }
	
	public List<Map<String, Object>> getCampByDate(String userid) {
		String ward = getWardByLoginId(userid, "si");
		String sql = "select id,camp_name from camp_details WHERE ward = ? AND DATE(cdate) = CURDATE() AND is_active=1 AND is_delete=0 ";
		return jdbcHealthCampTemplate.queryForList(sql,ward);
	}

	public String fileUpload(MultipartFile file, String name) {

		// Set the file path where you want to save it
		String uploadDirectory = environment.getProperty("file.upload.directory");
		String serviceFolderName = environment.getProperty("healthcamp_folder");

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
			String fileName = name + "_" + datetimetxt + "_" + file.getOriginalFilename();
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

	// Function to convert date string from dd-MM-yyyy to yyyy-MM-dd
	public static String convertDateFormat(String inputDate, int add) {
	    // Check if the inputDate is not null or blank
	    if (inputDate != null && !inputDate.isBlank()) {
	        try {
	            // Define the input and output date formats
	            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	            // Parse the input date string to LocalDate
	            LocalDate date = LocalDate.parse(inputDate, inputFormatter);

	            // Add the specified number of days to the date if 'add' is greater than 0
	            if (add > 0) {
	                date = date.plusDays(add);
	            }

	            // Format the date to the new format
	            return date.format(outputFormatter);

	        } catch (DateTimeParseException e) {
	            // Handle the case where the input date is invalid
	            System.out.println("Invalid date format: " + inputDate);
	            return "";
	        }
	    } else {
	        return "";
	    }
	}
		
	public String saveHealthCampDetails(int camptypeId, String latitude, String longitude, String zone, String ward,
			String address, String campName, String campPhotoPath, String userid, 
			String streetName,String doc_name,String mo_name,String si_name) {

		String sql = "INSERT INTO camp_details ("

				+ "camp_type_id, latitude, longitude, zone, ward, address, camp_name, camp_img, cby,"
				+ "street_name, doc_name, mo_name, si_name "

				+ ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try {
			int result = jdbcHealthCampTemplate.update(sql, camptypeId, latitude, longitude, zone, ward, address,
					campName, campPhotoPath, userid, streetName, doc_name, mo_name, si_name);

			return result > 0 ? "success" : "failed";
		} catch (Exception e) {
			e.printStackTrace();
			return "failed";
		}
	}

	public String savePatientDetails(int campId, String pat_name, String pat_age, int genderId, String mobileNo,
			String pat_address,

			String otherDisease, String remarks, String userid, List<String> diseaseIds) {

		String sql = "INSERT INTO patient_details ("

				+ " camp_id, pt_name, pt_age, pt_gender_id, pt_mob_no, pt_address, pt_other_disease, remarks, cby"

				+ ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();

		try {
			int result = jdbcHealthCampTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql, new String[] { "id" });
				ps.setInt(1, campId);
				ps.setString(2, pat_name);
				ps.setString(3, pat_age);
				ps.setInt(4, genderId);
				ps.setString(5, mobileNo);
				ps.setString(6, pat_address);
//					ps.setInt(7, diseaseId);
				ps.setString(7, otherDisease);
				ps.setString(8, remarks);
				ps.setString(9, userid);

				return ps;
			}, keyHolder);
			// return keyHolder.getKey().intValue();
			if (result > 0) {
				int generatedId = keyHolder.getKey().intValue();
				String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
				String patientId = prefix + generatedId;

				String updateSql = " UPDATE patient_details SET patient_id = ? WHERE id = ? ";
				jdbcHealthCampTemplate.update(updateSql, patientId, generatedId);

				// INSERT MULTIPLE DISEASES
				String diseaseSql = "INSERT INTO diseases_details (patient_id, disease_id) VALUES (?, ?)";

				for (String id : (diseaseIds)) {
					int diseaseId = Integer.parseInt(id);
					jdbcHealthCampTemplate.update(diseaseSql, patientId, diseaseId);
				}

				return patientId;
			}
			// return "error";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "error";
	}
	

	// report related

	public Map<String, Object> getCampSummary(String date) {
		
		String datetxt = convertDateFormat(date,0);

		try {

			// Total Counts
			String totalSql = "SELECT COUNT(DISTINCT cd.id) AS total_camp_count, " + "COUNT(pd.id) AS total_pat_count "
					+ "FROM camp_details cd " + "LEFT JOIN patient_details pd ON cd.id = pd.camp_id "
					+ "WHERE DATE(cd.cdate) = ?";

			Map<String, Object> totalCounts = jdbcHealthCampTemplate.queryForMap(totalSql, datetxt);

			// Zone-wise Counts
			String zoneSql = "SELECT cd.zone, COUNT(DISTINCT cd.id) AS total_camp_count, "
					+ "COUNT(pd.id) AS total_pat_count " + "FROM camp_details cd "
					+ "LEFT JOIN patient_details pd ON cd.id = pd.camp_id " + "WHERE DATE(cd.cdate) = ? "
					+ "GROUP BY cd.zone";

			List<Map<String, Object>> zoneList = jdbcHealthCampTemplate.queryForList(zoneSql, datetxt);
			totalCounts.put("zoneList", zoneList);

			return totalCounts;
		} catch (Exception ex) {
			ex.printStackTrace();
			Map<String, Object> response = new HashMap<>();
			response.put("status", "No data found");
			return Collections.singletonMap("status", "No data found");

		}

	}

	public Map<String, Object> getWardwiseDetails(String date, String zoneId) {
		
		String datetxt = convertDateFormat(date,0);

		try {

			// Total Counts
			String totalSql = "SELECT COUNT(DISTINCT cd.id) AS total_camp_count, " + "COUNT(pd.id) AS total_pat_count "
					+ "FROM camp_details cd " + "LEFT JOIN patient_details pd ON cd.id = pd.camp_id "
					+ "WHERE DATE(cd.cdate) = ? AND cd.zone=?";

			Map<String, Object> totalCounts = jdbcHealthCampTemplate.queryForMap(totalSql, datetxt, zoneId);

			// Zone-wise Counts
			String zoneSql = "SELECT cd.ward, COUNT(DISTINCT cd.id) AS total_camp_count, "
					+ "COUNT(pd.id) AS total_pat_count " + "FROM camp_details cd "
					+ "LEFT JOIN patient_details pd ON cd.id = pd.camp_id " + "WHERE DATE(cd.cdate) = ? "
					+ "AND cd.zone=? GROUP BY cd.ward";

			List<Map<String, Object>> wardList = jdbcHealthCampTemplate.queryForList(zoneSql, datetxt, zoneId);
			totalCounts.put("wardList", wardList);

			return totalCounts;
		} catch (Exception ex) {
			ex.printStackTrace();
			Map<String, Object> response = new HashMap<>();
			response.put("status", "No data found");
			return Collections.singletonMap("status", "No data found");

		}

	}

	public List<Map<String, Object>> getCampwiseDetails(String date, String ward) {

		String datetxt = convertDateFormat(date,0);
		
		String sql = " Select cd.id, cd.zone, cd.ward, cd.camp_name,count(pd.id) as total_pat_count " + " FROM camp_details cd "
				+ " LEFT JOIN patient_details pd " + " ON cd.id = pd.camp_id "
				+ " WHERE DATE(cd.cdate) = ? and cd.ward = ? Group by cd.id ";

		return jdbcHealthCampTemplate.queryForList(sql, datetxt, ward);

	}
	
/*
	public List<Map<String, Object>> getWardwiseDetails(String date, String zoneId) {
		
		String datetxt = convertDateFormat(date,0);

		String sql = "select cd.ward,count(pd.id) as pat_count "
				
				+ "from camp_details cd "
				
				+ "left join patient_details pd on pd.camp_id = cd.id "
				
				+ "where cd.zone=? AND DATE(cd.cdate)=? AND cd.is_active=1 AND cd.is_delete=0 "
				
				+ "group by cd.ward ";

		return jdbcHealthCampTemplate.queryForList(sql,zoneId,datetxt );

	}
	*/
	public List<Map<String, Object>> getCampDetails(String campId) {

		String sql = " select pd.id, pd.patient_id, pd.pt_name, pd.pt_mob_no, "
				+ "	gm.gender, pd.pt_age, pd.pt_address, ct.name as camp_type, "
				+ "	cd.camp_name, cd.address,pd.remarks, " + " DATE_FORMAT(cd.cdate, '%d-%m-%Y') AS regDate, "
				+ " DATE_FORMAT(cd.cdate, '%h:%i %p') AS Time, "
				+ " GROUP_CONCAT(dm.disease_name SEPARATOR ', ') AS diseases " + "	FROM patient_details pd "
				+ "	LEFT JOIN camp_details cd ON pd.camp_id = cd.id "
				+ "	LEFT JOIN gender_master gm on pd.pt_gender_id = gm.id "
				+ "	LEFT JOIN camp_type ct on cd.camp_type_id = ct.id "
				+ " LEFT JOIN diseases_details dd ON pd.patient_id = dd.patient_id "
				+ " LEFT JOIN disease_master dm ON dd.disease_id = dm.id " + "	WHERE cd.id = ? " + " GROUP BY pd.id ";

		return jdbcHealthCampTemplate.queryForList(sql, campId);

	}

}
