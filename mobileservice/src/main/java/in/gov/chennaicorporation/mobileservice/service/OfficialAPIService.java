package in.gov.chennaicorporation.mobileservice.service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.internal.build.AllowSysOut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.UpdateResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.client.RestTemplate;

@Service
public class OfficialAPIService {
	private JdbcTemplate jdbcErpTemplate;
	private JdbcTemplate jdbcUserTemplate;
	private JdbcTemplate jdbcOfficalTemplate;
	private JdbcTemplate jdbcActivityTemplate;
	@Autowired
	public void setDataSource(
			@Qualifier("oracleERPDataSource") DataSource erpDataSource,
			@Qualifier("mysqlGCCUserDataSource") DataSource gccUserDataSource,
			@Qualifier("mysqlPGRMasterDataSource") DataSource officialDataSource,
			@Qualifier("mysqlActivityDataSource") DataSource activityDataSource) {
		this.jdbcErpTemplate = new JdbcTemplate(erpDataSource);
		this.jdbcUserTemplate = new JdbcTemplate(gccUserDataSource);
		this.jdbcOfficalTemplate = new JdbcTemplate(officialDataSource);
		this.jdbcActivityTemplate = new JdbcTemplate(activityDataSource);
	}
	
	@Transactional
	public List<Map<String, Object>> versionCheck() {
		String sqlQuery = "SELECT * FROM `app_version` WHERE `isactive`=1 LIMIT 1"; 
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery);
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> nammaVersionCheck() {
		String sqlQuery = "SELECT * FROM `nammachennai_app_version` WHERE `isactive`=1 LIMIT 1"; 
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(sqlQuery);
		return result;
	}
	
	public List<Map<String, Object>> sendOTP(HttpServletRequest request,
            String MobileNo,
            String OTP,
            String checkmobileno
            ) {

		Boolean sendSMS = true;
		Map<String, Object> successResponse =null;
		if (!"false".equalsIgnoreCase(checkmobileno)) {
			if(getUserListByMobileNumber(MobileNo).size()<=0)
			{
				sendSMS = false;
				successResponse = Map.of(
			            "status", 404,
			            "message", "Given mobile number “"+MobileNo+"” is not registered."
			        );
			}
		}
		if(sendSMS) {
			//Integer OTP = OTP;
			String generatedToken = "Your OTP is " + OTP + ".OTP is valid for the next 5 minutes.\nBy\nGreater Chennai Corporation.";
			//String urlString = "https://tmegov.onex-aura.com/api/sms?key=pfTEYN6H&to="+MobileNo+"&from=GCCCRP&body="+generatedToken+"&entityid=1401572690000011081&templateid=1407165856492044436";
			String urlString = "https://tmegov.onex-aura.com/api/sms?key=pfTEYN6H&to=" + MobileNo + "&from=GCCCRP&body=" + generatedToken + "&entityid=1401572690000011081&templateid=1407165856492044436";
			//System.out.println(urlString);
			RestTemplate restTemplate = new RestTemplate();
			//System.out.println(getUserListByMobileNumber(MobileNo).size());
			String Response = restTemplate.getForObject(urlString, String.class);
			//System.out.println(Response);
			// Add a success status and message to the result
	        successResponse = Map.of(
	            "status", 200,
	            "message", "SMS Send successfully to Mobile No "+MobileNo+".",
	            //"data", Response,
	            "OTP", OTP
	        );
		}
        return Collections.singletonList(successResponse);
	}
	
	@Transactional
	public List<Map<String, Object>> getloginList(String username, String password, String usertype) {
		// PIN added in response for session logout needed by vaibalan 
		String sqlQuery = ""; 
		List<Map<String, Object>> result=null;
		try {
            switch (usertype) {
                case "erp":
                    //sqlQuery = "SELECT * FROM `EG_USER` WHERE `ISPORTALUSER` <> 1 AND `ISACTIVE` = 1 AND `EXTRAFIELD2` = ? AND `PIN` = ? LIMIT 1";
                	sqlQuery = "SELECT id, USER_NAME, EXTRAFIELD2, PIN FROM `EG_USER` WHERE `EXTRAFIELD2` = ? AND `PIN` = ? AND `ISACTIVE`=1 LIMIT 1";
                	result = jdbcOfficalTemplate.queryForList(sqlQuery, username, password);
                    break;
                case "nonerp":
                    sqlQuery = "SELECT id, USER_NAME, EXTRAFIELD2, PIN FROM `EG_USER` WHERE `EXTRAFIELD2` = ? AND `PIN` = ? AND `ISACTIVE`=1  LIMIT 1";
                    result = jdbcOfficalTemplate.queryForList(sqlQuery, username, password);
                    break;
                // additional cases can be added here
                default:
                	sqlQuery = "SELECT id, USER_NAME, EXTRAFIELD2, PIN FROM `EG_USER` WHERE `EXTRAFIELD2` = ? AND `PIN` = ? AND `ISACTIVE`=1  LIMIT 1";
                    result = jdbcOfficalTemplate.queryForList(sqlQuery, username, password);
                    //result = Collections.emptyList();
                    break;
            }
        } catch (Exception e) {
            // Log the error
            e.printStackTrace();
            // Return an error message as part of the result list
            return Collections.singletonList(Collections.singletonMap("error", "An error occurred while fetching data."));
        }
        
        // If no records found, return a status and message indicating no user found
        if (result == null || result.isEmpty()) {
            Map<String, Object> response = Map.of(
                "status", 404,
                "message", "User not found."
            );
            return Collections.singletonList(response);
        }
        
        // Add a success status and message to the result
        Map<String, Object> successResponse = Map.of(
            "status", 200,
            "message", "login successfuly.",
            "data", result
        );
        return Collections.singletonList(successResponse);
    }
	
	@Transactional
	public List<UpdateResponse> updatePassword(String username, String password) {
	    // Initialize the list to hold the result message
		List<UpdateResponse> responseList = new ArrayList<>();
	    
	    try {
	        // SQL query for updating the password
	        String sqlQuery = "UPDATE `EG_USER` SET `PIN` = ? WHERE `EXTRAFIELD2` = ?";
	        
	        // Execute the update query using jdbcTemplate and prepared statements to prevent SQL injection
	        int rowsAffected = jdbcOfficalTemplate.update(sqlQuery, password, username);
	        
	        // Check if any rows were updated
	        if (rowsAffected > 0) {
	        	responseList.add(new UpdateResponse(200, "Password or Pin updated successfully."));
	        } else {
	        	responseList.add(new UpdateResponse(404, "Password or Pin not updated."));
	        }
	    } catch (Exception e) {
	        // Handle any exceptions that occur during the update process
	    	responseList.add(new UpdateResponse(500, "Error updating Password or Pin: " + e.getMessage()));
	    }
	    
	    // Return the list containing the update message
	    return responseList;
	}
	
	@Transactional
	public List<Map<String, Object>> getUserListByMobileNumber(String mobileNo) {
		
		String sqlQuery = ""; 
		List<Map<String, Object>> result=null;
		sqlQuery = "SELECT * FROM `EG_USER` WHERE `EXTRAFIELD2` = ? AND `ISACTIVE`=1";
        result = jdbcOfficalTemplate.queryForList(sqlQuery, mobileNo);
        
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getUserList() {
		
		String SqlQuery = ""; 
		
		SqlQuery="SELECT * FROM `EG_USER` WHERE `ISACTIVE`=1";
		 
		List<Map<String, Object>> result = jdbcOfficalTemplate.queryForList(SqlQuery);
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getUserinfo(String USER_NAME) {
		
		String SqlQuery = ""; 
		
		SqlQuery="SELECT * FROM `EG_USER` WHERE `USER_NAME`=? AND `ISACTIVE`=1";
		 
		List<Map<String, Object>> result = jdbcOfficalTemplate.queryForList(SqlQuery,USER_NAME);
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getUserGroups(String loginId) {
		
		String SqlQuery = ""; 
		
		SqlQuery="SELECT `groups` FROM `user_mapping_details` WHERE `userid`=?";
		
		//System.out.println(SqlQuery);
		//System.out.println("loginId : "+loginId);
		 
		List<Map<String, Object>> result = jdbcUserTemplate.queryForList(SqlQuery,loginId);
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getUserMainMenu(String loginId) {
		
		String SqlQuery = ""; 
		String groups ="0";
		List<Map<String, Object>> groupids=getUserGroups(loginId);
		
		// Check if the list is not empty
		if (groupids != null && !groupids.isEmpty()) {
		    // Retrieve the first map in the list
		    Map<String, Object> firstEntry = groupids.get(0);
		    
		    // Check if the map contains the "groups" key
		    if (firstEntry.containsKey("groups")) {
		        // Get the "groups" value
		        groups = (String) firstEntry.get("groups");
		        
		        // Do something with the groups value
		        //System.out.println("Groups: " + groups);
		    } else {
		        System.out.println("The 'groups' key is not present.");
		    }
		} else {
		    System.out.println("No data found in groupids.");
		}
		
		//SqlQuery="SELECT GROUP_CONCAT(`module_id`) AS module_ids FROM `user_group_module_access` WHERE `group_id` IN ("+groups+") GROUP by `group_id`";
		SqlQuery="SELECT GROUP_CONCAT(DISTINCT `department_id` ORDER BY `department_id` ASC) AS department_ids "
				+ "FROM `user_group_department_access` "
				+ "WHERE `group_id` IN ("+groups+")";
		List<Map<String, Object>> result = jdbcUserTemplate.queryForList(SqlQuery);
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getUserModules(String loginId) {
		
		String SqlQuery = ""; 
		String groups ="0";
		List<Map<String, Object>> groupids=getUserGroups(loginId);
		
		// Check if the list is not empty
		if (groupids != null && !groupids.isEmpty()) {
		    // Retrieve the first map in the list
		    Map<String, Object> firstEntry = groupids.get(0);
		    
		    // Check if the map contains the "groups" key
		    if (firstEntry.containsKey("groups")) {
		        // Get the "groups" value
		        groups = (String) firstEntry.get("groups");
		        
		        // Do something with the groups value
		        //System.out.println("Groups: " + groups);
		    } else {
		        System.out.println("The 'groups' key is not present.");
		    }
		} else {
		    System.out.println("No data found in groupids.");
		}
		
		//SqlQuery="SELECT GROUP_CONCAT(`module_id`) AS module_ids FROM `user_group_module_access` WHERE `group_id` IN ("+groups+") GROUP by `group_id`";
		SqlQuery="SELECT GROUP_CONCAT(DISTINCT `module_id` ORDER BY `module_id` ASC) AS module_ids "
				+ "FROM `user_group_module_access` "
				+ "WHERE `group_id` IN ("+groups+")";
		List<Map<String, Object>> result = jdbcUserTemplate.queryForList(SqlQuery);
		return result;
	}
	
	
	@Transactional
	public List<Map<String, Object>> getUserMenuItem(String loginId) {
		
		String SqlQuery = ""; 
		String menus ="0";
		List<Map<String, Object>> moduleids=getUserModules(loginId);
		
		// Check if the list is not empty
		if (moduleids != null && !moduleids.isEmpty()) {
		    // Retrieve the first map in the list
		    Map<String, Object> firstEntry = moduleids.get(0);
		    
		    // Check if the map contains the "groups" key
		    if (firstEntry.containsKey("module_ids")) {
		        // Get the "groups" value
		    	menus = (String) firstEntry.get("module_ids");
		        
		        // Do something with the groups value
		        //System.out.println("Menus: " + menus);
		    } else {
		        System.out.println("The 'module_ids' key is not present.");
		    }
		} else {
		    System.out.println("No data found in moduleids.");
		}
		
		//SqlQuery="SELECT GROUP_CONCAT(`menuitem_id`) AS menuitem_ids FROM `user_group_menu_access` WHERE `module_id` IN ("+menus+") GROUP by `module_id`";
		 SqlQuery="SELECT GROUP_CONCAT(DISTINCT  `menuitem_id` ORDER BY `menuitem_id` ASC) AS menuitem_ids FROM "
		 		+ "`user_group_menu_access` WHERE `module_id` IN ("+menus+")";
		List<Map<String, Object>> result = jdbcUserTemplate.queryForList(SqlQuery);
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getMainMenu(String loginId) {
		//System.out.println("getMenu LoginID: " + loginId);
		String SqlQuery = ""; 
		String mainmenus ="0";
		if(loginId.isBlank() || loginId.isEmpty() || loginId==null || loginId.equals(null) || loginId.equals("null")) {
			loginId="11342";
		}
		List<Map<String, Object>> mainModuleids=getUserMainMenu(loginId);
		
		// Check if the list is not empty
		if (mainModuleids != null && !mainModuleids.isEmpty()) {
		    // Retrieve the first map in the list
		    Map<String, Object> firstEntry = mainModuleids.get(0);
		    
		    // Check if the map contains the "groups" key
		    if (firstEntry.containsKey("department_ids")) {
		        // Get the "groups" value
		    	mainmenus = (String) firstEntry.get("department_ids");
		        
		        // Do something with the groups value
		        //System.out.println("MainMenus: " + mainmenus);
		    } else {
		        System.out.println("The 'department_ids' key is not present.");
		    }
		} else {
		    System.out.println("No data found in moduleids.");
		}
		
		SqlQuery="SELECT *,CONCAT('https://gccservices.in/gccofficialapp/files/app_icon/menu/', icon_url) "
				+ "AS iconUrl FROM `department` WHERE `ISACTIVE`=1 AND `id` IN ("+mainmenus+") ORDER BY `orderby`, `name` ASC";
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(SqlQuery);
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getMenu(String loginId, String departmentId) {
		//System.out.println("getMenu LoginID: " + loginId);
		//System.out.println("getMenu DepartmentID: " + departmentId);
		String SqlQuery = ""; 
		String menus ="0";
		if(loginId.isBlank() || loginId.isEmpty() || loginId==null || loginId.equals(null) || loginId.equals("null")) {
			loginId="11342";
		}
		List<Map<String, Object>> moduleids=getUserModules(loginId);
		
		// Check if the list is not empty
		if (moduleids != null && !moduleids.isEmpty()) {
		    // Retrieve the first map in the list
		    Map<String, Object> firstEntry = moduleids.get(0);
		    
		    // Check if the map contains the "groups" key
		    if (firstEntry.containsKey("module_ids")) {
		        // Get the "groups" value
		    	menus = (String) firstEntry.get("module_ids");
		        
		        // Do something with the groups value
		        //System.out.println("Menus: " + menus);
		    } else {
		        System.out.println("The 'module_ids' key is not present.");
		    }
		} else {
		    System.out.println("No data found in moduleids.");
		}
		
		SqlQuery="SELECT *,CONCAT('https://gccservices.in/gccofficialapp/files/app_icon/menu/', icon_url) "
				+ "AS iconUrl FROM `modules` WHERE `ISACTIVE`=1 AND `id` IN ("+menus+") ";
		if(departmentId!=null && !departmentId.isEmpty() && !departmentId.isBlank()) {
			SqlQuery=SqlQuery+" AND `departmentId_id`='"+departmentId+"' ";
		}
		
		SqlQuery = SqlQuery+" ORDER BY `orderby`, `name` ASC";
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(SqlQuery);
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getSubMenu(String loginId, String menuId) {
		
		String SqlQuery = ""; 
		String menuitem ="0";
		
		if(loginId.isBlank() || loginId.isEmpty() || loginId==null || loginId.equals(null) || loginId.equals("null")) {
			loginId="11342";
		}
		
		List<Map<String, Object>> moduleids=getUserMenuItem(loginId);
		
		// Check if the list is not empty
		if (moduleids != null && !moduleids.isEmpty()) {
		    // Retrieve the first map in the list
		    Map<String, Object> firstEntry = moduleids.get(0);
		    
		    // Check if the map contains the "groups" key
		    if (firstEntry.containsKey("menuitem_ids")) {
		        // Get the "groups" value
		    	menuitem = (String) firstEntry.get("menuitem_ids");
		        
		        // Do something with the groups value
		        //System.out.println("MenuItems: " + menuitem);
		    } else {
		        System.out.println("The 'menuitem_ids' key is not present.");
		    }
		} else {
		    System.out.println("No data found in menuitem_ids.");
		}
		
		SqlQuery="SELECT *,CONCAT('https://gccservices.in/gccofficialapp/files/app_icon/menu/', icon_url) "
				+ "AS iconUrl FROM `menuitem` WHERE `ISACTIVE`=1 AND `module_id`=? AND `id` IN ("+menuitem+")  ORDER BY `orderby`, `name` ASC";
		 
		//System.out.println(SqlQuery);
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(SqlQuery,menuId);
		return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getSubSubMenu(String loginId, String menuId, String submenuId ) {
		
		String SqlQuery = ""; 
		
		SqlQuery="SELECT *,CONCAT('https://gccservices.in/gccofficialapp/files/app_icon/menu/', icon_url) AS iconUrl FROM `submenuitem` WHERE `ISACTIVE`=1 AND `module_id`=? AND `menuitem_id`=?";
		 
		List<Map<String, Object>> result = jdbcActivityTemplate.queryForList(SqlQuery,menuId,submenuId);
		return result;
	}
}
