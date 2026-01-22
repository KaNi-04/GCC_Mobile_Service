package in.gov.chennaicorporation.mobileservice.gccDomesticWaste.Controller;
import in.gov.chennaicorporation.mobileservice.gccDomesticWaste.Service.DomesticWasteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gccofficialapp/api/domesticwaste")
public class DomesticWasteController {

    @Autowired
    DomesticWasteService domesticWasteService;

    @PostMapping("/save-request")
    public ResponseEntity<?> saveRequest(
        @RequestParam String user_name,
        @RequestParam String mobile,
        @RequestParam String address,
        @RequestParam String latitude,
        @RequestParam String longitude,
        @RequestParam String zone,
        @RequestParam String division,
        @RequestParam String street_name,
        @RequestParam(value="street_id", required = false) String street_id,
        @RequestParam String remarks,
        @RequestParam(value="sofa_type", required = false) String sofa_type,
        @RequestParam("items") String itemsJson,
        @RequestParam (name = "image") MultipartFile imageFile,
        @RequestParam(value="is_app", required = false) String is_app) {

    try {
        Map<String, Object> result = domesticWasteService.saveMultipleWasteRequests(
                user_name, mobile, address,
                latitude, longitude, zone, division,
                street_name, street_id, remarks,
                imageFile, itemsJson, sofa_type, is_app
        );

        return ResponseEntity.ok(result);

	    } catch (Exception e) {
	        return ResponseEntity.status(500).body(Map.of("status", false, "message", e.getMessage()));
	    }
    }


    @GetMapping("/get-all-requests")
    public ResponseEntity<List<Map<String, Object>>> getAllWasteRequests() {
        List<Map<String, Object>> results = domesticWasteService.getAllWasteRequests();
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/get-all-requests-by-login")
    public ResponseEntity<List<Map<String, Object>>> getAllWasteRequests(@RequestParam String loginId) {
        List<Map<String, Object>> results = domesticWasteService.getAllWasteRequestsbylogin(loginId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/get-all-masters")
    public ResponseEntity<List<Map<String, Object>>> getAllmasters() {
        List<Map<String, Object>> results = domesticWasteService.getAllmasters();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/get-user-bymobile")
    public ResponseEntity<List<Map<String, Object>>> getuserbymobile(@RequestParam String mobile) {
    	List<Map<String,Object>> results =domesticWasteService.getuserbymobile(mobile);
    	return ResponseEntity.ok(results);
    }

    @GetMapping("/get-user-AE2")
    public ResponseEntity<List<Map<String, Object>>> getUserAE2(@RequestParam String loginId) {
        List<Map<String, Object>> results = domesticWasteService.getuserAE2(loginId);
        return ResponseEntity.ok(results);
    }

    // Zonal Requests based on loginId
    @GetMapping("/get-user-zonal")
    public ResponseEntity<List<Map<String, Object>>> getUserZonal(@RequestParam String loginId) {
        List<Map<String, Object>> results = domesticWasteService.getuserZonal(loginId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/userdetailsfilter")
    public ResponseEntity<List<Map<String, Object>>> userdetailsfilter(@RequestParam int id) {
        List<Map<String,Object>> results =domesticWasteService.userdetailsfilter(id);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/action-taken")
    public ResponseEntity<?> saveActionTaken(
            @RequestParam String reqid,
            @RequestParam(value = "image") MultipartFile image,
            @RequestParam("created_by") int createdBy,
            @RequestParam String latitude,
            @RequestParam String longitude
    ) {
        Map<String, Object> result = domesticWasteService.actionTaken(reqid, image, createdBy,latitude,longitude);

        if ((boolean) result.get("status")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/reject-user-req")
    public ResponseEntity<?> rejectuserreq(@RequestParam String requestId) {
    	Map<String, Object> result = domesticWasteService.rejectuserreq(requestId);
    	return ResponseEntity.ok(result);
    }

    @GetMapping("/verify-latlong")
    public ResponseEntity<Map<String, Object>> verifyLatLong(
            @RequestParam String requestId,
            @RequestParam String latitude,
            @RequestParam String longitude) {

        boolean result = domesticWasteService.verifylatlong(requestId, latitude, longitude);

        Map<String, Object> response = Map.of(
                "status", true,
                "message", String.valueOf(result)
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reject-request-officer")
    public ResponseEntity<Map<String, Object>> rejectUserRequestByOfficer(
            @RequestParam String requestId,
            @RequestParam String officer_remark,
            @RequestParam String rejecttype,
            @RequestParam int cancelled_by,
            @RequestParam String latitude,
            @RequestParam String longitude) {

        Map<String, Object> result = domesticWasteService.rejectuserreqofficer(requestId, officer_remark, rejecttype, cancelled_by,latitude,longitude);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping(value="/getZoneReport")
	public List<?> getZoneReport(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate
			){
		return domesticWasteService.getZoneReport(fromDate, toDate);
	}
    
    @GetMapping(value="/getWardReport")
	public List<?> getWardReport(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="zone", required = true) String zone
			){
		return domesticWasteService.getWardReport(fromDate, toDate, zone);
	}
    
    @GetMapping(value="/getWardStatusDetails")
	public List<?> getWardStatusDetails(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="ward", required = true) String ward,
			@RequestParam(value="status", required = true) String status
			){
		return domesticWasteService.getWardStatusDetails(fromDate, toDate, ward, status);
	}
}
