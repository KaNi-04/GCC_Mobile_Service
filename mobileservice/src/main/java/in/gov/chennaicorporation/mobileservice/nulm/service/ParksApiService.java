package in.gov.chennaicorporation.mobileservice.nulm.service;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.sql.Types;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class ParksApiService {
    private JdbcTemplate jdbcNULMTemplate;

    @Autowired
    private Environment environment;

    private String fileBaseUrl;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final int STRING_LENGTH = 15;

    private static final Random RANDOM = new SecureRandom();

    @Autowired
    public void setDataSource(@Qualifier("mysqlNulmDataSource") DataSource nulmDataSource) {
        this.jdbcNULMTemplate = new JdbcTemplate(nulmDataSource);
    }

    public ParksApiService(Environment environment) {
        this.environment = environment;
        this.fileBaseUrl = environment.getProperty("fileBaseUrl");
    }

    // public List<Map<String, Object>> getParkDetails(String division) {
    // String sql = "select * from park_details where division = ?";
    // List<Map<String, Object>> result = jdbcNULMTemplate.queryForList(sql,
    // division);
    // Map<String, Object> response = new HashMap<>();
    // response.put("status", "Success");
    // response.put("message", "Park Details");
    // response.put("Data", result);

    // return Collections.singletonList(response);
    // }

    public List<Map<String, Object>> getParkDetails(String division, String latitude, String longitude) {

        List<Map<String, Object>> result;

        // CASE 1: Lat & Long (radius search)
        if (latitude != null && !latitude.isEmpty() &&
                longitude != null && !longitude.isEmpty()) {

            String sql = "SELECT p.*, " +
                    "(6371000 * ACOS( " +
                    "LEAST(1, GREATEST(-1, " +
                    "COS(RADIANS(?)) * COS(RADIANS(CAST(p.latitude AS DECIMAL(10,6)))) * " +
                    "COS(RADIANS(CAST(p.longitude AS DECIMAL(10,6))) - RADIANS(?)) + " +
                    "SIN(RADIANS(?)) * SIN(RADIANS(CAST(p.latitude AS DECIMAL(10,6)))) " +
                    "))" +
                    ")) AS distance_in_meters " +
                    "FROM park_details p " +
                    "WHERE p.is_active = 1 AND p.is_delete = 0 " +
                    "HAVING distance_in_meters <= 1500 " + // UPDATED HERE
                    "ORDER BY distance_in_meters ASC";

            result = jdbcNULMTemplate.queryForList(sql,
                    Double.parseDouble(latitude),
                    Double.parseDouble(longitude),
                    Double.parseDouble(latitude));

        }
        // CASE 2: Division filter
        else if (division != null && !division.isEmpty()) {

            String sql = "SELECT * FROM park_details " +
                    "WHERE division = ? AND is_active = 1 AND is_delete = 0";

            result = jdbcNULMTemplate.queryForList(sql, division);

        }
        // CASE 3: All parks
        else {

            String sql = "SELECT * FROM park_details " +
                    "WHERE is_active = 1 AND is_delete = 0";

            result = jdbcNULMTemplate.queryForList(sql);
        }

        // OLD STYLE RESPONSE
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Success");
        response.put("message", "Park Details");
        response.put("Data", result);

        return Collections.singletonList(response);
    }

    public static String generateRandomString() {
        StringBuilder result = new StringBuilder(STRING_LENGTH);
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

    public String fileUpload(MultipartFile file, String name) {

        // Set the file path where you want to save it
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("parksstaffverification_foldername");
        var year = DateTimeUtil.getCurrentYear();
        var month = DateTimeUtil.getCurrentMonth();

        uploadDirectory = uploadDirectory + serviceFolderName + year + "/" + month;

        try {
            // Create directory if it doesn't exist
            Path directoryPath = Paths.get(uploadDirectory);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            if (file == null || file.isEmpty()) {
                throw new RuntimeException("Please upload image"); // custom message
            }

            // Datetime string
            String datetimetxt = DateTimeUtil.getCurrentDateTime();
            // File name
            String fileName = name + "_" + datetimetxt + "_" + file.getOriginalFilename();
            fileName = fileName.replaceAll("\s+", ""); // Remove space on filename

            String filePath = uploadDirectory + "/" + fileName;

            String filepath_txt = "/" + serviceFolderName + year + "/" + month + "/" + fileName;

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

    @Transactional
    public Map<String, Object> saveStaffVerificationDetails(
            String userid,
            String enrollmentId,
            String parkid,
            String photoUrl,
            String latitude,
            String longitude,
            String address,
            String verifiedStatus) {

        Map<String, Object> response = new HashMap<>();

        try {

            String sql = "INSERT INTO officer_feedback_parks " +
                    "(userid, enrollment_id, park_id, photo_url, lat, `long`, address, verified, verified_status, cby) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            String[] enrollmentIds = (enrollmentId != null && !enrollmentId.isBlank())
                    ? enrollmentId.split(",")
                    : new String[] { null };

            int successCount = 0;

            for (String id : enrollmentIds) {

                Integer parsedId = null;

                try {
                    if (id != null && !id.trim().isEmpty()) {
                        parsedId = Integer.parseInt(id.trim());
                    }
                } catch (Exception ex) {
                    parsedId = null;
                }

                final Integer finalParsedId = parsedId;

                String verifiedValue = "NO";
                String verifiedStatusValue = null;

                if (verifiedStatus != null && !verifiedStatus.isBlank()) {
                    verifiedValue = "YES";
                    verifiedStatusValue = verifiedStatus;
                }

                final String finalVerified = verifiedValue;
                final String finalVerifiedStatus = verifiedStatusValue;

                int rows = jdbcNULMTemplate.update(con -> {
                    PreparedStatement ps = con.prepareStatement(sql);

                    ps.setString(1, userid);

                    if (finalParsedId != null) {
                        ps.setInt(2, finalParsedId);
                    } else {
                        ps.setNull(2, Types.INTEGER);
                    }

                    // ps.setString(3, zone);
                    // ps.setString(4, ward);
                    ps.setString(3, parkid);
                    ps.setString(4, photoUrl);
                    ps.setString(5, latitude);
                    ps.setString(6, longitude);
                    ps.setString(7, address);
                    ps.setString(8, finalVerified);

                    if (finalVerifiedStatus != null) {
                        ps.setString(9, finalVerifiedStatus);
                    } else {
                        ps.setNull(9, Types.VARCHAR); // FIXED
                    }

                    ps.setString(10, userid); // FIXED (cby)

                    return ps;
                });

                if (rows > 0) {
                    successCount++;
                }
            }

            response.put("status", "Success");
            response.put("message", "Saved successfully for " + successCount + " records");

            return response;

        } catch (Exception ex) {
            ex.printStackTrace();

            response.put("status", "Failed");
            response.put("message", "Error while saving: " + ex.getMessage());

            return response;
        }
    }

    public List<Map<String, Object>> getStaffListForAttendance(String parkid, String date) {

        Map<String, Object> response = new HashMap<>();
        String formattedDate;

        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            LocalDate parsedDate = LocalDate.parse(date, inputFormatter);
            formattedDate = parsedDate.format(outputFormatter);

        } catch (Exception e) {
            response.put("status", "Failed");
            response.put("message", "Invalid date format. Use dd-MM-yyyy");
            response.put("Data", Collections.emptyList());
            return Collections.singletonList(response);
        }

        String sqlQuery = "SELECT e.*, " +
                "       IFNULL(DATE_FORMAT(a.indatetime, '%d-%m-%Y %l:%i %p'), '') AS indatetime, " +
                "       IFNULL(DATE_FORMAT(a.outdatetime, '%d-%m-%Y %l:%i %p'), '') AS outdatetime, " +
                "       a.inby, " +
                "       a.outby, " +
                "       a.inphoto, " +
                "       a.outphoto, " +

                // VERIFIED LOGIC
                "       CASE WHEN ofp.fid IS NOT NULL THEN 'YES' ELSE 'NO' END AS verified, " +
                "       IFNULL(ofp.verified_status, '') AS verified_status, " +
                "       CONCAT('" + fileBaseUrl + "/gccofficialapp/files', ofp.photo_url) AS verified_image_url " +

                "FROM enrollment_table e " +

                // Attendance JOIN
                "LEFT JOIN attendance a ON e.enrollment_id = a.enrollment_id " +
                "   AND DATE(a.indatetime) = ? " +
                "   AND a.indatetime = ( " +
                "       SELECT MAX(a2.indatetime) " +
                "       FROM attendance a2 " +
                "       WHERE a2.enrollment_id = e.enrollment_id " +
                "       AND DATE(a2.indatetime) = ? " +
                "   ) " +

                // Latest verification record
                "LEFT JOIN officer_feedback_parks ofp ON ofp.fid = ( " +
                "       SELECT MAX(ofp2.fid) " +
                "       FROM officer_feedback_parks ofp2 " +
                "       WHERE ofp2.enrollment_id = e.enrollment_id " +
                "       AND ofp2.is_active = 1 " +
                "       AND ofp2.is_delete = 0 " +
                "   ) " +

                "WHERE e.isactive = 1 " +
                "  AND e.appointed = 1 " +
                "  AND e.facial_attendance = 1 " +
                "  AND e.emp_type = 'Park' " +
                "  AND FIND_IN_SET(?, e.park_id) > 0";

        System.out.println("Executing Query: " + sqlQuery);
        System.out.println("Params → date: " + formattedDate + ", parkid: " + parkid);

        List<Map<String, Object>> result = jdbcNULMTemplate.queryForList(sqlQuery,
                formattedDate,
                formattedDate,
                parkid);

        // ADD THIS LINE
        int totalCount = result.size();

        response.put("status", "Success");
        response.put("message", "Request List");
        response.put("total_count", totalCount);
        response.put("Data", result);

        return Collections.singletonList(response);
    }

    private Object replaceNullWithEmpty(Object value) {
        return value == null ? "" : value;
    }

    public List<Map<String, Object>> getParksInspectionQuestionsList() {

        String sql = "SELECT "
                + "    ql.qid, "
                + "    ql.q_english, "
                + "    ql.q_tamil, "
                + "    ql.question_type, "
                + "    ql.field_name, "
                + "    ql.is_mandatory, "
                + "    ql.orderby, "

                + "    CASE "
                + "        WHEN ql.question_type IN ('select','radio') THEN "
                + "            JSON_ARRAYAGG( "
                + "                CASE "
                + "                    WHEN qov.aid IS NOT NULL THEN "
                + "                        JSON_OBJECT( "
                + "                            'option_id', qov.aid, "
                + "                            'english_name', qov.english_name, "
                + "                            'value', qov.aid, "
                + "                            'orderby', qov.orderby "
                + "                        ) "
                + "                END "
                + "            ) "
                + "        ELSE NULL "
                + "    END AS options "

                + "FROM parks_questions_master ql "

                + "LEFT JOIN parks_answer_master qov "
                + "    ON qov.qid = ql.qid "
                + "    AND qov.isactive = 1 "
                + "    AND qov.isdelete = 0 "

                + "WHERE ql.isactive = 1 "

                + "GROUP BY "
                + "    ql.qid, "
                + "    ql.q_english, "
                + "    ql.q_tamil, "
                + "    ql.question_type, "
                + "    ql.field_name, "
                + "    ql.orderby "

                + "ORDER BY ql.orderby ASC";

        List<Map<String, Object>> result = jdbcNULMTemplate.queryForList(sql);

        ObjectMapper mapper = new ObjectMapper();

        for (Map<String, Object> row : result) {

            // Replace nulls in main row
            row.replaceAll((key, value) -> replaceNullWithEmpty(value));

            Object optionsRaw = row.get("options");

            if (optionsRaw != null && optionsRaw instanceof String) {
                try {
                    List<Map<String, Object>> optionsParsed = mapper.readValue((String) optionsRaw, List.class);

                    // remove null entries
                    optionsParsed.removeIf(Objects::isNull);

                    // replace null inside each option
                    for (Map<String, Object> opt : optionsParsed) {
                        opt.replaceAll((k, v) -> replaceNullWithEmpty(v));
                    }

                    // sort
                    optionsParsed.sort(Comparator.comparing(opt -> {
                        Object order = opt.get("orderby");
                        return (order instanceof Number) ? ((Number) order).intValue() : 0;
                    }));

                    row.put("options", optionsParsed);

                } catch (Exception e) {
                    row.put("options", new ArrayList<>());
                }
            } else {
                row.put("options", new ArrayList<>());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "Success");
        response.put("message", "Parks Inspection Question List");
        response.put("data", result);

        return Collections.singletonList(response);
    }

    @Transactional
    public Map<String, Object> saveParksInspection(
            Integer userId,
            Integer parkId,
            String responsesJson,
            String verificationJson,
            String latitude,
            String longitude,
            String location,
            String ai_verified_count,
            String ai_not_verified_count,
            String photoUrl) {

        Map<String, Object> response = new HashMap<>();

        try {

            if (responsesJson == null || responsesJson.isEmpty()) {
                throw new RuntimeException("Responses required");
            }

            ObjectMapper mapper = new ObjectMapper();

            // Parse responses
            List<Map<String, Object>> responses = mapper.readValue(responsesJson, List.class);

            // SAFE parsing for verificationData
            List<Map<String, Object>> verificationList = new ArrayList<>();

            if (verificationJson != null && !verificationJson.trim().isEmpty()
                    && !verificationJson.equalsIgnoreCase("null")) {

                try {
                    verificationList = mapper.readValue(verificationJson, List.class);
                } catch (Exception ex) {
                    // If invalid JSON → treat as empty
                    verificationList = new ArrayList<>();
                }
            }

            // Ensure at least ONE insert
            if (verificationList == null || verificationList.isEmpty()) {
                verificationList = new ArrayList<>();
                verificationList.add(new HashMap<>()); // empty object
            }

            String insertDetailsSql = "INSERT INTO parks_inspection_details "
                    + "(userid, park_id, uploaded_image_url, latitude, longitude, location, "
                    + "ai_verified_count, ai_not_verified_count, verified_image_url, enrollment_id, "
                    + "cdate, isactive, isdelete) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), 1, 0)";

            String insertFeedbackSql = "INSERT INTO parks_inspection_feedback "
                    + "(pid, question_id, answer_id, cdate, isactive, isdelete) "
                    + "VALUES (?, ?, ?, NOW(), 1, 0)";

            List<Integer> insertedIds = new ArrayList<>();

            // LOOP (at least once always)
            for (Map<String, Object> verification : verificationList) {

                final String verifiedImageUrl = verification.get("verified_image_url") != null
                        ? verification.get("verified_image_url").toString()
                        : null;

                final String enrollmentId = verification.get("enrollment_id") != null
                        ? verification.get("enrollment_id").toString()
                        : null;

                KeyHolder keyHolder = new GeneratedKeyHolder();

                // Insert parent
                jdbcNULMTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(insertDetailsSql,
                            Statement.RETURN_GENERATED_KEYS);

                    ps.setObject(1, userId);
                    ps.setObject(2, parkId);
                    ps.setString(3, photoUrl);
                    ps.setString(4, latitude);
                    ps.setString(5, longitude);
                    ps.setString(6, location);
                    ps.setString(7, ai_verified_count);
                    ps.setString(8, ai_not_verified_count);
                    ps.setString(9, verifiedImageUrl); // can be null
                    ps.setString(10, enrollmentId); // can be null

                    return ps;
                }, keyHolder);

                int pid = keyHolder.getKey().intValue();
                insertedIds.add(pid);

                // Insert feedback
                for (Map<String, Object> item : responses) {

                    Integer questionId = item.get("question_id") != null
                            ? Integer.parseInt(item.get("question_id").toString())
                            : null;

                    Integer answerId = item.get("answer_id") != null
                            ? Integer.parseInt(item.get("answer_id").toString())
                            : null;

                    if (questionId == null)
                        continue;

                    jdbcNULMTemplate.update(
                            insertFeedbackSql,
                            pid,
                            questionId,
                            answerId);
                }
            }

            response.put("status", "Success");
            response.put("message", "Saved successfully");
            response.put("inspection_ids", insertedIds);

        } catch (Exception e) {
            response.put("status", "Error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getZoneWardReport(String zone, String ward,
            String fromDate, String toDate) {

        Map<String, Object> response = new HashMap<>();

        try {

            fromDate = (fromDate == null || fromDate.trim().isEmpty()) ? null : fromDate;
            toDate = (toDate == null || toDate.trim().isEmpty()) ? null : toDate;

            String sql = "SELECT " +
                    " p.zone, " +

                    " COUNT(DISTINCT p.park_id) AS park_count, " +

                    " COUNT(DISTINCT ofp.park_id) AS inspected_park_count, " +

                    " COALESCE(SUM(CASE " +
                    "     WHEN LOWER(ofp.verified_status) NOT LIKE '%wrong%' " +
                    "          AND ofp.verified_status IS NOT NULL " +
                    "     THEN 1 ELSE 0 END), 0) AS matched_employee_count, " +

                    " COALESCE(SUM(CASE " +
                    "     WHEN LOWER(ofp.verified_status) LIKE '%wrong%' " +
                    "     THEN 1 ELSE 0 END), 0) AS not_matched_employee_count " +

                    " FROM park_details p " +

                    " LEFT JOIN officer_feedback_parks ofp " +
                    "   ON p.park_id = ofp.park_id " +
                    "   AND ofp.is_active = 1 " +
                    "   AND ofp.is_delete = 0 " +
                    "   AND ( ? IS NULL OR ofp.cdate >= CONCAT(?, ' 00:00:00') ) " +
                    "   AND ( ? IS NULL OR ofp.cdate <= CONCAT(?, ' 23:59:59') ) " +

                    " GROUP BY p.zone " +
                    " ORDER BY p.zone";

            // ONLY 4 params
            Object[] params = new Object[] {
                    fromDate, fromDate,
                    toDate, toDate
            };

            List<Map<String, Object>> data = jdbcNULMTemplate.queryForList(sql, params);

            response.put("status", "Success");
            response.put("message", data.isEmpty() ? "No records found" : "Zone-wise report fetched successfully");
            response.put("data", data);

        } catch (Exception e) {

            response.put("status", "Failed");
            response.put("message", "Error fetching report");
            response.put("error", e.getMessage());
        }

        return response;
    }

}
