package in.gov.chennaicorporation.mobileservice.gccTenements.service;

import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.DataSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

import org.springframework.http.ResponseEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TenementService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private String fileBaseUrl;

    @Autowired
    private Environment environment;

    @Autowired
    public TenementService(Environment environment) {
        this.environment = environment;
        this.fileBaseUrl = environment.getProperty("fileBaseUrl");
    }

    @Autowired
    public void setDataSource(@Qualifier("mysqlTenementDataSource") DataSource tenementDataSource) {
        this.jdbcTemplate = new JdbcTemplate(tenementDataSource);
    }

    public Map<String, Object> getTenementsListByWard(String loginid, String type) {

        Map<String, Object> response = new HashMap<>();

        try {

            String sqlQuery = """
                    SELECT ward
                    FROM gcc_penalty_hoardings.hoading_user_list
                    WHERE userid = ?
                    AND isactive = 1
                    LIMIT 1
                    """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery, loginid);

            if (results.isEmpty()) {

                response.put("status", "failed");
                response.put("message", "No ward mapped");

                return response;
            }

            String ward = results.get(0).get("ward").toString();

            String sqlQuery2 = """
                    SELECT
                    zone,
                    ward,
                        id AS assetmasterid,
                        No_of_tenements,
                        scheme_name,
                        area
                    FROM asset_master
                    WHERE ward = ?
                    """;

            List<Map<String, Object>> results2 = jdbcTemplate.queryForList(sqlQuery2, ward);

            // ✅ check mapping using assetmasterid
            for (Map<String, Object> item : results2) {

                Integer assetmasterid = Integer.parseInt(item.get("assetmasterid").toString());

                String checkSql = """
                        SELECT COUNT(*)
                        FROM asset_list
                        WHERE am_id = ?
                        """;

                Integer count = jdbcTemplate.queryForObject(
                        checkSql,
                        Integer.class,
                        assetmasterid);

                // ✅ mapped / not mapped flag
                if (count != null && count > 0) {

                    item.put("mapping_status", true);

                } else {

                    item.put("mapping_status", false);
                }
            }

            response.put("status", "success");
            response.put("ward", ward);
            response.put("tenements", results2);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getTenementsListByWard1(String loginid, String type) {

        Map<String, Object> response = new HashMap<>();

        String sqlQuery = """
                SELECT ward
                FROM gcc_penalty_hoardings.hoading_user_list
                WHERE userid = ?

                AND isactive = 1
                LIMIT 1
                """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery, loginid);

        if (results.isEmpty()) {

            response.put("status", "failed");
            response.put("message", "No ward mapped");

            return response;
        }

        String ward = results.get(0).get("ward").toString();

        String sqlQuery2 = """
                SELECT *
                FROM asset_master
                WHERE ward = ?
                """;

        List<Map<String, Object>> results2 = jdbcTemplate.queryForList(sqlQuery2, ward);

        response.put("status", "success");
        response.put("ward", ward);
        response.put("tenements", results2);

        return response;
    }

    public String fileUpload(String name, String id, MultipartFile file) {

        int lastInsertId = 0;
        // Set the file path where you want to save it
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("tenement_image_foldername");
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
            String fileName = name + "_" + id + "_" + datetimetxt + "_" + file.getOriginalFilename();
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

    @Transactional
    public Map<String, Object> saveAsset(String zone, String ward, String latitude, String longitude, String address,
            String am_id, String name, String cby, MultipartFile image) {

        Map<String, Object> response = new HashMap<>();

        try {

            String imagePath = "";
            if (image == null || image.isEmpty()) {

                response.put("status", "failed");
                response.put("message", "image is required");

                return response;
            }

            // image upload
            if (image != null && !image.isEmpty()) {

                imagePath = fileUpload("asset", "0", image);
            }

            // ✅ final variable for lambda
            final String finalImagePath = imagePath;

            String sql = """
                    INSERT INTO asset_list
                    (zone, ward, latitude, longitude, address, am_id, name, image_path, cby)
                    VALUES
                    (?,?,?,?,?,?,?,?,?)
                    """;

            KeyHolder keyHolder = new GeneratedKeyHolder();

            int affectedRows = jdbcTemplate.update(connection -> {

                PreparedStatement ps = connection.prepareStatement(
                        sql,
                        new String[] { "id" });

                ps.setString(1, zone);
                ps.setString(2, ward);
                ps.setString(3, latitude);
                ps.setString(4, longitude);
                ps.setString(5, address);
                ps.setString(6, am_id);
                ps.setString(7, name);
                ps.setString(8, finalImagePath);
                ps.setString(9, cby);

                return ps;

            }, keyHolder);

            if (affectedRows > 0) {

                response.put("status", "success");
                response.put("message", "Asset saved successfully");
                response.put("asset_listid", keyHolder.getKey().intValue());

            } else {

                response.put("status", "failed");
                response.put("message", "Insert failed");
            }

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    // without scheme name
    /*
     * public List<Map<String, Object>> getAssetListByRadius(
     * String latitudeStr,
     * String longitudeStr) {
     * 
     * List<Map<String, Object>> response = new ArrayList<>();
     * Map<String, Object> result = new HashMap<>();
     * 
     * try {
     * 
     * double latitude = Double.parseDouble(latitudeStr);
     * double longitude = Double.parseDouble(longitudeStr);
     * 
     * // ✅ 500 meter radius check
     * String sql = "SELECT " +
     * "id as assetlistid, " +
     * "zone, " +
     * "ward, " +
     * "latitude, " +
     * "longitude, " +
     * "address, " +
     * "am_id, " +
     * "name, " +
     * "radius, " +
     * 
     * "CASE " +
     * "WHEN image_path IS NOT NULL " +
     * "THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', image_path) " +
     * "ELSE '' " +
     * "END AS image_path, " +
     * 
     * "(" +
     * "6371008.8 * ACOS( " +
     * "COS(RADIANS(?)) * " +
     * "COS(RADIANS(latitude)) * " +
     * "COS(RADIANS(longitude) - RADIANS(?)) + " +
     * "SIN(RADIANS(?)) * " +
     * "SIN(RADIANS(latitude)) " +
     * ")" +
     * ") AS distance " +
     * 
     * "FROM gcc_tenements.asset_list " +
     * 
     * "HAVING distance <= 500 " +
     * 
     * "ORDER BY distance";
     * 
     * List<Map<String, Object>> assetList = jdbcTemplate.queryForList(
     * sql,
     * latitude,
     * longitude,
     * latitude);
     * 
     * result.put("status", "success");
     * result.put("count", assetList.size());
     * result.put("data", assetList);
     * 
     * } catch (Exception e) {
     * 
     * e.printStackTrace();
     * 
     * result.put("status", "error");
     * result.put("message", e.getMessage());
     * }
     * 
     * response.add(result);
     * 
     * return response;
     * }
     */
    public Map<String, Object> getAssetListByRadius(
            String latitudeStr,
            String longitudeStr) {

        Map<String, Object> response = new HashMap<>();

        try {

            double latitude = Double.parseDouble(latitudeStr);
            double longitude = Double.parseDouble(longitudeStr);

            String sql = "SELECT " +

                    "al.id AS assetlistid, " +
                    "al.zone, " +
                    "al.ward, " +
                    "al.latitude, " +
                    "al.longitude, " +
                    "al.address, " +
                    "al.am_id, " +

                    // ✅ scheme name
                    "am.scheme_name, " +

                    "al.name, " +
                    "al.radius, " +

                    "CASE " +
                    "WHEN al.image_path IS NOT NULL " +
                    "THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', al.image_path) " +
                    "ELSE '' " +
                    "END AS image_path, " +
                    // ✅ pending count
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il1 " +
                    "WHERE il1.assetlist_id = al.id " +
                    "AND il1.final_status != 'verified' " +
                    ") AS pending_count, " +

                    "(" +
                    "6371008.8 * ACOS( " +
                    "COS(RADIANS(?)) * " +
                    "COS(RADIANS(al.latitude)) * " +
                    "COS(RADIANS(al.longitude) - RADIANS(?)) + " +
                    "SIN(RADIANS(?)) * " +
                    "SIN(RADIANS(al.latitude)) " +
                    ")" +
                    ") AS distance " +

                    "FROM asset_list al " +

                    "LEFT JOIN asset_master am " +
                    "ON am.id = al.am_id " +

                    "HAVING distance <= 500 " +

                    "ORDER BY distance";

            List<Map<String, Object>> assetList = jdbcTemplate.queryForList(
                    sql,
                    latitude,
                    longitude,
                    latitude);

            response.put("status", "success");
            response.put("count", assetList.size());
            response.put("data", assetList);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    /*
     * @Transactional
     * public Map<String, Object> saveIssue(
     * 
     * String zone,
     * String ward,
     * String latitude,
     * String longitude,
     * String assetlist_id,
     * MultipartFile before_image,
     * String cby) {
     * 
     * Map<String, Object> response = new HashMap<>();
     * 
     * try {
     * 
     * String imagePath = "";
     * 
     * // ✅ image upload
     * if (before_image != null && !before_image.isEmpty()) {
     * 
     * imagePath = fileUpload("issue", "0", before_image);
     * }
     * 
     * final String finalImagePath = imagePath;
     * 
     * String sql = """
     * INSERT INTO issue_list1
     * (
     * zone,
     * ward,
     * latitide,
     * longitude,
     * assetlist_id,
     * final_status,
     * before_image,
     * cby
     * )
     * VALUES
     * (?,?,?,?,?,?,?,?)
     * """;
     * 
     * KeyHolder keyHolder = new GeneratedKeyHolder();
     * 
     * int affectedRows = jdbcTemplate.update(connection -> {
     * 
     * PreparedStatement ps = connection.prepareStatement(
     * sql,
     * new String[] { "id" });
     * 
     * ps.setString(1, zone);
     * ps.setString(2, ward);
     * ps.setString(3, latitude);
     * ps.setString(4, longitude);
     * ps.setString(5, assetlist_id);
     * 
     * // ✅ fixed status
     * ps.setString(6, "created");
     * 
     * ps.setString(7, finalImagePath);
     * ps.setString(8, cby);
     * 
     * return ps;
     * 
     * }, keyHolder);
     * 
     * if (affectedRows > 0) {
     * 
     * response.put("status", "success");
     * response.put("message", "Issue saved successfully");
     * response.put("issue_id",
     * keyHolder.getKey().intValue());
     * 
     * } else {
     * 
     * response.put("status", "failed");
     * response.put("message", "Insert failed");
     * }
     * 
     * } catch (Exception e) {
     * 
     * e.printStackTrace();
     * 
     * response.put("status", "error");
     * response.put("message", e.getMessage());
     * }
     * 
     * return response;
     * }
     */
    @Transactional
    public Map<String, Object> saveIssue(

            String zone,
            String ward,
            String latitude,
            String longitude,
            String assetlist_id,
            MultipartFile before_image,
            String radius,
            String am_id,
            String cby, String remarks) {

        Map<String, Object> response = new HashMap<>();

        try {

            // ✅ convert lat/lng
            double lat = Double.parseDouble(latitude);
            double lng = Double.parseDouble(longitude);

            // ✅ check asset exists within 100 meters
            String checkSql = """
                    SELECT COUNT(*)
                    FROM asset_list
                    WHERE  am_id = ? AND (
                        6371008.8 * ACOS(
                            COS(RADIANS(?)) *
                            COS(RADIANS(latitude)) *
                            COS(RADIANS(longitude) - RADIANS(?)) +
                            SIN(RADIANS(?)) *
                            SIN(RADIANS(latitude))
                        )
                    ) <= ?
                    """;

            Integer count = jdbcTemplate.queryForObject(
                    checkSql,
                    Integer.class,
                    am_id,
                    lat,
                    lng,
                    lat,
                    radius);
            System.out.println("count" + count);

            if (count == null || count <= 0) {

                response.put("status", "failed");
                response.put("message",
                        "you are away from asset radius (" + radius + "metres)");

                return response;
            }

            String imagePath = "";
            if (before_image == null || before_image.isEmpty()) {

                response.put("status", "failed");
                response.put("message", "Before image is required");

                return response;
            }

            // ✅ image upload
            if (before_image != null && !before_image.isEmpty()) {

                imagePath = fileUpload("issue", "0", before_image);
            }

            final String finalImagePath = imagePath;

            String sql = """
                    INSERT INTO issue_list1
                    (
                        zone,
                        ward,
                        latitide,
                        longitude,
                        assetlist_id,
                        final_status,
                        before_image,
                        remarks,
                        cby
                    )
                    VALUES
                    (?,?,?,?,?,?,?,?,?)
                    """;

            KeyHolder keyHolder = new GeneratedKeyHolder();

            int affectedRows = jdbcTemplate.update(connection -> {

                PreparedStatement ps = connection.prepareStatement(
                        sql,
                        new String[] { "id" });

                ps.setString(1, zone);
                ps.setString(2, ward);
                ps.setString(3, latitude);
                ps.setString(4, longitude);
                ps.setString(5, assetlist_id);

                // ✅ fixed status
                ps.setString(6, "created");

                ps.setString(7, finalImagePath);
                ps.setString(8, remarks);
                ps.setString(9, cby);

                return ps;

            }, keyHolder);

            if (affectedRows > 0) {

                response.put("status", "success");
                response.put("message", "Issue saved successfully");
                response.put("issue_id",
                        keyHolder.getKey().intValue());

            } else {

                response.put("status", "failed");
                response.put("message", "Insert failed");
            }

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    @Transactional
    public Map<String, Object> saveIssueCompletion(

            String issuelist1_id,
            String zone,
            String ward,
            String latitude,
            String longitude,
            String assetlist_id,
            MultipartFile after_image,
            String remarks,
            String radius,
            String am_id,
            String cby) {

        Map<String, Object> response = new HashMap<>();

        try {
            double lat = Double.parseDouble(latitude);
            double lng = Double.parseDouble(longitude);

            // ✅ check asset exists within 100 meters
            String checkSql = """
                    SELECT COUNT(*)
                    FROM asset_list
                    WHERE  am_id = ? AND (
                        6371008.8 * ACOS(
                            COS(RADIANS(?)) *
                            COS(RADIANS(latitude)) *
                            COS(RADIANS(longitude) - RADIANS(?)) +
                            SIN(RADIANS(?)) *
                            SIN(RADIANS(latitude))
                        )
                    ) <= ?
                    """;

            Integer count = jdbcTemplate.queryForObject(
                    checkSql,
                    Integer.class,
                    am_id,
                    lat,
                    lng,
                    lat,
                    radius);
            System.out.println("count" + count);

            if (count == null || count <= 0) {

                response.put("status", "failed");
                response.put("message",
                        "you are away from asset radius (" + radius + "metres)");

                return response;
            }

            String imagePath = "";

            // ✅ upload image
            if (after_image != null && !after_image.isEmpty()) {

                imagePath = fileUpload("issue_completion", "0", after_image);
            }

            final String finalImagePath = imagePath;

            // ✅ insert into issue_list2
            String insertSql = """
                    INSERT INTO issue_list2
                    (
                        issuelist1_id,
                        zone,
                        ward,
                        latitide,
                        longitude,
                        assetlist_id,
                        final_status,
                        after_image,
                        cby,
                        remarks
                    )
                    VALUES
                    (?,?,?,?,?,?,?,?,?,?)
                    """;

            KeyHolder keyHolder = new GeneratedKeyHolder();

            int affectedRows = jdbcTemplate.update(connection -> {

                PreparedStatement ps = connection.prepareStatement(
                        insertSql,
                        new String[] { "id" });

                ps.setString(1, issuelist1_id);
                ps.setString(2, zone);
                ps.setString(3, ward);
                ps.setString(4, latitude);
                ps.setString(5, longitude);
                ps.setString(6, assetlist_id);

                // ✅ completed status
                ps.setString(7, "completed");

                ps.setString(8, finalImagePath);
                ps.setString(9, cby);
                ps.setString(10, remarks);

                return ps;

            }, keyHolder);

            // ✅ update issue_list1
            if (affectedRows > 0) {

                String updateSql = """
                        UPDATE issue_list1
                        SET final_status = 'completed'
                        WHERE id = ?
                        """;

                jdbcTemplate.update(updateSql, issuelist1_id);

                response.put("status", "success");
                response.put("message",
                        "Issue completed successfully");

                response.put("issue_list2_id",
                        keyHolder.getKey().intValue());

            } else {

                response.put("status", "failed");
                response.put("message", "Insert failed");
            }

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    @Transactional
    public Map<String, Object> saveIssueVerification(

            String issuelist1_id,
            String issuelist2_id,
            String zone,
            String ward,
            String latitude,
            String longitude,
            String assetlist_id,
            String remarks,

            String radius,
            String am_id,
            String cby) {

        Map<String, Object> response = new HashMap<>();

        try {

            // ✅ insert into issue_list3
            String insertSql = """
                    INSERT INTO issue_list3
                    (
                        issuelist1_id,
                        issuelist2_id,
                        zone,
                        ward,
                        latitide,
                        longitude,
                        assetlist_id,
                        final_status,

                        remarks,
                        cby
                    )
                    VALUES
                    (?,?,?,?,?,?,?,?,?,?)
                    """;

            KeyHolder keyHolder = new GeneratedKeyHolder();

            int affectedRows = jdbcTemplate.update(connection -> {

                PreparedStatement ps = connection.prepareStatement(
                        insertSql,
                        new String[] { "id" });

                ps.setString(1, issuelist1_id);
                ps.setString(2, issuelist2_id);
                ps.setString(3, zone);
                ps.setString(4, ward);
                ps.setString(5, latitude);
                ps.setString(6, longitude);
                ps.setString(7, assetlist_id);

                // ✅ verified status
                ps.setString(8, "verified");

                ps.setString(9, remarks);
                ps.setString(10, cby);

                return ps;

            }, keyHolder);

            // ✅ update issue_list1
            String updateSql1 = """
                    UPDATE issue_list1
                    SET final_status = 'verified'
                    WHERE id = ?
                    """;

            jdbcTemplate.update(updateSql1, issuelist1_id);

            response.put("status", "success");
            response.put("message",
                    "Issue verified successfully");

            response.put("issue_list3_id",
                    keyHolder.getKey().intValue());

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getCreatedIssueList(String loginid) {

        Map<String, Object> response = new HashMap<>();

        try {

            // ✅ fetch ward
            String wardSql = """
                    SELECT ward
                    FROM gcc_penalty_hoardings.hoading_user_list
                    WHERE userid = ?
                    AND isactive = 1
                    LIMIT 1
                    """;

            List<Map<String, Object>> wardResult = jdbcTemplate.queryForList(wardSql, loginid);

            if (wardResult.isEmpty()) {

                response.put("status", "failed");
                response.put("message", "No ward mapped");

                return response;
            }

            String ward = wardResult.get(0).get("ward").toString();

            // ✅ issue + asset details
            String sql = "SELECT " +

                    "il1.id AS issuelist1_id, " +
                    "il1.zone, " +
                    "il1.ward, " +

                    // issue lat/lng
                    "il1.latitide, " +
                    "il1.longitude, " +

                    "il1.assetlist_id, " +
                    "il1.final_status, " +
                    "il1.cby, " +
                    "il1.cdate, " +
                    "il1.remarks, " +
                    "am.scheme_name, " +

                    "al.radius, " +
                    "al.am_id, " +

                    // ✅ registered asset lat/lng
                    "al.latitude AS registered_latitude, " +
                    "al.longitude AS registered_longitude, " +
                    "al.name, " +

                    // ✅ registered asset image
                    "CASE " +
                    "WHEN al.image_path IS NOT NULL " +
                    "THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', al.image_path) " +
                    "ELSE '' " +
                    "END AS registered_image, " +

                    // ✅ before image
                    "CASE " +
                    "WHEN il1.before_image IS NOT NULL " +
                    "THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', il1.before_image) " +
                    "ELSE '' " +
                    "END AS before_image, " +
                    "il1.cdate as creation_date " +

                    "FROM issue_list1 il1 " +

                    // ✅ join asset_list
                    "LEFT JOIN asset_list al " +
                    "ON al.id = il1.assetlist_id " +

                    // ✅ join asset_master
                    "LEFT JOIN asset_master am " +
                    "ON am.id = al.am_id " +

                    "WHERE il1.ward = ? " +
                    "AND il1.final_status = 'created' " +

                    "ORDER BY il1.id DESC";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, ward);

            response.put("status", "success");
            response.put("ward", ward);
            response.put("count", result.size());
            response.put("data", result);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getIssueVerificationList(String loginid) {

        Map<String, Object> response = new HashMap<>();

        try {

            // ✅ fetch ward
            String wardSql = """
                    SELECT ward
                    FROM gcc_penalty_hoardings.hoading_user_list
                    WHERE userid = ?
                    AND isactive = 1
                    LIMIT 1
                    """;

            List<Map<String, Object>> wardResult = jdbcTemplate.queryForList(wardSql, loginid);

            if (wardResult.isEmpty()) {

                response.put("status", "failed");
                response.put("message", "No ward mapped");

                return response;
            }

            String ward = wardResult.get(0).get("ward").toString();

            // ✅ issue_list1 + asset_list + issue_list2
            String sql = "SELECT " +

            // issue_list1
                    "il1.id AS issuelist1_id, " +
                    "il1.zone, " +
                    "il1.ward, " +
                    "il1.latitide, " +
                    "il1.longitude, " +
                    "il1.assetlist_id, " +
                    "il1.final_status, " +
                    "il1.cby, " +
                    "il1.cdate, " +
                    "am.scheme_name, " +

                    "al.radius, " +
                    "al.am_id, " +
                    "al.name, " +

                    "il1.remarks as before_remarks, " +
                    "il1.cdate as creation_date, " +
                    // registered asset
                    "al.latitude AS registered_latitude, " +
                    "al.longitude AS registered_longitude, " +
                    "il2.remarks as after_remarks, " +

                    "CASE " +
                    "WHEN al.image_path IS NOT NULL " +
                    "THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', al.image_path) " +
                    "ELSE '' " +
                    "END AS registered_image, " +

                    // before image
                    "CASE " +
                    "WHEN il1.before_image IS NOT NULL " +
                    "THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', il1.before_image) " +
                    "ELSE '' " +
                    "END AS before_image, " +

                    // issue_list2
                    "il2.id AS issuelist2_id, " +
                    "il2.final_status AS completed_status, " +
                    "il2.latitide AS completed_latitude, " +
                    "il2.longitude AS completed_longitude, " +
                    "il2.cdate as completion_date, " +

                    "CASE " +
                    "WHEN il2.after_image IS NOT NULL " +
                    "THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', il2.after_image) " +
                    "ELSE '' " +
                    "END AS after_image " +

                    "FROM issue_list1 il1 " +

                    "LEFT JOIN asset_list al " +
                    "ON al.id = il1.assetlist_id " +

                    "LEFT JOIN issue_list2 il2 " +
                    "ON il2.issuelist1_id = il1.id " +

                    // ✅ join asset_master
                    "LEFT JOIN asset_master am " +
                    "ON am.id = al.am_id " +

                    "WHERE il2.ward = ? " +
                    "AND il2.final_status = 'completed' " +
                    "And il1.final_status= 'completed' " +

                    "ORDER BY il2.id DESC";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, ward);

            response.put("status", "success");
            response.put("ward", ward);
            response.put("count", result.size());
            response.put("data", result);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getZoneWiseReport(

            String fromDate,
            String toDate) {

        Map<String, Object> response = new HashMap<>();

        try {

            String sql = "SELECT " +

            // ✅ zone + total row
                    "IFNULL(am.zone, 'TOTAL') AS zone, " +

                    // ✅ total tenement
                    "COUNT(DISTINCT am.id) AS total_tenement_count, " +

                    // ✅ marked
                    "COUNT(DISTINCT al.am_id) AS tenement_marked_count, " +

                    // ✅ not marked
                    "(" +
                    "COUNT(DISTINCT am.id) - " +
                    "COUNT(DISTINCT al.am_id)" +
                    ") AS tenement_not_marked_count, " +

                    // ✅ total complaint count
                    "COUNT(DISTINCT il1.id) " +
                    "AS total_complaint_count, " +

                    // ✅ pending
                    "COUNT(DISTINCT CASE " +
                    "WHEN il1.final_status = 'created' " +
                    "THEN il1.id END) " +
                    "AS total_complaint_pending_count, " +

                    // ✅ closed = completed + verified
                    "COUNT(DISTINCT CASE " +
                    "WHEN il1.final_status IN ('completed','verified') " +
                    "THEN il1.id END) " +
                    "AS total_complaint_closed_count, " +

                    // ✅ verified
                    "COUNT(DISTINCT CASE " +
                    "WHEN il1.final_status = 'verified' " +
                    "THEN il1.id END) " +
                    "AS total_complaint_verified_count, " +

                    // ✅ not verified = only completed
                    "COUNT(DISTINCT CASE " +
                    "WHEN il1.final_status = 'completed' " +
                    "THEN il1.id END) " +
                    "AS total_complaint_not_verified_count " +

                    "FROM asset_master am " +

                    "LEFT JOIN asset_list al " +
                    "ON al.am_id = am.id " +

                    "LEFT JOIN issue_list1 il1 " +
                    "ON il1.assetlist_id = al.id " +

                    // ✅ complaint date filter
                    "AND DATE(il1.cdate) BETWEEN ? AND ? " +

                    // ✅ total row
                    "GROUP BY am.zone WITH ROLLUP " +

                    "ORDER BY am.zone IS NULL, am.zone";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(
                    sql,
                    fromDate,
                    toDate);

            response.put("status", "success");
            response.put("fromDate", fromDate);
            response.put("toDate", toDate);
            response.put("data", result);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getZoneWiseReport2(

            String fromDate,
            String toDate) {

        Map<String, Object> response = new HashMap<>();

        try {

            String sql = "SELECT " +

            // ✅ zone + total row
                    "IFNULL(am.zone, 'TOTAL') AS zone, " +

                    // ✅ total tenement
                    "COUNT(DISTINCT am.id) AS total_tenement_count, " +

                    // ✅ marked
                    "COUNT(DISTINCT al.am_id) AS tenement_marked_count, " +

                    // ✅ not marked
                    "(" +
                    "COUNT(DISTINCT am.id) - " +
                    "COUNT(DISTINCT al.am_id)" +
                    ") AS tenement_not_marked_count, " +
                    // ✅ total complaint count
                    "COUNT(DISTINCT il1.id) " +
                    "AS total_complaint_count, " +

                    // ✅ pending
                    "COUNT(DISTINCT CASE " +
                    "WHEN il1.final_status = 'created' " +
                    "THEN il1.id END) " +
                    "AS total_complaint_pending_count, " +

                    // ✅ closed
                    "COUNT(DISTINCT CASE " +
                    "WHEN il1.final_status = 'completed' " +
                    "THEN il1.id END) " +
                    "AS total_complaint_closed_count, " +

                    // ✅ verified
                    "COUNT(DISTINCT CASE " +
                    "WHEN il1.final_status = 'verified' " +
                    "THEN il1.id END) " +
                    "AS total_complaint_verified_count, " +

                    // ✅ not verified
                    "(" +
                    "COUNT(DISTINCT CASE " +
                    "WHEN il1.final_status = 'completed' " +
                    "THEN il1.id END) - " +

                    "COUNT(DISTINCT CASE " +
                    "WHEN il1.final_status = 'verified' " +
                    "THEN il1.id END)" +
                    ") AS total_complaint_not_verified_count " +

                    "FROM asset_master am " +

                    "LEFT JOIN asset_list al " +
                    "ON al.am_id = am.id " +

                    "LEFT JOIN issue_list1 il1 " +
                    "ON il1.assetlist_id = al.id " +

                    // ✅ complaint date filter
                    "AND DATE(il1.cdate) BETWEEN ? AND ? " +

                    // ✅ total row
                    "GROUP BY am.zone WITH ROLLUP " +
                    "ORDER BY am.zone IS NULL, am.zone";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(
                    sql,
                    fromDate,
                    toDate);

            response.put("status", "success");
            response.put("fromDate", fromDate);
            response.put("toDate", toDate);
            response.put("data", result);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getZoneWiseReport1(

            String fromDate,
            String toDate) {

        Map<String, Object> response = new HashMap<>();

        try {

            String sql = "SELECT " +

                    "am.zone, " +

                    // ✅ total tenement
                    "COUNT(DISTINCT am.id) AS total_tenement_count, " +

                    // ✅ marked
                    "COUNT(DISTINCT al.am_id) AS tenement_marked_count, " +

                    // ✅ not marked
                    "(" +
                    "COUNT(DISTINCT am.id) - " +
                    "COUNT(DISTINCT al.am_id)" +
                    ") AS tenement_not_marked_count, " +

                    // ✅ pending
                    "COUNT(DISTINCT CASE " +
                    "WHEN il1.final_status = 'created' " +
                    "THEN il1.id END) " +
                    "AS total_complaint_pending_count, " +

                    // ✅ closed
                    "COUNT(DISTINCT CASE " +
                    "WHEN il1.final_status = 'completed' " +
                    "THEN il1.id END) " +
                    "AS total_complaint_closed_count, " +

                    // ✅ verified
                    "COUNT(DISTINCT CASE " +
                    "WHEN il1.final_status = 'verified' " +
                    "THEN il1.id END) " +
                    "AS total_complaint_verified_count, " +

                    // ✅ total complaint count
                    "COUNT(DISTINCT il1.id) " +
                    "AS total_complaint_count, " +

                    // ✅ not verified = completed - verified
                    "(" +
                    "COUNT(DISTINCT CASE " +
                    "WHEN il1.final_status = 'completed' " +
                    "THEN il1.id END) - " +

                    "COUNT(DISTINCT CASE " +
                    "WHEN il1.final_status = 'verified' " +
                    "THEN il1.id END)" +
                    ") AS total_complaint_not_verified_count " +

                    "FROM asset_master am " +

                    "LEFT JOIN asset_list al " +
                    "ON al.am_id = am.id " +

                    "LEFT JOIN issue_list1 il1 " +
                    "ON il1.assetlist_id = al.id " +

                    // ✅ date filter only for complaints
                    "AND DATE(il1.cdate) BETWEEN ? AND ? " +

                    "GROUP BY am.zone " +

                    "ORDER BY am.zone";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(
                    sql,
                    fromDate,
                    toDate);

            response.put("status", "success");
            response.put("fromDate", fromDate);
            response.put("toDate", toDate);
            response.put("data", result);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getWardWiseReport(

            String zone,
            String fromDate,
            String toDate) {

        Map<String, Object> response = new HashMap<>();

        try {

            String sql = "SELECT " +

                    "am.ward, " +
                    "am.zone, " +

                    "COUNT(DISTINCT am.id) AS total_tenement_count, " +

                    "COUNT(DISTINCT al.am_id) AS tenement_marked_count, " +

                    "(" +
                    "COUNT(DISTINCT am.id) - " +
                    "COUNT(DISTINCT al.am_id)" +
                    ") AS tenement_not_marked_count, " +

                    // ✅ pending
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND il.final_status = 'created' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_pending_count, " +

                    // ✅ closed = completed + verified
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND il.final_status IN ('completed','verified') " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_closed_count, " +

                    // ✅ verified
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND il.final_status = 'verified' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_verified_count, " +

                    // ✅ not verified = only completed
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND il.final_status = 'completed' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_not_verified_count, " +

                    // ✅ total complaints
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_count " +

                    "FROM asset_master am " +

                    "LEFT JOIN asset_list al " +
                    "ON al.am_id = am.id " +

                    "WHERE am.zone = ? " +

                    "GROUP BY am.zone, am.ward " +

                    "ORDER BY am.ward";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(
                    sql,

                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,

                    zone);

            // ✅ total row
            int totalTenement = 0;
            int totalMarked = 0;
            int totalNotMarked = 0;
            int totalPending = 0;
            int totalClosed = 0;
            int totalVerified = 0;
            int totalNotVerified = 0;
            int totalComplaintCount = 0;

            for (Map<String, Object> row : result) {

                totalTenement += Integer.parseInt(
                        row.get("total_tenement_count").toString());

                totalMarked += Integer.parseInt(
                        row.get("tenement_marked_count").toString());

                totalNotMarked += Integer.parseInt(
                        row.get("tenement_not_marked_count").toString());

                totalPending += Integer.parseInt(
                        row.get("total_complaint_pending_count").toString());

                totalClosed += Integer.parseInt(
                        row.get("total_complaint_closed_count").toString());

                totalVerified += Integer.parseInt(
                        row.get("total_complaint_verified_count").toString());

                totalNotVerified += Integer.parseInt(
                        row.get("total_complaint_not_verified_count").toString());

                totalComplaintCount += Integer.parseInt(
                        row.get("total_complaint_count").toString());
            }

            Map<String, Object> totalRow = new HashMap<>();

            totalRow.put("ward", "TOTAL");
            totalRow.put("zone", zone);

            totalRow.put("total_tenement_count", totalTenement);
            totalRow.put("tenement_marked_count", totalMarked);
            totalRow.put("tenement_not_marked_count", totalNotMarked);

            totalRow.put("total_complaint_pending_count", totalPending);
            totalRow.put("total_complaint_closed_count", totalClosed);
            totalRow.put("total_complaint_verified_count", totalVerified);
            totalRow.put("total_complaint_not_verified_count", totalNotVerified);
            totalRow.put("total_complaint_count", totalComplaintCount);

            result.add(totalRow);

            response.put("status", "success");
            response.put("zone", zone);
            response.put("fromDate", fromDate);
            response.put("toDate", toDate);
            response.put("data", result);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getWardWiseReport2(

            String zone,
            String fromDate,
            String toDate) {

        Map<String, Object> response = new HashMap<>();

        try {

            String sql = "SELECT " +

                    "am.ward, " +
                    "am.zone, " +

                    "COUNT(DISTINCT am.id) AS total_tenement_count, " +

                    "COUNT(DISTINCT al.am_id) AS tenement_marked_count, " +

                    "(" +
                    "COUNT(DISTINCT am.id) - " +
                    "COUNT(DISTINCT al.am_id)" +
                    ") AS tenement_not_marked_count, " +

                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND il.final_status = 'created' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_pending_count, " +

                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND il.final_status = 'completed' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_closed_count, " +

                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND il.final_status = 'verified' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_verified_count, " +

                    "(" +

                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND il.final_status = 'completed' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") - " +

                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND il.final_status = 'verified' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ")" +

                    ") AS total_complaint_not_verified_count, " +

                    // ✅ total complaints
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_count " +

                    "FROM asset_master am " +

                    "LEFT JOIN asset_list al " +
                    "ON al.am_id = am.id " +

                    "WHERE am.zone = ? " +

                    "GROUP BY am.zone, am.ward " +

                    "ORDER BY am.ward";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(
                    sql,

                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,

                    zone);

            // ✅ total row
            int totalTenement = 0;
            int totalMarked = 0;
            int totalNotMarked = 0;
            int totalPending = 0;
            int totalClosed = 0;
            int totalVerified = 0;
            int totalNotVerified = 0;

            for (Map<String, Object> row : result) {

                totalTenement += Integer.parseInt(row.get("total_tenement_count").toString());

                totalMarked += Integer.parseInt(row.get("tenement_marked_count").toString());

                totalNotMarked += Integer.parseInt(row.get("tenement_not_marked_count").toString());

                totalPending += Integer.parseInt(row.get("total_complaint_pending_count").toString());

                totalClosed += Integer.parseInt(row.get("total_complaint_closed_count").toString());

                totalVerified += Integer.parseInt(row.get("total_complaint_verified_count").toString());

                totalNotVerified += Integer.parseInt(row.get("total_complaint_not_verified_count").toString());
            }

            Map<String, Object> totalRow = new HashMap<>();

            totalRow.put("ward", "TOTAL");
            totalRow.put("zone", zone);

            totalRow.put("total_tenement_count", totalTenement);
            totalRow.put("tenement_marked_count", totalMarked);
            totalRow.put("tenement_not_marked_count", totalNotMarked);

            totalRow.put("total_complaint_pending_count", totalPending);
            totalRow.put("total_complaint_closed_count", totalClosed);
            totalRow.put("total_complaint_verified_count", totalVerified);
            totalRow.put("total_complaint_not_verified_count", totalNotVerified);

            result.add(totalRow);

            response.put("status", "success");
            response.put("zone", zone);
            response.put("fromDate", fromDate);
            response.put("toDate", toDate);
            response.put("data", result);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getWardWiseReport1(

            String zone,
            String fromDate,
            String toDate) {

        Map<String, Object> response = new HashMap<>();

        try {

            String sql = "SELECT " +

                    "am.zone, " +
                    "am.ward, " +

                    // ✅ total tenement
                    "COUNT(DISTINCT am.id) AS total_tenement_count, " +

                    // ✅ marked
                    "COUNT(DISTINCT al.am_id) AS tenement_marked_count, " +

                    // ✅ not marked
                    "(" +
                    "COUNT(DISTINCT am.id) - " +
                    "COUNT(DISTINCT al.am_id)" +
                    ") AS tenement_not_marked_count, " +

                    // ✅ pending
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND il.final_status = 'created' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_pending_count, " +

                    // ✅ completed
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND il.final_status = 'completed' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_closed_count, " +

                    // ✅ verified
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND il.final_status = 'verified' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_verified_count, " +

                    // ✅ total complaint count
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_count" +

                    // ✅ not verified
                    "( " +

                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND il.final_status = 'completed' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") - " +

                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "WHERE il.zone = am.zone " +
                    "AND il.ward = am.ward " +
                    "AND il.final_status = 'verified' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ")" +

                    ") AS total_complaint_not_verified_count " +

                    "FROM asset_master am " +

                    "LEFT JOIN asset_list al " +
                    "ON al.am_id = am.id " +

                    "WHERE am.zone = ? " +

                    "GROUP BY am.zone, am.ward " +

                    "ORDER BY am.ward";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(
                    sql,

                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,

                    zone);

            response.put("status", "success");
            response.put("zone", zone);
            response.put("fromDate", fromDate);
            response.put("toDate", toDate);
            response.put("data", result);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getSchemeWiseReport(

            String ward,
            String fromDate,
            String toDate) {

        Map<String, Object> response = new HashMap<>();

        try {

            String sql = "SELECT " +

                    "am.zone, " +
                    "am.ward, " +
                    "am.scheme_name, " +
                    "am.id AS schemeid, " +

                    // ✅ total tenement
                    "COUNT(DISTINCT am.id) AS total_tenement_count, " +

                    // ✅ marked
                    "COUNT(DISTINCT al.am_id) AS tenement_marked_count, " +

                    // ✅ not marked
                    "(" +
                    "COUNT(DISTINCT am.id) - " +
                    "COUNT(DISTINCT al.am_id)" +
                    ") AS tenement_not_marked_count, " +

                    // ✅ pending
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "LEFT JOIN asset_list a " +
                    "ON a.id = il.assetlist_id " +
                    "WHERE a.am_id = am.id " +
                    "AND il.final_status = 'created' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_pending_count, " +

                    // ✅ completed
                    // ✅ closed = completed + verified
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "LEFT JOIN asset_list a " +
                    "ON a.id = il.assetlist_id " +
                    "WHERE a.am_id = am.id " +
                    "AND il.final_status IN ('completed','verified') " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_closed_count, " +

                    // ✅ verified
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "LEFT JOIN asset_list a " +
                    "ON a.id = il.assetlist_id " +
                    "WHERE a.am_id = am.id " +
                    "AND il.final_status = 'verified' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_verified_count, " +
                    // ✅ total complaint count
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "LEFT JOIN asset_list a " +
                    "ON a.id = il.assetlist_id " +
                    "WHERE a.am_id = am.id " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_count, " +

                    // ✅ not verified
                    // ✅ not verified = only completed
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "LEFT JOIN asset_list a " +
                    "ON a.id = il.assetlist_id " +
                    "WHERE a.am_id = am.id " +
                    "AND il.final_status = 'completed' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_not_verified_count " +

                    "FROM asset_master am " +

                    "LEFT JOIN asset_list al " +
                    "ON al.am_id = am.id " +

                    "WHERE am.ward = ? " +

                    "GROUP BY am.id, am.zone, am.ward, am.scheme_name " +

                    "ORDER BY am.ward";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(
                    sql,

                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,

                    ward);

            // ✅ total row
            Map<String, Object> totalRow = new HashMap<>();
            int totalComplaintCount = 0;

            int totalTenement = 0;
            int totalMarked = 0;
            int totalNotMarked = 0;
            int totalPending = 0;
            int totalClosed = 0;
            int totalVerified = 0;
            int totalNotVerified = 0;

            for (Map<String, Object> row : result) {
                totalComplaintCount += Integer.parseInt(
                        row.get("total_complaint_count").toString());

                totalTenement += Integer.parseInt(row.get("total_tenement_count").toString());

                totalMarked += Integer.parseInt(row.get("tenement_marked_count").toString());

                totalNotMarked += Integer.parseInt(row.get("tenement_not_marked_count").toString());

                totalPending += Integer.parseInt(row.get("total_complaint_pending_count").toString());

                totalClosed += Integer.parseInt(row.get("total_complaint_closed_count").toString());

                totalVerified += Integer.parseInt(row.get("total_complaint_verified_count").toString());

                totalNotVerified += Integer.parseInt(row.get("total_complaint_not_verified_count").toString());
            }

            totalRow.put("zone", "");
            totalRow.put("ward", "");
            totalRow.put("scheme_name", "TOTAL");
            totalRow.put("schemeid", "");
            totalRow.put("total_complaint_count", totalComplaintCount);
            totalRow.put("total_tenement_count", totalTenement);
            totalRow.put("tenement_marked_count", totalMarked);
            totalRow.put("tenement_not_marked_count", totalNotMarked);
            totalRow.put("total_complaint_pending_count", totalPending);
            totalRow.put("total_complaint_closed_count", totalClosed);
            totalRow.put("total_complaint_verified_count", totalVerified);
            totalRow.put("total_complaint_not_verified_count", totalNotVerified);

            result.add(totalRow);

            response.put("status", "success");
            response.put("ward", ward);
            response.put("fromDate", fromDate);
            response.put("toDate", toDate);
            response.put("data", result);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getSchemeWiseReport1(

            String zone,
            String fromDate,
            String toDate) {

        Map<String, Object> response = new HashMap<>();

        try {

            String sql = "SELECT " +

                    "am.zone, " +
                    "am.ward, " +
                    "am.scheme_name, " +
                    "am.id AS schemeid, " +

                    // ✅ total tenement
                    "COUNT(DISTINCT am.id) AS total_tenement_count, " +

                    // ✅ marked
                    "COUNT(DISTINCT al.am_id) AS tenement_marked_count, " +

                    // ✅ not marked
                    "(" +
                    "COUNT(DISTINCT am.id) - " +
                    "COUNT(DISTINCT al.am_id)" +
                    ") AS tenement_not_marked_count, " +

                    // ✅ pending
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "LEFT JOIN asset_list a " +
                    "ON a.id = il.assetlist_id " +
                    "WHERE a.am_id = am.id " +
                    "AND il.final_status = 'created' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_pending_count, " +

                    // ✅ completed
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "LEFT JOIN asset_list a " +
                    "ON a.id = il.assetlist_id " +
                    "WHERE a.am_id = am.id " +
                    "AND il.final_status = 'completed' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_closed_count, " +

                    // ✅ verified
                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "LEFT JOIN asset_list a " +
                    "ON a.id = il.assetlist_id " +
                    "WHERE a.am_id = am.id " +
                    "AND il.final_status = 'verified' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") AS total_complaint_verified_count, " +

                    // ✅ not verified
                    "(" +

                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "LEFT JOIN asset_list a " +
                    "ON a.id = il.assetlist_id " +
                    "WHERE a.am_id = am.id " +
                    "AND il.final_status = 'completed' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ") - " +

                    "(" +
                    "SELECT COUNT(*) " +
                    "FROM issue_list1 il " +
                    "LEFT JOIN asset_list a " +
                    "ON a.id = il.assetlist_id " +
                    "WHERE a.am_id = am.id " +
                    "AND il.final_status = 'verified' " +
                    "AND DATE(il.cdate) BETWEEN ? AND ? " +
                    ")" +

                    ") AS total_complaint_not_verified_count " +

                    "FROM asset_master am " +

                    "LEFT JOIN asset_list al " +
                    "ON al.am_id = am.id " +

                    "WHERE am.zone = ? " +

                    "GROUP BY am.id, am.zone, am.ward, am.scheme_name " +

                    "ORDER BY am.ward";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(
                    sql,

                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,
                    fromDate, toDate,

                    zone);

            response.put("status", "success");
            response.put("zone", zone);
            response.put("fromDate", fromDate);
            response.put("toDate", toDate);
            response.put("data", result);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getComplaintDetails(

            String ward,
            String schemeid,
            String status,
            String fromDate,
            String toDate) {

        Map<String, Object> response = new HashMap<>();

        try {

            String sql = "SELECT " +

            // ✅ issue_list1
                    "il1.id AS issuelist1_id, " +
                    "il1.zone, " +
                    "il1.ward, " +
                    "il1.latitide AS created_latitude, " +
                    "il1.longitude AS created_longitude, " +
                    "il1.final_status, " +
                    "IFNULL(DATE_FORMAT(il1.cdate, '%d-%m-%Y %h:%i %p'), '') AS created_date, " +
                    "il1.remarks AS created_remarks, " +

                    "CASE " +
                    "WHEN il1.before_image IS NOT NULL " +
                    "THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', il1.before_image) " +
                    "ELSE '' " +
                    "END AS before_image, " +

                    // ✅ issue_list2
                    "IFNULL(il2.id, '') AS issuelist2_id, " +
                    "IFNULL(il2.latitide, '') AS completed_latitude, " +
                    "IFNULL(il2.longitude, '') AS completed_longitude, " +
                    "IFNULL(DATE_FORMAT(il2.cdate, '%d-%m-%Y %h:%i %p'), '') AS completed_date, " +
                    "IFNULL(il2.remarks, '') AS completed_remarks, " +

                    "CASE " +
                    "WHEN il2.after_image IS NOT NULL " +
                    "THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', il2.after_image) " +
                    "ELSE '' " +
                    "END AS after_image, " +

                    // ✅ issue_list3
                    "IFNULL(il3.id, '') AS issuelist3_id, " +
                    "IFNULL(il3.latitide, '') AS verified_latitude, " +
                    "IFNULL(il3.longitude, '') AS verified_longitude, " +
                    "IFNULL(DATE_FORMAT(il3.cdate, '%d-%m-%Y %h:%i %p'), '') AS verified_date, " +
                    "IFNULL(il3.remarks, '') AS verified_remarks, " +

                    "CASE " +
                    "WHEN il3.Verify_image IS NOT NULL " +
                    "THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', il3.Verify_image) " +
                    "ELSE '' " +
                    "END AS verify_image " +

                    "FROM issue_list1 il1 " +

                    // ✅ asset join for scheme
                    "LEFT JOIN asset_list al " +
                    "ON al.id = il1.assetlist_id " +

                    "LEFT JOIN issue_list2 il2 " +
                    "ON il2.issuelist1_id = il1.id " +

                    "LEFT JOIN issue_list3 il3 " +
                    "ON il3.issuelist1_id = il1.id " +

                    "WHERE il1.ward = ? " +
                    "AND al.am_id = ? " +
                    "AND il1.final_status = ? " +
                    "AND DATE(il1.cdate) BETWEEN ? AND ? " +

                    "ORDER BY il1.id DESC";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(
                    sql,
                    ward,
                    schemeid,
                    status,
                    fromDate,
                    toDate);

            response.put("status", "success");
            response.put("ward", ward);
            response.put("schemeid", schemeid);
            response.put("filter_status", status);
            response.put("fromDate", fromDate);
            response.put("toDate", toDate);
            response.put("count", result.size());
            response.put("data", result);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getComplaintDetails1(

            String ward,
            String status,
            String fromDate,
            String toDate) {

        Map<String, Object> response = new HashMap<>();

        try {

            String sql = "SELECT " +

            // ✅ issue_list1
                    "il1.id AS issuelist1_id, " +
                    "il1.zone, " +
                    "il1.ward, " +
                    "il1.latitide AS created_latitude, " +
                    "il1.longitude AS created_longitude, " +
                    "il1.final_status, " +
                    "il1.cdate AS created_date, " +
                    "il1.remarks AS created_remarks, " +

                    "CASE " +
                    "WHEN il1.before_image IS NOT NULL " +
                    "THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', il1.before_image) " +
                    "ELSE '' " +
                    "END AS before_image, " +

                    // ✅ issue_list2
                    "IFNULL(il2.id, '') AS issuelist2_id, " +
                    "IFNULL(il2.latitide, '') AS completed_latitude, " +
                    "IFNULL(il2.longitude, '') AS completed_longitude, " +
                    "IFNULL(il2.cdate, '') AS completed_date, " +
                    "IFNULL(il2.remarks, '') AS completed_remarks, " +

                    "CASE " +
                    "WHEN il2.after_image IS NOT NULL " +
                    "THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', il2.after_image) " +
                    "ELSE '' " +
                    "END AS after_image, " +

                    // ✅ issue_list3
                    "IFNULL(il3.id, '') AS issuelist3_id, " +
                    "IFNULL(il3.latitide, '') AS verified_latitude, " +
                    "IFNULL(il3.longitude, '') AS verified_longitude, " +
                    "IFNULL(il3.cdate, '') AS verified_date, " +
                    "IFNULL(il3.remarks, '') AS verified_remarks, " +

                    "CASE " +
                    "WHEN il3.Verify_image IS NOT NULL " +
                    "THEN CONCAT('" + fileBaseUrl + "/gccofficialapp/files', il3.Verify_image) " +
                    "ELSE '' " +
                    "END AS verify_image " +

                    "FROM issue_list1 il1 " +

                    "LEFT JOIN issue_list2 il2 " +
                    "ON il2.issuelist1_id = il1.id " +

                    "LEFT JOIN issue_list3 il3 " +
                    "ON il3.issuelist1_id = il1.id " +

                    "WHERE il1.ward = ? " +
                    "AND il1.final_status = ? " +
                    "AND DATE(il1.cdate) BETWEEN ? AND ? " +

                    "ORDER BY il1.id DESC";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(
                    sql,
                    ward,
                    status,
                    fromDate,
                    toDate);

            response.put("status", "success");
            response.put("ward", ward);
            response.put("filter_status", status);
            response.put("fromDate", fromDate);
            response.put("toDate", toDate);
            response.put("count", result.size());
            response.put("data", result);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }
}

/*
 * public Map<String, Object> getTenementsListByWard(String loginid, String
 * type) {
 * 
 * Map<String, Object> response = new HashMap<>();
 * 
 * String sqlQuery =
 * "SELECT ward FROM gcc_penalty_hoardings.hoading_user_list WHERE userid = ?  AND isactive=1 LIMIT 1"
 * ;
 * 
 * List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery,
 * loginid);
 * 
 * String ward = results.get(0).get("ward").toString();
 * 
 * String sqlQuery2 =
 * "SELECT id as assetmasterid ,No_of_tenements,scheme_name,area FROM asset_master WHERE ward = ?"
 * ;
 * 
 * List<Map<String, Object>> results2 = jdbcTemplate.queryForList(sqlQuery2,
 * ward);
 * 
 * response.put("ward", ward);
 * response.put("tenements", results2);
 * 
 * return response;
 * }
 */
