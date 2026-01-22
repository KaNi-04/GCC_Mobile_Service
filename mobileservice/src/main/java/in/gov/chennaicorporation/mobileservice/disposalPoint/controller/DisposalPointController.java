package in.gov.chennaicorporation.mobileservice.disposalPoint.controller;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.disposalPoint.service.DisposalPointService;

@RequestMapping("/gccofficialapp/api/disposalpoint/")
@RestController("gccofficialappsdisposalpoint")
public class DisposalPointController {
	
	@Autowired
    private DisposalPointService disposalPointService;
	
	@GetMapping(value="/getDisposalPointListForMap")
	public List<?> getDisposalPointListFormap(
			@RequestParam(value="loginId", required = true) String cby
			){
		return disposalPointService.getDisposalPointListForMap(cby);
	}
	
	@GetMapping(value="/getDisposalPointList")
	public List<?> getDisposalPointList(
			@RequestParam(value="loginId", required = true) String cby
			){
		return disposalPointService.getDisposalPointList(cby);
	}
	 
    @PostMapping("/saveDisposalPoint")
 	public List<?> saveDisposalPoint(
             @RequestParam(value = "zone", required = true) String zone,
             @RequestParam(value = "ward", required = true) String ward,
             @RequestParam(value = "street_name", required = false) String street_name,
             @RequestParam(value = "street_id", required = false) String street_id,
             @RequestParam(value = "latitude", required = true) String latitude,
             @RequestParam(value = "longitude", required = true) String longitude,
             @RequestParam(value = "loginId", required = true) String loginId,
             @RequestParam(value = "file", required = true) MultipartFile file
     ) {
 		return disposalPointService.saveDisposalPoint(
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
            @RequestParam(value = "disposalpoint_id", required = true) String disposalpoint_id,
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
		return disposalPointService.saveActivity(
				disposalpoint_id,
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
		return disposalPointService.getZoneSummary(fromDate, toDate);
	}
	
	@GetMapping(value="/getWardSummary")
	public List<?> getWardSummary(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="zone", required = true) String zone
			){
		return disposalPointService.getWardSummary(fromDate, toDate, zone);
	}
	
	@GetMapping(value="/getDisposalPointSummary")
	public List<?> getDisposalPointSummary(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="ward", required = true) String ward
			){
		return disposalPointService.getDisposalPointSummary(fromDate, toDate, ward);
	}
}
