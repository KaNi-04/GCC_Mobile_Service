package in.gov.chennaicorporation.mobileservice.thooimaiMission.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.gov.chennaicorporation.mobileservice.thooimaiMission.service.ThooimaiMissionService;

@RequestMapping("/gccofficialapp/thooimaimission/api")
@RestController("thooimaimissionRestAPI")
public class ThooimaiMissionController {

	@Autowired
	ThooimaiMissionService thooimaiMissionService;
	
	@GetMapping("/getWasteTypeList")
    public List<?> getWasteTypeList() {
        return thooimaiMissionService.getWasteTypeList();
    }
	
	@PostMapping(value="/saveWaste") 
	public List<?> saveWaste(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "address", required = true) String address,
			@RequestParam(value = "place_name", required = true) String place_name,
			@RequestParam(value = "tonage", required = true) String tonage,
			@RequestParam(value = "wastetype", required = false) String type,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestParam(value = "file", required = true) MultipartFile file
			) {
		return thooimaiMissionService.saveWasteData(zone,ward,cby,latitude,longitude,address,place_name,tonage,remarks,file);
	}
	
	@GetMapping("/getComplaintList")
    public List<?> getComplaintList(@RequestParam(value = "loginId", required = true) String loginId) {
        return thooimaiMissionService.getComplaintList(loginId);
    }
	/*
	@PostMapping(value="/saveClose") 
	public List<?> saveClose(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "wlid", required = true) String wlid,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "file", required = true) MultipartFile file,
			@RequestParam(value = "remarks", required = true) String remarks,
			@RequestParam(value = "tonage", required = true) String tonage,
			@RequestPart("wastetype") List<Map<String, String>> wastetype,
			@RequestParam(value = "loginId", required = true) String cby
			) {
		return thooimaiMissionService.saveClose(wlid,zone,ward,latitude,longitude,file,remarks,wastetype,cby);
	}
	*/
	@PostMapping(value="/saveClose") 
	public List<?> saveClose(
	        @RequestParam("wlid") String wlid,
	        @RequestParam("zone") String zone,
	        @RequestParam("ward") String ward,
	        @RequestParam("latitude") String latitude,
	        @RequestParam("longitude") String longitude,
	        @RequestParam("file") MultipartFile file,
	        @RequestParam("remarks") String remarks,
	        @RequestParam("tonage") String tonage,

	        // 👇 JSON STRING
	        @RequestParam("wastetype") String wastetypeJson,

	        @RequestParam("loginId") String cby
	) throws Exception {

	    ObjectMapper mapper = new ObjectMapper();
	    List<Map<String, String>> wastetype =
	            mapper.readValue(wastetypeJson, new TypeReference<>() {});

	    return thooimaiMissionService.saveClose(
	            wlid, zone, ward, latitude, longitude,
	            file, remarks, wastetype, cby
	    );
	}
	
	// Reports
	
	@GetMapping("/getZoneSummary")
    public List<?> getZoneSummary(
    		@RequestParam(value = "loginId", required = true) String loginId,
    		@RequestParam(value = "fromDate", required = true) String fromDate,
    		@RequestParam(value = "toDate", required = true) String toDate
    		) {
        return thooimaiMissionService.getZoneSummary(fromDate,toDate);
    }
	
	@GetMapping("/getWardSummary")
    public List<?> getWardSummary(
    		@RequestParam(value = "loginId", required = true) String loginId,
    		@RequestParam(value = "zone", required = true) String zone,
    		@RequestParam(value = "fromDate", required = true) String fromDate,
    		@RequestParam(value = "toDate", required = true) String toDate
    		) {
        return thooimaiMissionService.getWardSummary(zone,fromDate,toDate);
    }
	
	@GetMapping("/getPendingSummary")
    public List<?> getPendingSummary(
    		@RequestParam(value = "loginId", required = true) String loginId,
    		@RequestParam(value = "ward", required = true) String ward,
    		@RequestParam(value = "fromDate", required = true) String fromDate,
    		@RequestParam(value = "toDate", required = true) String toDate
    		) {
        return thooimaiMissionService.getPendingSummary(ward,fromDate,toDate);
    }
	
	@GetMapping("/getCloseSummary")
    public List<?> getCloseSummary(
    		@RequestParam(value = "loginId", required = true) String loginId,
    		@RequestParam(value = "ward", required = true) String ward,
    		@RequestParam(value = "fromDate", required = true) String fromDate,
    		@RequestParam(value = "toDate", required = true) String toDate
    		) {
        return thooimaiMissionService.getCloseSummary(ward,fromDate,toDate);
    }
	
}
