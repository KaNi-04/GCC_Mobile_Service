package in.gov.chennaicorporation.mobileservice.gccCattleSurvey.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class CattleService {

	
	private JdbcTemplate jdbcTemplate;

    private final Environment environment;
    private String fileBaseUrl;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();

    @Autowired
    public void setDataSource(@Qualifier("mysqlCattleSurveyGccDataSource") DataSource CattleSurveyGccSource) {
        this.jdbcTemplate = new JdbcTemplate(CattleSurveyGccSource);
    }

    @Autowired
    public CattleService(Environment environment) {
        this.environment = environment;
        this.fileBaseUrl = environment.getProperty("fileBaseUrl");
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
    
    public String fileUpload(String name, MultipartFile file) {

        int lastInsertId = 0;
        // Set the file path where you want to save it
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("gcc_cattlesurvey_foldername");
        var year = DateTimeUtil.getCurrentYear();
        var month = DateTimeUtil.getCurrentMonth();
        var date = DateTimeUtil.getCurrentDay();

        uploadDirectory = uploadDirectory + serviceFolderName + year +
                "/" + month;

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
            String fileName = name + "_" + datetimetxt + "_" + file.getOriginalFilename();
            fileName = fileName.replaceAll("\\s+", "");

            String filePath = uploadDirectory + "/" + fileName;

            String filepath_txt = "/" + serviceFolderName + year + "/" +
                    month + "/"
                    + fileName;

            // Create a new Path object
            Path path = Paths.get(filePath);

            // Get the bytes of the file
            byte[] bytes = file.getBytes();

            // Compress the image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50%quality

            // Write the bytes to the file
            Files.write(path, bytes);

            System.out.println(filePath);
            return filepath_txt;

        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to save file " + file.getOriginalFilename();
        }
    }

	public List<Map<String, Object>> getbreeds() {

		Map<String, Object> response = new HashMap<>();
		
		try {
			
			String sql="SELECT * FROM breed_master WHERE isactive=1 AND isdelete=0";

    		List<Map<String, Object>> breed_details=jdbcTemplate.queryForList(sql);

    		response.put("data",breed_details);
			
    		response.put("message", "Breed Types");
            response.put("status", "Success");
			
		} catch (Exception e) {
			 response.put("message", "Error in getting Breeds");
	            response.put("status", "Failed");
	            e.printStackTrace();
		}
		
		return Collections.singletonList(response);

	}

	public List<Map<String, Object>> cattle_type() {
		
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			
			String sql="SELECT * FROM cattle_type_master WHERE isactive=1 AND isdelete=0";

    		List<Map<String, Object>> cattle_details=jdbcTemplate.queryForList(sql);

    		response.put("data",cattle_details);
			
    		response.put("message", "Cattles Types");
            response.put("status", "Success");
			
		} catch (Exception e) {
			 response.put("message", "Error in getting Cattles");
	            response.put("status", "Failed");
	            e.printStackTrace();
		}
		
		return Collections.singletonList(response);
	}

	public List<Map<String, Object>> getgcc_shed() {
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			
			String sql="SELECT * FROM gcc_shed_master WHERE isactive=1 AND isdelete=0";

    		List<Map<String, Object>> gcc_shed_details=jdbcTemplate.queryForList(sql);

    		response.put("data",gcc_shed_details);
			
    		response.put("message", "GCC Sheds");
            response.put("status", "Success");
			
		} catch (Exception e) {
			 response.put("message", "Error in getting GCC Sheds");
	            response.put("status", "Failed");
	            e.printStackTrace();
		}
		
		return Collections.singletonList(response);
		
	}

	public List<Map<String, Object>> getidproof() {
		
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			
			String sql="SELECT * FROM idproof_master WHERE isactive=1 AND isdelete=0";

    		List<Map<String, Object>> idproof_details=jdbcTemplate.queryForList(sql);

    		response.put("data",idproof_details);
			
    		response.put("message", "ID Proof Types");
            response.put("status", "Success");
			
		} catch (Exception e) {
			 response.put("message", "Error in getting ID Proofs");
	            response.put("status", "Failed");
	            e.printStackTrace();
		}
		
		return Collections.singletonList(response);
		
	}

	public List<Map<String, Object>> getspace_rearing() {
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			
			String sql="SELECT * FROM space_rearing_master WHERE isactive=1 AND isdelete=0";

    		List<Map<String, Object>> space_rearing_details=jdbcTemplate.queryForList(sql);

    		response.put("data",space_rearing_details);
			
    		response.put("message", "Space Rearing Types");
            response.put("status", "Success");
			
		} catch (Exception e) {
			 response.put("message", "Error in getting Space Rearing");
	            response.put("status", "Failed");
	            e.printStackTrace();
		}
		
		return Collections.singletonList(response);
		
	}

	public List<Map<String, Object>> getvaccination_type() {
		
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			
			String sql="SELECT * FROM vaccination_type_master WHERE isactive=1 AND isdelete=0";

    		List<Map<String, Object>> vaccination_type_details=jdbcTemplate.queryForList(sql);

    		response.put("data",vaccination_type_details);
			
    		response.put("message", "Vaccination Types");
            response.put("status", "Success");
			
		} catch (Exception e) {
			 response.put("message", "Error in getting Vaccination Types");
	            response.put("status", "Failed");
	            e.printStackTrace();
		}
		
		return Collections.singletonList(response);
		
	}
	
//	public List<Map<String, Object>> getcattle_housed() {
//		
//		Map<String, Object> response = new HashMap<>();
//		
//		try {
//			
//			String sql="SELECT * FROM cattle_housed_master WHERE isactive=1 AND isdelete=0";
//
//    		List<Map<String, Object>> cattle_housed_details=jdbcTemplate.queryForList(sql);
//
//    		response.put("data",cattle_housed_details);
//			
//    		response.put("message", "Cattle Housed Types");
//            response.put("status", "Success");
//			
//		} catch (Exception e) {
//			 response.put("message", "Error in getting Cattle Housed Types");
//	            response.put("status", "Failed");
//	            e.printStackTrace();
//		}
//		
//		return Collections.singletonList(response);
//	}

	public List<Map<String, Object>> saveownerdetails(String owner_name, String mobile_no, String address, int id_proof,
			String id_details, String house_type, int cattle_space, Double  cattle_space_area, int no_of_cattles,
			String userid, MultipartFile image, String latitude, String longitude, String zone, String ward,
			String location,Integer cattle_space_gcc_shed) {
		
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			
			
			String checkSql = "SELECT COUNT(*) FROM owner_details WHERE mobile_no = ? AND isactive=1 AND isdelete=0";

			Integer count = jdbcTemplate.queryForObject(
			        checkSql,
			        Integer.class,
			        mobile_no
			);

			if (count != null && count > 0) {
			    response.put("status", "Failed");
			    response.put("message", "Mobile number already exists");
			    return Collections.singletonList(response);
			}
			
			String imagePath = "";
            if (image == null || image.isEmpty()) {

                response.put("status", "Failed");
                response.put("message", "image is required");

                return Collections.singletonList(response);
            }
            // image upload
            if (image != null && !image.isEmpty()) {

                imagePath = fileUpload("owner", image);
            }
            final String finalImagePath = imagePath;
            
            String ownerSql="INSERT INTO owner_details (owner_name,mobile_no,address,id_proof,id_details,house_type,cattle_space,cattle_space_area,no_of_cattles,cby,latitude,longitude,zone,ward,location,owner_photo,cattle_space_gcc_shed)"
            		+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            
            KeyHolder keyHolder = new GeneratedKeyHolder();
            
            int result = jdbcTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(ownerSql, new String[] { "id" });
				ps.setString(1,owner_name);
				ps.setString(2,mobile_no);
				ps.setString(3,address);
				ps.setInt(4,id_proof);
				ps.setString(5,id_details);
				ps.setString(6,house_type);
				ps.setInt(7,cattle_space);
				if (cattle_space_area != null) ps.setDouble(8,cattle_space_area);
				else ps.setNull(8, java.sql.Types.DOUBLE);
				ps.setInt(9,no_of_cattles);
				ps.setString(10,userid);				
				ps.setString(11, latitude);
				ps.setString(12, longitude);
				ps.setString(13, zone);
				ps.setString(14, ward);
				ps.setString(15, location);
				ps.setString(16,finalImagePath);
				if (cattle_space_gcc_shed != null) ps.setInt(17, cattle_space_gcc_shed);
				else ps.setNull(17, java.sql.Types.INTEGER);
				return ps;
            }, keyHolder);
            
            if (result > 0) {

            	int generatedId = keyHolder.getKey().intValue();
	            String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
	            String refId = prefix + generatedId;
		         
	            String updateSql = " UPDATE owner_details SET owner_ref_id = ? WHERE id = ? ";
	            jdbcTemplate.update(updateSql,refId, generatedId);
	            
	            response.put("status", "Success");
	            response.put("message", "Owner Details Saved Successfully");
	            response.put("Ref_ID", refId);
            }
				
		} catch (Exception e) {
			e.printStackTrace();

            response.put("status", "Failed");
            response.put("message", e.getMessage());
		}
		
		return Collections.singletonList(response);
		
	}

	public List<Map<String, Object>> getownersbycby(String userid) {
		
		Map<String, Object> response = new HashMap<>();
				
				try {
										
					String sql =
						    "SELECT od.*, " +
						    " CONCAT('" + fileBaseUrl + "/gccofficialapp/files',od.owner_photo) AS img_full_path, " +
						    " COALESCE(cd.cattle_count,0) AS completed_count, " +
						    " GREATEST(0,(od.no_of_cattles - COALESCE(cd.cattle_count,0))) AS pending_count " +
						    " FROM owner_details od " +
						    " LEFT JOIN ( " +
						    "   SELECT owner_ref_id, COUNT(*) AS cattle_count " +
						    "   FROM cattle_details " +
						    "   WHERE isactive=1 AND isdelete=0 " +
						    "   GROUP BY owner_ref_id " +
						    " ) cd ON od.owner_ref_id = cd.owner_ref_id " +
						    " WHERE od.isactive=1 " +
						    " AND od.isdelete=0 " +
						    " AND od.is_completed=0 " +
						    " AND od.cby=?";
					
		    		List<Map<String, Object>> owner_details=jdbcTemplate.queryForList(sql,userid);
		
		    		response.put("data",owner_details);
					
		    		response.put("message", "Cattle Owners Details");
		            response.put("status", "Success");
					
				} catch (Exception e) {
					 response.put("message", "Error in getting Cattle Owners Details");
			            response.put("status", "Failed");
			            e.printStackTrace();
				}
				
				return Collections.singletonList(response);
	}

	public List<Map<String, Object>> getownerdetails(String owner_ref_id) {
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			
			String sql =
				    "SELECT od.*,CONCAT('" + fileBaseUrl + "/gccofficialapp/files',owner_photo) AS img_full_path, " +
				    "COALESCE(cd.cattle_count,0) AS completed_count, " +
				    "(od.no_of_cattles - COALESCE(cd.cattle_count,0)) AS pending_count " +
				    "FROM owner_details od " +
				    "LEFT JOIN ( " +
				    "   SELECT owner_ref_id, COUNT(*) AS cattle_count " +
				    "   FROM cattle_details " +
				    "   WHERE isactive=1 AND isdelete=0 " +
				    "   GROUP BY owner_ref_id " +
				    ") cd ON od.owner_ref_id = cd.owner_ref_id " +
				    "WHERE od.isactive=1 AND od.isdelete=0 AND od.owner_ref_id=?";

    		List<Map<String, Object>> owner_details=jdbcTemplate.queryForList(sql,owner_ref_id);

    		response.put("data",owner_details);
			
    		response.put("message", "Owner Details");
            response.put("status", "Success");
			
		} catch (Exception e) {
			 response.put("message", "Error in getting Owner Details");
	            response.put("status", "Failed");
	            e.printStackTrace();
		}
		
		return Collections.singletonList(response);
	}
	
	

	public List<Map<String, Object>> savecattledetails(String owner_ref_id, int cattle_type, int breed_type,
			String animal_name, Integer animal_age, String animal_gender, String microchip_flag, String microchip_no,
			String license_flag, String licenese_no, String vaccination_flag, Integer vaccination_type,
			String vaccination_date, String userid, MultipartFile image, String latitude, String longitude, String zone,
			String ward, String location,String cattle_maintained,Integer cattle_space_gcc_shed,String insurance_flag,String insurance_no,MultipartFile ipimage) {
		
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			
			String imagePath = "";
			String ipimagePath = "";
            if (image == null || image.isEmpty()) {

                response.put("status", "Failed");
                response.put("message", "cattle image is required");

                return Collections.singletonList(response);
            }
            // image upload
            if (image != null && !image.isEmpty()) {

                imagePath = fileUpload("cattle", image);
            }
            if (ipimage != null && !ipimage.isEmpty()) {

            	ipimagePath = fileUpload("cattle_insurance_photo", ipimage);
            }
            final String finalImagePath = imagePath;
            final String finalipimagePath=ipimagePath;
            final String vDate =
                    (vaccination_date != null && !vaccination_date.isEmpty())
                            ? convertDateFormat(vaccination_date, 0)
                            : null;
            
            String ownerSql="INSERT INTO cattle_details (owner_ref_id,cattle_type,breed_type,animal_name,animal_age,animal_gender,"
            		+ " microchip_flag,microchip_no,license_flag,licenese_no,vaccination_flag,vaccination_type,vaccination_date,cattle_photo,latitude,longitude,zone,"
            		+ " ward,location,cby,cattle_maintained,cattle_space_gcc_shed,insurance_flag,insurance_no,insurance_photo)"
            		+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            
            KeyHolder keyHolder = new GeneratedKeyHolder();
            
            int result = jdbcTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(ownerSql, new String[] { "id" });
				ps.setString(1,owner_ref_id);
				ps.setInt(2,cattle_type);
				ps.setInt(3,breed_type);
				ps.setString(4,animal_name);
				if (animal_age != null) ps.setInt(5, animal_age);
				else ps.setNull(5, java.sql.Types.INTEGER);
				ps.setString(6,animal_gender);
				ps.setString(7,microchip_flag);
				ps.setString(8,microchip_no);				
				ps.setString(9,license_flag);
				ps.setString(10,licenese_no);
				ps.setString(11,vaccination_flag);
				if (vaccination_type != null) ps.setInt(12, vaccination_type);
				else ps.setNull(12, java.sql.Types.INTEGER);
				if (vDate != null)
				    ps.setString(13, vDate);
				else
				    ps.setNull(13, java.sql.Types.VARCHAR);
				ps.setString(14,finalImagePath);			
				ps.setString(15, latitude);
				ps.setString(16, longitude);
				ps.setString(17, zone);
				ps.setString(18, ward);
				ps.setString(19, location);
				ps.setString(20,userid);
				ps.setString(21,cattle_maintained);
				if (cattle_space_gcc_shed != null) ps.setInt(22, cattle_space_gcc_shed);
				else ps.setNull(22, java.sql.Types.INTEGER);
				ps.setString(23,insurance_flag);
				ps.setString(24,insurance_no);
				ps.setString(25,finalipimagePath);				
				return ps;
            }, keyHolder);
            
            if (result > 0) {
            	
            	String updateSql =
            		    "UPDATE owner_details od " +
            		    "SET od.is_completed = 1 " +
            		    "WHERE od.owner_ref_id = ? " +
            		    "AND od.isactive = 1 " +
            		    "AND od.isdelete = 0 " +
            		    "AND od.no_of_cattles = ( " +
            		    "    SELECT COUNT(*) " +
            		    "    FROM cattle_details cd " +
            		    "    WHERE cd.owner_ref_id = od.owner_ref_id " +
            		    "    AND cd.isactive = 1 " +
            		    "    AND cd.isdelete = 0 " +
            		    ")";
            	
            	jdbcTemplate.update(updateSql, owner_ref_id);
	            
	            response.put("status", "Success");
	            response.put("message", "Cattle Details Saved for REF ID: "+owner_ref_id+" Owner Successfully");
            }
				
		} catch (Exception e) {
			e.printStackTrace();

            response.put("status", "Failed");
            response.put("message", e.getMessage());
		}
		
		return Collections.singletonList(response);
		
	}

	
    
    
	
}
