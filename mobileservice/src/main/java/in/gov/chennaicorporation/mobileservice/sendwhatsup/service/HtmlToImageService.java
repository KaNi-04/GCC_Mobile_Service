package in.gov.chennaicorporation.mobileservice.sendwhatsup.service;

import com.openhtmltopdf.java2d.api.Java2DRendererBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

@Service
public class HtmlToImageService {
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
    private Environment environment;
	private String fileBaseUrl;
	
	@Autowired
	public HtmlToImageService(Environment environment) {
		this.fileBaseUrl=environment.getProperty("fileBaseUrl");
	}
	
	@Autowired
	public void setDataSource(@Qualifier("mysqlVehiclTrackingDataSource") DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}
	
	public Map<String, Object> processHtmlToPng(String url, String tempId) {

        String pngPath = "gcc_mobile_service_uploads/whatsappreports/report_"
                + tempId + "_" + System.currentTimeMillis() + ".png";

        Map<String, Object> result = generatePng(url, pngPath);

        String todayDate = LocalDate.now().toString();

        if (result.get("status") != null
                && ((Number) result.get("status")).intValue() == 200) {

        	pngPath = pngPath.replace("gcc_mobile_service_uploads", ""); // Change the pngPath for whatsapp
        	
            String imagePath = pngPath;
            
            int msgId = saveMessageDetails(todayDate, tempId, imagePath);

            if (msgId > 0) {
            	System.err.println("Auto whatsapp: MSG ID"+ msgId);
                sendMessage(String.valueOf(msgId), "", "");
                disablemessage(String.valueOf(msgId));
            }
        }
        return result;
    }
	
    @Transactional
    public Map<String, Object> generatePng(String htmlPath, String outputPath) {

        Map<String, Object> response = new HashMap<>();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "wkhtmltoimage",
                //"--dpi","150",
                "--quality", "100",
                "--width", "1200",
                "--enable-local-file-access",
                htmlPath,
                outputPath
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                response.put("status", 200);
                response.put("message", "PNG generated successfully");
                outputPath = outputPath.replace("gcc_mobile_service_uploads/", "");
                response.put("path", "https://gccservices.in/gccofficialapp/files/"+outputPath);
            } else {
                response.put("status", 500);
                response.put("message", "wkhtmltoimage failed");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", 500);
            response.put("message", "Exception occurred");
        }

        return response;
    }
    /*
    public int saveMessageDetails(String date, String tempType, String imgPath) {
        String sql = "INSERT INTO `message_list` (`msg_date`, `template_type`, `msg_img`) VALUES (?,?,?)";
        int rowsAffected = jdbcTemplate.update(sql, date, tempType, imgPath);
        return rowsAffected;
    }
    */
    public int saveMessageDetails(String date, String tempType, String imgPath) {

        String sql = "INSERT INTO message_list (msg_date, template_type, msg_img) VALUES (?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, date);
            ps.setString(2, tempType);
            ps.setString(3, imgPath);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue(); // ✅ last inserted ID
    }
    
    public String sendMessage(String msgid, String datetxt, String fileurl) {
    	
    	String sql = "SELECT "
        		+ "ml.`id` as msgid, "
        		+ "DATE_FORMAT(ml.`msg_date`, '%d-%m-%Y') AS msg_temp_event_date, "
        		+ "CONCAT('"+fileBaseUrl+"/gccofficialapp/files', ml.msg_img) AS msg_attachfile, "
        		+ "wt.`id` as template_id "
        		+ "FROM `message_list` ml "
        		+ "JOIN `whatsapp_template` wt ON ml.`template_type`=wt.`id` "
        		+ "WHERE ml.`isactive` = 1 AND ml.`isdelete` = 0 AND ml.`id`=?";
    	// Fetch the data
    	List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, msgid);
    	
    	// Initialize variables to store the results
    	String tempid = "";
    	String msgDate = "";
    	String fileURL = "";
    	String sendTo="";
    	// Check if the result list is not empty
    	if (!result.isEmpty()) {
    	    Map<String, Object> row = result.get(0);  // Get the first row (assuming id is unique)
    	    
    	    // Extract values from the result map and assign to variables
    	    tempid = String.valueOf(row.get("template_id"));
    	    msgDate = String.valueOf(row.get("msg_temp_event_date"));
    	    fileURL = String.valueOf(row.get("msg_attachfile"));
    	    
    	    sql = "SELECT `id`, `name`, `mobile`, `userid`, `tempids` FROM `send_msg_to` WHERE `isactive` = 1 AND `isdelete` = 0 AND FIND_IN_SET(?, tempids);";
    	    List<Map<String, Object>> mobileResult = jdbcTemplate.queryForList(sql,tempid);
    	    // Create a StringBuilder to hold the comma-separated mobile numbers
    	    StringBuilder sendToBuilder = new StringBuilder();

    	    // Iterate through the mobileResult list and build the sendTo string
    	    for (Map<String, Object> mobileRow : mobileResult) {
    	        String mobile = String.valueOf(mobileRow.get("mobile"));
    	        
    	        // Append the mobile number to the StringBuilder
    	        if (sendToBuilder.length() > 0) {
    	            sendToBuilder.append(",");
    	        }
    	        sendToBuilder.append(mobile);
    	    }

    	    // Convert StringBuilder to String
    	    sendTo = sendToBuilder.toString();
    	    
    	    // Now you have the sendTo string with all mobile numbers
    	    System.out.println("Mobile Numbers: " + sendTo); // For debugging, you can print this value
    	}
    	
    	String urlString="";
    	
    	String username="2000233507";
    	String password="h2YjFNcJ";
    	//String sendTo="9176617754"; //9176617754,8610011680,9360777472,9123565217
    	//String msgDate=datetxt;
    	//String fileURL=fileurl;
    	String apikey = "5c995535-6244-11f0-98fc-02c8a5e042bd";
    	String from="919445061913";
    	switch (tempid) {
    	    case "1":
    	        System.out.println("TempID is 1 (Vehicle March Out -> officer_test -> 1308878)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1"
    	        		+ "&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+Vehicle+Monitoring+System%2A%0A%0AZone-wise+vehicle+march-out+details+for+Today%27s+%28"+msgDate+"%29+AM+shift+are+shared+above.%0A%0AFor+further+details%2C+please+click+on+the+button+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true"
    	        		+ "&footer=GCC+-+IT+Cell"
    	        		+ "&buttonUrlParam=login.php%3Funame%3Dgcc.admin2024@jtrack.in%26password%3D123456";
    	        break;
    	    case "2":
    	        System.out.println("TempID is 2 (Fule Dip Details -> fuel_dip_details -> 1309072)");
    	        
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+Vehicle+Monitoring+System%2A%0A%0AYesterday%27s+%28"+msgDate+"%29+Zone+wise+Fuel+Dip+Details+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	    case "3":
    	        System.out.println("TempID is 3 (Late Marchout Details -> late_marchout_details -> 1309075)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+Vehicle+Monitoring+System%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone-wise+Late+Marchout+Details+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	    case "4":
    	        System.out.println("TempID is 4 (LMV Route Deviation Details -> lmv_route_deviation_details -> 1309081)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+Vehicle+Monitoring+System%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone-wise+Compactors+LMV+Route+Deviation+Details+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	    case "5":
    	        System.out.println("TempID is 5 (HMV Route Deviation Details -> hmv_route_deviation_details -> 1309083)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+Vehicle+Monitoring+System%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone-wise+Compactors+HMV+Route+Deviation+Details+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	        
    	    case "6":
    	        System.out.println("TempID is 6 (C&D Waste Removal Monitoring -> c_and_d_waste_removal_monitoring -> 1335786)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+C%26D+Waste+Removal+Monitoring%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone+wise+C%26D+Waste+Removal+Details+shared+above+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	        
    	    case "7":
    	        System.out.println("TempID is 7 (Enforcement Team Monitoring -> enforcement_team_monitoring -> 1362100)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+Enforcement+Team+Monitoring%2A%0A%0AYesterday%27s+%28"+msgDate+"%29+Zone+wise+Enforcement+Team+activity+details+shared+above+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	        
    	    case "8":
    	        System.out.println("TempID is 8 (Toilets Monitoring -> gcc_toilets_monitoring -> 1475869)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+Toilets+Monitoring%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone+wise+Toilets+Morning+inspection+details+shared+above+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	        
    	    case "9":
    	        System.out.println("TempID is 9 (Toilets Monitoring -> gcc_toilets_evening_monitoring -> 1475881)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+Toilets+Monitoring%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone+wise+Toilets+Evening+inspection+details+shared+above+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	        
    	    case "10":
    	        System.out.println("TempID is 10 (Bus Shelter Monitoring -> 1508454 - gcc_bus_shelter_monitoring)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+Bus+Shelter+Monitoring%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone+wise+Bus+Shelter+inspection+details+shared+above+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	        
    	    case "11":
    	        System.out.println("TempID is 11 (Still Catch Pit -> 1566758 - silt_catch_pit_cleaning )");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+Silt+catch+Pit+Cleaning%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone+wise+Silt+Catch+Pit+cleaning+details+shared+above+for+your+reference%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	        
    	    case "12":
    	        System.out.println("TempID is 12 (1590940 - tree_planting_gcc - IMAGE - STATIC)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+Tree+Planting%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone+wise+tree+planting+details+shared+above+for+your+reference%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	        
    	    case "13":
    	        System.out.println("TempID is 13 (1590940 - Chennai Schools (userd of Com Temp ) - IMAGE - STATIC)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		//+ "&caption=%2AGCC+The+Chennai+School%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone+wise+Students+Aadhar+linked+details+shared+above+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&caption=%2AGCC+The+Chennai+School%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone+wise+Students+Aadhar+linked+details+shared+above+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	        
    	    case "14":
    	        System.out.println("TempID is 14 (1943196 - pet_wp_report - IMAGE - STATIC)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		//+ "&caption=%2AGCC+The+Chennai+School%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone+wise+Students+Aadhar+linked+details+shared+above+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&caption=%2AGCC+PET+LICENSE+%26+VACCINATION%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone+wise+Pet+license+%26+Vaccination+details+shared+above+for+your+kind+reference."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	        
    	    case "15":
    	        System.out.println("TempID is 15 (1590940 - GCC POS Penalty Monitoring Zone (userd of Com Temp ) - IMAGE - STATIC)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+POS+Penalty+Monitoring%2A%0A%0AToday%27s+%28"+msgDate+"%29++POS+Penalty+Zone+wise+details+shared+above+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	        
    	    case "16":
    	        System.out.println("TempID is 16 (1590940 - GCC POS Penalty Monitoring Catagory (userd of Com Temp ) - IMAGE - STATIC)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+POS+Penalty+Monitoring%2A%0A%0AToday%27s+%28"+msgDate+"%29++POS+Penalty+Catagory+wise+details+shared+above+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	        
    	    case "17":
    	        System.out.println("TempID is 17 (1590940 - GCC Enforcement Team Monitoring Temp (userd of Com Temp ) - IMAGE - STATIC)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+Enforcement+Team+Monitoring%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone+wise+Enforcement+Team+activity+details+shared+above+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        break;
    	        
    	    case "18":
    	        System.out.println("TempID is 18 (1590940 - GCC POS Penalty Monitoring Zone (userd of Com Temp ) - IMAGE - STATIC)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+POS+Penalty+Monitoring%2A%0A%0AToday%27s+%28"+msgDate+"%29++POS+Penalty+Zone+wise+details+shared+above+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        
    	        urlString ="https://sendapiv1.pinbot.ai/pinwa/sendMessage?type=template&"
    	        		+ "apikey="+apikey+"&from="+from+""
    	        				+ "&to=9444173345&templateid=2784943"
    	        				+ "&url="+fileURL
    	        				+ "&placeholders=POS Penalty Zone wise details";
    	        break;
    	        
    	    case "19":
    	        System.out.println("TempID is 19 (1590940 - GCC POS Penalty Monitoring Catagory (userd of Com Temp ) - IMAGE - STATIC)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+POS+Penalty+Monitoring%2A%0A%0AToday%27s+%28"+msgDate+"%29++POS+Penalty+Catagory+wise+details+shared+above+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        
    	        urlString ="https://sendapiv1.pinbot.ai/pinwa/sendMessage?type=template&"
    	        		+ "apikey="+apikey+"&from="+from+""
    	        				+ "&to=9444173345&templateid=2784943"
    	        				+ "&url="+fileURL
    	        				+ "&placeholders=POS Penalty Catagory wise details";
    	        break;
    	        
    	    case "20":
    	        System.out.println("TempID is 20 (1590940 - GCC Enforcement Team Monitoring Temp (userd of Com Temp ) - IMAGE - STATIC)");
    	        urlString="https://media.smsgupshup.com/GatewayAPI/rest?"
    	        		+ "userid="+username
    	        		+ "&password="+password
    	        		+ "&send_to="+sendTo
    	        		+ "&v=1.1&format=json"
    	        		+ "&msg_type=IMAGE"
    	        		+ "&method=SENDMEDIAMESSAGE"
    	        		+ "&caption=%2AGCC+Enforcement+Team+Monitoring%2A%0A%0AToday%27s+%28"+msgDate+"%29+Zone+wise+Enforcement+Team+activity+details+shared+above+for+your+reference.%0A%0AFor+more+details%2C+please+click+the+link+below."
    	        		+ "&media_url="+fileURL
    	        		+ "&isTemplate=true&footer=GCC+-+IT+Cell";
    	        
    	        urlString ="https://sendapiv1.pinbot.ai/pinwa/sendMessage?type=template&"
    	        		+ "apikey="+apikey+"&from="+from+""
    	        				+ "&to={{to}}&templateid=2784943"
    	        				+ "&url="+fileURL
    	        				+ "&placeholders=Zone wise Enforcement Team activity details";
    	        
    	        break;
    	        
    	    default:
    	        System.out.println("TempID ("+tempid+") is unknown");
    	}

    	if (!urlString.isBlank()) {
    		sql = "SELECT `id`, `name`, `mobile`, `userid`, `tempids` FROM `send_msg_to` WHERE `isactive` = 1 AND `isdelete` = 0 AND FIND_IN_SET(?, tempids);";
    	    List<Map<String, Object>> mobileResult = jdbcTemplate.queryForList(sql,tempid);
    		for (Map<String, Object> mobileRow : mobileResult) {
    	        String mobile = String.valueOf(mobileRow.get("mobile"));
    	        urlString = urlString.replace("{{to}}", mobile);
    	        
    	        System.out.println("=========================(Whatsapp Auto Start)========================");
    	        
    	        System.out.println("TempID (2784943)");
    	        System.out.println(urlString);
    	        
        	    String response = sendWhatsAppMessage(urlString);
        	    
        	    System.out.println("WhatsApp response: " + response);
        	    
    	        System.out.println("=========================(Whatsapp Auto End)========================");
        	    
    	    }
    	    
    	}
    	
    	return "success";
    }
	
    private String sendWhatsAppMessage(String urlString) {
        String response = "";
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            response = String.valueOf(responseCode);
            System.out.println("Response Code for URL: " + urlString + " is " + responseCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
    
    public String disablemessage(String msgid) {
        String sql = "update message_list set isdelete =1, isactive = 0 where id = ?";
        System.err.println("update message_list set isdelete =1, isactive = 0 where id = "+ msgid);
		int rowsAffected = jdbcTemplate.update(sql, msgid);

		if (rowsAffected > 0) {
			return "success";
		}else {
			return "failed";
		}
    }
}