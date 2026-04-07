package in.gov.chennaicorporation.mobileservice.foodDistribution.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.css.style.derived.StringValue;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class PmcService {

    private JdbcTemplate jdbcPmcTemplate;

    private final Environment environment;
    private String fileBaseUrl;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();

    @Autowired
    public void setDataSource(@Qualifier("mysqlGccFoodDistributionSource") DataSource FoodDistributionSource) {
        this.jdbcPmcTemplate = new JdbcTemplate(FoodDistributionSource);
    }

    @Autowired
    public PmcService(Environment environment) {
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

    public String fileUpload(String name, String id, MultipartFile file, String filetype) {

        int lastInsertId = 0;
        // Set the file path where you want to save it
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("fooddistribution_foldername");
        var year = DateTimeUtil.getCurrentYear();
        var month = DateTimeUtil.getCurrentMonth();
        var date = DateTimeUtil.getCurrentDay();

        uploadDirectory = uploadDirectory + serviceFolderName + "pmc/" + filetype + "/" + year + "/" + month + "/"
                + date;
        
        System.out.println("Uploading file: " + file.getOriginalFilename());
        System.out.println("Upload directory: " + uploadDirectory);


        try {
        	   // Create directory if it doesn't exist
            Path directoryPath = Paths.get(uploadDirectory);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
                System.out.println("Directory created: " + directoryPath);
            }

            // Datetime string
            String datetimetxt = DateTimeUtil.getCurrentDateTime();

            datetimetxt = datetimetxt + "_" + generateRandomFileString(6); // Attached Random text

            // File name
//            String fileName = name + "_" + id + "_" + datetimetxt + "_" + file.getOriginalFilename();
//            fileName = fileName.replaceAll("\\s+", ""); // Remove space on filename
            //
            String originalFileName = file.getOriginalFilename();

            System.out.println("Original filename BEFORE fix: " + originalFileName);

            if (originalFileName == null || originalFileName.trim().isEmpty()) {
                originalFileName = "q_" + id + "_" + System.currentTimeMillis() + ".jpg";
            }

            // optional sanitize
            originalFileName = originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");

            // File name
            String fileName = name + "_" + id + "_" + datetimetxt + "_" + originalFileName;
            fileName = fileName.replaceAll("\\s+", "");
            
            //

            String filePath = uploadDirectory + "/" + fileName;

            String filepath_txt = "/" + serviceFolderName + "pmc/" + filetype + "/" + year + "/" + month + "/" + date
                    + "/" + fileName;

            // Create a new Path object
            Path path = Paths.get(filePath);
            
            
            System.out.println("Uploading file: " + file.getOriginalFilename());
            System.out.println("Upload directory: " + uploadDirectory);
            System.out.println("Full file path: " + filePath);
            System.out.println("Directory exists: " + Files.exists(directoryPath));
            System.out.println("Directory writable: " + Files.isWritable(directoryPath));
            System.out.println("File size: " + file.getSize());
            System.out.println("Content type: " + file.getContentType());

            // Get the bytes of the file
            byte[] bytes = file.getBytes();

            if (filetype.equalsIgnoreCase("image")) {
                // Compress the image
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50% quality
                // Write the bytes to the file
                Files.write(path, compressedBytes);
            } else {
               
            	
            	System.out.println("File size: " + file.getSize());
            	System.out.println("Content type: " + file.getContentType());
            	 // Write the bytes to the file
                Files.write(path, bytes);
            }
            // Get current date & time
            LocalDateTime now = LocalDateTime.now();

            // Format date-time (optional)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            System.out.println("Date: " + now.format(formatter));
            System.out.println("Activity: SluicePoint");
            System.out.println("File Type: " + filetype);
            System.out.println("File Upload Path: " + filePath);
            System.out.println("File Path: " + filepath_txt);

            return filepath_txt;

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to save file " + file.getOriginalFilename());
            //return "error";
            
            
            return e.getMessage();
        }
    }

    public List<Map<String, Object>> getConfig(String loginid) {

        String sql = "SELECT * FROM `shift_master` WHERE `isactive`=1 AND `isdelete`=0 ORDER BY orderby";
        List<Map<String, Object>> configDetails = jdbcPmcTemplate.queryForList(sql);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "Success");
        response.put("message", "Configuration Details.");
        response.put("data", configDetails);

        return Collections.singletonList(response);
    }

    public List<Map<String, Object>> getFinalFoodCount(int shiftid, int loginid, String date) {

        String searchDate = convertDateFormat(date, 0);
        // System.out.println("searchDate="+searchDate);

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();

        try {

            // 0️ CHECK DUPLICATE ENTRY IN pmc_audit
            String auditCheckSql = " SELECT COUNT(*) "
                    + "	            FROM pmc_audit "
                    + "	            WHERE shiftid = ? "
                    + "	            AND audit_date = ? "
                    + "	            AND cby = ? "
                    + "	            AND isactive = 1 "
                    + "	            AND isdelete = 0 ";

            Integer auditCount = jdbcPmcTemplate.queryForObject(
                    auditCheckSql,
                    Integer.class,
                    shiftid,
                    searchDate,
                    loginid);

            if (auditCount != null && auditCount > 0) {
                response.put("message", "Data already saved for this hub");
                response.put("status", "Failed");
                response.put("feedbackstatus", "completed");
                return Collections.singletonList(response);

            }

            // 1️ Get hub_id safely
            String hubSql = " SELECT hub_id  "
                    + "	            FROM driver_login  "
                    + "	            WHERE loginid = ? "
                    + "	            AND isactive = 1 "
                    + "	            AND isdelete = 0 ";

            List<Integer> hubList = jdbcPmcTemplate.query(
                    hubSql,
                    (rs, rowNum) -> rs.getInt("hub_id"),
                    loginid);

            if (hubList.isEmpty() || hubList.get(0) == null || hubList.get(0) == 0) {
                response.put("message", "No hub mapped for login id");
                response.put("status", "Failed");
                return Collections.singletonList(response);

            }

            Integer hub_id = hubList.get(0);

            // 🔹 Zone-wise Sum
            String zoneSql = " SELECT  "
                    + "	                lm.zone, "
                    + "	                SUM( "
                    + "	                    COALESCE(dr.permanent,0) + "
                    + "	                    COALESCE(dr.nulm,0) + "
                    + "	                    COALESCE(dr.private,0) + "
                    + "	                    COALESCE(dr.nmr,0) + "
                    + "	                    COALESCE(dr.others,0) "
                    + "	                ) AS total_count "
                    + "	            FROM daily_request dr "
                    + "	            JOIN location_mapping lm  "
                    + "	                ON dr.request_by = lm.siloginid "
                    + "	            WHERE dr.shiftid = ? "
                    + "	            AND dr.required_date = ? "
                    + "	            AND dr.hub_id = ? "
                    + "	            AND dr.isactive = 1 "
                    + "	            AND dr.isdelete = 0 "
                    + "	            GROUP BY lm.zone ";

            List<Map<String, Object>> zoneData = jdbcPmcTemplate.queryForList(zoneSql, shiftid, searchDate, hub_id);

            // 🔹 3️ Total Sum
            String totalSql = " SELECT  "
                    + "	                SUM( "
                    + "	                    COALESCE(permanent,0) + "
                    + "	                    COALESCE(nulm,0) + "
                    + "	                    COALESCE(private,0) + "
                    + "	                    COALESCE(nmr,0) + "
                    + "	                    COALESCE(others,0) "
                    + "	                ) "
                    + "	            FROM daily_request "
                    + "	            WHERE shiftid = ? "
                    + "	            AND required_date = ? "
                    + "	            AND hub_id = ? "
                    + "	            AND isactive = 1 "
                    + "	            AND isdelete = 0 ";

            Integer totalCount = jdbcPmcTemplate.queryForObject(
                    totalSql, Integer.class,
                    shiftid, searchDate, hub_id);

            // 🔹 Shift
            String shiftSql = " SELECT  "
                    + "	                name "
                    + "	            FROM shift_master "
                    + "	            WHERE shiftid = ? "
                    + "	            AND isactive = 1 "
                    + "	            AND isdelete = 0 ";

            String shift_name = jdbcPmcTemplate.queryForObject(
                    shiftSql, String.class,
                    shiftid);

            if (totalCount == null) {
                totalCount = 0;
            }

            // 🔹 4️ If no data found
            if (zoneData.isEmpty() || totalCount == 0) {
                response.put("message", "No data available for this hub");
                response.put("status", "Failed");
                return Collections.singletonList(response);

            }

            // 🔹 5️ Success Response
            data.put("zonedata", zoneData);
            data.put("total_food_count", totalCount);

            response.put("hub_id", hub_id);
            response.put("feedbackstatus", "pending");
            response.put("shift_name", shift_name);
            response.put("req_date", date);
            response.put("data", data);
            response.put("message", "Zone Food count Details.");
            response.put("status", "Success");

        } catch (Exception e) {
            response.put("message", "Error in getting total food count");
            response.put("status", "Failed");
            e.printStackTrace();
        }

        return Collections.singletonList(response);

    }

    public List<Map<String, Object>> getfeedbackquestions(String loginId) {

        Map<String, Object> response = new HashMap<>();

        try {

            // 1️ Get Questions
            String questionSql = " SELECT qid, q_english, q_tamil, question_type, field_name, orderby "
                    + "	            FROM pmc_questions_master "
                    + "	            WHERE isactive = 1 AND isdelete = 0 "
                    + "	            ORDER BY orderby ";

            List<Map<String, Object>> questions = jdbcPmcTemplate.queryForList(questionSql);

            // 2 Get Answers
            String answerSql = " SELECT aid, qid, english_name, orderby "
                    + "	            FROM pmc_answer_master "
                    + "	            WHERE isactive = 1 AND isdelete = 0 "
                    + "	            ORDER BY qid, orderby ";

            List<Map<String, Object>> answers = jdbcPmcTemplate.queryForList(answerSql);

            // 3️ Group answers by qid
            Map<Integer, List<Map<String, Object>>> answerMap = new HashMap<>();

            for (Map<String, Object> ans : answers) {

                Integer qid = (Integer) ans.get("qid");

                Map<String, Object> option = new HashMap<>();
                option.put("value", ans.get("aid"));
                option.put("option_id", ans.get("aid"));
                option.put("english_name", ans.get("english_name"));
                option.put("orderby", ans.get("orderby"));

                answerMap.computeIfAbsent(qid, k -> new ArrayList<>())
                        .add(option);
            }

            // 4 Build final question list
            List<Map<String, Object>> finalList = new ArrayList<>();

            for (Map<String, Object> ques : questions) {

                Integer qid = (Integer) ques.get("qid");

                Map<String, Object> questionObj = new HashMap<>();

                questionObj.put("id", qid);
                questionObj.put("question_type", ques.get("question_type"));
                questionObj.put("q_english", ques.get("q_english"));
                questionObj.put("q_tamil", ques.get("q_tamil"));
                questionObj.put("field_name", ques.get("field_name"));
                questionObj.put("image_field_name", ques.get("field_name") + "_image");
                questionObj.put("orderby", ques.get("orderby"));

                questionObj.put("isactive", true);
                questionObj.put("img_required", false);
                questionObj.put("pid", 0);
                questionObj.put("showonreport", true);
                questionObj.put("group_title", "");

                // Attach options
                questionObj.put("options",
                        answerMap.getOrDefault(qid, new ArrayList<>()));

                finalList.add(questionObj);
            }

            response.put("data", finalList);
            response.put("message", "PMC feedback Question List.");
            response.put("status", "Success");

        } catch (Exception e) {
            response.put("message", "Failed to fetch questions");
            response.put("status", "Failed");
            e.printStackTrace();
        }

        return Collections.singletonList(response);
    }

    public List<Map<String, Object>> getParentQuestionsList(String loginId) {
        String sql = "SELECT "
                + "    ql.*, "
                + "    CASE   "
                + "        WHEN (ql.question_type = 'select' OR ql.question_type = 'radio') AND COUNT(qov.aid) > 0 THEN JSON_ARRAYAGG( "
                + "            JSON_OBJECT( "
                + "                'option_id', qov.aid, "
                + "                'english_name', qov.english_name,"
                + "				   'tamil_name',qov.tamil_name, "
                + "				   'opt_mandatory',qov.opt_mandatory, "
                + "                'value', qov.aid, "
                + "				   'remarksfield', (qov.remarks_required = 1), "
                + "				   'textfield', (qov.text_required = 1), "
                + "				   'imgfield', (qov.img_required = 1), "
                + "				   'textname', qov.text_name, "
                + "                'orderby', qov.orderby "
                + "            ) "
                + "        ) "
                + "        ELSE JSON_ARRAY()  "
                + "    END AS options "
                + "FROM pmc_questions_master ql "
                + "LEFT JOIN pmc_answer_master qov  "
                + "    ON qov.qid = ql.qid  "
                + "    AND qov.isactive = 1  "
                + "    AND qov.isdelete = 0 "
                + "WHERE ql.isactive = 1  "
                + "GROUP BY ql.qid";

        List<Map<String, Object>> result = jdbcPmcTemplate.queryForList(sql);
        Iterator<Map<String, Object>> iterator = result.iterator();
        ObjectMapper mapper = new ObjectMapper();
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
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Success");
        response.put("message", "PMC feedback Question List.");
        response.put("data", result);

        return Collections.singletonList(response);
    }

    public List<?> saveFeedback(String loginId, String auditdate, String shiftid, String latitude, String longitude,
            String zone, String ward,
            String address, String final_food_count, String foodid, String food_others, String hub_id,
            List<String> questionParams, MultipartFile image1, MultipartFile image2, MultipartFile image3,
            MultipartFile image4,
            MultipartFile image5) {

        Map<String, Object> response = new HashMap<>();

        try {

            String searchDate = convertDateFormat(auditdate, 0);

            // Insert into pmc_audit
            String auditSql = " INSERT INTO pmc_audit "
                    + "	            (audit_date, shiftid, zone, ward, address, "
                    + "	             final_food_count, foodid, food_others, cby, hub_id,latitude,longitude) "
                    + "	            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?) ";

            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcPmcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        auditSql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, searchDate);
                ps.setInt(2, Integer.parseInt(shiftid));
                ps.setString(3, zone);
                ps.setString(4, ward);
                ps.setString(5, address);
                ps.setInt(6, Integer.parseInt(final_food_count));
                ps.setInt(7, Integer.parseInt(foodid));
                if (food_others != null && !food_others.trim().isEmpty()) {
                    ps.setString(8, food_others.trim());
                } else {
                    ps.setNull(8, Types.VARCHAR);
                }
                ps.setInt(9, Integer.parseInt(loginId));
                ps.setInt(10, Integer.parseInt(hub_id));
                ps.setString(11, latitude);
                ps.setString(12, longitude);
                return ps;
            }, keyHolder);

            int auditId = keyHolder.getKey().intValue();

            // Insert into pmc_feedback (Loop q1–q18)
            String feedbackSql = " INSERT INTO pmc_feedback "
                    + "	            (pmc_audit_id, questions, answer, remarks, penalty_count) "
                    + "	            VALUES (?, ?, ?, ?, ?) ";

            for (String param : questionParams) {

                String[] parts = param.split(",", -1);

                Integer questionId = Integer.parseInt(parts[0]);
                Integer answerId = Integer.parseInt(parts[1]);

                String remarks = null;
                String penalty = null;

                if (parts.length >= 3 && !parts[2].trim().isEmpty()) {
                    remarks = parts[2];
                }

                if (parts.length >= 4 && !parts[3].trim().isEmpty()) {
                    penalty = parts[3];
                }

                jdbcPmcTemplate.update(feedbackSql,
                        auditId,
                        questionId,
                        answerId,
                        remarks,
                        penalty);
            }

            // Save Images
            String imgSql = " INSERT INTO pmc_feedback_img "
                    + "	            (pmc_audit_id, image1, image2, image3, image4, image5) "
                    + "	            VALUES (?, ?, ?, ?, ?, ?) ";

            // public String fileUpload(String name, String id, MultipartFile file, String
            // filetype)

            String pmcmid = String.valueOf(auditId);

            jdbcPmcTemplate.update(imgSql,
                    auditId,
                    fileUpload("image1", pmcmid, image1, "pmcfeedback"),
                    fileUpload("image2", pmcmid, image2, "pmcfeedback"),
                    fileUpload("image3", pmcmid, image3, "pmcfeedback"),
                    fileUpload("image4", pmcmid, image4, "pmcfeedback"),
                    fileUpload("image5", pmcmid, image5, "pmcfeedback"));

            response.put("status", "Success");
            response.put("message", "Feedback Saved Successfully");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "Failed");
            response.put("message", "Error Saving Feedback");
        }

        return Collections.singletonList(response);
    }

    public List<Map<String, Object>> getFinalFoodCountForDispatch(int shiftid, int loginid, String date) {

        String searchDate = convertDateFormat(date, 0);
        Map<String, Object> response = new HashMap<>();

        try {

            // 1️ Get hub_id safely
            String hubSql = " SELECT hub_id  "
                    + "	            FROM driver_login  "
                    + "	            WHERE loginid = ? "
                    + "	            AND isactive = 1 "
                    + "	            AND isdelete = 0 ";

            List<Integer> hubList = jdbcPmcTemplate.query(
                    hubSql,
                    (rs, rowNum) -> rs.getInt("hub_id"),
                    loginid);

            if (hubList.isEmpty() || hubList.get(0) == null || hubList.get(0) == 0) {
                response.put("message", "No hub mapped for login id");
                response.put("status", "Failed");
                return Collections.singletonList(response);

            }

            Integer hub_id = hubList.get(0);

            String zoneSql = " SELECT * FROM pmc_audit WHERE audit_date=? AND shiftid=? AND cby=? AND isactive=1 ";

            List<Map<String, Object>> pmcData = jdbcPmcTemplate.queryForList(zoneSql, searchDate, shiftid, loginid);
            // System.out.println("pmcData="+pmcData);

            int finalFoodCount = 0;
            int pmc_audit_id = 0;
            int yet_dispatch_count = 0;
            List<Map<String, Object>> location_foodcount = new ArrayList<>();
            List<Map<String, Object>> already_assigned = new ArrayList<>();
            if (pmcData.isEmpty()) {
                response.put("message", "No Audit data available for this kitchen");
                response.put("status", "Failed");
                return Collections.singletonList(response);

            } else {

                String wardSql = " SELECT  lm.id,lm.location,dr.ward,"
                        + "	                SUM( "
                        + "	                    COALESCE(dr.permanent,0) + "
                        + "	                    COALESCE(dr.nulm,0) + "
                        + "	                    COALESCE(dr.private,0) + "
                        + "	                    COALESCE(dr.nmr,0) + "
                        + "	                    COALESCE(dr.others,0) "
                        + "	                ) as foodcount "
                        + "	            FROM daily_request dr "
                        + " LEFT JOIN location_mapping lm ON lm.ward=dr.ward "
                        + "	            WHERE dr.shiftid = ? "
                        + "	            AND dr.required_date = ? "
                        + "	            AND dr.hub_id = ? "
                        + "	            AND dr.isactive = 1 "
                        + "	            AND dr.isdelete = 0 "
                        + " GROUP BY lm.id,lm.location,dr.ward"
                        + " ORDER BY dr.ward ";

                location_foodcount = jdbcPmcTemplate.queryForList(wardSql, shiftid, searchDate, hub_id);

                // System.out.println("location_foodcount="+location_foodcount);

                Map<String, Object> pmcRow = pmcData.get(0); // first row

                if (pmcRow.get("final_food_count") != null) {
                    finalFoodCount = ((Number) pmcRow.get("final_food_count")).intValue();
                }
                if (pmcRow.get("id") != null) {
                    pmc_audit_id = ((Number) pmcRow.get("id")).intValue();
                }

                // System.out.println("Final Food Count = " + finalFoodCount);
                // System.out.println("pmc id = " + pmc_audit_id);
            }

            if (!pmcData.isEmpty() && pmc_audit_id != 0) {

                String totalSql = " SELECT  "
                        + "	       COALESCE(SUM(food_count),0) FROM pmc_dispatch_food_counts WHERE pmc_audit_id=? "
                        + "	            AND isactive = 1 "
                        + "	            AND isdelete = 0 ";

                Integer dispatchfoodCount = jdbcPmcTemplate.queryForObject(
                        totalSql, Integer.class,
                        pmc_audit_id);

                yet_dispatch_count = finalFoodCount - dispatchfoodCount;
                // System.out.println(yet_dispatch_count);

                String assigned_locationsql = "SELECT delivery_location FROM pmc_dispatch_food_counts WHERE pmc_audit_id=? AND isactive=1 AND isdelete=0 ";
                already_assigned = jdbcPmcTemplate.queryForList(assigned_locationsql, pmc_audit_id);
                // System.out.println("already_assigned="+already_assigned);

                Set<Integer> assignedIds = already_assigned.stream()
                        .map(m -> ((Number) m.get("delivery_location")).intValue())
                        .collect(Collectors.toSet());

                location_foodcount = location_foodcount.stream()
                        .filter(loc -> !assignedIds.contains(((Number) loc.get("id")).intValue()))
                        .collect(Collectors.toList());

                // System.out.println("Filtered location_foodcount=" + location_foodcount);
            }

            // 🔹 Shift
            String shiftSql = " SELECT  name "
                    + "	            FROM shift_master "
                    + "	            WHERE shiftid = ? "
                    + "	            AND isactive = 1 "
                    + "	            AND isdelete = 0 ";

            String shift_name = jdbcPmcTemplate.queryForObject(
                    shiftSql, String.class,
                    shiftid);

            response.put("shift_name", shift_name);
            response.put("req_date", date);
            response.put("data", pmcData);

            if (yet_dispatch_count >= 0) {
                response.put("yet_dispatch_count", yet_dispatch_count);
            } else {
                response.put("yet_dispatch_count", 0);
            }
            // response.put("hub_id", hub_id);
            // response.put("pmc_audit_id", pmc_audit_id);
            response.put("location_foodcount", location_foodcount);

            response.put("message", "Food count Details for Dispatch.");
            response.put("status", "Success");

        } catch (Exception e) {
            response.put("message", "Error in getting food count Details for Dispatch.");
            response.put("status", "Failed");
            e.printStackTrace();
        }

        return Collections.singletonList(response);

    }

    public List<?> savedispatch(int pmc_audit_id, String driver_name, String driver_mob_num, String vehicle_number,
            MultipartFile packedfoodphoto, MultipartFile vehiclephoto, String loginId, int yet_dispatch_count,
            String dispatch_food_list) {

        Map<String, Object> response = new HashMap<>();

        try {

            if (dispatch_food_list.isEmpty()) {
                response.put("status", "Failed");
                response.put("message", "Dispatching Location Details is Empty.");
            } else if (yet_dispatch_count <= 0) {
                response.put("status", "Failed");
                response.put("message", "Dispatch Count is Zero or less than Zero.");
            } else {
                ObjectMapper mapper = new ObjectMapper();

                List<Map<String, Object>> dispatchList = mapper.readValue(dispatch_food_list, List.class);

                String pmc_audit_id_str = Integer.toString(pmc_audit_id);

                String packedfoodphoto_url = fileUpload("packedfoodphoto", pmc_audit_id_str, packedfoodphoto,
                        "packedfood");
                String vehiclephoto_url = fileUpload("vehiclephoto", pmc_audit_id_str, vehiclephoto, "vehiclephoto");

                // Insert into pmc_audit
                String auditSql = " INSERT INTO pmc_dispatch "
                        + "	            (pmc_audit_id,driver_name,driver_mob_num,vehicle_number,packed_food_url,vehicle_photo_url,cby) "
                        + "	            VALUES (?,?,?,?,?,?,?) ";

                KeyHolder keyHolder = new GeneratedKeyHolder();

                jdbcPmcTemplate.update(con -> {
                    PreparedStatement ps = con.prepareStatement(
                            auditSql, Statement.RETURN_GENERATED_KEYS);
                    ps.setInt(1, pmc_audit_id);
                    ps.setString(2, driver_name);
                    ps.setString(3, driver_mob_num);
                    ps.setString(4, vehicle_number);
                    ps.setString(5, packedfoodphoto_url);
                    ps.setString(6, vehiclephoto_url);
                    ps.setInt(7, Integer.parseInt(loginId));
                    return ps;
                }, keyHolder);
                int pmc_dispatch_id = keyHolder.getKey().intValue();

                String foodInsertSql = "INSERT INTO pmc_dispatch_food_counts "
                        + "(pmc_dispatch_id, delivery_location, food_count, pmc_audit_id) "
                        + "VALUES (?,?,?,?)";

                for (Map<String, Object> row : dispatchList) {

                    int locationId = ((Number) row.get("id")).intValue();
                    int foodCount = ((Number) row.get("foodcount")).intValue();

                    jdbcPmcTemplate.update(foodInsertSql,
                            pmc_dispatch_id,
                            locationId,
                            foodCount,
                            pmc_audit_id);
                }

                response.put("status", "Success");
                response.put("message", "Dispatch Saved Successfully");
            }

        } catch (Exception e) {
            response.put("status", "Failed");
            response.put("message", "Error Saving in Dispatch");
            e.printStackTrace();
        }

        return Collections.singletonList(response);
    }

    public List<Map<String, Object>> getvehicleallocated(int shiftid, int loginid, String date) {

        String searchDate = convertDateFormat(date, 0);
        Map<String, Object> response = new HashMap<>();

        try {

            String zoneSql = " SELECT * FROM pmc_audit WHERE audit_date=? AND shiftid=? AND cby=? AND isactive=1 ";

            List<Map<String, Object>> pmcData = jdbcPmcTemplate.queryForList(zoneSql, searchDate, shiftid, loginid);
            int pmc_audit_id = 0;
            if (pmcData.isEmpty()) {
                response.put("message", "No Audit data available for this kitchen");
                response.put("status", "Failed");
                return Collections.singletonList(response);

            } else {
                pmc_audit_id = ((Number) pmcData.get(0).get("id")).intValue();
            }

            String dsql = "SELECT COALESCE(SUM(food_count),0) FROM pmc_dispatch_food_counts WHERE pmc_audit_id=? AND isactive=1 AND isdelete=0";
            Integer total_food = jdbcPmcTemplate.queryForObject(
                    dsql, Integer.class,
                    pmc_audit_id);

            String sql = "SELECT d.id AS dispatch_id, d.driver_name, d.driver_mob_num, d.vehicle_number, d.pmc_audit_id,CONCAT('"
                    + fileBaseUrl + "/gccofficialapp/files', d.vehicle_photo_url) AS img_full_path, " +
                    "f.id AS food_id, f.delivery_location, f.food_count,lm.location as location,lm.si_name as si_name,lm.si_number as si_ph_no  "
                    +
                    "FROM pmc_dispatch d " +
                    "LEFT JOIN pmc_dispatch_food_counts f ON d.id = f.pmc_dispatch_id AND f.isactive=1 AND f.isdelete=0 "
                    +
                    " LEFT JOIN location_mapping lm ON lm.id=f.delivery_location " +
                    "WHERE d.pmc_audit_id=? AND d.isactive=1 AND d.isdelete=0";

            List<Map<String, Object>> rows = jdbcPmcTemplate.queryForList(sql, pmc_audit_id);

            Map<Integer, Map<String, Object>> dispatchMap = new LinkedHashMap<>();  

            for (Map<String, Object> row : rows) {

                int dispatchId = ((Number) row.get("dispatch_id")).intValue();

                // Create dispatch if not exists
                if (!dispatchMap.containsKey(dispatchId)) {

                    Map<String, Object> dispatch = new HashMap<>();
                    dispatch.put("id", dispatchId);
                    dispatch.put("driver_name", row.get("driver_name"));
                    dispatch.put("driver_mob_num", row.get("driver_mob_num"));
                    dispatch.put("vehicle_number", row.get("vehicle_number"));
                    dispatch.put("pmc_audit_id", row.get("pmc_audit_id"));
                    dispatch.put("img_full_path", row.get("img_full_path"));
                    dispatch.put("food_counts", new ArrayList<>());

                    dispatchMap.put(dispatchId, dispatch);
                }

                // Add food data if exists
                if (row.get("food_id") != null) {

                    Map<String, Object> food = new HashMap<>();
                    food.put("id", row.get("food_id"));
                    food.put("delivery_location", row.get("delivery_location"));
                    food.put("food_count", row.get("food_count"));
                    food.put("location", row.get("location"));
                    food.put("si_name", row.get("si_name"));
                    food.put("si_ph_no", row.get("si_ph_no"));

                    ((List<Map<String, Object>>) dispatchMap.get(dispatchId).get("food_counts")).add(food);
                }
            }

            for (Map<String, Object> dispatch : dispatchMap.values()) {

                List<Map<String, Object>> foodList = (List<Map<String, Object>>) dispatch.get("food_counts");

                dispatch.put("no_of_location", foodList.size());
            }

            // 🔹 Shift
            String shiftSql = " SELECT  name "
                    + "	            FROM shift_master "
                    + "	            WHERE shiftid = ? "
                    + "	            AND isactive = 1 "
                    + "	            AND isdelete = 0 ";

            String shift_name = jdbcPmcTemplate.queryForObject(
                    shiftSql, String.class,
                    shiftid);

            response.put("shift_name", shift_name);
            response.put("req_date", date);

            response.put("data", new ArrayList<>(dispatchMap.values()));
            response.put("total_food", total_food);
            response.put("message", "Dispatched Vehicle Details");
            response.put("status", "Success");

        } catch (Exception e) {
            response.put("status", "Failed");
            response.put("message", "Error in getting Dispatched details");
            e.printStackTrace();
        }

        return Collections.singletonList(response);
    }
    
    
    public List<Map<String, Object>> getFinalFoodCountPerHubId(int shiftid, int loginid, String date) {

        String searchDate = convertDateFormat(date, 0);
        // System.out.println("searchDate="+searchDate);

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();

        try {

           
            // 1️ Get hub_id safely
            String hubSql = " SELECT hub_id  "
                    + "	            FROM driver_login  "
                    + "	            WHERE loginid = ? "
                    + "	            AND isactive = 1 "
                    + "	            AND isdelete = 0 ";

            List<Integer> hubList = jdbcPmcTemplate.query(
                    hubSql,
                    (rs, rowNum) -> rs.getInt("hub_id"),
                    loginid);

            if (hubList.isEmpty() || hubList.get(0) == null || hubList.get(0) == 0) {
                response.put("message", "No hub mapped for login id");
                response.put("status", "Failed");
                return Collections.singletonList(response);

            }

            Integer hub_id = hubList.get(0);

            // 🔹 Zone-wise Sum
            String zoneSql = " SELECT  "
                    + "	                lm.location, "
                    + "	                SUM( "
                    + "	                    COALESCE(dr.permanent,0) + "
                    + "	                    COALESCE(dr.nulm,0) + "
                    + "	                    COALESCE(dr.private,0) + "
                    + "	                    COALESCE(dr.nmr,0) + "
                    + "	                    COALESCE(dr.others,0) "
                    + "	                ) AS total_count "
                    + "	            FROM daily_request dr "
                    + "	            left JOIN location_mapping lm  "
                    + "	                ON dr.request_by = lm.siloginid AND dr.ward=lm.ward "
                    + "	            WHERE dr.shiftid = ? "
                    + "	            AND dr.required_date = ? "
                    + "	            AND dr.hub_id = ? "
                    + "	            AND dr.isactive = 1 "
                    + "	            AND dr.isdelete = 0 "
                    + "	            GROUP BY lm.location ";

            List<Map<String, Object>> zoneData = jdbcPmcTemplate.queryForList(zoneSql, shiftid, searchDate, hub_id);

            // 🔹 3️ Total Sum
            String totalSql = " SELECT  "
                    + "	                SUM( "
                    + "	                    COALESCE(permanent,0) + "
                    + "	                    COALESCE(nulm,0) + "
                    + "	                    COALESCE(private,0) + "
                    + "	                    COALESCE(nmr,0) + "
                    + "	                    COALESCE(others,0) "
                    + "	                ) "
                    + "	            FROM daily_request "
                    + "	            WHERE shiftid = ? "
                    + "	            AND required_date = ? "
                    + "	            AND hub_id = ? "
                    + "	            AND isactive = 1 "
                    + "	            AND isdelete = 0 ";

            Integer totalCount = jdbcPmcTemplate.queryForObject(
                    totalSql, Integer.class,
                    shiftid, searchDate, hub_id);

            // 🔹 Shift
            String shiftSql = " SELECT  "
                    + "	                name "
                    + "	            FROM shift_master "
                    + "	            WHERE shiftid = ? "
                    + "	            AND isactive = 1 "
                    + "	            AND isdelete = 0 ";

            String shift_name = jdbcPmcTemplate.queryForObject(
                    shiftSql, String.class,
                    shiftid);

            if (totalCount == null) {
                totalCount = 0;
            }

            // 🔹 4️ If no data found
            if (zoneData.isEmpty() || totalCount == 0) {
                response.put("message", "No data available for this hub");
                response.put("status", "Failed");
                return Collections.singletonList(response);

            }

            // 🔹 5️ Success Response
            data.put("locationwisedata", zoneData);
            data.put("total_food_count", totalCount);

            response.put("hub_id", hub_id);
            response.put("feedbackstatus", "pending");
            response.put("shift_name", shift_name);
            response.put("req_date", date);
            response.put("data", data);
            response.put("message", "Zone Food count Details.");
            response.put("status", "Success");

        } catch (Exception e) {
            response.put("message", "Error in getting total food count");
            response.put("status", "Failed");
            e.printStackTrace();
        }

        return Collections.singletonList(response);

    }
    
    
//    public List<Map<String, Object>> getNotFilledCategories(int shiftid, int loginid, String date) {
//		
//    	
//    	String searchDate = convertDateFormat(date, 0);
//        // System.out.println("searchDate="+searchDate);
//
//        Map<String, Object> response = new HashMap<>();
//    	try {
//    		
//    		
//    		String zoneSql = " SELECT qcm_id FROM pmc_audit WHERE isactive=1 AND isdelete=0 AND shiftid=? AND audit_date=? AND cby=? ";
//
//            List<Map<String, Object>> filled_categories = jdbcPmcTemplate.queryForList(zoneSql, shiftid, searchDate, loginid);
//            
//            String allCategories_sql= "SELECT * FROM questions_category_master WHERE isactive=1 AND isdelete=0 ORDER BY qcm_id ";
//            
//            List<Map<String, Object>> all_categories = jdbcPmcTemplate.queryForList(allCategories_sql);
//            
//            // Collect filled qcm_id into Set
//            Set<Integer> filledIds = new HashSet<>();
//            for (Map<String, Object> row : filled_categories) {
//                filledIds.add(((Number) row.get("qcm_id")).intValue());
//            }
//
//            // Filter all_categories
//            List<Map<String, Object>> notFilledCategories = new ArrayList<>();
//            for (Map<String, Object> row : all_categories) {
//                Integer qcmId = ((Number) row.get("qcm_id")).intValue();
//                if (!filledIds.contains(qcmId)) {
//                    notFilledCategories.add(row);
//                }
//            }
//            
//            int completed_categories=filled_categories.size();
//            int total_categories=all_categories.size();
//            int pending_categories=total_categories-completed_categories;
//            
//            List<Map<String, Object>> countdata = new ArrayList<>();
//
//            Map<String, Object> countMap = new HashMap<>();
//            countMap.put("completed_categories", completed_categories);
//            countMap.put("total_categories", total_categories);
//            countMap.put("pending_categories", pending_categories);
//
//            countdata.add(countMap);
//                    
//            response.put("data", notFilledCategories);
//            response.put("countdata", countdata);
//    		
//    		 response.put("message", "Fetched UnFilled Categories.");
//             response.put("status", "Success");
//			
//		} catch (Exception e) {
//			response.put("message", "Error in getting UnFilled Categories");
//            response.put("status", "Failed");
//            e.printStackTrace();
//		}
//    	
//    	return Collections.singletonList(response);
//	}
    
    
    public List<Map<String, Object>> getNotFilledCategories(int shiftid, int loginid, String date) {

        String searchDate = convertDateFormat(date, 0);
        Map<String, Object> response = new HashMap<>();

        try {

            //   1. Get hub_id
            String hubSql = "SELECT hub_id FROM driver_login WHERE loginid=? AND isactive=1 AND isdelete=0";

            List<Integer> hubList = jdbcPmcTemplate.query(
                    hubSql,
                    (rs, rowNum) -> rs.getInt("hub_id"),
                    loginid
            );

            if (hubList.isEmpty() || hubList.get(0) == null || hubList.get(0) == 0) {
                response.put("message", "No hub mapped for login id");
                response.put("status", "Failed");
                return Collections.singletonList(response);
            }

            Integer hub_id = hubList.get(0);

            //   2. Get all categories with frequency + last filled date
            String sql = 
                "SELECT " +
                " qcm.qcm_id, qcm.audit_category,qcm.img_url, qcm.orderby, " +
                " fm.days, fm.frequency_name, " +
                " MAX(pa.audit_date) AS last_filled_date " +
                "FROM questions_category_master qcm " +
                "JOIN frequency_master fm ON fm.id = qcm.frequency_master_id " +
                "LEFT JOIN pmc_audit pa " +
                " ON pa.qcm_id = qcm.qcm_id " +
                " AND pa.cby = ? " +
                " AND pa.hub_id = ? " +
                " AND pa.isactive = 1 " +
                " AND pa.isdelete = 0 " +
                "WHERE qcm.isactive = 1 AND qcm.isdelete = 0 " +
                "GROUP BY qcm.qcm_id " +
                "ORDER BY qcm.orderby";

            List<Map<String, Object>> allCategories = jdbcPmcTemplate.queryForList(sql, loginid, hub_id);

            List<Map<String, Object>> filteredCategories = new ArrayList<>();

            LocalDate today = LocalDate.parse(searchDate);

            int completed = 0;

            //   3. Apply frequency logic
            for (Map<String, Object> row : allCategories) {

                Integer qcmId = ((Number) row.get("qcm_id")).intValue();
                Integer days = ((Number) row.get("days")).intValue();

                Date lastDateObj = (Date) row.get("last_filled_date");

                boolean shouldShow = false;
                boolean isCompleted = false;

                // ✅ DAILY (days = 1)
                if (days == 1) {
                    shouldShow = true;

                    if (lastDateObj != null && lastDateObj.toLocalDate().equals(today)) {
                        isCompleted = true;
                    }
                }

                // ✅ WEEKLY / MONTHLY / YEARLY
                else {

                    // ❗ Only Shift B allowed
                    if (shiftid != 2) {
                        continue;
                    }

                    if (lastDateObj == null) {
                        shouldShow = true;
                    } else {
                        LocalDate lastDate = lastDateObj.toLocalDate();
                        LocalDate nextEligibleDate = lastDate.plusDays(days);

                        if (!today.isBefore(nextEligibleDate)) {
                            shouldShow = true;
                        } else {
                            isCompleted = true;
                        }
                    }
                }

                if (shouldShow) {
                    filteredCategories.add(row);
                }

                if (isCompleted) {
                    completed++;
                }
            }

            int total = allCategories.size();
            int pending = filteredCategories.size();

            // ✅ 4. Count data
            List<Map<String, Object>> countdata = new ArrayList<>();

            Map<String, Object> countMap = new HashMap<>();
            countMap.put("completed_categories", completed);
            countMap.put("total_categories", total);
            countMap.put("pending_categories", pending);

            countdata.add(countMap);

            //   5. Response
            response.put("data", filteredCategories);
            //response.put("countdata", countdata);
            response.put("message", "Filtered Categories based on frequency");
            response.put("status", "Success");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Error in getting categories");
            response.put("status", "Failed");
        }

        return Collections.singletonList(response);
    }
    
    
    
    
//    public List<Map<String, Object>> getquestionsbycat(int qcm_id,int loginid,String latitude,String longitude) {
//    	
//    	Map<String, Object> response = new HashMap<>();
//    	
//    	// 1️ Get hub_id safely
//        String hubSql = " SELECT hub_id  "
//                + "	            FROM driver_login  "
//                + "	            WHERE loginid = ? "
//                + "	            AND isactive = 1 "
//                + "	            AND isdelete = 0 ";
//
//        List<Integer> hubList = jdbcPmcTemplate.query(
//                hubSql,
//                (rs, rowNum) -> rs.getInt("hub_id"),
//                loginid);
//
//        if (hubList.isEmpty() || hubList.get(0) == null || hubList.get(0) == 0) {
//            response.put("message", "No hub mapped for login id");
//            response.put("status", "Failed");
//            return Collections.singletonList(response);
//
//        }
//
//        Integer hub_id = hubList.get(0);
//        
//        String hubLatLongSql = "SELECT latitude, longitude,radius_range  FROM hub_master WHERE id=? AND is_active=1 AND is_delete=0";
//
//        Map<String, Object> hubLocation = jdbcPmcTemplate.queryForMap(hubLatLongSql, hub_id);
//
//        String hubLat = hubLocation.get("latitude") != null ? hubLocation.get("latitude").toString() : null;
//        String hubLng = hubLocation.get("longitude") != null ? hubLocation.get("longitude").toString() : null;
//        Double radiusRange = hubLocation.get("radius_range") != null 
//                ? Double.parseDouble(hubLocation.get("radius_range").toString()) 
//                : 200.0; 
//        
//        if (latitude != null && longitude != null && !latitude.isBlank() && !longitude.isBlank()
//                && hubLat != null && hubLng != null) {
//
//            String distanceSql =
//                    "SELECT (6371008.8 * acos(ROUND(" +
//                    "cos(radians(?)) * cos(radians(?)) * " +
//                    "cos(radians(?) - radians(?)) + " +
//                    "sin(radians(?)) * sin(radians(?))" +
//                    ", 9))) AS distance";
//
//            Double distance = jdbcPmcTemplate.queryForObject(
//                    distanceSql,
//                    Double.class,
//                    Double.parseDouble(latitude),   // user lat
//                    Double.parseDouble(hubLat),     // hub lat
//                    Double.parseDouble(hubLng),     // hub lng
//                    Double.parseDouble(longitude),  // user lng
//                    Double.parseDouble(latitude),
//                    Double.parseDouble(hubLat)
//            );
//            
//            System.out.println("distance="+distance);
//
//            if (distance == null || distance > radiusRange) {
//                response.put("status", "Failed");
//                response.put("message", "You are not within " + radiusRange + " meters of the kitchen location");
//                return Collections.singletonList(response);
//            }
//        }
//        
//    			
//    	String sql = "SELECT "
//                + "    ql.*, "
//                + "    CASE   "
//                + "        WHEN (ql.question_type = 'select' OR ql.question_type = 'radio') AND COUNT(qov.aid) > 0 THEN JSON_ARRAYAGG( "
//                + "            JSON_OBJECT( "
//                + "                'option_id', qov.aid, "
//                + "                'english_name', qov.english_name,"
//                + "				   'tamil_name',qov.tamil_name, "
//                + "				   'opt_mandatory',qov.opt_mandatory, "
//                + "                'value', qov.aid, "
//                + "				   'remarksfield', (qov.remarks_required = 1), "
//                + "				   'imgfield', (qov.img_required = 1), "
//                + "				   'textfield', (qov.text_required = 1), "
//                + "                'orderby', qov.orderby "
//                + "            ) "
//                + "        ) "
//                + "        ELSE JSON_ARRAY()  "
//                + "    END AS options "
//                + "FROM pmc_questions_master ql "
//                + "LEFT JOIN pmc_answer_master qov  "
//                + "    ON qov.qid = ql.qid  "
//                + "    AND qov.isactive = 1  "
//                + "    AND qov.isdelete = 0 "
//                + "WHERE ql.isactive = 1 AND ql.qcm_id=? "
//                + "GROUP BY ql.qid";
//
//        List<Map<String, Object>> result = jdbcPmcTemplate.queryForList(sql,qcm_id);
//        Iterator<Map<String, Object>> iterator = result.iterator();
//        ObjectMapper mapper = new ObjectMapper();
//        while (iterator.hasNext()) {
//            Map<String, Object> row = iterator.next();
//            Object optionsRaw = row.get("options");
//            if (optionsRaw != null && optionsRaw instanceof String) {
//                try {
//                    List<Map<String, Object>> optionsParsed = mapper.readValue((String) optionsRaw, List.class);
//
//                    // Sort options by 'orderby'
//                    optionsParsed.sort(Comparator.comparing(opt -> {
//                        Object order = opt.get("orderby");
//                        return (order instanceof Number) ? ((Number) order).intValue() : 0;
//                    }));
//
//                    row.put("options", optionsParsed);
//                } catch (Exception e) {
//                    row.put("options", null); // fallback if malformed
//                }
//            }
//        }
//        
//        String cat_sql="SELECT * FROM questions_category_master WHERE isactive=1 AND isdelete=0 AND qcm_id=? ";
//        List<Map<String, Object>> category_details = jdbcPmcTemplate.queryForList(cat_sql,qcm_id);
//        
//        
//        response.put("status", "Success");
//        response.put("message", "PMC feedback Question List.");
//        response.put("data", result);
//        response.put("category_details", category_details);
//
//        return Collections.singletonList(response);
//	}
    
    
    public List<Map<String, Object>> getquestionsbycat(int qcm_id, int loginid, int shiftid, String latitude, String longitude) {
    	Map<String, Object> response = new HashMap<>();

        try {

            // ✅ 1. Get hub_id
            String hubSql = "SELECT hub_id FROM driver_login WHERE loginid=? AND isactive=1 AND isdelete=0";

            List<Integer> hubList = jdbcPmcTemplate.query(
                    hubSql,
                    (rs, rowNum) -> rs.getInt("hub_id"),
                    loginid
            );

            if (hubList.isEmpty() || hubList.get(0) == null || hubList.get(0) == 0) {
                response.put("message", "No hub mapped for login id");
                response.put("status", "Failed");
                return Collections.singletonList(response);
            }

            Integer hub_id = hubList.get(0);
            
         // ✅ 2. Get frequency + last filled date
            String freqSql =
                    "SELECT fm.days, MAX(pa.audit_date) AS last_filled_date " +
                    "FROM questions_category_master qcm " +
                    "JOIN frequency_master fm ON fm.id = qcm.frequency_master_id " +
                    "LEFT JOIN pmc_audit pa " +
                    " ON pa.qcm_id = qcm.qcm_id " +
                    " AND pa.cby = ? " +
                    " AND pa.hub_id = ? " +
                    " AND pa.isactive = 1 AND pa.isdelete = 0 " +
                    "WHERE qcm.qcm_id = ? " +
                    "GROUP BY qcm.qcm_id";

            Map<String, Object> freqData = jdbcPmcTemplate.queryForMap(freqSql, loginid, hub_id, qcm_id);

            int days = ((Number) freqData.get("days")).intValue();

            Object lastDateObj = freqData.get("last_filled_date");

            LocalDate today = LocalDate.now();
            
            // ✅ 3. Apply frequency logic

            // DAILY
            if (days == 1) {
                // allow always
            }

            // WEEKLY / MONTHLY / YEARLY
            else {

                if (shiftid != 2) {
                    response.put("status", "Failed");
                    response.put("message", "This category allowed only for Shift B");
                    return Collections.singletonList(response);
                }

                if (lastDateObj != null) {

                    LocalDate lastDate = null;

                    if (lastDateObj instanceof java.sql.Date) {
                        lastDate = ((java.sql.Date) lastDateObj).toLocalDate();
                    }

                    LocalDate nextEligibleDate = lastDate.plusDays(days);

                    if (today.isBefore(nextEligibleDate)) {
                        response.put("status", "Failed");
                        response.put("message", "This category already submitted. Next available after " + nextEligibleDate);
                        return Collections.singletonList(response);
                    }
                }
            }
            
            String hubLatLongSql = "SELECT latitude, longitude, radius_range FROM hub_master WHERE id=? AND is_active=1 AND is_delete=0";

            Map<String, Object> hubLocation = jdbcPmcTemplate.queryForMap(hubLatLongSql, hub_id);

            String hubLat = hubLocation.get("latitude") != null ? hubLocation.get("latitude").toString() : null;
            String hubLng = hubLocation.get("longitude") != null ? hubLocation.get("longitude").toString() : null;
            Double radiusRange = hubLocation.get("radius_range") != null
                    ? Double.parseDouble(hubLocation.get("radius_range").toString())
                    : 200.0;

            if (latitude != null && longitude != null && !latitude.isBlank() && !longitude.isBlank()
                    && hubLat != null && hubLng != null) {

                String distanceSql =
                        "SELECT (6371008.8 * acos(ROUND(" +
                                "cos(radians(?)) * cos(radians(?)) * " +
                                "cos(radians(?) - radians(?)) + " +
                                "sin(radians(?)) * sin(radians(?))" +
                                ", 9))) AS distance";

                Double distance = jdbcPmcTemplate.queryForObject(
                        distanceSql,
                        Double.class,
                        Double.parseDouble(latitude),
                        Double.parseDouble(hubLat),
                        Double.parseDouble(hubLng),
                        Double.parseDouble(longitude),
                        Double.parseDouble(latitude),
                        Double.parseDouble(hubLat)
                );

                if (distance == null || distance > radiusRange) {
                    response.put("status", "Failed");
                    response.put("message", "You are not within " + radiusRange + " meters of the kitchen location");
                    return Collections.singletonList(response);
                }
            }
            
        	String sql = "SELECT "
          + "    ql.*, "
          + "    CASE   "
          + "        WHEN (ql.question_type = 'select' OR ql.question_type = 'radio') AND COUNT(qov.aid) > 0 THEN JSON_ARRAYAGG( "
          + "            JSON_OBJECT( "
          + "                'option_id', qov.aid, "
          + "                'english_name', qov.english_name,"
          + "				   'tamil_name',qov.tamil_name, "
          + "				   'opt_mandatory',qov.opt_mandatory, "
          + "                'value', qov.aid, "
          + "				   'remarksfield', (qov.remarks_required = 1), "
          + "				   'imgfield', (qov.img_required = 1), "
          + "				   'textfield', (qov.text_required = 1), "
          + "                'orderby', qov.orderby "
          + "            ) "
          + "        ) "
          + "        ELSE JSON_ARRAY()  "
          + "    END AS options "
          + "FROM pmc_questions_master ql "
          + "LEFT JOIN pmc_answer_master qov  "
          + "    ON qov.qid = ql.qid  "
          + "    AND qov.isactive = 1  "
          + "    AND qov.isdelete = 0 "
          + "WHERE ql.isactive = 1 AND ql.qcm_id=? "
          + "GROUP BY ql.qid";
            
        	 List<Map<String, Object>> result = jdbcPmcTemplate.queryForList(sql,qcm_id);
           Iterator<Map<String, Object>> iterator = result.iterator();
           ObjectMapper mapper = new ObjectMapper();
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
           }
           
           String cat_sql = "SELECT * FROM questions_category_master WHERE isactive=1 AND isdelete=0 AND qcm_id=?";
           List<Map<String, Object>> category_details = jdbcPmcTemplate.queryForList(cat_sql, qcm_id);

           response.put("status", "Success");
           response.put("message", "PMC feedback Question List.");
           response.put("data", result);
           response.put("category_details", category_details);

       } catch (Exception e) {
           e.printStackTrace();
           response.put("status", "Failed");
           response.put("message", "Error while fetching questions");
       }

       return Collections.singletonList(response);
            
    }
    
    @Transactional
    public List<?> saveFeedbackbycat(
            String loginId, String auditdate, String shiftid,
            String latitude, String longitude, String zone, String ward, String address,
            String final_food_count, String foodid, String food_others,
            String hub_id, String qcm_id,
            String questionAnswers, MultipartFile[] images) {
    	
    	System.out.println("images="+images);

        Map<String, Object> response = new HashMap<>();

        try {

            String formattedDate = convertDateFormat(auditdate, 0);
            
            final int finalFoodCount = 
            	    (final_food_count != null && !final_food_count.trim().isEmpty())
            	        ? Integer.parseInt(final_food_count.trim())
            	        : 0;

            //   1. Insert into pmc_audit
            String auditSql = "INSERT INTO pmc_audit (audit_date, shiftid, latitude, longitude, zone, ward, address, qcm_id, final_food_count, foodid, food_others, cby, hub_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcPmcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(auditSql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, formattedDate);
                ps.setInt(2, Integer.parseInt(shiftid));
                ps.setString(3, latitude);
                ps.setString(4, longitude);
                ps.setString(5, zone);
                ps.setString(6, ward);
                ps.setString(7, address);
                ps.setInt(8, Integer.parseInt(qcm_id));
                ps.setInt(9, finalFoodCount);
                ps.setInt(10, Integer.parseInt(foodid));
                ps.setString(11, food_others);
                ps.setInt(12, Integer.parseInt(loginId));
                ps.setInt(13, Integer.parseInt(hub_id));
                return ps;
            }, keyHolder);

            int auditId = keyHolder.getKey().intValue();
            String auditIdStr = String.valueOf(auditId);

            //   2. Parse JSON
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> qaList = mapper.readValue(
                    questionAnswers,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            //   3. Prepare Image Map (qid → file)
            Map<Integer, MultipartFile> imageMap = new HashMap<>();

            if (images != null) {
                for (MultipartFile file : images) {

                    String fileName = file.getOriginalFilename();

                    if (fileName != null && fileName.startsWith("q_")) {

                        try {
                            // Example: q_43_filename.jpg
                            String[] parts = fileName.split("_");

                            if (parts.length >= 2) {
                                Integer qidFromFile = Integer.parseInt(parts[1]);
                                imageMap.put(qidFromFile, file);
                            }

                        } catch (Exception e) {
                            System.out.println("Invalid filename format: " + fileName);
                        }
                    }
                }
            }

            //   4. Insert into pmc_feedback
            String feedbackSql = "INSERT INTO pmc_feedback (pmc_audit_id, questions, answer, remarks, image,qcmid,hub_id,penaltycount) VALUES (?, ?, ?, ?, ?,?,?,?)";

            for (Map<String, Object> qa : qaList) {

                // skip invalid
                if (qa.get("qid") == null || qa.get("value") == null) continue;

                Integer qid = Integer.parseInt(qa.get("qid").toString());
                Integer value = Integer.parseInt(qa.get("value").toString());
                String remarks = qa.get("remarks") != null ? qa.get("remarks").toString() : null;
                Integer textfield = (qa.get("textfield") != null && !qa.get("textfield").toString().trim().isEmpty())
                        ? Integer.parseInt(qa.get("textfield").toString())
                        : null;

                String imagePath = null;

                //   NEW: get image using qid
                MultipartFile file = imageMap.get(qid);

                if (file != null && !file.isEmpty()) {

                    imagePath = fileUpload(
                            "pmc_feedback_img",
                            auditIdStr,
                            file,
                            "pmcfeedback"
                    );
                }

                //   insert feedback row
                jdbcPmcTemplate.update(feedbackSql,
                        auditId,
                        qid,
                        value,
                        remarks,
                        imagePath,
                        Integer.parseInt(qcm_id),
                        Integer.parseInt(hub_id),
                        textfield
                );
            }

            response.put("status", "Success");
            response.put("message", "Feedback Saved Successfully");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "Failed");
            response.put("message", "Error while saving feedback");
        }

        return Collections.singletonList(response);
    }

	public List<Map<String, Object>> getselectedmenu(int shiftid, int loginid, String date) {
		
		String formattedDate = convertDateFormat(date, 0);
		 Map<String, Object> response = new HashMap<>();
		try {
			
			// 1️ Get hub_id safely
            String hubSql = " SELECT hub_id  "
                    + "	            FROM driver_login  "
                    + "	            WHERE loginid = ? "
                    + "	            AND isactive = 1 "
                    + "	            AND isdelete = 0 ";

            List<Integer> hubList = jdbcPmcTemplate.query(
                    hubSql,
                    (rs, rowNum) -> rs.getInt("hub_id"),
                    loginid);

            if (hubList.isEmpty() || hubList.get(0) == null || hubList.get(0) == 0) {
                response.put("message", "No hub mapped for login id");
                response.put("status", "Failed");
                return Collections.singletonList(response);

            }

            Integer hub_id = hubList.get(0);
            
            String totalSql = " SELECT  "
                    + "	                SUM( "
                    + "	                    COALESCE(permanent,0) + "
                    + "	                    COALESCE(nulm,0) + "
                    + "	                    COALESCE(private,0) + "
                    + "	                    COALESCE(nmr,0) + "
                    + "	                    COALESCE(others,0) "
                    + "	                ) "
                    + "	            FROM daily_request "
                    + "	            WHERE shiftid = ? "
                    + "	            AND required_date = ? "
                    + "	            AND hub_id = ? "
                    + "	            AND isactive = 1 "
                    + "	            AND isdelete = 0 ";

            Integer totalCount = jdbcPmcTemplate.queryForObject(
                    totalSql, Integer.class,
                    shiftid, formattedDate, hub_id);
            
            
    		String sql="SELECT pa.foodid,flm.name FROM pmc_audit pa LEFT JOIN food_list_master flm ON flm.foodid=pa.foodid WHERE flm.isactive=1 AND date(pa.audit_date)=? AND pa.shiftid=? AND pa.cby=? LIMIT 1";

    		List<Map<String, Object>> food_details=jdbcPmcTemplate.queryForList(sql,formattedDate,shiftid,loginid);
    		
    		response.put("totalCount",totalCount);
    		response.put("food_details",food_details);
    		
    		response.put("message", "Food Details.");
            response.put("status", "Success");
			
		} catch (Exception e) {
			 response.put("message", "Error in getting Food Details");
	            response.put("status", "Failed");
	            e.printStackTrace();
	        }

	        return Collections.singletonList(response);
		
	}

	public List<Map<String, Object>> getfeedbackreport(int shiftid, int loginid, String date) {

	    String formattedDate = convertDateFormat(date, 0);
	    Map<String, Object> response = new HashMap<>();

	    try {

	        //   1. Get hub_id
	        String hubSql = "SELECT hub_id FROM driver_login WHERE loginid=? AND isactive=1 AND isdelete=0";

	        List<Integer> hubList = jdbcPmcTemplate.query(
	                hubSql,
	                (rs, rowNum) -> rs.getInt("hub_id"),
	                loginid
	        );

	        if (hubList.isEmpty() || hubList.get(0) == null || hubList.get(0) == 0) {
	            response.put("message", "No hub mapped for login id");
	            response.put("status", "Failed");
	            return Collections.singletonList(response);
	        }

	        Integer hub_id = hubList.get(0);

	        //   2. Check audit data exists
	        StringBuilder auditCheckSql = new StringBuilder(
	        	    "SELECT id FROM pmc_audit WHERE shiftid=? AND hub_id=? AND isactive=1 AND isdelete=0 "
	        	);

	        	List<Object> auditParams = new ArrayList<>();
	        	auditParams.add(shiftid);
	        	auditParams.add(hub_id);

	        	if (date != null && !date.trim().isEmpty()) {
	        	    auditCheckSql.append(" AND audit_date=? ");
	        	    auditParams.add(formattedDate);
	        	}

	        	List<Integer> auditIds = jdbcPmcTemplate.query(
	        	        auditCheckSql.toString(),
	        	        (rs, rowNum) -> rs.getInt("id"),
	        	        auditParams.toArray()
	        	);

	        if (auditIds.isEmpty()) {
	            response.put("message", "No Audit data for "+date+".");
	            response.put("status", "Failed");
	            return Collections.singletonList(response);
	        }

	        //   3. Fetch full flat data
	        StringBuilder reportSql = new StringBuilder();

	        reportSql.append(
	            "SELECT pa.id AS audit_id, pa.qcm_id, qcm.audit_category, " +
	            "pf.questions AS qid, pq.q_english AS question, " +
	            "pf.answer AS aid, pam.english_name AS answer, " +
	            "IFNULL(pf.remarks, '') AS remarks, " +
	            "IFNULL(pf.image, '') AS image, " +
	            "qcm.penalty_amt, CAST(pam.opt_mandatory AS UNSIGNED) AS opt_mandatory, IFNULL(pf.penaltycount,0) AS penaltycount, " +
	            "CASE " +
	            " WHEN pf.image IS NULL OR pf.image = '' " +
	            " THEN '' " +
	            " ELSE CONCAT('" + fileBaseUrl + "/gccofficialapp/files', pf.image) " +
	            "END AS img_full_path "+
	            "FROM pmc_audit pa " +
	            "JOIN pmc_feedback pf ON pf.pmc_audit_id = pa.id AND pf.isactive=1 AND pf.isdelete=0 " +
	            "JOIN pmc_questions_master pq ON pq.qid = pf.questions AND pq.isactive=1 AND pq.isdelete=0 " +
	            "JOIN pmc_answer_master pam ON pam.aid = pf.answer AND pam.isactive=1 AND pam.isdelete=0 " +
	            "JOIN questions_category_master qcm ON qcm.qcm_id = pa.qcm_id AND qcm.isactive=1 AND qcm.isdelete=0 " +
	            "WHERE pa.shiftid=? AND pa.hub_id=? AND pa.isactive=1 AND pa.isdelete=0 "
	        );
	        
	        

	        List<Object> params = new ArrayList<>();

	        params.add(shiftid);
	        params.add(hub_id);

	        if (date != null && !date.trim().isEmpty()) {
	            reportSql.append(" AND pa.audit_date=? ");
	            params.add(formattedDate);
	        }
	        
	        reportSql.append(" ORDER BY pa.qcm_id, pq.orderby ");
	        
	        List<Map<String, Object>> flatData = jdbcPmcTemplate.queryForList(
	                reportSql.toString(),
	                params.toArray()
	        );

	        //   4. Convert to nested structure
	        Map<Integer, Map<String, Object>> categoryMap = new LinkedHashMap<>();

	        Map<Integer, Double> categoryPenaltyMap = new HashMap<>();
	        for (Map<String, Object> row : flatData) {

	            Integer qcmId = (Integer) row.get("qcm_id");

	            // 🔹 create category if not exists
	            if (!categoryMap.containsKey(qcmId)) {

	                Map<String, Object> category = new HashMap<>();
	                category.put("qcm_id", qcmId);
	                category.put("category", row.get("audit_category"));
	                category.put("questions", new ArrayList<>());

	                categoryMap.put(qcmId, category);
	            }

	            // 🔹 build question object
	            Map<String, Object> question = new HashMap<>();
	            question.put("qid", row.get("qid"));
	            question.put("question", row.get("question"));
	            question.put("aid", row.get("aid"));
	            question.put("answer", row.get("answer"));
	            question.put("remarks", row.get("remarks"));
	            //question.put("image", row.get("image"));
	            question.put("img_full_path", row.get("img_full_path"));
	            question.put("penaltycount", row.get("penaltycount"));
	            
	            Double penaltyAmt = row.get("penalty_amt") != null
	                    ? ((Number) row.get("penalty_amt")).doubleValue()
	                    : 0.0;

	            Integer penaltyCount = row.get("penaltycount") != null
	                    ? ((Number) row.get("penaltycount")).intValue()
	                    : 0;

	            Integer optMandatory = row.get("opt_mandatory") != null
	                    ? ((Number) row.get("opt_mandatory")).intValue()
	                    : 0;

	            double finalPenalty = 0;

	            //   ONLY if opt_mandatory = 1
	            if (optMandatory == 1) {

	                if (qcmId == 11 || qcmId == 12) {
	                    // 🔥 multiply logic
	                    finalPenalty = penaltyAmt * penaltyCount;
	                } else {
	                    // 🔥 normal logic
	                    finalPenalty = penaltyAmt;
	                }
	            }
	            
	            question.put("penalty_amt", finalPenalty);
	            question.put("penaltycount", penaltyCount);
	            
	            categoryPenaltyMap.put(
	            	    qcmId,
	            	    categoryPenaltyMap.getOrDefault(qcmId, 0.0) + finalPenalty
	            	);

	            // 🔹 add to category
	            List<Map<String, Object>> questions =
	                    (List<Map<String, Object>>) categoryMap.get(qcmId).get("questions");

	            questions.add(question);
	        }
	        
	        for (Map.Entry<Integer, Map<String, Object>> entry : categoryMap.entrySet()) {

	            Integer qcmId = entry.getKey();
	            Map<String, Object> category = entry.getValue();

	            double totalPenalty = categoryPenaltyMap.getOrDefault(qcmId, 0.0);

	            category.put("cat_penalty_amt", totalPenalty);
	        }

	        //   5. Final response
	        List<Map<String, Object>> finalData = new ArrayList<>(categoryMap.values());

	        response.put("data", finalData);
	        response.put("message", "Food Feedback Details");
	        response.put("status", "Success");

	    } catch (Exception e) {
	        e.printStackTrace();
	        response.put("message", "Error in getting food feedback reports");
	        response.put("status", "Failed");
	    }

	    return Collections.singletonList(response);
	}

	public List<Map<String, Object>> getFoodCountForDispatch(int shiftid, int loginid, String date) {
		String searchDate = convertDateFormat(date, 0);
        Map<String, Object> response = new HashMap<>();
        
        try {
        	
        	// 1️ Get hub_id safely
            String hubSql = " SELECT hub_id  "
                    + "	            FROM driver_login  "
                    + "	            WHERE loginid = ? "
                    + "	            AND isactive = 1 "
                    + "	            AND isdelete = 0 ";

            List<Integer> hubList = jdbcPmcTemplate.query(
                    hubSql,
                    (rs, rowNum) -> rs.getInt("hub_id"),
                    loginid);

            if (hubList.isEmpty() || hubList.get(0) == null || hubList.get(0) == 0) {
                response.put("message", "No hub mapped for login id");
                response.put("status", "Failed");
                return Collections.singletonList(response);

            }

            Integer hub_id = hubList.get(0);

            int finalFoodCount = 0;
            int yet_dispatch_count = 0;
            
            List<Map<String, Object>> location_foodcount = new ArrayList<>();
            List<Map<String, Object>> already_assigned = new ArrayList<>();

            String totalSql = " SELECT  "
                    + "	                SUM( "
                    + "	                    COALESCE(permanent,0) + "
                    + "	                    COALESCE(nulm,0) + "
                    + "	                    COALESCE(private,0) + "
                    + "	                    COALESCE(nmr,0) + "
                    + "	                    COALESCE(others,0) "
                    + "	                ) "
                    + "	            FROM daily_request "
                    + "	            WHERE shiftid = ? "
                    + "	            AND required_date = ? "
                    + "	            AND hub_id = ? "
                    + "	            AND isactive = 1 "
                    + "	            AND isdelete = 0 ";

            Integer totalCount = jdbcPmcTemplate.queryForObject(
                    totalSql, Integer.class,
                    shiftid, searchDate, hub_id);
            
            finalFoodCount=totalCount;
            
            if(finalFoodCount<=0) {
            	response.put("message", "NO Food Count for this hub");
                response.put("status", "Failed");
            }
            else {
            	
            	response.put("totalCount", totalCount);
                
                
                String wardSql = " SELECT  lm.id,lm.location,dr.ward,"
                        + "	                SUM( "
                        + "	                    COALESCE(dr.permanent,0) + "
                        + "	                    COALESCE(dr.nulm,0) + "
                        + "	                    COALESCE(dr.private,0) + "
                        + "	                    COALESCE(dr.nmr,0) + "
                        + "	                    COALESCE(dr.others,0) "
                        + "	                ) as foodcount "
                        + "	            FROM daily_request dr "
                        + " LEFT JOIN location_mapping lm ON lm.ward=dr.ward "
                        + "	            WHERE dr.shiftid = ? "
                        + "	            AND dr.required_date = ? "
                        + "	            AND dr.hub_id = ? "
                        + "	            AND dr.isactive = 1 "
                        + "	            AND dr.isdelete = 0 "
                        + " GROUP BY lm.id,lm.location,dr.ward"
                        + " ORDER BY dr.ward ";

                location_foodcount = jdbcPmcTemplate.queryForList(wardSql, shiftid, searchDate, hub_id);
                
                String disSql = " SELECT  "
                        + "	       COALESCE(SUM(dfc.food_count),0) FROM pmc_dispatch pd LEFT JOIN pmc_dispatch_food_counts dfc ON dfc.pmc_dispatch_id=pd.id "
                        + " WHERE date(pd.send_date)=? AND pd.shift_id=? AND pd.hub_id=? "
                        + "  AND pd.isactive = 1 AND pd.isdelete = 0 AND dfc.isactive = 1 AND dfc.isdelete = 0 ";

                Integer dispatchfoodCount = jdbcPmcTemplate.queryForObject(
               		 disSql, Integer.class,
                        searchDate,shiftid,hub_id);
                
                yet_dispatch_count = finalFoodCount - dispatchfoodCount;

                String assigned_locationsql = "SELECT dfc.delivery_location  FROM pmc_dispatch pd LEFT JOIN pmc_dispatch_food_counts dfc ON dfc.pmc_dispatch_id=pd.id "
                		+ "  WHERE date(pd.send_date)=? AND pd.shift_id=? AND pd.hub_id=? "
                		+ "   AND pd.isactive = 1 AND pd.isdelete = 0 AND dfc.isactive = 1 AND dfc.isdelete = 0";
                already_assigned = jdbcPmcTemplate.queryForList(assigned_locationsql,  searchDate,shiftid,hub_id);
                // System.out.println("already_assigned="+already_assigned);
                
                Set<Integer> assignedIds = already_assigned.stream()
                        .map(m -> ((Number) m.get("delivery_location")).intValue())
                        .collect(Collectors.toSet());
                
                location_foodcount = location_foodcount.stream()
                        .filter(loc -> !assignedIds.contains(((Number) loc.get("id")).intValue()))
                        .collect(Collectors.toList());
                
                String shiftSql = " SELECT  name "
                        + "	            FROM shift_master "
                        + "	            WHERE shiftid = ? "
                        + "	            AND isactive = 1 "
                        + "	            AND isdelete = 0 ";

                String shift_name = jdbcPmcTemplate.queryForObject(
                        shiftSql, String.class,
                        shiftid);

                response.put("shift_name", shift_name);
                response.put("req_date", date);
                if (yet_dispatch_count >= 0) {
                    response.put("yet_dispatch_count", yet_dispatch_count);
                } else {
                    response.put("yet_dispatch_count", 0);
                }
                
                response.put("location_foodcount", location_foodcount);

           	    response.put("message", "Food count Details for Dispatch.");
                response.put("status", "Success");
            	
            }
        	
             			
		} catch (Exception e) {
			response.put("message", "Error in getting food count Details for Dispatch.");
            response.put("status", "Failed");
            e.printStackTrace();
		}
        return Collections.singletonList(response);


	}

	public List<?> savefordispatch(String auditdate, String shiftid, String hub_id, String driver_name,
			String driver_mob_num, String vehicle_number, MultipartFile packedfoodphoto, MultipartFile vehiclephoto,
			String loginId, int yet_dispatch_count, String dispatch_food_list) {
		
		 Map<String, Object> response = new HashMap<>();

		 String formattedDate = convertDateFormat(auditdate, 0);
		 System.out.println("formattedDate: " + formattedDate);
		 System.out.println("shiftid: " + shiftid);
		 System.out.println("hub_id: " + hub_id);
		 System.out.println("driver_name: " + driver_name);
		 System.out.println("dispatch_food_list: " + dispatch_food_list);
		 try {
			 
			 if (packedfoodphoto == null || packedfoodphoto.isEmpty()) {
				    response.put("status", "Failed");
				    response.put("message", "Packed food photo missing");
				    return Collections.singletonList(response);
				}
			 
			 if (dispatch_food_list.isEmpty()) {
	                response.put("status", "Failed");
	                response.put("message", "Dispatching Location Details is Empty.");
	                return Collections.singletonList(response);
	            } else if (yet_dispatch_count <= 0) {
	                response.put("status", "Failed");
	                response.put("message", "Dispatch Count is Zero or less than Zero.");
	                return Collections.singletonList(response);
	            }
	            else {
	            	ObjectMapper mapper = new ObjectMapper();
	            	List<Map<String, Object>> dispatchList = mapper.readValue(dispatch_food_list, List.class);
	            	
	            	String packedfoodphoto_url = fileUpload("packedfoodphoto", hub_id, packedfoodphoto,
	                        "packedfood");
	                String vehiclephoto_url = fileUpload("vehiclephoto", hub_id, vehiclephoto, "vehiclephoto");
	                
	                String auditSql = " INSERT INTO pmc_dispatch "
	                        + "	            (send_date,driver_name,driver_mob_num,vehicle_number,packed_food_url,vehicle_photo_url,cby,shift_id,hub_id) "
	                        + "	            VALUES (?,?,?,?,?,?,?,?,?) ";

	                KeyHolder keyHolder = new GeneratedKeyHolder();

	                jdbcPmcTemplate.update(con -> {
	                    PreparedStatement ps = con.prepareStatement(
	                            auditSql, Statement.RETURN_GENERATED_KEYS);
	                    ps.setString(1,formattedDate );
	                    ps.setString(2, driver_name);
	                    ps.setString(3, driver_mob_num);
	                    ps.setString(4, vehicle_number);
	                    ps.setString(5, packedfoodphoto_url);
	                    ps.setString(6, vehiclephoto_url);
	                    ps.setInt(7, Integer.parseInt(loginId));
	                    ps.setInt(8,Integer.parseInt(shiftid));
	                    ps.setInt(9,Integer.parseInt(hub_id));
	                    return ps;
	                }, keyHolder);
	                
	                int pmc_dispatch_id = keyHolder.getKey().intValue();

	                String foodInsertSql = "INSERT INTO pmc_dispatch_food_counts "
	                        + "(pmc_dispatch_id, delivery_location, food_count) "
	                        + "VALUES (?,?,?)";

	                for (Map<String, Object> row : dispatchList) {

	                    int locationId = ((Number) row.get("id")).intValue();
	                    int foodCount = ((Number) row.get("foodcount")).intValue();
	                    

	                    jdbcPmcTemplate.update(foodInsertSql,
	                            pmc_dispatch_id,
	                            locationId,
	                            foodCount);
	                }


	                response.put("status", "Success");
	                response.put("message", "Dispatch Saved Successfully");
	            }
	
		} catch (Exception e) {
			 response.put("status", "Failed");
	            response.put("message", e.getMessage());
	            e.printStackTrace();

		}
		
		 return Collections.singletonList(response);

	}

	public List<Map<String, Object>> getfoodswingData(int shiftid, int loginid, String date) {
		
		String formattedDate = convertDateFormat(date, 0);
	    Map<String, Object> response = new HashMap<>();

	    try {

	        //   1. Get hub_id
	        String hubSql = "SELECT hub_id FROM driver_login WHERE loginid=? AND isactive=1 AND isdelete=0";

	        List<Integer> hubList = jdbcPmcTemplate.query(
	                hubSql,
	                (rs, rowNum) -> rs.getInt("hub_id"),
	                loginid
	        );

	        if (hubList.isEmpty() || hubList.get(0) == null || hubList.get(0) == 0) {
	            response.put("message", "No hub mapped for login id");
	            response.put("status", "Failed");
	            return Collections.singletonList(response);
	        }

	        Integer hub_id = hubList.get(0);

	        //   2. Check audit data exists
	        StringBuilder auditCheckSql = new StringBuilder(
	        	    "SELECT id FROM pmc_audit WHERE shiftid=? AND hub_id=? AND isactive=1 AND isdelete=0 "
	        	);

	        	List<Object> auditParams = new ArrayList<>();
	        	auditParams.add(shiftid);
	        	auditParams.add(hub_id);

	        	if (date != null && !date.trim().isEmpty()) {
	        	    auditCheckSql.append(" AND audit_date=? ");
	        	    auditParams.add(formattedDate);
	        	}

	        	List<Integer> auditIds = jdbcPmcTemplate.query(
	        	        auditCheckSql.toString(),
	        	        (rs, rowNum) -> rs.getInt("id"),
	        	        auditParams.toArray()
	        	);

	        if (auditIds.isEmpty()) {
	            response.put("message", "No Audit data for "+date+".");
	            response.put("status", "Failed");
	            return Collections.singletonList(response);
	        }

	        //   3. Fetch full flat data
	        StringBuilder reportSql = new StringBuilder();

	        reportSql.append(
	            "SELECT pf.id as main_id,pa.id AS audit_id, pa.qcm_id, qcm.audit_category, " +
	            "pf.questions AS qid, pq.q_english AS question, " +
	            "pf.answer AS aid, pam.english_name AS answer, " +
	            "IFNULL(pf.remarks, '') AS remarks, " +
	            "IFNULL(pf.image, '') AS image, " +
	            "CASE " +
	            " WHEN pf.image IS NULL OR pf.image = '' " +
	            " THEN '' " +
	            " ELSE CONCAT('" + fileBaseUrl + "/gccofficialapp/files', pf.image) " +
	            "END AS img_full_path "+
	            "FROM pmc_audit pa " +
	            "JOIN pmc_feedback pf ON pf.pmc_audit_id = pa.id AND pf.isactive=1 AND pf.isdelete=0 AND pf.food_swing_sts is NULL " +
	            "JOIN pmc_questions_master pq ON pq.qid = pf.questions AND pq.isactive=1 AND pq.isdelete=0 " +
	            "JOIN pmc_answer_master pam ON pam.aid = pf.answer AND pam.isactive=1 AND pam.isdelete=0 AND pam.opt_mandatory=1 " +
	            "JOIN questions_category_master qcm ON qcm.qcm_id = pa.qcm_id AND qcm.isactive=1 AND qcm.isdelete=0 " +
	            "WHERE pa.shiftid=? AND pa.hub_id=? AND pa.isactive=1 AND pa.isdelete=0 "
	        );
	        
	        

	        List<Object> params = new ArrayList<>();

	        params.add(shiftid);
	        params.add(hub_id);

	        if (date != null && !date.trim().isEmpty()) {
	            reportSql.append(" AND pa.audit_date=? ");
	            params.add(formattedDate);
	        }
	        
	        reportSql.append(" ORDER BY pa.qcm_id, pq.orderby ");
	        
	        List<Map<String, Object>> flatData = jdbcPmcTemplate.queryForList(
	                reportSql.toString(),
	                params.toArray()
	        );

	        //   4. Convert to nested structure
	        Map<Integer, Map<String, Object>> categoryMap = new LinkedHashMap<>();

	        for (Map<String, Object> row : flatData) {

	            Integer qcmId = (Integer) row.get("qcm_id");

	            // 🔹 create category if not exists
	            if (!categoryMap.containsKey(qcmId)) {

	            	Map<String, Object> category = new LinkedHashMap<>();
	            	category.put("category", row.get("audit_category"));
	            	category.put("qcm_id", qcmId);                       
	            	category.put("questions", new ArrayList<>());  

	                categoryMap.put(qcmId, category);
	            }

	            // 🔹 build question object
	            Map<String, Object> question = new LinkedHashMap<>();
	            question.put("question", row.get("question"));
	            question.put("answer", row.get("answer"));
	            question.put("question_type", "radio");
	            question.put("img_full_path", row.get("img_full_path"));
	            question.put("main_id", row.get("main_id"));
	            question.put("qid", row.get("qid"));
	            question.put("aid", row.get("aid"));
	            question.put("remarks", row.get("remarks"));
	            
	            question.put("question_type", "radio");
	            
	            List<Map<String, Object>> options = new ArrayList<>();

	            Map<String, Object> opt1 = new HashMap<>();
	            opt1.put("value", "accept");
	            opt1.put("orderby", 1);
	            opt1.put("english_name", "Accept");
	            opt1.put("remarksfield", false);
	            opt1.put("opt_mandatory", 0);

	            Map<String, Object> opt2 = new HashMap<>();
	            opt2.put("value", "challenge");
	            opt2.put("orderby", 2);
	            opt2.put("english_name", "Challenge");
	            opt2.put("remarksfield", true);
	            opt2.put("opt_mandatory", 1);

	            options.add(opt1);
	            options.add(opt2);

	            //   attach options
	            question.put("options", options);
	            

	            // 🔹 add to category
	            List<Map<String, Object>> questions =
	                    (List<Map<String, Object>>) categoryMap.get(qcmId).get("questions");

	            questions.add(question);
	        }
	        
	        

	        //   5. Final response
	        List<Map<String, Object>> finalData = new ArrayList<>(categoryMap.values());

	        response.put("data", finalData);
	        response.put("message", "Food Swing Details for "+ date);
	        response.put("status", "Success");

	    } catch (Exception e) {
	        e.printStackTrace();
	        response.put("message", "Error in getting food swing details");
	        response.put("status", "Failed");
	    }

	    return Collections.singletonList(response);
	}
	
	public List<Map<String, Object>> getfoodswingDataNoCategory(int shiftid, int hub_id, String date,String latitude,String longitude) {

	    String formattedDate = convertDateFormat(date, 0);
	    Map<String, Object> response = new HashMap<>();

	    try {

	    	String hubLatLongSql = "SELECT latitude, longitude, radius_range FROM hub_master WHERE id=? AND is_active=1 AND is_delete=0";

            Map<String, Object> hubLocation = jdbcPmcTemplate.queryForMap(hubLatLongSql, hub_id);

            String hubLat = hubLocation.get("latitude") != null ? hubLocation.get("latitude").toString() : null;
            String hubLng = hubLocation.get("longitude") != null ? hubLocation.get("longitude").toString() : null;
            Double radiusRange = hubLocation.get("radius_range") != null
                    ? Double.parseDouble(hubLocation.get("radius_range").toString())
                    : 200.0;

            if (latitude != null && longitude != null && !latitude.isBlank() && !longitude.isBlank()
                    && hubLat != null && hubLng != null) {

                String distanceSql =
                        "SELECT (6371008.8 * acos(ROUND(" +
                                "cos(radians(?)) * cos(radians(?)) * " +
                                "cos(radians(?) - radians(?)) + " +
                                "sin(radians(?)) * sin(radians(?))" +
                                ", 9))) AS distance";

                Double distance = jdbcPmcTemplate.queryForObject(
                        distanceSql,
                        Double.class,
                        Double.parseDouble(latitude),
                        Double.parseDouble(hubLat),
                        Double.parseDouble(hubLng),
                        Double.parseDouble(longitude),
                        Double.parseDouble(latitude),
                        Double.parseDouble(hubLat)
                );

                if (distance == null || distance > radiusRange) {
                    response.put("status", "Failed");
                    response.put("message", "You are not within " + radiusRange + " meters of the kitchen location");
                    return Collections.singletonList(response);
                }
            }
	    	
	    	
	        //   2. Check audit exists
	        StringBuilder auditCheckSql = new StringBuilder(
	                "SELECT id FROM pmc_audit WHERE shiftid=? AND hub_id=? AND isactive=1 AND isdelete=0 "
	        );

	        List<Object> auditParams = new ArrayList<>();
	        auditParams.add(shiftid);
	        auditParams.add(hub_id);

	        if (date != null && !date.trim().isEmpty()) {
	            auditCheckSql.append(" AND audit_date=? ");
	            auditParams.add(formattedDate);
	        }

	        List<Integer> auditIds = jdbcPmcTemplate.query(
	                auditCheckSql.toString(),
	                (rs, rowNum) -> rs.getInt("id"),
	                auditParams.toArray()
	        );

	        if (auditIds.isEmpty()) {
	            response.put("message", "No Audit data for " + date +" - shift -"+shiftid);
	            response.put("status", "Failed");
	            return Collections.singletonList(response);
	        }

	        //   3. Fetch data (same query)
	        StringBuilder reportSql = new StringBuilder();

	        reportSql.append(
	                "SELECT pf.id as main_id, pa.qcm_id,pq.question_type, " +
	                "pf.questions AS qid, pq.q_english AS question, " +
	                "pf.answer AS aid, pam.english_name AS answer, " +
	                "IFNULL(pf.remarks, '') AS remarks, " +
	                "CASE WHEN pf.image IS NULL OR pf.image = '' THEN '' " +
	                "ELSE CONCAT('" + fileBaseUrl + "/gccofficialapp/files', pf.image) END AS img_full_path, " +
	                
					 "    CASE   "
					 + "        WHEN (pq.question_type = 'select' OR pq.question_type = 'radio') AND COUNT(cam.id) > 0 THEN JSON_ARRAYAGG( "
					 + "            JSON_OBJECT( "
					 + "                'option_id', cam.id, "
					 + "                'english_name', cam.english_name,"
					 + "				   'tamil_name',cam.tamil_name, "
					 + "				   'opt_mandatory',cam.opt_mandatory, "
					 + "                'value', LOWER(cam.english_name), "
					 + "				   'remarksfield', (cam.remarks_required = 1), "
					 + "				   'imgfield', (cam.img_required = 1), "
					 + "				   'textfield', (cam.text_required = 1), "
					 + "                'orderby', cam.orderby "
					 + "            ) "
					 + "        ) "
					 + "        ELSE JSON_ARRAY()  "
					 + "    END AS options "+
	                
	                
	                "FROM pmc_audit pa " +
	                "JOIN pmc_feedback pf ON pf.pmc_audit_id = pa.id AND pf.isactive=1 AND pf.isdelete=0 AND pf.food_swing_sts is NULL " +
	                "JOIN pmc_questions_master pq ON pq.qid = pf.questions AND pq.isactive=1 AND pq.isdelete=0 " +
	                "JOIN challenge_answer_master cam ON cam.qid = pf.questions AND cam.isactive=1 AND cam.isdelete=0 " +
	                "JOIN pmc_answer_master pam ON pam.aid = pf.answer AND pam.isactive=1 AND pam.isdelete=0 AND pam.opt_mandatory=1 " +
	                "WHERE pa.shiftid=? AND pa.hub_id=? AND pa.isactive=1 AND pa.isdelete=0 "
	        );

	        List<Object> params = new ArrayList<>();
	        params.add(shiftid);
	        params.add(hub_id);

	        if (date != null && !date.trim().isEmpty()) {
	            reportSql.append(" AND pa.audit_date=? ");
	            params.add(formattedDate);
	        }
	        reportSql.append(" GROUP BY pf.id ");
	        reportSql.append(" ORDER BY pq.orderby ");

	        
	        List<Map<String, Object>> result = jdbcPmcTemplate.queryForList(reportSql.toString(), params.toArray());
	           Iterator<Map<String, Object>> iterator = result.iterator();
	           ObjectMapper mapper = new ObjectMapper();
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
	           
	           }

	        //   5. Final response
	        response.put("data", result);
	        response.put("message", "Food Swing Details for " + date);
	        response.put("status", "Success");

	    } catch (Exception e) {
	        e.printStackTrace();
	        response.put("message", "Error in getting data");
	        response.put("status", "Failed");
	    }

	    return Collections.singletonList(response);
	}

	public List<?> savefoodswingdata(String questionAnswers, String loginId,String latitude,String longitude,MultipartFile[] images) {

	    Map<String, Object> response = new HashMap<>();

	    try {

	        if (questionAnswers == null || questionAnswers.trim().isEmpty()) {
	            response.put("status", "Failed");
	            response.put("message", "questionAnswers is Empty");
	            return Collections.singletonList(response);
	        }
	        
	        ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> questionAnswersList = mapper.readValue(
                    questionAnswers,
                    new TypeReference<List<Map<String, Object>>>() {}
            );


            Map<Integer, MultipartFile> imageMap = new HashMap<>();

            if (images != null) {
                for (MultipartFile file : images) {

                    String fileName = file.getOriginalFilename();

                    if (fileName != null && fileName.startsWith("q_")) {

                        try {
                            // Example: q_43_filename.jpg
                            String[] parts = fileName.split("_");

                            if (parts.length >= 2) {
                                Integer qidFromFile = Integer.parseInt(parts[1]);
                                imageMap.put(qidFromFile, file);
                            }

                        } catch (Exception e) {
                            System.out.println("Invalid filename format: " + fileName);
                        }
                    }
                }
            }
            
	        int userId = Integer.parseInt(loginId);

	        String foodInsertSql = "UPDATE pmc_feedback SET food_swing_sts=?, fs_remarks=?,fs_image=?, fs_cby=?, fs_cdate=NOW(),fs_lat=? ,fs_long=? WHERE id=? AND isactive=1 AND isdelete=0";

	        for (Map<String, Object> row : questionAnswersList) {

	            if (row.get("main_id") == null) continue;

	            int main_id = ((Number) row.get("main_id")).intValue();
	            String answer = row.get("answer") != null ? row.get("answer").toString() : null;
	            String remarks = row.get("remarks") != null ? row.get("remarks").toString() : null;
	            
	            String imagePath = null;
	            
	            MultipartFile file = imageMap.get(main_id);
	            
	            if (file != null && !file.isEmpty()) {

                    imagePath = fileUpload(
                            "pmc_feedback_img",
                            loginId,
                            file,
                            "pmcfeedback"
                    );
                }

	            jdbcPmcTemplate.update(foodInsertSql, answer, remarks,imagePath, userId,latitude,longitude, main_id);
	        }

	        response.put("status", "Success");
	        response.put("message", "Feedback updated Successfully");

	    } catch (Exception e) {
	        e.printStackTrace();
	        response.put("status", "Failed");
	        response.put("message", "Error while updating feedback");
	    }

	    return Collections.singletonList(response);
	}

	public List<Map<String, Object>> getfeedbackhubdetails(int loginid, String date) {

	    String formattedDate = convertDateFormat(date, 0);
	    Map<String, Object> response = new HashMap<>();

	    try {

	        // (Optional) loginid check - you can keep or remove
//	        String hubSql = "SELECT hub_id FROM driver_login WHERE loginid=? AND isactive=1 AND isdelete=0";
//
//	        List<Integer> hubList = jdbcPmcTemplate.query(
//	                hubSql,
//	                (rs, rowNum) -> rs.getInt("hub_id"),
//	                loginid
//	        );
//
//	        if (hubList.isEmpty()) {
//	            response.put("message", "No hub mapped for login id");
//	            response.put("status", "Failed");
//	            return Collections.singletonList(response);
//	        }

	        // ✅ MAIN QUERY (all hubs)
	        String sql =
	                "SELECT hm.id AS hub_id, hm.permanent_location AS hub_name,hm.zone,hm.ward,hm.latitude,hm.longitude "
	                + " FROM hub_master hm "
	                + " WHERE EXISTS ( "
	                + "    SELECT 1 "
	                + "    FROM pmc_feedback pf "
	                + "    JOIN pmc_answer_master pam ON pam.aid = pf.answer "
	                + "    JOIN pmc_audit pa ON pa.id = pf.pmc_audit_id "
	                + "    WHERE pf.hub_id = hm.id "
	                + "    AND pa.audit_date = ? "
	                + "    AND pam.opt_mandatory = 1 "
	                + "    AND pf.isactive = 1 "
	                + "    AND pf.isdelete = 0"
	                + " )"; 

	        List<Map<String, Object>> hubdetails =
	                jdbcPmcTemplate.queryForList(sql, formattedDate);

	        response.put("data", hubdetails);

	        if (hubdetails.isEmpty()) {
	            response.put("message", "No Issue Found for any hub");
	        } else {
	            response.put("message", "Issue Found Hubs List");
	        }

	        response.put("status", "Success");

	    } catch (Exception e) {
	        e.printStackTrace();
	        response.put("message", "Error in getting Food Details");
	        response.put("status", "Failed");
	    }

	    return Collections.singletonList(response);
	}

	public List<Map<String, Object>> getceloginhubs( String date) {
		
		
		String formattedDate = convertDateFormat(date, 0);
	    Map<String, Object> response = new HashMap<>();

	    try {
	      
	        String sql =
	                "SELECT hm.id AS hub_id, hm.permanent_location AS hub_name,hm.zone,hm.ward,hm.latitude,hm.longitude "
	                + " FROM hub_master hm "
	                + " WHERE EXISTS ( "
	                + "    SELECT 1 "
	                + "    FROM pmc_feedback pf "
	                + "    JOIN pmc_answer_master pam ON pam.aid = pf.answer "
	                + "    JOIN pmc_audit pa ON pa.id = pf.pmc_audit_id "
	                + "    WHERE pf.hub_id = hm.id "
	                + "    AND pa.audit_date = ? "
	                + "    AND pam.opt_mandatory = 1 "
	                + "    AND pf.isactive = 1 "
	                + "    AND pf.isdelete = 0 AND pf.food_swing_sts = 'challenge' "
	                + " )"; 

	        List<Map<String, Object>> hubdetails =
	                jdbcPmcTemplate.queryForList(sql, formattedDate);

	        response.put("data", hubdetails);

	        if (hubdetails.isEmpty()) {
	            response.put("message", "No Issue Found for any hub");
	        } else {
	            response.put("message", "Issue Found Hubs List for SE/CE");
	        }

	        response.put("status", "Success");

	    } catch (Exception e) {
	        e.printStackTrace();
	        response.put("message", "Error in getting Food Details");
	        response.put("status", "Failed");
	    }

	    return Collections.singletonList(response);
		
	}

	public List<Map<String, Object>> getCEDataNoCategory(int shiftid, int hub_id, String date) {
		
		 String formattedDate = convertDateFormat(date, 0);
		    Map<String, Object> response = new HashMap<>();
		    
		    try {
	    	
		        //   2. Check audit exists
		        StringBuilder auditCheckSql = new StringBuilder(
		                "SELECT id FROM pmc_audit WHERE shiftid=? AND hub_id=? AND isactive=1 AND isdelete=0 "
		        );

		        List<Object> auditParams = new ArrayList<>();
		        auditParams.add(shiftid);
		        auditParams.add(hub_id);

		        if (date != null && !date.trim().isEmpty()) {
		            auditCheckSql.append(" AND audit_date=? ");
		            auditParams.add(formattedDate);
		        }

		        List<Integer> auditIds = jdbcPmcTemplate.query(
		                auditCheckSql.toString(),
		                (rs, rowNum) -> rs.getInt("id"),
		                auditParams.toArray()
		        );

		        if (auditIds.isEmpty()) {
		            response.put("message", "No Audit data for " + date +" - shift -"+shiftid);
		            response.put("status", "Failed");
		            return Collections.singletonList(response);
		        }

		        //   3. Fetch data (same query)
		        StringBuilder reportSql = new StringBuilder();

		        reportSql.append(
		                "SELECT pf.id as main_id, pa.qcm_id,pq.question_type, " +
		                "pf.questions AS qid, pq.q_english AS question, " +
		                "pf.answer AS aid, pam.english_name AS pmc_answer, " +
		                "IFNULL(pf.remarks, '') AS pmc_remarks, " +
		                "CASE WHEN pf.image IS NULL OR pf.image = '' THEN '' " +
		                "ELSE CONCAT('" + fileBaseUrl + "/gccofficialapp/files', pf.image) END AS pmc_img_full_path, " +
		                "UPPER(pf.food_swing_sts) AS fs_answer, " +
		                "IFNULL(pf.fs_remarks, '') AS fs_remarks, " +
		                "CASE WHEN pf.fs_image IS NULL OR pf.fs_image = '' THEN '' " +
		                "ELSE CONCAT('" + fileBaseUrl + "/gccofficialapp/files', pf.fs_image) END AS fs_img_full_path, " +
						 "    CASE   "
						 + "        WHEN (pq.question_type = 'select' OR pq.question_type = 'radio') AND COUNT(ceam.id) > 0 THEN JSON_ARRAYAGG( "
						 + "            JSON_OBJECT( "
						 + "                'option_id', ceam.id, "
						 + "                'english_name', ceam.english_name,"
						 + "				   'tamil_name',ceam.tamil_name, "
						 + "				   'opt_mandatory',ceam.opt_mandatory, "
						 + "                'value', LOWER(ceam.english_name), "
						 + "				   'remarksfield', (ceam.remarks_required = 1), "
						 + "				   'imgfield', (ceam.img_required = 1), "
						 + "				   'textfield', (ceam.text_required = 1), "
						 + "                'orderby', ceam.orderby "
						 + "            ) "
						 + "        ) "
						 + "        ELSE JSON_ARRAY()  "
						 + "    END AS options "+
		                
		                
		                "FROM pmc_audit pa " +
		                "JOIN pmc_feedback pf ON pf.pmc_audit_id = pa.id AND pf.isactive=1 AND pf.isdelete=0 AND pf.ce_sts is NULL AND food_swing_sts='challenge' " +
		                "JOIN pmc_questions_master pq ON pq.qid = pf.questions AND pq.isactive=1 AND pq.isdelete=0 " +
		                
		                "LEFT JOIN ce_answer_master ceam ON ceam.qid = pf.questions AND ceam.isactive=1 AND ceam.isdelete=0 " +
		                "JOIN pmc_answer_master pam ON pam.aid = pf.answer AND pam.isactive=1 AND pam.isdelete=0 AND pam.opt_mandatory=1 " +
		                "WHERE pa.shiftid=? AND pa.hub_id=? AND pa.isactive=1 AND pa.isdelete=0 "
		        );

		        List<Object> params = new ArrayList<>();
		        params.add(shiftid);
		        params.add(hub_id);

		        if (date != null && !date.trim().isEmpty()) {
		            reportSql.append(" AND pa.audit_date=? ");
		            params.add(formattedDate);
		        }
		        reportSql.append(" GROUP BY pf.id ");
		        reportSql.append(" ORDER BY pq.orderby ");

		        
		        List<Map<String, Object>> result = jdbcPmcTemplate.queryForList(reportSql.toString(), params.toArray());
		           Iterator<Map<String, Object>> iterator = result.iterator();
		           ObjectMapper mapper = new ObjectMapper();
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
		           
		           }

		        //   5. Final response
		        response.put("data", result);
		        response.put("message", "SE/CE Food Challenge Details for " + date);
		        response.put("status", "Success");

		    } catch (Exception e) {
		        e.printStackTrace();
		        response.put("message", "Error in getting data");
		        response.put("status", "Failed");
		    }

		    return Collections.singletonList(response);
		
	}

	public List<?> savecelogindata(String questionAnswers, String loginId) {
		Map<String, Object> response = new HashMap<>();

	    try {

	        if (questionAnswers == null || questionAnswers.trim().isEmpty()) {
	            response.put("status", "Failed");
	            response.put("message", "questionAnswers is Empty");
	            return Collections.singletonList(response);
	        }
	        
	        ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> questionAnswersList = mapper.readValue(
                    questionAnswers,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

	        int userId = Integer.parseInt(loginId);

	        String foodInsertSql = "UPDATE pmc_feedback SET ce_sts=?,ce_remarks=?, ce_cby=?, ce_cdate=NOW() WHERE id=? AND isactive=1 AND isdelete=0";

	        for (Map<String, Object> row : questionAnswersList) {

	            if (row.get("main_id") == null) continue;

	            int main_id = ((Number) row.get("main_id")).intValue();
	            String answer = row.get("answer") != null ? row.get("answer").toString() : null;
	            String remarks = row.get("remarks") != null ? row.get("remarks").toString() : null;
	            	         

	            jdbcPmcTemplate.update(foodInsertSql, answer, remarks, userId, main_id);
	        }

	        response.put("status", "Success");
	        response.put("message", "Feedback updated Successfully");

	    } catch (Exception e) {
	        e.printStackTrace();
	        response.put("status", "Failed");
	        response.put("message", "Error while updating feedback in CE login");
	    }

	    return Collections.singletonList(response);
	}

	public List<Map<String, Object>> gethubsforreport(String date, Integer loginId) {

	    Map<String, Object> response = new HashMap<>();

	    try {

	        String formattedDate = convertDateFormat(date, 0);
	        Integer hub_id = 0;

	        // ✅ optional hub filter
	        if (loginId != null && loginId != 0) {

	            String hubSql = "SELECT hub_id FROM driver_login WHERE loginid=? AND isactive=1 AND isdelete=0";

	            List<Integer> hubList = jdbcPmcTemplate.query(
	                    hubSql,
	                    (rs, rowNum) -> rs.getInt("hub_id"),
	                    loginId
	            );

	            if (!hubList.isEmpty() && hubList.get(0) != null) {
	                hub_id = hubList.get(0);
	            }
	        }

	        StringBuilder sql = new StringBuilder();

	        sql.append(
	            "SELECT hm.id AS hub_id, hm.permanent_location AS hub_name, " +
	            "sm.shiftid, sm.Code AS shift_name, " +
	            "CASE WHEN COUNT(pa.id) > 0 THEN 1 ELSE 0 END AS count " +

	            "FROM hub_master hm " +
	            "CROSS JOIN shift_master sm " +

	            "LEFT JOIN pmc_audit pa ON pa.hub_id = hm.id " +
	            "AND pa.shiftid = sm.shiftid " +
	            "AND pa.audit_date = ? " +
	            "AND pa.isactive = 1 " +
	            "AND pa.isdelete = 0 " +

	            "WHERE hm.is_active = 1 AND hm.is_delete = 0 " +
	            "AND sm.isactive = 1 AND sm.isdelete = 0 "
	        );

	        List<Object> params = new ArrayList<>();
	        params.add(formattedDate);

	        if (hub_id != 0) {
	            sql.append(" AND hm.id = ? ");
	            params.add(hub_id);
	        }

	        sql.append(" GROUP BY hm.id, hm.permanent_location, sm.shiftid, sm.Code ");
	        sql.append(" ORDER BY hm.id, sm.orderby ");

	        List<Map<String, Object>> raw =
	                jdbcPmcTemplate.queryForList(sql.toString(), params.toArray());

	        // ✅ Convert to grouped JSON (hub → shifts[])
	        Map<Integer, Map<String, Object>> hubMap = new LinkedHashMap<>();

	        for (Map<String, Object> row : raw) {

	            Integer hId = ((Number) row.get("hub_id")).intValue();

	            hubMap.putIfAbsent(hId, new LinkedHashMap<>());

	            Map<String, Object> hub = hubMap.get(hId);

	            hub.put("hub_id", hId);
	            hub.put("hub_name", row.get("hub_name"));

	            List<Map<String, Object>> shifts =
	                    (List<Map<String, Object>>) hub.getOrDefault("shifts", new ArrayList<>());

	            Map<String, Object> shift = new LinkedHashMap<>();
	            shift.put("shiftid", row.get("shiftid"));
	            shift.put("shift_name", row.get("shift_name"));
	            shift.put("count", row.get("count"));

	            shifts.add(shift);
	            hub.put("shifts", shifts);
	        }

	        response.put("data", new ArrayList<>(hubMap.values()));
	        response.put("status", "Success");
	        response.put("message", "Fetched Hub Shift Report Successfully");

	    } catch (Exception e) {
	        e.printStackTrace();
	        response.put("status", "Failed");
	        response.put("message", "Error in reports for getting hubs");
	    }

	    return Collections.singletonList(response);
	}


		public List<Map<String, Object>> getcatsforhubreport(int shiftid, int hub_id, String date) {

		    Map<String, Object> response = new HashMap<>();

		    try {

		        String formattedDate = convertDateFormat(date, 0);

		        String sql =
		            "SELECT qcm.* " +

		            "FROM pmc_audit pa " +

		            "JOIN questions_category_master qcm ON qcm.qcm_id = pa.qcm_id " +
		            "AND qcm.isactive = 1 AND qcm.isdelete = 0 " +

		            "WHERE pa.shiftid = ? " +
		            "AND pa.hub_id = ? " +
		            "AND pa.audit_date = ? " +
		            "AND pa.isactive = 1 " +
		            "AND pa.isdelete = 0 " +

		            "GROUP BY qcm.qcm_id, qcm.audit_category, qcm.img_url, qcm.orderby " +
		            "ORDER BY qcm.orderby";

		        List<Map<String, Object>> result =
		                jdbcPmcTemplate.queryForList(sql, shiftid, hub_id, formattedDate);

		        response.put("data", result);
		        response.put("status", "Success");
		        response.put("message", "Fetched Filled Categories");

		    } catch (Exception e) {
		        e.printStackTrace();
		        response.put("status", "Failed");
		        response.put("message", "Error in fetching categories");
		    }

		    return Collections.singletonList(response);
		}

		public List<Map<String, Object>> getreportdata(int shiftid, int hub_id, String date, int qcm_id) {

		    Map<String, Object> response = new HashMap<>();

		    try {

		        String formattedDate = convertDateFormat(date, 0);

		        String sql = "SELECT pf.id, pf.questions AS qid, pq.q_english AS question, " +
		                "pf.answer AS aid, pam.english_name AS answer, pam.opt_mandatory, " +

		                "pf.remarks, pf.image, " +

		                "pf.food_swing_sts, pf.fs_remarks, pf.fs_image, " +
		                "pf.ce_sts, pf.ce_remarks, " +

		                "qcm.penalty_amt " +

		                "FROM pmc_feedback pf " +
		                "JOIN pmc_audit pa ON pa.id = pf.pmc_audit_id " +
		                "JOIN pmc_questions_master pq ON pq.qid = pf.questions " +
		                "JOIN pmc_answer_master pam ON pam.aid = pf.answer " +
		                "JOIN questions_category_master qcm ON qcm.qcm_id = pf.qcmid " +

		                "WHERE pa.shiftid = ? " +
		                "AND pa.hub_id = ? " +
		                "AND pa.audit_date = ? " +
		                "AND pa.qcm_id = ? " +
		                "AND pf.isactive = 1 " +
		                "AND pf.isdelete = 0";

		        List<Map<String, Object>> dbData =
		                jdbcPmcTemplate.queryForList(sql, shiftid, hub_id, formattedDate, qcm_id);

		        List<Map<String, Object>> finalList = new ArrayList<>();

		        for (Map<String, Object> row : dbData) {

		            Map<String, Object> obj = new LinkedHashMap<>();

		            obj.put("question", row.get("question"));
		            obj.put("answer", row.get("answer"));
		            obj.put("qid", row.get("qid"));
		            obj.put("aid", row.get("aid"));

		            Object optObj = row.get("opt_mandatory");

		            int optMandatory = 0;

		            if (optObj instanceof Number) {
		                optMandatory = ((Number) optObj).intValue();
		            } else if (optObj instanceof Boolean) {
		                optMandatory = (Boolean) optObj ? 1 : 0;
		            }

		            //  Only for Issue Found
		            if (optMandatory == 1) {

		                //  PMC DATA
		                Map<String, Object> pmcData = new HashMap<>();
		                pmcData.put("remarks", row.get("remarks"));
		                pmcData.put("img_full_path",
		                        row.get("image") != null ? fileBaseUrl + "/gccofficialapp/files" + row.get("image") : "");

		                obj.put("pmc_data", pmcData);

		                //  FS DATA
//		                String fsStatus = row.get("food_swing_sts") != null ? row.get("food_swing_sts").toString() : null;
//
//		                if ("challenge".equalsIgnoreCase(fsStatus)) {
//
//		                    Map<String, Object> fsData = new HashMap<>();
//		                    fsData.put("food_swing_sts", fsStatus);
//		                    fsData.put("fs_remarks", row.get("fs_remarks"));
//		                    fsData.put("fs_image",
//		                            row.get("fs_image") != null ? fileBaseUrl + "/gccofficialapp/files" + row.get("fs_image") : "");
//
//		                    obj.put("fs_data", fsData);
//		                }
		                
		                Map<String, Object> fsData = new HashMap<>();

		                String fsStatus = row.get("food_swing_sts") != null 
		                        ? row.get("food_swing_sts").toString() 
		                        : null;

		                if (fsStatus != null) {

		                    fsData.put("food_swing_sts", fsStatus);

		                    if ("challenge".equalsIgnoreCase(fsStatus)) {
		                        fsData.put("fs_remarks", row.get("fs_remarks"));
		                        fsData.put("fs_image",
		                                row.get("fs_image") != null 
		                                ? fileBaseUrl + "/gccofficialapp/files" + row.get("fs_image") 
		                                : "");
		                    }

		                    obj.put("fs_data", fsData);
		                }

		                // CE DATA
		                String ceStatus = row.get("ce_sts") != null ? row.get("ce_sts").toString() : null;

		                if (ceStatus != null) {
		                    Map<String, Object> ceData = new HashMap<>();
		                    ceData.put("ce_sts", ceStatus);
		                    ceData.put("ce_remarks", row.get("ce_remarks"));
		                    obj.put("ce_data", ceData);
		                }

		                // PENALTY CALCULATION
		                Double penalty = (Double) row.get("penalty_amt");

		                if ("accept".equalsIgnoreCase(fsStatus) && ceStatus == null) {
		                    obj.put("penalty_amt", penalty);
		                } else if ("challenge".equalsIgnoreCase(fsStatus) && ceStatus == null) {
		                    obj.put("penalty_amt", "Pending");
		                } else if ("challenge".equalsIgnoreCase(fsStatus) && "accept".equalsIgnoreCase(ceStatus)) {
		                    obj.put("penalty_amt", 0);
		                } else if ("challenge".equalsIgnoreCase(fsStatus) && "reject".equalsIgnoreCase(ceStatus)) {
		                    obj.put("penalty_amt", penalty);
		                }
		            }

		            finalList.add(obj);
		        }

		        response.put("data", finalList);
		        response.put("status", "Success");
		        response.put("message", "Food Feedback Details");

		    } catch (Exception e) {
		        e.printStackTrace();
		        response.put("status", "Failed");
		        response.put("message", "Error in report data");
		    }

		    return Collections.singletonList(response);
		}
    

}
