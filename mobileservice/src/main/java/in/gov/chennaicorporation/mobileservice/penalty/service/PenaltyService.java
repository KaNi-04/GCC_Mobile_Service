package in.gov.chennaicorporation.mobileservice.penalty.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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

@Service
public class PenaltyService {
	private JdbcTemplate jdbcPenaltyTemplate;
	private final Environment environment;
	private String fileBaseUrl;
	
	@Autowired
	public void setDataSource(@Qualifier("mysqlActivityDataSource") DataSource penaltyDataSource) {
		this.jdbcPenaltyTemplate = new JdbcTemplate(penaltyDataSource);
	}
	
	@Autowired
	public PenaltyService(Environment environment) {
		this.environment = environment;
		this.fileBaseUrl=environment.getProperty("fileBaseUrl");
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
        String serviceFolderName = environment.getProperty("penalty_foldername");
        var year =DateTimeUtil.getCurrentYear();
        var month =DateTimeUtil.getCurrentMonth();
        
        uploadDirectory = uploadDirectory + serviceFolderName + year +"/"+month+"/";
        
        try {
	        // Create directory if it doesn't exist
	        Path directoryPath = Paths.get(uploadDirectory);
	        if (!Files.exists(directoryPath)) {
	            Files.createDirectories(directoryPath);
	        }
	        
	        // Datetime string
	        String datetimetxt = DateTimeUtil.getCurrentDateTime();
	        // File name 
            String fileName = name+ "_" +id + "_" + datetimetxt + "_" + file.getOriginalFilename();
            fileName = fileName.replaceAll("\\s+", ""); // Remove space on filename
            
	        String filePath = uploadDirectory + "/" + fileName;
	        
	        String filepath_txt = "/"+serviceFolderName + year +"/"+month+"/"+fileName;
	        
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
	public List<Map<String, Object>> getPenaltyTypeList(String departmentId) {
		//String sqlQuery = "SELECT `id`, `name`,`iconurl` FROM `penalty_category` WHERE `isdelete`=0 AND `isactive`=1 ORDER BY `name`"; 
		//List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
		String sqlQuery = "SELECT * FROM `penalty_category` WHERE (`isactive`=1 AND `isdelete`=0) ";
		
		if(departmentId!=null && !departmentId.isEmpty() && !departmentId.isBlank()) {
			sqlQuery= sqlQuery +" AND `mappingid`='"+departmentId+"'";
		}
		sqlQuery = sqlQuery + " ORDER BY `display_order`, `name` ASC";
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
		
		return result;
	}
		
	@Transactional
	public List<Map<String, Object>> generateChallan(
			MultiValueMap<String, String> formData,
			String category_id,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String streeId,
			String streetName,
			String loginId,
			String remarks,
			String isviolatorAvailable,
			String violatorName,
			String violatorPhone,
			String ton,
			MultipartFile file) {
		
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		String image = fileUpload("challan",category_id,file);
		
		String sqlQuery = "INSERT INTO `penalty_challan`(`category_id`, `image`, `latitude`, `longitude`, "
				+ "`zone`, `ward`, `streetid`, `streetname`, `challan_by`,`remarks`,"
				+ "`violator_info`, `violator_name`, `violator_phone`, `ton`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
            int affectedRows = jdbcPenaltyTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, category_id);
                    ps.setString(2, image);
                    ps.setString(3, latitude);
                    ps.setString(4, longitude);
                    ps.setString(5, zone);
                    ps.setString(6, ward);
                    ps.setString(7, streeId);
                    ps.setString(8, streetName);
                    ps.setString(9, loginId);
                    ps.setString(10, remarks);
                    ps.setString(11, isviolatorAvailable);
                    ps.setString(12, violatorName);
                    ps.setString(13, violatorPhone);
                    ps.setString(14, ton);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "A new Penalty (Chellan) was generated successfully!");
                System.out.println("A new Penalty (Chellan) was generated successfully! Insert ID: " + generatedId);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to insert a new Penalty (Chellan).");
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
	public List<Map<String, Object>> updateViolateInfo(
			MultiValueMap<String, String> formData,
			String challanId,
			String category_id,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String streeId,
			String streetName,
			String loginId,
			String remarks,
			String isviolatorAvailable,
			String violatorName,
			String violatorPhone,
			String ton,
			MultipartFile file) {
		
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		String image = fileUpload("challan",category_id,file);
		
		String sqlQuery = "UPDATE `penalty_challan` SET `category_id`=? , `image`=?, `latitude`=?, `longitude`=?, "
				+ "`zone`=?, `ward`=?, `streetid`=?, `streetname`=?, `challan_by`=?,`remarks`=?,"
				+ "`violator_info`=?, `violator_name`=?, `violator_phone`=?, `ton`=? "
				+ " WHERE `id`=? LIMIT 1";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
            int affectedRows = jdbcPenaltyTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, category_id);
                    ps.setString(2, image);
                    ps.setString(3, latitude);
                    ps.setString(4, longitude);
                    ps.setString(5, zone);
                    ps.setString(6, ward);
                    ps.setString(7, streeId);
                    ps.setString(8, streetName);
                    ps.setString(9, loginId);
                    ps.setString(10, remarks);
                    ps.setString(11, isviolatorAvailable);
                    ps.setString(12, violatorName);
                    ps.setString(13, violatorPhone);
                    ps.setString(14, ton);
                    ps.setString(15, challanId);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "A new Penalty (Chellan) was generated successfully!");
                System.out.println("A new Penalty (Chellan) was generated successfully! Insert ID: " + generatedId);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to insert a new Penalty (Chellan).");
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
	public List<Map<String, Object>> getChallanList() {
	    String sqlQuery = "SELECT *, "
	    		+ "DATE_FORMAT(challan_date, '%d-%m-%Y %r') AS formatted_challan_datetime, "
				+ "DATE_FORMAT(challan_date, '%d-%m-%Y') AS formatted_challan_date, "
				+ "DATE_FORMAT(challan_date, '%r') AS formatted_challan_time, "
				+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', image) AS imageUrl, "
	    		+ "CASE "
	    		+ "        WHEN status = 1 THEN 'Generated'"
				+ "        WHEN status = 2 THEN 'User Action Takened'"
				+ "        WHEN status = 3 THEN 'Fine Generated(Unpaid)'" 
				+ "        WHEN status = 4 THEN 'Fine Generated(Paid)'"
				+ "        WHEN status = 5 THEN 'Action Taken By GCC'"
				+ "        WHEN status = 6 THEN 'Completed'"
				+ "        ELSE 'unknown'"
				+ "    END AS status_name, "
	            + "DATE_FORMAT(challan_date, '%d-%m-%Y %H:%i:%s') AS challan_date "
	            + "FROM penalty_category "
	            + "WHERE isdelete=0 AND isactive=1 "
	            + "ORDER BY name"; 
	    System.out.println("SQL : " + sqlQuery);
	    List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
	    return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getOfficerChallanList(
	        String category_id,
	        String fromDate,
	        String toDate,
	        String zone,
	        String ward,
	        String streetid,
	        String status,
	        String loginId,
	        String isviolatorAvailable
	) {
		System.out.println(fileBaseUrl);
		
	    StringBuilder sqlQuery = new StringBuilder("SELECT pc.*, "
	            + "DATE_FORMAT(pc.challan_date, '%d-%m-%Y %r') AS formatted_challan_datetime, "
	            + "DATE_FORMAT(pc.challan_date, '%d-%m-%Y') AS formatted_challan_date, "
	            + "DATE_FORMAT(pc.challan_date, '%r') AS formatted_challan_time, "
	            + "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', pc.image) AS imageUrl, "
	            // Conditional formatting for fineissued_date fields
	            + "CASE WHEN pc.fineissued = 1 THEN DATE_FORMAT(pc.fineissued_date, '%d-%m-%Y %r') ELSE '-' END AS formatted_fineissue_datetime, "
	            + "CASE WHEN pc.fineissued = 1 THEN DATE_FORMAT(pc.fineissued_date, '%d-%m-%Y') ELSE '-' END AS formatted_fineissue_date, "
	            + "CASE WHEN pc.fineissued = 1 THEN DATE_FORMAT(pc.fineissued_date, '%r') ELSE '-' END AS formatted_fineissue_time, "
	            // Status name mapping
	            + "CASE "
	            + "    WHEN pc.status = 1 THEN 'Generated' "
	            + "    WHEN pc.status = 2 THEN 'User Action Taken' "
	            + "    WHEN pc.status = 3 THEN 'Fine Generated (Unpaid)' "
	            + "    WHEN pc.status = 4 THEN 'Fine Generated (Paid)' "
	            + "    WHEN pc.status = 5 THEN 'Action Taken By GCC' "
	            + "    WHEN pc.status = 6 THEN 'Completed' "
	            + "    ELSE 'Unknown' "
	            + "END AS status_name, "
	            + "DATE_FORMAT(pc.challan_date, '%d-%m-%Y %H:%i:%s') AS challan_date, "
	            + "penalty_category.name AS penalty_category_name, "
	            + "p.amount AS payment_amount, "
	            + "p.paymentStatus AS payment_status, "
	            + "DATE_FORMAT(p.paymentdate, '%d-%m-%Y %r') AS formatted_payment_datetime "
	            + "FROM penalty_challan pc "
	            + "JOIN penalty_category ON pc.category_id = penalty_category.id "
	            + "LEFT JOIN `Payment` p ON p.uniqueid = pc.id "
	            + "WHERE (pc.isdelete = 0 AND pc.isactive = 1) AND pc.challan_by = '" + loginId + "' ");

	    // Add additional filters if provided
	    if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
	        sqlQuery.append(" AND pc.zone = '" + zone + "'");
	    }
	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
	        sqlQuery.append(" AND pc.ward = '" + ward + "'");
	    }
	    if (streetid != null && !streetid.isEmpty() && !streetid.isBlank()) {
	        sqlQuery.append(" AND pc.streetid = '" + streetid + "'");
	    }
	    if (category_id != null && !category_id.isEmpty() && !category_id.isBlank()) {
	        sqlQuery.append(" AND pc.category_id = '" + category_id + "'");
	    }
	    if (isviolatorAvailable != null && !isviolatorAvailable.isEmpty() && !isviolatorAvailable.isBlank()) {
	        sqlQuery.append(" AND pc.violator_info = '" + isviolatorAvailable + "'");
	    }
	    if (status != null && !status.isEmpty() && !status.isBlank()) {
	        sqlQuery.append(" AND pc.status = '" + status + "'");
	    }

	    // Date filters
	    if (fromDate != null && toDate != null) {
	        fromDate = convertDateFormat(fromDate, 0);
	        toDate = convertDateFormat(toDate, 1);
	        sqlQuery.append(" AND pc.challan_date BETWEEN '" + fromDate + "' AND '" + toDate + "'");
	    }

	    sqlQuery.append(" ORDER BY pc.challan_date DESC");

	    System.out.println("SQL OF getOfficerChallanList");
	    System.out.println("SQL : " + sqlQuery);

	    List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery.toString());

	    return result;
	}
	
	@Transactional
	public List<Map<String, Object>> issueFine(
			MultiValueMap<String, String> formData,
			String challanId,
			String tonOnFine,
			String fineAmount,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String streeId,
			String streetName,
			String loginId,
			String remarks,
			MultipartFile file) {
		
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		String image = fileUpload("fine",challanId,file);
		
		String sqlQuery = "INSERT INTO `penalty_fine_info`(`challan_id`, `image`, `latitude`, `longitude`, `zone`, `ward`, `streetid`, `streetname`, `fineissued_by`,`remarks`,`ton_on_fine`,`fine_amount`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
            int affectedRows = jdbcPenaltyTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, challanId);
                    ps.setString(2, image);
                    ps.setString(3, latitude);
                    ps.setString(4, longitude);
                    ps.setString(5, zone);
                    ps.setString(6, ward);
                    ps.setString(7, streeId);
                    ps.setString(8, streetName);
                    ps.setString(9, loginId);
                    ps.setString(10, remarks);
                    ps.setString(11, tonOnFine);
                    ps.setString(12, fineAmount);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
            	
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "A new fine was generated successfully!");
                System.out.println("A new fine was generated successfully! Insert ID: " + generatedId);
                //Fine Generated(Unpaid) id 3
            	String sqlQuery_u = "UPDATE `penalty_challan` SET `status`=3, fineissued=1 WHERE `id`='"+challanId+"'";
            	jdbcPenaltyTemplate.update(sqlQuery_u);
            	System.out.println(sqlQuery_u);
            	System.out.println("Challan Updated as fine successfully! Challan ID: " + challanId);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to insert a new fine.");
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
	public List<Map<String, Object>> closeChallan(
			MultiValueMap<String, String> formData,
			String challanId,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String streeId,
			String streetName,
			String loginId,
			String remarks,
			MultipartFile file) {
		
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		String image = fileUpload("close",challanId,file);
		
		String sqlQuery = "INSERT INTO `penalty_close_info`(`challan_id`, `image`, `latitude`, `longitude`, `zone`, `ward`, `streetid`, `streetname`, `fineclose_by`,`remarks`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
            int affectedRows = jdbcPenaltyTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, challanId);
                    ps.setString(2, image);
                    ps.setString(3, latitude);
                    ps.setString(4, longitude);
                    ps.setString(5, zone);
                    ps.setString(6, ward);
                    ps.setString(7, streeId);
                    ps.setString(8, streetName);
                    ps.setString(9, loginId);
                    ps.setString(10, remarks);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "A Challan Close log was generated successfully!");
                System.out.println("A Challan Close log was generated successfully! Insert ID: " + generatedId);
                //Fine Generated(Unpaid) id 3
            	String sqlQuery_u = "UPDATE `penalty_challan` SET `status`=6 WHERE `id`='"+challanId+"'";
            	jdbcPenaltyTemplate.update(sqlQuery_u);
            	System.out.println(sqlQuery_u);
            	System.out.println("Challan Updated as closed successfully! Challan ID: " + challanId);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to close the challan.");
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
	public List<Map<String, Object>> getTaskListByZone(
			String zone,
			String loginId){
		String sqlQuery = "SELECT *,CASE "
						+ "        WHEN status = 1 THEN 'Open'"
						+ "        WHEN status = 2 THEN 'In-Progress'"
						+ "        WHEN status = 3 THEN 'Pending'"
						+ "        WHEN status = 4 THEN 'Completed'"
						+ "        ELSE 'unknown'"
						+ "    END AS status_name FROM "
						+ "`brr_task` WHERE `zone`=? AND (`isactive`=1 AND `isdelete`=0) ORDER BY `id` DESC";
		List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery, zone);
		
		return result;
	}
	
	
	//// POS ////
	
	@Transactional
	public List<Map<String, Object>> generateChallanPOS(
			MultiValueMap<String, String> formData,
			String category_id,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String loginId,
			String violatorName,
			String violatorPhone,
			String ton,
			String amount,
			MultipartFile file) {
		
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		String image = fileUpload("challan",category_id,file);
		
		String sqlQuery = "INSERT INTO `penalty_challan_pos`(`category_id`, `image`, `latitude`, `longitude`, "
				+ "`zone`, `ward`, `challan_by`"
				+ " `violator_name`, `violator_phone`, `ton`,`amount`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
            int affectedRows = jdbcPenaltyTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, category_id);
                    ps.setString(2, image);
                    ps.setString(3, latitude);
                    ps.setString(4, longitude);
                    ps.setString(5, zone);
                    ps.setString(6, ward);
                    ps.setString(7, loginId);
                    ps.setString(8, violatorName);
                    ps.setString(9, violatorPhone);
                    ps.setString(10, ton);
                    ps.setString(11, amount);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "A new Penalty (Chellan) was generated successfully!");
                System.out.println("A new Penalty (Chellan) was generated successfully! Insert ID: " + generatedId);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to insert a new Penalty (Chellan).");
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
