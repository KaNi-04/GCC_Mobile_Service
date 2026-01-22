package in.gov.chennaicorporation.mobileservice.toilets.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.toilets.service.activity;

@RequestMapping("/gccofficialapp/api/toilets/")
@RestController("gccofficialappstoilets")
public class APIController {
	@Autowired
    private activity activity;
	
	@GetMapping(value="/getToiletsList")
	public List<?> getToiletsList(@RequestParam(value="loginId", required = true) String cby){
		return activity.getToiletsList(cby);
	}
	
	@GetMapping("/toiletlistRest")
    public List<?> toiletlistRest(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude) {
        return activity.toiletlistRest(formData, latitude, longitude);
    }
	
	@PostMapping(value="/updateToiletLocation")
	public List<?> updateToiletLocation(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value="toilet_id", required = true) String toilet_id,
			@RequestParam(value="latitude", required = true) String latitude,
			@RequestParam(value="longitude", required = true) String longitude,
			@RequestParam(value = "file", required = false) MultipartFile filedata
			){
		return activity.updateToiletLocation(toilet_id,latitude, longitude, filedata);
	}
	
	@GetMapping("/getToiletsListWithButton")
    public List<?> getToiletsListWithButton(
			@RequestParam(value = "loginId", required = false) String cby) {
        return activity.getToiletsListWithButton(cby);
    }
	
	@GetMapping(value="/getQuestionsList") // Old App
	public List<?> getQuestionsList(@RequestParam(value="loginId", required = false) String cby){
		return activity.getQuestionsList();
	}
	
	@GetMapping(value="/getParentQuestionsList")
	public List<?> getParentQuestionsList(@RequestParam(value="loginId", required = false) String cby){
		return activity.getParentQuestionsList();
	}
	
	@GetMapping(value="/getChildQuestionsList")
	public List<?> getChildQuestionsList(
			@RequestParam(value="loginId", required = false) String cby,
			@RequestParam(value="pid", required = true) String pid
			){
		return activity.getChildQuestionsList(pid);
	}
	
	@PostMapping(value="/addNewToilet")
	public List<?> addNewToilet(
			@RequestParam(value="latitude", required = true) String latitude,
			@RequestParam(value="longitude", required = true) String longitude,
			@RequestParam(value="zone", required = true) String zone,
			@RequestParam(value="ward", required = true) String ward,
			@RequestParam(value="locality", required = true) String locality,
			@RequestParam(value="name", required = true) String name,
			@RequestParam(value="loginId", required = false) String cby,
			@RequestParam(value = "file", required = false) MultipartFile filedata
			){
		return activity.addNewToilet(latitude, longitude, zone, ward, name, locality, cby, filedata);
	}
	
	@GetMapping("/toiletlistbylatlong")
    public List<?> toiletlistbylatlong(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "assetid", required = true) String id,
			@RequestParam(value = "loginId", required = false) String loginId) {
        return activity.toiletlistbylatlong(formData, latitude, longitude,id,loginId);
    }
	
	@PostMapping(value="/saveFeedback")
	public List<?> saveFeedback(
			@RequestParam(value="toilet_id", required = true) String toilet_id,
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value="latitude", required = true) String latitude,
			@RequestParam(value="longitude", required = true) String longitude,
			@RequestParam(value="zone", required = true) String zone,
			@RequestParam(value="ward", required = true) String ward,
			@RequestParam(value="q_1", required = false) String q_1,
			@RequestParam(value="q_2", required = false) String q_2,
			@RequestParam(value="q_3", required = false) String q_3,
			@RequestParam(value="q_4", required = false) String q_4,
			@RequestParam(value="q_5", required = false) String q_5,
			@RequestParam(value="q_6", required = false) String q_6,
			@RequestParam(value="q_7", required = false) String q_7,
			@RequestParam(value="q_8", required = false) String q_8,
			@RequestParam(value="q_9", required = false) String q_9,
			@RequestParam(value="q_10", required = false) String q_10,
			@RequestParam(value="q_11", required = false) String q_11,
			@RequestParam(value="remarks", required = false) String remarks,
			@RequestParam(value = "file", required = true) MultipartFile filedata,
			
			@RequestParam(value="file_1", required = false) MultipartFile file_1,
			@RequestParam(value="file_2", required = false) MultipartFile file_2,
			@RequestParam(value="file_3", required = false) MultipartFile file_3,
			@RequestParam(value="file_4", required = false) MultipartFile file_4,
			@RequestParam(value="file_5", required = false) MultipartFile file_5,
			@RequestParam(value="file_6", required = false) MultipartFile file_6,
			@RequestParam(value="file_7", required = false) MultipartFile file_7,
			@RequestParam(value="file_8", required = false) MultipartFile file_8,
			@RequestParam(value="file_9", required = false) MultipartFile file_9,
			@RequestParam(value="file_10", required = false) MultipartFile file_10,
			@RequestParam(value="file_11", required = false) MultipartFile file_11
			){
		return activity.saveFeedback(toilet_id, cby, latitude, longitude, zone, ward, 
				q_1, q_2, q_3, q_4, q_5, q_6, q_7, q_8, q_9, q_10, q_11, remarks, filedata,
				file_1, file_2, file_3, file_4, file_5, file_6, file_7, file_8, file_9, file_10, file_11);
	}
	
	@GetMapping(value="/zoneReport")
	public List<?> zoneReport(
			@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="loginId", required = false) String cby
			){
		return activity.zoneReport(formData,fromDate,toDate,cby);
	}
	
	@GetMapping(value="/wardReport")
	public List<?> wardReport(
			@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="zone", required = true) String zone,
			@RequestParam(value="loginId", required = false) String cby
			){
		return activity.wardReport(formData,fromDate,toDate,zone,cby);
	}
	
	@GetMapping(value="/toiletReport")
	public List<?> toiletReport(
			@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="ward", required = true) String ward,
			@RequestParam(value="loginId", required = false) String cby
			){
		return activity.toiletReport(formData,fromDate,toDate,ward,cby);
	}
	
	@GetMapping(value="/feedbackReport")
	public List<?> feedbackReport(
			@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="type", required = true) String type,
			@RequestParam(value="toilet_id", required = true) String id,
			@RequestParam(value="loginId", required = false) String cby
			){
		return activity.feedbackReport(formData,fromDate,toDate,type,id,cby);
	}
}
