package in.gov.chennaicorporation.mobileservice.pump.controller;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.pump.service.PumpsService;

@RequestMapping("/gccofficialapp/api/pump/")
@RestController("gccofficialappspumps")
public class PumpsController {
	
	@Autowired
    private PumpsService pumpsService;
	
	@GetMapping(value="/getPumpListForMap")
	public List<?> getPumpListFormap(
			@RequestParam(value="loginId", required = true) String cby
			){
		return pumpsService.getPumpListForMap(cby);
	}
	
	@GetMapping(value="/getPumpList")
	public List<?> getPumpList(
			@RequestParam(value="loginId", required = true) String cby
			){
		return pumpsService.getPumpList(cby);
	}
	 
    @PostMapping("/savePump")
 	public List<?> savePump(
             @RequestParam(value = "zone", required = true) String zone,
             @RequestParam(value = "ward", required = true) String ward,
             @RequestParam(value = "street_name", required = false) String street_name,
             @RequestParam(value = "street_id", required = false) String street_id,
             @RequestParam(value = "latitude", required = true) String latitude,
             @RequestParam(value = "longitude", required = true) String longitude,
             @RequestParam(value = "loginId", required = true) String loginId,
             @RequestParam(value = "file", required = true) MultipartFile file
     ) {
 		return pumpsService.savePump(
                 zone,
                 ward,
                 street_name,
                 street_id,
                 latitude,
                 longitude,
                 loginId,
                 file
         );
     }
	 
	@PostMapping("/saveActivity")
	public List<?> saveActivity(
            @RequestParam(value = "Pump_id", required = true) String Pump_id,
            @RequestParam(value = "file", required = true) MultipartFile file,
            @RequestParam(value = "video", required = true) MultipartFile video,
            @RequestParam(value = "zone", required = true) String zone,
            @RequestParam(value = "ward", required = true) String ward,
            @RequestParam(value = "street_name", required = false) String street_name,
            @RequestParam(value = "street_id", required = false) String street_id,
            @RequestParam(value = "latitude", required = true) String latitude,
            @RequestParam(value = "longitude", required = true) String longitude,
            @RequestParam(value = "loginId", required = true) String loginId
    ) {
		return pumpsService.saveActivity(
				Pump_id,
                file,
                video,
                zone,
                ward,
                street_name,
                street_id,
                latitude,
                longitude,
                loginId,
                "before"
        );
    }

	@GetMapping(value="/getZoneSummary")
	public List<?> getZoneSummary(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate
			){
		return pumpsService.getZoneSummary(fromDate, toDate);
	}
	
	@GetMapping(value="/getWardSummary")
	public List<?> getWardSummary(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="zone", required = true) String zone
			){
		return pumpsService.getWardSummary(fromDate, toDate, zone);
	}
	
	@GetMapping(value="/getPumpSummary")
	public List<?> getPumpSummary(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="ward", required = true) String ward
			){
		return pumpsService.getPumpSummary(fromDate, toDate, ward);
	}
}
