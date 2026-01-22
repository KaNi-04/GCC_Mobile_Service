package in.gov.chennaicorporation.mobileservice.sluice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.sluice.service.SluiceService;

@RequestMapping("/gccofficialapp/api/sluicepoint/")
@RestController("gccofficialappssluicepoint")
public class SluiceController {
	
	@Autowired
    private SluiceService sluiceService;
	
	@GetMapping(value="/getSluicePointListForMap")
	public List<?> getSluicePointListFormap(
			@RequestParam(value="loginId", required = true) String cby
			){
		return sluiceService.getSluicePointListForMap(cby);
	}
	
	@GetMapping(value="/getSluicePointList")
	public List<?> getSluicePointList(
			@RequestParam(value="loginId", required = true) String cby
			){
		return sluiceService.getSluicePointList(cby);
	}
	 
    @PostMapping("/saveSluicePoint")
 	public List<?> saveSluicePoint(
             @RequestParam(value = "zone", required = true) String zone,
             @RequestParam(value = "ward", required = true) String ward,
             @RequestParam(value = "street_name", required = false) String street_name,
             @RequestParam(value = "street_id", required = false) String street_id,
             @RequestParam(value = "latitude", required = true) String latitude,
             @RequestParam(value = "longitude", required = true) String longitude,
             @RequestParam(value = "loginId", required = true) String loginId,
             @RequestParam(value = "file", required = true) MultipartFile file
     ) {
 		return sluiceService.saveSluicePoint(
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
            @RequestParam(value = "sluicepoint_id", required = true) String sluicepoint_id,
            @RequestParam(value = "rust_painting", required = true) String rust_painting,
            @RequestParam(value = "greasing", required = true) String greasing,
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
		return sluiceService.saveActivity(
				sluicepoint_id,
				rust_painting,
				greasing,
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
		return sluiceService.getZoneSummary(fromDate, toDate);
	}
	
	@GetMapping(value="/getWardSummary")
	public List<?> getWardSummary(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="zone", required = true) String zone
			){
		return sluiceService.getWardSummary(fromDate, toDate, zone);
	}
	
	@GetMapping(value="/getSluicePointSummary")
	public List<?> getSluicePointSummary(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="ward", required = true) String ward
			){
		return sluiceService.getSluicePointSummary(fromDate, toDate, ward);
	}
}
