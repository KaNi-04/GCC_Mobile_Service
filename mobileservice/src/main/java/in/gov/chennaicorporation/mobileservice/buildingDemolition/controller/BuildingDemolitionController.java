package in.gov.chennaicorporation.mobileservice.buildingDemolition.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.buildingDemolition.service.BuildingDemolitionActivity;

@RequestMapping("/gccofficialapp/api/buildingdemolition/")
@RestController("buildingDemolitionRest")
public class BuildingDemolitionController {
	@Autowired
	BuildingDemolitionActivity buildingDemolitionService;
	
	@GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> getBuildingList(
            @RequestParam String loginId) {
        return ResponseEntity.ok(buildingDemolitionService.getBuildingList(loginId));
    }
	
	@PostMapping("/update")
    public ResponseEntity<List<Map<String, Object>>> updateDetails(
            @RequestParam String id,
            @RequestParam String status,
            @RequestParam String cby,
            @RequestParam(required = true) String latitude,
            @RequestParam(required = true) String longitude,
            @RequestParam(required = true) String remarks,
            @RequestParam(required = true) MultipartFile file) {
        return ResponseEntity.ok(buildingDemolitionService.updateDetails(id, status, cby, latitude, longitude, remarks, file));
    }

	// Zone wise Report
    @GetMapping("/report/zone")
    public ResponseEntity<List<Map<String, Object>>> zoneReport() {
        return ResponseEntity.ok(buildingDemolitionService.zoneReport());
    }

    // Ward wise Report (filter by zone)
    @GetMapping("/report/ward")
    public ResponseEntity<List<Map<String, Object>>> wardReport(@RequestParam String zone) {
        return ResponseEntity.ok(buildingDemolitionService.wardReport(zone));
    }

    // Pending list Report (filter by ward)
    @GetMapping("/report/pending")
    public ResponseEntity<List<Map<String, Object>>> pendingReport(@RequestParam String ward) {
        return ResponseEntity.ok(buildingDemolitionService.pendingReport(ward));
    }

    // Completed list Report (filter by ward)
    @GetMapping("/report/completed")
    public ResponseEntity<List<Map<String, Object>>> completedReport(@RequestParam String ward) {
        return ResponseEntity.ok(buildingDemolitionService.completedReport(ward));
    }
}
