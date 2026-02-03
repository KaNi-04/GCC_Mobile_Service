package in.gov.chennaicorporation.mobileservice.mosquitosurvey.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.mosquitosurvey.service.MosquitoService;

@RequestMapping("/gccofficialapp/mosquito/api")
@RestController("apiMosquitoRest")
public class API_MosquitoController {
	@Autowired
	private MosquitoService mosquitoService;
	
	@GetMapping("/getCategoryList")
    public List<?> getCategoryList(
			) {
        return mosquitoService.getCategoryList();
    }
	
	@GetMapping("/getSubCategoryList")
    public List<?> getSubCategoryList(
    		@RequestParam(value = "cid", required = true) String cid
			) {
        return mosquitoService.getSubCategoryList(cid);
    }
	
	@GetMapping("/getTreatmentList")
    public List<?> getTreatmentList(
			) {
        return mosquitoService.getTreatmentList();
    }
	
	@GetMapping("/getBreadingSourceCategoryList")
    public List<?> getBreadingSourceCategoryList(
			) {
        return mosquitoService.getBreadingSourceCategoryList();
    }
	
	@GetMapping("/getBreadingSourceList")
    public List<?> getBreadingSourceList(
			) {
        return mosquitoService.getBreadingSourceList();
    }

	@GetMapping("/getChronicDiseases")
    public List<?> getChronicDiseases(
			) {
        return mosquitoService.getChronicDiseases();
    }
	
	// ************************************************************************************************************************* //
	// For House, Companies & Education Institution
	
	@PostMapping(value="/saveSurveyFlow1") 
	public List<?> saveSurveyFlow1(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "category", required = true) String cid,
			@RequestParam(value = "typeofsite", required = true) String scid,
			@RequestParam(value = "visitstatus", required = true) String visitstatus,
			@RequestParam(value = "feverfound", required = true) String q1,
			@RequestParam(value = "treatment", required = false) String treatment,
			@RequestParam(value = "noofsource", required = false) String noofsource,
			@RequestParam(value = "breedfound", required = true) String q2,
			@RequestParam(value = "sourcetype", required = false) String sourcetype,
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "name", required = true) String name,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "address", required = false) String address,
			@RequestParam(value = "coughcold", required = false) String coughcold,
			@RequestParam(value = "ischronic", required = false) String ischronic,
			@RequestParam(value = "chronicdisease", required = false) String chronicdisease
			//@RequestParam(value = "file", required = true) MultipartFile file,
			) {
		return mosquitoService.saveSurveyFlow1(cid,scid,q1,treatment,q2,sourcetype,cby,name,zone,ward,latitude,longitude,visitstatus,noofsource,address,coughcold,ischronic, chronicdisease);
	}
	
	@GetMapping("/checkHasPending")
    public List<?> checkHasPending(
    		@RequestParam(value = "loginId", required = true) String cby
			) {
        return mosquitoService.checkHasPending(cby);
    }
	
	@GetMapping("/getHousePendingList")
    public List<?> getHousePendingList(
    		@RequestParam(value = "loginId", required = true) String cby
			) {
        return mosquitoService.getHousePendingList(cby);
    }
	
	@GetMapping("/checkPendingUpdateLocation")
    public List<?> checkPendingUpdateLocation(
    		@RequestParam(value = "id", required = true) String id, 
    		@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude
			) {
        return mosquitoService.checkPendingUpdateLocation(id,latitude,longitude);
    }
	
	@PostMapping(value="/updateSurveyFlow1Pending")
	public List<?> updatePendingStatus(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "sid", required = true) String sid,
			@RequestParam(value = "action", required = true) String q3,
			@RequestParam(value = "file_before", required = false) MultipartFile file_before,
			@RequestParam(value = "file", required = true) MultipartFile file,
			@RequestParam(value = "remarks", required = true) String remarks,
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude
			) {
		return mosquitoService.updateFlow1PendingStatus("close", q3, file, remarks, cby, zone, ward, latitude, longitude, sid,file_before);
	}
	
	// ************************************************************************************************************************* //
	
	// For Construction Site & Vacant Land
	
	@PostMapping(value="/saveSurveyFlow2") 
	public List<?> saveSurveyFlow2(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "category", required = true) String cid,
			@RequestParam(value = "typeofsite", required = true) String scid,
			@RequestParam(value = "name", required = true) String name,
			@RequestParam(value = "breedfound", required = true) String q2,
			@RequestParam(value = "action", required = false) String q3,
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "address", required = false) String address
			) {
		return mosquitoService.saveSurveyFlow2(cid, scid, name, q2, q3, file, remarks, cby, zone, ward, latitude, longitude, "close", address);
	}
	
	// FOR REPORTS
	
	@GetMapping(value="/breedingCheck/reports/zone")
	public List<?> breedingZoneReports(
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "fromDate", required = true) String fromDate,
			@RequestParam(value = "toDate", required = true) String toDate
			) {
		return mosquitoService.breedingZoneReports(cby,fromDate,toDate);
	}
	
	@GetMapping(value="/breedingCheck/reports/breedingWardReports")
	public List<?> breedingWardReports(
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "fromDate", required = true) String fromDate,
			@RequestParam(value = "toDate", required = true) String toDate,
			@RequestParam(value = "zone", required = true) String zone
			) {
		return mosquitoService.breedingWardReports(cby,fromDate,toDate,zone);
	}
	
	@GetMapping(value="/breedingCheck/reports/breedinglistReports")
	public List<?> breedinglistReports(
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "fromDate", required = true) String fromDate,
			@RequestParam(value = "toDate", required = true) String toDate,
			@RequestParam(value = "ward", required = true) String ward
			) {
		return mosquitoService.breedinglistReports(cby,fromDate,toDate,ward);
	}
	
	@GetMapping(value="/breedingCheck/reports/getBreedingDetails")
	public List<?> getBreedingDetails(
			@RequestParam(value = "id", required = true) String id
			) {
		return mosquitoService.getBreedingDetails(id);
	}
	
	@GetMapping(value="/breedingCheck/reports/userReports")
	public List<?> userReports(
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "fromDate", required = true) String fromDate,
			@RequestParam(value = "toDate", required = true) String toDate,
			@RequestParam(value = "ward", required = true) String ward
			) {
		return mosquitoService.userReports(cby,fromDate,toDate,ward);
	}
	
	@GetMapping(value="/breedingCheck/reports/userDayReports")
	public List<?> userDayReports(
			@RequestParam(value = "cby", required = true) String cby,
			@RequestParam(value = "fromDate", required = true) String fromDate,
			@RequestParam(value = "toDate", required = true) String toDate,
			@RequestParam(value = "ward", required = true) String ward
			) {
		return mosquitoService.userDayReports(cby,fromDate,toDate,ward);
	}
	
	@GetMapping(value="/breedingCheck/reports/userBreedinglistReports")
	public List<?> userBreedinglistReports(
			@RequestParam(value = "cby", required = true) String cby,
			@RequestParam(value = "selectDate", required = true) String selectedDate,
			@RequestParam(value = "ward", required = true) String ward
			) {
		return mosquitoService.userBreedinglistReports(cby,selectedDate,ward);
	}
}
