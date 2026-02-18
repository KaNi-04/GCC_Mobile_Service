package in.gov.chennaicorporation.mobileservice.c_d_waste.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;

@Service
public class c_d_WasteService {
	private JdbcTemplate jdbcWasteTemplate;
	private JdbcTemplate jdbcPGRTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
	@Autowired
	public void setDataSource(
			@Qualifier("mysqlC_D_WasteDataSource") DataSource wasteDataSource,
			@Qualifier("mysqlPGRMasterDataSource") DataSource pgrDataSource
			) {
		this.jdbcWasteTemplate = new JdbcTemplate(wasteDataSource);
		this.jdbcPGRTemplate = new JdbcTemplate(pgrDataSource);
	}
	
	@Autowired
	public c_d_WasteService(Environment environment) {
		this.environment = environment;
		this.fileBaseUrl=environment.getProperty("fileBaseUrl");
	}
	
	//CONCAT('"+fileBaseUrl+"/gccofficialapp/files/', al.image) AS imageUrl, 
	
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
        String serviceFolderName = environment.getProperty("c_d_waste_foldername");
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
            Files.write(path, bytes);
            
            System.out.println(filePath);
            return filepath_txt;
            
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to save file " + file.getOriginalFilename();
        } 
	}
	
	public String fileUrlUpload(String name, String id, String fileUrl) {

	    // Set the directory for saving the file
	    String uploadDirectory = environment.getProperty("file.upload.directory");
	    String serviceFolderName = environment.getProperty("c_d_waste_foldername");
	    var year = DateTimeUtil.getCurrentYear();
	    var month = DateTimeUtil.getCurrentMonth();

	    uploadDirectory = uploadDirectory + serviceFolderName + year + "/" + month +"/reply";

	    try {
	        // Bypass SSL certificate validation (this will ignore SSL handshake errors)
	        TrustManager[] trustAllCertificates = new TrustManager[]{
	            new X509TrustManager() {
	                public X509Certificate[] getAcceptedIssuers() {
	                    return null;
	                }
	                public void checkClientTrusted(X509Certificate[] certs, String authType) {
	                }
	                public void checkServerTrusted(X509Certificate[] certs, String authType) {
	                }
	            }
	        };

	        SSLContext sc = SSLContext.getInstance("TLS");
	        sc.init(null, trustAllCertificates, new java.security.SecureRandom());
	        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

	        // Create directory if it doesn't exist
	        Path directoryPath = Paths.get(uploadDirectory);
	        if (!Files.exists(directoryPath)) {
	            Files.createDirectories(directoryPath);
	        }

	        // Generate datetime string for the file name
	        String datetimetxt = DateTimeUtil.getCurrentDateTime();

	        // Generate a random string for the file name
	        String fileName = name + "_" + id + "_" + datetimetxt + "_" + generateRandomStringForFile(10) + ".jpg"; // Assuming it's a .jpg file
	        fileName = fileName.replaceAll("\\s+", ""); // Remove spaces in filename

	        String filePath = uploadDirectory + "/" + fileName; // Full local file path
	        String relativeFilePath = "/" + serviceFolderName + year + "/" + month + "/reply/" + fileName; // Relative path to save in DB

	        // Create a URL object from the file URL
	        URL url = new URL(fileUrl);

	        // Open a connection to the URL and download the file
	        try (InputStream inputStream = url.openStream()) {
	            // Write the input stream to the local file
	            Files.copy(inputStream, Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
	            System.out.println("File downloaded and saved at: " + filePath);
	        }

	        // Return the relative path to be stored in the database
	        return relativeFilePath;

	    } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
	        e.printStackTrace();
	        System.out.println("Failed to download and save file from URL " + fileUrl);
	        return "error";
	    }
	}
	
	public List<Map<String, Object>> getWasteTypeList() {
        String sql = "SELECT * FROM `waste_type` WHERE isactive=1 ORDER BY `name`";
        return jdbcWasteTemplate.queryForList(sql);
    }
	
	public List<Map<String, Object>> checkComplaintExits(String latitude, String longitude) {
    	
		String sql = "SELECT "
	            + "    wlm.`wlid` AS comp_id, "
	            + "    wlm.`zone` AS comp_zone, "
	            + "    wlm.`ward` AS comp_ward, "
	            + "    eg_user.`EXTRAFIELD2` AS comp_contact, "
	            + "    eg_user.`FIRST_NAME` AS comp_name, "
	            + "    DATE_FORMAT(wlm.`cdate`, '%d-%m-%Y %r') AS comp_date, "
	            + "    wlm.`latitude` AS comp_latitude, "
	            + "    wlm.`longitude` AS comp_longitude, "
	            + "    wlm.`address` AS comp_area, "
	            + "    wlm.`tonage` AS appx_tonage, "
	            + "    wt.`name` AS comp_type, "
	            + "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', wlm.`file`) AS image, "
	            + "    wlm.`status` AS comp_status, "
	            + "		`street_type` AS comp_street_type, "
        		+ "		`kpi` AS comp_kpi "
	            + "FROM "
	            + "    `waste_location_mapping` wlm "
	            + "JOIN "
	            + "    `erp_pgr`.`EG_USER` eg_user "
	            + "    ON wlm.`cby` = eg_user.`id` "
	            + "LEFT JOIN "
	            + "    `waste_type` wt "
	            + "    ON wlm.`type` = wt.`typeid` "
	            + "WHERE "
	            + "    wlm.`isactive` = 12 "
	            + "    AND ((6371008.8 * acos(ROUND(cos(radians(?)) * cos(radians(wlm.latitude)) * cos(radians(wlm.longitude) - radians(?)) + sin(radians(?)) * sin(radians(wlm.latitude)), 9))) < 10) "
	            + "    AND wlm.`status` = 'open' "
	            + "ORDER BY wlm.`cdate` DESC";
        		
		// we set wlm.`isactive` = 12 /***************** Important *********************/
		
        // Use latitude, longitude, and schoolType as positional parameters
        return jdbcWasteTemplate.queryForList(sql, new Object[]{latitude, longitude, latitude});
    }

	public List<Map<String, Object>> getComplaintList(String fromDATE, String toDATE) {
        String sql = "SELECT "
        		+ "    `wlid` AS comp_id,  "
        		+ "    `zone` AS comp_zone,  "
        		+ "    `ward` AS comp_ward,  "
        		+ "    `EG_USER`.`EXTRAFIELD2` AS comp_contact,  "
        		+ "    `EG_USER`.`FIRST_NAME` AS comp_name,"
        		+ "		DATE_FORMAT(`cdate`, '%d-%m-%Y %r') AS comp_date, "
        		+ "    `latitude` AS comp_latitude,  "
        		+ "    `longitude` AS comp_longitude,  "
        		+ "    `address` AS comp_area,  "
        		+ "    `tonage` AS appx_tonage,  "
        		+ "    wt.`name` AS comp_type,  "
        		+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `file`) AS image, "
        		+ "    `ref_id` AS ref_id, "
        		+ "    CASE WHEN `final_vendor_approval` = 'yes' THEN 'approved' "
        		+ "	   	WHEN `final_vendor_approval` = 'no' THEN 'rejected' "
        		+ "    	ELSE 'pending'"
        		+ "		END AS approval_status, "
        		+ "		DATE_FORMAT(`approved_date`, '%d-%m-%Y %r') AS comp_approved_date, "
        		+ "		`street_type` AS comp_street_type, "
        		+ "		`kpi` AS comp_kpi "
        		+ "FROM  "
        		+ "    `waste_location_mapping` "
        		+ "JOIN  "
        		+ "    `erp_pgr`.`EG_USER`  "
        		+ "    ON `waste_location_mapping`.cby = `EG_USER`.id "
	    		+ "LEFT JOIN "
	            + "    `waste_type` wt " 
	            + "    ON waste_location_mapping.type = wt.typeid "
        		+ "WHERE  "
        		+ "    `waste_location_mapping`.isactive = 1 "
        		//+ "    AND `waste_location_mapping`.isactive = 1 "
        		+ "    AND DATE(`waste_location_mapping`.approved_date) BETWEEN ? AND ?";
        
        return jdbcWasteTemplate.queryForList(sql, fromDATE, toDATE);
    }
	
	private boolean checkisCouncillor(String cby) {
		String sql = "SELECT `id`,`EXTRAFIELD1` FROM `erp_pgr`.`EG_USER` WHERE `id`=? AND `EXTRAFIELD1`='councillor'";
		System.out.println("SELECT `id`,`EXTRAFIELD1` FROM `erp_pgr`.`EG_USER` WHERE `id`="+cby+" AND `EXTRAFIELD1`='councillor'");
		List<Map<String, Object>> taskresult = jdbcPGRTemplate.queryForList(sql, cby);
		return !taskresult.isEmpty();
	}
	
	private String checkIsOtherOfficer(String cby) {
	    String sql = "SELECT `id`, `EXTRAFIELD1` FROM `erp_pgr`.`EG_USER` WHERE `id` = ? AND `EXTRAFIELD1` = 'otherOfficer'";
	    
	    // Debugging Log (Safe Version)
	    System.out.println("Executing SQL Query: " + sql + " with parameter: " + cby);
	    
	    List<Map<String, Object>> taskresult = jdbcPGRTemplate.queryForList(sql, cby);

	    // Check if results exist before accessing
	    if (!taskresult.isEmpty()) {
	        return (String) taskresult.get(0).get("EXTRAFIELD1"); // Get EXTRAFIELD1 safely
	    } else {
	        return "no"; 
	    }
	}
	
	@Transactional
	public List<Map<String, Object>> saveWaste(
			String cby,
			String latitude,
			String longitude, 
			String zone, 
			String ward,
			String address,
			String tonage,
			//String remarks,
			MultipartFile file,
			String type,
			String StreetType) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;

		//KPI Check
		String KPI = "2";
		
		if("BRR".equalsIgnoreCase(StreetType)) {
			KPI = "3";
		}
		
		String kpi_final = KPI;
				
		String image = "";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			var year = DateTimeUtil.getCurrentYear();
			var month = DateTimeUtil.getCurrentMonth();
			image = fileUpload("Created", cby, file);
		}
		
		String storeIMG = image;
		
		String sqlQuery = "INSERT INTO `waste_location_mapping`"
				+ "(`zone`, `ward`, `cby`, `latitude`, `longitude`, `address`, `tonage`, `file`, "
				+ "`status`,`type`,`request_by_type`, `approved`, `approvedby`,`final_vendor_approval`,`approved_date`,`street_type`,`kpi`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		String request_by_type = "Officer";
		String approved = "yes";
		String approvedby = cby;
		String final_vendor_approval = "yes";
		Boolean councillor = checkisCouncillor(cby);
		String OtherOfficer = checkIsOtherOfficer(cby);
		System.out.println("councillor :"+councillor);
		
		if(councillor) {
			request_by_type = "councillor";
			approved = "pending";
			approvedby ="0";
			final_vendor_approval = "pending";
		}
		
		System.out.println("IS otherOfficer :"+OtherOfficer);
		
		if(OtherOfficer.contains("otherOfficer")){
			request_by_type = "IE";
			approved = "yes"; //"pending";
			approvedby = cby; //"0";
			final_vendor_approval = "yes"; //"pending";
		}
		
		final String request_by_type_final = request_by_type;
		final String approved_final = approved;
		final String approvedby_final = approvedby;
		final String final_vendor_approval_final = final_vendor_approval;
		
		try {
			int affectedRows = jdbcWasteTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "wlid" });
					ps.setString(1, zone);
					ps.setString(2, ward);
					ps.setString(3, cby);
					ps.setString(4, latitude);
					ps.setString(5, longitude);
					ps.setString(6, address);
					ps.setString(7, tonage);
					ps.setString(8, storeIMG);
					ps.setString(9, "open");
					ps.setString(10, type);
					ps.setString(11, request_by_type_final);
					ps.setString(12, approved_final);
					ps.setString(13, approvedby_final);
					ps.setString(14, final_vendor_approval_final);
					ps.setString(15, formattedDate);
					ps.setString(16, StreetType);
					ps.setString(17, kpi_final);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "A new C&D Waste was inserted successfully!");
				System.out.println("A new C&D Waste was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new C&D Waste.");
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
	public List<Map<String, Object>> saveVendorWaste(
			String latitude,
			String longitude, 
			String zone, 
			String ward,
			String address,
			int tonage,
			MultipartFile file,
			String type,
			String ref_id,
			String StreetType) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		//KPI Check
		String KPI = "1";
		
		if("BRR".equalsIgnoreCase(StreetType)) {
			KPI = "3";
		}
		
		String kpi_final = KPI;
				
		String image = "";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			var year = DateTimeUtil.getCurrentYear();
			var month = DateTimeUtil.getCurrentMonth();
			image = fileUpload("Created", "0", file);
		}
		
		String storeIMG = image;
		
		String sqlQuery = "INSERT INTO `waste_location_mapping`"
				+ "(`zone`, `ward`, `cby`, `latitude`, `longitude`, `address`, `tonage`, `file`, `status`,`type`,`request_by_type`,`ref_id`,`street_type`,`kpi`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
			String tonagetxt = String.valueOf(tonage); 
			
			int affectedRows = jdbcWasteTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "wlid" });
					ps.setString(1, zone);
					ps.setString(2, ward);
					ps.setString(3, "13451");
					ps.setString(4, latitude);
					ps.setString(5, longitude);
					ps.setString(6, address);
					ps.setString(7, tonagetxt);
					ps.setString(8, storeIMG);
					ps.setString(9, "open");
					ps.setString(10, type);
					ps.setString(11, "Vendor");
					ps.setString(12, ref_id);
					ps.setString(13, StreetType);
					ps.setString(14, kpi_final);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				response.put("insertId", lastInsertId);
				response.put("ref_id", ref_id);
				response.put("status", "success");
				response.put("message", "A new C&D Waste was inserted successfully!");
				System.out.println("A new C&D Waste was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new C&D Waste.");
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
	
	private void inactiveTheReply(String wlid) {
	    String sql = "UPDATE `waste_reply` SET `isactive`=0 WHERE `wlid`=?";
	    jdbcWasteTemplate.update(sql,wlid);
	}
	
	private void updateStatus(String wlid, int reply_id, String status) {
	    String sql = "UPDATE `waste_location_mapping` SET `status`=?, reply_id=? WHERE `wlid`=?";
	    jdbcWasteTemplate.update(sql, status, reply_id, wlid);
	}
	
	@Transactional
	public List<Map<String, Object>> saveReply(
			String wlid,
			String latitude,
			String longitude, 
			String file, 
			String remarks,
			String datetxt,
			String tonage) {
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
		
		// Call fileUpload to download and save the file locally
	    String filePath = fileUrlUpload("Reply", wlid, file);  // Save file locally and get its path
	    
	    if(filePath.equals("error")) {
	    	filePath = file;
	    }
	    else {
	    	filePath = "https://gccservices.in/gccofficialapp/files"+filePath;
	    }
	    String fileURL = filePath;
	    
		inactiveTheReply(wlid); // Set Old reply inactive 
		
		String sqlQuery = "INSERT INTO `waste_reply`(`wlid`, `latitude`, `longitude`, `file`, `remarks`,`completed_date`,`tonage`) "
				+ "VALUES (?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
			int affectedRows = jdbcWasteTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "wlid" });
					ps.setString(1, wlid);
					ps.setString(2, latitude);
					ps.setString(3, longitude);
					ps.setString(4, fileURL);
					ps.setString(5, remarks);
					ps.setString(6, datetxt);
					ps.setString(7, tonage);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				
				updateStatus(wlid, lastInsertId, "close"); // Update the complaint status
				
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "A complaint reply added successfully!");
				System.out.println("A new complaint reply added successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to add the complaint reply!.");
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
	
//	public List<Map<String, Object>> getMyticketllist(String loginId, String fromDATE, String toDATE) {
//		
//		// Define the input and output date formats
//        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
//        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//        // Parse the input dates to LocalDate
//        LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
//        LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);
//
//        // Format the LocalDate to the required format
//        String formattedFromDate = fromDate.format(outputFormatter);
//        String formattedToDate = toDate.format(outputFormatter);
//        
//        String sql = "SELECT  "
//        		+ "    `wlid` AS comp_id,  "
//        		+ "    `zone` AS comp_zone,  "
//        		+ "    `ward` AS comp_ward,  "
//        		+ "    `EG_USER`.`EXTRAFIELD2` AS comp_contact,  "
//        		//+ "    `EG_USER`.`FIRST_NAME` AS comp_name,  "
//        		+ "		CONCAT(COALESCE(EG_USER.FIRST_NAME, ''), ' ', COALESCE(EG_USER.MIDDLE_NAME, '')) AS comp_name, "
//        		+ "		DATE_FORMAT(`cdate`, '%d-%m-%Y %r') AS comp_date, "
//        		+ "    `latitude` AS comp_latitude,  "
//        		+ "    `longitude` AS comp_longitude,  "
//        		+ "    `address` AS comp_area,  "
//        		+ "    `tonage` AS appx_tonage,  "
//        		+ "    wt.`name` AS comp_type,  "
//        		+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `file`) AS image,  "
//        		+ "    `status` AS comp_status, "
//        		+ "		`street_type` AS comp_street_type, "
//        		+ "		`kpi` AS comp_kpi "
//        		+ "FROM  "
//        		+ "    `waste_location_mapping` "
//        		+ "JOIN  "
//        		+ "    `erp_pgr`.`EG_USER`  "
//        		+ "    ON `waste_location_mapping`.cby = `EG_USER`.id  "
//        		+ "LEFT JOIN "
//	            + "    `waste_type` wt " 
//	            + "    ON waste_location_mapping.type = wt.typeid "
//        		+ "WHERE  "
//        		+ "    `waste_location_mapping`.isactive = 1 "
//        		+ " AND `cby`=? "
//        		+ " AND DATE(`waste_location_mapping`.cdate) BETWEEN ? AND ? "
//        		+ "ORDER BY `cdate` DESC "
//        		+ "LIMIT 50";
//        
//        return jdbcWasteTemplate.queryForList(sql, loginId, formattedFromDate, formattedToDate);
//    }
	
//	public String getWardByLoginId(String loginid, String type) {
//	    String sqlQuery = "SELECT `ward` FROM `gcc_penalty_hoardings`.`hoading_user_list` WHERE `userid` = ? LIMIT 1";
//	    
//	    // Query the database using queryForList
//	    List<Map<String, Object>> results = jdbcWasteTemplate.queryForList(sqlQuery, loginid);
//	    
//	    // Check if results is not empty and extract the ward value
//	    if (!results.isEmpty()) {
//	        // Extract the ward value from the first result
//	        return (String) results.get(0).get("ward");
//	    }
//	    
//	    // Handle the case where no result is found
//	    return "000";  // or return null based on your needs
//	}
//	
//	public List<Map<String, Object>> getApprovalPendingList(String loginId, String ward) {
//		
//		String approve_check_ward = getWardByLoginId(loginId,"");
//		
//		System.out.println("Login ID: "+ loginId 
//				+ "/n Type: ae "
//				+ "/n approve_check_ward: "+approve_check_ward);
//		
//        String sql = "SELECT  "
//        		+ "    `wlid` AS comp_id,  "
//        		+ "    `zone` AS comp_zone,  "
//        		+ "    `ward` AS comp_ward,  "
//        		+ "    `EG_USER`.`EXTRAFIELD2` AS comp_contact,  "
//        		+ "		CONCAT(COALESCE(EG_USER.FIRST_NAME, ''), ' ', COALESCE(EG_USER.MIDDLE_NAME, '')) AS comp_name, "
//        		//+ "    `EG_USER`.`FIRST_NAME` AS comp_name,  "
//        		+ "		DATE_FORMAT(`cdate`, '%d-%m-%Y %r') AS comp_date, "
//        		+ "    `latitude` AS comp_latitude,  "
//        		+ "    `longitude` AS comp_longitude,  "
//        		+ "    `address` AS comp_area,  "
//        		+ "    `tonage` AS appx_tonage,  "
//        		+ "    wt.`name` AS comp_type,  "
//        		+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `file`) AS image,  "
//        		+ "    `status` AS comp_status,  "
//        		+ "		`street_type` AS comp_street_type, "
//        		+ "		`kpi` AS comp_kpi "
//        		+ "FROM  "
//        		+ "    `waste_location_mapping` "
//        		+ "JOIN  "
//        		+ "    `erp_pgr`.`EG_USER`  "
//        		+ "    ON `EG_USER`.id =  `waste_location_mapping`.`cby`"
//        		+ "LEFT JOIN "
//	            + "    `waste_type` wt " 
//	            + "    ON waste_location_mapping.type = wt.typeid "
//        		+ "WHERE  "
//        		+ "    `waste_location_mapping`.isactive = 1 "
//        		+ " AND `ward`= ? "
//        		//+ " AND `request_by_type`='Vendor' "
//        		+ " AND `status`='open' "
//        		+ " AND `approved`='pending' "
//        		+ "ORDER BY `cdate` DESC";
//        
//        return jdbcWasteTemplate.queryForList(sql, approve_check_ward);
//    }
	
	public Map<String, Object> getWardByLoginId(String loginid, String type) {
	    String sqlQuery = "SELECT `ward`,`type`,`zone` FROM `gcc_penalty_hoardings`.`hoading_user_list` WHERE `userid` = ? LIMIT 1";
	    
	    // Query the database using queryForList
	    List<Map<String, Object>> results = jdbcWasteTemplate.queryForList(sqlQuery, loginid);
	    
	    // Check if results is not empty and extract the ward value
	    if (!results.isEmpty()) {
	        // Extract the ward value from the first result
	        return results.get(0);
	    }
	    
	    // Handle the case where no result is found
	    return new HashMap<>(); // or return null based on your needs
	}
	
//	public List<Map<String, Object>> getApprovalPendingList(String loginId, String ward) {
//		
//		//String approve_check_ward = getWardByLoginId(loginId,"");
//		
//		Map<String, Object> approve_check=getWardByLoginId(loginId,"");
//
//		String type="";
//		String zone="00";
//		String userWard="000";
//		
//		if (approve_check != null && !approve_check.isEmpty()) {
//
//	         type = approve_check.get("type") != null 
//	                      ? approve_check.get("type").toString() 
//	                      : "";
//
//	         zone = approve_check.get("zone") != null 
//	                      ? approve_check.get("zone").toString() 
//	                      : "00";
//
//	         userWard = approve_check.get("ward") != null 
//	                          ? approve_check.get("ward").toString() 
//	                          : "000";
//
////	        System.out.println("Type: " + type);
////	        System.out.println("Zone: " + zone);
////	        System.out.println("Ward: " + userWard);
//	    }
//		
//		System.out.println("Login ID: "+ loginId 
//				+ "/n Type: ae "
//				+ "/n approve_check_ward: "+userWard);
//		
//		if("swmee".equals(type)) {
//			
//			//System.out.println("if block");
//			
//			String sql = "SELECT  "
//	        		+ "    `wlid` AS comp_id,  "
//	        		+ "    `zone` AS comp_zone,  "
//	        		+ "    `ward` AS comp_ward,  "
//	        		+ "    `EG_USER`.`EXTRAFIELD2` AS comp_contact,  "
//	        		+ "		CONCAT(COALESCE(EG_USER.FIRST_NAME, ''), ' ', COALESCE(EG_USER.MIDDLE_NAME, '')) AS comp_name, "
//	        		//+ "    `EG_USER`.`FIRST_NAME` AS comp_name,  "
//	        		+ "		DATE_FORMAT(`cdate`, '%d-%m-%Y %r') AS comp_date, "
//	        		+ "    `latitude` AS comp_latitude,  "
//	        		+ "    `longitude` AS comp_longitude,  "
//	        		+ "    `address` AS comp_area,  "
//	        		+ "    `tonage` AS appx_tonage,  "
//	        		+ "    wt.`name` AS comp_type,  "
//	        		+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `file`) AS image,  "
//	        		+ "    `status` AS comp_status,  "
//	        		+ "		`street_type` AS comp_street_type, "
//	        		+ "		`kpi` AS comp_kpi "
//	        		+ "FROM  "
//	        		+ "    `waste_location_mapping` "
//	        		+ "JOIN  "
//	        		+ "    `erp_pgr`.`EG_USER`  "
//	        		+ "    ON `EG_USER`.id =  `waste_location_mapping`.`cby`"
//	        		+ "LEFT JOIN "
//		            + "    `waste_type` wt " 
//		            + "    ON waste_location_mapping.type = wt.typeid "
//	        		+ "WHERE  "
//	        		+ "    `waste_location_mapping`.isactive = 1 "
//	        		//+ " AND `ward`= ? "
//	        		+ " AND `escalation_flag`=1 "
//	        		//+ " AND `request_by_type`='Vendor' "
//	        		+ " AND `status`='open' "
//	        		+ " AND `approved`='pending' "
//	        		+ "ORDER BY `cdate` DESC";
//			
//			return jdbcWasteTemplate.queryForList(sql);
//			
//		}
//		else if("zo".equals(type)) {
//			//System.out.println("else if block");
//			
//			String sql = "SELECT  "
//	        		+ "    `wlid` AS comp_id,  "
//	        		+ "    `zone` AS comp_zone,  "
//	        		+ "    `ward` AS comp_ward,  "
//	        		+ "    `EG_USER`.`EXTRAFIELD2` AS comp_contact,  "
//	        		+ "		CONCAT(COALESCE(EG_USER.FIRST_NAME, ''), ' ', COALESCE(EG_USER.MIDDLE_NAME, '')) AS comp_name, "
//	        		//+ "    `EG_USER`.`FIRST_NAME` AS comp_name,  "
//	        		+ "		DATE_FORMAT(`cdate`, '%d-%m-%Y %r') AS comp_date, "
//	        		+ "    `latitude` AS comp_latitude,  "
//	        		+ "    `longitude` AS comp_longitude,  "
//	        		+ "    `address` AS comp_area,  "
//	        		+ "    `tonage` AS appx_tonage,  "
//	        		+ "    wt.`name` AS comp_type,  "
//	        		+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `file`) AS image,  "
//	        		+ "    `status` AS comp_status,  "
//	        		+ "		`street_type` AS comp_street_type, "
//	        		+ "		`kpi` AS comp_kpi "
//	        		+ "FROM  "
//	        		+ "    `waste_location_mapping` "
//	        		+ "JOIN  "
//	        		+ "    `erp_pgr`.`EG_USER`  "
//	        		+ "    ON `EG_USER`.id =  `waste_location_mapping`.`cby`"
//	        		+ "LEFT JOIN "
//		            + "    `waste_type` wt " 
//		            + "    ON waste_location_mapping.type = wt.typeid "
//	        		+ "WHERE  "
//	        		+ "    `waste_location_mapping`.isactive = 1 "
//	        		//+ " AND `ward`= ? "
//	        		+ " AND `escalation_flag`=1 "
//	        		+ " AND `zone`= ? "
//	        		//+ " AND `request_by_type`='Vendor' "
//	        		+ " AND `status`='open' "
//	        		+ " AND `approved`='pending' "
//	        		+ "ORDER BY `cdate` DESC";
//			
//			return jdbcWasteTemplate.queryForList(sql, zone);
//		}
//		else {
//			
//			//System.out.println("else block");
//			
//			String sql = "SELECT  "
//	        		+ "    `wlid` AS comp_id,  "
//	        		+ "    `zone` AS comp_zone,  "
//	        		+ "    `ward` AS comp_ward,  "
//	        		+ "    `EG_USER`.`EXTRAFIELD2` AS comp_contact,  "
//	        		+ "		CONCAT(COALESCE(EG_USER.FIRST_NAME, ''), ' ', COALESCE(EG_USER.MIDDLE_NAME, '')) AS comp_name, "
//	        		//+ "    `EG_USER`.`FIRST_NAME` AS comp_name,  "
//	        		+ "		DATE_FORMAT(`cdate`, '%d-%m-%Y %r') AS comp_date, "
//	        		+ "    `latitude` AS comp_latitude,  "
//	        		+ "    `longitude` AS comp_longitude,  "
//	        		+ "    `address` AS comp_area,  "
//	        		+ "    `tonage` AS appx_tonage,  "
//	        		+ "    wt.`name` AS comp_type,  "
//	        		+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `file`) AS image,  "
//	        		+ "    `status` AS comp_status,  "
//	        		+ "		`street_type` AS comp_street_type, "
//	        		+ "		`kpi` AS comp_kpi "
//	        		+ "FROM  "
//	        		+ "    `waste_location_mapping` "
//	        		+ "JOIN  "
//	        		+ "    `erp_pgr`.`EG_USER`  "
//	        		+ "    ON `EG_USER`.id =  `waste_location_mapping`.`cby`"
//	        		+ "LEFT JOIN "
//		            + "    `waste_type` wt " 
//		            + "    ON waste_location_mapping.type = wt.typeid "
//	        		+ "WHERE  "
//	        		+ "    `waste_location_mapping`.isactive = 1 "
//	        		+ " AND `ward`= ? "
//	        		//+ " AND `request_by_type`='Vendor' "
//	        		+ " AND `status`='open' "
//	        		+ " AND `approved`='pending' "
//	        		+ "ORDER BY `cdate` DESC";
//	        
//	        return jdbcWasteTemplate.queryForList(sql, userWard);
//		}
//		
//        
//    }

//scheduler method
    public Map<String, Object> updateCDWasteEscalationStatus(String status) {
        try {
            String sql = "UPDATE `waste_location_mapping` "
            		+ "SET `escalation_flag` = ? "
            		+ "WHERE `isactive`=1 AND `escalation_flag` = 0 "
            		+ " AND `status`='open' "
	        		+ " AND `approved`='pending' "
            		+ "AND `cdate` < NOW() - INTERVAL 48 HOUR";

            int updated = jdbcWasteTemplate.update(sql, status);

            return Map.of("status", true, "message", "Escalation status updated in CD_Waste", "rowsAffected", updated);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("status", false, "message", "Error: " + e.getMessage());
        }
    }
	
	public List<Map<String, Object>> checkApprovalLocation(String comp_id, String latitude, String longitude) {
    	
		String sql = "SELECT "
	            + "    wlm.`wlid` AS comp_id, "
				/*
	            + "    wlm.`zone` AS comp_zone, "
	            + "    wlm.`ward` AS comp_ward, "
	            + "    eg_user.`EXTRAFIELD2` AS comp_contact, "
	            + "    eg_user.`FIRST_NAME` AS comp_name, "
	            + "    DATE_FORMAT(wlm.`cdate`, '%d-%m-%Y %r') AS comp_date, "
	            + "    wlm.`latitude` AS comp_latitude, "
	            + "    wlm.`longitude` AS comp_longitude, "
	            + "    wlm.`address` AS comp_area, "
	            + "    wlm.`tonage` AS appx_tonage, "
	            + "    wt.`name` AS comp_type, "
	            + "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', wlm.`file`) AS image, "
	            */
	            + "    wlm.`status` AS comp_status "
	            + "FROM "
	            + "    `waste_location_mapping` wlm "
	            + "JOIN "
	            + "    `erp_pgr`.`EG_USER` eg_user "
	            + "    ON wlm.`cby` = eg_user.`id` "
	            + "LEFT JOIN "
	            + "    `waste_type` wt "
	            + "    ON wlm.`type` = wt.`typeid` "
	            + "WHERE "
	            + "    wlm.`isactive` = 1 "
	            + "    AND wlm.`wlid` = ? "
	            + "    AND ((6371008.8 * acos(ROUND(cos(radians(?)) * cos(radians(wlm.latitude)) * cos(radians(wlm.longitude) - radians(?)) + sin(radians(?)) * sin(radians(wlm.latitude)), 9))) < 50) "
	            + "    AND wlm.`status` = 'open' "
	            + "ORDER BY wlm.`cdate` DESC";
        	
		// Execute the query and check the result
	    List<Map<String, Object>> result = jdbcWasteTemplate.queryForList(sql, new Object[]{comp_id, latitude, longitude, latitude});

	    // Create response map with 'status' field based on result existence
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", !result.isEmpty());

	    // Wrap the response in a list as required
	    return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> saveApproval(
		String loginId, String comp_id, 
		String latitude, String longitude, 
		String in_out, String status, String reject_txt
		) {
    	
		//approve : yes or no
		String final_vendor_approval = "pending";
		
		if(in_out.equals("in") && status.equals("yes")) {
			final_vendor_approval = "yes";
		}
		
		if(in_out.equals("in") && status.equals("no")) {
			final_vendor_approval = "no";
		}
		
		if(in_out.equals("out") && status.equals("yes")) {
			final_vendor_approval = "yes";
			//final_vendor_approval = "pending";
		}
		
		if(in_out.equals("out") && status.equals("no")) {
			final_vendor_approval = "no";
		}
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		String sql = "UPDATE `waste_location_mapping` SET `approved`=? , `approvedby`=?, `reject_comments`=?, "
				+ "`approve_latitude`=?, `approve_longitude`=?, `approve_in_out`=?, `final_vendor_approval`=?, `approved_date`=? "
				+ " WHERE `wlid`=?";
		
		// Execute the update query
	    int rowsAffected = jdbcWasteTemplate.update(sql, new Object[]{
	        status, loginId, reject_txt, latitude, longitude, in_out, final_vendor_approval, formattedDate, comp_id
	    });

	    // Create a response map
	    Map<String, Object> response = new HashMap<>();
	    
	    // If rows are affected, it means the update was successful
	    response.put("status", rowsAffected > 0);

	    // Wrap the response in a list as required
	    return Collections.singletonList(response);
    }
	
	
	public List<Map<String, Object>> get2ApprovalPendingList(String loginId, String ward) {
        String sql = "SELECT  "
        		+ "    `wlid` AS comp_id,  "
        		+ "    `zone` AS comp_zone,  "
        		+ "    `ward` AS comp_ward,  "
        		+ "    `EG_USER`.`EXTRAFIELD2` AS comp_contact,  "
        		//+ "    `EG_USER`.`FIRST_NAME` AS comp_name,  "
        		+ "		CONCAT(COALESCE(EG_USER.FIRST_NAME, ''), ' ', COALESCE(EG_USER.MIDDLE_NAME, '')) AS comp_name, "
        		+ "		DATE_FORMAT(`cdate`, '%d-%m-%Y %r') AS comp_date, "
        		+ "    `latitude` AS comp_latitude,  "
        		+ "    `longitude` AS comp_longitude,  "
        		+ "    `address` AS comp_area,  "
        		+ "    `tonage` AS appx_tonage,  "
        		+ "    wt.`name` AS comp_type,  "
        		+ "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `file`) AS image,  "
        		+ "    `status` AS comp_status,  "
        		+ "		`street_type` AS comp_street_type, "
        		+ "		`kpi` AS comp_kpi "
        		+ "FROM  "
        		+ "    `waste_location_mapping` "
        		+ "JOIN  "
        		+ "    `erp_pgr`.`EG_USER`  "
        		+ "    ON `EG_USER`.id =? "
        		+ "LEFT JOIN "
	            + "    `waste_type` wt " 
	            + "    ON waste_location_mapping.type = wt.typeid "
        		+ "WHERE  "
        		+ "    `waste_location_mapping`.isactive = 1 "
        		+ " AND `ward`=? "
        		//+ " AND `request_by_type`='Vendor' "
        		+ " AND `status`='open' "
        		+ " AND `approved`='yes' "
        		+ " AND `approve_in_out`='out' "
        		+ " AND `approved_2`='-' "
        		+ "ORDER BY `cdate` DESC";
        
        return jdbcWasteTemplate.queryForList(sql,loginId, ward);
    }
	
	public List<Map<String, Object>> save2Approval(
			String loginId, String comp_id, 
			String latitude, String longitude, 
			String status
			) {
	    	
			//approve : yes or no
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		String sql = "UPDATE `waste_location_mapping` SET `approved_2`=? , `approvedby_2`=?, `approve_latitude_2`=?, "
				+ "`approve_longitude_2`=?, `final_vendor_approval`=?,`approved_date_2`=? "
				+ " WHERE `wlid`=? AND `approve_in_out`='out'";
		
		// Execute the update query
	    int rowsAffected = jdbcWasteTemplate.update(sql, new Object[]{
	        status, loginId, latitude, longitude, status, formattedDate, comp_id
	    });

	    // Create a response map
	    Map<String, Object> response = new HashMap<>();
	    
	    // If rows are affected, it means the update was successful
	    response.put("status", rowsAffected > 0);

	    // Wrap the response in a list as required
	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getZoneWiseCount(String fromDATE, String toDATE, String requestBy) {
		// Define the input and output date formats
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Parse the input dates to LocalDate
        LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
        LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);

        // Format the LocalDate to the required format
        String formattedFromDate = fromDate.format(outputFormatter);
        String formattedToDate = toDate.format(outputFormatter);
        
        String sql = "SELECT "
                + "    CASE  "
                + "        WHEN `waste_location_mapping`.`zone` IS NULL THEN 'Total' "
                + "        ELSE `waste_location_mapping`.`zone` "
                + "    END AS `zone`,  "
                + "    COUNT(CASE WHEN `waste_location_mapping`.`status` = 'Open' THEN 1 END) AS `Open`, "
                + "    COUNT(CASE WHEN `waste_location_mapping`.`status` = 'Close' THEN 1 END) AS `Close`, "
                + "    COUNT(`waste_location_mapping`.`wlid`) AS `Total`, "
                + "    FORMAT(SUM(`waste_location_mapping`.`tonage`),2) AS `Open_Tonage`, "
                + "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`status` = 'Close' THEN wr.`tonage` ELSE 0 END),2) AS `Close_Tonage` "
                + "FROM "
                + "    `waste_location_mapping` "
                + "JOIN "
                + "    `erp_pgr`.`EG_USER` ON `waste_location_mapping`.cby = `EG_USER`.id "
                + "LEFT JOIN "
                + "    `waste_reply` wr ON `waste_location_mapping`.`wlid` = wr.`wlid` AND wr.`isactive`=1 "
                + "WHERE "
                + "    `waste_location_mapping`.isactive = 1 "
                + "    AND DATE(`waste_location_mapping`.cdate) BETWEEN ? AND ? "
                + "    AND `waste_location_mapping`.request_by_type IN ("+requestBy+") "
                + "GROUP BY "
                + "    `waste_location_mapping`.`zone` "
                + "WITH ROLLUP";
        
        return jdbcWasteTemplate.queryForList(sql, formattedFromDate, formattedToDate);
    }
	
	public List<Map<String, Object>> getWardWiseCount(String fromDATE, String toDATE, String zone, String requestBy) {
		
		// Define the input and output date formats
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Parse the input dates to LocalDate
        LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
        LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);

        // Format the LocalDate to the required format
        String formattedFromDate = fromDate.format(outputFormatter);
        String formattedToDate = toDate.format(outputFormatter);
        
        String sql = "SELECT "
                + "    CASE  "
                + "        WHEN `waste_location_mapping`.`ward` IS NULL THEN 'Total' "
                + "        ELSE `waste_location_mapping`.`ward` "
                + "    END AS `ward`,  "
                + "    COUNT(CASE WHEN `waste_location_mapping`.`status` = 'Open' THEN 1 END) AS `Open`, "
                + "    COUNT(CASE WHEN `waste_location_mapping`.`status` = 'Close' THEN 1 END) AS `Close`, "
                + "    COUNT(`waste_location_mapping`.`wlid`) AS `Total`, "
                + "    FORMAT(SUM(`waste_location_mapping`.`tonage`),2) AS `Open_Tonage`, "
                + "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`status` = 'Close' THEN wr.`tonage` ELSE 0 END),2) AS `Close_Tonage` "
                + "FROM "
                + "    `waste_location_mapping` "
                + "JOIN "
                + "    `erp_pgr`.`EG_USER` ON `waste_location_mapping`.cby = `EG_USER`.id "
                + "LEFT JOIN "
                + "    `waste_reply` wr ON `waste_location_mapping`.`wlid` = wr.`wlid` AND wr.`isactive`=1 "
                + "WHERE "
                + "    `waste_location_mapping`.isactive = 1 "
                + " AND `waste_location_mapping`.`zone`= ? "
                + "    AND DATE(`waste_location_mapping`.cdate) BETWEEN ? AND ? "
                + "    AND `waste_location_mapping`.request_by_type IN ("+requestBy+") "
                + "GROUP BY "
                + "    `waste_location_mapping`.`ward` "
                + "WITH ROLLUP";
        
        return jdbcWasteTemplate.queryForList(sql, zone, formattedFromDate, formattedToDate);
    }
	
	// New Report
	
//	public List<Map<String, Object>> getOfficerZoneWiseCount(String fromDATE, String toDATE, String requestBy) {
//		// Define the input and output date formats
//        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
//        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//        // Parse the input dates to LocalDate
//        LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
//        LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);
//
//        // Format the LocalDate to the required format
//        String formattedFromDate = fromDate.format(outputFormatter);
//        String formattedToDate = toDate.format(outputFormatter);
//        
//        String sql = "SELECT "
//        		+ "    CASE  "
//        		+ "        WHEN `waste_location_mapping`.`zone` IS NULL THEN 'Total' "
//        		+ "        ELSE `waste_location_mapping`.`zone` "
//        		+ "    END AS `zone`,  "
//        		+ "    COUNT(`waste_location_mapping`.`wlid`) AS `Total_Identified_Count`, "
//        		+ "    FORMAT(SUM(`waste_location_mapping`.`tonage`)/1000, 2) AS `Total_Identified_Tonage`, "
//        		+ "    COUNT(CASE WHEN `waste_location_mapping`.`status` = 'Open' THEN 1 END) AS `Pending_Count`, "
//        		+ "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`status` = 'Open' THEN `waste_location_mapping`.`tonage` ELSE 0 END)/1000, 2) AS `Pending_Tonage`, "
//        		+ "    COUNT(CASE WHEN `waste_location_mapping`.`status` = 'Close' THEN 1 END) AS `Closed_Count`, "
//        		+ "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`status` = 'Close' THEN wr.`tonage` ELSE 0 END)/1000, 2) AS `Close_Tonage` "
//        		+ "FROM "
//        		+ "    `waste_location_mapping` "
//        		+ "JOIN "
//        		+ "    `erp_pgr`.`EG_USER` ON `waste_location_mapping`.cby = `EG_USER`.id "
//        		+ "LEFT JOIN "
//        		+ "    `waste_reply` wr ON `waste_location_mapping`.`wlid` = wr.`wlid` AND wr.`isactive`=1 "
//        		+ "WHERE "
//        		+ "    `waste_location_mapping`.isactive = 1 "
//        		+ "    AND DATE(`waste_location_mapping`.cdate) BETWEEN ? AND ? "
//        		+ "    AND `waste_location_mapping`.request_by_type IN ("+requestBy+") "
//        		+ "GROUP BY "
//        		+ "    `waste_location_mapping`.`zone` "
//        		+ "WITH ROLLUP";
//        
//        return jdbcWasteTemplate.queryForList(sql, formattedFromDate, formattedToDate);
//    }
//	
//	public List<Map<String, Object>> getOfficerWardWiseCount(String fromDATE, String toDATE, String zone, String requestBy) {
//		
//		// Define the input and output date formats
//        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
//        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//        // Parse the input dates to LocalDate
//        LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
//        LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);
//
//        // Format the LocalDate to the required format
//        String formattedFromDate = fromDate.format(outputFormatter);
//        String formattedToDate = toDate.format(outputFormatter);
//        
//        String sql = "SELECT "
//        		+ "    CASE  "
//        		+ "        WHEN `waste_location_mapping`.`ward` IS NULL THEN 'Total' "
//        		+ "        ELSE `waste_location_mapping`.`ward` "
//        		+ "    END AS `ward`,  "
//        		+ "    COUNT(`waste_location_mapping`.`wlid`) AS `Total_Identified_Count`, "
//        		+ "    FORMAT(SUM(`waste_location_mapping`.`tonage`)/1000, 2) AS `Total_Identified_Tonage`, "
//        		+ "    COUNT(CASE WHEN `waste_location_mapping`.`status` = 'Open' THEN 1 END) AS `Pending_Count`, "
//        		+ "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`status` = 'Open' THEN `waste_location_mapping`.`tonage` ELSE 0 END)/1000, 2) AS `Pending_Tonage`, "
//        		+ "    COUNT(CASE WHEN `waste_location_mapping`.`status` = 'Close' THEN 1 END) AS `Closed_Count`, "
//        		+ "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`status` = 'Close' THEN wr.`tonage` ELSE 0 END)/1000, 2) AS `Close_Tonage` "
//        		+ "FROM "
//        		+ "    `waste_location_mapping` "
//        		+ "JOIN "
//        		+ "    `erp_pgr`.`EG_USER` ON `waste_location_mapping`.cby = `EG_USER`.id "
//        		+ "LEFT JOIN "
//        		+ "    `waste_reply` wr ON `waste_location_mapping`.`wlid` = wr.`wlid` AND wr.`isactive`=1 "
//        		+ "WHERE "
//        		+ "    `waste_location_mapping`.isactive = 1 "
//        		+ "    AND `waste_location_mapping`.`zone`= ? "
//        		+ "    AND DATE(`waste_location_mapping`.cdate) BETWEEN ? AND ? "
//        		+ "    AND `waste_location_mapping`.request_by_type IN ("+requestBy+") "
//        		+ "GROUP BY "
//        		+ "    `waste_location_mapping`.`ward` "
//        		+ "WITH ROLLUP";
//        
//        return jdbcWasteTemplate.queryForList(sql, zone, formattedFromDate, formattedToDate);
//    }
//	
//	public List<Map<String, Object>> getVendorZoneWiseCount(String fromDATE, String toDATE, String requestBy) {
//		// Define the input and output date formats
//        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
//        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//        // Parse the input dates to LocalDate
//        LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
//        LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);
//
//        // Format the LocalDate to the required format
//        String formattedFromDate = fromDate.format(outputFormatter);
//        String formattedToDate = toDate.format(outputFormatter);
//        
//        String sql = "SELECT "
//        		+ "    CASE "
//        		+ "        WHEN `waste_location_mapping`.`zone` IS NULL THEN 'Total' "
//        		+ "        ELSE `waste_location_mapping`.`zone` "
//        		+ "    END AS `zone`, "
//        		+ "    COUNT(`waste_location_mapping`.`wlid`) AS `Total_Identified_Count`, "
//        		+ "    FORMAT(SUM(`waste_location_mapping`.`tonage`)/1000, 2) AS `Total_Identified_Tonage`, "
//        		+ "    COUNT(CASE WHEN `waste_location_mapping`.`final_vendor_approval` = 'yes' THEN 1 END) AS `Approved_Count`, "
//        		+ "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`final_vendor_approval` = 'yes' THEN waste_location_mapping.`tonage` ELSE 0 END)/1000, 2) AS `Approved_Tonage`, "
//        		+ "    COUNT(CASE WHEN `waste_location_mapping`.`final_vendor_approval` = 'no' THEN 1 END) AS `Rejected_Count`, "
//        		+ "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`final_vendor_approval` = 'no' THEN waste_location_mapping.`tonage` ELSE 0 END)/1000, 2) AS `Rejected_Tonage`, "
//        		+ "    COUNT(CASE WHEN `waste_location_mapping`.`final_vendor_approval` = 'pending' THEN 1 END) AS `Approved_Pending_Count`, "
//        		+ "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`final_vendor_approval` = 'pending' THEN waste_location_mapping.`tonage` ELSE 0 END)/1000, 2) AS `Approved_Pending_Tonage`, "
//        		+ "    COUNT(CASE WHEN `waste_location_mapping`.`status` = 'Open' AND `waste_location_mapping`.`final_vendor_approval` = 'yes' THEN 1 END) AS `Pending_Count`, "
//        		+ "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`status` = 'Open' AND `waste_location_mapping`.`final_vendor_approval` = 'yes' THEN waste_location_mapping.`tonage` ELSE 0 END)/1000, 2) AS `Pending_Tonage`, "
//        		+ "    COUNT(CASE WHEN `waste_location_mapping`.`status` = 'Close' AND `waste_location_mapping`.`final_vendor_approval` = 'yes' THEN 1 END) AS `Closed_Count`, "
//        		+ "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`status` = 'Close' AND `waste_location_mapping`.`final_vendor_approval` = 'yes' THEN wr.`tonage` ELSE 0 END)/1000, 2) AS `Close_Tonage` "
//        		+ "FROM "
//        		+ "    `waste_location_mapping` "
//        		+ "JOIN"
//        		+ "    `erp_pgr`.`EG_USER` ON `waste_location_mapping`.cby = `EG_USER`.id "
//        		+ "LEFT JOIN "
//        		+ "    `waste_reply` wr ON `waste_location_mapping`.`wlid` = wr.`wlid` AND wr.`isactive`=1 "
//        		+ "WHERE"
//        		+ "    `waste_location_mapping`.isactive = 1 "
//        		+ "    AND DATE(`waste_location_mapping`.cdate) BETWEEN ? AND ? "
//        		+ "    AND `waste_location_mapping`.request_by_type IN ("+requestBy+") "
//        		+ "GROUP BY"
//        		+ "    `waste_location_mapping`.`zone` "
//        		+ "WITH ROLLUP";
//        
//        return jdbcWasteTemplate.queryForList(sql, formattedFromDate, formattedToDate);
//    }
//	
//	
//	public List<Map<String, Object>> getVendorWardWiseCount(String fromDATE, String toDATE, String zone, String requestBy) {
//		
//		// Define the input and output date formats
//        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
//        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//        // Parse the input dates to LocalDate
//        LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
//        LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);
//
//        // Format the LocalDate to the required format
//        String formattedFromDate = fromDate.format(outputFormatter);
//        String formattedToDate = toDate.format(outputFormatter);
//        
//        String sql = "SELECT "
//        		+ "    CASE "
//        		+ "        WHEN `waste_location_mapping`.`ward` IS NULL THEN 'Total' "
//        		+ "        ELSE `waste_location_mapping`.`ward` "
//        		+ "    END AS `ward`, "
//        		+ "    COUNT(`waste_location_mapping`.`wlid`) AS `Total_Identified_Count`, "
//        		+ "    FORMAT(SUM(`waste_location_mapping`.`tonage`)/1000, 2) AS `Total_Identified_Tonage`, "
//        		+ "    COUNT(CASE WHEN `waste_location_mapping`.`final_vendor_approval` = 'yes' THEN 1 END) AS `Approved_Count`, "
//        		+ "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`final_vendor_approval` = 'yes' THEN waste_location_mapping.`tonage` ELSE 0 END)/1000, 2) AS `Approved_Tonage`, "
//        		+ "    COUNT(CASE WHEN `waste_location_mapping`.`final_vendor_approval` = 'no' THEN 1 END) AS `Rejected_Count`, "
//        		+ "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`final_vendor_approval` = 'no' THEN waste_location_mapping.`tonage` ELSE 0 END)/1000, 2) AS `Rejected_Tonage`, "
//        		+ "    COUNT(CASE WHEN `waste_location_mapping`.`final_vendor_approval` = 'pending' THEN 1 END) AS `Approved_Pending_Count`, "
//        		+ "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`final_vendor_approval` = 'pending' THEN waste_location_mapping.`tonage` ELSE 0 END)/1000, 2) AS `Approved_Pending_Tonage`, "
//        		+ "    COUNT(CASE WHEN `waste_location_mapping`.`status` = 'Open' AND `waste_location_mapping`.`final_vendor_approval` = 'yes' THEN 1 END) AS `Pending_Count`, "
//        		+ "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`status` = 'Open' AND `waste_location_mapping`.`final_vendor_approval` = 'yes' THEN waste_location_mapping.`tonage` ELSE 0 END)/1000, 2) AS `Pending_Tonage`, "
//        		+ "    COUNT(CASE WHEN `waste_location_mapping`.`status` = 'Close' AND `waste_location_mapping`.`final_vendor_approval` = 'yes' THEN 1 END) AS `Closed_Count`, "
//        		+ "    FORMAT(SUM(CASE WHEN `waste_location_mapping`.`status` = 'Close' AND `waste_location_mapping`.`final_vendor_approval` = 'yes' THEN wr.`tonage` ELSE 0 END)/1000, 2) AS `Close_Tonage` "
//        		+ "FROM "
//        		+ "    `waste_location_mapping` "
//        		+ "JOIN"
//        		+ "    `erp_pgr`.`EG_USER` ON `waste_location_mapping`.cby = `EG_USER`.id "
//        		+ "LEFT JOIN "
//        		+ "    `waste_reply` wr ON `waste_location_mapping`.`wlid` = wr.`wlid` AND wr.`isactive`=1 "
//        		+ "WHERE"
//        		+ "    `waste_location_mapping`.isactive = 1 "
//        		+ "    AND `waste_location_mapping`.`zone`= ? "
//        		+ "    AND DATE(`waste_location_mapping`.cdate) BETWEEN ? AND ? "
//        		+ "    AND `waste_location_mapping`.request_by_type IN ("+requestBy+") "
//        		+ "GROUP BY"
//        		+ "    `waste_location_mapping`.`ward` "
//        		+ "WITH ROLLUP";
//        
//        return jdbcWasteTemplate.queryForList(sql, zone, formattedFromDate, formattedToDate);
//    }
	///////////
	
	
	
	public List<Map<String, Object>> getMyticketllist(String loginId,String fromDATE,String toDATE,String type) {

		// Date conversion
		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
		LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
        LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);
		
		String formattedFromDate = fromDate.format(outputFormatter);
		String formattedToDate = toDate.format(outputFormatter);
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT ")
		.append(" wlid AS comp_id, ")
		.append(" zone AS comp_zone, ")
		.append(" ward AS comp_ward, ")
		.append(" EG_USER.EXTRAFIELD2 AS comp_contact, ")
		.append(" CONCAT(COALESCE(EG_USER.FIRST_NAME,''),' ',COALESCE(EG_USER.MIDDLE_NAME,'')) AS comp_name, ")
		.append(" DATE_FORMAT(cdate, '%d-%m-%Y %r') AS comp_date, ")
		.append(" latitude AS comp_latitude, ")
		.append(" longitude AS comp_longitude, ")
		.append(" address AS comp_area, ")
		.append(" tonage AS appx_tonage, ")
		.append(" wt.name AS comp_type, ")
		.append(" CONCAT('").append(fileBaseUrl).append("/gccofficialapp/files', file) AS image, ")
		.append(" status AS comp_status, ")
		.append(" street_type AS comp_street_type, ")
		.append(" kpi AS comp_kpi ")
		.append(" FROM waste_location_mapping ")
		.append(" JOIN erp_pgr.EG_USER ON waste_location_mapping.cby = EG_USER.id ")
		.append(" LEFT JOIN waste_type wt ON waste_location_mapping.type = wt.typeid ")
		.append(" WHERE waste_location_mapping.isactive = 1 ")
		.append(" AND cby = ? ")
		.append(" AND DATE(waste_location_mapping.cdate) BETWEEN ? AND ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(loginId);
		params.add(formattedFromDate);
		params.add(formattedToDate);
		
		//  Append type condition only if present
		if (type != null && !type.trim().isEmpty()) {
			int wasteType = Integer.parseInt(type);
			sql.append(" AND waste_location_mapping.type = ? ");
			params.add(wasteType);
		}
		
		sql.append(" ORDER BY cdate DESC LIMIT 50");
		
		return jdbcWasteTemplate.queryForList(sql.toString(), params.toArray());
	}



public List<Map<String, Object>> getApprovalPendingList(String loginId, String ward, String type) {

    Map<String, Object> approve_check = getWardByLoginId(loginId, "");

    String dtype = "";
    String zone = "00";
    String userWard = "000";

    if (approve_check != null && !approve_check.isEmpty()) {
        dtype = approve_check.get("type") != null ? approve_check.get("type").toString() : "";
        zone = approve_check.get("zone") != null ? approve_check.get("zone").toString() : "00";
        userWard = approve_check.get("ward") != null ? approve_check.get("ward").toString() : "000";
    }
    
	System.out.println("Login ID: "+ loginId + " Type: "+dtype+  " approve_check_ward: "+userWard);

    StringBuilder sql = new StringBuilder();

    
    
    sql.append("SELECT ")
       .append(" wlid AS comp_id, ")
       .append(" zone AS comp_zone, ")
       .append(" ward AS comp_ward, ")
       .append(" EG_USER.EXTRAFIELD2 AS comp_contact, ")
       .append(" CONCAT(COALESCE(EG_USER.FIRST_NAME, ''), ' ', COALESCE(EG_USER.MIDDLE_NAME, '')) AS comp_name, ")       
       .append(" DATE_FORMAT(cdate, '%d-%m-%Y %r') AS comp_date, ")
       .append(" latitude AS comp_latitude, ")
       .append(" longitude AS comp_longitude, ")
       .append(" address AS comp_area, ")
       .append(" tonage AS appx_tonage, ")
       .append(" wt.name AS comp_type, ")
       .append(" CONCAT('").append(fileBaseUrl).append("/gccofficialapp/files', file) AS image, ")
       .append(" status AS comp_status, ")
       .append(" street_type AS comp_street_type, ")
       .append(" kpi AS comp_kpi ")
       .append(" FROM waste_location_mapping ")
       .append(" JOIN erp_pgr.EG_USER ON EG_USER.id = waste_location_mapping.cby ")
       .append(" LEFT JOIN waste_type wt ON waste_location_mapping.type = wt.typeid ")
       .append(" WHERE waste_location_mapping.isactive = 1 ")
       .append(" AND status = 'open' ")
       .append(" AND approved = 'pending' ");

    List<Object> params = new ArrayList<>();

    //  Role-based filtering
    if ("swmee".equals(dtype)) {
        sql.append(" AND escalation_flag = 1 ");
    } 
    else if ("zo".equals(dtype)) {
        sql.append(" AND escalation_flag = 1 ");
        sql.append(" AND zone = ? ");
        params.add(zone);
    } 
    else {
        sql.append(" AND ward = ? ");
        params.add(userWard);
    }

    //  type dynamic condition
    if (type != null && !type.trim().isEmpty()) {
        int wasteType = Integer.parseInt(type);
        sql.append(" AND waste_location_mapping.type = ? ");
        params.add(wasteType);
    }

    sql.append(" ORDER BY cdate DESC ");

    return jdbcWasteTemplate.queryForList(sql.toString(), params.toArray());
}



public List<Map<String, Object>> getOfficerZoneWiseCount(String fromDATE,
        String toDATE,
        String requestBy,
        String type) {

		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
		LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
		LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);
		
		String formattedFromDate = fromDate.format(outputFormatter);
		String formattedToDate = toDate.format(outputFormatter);
		
		StringBuilder sql = new StringBuilder();
		List<Object> params = new ArrayList<>();
		
		sql.append("SELECT ")
		.append(" CASE WHEN waste_location_mapping.zone IS NULL THEN 'Total' ")
		.append(" ELSE waste_location_mapping.zone END AS zone, ")
		.append(" COUNT(waste_location_mapping.wlid) AS Total_Identified_Count, ")
		.append(" FORMAT(SUM(waste_location_mapping.tonage)/1000, 2) AS Total_Identified_Tonage, ")
		.append(" COUNT(CASE WHEN waste_location_mapping.status = 'Open' THEN 1 END) AS Pending_Count, ")
		.append(" FORMAT(SUM(CASE WHEN waste_location_mapping.status = 'Open' ")
		.append(" THEN waste_location_mapping.tonage ELSE 0 END)/1000, 2) AS Pending_Tonage, ")
		.append(" COUNT(CASE WHEN waste_location_mapping.status = 'Close' THEN 1 END) AS Closed_Count, ")
		.append(" FORMAT(SUM(CASE WHEN waste_location_mapping.status = 'Close' ")
		.append(" THEN wr.tonage ELSE 0 END)/1000, 2) AS Close_Tonage ")
		.append(" FROM waste_location_mapping ")
		.append(" JOIN erp_pgr.EG_USER ON waste_location_mapping.cby = EG_USER.id ")
		.append(" LEFT JOIN waste_reply wr ON waste_location_mapping.wlid = wr.wlid AND wr.isactive=1 ")
		.append(" WHERE waste_location_mapping.isactive = 1 ")
		.append(" AND DATE(waste_location_mapping.cdate) BETWEEN ? AND ? ");
		
		params.add(formattedFromDate);
		params.add(formattedToDate);
		
		//  requestBy (assuming already formatted like 'Officer' or 'Vendor','IE')
		sql.append(" AND waste_location_mapping.request_by_type IN (" + requestBy + ") ");
		
		//  Append type only if present
		if (type != null && !type.trim().isEmpty()) {
		int wasteType = Integer.parseInt(type);
		sql.append(" AND waste_location_mapping.type = ? ");
		params.add(wasteType);
		}
		
		sql.append(" GROUP BY waste_location_mapping.zone WITH ROLLUP ");
		
		return jdbcWasteTemplate.queryForList(sql.toString(), params.toArray());
}


public List<Map<String, Object>> getOfficerWardWiseCount(String fromDATE,
        String toDATE,
        String zone,
        String requestBy,
        String type) {

		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
		LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
		LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);
		
		String formattedFromDate = fromDate.format(outputFormatter);
		String formattedToDate = toDate.format(outputFormatter);
		
		StringBuilder sql = new StringBuilder();
		List<Object> params = new ArrayList<>();
		
		sql.append("SELECT ")
		.append(" CASE WHEN waste_location_mapping.ward IS NULL THEN 'Total' ")
		.append(" ELSE waste_location_mapping.ward END AS ward, ")
		.append(" COUNT(waste_location_mapping.wlid) AS Total_Identified_Count, ")
		.append(" FORMAT(SUM(waste_location_mapping.tonage)/1000, 2) AS Total_Identified_Tonage, ")
		.append(" COUNT(CASE WHEN waste_location_mapping.status = 'Open' THEN 1 END) AS Pending_Count, ")
		.append(" FORMAT(SUM(CASE WHEN waste_location_mapping.status = 'Open' ")
		.append(" THEN waste_location_mapping.tonage ELSE 0 END)/1000, 2) AS Pending_Tonage, ")
		.append(" COUNT(CASE WHEN waste_location_mapping.status = 'Close' THEN 1 END) AS Closed_Count, ")
		.append(" FORMAT(SUM(CASE WHEN waste_location_mapping.status = 'Close' ")
		.append(" THEN wr.tonage ELSE 0 END)/1000, 2) AS Close_Tonage ")
		.append(" FROM waste_location_mapping ")
		.append(" JOIN erp_pgr.EG_USER ON waste_location_mapping.cby = EG_USER.id ")
		.append(" LEFT JOIN waste_reply wr ON waste_location_mapping.wlid = wr.wlid AND wr.isactive=1 ")
		.append(" WHERE waste_location_mapping.isactive = 1 ")
		.append(" AND waste_location_mapping.zone = ? ")
		.append(" AND DATE(waste_location_mapping.cdate) BETWEEN ? AND ? ");
		
		params.add(zone);
		params.add(formattedFromDate);
		params.add(formattedToDate);
		
		// requestBy (assumed already formatted properly like 'Officer')
		sql.append(" AND waste_location_mapping.request_by_type IN (" + requestBy + ") ");
		
		//  Append type condition if present
		if (type != null && !type.trim().isEmpty()) {
			int wasteType = Integer.parseInt(type);
			sql.append(" AND waste_location_mapping.type = ? ");
			params.add(wasteType);
		}
		
		sql.append(" GROUP BY waste_location_mapping.ward WITH ROLLUP ");
		
		return jdbcWasteTemplate.queryForList(sql.toString(), params.toArray());
	}



public List<Map<String, Object>> getVendorZoneWiseCount(String fromDATE,
        String toDATE,
        String requestBy,
        String type)
{

	DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
	LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);
	
	String formattedFromDate = fromDate.format(outputFormatter);
	String formattedToDate = toDate.format(outputFormatter);
	
	StringBuilder sql = new StringBuilder();
	List<Object> params = new ArrayList<>();
	
	sql.append("SELECT ")
	.append(" CASE WHEN waste_location_mapping.zone IS NULL THEN 'Total' ")
	.append(" ELSE waste_location_mapping.zone END AS zone, ")
	.append(" COUNT(waste_location_mapping.wlid) AS Total_Identified_Count, ")
	.append(" FORMAT(SUM(waste_location_mapping.tonage)/1000, 2) AS Total_Identified_Tonage, ")
	.append(" COUNT(CASE WHEN waste_location_mapping.final_vendor_approval='yes' THEN 1 END) AS Approved_Count, ")
	.append(" FORMAT(SUM(CASE WHEN waste_location_mapping.final_vendor_approval='yes' ")
	.append(" THEN waste_location_mapping.tonage ELSE 0 END)/1000,2) AS Approved_Tonage, ")
	.append(" COUNT(CASE WHEN waste_location_mapping.final_vendor_approval='no' THEN 1 END) AS Rejected_Count, ")
	.append(" FORMAT(SUM(CASE WHEN waste_location_mapping.final_vendor_approval='no' ")
	.append(" THEN waste_location_mapping.tonage ELSE 0 END)/1000,2) AS Rejected_Tonage, ")
	.append(" COUNT(CASE WHEN waste_location_mapping.final_vendor_approval='pending' THEN 1 END) AS Approved_Pending_Count, ")
	.append(" FORMAT(SUM(CASE WHEN waste_location_mapping.final_vendor_approval='pending' ")
	.append(" THEN waste_location_mapping.tonage ELSE 0 END)/1000,2) AS Approved_Pending_Tonage, ")
	.append(" COUNT(CASE WHEN waste_location_mapping.status='Open' ")
	.append(" AND waste_location_mapping.final_vendor_approval='yes' THEN 1 END) AS Pending_Count, ")
	.append(" FORMAT(SUM(CASE WHEN waste_location_mapping.status='Open' ")
	.append(" AND waste_location_mapping.final_vendor_approval='yes' ")
	.append(" THEN waste_location_mapping.tonage ELSE 0 END)/1000,2) AS Pending_Tonage, ")
	.append(" COUNT(CASE WHEN waste_location_mapping.status='Close' ")
	.append(" AND waste_location_mapping.final_vendor_approval='yes' THEN 1 END) AS Closed_Count, ")
	.append(" FORMAT(SUM(CASE WHEN waste_location_mapping.status='Close' ")
	.append(" AND waste_location_mapping.final_vendor_approval='yes' ")
	.append(" THEN wr.tonage ELSE 0 END)/1000,2) AS Close_Tonage ")
	.append(" FROM waste_location_mapping ")
	.append(" JOIN erp_pgr.EG_USER ON waste_location_mapping.cby = EG_USER.id ")
	.append(" LEFT JOIN waste_reply wr ON waste_location_mapping.wlid = wr.wlid AND wr.isactive=1 ")
	.append(" WHERE waste_location_mapping.isactive = 1 ")
	.append(" AND DATE(waste_location_mapping.cdate) BETWEEN ? AND ? ");
	
	params.add(formattedFromDate);
	params.add(formattedToDate);
	
	// requestBy (assumed already formatted like 'Vendor','IE')
	sql.append(" AND waste_location_mapping.request_by_type IN (" + requestBy + ") ");
	
	//  Append type condition if present
	if (type != null && !type.trim().isEmpty()) {
	int wasteType = Integer.parseInt(type);
	sql.append(" AND waste_location_mapping.type = ? ");
	params.add(wasteType);
	}
	
	sql.append(" GROUP BY waste_location_mapping.zone WITH ROLLUP ");
	
	return jdbcWasteTemplate.queryForList(sql.toString(), params.toArray());
}



public List<Map<String, Object>> getVendorWardWiseCount(String fromDATE,
        String toDATE,
        String zone,
        String requestBy,
        String type) {

		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
		LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
		LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);
		
		String formattedFromDate = fromDate.format(outputFormatter);
		String formattedToDate = toDate.format(outputFormatter);
		
		StringBuilder sql = new StringBuilder();
		List<Object> params = new ArrayList<>();
		
		sql.append("SELECT ")
		.append(" CASE WHEN waste_location_mapping.ward IS NULL THEN 'Total' ")
		.append(" ELSE waste_location_mapping.ward END AS ward, ")
		.append(" COUNT(waste_location_mapping.wlid) AS Total_Identified_Count, ")
		.append(" FORMAT(SUM(waste_location_mapping.tonage)/1000, 2) AS Total_Identified_Tonage, ")
		.append(" COUNT(CASE WHEN waste_location_mapping.final_vendor_approval='yes' THEN 1 END) AS Approved_Count, ")
		.append(" FORMAT(SUM(CASE WHEN waste_location_mapping.final_vendor_approval='yes' ")
		.append(" THEN waste_location_mapping.tonage ELSE 0 END)/1000,2) AS Approved_Tonage, ")
		.append(" COUNT(CASE WHEN waste_location_mapping.final_vendor_approval='no' THEN 1 END) AS Rejected_Count, ")
		.append(" FORMAT(SUM(CASE WHEN waste_location_mapping.final_vendor_approval='no' ")
		.append(" THEN waste_location_mapping.tonage ELSE 0 END)/1000,2) AS Rejected_Tonage, ")
		.append(" COUNT(CASE WHEN waste_location_mapping.final_vendor_approval='pending' THEN 1 END) AS Approved_Pending_Count, ")
		.append(" FORMAT(SUM(CASE WHEN waste_location_mapping.final_vendor_approval='pending' ")
		.append(" THEN waste_location_mapping.tonage ELSE 0 END)/1000,2) AS Approved_Pending_Tonage, ")
		.append(" COUNT(CASE WHEN waste_location_mapping.status='Open' ")
		.append(" AND waste_location_mapping.final_vendor_approval='yes' THEN 1 END) AS Pending_Count, ")
		.append(" FORMAT(SUM(CASE WHEN waste_location_mapping.status='Open' ")
		.append(" AND waste_location_mapping.final_vendor_approval='yes' ")
		.append(" THEN waste_location_mapping.tonage ELSE 0 END)/1000,2) AS Pending_Tonage, ")
		.append(" COUNT(CASE WHEN waste_location_mapping.status='Close' ")
		.append(" AND waste_location_mapping.final_vendor_approval='yes' THEN 1 END) AS Closed_Count, ")
		.append(" FORMAT(SUM(CASE WHEN waste_location_mapping.status='Close' ")
		.append(" AND waste_location_mapping.final_vendor_approval='yes' ")
		.append(" THEN wr.tonage ELSE 0 END)/1000,2) AS Close_Tonage ")
		.append(" FROM waste_location_mapping ")
		.append(" JOIN erp_pgr.EG_USER ON waste_location_mapping.cby = EG_USER.id ")
		.append(" LEFT JOIN waste_reply wr ON waste_location_mapping.wlid = wr.wlid AND wr.isactive=1 ")
		.append(" WHERE waste_location_mapping.isactive = 1 ")
		.append(" AND waste_location_mapping.zone = ? ")
		.append(" AND DATE(waste_location_mapping.cdate) BETWEEN ? AND ? ");
		
		params.add(zone);
		params.add(formattedFromDate);
		params.add(formattedToDate);
		
		// requestBy already formatted like 'Vendor','IE'
		sql.append(" AND waste_location_mapping.request_by_type IN (" + requestBy + ") ");
		
		// ✅ Append type condition if provided
		if (type != null && !type.trim().isEmpty()) {
			int wasteType = Integer.parseInt(type);
			sql.append(" AND waste_location_mapping.type = ? ");
			params.add(wasteType);
		}
		
		sql.append(" GROUP BY waste_location_mapping.ward WITH ROLLUP ");
		
		return jdbcWasteTemplate.queryForList(sql.toString(), params.toArray());
	}



public List<Map<String, Object>> getListByStatus(String fromDATE,
        String toDATE,
        String zone,
        String ward,
        String status,
        String requestBy,
        String type)
{

	DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
	LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);
	
	String formattedFromDate = fromDate.format(outputFormatter);
	String formattedToDate = toDate.format(outputFormatter);
	
	StringBuilder sql = new StringBuilder();
	List<Object> params = new ArrayList<>();
	
	//  Status handling
	if ("pending".equalsIgnoreCase(status)) {
		status = "open";
		sql.append(" AND wlm.final_vendor_approval = 'pending' ");
	}
	
	//  RequestBy handling
	if ("Vendor".equalsIgnoreCase(requestBy)) {
		requestBy = "'Vendor','councillor','IE'";
	} else if ("Officer".equalsIgnoreCase(requestBy)) {
		requestBy = "'Officer'";
	} else {
		requestBy = "'" + requestBy + "'";
	}
	
	sql.insert(0,
	"SELECT "
	+ " wlm.wlid AS comp_id, "
	+ " wlm.zone AS comp_zone, "
	+ " wlm.ward AS comp_ward, "
	+ " EG_USER.EXTRAFIELD2 AS comp_contact, "
	+ " CONCAT(COALESCE(EG_USER.FIRST_NAME,''),' ',COALESCE(EG_USER.MIDDLE_NAME,'')) AS comp_name, "
	+ " DATE_FORMAT(wlm.cdate, '%d-%m-%Y %r') AS comp_date, "
	+ " wlm.latitude AS comp_latitude, "
	+ " wlm.longitude AS comp_longitude, "
	+ " wlm.address AS comp_area, "
	+ " wlm.tonage AS appx_tonage, "
	+ " wt.name AS comp_type, "
	+ " CASE WHEN wlm.status='Close' THEN wr.tonage ELSE 0 END AS reply_tonage, "
	+ " CONCAT('" + fileBaseUrl + "/gccofficialapp/files', wlm.file) AS image, "
	+ " wlm.status AS comp_status, "
	+ " wlm.street_type AS comp_street_type, "
	+ " wlm.kpi AS comp_kpi "
	+ " FROM waste_location_mapping wlm "
	+ " JOIN erp_pgr.EG_USER ON wlm.cby = EG_USER.id "
	+ " LEFT JOIN waste_reply wr ON wlm.wlid = wr.wlid AND wr.isactive=1 "
	+ " LEFT JOIN waste_type wt ON wlm.type = wt.typeid "
	+ " WHERE wlm.isactive = 1 "
	+ " AND wlm.zone = ? "
	+ " AND wlm.ward = ? "
	);
	
	params.add(zone);
	params.add(ward);
	
	sql.append(" AND wlm.status = ? ");
	params.add(status);
	
	sql.append(" AND wlm.request_by_type IN (" + requestBy + ") ");
	
	sql.append(" AND DATE(wlm.cdate) BETWEEN ? AND ? ");
	params.add(formattedFromDate);
	params.add(formattedToDate);
	
	//  Append type condition only if present
	if (type != null && !type.trim().isEmpty()) {
	int wasteType = Integer.parseInt(type);
	sql.append(" AND wlm.type = ? ");
	params.add(wasteType);
	}
	
	//sql.append(" ORDER BY wlm.cdate DESC ");
	
	System.out.println(sql);
	
	return jdbcWasteTemplate.queryForList(sql.toString(), params.toArray());
}
	
	
//	public List<Map<String, Object>> getListByStatus(String fromDATE, String toDATE, String zone, String ward, String status, String requestBy) {
//		
//		// Define the input and output date formats
//        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
//        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//        // Parse the input dates to LocalDate
//        LocalDate fromDate = LocalDate.parse(fromDATE, inputFormatter);
//        LocalDate toDate = LocalDate.parse(toDATE, inputFormatter);
//
//        // Format the LocalDate to the required format
//        String formattedFromDate = fromDate.format(outputFormatter);
//        String formattedToDate = toDate.format(outputFormatter);
//        String sqlwhere="";
//        if(status.equals("pending")) {
//        	status = "open";
//        	sqlwhere = " AND wlm.`final_vendor_approval`='pending'";
//        	System.out.println(sqlwhere);
//        }
//        
//        //System.out.println("Before check: " + requestBy);
//
//        if ("Vendor".equalsIgnoreCase(requestBy)) {
//            requestBy = "'Vendor','councillor','IE'";
//        }
//        else if ("Officer".equalsIgnoreCase(requestBy)) {
//            requestBy = "'Officer'";
//        }
//        else {
//        	requestBy = "'"+requestBy+"'";
//        }
//
//        //System.out.println("After check: " + requestBy);
//        
//        String sql = "SELECT "
//                + "    wlm.`wlid` AS comp_id, "  
//                + "    wlm.`zone` AS comp_zone, "
//                + "    wlm.`ward` AS comp_ward, "
//                + "    `EG_USER`.`EXTRAFIELD2` AS comp_contact, "
//                //+ "    `EG_USER`.`FIRST_NAME` AS comp_name, "
//                + "		CONCAT(COALESCE(EG_USER.FIRST_NAME, ''), ' ', COALESCE(EG_USER.MIDDLE_NAME, '')) AS comp_name, "
//                + "    DATE_FORMAT(wlm.`cdate`, '%d-%m-%Y %r') AS comp_date, "
//                + "    wlm.`latitude` AS comp_latitude, "
//                + "    wlm.`longitude` AS comp_longitude, "
//                + "    wlm.`address` AS comp_area, "
//                + "    wlm.`tonage` AS appx_tonage, "
//                + "    wt.`name` AS comp_type, "
//                + "    CASE WHEN wlm.`status` = 'Close' THEN wr.`tonage` ELSE 0 END AS `reply_tonage`, "
//                + "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', wlm.`file`) AS image,  "
//                + "    wlm.`status` AS comp_status, "
//                + "		wlm.`street_type` AS comp_street_type, "
//        		+ "		wlm.`kpi` AS comp_kpi "
//                + "FROM "
//                + "    `waste_location_mapping` wlm "
//                + "JOIN "
//                + "    `erp_pgr`.`EG_USER` "
//                + "    ON wlm.`cby` = `EG_USER`.id "
//                + "LEFT JOIN "
//                + "    `waste_reply` wr "
//                + "    ON wlm.`wlid` = wr.`wlid` AND wr.`isactive`=1 "
//                + "LEFT JOIN "
//                + "    `waste_type` wt "
//                + "    ON wlm.`type` = wt.`typeid` "
//                + "WHERE "
//                + "    wlm.`isactive` = 1 "
//                + "    AND wlm.`zone` = ? "
//                + "    AND wlm.`ward` = ? "
//                + sqlwhere
//                + "    AND wlm.`status` = ? "
//                + "    AND wlm.`request_by_type` IN ("+requestBy+") "
//                + "    AND DATE(wlm.`cdate`) BETWEEN ? AND ?";
//		
//        System.out.println(sql);
//        return jdbcWasteTemplate.queryForList(sql, zone, ward, status, formattedFromDate, formattedToDate);
//    }
	
	public List<Map<String, Object>> getWardSumReport(String ward) {
	    
	    // Get current date
	    LocalDate toDate = LocalDate.now();
	    // Calculate 7 days before current date
	    LocalDate fromDate = toDate.minusDays(7);

	    // Define the input and output date formats
	    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	    
	    // Format the LocalDate to the required format
	    String formattedFromDate = fromDate.format(outputFormatter);
	    String formattedToDate = toDate.format(outputFormatter);

	    // SQL query for fetching data
	    String sql = "SELECT "
	            + "    CASE "
	            + "        WHEN `waste_location_mapping`.`ward` IS NULL THEN 'Total' "
	            + "        ELSE `waste_location_mapping`.`ward` "
	            + "    END AS `ward`,  "
	            + "    `waste_location_mapping`.`request_by_type` AS `request_by_type`, "
	            + "    COUNT(CASE WHEN `waste_location_mapping`.`status` = 'Open' THEN 1 END) AS `Open`, "
	            + "    COUNT(CASE WHEN `waste_location_mapping`.`status` = 'Close' THEN 1 END) AS `Close`, "
	            + "    COUNT(`waste_location_mapping`.`wlid`) AS `Total`, "
	            + "    SUM(`waste_location_mapping`.`tonage`) AS `Total_Tonage`, "
	            + "    SUM(CASE WHEN `waste_location_mapping`.`status` = 'Close' THEN wr.`tonage` ELSE 0 END) AS `Close_Tonage`, "
	            + "    SUM(`waste_location_mapping`.`tonage`) - "
	            + "    SUM(CASE WHEN `waste_location_mapping`.`status` = 'Close' THEN wr.`tonage` ELSE 0 END) AS `Ton_Difference`, "
	            + "    SUM(CASE "
	            + "        WHEN waste_location_mapping.`approved` = 'yes' THEN waste_location_mapping.`tonage` "
	            + "        ELSE 0 "
	            + "    END) AS `Total_Ton_Approved`, "
	            + "    SUM(CASE "
	            + "        WHEN waste_location_mapping.`approved` = 'no' THEN waste_location_mapping.`tonage` "
	            + "        ELSE 0 "
	            + "    END) AS `Total_Ton_Rejected`, "
	            + "    SUM(CASE "
	            + "        WHEN waste_location_mapping.`approved` = 'pending' THEN waste_location_mapping.`tonage` "
	            + "        ELSE 0 "
	            + "    END) AS `Total_Ton_Pending` "
	            + "FROM "
	            + "    `waste_location_mapping` "
	            + "JOIN "
	            + "    `erp_pgr`.`EG_USER` ON `waste_location_mapping`.cby = `EG_USER`.id "
	            + "LEFT JOIN "
	            + "    `waste_reply` wr ON `waste_location_mapping`.`wlid` = wr.`wlid` AND wr.`isactive` = 1 "
	            + "WHERE "
	            + "    `waste_location_mapping`.isactive = 1 "
	            + "    AND `waste_location_mapping`.`ward` = ? "
	            + "    AND `waste_location_mapping`.`request_by_type` IS NOT NULL "
	            + "    AND DATE(`waste_location_mapping`.cdate) BETWEEN ? AND ? "
	            + "GROUP BY "
	            + "    `waste_location_mapping`.`ward`, `waste_location_mapping`.`request_by_type` "
	            + "ORDER BY `waste_location_mapping`.`ward`";

	    // Fetch the data from the database
	    List<Map<String, Object>> result = jdbcWasteTemplate.queryForList(sql, ward, formattedFromDate, formattedToDate);

	    // Prepare the response
	    Map<String, Object> response = new HashMap<>();
	    response.put("filterDate", formattedFromDate + " to " + formattedToDate);
	    
	    boolean hasVendorData = false;
	    boolean hasOfficerData = false;
	    
	    // Separate Officer and Vendor data
	    List<Map<String, Object>> officerData = new ArrayList<>();
	    List<Map<String, Object>> vendorData = new ArrayList<>();
	    
	    for (Map<String, Object> row : result) {
	        String requestByType = (String) row.get("request_by_type");
	        
	        if ("Officer".equals(requestByType)) {
	            officerData.add(row);
	            hasOfficerData = true;
	        } else if ("Vendor".equals(requestByType)) {
	            vendorData.add(row);
	            hasVendorData = true;
	        }
	    }
	    
	    // If no Officer data, add a default entry with zero values
	    if (!hasOfficerData) {
	        Map<String, Object> officerRow = new HashMap<>();
	        officerRow.put("ward", "0");
	        officerRow.put("request_by_type", "Officer");
	        officerRow.put("Open", 0);
	        officerRow.put("Close", 0);
	        officerRow.put("Total", 0);
	        officerRow.put("Total_Tonage", 0);
	        officerRow.put("Close_Tonage", 0);
	        officerRow.put("Ton_Difference", 0);
	        officerRow.put("Total_Ton_Approved", 0);
	        officerRow.put("Total_Ton_Rejected", 0);
	        officerRow.put("Total_Ton_Pending", 0);

	        officerData.add(officerRow);
	    }
	    
	    // If no vendor data, add a default entry with zero values
	    if (!hasVendorData) {
	        Map<String, Object> vendorRow = new HashMap<>();
	        vendorRow.put("ward", "0");
	        vendorRow.put("request_by_type", "Vendor");
	        vendorRow.put("Open", 0);
	        vendorRow.put("Close", 0);
	        vendorRow.put("Total", 0);
	        vendorRow.put("Total_Tonage", 0);
	        vendorRow.put("Close_Tonage", 0);
	        vendorRow.put("Ton_Difference", 0);
	        vendorRow.put("Total_Ton_Approved", 0);
	        vendorRow.put("Total_Ton_Rejected", 0);
	        vendorRow.put("Total_Ton_Pending", 0);

	        vendorData.add(vendorRow);
	    }
	    
	    response.put("Officer", officerData);
	    response.put("Vendor", vendorData);
	    response.put("message", "Data List.");

	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getTicketDetailsByWLID(String wlid) {
	    
	    String sql = "SELECT  "
	            + "    wl.`wlid` AS comp_id,  "
	            + "    wl.`zone` AS comp_zone,  "
	            + "    wl.`ward` AS comp_ward,  "
	            + "    eg.`EXTRAFIELD2` AS comp_contact,  "
	            + "		CONCAT(COALESCE(eg.FIRST_NAME, ''), ' ', COALESCE(eg.MIDDLE_NAME, '')) AS comp_name, "
	            // + "    eg.`FIRST_NAME` AS comp_name,  "
	            + "    DATE_FORMAT(wl.`cdate`, '%d-%m-%Y %r') AS comp_date, "
	            + "    wl.`latitude` AS comp_latitude,  "
	            + "    wl.`longitude` AS comp_longitude,  "
	            + "    wl.`address` AS comp_area,  "
	            + "    wl.`tonage` AS appx_tonage,  "
	            + "    wt.`name` AS comp_type,  "
	            + "    CONCAT('"+fileBaseUrl+"/gccofficialapp/files', wl.`file`) AS image,  "
	            + "    wl.`status` AS comp_status,  "
	            + "    wr.`remarks` AS reply_remarks, "
	            + "    wr.`latitude` AS reply_latitude, "
	            + "    wr.`longitude` AS reply_longitude, "
	            + "    wr.`completed_date` AS reply_completed_date, "
	            + "    DATE_FORMAT(wr.`cdate`, '%d-%m-%Y %r') AS reply_in_date, "
	            + "    wr.`file` AS reply_file, "
	            + "    wr.`tonage` AS reply_tonage, "
	            + "    wr.`inby` AS reply_inby "
	            + "FROM  "
	            + "    `waste_location_mapping` wl "
	            + "JOIN  "
	            + "    `erp_pgr`.`EG_USER` eg "
	            + "    ON wl.cby = eg.id  "
	            + "LEFT JOIN  "
	            + "    `waste_reply` wr "
	            + "    ON wl.`wlid` = wr.`wlid` AND wr.`isactive`=1 "
	            + "LEFT JOIN "
	            + "    `waste_type` wt " 
	            + "    ON wl.type = wt.typeid "
	            + "WHERE  "
	            + "    wl.`isactive` = 1 "
	            + "AND wl.`wlid` = ?";

	    return jdbcWasteTemplate.queryForList(sql, wlid);
	}
}
