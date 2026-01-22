package in.gov.chennaicorporation.mobileservice.penalty.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.penalty.service.CattleCatchService;

@RequestMapping("/gccofficialapp/cattlecatch/api")
@RestController("apiCattleCatchRest")
public class APICattleCatchController {
	@Autowired
	private CattleCatchService cattleCatchService;
	
	@PostMapping(value="/step1") // Cattle Catch Step 1 
	public List<?> step1(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "categoryId", required = false) String categoryId, 
			@RequestParam(value = "latitude", required = false) String latitude, 
			@RequestParam(value = "longitude", required = false) String longitude,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streeId", required = false) String streeId,
			@RequestParam(value = "streetName", required = false) String streetName,
			@RequestParam(value = "loginId", required = false) String loginId,
			@RequestParam(value = "remarks", required = false) String remarks,
			//@RequestParam(value = "isviolatorAvailable", required = false) String isviolatorAvailable,
			//@RequestParam(value = "violatorName", required = false) String violatorName,
			//@RequestParam(value = "violatorPhone", required = false) String violatorPhone,
			//@RequestParam(value = "ton", required = false) String ton,
			@RequestParam(value = "file", required = false) MultipartFile file
			) {
		return cattleCatchService.step1(formData, categoryId, latitude, longitude, zone, ward, streeId, 
				streetName, loginId,remarks,file);
	} 
	
	@GetMapping("/step1list")
    public List<?> step1list(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "loginid", required = false) String loginId) {
        return cattleCatchService.step1list(formData,loginId);
    }
	
	@GetMapping(value = "/getdepotlist")
	public List<?> getdepotlist(@RequestParam(value = "latitude", required = false) String latitude,
			@RequestParam(value = "longitude", required = false) String longitude) {
		return cattleCatchService.getdepotlist(latitude, longitude);
	}
	
	@PostMapping(value="/step2") // Move Catched cattle to depot 
	public List<?> step2(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "cinfoid", required = false) String cinfoid,
			@RequestParam(value = "depotid", required = false) String depotid,
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
		return cattleCatchService.step2(formData, cinfoid, depotid,latitude, longitude, zone, ward, streeId, 
				streetName, loginId,remarks,file);
	}
	
	@GetMapping("/step2list")
    public List<?> step2list(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "depotid", required = true) String depotid) {
        return cattleCatchService.step2list(formData,depotid);
    }
	
	@PostMapping(value="/step3")  // Generate Fine Chellan when Violator is Available 
	public List<?> generateChallan(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "cinfoids", required = false) String cinfoids, 
			@RequestParam(value = "latitude", required = false) String latitude, 
			@RequestParam(value = "longitude", required = false) String longitude,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streeId", required = false) String streeId,
			@RequestParam(value = "streetName", required = false) String streetName,
			@RequestParam(value = "loginId", required = false) String loginId,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestParam(value = "depotid", required = false) String depotid,
			@RequestParam(value = "violatorName", required = false) String violatorName,
			@RequestParam(value = "violatorPhone", required = false) String violatorPhone,
			@RequestParam(value = "fineAmount", required = false) String fineAmount,
			@RequestParam(value = "file", required = false) MultipartFile file
			) {
		return cattleCatchService.step3(formData, cinfoids, latitude, longitude, zone, ward, streeId, 
				streetName, loginId,remarks,violatorName,violatorPhone,fineAmount,depotid,file);
	}
	
	@GetMapping("/getChallanPOSData")
    public List<?> getChallanPOSData(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "orderid", required = false) String orderid, 
			@RequestParam(value = "loginid", required = true) String loginid) {
        return cattleCatchService.getChallanPOSData(formData,orderid,loginid);
    }
	
	@GetMapping("/updateSerialNumber")
    public List<?> updateSerialNumber(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "tid", required = true) String tid, 
			@RequestParam(value = "mid", required = true) String mid,
			@RequestParam(value = "serialNumber", required = true) String serialNumber,
			@RequestParam(value = "orderid", required = true) String orderid) {
        return cattleCatchService.updateSerialNumber(formData,tid, mid, serialNumber, orderid);
    }
	
	@PostMapping("/storeBankTransaction")
    public List<?> storeBankTransaction(@RequestBody Map<String, Object> transactionData) {
        return cattleCatchService.storeBankTransaction(transactionData);
    }
	
	@PostMapping("/storeFailedBankTransaction")
    public List<?> storeFailedBankTransaction(@RequestBody Map<String, Object> transactionData) {
        return cattleCatchService.storeFailedBankTransaction(transactionData);
    }
	
	@GetMapping("/step3list")
    public List<?> step3list(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "loginid", required = true) String loginid) {
        return cattleCatchService.step3list(formData,loginid);
    }
	
	@PostMapping(value="/step4")  // Cattle Catch Step 1 
	public List<?> generateChallan(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "cinfoid", required = false) String cinfoid, 
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
		return cattleCatchService.step4(formData, cinfoid, latitude, longitude, zone, ward, streeId, 
				streetName, loginId,remarks,file);
	}
	
	@GetMapping("/depotReport")
    public List<?> DepotReport(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "fromDate", required = true) String fromDate,
    		@RequestParam(value = "toDate", required = true) String toDate,
    		@RequestParam(value = "loginid", required = false) String loginid) {
        return cattleCatchService.depotReport(formData,fromDate,toDate,loginid);
    }
	
	@GetMapping("/zoneReport")
    public List<?> zoneReport(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "fromDate", required = true) String fromDate,
    		@RequestParam(value = "toDate", required = true) String toDate,
    		@RequestParam(value = "depotid", required = true) String depotid,
    		@RequestParam(value = "loginid", required = false) String loginid) {
        return cattleCatchService.zoneReport(formData,fromDate,toDate,depotid,loginid);
    }
	
	@GetMapping("/wardReport")
    public List<?> zoneReport(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "fromDate", required = true) String fromDate,
    		@RequestParam(value = "toDate", required = true) String toDate,
    		@RequestParam(value = "zone", required = true) String zone,
    		@RequestParam(value = "depotid", required = true) String depotid,
    		@RequestParam(value = "loginid", required = false) String loginid) {
        return cattleCatchService.wardReport(formData,fromDate,toDate,zone,depotid,loginid);
    }
	
	@GetMapping("/inListReport")
    public List<?> inListReport(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "fromDate", required = true) String fromDate,
    		@RequestParam(value = "toDate", required = true) String toDate,
    		@RequestParam(value = "zone", required = true) String zone,
    		@RequestParam(value = "ward", required = true) String ward,
    		@RequestParam(value = "depotid", required = true) String depotid,
    		@RequestParam(value = "loginid", required = false) String loginid) {
        return cattleCatchService.inListReport(formData,fromDate,toDate,zone,ward,depotid,loginid);
    }
	
	@GetMapping("/outListReport")
    public List<?> outListReport(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "fromDate", required = true) String fromDate,
    		@RequestParam(value = "toDate", required = true) String toDate,
    		@RequestParam(value = "zone", required = true) String zone,
    		@RequestParam(value = "ward", required = true) String ward,
    		@RequestParam(value = "depotid", required = true) String depotid,
    		@RequestParam(value = "loginid", required = false) String loginid) {
        return cattleCatchService.outListReport(formData,fromDate,toDate,zone,ward,depotid,loginid);
    }
	
}
