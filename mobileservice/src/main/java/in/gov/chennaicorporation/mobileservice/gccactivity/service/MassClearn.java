package in.gov.chennaicorporation.mobileservice.gccactivity.service;

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
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MassClearn {
	private JdbcTemplate jdbcActivityTemplate;
	private final Environment environment;
	private String fileBaseUrl;
	
	@Autowired
	public void setDataSource(@Qualifier("mysqlActivityDataSource") DataSource activityDataSource) {
		this.jdbcActivityTemplate = new JdbcTemplate(activityDataSource);
	}
	
	@Autowired
	public MassClearn(Environment environment) {
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
	
	@Transactional
	public List<Map<String, Object>> getBrrRoadType(String loginId){
		String sqlQuery = "SELECT * FROM `brr_road_type`";
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery);
		
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getAllTaskList(String loginId){
		String sqlQuery = "SELECT * FROM `brr_task` WHERE (`isactive`=1 AND `isdelete`=0) ORDER BY `id` DESC";
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery);
		
		return result;
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
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery, zone);
		
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getTaskListByWard(
			String ward,
			String loginId){
		String sqlQuery = "SELECT *,CASE "
						+ "        WHEN status = 1 THEN 'Open'"
						+ "        WHEN status = 2 THEN 'In-Progress'"
						+ "        WHEN status = 3 THEN 'Pending'"
						+ "        WHEN status = 4 THEN 'Completed'"
						+ "        ELSE 'unknown'"
						+ "    END AS status_name "
						+ "FROM `brr_task` WHERE `ward`=? AND (`isactive`=1 AND `isdelete`=0) ORDER BY `id` DESC";
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery, ward);
		
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getAllTaskListByStreetId(
			String streetId,
			String loginId){
		String sqlQuery = "SELECT *,CASE "
						+ "        WHEN status = 1 THEN 'Open'"
						+ "        WHEN status = 2 THEN 'In-Progress'"
						+ "        WHEN status = 3 THEN 'Pending'"
						+ "        WHEN status = 4 THEN 'Completed'"
						+ "        ELSE 'unknown'"
						+ "    END AS status_name "
						+ "FROM `brr_task` WHERE `streeId`=? AND (`isactive`=1 AND `isdelete`=0) ORDER BY `id` DESC";
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery, streetId);
		
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getTaskDatesInMonth(
			String month, 
			String year,
			String loginId) {
	    String sqlQuery = "WITH RECURSIVE date_sequence AS ("
	                    + "    SELECT DATE(CONCAT(?, '-', ?, '-01')) AS taskdate "
	                    + "    UNION ALL "
	                    + "    SELECT DATE_ADD(taskdate, INTERVAL 1 DAY) "
	                    + "    FROM date_sequence "
	                    + "    WHERE taskdate < LAST_DAY(CONCAT(?, '-', ?, '-01')) "
	                    + ") "
	                    + "SELECT "
	                    + "    ds.taskdate, "
	                    + "    bt.id AS taskid "
	                    + "FROM "
	                    + "    date_sequence ds "
	                    + "JOIN "
	                    + "    brr_task bt "
	                    + "ON ds.taskdate BETWEEN bt.fromdate AND bt.todate "
	                    + "AND bt.isactive = 1 AND bt.isdelete = 0 "
	                    + "ORDER BY ds.taskdate ASC, bt.id ASC";

	    List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery, year, month, year, month);
	    return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getAllTaskListByMonth(
			String month, 
			String year,
			String loginId) {
	    String sqlQuery = "SELECT *, CASE "
			    		+ "        WHEN status = 1 THEN 'Open'"
						+ "        WHEN status = 2 THEN 'In-Progress'"
						+ "        WHEN status = 3 THEN 'Pending'"
						+ "        WHEN status = 4 THEN 'Completed'"
	                    + "        ELSE 'unknown' "
	                    + "    END AS status_name "
	                    + "FROM `brr_task` "
	                    + "WHERE (MONTH(`fromdate`) = ? AND YEAR(`fromdate`) = ?) "
	                    + "OR (MONTH(`todate`) = ? AND YEAR(`todate`) = ?) "
	                    + "AND `isactive` = 1 AND `isdelete` = 0 "
	                    + "ORDER BY `id` DESC";

	    List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery, month, year, month, year);
	    return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getAllTaskListByDate(
			String workdate,
			String loginId) {
	    String sqlQuery = "SELECT *, CASE "
			    		+ "        WHEN status = 1 THEN 'Open'"
						+ "        WHEN status = 2 THEN 'In-Progress'"
						+ "        WHEN status = 3 THEN 'Pending'"
						+ "        WHEN status = 4 THEN 'Completed'"
	                    + "        ELSE 'unknown' "
	                    + "    END AS status_name "
	                    + "FROM `brr_task` "
	                    + "WHERE (STR_TO_DATE(?, '%d-%m-%Y') BETWEEN `fromdate` AND `todate`) "
	                    + "AND `isactive` = 1 AND `isdelete` = 0 "
	                    + "ORDER BY `id` DESC";
	    System.out.println(sqlQuery);
	    List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery, workdate);
	    return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getTaskInfoBytaskId(
			String taskId,
			String loginId){
		String sqlQuery = "SELECT *,CASE "
						+ "        WHEN status = 1 THEN 'Open'"
						+ "        WHEN status = 2 THEN 'In-Progress'"
						+ "        WHEN status = 3 THEN 'Pending'"
						+ "        WHEN status = 4 THEN 'Completed'"
						+ "        ELSE 'unknown'"
						+ "    END AS status_name "
						+ "FROM `brr_task` WHERE `id`=? AND (`isactive`=1 AND `isdelete`=0) ORDER BY `id` DESC";
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery, taskId);
		
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getTaskHistoryBytaskId(
			String taskId,
			String loginId){
		String sqlQuery = "SELECT *,CASE "
						+ "        WHEN status = 1 THEN 'Open'"
						+ "        WHEN status = 2 THEN 'In-Progress'"
						+ "        WHEN status = 3 THEN 'Pending'"
						+ "        WHEN status = 4 THEN 'Completed'"
						+ "        ELSE 'unknown'"
						+ "    END AS status_name "
						+ "FROM `brr_task` WHERE `id`=? AND (`isactive`=1 AND `isdelete`=0) ORDER BY `id` DESC";
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery, taskId);
		
	    if (result.isEmpty()) {
	    	return result;
	    }else {
	    
			String sqlQuery3 = "SELECT *,CONCAT('"+fileBaseUrl+"/gccofficialapp/files', filepath) AS imageUrl "
					+ "FROM `brr_task_activity_log` WHERE taskid=? ORDER BY `id` DESC";
		    List<Map<String, Object>> result2 = jdbcActivityTemplate.queryForList(sqlQuery3, taskId);
		   
		    // Add the result2 and result3 to the main result map
		    result.get(0).put("task_activity_data", result2);
	    }
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> saveTask(
			String taskname,
			String roadtype,
			String fromdate,
			String todate,
			String activitytype,
			String zone,
			String ward,
			String streeId,
			String streetName,
			String taskremarks,
			String loginId,
			String streetlength) {
		List<Map<String, Object>> result=null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		String sqlQuery = "INSERT INTO `brr_task`(`taskname`, `roadtype`, `fromdate`, `todate`, `activitytype`, `zone`, `ward`, `streetid`, `streetname`, `taskremarks`, `cby`,`streetlength`) "
				+ "VALUES (?,?,STR_TO_DATE(?, '%d-%m-%Y'),STR_TO_DATE(?, '%d-%m-%Y'),?,?,?,?,?,?,?,?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
            int affectedRows = jdbcActivityTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, taskname);
                    ps.setString(2, roadtype);
                    ps.setString(3, fromdate);
                    ps.setString(4, todate);
                    ps.setString(5, activitytype);
                    ps.setString(6, zone);
                    ps.setString(7, ward);
                    ps.setString(8, streeId);
                    ps.setString(9, streetName);
                    ps.setString(10, taskremarks);
                    ps.setString(11, loginId);
                    ps.setString(12, streetlength);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "A new task was inserted successfully!");
                System.out.println("A new task was inserted successfully! Insert ID: " + generatedId);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to insert a new task.");
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
	public List<Map<String, Object>> DeleteTask(String taskId, String loginId){
		
		String sqlQuery = "SELECT * FROM brr_task WHERE `cby`='"+loginId+"' and `id`='"+taskId+"'";
		System.out.println(sqlQuery);
		List<Map<String, Object>> results = jdbcActivityTemplate.queryForList(sqlQuery);

		if (!results.isEmpty()) {
			sqlQuery = "UPDATE `brr_task` SET isdelete=1 WHERE `id`=?";
			jdbcActivityTemplate.update(sqlQuery,taskId);
			
			sqlQuery = "INSERT INTO `brr_task_log`(`type`, `taskid`, `cby`) "
					+ "VALUES ('Delete',?,?)";
			jdbcActivityTemplate.update(sqlQuery,taskId,loginId);
			Map<String, Object> response = new HashMap<>();
			response.put("Activity", "Delete");
	        response.put("status", "success");
	        response.put("message", "Task Delete successfully!");
	        System.out.println("Task was delete successfully! Task ID: " + taskId + "Delete Request By: "+loginId);
	        return Collections.singletonList(response);
		} else {
		    // Handle the case when there are no results
			Map<String, Object> response = new HashMap<>();
			response.put("Activity", "Delete");
	        response.put("status", "Failed");
	        response.put("message", "You don’t have access to delete this task!");
	        System.out.println("Task was delete failed! Task ID: " + taskId + "Delete Request By: "+loginId);
	        return Collections.singletonList(response);
		}
	}
	
	@Transactional
	public List<Map<String, Object>> saveTaskActivity(
			String taskid,
			String before_after,
			String activitystatus,
			String activity_id,
			String remarks,
			String loginId,
			MultipartFile file) {
		List<Map<String, Object>> result=null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		// Set the file path where you want to save it
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("brr_activity_foldername");
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
            String OrgfileName = before_after +"_"+ datetimetxt + "_" + file.getOriginalFilename();
            String fileName = OrgfileName.replaceAll("\\s+", ""); // Remove space on filename
            
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
            Files.write(path, bytes);
            
            //File Size
            Long fileSize = file.getSize();
            
            // File Type
            String fileType = file.getContentType();
            
            String sqlQuery = "INSERT INTO `brr_task_activity_log`(`taskid`, `activity_photo`, `activitystatus`, `remarks`, `before_after`, `cby`, `filetype`, `filesize`, `filepath`, `activity_id`) "
    				+ "VALUES (?,?,?,?,?,?,?,?,?,?)";
    		
    		KeyHolder keyHolder = new GeneratedKeyHolder();
    		
    		try {
                int affectedRows = jdbcActivityTemplate.update(new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                        PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                        ps.setString(1, taskid);
                        ps.setString(2, fileName);
                        ps.setString(3, activitystatus);
                        ps.setString(4, remarks);
                        ps.setString(5, before_after);
                        ps.setString(6, loginId);
	                    ps.setString(7, fileType);
	                    ps.setLong(8, fileSize);
	                    ps.setString(9, filepath_txt);
	                    ps.setString(10, activity_id);
                        return ps;
                    }
                }, keyHolder);
                
                if (affectedRows > 0) {
                    Number generatedId = keyHolder.getKey();
                    lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                    response.put("insertId", lastInsertId);
                    response.put("status", "success");
                    response.put("message", "A new task was inserted successfully!");
                    System.out.println("A new task was inserted successfully! Insert ID: " + generatedId);
                    
                    if(!activitystatus.isBlank() && !activitystatus.isEmpty() && activitystatus.equalsIgnoreCase("yes") && before_after.equalsIgnoreCase("after")) {
                        // Pending 
                    	String sqlQuery_u = "UPDATE `brr_task` SET `status`=4 WHERE `id`='"+taskid+"'";
                    	jdbcActivityTemplate.update(sqlQuery_u);
                    }
                    if(!activitystatus.isBlank() && !activitystatus.isEmpty() && activitystatus.equalsIgnoreCase("no") && before_after.equalsIgnoreCase("after")) {
                        // Closed
                    	String sqlQuery_u = "UPDATE `brr_task` SET `status`=3 WHERE `id`='"+taskid+"'";
                    	jdbcActivityTemplate.update(sqlQuery_u);
                    }
                    if(!activitystatus.isBlank() && !activitystatus.isEmpty() && before_after.equalsIgnoreCase("before")) {
                        // In-Progress
                    	String sqlQuery_u = "UPDATE `brr_task` SET `status`=2 WHERE `id`='"+taskid+"'";
                    	jdbcActivityTemplate.update(sqlQuery_u);
                    }
                    
                } else {
                    response.put("status", "error");
                    response.put("message", "Failed to insert a new task.");
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
            response.put("message", "Image Upload failed!");
        }
		return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> reportByDate(
			String fromdate, 
			String todate, 
			String roadtype, 
			String zone, 
			String ward, 
			String streetid,
			String loginId,
			String Status) {
		
		String selectVariable ="zone";
		String statusidQuery ="";
		// Use equals() for string comparison
	    if ("Open".equals(Status)) {
	    	statusidQuery =  " AND `status` = 1 "; // Open
	    } else if ("In-Progress".equals(Status)) {
	    	statusidQuery =  " AND `status` = 2 "; // Pending
	    } else if ("Pending".equals(Status)) {
	    	statusidQuery =  " AND `status` = 3 "; // Closed
	    } else if ("Completed".equals(Status)) {
	    	statusidQuery =  " AND `status` = 4 "; // Closed
	    }
	    
		// Add additional filters if provided
 	    if (zone != null && !zone.isEmpty() && !zone.isBlank()) {
 	    	selectVariable = selectVariable + ",ward";
 	    }
 	    if (ward != null && !ward.isEmpty() && !ward.isBlank()) {
 	    	selectVariable = selectVariable + ",streetid,streetName";
 	    }
 	    
		String sqlQuery = "SELECT "
				+ selectVariable +","
				+ "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS `Open`, "
				+ "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS `In-Progress`, "
				+ "SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) AS `Pending`, "
				+ "SUM(CASE WHEN status = 4 THEN 1 ELSE 0 END) AS `Completed` "
				+ "FROM `brr_task` "
				+ "WHERE "
				+ "(`fromdate` >= STR_TO_DATE(?, '%d-%m-%Y') AND `todate` <= STR_TO_DATE(?, '%d-%m-%Y')) And "
				+ "(`fromdate` <= STR_TO_DATE(?, '%d-%m-%Y') AND `todate` >= STR_TO_DATE(?, '%d-%m-%Y')) ";
				
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
	         	    if (roadtype != null && !roadtype.isEmpty() && !roadtype.isBlank()) {
	         	    	sqlQuery = sqlQuery + " AND roadtype = '"+roadtype+"' ";
	         	    }
	         sqlQuery = sqlQuery+ statusidQuery +" AND (`isactive` = 1 AND `isdelete` = 0) "
	                    + "GROUP BY "+selectVariable+" ORDER BY zone ASC";
	    System.out.println(sqlQuery);
	    List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery, fromdate, todate, todate, fromdate);
	    return result;
	}
	
	@Transactional
	public List<Map<String, Object>> reportByDate2(
			String fromdate, 
			String todate, 
			String roadtype, 
			String zone, 
			String ward, 
			String streetid,
			String loginId,
			String Status) {
	    String sqlQuery = "SELECT "
	                    + "    streetname, streetid, zone, ward, "
	                    + "    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS `Open`, "
	                    + "    SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS `In-Progress`, "
	                    + "    SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) AS `Pending`, "
	                    + "    SUM(CASE WHEN status = 4 THEN 1 ELSE 0 END) AS `Completed` "
	                    + "FROM `brr_task` "
	                    + "WHERE  "
	                    + "("
	                    + "(STR_TO_DATE(?, '%d-%m-%Y') <= `fromdate` AND STR_TO_DATE(?, '%d-%m-%Y') >= `fromdate`) OR "
	                    + "(STR_TO_DATE(?, '%d-%m-%Y') <= `todate` AND STR_TO_DATE(?, '%d-%m-%Y') >= `todate`) "
	                    + ") ";
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
	         	    if (roadtype != null && !roadtype.isEmpty() && !roadtype.isBlank()) {
	         	    	sqlQuery = sqlQuery + " AND roadtype = '"+roadtype+"' ";
	         	    }
	         sqlQuery = sqlQuery+ " AND (`isactive` = 1 AND `isdelete` = 0) "
	                    + "GROUP BY streetname,streetid, zone, ward "
	                    + "ORDER BY streetname ASC";
	       
	    List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery, fromdate, todate,fromdate, todate);
	    return result;
	}
	
	@Transactional
	public List<Map<String, Object>> reportByStatus(
			String fromdate, 
			String todate, 
			String Status, 
			String roadtype, 
			String zone, 
			String ward, 
			String streetid,
			String loginId) {
	    int statusid = 0;

	    // Use equals() for string comparison
	    if ("Open".equals(Status)) {
	        statusid = 1; // Open
	    } else if ("In-Progress".equals(Status)) {
	        statusid = 2; // Pending
	    } else if ("Pending".equals(Status)) {
	        statusid = 3; // Closed
	    } else if ("Completed".equals(Status)) {
	    	statusid = 4; // Closed
	    }
	    
	    System.out.println(statusid);

	    // Main SQL query to retrieve tasks based on status and date range
	    String sqlQuery = "SELECT *,CASE "
	    		+ "        WHEN status = 1 THEN 'Open'"
				+ "        WHEN status = 2 THEN 'In-Progress'"
				+ "        WHEN status = 3 THEN 'Pending'"
				+ "        WHEN status = 4 THEN 'Completed'"
				+ "        ELSE 'unknown'"
				+ "    END AS status_name "
	            + "FROM `brr_task` "
	            + "WHERE  "
	            + "("
	           // + "(STR_TO_DATE(?, '%d-%m-%Y') <= `fromdate` AND STR_TO_DATE(?, '%d-%m-%Y') >= `fromdate`) OR "
	           // + "(STR_TO_DATE(?, '%d-%m-%Y') <= `todate` AND STR_TO_DATE(?, '%d-%m-%Y') >= `todate`) "
	            + "(`fromdate` >= STR_TO_DATE(?, '%d-%m-%Y') AND `todate` <= STR_TO_DATE(?, '%d-%m-%Y')) And "
				+ "(`fromdate` <= STR_TO_DATE(?, '%d-%m-%Y') AND `todate` >= STR_TO_DATE(?, '%d-%m-%Y')) "
	            + ") AND `status` = ? ";
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
         	    if (roadtype != null && !roadtype.isEmpty() && !roadtype.isBlank()) {
         	    	sqlQuery = sqlQuery + " AND roadtype = '"+roadtype+"' ";
         	    }
         	   sqlQuery = sqlQuery +  "AND `isactive` = 1 AND `isdelete` = 0 "
	            + "GROUP BY streetname, id "
	            + "ORDER BY streetname ASC";

	    List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery, fromdate, todate, todate,fromdate, statusid);

	    if (result.isEmpty()) {
	        return result;
	    } else {
	        for (Map<String, Object> task : result) {
	            String taskId = task.get("id").toString();

	            // SQL query to retrieve task activity logs
	            String sqlQuery2 = "("
	            		+ "    SELECT "
	            		+ "        *,"
	            		+ "        CONCAT('"+fileBaseUrl+"/gccofficialapp/files', filepath) AS imageUrl,"
	            		+ "        0 AS activity_id "
	            		+ "    FROM "
	            		+ "        `brr_task_activity_log` "
	            		+ "    WHERE "
	            		+ "        taskid = ? AND before_after = 'before' "
	            		+ "    ORDER BY "
	            		+ "        `id` DESC "
	            		+ "    LIMIT 1 "
	            		+ ") "
	            		+ "UNION ALL "
	            		+ "( "
	            		+ "    SELECT "
	            		+ "        *,"
	            		+ "        CONCAT('"+fileBaseUrl+"/gccofficialapp/files', filepath) AS imageUrl, "
	            		+ "        (SELECT id FROM brr_task_activity_log bl WHERE bl.taskid = brr_task_activity_log.taskid AND bl.before_after = 'before' ORDER BY id DESC LIMIT 1) AS activity_id "
	            		+ "    FROM "
	            		+ "        `brr_task_activity_log` "
	            		+ "    WHERE "
	            		+ "        taskid = ? AND before_after = 'after'"
	            		+ "    ORDER BY "
	            		+ "        `id` DESC "
	            		+ "    LIMIT 1"
	            		+ ") "
	            		+ "ORDER BY `id` DESC";
	            List<Map<String, Object>> result2 = jdbcActivityTemplate.queryForList(sqlQuery2, taskId, taskId);

	            // Add the task activity data to the corresponding task
	            task.put("task_activity_data", result2);
	        }
	    }

	    return result;
	}
	
}
