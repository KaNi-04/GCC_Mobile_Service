package in.gov.chennaicorporation.mobileservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.service.DataSyncService;
import in.gov.chennaicorporation.mobileservice.service.OTPGenerator;
import in.gov.chennaicorporation.mobileservice.service.OfficialAPIService;
import jakarta.servlet.http.HttpServletRequest;

@RequestMapping("/gccofficialapp/api/")
@RestController("gccofficialappRest")
public class APIOfficialController {
	@Autowired
    private OfficialAPIService officialAPIService;
	
	@Autowired
    private DataSyncService dataSyncService;
	
	@GetMapping(value="")
	public String index() {
		return "Invalid Url";
	}
	
	@GetMapping({"/versionCheck"}) // Template
	public List <?> versionCheck() {
		return officialAPIService.versionCheck();
	}
	
	@GetMapping({"/nammaVersionCheck"}) // Template
	public List <?> nammaVersionCheck() {
		return officialAPIService.nammaVersionCheck();
	}
	
	// API FOR TOTHER APP
    @GetMapping(value="/sendOTP")
	public List sendOTP(HttpServletRequest request,
			@RequestParam("mobileNo") String mobileNo,
			@RequestParam("otpLength") Integer OTP_LENGTH,
			@RequestParam(value = "mobilenocheck", required = false) String checkmobileno) {
    		String generatedToken = OTPGenerator.generateOTP(OTP_LENGTH);
		return officialAPIService.sendOTP(request, mobileNo, generatedToken, checkmobileno);
	}
	@PostMapping(value="/savePassword")
	public List savePetition(
			@RequestParam("username") String username,
			@RequestParam("password") String password
			) {
		
		return officialAPIService.updatePassword(username,password);
	}
	
	@GetMapping(value="login")
	public List login(@RequestParam("username") String username, @RequestParam("password") String Password, @RequestParam("usertype") String usertype) {
		return officialAPIService.getloginList(username, Password, usertype);
	}
	
	@GetMapping(value="getUserByMobile")
	public List getUserByMobile(@RequestParam("mobileNo") String mobileNo) {
		return officialAPIService.getUserListByMobileNumber(mobileNo);
	}
	
	@GetMapping(value="getUserinfo")
	public List getUserinfo(@RequestParam("username") String username) {
		return officialAPIService.getUserinfo(username);
	}
	
	@GetMapping(value="getUserList")
	public List getUserList() {
		return officialAPIService.getUserList();
	}
	
	@GetMapping(value="getUserGroups")
	public List getUserGroups(@RequestParam String loginId) {
		return officialAPIService.getUserGroups(loginId);
	}
	
	@GetMapping(value="getUserModules")
	public List getUserModules(@RequestParam String loginId) {
		return officialAPIService.getUserModules(loginId);
	}
	
	@GetMapping(value="getUserMenuItem")
	public List getUserMenuItem(@RequestParam String loginId) {
		return officialAPIService.getUserMenuItem(loginId);
	}
	
	// For Menu ////////////////////////////////////////
	@GetMapping(value="/getMainMenu")
	public List getMainMenu(@RequestParam String loginId){
		return officialAPIService.getMainMenu(loginId);
	}
	
	@GetMapping(value="/getMenu")
	public List getMenu(
			@RequestParam String loginId,
			@RequestParam(value = "departmentId", required = false) String departmentId){
		return officialAPIService.getMenu(loginId,departmentId);
	}
	
	@GetMapping(value="/getSubMenu")
	public List getSubMenu(@RequestParam String loginId,@RequestParam String menuId){
		return officialAPIService.getSubMenu(loginId,menuId);
	}
	
	@GetMapping(value="/getSubSubMenu")
	public List getSubSubMenu(@RequestParam String loginId,@RequestParam String menuId,@RequestParam String submenuId){
		return officialAPIService.getSubSubMenu(loginId,menuId,submenuId);
	}
	// Menu End ////////////////////////////////////////
	
	@GetMapping(value="/syncERPtoLocal")
	public String syncERPtoLocal(@RequestParam("syncType") String syncType) {
		return syncData(syncType);
	}
	
	public String syncData(String syncType) {
		switch (syncType) {
	    case "egUser":
	    	System.out.println("Starting Syning EG_USER Table Data.\n");
			dataSyncService.syncEgUserData();
			System.out.println("EG_USER Table Data synchronization completed.\n");
			break;
	    case "EgDepartment":
	    	System.out.println("Starting Syning EG_DEPARTMENT Table Data.\n");
			dataSyncService.syncEgDepartmentData();
			System.out.println("EG_DEPARTMENT Table Data synchronization completed.\n");
	        break;
	    case "EgBoundary":
	    	System.out.println("Starting Syning EG_BOUNDARY Table Data.\n");
			dataSyncService.syncEgBoundaryData();
			System.out.println("EG_BOUNDARY Table Data synchronization completed.\n");
			break;
	    case "EggrComplaintStatus":
	    	System.out.println("Starting Syning EGGR_COMPLAINTSTATUS Table Data.\n");
			dataSyncService.syncEggrComplaintStatusData();
			System.out.println("EGGR_COMPLAINTSTATUS Table Data synchronization completed.\n");
			break;
	    case "EggrComplaintTypes":
	    	System.out.println("Starting Syning EGGR_COMPLAINTTYPES Table Data.\n");
			dataSyncService.syncEggrComplaintTypesData();
			System.out.println("EGGR_COMPLAINTTYPES Table Data synchronization completed.\n");
			break;
	    case "EggrComplaintGroup":
	    	System.out.println("Starting Syning EGGR_COMPLAINTGROUP Table Data.\n");
			dataSyncService.syncEggrComplaintGroupData();
			System.out.println("EGGR_COMPLAINTGROUP Table Data synchronization completed.\n");
			break;
	    case "EggrComplaintDetails":
	    	System.out.println("Starting Syning EGGR_COMPLAINTDETAILS Table Data.\n");
			dataSyncService.syncEggrComplaintDetailsData();
			System.out.println("EGGR_COMPLAINTDETAILS Table Data synchronization completed.\n");
			break;
	    case "EggrRedressalDetails":
	    	System.out.println("Starting Syning EGGR_REDRESSALDETAILS Table Data.\n");
			dataSyncService.syncEggrRedressalDetailsData();
			System.out.println("EGGR_REDRESSALDETAILS Table Data synchronization completed.\n");
			break;
	    // additional cases
	    default:
	    	System.out.println("Starting Syning EG_USER Table Data.\n");
			dataSyncService.syncEgUserData();
			System.out.println("EG_USER Table Data synchronization completed.\n");
			
			System.out.println("Starting Syning EG_DEPARTMENT Table Data.\n");
			dataSyncService.syncEgDepartmentData();
			System.out.println("EG_DEPARTMENT Table Data synchronization completed.\n");
			
			System.out.println("Starting Syning EG_BOUNDARY Table Data.\n");
			dataSyncService.syncEgBoundaryData();
			System.out.println("EG_BOUNDARY Table Data synchronization completed.\n");
			
			System.out.println("Starting Syning EGGR_COMPLAINTSTATUS Table Data.\n");
			dataSyncService.syncEggrComplaintStatusData();
			System.out.println("EGGR_COMPLAINTSTATUS Table Data synchronization completed.\n");
			
			System.out.println("Starting Syning EGGR_COMPLAINTTYPES Table Data.\n");
			dataSyncService.syncEggrComplaintTypesData();
			System.out.println("EGGR_COMPLAINTTYPES Table Data synchronization completed.\n");
			
			System.out.println("Starting Syning EGGR_COMPLAINTGROUP Table Data.\n");
			dataSyncService.syncEggrComplaintGroupData();
			System.out.println("EGGR_COMPLAINTGROUP Table Data synchronization completed.\n");
			
			System.out.println("Starting Syning EGGR_COMPLAINTDETAILS Table Data.\n");
			dataSyncService.syncEggrComplaintDetailsData();
			System.out.println("EGGR_COMPLAINTDETAILS Table Data synchronization completed.\n");
			
			System.out.println("Starting Syning EGGR_REDRESSALDETAILS Table Data.\n");
			dataSyncService.syncEggrRedressalDetailsData();
			System.out.println("EGGR_REDRESSALDETAILS Table Data synchronization completed.\n");
		}
		
        return "ERP -> All PGR Data synchronization completed!";
    }
	
}
