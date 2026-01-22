package in.gov.chennaicorporation.mobileservice.speedbrake.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.speedbrake.service.APIService;

@RequestMapping("/gccofficialapp/api/speedbrake/")
@RestController("gccofficialappsspeedbrake")
public class APIController {
	@Autowired
	private APIService apiService;
	
	@GetMapping(value="/getQuestions")
	public List<?> getQuestions(){
		return apiService.getQuestions();
	}
	
	@GetMapping(value="/checkAssetExists")
	public List<?> checkAssetExists(
			@RequestParam(value = "latitude", required = true) String latitude,
	        @RequestParam(value = "longitude", required = true) String longitude
			){
		return apiService.checkAssetExists(latitude,longitude);
	}
	
	@PostMapping(value="/saveSpeedBrakeDetails")
	public List<Map<String, Object>> saveConstructionDetails(
			@RequestParam(value = "zone", required = true) String zone,
	        @RequestParam(value = "ward", required = true) String ward,
	        @RequestParam(value = "streetid", required = true) String streetid,
	        @RequestParam(value = "streetname", required = true) String streetname,
	        @RequestParam(value = "streettype", required = true) String streettype,
	        @RequestParam(value = "roadtype", required = true) String roadtype,
	        @RequestParam(value = "marktype", required = true) String marktype,
	        @RequestParam(value = "q1", required = false) String q1,
	        @RequestParam(value = "q2", required = false) String q2,
	        @RequestParam(value = "q3", required = false) String q3,
	        @RequestParam(value = "cby", required = true) String cby,
	        @RequestParam(value = "latitude", required = true) String latitude,
	        @RequestParam(value = "longitude", required = true) String longitude,
	        @RequestParam(value = "sb_condition", required = false) String sb_condition,
	        @RequestParam(value = "mainfile", required = true) MultipartFile mainFile
			){
		return apiService.saveSpeedBrakeDetails(
				zone, ward, streetid, streetname, streettype,
				roadtype, marktype, q1, q2, q3, cby, latitude, longitude, sb_condition, mainFile
	        );
	}
	
	// 1. Zone-wise report
    @GetMapping("/zone-wise")
    public List<Map<String, Object>> getZoneWiseReport() {
        return apiService.getZoneWiseReport();
    }

    // 2. Ward-wise report by zone
    @GetMapping("/ward-wise")
    public List<Map<String, Object>> getWardWiseReport(@RequestParam String zone) {
        return apiService.getWardeWiseReport(zone);
    }

    // 3. Street-wise report (New) by ward
    @GetMapping("/new-street-wise")
    public List<Map<String, Object>> getNewStreetWiseReport(@RequestParam String ward) {
        return apiService.getNewStreetWiseReport(ward);
    }

    // 4. Street-wise report (Existing) by ward
    @GetMapping("/existing-street-wise")
    public List<Map<String, Object>> getExistingStreetWiseReport(@RequestParam String ward) {
        return apiService.getExistingStreetWiseReport(ward);
    }

    // 5. Detailed Speedbrake Entry with Q&A
    @GetMapping("/details")
    public List<Map<String, Object>> getSpeedbrakeDetails(
            @RequestParam String ward,
            @RequestParam String streetname,
            @RequestParam String marktype,
            @RequestParam(required = false) String sb_condition
    ) {
        return apiService.getSpeedbrakeDetailsWithQuestions(ward, marktype, sb_condition, streetname);
    }
}
