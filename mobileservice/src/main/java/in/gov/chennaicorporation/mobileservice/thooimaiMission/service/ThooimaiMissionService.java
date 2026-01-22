package in.gov.chennaicorporation.mobileservice.thooimaiMission.service;

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
import java.time.format.DateTimeParseException;
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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class ThooimaiMissionService {
private JdbcTemplate jdbcTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlThooimaiMissionSource") DataSource ThooimaiMissionDataSource) {
		this.jdbcTemplate = new JdbcTemplate(ThooimaiMissionDataSource);
	}
    
    @Autowired
	public ThooimaiMissionService(Environment environment) {
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
		
	public String fileUpload(String name, String id, MultipartFile file, String filetype) {

		int lastInsertId = 0;
		// Set the file path where you want to save it
		String uploadDirectory = environment.getProperty("file.upload.directory");
		String serviceFolderName = environment.getProperty("thooimaimission_foldername");
		var year = DateTimeUtil.getCurrentYear();
		var month = DateTimeUtil.getCurrentMonth();
		var date = DateTimeUtil.getCurrentDay();

		uploadDirectory = uploadDirectory + serviceFolderName + filetype + "/" + year + "/" + month + "/" + date;

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

			String filepath_txt = "/" + serviceFolderName + filetype + "/" + year + "/" + month + "/" + date + "/" + fileName;

			// Create a new Path object
			Path path = Paths.get(filePath);

			// Get the bytes of the file
			byte[] bytes = file.getBytes();
			
			if(filetype.equalsIgnoreCase("image"))
			{
				// Compress the image
				BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
				byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50% quality
				// Write the bytes to the file
				Files.write(path, compressedBytes);
			}
			else {
				// Write the bytes to the file
				Files.write(path, bytes);
			}
				// Get current date & time
		        LocalDateTime now = LocalDateTime.now();
	
		        // Format date-time (optional)
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		        
				System.out.println("Date: "+ now.format(formatter));
				System.out.println("Activity: " + serviceFolderName);
				System.out.println("File Type: " + filetype);
				System.out.println("File Upload Path: " + filePath);
				System.out.println("File Path: " + filepath_txt);
				
			return filepath_txt;

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to save file " + file.getOriginalFilename());
			return "error";
		}
	}
	
	public String getWardByLoginId(String loginid, String type) {
        String sqlQuery = "SELECT ward FROM gcc_penalty_hoardings.hoading_user_list WHERE userid = ? LIMIT 1";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery, loginid);

        if (!results.isEmpty()) {
            //System.out.println("Ward....." + results);
            // Extract the ward value from the first result
            return (String) results.get(0).get("ward");
        }

        return "000";
    }
	
	public List<Map<String, Object>> getWasteTypeList() {
        String sql = "SELECT * FROM `waste_type` WHERE isactive=1 ORDER BY `name`";
        return jdbcTemplate.queryForList(sql);
    }
	
	@Transactional
	public List<Map<String, Object>> saveWasteData(
	        String zone,
	        String ward,
	        String cby,
	        String latitude,
	        String longitude,
	        String address,
	        String place_name,
	        String tonage,
	        //String type,
	        String remarks,
	        MultipartFile file_1
	) {

		String filetxt = cby;
		
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    // For Image
	    String filetype = "image";
	    String action = "identified_waste";
	    
	    // for Image 1
	    String file1 = fileUpload(action, filetxt, file_1, filetype);
	    
	    if ("error".equalsIgnoreCase(file1)) {
	        response.put("status", "error");
	        response.put("message", "Image-1 insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    String insertSqltxt = "INSERT INTO `waste_location_mapping` "
	    		+ "(`zone`, `ward`, `cby`, `latitude`, `longitude`, "
	    		+ "	`address`, `place_name`,`tonage`, `remarks`, `file`,`status`) "
                + "VALUES "
                + "(?,?,?,?,?,?,?,?,?,?,?)";

	    String insertSql = insertSqltxt;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"wlid"});
	        int i = 1;
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, cby);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        
	        ps.setString(i++, address);
	        ps.setString(i++, place_name);
	        ps.setString(i++, tonage);
	       // ps.setString(i++, type);
	        ps.setString(i++, remarks);
	        
	        ps.setString(i++, file1);
	        ps.setString(i++, "Pending");
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("thooimai_mission", action + " insertId: " +lastInsertId);
	        response.put("status", "success");
	        response.put("message", "Data inserted successfully.");
	    } else {
	    	response.put("thooimai_mission", action + " error!");
	        response.put("status", "error");
	        response.put("message", "Data insert failed.");
	    }
	
	    result.add(response);
	    return result;
	}
	
	public List<Map<String, Object>> getComplaintList(String loginId) {
		
		String ward = getWardByLoginId(loginId,"");
		
        String sql = "SELECT "
        		+ "    wlm.wlid, "
        		+ "    wlm.zone AS comp_zone, "
        		+ "    wlm.ward AS comp_ward, "
        		+ "    eu.EXTRAFIELD2 AS comp_contact, "
        		+ "    eu.FIRST_NAME AS comp_name, "
        		+ "    DATE_FORMAT(wlm.cdate, '%d-%m-%Y %r') AS comp_date, "
        		+ "    wlm.latitude AS comp_latitude, "
        		+ "    wlm.longitude AS comp_longitude, "
        		+ "    wlm.address AS comp_address, "
        		+ "    wlm.place_name AS comp_place_name, "
        		+ "    wlm.tonage AS appx_tonage, "
        		+ "    wt.name AS comp_type, "
        		+ "    CONCAT('https://gccservices.in/gccofficialapp/files', wlm.file) AS comp_image "
        		+ "FROM waste_location_mapping wlm "
        		+ "JOIN erp_pgr.EG_USER eu "
        		+ "    ON wlm.cby = eu.id "
        		+ "LEFT JOIN waste_type wt "
        		+ "    ON wlm.typeid = wt.typeid "
        		+ "WHERE wlm.isactive = 1 "
        		+ "  AND wlm.status !='Close' "
        		+ "  AND wlm.ward = ? ";
        
        return jdbcTemplate.queryForList(sql, ward);
    }
	
	private void inactiveTheReply(String wlid) {
	    String sql = "UPDATE `waste_reply` SET `isactive`=0 WHERE `wlid`=?";
	    jdbcTemplate.update(sql,wlid);
	}
	
	private void updateStatus(String wlid, int reply_id, String status) {
	    String sql = "UPDATE `waste_location_mapping` SET `status`=?, reply_id=? WHERE `wlid`=?";
	    jdbcTemplate.update(sql, status, reply_id, wlid);
	}
	
	private boolean isAlreadyClosed(String wlid) {
	    String sql = "SELECT COUNT(*) FROM waste_location_mapping " +
	                 "WHERE wlid = ? AND status = 'Close'";
	    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, wlid);
	    return count != null && count > 0;
	}
	
	@Transactional
	public List<Map<String, Object>> saveClose(
			String wlid,
			String zone,
			String ward,
			String latitude,
			String longitude, 
			MultipartFile file, 
			String remarks,
			List<Map<String, String>> wastetype,
			String cby) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		// Check if already closed
		if (isAlreadyClosed(wlid)) {
		    response.put("status", "error");
		    response.put("message", "This complaint is already closed.");
		    result.add(response);
		    return result;
		}
		
		String filetxt = cby;
		
		// Call fileUpload to download and save the file locally
		// For Image
	    String filetype = "image";
	    String action = "identified_waste";
	    
	    // for Image 1
	    String file1 = fileUpload(action, filetxt, file, filetype);
	    
	    if ("error".equalsIgnoreCase(file1)) {
	        response.put("status", "error");
	        response.put("message", "Image-1 insert failed.");
	        result.add(response);
	        return result;
	    }
	    
		inactiveTheReply(wlid); // Set Old reply inactive 
		
		String sqlQuery = "INSERT INTO `waste_reply`(`wlid`, `zone`, `ward`, `latitude`, `longitude`, `file`, `remarks`,`cby`) "
				+ "VALUES (?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
			int affectedRows = jdbcTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "wlid" });
					int i = 1;
			        ps.setString(i++, wlid);
			        ps.setString(i++, zone);
			        ps.setString(i++, ward);
			        ps.setString(i++, latitude);
			        ps.setString(i++, longitude);
			        //ps.setString(i++, type);
					ps.setString(i++, file1);
					ps.setString(i++, remarks);
					//ps.setString(i++, tonage);
					ps.setString(i++, cby);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				
				// 2️⃣ Insert waste_info (MULTIPLE ROWS)
			    String insertWasteInfoSql =
			        "INSERT INTO waste_info (typeid, kg, wlid, wrid) " +
			        "VALUES (?,?,?,?)";

			    for (Map<String, String> wt : wastetype) {
			        jdbcTemplate.update(
			            insertWasteInfoSql,
			            wt.get("id"),
			            wt.get("kg"),
			            wlid,
			            lastInsertId
			        );
			    }
			    
				updateStatus(wlid, lastInsertId, "Close"); // Update the complaint status
				
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "A complaint reply added successfully!");
				//System.out.println("A new complaint reply added successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to add the complaint reply!.");
			}
		} catch (DataAccessException e) {
			System.out.println("Data Access Exception:");
			Throwable rootCause = e.getMostSpecificCause();
			if (rootCause instanceof SQLException) {
				SQLException sqlException = (SQLException) rootCause;
				/*
				System.out.println("SQL State: " + sqlException.getSQLState());
				System.out.println("Error Code: " + sqlException.getErrorCode());
				System.out.println("Message: " + sqlException.getMessage());
				*/
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
	public List<Map<String, Object>> getZoneSummary(String fromDate, String toDate) {
		
		fromDate = convertDateFormat(fromDate,0);
    	toDate = convertDateFormat(toDate,1);
    	
    	String sql = "SELECT "
    			+ "    wlm.zone, "
    			+ "    COUNT(wlm.wlid) AS identified, "
    			+ "    SUM(CASE WHEN wr.wlid IS NULL THEN 1 ELSE 0 END) AS pending, "
    			+ "    SUM(CASE WHEN wr.wlid IS NOT NULL THEN 1 ELSE 0 END) AS `close` "
    			+ "FROM waste_location_mapping wlm "
    			+ "LEFT JOIN waste_reply wr "
    			+ "    ON wlm.wlid = wr.wlid"
    			+ "    AND wr.isactive = 1 "
    			+ "WHERE wlm.isactive = 1 "
    			+ "  AND wlm.isdelete = 0 "
    			+ "  AND wlm.cdate >= ? "
    			+ "  AND wlm.cdate < ? "
    			+ "GROUP BY wlm.zone "
    			
    			+ " UNION ALL "
    			
    			+ "SELECT "
    			+ "    'TOTAL' AS zone, "
    			+ "    COUNT(wlm.wlid), "
    			+ "    COALESCE(SUM(CASE WHEN wr.wlid IS NULL THEN 1 ELSE 0 END), 0), " 
    	        + "    COALESCE(SUM(CASE WHEN wr.wlid IS NOT NULL THEN 1 ELSE 0 END), 0) " 
    			+ "FROM waste_location_mapping wlm "
    			+ "LEFT JOIN waste_reply wr "
    			+ "    ON wlm.wlid = wr.wlid "
    			+ "    AND wr.isactive = 1 "
    			+ "WHERE wlm.isactive = 1 "
    			+ "  AND wlm.isdelete = 0 "
    			+ "  AND wlm.cdate >= ? "
    			+ "  AND wlm.cdate < ? ";
        
        return jdbcTemplate.queryForList(sql, fromDate, toDate, fromDate,toDate);
	}
	
	public List<Map<String, Object>> getWardSummary(String zone, String fromDate, String toDate) {
		
		fromDate = convertDateFormat(fromDate,0);
    	toDate = convertDateFormat(toDate,1);
    	
    	String sql = "SELECT  "
    			+ "    wlm.ward, "
    			+ "    COUNT(wlm.wlid) AS identified, "
    			+ "    SUM(CASE WHEN wr.wlid IS NULL THEN 1 ELSE 0 END) AS pending, "
    			+ "    SUM(CASE WHEN wr.wlid IS NOT NULL THEN 1 ELSE 0 END) AS `close` "
    			+ "FROM waste_location_mapping wlm "
    			+ "LEFT JOIN waste_reply wr  "
    			+ "    ON wlm.wlid = wr.wlid "
    			+ "    AND wr.isactive = 1 "
    			+ "WHERE wlm.isactive = 1 "
    			+ "  AND wlm.isdelete = 0 "
    			+ "  AND wlm.zone = ? "
    			+ "  AND wlm.cdate >= ? "
    			+ "  AND wlm.cdate < ? "
    			+ "GROUP BY wlm.ward "
    			+ " "
    			+ "UNION ALL "
    			+ " "
    			+ "SELECT  "
    			+ "    'TOTAL' AS ward, "
    			+ "    COUNT(wlm.wlid), "
    	        + "    COALESCE(SUM(CASE WHEN wr.wlid IS NULL THEN 1 ELSE 0 END), 0), " 
    	        + "    COALESCE(SUM(CASE WHEN wr.wlid IS NOT NULL THEN 1 ELSE 0 END), 0) " 
    			+ "FROM waste_location_mapping wlm "
    			+ "LEFT JOIN waste_reply wr  "
    			+ "    ON wlm.wlid = wr.wlid "
    			+ "    AND wr.isactive = 1 "
    			+ "WHERE wlm.isactive = 1 "
    			+ "  AND wlm.isdelete = 0 "
    			+ "  AND wlm.zone = ? "
    			+ "  AND wlm.cdate >= ? "
    			+ "  AND wlm.cdate < ? ";
        
        return jdbcTemplate.queryForList(sql, zone, fromDate, toDate, zone, fromDate, toDate);
	}
	
	public List<Map<String, Object>> getPendingSummary(String ward, String fromDate, String toDate) {
		
		fromDate = convertDateFormat(fromDate,0);
    	toDate = convertDateFormat(toDate,1);
    	
    	String sql = "SELECT "
    			+ "    wlm.wlid, "
    			+ "    wlm.zone, "
    			+ "    wlm.ward, "
    			+ "    DATE_FORMAT(wlm.cdate, '%d-%m-%Y %r') AS identified_date, "
    			+ "    wlm.latitude, "
    			+ "    wlm.longitude, "
    			+ "    wlm.address, "
    			+ "    wlm.place_name, "
    			+ "    wlm.tonage, "
    			+ "    wt.name AS waste_type, "
    			+ "    wlm.remarks, "
    			+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', wlm.file) AS image "
    			+ "FROM waste_location_mapping wlm "
    			+ "LEFT JOIN waste_reply wr "
    			+ "    ON wlm.wlid = wr.wlid "
    			+ "    AND wr.isactive = 1 "
    			+ "LEFT JOIN waste_type wt "
    			+ "    ON wlm.typeid = wt.typeid "
    			+ "WHERE wlm.isactive = 1 "
    			+ "  AND wlm.isdelete = 0 "
    			+ "  AND wlm.ward = ? "
    			+ "  AND wr.wlid IS NULL "
    			+ "  AND wlm.cdate >= ? "
    			+ "  AND wlm.cdate < ? "
    			+ "ORDER BY wlm.cdate DESC";
        
        return jdbcTemplate.queryForList(sql, ward, fromDate, toDate);
	}
	
public List<Map<String, Object>> getCloseSummary(String ward, String fromDate, String toDate) {
		
		fromDate = convertDateFormat(fromDate,0);
    	toDate = convertDateFormat(toDate,1);
    	
    	String sql =
    	        "SELECT "
    	      + "    wlm.wlid, "
    	      + "    wlm.zone, "
    	      + "    wlm.ward, "
    	      + "    DATE_FORMAT(wlm.cdate, '%d-%m-%Y %r') AS identified_date, "
    	      + "    DATE_FORMAT(wr.cdate, '%d-%m-%Y %r') AS closed_date, "
    	      + "    wlm.latitude, "
    	      + "    wlm.longitude, "
    	      + "    wlm.address, "
    	      + "    wlm.place_name, "
    	      + "    wlm.tonage, "
    	      + "    wr.remarks AS closing_remarks, "
    	      + "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', wlm.file) AS identified_image, "
    	      + "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', wr.file) AS close_image, "
    	      + "    COALESCE( "
    	      + "        JSON_ARRAYAGG( "
    	      + "            JSON_OBJECT( "
    	      + "                'typeId', wi.typeid, "
    	      + "                'typeName', wt.name, "
    	      + "                'kg', wi.kg "
    	      + "            ) "
    	      + "        ), "
    	      + "        JSON_ARRAY() "
    	      + "    ) AS waste_types "
    	      + "FROM waste_location_mapping wlm "
    	      + "INNER JOIN waste_reply wr "
    	      + "    ON wlm.wlid = wr.wlid "
    	      + "    AND wr.isactive = 1 "
    	      + "LEFT JOIN waste_info wi "
    	      + "    ON wi.wlid = wlm.wlid "
    	      + "    AND wi.isactive = 1 "
    	      + "    AND wi.isdelete = 0 "
    	      + "LEFT JOIN waste_type wt "
    	      + "    ON wi.typeid = wt.typeid "
    	      + "WHERE wlm.isactive = 1 "
    	      + "  AND wlm.isdelete = 0 "
    	      + "  AND wlm.ward = ? "
    	      + "  AND wlm.cdate >= ? "
    	      + "  AND wlm.cdate < ? "
    	      + "GROUP BY wlm.wlid "
    	      + "ORDER BY wr.cdate DESC";
        
    	ObjectMapper mapper = new ObjectMapper();

    	List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, ward, fromDate, toDate);

    	for (Map<String, Object> row : list) {
    	    Object wt = row.get("waste_types");

    	    if (wt != null && wt instanceof String) {
    	        try {
    	            List<Map<String, Object>> wasteTypes =
    	                    mapper.readValue(wt.toString(), new TypeReference<List<Map<String, Object>>>() {});
    	            row.put("waste_types", wasteTypes);
    	        } catch (JsonProcessingException e) {
    	            row.put("waste_types", Collections.emptyList()); // fallback
    	        }
    	    }
    	}

    	return list;
	}

	
}
