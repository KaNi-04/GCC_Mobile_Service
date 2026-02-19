package in.gov.chennaicorporation.mobileservice.foodDistribution.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

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

        try {
            // Create directory if it doesn't exist
            Path directoryPath = Paths.get(uploadDirectory);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Datetime string
            String datetimetxt = DateTimeUtil.getCurrentDateTime();

            datetimetxt = datetimetxt + "_" + generateRandomFileString(6); // Attached Random text

            // File name
            String fileName = name + "_" + id + "_" + datetimetxt + "_" + file.getOriginalFilename();
            fileName = fileName.replaceAll("\\s+", ""); // Remove space on filename

            String filePath = uploadDirectory + "/" + fileName;

            String filepath_txt = "/" + serviceFolderName + "pmc/" + filetype + "/" + year + "/" + month + "/" + date
                    + "/" + fileName;

            // Create a new Path object
            Path path = Paths.get(filePath);

            // Get the bytes of the file
            byte[] bytes = file.getBytes();

            if (filetype.equalsIgnoreCase("image")) {
                // Compress the image
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50% quality
                // Write the bytes to the file
                Files.write(path, compressedBytes);
            } else {
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
            return "error";
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

            // 🔹 1️⃣ Insert into pmc_audit
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

            // 🔹 2️⃣ Insert into pmc_feedback (Loop q1–q18)
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

            // 🔹 3️⃣ Save Images
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

}
