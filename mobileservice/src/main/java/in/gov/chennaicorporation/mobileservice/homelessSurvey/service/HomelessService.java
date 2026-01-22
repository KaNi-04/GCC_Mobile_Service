package in.gov.chennaicorporation.mobileservice.homelessSurvey.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class HomelessService {
private JdbcTemplate jdbcHomelessTemplate;
	
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlGccHomelessDataSource") DataSource HomelessSource) {
		this.jdbcHomelessTemplate = new JdbcTemplate(HomelessSource);
	}
    
    @Autowired
	public HomelessService(Environment environment) {
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
		String serviceFolderName = environment.getProperty("homeless_foldername");
		var year = DateTimeUtil.getCurrentYear();
		var month = DateTimeUtil.getCurrentMonth();
		var date = DateTimeUtil.getCurrentDay();

		uploadDirectory = uploadDirectory + serviceFolderName + name +"/" + filetype + "/" + year + "/" + month + "/" + date;

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

			String filepath_txt = "/" + serviceFolderName + "driver/" + filetype + "/" + year + "/" + month + "/" + date + "/" + fileName;

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
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		        
				System.out.println("Date: "+ now.format(formatter));
				System.out.println("Activity: SluicePoint");
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
	
	@Transactional
	public List<Map<String, Object>> versionCheck() {
		String sqlQuery = "SELECT * FROM `app_version` WHERE `isactive`=1 LIMIT 1"; 
		List<Map<String, Object>> result = jdbcHomelessTemplate.queryForList(sqlQuery);
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getloginList(String username, String password) {
		
		String sqlQuery = ""; 
		
		List<Map<String, Object>> result=null;
		try {     
        	sqlQuery = "SELECT `loginid`, `name`, `username`  FROM `appuser` WHERE (`username`=? AND `password`=?) AND (`isactive`=1 AND `isdelete`=0) LIMIT 1";
        	result = jdbcHomelessTemplate.queryForList(sqlQuery, username, password);
        } catch (Exception e) {
            // Log the error
            e.printStackTrace();
            // Return an error message as part of the result list
            return Collections.singletonList(Collections.singletonMap("error", "An error occurred while fetching data."));
        }
        
        // If no records found, return a status and message indicating no user found
        if (result == null || result.isEmpty()) {
            Map<String, Object> response = Map.of(
                "status", 404,
                "message", "User not found."
            );
            return Collections.singletonList(response);
        }
        
        // Add a success status and message to the result
        Map<String, Object> successResponse = Map.of(
            "status", 200,
            "message", "login successfuly.",
            "data", result
        );
        return Collections.singletonList(successResponse);
    }
	
	@Transactional
	public List<Map<String, Object>> getCategory(String userid) {
		
		String sqlQuery = "";
		
		List<Map<String, Object>> result=null;
		try {     
        	sqlQuery = "SELECT *  FROM `category` WHERE (`isactive`=1 AND `isdelete`=0) ORDER BY `orderby`";
        	result = jdbcHomelessTemplate.queryForList(sqlQuery);
        } catch (Exception e) {
            // Log the error
            e.printStackTrace();
            // Return an error message as part of the result list
            return Collections.singletonList(Collections.singletonMap("error", "An error occurred while fetching data."));
        }
        
        // If no records found, return a status and message indicating no user found
        if (result == null || result.isEmpty()) {
            Map<String, Object> response = Map.of(
                "status", 404,
                "message", "User not found."
            );
            return Collections.singletonList(response);
        }
        
        // Add a success status and message to the result
        Map<String, Object> successResponse = Map.of(
            "status", 200,
            "message", "login successfuly.",
            "data", result
        );
        return Collections.singletonList(successResponse);
    }
	
	@Transactional
	public List<Map<String, Object>> getFormFields(int formId) {

	    String sql;
	    List<Map<String, Object>> result;

	    try {
	        sql = """
	            SELECT 
	                ff.id AS field_id,
	                ff.field_key,
	                ff.field_label,
	                ff.field_type,
	                ff.placeholder,
	                ff.is_required,
	                ff.parent_field_id,
	                ff.parent_field_value,
	                ff.sort_order
	            FROM form_field ff
	            WHERE ff.form_id = ?
	              AND ff.is_active = 1
	            ORDER BY ff.sort_order
	        """;

	        result = jdbcHomelessTemplate.queryForList(sql, formId);

	    } catch (Exception e) {
	        e.printStackTrace();
	        return Collections.singletonList(
	            Map.of("status", 500, "message", "Error while fetching form fields")
	        );
	    }

	    if (result == null || result.isEmpty()) {
	        return Collections.singletonList(
	            Map.of("status", 404, "message", "No form fields found")
	        );
	    }

	    return Collections.singletonList(
	        Map.of(
	            "status", 200,
	            "message", "Form fields fetched successfully",
	            "data", result
	        )
	    );
	}
	
	@Transactional
	public List<Map<String, Object>> getFieldOptions(int fieldId) {

	    String sql;
	    List<Map<String, Object>> result;

	    try {
	        sql = """
	            SELECT 
	                option_value,
	                option_label
	            FROM form_field_option
	            WHERE field_id = ?
	              AND is_active = 1
	            ORDER BY sort_order
	        """;

	        result = jdbcHomelessTemplate.queryForList(sql, fieldId);

	    } catch (Exception e) {
	        e.printStackTrace();
	        return Collections.singletonList(
	            Map.of("status", 500, "message", "Error while fetching field options")
	        );
	    }

	    if (result == null || result.isEmpty()) {
	        return Collections.singletonList(
	            Map.of("status", 404, "message", "No options found")
	        );
	    }

	    return Collections.singletonList(
	        Map.of(
	            "status", 200,
	            "message", "Options fetched successfully",
	            "data", result
	        )
	    );
	}
	
	@Transactional
	public List<Map<String, Object>> submitForm(
	        int formId,
	        String submittedBy,
	        String latitude,
	        String longitude,
	        Map<Integer, String> fieldValues) {

	    try {

	        String insertSubmission = """
	            INSERT INTO form_submission(form_id, submitted_by, latitude, longitude)
	            VALUES (?, ?, ?, ?)
	        """;

	        jdbcHomelessTemplate.update(insertSubmission, formId, submittedBy, latitude, longitude);

	        Integer submissionId = jdbcHomelessTemplate.queryForObject(
	                "SELECT LAST_INSERT_ID()", Integer.class);

	        String insertValue = """
	            INSERT INTO form_submission_value(submission_id, field_id, field_value)
	            VALUES (?, ?, ?)
	        """;

	        for (Map.Entry<Integer, String> entry : fieldValues.entrySet()) {
	        	jdbcHomelessTemplate.update(insertValue,
	                    submissionId,
	                    entry.getKey(),
	                    entry.getValue());
	        }

	        return Collections.singletonList(
	            Map.of(
	                "status", 200,
	                "message", "Form submitted successfully",
	                "submission_id", submissionId
	            )
	        );

	    } catch (Exception e) {
	        e.printStackTrace();
	        return Collections.singletonList(
	            Map.of("status", 500, "message", "Error while submitting form")
	        );
	    }
	}
	
	
	@Transactional
	public List<Map<String, Object>> getSubmission(int submissionId) {

	    String sql;
	    List<Map<String, Object>> result;

	    try {
	        sql = """
	            SELECT 
	                ff.field_label,
	                fsv.field_value
	            FROM form_submission_value fsv
	            JOIN form_field ff ON ff.id = fsv.field_id
	            WHERE fsv.submission_id = ?
	            ORDER BY ff.sort_order
	        """;

	        result = jdbcHomelessTemplate.queryForList(sql, submissionId);

	    } catch (Exception e) {
	        e.printStackTrace();
	        return Collections.singletonList(
	            Map.of("status", 500, "message", "Error while fetching submission data")
	        );
	    }

	    if (result == null || result.isEmpty()) {
	        return Collections.singletonList(
	            Map.of("status", 404, "message", "No submission data found")
	        );
	    }

	    return Collections.singletonList(
	        Map.of(
	            "status", 200,
	            "message", "Submission data fetched successfully",
	            "data", result
	        )
	    );
	}
}
