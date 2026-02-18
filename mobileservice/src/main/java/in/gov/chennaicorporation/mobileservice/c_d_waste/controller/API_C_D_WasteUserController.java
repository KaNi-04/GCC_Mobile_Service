package in.gov.chennaicorporation.mobileservice.c_d_waste.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.c_d_waste.service.c_d_WasteUserService;

@RequestMapping("/gccofficialapp/c_dwaste/api")
@RestController("apiCandDUserWasteRest")
public class API_C_D_WasteUserController {

	@Autowired
	private c_d_WasteUserService c_d_WasteUserService;
	
	@PostMapping(value="/saveUserWaste") 
	public List<?> saveuserWaste(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "latitude", required = false) String latitude, 
			@RequestParam(value = "longitude", required = false) String longitude,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "address", required = true) String address,
			@RequestParam(value = "ref_id", required = true) String ref_id,
			@RequestParam(value = "tonage", required = true) String tonage,
			@RequestParam(value = "file", required = true) MultipartFile file,
			@RequestParam(value = "wastetype", required = true) String type
			) {
		return c_d_WasteUserService.saveUserWaste(latitude,longitude,zone,ward,address,tonage,file,type,ref_id);
	}
	
//	@GetMapping("/getUserApprovalPendingList")
//    public List<?> getApprovalPendingList(
//			@RequestParam(value = "loginId", required = true) String loginId,
//			@RequestParam(value = "ward", required = true) String ward
//			) {
//        return c_d_WasteUserService.getApprovalPendingList(loginId,ward);
//    }
	
	
	@GetMapping("/getUserApprovalPendingList")
    public List<?> getApprovalPendingList(
			@RequestParam(value = "loginId", required = true) String loginId,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "type", required = false) String type
			) {
        return c_d_WasteUserService.getApprovalPendingList(loginId,ward,type);
    }
	
	@PostMapping(value="/saveUserApproval") 
	public List<?> saveApproval(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "loginId", required = true) String loginId,
			@RequestParam(value = "comp_id", required = true) String comp_id, 
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "in_out", required = true) String in_out,
			@RequestParam(value = "status", required = true) String status,
			@RequestParam(value = "tonage_kg", required = true) String tonage_kg,
			@RequestParam(value = "reject_txt", required = false) String reject_txt
			) {
		return c_d_WasteUserService.saveApproval(loginId, comp_id, latitude, longitude, in_out, status, reject_txt, tonage_kg);
	}
}
