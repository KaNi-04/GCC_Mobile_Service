package in.gov.chennaicorporation.mobileservice.busshelters.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.busshelters.service.BusShelterRenovationActivity;

@RequestMapping("/gccofficialapp/api/busshelter/renovation/")
@RestController("busshelterRenovationRest")
public class BusShelterController_renovation {
	@Autowired
    private JdbcTemplate jdbcBusShelterTemplate;
	@Autowired
	private BusShelterRenovationActivity busShelterRenovationActivity;
	
	@GetMapping(value="/getBusShelterList")
	public List<?> getBusShelterList(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "loginid", required = false) String loginid){
		return busShelterRenovationActivity.getBusShelterList(formData, latitude, longitude, loginid);
	}
	
	@GetMapping(value="/getBusCompletedShelterList")
	public List<?> getBusCompletedShelterList(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "latitude", required = false) String latitude, 
			@RequestParam(value = "longitude", required = false) String longitude,
			@RequestParam(value = "loginid", required = true) String loginid){
		return busShelterRenovationActivity.getBusCompletedShelterList(formData, latitude, longitude, loginid);
	}
	
	@GetMapping(value="/getStageList") // Old App
	public List<?> getQuestionsList(@RequestParam(value="loginId", required = false) String cby){
		return busShelterRenovationActivity.getStageList();
	}
	
	@GetMapping(value="/getBusShelterDetails")
	public List<?> getBusShelterDetails(
			@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value="id", required = true) String id,
			@RequestParam(value="latitude", required = true) String latitude,
			@RequestParam(value="longitude", required = true) String longitude,
			@RequestParam(value="loginid", required = false) String loginid,
			@RequestParam(value="renovation_required_condition", required = false) String renovation_required_condition){
		return busShelterRenovationActivity.getBusShelterDetails(formData,id,loginid, latitude, longitude, renovation_required_condition);
	}
	
	@PostMapping(value="/saveStageActivityData")
	public List<?> saveStageActivityData(
			@RequestParam(value="shelter_id", required = true) String shelter_id,
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value="latitude", required = true) String latitude,
			@RequestParam(value="longitude", required = true) String longitude,
			@RequestParam(value="zone", required = true) String zone,
			@RequestParam(value="ward", required = true) String ward,
			@RequestParam(value="streetname", required = false) String streetname,
			@RequestParam(value="streetid", required = false) String streetid,
			@RequestParam(value="stageid", required = true) String stageid,
			@RequestParam(value="remarks", required = true) String remarks,
			@RequestParam(value="weightage", required = true) String weightage,
			@RequestParam(value="renovation_required", required = false) String renovation_required,
			
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value="stagefile", required = false) MultipartFile stage_file
			){
		return busShelterRenovationActivity.saveStageActivityData(shelter_id, cby, latitude, longitude, zone, ward, 
				streetname, streetid, stageid, remarks, weightage, file, stage_file, renovation_required);
	}
	
	// Reports
	
	@GetMapping(value="/getZoneList")
	public List<?> getZoneList(){
		return busShelterRenovationActivity.getZoneList();
	}
	
	@GetMapping(value="/getWardList")
	public List<?> getWardList(@RequestParam(value="zone", required = true) String zone){
		return busShelterRenovationActivity.getWardList(zone);
	}
	
	@GetMapping(value="/getVistPendingList")
	public List<?> getVistPendingList(@RequestParam(value="ward", required = true) String ward){
		return busShelterRenovationActivity.getVistPendingList(ward);
	}
	
	@GetMapping(value="/getRenovationRequiredList")
	public List<?> getRenovationRequiredList(@RequestParam(value="ward", required = true) String ward){
		return busShelterRenovationActivity.getRenovationRequiredList(ward);
	}
	
	@GetMapping(value="/getRenovationNotRequiredList")
	public List<?> getRenovationNotRequiredList(@RequestParam(value="ward", required = true) String ward){
		return busShelterRenovationActivity.getRenovationNotRequiredList(ward);
	}
	
	@GetMapping(value="/getWorkPendingList")
	public List<?> getWorkPendingList(@RequestParam(value="ward", required = true) String ward){
		return busShelterRenovationActivity.getWorkPendingList(ward);
	}
	
	@GetMapping(value="/getWorkCompletedList")
	public List<?> getWorkCompletedList(@RequestParam(value="ward", required = true) String ward){
		return busShelterRenovationActivity.getWorkCompletedList(ward);
	}
}
