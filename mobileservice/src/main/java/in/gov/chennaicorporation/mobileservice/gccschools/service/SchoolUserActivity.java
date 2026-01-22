package in.gov.chennaicorporation.mobileservice.gccschools.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.security.SecureRandom;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class SchoolUserActivity {
	
	private JdbcTemplate jdbcSchoolsTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
	@Autowired
	public void setDataSource(@Qualifier("mysqlGccSchoolsDataSource") DataSource schoolDataSource) {
		this.jdbcSchoolsTemplate = new JdbcTemplate(schoolDataSource);
	}
	
	@Autowired
	public SchoolUserActivity(Environment environment) {
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
        String serviceFolderName = environment.getProperty("gccschools_foldername");
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
	
	public List<Map<String, Object>> getSchoolList(String ward, String category_type) {
        String sql = "SELECT * FROM school_list WHERE `division` = ? AND `category_type`= ? AND `gcc_app_updated`=0";
        return jdbcSchoolsTemplate.queryForList(sql,ward,category_type);
    }
	
	public void saveSchool(String latitude, String longitude, MultipartFile photo, Integer schoolType, String schoolName, String earmark, String zone, String division, String userId) {
	    // Save or retrieve the school type ID based on schoolType
	    //Integer schoolTypeId = saveSchoolType(schoolType);
	    if (schoolType == null || schoolType<=0) {
	        throw new IllegalArgumentException("Invalid school type ID: " + schoolType);
	    }

	    // First, check if the school already exists based on the school name
	    String checkIfExistsSql = "SELECT id FROM school_details WHERE school_name = ?";

	    // Check if the school exists
	    List<Map<String, Object>> existingSchool = jdbcSchoolsTemplate.queryForList(checkIfExistsSql, schoolName);

	    // If the school exists, return the existing school ID
	    if (!existingSchool.isEmpty()) {
	    	throw new IllegalArgumentException("School Name Alerdy Exits : " + existingSchool.get(0).get("id"));
	    }
	    
	    // Save school details and retrieve the generated school_details_id
	    int schoolDetailsId = saveSchoolDetails(latitude, longitude, schoolType, schoolName, earmark, zone, division, userId);
	    
	    updateSchoolInactive(schoolName,schoolDetailsId);
	    
	    String photoUrl = null;
	    
	    // Handle file upload if a photo is provided
	    if (photo != null && !photo.isEmpty()) {
	        // Make sure to pass the correct type for the first argument
	    	String id = String.valueOf(schoolDetailsId);
	    	String sdid = String.valueOf(schoolDetailsId);
			photoUrl = fileUpload(id, sdid, photo); // Adjusted to use schoolDetailsId
	    }

	    // Save the photo URL in school_photos if the photo was uploaded
	    if (photoUrl != null) {
	        saveSchoolPhoto(schoolDetailsId, photoUrl);
	    }
	}

	private void updateSchoolInactive(String id, int schoolDetailsId) {
	    String sql = "UPDATE `school_list` SET `gcc_app_updated`=? WHERE `school_name`=?";
	    jdbcSchoolsTemplate.update(sql, schoolDetailsId, id);
	    
	    // For education Web (gcc_schools_web Database)
	    String sql_web = "UPDATE `gcc_schools_web`.`school_list` SET `gcc_app_updated`=? WHERE `school_name`=?";
	    jdbcSchoolsTemplate.update(sql_web, schoolDetailsId, id);
	}
	
	private int saveSchoolDetails(String latitude, String longitude, Integer schoolTypeId, String schoolName, String earmark, String zone, String division, String userId) {
		
			    
	    // SQL statement to insert school details
	    String sql = "INSERT INTO school_details (latitude, longitude, school_type, school_name, earmark, zone, division, user_id) " +
	                 "VALUES (?, ?, ?, ?, ?, ?, ?, ? )";

	    // Debugging
	    System.out.println("Executing SQL: " + sql);
	    System.out.println("Parameters: Latitude = " + latitude + ", Longitude = " + longitude + 
	                       " SchoolTypeId = " + schoolTypeId + 
	                       ", SchoolName = " + schoolName + ", Zone = " + zone + ", Division = " + division);

	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    // Execute the SQL statement
	    try {
	        jdbcSchoolsTemplate.update(connection -> {
	            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
	            ps.setString(1, latitude);
	            ps.setString(2, longitude);
	            ps.setInt(3, schoolTypeId); // Correctly saving the ID of scl_type
	            ps.setString(4, schoolName);
	            ps.setString(5, earmark);
	            ps.setString(6, zone);
	            ps.setString(7, division);
	            ps.setString(8, userId);
	            return ps;
	        }, keyHolder);

	    } catch (Exception e) {
	        System.err.println("Error during SQL execution: " + e.getMessage());
	        throw e; // Rethrow to be handled by the controller
	    }

	    return keyHolder.getKey().intValue(); // Return the generated school_details_id
	}


	private void saveSchoolPhoto(int schoolDetailsId, String photoPath) {
	    String sql = "INSERT INTO school_photos (photo_url, is_active, is_deleted, school_details_id) VALUES (?, 1, 0, ?)";

	    // Execute the SQL update to insert the photo information
	    jdbcSchoolsTemplate.update(sql, photoPath, schoolDetailsId);
	}
    
    ////feedback///////////////
	public void addCategory(String category, List<String> questions) {
	    // Check if the category already exists
	    String checkCategorySql = "SELECT id FROM category WHERE category = ? LIMIT 1";
	    List<Integer> existingCategoryIds = jdbcSchoolsTemplate.query(
	        checkCategorySql,
	        (rs, rowNum) -> rs.getInt("id"),
	        category
	    );
	    
	    int categoryId;
	    
	    if (existingCategoryIds.isEmpty()) {
	        // Category does not exist, insert the new category
	        String insertCategorySql = "INSERT INTO category (category, is_active, is_deleted) VALUES (?, ?, ?)";
	        KeyHolder keyHolder = new GeneratedKeyHolder();

	        jdbcSchoolsTemplate.update(connection -> {
	            PreparedStatement ps = connection.prepareStatement(insertCategorySql, new String[]{"id"});
	            ps.setString(1, category);
	            ps.setInt(2, 1); // is_active
	            ps.setInt(3, 0); // is_deleted
	            return ps;
	        }, keyHolder);
	        
	        // Get the auto-generated category ID
	        categoryId = keyHolder.getKey().intValue();
	    } else {
	        // Category already exists, get its ID
	        categoryId = existingCategoryIds.get(0);
	    }

	    // Insert the questions related to the category
	    for (String question : questions) {
	        String insertQuestionSql = "INSERT INTO category_question (category_questions, is_active, is_delete, category_id) VALUES (?, ?, ?, ?)";
	        jdbcSchoolsTemplate.update(insertQuestionSql, question, 1, 0, categoryId);
	    }
	}

	public Integer saveSchoolType(String schoolType) {
	    // Check if the school type already exists
	    List<Integer> existingIds = jdbcSchoolsTemplate.query(
	        "SELECT id FROM school_type WHERE scl_type = ? LIMIT 1", 
	        (rs, rowNum) -> rs.getInt("id"), 
	        schoolType // Passing schoolType as a parameter
	    );

	    // If the school type exists, return its ID
	    if (!existingIds.isEmpty()) {
	        return existingIds.get(0);
	    }

	    // Insert the new school type if not found
	    jdbcSchoolsTemplate.update(
	        "INSERT INTO school_type (scl_type) VALUES (?)",
	        schoolType // Insert the school type
	    );

	    // Retrieve the newly generated ID for the inserted school type
	    Integer newId = jdbcSchoolsTemplate.queryForObject(
	        "SELECT id FROM school_type WHERE scl_type = ? LIMIT 1", 
	        Integer.class, 
	        schoolType
	    );

	    // Return the new ID
	    return newId;
	}

	///////////////////////////////////////////////////////////////////////////////  
    public Map<String, Object> getCategoryWithQuestions(int categoryId) {
        // Query to get category details
        String categorySql = "SELECT id, category FROM category WHERE id = ?";
        Map<String, Object> category = jdbcSchoolsTemplate.queryForMap(categorySql, categoryId);

        // Query to get questions associated with the category
        String questionsSql = "SELECT category_questions FROM category_question WHERE category_id = ? AND is_active = 1 AND is_delete = 0";
        List<String> questions = jdbcSchoolsTemplate.queryForList(questionsSql, String.class, categoryId);

        // Add questions to the category details
        category.put("questions", questions);
        return category;
    }
    
    //////////////////////////////////////////////////////////////////////////
    
    public List<Map<String, Object>> getFeedbackByQuestionId(int catQuestionId) {
        String sql = "SELECT f.cat_question_id, f.feedback, f.feed_photo, q.category_questions " +
                     "FROM feedback f " +
                     "JOIN category_question q ON f.cat_question_id = q.id " +
                     "WHERE f.cat_question_id = ?";

        // Query the database and return the result
        return jdbcSchoolsTemplate.queryForList(sql, catQuestionId);
    }
    
    //////////////////////////////////////////////////////////////////////
    public List<Map<String, Object>> getSchoolsByLocationAndType(String latitude, String longitude) {
    	
    	//ac.school_type = 10 Cluster kitchen
        String sql = "SELECT  ac.id, ac.school_name,school_type FROM  school_details ac WHERE "
        		+ "((6371008.8 * acos(ROUND(cos(radians(?)) * cos(radians(ac.latitude)) * cos(radians(ac.longitude) - radians(?)) + sin(radians(?)) * sin(radians(ac.latitude)), 9))) < 500) "
        		+ " AND ac.school_type=10 GROUP BY ac.id, ac.school_name,ac.school_type ";
        	
        // Use latitude, longitude, and schoolType as positional parameters
        return jdbcSchoolsTemplate.queryForList(sql, new Object[]{latitude, longitude, latitude});
    }
	
	////////////////////////
		public void saveChennaiSchoolBoard(int categoryId, String newboardQa, String newboardAns, 
		        String schoolConstructQa, String schoolConstructAns, 
		        MultipartFile feedPhoto, int school_details_id, 
		        String zone, String division, double latitude,
		        double longitude, String user_id) {
		
			String feedPhotoPath = null;
			String feedref = generateRandomString(); //generate feedback reference text
			
			// Handle file upload if a file is provided
			if (feedPhoto != null && !feedPhoto.isEmpty()) {
				String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	feedPhotoPath = fileUpload(id, sdid, feedPhoto); // Adjusted to use schoolDetailsId
			}
			
			// Save feedback for both questions
			saveFeedbackDetails(categoryId, newboardQa, newboardAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
			saveFeedbackDetails(categoryId, schoolConstructQa, schoolConstructAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
			
			// If a photo was uploaded, save it in category_photos
			if (feedPhotoPath != null) {
				String qid="1";
				saveCategoryPhoto(categoryId, feedPhotoPath, qid, feedref);
			}
		}
		
		////////////////////////////////////////////////////
		public void savewaterfd(
		        int categoryId,
		        String MetroWaterFacilitiesQa,
		        String MetroWaterFacilitiesAns,
		        String SumpFacilitiesAvailableQa,
		        String SumpFacilitiesAvailableAns,
		        String SumpGoodConditionQa,
		        String SumpGoodConditionAns,
		        String ROFacilitiesAvailableQa,
		        String ROFacilitiesAvailableAns,
		        String ROGoodConditionQa,
		        String ROGoodConditionAns,
		        String DrinkingwaterfacilitiesQa,
		        String DrinkingwaterfacilitiesAns,
		        String typeoffacilitiesQa,
		        String typeoffacilitiesAns,
		        
		        MultipartFile feedPhoto, // feedPhoto 
		        MultipartFile storageofdrinkingPhoto, // storageofdrinkingPhoto 
		        MultipartFile roPhoto, // roPhoto 
		        
		        int school_details_id,
		        String zone,
		        String division ,
		        double latitude  ,
		        double longitude ,
		        String user_id) {

		    String feedPhotoPath = null;
		    String storageofdrinkingPhotoPath = null;
		    String roPhotoPath = null;
		    
		    String feedref = generateRandomString(); //generate feedback reference text
		    
		    // Handle file upload if a file is provided
		    if (feedPhoto != null && !feedPhoto.isEmpty()) {
		    	String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	feedPhotoPath = fileUpload(id, sdid, feedPhoto); // Adjusted to use schoolDetailsId
		        
		    }
		    
		    // Handle file upload if a file is provided
		    if (storageofdrinkingPhoto != null && !storageofdrinkingPhoto.isEmpty()) {
		    	String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	storageofdrinkingPhotoPath = fileUpload(id, sdid, storageofdrinkingPhoto); // Adjusted to use schoolDetailsId
		        
		    }
		    
		    // Handle file upload if a file is provided
		    if (roPhoto != null && !roPhoto.isEmpty()) {
		    	String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	roPhotoPath = fileUpload(id, sdid, roPhoto); // Adjusted to use schoolDetailsId
		        
		    }

		    // Save feedback for each question and answer pair
		    saveFeedbackDetails(categoryId, MetroWaterFacilitiesQa, MetroWaterFacilitiesAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, SumpFacilitiesAvailableQa, SumpFacilitiesAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, SumpGoodConditionQa, SumpGoodConditionAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, ROFacilitiesAvailableQa, ROFacilitiesAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, ROGoodConditionQa, ROGoodConditionAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, DrinkingwaterfacilitiesQa, DrinkingwaterfacilitiesAns	, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    
		    saveFeedbackDetails(categoryId, typeoffacilitiesQa, typeoffacilitiesAns	, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    
		    // If a photo was uploaded, save it in the category_photos table
		    if (feedPhotoPath != null) {
		    	String qid = "33";
		        saveCategoryPhoto(categoryId, feedPhotoPath, qid, feedref);
		    }
		    
		    // If a photo was uploaded, save it in the category_photos table
		    if (storageofdrinkingPhotoPath != null) {
		    	String qid = "5";
		        saveCategoryPhoto(categoryId, storageofdrinkingPhotoPath, qid, feedref);
		    }
		    
		    // If a photo was uploaded, save it in the category_photos table
		    if (roPhotoPath != null) {
		    	String qid = "7";
		        saveCategoryPhoto(categoryId, roPhotoPath, qid, feedref);
		    }
		}
		
		/////////////////////////////////////////////////
		
		public void saveFirstAidKitFeedback(
		        int categoryId,
		        String FirstAidKitAvailableQa,
		        String FirstAidKitAvailableAns,
		        MultipartFile feedPhoto,
		        int school_details_id,
		        String zone,
		        String division  ,
		        double latitude,
		        double longitude ,
		        String user_id) {

		    String feedPhotoPath = null;

		    // Handle file upload if a file is provided
		    if (feedPhoto != null && !feedPhoto.isEmpty()) {
		    	String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	feedPhotoPath = fileUpload(id, sdid, feedPhoto); // Adjusted to use schoolDetailsId
		        //feedPhotoPath = assetFileUpload(categoryId, feedPhoto, school_details_id);
		    }

		    String feedref = generateRandomString(); //generate feedback reference text
		    // Save feedback for the First Aid Kit question and answer pair
		    saveFeedbackDetails(categoryId, FirstAidKitAvailableQa, FirstAidKitAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);

		    // If a photo was uploaded, save it in the category_photos table
		    if (feedPhotoPath != null) {
		    	String qid="8";
		        saveCategoryPhoto(categoryId, feedPhotoPath, qid, feedref);
		    }
		}
		//////////////////////////////////////////////////////////////
		public void saveCleaningFeedback(
		        int categoryId,
		        String ToiletsAvailableQa,
		        String ToiletsAvailableAns,
		        String UrinariesAvailableQa,
		        String UrinariesAvailableAns,
		        String SweepersAvailableQa,
		        String SweepersAvailableAns,
		        String SewagesAvailableQa,
		        String SewagesAvailableAns,
		        String RoomsAvailableQa,
		        String RoomsAvailableAns,
		        String PlaygroundAvailableQa,
		        String PlaygroundAvailableAns,
		        String DrainageAvailableQa,
		        String DrainageAvailableAns,
		        MultipartFile toiletfeedPhoto,
		        MultipartFile RoomsfeedPhoto,
		        MultipartFile PlaygroundfeedPhoto,
		        int school_details_id,
		        String zone,
		        String division,
		        double latitude,
		        double longitude, 
		        String user_id) {

		    // Handle photo uploads (toilet, room, and playground photos)
		    String toiletPhotoPath = null;
		    String roomsPhotoPath = null;
		    String playgroundPhotoPath = null;

		    // If each photo is present, upload and get the path
		    if (toiletfeedPhoto != null && !toiletfeedPhoto.isEmpty()) {
		    	String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	toiletPhotoPath = fileUpload(id, sdid, toiletfeedPhoto); // Adjusted to use schoolDetailsId
		        //toiletPhotoPath = assetFileUpload(categoryId, toiletfeedPhoto, school_details_id);
		    }

		    if (RoomsfeedPhoto != null && !RoomsfeedPhoto.isEmpty()) {
		    	String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	roomsPhotoPath = fileUpload(id, sdid, RoomsfeedPhoto); // Adjusted to use schoolDetailsId
		        //roomsPhotoPath = assetFileUpload(categoryId, RoomsfeedPhoto, school_details_id);
		    }

		    if (PlaygroundfeedPhoto != null && !PlaygroundfeedPhoto.isEmpty()) {
		    	String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	playgroundPhotoPath = fileUpload(id, sdid, PlaygroundfeedPhoto); // Adjusted to use schoolDetailsId
		        //playgroundPhotoPath = assetFileUpload(categoryId, PlaygroundfeedPhoto, school_details_id);
		    }

		    String feedref = generateRandomString(); //generate feedback reference text
		    // Save feedback for each question and answer pair
		    saveFeedbackDetails(categoryId, ToiletsAvailableQa, ToiletsAvailableAns, school_details_id, zone, division, latitude, longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, UrinariesAvailableQa, UrinariesAvailableAns, school_details_id, zone, division, latitude, longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, SweepersAvailableQa, SweepersAvailableAns, school_details_id, zone, division, latitude, longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, SewagesAvailableQa, SewagesAvailableAns, school_details_id, zone, division, latitude, longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, RoomsAvailableQa, RoomsAvailableAns, school_details_id, zone, division, latitude, longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, PlaygroundAvailableQa, PlaygroundAvailableAns, school_details_id, zone, division, latitude, longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, DrainageAvailableQa, DrainageAvailableAns, school_details_id, zone, division, latitude, longitude,user_id,feedref);

		    // If photos were uploaded, save them in the category_photos table
		    if (toiletPhotoPath != null) {
		    	String qid="9";
		        saveCategoryPhoto(categoryId, toiletPhotoPath, qid, feedref);
		    }
		    if (roomsPhotoPath != null) {
		    	String qid="13";
		        saveCategoryPhoto(categoryId, roomsPhotoPath, qid, feedref);
		    }
		    if (playgroundPhotoPath != null) {
		    	String qid="14";
		        saveCategoryPhoto(categoryId, playgroundPhotoPath, qid, feedref);
		    }
		}


////////////////////////////////////////
		public void saveNoonMealFeedback(
		        int categoryId,
		        String NoonMealKitchenAvailableQa,
		        String NoonMealKitchenAvailableAns,
		        String GasStoveQa,
		        String GasStoveAns,
		        String StoreRoomAvailableQa,
		        String StoreRoomAvailableAns,
		        String UtensilsAvailableQa,
		        String UtensilsAvailableAns,
		        String SufficientNumberQa,
		        String SufficientNumberAns,
		        String BlockageInWashAreaQa,
		        String BlockageInWashAreaAns,
		        String SeepageQa,
		        String SeepageAns,
		        MultipartFile feedPhoto,
		        MultipartFile utensilsPhoto,
		        MultipartFile blockagePhoto,
		        MultipartFile seepagePhoto,
		        int school_details_id,
		        String zone,
		        String division ,
		        double latitude ,
		        double longitude,
		        String user_id) {

		    String feedPhotoPath = null;
		    String utensilsPhotoPath = null;
		    String blockagePhotoPath = null;
		    String seepagePhotoPath = null;

		    // Handle file upload if a file is provided
		    if (feedPhoto != null && !feedPhoto.isEmpty()) {
		    	String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	feedPhotoPath = fileUpload(id, sdid, feedPhoto); // Adjusted to use schoolDetailsId
		        //feedPhotoPath = assetFileUpload(categoryId, feedPhoto, school_details_id);
		    }
		    
		    // Handle file upload if a file is provided
		    if (utensilsPhoto != null && !utensilsPhoto.isEmpty()) {
		    	String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	utensilsPhotoPath = fileUpload(id, sdid, utensilsPhoto); // Adjusted to use schoolDetailsId
		    }
		    
		    // Handle file upload if a file is provided
		    if (blockagePhoto != null && !blockagePhoto.isEmpty()) {
		    	String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	blockagePhotoPath = fileUpload(id, sdid, blockagePhoto); // Adjusted to use schoolDetailsId
		    }
		    
		    // Handle file upload if a file is provided
		    if (seepagePhoto != null && !seepagePhoto.isEmpty()) {
		    	String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	seepagePhotoPath = fileUpload(id, sdid, seepagePhoto); // Adjusted to use schoolDetailsId
		    }
		    
		    String feedref = generateRandomString(); //generate feedback reference text
		    // Save feedback for each question and answer pair
		    saveFeedbackDetails(categoryId, NoonMealKitchenAvailableQa, NoonMealKitchenAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, GasStoveQa, GasStoveAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, StoreRoomAvailableQa, StoreRoomAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, UtensilsAvailableQa, UtensilsAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, SufficientNumberQa, SufficientNumberAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, BlockageInWashAreaQa, BlockageInWashAreaAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, SeepageQa, SeepageAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);

		    // If a photo was uploaded, save it in the category_photos table
		    if (feedPhotoPath != null) {
		    	String qid="16";
		        saveCategoryPhoto(categoryId, feedPhotoPath, qid, feedref);
		    }
		    // If a photo was uploaded, save it in the category_photos table
		    if (utensilsPhotoPath != null) {
		    	String qid="19";
		        saveCategoryPhoto(categoryId, utensilsPhotoPath, qid, feedref);
		    }
		    // If a photo was uploaded, save it in the category_photos table
		    if (blockagePhotoPath != null) {
		    	String qid="21";
		        saveCategoryPhoto(categoryId, blockagePhotoPath, qid, feedref);
		    }
		    // If a photo was uploaded, save it in the category_photos table
		    if (seepagePhotoPath != null) {
		    	String qid="22";
		        saveCategoryPhoto(categoryId, seepagePhotoPath, qid, feedref);
		    }
		}
		////////////////////////////////////////////////////////////
		public void saveElectricityFeedback(
		        int categoryId,
		        String FansAvailableQa,
		        String FansAvailableAns,
		        String FansNotAvailableQa,
		        String FansNotAvailableAns,
		        String LightsAvailableQa,
		        String LightsAvailableAns,
		        String LightsNotAvailableQa,
		        String LightsNotAvailableAns,
		        String SwitchBoardQa,
		        String SwitchBoardAns,
		        String GenSetQa,
		        String GenSetAns,
		        String GenSetconditionQa,
		        String GenSetconditionAns,
		        MultipartFile feedPhoto,
		        int school_details_id,
		        String zone,
		        String division,
		        double latitude ,
		        double longitude,
		        String user_id) {

		    String feedPhotoPath = null;

		    // Handle file upload if a file is provided
		    if (feedPhoto != null && !feedPhoto.isEmpty()) {
		    	String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	feedPhotoPath = fileUpload(id, sdid, feedPhoto); // Adjusted to use schoolDetailsId
		        //feedPhotoPath = assetFileUpload(categoryId, feedPhoto, school_details_id);
		    }

		    String feedref = generateRandomString(); //generate feedback reference text
		    // Save feedback for each question and answer pair
		    saveFeedbackDetails(categoryId, FansAvailableQa, FansAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, FansNotAvailableQa, FansNotAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, LightsAvailableQa, LightsAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, LightsNotAvailableQa, LightsNotAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, SwitchBoardQa, SwitchBoardAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, GenSetQa, GenSetAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, GenSetconditionQa, GenSetconditionAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);


		    // If a photo was uploaded, save it in the category_photos table
		    if (feedPhotoPath != null) {
		    	String qid="26";
		        saveCategoryPhoto(categoryId, feedPhotoPath, qid, feedref);
		    }
		}
/////////////////////////////////////////////
		public void saveITAssetsFeedback(
		        int categoryId,
		        String SystemAvailableQa,
		        String SystemAvailableAns,
		        String SystemNotAvailableQa,
		        String SystemNotAvailableAns,
		        String PrinterAvailableQa,
		        String PrinterAvailableAns,
		        String PrinterNotAvailableQa,
		        String PrinterNotAvailableAns,
		        String UPSAvailableQa,
		        String UPSAvailableAns,
		        String UPSNotAvailableQa,
		        String UPSNotAvailableAns,
		        String CCTVAvailableQa,
		        String CCTVAvailableAns,
		        String CCTVNotAvailableQa,
		        String CCTVNotAvailableAns,
		        String NetworkAvailableQa,
		        String NetworkAvailableAns,
		        String projectorgoodconditionQa, 
		        String projectorgoodconditionAns,
                String smartboardgoodconditionQa, 
                String smartboardgoodconditionAns,
		        MultipartFile feedPhoto,
		        int school_details_id,
		        String zone,
		        String division,
		        double latitude,
		        double longitude,
		        String user_id) {

		    String feedPhotoPath = null;

		    // Handle file upload if a file is provided
		    if (feedPhoto != null && !feedPhoto.isEmpty()) {
		    	String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	feedPhotoPath = fileUpload(id, sdid, feedPhoto); // Adjusted to use schoolDetailsId
		        //feedPhotoPath = assetFileUpload(categoryId, feedPhoto, school_details_id);
		    }

		    String feedref = generateRandomString(); //generate feedback reference text
		    
		    // Save feedback for each question and answer pair
		    saveFeedbackDetails(categoryId, SystemAvailableQa, SystemAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, SystemNotAvailableQa, SystemNotAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, PrinterAvailableQa, PrinterAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, PrinterNotAvailableQa, PrinterNotAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, UPSAvailableQa, UPSAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, UPSNotAvailableQa, UPSNotAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, NetworkAvailableQa, NetworkAvailableAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, projectorgoodconditionQa, projectorgoodconditionAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, smartboardgoodconditionQa, smartboardgoodconditionAns, school_details_id, zone, division,latitude,longitude,user_id,feedref);

		    // If a photo was uploaded, save it in the category_photos table
		    if (feedPhotoPath != null) {
		    	//String qid="8";
		        //saveCategoryPhoto(categoryId, feedPhotoPath, qid);
		    }
		}
/////////////////////////////////////
		public void saveSeepageSchoolFeedback(
		        int categoryId,
		        String SeepageinSchoolQa,
		        String SeepageinSchoolAns,
		        String HowmanyLocationsQa,
		        String HowmanyLocationsAns,
		        MultipartFile seepageinPhoto,
		        int school_details_id,
		        String zone,
		        String division,
		        double latitude ,
		        double longitude,
		        String user_id) { // Add latitude parameter

			String seepageinPhotoPath = null;
			
			if (seepageinPhoto != null && !seepageinPhoto.isEmpty()) {
		    	String id = String.valueOf(categoryId);
		    	String sdid = String.valueOf(school_details_id);
		    	seepageinPhotoPath = fileUpload(id, sdid, seepageinPhoto); // Adjusted to use schoolDetailsId
		    }
			
			String feedref = generateRandomString(); //generate feedback reference text
		    // Save feedback for each question and answer pair
		    saveFeedbackDetails(categoryId, SeepageinSchoolQa, SeepageinSchoolAns, school_details_id, zone, division, latitude,longitude,user_id,feedref);
		    saveFeedbackDetails(categoryId, HowmanyLocationsQa, HowmanyLocationsAns, school_details_id, zone, division, latitude,longitude,user_id,feedref);
		
		    // If a photo was uploaded, save it in the category_photos table
		    if (seepageinPhotoPath != null) {
		    	String qid="32";
		        saveCategoryPhoto(categoryId,seepageinPhotoPath,qid,feedref);
		    }
		    
		}

	private void saveFeedbackDetails(
			int categoryId, String qid, String feedback, 
			int school_details_id, String zone, String division, 
			double latitude,double longitude,String user_id,
			String feedref) {
		String sql = "INSERT INTO feedback (cat_question_id, feedback, school_details_id, "
				+ "zone, division, latitude, longitude, user_id, qid, feedref) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		// Execute SQL query to save feedback for the provided question
		jdbcSchoolsTemplate.update(sql, categoryId, feedback, school_details_id, zone, division, latitude, longitude, user_id, qid, feedref);
		
		isComplaintRequired(""+categoryId, qid, feedref, ""+school_details_id, user_id, feedback);
		
	} 
	
	private Boolean isComplaintRequired(String categoryId, String qid, String feedRef, String school_details_id, String user_id,String feedback) {
		String sqlQuery="SELECT `category_questions`,`positive_ans`,`input_type` FROM `category_question` WHERE `id`= ? AND `risecompliance`=1 AND `is_active`=1";
		List<Map<String, Object>> result = jdbcSchoolsTemplate.queryForList(sqlQuery, qid);

        if (result.isEmpty()) {
            return false;
        }

        // Build response map
        String complaint_txt ="";
        Map<String, Object> firstRow = result.get(0);
        
        complaint_txt = firstRow.get("category_questions").toString();
        String positive_ans = firstRow.get("positive_ans").toString();
        String input_type = firstRow.get("input_type").toString();
        
        complaint_txt = complaint_txt + " : " + feedback;
        
        if(input_type.equals("radio")) {
        	
        	if(feedback != null && !feedback.trim().isEmpty() && !positive_ans.equals(feedback)) {
        		System.out.println("Radio:" + feedback + "Qid : " + qid);
        		createComplaint(categoryId, qid, feedRef, school_details_id, user_id, complaint_txt, feedback);
        	}
        }
        
        if(input_type.equals("input_count")) {
        	
        	if(feedback != null && !feedback.trim().isEmpty() && !positive_ans.equals(feedback)) {
        		System.out.println("input_count:" + feedback + "Qid : " + qid);
        		createComplaint(categoryId, qid, feedRef, school_details_id, user_id, complaint_txt, feedback);
        	}
        }
	    return true;
	}
	
	private void saveCategoryPhoto(int categoryId, String photoPath, String qid, String feedRef) {
		String sql = "INSERT INTO category_photos "
				+ "(photo_url, is_active, is_deleted, category_id, qid, feedref) VALUES (?, ?, ?, ?, ?, ?)";
		
		// Execute SQL query to save the photo path related to the category
		jdbcSchoolsTemplate.update(sql, photoPath, 1, 0, categoryId, qid, feedRef);
	}
	
	
	/// Complaints
	
	private void createComplaint(String categoryId, String qid, String feedRef, String school_details_id, String user_id, String complaint_txt, String feedback) {
		System.out.println("Complaint : " + complaint_txt);
		String sql = "INSERT INTO `complaints`(`cid`, `qid`, `feedref`, `school_details_id`, `user_id`, `complaint_txt`, `feedback`) "
				+ "VALUES (?,?,?,?,?,?,?)";
		
		// Execute SQL query to save the photo path related to the category
		jdbcSchoolsTemplate.update(sql, categoryId, qid, feedRef, school_details_id, user_id, complaint_txt,feedback);
	}
	
	public void saveComplaintReplay(String complaintId, String user_id, String replay, MultipartFile cPhoto) {
		String filePhotoPath="";
		// Handle file upload if a photo is provided
	    if (cPhoto != null && !cPhoto.isEmpty()) {
	        // Make sure to pass the correct type for the first argument
	    	String id = String.valueOf(complaintId);
	    	String sdid = String.valueOf(user_id);
	    	filePhotoPath = fileUpload("reply_"+id, sdid, cPhoto); // Adjusted to use schoolDetailsId
	    }
		
		String sql = "INSERT INTO `complaints_reply`(`comid`, `replay`, `file`, `cby`) "
				+ "VALUES (?,?,?,?)";
		
		// Execute SQL query to save the photo path related to the category
		jdbcSchoolsTemplate.update(sql, complaintId, replay, filePhotoPath, user_id);
		
		updateComplaintStatus(complaintId);
	}
	
	private void updateComplaintStatus(String complaintId) {
		String sql = "UPDATE `complaints` SET `status`='close' WHERE id = ?";
		// Execute SQL query to save the photo path related to the category
		jdbcSchoolsTemplate.update(sql,complaintId );
	}
	////////////////////////////
		
   public List<Map<String, Object>> getAllSubCategories() {
        String sql = "SELECT * FROM sub_category";
        return jdbcSchoolsTemplate.queryForList(sql);
    }
   
   public Map<String, Object> getSchoolTypeById(int id) {
        String sql = "SELECT * FROM school_type WHERE id = ?";
        return jdbcSchoolsTemplate.queryForMap(sql, id);
    }
   
    public List<Map<String, Object>> getAllSchoolTypes() {
    	String sql = "SELECT * FROM school_type";
    	return jdbcSchoolsTemplate.queryForList(sql);
	}
    
    public List<Map<String, Object>> getFeedbackCountByCategoryAndDate(int categoryId, LocalDate startDate, LocalDate endDate,String user_id) {
        String sql = "SELECT fb.zone ,COUNT(DISTINCT fb.cdate) AS feedback_count FROM "
        		+ "feedback fb WHERE fb.cat_question_id = ? AND DATE(fb.cdate) BETWEEN ? AND ? "
        		+ "GROUP BY fb.zone;";

        return jdbcSchoolsTemplate.queryForList(sql, new Object[]{categoryId, startDate, endDate});
    }

    ////////////////////////////////
    public List<Map<String, Object>> getFeedbackCountByDivision(String division) {
        String sql = "SELECT fd.division, COUNT(fd.feedback) AS feedback_count " +
                     "FROM feedback fd " +
                     "WHERE fd.division = ? " +
                     "GROUP BY fd.division";

        return jdbcSchoolsTemplate.queryForList(sql, new Object[]{division});
    }
    
    public List<Map<String, Object>> getFeedbackCountByDivisionAndSchool(String division, String categoryId, LocalDate fromDate, LocalDate toDate,String user_id) {
        String sql = "SELECT sd.*, fb.zone,fb.division,COUNT(DISTINCT fb.cdate) AS feedback_count FROM feedback fb, "
        		+ "school_details sd WHERE fb.cat_question_id = ? AND (DATE(fb.cdate) "
        		+ "BETWEEN ? AND ?) AND "
        		+ "(fb.division=?) AND "
        		+ "(sd.id=fb.school_details_id) "
        		+ "GROUP BY fb.zone,fb.division,sd.school_name";

        return jdbcSchoolsTemplate.queryForList(sql, new Object[]{categoryId, fromDate, toDate, division});
    }
    
    public List<Map<String, Object>> getFeedbackCountByDivisionAndSchoolList(String division, String schoolId, String categoryId, LocalDate fromDate, LocalDate toDate,String user_id) {
        String sql = "SELECT sd.`id`, sd.`latitude`, sd.`longitude`, sd.`school_type`, sd.`school_name`, st.scl_type, "
        		+ "fb.zone, fb.division, COUNT(DISTINCT fb.cdate) AS feedback_count, "
        		+ "DATE_FORMAT(fb.cdate, '%d-%m-%Y %r') AS formatted_date, "
        		+ "DATE_FORMAT(fb.cdate, '%d-%m-%Y') AS fdate, "
        		+ "DATE_FORMAT(fb.cdate, '%r') AS ftime, "
        		+ "fb.user_id as activityby, "
        		+ "fb.`feedref` "
        		+ "FROM feedback fb, school_details sd, school_type st WHERE "
        		+ "fb.cat_question_id = ? AND (DATE(fb.cdate) BETWEEN ? AND ?) "
        		+ "AND (fb.school_details_id = ? AND fb.division = ?) "
        		+ "AND (sd.id=fb.school_details_id) "
        		+ "AND (st.id=sd.school_type) "
        		+ "GROUP BY fb.zone,fb.division,fb.`feedref` "
        		+ "ORDER BY formatted_date DESC";

        return jdbcSchoolsTemplate.queryForList(sql, new Object[]{categoryId, fromDate, toDate, schoolId, division});
    }
    
    public List<Map<String, Object>> getFeedbackCountSchoolListByUserId(String categoryId, LocalDate fromDate, LocalDate toDate,String user_id) {
        String sql = "SELECT sd.`id`, sd.`latitude`, sd.`longitude`, sd.`school_type`, sd.`school_name`, "
        		+ "       st.scl_type, fb.zone, fb.division, COUNT(DISTINCT fb.cdate) AS feedback_count, "
        		+ "       DATE_FORMAT(fb.cdate, '%d-%m-%Y %r') AS formatted_date, "
        		+ "       DATE_FORMAT(fb.cdate, '%d-%m-%Y') AS fdate, "
        		+ "       DATE_FORMAT(fb.cdate, '%r') AS ftime, "
        		+ "       fb.user_id as activityby, fb.`feedref` "
        		+ "FROM feedback fb "
        		+ "JOIN school_details sd ON sd.id = fb.school_details_id "
        		+ "JOIN school_type st ON st.id = sd.school_type "
        		+ "WHERE fb.cat_question_id = ? "
        		+ "  AND (DATE(fb.cdate) BETWEEN ? AND ?) "
        		+ "  AND fb.user_id = ? "
        		+ "GROUP BY fb.zone, fb.division, fb.`feedref` "
        		+ "ORDER BY formatted_date DESC;";

        return jdbcSchoolsTemplate.queryForList(sql, new Object[]{categoryId, fromDate, toDate, user_id});
    }
    
    public List<Map<String, Object>> getFeedbackCountByZoneCat(String zone, int categoryId, LocalDate fromDate, LocalDate toDate,String user_id) {
        String sql = "SELECT fb.zone,fb.division,COUNT(DISTINCT fb.cdate) AS feedback_count "
        		+ "FROM feedback fb WHERE fb.cat_question_id = ? AND "
        		+ "(DATE(fb.cdate) BETWEEN ? AND ?) "
        		+ "AND fb.zone=? GROUP BY fb.zone,fb.division";

        return jdbcSchoolsTemplate.queryForList(sql, new Object[]{categoryId, fromDate, toDate, zone});
    }

    public List<Map<String, Object>> getActiveCategories() {
        String sql = "SELECT * FROM category WHERE is_active = 1 AND is_deleted = 0";
        return jdbcSchoolsTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getFeedbackCountByDivCat(String division, int categoryId, LocalDate fromDate, LocalDate toDate) {
    	
        String sql = "SELECT fd.division, sd.school_name, COUNT(fd.feedback) AS feedback_count " +
                     "FROM feedback fd " +
                     "JOIN school_details sd ON fd.school_details_id = sd.id " +
                     "JOIN category cd ON fd.cat_question_id = cd.id " + // Join category table
                     "WHERE fd.division = ? AND cd.id = ? AND sd.cdate BETWEEN ? AND ? " + // Filter by division, categoryId, and date range
                     "GROUP BY fd.division, sd.school_name";

        return jdbcSchoolsTemplate.queryForList(sql, new Object[]{division, categoryId, fromDate, toDate});
    }
    
    public Map<String, Object> getFeedbackBySchoolId(int schoolId, String division, String zone, int categoryId, String fromDate, String toDate, String userId) {
        // Convert fromDate and toDate to YYYY-MM-DD format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate fromLocalDate = LocalDate.parse(fromDate, formatter);
        LocalDate toLocalDate = LocalDate.parse(toDate, formatter);
        String fromSqlDate = fromLocalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String toSqlDate = toLocalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String sql = "SELECT fb.zone, fb.division, sd.school_name, cd.category, cq.id AS cat_question_id, " +
                "sd.school_type, DATE_FORMAT(fb.cdate, '%Y-%m-%d') AS feedback_date, fb.feedref, " +
                "cq.category_questions AS question, fb.feedback AS ans, " +
                "GROUP_CONCAT(DISTINCT CONCAT('"+fileBaseUrl+"/gccofficialapp/files', cp.photo_url) SEPARATOR ',') AS photos " +
                "FROM feedback fb " +
                "JOIN category cd ON fb.cat_question_id = cd.id " +
                "JOIN school_details sd ON fb.school_details_id = sd.id " +
                "JOIN category_question cq ON fb.qid = cq.id " +
                "LEFT JOIN category_photos cp ON fb.qid = cp.qid " +
                "WHERE fb.zone = ? AND fb.division = ? AND fb.school_details_id = ? AND cd.id = ? " +
                "AND DATE(fb.cdate) BETWEEN ? AND ? " +
                "GROUP BY fb.zone, fb.division, fb.id, sd.school_name, cd.category, cq.id, fb.feedback, fb.feedref, fb.cdate";

        List<Map<String, Object>> result = jdbcSchoolsTemplate.queryForList(sql, zone, division, schoolId, categoryId, fromSqlDate, toSqlDate);

        if (result.isEmpty()) {
            return Collections.emptyMap();
        }

        // Build response map
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> firstRow = result.get(0);
        response.put("zone", firstRow.get("zone"));
        response.put("division", firstRow.get("division"));
        response.put("schoolName", firstRow.get("school_name"));
        response.put("category", firstRow.get("category"));
        response.put("catQuestionId", firstRow.get("cat_question_id"));
        response.put("schoolType", firstRow.get("school_type"));
        response.put("feedbackDate", firstRow.get("feedback_date"));
        response.put("feedbackRef", firstRow.get("feedref"));

        List<Map<String, Object>> feedbackData = new ArrayList<>();
        for (Map<String, Object> row : result) {
            Map<String, Object> feedbackEntry = new HashMap<>();
            feedbackEntry.put("question", row.get("question"));
            feedbackEntry.put("ans", row.get("ans"));

            // Split photo URLs into a list
            String photos = (String) row.get("photos");
            feedbackEntry.put("photos", (photos != null && !photos.isEmpty()) ? Arrays.asList(photos.split(",")) : Collections.emptyList());

            feedbackData.add(feedbackEntry);
        }

        response.put("feedbackData", feedbackData);
        return response;
    }
    
    
    public Map<String, Object> getFeedbackByFeedRef( String feedref, String userId) {
        String sql = "SELECT sd.`id`, sd.`latitude`, sd.`longitude`, sd.`school_type`, sd.`school_name`, st.scl_type, "
        		+ "fb.zone, fb.division, fb.id, fb.feedback, cd.category, cd.id AS cat_question_id, "
        		+ "cq.category_questions AS question, fb.feedback AS ans, fb.feedref, "
        		+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', cp.photo_url) AS photos, "
        		//+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', sp.photo_url) AS schoolphotos, "
        		+ "GROUP_CONCAT(cq.category_questions) AS category_questions, "
        		+ "DATE_FORMAT(fb.cdate, '%d-%m-%Y %r') AS cdate, "
        		+ "DATE_FORMAT(fb.cdate, '%d-%m-%Y %r') AS formatted_date, "
        		+ "DATE_FORMAT(fb.cdate, '%d-%m-%Y') AS fdate, "
        		+ "DATE_FORMAT(fb.cdate, '%r') AS ftime "
        		+ "FROM feedback fb "
        		+ "JOIN category cd ON fb.cat_question_id = cd.id "
        		+ "JOIN school_details sd ON fb.school_details_id = sd.id "
        		+ "LEFT JOIN school_photos sp ON sd.id = sp.school_details_id "
        		+ "LEFT JOIN category_photos cp ON fb.qid = cp.qid AND cp.feedref= ? "
        		+ "LEFT JOIN category_question cq ON fb.qid = cq.id "
        		+ "JOIN school_type st ON st.id = sd.school_type "
        		+ "WHERE fb.feedref = ? "
        		+ "GROUP BY fb.zone, fb.division, fb.id, sd.school_name, cd.category, sd.school_type, fb.feedback, fb.feedref";

        List<Map<String, Object>> result = jdbcSchoolsTemplate.queryForList(sql,feedref,feedref);

        if (result.isEmpty()) {
            return Collections.emptyMap();
        }

        // Build response map
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> firstRow = result.get(0);
        response.put("zone", firstRow.get("zone"));
        response.put("division", firstRow.get("division"));
        response.put("schoolName", firstRow.get("school_name"));
        response.put("category", firstRow.get("category"));
        response.put("catQuestionId", firstRow.get("cat_question_id"));
        response.put("schoolType", firstRow.get("scl_type"));
        response.put("feedbackDate", firstRow.get("formatted_date"));
        response.put("feedDate", firstRow.get("fdate"));
        response.put("feedTime", firstRow.get("ftime"));
        response.put("feedbackRef", firstRow.get("feedref"));

        List<Map<String, Object>> feedbackData = new ArrayList<>();
        for (Map<String, Object> row : result) {
            Map<String, Object> feedbackEntry = new HashMap<>();
            feedbackEntry.put("question", row.get("question"));
            feedbackEntry.put("ans", row.get("ans"));

            // Split photo URLs into a list
            String photos = (String) row.get("photos");
            feedbackEntry.put("photos", (photos != null && !photos.isEmpty()) ? Arrays.asList(photos.split(",")) : Collections.emptyList());

            feedbackData.add(feedbackEntry);
        }

        response.put("feedbackData", feedbackData);
        return response;
    }
    
/*
    public List<Map<String, Object>> getFeedbackBySchoolId(int schoolId, String division, String zone, int categoryId, String fromDate, String toDate,String user_id) {
        // Convert fromDate and toDate to YYYY-MM-DD format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate fromLocalDate = LocalDate.parse(fromDate, formatter);
        LocalDate toLocalDate = LocalDate.parse(toDate, formatter);
        String fromSqlDate = fromLocalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String toSqlDate = toLocalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String sql = "SELECT fd.zone, fd.division, fd.id, sd.school_name, cd.category, cd.id AS cat_question_id, " +
                "sd.school_type, fd.feedback, " +
                "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', cp.photo_url) AS feedbackphotos, " +
                "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', sp.photo_url) AS schoolphotos, " +
                "GROUP_CONCAT(cq.category_questions) AS category_questions, " +
                "DATE_FORMAT(fd.cdate, '%d-%m-%Y') AS cdate " +
                "FROM feedback fd " +
                "JOIN category cd ON fd.cat_question_id = cd.id " +
                "JOIN school_details sd ON fd.school_details_id = sd.id " +
                "LEFT JOIN school_photos sp ON sd.id = sp.school_details_id " +
                "LEFT JOIN category_photos cp ON cd.id = cp.category_id " +
                "LEFT JOIN category_question cq ON cd.id = cq.category_id " +
                "WHERE sd.id = ? AND fd.division = ? AND fd.zone = ? AND cd.id = ? AND DATE(fd.cdate) BETWEEN ? AND ? " + 
                "GROUP BY fd.zone, fd.division, fd.id, sd.school_name, cd.category, sd.school_type, fd.feedback, DATE_FORMAT(fd.cdate, '%d-%m-%Y') "
                + "";
        return jdbcSchoolsTemplate.queryForList(sql, new Object[]{schoolId, division, zone, categoryId, fromSqlDate, toSqlDate});
    }
    */
    public void saveCmSheme(
	        int categoryId,
	         String BreakFastQualityQa,
	         String BreakFastQualityAns,
	         String CMBFhelpersPresentSchoolQa,
	         String CMBFhelpersPresentSchoolAns,
	         String DinningAreaKeptQa,
	         String DinningAreaKeptAns,
	         String IsschoolhygienicallyQa,
	         String IsschoolhygienicallyAns,
	        MultipartFile feedPhoto,
	        int school_details_id,
	        String zone,
	        String division,
	        double latitude,
	        double longitude,
	        String user_id,
	        String question_id) {

	    String feedPhotoPath = null;

	    
	    // Handle file upload if a photo is provided
	    if (feedPhoto != null && !feedPhoto.isEmpty()) {
	        // Make sure to pass the correct type for the first argument
	    	String id = String.valueOf(categoryId);
	    	String sdid = String.valueOf(school_details_id);
	    	feedPhotoPath = fileUpload(id, sdid, feedPhoto); // Adjusted to use schoolDetailsId
	    }

	    // Create a map for questions and answers
	    Map<String, String> questionsAndAnswers = new HashMap<>();
	    questionsAndAnswers.put(BreakFastQualityQa, BreakFastQualityAns);
	    questionsAndAnswers.put(CMBFhelpersPresentSchoolQa, CMBFhelpersPresentSchoolAns);
	    questionsAndAnswers.put(DinningAreaKeptQa, DinningAreaKeptAns);
	    questionsAndAnswers.put(IsschoolhygienicallyQa, IsschoolhygienicallyAns);
	   // questionsAndAnswers.put(genSetConditionQa, genSetConditionAns);
	    
	    String feedref = generateRandomString(); //generate feedback reference text
	    
	    // Save feedback for each question and answer pair
	    questionsAndAnswers.forEach((question, answer) -> 
	        saveFeedbackDetails(categoryId, question, answer, school_details_id, zone, division, latitude, longitude, user_id,feedref)
	    );

	    // If a photo was uploaded, save it in the category_photos table
	    if (feedPhotoPath != null) {
	    	String qid="38";
	        saveCategoryPhoto(categoryId, feedPhotoPath, qid, feedref);
	    }
    }
    
    public void saveClusterKitchen(int categoryId, String clusterKitchencleanandneatQa,
			String clusterKitchencleanandneatAns, String sampleoffoodmaintainedQa,
			String sampleoffoodmaintainedAns, String cookingstaffwearingQa, String cookingstaffwearingAns,
			String fooditemsmatchwiththestockQa, String fooditemsmatchwiththestockAns,
			String fooditemsusedofgoodqualityQa, String fooditemsusedofgoodqualityAns, MultipartFile feedPhoto,
			int school_details_id, String zone, String division, double latitude, double longitude,
			String user_id, String question_id) {
    		
    	String feedPhotoPath = null;
    	
    	// Handle file upload if a photo is provided
	    if (feedPhoto != null && !feedPhoto.isEmpty()) {
	        // Make sure to pass the correct type for the first argument
	    	String id = String.valueOf(categoryId);
	    	String sdid = String.valueOf(school_details_id);
	    	feedPhotoPath = fileUpload(id, sdid, feedPhoto); // Adjusted to use schoolDetailsId
	    }
	    // Create a map for questions and answers
	    Map<String, String> questionsAndAnswers = new HashMap<>();
	    questionsAndAnswers.put(clusterKitchencleanandneatQa, clusterKitchencleanandneatAns);
	    questionsAndAnswers.put(sampleoffoodmaintainedQa, sampleoffoodmaintainedAns);
	    questionsAndAnswers.put(cookingstaffwearingQa, cookingstaffwearingAns);
	    questionsAndAnswers.put(fooditemsmatchwiththestockQa, fooditemsmatchwiththestockAns);
	    questionsAndAnswers.put(fooditemsusedofgoodqualityQa, fooditemsusedofgoodqualityAns);

	    String feedref = generateRandomString(); //generate feedback reference text
	    
	    // Save feedback for each question and answer pair
	    questionsAndAnswers.forEach((question, answer) -> 
	        saveFeedbackDetails(categoryId, question, answer, school_details_id, zone, division, latitude, longitude, user_id,feedref)
	    );
	    
	    // If a photo was uploaded, save it in the category_photos table
	    if (feedPhotoPath != null) {
	    	String qid="40";
	        saveCategoryPhoto(categoryId,feedPhotoPath,qid,feedref);
	    }			
	}
    
    public void savePlayFields(int categoryId, 
    		String playfieldavailableQa, String playfieldavailableAns, 
    		String civilworkQa, String civilworkAns, 
    		String electricalworkQa, String electricalworkAns,
			String sportsmaterialkQa, String sportsmaterialAns,
			MultipartFile playfieldPhoto,
			MultipartFile civilworkPhoto,
			int school_details_id, String zone, String division, double latitude, double longitude,
			String user_id, String question_id) {
    		
    	String playfieldPhotoPath = null;
    	String civilworkPhotoPath = null;
    	
    	// Handle file upload if a photo is provided
	    if (playfieldPhoto != null && !playfieldPhoto.isEmpty()) {
	        // Make sure to pass the correct type for the first argument
	    	String id = String.valueOf(categoryId);
	    	String sdid = String.valueOf(school_details_id);
	    	playfieldPhotoPath = fileUpload(id, sdid, playfieldPhoto); // Adjusted to use schoolDetailsId
	    }
	    if (civilworkPhoto != null && !civilworkPhoto.isEmpty()) {
	    	String id = String.valueOf(categoryId);
	    	String sdid = String.valueOf(school_details_id);
	    	civilworkPhotoPath = fileUpload(id, sdid, civilworkPhoto); // Adjusted to use schoolDetailsId
	        //playgroundPhotoPath = assetFileUpload(categoryId, PlaygroundfeedPhoto, school_details_id);
	    }
	    // Create a map for questions and answers
	    Map<String, String> questionsAndAnswers = new HashMap<>();
	    questionsAndAnswers.put(playfieldavailableQa, playfieldavailableAns);
	    questionsAndAnswers.put(civilworkQa, civilworkAns);
	    questionsAndAnswers.put(electricalworkQa, electricalworkAns);
	    questionsAndAnswers.put(sportsmaterialkQa, sportsmaterialAns);

	    String feedref = generateRandomString(); //generate feedback reference text
	    
	    // Save feedback for each question and answer pair
	    questionsAndAnswers.forEach((question, answer) -> 
	        saveFeedbackDetails(categoryId, question, answer, school_details_id, zone, division, latitude, longitude, user_id,feedref)
	    );
	    
	    // If a photo was uploaded, save it in the category_photos table
	    if (playfieldPhotoPath != null) {
	    	String qid="59";
	        saveCategoryPhoto(categoryId,playfieldPhotoPath,qid,feedref);
	    }
	    if (civilworkPhotoPath != null) {
	    	String qid="60";
	        saveCategoryPhoto(categoryId,civilworkPhotoPath,qid,feedref);
	    }
	}
    
    // Complaints List
    
    public List<Map<String, Object>> getComplaints(String userId) {
        String sql = "SELECT `id`, `cid`, `qid`, `feedref`, `school_details_id`, `complaint_txt`, `cdate`, `status`, `user_id` FROM `complaints` WHERE `user_id`=?";

        List<Map<String, Object>> result = jdbcSchoolsTemplate.queryForList(sql,userId);
        return result;
    }
    
    public List<Map<String, Object>> getComplaintsReplay(String comid) {
        String sql = "SELECT `id`, `comid`, `replay`, `file`, "
        		+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', file) AS photos, "
        		+ "`cby`, `cdate`, `isactive` FROM `complaints_reply` WHERE `comid`=?";

        List<Map<String, Object>> result = jdbcSchoolsTemplate.queryForList(sql,comid);
        return result;
    }
    
}


