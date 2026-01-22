package in.gov.chennaicorporation.mobileservice.gcceducation.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gcceducation.service.EducationActivity;

@RequestMapping("/gccofficialapp/api/gcceducation/")
@RestController("gccofficialappEducationRest")
public class EducationController {
	@Autowired
    private JdbcTemplate jdbcEducationTemplate;
	
	@Autowired
	private EducationActivity educationActivity;
	
	@GetMapping(value="/getUnmapSchoolList")
	public List<?> getSchoolList(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "loginId", required = false) String loginId){
		return educationActivity.getUnmapSchoolList(formData,loginId);
	}
	
	@GetMapping(value="/getSchoolList")
	public List<?> getSchoolList(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "loginId", required = false) String loginId){
		return educationActivity.getSchoolList(formData, latitude, longitude, loginId);
	}
	
	@GetMapping(value="/getMenu")
	public List<?> getMenu(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "loginId", required = false) String loginId){
		return educationActivity.getMenu(formData, loginId);
	}
	
	@GetMapping(value="/getBuildings")
	public List<?> getBuildings(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(required = true) String school_details_id,
			@RequestParam(value = "loginId", required = false) String loginId){
		return educationActivity.getBuildings(formData, school_details_id, loginId);
	}
	
	@GetMapping(value="/getBuilding_list")
	public List<?> getBuilding_list(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(required = true) String school_details_id,
			@RequestParam(required = true) String menu_id,
			@RequestParam(value = "loginId", required = false) String loginId){
		return educationActivity.getBuilding_list(formData, school_details_id, menu_id, loginId);
	}
	
	@GetMapping(value="/getFloor_list")
	public List<?> getFloor_list(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(required = true) String school_details_id,
			@RequestParam(required = true) String menu_id,
			@RequestParam(required = true) String building_id,
			@RequestParam(value = "loginId", required = false) String loginId){
		return educationActivity.getFloor_list(formData, school_details_id, menu_id, building_id, loginId);
	}
	
	@GetMapping(value="/getRoom_list")
	public List<?> getRoom_list(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(required = true) String school_details_id,
			@RequestParam(required = true) String menu_id,
			@RequestParam(required = true) String building_id,
			@RequestParam(required = true) String floors_id,
			@RequestParam(value = "loginId", required = false) String loginId){
		return educationActivity.getRoom_list(formData, school_details_id, menu_id, building_id, floors_id, loginId);
	}
	
	@GetMapping(value="/getParentQuestionsList")
	public List<?> getParentQuestionsList(
			@RequestParam(required = true) String school_details_id,
			@RequestParam(value="menu_id", required = true) String menu_id,
			@RequestParam(required = true) String building_id,
			@RequestParam(required = true) String floors_id,
			@RequestParam(required = true) String room_id,
			@RequestParam(value="loginId", required = false) String cby,
			@RequestParam(value="qtype", required = true) String qtype){
		return educationActivity.getParentQuestionsList(school_details_id, menu_id, building_id, floors_id, room_id, qtype);
	}
	
	@GetMapping(value="/getChildQuestionsList")
	public List<?> getChildQuestionsList(
			@RequestParam(required = true) String school_details_id,
			@RequestParam(required = true) String menu_id,
			@RequestParam(required = true) String building_id,
			@RequestParam(required = true) String floors_id,
			@RequestParam(required = true) String room_id,
			@RequestParam(value="loginId", required = false) String cby,
			@RequestParam(value="pid", required = true) String pid
			){
		return educationActivity.getChildQuestionsList(school_details_id, menu_id, building_id, floors_id, room_id, pid);
	}
	
	@PostMapping("/saveFeedback")
    public List<?> saveFeedback(
    		@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam Map<String, MultipartFile> fileMap,
    		@RequestParam(required = true) String school_details_id,
    		@RequestParam(required = true) String building_id,
    		@RequestParam(required = true) String floor_id,
    		@RequestParam(required = true) String room_id,
    		@RequestParam(required = true) String categoryId,
    		@RequestParam(required = true) String zone,
    		@RequestParam(required = true) String division,
    		@RequestParam(required = true) String latitude,
    		@RequestParam(required = true) String longitude,
    		@RequestParam(required = true) String inby
         ) {
		return educationActivity.saveFeedback(
				formData, fileMap, school_details_id, building_id, floor_id, room_id, 
				categoryId, zone, division, latitude, longitude, inby);
    }
	
	@PostMapping("/saveCompoundAndBoard")
    public ResponseEntity<?> saveCompoundAndBoard(
        
        @RequestParam(required = false) MultipartFile feedPhoto, // file is optional
        @RequestParam(required = true) int school_details_id, // school_details_id is optional
        @RequestParam String zone,
        @RequestParam String division,
        @RequestParam double latitude,
        @RequestParam double longitude,  
        @RequestParam String user_id ) {

        try {
            // Save feedback for both questions
        	//educationActivity.saveCompoundAndBoard(categoryId, newboardconstructQa, newboardconstructAns,
        	//       SchoolunderConstructQa, SchoolunderConstructAns, feedPhoto, school_details_id, zone, division,latitude,longitude,user_id);

            return new ResponseEntity<>("Feedback saved successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to save feedback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
