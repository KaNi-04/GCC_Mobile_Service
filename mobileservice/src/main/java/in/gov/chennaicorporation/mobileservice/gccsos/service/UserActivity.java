package in.gov.chennaicorporation.mobileservice.gccsos.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class UserActivity {
	
	private JdbcTemplate jdbcSOSTemplate;
	private final Environment environment;
	private String fileBaseUrl;
	
	@Autowired
	public void setDataSource(@Qualifier("mysqlGccSOSDataSource") DataSource sosDataSource) {
		this.jdbcSOSTemplate = new JdbcTemplate(sosDataSource);
	}
	
	@Autowired
	public UserActivity(Environment environment) {
		this.environment = environment;
		this.fileBaseUrl=environment.getProperty("fileBaseUrl");
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
	public List<Map<String, Object>> getAlert(){
		String alertQuery ="";
		
		alertQuery="SELECT "
				+ "    `id`,"
				+ "    `name`,"
				+ "    `color`,"
				+ "    DATE_FORMAT(`fromdate`, '%d-%m-%Y') AS `fromdate`,"
				+ "    DATE_FORMAT(`todate`, '%d-%m-%Y') AS `todate`,"
				+ "    `isactive`,"
				+ "    `isdelete` "
				+ "FROM "
				+ "    `alerts` "
				+ "WHERE  isactive=1 AND isdelete=0 LIMIT 1";
		List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(alertQuery);
		Map<String, Object> response = new HashMap<>();
	    
	    response.put("status", "success");
        response.put("message", "Request Information");
        response.put("Data", result);
        
        return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> saveRequest(
			String contact_name,
			String contact_number,
			String latitude,
			String longitude,
			String zone,
			String ward,
			String streetid,
			String streetname,
			String location_details,
			String request_type,
			String no_of_count,
			String if_any,
			String land_mark,
			String remarks,
			String loginId,
			String mode
			) {
		List<Map<String, Object>> result=null;
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		
		String sqlQuery = "INSERT INTO `rescue`(`contact_name`, `contact_number`, `latitude`, `longitude`, `zone`, `ward`, "
				+ "`streetid`, `streetname`, `location_details`, `request_type`, `no_of_count`, `if_any`, `land_mark`, "
				+ "`remarks`, `user_id`,`modeofcomplient`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		
		try {
            int affectedRows = jdbcSOSTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
                    ps.setString(1, contact_name);
                    ps.setString(2, contact_number);
                    ps.setString(3, latitude);
                    ps.setString(4, longitude);
                    ps.setString(5, zone);
                    ps.setString(6, ward);
                    ps.setString(7, streetid);
                    ps.setString(8, streetname);
                    ps.setString(9, location_details);
                    ps.setString(10, request_type);
                    ps.setString(11, no_of_count);
                    ps.setString(12, if_any);
                    ps.setString(13, land_mark);
                    ps.setString(14, remarks);
                    ps.setString(15, loginId);
                    ps.setString(16, mode);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                response.put("insertId", lastInsertId);
                response.put("status", "success");
                response.put("message", "A new SOS request was inserted successfully!");
                System.out.println("A new SOS request was inserted successfully! Insert ID: " + generatedId);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to insert a new SOS request.");
            }
        } catch (DataAccessException e) {
            System.out.println("Data Access Exception:");
            Throwable rootCause = e.getMostSpecificCause();
            if (rootCause instanceof SQLException) {
                SQLException sqlException = (SQLException) rootCause;
                System.out.println("SQL State: " + sqlException.getSQLState());
                System.out.println("Error Code: " + sqlException.getErrorCode());
                System.out.println("Message: " + sqlException.getMessage());
                response.put("status", "error");
                response.put("message", sqlException.getMessage());
                response.put("sqlState", sqlException.getSQLState());
                response.put("errorCode", sqlException.getErrorCode());
            } else {
                System.out.println("Message: " + rootCause.getMessage());
                response.put("status", "error");
                response.put("message", rootCause.getMessage());
            }
        }

        return Collections.singletonList(response);
    }
	

	@Transactional
	public List<Map<String, Object>> getMyRequest(
			String loginId,
			String status){
		String statusidQuery ="";
		// Use equals() for string comparison
	    if ("Pending".equals(status)) {
	    	statusidQuery =  " AND `status` = 0 "; // Open
	    } else if ("Closed".equals(status)) {
	    	statusidQuery =  " AND `status` = 1 "; // Closed
	    }
	    
		String sqlQuery = "SELECT *,CASE"
				+ "	WHEN status = 0 THEN 'Pending' "
				+ "	WHEN status = 1 THEN 'Closed' "
				+ "    ELSE 'unknown'"
				+ "    END AS status_name "
				+ " FROM `rescue` WHERE `user_id`='"+loginId+"' ";
		sqlQuery = sqlQuery + statusidQuery;
		
		System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
	    
	    response.put("status", "success");
        response.put("message", "Request Information");
        response.put("Data", result);
        
        return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> getRequestDataById(
			String rescue_id){
		String sqlQuery = "SELECT *,CASE"
				+ "	WHEN status = 0 THEN 'Pending' "
				+ "	WHEN status = 1 THEN 'Closed' "
				+ "    ELSE 'unknown'"
				+ "    END AS status_name "
				+ " FROM `rescue` WHERE `id`='"+rescue_id+"'";
		List<Map<String, Object>> result = jdbcSOSTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
	    if (result.isEmpty()) {
	    	return result;
	    }else {
	    
			String sqlQuery2 = "SELECT *,CONCAT('"+fileBaseUrl+"/gccofficialapp/files', filepath) AS imageUrl "
					+ "FROM `rescue_officer_update` WHERE rescue_id='"+rescue_id+"'";
		    List<Map<String, Object>> result2 = jdbcSOSTemplate.queryForList(sqlQuery2);
		   System.out.println(sqlQuery2);
		    // Add the result2 and result3 to the main result map
		    result.get(0).put("officer_activity_data", result2);
	    }
	    response.put("status", "success");
        response.put("message", "Request Information");
        response.put("Data", result);
        return Collections.singletonList(response);
	}
	
}
