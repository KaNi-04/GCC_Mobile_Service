package in.gov.chennaicorporation.mobileservice.railwayCulverts.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.railwayCulverts.service.CulvertsService;

@RequestMapping("/gccofficialapp/api/railwayculvert/")
@RestController("gccofficialappsrailwayculvert")
public class CulvertsController {
	
	@Autowired
    private CulvertsService culvertsService;
	
	@GetMapping(value="/getCulvertPointListForMap")
	public List<?> getCulvertPointListFormap(
			@RequestParam(value="loginId", required = true) String cby
			){
		return culvertsService.getCulvertPointListForMap(cby);
	}
	
	@GetMapping(value="/getCulvertPointList")
	public List<?> getCulvertPointList(
			@RequestParam(value="loginId", required = true) String cby
			){
		return culvertsService.getCulvertPointList(cby);
	}
	 
    @PostMapping("/saveCulvertPoint")
 	public List<?> saveCulvertPoint(
             @RequestParam(value = "zone", required = true) String zone,
             @RequestParam(value = "ward", required = true) String ward,
             @RequestParam(value = "street_name", required = false) String street_name,
             @RequestParam(value = "street_id", required = false) String street_id,
             @RequestParam(value = "latitude", required = true) String latitude,
             @RequestParam(value = "longitude", required = true) String longitude,
             @RequestParam(value = "loginId", required = true) String loginId,
             @RequestParam(value = "file", required = true) MultipartFile file
     ) {
 		return culvertsService.saveCulvertPoint(
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
            @RequestParam(value = "culvert_id", required = true) String culvert_id,
            //@RequestParam(value = "rust_painting", required = true) String rust_painting,
            //@RequestParam(value = "greasing", required = true) String greasing,
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
		return culvertsService.saveActivity(
				culvert_id,
				//rust_painting,
				//greasing,
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
		return culvertsService.getZoneSummary(fromDate, toDate);
	}
	
	@GetMapping(value="/getWardSummary")
	public List<?> getWardSummary(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="zone", required = true) String zone
			){
		return culvertsService.getWardSummary(fromDate, toDate, zone);
	}
	
	@GetMapping(value="/getCulvertPointSummary")
	public List<?> getCulvertPointSummary(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="ward", required = true) String ward
			){
		return culvertsService.getCulvertPointSummary(fromDate, toDate, ward);
	}
	
	@PostMapping(value="/saveWhatsAppResponse")
	public List<?> saveWhatsAppResponse(
			@RequestParam(value="mobilenumber", required = true) String mobilenumber,
			@RequestParam(value="response", required = true) String response,
			@RequestParam(value="event", required = true) String event
			){
		return culvertsService.saveWhatsAppResponse(mobilenumber, response, event);
	}
	
}
