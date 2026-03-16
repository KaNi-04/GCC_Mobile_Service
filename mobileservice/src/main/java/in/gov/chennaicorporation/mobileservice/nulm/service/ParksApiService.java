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
                "IFNULL(DATE_FORMAT(a.indatetime, '%d-%m-%Y %l:%i %p'), '') AS indatetime, " +
                "IFNULL(DATE_FORMAT(a.outdatetime, '%d-%m-%Y %l:%i %p'), '') AS outdatetime, " +
                "a.inby, a.outby, a.inphoto, a.outphoto, " +

                // VERIFIED LOGIC
                "CASE WHEN ofp.fid IS NOT NULL THEN 'YES' ELSE 'NO' END AS verified, " +

                "CASE WHEN ofp.fid IS NOT NULL THEN IFNULL(ofp.verified_status, '') ELSE '' END AS verified_status, " +

                "CASE WHEN ofp.fid IS NOT NULL THEN DATE_FORMAT(ofp.cdate, '%d-%m-%Y %l:%i %p') ELSE '' END AS verified_date, "
                +

                // FIXED IMAGE LOGIC
                "CASE WHEN ofp.photo_url IS NOT NULL " +
                "THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', ofp.photo_url) " +
                "ELSE '' END AS verified_image_url " +

                "FROM enrollment_table e " +

                "LEFT JOIN attendance a ON e.enrollment_id = a.enrollment_id " +
                "AND DATE(a.indatetime) = ? " +
                "AND a.indatetime = ( " +
                "   SELECT MAX(a2.indatetime) " +
                "   FROM attendance a2 " +
                "   WHERE a2.enrollment_id = e.enrollment_id " +
                "   AND DATE(a2.indatetime) = ? " +
                ") " +

                // DATE BASED VERIFICATION
                "LEFT JOIN officer_feedback_parks ofp ON ofp.fid = ( " +
                "   SELECT MAX(ofp2.fid) " +
                "   FROM officer_feedback_parks ofp2 " +
                "   WHERE ofp2.enrollment_id = e.enrollment_id " +
                "   AND ofp2.is_active = 1 " +
                "   AND ofp2.is_delete = 0 " +
                "   AND DATE(ofp2.cdate) = ? " +
                ") " +

                "WHERE e.isactive = 1 " +
                "AND e.appointed = 1 " +
                "AND e.facial_attendance = 1 " +
                "AND e.emp_type = 'Park' " +
                "AND e.park_id IS NOT NULL " +
                "AND FIND_IN_SET(?, e.park_id) > 0";

        List<Map<String, Object>> result;

        try {
            result = jdbcNULMTemplate.queryForList(sqlQuery,
                    formattedDate,
                    formattedDate,
                    formattedDate,
                    parkid);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "Failed");
            response.put("message", "Database error");
            response.put("Data", Collections.emptyList());
            return Collections.singletonList(response);
        }

        response.put("status", "Success");
        response.put("message", "Request List");
        response.put("total_count", result.size());
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

    // public Map<String, Object> getParksVerificationReport(String zone, String
    // ward,
    // String fromDate, String toDate, Integer parkId, String verifiedFlag, String
    // verificationDate) {

    // Map<String, Object> response = new HashMap<>();

    // try {

    // /*
    // * EMPLOYEE LEVEL DRILL
    // */

    // if (parkId != null && verifiedFlag != null && verificationDate != null) {

    // StringBuilder empSql = new StringBuilder();

    // empSql.append("SELECT ");
    // empSql.append("ofp.fid, ");
    // empSql.append("ofp.userid, ");
    // empSql.append("IFNULL(ofp.enrollment_id, '') AS enrollment_id, ");
    // empSql.append("ofp.verified_status, ");
    // empSql.append("ofp.lat, ");
    // empSql.append("ofp.long, ");
    // empSql.append("ofp.address, ");
    // empSql.append("IFNULL(DATE_FORMAT(ofp.cdate, '%d-%m-%Y %H:%i:%s'), '') AS
    // verification_date, ");

    // empSql.append("CASE WHEN ofp.photo_url IS NOT NULL ");
    // empSql.append("THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files',
    // ofp.photo_url) ");
    // empSql.append("ELSE '' END AS verified_image_url ");

    // empSql.append("FROM officer_feedback_parks ofp ");

    // empSql.append("WHERE ofp.is_active = 1 ");
    // empSql.append("AND ofp.is_delete = 0 ");
    // empSql.append("AND ofp.park_id = ? ");

    // empSql.append("AND LOWER(ofp.verified_status) = ? ");
    // empSql.append("AND DATE(ofp.cdate) = ? ");

    // // empSql.append("AND ofp.cdate >= ? ");
    // // empSql.append("AND ofp.cdate <= ? ");

    // // empSql.append("ORDER BY ofp.cdate DESC ");

    // List<Map<String, Object>> empList = jdbcNULMTemplate.queryForList(
    // empSql.toString(),
    // parkId,
    // verifiedFlag.toLowerCase(),
    // verificationDate);

    // response.put("status", "Success");
    // response.put("message",
    // empList.isEmpty() ? "No employee records found" : "Data details fetched
    // successfully");
    // response.put("data", empList);

    // return response;
    // }

    // /*
    // * 1. IF parkId PASSED → RETURN DRILL DOWN
    // */

    // if (parkId != null) {

    // StringBuilder drillSql = new StringBuilder();

    // drillSql.append("SELECT ");
    // drillSql.append("DATE_FORMAT(ofp.cdate, '%d-%m-%Y') AS inspection_date, ");

    // drillSql.append("COUNT(*) AS verified_count, ");

    // drillSql.append("COUNT(CASE WHEN LOWER(ofp.verified_status) = 'same' THEN 1
    // END) AS same_count, ");
    // drillSql.append("COUNT(CASE WHEN LOWER(ofp.verified_status) = 'wrong' THEN 1
    // END) AS wrong_count ");

    // // FIXED (use MAX)
    // // drillSql.append("CASE WHEN MAX(ofp.photo_url) IS NOT NULL ");
    // // drillSql.append("THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files',
    // // MAX(ofp.photo_url)) ");
    // // drillSql.append("ELSE '' END AS verified_image_url ");

    // drillSql.append("FROM officer_feedback_parks ofp ");

    // drillSql.append("WHERE ofp.is_active = 1 ");
    // drillSql.append("AND ofp.is_delete = 0 ");
    // drillSql.append("AND ofp.park_id = ? ");

    // drillSql.append("AND ofp.cdate >= ? ");
    // drillSql.append("AND ofp.cdate <= ? ");

    // drillSql.append("GROUP BY DATE_FORMAT(ofp.cdate, '%d-%m-%Y') ");
    // drillSql.append("ORDER BY inspection_date DESC ");

    // List<Map<String, Object>> drillList = jdbcNULMTemplate.queryForList(
    // drillSql.toString(),
    // parkId,
    // fromDate + " 00:00:00",
    // toDate + " 23:59:59");

    // response.put("status", "Success");
    // response.put("message", drillList.isEmpty() ? "No records found" : "Data
    // fetched successfully");
    // response.put("data", drillList);

    // return response;
    // }

    // /*
    // * 2. EXISTING SUMMARY LOGIC (UNCHANGED)
    // */

    // StringBuilder sql = new StringBuilder();

    // sql.append("SELECT p.zone ");

    // if (zone != null && !zone.trim().isEmpty()) {
    // sql.append(", p.division AS ward ");
    // }

    // sql.append(", COUNT(DISTINCT p.park_id) AS park_count ");
    // sql.append(", COUNT(DISTINCT ofp.park_id) AS inspected_park_count ");

    // sql.append(", COUNT(DISTINCT CASE ");
    // sql.append(" WHEN LOWER(ofp.verified_status) = 'same' ");
    // sql.append(" THEN p.park_id END) AS matched_employee_count ");

    // sql.append(", COUNT(DISTINCT CASE ");
    // sql.append(" WHEN LOWER(ofp.verified_status) = 'wrong' ");
    // sql.append(" THEN p.park_id END) AS not_matched_employee_count ");

    // sql.append(" FROM park_details p ");

    // sql.append(" LEFT JOIN ( ");
    // sql.append(" SELECT t1.* FROM officer_feedback_parks t1 ");
    // sql.append(" INNER JOIN ( ");
    // sql.append(" SELECT park_id, MAX(cdate) AS max_date ");
    // sql.append(" FROM officer_feedback_parks ");
    // sql.append(" WHERE is_active = 1 AND is_delete = 0 ");
    // sql.append(" GROUP BY park_id ");
    // sql.append(" ) t2 ON t1.park_id = t2.park_id AND t1.cdate = t2.max_date ");
    // sql.append(" ) ofp ON p.park_id = ofp.park_id ");

    // sql.append(" WHERE 1=1 ");

    // List<Object> params = new ArrayList<>();

    // if (fromDate != null && !fromDate.trim().isEmpty()) {
    // sql.append(" AND (ofp.cdate >= ? OR ofp.cdate IS NULL) ");
    // params.add(fromDate + " 00:00:00");
    // }

    // if (toDate != null && !toDate.trim().isEmpty()) {
    // sql.append(" AND (ofp.cdate <= ? OR ofp.cdate IS NULL) ");
    // params.add(toDate + " 23:59:59");
    // }

    // if (zone != null && !zone.trim().isEmpty()) {
    // sql.append(" AND p.zone = ? ");
    // params.add(zone);
    // }

    // if (ward != null && !ward.trim().isEmpty()) {
    // sql.append(" AND p.division = ? ");
    // params.add(ward);
    // }

    // if (zone != null && !zone.trim().isEmpty()) {
    // sql.append(" GROUP BY p.zone, p.division ORDER BY p.zone, p.division ");
    // } else {
    // sql.append(" GROUP BY p.zone ORDER BY p.zone ");
    // }

    // List<Map<String, Object>> summaryList =
    // jdbcNULMTemplate.queryForList(sql.toString(), params.toArray());

    // /*
    // * PARK WISE QUERY
    // */
    // StringBuilder parkSql = new StringBuilder();

    // parkSql.append("SELECT p.zone, p.division AS ward, p.park_id, p.park_name,
    // ");
    // parkSql.append("COUNT(*) AS employee_count, ");
    // parkSql.append(
    // "COUNT(CASE WHEN LOWER(ofp.verified_status) = 'same' THEN 1 END) AS
    // matched_employee_count, ");
    // parkSql.append(
    // "COUNT(CASE WHEN LOWER(ofp.verified_status) = 'wrong' THEN 1 END) AS
    // not_matched_employee_count ");

    // parkSql.append("FROM park_details p ");

    // parkSql.append("LEFT JOIN ( ");
    // parkSql.append(" SELECT t1.* FROM officer_feedback_parks t1 ");
    // parkSql.append(" INNER JOIN ( ");
    // parkSql.append(" SELECT park_id, MAX(cdate) AS max_date ");
    // parkSql.append(" FROM officer_feedback_parks ");
    // parkSql.append(" WHERE is_active = 1 AND is_delete = 0 ");
    // parkSql.append(" GROUP BY park_id ");
    // parkSql.append(" ) t2 ON t1.park_id = t2.park_id AND t1.cdate = t2.max_date
    // ");
    // parkSql.append(") ofp ON p.park_id = ofp.park_id ");

    // parkSql.append("WHERE 1=1 ");

    // List<Object> parkParams = new ArrayList<>();

    // if (fromDate != null && !fromDate.trim().isEmpty()) {
    // parkSql.append(" AND (ofp.cdate >= ? OR ofp.cdate IS NULL) ");
    // parkParams.add(fromDate + " 00:00:00");
    // }

    // if (toDate != null && !toDate.trim().isEmpty()) {
    // parkSql.append(" AND (ofp.cdate <= ? OR ofp.cdate IS NULL) ");
    // parkParams.add(toDate + " 23:59:59");
    // }

    // if (zone != null && !zone.trim().isEmpty()) {
    // parkSql.append(" AND p.zone = ? ");
    // parkParams.add(zone);
    // }

    // if (ward != null && !ward.trim().isEmpty()) {
    // parkSql.append(" AND p.division = ? ");
    // parkParams.add(ward);
    // }

    // parkSql.append(" GROUP BY p.zone, p.division, p.park_id, p.park_name ");

    // List<Map<String, Object>> parkList =
    // jdbcNULMTemplate.queryForList(parkSql.toString(),
    // parkParams.toArray());

    // /*
    // * MAP
    // */

    // // If ward selected → return park wise data
    // if (ward != null && !ward.trim().isEmpty()) {

    // response.put("status", "Success");
    // response.put("message",
    // parkList.isEmpty() ? "No parks found" : "Park wise report fetched
    // successfully");
    // response.put("data", parkList);

    // return response;
    // }
    // for (Map<String, Object> summary : summaryList) {

    // List<Map<String, Object>> parks = new ArrayList<>();
    // int totalEmployeeCount = 0;

    // for (Map<String, Object> park : parkList) {

    // boolean match = summary.get("zone").toString()
    // .equals(park.get("zone").toString());

    // if (summary.containsKey("ward")) {
    // match = match &&
    // summary.get("ward").toString()
    // .equals(park.get("ward").toString());
    // }

    // if (match) {
    // parks.add(park);

    // Object empObj = park.get("employee_count");
    // if (empObj != null) {
    // totalEmployeeCount += Integer.parseInt(empObj.toString());
    // }
    // }
    // }

    // summary.put("total_employee_count", totalEmployeeCount);
    // // summary.put("parks", parks);
    // }

    // response.put("status", "Success");
    // response.put("message", summaryList.isEmpty() ? "No records found" : "Data
    // fetched successfully");
    // response.put("data", summaryList);

    // } catch (Exception e) {
    // response.put("status", "Failed");
    // response.put("message", "Error fetching report");
    // response.put("error", e.getMessage());
    // }

    // return response;
    // }

    public Map<String, Object> getParksVerificationReport(String zone, String ward,
            String fromDate, String toDate, Integer parkId, String verifiedFlag, String verificationDate) {

        Map<String, Object> response = new HashMap<>();

        try {

            /*
             * EMPLOYEE LEVEL DRILL
             */
            if (parkId != null && verifiedFlag != null && verificationDate != null) {

                StringBuilder empSql = new StringBuilder();

                empSql.append("SELECT ");
                empSql.append("ofp.fid, ");
                empSql.append("ofp.userid, ");
                empSql.append("IFNULL(ofp.enrollment_id,'') AS enrollment_id, ");
                empSql.append("IFNULL(ofp.verified_status,'') AS verified_status, ");
                empSql.append("IFNULL(ofp.lat,'') AS lat, ");
                empSql.append("IFNULL(ofp.`long`,'') AS `long`, ");
                empSql.append("IFNULL(ofp.address,'') AS address, ");
                empSql.append("DATE_FORMAT(ofp.cdate,'%Y-%m-%d %H:%i:%s') AS verification_date, ");
                empSql.append("CASE WHEN ofp.photo_url IS NOT NULL ");
                empSql.append("THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', ofp.photo_url) ");
                empSql.append("ELSE '' END AS verified_image_url ");
                empSql.append("FROM officer_feedback_parks ofp ");
                empSql.append("WHERE ofp.is_active = 1 ");
                empSql.append("AND ofp.is_delete = 0 ");
                empSql.append("AND ofp.park_id = ? ");
                empSql.append("AND LOWER(ofp.verified_status) = ? ");
                empSql.append("AND DATE(ofp.cdate) = ? ");

                List<Map<String, Object>> empList = jdbcNULMTemplate.queryForList(
                        empSql.toString(),
                        parkId,
                        verifiedFlag.toLowerCase(),
                        verificationDate);

                response.put("status", "Success");
                response.put("message",
                        empList.isEmpty() ? "No employee records found"
                                : "Employee verification fetched successfully");
                response.put("data", empList);

                return response;
            }

            /*
             * PARK DATE DRILL
             */
            if (parkId != null) {

                StringBuilder drillSql = new StringBuilder();

                drillSql.append("SELECT ");
                drillSql.append("DATE_FORMAT(ofp.cdate,'%Y-%m-%d') AS inspection_date, ");
                drillSql.append("COUNT(DISTINCT ofp.enrollment_id) AS verified_count, ");
                drillSql.append("COUNT(DISTINCT CASE WHEN LOWER(ofp.verified_status)='same' ");
                drillSql.append("THEN ofp.enrollment_id END) AS same_count, ");
                drillSql.append("COUNT(DISTINCT CASE WHEN LOWER(ofp.verified_status)='wrong' ");
                drillSql.append("THEN ofp.enrollment_id END) AS wrong_count ");
                drillSql.append("FROM officer_feedback_parks ofp ");
                drillSql.append("WHERE ofp.is_active = 1 ");
                drillSql.append("AND ofp.is_delete = 0 ");
                drillSql.append("AND ofp.park_id = ? ");

                List<Object> drillParams = new ArrayList<>();
                drillParams.add(parkId);

                if (fromDate != null && toDate != null) {

                    drillSql.append("AND DATE(ofp.cdate) BETWEEN ? AND ? ");
                    drillParams.add(fromDate);
                    drillParams.add(toDate);
                }

                drillSql.append("GROUP BY inspection_date ");
                drillSql.append("ORDER BY inspection_date DESC ");

                List<Map<String, Object>> drillList = jdbcNULMTemplate.queryForList(
                        drillSql.toString(),
                        drillParams.toArray());

                response.put("status", "Success");
                response.put("message",
                        drillList.isEmpty() ? "No records found"
                                : "Inspection data fetched successfully");
                response.put("data", drillList);

                return response;
            }

            /*
             * SUMMARY QUERY
             */
            StringBuilder sql = new StringBuilder();

            sql.append("SELECT p.zone ");

            if (zone != null && !zone.trim().isEmpty()) {
                sql.append(", p.division AS ward ");
            }

            sql.append(", COUNT(DISTINCT p.park_id) AS park_count ");
            sql.append(", COUNT(DISTINCT ofp.park_id) AS inspected_park_count ");

            sql.append(", COUNT(DISTINCT CASE WHEN LOWER(ofp.verified_status)='same' ");
            sql.append("THEN ofp.enrollment_id END) AS matched_employee_count ");

            sql.append(", COUNT(DISTINCT CASE WHEN LOWER(ofp.verified_status)='wrong' ");
            sql.append("THEN ofp.enrollment_id END) AS not_matched_employee_count ");

            sql.append("FROM park_details p ");

            sql.append("LEFT JOIN officer_feedback_parks ofp ");
            sql.append("ON p.park_id = ofp.park_id ");
            sql.append("AND ofp.is_active = 1 ");
            sql.append("AND ofp.is_delete = 0 ");

            List<Object> params = new ArrayList<>();

            if (fromDate != null && toDate != null) {
                sql.append("AND DATE(ofp.cdate) BETWEEN ? AND ? ");
                params.add(fromDate);
                params.add(toDate);
            }

            sql.append("WHERE 1=1 ");

            if (zone != null && !zone.trim().isEmpty()) {
                sql.append("AND p.zone = ? ");
                params.add(zone);
            }

            if (ward != null && !ward.trim().isEmpty()) {
                sql.append("AND p.division = ? ");
                params.add(ward);
            }

            if (zone != null && !zone.trim().isEmpty()) {
                sql.append("GROUP BY p.zone, p.division ");
            } else {
                sql.append("GROUP BY p.zone ");
            }

            List<Map<String, Object>> summaryList = jdbcNULMTemplate.queryForList(sql.toString(), params.toArray());

            /*
             * PARK WISE QUERY
             */
            StringBuilder parkSql = new StringBuilder();

            parkSql.append("SELECT ");
            parkSql.append("p.zone, ");
            parkSql.append("p.division AS ward, ");
            parkSql.append("e.park_id, ");
            parkSql.append("e.park_name, ");
            parkSql.append("COUNT(DISTINCT e.enrollment_id) AS employee_count, ");

            parkSql.append("COUNT(DISTINCT CASE WHEN LOWER(ofp.verified_status)='same' ");
            parkSql.append("THEN ofp.enrollment_id END) AS matched_employee_count, ");

            parkSql.append("COUNT(DISTINCT CASE WHEN LOWER(ofp.verified_status)='wrong' ");
            parkSql.append("THEN ofp.enrollment_id END) AS not_matched_employee_count ");

            parkSql.append("FROM enrollment_table e ");
            parkSql.append("LEFT JOIN park_details p ON p.park_id = e.park_id ");

            parkSql.append("LEFT JOIN officer_feedback_parks ofp ");
            parkSql.append("ON e.enrollment_id = ofp.enrollment_id ");
            parkSql.append("AND ofp.is_active = 1 ");
            parkSql.append("AND ofp.is_delete = 0 ");

            List<Object> parkParams = new ArrayList<>();

            if (fromDate != null && toDate != null) {
                parkSql.append("AND DATE(ofp.cdate) BETWEEN ? AND ? ");
                parkParams.add(fromDate);
                parkParams.add(toDate);
            }

            parkSql.append("WHERE e.isactive = 1 ");
            parkSql.append("AND e.isdelete = 0 ");

            if (zone != null && !zone.trim().isEmpty()) {
                parkSql.append("AND p.zone = ? ");
                parkParams.add(zone);
            }

            if (ward != null && !ward.trim().isEmpty()) {
                parkSql.append("AND p.division = ? ");
                parkParams.add(ward);
            }

            parkSql.append("GROUP BY p.zone,p.division,e.park_id,e.park_name ");

            List<Map<String, Object>> parkList = jdbcNULMTemplate.queryForList(parkSql.toString(),
                    parkParams.toArray());

            if (ward != null && !ward.trim().isEmpty()) {

                response.put("status", "Success");
                response.put("message",
                        parkList.isEmpty() ? "No parks found"
                                : "Park wise report fetched successfully");
                response.put("data", parkList);

                return response;
            }

            for (Map<String, Object> summary : summaryList) {

                int totalEmployeeCount = 0;

                for (Map<String, Object> park : parkList) {

                    String summaryZone = String.valueOf(summary.get("zone"));
                    String parkZone = String.valueOf(park.get("zone"));

                    boolean match = summaryZone.equals(parkZone);

                    if (summary.containsKey("ward")) {

                        String summaryWard = String.valueOf(summary.get("ward"));
                        String parkWard = String.valueOf(park.get("ward"));

                        match = match && summaryWard.equals(parkWard);
                    }

                    if (match) {

                        Object empObj = park.get("employee_count");

                        if (empObj != null) {
                            totalEmployeeCount += Integer.parseInt(empObj.toString());
                        }
                    }
                }

                summary.put("total_employee_count", totalEmployeeCount);
            }

            response.put("status", "Success");
            response.put("message",
                    summaryList.isEmpty() ? "No records found"
                            : "Report fetched successfully");
            response.put("data", summaryList);

        } catch (Exception e) {

            response.put("status", "Failed");
            response.put("message", "Error fetching report");
            response.put("error", e.getMessage());
        }

        return response;
    }

    @Transactional
    public Map<String, Object> saveStaffDeviceDetails(
            String userid,
            String supervisor_id,
            String parkid,
            String deviceId,
            String latitude,
            String longitude,
            String address) {

        Map<String, Object> response = new HashMap<>();

        try {

            // Validate park_id
            if (parkid == null || parkid.trim().isEmpty()) {
                response.put("status", "Failed");
                response.put("message", "park_id is required");
                return response;
            }

            Integer parsedParkId = Integer.parseInt(parkid.trim());

            // CHECK DUPLICATE (IMPORTANT)
            String checkSql = "SELECT COUNT(*) FROM parks_supervisor_device_data WHERE park_id = ?";
            Integer count = jdbcNULMTemplate.queryForObject(checkSql, Integer.class, parsedParkId);

            if (count != null && count > 0) {
                response.put("status", "Failed");
                response.put("message", "Device already saved for this park_id");
                return response;
            }

            // INSERT QUERY
            String sql = "INSERT INTO parks_supervisor_device_data " +
                    "(userid, park_id, supervisor_id, device_id, latitude, longitude, address) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            String[] supervisorIds = (supervisor_id != null && !supervisor_id.isBlank())
                    ? supervisor_id.split(",")
                    : new String[] { null };

            int successCount = 0;

            for (String id : supervisorIds) {

                Integer parsedSupervisorId = null;

                try {
                    if (id != null && !id.trim().isEmpty()) {
                        parsedSupervisorId = Integer.parseInt(id.trim());
                    }
                } catch (Exception ignored) {
                }

                final Integer finalSupervisorId = parsedSupervisorId;

                int rows = jdbcNULMTemplate.update(con -> {
                    PreparedStatement ps = con.prepareStatement(sql);

                    ps.setString(1, userid);
                    ps.setInt(2, parsedParkId);

                    if (finalSupervisorId != null) {
                        ps.setInt(3, finalSupervisorId);
                    } else {
                        ps.setNull(3, Types.INTEGER);
                    }

                    ps.setString(4, deviceId);
                    ps.setString(5, latitude);
                    ps.setString(6, longitude);
                    ps.setString(7, address);

                    return ps;
                });

                if (rows > 0) {
                    successCount++;
                }
            }

            response.put("status", "Success");
            response.put("message", "Saved successfully for " + successCount + " records");

        } catch (Exception ex) {
            ex.printStackTrace();

            response.put("status", "Failed");
            response.put("message", "Error while saving: " + ex.getMessage());
        }

        return response;
    }

    // parks device details updation

    public Map<String, Object> checkSupervisorDevice(Integer parkId, int supervisorId, String deviceId) {

        Map<String, Object> response = new HashMap<>();

        try {

            List<Map<String, Object>> supervisorList;

            // 1. Check supervisor exists
            if (parkId != null) {
                String sql = "SELECT * FROM parks_supervisor_device_data WHERE park_id = ? AND supervisor_id = ? AND is_delete = 0";
                supervisorList = jdbcNULMTemplate.queryForList(sql, parkId, supervisorId);
            } else {
                String sql = "SELECT * FROM parks_supervisor_device_data WHERE supervisor_id = ? AND is_delete = 0";
                supervisorList = jdbcNULMTemplate.queryForList(sql, supervisorId);
            }

            if (supervisorList.isEmpty()) {
                response.put("status", "Success");
                response.put("message", false);
                response.put("msg", "Supervisor details not found");
                return response;
            }

            // 2. Check same supervisor + same device
            List<Map<String, Object>> deviceList;

            if (parkId != null) {
                String sql = "SELECT * FROM parks_supervisor_device_data WHERE park_id = ? AND supervisor_id = ? AND device_id = ? AND is_delete = 0";
                deviceList = jdbcNULMTemplate.queryForList(sql, parkId, supervisorId, deviceId);
            } else {
                String sql = "SELECT * FROM parks_supervisor_device_data WHERE supervisor_id = ? AND device_id = ? AND is_delete = 0";
                deviceList = jdbcNULMTemplate.queryForList(sql, supervisorId, deviceId);
            }

            if (!deviceList.isEmpty()) {
                response.put("status", "Success");
                response.put("message", true);
                response.put("msg", "Already registered for this supervisor and device");
                return response;
            }

            // 3. NEW: Check device already exists globally (any supervisor)
            String globalCheckSql = "SELECT supervisor_id FROM parks_supervisor_device_data WHERE device_id = ? AND is_delete = 0";
            List<Map<String, Object>> globalDeviceList = jdbcNULMTemplate.queryForList(globalCheckSql, deviceId);

            if (!globalDeviceList.isEmpty()) {

                Map<String, Object> existing = globalDeviceList.get(0);

                response.put("status", "Failed");
                response.put("message", false);
                response.put("msg", "Device already mapped to supervisor ID: " + existing.get("supervisor_id"));

                return response;
            }

            // 4. Move old record → history
            Map<String, Object> existingRecord = supervisorList.get(0);

            String insertHistorySql = "INSERT INTO parks_supervisor_device_history " +
                    "(userid, park_id, supervisor_id, device_id, latitude, longitude, address, cdate, updated_date, is_active, is_delete) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?)";

            jdbcNULMTemplate.update(insertHistorySql,
                    existingRecord.get("userid"),
                    existingRecord.get("park_id"),
                    existingRecord.get("supervisor_id"),
                    existingRecord.get("device_id"),
                    existingRecord.get("latitude"),
                    existingRecord.get("longitude"),
                    existingRecord.get("address"),
                    existingRecord.get("cdate"),
                    existingRecord.get("is_active"),
                    existingRecord.get("is_delete"));

            // 5. Update device_id
            String updateSql;

            if (parkId != null) {
                updateSql = "UPDATE parks_supervisor_device_data SET device_id = ?, cdate = NOW() WHERE park_id = ? AND supervisor_id = ?";
                jdbcNULMTemplate.update(updateSql, deviceId, parkId, supervisorId);
            } else {
                updateSql = "UPDATE parks_supervisor_device_data SET device_id = ?, cdate = NOW() WHERE supervisor_id = ?";
                jdbcNULMTemplate.update(updateSql, deviceId, supervisorId);
            }

            response.put("status", "Success");
            response.put("message", true);
            response.put("msg", "Device updated successfully");

        } catch (Exception e) {

            response.put("status", "Error");
            response.put("message", false);
            response.put("msg", e.getMessage());
        }

        return response;
    }
}
