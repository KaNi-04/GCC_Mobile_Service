package in.gov.chennaicorporation.mobileservice.works.service;

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

@Service
public class WorksService {
	private JdbcTemplate jdbcWorksTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
	@Autowired
	public void setDataSource(@Qualifier("mysqlWorksDataSource") DataSource worksDataSource) {
		this.jdbcWorksTemplate = new JdbcTemplate(worksDataSource);
	}
	
	@Autowired
	public WorksService(Environment environment) {
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
        String serviceFolderName = environment.getProperty("works_task_foldername");
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
            Files.write(path, compressedBytes);
            
            System.out.println(filePath);
            return filepath_txt;
            
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to save file " + file.getOriginalFilename();
        } 
	}
	
	public String getWardByLoginId(String loginid, String type) {
	    String sqlQuery = "SELECT `ward`, `works` FROM `gcc_penalty_hoardings`.`hoading_user_list` WHERE `userid` = ? LIMIT 1";
	    
	    List<Map<String, Object>> results = jdbcWorksTemplate.queryForList(sqlQuery, loginid);

	    if (!results.isEmpty()) {
	        Map<String, Object> row = results.get(0);
	        String wardClause;
	        String departmentClause = "";

	        Object wardValue = row.get("ward");
	        Object worksValue = row.get("works");

	        if (wardValue == null || wardValue.toString().equals("0")) {
	            wardClause = "wu.`ward` NOT IN (0)";
	        } else {
	            wardClause = "wu.`ward` IN ('" + wardValue + "')";
	        }

	        if (worksValue != null) {
	            departmentClause = " AND wu.`department` IN (" + worksValue + ")";
	        }

	        return wardClause + departmentClause;
	    }

	    return "wu.`ward` IN ('-')";
	}
	
	public List<Map<String, Object>> getESTList(String loginid) {
       /*
		String sql = "SELECT wu.`Id` AS estid,  `wu`.`sub_category` AS works, tw.id AS typeofworkid,  `wu`.`estimate_no`, "
        		+ "`wu`.`estimate_date`, `wu`.`zone`, `wu`.`ward`, `wu`.`project_name`, `wu`.`location`, `wu`.`department`, "
        		+ "`wu`.`contractor_name`, `wu`.`contractor_period`, `wu`.`fund_source`, `wu`.`scheme`, `wu`.`category`, `wu`.`sub_category` AS `sub_category`, "
        		+ "`wu`.`est_amount`, `wu`.`tech_sanc_date`, `wu`.`admin_sanc_date`, `wu`.`tender_call_date`, `wu`.`tender_fin_date`, "
        		+ "`wu`.`loa_date`, `wu`.`agreement_date`, `wu`.`work_order_date`, `wu`.`work_comm_date` "
        		+ "FROM `works_update` `wu` LEFT JOIN EGW_TYPEOFWORK `tw` ON trim(`tw`.description) = trim(`wu`.`sub_category`)";
       */
		
		String where = getWardByLoginId(loginid,"ae");
		//System.out.println("ward: "+ward);
		String sql = "SELECT "
				+ "wu.`estid` AS estid, "
				+ "tm.`description` AS works, "
				+ "tm.id AS typeofworkid, "
				+ "wu.`estimate_no`,  "
				+ "wu.`estimate_date`, "
				+ "wu.`zone`, "
				+ "wu.`ward`,  "
				+ "wu.`project_name`, "
				+ "wu.`location`, "
				+ "dm.`dept_name` AS `department`, "
				+ "wu.`contractor_name`, "
				+ "CAST(wu.contractor_period AS CHAR) AS contractor_period, "
				+ "fs.`name` AS `fund_source`, "
				+ "s.`name` AS `scheme`, "
				+ "wm.`name` AS `category`, "
				+ "tm.`description` AS `sub_category`, "
				+ "wu.`estimation_amount` AS `est_amount`, "
				+ "wu.`technical_sanction_date` AS `tech_sanc_date`, "
				+ "wu.`admin_sanction_date` AS `admin_sanc_date`, "
				+ "wu.`tender_called_date` AS `tender_call_date`, "
				+ "wu.`tender_finalized_date` AS `tender_fin_date`, "
				+ "wu.`loa_date` AS `loa_date`, "
				+ "wu.`agreement_date` AS `agreement_date`, "
				+ "wu.`work_order_date` AS `work_order_date`, "
				+ "wu.`work_commenced_date` AS `work_comm_date`, "
				+ "wu.`percentage` AS `percentage` "
				+ "FROM `erp_works` wu "
				//+ "LEFT JOIN typeofwork_master tw ON trim(tw.description) = trim(wu.`sub_category`)"
				+ "LEFT JOIN department_master dm ON dm.id = wu.`department`"
				+ "LEFT JOIN fundsource_master fs ON fs.id = wu.`fund_source` "
				+ "LEFT JOIN scheme_master s ON s.id = wu.`scheme` "
				+ "LEFT JOIN workstype_master wm ON wm.id = wu.`category` "
				+ "LEFT JOIN typeofwork_master tm ON tm.id = wu.`sub_category` "
				+ "WHERE "+where;
		
        List<Map<String, Object>> result = jdbcWorksTemplate.queryForList(sql);
        /*
        List<Map<String, Object>> estWorklistData = new ArrayList<>();
        
        for (Map<String, Object> row : result) {
        	// CHECK IN WORK_TASK
        	
        	// CHECK IN typeofwork_task_master
        	
        	// CHECK IN task_data last DATA for each typeofwork_task_master 
        }
        */
        
        return jdbcWorksTemplate.queryForList(sql);
    }
	
	/*
	public List<Map<String, Object>> getTaskDataListWithData(String estid, String typeofworkid) {
        String sql = "SELECT `tdid`, `estid`, `tmid`, `percentage`, `cdate`, `cby`, `zone`, `ward`, `latitude`, `longitude`, `stage_file`, `status`, `isactive` FROM `task_data` WHERE 1";
        return jdbcWorksTemplate.queryForList(sql,typeofworkid);
    }
	*/
	
	public List<Map<String, Object>> getESTWorkListData(String estid) {
	    
	    String sql = "SELECT `id` AS wtid, `estid`, `estimate_no`, `sub_cat_id`, `question_id`, `ans_id`,"
	    		+ " `remarks`, `cdate`, `isactive`, `isdelete`, `percentage` "
	    		+ " FROM `erp_works_task` "
	    		+ " WHERE `isactive`=1 AND `isdelete`=0 AND `estid`= ?";

	    List<Map<String, Object>> result = jdbcWorksTemplate.queryForList(sql, estid);
	   
	    // Prepare the response
	    Map<String, Object> response = new HashMap<>();
	    response.put("estWorklistData", result);
	    response.put("message", "EST Activity Data List.");
	    
	    // Return the response wrapped in a singleton list
	    return Collections.singletonList(response);
	}

	public List<Map<String, Object>> getTaskDataListWithData(String wtid, String estid, String typeofworkid) {
	    
	    String sql = "SELECT `id`, `estid`, `estimate_no`, `sub_cat_id`, `question_id`, `ans_id`,"
	    		+ " `remarks`, `cdate`, `isactive`, `isdelete`, `percentage` FROM `erp_works_task` "
	    		+ "WHERE `id`=? AND `estid`= ? AND `isactive`=1 AND `isdelete`=0";

	    List<Map<String, Object>> result = jdbcWorksTemplate.queryForList(sql, wtid, estid);
	    
	    List<Map<String, Object>> estTaskWorklistData = new ArrayList<>();
	  
	    for (Map<String, Object> row : result) {
	    	
	        // Query for the task related to the current activity
	        String sqlForTask = "SELECT `tmid`, `stagename` AS `taskname`, `isactive`, `isdelete`, `orderby`, "
	        		+ "("
	        		+ "        SELECT `percentage` "
	        		+ "        FROM `erp_task_data` "
	        		+ "        WHERE `tmid` = `erp_works_stages`.`tmid` AND wtid=? AND `isactive`=1 "
	        		+ "        ORDER BY `cdate` DESC "
	        		+ "        LIMIT 1 "
	        		+ "    ) AS percentage  "
	        		+ "FROM `erp_works_stages` WHERE `isactive`=1 AND `isdelete`=0 AND `estid`=? ORDER BY `orderby`";
	        
	        List<Map<String, Object>> taskresult = jdbcWorksTemplate.queryForList(sqlForTask, wtid, estid);
	        
	        if (!taskresult.isEmpty()) {
	            // For each task in the taskresult, add taskdataresult
	            for (Map<String, Object> task : taskresult) {
	                // Query for the task-related data
	                String tmid = ""+task.get("tmid"); // Assuming tmid is a field in task
	                String sqlForTaskData = "SELECT `tdid`, `estid`, `tmid`, `percentage`, `remarks`, "
	                		+ "DATE_FORMAT(`cdate`, '%d-%m-%Y %r') AS createddate, "
	                		+ "`cby`, `zone`, `ward`, `latitude`, `longitude`, "
	                		+ " CONCAT('"+fileBaseUrl+"/gccofficialapp/files', `stage_file`) AS image,  "
	                		+ "`status`, `isactive` FROM `erp_task_data` WHERE `wtid`=? AND `tmid`=? AND `estid`=?  AND `isactive`=1";
	                
	                List<Map<String, Object>> taskdataresult = jdbcWorksTemplate.queryForList(sqlForTaskData, wtid, tmid, estid);
	                
	                // Add taskdataresult to each task
	                task.put("taskdata", taskdataresult);
	            }
	        }
	        // Add tasklist (with taskdata) to the row
            row.put("tasklist", taskresult); 
            
	        // Add the row to SWDData
	        estTaskWorklistData.add(row);
	    }
	    
	    // Prepare the response
	    Map<String, Object> response = new HashMap<>();
	    response.put("estTaskWorklistData", estTaskWorklistData);
	    response.put("message", "EST Activity Data List.");
	    
	    // Return the response wrapped in a singleton list
	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> addTaskStageActivityData(
			String estid,
			String tmid,
			String wtid,
			MultipartFile file,
			String percentage,
			String remarks,
			String cby,
			String zone,
			String ward,
			String latitude,
			String longitude,
			String status) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		
		int lastInsertId = 0;
	
		String filetxt = "";
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			filetxt = fileUpload(estid+"_"+tmid, cby, file);
		}
		String finalfile = filetxt;
		String sqlQuery = "INSERT INTO `erp_task_data`"
				+ "(`estid`, `tmid`, `percentage`, `remarks`, `cby`, `zone`, `ward`, `latitude`, `longitude`, `stage_file`, `status`,`wtid`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		LocalDateTime approvedDate = LocalDateTime.now();

		String formattedDate = approvedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		try {
			status = "open";
			if(percentage.contains("100")) {
				status = "close";
			}
			
			String statustxt = status;
			
				// Insert the data the required number of times
				int affectedRows = jdbcWorksTemplate.update(new PreparedStatementCreator() {
					@Override
					public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
						PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "did" });
						ps.setString(1, estid);
						ps.setString(2, tmid);
						ps.setString(3, percentage);
						ps.setString(4, remarks);
						ps.setString(5, cby);
						ps.setString(6, zone);
						ps.setString(7, ward);
						ps.setString(8, latitude);
						ps.setString(9, longitude);
						ps.setString(10, finalfile);
						ps.setString(11, statustxt);
						ps.setString(12, wtid);
						return ps;
					}
				}, keyHolder);
			
				if (affectedRows > 0) {
					Number generatedId = keyHolder.getKey();
					lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
					response.put("insertId", lastInsertId);
					response.put("status", "success");
					response.put("message", "Stage Task data inserted successfully!");
					System.out.println("Stage Task was inserted successfully! Insert ID: " + generatedId);
					updateStatus(percentage, estid, wtid);
				} else {
					response.put("status", "error");
					response.put("message", "Failed to insert stage task data.");
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
/*
		// Update Percentage of works progress 
		String sqlUpdateQuery ="UPDATE erp_works ew "
				+ "JOIN ( "
				+ "    SELECT  "
				+ "        ew.estid, "
				+ "        ROUND(SUM(COALESCE(etd.percentage, 0)) / COUNT(*), 0) AS physical_progress "
				+ "    FROM erp_works ew "
				+ "    JOIN erp_works_task ewt ON ewt.estid = ew.estid "
				+ "    JOIN erp_works_stages ews ON ews.estid = ew.estid "
				+ "    LEFT JOIN erp_task_data etd  "
				+ "        ON etd.estid = ew.estid  "
				+ "        AND etd.wtid = ewt.id  "
				+ "        AND etd.tmid = ews.tmid "
				+ "    GROUP BY ew.estid "
				+ "	HAVING physical_progress > 0 "
				+ ") AS prog "
				+ "ON ew.estid = prog.estid "
				+ "SET  "
				+ "    ew.percentage = prog.physical_progress, "
				+ "    ew.update_status = CASE "
				+ "        WHEN prog.physical_progress = 0 THEN 'pending' "
				+ "        WHEN prog.physical_progress < 100 THEN 'Inprogress' "
				+ "        ELSE 'Completed' "
				+ "    END";
*/		
		String sqlUpdateQuery ="UPDATE erp_works ew "
				+ "JOIN ( "
				+ "    SELECT   "
				+ "        ew.estid, "
				+ "        ROUND(SUM(COALESCE(etd.percentage, 0)) / COUNT(*), 0) AS physical_progress "
				+ "    FROM erp_works ew "
				+ "    JOIN erp_works_task ewt ON ewt.estid = ew.estid "
				+ "    JOIN erp_works_stages ews ON ews.estid = ew.estid "
				+ "    LEFT JOIN ( "
				//+ "        -- Get only the latest active record per (estid, wtid, tmid) "
				+ "        SELECT etd.* "
				+ "        FROM erp_task_data etd "
				+ "        INNER JOIN ( "
				+ "            SELECT estid, wtid, tmid, MAX(tdid) AS max_tdid "
				+ "            FROM erp_task_data "
				+ "            WHERE isactive = 1 "
				+ "            GROUP BY estid, wtid, tmid "
				+ "        ) latest ON latest.estid = etd.estid "
				+ "                 AND latest.wtid = etd.wtid "
				+ "                 AND latest.tmid = etd.tmid "
				+ "                 AND latest.max_tdid = etd.tdid "
				+ "    ) etd ON etd.estid = ew.estid  "
				+ "         AND etd.wtid = ewt.id  "
				+ "         AND etd.tmid = ews.tmid "
				+ "    GROUP BY ew.estid "
				+ "    HAVING physical_progress > 0 "
				+ ") AS prog "
				+ "ON ew.estid = prog.estid "
				+ "SET   "
				+ "    ew.percentage = prog.physical_progress, "
				+ "    ew.update_status = CASE  "
				+ "        WHEN prog.physical_progress = 0 THEN 'pending' "
				+ "        WHEN prog.physical_progress < 100 THEN 'Inprogress' "
				+ "        ELSE 'Completed' "
				+ "    END";
				
		System.out.println("Percentage Update : " + sqlUpdateQuery + " --> ESTID : " + estid);
		jdbcWorksTemplate.update(sqlUpdateQuery);
		
		return Collections.singletonList(response);
	}
	
	public boolean updateStatus(String percentage,String estid, String wtid) {
		int statusAffected = 0;
		/*
		String selectSql = "SELECT "
				+ "        ewt.id,"
				+ "        ROUND(SUM(COALESCE(etd.percentage, 0)) / COUNT(*), 0) AS physical_progress"
				+ "    FROM erp_works ew"
				+ "    JOIN erp_works_task ewt ON ewt.estid = ew.estid"
				+ "    JOIN erp_works_stages ews ON ews.estid = ew.estid"
				+ "    LEFT JOIN erp_task_data etd "
				+ "        ON etd.estid = ew.estid "
				+ "        AND etd.wtid = ewt.id "
				+ "        AND etd.tmid = ews.tmid"
				+ "        AND ewt.id = ?"
				+ "    GROUP BY ew.estid"
				+ "	HAVING physical_progress > 0";
		*/
		
		String selectSql ="SELECT  "
				+ "    ewt.id, "
				+ "    ROUND(SUM(COALESCE(etd.percentage, 0)) / COUNT(*), 0) AS physical_progress "
				+ "FROM  "
				+ "    erp_works ew "
				+ "JOIN  "
				+ "    erp_works_task ewt ON ewt.estid = ew.estid "
				+ "JOIN  "
				+ "    erp_works_stages ews ON ews.estid = ew.estid "
				+ "LEFT JOIN ( "
				//+ "    -- Subquery to get only latest entry per estid/wtid/tmid based on highest tdid "
				+ "    SELECT etd.* "
				+ "    FROM erp_task_data etd "
				+ "    INNER JOIN ( "
				+ "        SELECT estid, wtid, tmid, MAX(tdid) AS max_tdid "
				+ "        FROM erp_task_data "
				+ "        WHERE isactive = 1 "
				+ "        GROUP BY estid, wtid, tmid "
				+ "    ) latest ON latest.estid = etd.estid "
				+ "             AND latest.wtid = etd.wtid "
				+ "             AND latest.tmid = etd.tmid "
				+ "             AND latest.max_tdid = etd.tdid "
				+ ") etd ON etd.estid = ew.estid  "
				+ "     AND etd.wtid = ewt.id  "
				+ "     AND etd.tmid = ews.tmid "
				+ "WHERE  "
				+ "    ewt.id = ? "
				+ "GROUP BY  "
				+ "    ew.estid "
				+ "HAVING  "
				+ "    physical_progress > 0";
		
		List<Map<String, Object>> rows = jdbcWorksTemplate.queryForList(selectSql, new Object[]{wtid});

		for (Map<String, Object> row : rows) {
		    int physical_progress = ((Number) row.get("physical_progress")).intValue();

		    String updateSql = "UPDATE `erp_works_task` SET `percentage`=? WHERE `estid`=? AND `id`=?";
		    statusAffected = jdbcWorksTemplate.update(updateSql, physical_progress, estid, wtid);

		    System.out.println("Updated " + statusAffected + " row(s) for estid=" + estid + ", wtid=" + wtid);
		}
	    
	    // System.out.println("UPDATE `erp_works_task` SET `percentage`="+percentage+" WHERE `estid`="+estid+" AND `id`="+wtid);
	    // System.out.println("UPDATE `erp_works_task` Status: " + statusAffected);
		
		//int rowsAffected = 0;
	    //if (statusAffected > 0) {
	    	/*
	    	sqlQuery = "UPDATE `works_update` SET `percentage`=? WHERE `id`=?";
		    rowsAffected = jdbcWorksTemplate.update(sqlQuery, new Object[]{percentage,estid});
		    
		    System.out.println("UPDATE `works_update` SET `percentage`="+percentage+" WHERE `id`="+estid);
		    System.out.println("UPDATE `works_update` Status: " + rowsAffected);
		    */
	    //}
	    return true;
	}
	
	
	public List<Map<String, Object>> getZoneReport() {
	    
	    String sql = "SELECT "
	    		+ "    CASE "
	    		+ "        WHEN zone = '00' THEN 'HQ' "
	    		+ "        ELSE zone "
	    		+ "    END AS zone, "
	    		+ "    COUNT(estid) AS Total, "
	    		+ "    SUM(CASE WHEN percentage = 100 THEN 1 ELSE 0 END) AS CompletedTotal, "
	    		+ "    SUM(CASE WHEN percentage > 0 AND percentage < 100 THEN 1 ELSE 0 END) AS OngoingTotal, "
	    		+ "	SUM(CASE WHEN percentage = 0 THEN 1 ELSE 0 END) AS NotStartedTotal "
	    		+ "FROM "
	    		+ "    `erp_works` "
	    		+ "GROUP BY "
	    		+ "    zone "
	    		+ "ORDER BY "
	    		+ "    zone;";

	    List<Map<String, Object>> result = jdbcWorksTemplate.queryForList(sql);
	   
	    // Prepare the response
	    Map<String, Object> response = new HashMap<>();
	    response.put("ZoneData", result);
	    response.put("message", "EST Activity Zone Data List.");
	    
	    // Return the response wrapped in a singleton list
	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getWardReport(String zone) {
	    
	    String sql = "SELECT "
	    		+ "    CASE "
	    		+ "        WHEN zone = '00' THEN 'HQ' "
	    		+ "        ELSE zone "
	    		+ "    END AS zone, "
	    		+ "	CASE "
	    		+ "        WHEN ward = '000' THEN 'HQ' "
	    		+ "        ELSE ward "
	    		+ "    END AS ward, "
	    		+ "    COUNT(estid) AS Total, "
	    		+ "    SUM(CASE WHEN percentage = 100 THEN 1 ELSE 0 END) AS CompletedTotal, "
	    		+ "    SUM(CASE WHEN percentage > 0 AND percentage < 100 THEN 1 ELSE 0 END) AS OngoingTotal, "
	    		+ "	SUM(CASE WHEN percentage = 0 THEN 1 ELSE 0 END) AS NotStartedTotal "
	    		+ "FROM  "
	    		+ "    `erp_works` "
	    		+ "WHERE zone=? "
	    		+ "GROUP BY "
	    		+ "    zone, ward "
	    		+ "ORDER BY "
	    		+ "    ward";

	    List<Map<String, Object>> result = jdbcWorksTemplate.queryForList(sql,zone);
	   
	    // Prepare the response
	    Map<String, Object> response = new HashMap<>();
	    response.put("estWorklistData", result);
	    response.put("message", "EST Activity Data List.");
	    
	    // Return the response wrapped in a singleton list
	    return Collections.singletonList(response);
	}

	public List<Map<String, Object>> getListReport(String ward, String statusFilter) {
	    
		

		StringBuilder sqlBuilder = new StringBuilder("SELECT "
		        + "wu.`estid` AS estid, "
		        + "tm.`description` AS works, "
		        + "tm.id AS typeofworkid, "
		        + "wu.`estimate_no`,  "
		        + "wu.`estimate_date`, "
		        + "wu.`zone`, "
		        + "wu.`ward`,  "
		        + "wu.`project_name`, "
		        + "wu.`location`, "
		        + "dm.`dept_name` AS `department`, "
		        + "wu.`contractor_name`, "
		        + "CAST(wu.contractor_period AS CHAR) AS contractor_period, "
		        + "fs.`name` AS `fund_source`, "
		        + "s.`name` AS `scheme`, "
		        + "wm.`name` AS `category`, "
		        + "tm.`description` AS `sub_category`, "
		        + "wu.`estimation_amount` AS `est_amount`, "
		        + "wu.`technical_sanction_date` AS `tech_sanc_date`, "
		        + "wu.`admin_sanction_date` AS `admin_sanc_date`, "
		        + "wu.`tender_called_date` AS `tender_call_date`, "
		        + "wu.`tender_finalized_date` AS `tender_fin_date`, "
		        + "wu.`loa_date` AS `loa_date`, "
		        + "wu.`agreement_date` AS `agreement_date`, "
		        + "wu.`work_order_date` AS `work_order_date`, "
		        + "wu.`work_commenced_date` AS `work_comm_date`, "
		        + "wu.`percentage` AS `percentage` "
		        + "FROM `erp_works` wu "
		        + "LEFT JOIN department_master dm ON dm.id = wu.`department` "
		        + "LEFT JOIN fundsource_master fs ON fs.id = wu.`fund_source` "
		        + "LEFT JOIN scheme_master s ON s.id = wu.`scheme` "
		        + "LEFT JOIN workstype_master wm ON wm.id = wu.`category` "
		        + "LEFT JOIN typeofwork_master tm ON tm.id = wu.`sub_category` "
		        + "WHERE wu.`ward` = '" + ward + "' ");

		switch (statusFilter) {
		    case "completed":
		        sqlBuilder.append("AND wu.percentage = 100 ");
		        break;
		    case "ongoing":
		        sqlBuilder.append("AND wu.percentage > 0 AND wu.percentage < 100 ");
		        break;
		    case "not_started":
		        sqlBuilder.append("AND wu.percentage = 0 ");
		        break;
		    // default is no filtering (all)
		}

		String sql = sqlBuilder.toString();
		List<Map<String, Object>> result = jdbcWorksTemplate.queryForList(sql);
	   
	    // Prepare the response
	    Map<String, Object> response = new HashMap<>();
	    response.put("estWorklistData", result);
	    response.put("message", "EST Data List.");
	    
	    // Return the response wrapped in a singleton list
	    return Collections.singletonList(response);
	}
	
	
}
