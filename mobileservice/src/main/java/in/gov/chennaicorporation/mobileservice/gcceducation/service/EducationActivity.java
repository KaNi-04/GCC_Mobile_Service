package in.gov.chennaicorporation.mobileservice.gcceducation.service;

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
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class EducationActivity {
	private JdbcTemplate jdbcEducationTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlGccEducationDataSource") DataSource educationDataSource) {
		this.jdbcEducationTemplate = new JdbcTemplate(educationDataSource);
	}
    
    @Autowired
	public EducationActivity(Environment environment) {
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
        String serviceFolderName = environment.getProperty("gcceducation_foldername");
        var year =DateTimeUtil.getCurrentYear();
        var month =DateTimeUtil.getCurrentMonth();
        
        uploadDirectory = uploadDirectory + serviceFolderName + year +"/"+month;
        
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
	        
	        String filepath_txt = "/"+serviceFolderName + year + "/" + month + "/" + fileName;
	        
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
	
	// Helper method to convert G, G+1, etc. to readable names
	private String getReadableFloorName(String shortName) {
	    Map<String, String> floorMap = Map.of(
	        "G", "Ground Floor",
	        "G+1", "First Floor",
	        "G+2", "Second Floor",
	        "G+3", "Third Floor",
	        "G+4", "Fourth Floor",
	        "G+5", "Fifth Floor",
	        "G+6", "Sixth Floor",
	        "G+7", "Seventh Floor",
	        "G+8", "Eighth Floor"
	    );
	    return floorMap.getOrDefault(shortName, shortName); // fallback to original if not found
	}
	
	//////////////////////////////
	
	public String getConfigValue() {
		String sqlQuery = "SELECT `id`, `lat_long_radius` FROM `config` LIMIT 1";
		String value = "50"; // default fallback value
		List<Map<String, Object>> results = jdbcEducationTemplate.queryForList(sqlQuery);
		
		if (!results.isEmpty()) {
		Map<String, Object> row = results.get(0);
		value = String.valueOf(row.get("lat_long_radius")); // corrected key name
		}
		
		return value;
	}
	
	//////////////////////////////
	
	public List<Map<String, Object>> getUnmapSchoolList(
			MultiValueMap<String, String> fromData,
			String loginid){
		String sql = "SELECT * FROM `school_list` WHERE `gcc_app_updated`=0 AND `isactive`=1";
		List<Map<String, Object>> result = jdbcEducationTemplate.queryForList(sql);
		
		Map<String, Object> response = new HashMap<>();
		response.put("status", "Success");
		response.put("message", "UnMapped School List.");
		response.put("data", result);
		
		return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getSchoolList(
			MultiValueMap<String, String> formData,
			String latitude,
			String longitude,
			String loginid
			) {
		
			String sqlWhere = "";
			String radius = getConfigValue();
			
			if (latitude != null && longitude != null && !latitude.isBlank() && !latitude.isEmpty() && !longitude.isBlank()
			&& !longitude.isEmpty()) {
			sqlWhere += " AND ((6371008.8 * acos(ROUND(cos(radians(" + latitude
			+ ")) * cos(radians(sd.latitude)) * cos(radians(sd.longitude) - radians(" + longitude
			+ ")) + sin(radians(" + latitude + ")) * sin(radians(sd.latitude)), 9))) < "+radius+")"
			+ " ORDER BY"
			+ "    sd.`id` DESC";
			}
			
			String sql = "SELECT sl.*, sd.latitude, sd.longitude FROM `school_details` sd "
					+ "LEFT JOIN `school_list` sl  ON sl.gcc_app_updated=sd.id "
					+ "WHERE `isactive`=1 "+sqlWhere;
			//System.out.println(sql);
			List<Map<String, Object>> result = jdbcEducationTemplate.queryForList(sql);
			
			Map<String, Object> response = new HashMap<>();
			response.put("status", "Success");
			response.put("message", "Near By School List.");
			response.put("data", result);
			
			return Collections.singletonList(response);
		}
	///////////////////////////////
	
	public List<Map<String, Object>> getMenu(
			MultiValueMap<String, String> formData,
			String loginid
			){
		
		String sql ="SELECT *,CONCAT('https://gccservices.in/gccofficialapp/files/app_icon/menu/', icon) "
				+ "AS iconUrl  FROM `mobile_category` WHERE `isactive`=1 AND `isdelete`=0 "
				+ "ORDER BY orderby";
		List<Map<String, Object>> result = jdbcEducationTemplate.queryForList(sql);
		
		Map<String, Object> response = new HashMap<>();
		response.put("status", "Success");
		response.put("message", "Menu List.");
		response.put("data", result);
		
		return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getBuildings(
			MultiValueMap<String, String> formData,
			String school_details_id,
			String loginid
			){
		
		String sql ="SELECT `buildng_id` as building_id, `building_name`, `total_floor`, `bmm`.`content` AS `building_material`, "
				+ "`rmm`.`content` AS `roof_material`, `building_order`, `school_id` FROM `buildings` as `b` "
				+ "LEFT JOIN `building_material_master` as `bmm` ON `bmm`.`bm_id`=`b`.`building_material` "
				+ "LEFT JOIN `roof_material_master` as `rmm` ON `rmm`.`rm_id`=`b`.`roof_material` "
				+ " WHERE (`b`.`is_active`=1 AND `b`.`is_delete`=0) AND `b`.`school_id`=?";
		List<Map<String, Object>> result = jdbcEducationTemplate.queryForList(sql, school_details_id);
		
		Map<String, Object> response = new HashMap<>();
		response.put("status", "Success");
		response.put("message", "Building List.");
		response.put("data", result);
		
		return Collections.singletonList(response);
	}
	
	private List<String> getRoomTypeByMenuId(String menu_id) {
	    String sql = "SELECT room_master_id FROM room_master WHERE mobile_category = ?";
	    List<String> roomTypes = jdbcEducationTemplate.queryForList(sql, new Object[]{menu_id}, String.class);
	    // If no data is found, return a list with "0"
	    if (roomTypes.isEmpty()) {
	        roomTypes.add("0");
	    }

	    return roomTypes;
	}
	
	public List<Map<String, Object>> getBuilding_list(
			MultiValueMap<String, String> formData,
			String school_details_id,
			String menu_id,
			String loginid
			){
		
		// Get room types
	    List<String> roomTypes = getRoomTypeByMenuId(menu_id);
	    // Construct the SQL
	    String sql = "SELECT b.buildng_id AS building_id, b.building_name, b.total_floor, " +
	                 "bmm.content AS building_material, rmm.content AS roof_material, " +
	                 "b.building_order, b.school_id " +
	                 "FROM buildings b " +
	                 "LEFT JOIN building_material_master bmm ON bmm.bm_id = b.building_material " +
	                 "LEFT JOIN roof_material_master rmm ON rmm.rm_id = b.roof_material " +
	                 "WHERE b.is_active = 1 AND b.is_delete = 0 AND b.buildng_id IN ( " +
	                 "   SELECT building_id FROM room_details WHERE room_type IN (:roomTypes) AND school_id = :schoolId " +
	                 ")";
	   // System.out.println(roomTypes);
	    // Use NamedParameterJdbcTemplate to inject list
	    Map<String, Object> params = new HashMap<>();
	    params.put("roomTypes", roomTypes);
	    params.put("schoolId", school_details_id);
	    
	    NamedParameterJdbcTemplate namedJdbc = new NamedParameterJdbcTemplate(jdbcEducationTemplate);
	    List<Map<String, Object>> result = namedJdbc.queryForList(sql, params);
	    
		Map<String, Object> response = new HashMap<>();
		response.put("status", "Success");
		response.put("message", "Building List.");
		response.put("data", result);
		
		return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getFloor_list(
			MultiValueMap<String, String> formData,
			String school_details_id,
			String menu_id,
			String building_id,
			String loginid
			){
		
		// Get room types
	    List<String> roomTypes = getRoomTypeByMenuId(menu_id);
		// Construct the SQL
	    String sql = "SELECT floors_id, floor_name FROM `floor_list` where floors_id IN (" +
	                 "   SELECT floors_id FROM room_details WHERE room_type IN (:roomTypes) AND school_id = :schoolId AND `building_id` = :buildingId" +
	                 ")";
		//List<Map<String, Object>> result = jdbcEducationTemplate.queryForList(sql, school_details_id, building_id);
	    
		// Use NamedParameterJdbcTemplate to inject list
	    Map<String, Object> params = new HashMap<>();
	    params.put("roomTypes", roomTypes);
	    params.put("schoolId", school_details_id);
	    params.put("buildingId", building_id);
	    
	    NamedParameterJdbcTemplate namedJdbc = new NamedParameterJdbcTemplate(jdbcEducationTemplate);
	    List<Map<String, Object>> result = namedJdbc.queryForList(sql, params);
	    
		// Map short floor name to readable format
	    for (Map<String, Object> floor : result) {
	        String shortName = (String) floor.get("floor_name");
	        floor.put("floor_name", getReadableFloorName(shortName));
	    }
	    
 		Map<String, Object> response = new HashMap<>();
 		response.put("status", "Success");
 		response.put("message", "Building List.");
 		response.put("data", result);

		return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getRoom_list(
			MultiValueMap<String, String> formData,
			String school_details_id,
			String menu_id,
			String building_id,
			String floors_id,
			String loginid
			){
		
		// Get room types
	    List<String> roomTypes = getRoomTypeByMenuId(menu_id);
		// Construct the SQL
	    String sql = "SELECT rd.`room_id`, rd.`room_type`, rm.`room_type` AS `room_type_name`, rd.`room_num` as `room_name`, rd.`building_id`, rd.`floors_id`, rd.`school_id` FROM `room_details` rd "
	    		+ "LEFT JOIN room_master rm ON rm.room_master_id=rd.room_type WHERE rd.room_type IN (:roomTypes) AND rd.`school_id` = :schoolId AND rd.`building_id` = :buildingId AND rd.`floors_id` = :foorsId";
	    		
		//List<Map<String, Object>> result = jdbcEducationTemplate.queryForList(sql, school_details_id, building_id, floors_id);
	    
		// Use NamedParameterJdbcTemplate to inject list
	    Map<String, Object> params = new HashMap<>();
	    params.put("roomTypes", roomTypes);
	    params.put("schoolId", school_details_id);
	    params.put("buildingId", building_id);
	    params.put("foorsId", floors_id);
	    
	    NamedParameterJdbcTemplate namedJdbc = new NamedParameterJdbcTemplate(jdbcEducationTemplate);
	    List<Map<String, Object>> result = namedJdbc.queryForList(sql, params);
	    
 		Map<String, Object> response = new HashMap<>();
 		response.put("status", "Success");
 		response.put("message", "Floors List.");
 		response.put("data", result);

		return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getLibrary(
			MultiValueMap<String, String> formData,
			String school_details_id,
			String loginid
			){
		
		String sql ="SELECT `room_id`, `LIBRARY`, `room_num`, `building_id`, `floors_id`, `school_id`, "
				+ "`is_active`, `is_delete` FROM `room_details` WHERE `building_id`=? AND `school_id`=?";
		List<Map<String, Object>> result = jdbcEducationTemplate.queryForList(sql, school_details_id);
		
		Map<String, Object> response = new HashMap<>();
		response.put("status", "Success");
		response.put("message", "Rooms List.");
		response.put("data", result);
		
		return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getParentQuestionsList(String school_details_id, String menu_id, 
			String building_id, String floors_id, String room_id, String qtype) {
        //String sql = "SELECT * FROM question_list WHERE isactive=1 AND `pid`=0 AND `qtype` LIKE ?";
        String sql = "SELECT  "
        		+ "    ql.id AS question_id, "
        		+ "    ql.q_english, "
        		+ "    ql.q_tamil, "
        		+ "    ql.isactive, "
        		+ "    ql.ismandatory, "
        		+ "    ql.img_required, "
        		+ "    ql.pid, "
        		+ "    ql.qtype, "
        		+ "    ql.feedbacktype, "
        		
        		+ "    ql.view_table_name, "
        		+ "    ql.view_field_name, "
        		+ "    ql.school_details_id, "
        		+ "    ql.building_id, "
        		+ "    ql.floors_id, "
        		+ "    ql.room_id, "
        		+ "    ql.othercond, "
        		+ "    ql.is_value_boolen, "
        		+ "    ql.parent_condition, "
        		
        		+ "    ql.feedback_column as return_name, "
        		+ "    CASE  "
        		+ "        WHEN (ql.feedbacktype = 'select' OR ql.feedbacktype = 'radio') THEN JSON_ARRAYAGG( "
        		+ "            JSON_OBJECT( "
        		+ "                'option_id', qov.id, "
        		+ "                'english_name', qov.english_name, "
        		+ "                'tamil_name', qov.tamil_name, "
        		+ "                'value', qov.value, "
        		+ "                'ex_function', qov.ex_function, "
        		//+ "                'isdelete', qov.isdelete "
        		+ "                'orderby', qov.orderby "
        		+ "            ) "
        		+ "        ) "
        		+ "        ELSE NULL "
        		+ "    END AS options "
        		+ "FROM  "
        		+ "    mobile_question_list ql "
        		+ "LEFT JOIN  "
        		+ "    mobile_q_option_value qov  "
        		+ "    ON qov.qid = ql.id "
        		+ "AND (ql.feedbacktype = 'select' OR ql.feedbacktype = 'radio') "
        		+ "AND (qov.isactive=1 AND qov.isdelete=0) "
        		+ "WHERE  "
        		+ "    ql.isactive = 1  "
        		+ "    AND ql.pid = 0  "
        		+ "    AND ql.mobile_category = ?  "
        		+ "    AND ql.qtype LIKE ?  "
        		+ "GROUP BY  "
        		+ "    ql.id  "
        		+ "ORDER BY ql.orderby";
        
        List<Map<String, Object>> result = jdbcEducationTemplate.queryForList(sql,menu_id, "%" + qtype + "%");
        
        ObjectMapper mapper = new ObjectMapper();
        //for (Map<String, Object> row : result) {
        Iterator<Map<String, Object>> iterator = result.iterator();

        while (iterator.hasNext()) {
            Map<String, Object> row = iterator.next();
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
            
            // Get Question value from HM Entry
            String tableName = (String) row.get("view_table_name");
            String fieldName = (String) row.get("view_field_name");
            String whereCond = (String) row.get("othercond");
            
            String excludeQuestionQuery = (String) row.get("parent_condition");
            System.out.println("parent_condition : "+excludeQuestionQuery);
            if (excludeQuestionQuery != null && !excludeQuestionQuery.trim().isEmpty()) {
            	try {
            		if (excludeQuestionQuery != null && excludeQuestionQuery.contains("+school_details_id+")) {
            		    excludeQuestionQuery = excludeQuestionQuery.replace("+school_details_id+", school_details_id);
            		}

            		System.out.println("Final SQL: " + excludeQuestionQuery);
            		
                    List<Map<String, Object>> excluderesult = jdbcEducationTemplate.queryForList(excludeQuestionQuery);
                    System.out.println("excluderesult : "+excluderesult);
                    if (excluderesult == null || excluderesult.isEmpty()) {
                        iterator.remove(); // ❗️ Remove the row if no result
                        continue;
                    }
                } catch (Exception e) {
                    // Handle or log the invalid SQL
                    System.out.println("Invalid SQL in excludeQuestionQuery: " + excludeQuestionQuery);
                    System.out.println("Error: " + e.getMessage());
                    iterator.remove(); // ❗️ Optionally remove if SQL is bad
                    continue;
                }
            }
            if (tableName != null && fieldName != null) {
                StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
                List<Object> params = new ArrayList<>();

                // Always check school_details_id (assuming it's always used)
                Boolean schoolIdFlag = row.get("school_details_id") instanceof Boolean && (Boolean) row.get("school_details_id");
                if (schoolIdFlag) {
                	String schoolIdVal = school_details_id;
                	if(schoolIdVal != null) {
                		if (tableName != null && tableName.contains("_feedback")) {
                			whereClause.append(" AND school_details_id = ?");
                		}
                		else {
                			whereClause.append(" AND school_id = ?");
                		}
                		params.add(schoolIdVal);
                	}
                }

                // Conditionally check and add based on boolean flags
                Boolean buildingIdFlag = row.get("building_id") instanceof Boolean && (Boolean) row.get("building_id");
                if (buildingIdFlag) {
                    String buildingIdVal = building_id;
                    if (buildingIdVal != null) {
                        whereClause.append(" AND building_id = ?");
                        params.add(buildingIdVal);
                    }
                }

                Boolean floorIdFlag = row.get("floors_id") instanceof Boolean && (Boolean) row.get("floors_id");
                if (floorIdFlag) {
                	String floorIdVal = floors_id;
                    if (floorIdVal != null) {
                        whereClause.append(" AND floor_id = ?");
                        params.add(floorIdVal);
                    }
                }

                Boolean roomIdFlag = row.get("room_id") instanceof Boolean && (Boolean) row.get("room_id");
                if (roomIdFlag) {
                	String roomIdVal = room_id;
                    if (roomIdVal != null) {
                        whereClause.append(" AND room_id = ?");
                        params.add(roomIdVal);
                    }
                }
                
                //whereClause.append("ORDER BY `id` DESC limit 1");
                
                String whereCondQuery ="";
                System.out.println("whereCond: "+whereCond);
                if (whereCond != null && !whereCond.trim().isEmpty()) {
                	whereCondQuery = " AND "+whereCond+"";
                }
                
                if (!params.isEmpty()) {
                    try {
                    	/*
                        String query = String.format("SELECT %s AS val FROM %s %s LIMIT 1",
                                fieldName, tableName, whereClause.toString());

                        String value = jdbcEducationTemplate.query(query, params.toArray(),
                                rs -> rs.next() ? rs.getString("val") : null);

                        row.put("q_display_value", value != null ? value : "");
                        */
                    	
                        String query = String.format("SELECT %s AS val FROM %s %s %s", fieldName, tableName, whereClause.toString(), whereCondQuery);

                        //List<String> values = jdbcEducationTemplate.query(query, params.toArray(), (rs, rowNum) -> rs.getString("val"));
                        List<String> values = jdbcEducationTemplate.query(query, params.toArray(), (rs, rowNum) -> {
                            String val = rs.getString("val");
                            if (fieldName.contains("_file")) {
                                return "https://gccservices.in/gccofficialapp/files" + val;
                            } else {
                                Boolean booleanFlag = row.get("is_value_boolen") instanceof Boolean && (Boolean) row.get("is_value_boolen");
                                System.out.println("is_value_boolen : "+row.get("is_value_boolen"));
                                System.out.println("Getting val : "+val);
                                if (booleanFlag) {
                                    if ("1".equals(val) || "true".equalsIgnoreCase(val)) {
                                        return "Yes";
                                    } else {
                                        return "No";
                                    }
                                } else {
                                    return val;
                                }
                            }
                        });

                        String joinedValues = String.join(",", values);

                        row.put("q_display_value", joinedValues.isEmpty() ? "" : joinedValues);
                        
                        
                        
                        // FOR ERROR CHECK 
                        String reconstructedQuery = query;
                        for (Object param : params) {
                            String paramStr = (param instanceof String) ? "'" + param + "'" : String.valueOf(param);
                            reconstructedQuery = reconstructedQuery.replaceFirst("\\?", paramStr);
                        }
                        System.out.println("Full SQL with values: " + reconstructedQuery);
                        
                    } catch (Exception e) {
                        row.put("q_display_value", "");
                        System.out.println("Edu Error : "+e.getMessage().toString());
                    }
                } else {
                    row.put("q_display_value", "");
                }
            } else {
                row.put("q_display_value", "");
            }
        }
        
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Parent Question List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getChildQuestionsList(String school_details_id, String menu_id, 
			String building_id, String floors_id, String room_id, String pid) {
		String sql = "SELECT  "
        		+ "    ql.id AS question_id, "
        		+ "    ql.q_english, "
        		+ "    ql.q_tamil, "
        		+ "    ql.isactive, "
        		+ "    ql.ismandatory, "
        		+ "    ql.img_required, "
        		+ "    ql.pid, "
        		+ "    ql.qtype, "
        		+ "    ql.feedbacktype, "
        		+ "    ql.feedback_column as return_name, "
        		+ "    CASE  "
        		+ "        WHEN (ql.feedbacktype = 'select' OR ql.feedbacktype = 'radio') THEN JSON_ARRAYAGG( "
        		+ "            JSON_OBJECT( "
        		+ "                'option_id', qov.id, "
        		+ "                'english_name', qov.english_name, "
        		+ "                'tamil_name', qov.tamil_name, "
        		+ "                'value', qov.value, "
        		//+ "                'isactive', qov.isactive, "
        		//+ "                'isdelete', qov.isdelete "
        		+ "                'orderby', qov.orderby "
        		+ "            ) "
        		+ "        ) "
        		+ "        ELSE NULL "
        		+ "    END AS options "
        		+ "FROM  "
        		+ "    mobile_question_list ql "
        		+ "LEFT JOIN  "
        		+ "    mobile_q_option_value qov  "
        		+ "    ON qov.qid = ql.id "
        		+ "AND (ql.feedbacktype = 'select' OR ql.feedbacktype = 'radio') "
        		+ "AND (qov.isactive=1 AND qov.isdelete=0) "
        		+ "WHERE  "
        		+ "    ql.isactive = 1  "
        		+ "    AND ql.pid = ?  "
        		+ "GROUP BY  "
        		+ "    ql.id  "
        		+ "ORDER BY ql.orderby";
        List<Map<String, Object>> result = jdbcEducationTemplate.queryForList(sql,pid);
        
        ObjectMapper mapper = new ObjectMapper();
        for (Map<String, Object> row : result) {
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
        
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Child Question List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	private String setValue(String school_details_id, String categoryId, MultipartFile value, String key) {
		String fileURL="";
		if (value != null && !value.isEmpty()) {
			fileURL = fileUpload(school_details_id+"_"+categoryId, key, value);
		}
		
		return fileURL;
	}
	/*
	@Transactional
	public List<Map<String, Object>> saveFeedback(
	        MultiValueMap<String, String> formData,
	        Map<String, MultipartFile> fileMap,
	        String school_details_id,
	        String building_id,
	        String floor_id,
	        String room_id,
	        String categoryId,
	        String zone, 
	        String division,
	        String latitude, 
	        String longitude,
	        String inby) {

	    Map<String, Object> response = new HashMap<>();
	    KeyHolder keyHolder = new GeneratedKeyHolder();
	    int lastInsertId = 0;

	    String tableName = categoryId + "_feedback"; // e.g., 1_feedback

	    List<String> columns = new ArrayList<>();
	    List<String> placeholders = new ArrayList<>();
	    List<Object> values = new ArrayList<>();

	    for (String key : formData.keySet()) {
	    	
	        columns.add("`" + key + "`");
	        placeholders.add("?");
	        values.add(formData.getFirst(key));

	        // Check for file matching this key
	        String fileKey = key + "_file";
	        
	        System.out.println("Field: "+ fileKey);
	        
	        if (fileMap.containsKey(fileKey)) {
	            columns.add("`" + fileKey + "`");
	            placeholders.add("?");
	            MultipartFile file = fileMap.get(fileKey);
	            String fileUrl = setValue(school_details_id, categoryId, file, fileKey);
	            System.out.println(fileUrl);
	            values.add(fileUrl);
	        }
	    }

	    String sql = String.format(
	        "INSERT INTO `%s` (%s) VALUES (%s)",
	        tableName,
	        String.join(", ", columns),
	        String.join(", ", placeholders)
	    );
	    System.out.println(sql);
	    try {
	        int affectedRows = jdbcEducationTemplate.update(connection -> {
	            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	            for (int i = 0; i < values.size(); i++) {
	                ps.setObject(i + 1, values.get(i));
	                System.out.println(i + "," + values.get(i));
	            }
	            return ps;
	        }, keyHolder);

	        if (affectedRows > 0) {
	            Number generatedId = keyHolder.getKey();
	            lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
	            response.put("insertId", lastInsertId);
	            response.put("status", "success");
	            response.put("message", "A new feedback was inserted successfully!");
	            System.out.println("A new feedback was inserted successfully! Insert ID: " + generatedId);
	        } else {
	            response.put("status", "error");
	            response.put("message", "Failed to insert a new feedback. School Details ID: " + school_details_id);
	        }

	    } catch (DataAccessException e) {
	        Throwable rootCause = e.getMostSpecificCause();
	        if (rootCause instanceof SQLException) {
	            SQLException sqlException = (SQLException) rootCause;
	            response.put("status", "error");
	            response.put("message", sqlException.getMessage());
	            response.put("sqlState", sqlException.getSQLState());
	            response.put("errorCode", sqlException.getErrorCode());
	        } else {
	            response.put("status", "error");
	            response.put("message", rootCause.getMessage());
	        }
	    }

	    return Collections.singletonList(response);
	}
*/
	@Transactional
	public List<Map<String, Object>> saveFeedback(
	        MultiValueMap<String, String> formData,
	        Map<String, MultipartFile> fileMap,
	        String school_details_id,
	        String building_id,
	        String floor_id,
	        String room_id,
	        String categoryId,
	        String zone,
	        String division,
	        String latitude,
	        String longitude,
	        String inby) {

	    Map<String, Object> response = new HashMap<>();
	    KeyHolder keyHolder = new GeneratedKeyHolder();
	    int lastInsertId = 0;

	    String tableName = categoryId + "_feedback"; // e.g., 1_feedback

	    List<String> columns = new ArrayList<>();
	    List<String> placeholders = new ArrayList<>();
	    List<Object> values = new ArrayList<>();

	    // 1. Handle formData (keys as-is)
	    for (String key : formData.keySet()) {
	        columns.add("`" + key + "`");
	        placeholders.add("?");
	        values.add(formData.getFirst(key));
	    }

	    // 2. Handle fileMap (keys as key_file)
	    for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
	        String originalKey = entry.getKey();
	        String columnName = originalKey; // already like fieldname_file
	        MultipartFile file = entry.getValue();

	        String fileUrl = setValue(school_details_id, categoryId, file, columnName);

	        columns.add("`" + columnName + "`");
	        placeholders.add("?");
	        values.add(fileUrl);
	    }

	    String sql = String.format(
	            "INSERT INTO `%s` (%s) VALUES (%s)",
	            tableName,
	            String.join(", ", columns),
	            String.join(", ", placeholders)
	    );
	    	System.out.println(sql);
	    try {
	        int affectedRows = jdbcEducationTemplate.update(connection -> {
	            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	            for (int i = 0; i < values.size(); i++) {
	                ps.setObject(i + 1, values.get(i));
	            }
	            return ps;
	        }, keyHolder);

	        if (affectedRows > 0) {
	            Number generatedId = keyHolder.getKey();
	            lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
	            response.put("insertId", lastInsertId);
	            response.put("status", "success");
	            response.put("message", "A new feedback was inserted successfully!");
	        } else {
	            response.put("status", "error");
	            response.put("message", "Failed to insert a new feedback. School Details ID: " + school_details_id);
	        }

	    } catch (DataAccessException e) {
	        Throwable rootCause = e.getMostSpecificCause();
	        if (rootCause instanceof SQLException) {
	            SQLException sqlException = (SQLException) rootCause;
	            response.put("status", "error");
	            response.put("message", sqlException.getMessage());
	            response.put("sqlState", sqlException.getSQLState());
	            response.put("errorCode", sqlException.getErrorCode());
	        } else {
	            response.put("status", "error");
	            response.put("message", rootCause.getMessage());
	        }
	    }

	    return Collections.singletonList(response);
	}
	
	/*	
	@Transactional
	public List<Map<String, Object>> saveFeedback(
			String school_id,
			String cby,
			String latitude,
			String longitude, 
			String zone, 
			String ward,
			String q_1,
			String q_2,
			String q_3,
			String q_4,
			String q_5,
			String q_6,
			String q_7,
			String q_8,
			String q_9,
			String q_10,
			String q_11,
			String remarks,
			MultipartFile file,
			
			MultipartFile file_1, 
			MultipartFile file_2, 
			MultipartFile file_3, 
			MultipartFile file_4, 
			MultipartFile file_5, 
			MultipartFile file_6, 
			MultipartFile file_7, 
			MultipartFile file_8, 
			MultipartFile file_9, 
			MultipartFile file_10, 
			MultipartFile file_11,
			String type) {
		List<Map<String, Object>> result = null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;

		String image = "";
		String image_q1 = "";
		String image_q2 = "";
		String image_q3 = "";
		String image_q4 = "";
		String image_q5 = "";
		String image_q6 = "";
		String image_q7 = "";
		String image_q8 = "";
		String image_q9 = "";
		String image_q10 = "";
		String image_q11 = "";
		
		var year = DateTimeUtil.getCurrentYear();
		var month = DateTimeUtil.getCurrentMonth();
		
		// Handle file upload if a file is provided
		if (file != null && !file.isEmpty()) {
			image = fileUpload("feedback", "0", file);
		}
		
		// Question Images
		if (file_1 != null && !file_1.isEmpty()) {
			image_q1 = fileUpload("1", "q1", file_1);
		}
		
		if (file_2 != null && !file_2.isEmpty()) {
			image_q2 = fileUpload("2", "q2", file_2);
		}
		
		if (file_3 != null && !file_3.isEmpty()) {
			image_q3 = fileUpload("3", "q3", file_3);
		}
		
		if (file_4 != null && !file_4.isEmpty()) {
			image_q4 = fileUpload("4", "q4", file_4);
		}
		
		if (file_5 != null && !file_5.isEmpty()) {
			image_q5 = fileUpload("5", "q5", file_5);
		}
		
		if (file_6 != null && !file_6.isEmpty()) {
			image_q6 = fileUpload("6", "q6", file_6);
		}
		
		if (file_7 != null && !file_7.isEmpty()) {
			image_q7 = fileUpload("7", "q7", file_7);
		}
		
		if (file_8 != null && !file_8.isEmpty()) {
			image_q8 = fileUpload("8", "q8", file_8);
		}
		
		if (file_9 != null && !file_9.isEmpty()) {
			image_q9 = fileUpload("9", "q9", file_9);
		}
		
		if (file_10 != null && !file_10.isEmpty()) {
			image_q10 = fileUpload("10", "q10", file_10);
		}
		
		if (file_11 != null && !file_11.isEmpty()) {
			image_q11 = fileUpload("11", "q11", file_11);
		}
		
		String feedbackimg = image;
		
		String q1_image = image_q1;
		String q2_image = image_q2;
		String q3_image = image_q3;
		String q4_image = image_q4;
		String q5_image = image_q5;
		String q6_image = image_q6;
		String q7_image = image_q7;
		String q8_image = image_q8;
		String q9_image = image_q9;
		String q10_image = image_q10;
		String q11_image = image_q11;
		
		//String type = // Get before & after
		
		// Get today's date
        LocalDate today = LocalDate.now();

        // Format it in the desired format (yyyy-MM-dd)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String todayDate = today.format(formatter);
        
		//inactiveFeedBack(type, shelter_id); // Upadte already insert data.
		
		String sqlQuery = "INSERT INTO `bus_shelter_feedback`("
				+ "`shelter_id`, `cby`, "
				+ "`q1`, `q2`, `q3`, `q4`, `q5`, `q6`, `q7`, `q8`, `q9`, `q10`, `q11`, "
				+ "`remarks`,`latitude`, `longitude`,`zone`,`ward`,`type`,`image`,"
				+ "`q1_image`, `q2_image`, `q3_image`, `q4_image`, `q5_image`, `q6_image`, "
				+ "`q7_image`, `q8_image`, `q9_image`, `q10_image`, `q11_image`"
				+ ") "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
			int affectedRows = jdbcEducationTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[] { "id" });
					ps.setString(1, shelter_id);
					ps.setString(2, cby);
					ps.setString(3, q_1);
					ps.setString(4, q_2);
					ps.setString(5, q_3);
					ps.setString(6, q_4);
					ps.setString(7, q_5);
					ps.setString(8, q_6);
					ps.setString(9, q_7);
					ps.setString(10, q_8);
					ps.setString(11, q_9);
					ps.setString(12, q_10);
					ps.setString(13, q_11);
					ps.setString(14, remarks);
					ps.setString(15, latitude);
					ps.setString(16, longitude);
					ps.setString(17, zone);
					ps.setString(18, ward);
					ps.setString(19, type);
					ps.setString(20, feedbackimg);
					
					ps.setString(21, q1_image);
					ps.setString(22, q2_image);
					ps.setString(23, q3_image);
					ps.setString(24, q4_image);
					ps.setString(25, q5_image);
					ps.setString(26, q6_image);
					ps.setString(27, q7_image);
					ps.setString(28, q8_image);
					ps.setString(29, q9_image);
					ps.setString(30, q10_image);
					ps.setString(31, q11_image);
					return ps;
				}
			}, keyHolder);

			if (affectedRows > 0) {
				Number generatedId = keyHolder.getKey();
				lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
				response.put("insertId", lastInsertId);
				response.put("status", "success");
				response.put("message", "A new feedback was inserted successfully!");
				System.out.println("A new feedback was inserted successfully! Insert ID: " + generatedId);
			} else {
				response.put("status", "error");
				response.put("message", "Failed to insert a new feedback. Asset ID:" + shelter_id);
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
	*/
}
