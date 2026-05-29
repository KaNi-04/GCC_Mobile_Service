package in.gov.chennaicorporation.mobileservice.gccCattleSurvey.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccCattleSurvey.service.CattleService;

@RequestMapping("/gccofficialapp/api/cattlesurveygcc")
@RestController("gccofficialappsgccCattleSurveyController")
public class CattleApiController {

	
	@Autowired
    private CattleService cattleService;
	
	@GetMapping("/getbreeds")
    public List<Map<String, Object>> getbreeds() {

        return cattleService.getbreeds();
    }
	
	@GetMapping("/getcattle_type")
    public List<Map<String, Object>> cattle_type() {

        return cattleService.cattle_type();
    }
	
	@GetMapping("/getgcc_shed")
    public List<Map<String, Object>> getgcc_shed() {

        return cattleService.getgcc_shed();
    }
	
	
	@GetMapping("/getidproof")
    public List<Map<String, Object>> getidproof() {

        return cattleService.getidproof();
    }
	
	@GetMapping("/getspace_rearing")
    public List<Map<String, Object>> getspace_rearing() {

        return cattleService.getspace_rearing();
    }
	
	@GetMapping("/getvaccination_type")
    public List<Map<String, Object>> getvaccination_type() {

        return cattleService.getvaccination_type();
    }
	
	@PostMapping("/saveownerdetails")
	public List<Map<String, Object>> saveownerdetails(
			@RequestParam (required = false) String owner_name,
			@RequestParam (required = false) String mobile_no,
			@RequestParam (required = false) String address,
			@RequestParam (required = false) int id_proof,
			@RequestParam (required = false) String id_details,
			@RequestParam (required = false) String house_type,
			@RequestParam (required = false) int cattle_space,
			@RequestParam (required = false) Double cattle_space_area,
			@RequestParam (required = false) Integer cattle_space_gcc_shed,
			@RequestParam (required = false) int no_of_cattles,
			@RequestParam (required = false) String userid,
			@RequestParam(value = "owner_photo", required = true) MultipartFile image,
			@RequestParam (required = false) String latitude,
            @RequestParam (required = false) String longitude,
            @RequestParam (required = false) String zone,
            @RequestParam (required = false) String ward,
            @RequestParam (required = false) String location
			){
				return cattleService.saveownerdetails(owner_name,mobile_no,address,id_proof,id_details,house_type,
						cattle_space,cattle_space_area,no_of_cattles,userid,image,latitude,
						longitude,zone,ward,location,cattle_space_gcc_shed);
		
		
	}
	
	@GetMapping("/getownersbycby")
    public List<Map<String, Object>> getownersbycby(@RequestParam String userid) {

        return cattleService.getownersbycby(userid);
    }
	
	@GetMapping("/getownerdetails")
    public List<Map<String, Object>> getownerdetails(@RequestParam String owner_ref_id) {

        return cattleService.getownerdetails(owner_ref_id);
    }
	
	@PostMapping("/savecattledetails")
	public List<Map<String, Object>> saveownerdetails(
			@RequestParam (required = false) String owner_ref_id,
			@RequestParam (required = false) int cattle_type,
			@RequestParam (required = false) int breed_type,
			@RequestParam (required = false) String animal_name,
			@RequestParam (required = false) Integer animal_age,
			@RequestParam (required = false) String animal_gender,
			@RequestParam (required = false) String microchip_flag,
			@RequestParam (required = false) String microchip_no,
			@RequestParam (required = false) String license_flag,
			@RequestParam (required = false) String licenese_no,
			@RequestParam (required = false) String vaccination_flag,
			@RequestParam (required = false) Integer vaccination_type,
			@RequestParam (required = false) String vaccination_date,
			@RequestParam (required = false) String userid,
			@RequestParam(value = "cattle_photo", required = true) MultipartFile image,
			@RequestParam (required = false) String latitude,
            @RequestParam (required = false) String longitude,
            @RequestParam (required = false) String zone,
            @RequestParam (required = false) String ward,
            @RequestParam (required = false) String location
			){
				return cattleService.savecattledetails(owner_ref_id,cattle_type,breed_type,animal_name,animal_age,animal_gender,
						microchip_flag,microchip_no,license_flag,licenese_no,vaccination_flag,vaccination_type,vaccination_date,userid,image,latitude,
						longitude,zone,ward,location);
		
		
	}
	
	
}
