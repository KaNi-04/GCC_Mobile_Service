package in.gov.chennaicorporation.mobileservice.gccactivity.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.Activity;

@RequestMapping("/gccofficialapp/api/")
@RestController("gccofficialappActivityRest")
public class APIActivityController {
	@Autowired
    private Activity activity;
	
	@GetMapping(value="/asset/getActivityCategory")
	public List<?> getActivityCategory(@RequestParam(value = "departmentId", required = false) String departmentId){
		return activity.getActivityCategory(departmentId);
	}
	
	@GetMapping(value="/asset/checkAssetExistsCount")
	public Map<String, Object> checkAssetExistsCount(
			@RequestParam(value = "categoryId", required = false) String categoryId,
			@RequestParam(value = "streetId", required = false) String streetId,
			@RequestParam(value = "latitude", required = false) String latitude,
			@RequestParam(value = "longitude", required = false) String longitude
			){
		int count = activity.checkAssetExistsCount(categoryId, streetId, latitude, longitude);
	    Map<String, Object> response = new HashMap<>();
	    response.put("count", count);
	    return response;
	}
	
	@PostMapping(value="/asset/save")
	public List<?> saveAsset(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam("assetTypeId") String assetTypeId, 
			@RequestParam("latitude") String latitude, 
			@RequestParam("longitude") String longitude,
			@RequestParam("zone") String zone,
			@RequestParam("ward") String ward,
			@RequestParam("streeId") String streeId,
			@RequestParam("streetName") String streetName,
			@RequestParam("loginId") String loginId,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "file", required = false) MultipartFile file
			) {
		return activity.saveAsset(formData, assetTypeId, latitude, longitude, zone, ward, streeId, streetName, loginId, name, file);
	}
	
	@GetMapping(value="/asset/loadAssetlistbyStreetId")
	public List<?> loadAssetlistbyStreetId(@RequestParam("assetTypeId") String assetTypeId,@RequestParam("streetId") String streetId){
		return activity.loadAssetlistbyStreetId(assetTypeId,streetId);
	}
	
	@GetMapping(value="/asset/loadAssetQuestionlistbyAssetTypeId")
	public List<?> loadAssetQuestionlistbyAssetTypeId(@RequestParam("assetTypeId") String assetTypeId){
		return activity.loadAssetQuestionlistbyAssetTypeId(assetTypeId);
	}
			
	@PostMapping(value="/asset/saveAssetAction")
	public List<?> saveAssetAction(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam("catId") String catId,
			@RequestParam("assetId") String assetId, 
			@RequestParam("remarks") String remarks, 
			@RequestParam("latitude") String latitude,
			@RequestParam("longitude") String longitude,
			@RequestParam(value = "loginId", required = false) String loginId,
			@RequestParam("feedbackId") String feedbackId,
			@RequestParam(value = "before_file", required = false) MultipartFile beforeFile,
			@RequestParam(value = "after_file", required = false) MultipartFile afterFile
			) {
		return activity.saveAssetAction(formData, catId, assetId, remarks, latitude, longitude, loginId, feedbackId, beforeFile, afterFile);
	}
	
	@GetMapping(value="/asset/loadAssetLastFeedbackHistoryByUser")
	public List<?> loadAssetLastFeedbackHistoryByUser(@RequestParam("catId") String catId, @RequestParam("assetId") String assetId,@RequestParam("loginId") String loginId){
		return activity.loadAssetLastFeedbackHistoryByUser(catId,assetId,loginId);
	}
	
	@GetMapping(value="/asset/loadAssetLastFeedbackByUser")
	public List<?> loadAssetLastFeedbackByUser(@RequestParam("catId") String catId, @RequestParam("assetId") String assetId,@RequestParam("loginId") String loginId){
		return activity.loadAssetLastFeedbackByUser(catId,assetId,loginId);
	}
	
	@GetMapping(value = "/asset/filterReports")
	public Map<String, Object> filterReports(
			@RequestParam(value = "assetTypeId", required = false) String assetTypeId,
			@RequestParam(value = "fromDate", required = false) String fromDate,
			@RequestParam(value = "toDate", required = false) String toDate,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streetid", required = false) String streetid,
			@RequestParam(value = "loginId", required = false) String loginId
			){
		return activity.filterReports(assetTypeId, fromDate, toDate, zone, ward, streetid,loginId);
	}
	
	@GetMapping(value = "/asset/scpfilterReports")
	public Map<String, Object> scpfilterReports(
			@RequestParam(value = "assetTypeId", required = false) String assetTypeId,
			@RequestParam(value = "fromDate", required = false) String fromDate,
			@RequestParam(value = "toDate", required = false) String toDate,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streetid", required = false) String streetid,
			@RequestParam(value = "loginId", required = false) String loginId
			){
		return activity.scpfilterReports(assetTypeId, fromDate, toDate, zone, ward, streetid,loginId);
	}
	
	
	@GetMapping(value = "/asset/scpReportAssetList")
	public Map<String, Object> scpReportAssetList(
			@RequestParam(value = "assetTypeId", required = false) String assetTypeId,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "fromDate", required = false) String fromDate,
			@RequestParam(value = "toDate", required = false) String toDate,
			@RequestParam(value = "streetid", required = false) String streetid,
			@RequestParam(value = "loginId", required = false) String loginId
			){
		return activity.scpReportAssetList(assetTypeId, status, fromDate, toDate, streetid,loginId);
	}
	
	@GetMapping(value = "/asset/scpDeactivate")
	public String scpDeactivate(
			@RequestParam(value = "assetid", required = true) String id,
			@RequestParam(value = "loginId", required = true) String loginId
			){
		return activity.scpDeactivate(id,loginId);
	}
	
	@GetMapping(value = "/asset/filterReportsByDate")
	public List<Map<String, Object>> filterReportsByDate(
			@RequestParam(value = "assetTypeId", required = false) String assetTypeId,
			@RequestParam(value = "fromDate", required = false) String fromDate,
			@RequestParam(value = "toDate", required = false) String toDate,
			@RequestParam(value = "streetid", required = false) String streetid,
			@RequestParam(value = "loginId", required = false) String loginId
			){
		return activity.filterReportsByDate(assetTypeId, fromDate, toDate, fromDate, toDate, streetid, loginId);
	}
	
	@GetMapping(value = "/asset/dashboardFiletReport")
	public List<Map<String, Object>> dashboardFiletReport(
			@RequestParam(value = "assetTypeId", required = false) String assetTypeId,
			@RequestParam(value = "fromDate", required = false) String fromDate,
			@RequestParam(value = "toDate", required = false) String toDate,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streetid", required = false) String streetid,
			@RequestParam(value = "loginId", required = false) String loginId
			){
		return activity.dashboardFiletReport(assetTypeId, fromDate, toDate, zone, ward, streetid, loginId);
	}
	
	@GetMapping("/sendwhatsapp")
    public String fetchApi() {
		String url="https://media.smsgupshup.com/GatewayAPI/rest?userid=2000233507&password=h2YjFNcJ&send_to=7845014181&v=1.1&format=json&msg_type=TEXT&method=SENDMESSAGE&msg=Welcome to GCC%21+%0A%0AYour+OTP+for+GCC+Community+Centre+is+36837+.";
		System.out.println(activity.getApiResponse());
        return "test";
    }
	
	@GetMapping(value="/asset/encroachment_vendor_type")
	public List<?> encroachment_vendor_type(){
		return activity.encroachment_vendor_type();
	}
	
	@GetMapping(value="/asset/encroachment_vendor_items")
	public List<?> encroachment_vendor_items(){
		return activity.encroachment_vendor_items();
	}
	
}
