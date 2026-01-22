package in.gov.chennaicorporation.mobileservice.penalty.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.penalty.service.PenaltyService;

@RequestMapping("/gccofficialapp/api/penalty")
@RestController("apiPenaltyRest")
public class APIPenaltyController {
	@Autowired
	private PenaltyService penaltyService;
	
	@GetMapping(value="/getpenaltytypes")
	public List<Map<String, Object>> getPenaltyTypes(@RequestParam(value = "departmentId", required = false) String departmentId) {
        return penaltyService.getPenaltyTypeList(departmentId);
    }
	
	@PostMapping(value="/generateChallan")
	public List<?> generateChallan(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "categoryId", required = false) String categoryId, 
			@RequestParam(value = "latitude", required = false) String latitude, 
			@RequestParam(value = "longitude", required = false) String longitude,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streeId", required = false) String streeId,
			@RequestParam(value = "streetName", required = false) String streetName,
			@RequestParam(value = "loginId", required = false) String loginId,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestParam(value = "isviolatorAvailable", required = false) String isviolatorAvailable,
			@RequestParam(value = "violatorName", required = false) String violatorName,
			@RequestParam(value = "violatorPhone", required = false) String violatorPhone,
			@RequestParam(value = "ton", required = false) String ton,
			@RequestParam(value = "file", required = false) MultipartFile file
			) {
		return penaltyService.generateChallan(formData, categoryId, latitude, longitude, zone, ward, streeId, 
				streetName, loginId,remarks,isviolatorAvailable,violatorName,violatorPhone,ton,file);
	}
	
	@PostMapping(value="/updateViolateInfo")
	public List<?> updateViolateInfo(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "challanId", required = false) String challanId, 
			@RequestParam(value = "categoryId", required = false) String categoryId, 
			@RequestParam(value = "latitude", required = false) String latitude, 
			@RequestParam(value = "longitude", required = false) String longitude,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streeId", required = false) String streeId,
			@RequestParam(value = "streetName", required = false) String streetName,
			@RequestParam(value = "loginId", required = false) String loginId,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestParam(value = "isviolatorAvailable", required = false) String isviolatorAvailable,
			@RequestParam(value = "violatorName", required = false) String violatorName,
			@RequestParam(value = "violatorPhone", required = false) String violatorPhone,
			@RequestParam(value = "ton", required = false) String ton,
			@RequestParam(value = "file", required = false) MultipartFile file
			) {
		return penaltyService.updateViolateInfo(formData, challanId, categoryId, latitude, longitude, zone, ward, streeId, 
				streetName, loginId, remarks, isviolatorAvailable, violatorName, violatorPhone, ton, file);
	}
	
	@GetMapping(value = "/getOfficerChallanList")
	public List<Map<String, Object>> getOfficerChallanList(
			@RequestParam(value = "categoryId", required = false) String categoryId,
			@RequestParam(value = "fromDate", required = false) String fromDate,
			@RequestParam(value = "toDate", required = false) String toDate,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streetid", required = false) String streetid,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "isviolatorAvailable", required = false) String isviolatorAvailable,
			@RequestParam(value = "loginId", required = false) String loginId
			){
		return penaltyService.getOfficerChallanList(categoryId, fromDate, toDate, zone, ward, streetid, status, loginId,isviolatorAvailable);
	}
	
	@PostMapping(value="/issueFine")
	public List<?> issueFine(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "challanId", required = false) String challanId, 
			@RequestParam(value = "tonOnFine", required = false) String tonOnFine,
			@RequestParam(value = "fineAmount", required = false) String fineAmount,
			@RequestParam(value = "latitude", required = false) String latitude, 
			@RequestParam(value = "longitude", required = false) String longitude,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streeId", required = false) String streeId,
			@RequestParam(value = "streetName", required = false) String streetName,
			@RequestParam(value = "loginId", required = false) String loginId,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestParam(value = "file", required = false) MultipartFile file
			) {
		return penaltyService.issueFine(formData, challanId, tonOnFine, fineAmount, latitude, longitude, zone, ward, streeId, streetName, loginId,remarks,file);
	}
	
	@PostMapping(value="/closeChallan")
	public List<?> closeChallan(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "challanId", required = false) String challanId,
			@RequestParam(value = "latitude", required = false) String latitude, 
			@RequestParam(value = "longitude", required = false) String longitude,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streeId", required = false) String streeId,
			@RequestParam(value = "streetName", required = false) String streetName,
			@RequestParam(value = "loginId", required = false) String loginId,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestParam(value = "file", required = false) MultipartFile file
			) {
		return penaltyService.closeChallan(formData, challanId, latitude, longitude, zone, ward, streeId, streetName, loginId,remarks,file);
	}
}
