package in.gov.chennaicorporation.mobileservice.greenCommittee.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JacksonInject.Value;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service("greencommitteeservice")
public class GreenCommitteeService {
	private JdbcTemplate jdbcTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlGccGreenCommitteeSource") DataSource GreenCommitteeDataSource) {
		this.jdbcTemplate = new JdbcTemplate(GreenCommitteeDataSource);
	}
    
    @Autowired
	public GreenCommitteeService(Environment environment) {
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
		String serviceFolderName = environment.getProperty("greencommittee_foldername");
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
	
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getInspectionBy(String userid) {
		
		String sql =
	    	    "SELECT `mapid`, `userid`, `name`, `mobile`, `inspection_department`, "
	    	    + "`zone`, `ward`, `isactive` FROM `user_maping` WHERE userid = ? LIMIT 1";

	    List<Map<String, Object>> results =
	    	    jdbcTemplate.queryForList(sql,userid);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Inspection BY");
	    response.put("data", results);

	    return Collections.singletonList(response);
	}
	
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getInspectionDepartmentList() {
		
		String sql =
	    	    "SELECT * "
	    	    + "FROM inspection_department "
	    	    + "WHERE isactive = 1 AND isdelete = 0 "
	    	    + "ORDER BY `orderby`";

	    List<Map<String, Object>> results =
	    	    jdbcTemplate.queryForList(sql);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Inspection Department List");
	    response.put("data", results);

	    return Collections.singletonList(response);
	}
	
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getRecommendMasterList() {
		
		String sql =
	    	    "SELECT * "
	    	    + "FROM inspection_recommend_master "
	    	    + "WHERE isactive = 1 AND isdelete = 0 "
	    	    + "ORDER BY `orderby`";

	    List<Map<String, Object>> results =
	    	    jdbcTemplate.queryForList(sql);
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Recommend Master List");
	    response.put("data", results);

	    return Collections.singletonList(response);
	}
	
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getComplaintList(String userid) {

		String sql =
			    "SELECT " +
			    " rd.id AS reg_id, " +
			    " rd.ref_id, " +
			    " rd.p_name, " +
			    " rd.ph_no, " +
			    " rd.gender, " +
			    " rd.address, " +
			    " rd.latitude, " +
			    " rd.longitude, " +
			    " rd.zone, " +
			    " rd.ward, " +
			    " rd.total_trees, " +
			    " rd.dept_id, " +
			    " rd.dept_name, " +
			    " rd.govt_type, " +
			    " rd.designation, " +
			    " rd.remarks, " +
			    " rd.source, " +
			    " DATE_FORMAT(rd.cdate,'%d-%m-%Y %l:%i %p') AS reg_date, " +

			    " cd.id AS complaint_id, " +
			    " cd.no_of_trees, " +

			    " cn.id AS comp_nature_id, " +
			    " cn.name AS comp_nature_name, " +
			    " cn.category AS comp_category, " +

			    " COALESCE( " +
			    "   JSON_ARRAYAGG( " +
			    "     JSON_OBJECT( " +
			    "       'img_path', CONCAT(?, iu.img_path) " +
			    "     ) " +
			    "   ), JSON_ARRAY() " +
			    " ) AS image_paths " +

			    "FROM reg_details rd " +

			    "LEFT JOIN complaint_details cd " +
			    " ON cd.ref_id = rd.ref_id " +
			    " AND cd.is_active = 1 " +
			    " AND cd.is_delete = 0 " +

			    "LEFT JOIN comp_nature cn " +
			    " ON cn.id = cd.comp_nature_id " +
			    " AND cn.is_active = 1 " +
			    " AND cn.is_delete = 0 " +

			    "LEFT JOIN img_uploads iu " +
			    " ON iu.ref_id = rd.ref_id " +
			    " AND iu.is_active = 1 " +
			    " AND iu.is_delete = 0 " +
			    " AND iu.img_path IS NOT NULL " +

			    "WHERE rd.is_active = 1 " +
			    " AND rd.is_delete = 0 " +

			    "GROUP BY " +
			    " rd.id, rd.ref_id, rd.p_name, rd.ph_no, rd.gender, rd.address, " +
			    " rd.latitude, rd.longitude, rd.zone, rd.ward, rd.total_trees, " +
			    " rd.dept_id, rd.dept_name, rd.govt_type, rd.designation, " +
			    " rd.remarks, rd.source, rd.cdate, " +
			    " cd.id, cd.no_of_trees, " +
			    " cn.id, cn.name, cn.category";

	    List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, fileBaseUrl + "/gccofficialapp/files");

	    ObjectMapper mapper = new ObjectMapper();

	    for (Map<String, Object> row : results) {
	        Object imgObj = row.get("image_paths");
	        if (imgObj != null) {
	            try {
	                row.put(
	                    "image_paths",
	                    mapper.readValue(imgObj.toString(), List.class)
	                );
	            } catch (Exception e) {
	                // fallback to empty list if JSON parse fails
	                row.put("image_paths", Collections.emptyList());
	            }
	        }
	    }
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Complaint List");
	    response.put("data", results);

	    return Collections.singletonList(response);
	}

	@Transactional
	public List<Map<String, Object>> saveInspectionData(
	        String ref_id,
	        String inspection_by,
	        String visit_type,
	        String visit_by,
	        String remarks,
	        String zone,
	        String ward,
	        String street_name,
	        String street_id,
	        String latitude,
	        String longitude,
	        String inby,
	        MultipartFile file_1,
	        MultipartFile file_2,
	        MultipartFile file_3,
	        List<Map<String, Object>> treeList
	) {

		String filetxt = inby;
		
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    // For Image
	    String filetype = "image";
	    String action = "inspection";
	    
	    // for Image 1
	    String file1 = fileUpload(action, filetxt, file_1, filetype);
	    
	    if ("error".equalsIgnoreCase(file1)) {
	        response.put("status", "error");
	        response.put("message", "Image-1 insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    // for Image 2
	    String file2 = fileUpload(action, filetxt, file_2, filetype);
	    
	    if ("error".equalsIgnoreCase(file2)) {
	        response.put("status", "error");
	        response.put("message", "Image-2 insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    // for Image 3
	    String file3 = fileUpload(action, filetxt, file_3, filetype);
	    
	    if ("error".equalsIgnoreCase(file3)) {
	        response.put("status", "error");
	        response.put("message", "Image-3 insert failed.");
	        result.add(response);
	        return result;
	    }
	    
	    String insertSqltxt = "INSERT INTO `inspection_data` "
	    		+ "(`ref_id`, `inspection_by`, `file_1`, `file_2`, `file_3`, `visit_type`, `visit_by`, `remarks`, `cby`, "
	    		+ "`zone`, `ward`, `street_name`, `street_id`, `latitude`, `longitude`) "
                + "VALUES "
                + "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	    String insertSql = insertSqltxt;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"inspection_data_id"});
	        int i = 1;
	        ps.setString(i++, ref_id);
	        ps.setString(i++, inspection_by);
	        ps.setString(i++, file1);
	        ps.setString(i++, file2);
	        ps.setString(i++, file3);
	        
	        ps.setString(i++, visit_type);
	        ps.setString(i++, visit_by);
	        ps.setString(i++, remarks);
	        ps.setString(i++, inby);
	        ps.setString(i++, zone);
	        
	        ps.setString(i++, ward);
	        ps.setString(i++, street_name);
	        ps.setString(i++, street_id);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        for (Map<String, Object> tree : treeList) {
		        String tree_species_name=tree.get("tree_species_name").toString();
		        String tree_count=tree.get("tree_count").toString();
		        String work_status_id=tree.get("work_status_id").toString();
		        String work_status_name="";
		        
		        if(saveInspectionTreeData(lastInsertId, ref_id, inby, tree_species_name, tree_count, work_status_id, work_status_name)) {
		        	response.put("tree_inpection", action + " insertId: " +lastInsertId);
			        response.put("status", "success");
			        response.put("message", "Tree inpection data inserted successfully.");
		        }
		        else {
		        	response.put("tree_inpection", action + " error!");
			        response.put("status", "error");
			        response.put("message", "Tree inpection data insert failed.");
		        }
	        }
	    } else {
	    	response.put("tree_inpection", action + " error!");
	        response.put("status", "error");
	        response.put("message", "Tree inpection data insert failed.");
	    }
	
	    result.add(response);
	    return result;
	}
	
	//Add tree Inspection details
	public Boolean saveInspectionTreeData(
	        int inspection_data_id,
	        String ref_id,
	        String inby,
	        String treename,
	        String nooftrees,
	        String actionid,
	        String action
	) {

		String filetxt = inby;
		
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    String insertSqltxt = "INSERT INTO `inspection_tree_data` "
	    		+ "(`treename`, `nooftrees`, `action`, `inspection_data_id`, `ref_id`, `cby`) "
                + "VALUES "
                + "(?,?,?,?,?,?)";

	    String insertSql = insertSqltxt;
	    		
	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcTemplate.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"tid"});
	        int i = 1;
	        ps.setString(i++, treename);
	        ps.setString(i++, nooftrees);
	        ps.setString(i++, actionid);
	        ps.setInt(i++, inspection_data_id);
	        ps.setString(i++, ref_id);
	        ps.setString(i++, inby);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        return true;
	    } else {
	    	// Update on inspection_data to delete status
	    	String updateSQL = "UPDATE `inspection_data` SET isactive=0, isdelete=1 WHERE inspection_data_id=?";
	    	jdbcTemplate.update(updateSQL,inspection_data_id);
	    	
	    	updateSQL = "UPDATE `inspection_tree_data` SET isactive=0, isdelete=1 WHERE inspection_data_id=?";
	    	jdbcTemplate.update(updateSQL,inspection_data_id);
	    	
	    	return false;
	    } 
	}
	
}
