package in.gov.chennaicorporation.mobileservice.buildingDemolition.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
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
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class BuildingDemolitionActivity {
	private JdbcTemplate jdbcBuildingDemolitionTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlGccBuildingDemolitionSource") DataSource BuildingDemolitionDataSource) {
		this.jdbcBuildingDemolitionTemplate = new JdbcTemplate(BuildingDemolitionDataSource);
	}
    
    @Autowired
	public BuildingDemolitionActivity(Environment environment) {
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
        String serviceFolderName = environment.getProperty("buildingdemolition_foldername");
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
	
	public List<Map<String, Object>> getBuildingList(String aeUserId) {
		
		String sql = "SELECT zone, ward FROM gcc_penalty_hoardings.`hoading_user_list` WHERE userid = ? AND type='ae'";
        List<Map<String, Object>> userList = jdbcBuildingDemolitionTemplate.queryForList(sql, aeUserId);

        if (userList.isEmpty()) {
            // You can return an empty list or throw a custom exception
            return Collections.emptyList(); // or handle as needed
        }

        Map<String, Object> vendorData = userList.get(0);
        String ward = String.valueOf(vendorData.get("ward"));
        
         sql = "SELECT  "
        		+ "    bd.* "
        		+ "FROM building_demolition bd "
        		+ "LEFT JOIN building_demolition_status bds ON bd.id = bds.bdid "
        		+ "WHERE bds.bdid IS NULL "
        		+ "  AND bd.ward = ?";
        List<Map<String, Object>> result = jdbcBuildingDemolitionTemplate.queryForList(sql, ward);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Building Demolition List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> updateDetails(
			String id, 
			String status,
			String cby,
			String latitude,
			String longitude,
			String remarks,
			MultipartFile file) {
		
		String filepath = null;
		// 1. Fetch building details
	    String sql = "SELECT `id` as bdid, `council_resolution_date`, `year`, `zone`, `ward`, " +
	            "`description`, `category_of_building`, `area_sqm` " +
	            "FROM `building_demolition` WHERE id=?";

	    List<Map<String, Object>> result = jdbcBuildingDemolitionTemplate.queryForList(sql, id);

	    if (result.isEmpty()) {
	        Map<String, Object> response = new HashMap<>();
	        response.put("status", "Failed");
	        response.put("message", "No record found for id " + id);
	        return Collections.singletonList(response);
	    }
	    Map<String, Object> building = result.get(0);
	    
	    // 2. Check if record already exists in building_demolition_status
	    String checkSql = "SELECT COUNT(*) FROM `building_demolition_status` WHERE bdid = ?";
	    Integer count = jdbcBuildingDemolitionTemplate.queryForObject(checkSql, Integer.class, building.get("bdid"));

	    if (count != null && count > 0) {
	        Map<String, Object> response = new HashMap<>();
	        response.put("status", "Failed");
	        response.put("message", "Duplicate entry: Demolition status already exists for building id " + id);
	        return Collections.singletonList(response);
	    }
	    
	    // 2.1. Handle file upload (optional)
	    if (file != null && !file.isEmpty()) {
	    	filepath = fileUpload(id, cby, file);
		}
	    
	    // 3. Insert into building_demolition_status
	    String insertSql = "INSERT INTO `building_demolition_status` " +
	            "(`bdid`, `council_resolution_date`, `year`, `zone`, `ward`, `description`, " +
	            "`category_of_building`, `area_sqm`, `status`, `inby`, " +
	            " `file1`, `remarks`, `latitude`, `longitude`) " +
	            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	    jdbcBuildingDemolitionTemplate.update(insertSql,
	            building.get("bdid"),
	            building.get("council_resolution_date"),
	            building.get("year"),
	            building.get("zone"),
	            building.get("ward"),
	            building.get("description"),
	            building.get("category_of_building"),
	            building.get("area_sqm"),
	            status,
	            cby,
	            filepath,
	            remarks,
	            latitude,
	            longitude
	    );
	    
	 // 4. Build response
	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Building Demolition Status inserted successfully.");
	    response.put("data", building);
        
        return Collections.singletonList(response);
    }
	
	// Reports
	public List<Map<String, Object>> zoneReport() {
		String sqlQuery = "SELECT "
				+ "    bd.zone, "
				+ "    COUNT(bd.id) AS total_building, "
				+ "    SUM(CASE WHEN bds.bdid IS NULL THEN 1 ELSE 0 END) AS pending, "
				+ "    SUM(CASE WHEN bds.bdid IS NOT NULL THEN 1 ELSE 0 END) AS completed "
				+ "FROM building_demolition bd "
				+ "LEFT JOIN building_demolition_status bds ON bd.id = bds.bdid "
				+ "GROUP BY bd.zone "
				+ "ORDER BY bd.zone";

	    List<Map<String, Object>> result = jdbcBuildingDemolitionTemplate.queryForList(sqlQuery);
	    
		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
	    response.put("message", "Zone list report.");
	    response.put("data", result);
	    
	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> wardReport(String zone) {
		String sqlQuery = "SELECT  "
				+ "    bd.ward, "
				+ "    COUNT(bd.id) AS total_building, "
				+ "    SUM(CASE WHEN bds.bdid IS NULL THEN 1 ELSE 0 END) AS pending, "
				+ "    SUM(CASE WHEN bds.bdid IS NOT NULL THEN 1 ELSE 0 END) AS completed "
				+ "FROM building_demolition bd "
				+ "LEFT JOIN building_demolition_status bds ON bd.id = bds.bdid "
				+ "WHERE bd.zone = ? "
				+ "GROUP BY bd.ward "
				+ "ORDER BY bd.ward";

	    List<Map<String, Object>> result = jdbcBuildingDemolitionTemplate.queryForList(sqlQuery,zone);
	    
		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
	    response.put("message", "Ward list report.");
	    response.put("data", result);
	    
	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> pendingReport(String ward) {
		String sqlQuery = "SELECT  "
				+ "    bd.* "
				+ "FROM building_demolition bd "
				+ "LEFT JOIN building_demolition_status bds ON bd.id = bds.bdid "
				+ "WHERE bds.bdid IS NULL "
				+ "  AND bd.ward = ?";

	    List<Map<String, Object>> result = jdbcBuildingDemolitionTemplate.queryForList(sqlQuery,ward);
	    
		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
	    response.put("message", "Pending list report.");
	    response.put("data", result);
	    
	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> completedReport(String ward) {
		String sqlQuery = "SELECT  "
				+ "    bds.*,"
				+ "  CONCAT('" + fileBaseUrl + "/gccofficialapp/files', bds.file1) AS image_url "
				+ "FROM building_demolition bd "
				+ "INNER JOIN building_demolition_status bds ON bd.id = bds.bdid "
				+ "WHERE bd.ward = ?";

	    List<Map<String, Object>> result = jdbcBuildingDemolitionTemplate.queryForList(sqlQuery,ward);
	    
		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
	    response.put("message", "Completed list report.");
	    response.put("data", result);
	    
	    return Collections.singletonList(response);
	}
	
}
