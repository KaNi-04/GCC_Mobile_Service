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

import in.gov.chennaicorporation.mobileservice.penalty.service.PenaltyServicePOS;

@RequestMapping("/gccofficialapp/api/penalty/pos")
@RestController("apiPenaltyPOSRest")
public class APIPenaltyControllerPOS {
	@Autowired
	private PenaltyServicePOS penaltyServicePOS;
	
	@GetMapping(value="/getPenaltyDepartment")
	public List<Map<String, Object>> getPenaltyDepartment(
			@RequestParam(value = "loginId", required = false) String loginId
			) {
        return penaltyServicePOS.getPenaltyDepartment();
    }
	
	@GetMapping(value="/getPenaltytypes")
	public List<Map<String, Object>> getPenaltyTypes(
			@RequestParam(value = "departmentId", required = false) String departmentId,
			@RequestParam(value = "loginId", required = false) String loginId) {
        return penaltyServicePOS.getPenaltyTypeList(departmentId);
    }
	
	@GetMapping(value="/getPenaltyAmount")
	public List<Map<String, Object>> getPenaltyAmount(
			@RequestParam(value = "categoryId", required = false) String categoryId,
			@RequestParam(value = "ton", required = false) String ton,
			@RequestParam(value = "loginId", required = false) String loginId
			) {
        return penaltyServicePOS.getPenaltyAmount(categoryId,ton);
    }
	
	@GetMapping(value="/getOfflineModeTypes")
	public List<Map<String, Object>> getOfflineModeTypes() {
        return penaltyServicePOS.getOfflineModeTypes();
    }
	
	@GetMapping(value="/getBankList")
	public List<Map<String, Object>> getBankList() {
        return penaltyServicePOS.getBankList();
    }
	
	@PostMapping(value="/generateChallanPOS")
	public List<?> generateChallan(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "categoryId", required = false) String categoryId, 
			@RequestParam(value = "latitude", required = false) String latitude, 
			@RequestParam(value = "longitude", required = false) String longitude,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "loginId", required = false) String loginId,
			@RequestParam(value = "violatorName", required = false) String violatorName,
			@RequestParam(value = "violatorPhone", required = false) String violatorPhone,
			@RequestParam(value = "violatorCompany", required = false) String violatorCompany,
			@RequestParam(value = "ton", required = false) String ton,
			@RequestParam(value = "amount", required = false) String amount,
			@RequestParam(value = "tid", required = false) String tid,
			@RequestParam(value = "mid", required = false) String mid,
			@RequestParam(value = "serialNumber", required = false) String serialNumber,
			@RequestParam(value = "file", required = false) MultipartFile file
			) {
		return penaltyServicePOS.generateChallanPOS(formData, categoryId, latitude, longitude, zone, ward,
				loginId, violatorName, violatorPhone, violatorCompany, ton, amount, tid, mid, serialNumber, file);
	}
	
	@PostMapping("/storeBankTransaction")
    public List<?> storeBankTransaction(@RequestBody Map<String, Object> transactionData) {
        return penaltyServicePOS.storeBankTransaction(transactionData);
    }
	
	@PostMapping("/storeFailedBankTransaction")
    public List<?> storeFailedBankTransaction(@RequestBody Map<String, Object> transactionData) {
        return penaltyServicePOS.storeFailedBankTransaction(transactionData);
    }
	
	@PostMapping(value="/storeOfflineTransaction")
	public List<?> generateOfflineChallan(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "transactionResponseStatus", required = false) String transactionResponseStatus, 
			@RequestParam(value = "bankName", required = false) String bankName, 
			@RequestParam(value = "transactionAmount", required = false) String transactionAmount,
			@RequestParam(value = "txnDate", required = false) String txnDate,
			@RequestParam(value = "txnTime", required = false) String txnTime,
			@RequestParam(value = "branchname", required = false) String branchname,
			@RequestParam(value = "extraInfo", required = false) String extraInfo,
			@RequestParam(value = "extraInfo2", required = false) String extraInfo2,
			@RequestParam(value = "extraInfo3", required = false) String extraInfo3,
			@RequestParam(value = "txnMode", required = false) String txnMode,
			@RequestParam(value = "txnId", required = false) String txnId,
			@RequestParam(value = "file", required = false) MultipartFile file
			) {
		return penaltyServicePOS.storeOfflineTransaction(formData, transactionResponseStatus, bankName, transactionAmount, txnDate, txnTime,branchname,extraInfo,extraInfo2,extraInfo3,txnMode,txnId,file);
	}
}
