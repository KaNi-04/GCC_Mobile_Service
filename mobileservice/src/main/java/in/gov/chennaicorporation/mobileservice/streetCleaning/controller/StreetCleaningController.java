package in.gov.chennaicorporation.mobileservice.streetCleaning.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.streetCleaning.service.StreetCleaningActivity;

@RequestMapping("/gccofficialapp/api/streetcleaning/")
@RestController("gccofficialappstreetcleaning")
public class StreetCleaningController {
	@Autowired
    private StreetCleaningActivity streetCleaningActivity ;
	
	@GetMapping(value="/checkStreet")
	public String checkStreet_id(
			@RequestParam(value="ward", required = true) String ward,
			@RequestParam(value="street_id", required = true) String street_id,
			@RequestParam(value="loginId", required = true) String loginId
			){
		return streetCleaningActivity.checkStreet_id(loginId,street_id, ward);
	}
	
	@GetMapping(value="/getStreetPendingList")
	public List<?> getStreetPendingList(
			@RequestParam(value="ward", required = true) String ward,
			@RequestParam(value="loginId", required = true) String loginId
			){
		return streetCleaningActivity.getStreetPendingList(loginId, ward);
	}
	
	@GetMapping(value="/getStreetList")
	public List<?> getStreetList(
			@RequestParam(value="ward", required = true) String ward,
			@RequestParam(value="street_id", required = true) String street_id,
			@RequestParam(value="loginId", required = true) String loginId
			){
		return streetCleaningActivity.getStreetList(loginId,street_id, ward);
	}
	
	@PostMapping("/saveActivity")
	public List<?> saveActivity(
            @RequestParam("video_file") MultipartFile video_file,
            @RequestParam("left_file") MultipartFile left_file,
            @RequestParam("right_file") MultipartFile right_file,
            @RequestParam("work_status") String work_status,
            @RequestParam("cleaned_meter") String cleaned_meter,
            @RequestParam("zone") String zone,
            @RequestParam("ward") String ward,
            @RequestParam("street_id") String gis_street_id,
            @RequestParam("street_name") String street_name,
            @RequestParam("road_type") String road_type,
            @RequestParam("street_length") String street_length,
            @RequestParam("latitude") String latitude,
            @RequestParam("longitude") String longitude,
            @RequestParam("loginId") String inby
            
    ) {
		return streetCleaningActivity.saveActivity(
				video_file,
				left_file,
				right_file,
				work_status,
				cleaned_meter,
				zone,
				ward,
				gis_street_id,
				street_name,
				road_type,
				street_length,
				latitude,
				longitude,
				inby
        );
    }
	
	@GetMapping(value="/getZoneReport")
	public List<?> getZoneReport(String loginId){
		return streetCleaningActivity.getZoneReport(loginId);
	}
	
	@GetMapping(value="/getWardReportByZone")
	public List<?> getWardReportByZone(
			@RequestParam(value="loginid") String loginId,
			@RequestParam(value="zone") String zone ){
		return streetCleaningActivity.getWardReportByZone(loginId, zone);
	}
	
	@GetMapping(value="/getStreetListByWardAndStatus")
	public List<?> getStreetListByWardAndStatus(
			@RequestParam(value="loginid") String loginid, 
			@RequestParam(value="ward") String ward, 
			@RequestParam(value="status", required = false) String status){
		return streetCleaningActivity.getStreetListByWardAndStatus(loginid, ward, status);
	}
	
	// For Rating
	@GetMapping(value="/getStreetListForRating")
	public List<?> getStreetListForRating(
			@RequestParam(value="ward", required = true) String ward,
			@RequestParam(value="street_id", required = true) String street_id,
			@RequestParam(value="loginId", required = true) String loginId
			){
		return streetCleaningActivity.getStreetListForRating(loginId,street_id, ward);
	}
	
	@PostMapping("/saveRatingActivity")
	public List<?> saveRatingActivity(
            @RequestParam("video_file") MultipartFile video_file,
            @RequestParam("left_file") MultipartFile left_file,
            @RequestParam("right_file") MultipartFile right_file,
            @RequestParam("work_rating") String work_rating,
            @RequestParam("zone") String zone,
            @RequestParam("ward") String ward,
            @RequestParam("street_id") String gis_street_id,
            @RequestParam("street_name") String street_name,
            @RequestParam("road_type") String road_type,
            @RequestParam("latitude") String latitude,
            @RequestParam("longitude") String longitude,
            @RequestParam("loginId") String inby
            
    ) {
		return streetCleaningActivity.saveRatingActivity(
				video_file,
				left_file,
				right_file,
				work_rating,
				zone,
				ward,
				gis_street_id,
				street_name,
				road_type,
				latitude,
				longitude,
				inby
        );
    }
}
