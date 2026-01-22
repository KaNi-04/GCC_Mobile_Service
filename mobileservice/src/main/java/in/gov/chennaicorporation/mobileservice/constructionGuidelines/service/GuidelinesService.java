package in.gov.chennaicorporation.mobileservice.constructionGuidelines.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class GuidelinesService {
	private JdbcTemplate jdbcGuidlines;
	private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final int STRING_LENGTH = 15;
	private static final Random RANDOM = new SecureRandom();

	@Autowired
	public void setDataSource(@Qualifier("mysqlGccConstructionGuidelinesSource") DataSource guidlinesDataSource) {
		this.jdbcGuidlines = new JdbcTemplate(guidlinesDataSource);
	}
	
	@Autowired
	public GuidelinesService(Environment environment) {
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

	public String fileUpload(String name, String id, MultipartFile file) {

		int lastInsertId = 0;
		// Set the file path where you want to save it
		String uploadDirectory = environment.getProperty("file.upload.directory");
		String serviceFolderName = environment.getProperty("construction_guidelines_foldername");
		var year = DateTimeUtil.getCurrentYear();
		var month = DateTimeUtil.getCurrentMonth();
		var date = DateTimeUtil.getCurrentDay();

		uploadDirectory = uploadDirectory + serviceFolderName + year + "/" + month + "/" + date;

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

			String filepath_txt = "/" + serviceFolderName + year + "/" + month + "/" + date + "/" + fileName;

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
        String sqlQuery = "SELECT ward FROM gcc_penalty_hoardings.hoading_user_list WHERE userid = ? LIMIT 1";

        List<Map<String, Object>> results = jdbcGuidlines.queryForList(sqlQuery, loginid);

        if (!results.isEmpty()) {
            //System.out.println("Ward....." + results);
            // Extract the ward value from the first result
            return (String) results.get(0).get("ward");
        }

        return "000";
    }
	public String sendMessage(String tempid, String cdid) {
		
		String sendTo="";
		String datetxt="";
		String name="";
		String Nos="";
		String fineAmount="";
		
	    String sql = "SELECT `id`, `name`, `mobile`, `userid`, `tempids` FROM `send_msg_to` WHERE `isactive` = 1 AND `isdelete` = 0 AND FIND_IN_SET(?, tempids);";
	    List<Map<String, Object>> mobileResult = jdbcGuidlines.queryForList(sql,tempid);
	    // Create a StringBuilder to hold the comma-separated mobile numbers
	    StringBuilder sendToBuilder = new StringBuilder();

	    // Iterate through the mobileResult list and build the sendTo string
	    for (Map<String, Object> mobileRow : mobileResult) {
	        String mobile = String.valueOf(mobileRow.get("mobile"));
	        
	        // Append the mobile number to the StringBuilder
	        if (sendToBuilder.length() > 0) {
	            sendToBuilder.append(",");
	        }
	        sendToBuilder.append(mobile);
	    }

	    // Convert StringBuilder to String
	    sendTo = sendToBuilder.toString();
	    
	    // Now you have the sendTo string with all mobile numbers
	    System.out.println("Mobile Numbers: " + sendTo); // For debugging, you can print this value
	
    	String urlString="";
    	
    	String username="2000233507";
    	String password="h2YjFNcJ";
    	
    	switch (tempid) {
    	    case "1":
    	        System.out.println("TempID is 1 (Initial Notice)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1"
    	        		+ "&format=json"
    	        		+ "&msg_type=TEXT"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&msg=Your+Construction+%28"+name+"%29+falls+under+the+category+of+High+Significance+violations+as+per+GCC+Norms.+As+per+Clauses+%28"+Nos+"%29+of+the+Clean+and+Safe+Construction+guidelines.+%0A%0AYou+are+hereby+directed+to+rectify+the+violations+within+15+days+from+the+date+of+the+notice+issued.+%0A%0ASubmit+photographic%2Fvideo+evidence+and+compliance+report+to+this+office+upon+the+rectification+within+15+days."
    	        		+ "&isTemplate=true"
    	        		+ "&header=GCC+Clean+%26+Safe+Construction"
    	        		+ "&footer=GCC+-+IT+Cell";
    	        break;
    	    case "2":
    	        System.out.println("TempID is 2 (Compliance resolved)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1"
    	        		+ "&format=json"
    	        		+ "&msg_type=TEXT"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&msg=On+%28"+datetxt+"%29+the+Greater+Chennai+Corporation+instructed+you+to+rectify+the+violations+on+your+Construction+%28"+name+"%29+within+15+days.+Accordingly+it+has+been+found+that+the+violations+on+your+Construction+site+have+been+rectified."
    	        		+ "&isTemplate=true"
    	        		+ "&header=GCC+Clean+%26+Safe+Construction"
    	        		+ "&footer=GCC+-+IT+Cell";
    	        break;
    	        
    	    case "3":
    	        System.out.println("TempID is 3 (Penalty)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1"
    	        		+ "&format=json"
    	        		+ "&msg_type=TEXT"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&msg=On+%28%2A"+datetxt+"%2A%29+the+Greater+Chennai+Corporation+has+issued+a+notice+and+instructed+you+to+rectify+the+violations+on+your+Construction+%28"+name+"%29+within+15+days.+Accordingly+it+has+been+found+that+the+violations+on+your+Construction+site+have+not+been+rectified+yet.+Hence+the+penalty+of+%2ARs."+fineAmount+"%2A+has+been+issued+to+your+Construction+site.+%0A%0AFailing+to+rectify+the+violations+within+7+Days+of+imposing+of+fine%2C+may+result+in+%E2%80%9C%2Astop+work+notice%2A%E2%80%9D+and+construction+activity+at+your+site+will+be+halted."
    	        		+ "&isTemplate=true"
    	        		+ "&header=GCC+Clean+%26+Safe+Construction"
    	        		+ "&footer=GCC+-+IT+Cell";
    	        break;
    	    case "4":
    	        System.out.println("TempID is 4 (Compliance resolved Message After Penalty)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1"
    	        		+ "&format=json"
    	        		+ "&msg_type=TEXT"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&msg=On+%28"+datetxt+"%29+the+Greater+Chennai+Corporation+as+issued+Fine+of+Rs."+fineAmount+"+and+instructed+you+to+rectify+the+violations+on+your+%28"+name+"%29+Construction+site+within+7+days.+Accordingly+it+has+been+found+that+the+violations+on+your+Construction+site+have+been+rectified."
    	        		+ "&isTemplate=true"
    	        		+ "&header=GCC+Clean+%26+Safe+Construction"
    	        		+ "&footer=GCC+-+IT+Cell";
    	        break;
    	    case "5":
    	        System.out.println("TempID is 5 (Stop Work Notice)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1"
    	        		+ "&format=json"
    	        		+ "&msg_type=TEXT"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&msg=On+%28"+datetxt+"%29+the+Greater+Chennai+Corporation+has+issued+Rs."+fineAmount+"+penalty+to+your+Construction+Site+for+not+rectifying+the+violations+and+instructed+to+rectify+the+violations+within+7+days%2C+on+your+%28"+name+"%29+Construction+site.+%0AAccordingly+it+has+been+found+that+the+violations+on+your+%2AConstruction+site+have+not+been+rectified+yet%2A.+%0AHence+it+results+in+%E2%80%9C%2Astop+work+notice%2A%E2%80%9D+and+%2Aconstruction+activity+at+your+site+is+halted%2A."
    	        		+ "&isTemplate=true"
    	        		+ "&header=GCC+Clean+%26+Safe+Construction"
    	        		+ "&footer=GCC+-+IT+Cell";
    	        break;
    	    default:
    	        System.out.println("TempID ("+tempid+") is unknown");
    	}

    	if (!urlString.isBlank()) {
    	    String response = sendWhatsAppMessage(urlString);
    	    System.out.println("WhatsApp response: " + response);
    	}
    	
    	return "success";
    }
	
    private String sendWhatsAppMessage(String urlString) {
        String response = "";
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            response = String.valueOf(responseCode);
            System.out.println("Response Code for URL: " + urlString + " is " + responseCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
    
	public List<Map<String, Object>> getConstructionList(String loginid, 
			String latitude,
	        String longitude) {
        String sql = "SELECT cd.*"
        		+ ",CONCAT('"+fileBaseUrl+"/gccofficialapp/files',cd.image) AS photo,"
        		+ "ci.`ciid`, ci.`under_guidelines`, ci.`penalty` "
        		+ " FROM `construction_details` cd "
        		+ "LEFT JOIN `construction_inspection` ci ON ci.cdid=cd.cdid "
        		+ "WHERE cd.`isactive`=1 AND ci.`isactive`=1 AND ci.`cby` = ?";
        List<Map<String, Object>> result = jdbcGuidlines.queryForList(sql,loginid);
        
        for (Map<String, Object> row : result) {
        	String downloadLink ="";
	    	if(row.get("penalty") != null) {
	        downloadLink = "https://gccservices.in/gccofficialapp/api/pdf/template?"
	        		+ "cdid="+row.get("cdid")
	        		+ "&ciid="+row.get("ciid");
	    	}
	        row.put("downloadNoticeLink", downloadLink);
	    }
        
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Construction List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getBuildingTypeList() {
        String sql = "SELECT * FROM `building_type_master` WHERE `isactive`=1";
        List<Map<String, Object>> result = jdbcGuidlines.queryForList(sql);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Building Type List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getUnitOfficeList(String zone, String ward) {
        String sql = "SELECT * FROM `unit_office_master` WHERE `zone`=? AND `ward`=?";
        List<Map<String, Object>> result = jdbcGuidlines.queryForList(sql,zone, ward);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Unit Office List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	public List<Map<String, Object>> getGuidelinesList() {
        String sql = "SELECT * FROM `guidelines` WHERE `isactive`=1 ORDER BY `orderby`";
        List<Map<String, Object>> result = jdbcGuidlines.queryForList(sql);
		Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
        response.put("message", "Unit Office List.");
        response.put("data", result);
        
        return Collections.singletonList(response);
    }
	
	@Transactional
	public List<Map<String, Object>> saveConstructionDetails(
	        String contractor_name,
	        String contractor_mobile,
	        String buildup_area,
	        String building_type,
	        String high_rise,
	        String zone,
	        String ward,
	        String unit,
	        String under_guidelines,
	        String site_address,
	        String cby,
	        String latitude,
	        String longitude,
	        MultipartFile mainfile
	) {
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    String constimg = fileUpload("Construction", "main", mainfile);
	    
	    String type = "";
	    
	    // Prepare question image uploads
	    MultipartFile[] files = {
	    	mainfile
	    };
	    
	    // Insert query
	    String insertSql = "INSERT INTO `construction_details`(`contractor_name`, `contractor_mobile`, `buildup_area`, "
	    		+ "`building_type`, `high_rise`, `zone`, `ward`, `unit`, `site_address`, `cby`, `latitude`, `longitude`, "
	    		+ "`image`) VALUES "
	    		+ "(?,?,?,?,?,?,?,?,?,?,?,?,?)";

	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcGuidlines.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"cdid"});
	        int i = 1;
	        ps.setString(i++, contractor_name);
	        ps.setString(i++, contractor_mobile);
	        ps.setString(i++, buildup_area);
	        ps.setString(i++, building_type);
	        ps.setString(i++, high_rise);
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, unit);
	        ps.setString(i++, site_address);
	        ps.setString(i++, cby);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, constimg);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        int cdid = saveConstructionInspection(lastInsertId, under_guidelines, cby);
	        if(cdid>0) {
	        	response.put("insertId", lastInsertId);
		        response.put("status", "success");
		        response.put("message", "New Construction details inserted successfully.");
	        }
	        else {
	        	response.put("status", "error");
		        response.put("message", "New Construction details insert failed.");
	        }
	        
	    } else {
	        response.put("status", "error");
	        response.put("message", "New Construction details  insert failed.");
	    }

	    result.add(response);
	    return result;
	}
	
	private double getBuildupArea(String cdid) {
	    String sql = "SELECT buildup_area FROM construction_details WHERE cdid = ?";
	    
	    List<Map<String, Object>> result = jdbcGuidlines.queryForList(sql, cdid);
	    
	    if (result.isEmpty()) {
	        return 0.0; // default if not found
	    }
	    
	    Object value = result.get(0).get("buildup_area");
	    if (value == null) {
	        return 0.0;
	    }
	    
	    try {
	        return Double.parseDouble(value.toString()); // ✅ always safe
	    } catch (NumberFormatException e) {
	        return 0.0; // or throw if you want to enforce valid data
	    }
	}
	
	@Transactional
	public int saveConstructionInspection(int cdid, String under_guidelines, String cby) {
		int ciid = 0;
		// Insert query
	    String insertSql = "INSERT INTO `construction_inspection`(`cdid`, `under_guidelines`, `cby`) VALUES "
	    		+ "(?,?,?)";

	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcGuidlines.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"ciid"});
	        int i = 1;
	        ps.setInt(i++, cdid);
	        ps.setString(i++, under_guidelines);
	        ps.setString(i++, cby);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        ciid = lastInsertId;
	    } else {
	    	 // ❗ Rollback action: mark the original cdid as inactive
	        String updateSql = "UPDATE `construction_details` SET `isactive` = 0 WHERE `cdid` = ?";
	        jdbcGuidlines.update(updateSql, cdid);
	    }
		return ciid;
	}
	
	@Transactional
	public List<Map<String, Object>> saveGuidlinesFeedback(
	        String cdid,
	        String ciid,
	        String cby,
	        String latitude,
	        String longitude,
	        String zone,
	        String ward,
	        String q1, String q2, String q3, String q4, String q5, String q6, String q7, String q8, String q9, String q10,
	        String q11, String q12, String q13, String q14, String q15, String q16, String q17, String q18,
	        MultipartFile image1, MultipartFile image2, MultipartFile image3, MultipartFile image4, MultipartFile image5
	) {
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();
	    
	    String type = "";
	    
	    // Prepare question image uploads
	    MultipartFile[] files = {
	    	image1, image2, image3, image4, image5
	    };
	    String[] imagePaths = new String[5];
	    for (int i = 0; i < files.length; i++) {
	        if (files[i] != null && !files[i].isEmpty()) {
	            imagePaths[i] = fileUpload(cdid, "_"+ciid+"_"+(i + 1), files[i]);
	        } else {
	            imagePaths[i] = "";
	        }
	    }
	    
	    // Insert query
	    String insertSql = "INSERT INTO `guildelines_inspection` "
	    		+ "(`cdid`, `ciid`, `cby`, `latitude`, `longitude`, `zone`, `ward`,"
	    		+ "`q1`, `q2`, `q3`, `q4`, `q5`, `q6`, `q7`, `q8`, `q9`, `q10`, "
	    		+ "`q11`, `q12`, `q13`, `q14`, `q15`, `q16`, `q17`, `q18`, "
	    		+ "`img1`, `img2`, `img3`, `img4`, `img5`) " +
	            "VALUES (" + "?,".repeat(7 + 18 + 5).replaceAll(",$", "") + ")";

	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcGuidlines.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"giid"});
	        int i = 1;
	        ps.setString(i++, cdid);
	        ps.setString(i++, ciid);
	        ps.setString(i++, cby);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);

	        // 18 question answers
	        for (String q : new String[]{q1, q2, q3, q4, q5, q6, q7, q8, q9, q10,
	                q11, q12, q13, q14, q15, q16, q17, q18}) {
	            ps.setString(i++, q);
	        }

	        // 5 image paths
	        for (String path : imagePaths) {
	            ps.setString(i++, path);
	        }

	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	        int lastInsertId = keyHolder.getKey().intValue();
	        response.put("insertId", lastInsertId);
	        // Get penalty type
	        String penalty= findPenaltyType(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10,
	                q11, q12, q13, q14, q15, q16, q17, q18);
	        
	        int fineAmount = 0;
	        double buildup_area = 0;
	        buildup_area = getBuildupArea(cdid);
	        if (buildup_area > 20000) {
	            if ("High".equalsIgnoreCase(penalty)) {
	                fineAmount = 500000;
	            } else {
	                fineAmount = 100000;
	            }
	        } else if (buildup_area > 500) {
	            if ("High".equalsIgnoreCase(penalty)) {
	                fineAmount = 25000;
	            } else {
	                fineAmount = 10000;
	            }
	        } else if (buildup_area >= 300 && buildup_area <= 500) {
	            if ("High".equalsIgnoreCase(penalty)) {
	                fineAmount = 10000;
	            } else {
	                fineAmount = 1000;
	            }
	        }
	        
	        if(updateConstructionInspection(lastInsertId, ciid, cdid, penalty, String.valueOf(fineAmount))) {
	        	response.put("status", "success");
		        response.put("message", "Feedback inserted successfully.");
	        }
	        else {
	        	response.put("status", "error");
		        response.put("message", "Feedback Insert failed.");
	        }
	    } else {
	        response.put("status", "error");
	        response.put("message", "Insert failed.");
	    }
	    
	    result.add(response);
	    return result;
	}

	@Transactional
	public boolean updateConstructionInspection(int giid, String ciid, String cdid, String penalty, String fineAmount) {
	    String updateCISql = "UPDATE `construction_inspection` SET `penalty` = ?, `fine_amount` = ? WHERE ciid = ? AND cdid = ?";
	    
	    int affectedRows = jdbcGuidlines.update(updateCISql, penalty, fineAmount, ciid, cdid);

	    if (affectedRows > 0) {
	        return true;
	    } else {
	        // Optional rollback: mark original inspection inactive
	        String updateSql = "UPDATE `guildelines_inspection` SET `isactive` = 0 WHERE giid = ?";
	        jdbcGuidlines.update(updateSql, giid);
	        return false;
	    }
	}
	
	private String findPenaltyType(String q1, String q2, String q3, String q4, String q5, String q6, String q7, String q8, String q9, String q10,
	        String q11, String q12, String q13, String q14, String q15, String q16, String q17, String q18) {
		// Check High Penalty: any of q1–q14 is "No"
	    if ("No".equalsIgnoreCase(q1) || "No".equalsIgnoreCase(q2) || "No".equalsIgnoreCase(q3) ||
	        "No".equalsIgnoreCase(q4) || "No".equalsIgnoreCase(q5) || "No".equalsIgnoreCase(q6) ||
	        "No".equalsIgnoreCase(q7) || "No".equalsIgnoreCase(q8) || "No".equalsIgnoreCase(q9) ||
	        "No".equalsIgnoreCase(q10) || "No".equalsIgnoreCase(q11) || "No".equalsIgnoreCase(q12) ||
	        "No".equalsIgnoreCase(q13) || "No".equalsIgnoreCase(q14))
	    {
	    	return "High";
	    }
	    // Check Medium Penalty: any of q15–q17 is "No"
	    if("No".equalsIgnoreCase(q15) || "No".equalsIgnoreCase(q16) || "No".equalsIgnoreCase(q17))
	    {
	    	return "Medium";
	    }
	    // Default to Low Penalty
	    return "Low";
	}
	
	/*
	public List<Map<String, Object>> getRevisitConstructionList(String loginid, String latitude, String longitude) {
	    String sql = "SELECT cd.*, "
	            + "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', cd.image) AS photo, "
	            + "ci.ciid, ci.under_guidelines, ci.penalty "
	            + "FROM construction_details cd "
	            + "LEFT JOIN construction_inspection ci ON ci.cdid = cd.cdid "
	            + "WHERE cd.isactive = 1 AND ci.isactive = 1 AND ci.penalty IN ('High','Medium','Low')";

	    List<Map<String, Object>> result = jdbcGuidlines.queryForList(sql);

	    for (Map<String, Object> row : result) {
	    	
	    	Boolen allowRevisit = false;
	    	Object cdate = row.get("cdate");
	    	Object finalDate = cdate + 15 Days
	    	if(finalDate>todaydate) {
	    		allowRevisit = true;
	    	}
	    	row.put("allowRevisit", allowRevisit);
	    	
	        String downloadLink = "https://gccservices.in/gccofficialapp/api/pdf/template?"
	        		+ "cdid="+row.get("cdid")
	        		+ "&ciid="+row.get("ciid");

	        row.put("downloadNoticeLink", downloadLink);
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Construction List with Guidelines & Answers.");
	    response.put("data", result);

	    return Collections.singletonList(response);
	}
	*/
	
	public List<Map<String, Object>> getRevisitConstructionList(String loginid, String latitude, String longitude) {
		
		String ward = getWardByLoginId(loginid,"");
		
	    String sql = "SELECT cd.*, " +
	            "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', cd.image) AS photo, " +
	            "ci.ciid, ci.under_guidelines, ci.penalty " +
	            "FROM construction_details cd " +
	            "LEFT JOIN construction_inspection ci ON ci.cdid = cd.cdid " +
	            "WHERE cd.isactive = 1 AND ci.isactive = 1 AND ci.penalty IN ('High','Medium','Low')"
	            + "AND cd.cdid NOT IN ( "
	            + "        SELECT cdid  "
	            + "        FROM after_notice_inspection  "
	            + "        WHERE isactive = 1 "
	            + "  ) AND cd.ward=?";

	    List<Map<String, Object>> result = jdbcGuidlines.queryForList(sql,ward);

	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	    LocalDate today = LocalDate.now();

	    for (Map<String, Object> row : result) {
	        Boolean allowRevisit = false;
	        Object cdateObj = row.get("cdate");
	       /*
	        if (cdateObj != null) {
	            System.err.println("Raw cdate type: " + cdateObj.getClass().getName() + " | value: " + cdateObj);
	        }
	        */
	        
	        if (cdateObj != null) {
	            LocalDate cdate = null;

	            if (cdateObj instanceof LocalDate) {
	                cdate = (LocalDate) cdateObj;
	            } else if (cdateObj instanceof java.sql.Date) {
	                cdate = ((java.sql.Date) cdateObj).toLocalDate();
	            } else if (cdateObj instanceof java.sql.Timestamp) {
	                cdate = ((java.sql.Timestamp) cdateObj).toLocalDateTime().toLocalDate();
	            } else if (cdateObj instanceof LocalDateTime) {
	                cdate = ((LocalDateTime) cdateObj).toLocalDate();   // ✅ FIX
	            } else if (cdateObj instanceof String) {
	                try {
	                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	                    cdate = LocalDateTime.parse((String) cdateObj, dtf).toLocalDate();
	                } catch (Exception e) {
	                    System.err.println("❌ Could not parse cdate: " + cdateObj);
	                }
	            }
	           
	            if (cdate != null) {
	                LocalDate finalDate = cdate.plusDays(15);
	                allowRevisit = !today.isBefore(finalDate); // true if today >= finalDate
/*
	                System.err.println("ID: " + row.get("cdid") 
	                    + " | finalDate: " + finalDate
	                    + " | today: " + today
	                    + " | allowRevisit: " + allowRevisit);
	                /*
	                if (finalDate.isBefore(today) || finalDate.isEqual(today)) {
	                    allowRevisit = true;
	                }
	                */
	            }
	        }

	        row.put("allowRevisit", allowRevisit);

	        String downloadLink = "https://gccservices.in/gccofficialapp/api/pdf/template?" +
	                "cdid=" + row.get("cdid") +
	                "&ciid=" + row.get("ciid");

	        row.put("downloadNoticeLink", downloadLink);
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Construction List with Guidelines & Answers.");
	    response.put("data", result);
	    
	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getRevisitConstructionListWithFeedback(String loginid, String cdidtxt, String ciidtxt) {
	    String sql = "SELECT cd.*, "
	            + "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', cd.image) AS photo, "
	            + "DATE_FORMAT(cd.cdate, '%d-%m-%Y') AS ddate, "
	            + "ci.ciid, ci.under_guidelines, ci.penalty "
	            + "FROM construction_details cd "
	            + "LEFT JOIN construction_inspection ci ON ci.cdid = cd.cdid "
	            + "WHERE cd.isactive = 1 AND ci.isactive = 1 AND ci.penalty IN ('High','Medium','Low') AND cd.cdid=? AND ci.ciid=?";

	    List<Map<String, Object>> result = jdbcGuidlines.queryForList(sql, cdidtxt, ciidtxt);

	    // Fetch master guideline list only once
	    String guidelinesSql = "SELECT `gid`, `guideline`, `significance`, `orderby`, `isactive` FROM guidelines WHERE isactive = 1 ORDER BY orderby";
	    List<Map<String, Object>> guidelineList = jdbcGuidlines.queryForList(guidelinesSql);

	    for (Map<String, Object> row : result) {
	        Object cdid = row.get("cdid");
	        Object ciid = row.get("ciid");

	        // Fetch answer row for this cdid + ciid
	        String ansSql = "SELECT `giid`, `cdate`, `q1`, `q2`, `q3`, `q4`, `q5`, `q6`, `q7`, `q8`, `q9`, `q10`, `q11`, `q12`, `q13`, `q14`, `q15`, `q16`, `q17`, `q18`, "
	        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', img1) AS photo1, "
	        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', img2) AS photo2, "
	        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', img3) AS photo3, "
	        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', img4) AS photo4, "
	        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', img5) AS photo5 "
	        		+ "FROM guildelines_inspection WHERE isactive = 1 AND cdid = ? AND ciid = ? LIMIT 1";
	        List<Map<String, Object>> ansList = jdbcGuidlines.queryForList(ansSql, cdid, ciid);

	        Map<String, Object> answers = ansList.isEmpty() ? new HashMap<>() : ansList.get(0);

	        // Merge each guideline with its corresponding answer
	        List<Map<String, Object>> guidelineWithAnswers = new ArrayList<>();
	        for (int i = 1; i <= guidelineList.size(); i++) {
	            Map<String, Object> guideline = guidelineList.get(i - 1);
	            String qKey = "q" + i;
	            Map<String, Object> qa = new LinkedHashMap<>();
	            qa.put("question", guideline.get("guideline"));
	            qa.put("question_type", guideline.get("significance"));
	            qa.put("answer", answers.getOrDefault(qKey, ""));
	            guidelineWithAnswers.add(qa);
	        }
	        
	        List<Map<String, Object>> guidelineWithImages = new ArrayList<>();
	        Map<String, Object> qa_img = new LinkedHashMap<>();
	        qa_img.put("image1", answers.get("photo1"));
	        qa_img.put("image2", answers.get("photo2"));
	        qa_img.put("image3", answers.get("photo3"));
	        qa_img.put("image4", answers.get("photo4"));
	        qa_img.put("image5", answers.get("photo5"));
	        guidelineWithImages.add(qa_img);
	        
	        // attach to response
	        row.put("guidelines_id", answers.get("giid"));
	        row.put("guidelines_images", guidelineWithImages);
	        row.put("guidelines_answers", guidelineWithAnswers);
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Construction List with Guidelines & Answers.");
	    response.put("data", result);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveAfterNoticeDetails(
	        String cdid,
	        String ciid,
	        String giid,
	        String remarks,
	        String status,
	        String zone,
	        String ward,
	        String cby,
	        String latitude,
	        String longitude,
	        MultipartFile mainfile
	) {
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    String constimg = fileUpload("After_Notice",cdid+"_"+ciid+"_"+giid+"_"+status , mainfile);
	    
	    String type = "";
	    
	    // Prepare question image uploads
	    MultipartFile[] files = {
	    	mainfile
	    };
	    
	    // Insert query
	    String insertSql = "INSERT INTO `after_notice_inspection`(`cdid`, `ciid`, `giid`, `image1`, `remarks`, `zone`, `ward`, `latitude`, `longitude`, `cby`, `status`) VALUES "
	    		+ "(?,?,?,?,?,?,?,?,?,?,?)";

	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcGuidlines.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"cdid"});
	        int i = 1;
	        ps.setString(i++, cdid);
	        ps.setString(i++, ciid);
	        ps.setString(i++, giid);
	        ps.setString(i++, constimg);
	        ps.setString(i++, remarks);
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, cby);
	        ps.setString(i++, status);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	    	int lastInsertId = keyHolder.getKey().intValue();
	    	response.put("insertId", lastInsertId);
	    	response.put("status", "success");
	    	response.put("message", "New After Notice details inserted successfully.");   
	    } else {
	        response.put("status", "error");
	        response.put("message", "New After Notice details  insert failed.");
	    }

	    result.add(response);
	    return result;
	}
	
	public List<Map<String, Object>> get7thRevisitConstructionList(String loginid, String latitude, String longitude) {
		
		String ward = getWardByLoginId(loginid,"");
		
	    String sql = "SELECT cd.*, " +
	            "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', cd.image) AS photo, " +
	            "ci.ciid, ci.under_guidelines, ci.penalty " +
	            "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', ani.image1) AS finephoto, " +
	            "date_format(ani.cdate, '%d-%m-%Y') as Fine Date,  " +
	            "ani.cdate as finedate " +
	            "FROM construction_details cd " +
	            "LEFT JOIN construction_inspection ci ON ci.cdid = cd.cdid " +
	            "LEFT JOIN after_notice_inspection ani ON ani.cdid = cd.cdid " +
	            "WHERE cd.isactive = 1 AND ci.isactive = 1 AND ci.penalty IN ('High','Medium','Low') AND cd.ward=?";

	    List<Map<String, Object>> result = jdbcGuidlines.queryForList(sql,ward);

	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	    LocalDate today = LocalDate.now();

	    for (Map<String, Object> row : result) {
	        Boolean allowRevisit = false;
	        Object cdateObj = row.get("finedate");

	        if (cdateObj != null) {
	            System.err.println("Raw cdate type: " + cdateObj.getClass().getName() + " | value: " + cdateObj);
	        }
	        
	        if (cdateObj != null) {
	            LocalDate cdate = null;

	            if (cdateObj instanceof LocalDate) {
	                cdate = (LocalDate) cdateObj;
	            } else if (cdateObj instanceof java.sql.Date) {
	                cdate = ((java.sql.Date) cdateObj).toLocalDate();
	            } else if (cdateObj instanceof java.sql.Timestamp) {
	                cdate = ((java.sql.Timestamp) cdateObj).toLocalDateTime().toLocalDate();
	            } else if (cdateObj instanceof LocalDateTime) {
	                cdate = ((LocalDateTime) cdateObj).toLocalDate();   // ✅ FIX
	            } else if (cdateObj instanceof String) {
	                try {
	                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	                    cdate = LocalDateTime.parse((String) cdateObj, dtf).toLocalDate();
	                } catch (Exception e) {
	                    System.err.println("❌ Could not parse cdate: " + cdateObj);
	                }
	            }

	            if (cdate != null) {
	                LocalDate finalDate = cdate.plusDays(7);
	                allowRevisit = !today.isBefore(finalDate); // true if today >= finalDate
	               
	                System.err.println("ID: " + row.get("cdid") 
	                    + " | finalDate: " + finalDate
	                    + " | today: " + today
	                    + " | allowRevisit: " + allowRevisit);
	                	               /*
	                if (finalDate.isBefore(today) || finalDate.isEqual(today)) {
	                    allowRevisit = true;
	                }
	                */
	            }
	        }

	        row.put("allowRevisit", allowRevisit);

	        String downloadLink = "https://gccservices.in/gccofficialapp/api/pdf/template?" +
	                "cdid=" + row.get("cdid") +
	                "&ciid=" + row.get("ciid");

	        row.put("downloadNoticeLink", downloadLink);
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Construction List with Guidelines & Answers.");
	    response.put("data", result);

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveFinalNoticeDetails(
	        String cdid,
	        String ciid,
	        String giid,
	        String remarks,
	        String status,
	        String zone,
	        String ward,
	        String cby,
	        String latitude,
	        String longitude,
	        MultipartFile mainfile
	) {
	    Map<String, Object> response = new HashMap<>();
	    List<Map<String, Object>> result = new ArrayList<>();

	    String constimg = fileUpload("Final_Notice",cdid+"_"+ciid+"_"+giid+"_"+status , mainfile);
	    
	    String type = "";
	    
	    // Prepare question image uploads
	    MultipartFile[] files = {
	    	mainfile
	    };
	    
	    // Insert query
	    String insertSql = "INSERT INTO `final_notice_inspection`(`cdid`, `ciid`, `giid`, `image1`, `remarks`, `zone`, `ward`, `latitude`, `longitude`, `cby`, `status`) VALUES "
	    		+ "(?,?,?,?,?,?,?,?,?,?,?)";

	    KeyHolder keyHolder = new GeneratedKeyHolder();

	    int affectedRows = jdbcGuidlines.update(connection -> {
	        PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"cdid"});
	        int i = 1;
	        ps.setString(i++, cdid);
	        ps.setString(i++, ciid);
	        ps.setString(i++, giid);
	        ps.setString(i++, constimg);
	        ps.setString(i++, remarks);
	        ps.setString(i++, zone);
	        ps.setString(i++, ward);
	        ps.setString(i++, latitude);
	        ps.setString(i++, longitude);
	        ps.setString(i++, cby);
	        ps.setString(i++, status);
	        return ps;
	    }, keyHolder);

	    if (affectedRows > 0) {
	    	int lastInsertId = keyHolder.getKey().intValue();
	    	response.put("insertId", lastInsertId);
	    	response.put("status", "success");
	    	response.put("message", "New Final Notice details inserted successfully.");   
	    } else {
	        response.put("status", "error");
	        response.put("message", "New Final Notice details  insert failed.");
	    }

	    result.add(response);
	    return result;
	}
	
	public Map<String, Object> getNoticeData(String cdidtxt, String ciidtxt) {
		
		 String sql = "SELECT cd.*, "
		            + "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', cd.image) AS photo, "
		            + "DATE_FORMAT(cd.cdate, '%d-%m-%Y') AS ddate, "
		            + "ci.ciid, ci.under_guidelines, ci.penalty "
		            + "FROM construction_details cd "
		            + "LEFT JOIN construction_inspection ci ON ci.cdid = cd.cdid "
		            + "WHERE cd.isactive = 1 AND ci.isactive = 1 AND ci.penalty IN ('High','Medium','Low') AND cd.cdid=? AND ci.ciid=?";

		    List<Map<String, Object>> result = jdbcGuidlines.queryForList(sql, cdidtxt, ciidtxt);
		    
		    Map<String, Object> data = new HashMap<>();
		    
		    // Fetch master guideline list only once
		    String guidelinesSql = "SELECT `gid`, `guideline`, `significance`, `orderby`, `isactive` FROM guidelines WHERE isactive = 1 ORDER BY orderby";
		    List<Map<String, Object>> guidelineList = jdbcGuidlines.queryForList(guidelinesSql);

		    for (Map<String, Object> row : result) {
		        Object cdid = row.get("cdid");
		        Object ciid = row.get("ciid");

		        // Fetch answer row for this cdid + ciid
		        String ansSql = "SELECT `giid`, `cdate`, `q1`, `q2`, `q3`, `q4`, `q5`, `q6`, `q7`, `q8`, `q9`, `q10`, `q11`, `q12`, `q13`, `q14`, `q15`, `q16`, `q17`, `q18`, "
		        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', img1) AS photo1, "
		        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', img2) AS photo2, "
		        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', img3) AS photo3, "
		        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', img4) AS photo4, "
		        		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', img5) AS photo5 "
		        		+ "FROM guildelines_inspection WHERE isactive = 1 AND cdid = ? AND ciid = ? LIMIT 1";
		        List<Map<String, Object>> ansList = jdbcGuidlines.queryForList(ansSql, cdid, ciid);

		        Map<String, Object> answers = ansList.isEmpty() ? new HashMap<>() : ansList.get(0);

		        // Merge each guideline with its corresponding answer
		        List<Map<String, Object>> guidelineWithAnswers = new ArrayList<>();
		        List<Integer> clauseList = new ArrayList<>();
		        List<Map<String, Object>> violations = new ArrayList<>();
		        for (int i = 1; i <= guidelineList.size(); i++) {
		            Map<String, Object> guideline = guidelineList.get(i - 1);
		            String qKey = "q" + i;
		            Map<String, Object> qa = new LinkedHashMap<>();
		            String ans = String.valueOf(answers.get(qKey));
		            if("No".equalsIgnoreCase(ans)) {
		            	Map<String, Object> v = new HashMap<>();
		                v.put("message", guideline.get("guideline"));  // You can also include clause if needed
		                violations.add(v);
		                clauseList.add(i);
		            }
		        }
		        
		        String buildupAreaStr = row.get("buildup_area").toString();
		        int buildup_area = Integer.parseInt(buildupAreaStr);
		    
		        System.out.println("buildup_area : "+buildup_area);

		        String penaltyCategory = row.get("penalty").toString(); // should be "High" or "Low"
		        
		        System.out.println("penaltyCategory : "+penaltyCategory);
		        
		        int fineAmount = 0;

		        if (buildup_area > 20000) {
		            if ("High".equalsIgnoreCase(penaltyCategory)) {
		                fineAmount = 500000;
		            } else {
		                fineAmount = 100000;
		            }
		        } else if (buildup_area > 500) {
		            if ("High".equalsIgnoreCase(penaltyCategory)) {
		                fineAmount = 25000;
		            } else {
		                fineAmount = 10000;
		            }
		        } else if (buildup_area >= 300 && buildup_area <= 500) {
		            if ("High".equalsIgnoreCase(penaltyCategory)) {
		                fineAmount = 10000;
		            } else {
		                fineAmount = 1000;
		            }
		        }
		        System.out.println("fineAmount : "+fineAmount);
		        
		        data.put("siteLocation", row.get("site_address"));
		        data.put("violations", violations);
		        data.put("violationLevel", row.get("penalty"));
		        data.put("clause", clauseList);
		        data.put("fineAmount", fineAmount);
		        data.put("fineAmountWords", convertNumberToIndianWords(fineAmount));
		        data.put("date", row.get("ddate"));
		    }
        return data;
	}
	
	public static String convertNumberToIndianWords(long number) {
	    if (number == 0) return "Zero";

	    final String[] units = { "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", 
	                             "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", 
	                             "Seventeen", "Eighteen", "Nineteen" };

	    final String[] tens = { "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety" };

	    final String[] places = { "", "Thousand", "Lakh", "Crore" };

	    StringBuilder words = new StringBuilder();

	    int[] parts = new int[4];
	    parts[0] = (int) (number % 1000);            // hundreds
	    parts[1] = (int) ((number / 1000) % 100);     // thousands
	    parts[2] = (int) ((number / 100000) % 100);   // lakhs
	    parts[3] = (int) (number / 10000000);         // crores

	    for (int i = 3; i >= 0; i--) {
	        if (parts[i] != 0) {
	            if (parts[i] < 20) {
	                words.append(units[parts[i]]);
	            } else {
	                words.append(tens[parts[i] / 10]);
	                if ((parts[i] % 10) != 0) {
	                    words.append(" ").append(units[parts[i] % 10]);
	                }
	            }
	            words.append(" ").append(places[i]).append(" ");
	        }
	    }

	    if (parts[0] > 0) {
	        int h = parts[0] / 100;
	        int rem = parts[0] % 100;
	        if (h > 0) {
	            words.append(units[h]).append(" Hundred ");
	        }
	        if (rem > 0) {
	            if (rem < 20) {
	                words.append(units[rem]);
	            } else {
	                words.append(tens[rem / 10]);
	                if ((rem % 10) != 0) {
	                    words.append(" ").append(units[rem % 10]);
	                }
	            }
	        }
	    }

	    return words.toString().trim();
	}
}
