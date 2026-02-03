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
        return ResponseEntity
                .ok(buildingDemolitionService.updateDetails(id, status, cby, latitude, longitude, remarks, file));
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
    // --- New Endpoints Replicated from Construction Guidelines ---

    @PostMapping("/saveFeedback")
    public ResponseEntity<List<Map<String, Object>>> saveDemolitionFeedback(
            @RequestParam(required = true) String bdid,
            @RequestParam(required = true) String bdsid,
            @RequestParam(required = true) String cby,
            @RequestParam(required = true) String latitude,
            @RequestParam(required = true) String longitude,
            @RequestParam(required = true) String zone,
            @RequestParam(required = true) String ward,
            @RequestParam(required = true) String q1,
            @RequestParam(required = true) String q2,
            @RequestParam(required = true) String q3,
            @RequestParam(required = true) String q4,
            @RequestParam(required = true) String q5,
            @RequestParam(required = true) String q6,
            @RequestParam(required = true) String q7,
            @RequestParam(required = true) String q8,
            @RequestParam(required = true) String q9,
            @RequestParam(required = true) String q10,
            @RequestParam(required = true) String q11,
            @RequestParam(required = true) String q12,
            @RequestParam(required = true) String q13,
            @RequestParam(required = true) String q14,
            @RequestParam(required = true) String q15,
            @RequestParam(required = true) String q16,
            @RequestParam(required = true) String q17,
            @RequestParam(required = true) String q18,
            @RequestParam(required = true) MultipartFile image1,
            @RequestParam(required = true) MultipartFile image2,
            @RequestParam(required = true) MultipartFile image3,
            @RequestParam(required = true) MultipartFile image4,
            @RequestParam(required = true) MultipartFile image5) {
        return ResponseEntity.ok(buildingDemolitionService.saveDemolitionFeedback(
                bdid, bdsid, cby, latitude, longitude, zone, ward,
                q1, q2, q3, q4, q5, q6, q7, q8, q9, q10,
                q11, q12, q13, q14, q15, q16, q17, q18,
                image1, image2, image3, image4, image5));
    }

    @GetMapping("/getRevisitList")
    public ResponseEntity<List<Map<String, Object>>> getRevisitDemolitionList(
            @RequestParam String loginId,
            @RequestParam String latitude,
            @RequestParam String longitude) {
        return ResponseEntity.ok(buildingDemolitionService.getRevisitDemolitionList(loginId, latitude, longitude));
    }

    @PostMapping("/saveAfterNoticeDetails")
    public ResponseEntity<List<Map<String, Object>>> saveDemolitionAfterNotice(
            @RequestParam String bdid,
            @RequestParam String bdsid,
            @RequestParam String bdgiid,
            @RequestParam String remarks,
            @RequestParam String status,
            @RequestParam String zone,
            @RequestParam String ward,
            @RequestParam String loginId,
            @RequestParam String latitude,
            @RequestParam String longitude,
            @RequestParam MultipartFile mainfile) {
        return ResponseEntity.ok(buildingDemolitionService.saveDemolitionAfterNotice(
                bdid, bdsid, bdgiid, remarks, status, zone, ward, loginId, latitude, longitude, mainfile));
    }

    @PostMapping("/saveFinalNoticeDetails")
    public ResponseEntity<List<Map<String, Object>>> saveDemolitionFinalNotice(
            @RequestParam String bdid,
            @RequestParam String bdsid,
            @RequestParam String bdgiid,
            @RequestParam String remarks,
            @RequestParam String status,
            @RequestParam String zone,
            @RequestParam String ward,
            @RequestParam String loginId,
            @RequestParam String latitude,
            @RequestParam String longitude,
            @RequestParam MultipartFile mainfile) {
        return ResponseEntity.ok(buildingDemolitionService.saveDemolitionFinalNotice(
                bdid, bdsid, bdgiid, remarks, status, zone, ward, loginId, latitude, longitude, mainfile));
    }
}
