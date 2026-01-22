package in.gov.chennaicorporation.mobileservice.roadcut.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.roadcut.service.RoadCutService;

@RequestMapping("/gccofficialapp/api/readcut/")
@RestController("restRoadCut")
public class RoadCutController {
	@Autowired
    private RoadCutService roadCutService;

    @GetMapping("/fetch-roadcut-json")
    public ResponseEntity<?> loadData(
    		@RequestParam("fromdate") String fromDate,
    		@RequestParam("todate") String toDate) {
        try {
            roadCutService.fetchAndSave(fromDate,toDate);
            return ResponseEntity.ok("Data saved successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    // TNEB & Metro Water
    @GetMapping(value="/get-roadcut-list")
	public List<?> getRoadCutList(@RequestParam(value="loginId", required = true) String cby){
		return roadCutService.getRoadCutList(cby);
	}
    
    
    @GetMapping(value="/check-roadcut-loation")
	public List<?> getLoactionCheck(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value="refId", required = true) String refid,
			@RequestParam(value="streetId", required = true) String streetid,
			@RequestParam(value="streetName", required = true) String streetname
			){
		return roadCutService.getLoactionCheck(cby,refid,streetid,streetname);
	}
    
    
    @GetMapping(value="/get-roadcut-details")
	public List<?> getRoadCutDetails(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value="refId", required = true) String refid
			){
		return roadCutService.getRoadCutDetails(cby,refid);
	}
    
    @PostMapping(value="/saveAction")
    public List<?> saveFeedback(
		    @RequestParam("uid") String uid,
		    @RequestParam("cby") String cby,
		    @RequestParam("latitude") String latitude,
		    @RequestParam("longitude") String longitude,
		    @RequestParam("zone") String zone,
		    @RequestParam("ward") String ward,
		    @RequestParam("streetid") String streetid,
		    @RequestParam("streetname") String streetname,
		    @RequestParam("action") String action,
		    @RequestParam("workstatus") String workstatus,
		    @RequestParam("remarks") String remarks,
		    @RequestParam("nocid") String nocid,
		    @RequestParam(value = "file", required = false) MultipartFile filedata) {
    	
    	return roadCutService.saveStatusUpdate(
                uid, cby, latitude, longitude, zone, ward, streetid,
                streetname, action, workstatus, remarks, nocid, filedata
        );
    	
    }
    
    // AE GCC Officer
    @GetMapping(value="/get-roadcut-list-offical")
	public List<?> getRoadCutList_offical(@RequestParam(value="loginId", required = true) String cby){
		return roadCutService.getRoadCutList_offical(cby);
	}
    
    @GetMapping(value="/check-roadcut-loation-offical")
	public List<?> getLoactionCheck_offical(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value="refId", required = true) String refid,
			@RequestParam(value="streetId", required = true) String streetid,
			@RequestParam(value="streetName", required = true) String streetname
			){
		return roadCutService.getLoactionCheck(cby,refid,streetid,streetname);
	}
    
    
    @GetMapping(value="/get-roadcut-details-offical")
	public List<?> getRoadCutDetails_offical(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value="refId", required = true) String refid
			){
		return roadCutService.getRoadCutDetails_offical(cby,refid);
	}
    
    @PostMapping(value="/saveAction-offical")
    public List<?> saveFeedback_offical(
		    @RequestParam("uid") String uid,
		    @RequestParam("cby") String cby,
		    @RequestParam("latitude") String latitude,
		    @RequestParam("longitude") String longitude,
		    @RequestParam("zone") String zone,
		    @RequestParam("ward") String ward,
		    @RequestParam("streetid") String streetid,
		    @RequestParam("streetname") String streetname,
		    @RequestParam("action") String action,
		    @RequestParam("workstatus") String workstatus,
		    @RequestParam("remarks") String remarks,
		    @RequestParam("nocid") String nocid,
		    @RequestParam(value = "file", required = false) MultipartFile filedata) {
    	
    	return roadCutService.saveStatusUpdate_offical(
                uid, cby, latitude, longitude, zone, ward, streetid,
                streetname, action, workstatus, remarks, nocid, filedata
        );
    	
    }
}
