package in.gov.chennaicorporation.mobileservice.hoardings.controller;

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

import in.gov.chennaicorporation.mobileservice.hoardings.services.HoardingsService;

@RequestMapping("/gccofficialapp/hoardings/api")
@RestController("apiHoardingsRest")
public class APIHoardingsController {
	@Autowired
	private HoardingsService hoardingsService;
	
	
	@GetMapping("/licenseVerify")
    public List<?> licenseVerify(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "license_no", required = true) String license_no,
    		@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "loginId", required = false) String loginId) {
        return hoardingsService.licenseVerify(license_no,latitude,longitude);
    }
	
	@PostMapping(value="/saveHoardings") // Enumeration
	public List<?> saveHoardings(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "categoryId", required = false) String categoryId,
			@RequestParam(value = "assetName", required = false) String assetName,
			@RequestParam(value = "latitude", required = false) String latitude, 
			@RequestParam(value = "longitude", required = false) String longitude,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streeId", required = false) String streeId,
			@RequestParam(value = "streetName", required = false) String streetName,
			@RequestParam(value = "loginId", required = false) String loginId,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "hoarding_info_type", required = false) String hoarding_info_type,
			@RequestParam(value = "lincenses_info", required = false) String lincenses_info,
			@RequestParam(value = "lincenses_no", required = false) String lincenses_no, 
			@RequestParam(value = "validity_date", required = false) String validity_date,
			@RequestParam(value = "case_no", required = false) String case_no, 
			@RequestParam(value = "ptax_no", required = false) String ptax_no,
			@RequestParam(value = "ptax_name", required = false) String ptax_name, 
			@RequestParam(value = "ptax_mobile", required = false) String ptax_mobile,
			@RequestParam(value = "agency_name", required = false) String agency_name, 
			@RequestParam(value = "agency_mobile", required = false) String agency_mobile,
			@RequestParam(value = "fine_to", required = false) String fine_to
			) {
		return hoardingsService.saveHoardings(formData, categoryId, assetName, latitude, longitude, zone, ward, streeId, 
				streetName, loginId,remarks,file,
				hoarding_info_type,
				lincenses_info,
    			lincenses_no, 
    			validity_date,
    			case_no, 
    			ptax_no,
    			ptax_name, 
    			ptax_mobile,
    			agency_name, 
    			agency_mobile,
    			fine_to);
	} 
	
	@PostMapping(value="/updateHoardingInfo") // Enumeration
	public List<?> updateHoardingInfo(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "loginId", required = true) String loginId,
			@RequestParam(value = "asset_id", required = true) String asset_id,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestParam(value = "hoarding_info_type", required = false) String hoarding_info_type,
			@RequestParam(value = "lincenses_info", required = false) String lincenses_info,
			@RequestParam(value = "lincenses_no", required = false) String lincenses_no, 
			@RequestParam(value = "validity_date", required = false) String validity_date,
			@RequestParam(value = "case_no", required = false) String case_no, 
			@RequestParam(value = "ptax_no", required = false) String ptax_no,
			@RequestParam(value = "ptax_name", required = false) String ptax_name, 
			@RequestParam(value = "ptax_mobile", required = false) String ptax_mobile,
			@RequestParam(value = "agency_name", required = false) String agency_name, 
			@RequestParam(value = "agency_mobile", required = false) String agency_mobile,
			@RequestParam(value = "fine_to", required = false) String fine_to
			) {
		return hoardingsService.updateHoardingInfo(formData, 
				asset_id,
				hoarding_info_type,
				remarks,
				lincenses_info,
    			lincenses_no, 
    			validity_date,
    			case_no, 
    			ptax_no,
    			ptax_name, 
    			ptax_mobile,
    			agency_name, 
    			agency_mobile,
    			fine_to,
    			loginId);
	} 
	
	
	
	
	@GetMapping("/assetlist")
    public List<?> assetlist(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "loginId", required = false) String loginId) {
        return hoardingsService.assetlist(formData,loginId);
    }
	
	@GetMapping("/assetlistbylogin")
    public List<?> assetlistbylogin(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "loginId", required = true) String loginId) {
        return hoardingsService.assetlistbylogin(formData,loginId);
    }
	
	@GetMapping("/assetlistbylatlong")
    public List<?> assetlistbylatlong(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "loginId", required = false) String loginId) {
        return hoardingsService.assetlistbylatlong(formData, latitude, longitude,loginId);
    }
	
	@GetMapping("/getChallanPOSData")
    public List<?> getChallanPOSData(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "orderid", required = false) String orderid, 
			@RequestParam(value = "loginid", required = true) String loginid) {
        return hoardingsService.getChallanPOSData(formData,orderid,loginid);
    }
	
	@GetMapping("/updateSerialNumber")
    public List<?> updateSerialNumber(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "tid", required = true) String tid, 
			@RequestParam(value = "mid", required = true) String mid,
			@RequestParam(value = "serialNumber", required = true) String serialNumber,
			@RequestParam(value = "orderid", required = true) String orderid) {
        return hoardingsService.updateSerialNumber(formData,tid, mid, serialNumber, orderid);
    }
	
	@PostMapping("/storeBankTransaction")
    public List<?> storeBankTransaction(@RequestBody Map<String, Object> transactionData) {
        return hoardingsService.storeBankTransaction(transactionData);
    }
	
	@PostMapping("/storeFailedBankTransaction")
    public List<?> storeFailedBankTransaction(@RequestBody Map<String, Object> transactionData) {
        return hoardingsService.storeFailedBankTransaction(transactionData);
    }
	
	@PostMapping(value="/updateHoardings") // Enumeration
	public List<?> insertHoardingUpdatedData(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "asset_id", required = false) String asset_id, 
			@RequestParam(value = "update_type", required = false) String update_type,
			@RequestParam(value = "removed_type", required = false) String removed_type, 
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "licenses_no_visible", required = false) String licenses_no_visible, 
			@RequestParam(value = "validity_date_visible", required = false) String validity_date_visible,
			@RequestParam(value = "not_as_per_size", required = false) String not_as_per_size, 
			@RequestParam(value = "licenses_no", required = false) String licenses_no,
			@RequestParam(value = "loginId", required = false) String loginId
			) {
		return hoardingsService.insertHoardingUpdatedData(formData, 
				asset_id, 
				update_type,
				removed_type, 
				file,
				licenses_no_visible, 
				validity_date_visible,
				not_as_per_size, 
				licenses_no,
				loginId);
	}
	
	@GetMapping("/unauterrizedassetlistbylogin")
    public List<?> unAuterrizedAssetlist(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "loginId", required = true) String loginId) {
        return hoardingsService.unAuterrizedAssetlist(formData,loginId);
    }
	
	@GetMapping("/unauterrizedassetlistbylatlong")
    public List<?> assetlistbylatlong(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "assetid", required = false) String assetid,
			@RequestParam(value = "loginId", required = false) String loginId) {
        return hoardingsService.unauterrizedassetlistbylatlong(formData, latitude, longitude, assetid, loginId);
    }
	
	@PostMapping(value="/updateHoardingsAE") // Enumeration
	public List<?> insertHoardingUpdatedDataAE(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "asset_id", required = false) String asset_id, 
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestParam(value = "removed_type", required = false) String removed_type, 
			@RequestParam(value = "file", required = false) MultipartFile file,
			//@RequestParam(value = "firfile", required = false) MultipartFile firfile,
			//@RequestParam(value = "fir", required = false) String fir, 
			//@RequestParam(value = "fir_number", required = false) String fir_number,
			//@RequestParam(value = "material_collected", required = false) String material_collected,
			//@RequestParam(value = "materialfile", required = false) MultipartFile materialfile,
			@RequestParam(value = "loginId", required = false) String loginId
			) {
		return hoardingsService.insertHoardingUpdatedDataAE(formData, 
				asset_id, 
				remarks,
				removed_type, 
				file,
				//firfile, 
				//fir,
				//fir_number, 
				//material_collected,
				//materialfile,
				loginId);
	}
	
	@PostMapping(value="/updateMaterialInfo") // Enumeration
	public List<?> updateMaterialInfo(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "id", required = true) String id,
			@RequestParam(value = "asset_id", required = true) String asset_id,
			@RequestParam(value = "material_collected", required = false) String material_collected,
			@RequestParam(value = "material_remarks", required = false) String material_remarks,
			@RequestParam(value = "materialfile", required = false) MultipartFile materialfile,
			@RequestParam(value = "loginId", required = false) String loginId
			) {
		return hoardingsService.updateMaterialInfo(
				id, 
				asset_id, 
				material_collected,
				material_remarks,
				materialfile);
	}
	
	@PostMapping(value="/updateFirInfo") // Enumeration
	public List<?> updateFirInfo(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "id", required = true) String id,
			@RequestParam(value = "asset_id", required = true) String asset_id,
			@RequestParam(value = "firfile", required = false) MultipartFile firfile,
			@RequestParam(value = "fir_number", required = false) String fir_number,
			@RequestParam(value = "fir_remarks", required = false) String fir_remarks,
			@RequestParam(value = "loginId", required = false) String loginId
			) {
		return hoardingsService.updateFirInfo(
				id,
				asset_id,
				fir_number,
				fir_remarks,
				firfile);
	}
	
	@GetMapping("/unAuterrizedAssetlistAEPending")
    public List<?> unAuterrizedAssetlistAEPending(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "pendingType", required = true) String pendingType,
			@RequestParam(value = "loginId", required = true) String loginId) {
        return hoardingsService.unAuterrizedAssetlistAEPending(formData, pendingType, loginId);
    }
	
	@GetMapping("/ReportTile")
    public List<?> ReportTile(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "fromDate", required = true) String fromDate,
    		@RequestParam(value = "toDate", required = true) String toDate,
			@RequestParam(value = "loginId", required = false) String loginId) {
        return hoardingsService.ReportTile(fromDate, toDate);
    }
	
	@GetMapping("/reportZoneList")
    public List<?> reportZoneList(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "fromDate", required = true) String fromDate,
    		@RequestParam(value = "toDate", required = true) String toDate,
			@RequestParam(value = "loginId", required = false) String loginId) {
        return hoardingsService.reportZoneList(fromDate, toDate);
    }
	
	@GetMapping("/reportWardList")
    public List<?> reportZoneList(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "zone", required = true) String zone,
    		@RequestParam(value = "fromDate", required = true) String fromDate,
    		@RequestParam(value = "toDate", required = true) String toDate,
			@RequestParam(value = "loginId", required = false) String loginId) {
        return hoardingsService.reportWardList(zone, fromDate, toDate);
    }
	
	@GetMapping("/reportListBySelect")
    public List<?> reportZoneList(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "ward", required = true) String ward,
    		@RequestParam(value = "hoarding_info_type", required = true) String hoarding_info_type,
    		@RequestParam(value = "fromDate", required = true) String fromDate,
    		@RequestParam(value = "toDate", required = true) String toDate,
			@RequestParam(value = "loginId", required = false) String loginId) {
        return hoardingsService.reportListBySelect(ward, hoarding_info_type, fromDate, toDate);
    }
}
