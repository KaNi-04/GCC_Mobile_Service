package in.gov.chennaicorporation.mobileservice.viewCutter.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.viewCutter.service.viewCutterActivity;


@RequestMapping("/gccofficialapp/api/viewcutter/")
@RestController("viewCutterRest")
public class viewCutterController {
	@Autowired
	private viewCutterActivity viewCutterActivity;

	@PostMapping(value="/saveViewCutterData")
	public List<?> saveViewCutterData(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value="latitude", required = true) String latitude,
			@RequestParam(value="longitude", required = true) String longitude,
			@RequestParam(value="zone", required = true) String zone,
			@RequestParam(value="ward", required = true) String ward,
			@RequestParam(value="streetname", required = false) String streetname,
			@RequestParam(value="streetid", required = false) String streetid,
			@RequestParam(value="remarks", required = true) String remarks,
			
			@RequestParam(value="file", required = false) MultipartFile file
			){
		return viewCutterActivity.saveViewCutterData(cby, zone, ward, streetid, streetname, latitude, longitude, remarks, file);
	}
	
	@GetMapping(value="/getViewCutterList")
	public List<?> getViewCutterList(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "loginid", required = false) String loginid){
		return viewCutterActivity.getViewCutterList(formData, latitude, longitude, loginid);
	}
	
	@GetMapping(value="/getViewCutterCompletedList")
	public List<?> getViewCutterCompletedList(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "latitude", required = false) String latitude, 
			@RequestParam(value = "longitude", required = false) String longitude,
			@RequestParam(value = "loginid", required = true) String loginid){
		return viewCutterActivity.getViewCutterCompletedList(formData, latitude, longitude, loginid);
	}
	
	@GetMapping(value="/getStageList") // Old App
	public List<?> getQuestionsList(@RequestParam(value="loginId", required = false) String cby){
		return viewCutterActivity.getStageList();
	}
	
	@GetMapping(value="/getViewCutterDetails")
	public List<?> getViewCutterDetails(
			@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value="id", required = true) String id,
			@RequestParam(value="latitude", required = true) String latitude,
			@RequestParam(value="longitude", required = true) String longitude,
			@RequestParam(value="loginid", required = false) String loginid,
			@RequestParam(value="streetid", required = true) String streetid,
			@RequestParam(value="undertaken_condition", required = false) String undertaken_condition){
		return viewCutterActivity.getViewCutterDetails(formData,id,loginid, latitude, longitude, undertaken_condition, streetid);
	}
	
	@PostMapping(value="/saveStageActivityData")
	public List<?> saveStageActivityData(
			@RequestParam(value="id", required = true) String id,
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
			@RequestParam(value="undertaken", required = false) String undertaken,
			
			@RequestParam(value="stagefile", required = false) MultipartFile stage_file
			){
		return viewCutterActivity.saveStageActivityData(id, cby, latitude, longitude, zone, ward, 
				streetname, streetid, stageid, remarks, weightage, stage_file, undertaken);
	}
	
	// Reports
	
		@GetMapping(value="/getZoneList")
		public List<?> getZoneList(){
			return viewCutterActivity.getZoneList();
		}
		
		@GetMapping(value="/getWardList")
		public List<?> getWardList(@RequestParam(value="zone", required = true) String zone){
			return viewCutterActivity.getWardList(zone);
		}
		
		@GetMapping(value="/getUndertakenList")
		public List<?> getUndertakenList(@RequestParam(value="ward", required = true) String ward){
			return viewCutterActivity.getUndertakenList(ward);
		}
		
		@GetMapping(value="/getNotUndertakenList")
		public List<?> getNotUndertakenList(@RequestParam(value="ward", required = true) String ward){
			return viewCutterActivity.getNotUndertakenList(ward);
		}
		
		@GetMapping(value="/getWorkPendingList")
		public List<?> getWorkPendingList(@RequestParam(value="ward", required = true) String ward){
			return viewCutterActivity.getWorkPendingList(ward);
		}
		
		@GetMapping(value="/getWorkCompletedList")
		public List<?> getWorkCompletedList(@RequestParam(value="ward", required = true) String ward){
			return viewCutterActivity.getWorkCompletedList(ward);
		}
		
		@GetMapping(value="/getReportViewCutterDetails")
		public List<?> getReportViewCutterDetails(
				@RequestParam MultiValueMap<String, String> formData,
				@RequestParam(value="id", required = true) String id,
				@RequestParam(value="latitude", required = true) String latitude,
				@RequestParam(value="longitude", required = true) String longitude,
				@RequestParam(value="loginid", required = false) String loginid,
				@RequestParam(value="undertaken_condition", required = false) String undertaken_condition){
			return viewCutterActivity.getReportViewCutterDetails(formData,id,loginid, latitude, longitude, undertaken_condition);
		}
}
