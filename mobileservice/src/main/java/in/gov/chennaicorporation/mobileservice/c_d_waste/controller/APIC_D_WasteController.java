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

import in.gov.chennaicorporation.mobileservice.c_d_waste.service.c_d_WasteService;

@RequestMapping("/gccofficialapp/c_dwaste/api")
@RestController("apiCandDWasteRest")
public class APIC_D_WasteController {

	@Autowired
	private c_d_WasteService c_d_WasteService;
	
	@GetMapping("/getWasteTypeList")
    public List<?> getWasteTypeList(
			) {
        return c_d_WasteService.getWasteTypeList();
    }
	
	@GetMapping("/checkComplaintExits")
    public List<?> checkComplaintExits(
    		@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude
			) {
        return c_d_WasteService.checkComplaintExits(latitude,longitude);
    }
	
	@PostMapping(value="/saveWaste") 
	public List<?> saveWaste(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "address", required = true) String address,
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "tonage", required = true) String tonage,
			@RequestParam(value = "file", required = true) MultipartFile file,
			@RequestParam(value = "wastetype", required = true) String type,
			@RequestParam(value = "StreetType", required = false) String StreetType
			) {
		return c_d_WasteService.saveWaste(cby,latitude,longitude,zone,ward,address,tonage,file, type, StreetType);
	}
	
	@PostMapping(value="/saveVendorWaste") 
	public List<?> saveVendorWaste(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "address", required = true) String address,
			@RequestParam(value = "ref_id", required = true) String ref_id,
			@RequestParam(value = "tonage", required = true) int tonage,
			@RequestParam(value = "file", required = true) MultipartFile file,
			@RequestParam(value = "wastetype", required = true) String type,
			@RequestParam(value = "StreetType", required = false) String StreetType
			) {
		return c_d_WasteService.saveVendorWaste(latitude,longitude,zone,ward,address,tonage,file,type,ref_id, StreetType);
	}
	
	@GetMapping("/getComplaintList")
    public List<?> getComplaintList(
			@RequestParam(value = "fromDate", required = true) String fromDATE,
			@RequestParam(value = "toDate", required = true) String toDATE
			) {
        return c_d_WasteService.getComplaintList(fromDATE,toDATE);
    }
	
	@PostMapping(value="/updateComplaint") 
	public List<?> updateComplaint(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "comp_id", required = true) String wlid,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "after_image", required = true) String file,
			@RequestParam(value = "remarks", required = true) String remarks,
			@RequestParam(value = "completed_date", required = true) String datetxt,
			@RequestParam(value = "tonage", required = true) String tonage
			) {
		return c_d_WasteService.saveReply(wlid,latitude,longitude,file,remarks,datetxt,tonage);
	}
	
	
	@GetMapping("/getMyticketllist")
    public List<?> getMyticketllist(
			@RequestParam(value = "loginId", required = true) String loginId,
			@RequestParam(value = "fromDate", required = false) String fromDATE,
			@RequestParam(value = "toDate", required = false) String toDATE
			) {
        return c_d_WasteService.getMyticketllist(loginId,fromDATE,toDATE);
    }
	
	@GetMapping("/getApprovalPendingList")
    public List<?> getApprovalPendingList(
			@RequestParam(value = "loginId", required = true) String loginId,
			@RequestParam(value = "ward", required = true) String ward
			) {
        return c_d_WasteService.getApprovalPendingList(loginId,ward);
    }
	
	@GetMapping("/checkApprovalLocation")
    public List<?> checkApprovalLocation(
    		@RequestParam(value = "comp_id", required = true) String id, 
    		@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude
			) {
        return c_d_WasteService.checkApprovalLocation(id,latitude,longitude);
    }
	
	@PostMapping(value="/saveApproval") 
	public List<?> saveApproval(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "loginId", required = true) String loginId,
			@RequestParam(value = "comp_id", required = true) String comp_id, 
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "in_out", required = true) String in_out,
			@RequestParam(value = "status", required = true) String status,
			@RequestParam(value = "reject_txt", required = false) String reject_txt
			) {
		return c_d_WasteService.saveApproval(loginId, comp_id, latitude, longitude, in_out, status, reject_txt);
	}
	
	@GetMapping("/get2ApprovalPendingList")
    public List<?> get2ApprovalPendingList(
			@RequestParam(value = "loginId", required = true) String loginId,
			@RequestParam(value = "ward", required = true) String ward
			) {
        return c_d_WasteService.get2ApprovalPendingList(loginId,ward);
    }
	
	@PostMapping(value="/save2Approval") 
	public List<?> save2Approval(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "loginId", required = true) String loginId,
			@RequestParam(value = "comp_id", required = true) String comp_id, 
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "status", required = true) String status
			) {
		return c_d_WasteService.save2Approval(loginId, comp_id, latitude, longitude, status);
	}
	
	@GetMapping("/getZoneWiseCount")
    public List<?> getZoneWiseCount(
			@RequestParam(value = "fromDate", required = true) String fromDATE,
			@RequestParam(value = "toDate", required = true) String toDATE,
			@RequestParam(value = "requestBy", required = false) String requestBy
			) {
		
			if (requestBy == null || requestBy.isBlank()) {
				requestBy = "'Vendor','Officer'";
			}
			else {
				requestBy = "'"+requestBy+"'";
			}
			
        return c_d_WasteService.getZoneWiseCount(fromDATE,toDATE,requestBy);
    }
	
	@GetMapping("/getWardWiseCount")
    public List<?> getWardWiseCount(
			@RequestParam(value = "fromDate", required = true) String fromDATE,
			@RequestParam(value = "toDate", required = true) String toDATE,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "requestBy", required = false) String requestBy
			) {
		
			if (requestBy == null || requestBy.isBlank()) {
				requestBy = "'Vendor','Officer'";
			}
			else {
				requestBy = "'"+requestBy+"'";
			}
			
        return c_d_WasteService.getWardWiseCount(fromDATE,toDATE, zone,requestBy);
    }
	
	@GetMapping("/getOfficerZoneWiseCount")
    public List<?> getOfficerZoneWiseCount(
			@RequestParam(value = "fromDate", required = true) String fromDATE,
			@RequestParam(value = "toDate", required = true) String toDATE,
			@RequestParam(value = "requestBy", required = false) String requestBy
			) {
		
			if (requestBy == null || requestBy.isBlank()) {
				requestBy = "'Officer'";
			}
			else {
				requestBy = "'"+requestBy+"'";
			}
			
        return c_d_WasteService.getOfficerZoneWiseCount(fromDATE,toDATE,requestBy);
    }
	
	@GetMapping("/getOfficerWardWiseCount")
    public List<?> getOfficerWardWiseCount(
			@RequestParam(value = "fromDate", required = true) String fromDATE,
			@RequestParam(value = "toDate", required = true) String toDATE,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "requestBy", required = false) String requestBy
			) {
		
			if (requestBy == null || requestBy.isBlank()) {
				requestBy = "'Officer'";
			}
			else {
				requestBy = "'"+requestBy+"'";
			}
			
        return c_d_WasteService.getOfficerWardWiseCount(fromDATE,toDATE, zone,requestBy);
    }
	
	@GetMapping("/getVendorZoneWiseCount")
    public List<?> getVendorZoneWiseCount(
			@RequestParam(value = "fromDate", required = true) String fromDATE,
			@RequestParam(value = "toDate", required = true) String toDATE,
			@RequestParam(value = "requestBy", required = false) String requestBy
			) {
		
			if (requestBy == null || requestBy.isBlank()) {
				requestBy = "'Vendor','councillor','IE'";
			}
			else {
				requestBy = "'"+requestBy+"'";
			}
			
        return c_d_WasteService.getVendorZoneWiseCount(fromDATE,toDATE,requestBy);
    }
	
	@GetMapping("/getVendorWardWiseCount")
    public List<?> getVendorWardWiseCount(
			@RequestParam(value = "fromDate", required = true) String fromDATE,
			@RequestParam(value = "toDate", required = true) String toDATE,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "requestBy", required = false) String requestBy
			) {
		
			if (requestBy == null || requestBy.isBlank()) {
				requestBy = "'Vendor','councillor','IE'";
			}
			else {
				requestBy = "'"+requestBy+"'";
			}
			
        return c_d_WasteService.getVendorWardWiseCount(fromDATE,toDATE, zone,requestBy);
    }
	
	@GetMapping("/getListByStatus")
    public List<?> getComplaintList(
			@RequestParam(value = "fromDate", required = true) String fromDATE,
			@RequestParam(value = "toDate", required = true) String toDATE,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "status", required = true) String status,
			@RequestParam(value = "requestBy", required = false) String requestBy
			) {
		
			if (requestBy == null || requestBy.isBlank()) {
				requestBy = "'Vendor','Officer','councillor','IE'";
			}
			else {
				//requestBy = "'"+requestBy+"'";
			}
				
        return c_d_WasteService.getListByStatus(fromDATE,toDATE, zone, ward, status,requestBy);
    }
	
	@GetMapping("/getWardSumReport")
    public List<?> getWardSumReport(
			@RequestParam(value = "fromDate", required = false) String fromDATE,
			@RequestParam(value = "toDate", required = false) String toDATE,
			@RequestParam(value = "ward", required = true) String ward
			) {
        return c_d_WasteService.getWardSumReport(ward);
    }
	
	@GetMapping("/getTicketDetailsByWLID")
    public List<?> getTicketDetailsByWLID(
			@RequestParam(value = "wlid", required = true) String wlid
			) {
        return c_d_WasteService.getTicketDetailsByWLID(wlid);
    }
}
