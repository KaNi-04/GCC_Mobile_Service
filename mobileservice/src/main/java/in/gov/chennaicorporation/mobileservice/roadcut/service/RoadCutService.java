package in.gov.chennaicorporation.mobileservice.roadcut.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
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
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;
import in.gov.chennaicorporation.mobileservice.roadcut.modelclass.RoadCutDetail;
import in.gov.chennaicorporation.mobileservice.roadcut.modelclass.RoadCutResponse;

@Service
public class RoadCutService {

    @Autowired
    private JdbcTemplate jdbcRoadCutTemplate;
    private final Environment environment;
	private String fileBaseUrl;
	
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final int STRING_LENGTH = 15;
	private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlRoadCutDataSource") DataSource roadCutDataSource) {
		this.jdbcRoadCutTemplate = new JdbcTemplate(roadCutDataSource);
	}

    @Autowired
	public RoadCutService(Environment environment) {
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
		String serviceFolderName = environment.getProperty("roadcut_foldername");
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
	
    private String safeValue(String val) {
        return (val == null || val.trim().equalsIgnoreCase("N/A")) ? null : val.trim();
    }
    
    public void fetchAndSave(String fromDate, String toDate) throws Exception {
    	
    	// expected format = dd-MM-yyyy → convert to dd/MM/yyyy for URL
    	String formattedFromDate = fromDate.replace("-", "/");
    	
        String formattedToDate = toDate.replace("-", "/");
        
        String url = "http://coc-staging.egovernments.org:9980/pgr/external/mobileservice?"
        		+ "serviceId=common_api"
        		+ "&jsonResp=Yes"
        		+ "&subService=getRoadcutselfDet"
        		+ "&fromDate=" + formattedFromDate
        		+ "&toDate=" + formattedToDate;
        
        RestTemplate restTemplate = new RestTemplate();
        String json = restTemplate.getForObject(url, String.class);

        ObjectMapper mapper = new ObjectMapper();
        RoadCutResponse response = mapper.readValue(json, RoadCutResponse.class);

        String sql = "INSERT INTO road_cut_details (" +
                "NOC_APP_REM, ROAD_BREADTH, DAMAGE_FEE, ROAD_REMARKS, DEPT_NAME, PAYMENT_STATUS, " +
                "STREET, LOC, TENT_WORK_STARTDATE, ROAD_LENGTH, NOC_STATUS, APPLICATION_NO, " +
                "INTERLOCKING, NOC_APP_FROMDT, APPLICATION_DATE, AREA, ADDRESS, WARD, " +
                "DEPOSIT_WORKS_CATEGORY, ZONE, UNIQUEID, TENT_WORK_ENDDATE, APP_STATUS, " +
                "NOC_APP_TODT, NOC_APP_DATE, NO_OF_PIT, APPLICANT_NAME, TYPE_OF_ROAD, ROAD_DEPTH) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"+
                "ON DUPLICATE KEY UPDATE " +
                "NOC_APP_REM = VALUES(NOC_APP_REM), " +
                "ROAD_BREADTH = VALUES(ROAD_BREADTH), " +
                "DAMAGE_FEE = VALUES(DAMAGE_FEE), " +
                "ROAD_REMARKS = VALUES(ROAD_REMARKS), " +
                "DEPT_NAME = VALUES(DEPT_NAME), " +
                "PAYMENT_STATUS = VALUES(PAYMENT_STATUS), " +
                "STREET = VALUES(STREET), " +
                "LOC = VALUES(LOC), " +
                "TENT_WORK_STARTDATE = VALUES(TENT_WORK_STARTDATE), " +
                "ROAD_LENGTH = VALUES(ROAD_LENGTH), " +
                "NOC_STATUS = VALUES(NOC_STATUS), " +
                "APPLICATION_NO = VALUES(APPLICATION_NO), " +
                "INTERLOCKING = VALUES(INTERLOCKING), " +
                "NOC_APP_FROMDT = VALUES(NOC_APP_FROMDT), " +
                "APPLICATION_DATE = VALUES(APPLICATION_DATE), " +
                "AREA = VALUES(AREA), " +
                "ADDRESS = VALUES(ADDRESS), " +
                "WARD = VALUES(WARD), " +
                "DEPOSIT_WORKS_CATEGORY = VALUES(DEPOSIT_WORKS_CATEGORY), " +
                "ZONE = VALUES(ZONE), " +
                "TENT_WORK_ENDDATE = VALUES(TENT_WORK_ENDDATE), " +
                "APP_STATUS = VALUES(APP_STATUS), " +
                "NOC_APP_TODT = VALUES(NOC_APP_TODT), " +
                "NOC_APP_DATE = VALUES(NOC_APP_DATE), " +
                "NO_OF_PIT = VALUES(NO_OF_PIT), " +
                "APPLICANT_NAME = VALUES(APPLICANT_NAME), " +
                "TYPE_OF_ROAD = VALUES(TYPE_OF_ROAD), " +
                "ROAD_DEPTH = VALUES(ROAD_DEPTH)";
        
        for (RoadCutDetail rc : response.ListResult) {
        	//System.out.println("=======");
        	//System.out.println(mapper.writeValueAsString(rc));
        	//System.out.println("=======");
        	try {
        	jdbcRoadCutTemplate.update(sql,
        	        safeValue(rc.NOC_APP_REM),
        	        safeValue(rc.ROAD_BREADTH),
        	        safeValue(rc.DAMAGE_FEE),
        	        safeValue(rc.ROAD_REMARKS),
        	        safeValue(rc.DEPT_NAME),
        	        safeValue(rc.PAYMENT_STATUS),
        	        safeValue(rc.STREET),
        	        safeValue(rc.LOC),
        	        safeValue(rc.TENT_WORK_STARTDATE),
        	        safeValue(rc.ROAD_LENGTH),
        	        safeValue(rc.NOC_STATUS),
        	        safeValue(rc.APPLICATION_NO),
        	        safeValue(rc.INTERLOCKING),
        	        safeValue(rc.NOC_APP_FROMDT),
        	        safeValue(rc.APPLICATION_DATE),
        	        safeValue(rc.AREA),
        	        safeValue(rc.ADDRESS),
        	        safeValue(rc.WARD),
        	        safeValue(rc.DEPOSIT_WORKS_CATEGORY),
        	        safeValue(rc.ZONE),
        	        safeValue(rc.UNIQUEID),  // NOTE: This is NOT NULL. Must not be null!
        	        safeValue(rc.TENT_WORK_ENDDATE),
        	        safeValue(rc.APP_STATUS),
        	        safeValue(rc.NOC_APP_TODT),
        	        safeValue(rc.NOC_APP_DATE),
        	        safeValue(rc.NO_OF_PIT),
        	        safeValue(rc.APPLICANT_NAME),
        	        safeValue(rc.TYPE_OF_ROAD),
        	        safeValue(rc.ROAD_DEPTH)
        	    );
        	} catch (Exception e) {
        		 e.printStackTrace();  // shows exact SQL error
        	}
        }
    }
    
 // TNEB & Metro Water
    
    public List<Map<String, Object>> getRoadCutList(String loginid) {
        String sql = "SELECT  "
        		+ "    rcd.*,  "
        		+ "		CASE  "
        		+ "        WHEN rcd.NOC_STATUS = 'NOC Pending' THEN 1 "
        		+ "        WHEN rcd.NOC_STATUS = 'NOC Approved' THEN 2 "
        		+ "        ELSE 0 "
        		+ "    END AS NOC_STATUS_CODE, "
        		+ "    IFNULL(rcs.status, 'new') AS status "
        		+ "FROM  "
        		+ "    road_cut_details rcd "
        		+ "LEFT JOIN ( "
        		+ "    SELECT * "
        		+ "    FROM road_cut_status rcs1 "
        		+ "    WHERE rcs1.id IN ( "
        		+ "        SELECT MAX(id) "
        		+ "        FROM road_cut_status "
        		+ "        GROUP BY uid "
        		+ "    ) "
        		+ ") rcs ON rcs.uid = rcd.uid "
        		+ "WHERE  "
        		+ "    rcd.isactive = 1  "
        		+ "    AND rcd.isdelete = 0 "
        		+ "    AND rcd.PAYMENT_STATUS = 'Payment Done' "
        		+ "    AND rcd.NOC_STATUS IS NOT NULL "
        		+ "	   AND IFNULL(rcs.status, 'new') <> 'completed'";
        
        List<Map<String, Object>> result = jdbcRoadCutTemplate.queryForList(sql);
		Map<String, Object> response = new HashMap<>();
		if (result.isEmpty()) {
		    response.put("status", "No Data");
		    response.put("message", "No road cut list found.");
		    response.put("data", Collections.emptyList()); // or just null
		} else {
		    response.put("status", "Success");
		    response.put("message", "RoadCut List.");
		    response.put("data", result);
		}
        
        return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getLoactionCheck(String loginid, String refid, String streetid, String streetname) {
    	String sql = "SELECT " +
    		    "    rcd.*, " +
    		    "    CASE " +
    		    "        WHEN rcd.NOC_STATUS = 'NOC Pending' THEN 1 " +
    		    "        WHEN rcd.NOC_STATUS = 'NOC Approved' THEN 2 " +
    		    "        ELSE 0 " +
    		    "    END AS NOC_STATUS_CODE, " +  
    		    "    IFNULL(rcs.status, 'new') AS status " +  
    		    "FROM " +
    		    "    road_cut_details rcd " +
    		    "LEFT JOIN ( " +
    		    "    SELECT * " +
    		    "    FROM road_cut_status rcs1 " +
    		    "    WHERE rcs1.id IN ( " +
    		    "        SELECT MAX(id) " +
    		    "        FROM road_cut_status " +
    		    "        GROUP BY uid " +
    		    "    ) " +
    		    ") rcs ON rcs.uid = rcd.uid " +
    		    "WHERE " +
    		    "    rcd.isactive = 1 " +
    		    "    AND rcd.isdelete = 0 " +
    		    "    AND rcd.PAYMENT_STATUS = 'Payment Done' " +
    		    "    AND rcd.NOC_STATUS IS NOT NULL " +
    		    "    AND rcd.UNIQUEID = ? " +
    		    "    AND rcd.STREET = ?";
        
        List<Map<String, Object>> result = jdbcRoadCutTemplate.queryForList(sql,refid,streetname);
		Map<String, Object> response = new HashMap<>();
	    
		if (result.isEmpty()) {
		    response.put("status", "False");
		    response.put("message", "No road cut details found for the given criteria.");
		} else {
		    response.put("status", "True");
		    response.put("message", "Road cut found.");
		}
        
        return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getRoadCutDetails(String loginid, String refid) {
        String sql = "SELECT rcd.*, IFNULL(rcs.status, 'new') AS status "
                   + "FROM road_cut_details rcd "
                   + "LEFT JOIN ( "
                   + "    SELECT * FROM road_cut_status rcs1 "
                   + "    WHERE rcs1.id IN ( "
                   + "        SELECT MAX(id) FROM road_cut_status GROUP BY uid "
                   + "    ) "
                   + ") rcs ON rcs.uid = rcd.uid "
                   + "WHERE rcd.isactive = 1 "
                   + "AND rcd.isdelete = 0 "
                   + "AND rcd.PAYMENT_STATUS = 'Payment Done' "
                   + "AND rcd.NOC_STATUS IS NOT NULL "
                   + "AND rcd.UNIQUEID = ? "
                   + "AND IFNULL(rcs.status, 'new') <> 'completed'";

        List<Map<String, Object>> result = jdbcRoadCutTemplate.queryForList(sql, refid);

        for (Map<String, Object> roadCut : result) {
            String uid = String.valueOf(roadCut.get("uid"));

            String statusSql = "SELECT id, uid, "
            		+ "    DATE_FORMAT(indate, '%d-%m-%Y %I:%i %p') AS indate, "
            		+ "inby, status, "
            		+ "  CONCAT('" + fileBaseUrl + "/gccofficialapp/files', filepath) AS filepath, "
            		+ " action, isactive, "
                             + "latitude, longitude, zone, ward, streetid, streetname, nocid "
                             + "FROM road_cut_status WHERE uid = ? ORDER BY id ASC";

            List<Map<String, Object>> statusList = jdbcRoadCutTemplate.queryForList(statusSql, uid);

            List<Map<String, Object>> nocList = new ArrayList<>();
            Map<String, Object> nocEntry = null;
            int count = 1;

            for (Map<String, Object> statusRow : statusList) {
                String action = String.valueOf(statusRow.get("action")).toLowerCase();

                if ("before".equals(action)) {
                    // Save existing 'before' if it had no 'after'
                    if (nocEntry != null) {
                        nocEntry.put("after_date", null);
                        nocEntry.put("after_image", null);
                        nocEntry.put("after_status", null);
                        nocEntry.put("after_latitude", null);
                        nocEntry.put("after_longitude", null);
                        nocEntry.put("after_zone", null);
                        nocEntry.put("after_ward", null);
                        nocEntry.put("NOC_LINK", null);
                        nocList.add(nocEntry);
                        count++;
                    }

                    nocEntry = new LinkedHashMap<>();
                    nocEntry.put("name", "NOC - " + count);
                    nocEntry.put("before_date", statusRow.get("indate"));
                    nocEntry.put("before_image", statusRow.get("filepath"));
                    nocEntry.put("before_status", statusRow.get("status"));
                    nocEntry.put("before_latitude", statusRow.get("latitude"));
                    nocEntry.put("before_longitude", statusRow.get("longitude"));
                    nocEntry.put("before_zone", statusRow.get("zone"));
                    nocEntry.put("before_ward", statusRow.get("ward"));
                    nocEntry.put("NOC_ID", statusRow.get("nocid"));

                } else if ("after".equals(action) && nocEntry != null) {
                    nocEntry.put("after_date", statusRow.get("indate"));
                    nocEntry.put("after_image", statusRow.get("filepath"));
                    nocEntry.put("after_status", statusRow.get("status"));
                    nocEntry.put("after_latitude", statusRow.get("latitude"));
                    nocEntry.put("after_longitude", statusRow.get("longitude"));
                    nocEntry.put("after_zone", statusRow.get("zone"));
                    nocEntry.put("after_ward", statusRow.get("ward"));
                    
                    nocEntry.put("NOC_LINK", statusRow.get("filepath"));

                    nocList.add(nocEntry);
                    nocEntry = null;
                    count++;
                }
            }

            // Final unmatched 'before'
            if (nocEntry != null) {
                nocEntry.put("after_date", null);
                nocEntry.put("after_image", null);
                nocEntry.put("after_status", null);
                nocEntry.put("after_latitude", null);
                nocEntry.put("after_longitude", null);
                nocEntry.put("after_zone", null);
                nocEntry.put("after_ward", null);
                nocEntry.put("NOC_LINK", null);
                nocList.add(nocEntry);
            }

            roadCut.put("NOC_LIST", nocList);
        }

        Map<String, Object> response = new HashMap<>();
        if (result.isEmpty()) {
            response.put("status", "No Data");
            response.put("message", "No road cut details found for the given refid (" + refid + ").");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", "Success");
            response.put("message", "RoadCut Details.");
            response.put("data", result);
        }

        return Collections.singletonList(response);
    }
    
    @Transactional
    public List<Map<String, Object>> saveStatusUpdate(
            String uid,
            String cby,
            String latitude,
            String longitude,
            String zone,
            String ward,
            String streetid,
            String streetname,
            String action,
            String workstatus,
            String remarks,
            String nocid,
            MultipartFile filedata) {

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // Step 1: Get last action for the given uid
            String checkSql = "SELECT action, status FROM road_cut_status "
                            + "WHERE uid = ? ORDER BY id DESC LIMIT 1";

            String lastAction = null;
            String lastStatus = null;
            try {
                //lastAction = jdbcRoadCutTemplate.queryForObject(checkSql, new Object[]{uid}, String.class);
                Map<String, Object> lastRow = jdbcRoadCutTemplate.queryForMap(checkSql, uid);
                lastAction = (String) lastRow.get("action");
                lastStatus = (String) lastRow.get("status");
            } catch (EmptyResultDataAccessException e) {
                // No previous entry — lastAction stays null
            }

            // Step 2: Logic check
            if("completed".equalsIgnoreCase(lastStatus)) {
            	response.put("status", "error");
                response.put("message", "Cannot update ‘after’ or `before` status — This action is already completed");
                result.add(response);
                return result;
            }
            
            if ("after".equalsIgnoreCase(action)) {
                if (!"before".equalsIgnoreCase(lastAction)) {
                    response.put("status", "error");
                    response.put("message", "Cannot upload ‘after’ status — please upload a ‘before’ status first.");
                    result.add(response);
                    return result;
                }
            } else if ("before".equalsIgnoreCase(action)) {
                if ("before".equalsIgnoreCase(lastAction)) {
                    response.put("status", "error");
                    response.put("message", "'Before' already exists. You cannot insert it again before 'after'.");
                    result.add(response);
                    return result;
                }
            }

            // Step 3: Upload file
            String actionimg = fileUpload("other_"+uid, action, filedata);

            // Step 4: Insert data
            
            // Before Create new NOC 
            if ("before".equalsIgnoreCase(action)) {
            	String insertNOCSql = "INSERT INTO `road_cut_noc_list`(`uid`, `other_before`) VALUES (?,?)";
            	KeyHolder keyHolder_noc = new GeneratedKeyHolder();
            	int affectedRows_noc = jdbcRoadCutTemplate.update(connection -> {
                    PreparedStatement ps_noc = connection.prepareStatement(insertNOCSql, new String[]{"nocid"});
                    int i = 1;
                    ps_noc.setString(i++, uid);
                    ps_noc.setInt(i++, 1);
                    return ps_noc;
                }, keyHolder_noc);

                if (affectedRows_noc > 0) {
                    int nocInsertId = keyHolder_noc.getKey().intValue();
                    nocid=""+nocInsertId;
                } else {
                	response.put("status", "error");
                    response.put("message", "'NOC - Insert `before` failed.");
                    result.add(response);
                    return result;
                }
            }
            
            String noc_id = nocid;
            
         // Update After NOC
            if ("after".equalsIgnoreCase(action)) {
                String updateNOCSql = "UPDATE `road_cut_noc_list` SET `other_after`=? WHERE `nocid`=?";
                int affectedRowsNOC = jdbcRoadCutTemplate.update(updateNOCSql, 1, nocid);

                if (affectedRowsNOC > 0) {
                    
                } else {
                    response.put("status", "error");
                    response.put("message", "NOC - Update 'after' failed.");
                    result.add(response);
                    return result;
                }
            }
            
            String insertSql = "INSERT INTO road_cut_status "
                    + "(uid, inby, latitude, longitude, zone, ward, streetid, streetname, "
                    + "status, filepath, action, isactive, nocid) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();
            int affectedRows = jdbcRoadCutTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"id"});
                int i = 1;
                ps.setString(i++, uid);
                ps.setString(i++, cby);
                ps.setString(i++, latitude);
                ps.setString(i++, longitude);
                ps.setString(i++, zone);
                ps.setString(i++, ward);
                ps.setString(i++, streetid);
                ps.setString(i++, streetname);
                ps.setString(i++, workstatus);
                ps.setString(i++, actionimg);
                ps.setString(i++, action);
                ps.setInt(i++, 1);
                ps.setString(i++, noc_id);
                return ps;
            }, keyHolder);

            if (affectedRows > 0) {
                int lastInsertId = keyHolder.getKey().intValue();
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "Status update inserted successfully.");
            } else {
                response.put("status", "error");
                response.put("message", "Insert failed.");
            }

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Exception occurred: " + e.getMessage());
            e.printStackTrace();
        }

        result.add(response);
        return result;
    }
    
    /////// ********************************************************** ///////
    
    // AE GCC Officer

    public List<Map<String, Object>> getRoadCutList_offical(String loginid) {
        String sql = "SELECT "
        		+ "    rcd.*, "
        		+ "		CASE  "
        		+ "        WHEN rcd.NOC_STATUS = 'NOC Pending' THEN 1 "
        		+ "        WHEN rcd.NOC_STATUS = 'NOC Approved' THEN 2 "
        		+ "        ELSE 0 "
        		+ "    END AS NOC_STATUS_CODE, "
        		+ "    status_summary.status "
        		+ "FROM road_cut_details rcd "
        		+ "JOIN ( "
        		+ "    SELECT "
        		+ "        noc.uid, "
        		+ "        CASE "
        		+ "            WHEN SUM(CASE WHEN noc.gcc_before != 1 OR noc.gcc_after != 1 THEN 1 ELSE 0 END) > 0 THEN 'pending' "
        		+ "            ELSE 'completed' "
        		+ "        END AS status "
        		+ "    FROM road_cut_noc_list noc "
        		+ "    WHERE noc.isactive = 1 AND noc.other_before = 1 AND noc.other_after = 1 "
        		+ "    GROUP BY noc.uid "
        		+ ") AS status_summary "
        		+ "ON rcd.uid = status_summary.uid "
        		+ "WHERE  "
        		+ "    rcd.isactive = 1 "
        		+ "    AND rcd.isdelete = 0 "
        		+ "    AND rcd.PAYMENT_STATUS = 'Payment Done' "
        		+ "    AND rcd.NOC_STATUS IS NOT NULL "
        		+ "ORDER BY rcd.uid";
        
        List<Map<String, Object>> result = jdbcRoadCutTemplate.queryForList(sql);
		Map<String, Object> response = new HashMap<>();
		if (result.isEmpty()) {
		    response.put("status", "No Data");
		    response.put("message", "No officer road cut list found.");
		    response.put("data", Collections.emptyList()); // or just null
		} else {
		    response.put("status", "Success");
		    response.put("message", "Officer RoadCut List.");
		    response.put("data", result);
		}
        
        return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getLoactionCheck_offical(String loginid, String refid, String streetid, String streetname) {
    	String sql = "SELECT " +
    		    "    rcd.*, " +
    		    "    CASE " +
    		    "        WHEN rcd.NOC_STATUS = 'NOC Pending' THEN 1 " +
    		    "        WHEN rcd.NOC_STATUS = 'NOC Approved' THEN 2 " +
    		    "        ELSE 0 " +
    		    "    END AS NOC_STATUS_CODE, " +  
    		    "    IFNULL(rcs.status, 'new') AS status " +  
    		    "FROM " +
    		    "    road_cut_details rcd " +
    		    "LEFT JOIN ( " +
    		    "    SELECT * " +
    		    "    FROM road_cut_status rcs1 " +
    		    "    WHERE rcs1.id IN ( " +
    		    "        SELECT MAX(id) " +
    		    "        FROM road_cut_status " +
    		    "        GROUP BY uid " +
    		    "    ) " +
    		    ") rcs ON rcs.uid = rcd.uid " +
    		    "WHERE " +
    		    "    rcd.isactive = 1 " +
    		    "    AND rcd.isdelete = 0 " +
    		    "    AND rcd.PAYMENT_STATUS = 'Payment Done' " +
    		    "    AND rcd.NOC_STATUS IS NOT NULL " +
    		    "    AND rcd.UNIQUEID = ? " +
    		    "    AND rcd.STREET = ?";
        
        List<Map<String, Object>> result = jdbcRoadCutTemplate.queryForList(sql,refid,streetname);
		Map<String, Object> response = new HashMap<>();
	    
		if (result.isEmpty()) {
		    response.put("status", "False");
		    response.put("message", "No road cut details found for the given criteria.");
		} else {
		    response.put("status", "True");
		    response.put("message", "Road cut found.");
		}
        
        return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getRoadCutDetails_offical(String loginid, String refid) {
        String sql = "SELECT rcd.*, IFNULL(rcs.status, 'new') AS status "
                   + "FROM road_cut_details rcd "
                   + "LEFT JOIN ( "
                   + "    SELECT * FROM road_cut_status rcs1 "
                   + "    WHERE rcs1.id IN ( "
                   + "        SELECT MAX(id) FROM road_cut_status GROUP BY uid "
                   + "    ) "
                   + ") rcs ON rcs.uid = rcd.uid "
                   + "WHERE rcd.isactive = 1 "
                   + "AND rcd.isdelete = 0 "
                   + "AND rcd.PAYMENT_STATUS = 'Payment Done' "
                   + "AND rcd.NOC_STATUS IS NOT NULL "
                   + "AND rcd.UNIQUEID = ?";

        List<Map<String, Object>> result = jdbcRoadCutTemplate.queryForList(sql, refid);

        for (Map<String, Object> roadCut : result) {
            String uid = String.valueOf(roadCut.get("uid"));

            String statusSql = "SELECT `nocid`, `uid`, `other_before`, `other_after`, "
                             + "`gcc_before`, `gcc_after`, `noc_file` "
                             + "FROM `road_cut_noc_list` "
                             + "WHERE uid=? AND `isactive`=1 "
                             + "ORDER BY `nocid` ASC";

            List<Map<String, Object>> statusList = jdbcRoadCutTemplate.queryForList(statusSql, uid);
            List<Map<String, Object>> nocList = new ArrayList<>();

            for (Map<String, Object> statusRow : statusList) {
                String nocid = String.valueOf(statusRow.get("nocid"));

                String formattedStatusSql = "SELECT id, uid, "
                        + "DATE_FORMAT(indate, '%d-%m-%Y %I:%i %p') AS indate, "
                        + "inby, status, "
                        + "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', filepath) AS filepath, "
                        + "action, isactive, latitude, longitude, zone, ward, streetid, streetname, nocid "
                        + "FROM road_cut_status "
                        + "WHERE uid = ? AND nocid = ? "
                        + "ORDER BY id ASC";

                List<Map<String, Object>> formattedList = jdbcRoadCutTemplate.queryForList(formattedStatusSql, uid, nocid);

                String statusNOCSql = "SELECT id, uid, "
                		+ "DATE_FORMAT(indate, '%d-%m-%Y %I:%i %p') AS indate, "
                		+ "inby, status, "
                		+ "CONCAT('" + fileBaseUrl + "/gccofficialapp/files', filepath) AS filepath, "
                		+ "action, isactive, latitude, longitude, zone, ward, streetid, streetname, nocid "
                        + "FROM road_cut_officer_status "
                        + "WHERE uid = ? AND nocid = ? "
                        + "ORDER BY id ASC";
                
                List<Map<String, Object>> officerList = jdbcRoadCutTemplate.queryForList(statusNOCSql, uid, nocid);

                Map<String, Object> nocEntry = new LinkedHashMap<>();
                nocEntry.put("name", "NOC - " + nocid);
                nocEntry.put("NOC_ID", nocid);

                // 🟩 Add default nulls for officer_* fields
                nocEntry.put("officer_before_date", null);
                nocEntry.put("officer_before_image", null);
                nocEntry.put("officer_before_status", null);
                nocEntry.put("officer_before_latitude", null);
                nocEntry.put("officer_before_longitude", null);
                nocEntry.put("officer_before_zone", null);
                nocEntry.put("officer_before_ward", null);
                nocEntry.put("officer_after_date", null);
                nocEntry.put("officer_after_image", null);
                nocEntry.put("officer_after_status", null);
                nocEntry.put("officer_after_latitude", null);
                nocEntry.put("officer_after_longitude", null);
                nocEntry.put("officer_after_zone", null);
                nocEntry.put("officer_after_ward", null);

                // Add formatted (app) status
                for (Map<String, Object> formattedRow : formattedList) {
                    String action = String.valueOf(formattedRow.get("action")).trim().toLowerCase();
                    if ("before".equals(action)) {
                        nocEntry.put("before_date", formattedRow.get("indate"));
                        nocEntry.put("before_image", formattedRow.get("filepath"));
                        nocEntry.put("before_status", formattedRow.get("status"));
                        nocEntry.put("before_latitude", formattedRow.get("latitude"));
                        nocEntry.put("before_longitude", formattedRow.get("longitude"));
                        nocEntry.put("before_zone", formattedRow.get("zone"));
                        nocEntry.put("before_ward", formattedRow.get("ward"));
                    } else if ("after".equals(action)) {
                        nocEntry.put("after_date", formattedRow.get("indate"));
                        nocEntry.put("after_image", formattedRow.get("filepath"));
                        nocEntry.put("after_status", formattedRow.get("status"));
                        nocEntry.put("after_latitude", formattedRow.get("latitude"));
                        nocEntry.put("after_longitude", formattedRow.get("longitude"));
                        nocEntry.put("after_zone", formattedRow.get("zone"));
                        nocEntry.put("after_ward", formattedRow.get("ward"));
                        nocEntry.put("NOC_LINK", formattedRow.get("filepath"));
                    }
                }

                //  Add officer_* if available
                for (Map<String, Object> officerRow : officerList) {
                	System.out.println("I am IN "+officerRow.get("id"));
                    String action = String.valueOf(officerRow.get("action")).trim().toLowerCase();
                    if ("before".equals(action)) {
                        nocEntry.put("officer_before_date", officerRow.get("indate"));
                        nocEntry.put("officer_before_image", officerRow.get("filepath"));
                        nocEntry.put("officer_before_status", officerRow.get("status"));
                        nocEntry.put("officer_before_latitude", officerRow.get("latitude"));
                        nocEntry.put("officer_before_longitude", officerRow.get("longitude"));
                        nocEntry.put("officer_before_zone", officerRow.get("zone"));
                        nocEntry.put("officer_before_ward", officerRow.get("ward"));
                    } else if ("after".equals(action)) {
                        nocEntry.put("officer_after_date", officerRow.get("indate"));
                        nocEntry.put("officer_after_image", officerRow.get("filepath"));
                        nocEntry.put("officer_after_status", officerRow.get("status"));
                        nocEntry.put("officer_after_latitude", officerRow.get("latitude"));
                        nocEntry.put("officer_after_longitude", officerRow.get("longitude"));
                        nocEntry.put("officer_after_zone", officerRow.get("zone"));
                        nocEntry.put("officer_after_ward", officerRow.get("ward"));
                        //nocEntry.put("NOC_LINK", officerRow.get("filepath")); // fallback
                    }
                }

                nocList.add(nocEntry);
            }

            roadCut.put("NOC_LIST", nocList);
        }

        Map<String, Object> response = new HashMap<>();
        if (result.isEmpty()) {
            response.put("status", "No Data");
            response.put("message", "No road cut details found for the given refid (" + refid + ").");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", "Success");
            response.put("message", "RoadCut Details.");
            response.put("data", result);
        }

        return Collections.singletonList(response);
    }
    
    @Transactional
    public List<Map<String, Object>> saveStatusUpdate_offical(
            String uid,
            String cby,
            String latitude,
            String longitude,
            String zone,
            String ward,
            String streetid,
            String streetname,
            String action,
            String workstatus,
            String remarks,
            String nocid,
            MultipartFile filedata) {

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // Step 1: Get last action for the given uid
            String checkSql = "SELECT action, status FROM `road_cut_officer_status` "
                            + "WHERE uid = ? AND nocid = ? ORDER BY id DESC LIMIT 1";

            String lastAction = null;
            String lastStatus = null;
            try {
                //lastAction = jdbcRoadCutTemplate.queryForObject(checkSql, new Object[]{uid}, String.class);
                Map<String, Object> lastRow = jdbcRoadCutTemplate.queryForMap(checkSql, uid, nocid);
                lastAction = (String) lastRow.get("action");
                lastStatus = (String) lastRow.get("status");
            } catch (EmptyResultDataAccessException e) {
                // No previous entry — lastAction stays null
            }

            // Step 2: Logic check
            if("completed".equalsIgnoreCase(lastStatus)) {
            	response.put("status", "error");
                response.put("message", "Officer - Cannot update ‘after’ or `before` status — This action is already completed");
                result.add(response);
                return result;
            }
            
            if ("after".equalsIgnoreCase(action)) {
                if (!"before".equalsIgnoreCase(lastAction)) {
                    response.put("status", "error");
                    response.put("message", "Cannot upload ‘after’ status — please upload a ‘before’ status first.");
                    result.add(response);
                    return result;
                }
            } else if ("before".equalsIgnoreCase(action)) {
                if ("before".equalsIgnoreCase(lastAction)) {
                    response.put("status", "error");
                    response.put("message", "'Before' already exists. You cannot insert it again before 'after'.");
                    result.add(response);
                    return result;
                }
            }

            // Step 3: Upload file
            String actionimg = fileUpload("officer_"+uid, action, filedata);

            // Step 4: Insert data
            
            // Update before NOC
            if ("before".equalsIgnoreCase(action)) {
                String updateNOCSql = "UPDATE `road_cut_noc_list` SET `gcc_before`=? WHERE `nocid`=?";
                int affectedRowsNOC = jdbcRoadCutTemplate.update(updateNOCSql, 1, nocid);

                if (affectedRowsNOC > 0) {
                    
                } else {
                    response.put("status", "error");
                    response.put("message", "Oficer NOC - Update 'before' failed.");
                    result.add(response);
                    return result;
                }
            }
            
            // Update After NOC
            if ("after".equalsIgnoreCase(action)) {
                String updateNOCSql = "UPDATE `road_cut_noc_list` SET `gcc_after`=? WHERE `nocid`=?";
                int affectedRowsNOC = jdbcRoadCutTemplate.update(updateNOCSql, 1, nocid);

                if (affectedRowsNOC > 0) {
                    
                } else {
                    response.put("status", "error");
                    response.put("message", "Oficer NOC - Update 'after' failed.");
                    result.add(response);
                    return result;
                }
            }
            
            String insertSql = "INSERT INTO road_cut_officer_status "
                    + "(uid, inby, latitude, longitude, zone, ward, streetid, streetname, "
                    + "status, filepath, action, isactive, nocid) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();
            int affectedRows = jdbcRoadCutTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"id"});
                int i = 1;
                ps.setString(i++, uid);
                ps.setString(i++, cby);
                ps.setString(i++, latitude);
                ps.setString(i++, longitude);
                ps.setString(i++, zone);
                ps.setString(i++, ward);
                ps.setString(i++, streetid);
                ps.setString(i++, streetname);
                ps.setString(i++, workstatus);
                ps.setString(i++, actionimg);
                ps.setString(i++, action);
                ps.setInt(i++, 1);
                ps.setString(i++, nocid);
                return ps;
            }, keyHolder);

            if (affectedRows > 0) {
                int lastInsertId = keyHolder.getKey().intValue();
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "Officer Status update inserted successfully.");
            } else {
                response.put("status", "error");
                response.put("message", "Officer Insert failed.");
            }

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Exception occurred: " + e.getMessage());
            e.printStackTrace();
        }

        result.add(response);
        return result;
    }
}
