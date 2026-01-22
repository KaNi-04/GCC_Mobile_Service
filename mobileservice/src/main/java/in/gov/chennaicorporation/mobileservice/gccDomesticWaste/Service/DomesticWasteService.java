package in.gov.chennaicorporation.mobileservice.gccDomesticWaste.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;

@Service
public class DomesticWasteService {

    @Autowired

    private JdbcTemplate jdbcDomesticWasteTemplate;

    @Value("${file.upload.directory}")
    private String baseUploadDir;

    @Value("${domestic_waste_foldername}")
    private String wasteFolder;


    private String fileBaseUrl;

    @Autowired
    private Environment environment;

    @Autowired
    public void setDataSource(@Qualifier("mysqlGccDomesticWasteManagementDataSource") DataSource DomesticWasteManagementDataSource) {
        this.jdbcDomesticWasteTemplate = new JdbcTemplate(DomesticWasteManagementDataSource);
    }



    public DomesticWasteService(Environment environment) {
        this.environment = environment;
        this.fileBaseUrl = environment.getProperty("fileBaseUrl");
    }


    public String fileUpload(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String year = String.valueOf(LocalDate.now().getYear());
            String month = String.format("%02d", LocalDate.now().getMonthValue());
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyyHHmmss"));

            // Clean original file name
            String originalFilename = file.getOriginalFilename().replaceAll("\\s+", "");
            String fileName = prefix + "_" + timestamp + "_" + originalFilename;

            // Create upload directory path
            String uploadDirPath = baseUploadDir + wasteFolder + year + "/" + month;
            File uploadDir = new File(uploadDirPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Save file to server
            File fullFilePath = new File(uploadDirPath + File.separator + fileName);
            file.transferTo(fullFilePath);

            // Return relative path to save in DB
            return "/" + wasteFolder + year + "/" + month + "/" + fileName;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, Object> saveMultipleWasteRequests(String user_name, String mobile, String address,
                                                         String latitude, String longitude, String zone, String division,
                                                         String street_name, String street_id, String remarks,
                                                         MultipartFile imageFile, String itemsJson, String sofa_type, String is_app) {
    	
    	final String is_app_value = (is_app == null || is_app.trim().isEmpty()) ? "1" : is_app;
    	
        String imagePath = fileUpload(imageFile, "domestic_waste_photo");
        if (imagePath == null) {
            return Map.of("status", false, "message", "Image upload failed");
        }

        try {
            // Step 1: Insert main request (one time)
            String insertMainSql = "INSERT INTO user_request_table " +
                    "(request_id, user_name, mobile, address, latitude, longitude, zone, division, street_name, street_id, remarks, image_url, sofa_type, is_app) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcDomesticWasteTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(insertMainSql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, "TEMP"); // placeholder
                ps.setString(2, user_name);
                ps.setString(3, mobile);
                ps.setString(4, address);
                ps.setString(5, latitude);
                ps.setString(6, longitude);
                ps.setString(7, zone);
                ps.setString(8, division);
                ps.setString(9, street_name);
                ps.setString(10, street_id);
                ps.setString(11, remarks);
                ps.setString(12, imagePath);
                ps.setString(13, sofa_type);
                ps.setString(14, is_app_value);
                return ps;
            }, keyHolder);

            int generatedId = keyHolder.getKey().intValue();
            String requestId = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + generatedId;

            // Update request_id in user_request_table
            jdbcDomesticWasteTemplate.update("UPDATE user_request_table SET request_id = ? WHERE id = ?", requestId, generatedId);

            // Step 2: Insert items under the same request_id into the `items` table
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> items = objectMapper.readValue(itemsJson, new TypeReference<>() {});

            String insertItemSql = "INSERT INTO items (iteam_id, quantity, req_table_id, requedt_id) VALUES (?, ?, ?, ?)";

            for (Map<String, Object> item : items) {
                Object itemIdObj = item.get("item_id");
                Object quantityObj = item.get("quantity");

                if (itemIdObj == null || quantityObj == null) {
                    continue;
                }

                int itemId = Integer.parseInt(itemIdObj.toString());
                int quantity = Integer.parseInt(quantityObj.toString());

                jdbcDomesticWasteTemplate.update(insertItemSql, itemId, quantity, generatedId, requestId);
            }

            return Map.of(
                    "status", true,
                    "message", "Request saved successfully",
                    "requestId", requestId
            );

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("status", false, "message", "Error: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getAllWasteRequests() {
        /*String sql = "SELECT " +
                "u.id, u.request_id, u.user_name, u.mobile, u.address, u.latitude, u.longitude, " +
                "u.zone, u.division, u.street_name, u.street_id, u.quantity, u.remarks, " +
                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.image_url) AS image_url, " +
                "itm.quantity, i.item_name " +
                "FROM user_request_table u " +
                "LEFT JOIN items itm ON u.id = itm.req_table_id " +
                "LEFT JOIN item_master i ON itm.iteam_id = i.id AND i.is_active = 1";
        */
        String sql = "SELECT " +
                "u.*, " +
                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.image_url) AS image_full_url, " +
                "itm.quantity, i.item_name " +
                "FROM user_request_table u " +
                "LEFT JOIN items itm ON u.id = itm.req_table_id " +
                "LEFT JOIN item_master i ON itm.iteam_id = i.id AND i.is_active = 1";

        List<Map<String, Object>> rawData = jdbcDomesticWasteTemplate.queryForList(sql);
        return groupRequestsWithItems(rawData);
    }

    public String getWardByLoginId(String loginid) {
	    String sqlQuery = "SELECT `ward` FROM gcc_penalty_hoardings.`hoading_user_list` WHERE `userid` = ? LIMIT 1";
	    
	    // Query the database using queryForList
	    List<Map<String, Object>> results = jdbcDomesticWasteTemplate.queryForList(sqlQuery, loginid);
	    
	    // Check if results is not empty and extract the ward value
	    if (!results.isEmpty()) {
	        // Extract the ward value from the first result
	        return (String) results.get(0).get("ward");
	    }
	    
	    // Handle the case where no result is found
	    return "000";  // or return null based on your needs
	}
    
    public List<Map<String, Object>> getAllWasteRequestsbylogin(String loginId) {
    	/*
    	String zoneSql = "SELECT zone, division FROM officer_master WHERE loginId = ? AND is_active = 1 AND is_delete = 0";
        List<Map<String, Object>> zoneList = jdbcDomesticWasteTemplate.queryForList(zoneSql, loginId);

        //if (zoneList.isEmpty()) return List.of();
        if (zoneList.isEmpty()) {
        	Map<String, Object> response = new HashMap<>();
            response.put("status", false);
            response.put("message", "No data found");
            return Collections.singletonList(response); // or just `response` depending on your return type
        }

        List<Object> params = new ArrayList<>();
        StringBuilder zoneCondition = new StringBuilder("(");

        for (int i = 0; i < zoneList.size(); i++) {
            Map<String, Object> row = zoneList.get(i);
            zoneCondition.append("(u.zone = ? AND u.division = ?)");
            if (i < zoneList.size() - 1) zoneCondition.append(" OR ");
            params.add(row.get("zone"));
            params.add(row.get("division"));
        }
        */
    	List<Object> params = new ArrayList<>();
        StringBuilder zoneCondition = new StringBuilder("(");
        zoneCondition.append("(u.division = ?)");
        params.add(getWardByLoginId(loginId));
        zoneCondition.append(") ORDER BY u.cdate DESC");
        /*
        String sql = "SELECT " +
                "u.*, " +
                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.image_url) AS image_full_url, " +
                "itm.quantity, i.item_name " +
                "FROM user_request_table u " +
                "LEFT JOIN items itm ON u.id = itm.req_table_id " +
                "LEFT JOIN item_master i ON itm.iteam_id = i.id AND i.is_active = 1";
        */
        String sql = "SELECT " +
                "u.id, u.request_id, u.user_name, u.mobile, u.address, u.latitude, u.longitude, " +
                "u.zone, u.division, u.street_name, u.quantity AS main_quantity, u.remarks, " +
                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.image_url) AS image_url, " +
                "DATE_FORMAT(u.cdate, '%d-%m-%Y') AS cdate, " +
                "u.is_active, u.is_delete, u.street_id, sm.status_master AS status, u.is_app, " +
                "DATE_FORMAT(u.close_date, '%d-%m-%Y') AS close_date, " +
                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.officer_upload_url) AS officer_upload_url, " +
                "itm.quantity, im.item_name " +
                "FROM user_request_table u " +
                "LEFT JOIN items itm ON u.id = itm.req_table_id " +
                "LEFT JOIN item_master im ON itm.iteam_id = im.id AND im.is_active = 1 " +
                "LEFT JOIN status_master sm ON sm.id = u.status AND sm.is_active = 1 AND sm.is_delete = 0 " +
                "WHERE u.status IN(1,3) " +
                "AND u.is_active = 1 AND " + zoneCondition;

        System.err.println(sql);
        System.err.println(Arrays.toString(params.toArray()));
        System.err.println(loginId);
        List<Map<String, Object>> rawData = jdbcDomesticWasteTemplate.queryForList(sql, params.toArray());
        return groupRequestsWithItems(rawData);
    }

    public List<Map<String, Object>> getAllmasters() {
        String sql = "select id,item_name from item_master where is_active= 1";
        return jdbcDomesticWasteTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getuserbymobile(String mobile) {
        String sql = "SELECT " +
                "u.id, u.request_id, u.user_name, u.mobile, u.address, u.latitude, u.longitude, " +
                "u.zone, u.division, u.street_name, u.quantity AS main_quantity, u.remarks, " +
                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.image_url) AS image_url, " +
                "DATE_FORMAT(u.cdate, '%d-%m-%Y') AS cdate, " +
                "u.is_active, u.is_delete, u.street_id, sm.status_master AS status, u.is_app, " +
                "DATE_FORMAT(u.close_date, '%d-%m-%Y') AS close_date, " +
                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.officer_upload_url) AS officer_upload_url, " +
                "itm.iteam_id, itm.quantity, im.item_name " +
                "FROM user_request_table u " +
                "LEFT JOIN items itm ON u.id = itm.req_table_id " +
                "LEFT JOIN item_master im ON itm.iteam_id = im.id AND im.is_active = 1 " +
                "LEFT JOIN status_master sm ON sm.id = u.status AND sm.is_active = 1 AND sm.is_delete = 0 " +
                "WHERE u.mobile = ? AND u.is_active = 1";

        List<Map<String, Object>> rawList = jdbcDomesticWasteTemplate.queryForList(sql, mobile);

        Map<Integer, Map<String, Object>> groupedMap = new LinkedHashMap<>();

        for (Map<String, Object> row : rawList) {
            Integer id = (Integer) row.get("id");

            Map<String, Object> item = null;
            if (row.get("item_name") != null) {
                item = new LinkedHashMap<>();
                item.put("item_name", row.get("item_name"));
                item.put("quantity", row.get("quantity"));
            }

            if (groupedMap.containsKey(id)) {
                if (item != null) {
                    ((List<Map<String, Object>>) groupedMap.get(id).get("items")).add(item);
                }
            } else {
                Map<String, Object> newEntry = new LinkedHashMap<>();
                newEntry.put("id", id);
                newEntry.put("request_id", row.get("request_id"));
                newEntry.put("user_name", row.get("user_name"));
                newEntry.put("mobile", row.get("mobile"));
                newEntry.put("address", row.get("address"));
                newEntry.put("latitude", row.get("latitude"));
                newEntry.put("longitude", row.get("longitude"));
                newEntry.put("zone", row.get("zone"));
                newEntry.put("division", row.get("division"));
                newEntry.put("street_name", row.get("street_name"));
                newEntry.put("main_quantity", row.get("main_quantity"));
                newEntry.put("remarks", row.get("remarks"));
                newEntry.put("image_url", row.get("image_url"));
                newEntry.put("cdate", row.get("cdate"));
                newEntry.put("is_active", row.get("is_active"));
                newEntry.put("is_delete", row.get("is_delete"));
                newEntry.put("street_id", row.get("street_id"));
                newEntry.put("status", row.get("status")); // Now this is human-readable
                newEntry.put("is_app", row.get("is_app"));
                newEntry.put("close_date", row.get("close_date"));
                newEntry.put("officer_upload_url", row.get("officer_upload_url"));

                List<Map<String, Object>> items = new ArrayList<>();
                if (item != null) {
                    items.add(item);
                }
                newEntry.put("items", items);

                groupedMap.put(id, newEntry);
            }
        }

        return new ArrayList<>(groupedMap.values());
    }

    public List<Map<String, Object>> getuserAE2(String loginId) {
      /*
    	String zoneSql = "SELECT zone, division FROM officer_master WHERE loginId = ? AND is_active = 1 AND is_delete = 0";
        List<Map<String, Object>> zoneList = jdbcDomesticWasteTemplate.queryForList(zoneSql, loginId);

        //if (zoneList.isEmpty()) return List.of();
        if (zoneList.isEmpty()) {
        	Map<String, Object> response = new HashMap<>();
            response.put("status", false);
            response.put("message", "No data found");
            return Collections.singletonList(response); // or just `response` depending on your return type
        }

        List<Object> params = new ArrayList<>();
        StringBuilder zoneCondition = new StringBuilder("(");

        for (int i = 0; i < zoneList.size(); i++) {
            Map<String, Object> row = zoneList.get(i);
            zoneCondition.append("(u.zone = ? AND u.division = ?)");
            if (i < zoneList.size() - 1) zoneCondition.append(" OR ");
            params.add(row.get("zone"));
            params.add(row.get("division"));
        }
        zoneCondition.append(")");
*/
        List<Object> params = new ArrayList<>();
        StringBuilder zoneCondition = new StringBuilder("(");
        zoneCondition.append("(u.division = ?)");
        params.add(getWardByLoginId(loginId));
        zoneCondition.append(") ORDER BY u.cdate DESC");
        
        String sql = "SELECT " +
                "u.id, u.request_id, u.user_name, u.mobile, u.address, u.latitude, u.longitude, " +
                "u.zone, u.division, u.street_name, u.quantity AS main_quantity, u.remarks, " +
                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.image_url) AS image_url, " +
                "DATE_FORMAT(u.cdate, '%d-%m-%Y') AS cdate, " +
                "u.is_active, u.is_delete, u.street_id, sm.status_master AS status, u.is_app, " +
                "DATE_FORMAT(u.close_date, '%d-%m-%Y') AS close_date, " +
                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.officer_upload_url) AS officer_upload_url, " +
                "itm.quantity, im.item_name " +
                "FROM user_request_table u " +
                "LEFT JOIN items itm ON u.id = itm.req_table_id " +
                "LEFT JOIN item_master im ON itm.iteam_id = im.id AND im.is_active = 1 " +
                "LEFT JOIN status_master sm ON sm.id = u.status AND sm.is_active = 1 AND sm.is_delete = 0 " +
                "WHERE u.status != '2' " +
                "AND u.cdate <= ( CURDATE() - INTERVAL ((WEEKDAY(CURDATE()) + 5) % 7) DAY ) " +
                "AND u.is_active = 1 AND " + zoneCondition;

        List<Map<String, Object>> rawData = jdbcDomesticWasteTemplate.queryForList(sql, params.toArray());
        return groupRequestsWithItems(rawData);
    }


    public List<Map<String, Object>> getuserZonal(String loginId) {
       /*
    	String zoneSql = "SELECT zone, division FROM officer_master WHERE loginId = ? AND is_active = 1 AND is_delete = 0";
        List<Map<String, Object>> zoneList = jdbcDomesticWasteTemplate.queryForList(zoneSql, loginId);

       //if (zoneList.isEmpty()) return List.of();
        if (zoneList.isEmpty()) {
        	Map<String, Object> response = new HashMap<>();
            response.put("status", false);
            response.put("message", "No data found");
            return Collections.singletonList(response); // or just `response` depending on your return type
        }

        List<Object> params = new ArrayList<>();
        StringBuilder zoneCondition = new StringBuilder("(");

        for (int i = 0; i < zoneList.size(); i++) {
            Map<String, Object> row = zoneList.get(i);
            zoneCondition.append("(u.zone = ? AND u.division = ?)");
            if (i < zoneList.size() - 1) zoneCondition.append(" OR ");
            params.add(row.get("zone"));
            params.add(row.get("division"));
        }
        zoneCondition.append(")");
*/
        List<Object> params = new ArrayList<>();
        StringBuilder zoneCondition = new StringBuilder("(");
        zoneCondition.append("(u.division = ?)");
        params.add(getWardByLoginId(loginId));
        zoneCondition.append(") ORDER BY u.cdate DESC");
        
        String sql = "SELECT " +
                "u.id, u.request_id, u.user_name, u.mobile, u.address, u.latitude, u.longitude, " +
                "u.zone, u.division, u.street_name, u.remarks, " +
                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.image_url) AS image_url, " +
                "DATE_FORMAT(u.cdate, '%d-%m-%Y') AS cdate, " +
                "u.is_active, u.is_delete, u.street_id, sm.status_master AS status, u.is_app, " +
                "DATE_FORMAT(u.close_date, '%d-%m-%Y') AS close_date, " +
                "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.officer_upload_url) AS officer_upload_url, " +
                "itm.quantity, im.item_name " +
                "FROM user_request_table u " +
                "LEFT JOIN items itm ON u.id = itm.req_table_id " +
                "LEFT JOIN item_master im ON itm.iteam_id = im.id AND im.is_active = 1 " +
                "LEFT JOIN status_master sm ON sm.id = u.status AND sm.is_active = 1 AND sm.is_delete = 0 " +
                "WHERE u.status != '2' " +
                "AND u.cdate <= ( CURDATE() - INTERVAL ((WEEKDAY(CURDATE()) + 4) % 7) DAY ) " +
                "AND u.is_active = 1 AND " + zoneCondition;

        List<Map<String, Object>> rawData = jdbcDomesticWasteTemplate.queryForList(sql, params.toArray());
        return groupRequestsWithItems(rawData);
    }


    public List<Map<String, Object>> userdetailsfilter(int id) {
        String fileBaseUrl = environment.getProperty("fileBaseUrl");

        // Step 1: Fetch zone, division, and street_id for the given request ID
        String sql = "SELECT zone, division, street_id FROM user_request_table WHERE id = ?";
        List<Map<String, Object>> result = jdbcDomesticWasteTemplate.queryForList(sql, id);

        if (result.isEmpty()) {
        	Map<String, Object> response = new HashMap<>();
            response.put("status", false);
            response.put("message", "No data found");
            return Collections.singletonList(response); // or just `response` depending on your return type
        }

        Map<String, Object> user = result.get(0);
        String zone = (String) user.get("zone");
        String division = (String) user.get("division");
        Object streetIdObj = user.get("street_id");

        // Step 2: Construct dynamic SQL query with/without street_id
        String finalSql;
        List<Object> params = new ArrayList<>();
        params.add(zone);
        params.add(division);

        if (streetIdObj != null && !streetIdObj.toString().isBlank()) {
            params.add(streetIdObj);
            params.add(id);
            finalSql = "SELECT u.id, u.request_id, u.user_name, u.mobile, u.address, u.latitude, u.longitude, " +
                    "u.zone, u.division, u.street_name, u.remarks, " +
                    "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.image_url) AS image_url, " +
                    "DATE_FORMAT(u.cdate, '%d-%m-%Y') AS cdate, u.is_active, u.is_delete, u.street_id, " +
                    "sm.status_master AS status, u.is_app, DATE_FORMAT(u.close_date, '%d-%m-%Y') AS close_date, " +
                    "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.officer_upload_url) AS officer_upload_url, " +
                    "itm.quantity, im.item_name " +
                    "FROM user_request_table u " +
                    "LEFT JOIN items itm ON u.id = itm.req_table_id " +
                    "LEFT JOIN item_master im ON itm.iteam_id = im.id AND im.is_active = 1 " +
                    "LEFT JOIN status_master sm ON sm.id = u.status AND sm.is_active = 1 AND sm.is_delete = 0 " +
                    "WHERE u.zone = ? AND u.division = ? AND u.street_id = ? AND u.id = ? AND u.is_active = 1";
        } else {
            params.add(id);
            finalSql = "SELECT u.id, u.request_id, u.user_name, u.mobile, u.address, u.latitude, u.longitude, " +
                    "u.zone, u.division, u.street_name, u.remarks, " +
                    "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.image_url) AS image_url, " +
                    "DATE_FORMAT(u.cdate, '%d-%m-%Y') AS cdate, u.is_active, u.is_delete, u.street_id, " +
                    "sm.status_master AS status, u.is_app, DATE_FORMAT(u.close_date, '%d-%m-%Y') AS close_date, " +
                    "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.officer_upload_url) AS officer_upload_url, " +
                    "itm.quantity, im.item_name " +
                    "FROM user_request_table u " +
                    "LEFT JOIN items itm ON u.id = itm.req_table_id " +
                    "LEFT JOIN item_master im ON itm.iteam_id = im.id AND im.is_active = 1 " +
                    "LEFT JOIN status_master sm ON sm.id = u.status AND sm.is_active = 1 AND sm.is_delete = 0 " +
                    "WHERE u.zone = ? AND u.division = ? AND u.id = ? AND u.is_active = 1";
        }

        List<Map<String, Object>> rawData = jdbcDomesticWasteTemplate.queryForList(finalSql, params.toArray());
        return groupRequestsWithItems(rawData);
    }


    public Map<String, Object> actionTaken(String requestId, MultipartFile imageFile, int createdBy,String latitude,String longitude  ) {
        try {
            // Step 0: Fetch internal id using requestId
            String fetchIdSql = "SELECT id FROM user_request_table WHERE request_id = ?";
            List<Integer> idList = jdbcDomesticWasteTemplate.queryForList(fetchIdSql, new Object[]{requestId}, Integer.class);
            if (idList.isEmpty()) {
                return Map.of("status", false, "message", "No record found for the given requestId: " + requestId);
            }
            Integer id = idList.get(0);

            // Step 1: Upload image
            String imagePath = fileUpload(imageFile, "officer_action_photo");

            // Step 2: Insert into officer_data
            String insertSql = "INSERT INTO officer_data (request_id, image_url, cdate, is_active, created_by,latitude,longitude) " +
                    "VALUES (?, ?, NOW(), ?, ?, ?, ?)";
            jdbcDomesticWasteTemplate.update(insertSql, requestId, imagePath, true, createdBy, latitude, longitude);

            // Step 3: Update user_request_table - status, officer_upload_url, and close_date from officer_data
            String updateSql = "UPDATE user_request_table u " +
                    "JOIN officer_data o ON o.request_id = u.request_id " +
                    "SET u.status = '2', " +
                    "    u.officer_upload_url = ?, " +
                    "    u.close_date = DATE(o.cdate) " +
                    "WHERE u.id = ?";
            jdbcDomesticWasteTemplate.update(updateSql, imagePath, id);



            return Map.of("status", true, "message", "Action taken recorded successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("status", false, "message", "Error: " + e.getMessage());
        }
    }

//scheduler method
    public Map<String, Object> updateOrderStatus(String status) {
        try {
            String sql = "UPDATE user_request_table " +
                    "SET status = ? " +
                    "WHERE status = '1' AND DATE(cdate) < CURDATE()";//

            int updated = jdbcDomesticWasteTemplate.update(sql, status);

            return Map.of("status", true, "message", "Order status updated", "rowsAffected", updated);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("status", false, "message", "Error: " + e.getMessage());
        }
    }

//to group items and quantity common method
    private List<Map<String, Object>> groupRequestsWithItems(List<Map<String, Object>> flatList) {
        Map<Integer, Map<String, Object>> groupedMap = new LinkedHashMap<>();

        for (Map<String, Object> row : flatList) {
            Integer id = (Integer) row.get("id");

            if (!groupedMap.containsKey(id)) {
                Map<String, Object> base = new LinkedHashMap<>(row);
                base.remove("item_name");
                base.remove("quantity");
                base.remove("iteam_id");
                base.put("items", new ArrayList<Map<String, Object>>());
                groupedMap.put(id, base);
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("item_name", row.get("item_name"));
            item.put("quantity", row.get("quantity"));

            ((List<Map<String, Object>>) groupedMap.get(id).get("items")).add(item);
        }

        //return new ArrayList<>(groupedMap.values());
        List<Object> result = new ArrayList<>(groupedMap.values());

        if (result.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", false);
            response.put("message", "No data found");
            return Collections.singletonList(response); // or just `response` depending on your return type
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("status", true);
            response.put("message", "Data retrieved successfully");
            response.put("data", result);
            return Collections.singletonList(response); // or `response` directly
        }
    }

    public boolean verifylatlong(String requestId, String latitude, String longitude) {
        int radius = 50;

        try {
            String sql = "SELECT 1 FROM user_request_table u " +
                    "WHERE u.request_id = ? AND " +
                    "((6371008.8 * ACOS(" +
                    "COS(RADIANS(?)) * COS(RADIANS(u.latitude)) * COS(RADIANS(u.longitude) - RADIANS(?)) + " +
                    "SIN(RADIANS(?)) * SIN(RADIANS(u.latitude)))) < ?)";

            List<Map<String, Object>> rows = jdbcDomesticWasteTemplate.queryForList(sql,
                    requestId, latitude, longitude, latitude, radius);

            return !rows.isEmpty(); // true if one or more rows match the criteria

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public Map<String, Object> rejectuserreq(String requestId) {
        try {
            // Step 0: Fetch internal id using requestId
            String fetchIdSql = "SELECT id FROM user_request_table WHERE request_id = ?";
            Integer id = jdbcDomesticWasteTemplate.queryForObject(fetchIdSql, new Object[]{requestId}, Integer.class);
            if (id == null) {
                return Map.of("status", false, "message", "No record found for the given requestId");
            }

            // Step 1: Update using id
            String sql = "UPDATE user_request_table SET status = '4' WHERE id = ?";
            jdbcDomesticWasteTemplate.update(sql, id);

            return Map.of("status", true, "message", "Rejected user request successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("status", false, "message", "Error: " + e.getMessage());
        }
    }

    public Map<String, Object> rejectuserreqofficer(String requestId, String officer_remark, 
    		String rejecttype, int cancelled_by,
    		String latitude, String longitude){
        try {
            // Step 0: Fetch internal id using requestId
            String fetchIdSql = "SELECT id FROM user_request_table WHERE request_id = ?";
            Integer id = jdbcDomesticWasteTemplate.queryForObject(fetchIdSql, new Object[]{requestId}, Integer.class);
            if (id == null) {
                return Map.of("status", false, "message", "No record found for the given requestId");
            }

            // Step 1: Proper SQL with commas
            String sql = "UPDATE user_request_table SET `status` = '5', officer_remark = ?, "
            		+ "cancelled_by = ?, rejecttype = ?, reject_latitude = ?, reject_longitude = ? WHERE id = ?";

            jdbcDomesticWasteTemplate.update(sql, officer_remark, cancelled_by, rejecttype, latitude, longitude, id);

            return Map.of("status", true, "message", "Rejected user request successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("status", false, "message", "Error: " + e.getMessage());
        }
    }
    
    @Transactional
	public List<Map<String, Object>> getZoneReport(String fromDate, String toDate) {
	    // 1. Fetch all active streets for the ward (with zone info)
	    String sqlStreet = "("
	    		+ "    SELECT "
	    		+ "        ur.zone,"
	    		+ "        COUNT(*) AS total_request,"
	    		+ "        SUM(CASE WHEN ur.status = 1 THEN 1 ELSE 0 END) AS open,"
	    		+ "        SUM(CASE WHEN ur.status = 2 THEN 1 ELSE 0 END) AS completed,"
	    		+ "        SUM(CASE WHEN ur.status = 3 THEN 1 ELSE 0 END) AS pending,"
	    		+ "        SUM(CASE WHEN ur.status = 4 THEN 1 ELSE 0 END) AS user_cancel,"
	    		+ "        SUM(CASE WHEN ur.status = 5 THEN 1 ELSE 0 END) AS officer_cancel"
	    		+ "    FROM user_request_table ur"
	    		+ "    WHERE ur.is_active = 1 AND ur.is_delete = 0"
	    		+ "    GROUP BY ur.zone"
	    		+ ")"
	    		
	    		+ "UNION ALL"
	    		
	    		+ "("
	    		+ "    SELECT "
	    		+ "        'TOTAL' AS zone,"
	    		+ "        COUNT(*) AS total_request,"
	    		+ "        SUM(CASE WHEN ur.status = 1 THEN 1 ELSE 0 END) AS open,"
	    		+ "        SUM(CASE WHEN ur.status = 2 THEN 1 ELSE 0 END) AS completed,"
	    		+ "        SUM(CASE WHEN ur.status = 3 THEN 1 ELSE 0 END) AS pending,"
	    		+ "        SUM(CASE WHEN ur.status = 4 THEN 1 ELSE 0 END) AS user_cancel,"
	    		+ "        SUM(CASE WHEN ur.status = 5 THEN 1 ELSE 0 END) AS officer_cancel"
	    		+ "    FROM user_request_table ur"
	    		+ "    WHERE ur.is_active = 1 AND ur.is_delete = 0"
	    		+ ") "
	    		+ "ORDER BY "
	    		+ "    CASE WHEN zone = 'TOTAL' THEN 999 ELSE zone END";

	    List<Map<String, Object>> reportList = jdbcDomesticWasteTemplate.queryForList(sqlStreet);

	    Map<String, Object> response = new LinkedHashMap<>();
	    response.put("status", "Success");
	    response.put("message", "Zone wise Report");
	    response.put("data", reportList);

	    return Collections.singletonList(response);
	}
    
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWardReport(String fromDate, String toDate, String zone) {
        String sql = 
            "("
            + "    SELECT "
            + "        ur.division AS division, "
            + "        COUNT(*) AS total_request, "
            + "        SUM(CASE WHEN ur.status = 1 THEN 1 ELSE 0 END) AS open, "
            + "        SUM(CASE WHEN ur.status = 2 THEN 1 ELSE 0 END) AS completed, "
            + "        SUM(CASE WHEN ur.status = 3 THEN 1 ELSE 0 END) AS pending, "
            + "        SUM(CASE WHEN ur.status = 4 THEN 1 ELSE 0 END) AS user_cancel, "
            + "        SUM(CASE WHEN ur.status = 5 THEN 1 ELSE 0 END) AS officer_cancel "
            + "    FROM user_request_table ur "
            + "    WHERE ur.is_active = 1 AND ur.is_delete = 0 AND ur.zone = ? "
            + "    GROUP BY ur.division "
            + ") "
            + "UNION ALL "
            + "("
            + "    SELECT "
            + "        'TOTAL' AS division, "
            + "        COUNT(*) AS total_request, "
            + "        SUM(CASE WHEN ur.status = 1 THEN 1 ELSE 0 END) AS open, "
            + "        SUM(CASE WHEN ur.status = 2 THEN 1 ELSE 0 END) AS completed, "
            + "        SUM(CASE WHEN ur.status = 3 THEN 1 ELSE 0 END) AS pending, "
            + "        SUM(CASE WHEN ur.status = 4 THEN 1 ELSE 0 END) AS user_cancel, "
            + "        SUM(CASE WHEN ur.status = 5 THEN 1 ELSE 0 END) AS officer_cancel "
            + "    FROM user_request_table ur "
            + "    WHERE ur.is_active = 1 AND ur.is_delete = 0 AND ur.zone = ? "
            + ") "
            + "ORDER BY "
            + "    CASE WHEN division = 'TOTAL' THEN 999 ELSE CAST(division AS UNSIGNED) END";

        List<Map<String, Object>> reportList = jdbcDomesticWasteTemplate.queryForList(sql, zone, zone);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "Success");
        response.put("message", "Ward-wise Report");
        response.put("data", reportList);

        return Collections.singletonList(response);
    }
    
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWardStatusDetails(
    		String fromDate, String toDate,
            String ward, String status) {

    	// ✅ Base URL for images
        String fileBaseUrl = "https://gccservices.in"; // or your configured value

        // ✅ SQL query with ward + status filters
        String sql =
            "SELECT " +
            "    u.id, u.request_id, u.user_name, u.mobile, u.address, " +
            "    u.latitude, u.longitude, u.zone, u.division, u.street_name, " +
            "    u.quantity AS main_quantity, u.remarks, " +
            "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.image_url) AS image_url, " +
            "    DATE_FORMAT(u.cdate, '%d-%m-%Y') AS cdate, " +
            "    sm.status_master AS status, " +
            "    DATE_FORMAT(u.close_date, '%d-%m-%Y') AS close_date, " +
            "    CONCAT('" + fileBaseUrl + "/gccofficialapp/files', u.officer_upload_url) AS officer_upload_url, " +
            "    im.item_name, itm.quantity " +
            "FROM user_request_table u " +
            "LEFT JOIN items itm ON u.id = itm.req_table_id " +
            "LEFT JOIN item_master im ON itm.iteam_id = im.id AND im.is_active = 1 " +
            "LEFT JOIN status_master sm ON sm.id = u.status AND sm.is_active = 1 AND sm.is_delete = 0 " +
            "WHERE u.is_active = 1 AND u.is_delete = 0 " +
            "  AND u.division = ? " +
            "  AND u.status = ? " +
            "ORDER BY u.cdate DESC";

        // ✅ Execute query
        List<Map<String, Object>> reportList = jdbcDomesticWasteTemplate.queryForList(sql, ward, status);
        return groupRequestsWithItems(reportList);
        
    }
}
