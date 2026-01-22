package in.gov.chennaicorporation.mobileservice.gccHealthCamp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccHealthCamp.service.HealthCampService;

@RequestMapping("/gccofficialapp/api/healthcamp")
@RestController("gccofficialapphealthcamp")

public class HealthCampController {

	@Autowired
	private HealthCampService healthCampService;

	// get all camptypes

	@GetMapping("/camptypes")
	public List<Map<String, Object>> getAllHealthCamp() {
		return healthCampService.getAllHealthCamp();
	}

	// get all disease category

	@GetMapping("/diseasecategory")
	public List<Map<String, Object>> getAllDisease() {
		return healthCampService.getAlldisease();
	}

	// get all gender

	@GetMapping("/gender")
	public List<Map<String, Object>> getAllGender() {
		return healthCampService.getAllgender();
	}

	@GetMapping("/getcampbydate")
	public List<Map<String, Object>> getCampByDate(@RequestParam String userid) {
		return healthCampService.getCampByDate(userid);
	}


	// save the camp details

	@PostMapping("/savecampdetails")
	public ResponseEntity<Map<String, Object>> saveHealthCampDetails(

			@RequestParam("camptypeId") int camptypeId, 
			@RequestParam("latitude") String latitude,
			@RequestParam("longitude") String longitude, 
			@RequestParam("zone") String zone,
			@RequestParam("ward") String ward, 
			@RequestParam("address") String address,
			@RequestParam("campName") String campName, 
			//@RequestParam("noofpamphlets") int noofpamphlets,
			@RequestParam("camp_photo") MultipartFile campPhoto, 
			@RequestParam("userid") String userid,
			@RequestParam("streetName") String streetName,
			@RequestParam("doc_name") String doc_name, 
			@RequestParam("mo_name") String mo_name,
			@RequestParam("si_name") String si_name) {

		// File upload paths

		String campPhotoPath = (campPhoto != null) ? healthCampService.fileUpload(campPhoto, "camp_photo") : "";

		// Save to DB
		String saveStatus = healthCampService.saveHealthCampDetails(camptypeId, latitude, longitude, zone, ward,
				address, campName, campPhotoPath, userid,streetName,doc_name,mo_name,si_name

		);

		if (saveStatus == null || saveStatus.isEmpty()) {
			return ResponseEntity
					.ok(Map.of("status", false, "message", "failed", "description", "Failed to save the details"));
		}

		return ResponseEntity
				.ok(Map.of("status", true, "message", "success", "description", "Details saved successfully"));

	}

	// save the patient details

	@PostMapping("/savepatientdetails")
	public ResponseEntity<Map<String, Object>> savePatientDetails(

			@RequestParam("campId") int campId, 
			@RequestParam("pat_name") String pat_name,
			@RequestParam("pat_age") String pat_age, 
			@RequestParam("genderId") int genderId,
			@RequestParam("mobileNo") String mobileNo, 
			@RequestParam("pat_address") String pat_address,
			@RequestParam(required = false, name = "otherDisease") String otherDisease,
			@RequestParam("remarks") String remarks, 
			@RequestParam("userid") String userid,
			@RequestParam("diseaseIds") List<String> diseaseIds) {

		// Save to DB
		String saveStatus = healthCampService.savePatientDetails(

				campId, pat_name, pat_age, genderId, mobileNo, pat_address, otherDisease, remarks, userid, diseaseIds

		);

		if (saveStatus == null || saveStatus.isEmpty()) {
			return ResponseEntity
					.ok(Map.of("status", false, "message", "failed", "description", "Failed to save the details"));
		}

		return ResponseEntity
				.ok(Map.of("status", true, "message", "success", "description", "Details saved successfully"));

	}
	
	// Reports
	
	@GetMapping("/campsummary")
	public Map<String, Object> getCampSummary(@RequestParam("date") String date) {

		return healthCampService.getCampSummary(date);

	}

	@GetMapping("/zonewisedetails")

	public Map<String, Object> getWardwiseDetails(@RequestParam("date") String date,
			@RequestParam("zoneId") String zoneId) {
		return healthCampService.getWardwiseDetails(date, zoneId);

	}

	@GetMapping("/wardwisedetails")

	public List<Map<String, Object>> getCampwiseDetails(@RequestParam("date") String date,
			@RequestParam("ward") String ward) {
		return healthCampService.getCampwiseDetails(date, ward);

	}
	
	@GetMapping("/campwisedetails")

	public List<Map<String, Object>> getCampDetails(@RequestParam("campId") String campId) {
		return healthCampService.getCampDetails(campId);

	}

}
