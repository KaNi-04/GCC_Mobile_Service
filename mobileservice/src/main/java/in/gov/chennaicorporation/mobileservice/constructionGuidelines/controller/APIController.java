package in.gov.chennaicorporation.mobileservice.constructionGuidelines.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.constructionGuidelines.service.GuidelinesService;

@RequestMapping("/gccofficialapp/api/constructionguidelines/")
@RestController("gccofficialappsconstructionguidelines")
public class APIController {
	@Autowired
	private GuidelinesService guidelinesService;
	
	@GetMapping(value="/getConstructionList")
	public List<?> getToiletsList(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value = "latitude", required = true) String latitude,
	        @RequestParam(value = "longitude", required = true) String longitude
			){
		return guidelinesService.getConstructionList(cby,latitude,longitude);
	}
	
	@GetMapping(value="/getUnitOfficeList")
	public List<?> getUnitOfficeList(
			@RequestParam(value="zone", required = true) String zone,
			@RequestParam(value="ward", required = true) String ward
			){
		return guidelinesService.getUnitOfficeList(zone,ward);
	}
	
	@GetMapping(value="/getBuildingType")
	public List<?> getBuildingTypeList(){
		return guidelinesService.getBuildingTypeList();
	}
	
	@PostMapping(value="/saveDetails")
	public List<Map<String, Object>> saveConstructionDetails(
			@RequestParam(value = "contractor_name", required = true) String contractorName,
	        @RequestParam(value = "contractor_mobile", required = true) String contractorMobile,
	        @RequestParam(value = "buildup_area", required = true) String buildupArea,
	        @RequestParam(value = "building_type", required = true) String buildingType,
	        @RequestParam(value = "high_rise", required = true) String highRise,
	        @RequestParam(value = "zone", required = true) String zone,
	        @RequestParam(value = "ward", required = true) String ward,
	        @RequestParam(value = "unit", required = true) String unit,
	        @RequestParam(value = "under_guidelines", required = true) String underGuidelines,
	        @RequestParam(value = "site_address", required = true) String siteAddress,
	        @RequestParam(value = "cby", required = true) String cby,
	        @RequestParam(value = "latitude", required = true) String latitude,
	        @RequestParam(value = "longitude", required = true) String longitude,
	        @RequestParam(value = "mainfile", required = true) MultipartFile mainFile
			){
		return guidelinesService.saveConstructionDetails(
	            contractorName, contractorMobile, buildupArea, buildingType, highRise,
	            zone, ward, unit, underGuidelines, siteAddress, cby, latitude, longitude, mainFile
	        );
	}
	
	@GetMapping(value="/getguidelines")
	public List<?> getGuidelinesList(){
		return guidelinesService.getGuidelinesList();
	}
	
	@PostMapping(value="/saveFeedback")
	public List<?> saveGuidelinesFeedback(
			@RequestParam(value = "cdid", required = true) String cdid,
	        @RequestParam(value = "ciid", required = true) String ciid,
	        @RequestParam(value = "cby", required = true) String cby,
	        @RequestParam(value = "latitude", required = true) String latitude,
	        @RequestParam(value = "longitude", required = true) String longitude,
	        @RequestParam(value = "zone", required = true) String zone,
	        @RequestParam(value = "ward", required = true) String ward,
	        @RequestParam(value = "q1", required = true) String q1,
	        @RequestParam(value = "q2", required = true) String q2,
	        @RequestParam(value = "q3", required = true) String q3,
	        @RequestParam(value = "q4", required = true) String q4,
	        @RequestParam(value = "q5", required = true) String q5,
	        @RequestParam(value = "q6", required = true) String q6,
	        @RequestParam(value = "q7", required = true) String q7,
	        @RequestParam(value = "q8", required = true) String q8,
	        @RequestParam(value = "q9", required = true) String q9,
	        @RequestParam(value = "q10", required = true) String q10,
	        @RequestParam(value = "q11", required = true) String q11,
	        @RequestParam(value = "q12", required = true) String q12,
	        @RequestParam(value = "q13", required = true) String q13,
	        @RequestParam(value = "q14", required = true) String q14,
	        @RequestParam(value = "q15", required = true) String q15,
	        @RequestParam(value = "q16", required = true) String q16,
	        @RequestParam(value = "q17", required = true) String q17,
	        @RequestParam(value = "q18", required = true) String q18,
	        @RequestParam(value = "image1", required = true) MultipartFile image1,
	        @RequestParam(value = "image2", required = true) MultipartFile image2,
	        @RequestParam(value = "image3", required = true) MultipartFile image3,
	        @RequestParam(value = "image4", required = true) MultipartFile image4,
	        @RequestParam(value = "image5", required = true) MultipartFile image5
	){
		return guidelinesService.saveGuidlinesFeedback(
	            cdid, ciid, cby, latitude, longitude, zone, ward,
	            q1, q2, q3, q4, q5, q6, q7, q8, q9, q10,
	            q11, q12, q13, q14, q15, q16, q17, q18,
	            image1, image2, image3, image4, image5
	        );
	}
	
	@GetMapping(value="/getRevisitConstructionList")
	public List<?> getRevisitConstructionList(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value = "latitude", required = true) String latitude,
	        @RequestParam(value = "longitude", required = true) String longitude
			){
		return guidelinesService.getRevisitConstructionList(cby,latitude,longitude);
	}
	
	@GetMapping(value="/getRevisitConstructionListWithFeedback")
	public List<?> getRevisitConstructionListWithFeedback(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value = "cdid", required = true) String cdid,
	        @RequestParam(value = "ciid", required = true) String ciid
			){
		return guidelinesService.getRevisitConstructionListWithFeedback(cby,cdid,ciid);
	}
	
	@GetMapping(value="/get7thRevisitConstructionList")
	public List<?> get7RevisitConstructionList(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value = "latitude", required = true) String latitude,
	        @RequestParam(value = "longitude", required = true) String longitude
			){
		return guidelinesService.getRevisitConstructionList(cby,latitude,longitude);
	}
	
	@PostMapping(value = "/saveAfterNoticeDetails")
	public List<Map<String, Object>> saveAfterNoticeDetails(
	        @RequestParam(value = "cdid", required = true) String cdid,
	        @RequestParam(value = "ciid", required = true) String ciid,
	        @RequestParam(value = "giid", required = true) String giid,
	        @RequestParam(value = "remarks", required = true) String remarks,
	        @RequestParam(value = "status", required = true) String status,
	        @RequestParam(value = "zone", required = true) String zone,
	        @RequestParam(value = "ward", required = true) String ward,
	        @RequestParam(value = "loginId", required = true) String cby,
	        @RequestParam(value = "latitude", required = true) String latitude,
	        @RequestParam(value = "longitude", required = true) String longitude,
	        @RequestParam(value = "mainfile", required = true) MultipartFile mainfile
	) {
	    return guidelinesService.saveAfterNoticeDetails(
	            cdid, ciid, giid, remarks, status, zone, ward, cby, latitude, longitude, mainfile
	    );
	}
	
	@PostMapping(value = "/saveFinalNoticeDetails")
	public List<Map<String, Object>> saveFinalNoticeDetails(
	        @RequestParam(value = "cdid", required = true) String cdid,
	        @RequestParam(value = "ciid", required = true) String ciid,
	        @RequestParam(value = "giid", required = true) String giid,
	        @RequestParam(value = "remarks", required = true) String remarks,
	        @RequestParam(value = "status", required = true) String status,
	        @RequestParam(value = "zone", required = true) String zone,
	        @RequestParam(value = "ward", required = true) String ward,
	        @RequestParam(value = "loginId", required = true) String cby,
	        @RequestParam(value = "latitude", required = true) String latitude,
	        @RequestParam(value = "longitude", required = true) String longitude,
	        @RequestParam(value = "mainfile", required = true) MultipartFile mainfile
	) {
	    return guidelinesService.saveFinalNoticeDetails(
	            cdid, ciid, giid, remarks, status, zone, ward, cby, latitude, longitude, mainfile
	    );
	}
}
