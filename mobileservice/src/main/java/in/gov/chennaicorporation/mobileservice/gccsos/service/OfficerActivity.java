package in.gov.chennaicorporation.mobileservice.gccsos.service;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service("gccofficialappOfficerActivity")
public class OfficerActivity {
	private JdbcTemplate jdbcSOSTemplate;
	private final Environment environment;
	private String fileBaseUrl;
	
	@Autowired
	public void setDataSource(@Qualifier("mysqlGccSOSDataSource") DataSource sosDataSource) {
		this.jdbcSOSTemplate = new JdbcTemplate(sosDataSource);
	}
	
	@Autowired
	public OfficerActivity(Environment environment) {
		this.environment = environment;
		this.fileBaseUrl=environment.getProperty("fileBaseUrl");
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

	public List<Map<String, Object>> dashboard(
			String loginId,
			String request_type
			){
		String requestQueryValue="";
				
		if ("Assistance".equals(request_type)) {
	    	requestQueryValue =  "'Medical','Food'"; // Assistance
	    } else if ("Rescue Operation".equals(request_type)) {
	    	requestQueryValue =  "'Rescue'"; // Rescue Operation
	    }
	    else if ("Emergency".equals(request_type)) {
	    	requestQueryValue =  "'CurrentInWater','FloodWater'"; // Rescue Operation
	    }
		
		String sqlQuery = "SELECT "
				+ "SUM(1) AS `Requested`, "
				+ "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS `Pending`, "
				+ "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS `Closed` "
				+ "FROM `rescue` "
				+ "WHERE `isactive`=1 ";
		
         	    if (request_type != null && !request_type.isEmpty() && !request_type.isBlank()) {
         	    	sqlQuery = sqlQuery + " AND request_type IN ("+requestQueryValue+")";
         	    }
		       	 	sqlQuery = sqlQuery + "";
		
		List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Request List");
        response.put("data", result);
	    
        return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> dashboardByType(
			String loginId,
			String request_type,
			String status
			){
		String requestQueryValue="";
		String statusQueryValue="";
				
		if ("Assistance".equals(request_type)) {
	    	requestQueryValue =  "'Medical','Food'"; // Assistance
	    } else if ("Rescue Operation".equals(request_type)) {
	    	requestQueryValue =  "'Rescue'"; // Rescue Operation
	    }
	    else if ("Emergency".equals(request_type)) {
	    	requestQueryValue =  "'CurrentInWater','FloodWater'"; // Rescue Operation
	    }
		
		if ("Request".equals(status)) {
			statusQueryValue =  "0,1"; // Assistance
	    } else if ("Pending".equals(status)) {
	    	statusQueryValue =  "0"; // Open
	    } else if ("Closed".equals(status)) {
	    	statusQueryValue =  "1"; // Closed
	    }
		
		String sqlQuery = "SELECT ";
		
		if ("Assistance".equals(request_type)) {
			sqlQuery = sqlQuery + "SUM(CASE WHEN `request_type` = 'Medical' THEN 1 ELSE 0 END) AS `Medical`, "
						+ "SUM(CASE WHEN `request_type` = 'Food' THEN 1 ELSE 0 END) AS `Food` ";
		}
		else if ("Rescue Operation".equals(request_type)) {
			sqlQuery =  sqlQuery + "SUM(CASE WHEN `request_type` = 'Rescue' THEN 1 ELSE 0 END) AS `Rescue` ";
	    }
		else if ("Emergency".equals(request_type)) {
			sqlQuery = sqlQuery + "SUM(CASE WHEN `request_type` = 'CurrentInWater' THEN 1 ELSE 0 END) AS `CurrentInWater`, "
					+ "SUM(CASE WHEN `request_type` = 'FloodWater' THEN 1 ELSE 0 END) AS `FloodWater` ";
	    }
				
		sqlQuery =  sqlQuery + "FROM `rescue` WHERE `isactive`=1 ";

 	    if (requestQueryValue != null && !requestQueryValue.isEmpty() && !requestQueryValue.isBlank()) {
 	    	sqlQuery = sqlQuery + " AND request_type IN ("+requestQueryValue+")";
 	    }
 	    
 	   if (statusQueryValue != null && !statusQueryValue.isEmpty() && !statusQueryValue.isBlank()) {
	    	sqlQuery = sqlQuery + " AND status IN ("+statusQueryValue+")";
	    }
       	 	sqlQuery = sqlQuery + "";
		
		List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Request List");
        response.put("data", result);
	    
        return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> dashboardByZone(
			String loginId,
			String request_type,
			String status,
			String zone,
			String ward
			){
		String selectVariable ="zone";
		String requestQueryValue="";
		String statusQueryValue="";
				
		if ("Medical".equals(request_type)) {
	    	requestQueryValue =  "'Medical'"; // Medical
	    } else if ("Food".equals(request_type)) {
	    	requestQueryValue =  "'Food'"; // Food
	    } else if ("Rescue".equals(request_type)) {
	    	requestQueryValue =  "'Rescue'"; // Rescue
	    }else if ("CurrentInWater".equals(request_type)) {
	    	requestQueryValue =  "'CurrentInWater'"; // Current In Water
	    }else if ("FloodWater".equals(request_type)) {
	    	requestQueryValue =  "'FloodWater'"; // Flood Water
	    }
		
		if ("Request".equals(status)) {
			statusQueryValue =  "0,1"; // Assistance
	    } else if ("Pending".equals(status)) {
	    	statusQueryValue =  "0"; // Open
	    } else if ("Closed".equals(status)) {
	    	statusQueryValue =  "1"; // Closed
	    }
		
		// Add additional filters if provided
 	    if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
 	    	selectVariable = selectVariable + ",ward";
 	    }
 	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
 	    	selectVariable = selectVariable + ",streetid,streetName";
 	    }
		
		String sqlQuery = "SELECT ";
		
		sqlQuery =  sqlQuery + selectVariable + ""
				+ " ,SUM(1) AS `Total` ";
		
		sqlQuery =  sqlQuery + "FROM `rescue` WHERE `isactive`=1 ";

 	    if (requestQueryValue != null && !requestQueryValue.isEmpty() && !requestQueryValue.isBlank()) {
 	    	sqlQuery = sqlQuery + " AND request_type IN ("+requestQueryValue+")";
 	    }
 	    
 	   if (statusQueryValue != null && !statusQueryValue.isEmpty() && !statusQueryValue.isBlank()) {
	    	sqlQuery = sqlQuery + " AND status IN ("+statusQueryValue+")";
	    }
 	   		
 	   sqlQuery = sqlQuery + "GROUP BY "+selectVariable+" ORDER BY zone ASC";
		
 	   System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Request List");
        response.put(request_type, result);
	    
        return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> dashboardByWard(
			String loginId,
			String request_type,
			String status,
			String zone,
			String ward
			){
		String selectVariable ="ward";
		String requestQueryValue="";
		String statusQueryValue="";
				
		if ("Medical".equals(request_type)) {
	    	requestQueryValue =  "'Medical'"; // Medical
	    } else if ("Food".equals(request_type)) {
	    	requestQueryValue =  "'Food'"; // Food
	    } else if ("Rescue".equals(request_type)) {
	    	requestQueryValue =  "'Rescue'"; // Rescue
	    }else if ("CurrentInWater".equals(request_type)) {
	    	requestQueryValue =  "'CurrentInWater'"; // Current In Water
	    }else if ("FloodWater".equals(request_type)) {
	    	requestQueryValue =  "'FloodWater'"; // Flood Water
	    }
		
		if ("Request".equals(status)) {
			statusQueryValue =  "0,1"; // Assistance
	    } else if ("Pending".equals(status)) {
	    	statusQueryValue =  "0"; // Open
	    } else if ("Closed".equals(status)) {
	    	statusQueryValue =  "1"; // Closed
	    }
		
		
 	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
 	    	selectVariable = selectVariable + ",streetid,streetName";
 	    }
		
		String sqlQuery = "SELECT ";
		
		sqlQuery =  sqlQuery + selectVariable + ""
				+ " ,SUM(1) AS `Total` ";
		
		sqlQuery =  sqlQuery + "FROM `rescue` WHERE `isactive`=1 ";

 	    if (requestQueryValue != null && !requestQueryValue.isEmpty() && !requestQueryValue.isBlank()) {
 	    	sqlQuery = sqlQuery + " AND request_type IN ("+requestQueryValue+")";
 	    }
 	    
 	   if (statusQueryValue != null && !statusQueryValue.isEmpty() && !statusQueryValue.isBlank()) {
	    	sqlQuery = sqlQuery + " AND status IN ("+statusQueryValue+")";
	    }
 	   		
 	   sqlQuery = sqlQuery + "GROUP BY "+selectVariable+" ORDER BY `ward` ASC";
		
 	   System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Request List");
        response.put("zone", result);
	    
        return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> dashboardByList(
			String loginId,
			String request_type,
			String status,
			String zone,
			String ward
			){
		String selectVariable ="ward";
		String requestQueryValue="";
		String statusQueryValue="";
				
		if ("Medical".equals(request_type)) {
	    	requestQueryValue =  "'Medical'"; // Medical
	    } else if ("Food".equals(request_type)) {
	    	requestQueryValue =  "'Food'"; // Food
	    } else if ("Rescue".equals(request_type)) {
	    	requestQueryValue =  "'Rescue'"; // Rescue
	    }else if ("CurrentInWater".equals(request_type)) {
	    	requestQueryValue =  "'CurrentInWater'"; // Current In Water
	    }else if ("FloodWater".equals(request_type)) {
	    	requestQueryValue =  "'FloodWater'"; // Flood Water
	    }
		
		if ("Request".equals(status)) {
			statusQueryValue =  "0,1"; // Assistance
	    } else if ("Pending".equals(status)) {
	    	statusQueryValue =  "0"; // Open
	    } else if ("Closed".equals(status)) {
	    	statusQueryValue =  "1"; // Closed
	    }
		
		String sqlQuery = "SELECT * FROM `rescue` WHERE `isactive`=1 ";

 	    if (requestQueryValue != null && !requestQueryValue.isEmpty() && !requestQueryValue.isBlank()) {
 	    	sqlQuery = sqlQuery + " AND request_type IN ("+requestQueryValue+")";
 	    }
 	    
 	   if (statusQueryValue != null && !statusQueryValue.isEmpty() && !statusQueryValue.isBlank()) {
	    	sqlQuery = sqlQuery + " AND status IN ("+statusQueryValue+") ";
	    }
 	   		
 	   sqlQuery = sqlQuery + "ORDER BY cdate ASC";
		
 	   System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Request List");
        response.put("ward", result);
	    
        return Collections.singletonList(response);
	}
	
	@Transactional
 	public List<Map<String, Object>> getAllRequest(
			String request_type, 
			String zone,
			String ward,
			String streetid,
			String fromdate,
			String todate,
			String loginId,
			String status){
		
		String statusidQuery ="";
	
		System.out.println("Status: "+status);
		// Use equals() for string comparison
	    if ("Pending".equals(status)) {
	    	statusidQuery =  " AND `status` = 0 "; // Open
	    } else if ("Closed".equals(status)) {
	    	statusidQuery =  " AND `status` = 1 "; // Closed
	    }
	    
		String sqlQuery = "SELECT *,CASE"
				+ "	WHEN status = 0 THEN 'Pending' "
				+ "	WHEN status = 1 THEN 'Closed' "
				+ "    ELSE 'unknown'"
				+ "    END AS status_name "
				+ " FROM `rescue` WHERE `isactive`=1 ";
		// Add additional filters if provided
 	    if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
 	        sqlQuery = sqlQuery + " AND zone = '"+zone+"' ";
 	    }
 	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
 	    	sqlQuery = sqlQuery + " AND ward = '"+ward+"' ";
 	    }
 	    if (streetid != null && !streetid.isEmpty() && !streetid.isBlank()) {
 	    	sqlQuery = sqlQuery + " AND streetid = '"+streetid+"'";
 	    }
 	    if (request_type != null && !request_type.isEmpty() && !request_type.isBlank()) {
 	    	sqlQuery = sqlQuery + " AND request_type = '"+request_type+"' ";
 	    }
 	    if (todate == null || todate.isEmpty() || todate.isBlank()) {
 		    todate = fromdate;
 		}
	 	if (fromdate != null && !fromdate.isEmpty() && !fromdate.isBlank()) {
	 		
	 		sqlQuery = sqlQuery + " "
	 				+ "AND `cdate` BETWEEN STR_TO_DATE('"+fromdate+" 00:00:00', '%d-%m-%Y %H:%i:%s') "
	 				+ "AND STR_TO_DATE('"+todate+" 23:59:59', '%d-%m-%Y %H:%i:%s')";
		}
 	   
			sqlQuery = sqlQuery + statusidQuery + "ORDER BY `id` DESC";
			System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(sqlQuery);
		
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Request List");
        response.put("data", result);
	    
        return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> reportByDate(
			String fromdate, 
			String todate, 
			String request_type, 
			String zone, 
			String ward, 
			String streetid,
			String loginId,
			String status) {
		
		String selectVariable ="zone";
		String statusidQuery ="";
		/*
		System.out.println("Status: "+status);
		// Use equals() for string comparison
	    if ("Open".equals(status)) {
	    	statusidQuery =  " AND `status` = 0 "; // Open
	    } else if ("Closed".equals(status)) {
	    	statusidQuery =  " AND `status` = 1 "; // Closed
	    }
	    */
		// Add additional filters if provided
 	    if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
 	    	selectVariable = selectVariable + ",ward";
 	    }
 	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
 	    	selectVariable = selectVariable + ",streetid,streetName";
 	    }
 	    
		String sqlQuery = "SELECT "
				+ selectVariable +","
				+ "SUM(1) AS `Requested`, "
				+ "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS `Pending`, "
				+ "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS `Closed` "
				+ "FROM `rescue` "
				+ "WHERE `isactive`=1 ";
				
	                 // Add additional filters if provided
	         	    if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
	         	        sqlQuery = sqlQuery + " AND zone = '"+zone+"' ";
	         	    }
	         	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
	         	    	sqlQuery = sqlQuery + " AND ward = '"+ward+"' ";
	         	    }
	         	    if (streetid != null && !streetid.isEmpty() && !streetid.isBlank()) {
	         	    	sqlQuery = sqlQuery + " AND streetid = '"+streetid+"'";
	         	    }
	         	    if (request_type != null && !request_type.isEmpty() && !request_type.isBlank()) {
	         	    	sqlQuery = sqlQuery + " AND request_type = '"+request_type+"' ";
	         	    }
	         	    if (todate == null || todate.isEmpty() || todate.isBlank()) {
	         		   todate = fromdate;
		       		}
		       	 	if (fromdate != null && !fromdate.isEmpty() && !fromdate.isBlank()) {
		       	 		
		       	 		sqlQuery = sqlQuery + statusidQuery +" "
		       	 				+ "AND `cdate` BETWEEN STR_TO_DATE('"+fromdate+" 00:00:00', '%d-%m-%Y %H:%i:%s') "
		       	 				+ "AND STR_TO_DATE('"+todate+" 23:59:59', '%d-%m-%Y %H:%i:%s') ";
		       		}
	         
		       	 	sqlQuery = sqlQuery + "GROUP BY "+selectVariable+" ORDER BY zone ASC";
	    System.out.println(sqlQuery);
	    List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(sqlQuery);
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Request List");
        response.put("data", result);
	    
        return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveRequestUpdate(
			String rescue_id,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String streetid,
			String streetname,
			String remarks,
			String loginId,
			MultipartFile file
			) {
		List<Map<String, Object>> result=null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		// Set the file path where you want to save it
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("sos_activity_foldername");
        var year = DateTimeUtil.getCurrentYear();
        var month = DateTimeUtil.getCurrentMonth();
        var day = DateTimeUtil.getCurrentDay();
        
        uploadDirectory = uploadDirectory + serviceFolderName + year +"/"+month+"/"+day+"/";
        
		try {
	        // Create directory if it doesn't exist
	        Path directoryPath = Paths.get(uploadDirectory);
	        if (!Files.exists(directoryPath)) {
	            Files.createDirectories(directoryPath);
	        }
	        
	        // Datetime string
	        String datetimetxt = DateTimeUtil.getCurrentDateTime();
	        // File name 
            String OrgfileName = rescue_id +"_"+ datetimetxt + "_" + file.getOriginalFilename();
            String fileName = OrgfileName.replaceAll("\\s+", ""); // Remove space on filename
            
	        String filePath = uploadDirectory + "/" + fileName;
	        
	        String filepath_txt = "/"+serviceFolderName + year +"/"+month+"/"+day+"/"+fileName;
	        		
            // Create a new Path object
            Path path = Paths.get(filePath);
            
            // Get the bytes of the file
            byte[] bytes = file.getBytes();

            // Compress the image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50% quality
            
            // Write the bytes to the file
            Files.write(path, bytes);
            
            //File Size
            Long fileSize = file.getSize();
            
            // File Type
            String fileType = file.getContentType();
            
            String sqlQuery = "INSERT INTO `rescue_officer_update`(`rescue_id`, "
            		+ "`activity_photo`, `filepath`, `filetype`, `filesize`, `remarks`, "
            		+ "`latitude`, `longitude`, `zone`, `ward`, `streetid`, `streetname`, `officer_id`) "
    				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
    		
    		KeyHolder keyHolder = new GeneratedKeyHolder();
    		
    		try {
                int affectedRows = jdbcSOSTemplate.update(new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                        PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                        ps.setString(1, rescue_id);
                        ps.setString(2, fileName);
                        ps.setString(3, filepath_txt);
                        ps.setString(4, fileType);
                        ps.setLong(5, fileSize);
                        ps.setString(6, remarks);
	                    ps.setString(7, latitude);
	                    ps.setString(8, longitude);
	                    ps.setString(9, zone);
	                    ps.setString(10, ward);
	                    ps.setString(11, streetid);
	                    ps.setString(12, streetname);
	                    ps.setString(13, loginId);
                        return ps;
                    }
                }, keyHolder);
                
                if (affectedRows > 0) {
                    Number generatedId = keyHolder.getKey();
                    lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                    response.put("insertId", lastInsertId);
                    response.put("status", "success");
                    response.put("message", "A rescue officer update was inserted successfully!");
                    System.out.println("A rescue officer update was inserted successfully! rescue Update ID: " + generatedId);
                    
                    if(!rescue_id.isBlank() && !rescue_id.isEmpty()) {
                        // Pending 
                    	String sqlQuery_u = "UPDATE `rescue` SET `status`=1 WHERE `id`='"+rescue_id+"'";
                    	jdbcSOSTemplate.update(sqlQuery_u);
                    }
                    
                    
                } else {
                    response.put("status", "error");
                    response.put("message", "Failed to insert rescue officer update.");
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
            
		} catch (IOException e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Image Upload failed (officer update)!");
        }
		return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> getRequestDataById(
			String rescue_id){
		String sqlQuery = "SELECT *,CASE"
				+ "	WHEN status = 0 THEN 'Pending' "
				+ "	WHEN status = 1 THEN 'Closed' "
				+ "    ELSE 'unknown'"
				+ "    END AS status_name "
				+ " FROM `rescue` WHERE `id`='"+rescue_id+"'";
		List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
	    if (result.isEmpty()) {
	    	return result;
	    }else {
	    
			String sqlQuery2 = "SELECT *,CONCAT('"+fileBaseUrl+"/gccofficialapp/files', filepath) AS imageUrl "
					+ "FROM `rescue_officer_update` WHERE rescue_id='"+rescue_id+"'";
		    List<Map<String, Object>> result2 = jdbcSOSTemplate.queryForList(sqlQuery2);
		   System.out.println(sqlQuery2);
		    // Add the result2 and result3 to the main result map
		    result.get(0).put("officer_activity_data", result2);
	    }
	    response.put("status", "success");
        response.put("message", "Request Information");
        response.put("Data", result);
        return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> getFloodGrandData(
			String fromDate,
			String toDate){
		String sqlQuery = "SELECT IFNULL(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) AS Pending, "
				+ "IFNULL(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS Completed, "
				+ "IFNULL(COUNT(id), 0) AS Register FROM rescue "
				+ "WHERE DATE(cdate) BETWEEN STR_TO_DATE('"+fromDate+"', '%d-%m-%Y') AND STR_TO_DATE('"+toDate+"', '%d-%m-%Y')";
		List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "success");
        response.put("message", "Request Information");
        response.put("Data", result);
        return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> getFloodAbsData(String fromDate, String toDate) {
	    String sqlQuery = "SELECT IFNULL(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) AS Pending, "
	            + "IFNULL(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS Completed, "
	            + "IFNULL(COUNT(id), 0) AS Requested "
	            + "FROM rescue "
	            + "WHERE DATE(cdate) BETWEEN STR_TO_DATE('" + fromDate + "', '%d-%m-%Y') AND STR_TO_DATE('" + toDate + "', '%d-%m-%Y')";
	    List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(sqlQuery);

	    if (result.isEmpty()) {
	        return result;
	    }

	    Map<String, Object> response = new HashMap<>();
	    Map<String, Object> dataMap = new HashMap<>();
	    dataMap.putAll(result.get(0));

	    // Categories: Medical, Food, Rescue
	    String[] categories = {"Medical", "Food", "Rescue"};
	    String[] modes = {"1913", "whatsapp", "nca"};

	    for (String category : categories) {
	        Map<String, List<Map<String, Object>>> categoryData = new HashMap<>();
	        int categoryTotalPending = 0;
	        int categoryTotalCompleted = 0;
	        int categoryTotalRequested = 0;
	        
	        for (String mode : modes) {
	            String sqlCategoryQuery = "SELECT IFNULL(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) AS Pending, "
	                    + "IFNULL(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS Completed, "
	                    + "IFNULL(COUNT(id), 0) AS Requested "
	                    + "FROM rescue WHERE `modeofcomplient` = '" + mode + "' AND `request_type` = '" + category + "' "
	                    + "AND DATE(cdate) BETWEEN STR_TO_DATE('" + fromDate + "', '%d-%m-%Y') AND STR_TO_DATE('" + toDate + "', '%d-%m-%Y')";
	            List<Map<String, Object>> modeResult = jdbcSOSTemplate.queryForList(sqlCategoryQuery);

	            //categoryData.put(mode, modeResult);
	            if (!modeResult.isEmpty()) {
	                Map<String, Object> modeData = modeResult.get(0);
	                categoryData.put(mode, Collections.singletonList(modeData));

	                // Update totals for the category
	                categoryTotalPending += ((Number) modeData.get("Pending")).intValue();
	                categoryTotalCompleted += ((Number) modeData.get("Completed")).intValue();
	                categoryTotalRequested += ((Number) modeData.get("Requested")).intValue();
	            }else {
	                // If there's no data for the current mode, add a default entry
	                Map<String, Object> defaultModeData = new HashMap<>();
	                defaultModeData.put("Pending", 0);
	                defaultModeData.put("Completed", 0);
	                defaultModeData.put("Requested", 0);
	                categoryData.put(mode, Collections.singletonList(defaultModeData));
	            }
	        }
	        // Add the total data for the current category
	        Map<String, Object> totalData = new HashMap<>();
	        totalData.put("Pending", categoryTotalPending);
	        totalData.put("Completed", categoryTotalCompleted);
	        totalData.put("Requested", categoryTotalRequested);
	        categoryData.put("Total", Collections.singletonList(totalData));
	        dataMap.put(category, categoryData);
	    }

	    response.put("status", "success");
	    response.put("message", "Request Information");
	    response.put("fromDate", fromDate);
	    response.put("toDate", toDate);
	    response.put("Data", Collections.singletonList(dataMap));

	    return Collections.singletonList(response);
	}
	/*
	@Transactional
	public List<Map<String, Object>> getFloodZoneData(
	        String fromDate,
	        String toDate,
	        String categorie,
	        String mode) {
	    String sqlQuery = "SELECT zone, "
	            + "IFNULL(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) AS Pending, "
	            + "IFNULL(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS Completed, "
	            + "IFNULL(COUNT(id), 0) AS Register "
	            + "FROM rescue "
	            + "WHERE `request_type` = ? "
	            + "AND `modeofcomplient` = ? "
	            + "AND DATE(cdate) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
	            + "GROUP BY zone";
	    
	    List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(
	            sqlQuery, new Object[]{categorie, mode, fromDate, toDate});
	    
	    Map<String, Object> response = new HashMap<>();
	    
	    if (result.isEmpty()) {
	        response.put("Data", Collections.emptyList());
	    } else {
	        response.put("Data", result);
	    }
	    response.put("status", "success");
	    response.put("message", "Request Information");
	    response.put("fromDate", fromDate);
	    response.put("toDate", toDate);
	    response.put("categorie", categorie);
	    response.put("mode", mode);
	    
	    return Collections.singletonList(response);
	}
	*/
	@Transactional
	public List<Map<String, Object>> getFloodZoneData(
	        String fromDate,
	        String toDate,
	        String categorie,
	        String mode,
	        String status) {
		String whereStatus="";
		String whereMode="";
		// Check if the status is provided and is either "0" or "1"
	    if ("0".equals(status) || "1".equals(status)) {
	        whereStatus = "AND `status` = '"+status+"' ";
	    }
	    
	    if ("Total".equals(mode) || "Total".equals(mode)) {
	    	whereMode = "AND `modeofcomplient` <> ? ";
	    }
	    else {
	    	whereMode = "AND `modeofcomplient` = ? ";
	    }
	    
	    String sqlQuery = "SELECT zone, "
	            + "IFNULL(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) AS Pending, "
	            + "IFNULL(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS Completed, "
	            + "IFNULL(COUNT(id), 0) AS Register "
	            + "FROM rescue "
	            + "WHERE `request_type` = ? "
	            + whereMode
	            + "AND DATE(cdate) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
	            + whereStatus
	            + "GROUP BY zone";
	    
	    List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(
	            sqlQuery, new Object[]{categorie, mode, fromDate, toDate});
	    
	    Map<String, Object> response = new HashMap<>();
	    int totalPending = 0;
	    int totalCompleted = 0;
	    int totalRegister = 0;

	    if (!result.isEmpty()) {
	        for (Map<String, Object> zoneData : result) {
	            totalPending += ((Number) zoneData.get("Pending")).intValue();
	            totalCompleted += ((Number) zoneData.get("Completed")).intValue();
	            totalRegister += ((Number) zoneData.get("Register")).intValue();
	        }
	    }

	    // Add totals
	    Map<String, Object> totalData = new HashMap<>();
	    totalData.put("Pending", totalPending);
	    totalData.put("Completed", totalCompleted);
	    totalData.put("Register", totalRegister);

	    response.put("Data", result);
	    response.put("Total", totalData);
	    response.put("status", "success");
	    response.put("message", "Request Information");
	    response.put("fromDate", fromDate);
	    response.put("toDate", toDate);
	    response.put("categorie", categorie);
	    response.put("mode", mode);

	    return Collections.singletonList(response);
	}
	
	/*
	@Transactional
	public List<Map<String, Object>> getFloodWardData(
	        String fromDate,
	        String toDate,
	        String categorie,
	        String mode,
	        String zone) {
	    String sqlQuery = "SELECT ward, "
	            + "IFNULL(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) AS Pending, "
	            + "IFNULL(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS Completed, "
	            + "IFNULL(COUNT(id), 0) AS Register "
	            + "FROM rescue "
	            + "WHERE `request_type` = ? "
	            + "AND `modeofcomplient` = ? "
	            + "AND `zone` = ? "
	            + "AND DATE(cdate) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
	            + "GROUP BY ward";
	    
	    List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(
	            sqlQuery, new Object[]{categorie, mode, zone, fromDate, toDate});
	    
	    Map<String, Object> response = new HashMap<>();
	    
	    if (result.isEmpty()) {
	        response.put("Data", Collections.emptyList());
	    } else {
	        response.put("Data", result);
	    }
	    response.put("status", "success");
	    response.put("message", "Request Information");
	    response.put("fromDate", fromDate);
	    response.put("toDate", toDate);
	    response.put("categorie", categorie);
	    response.put("mode", mode);
	    response.put("zone", zone);
	    
	    return Collections.singletonList(response);
	}*/
	
	@Transactional
	public List<Map<String, Object>> getFloodWardData(
	        String fromDate,
	        String toDate,
	        String categorie,
	        String mode,
	        String zone,
	        String status) {
		String whereStatus="";
		String whereMode="";
		
		// Check if the status is provided and is either "0" or "1"
	    if ("0".equals(status) || "1".equals(status)) {
	    	whereStatus = "AND `status` = '"+status+"' ";
	    }

	    if ("Total".equals(mode) || "Total".equals(mode)) {
	    	whereMode = "AND `modeofcomplient` <> ? ";
	    }
	    else {
	    	whereMode = "AND `modeofcomplient` = ? ";
	    }
	    String sqlQuery = "SELECT ward, "
	            + "IFNULL(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) AS Pending, "
	            + "IFNULL(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS Completed, "
	            + "IFNULL(COUNT(id), 0) AS Register "
	            + "FROM rescue "
	            + "WHERE `request_type` = ? "
	            + whereMode
	            + "AND `zone` = ? "
	            + "AND DATE(cdate) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') "
	            + whereStatus
	            + "GROUP BY ward";
	    
	    List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(
	            sqlQuery, new Object[]{categorie, mode, zone, fromDate, toDate});
	    
	    Map<String, Object> response = new HashMap<>();
	    int totalPending = 0;
	    int totalCompleted = 0;
	    int totalRegister = 0;

	    if (!result.isEmpty()) {
	        for (Map<String, Object> wardData : result) {
	            totalPending += ((Number) wardData.get("Pending")).intValue();
	            totalCompleted += ((Number) wardData.get("Completed")).intValue();
	            totalRegister += ((Number) wardData.get("Register")).intValue();
	        }
	    }

	    // Add totals
	    Map<String, Object> totalData = new HashMap<>();
	    totalData.put("Pending", totalPending);
	    totalData.put("Completed", totalCompleted);
	    totalData.put("Register", totalRegister);

	    response.put("Data", result);
	    response.put("Total", totalData);
	    response.put("status", "success");
	    response.put("message", "Request Information");
	    response.put("fromDate", fromDate);
	    response.put("toDate", toDate);
	    response.put("categorie", categorie);
	    response.put("mode", mode);
	    response.put("zone", zone);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> getFloodData(
	        String fromDate,
	        String toDate,
	        String categorie,
	        String mode,
	        String zone,
			String ward,
	        String status) {
		String whereStatus="";
		String whereMode="";
		
		// Check if the status is provided and is either "0" or "1"
	    if ("0".equals(status) || "1".equals(status)) {
	    	whereStatus = "AND `status` = '"+status+"' ";
	    }

	    if ("Total".equals(mode) || "Total".equals(mode)) {
	    	whereMode = "AND `modeofcomplient` <> ? ";
	    }
	    else {
	    	whereMode = "AND `modeofcomplient` = ? ";
	    }
	    String sqlQuery = "SELECT * "
	            + "FROM rescue "
	            + "WHERE `request_type` = ? "
	            + whereMode
	            + "AND `zone` = ? "
	            + "AND `ward` = ? "
	            + whereStatus
	            + "AND DATE(cdate) BETWEEN STR_TO_DATE(?, '%d-%m-%Y') AND STR_TO_DATE(?, '%d-%m-%Y') ";
	    
	    List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(
	            sqlQuery, new Object[]{categorie, mode, zone, ward ,fromDate, toDate});
	    
	    Map<String, Object> response = new HashMap<>();
	    
	    if (result.isEmpty()) {
	        response.put("Data", Collections.emptyList());
	    } else {
	        response.put("Data", result);
	    }
	    response.put("status", "success");
	    response.put("message", "Request Information");
	    response.put("fromDate", fromDate);
	    response.put("toDate", toDate);
	    response.put("categorie", categorie);
	    response.put("mode", mode);
	    response.put("zone", zone);
	    response.put("ward", ward);
	    
	    return Collections.singletonList(response);
	}
	
}
