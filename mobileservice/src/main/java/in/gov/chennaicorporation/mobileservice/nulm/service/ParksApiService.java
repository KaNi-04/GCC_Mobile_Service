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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public List<Map<String, Object>> getParkDetails(String division) {
        String sql = "select * from park_details where division = ?";
        List<Map<String, Object>> result = jdbcNULMTemplate.queryForList(sql, division);
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
                throw new RuntimeException("Please upload image"); // ✅ custom message
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
                    "(userid, enrollment_id, park_id, photo_url, lat, `long`, address, verified, verified_status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            String[] enrollmentIds = (enrollmentId != null && !enrollmentId.isBlank())
                    ? enrollmentId.split(",")
                    : new String[] { null };

            int successCount = 0;

            for (String id : enrollmentIds) {

                Integer parsedId = null;
                String status = ""; // default empty

                // ✅ validation logic
                try {
                    if (id != null && !id.trim().isEmpty()) {
                        parsedId = Integer.parseInt(id.trim());
                    } else {
                        status = "WRONG";
                    }
                } catch (Exception ex) {
                    status = "WRONG";
                }

                // ✅ make final for lambda
                final Integer finalParsedId = parsedId;
                final String finalStatus = status;

                int rows = jdbcNULMTemplate.update(con -> {
                    PreparedStatement ps = con.prepareStatement(sql);

                    ps.setString(1, userid);

                    if (finalParsedId != null) {
                        ps.setInt(2, finalParsedId);
                    } else {
                        ps.setNull(2, Types.INTEGER);
                    }
                    ps.setString(3, parkid);
                    ps.setString(4, photoUrl);
                    ps.setString(5, latitude);
                    ps.setString(6, longitude);
                    ps.setString(7, address);
                    ps.setString(8, "NO"); // always NO
                    ps.setString(9, finalStatus); // "" or WRONG

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

    // @Transactional
    // public Map<String, Object> saveStaffVerificationDetails(
    // String userid,
    // String enrollmentId,
    // String photoUrl,
    // String latitude,
    // String longitude,
    // String address,
    // String verifiedStatus) {

    // Map<String, Object> response = new HashMap<>();

    // try {

    // String sql = "INSERT INTO officer_feedback_parks " +
    // "(userid, enrollment_id, photo_url, lat, `long`, address, verified,
    // verified_status) " +
    // "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    // String[] enrollmentIds = enrollmentId.split(","); // split

    // KeyHolder keyHolder = new GeneratedKeyHolder();

    // for (String id : enrollmentIds) {

    // jdbcNULMTemplate.update(con -> {
    // PreparedStatement ps = con.prepareStatement(sql,
    // Statement.RETURN_GENERATED_KEYS);

    // ps.setString(1, userid);
    // ps.setInt(2, Integer.parseInt(id.trim())); // ✅ safe now
    // ps.setString(3, photoUrl);
    // ps.setString(4, latitude);
    // ps.setString(5, longitude);
    // ps.setString(6, address);
    // ps.setString(7, "NO");
    // ps.setString(8, verifiedStatus != null ? verifiedStatus : "");

    // return ps;
    // }, keyHolder);
    // }

    // response.put("status", "Success");
    // response.put("message", "Saved successfully for all enrollmentIds");

    // return response;

    // } catch (Exception ex) {
    // ex.printStackTrace();

    // response.put("status", "Failed");
    // response.put("message", "Error while saving: " + ex.getMessage());

    // return response;
    // }
    // }

    // public List<Map<String, Object>> getStaffListForAttendance(String parkid,
    // String date) {

    // Map<String, Object> response = new HashMap<>();
    // String formattedDate;

    // try {
    // // Convert dd-MM-yyyy → yyyy-MM-dd
    // DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    // DateTimeFormatter outputFormatter =
    // DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // LocalDate parsedDate = LocalDate.parse(date, inputFormatter);
    // formattedDate = parsedDate.format(outputFormatter);

    // } catch (Exception e) {
    // response.put("status", "Failed");
    // response.put("message", "Invalid date format. Use dd-MM-yyyy");
    // response.put("Data", Collections.emptyList());
    // return Collections.singletonList(response);
    // }

    // String sqlQuery = "SELECT e.*, " +
    // " IFNULL(DATE_FORMAT(a.indatetime, '%d-%m-%Y %l:%i %p'), '') AS indatetime, "
    // +
    // " IFNULL(DATE_FORMAT(a.outdatetime, '%d-%m-%Y %l:%i %p'), '') AS outdatetime,
    // " +
    // " a.inby, " +
    // " a.outby, " +
    // " a.inphoto, " +
    // " a.outphoto " +
    // "FROM enrollment_table e " +
    // "LEFT JOIN attendance a ON e.enrollment_id = a.enrollment_id " +
    // " AND DATE(a.indatetime) = ? " + // filter by date
    // " AND a.indatetime = ( " +
    // " SELECT MAX(a2.indatetime) " +
    // " FROM attendance a2 " +
    // " WHERE a2.enrollment_id = e.enrollment_id " +
    // " AND DATE(a2.indatetime) = ? " + // same date filter
    // " ) " +
    // "WHERE e.isactive = 1 " +
    // " AND e.appointed = 1 " +
    // " AND e.facial_attendance = 1 " +
    // " AND e.emp_type = 'Park' " +
    // " AND FIND_IN_SET(?, e.park_id) > 0";

    // System.out.println("Executing Query: " + sqlQuery);
    // System.out.println("Params → date: " + formattedDate + ", parkid: " +
    // parkid);

    // List<Map<String, Object>> result = jdbcNULMTemplate.queryForList(sqlQuery,
    // formattedDate, formattedDate,
    // parkid);

    // response.put("status", "Success");
    // response.put("message", "Request List");
    // response.put("Data", result);

    // return Collections.singletonList(response);
    // }

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

}
