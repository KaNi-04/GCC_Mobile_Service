package in.gov.chennaicorporation.mobileservice.ie_complaints.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import in.gov.chennaicorporation.mobileservice.ie_complaints.service.ieComplaintService;

@RequestMapping("/gccofficialapp/api/iecomplaints")
@RestController("gccofficialappiecomplaints")
public class ieComplaintsController {

    @Autowired
    ieComplaintService iecomplaintservice;

    /*
     * @PostMapping("/bovbinscomplaint")
     * public List<?> bovbinscomplaint(
     * 
     * @RequestParam(value = "zone", required = true) String zone,
     * 
     * @RequestParam(value = "ward", required = true) String ward,
     * 
     * @RequestParam(value = "street_name", required = false) String street_name,
     * 
     * @RequestParam(value = "latitude", required = true) String latitude,
     * 
     * @RequestParam(value = "longitude", required = true) String longitude,
     * 
     * @RequestParam(value = "cby", required = true) String cby,
     * 
     * @RequestParam(value = "vechileno", required = true) String vechileno,
     * 
     * @RequestParam(value = "image", required = true) MultipartFile image,
     * 
     * @RequestParam(value = "remarks", required = true) String remarks) {
     * return iecomplaintservice.bovbinscomplaint(
     * zone,
     * ward,
     * street_name,
     * latitude,
     * longitude,
     * cby,
     * vechileno,
     * image,
     * remarks
     * 
     * );
     * }
     */

    // equipmentmaster

    @GetMapping("/getequipement")
    public List<Map<String, Object>> getEquipmentList() {
        return iecomplaintservice.getEquipmentList();
    }

    // save singleapi

    /*
     * @PostMapping("/saveComplaint")
     * public List<?> saveComplaint(
     * 
     * @RequestParam(value = "complaint_id", required = true) Integer complaint_id,
     * 
     * @RequestParam(value = "zone", required = true) String zone,
     * 
     * @RequestParam(value = "ward", required = true) String ward,
     * 
     * @RequestParam(value = "street_name", required = false) String street_name,
     * 
     * @RequestParam(value = "latitude", required = true) String latitude,
     * 
     * @RequestParam(value = "longitude", required = true) String longitude,
     * 
     * @RequestParam(value = "cby", required = true) String cby,
     * 
     * @RequestParam(value = "vechileno", required = false) String vechileno,
     * // @RequestParam(value = "equipment_id", required = false) Long equipment_id,
     * 
     * @RequestParam(value = "equipment_id", required = false) List<Long>
     * equipment_id,
     * 
     * @RequestParam(value = "sweeping_id", required = false) List<Long>
     * sweeping_id,
     * 
     * @RequestParam(value = "cleaningDetails", required = false) String
     * cleaningDetails,
     * // @RequestParam(value = "area_id", required = false) Integer area_id,
     * // @RequestParam(value = "equipment_check_id", required = false) List<Long>
     * // equipment_check_id,
     * 
     * @RequestParam(value = "image", required = true) MultipartFile image,
     * 
     * @RequestParam(value = "q_id", required = false) String q_id,
     * 
     * @RequestParam(value = "remarks", required = false) String remarks) {
     * 
     * return iecomplaintservice.saveComplaint(
     * complaint_id,
     * zone,
     * ward,
     * street_name,
     * latitude,
     * longitude,
     * cby,
     * vechileno,
     * equipment_id,
     * sweeping_id,
     * // area_id,
     * // equipment_check_id,
     * cleaningDetails,
     * image,
     * remarks,
     * q_id);
     * }
     */

    // sweeping
    @GetMapping("/getChecklist")
    public List<Map<String, Object>> getChecklistList() {
        return iecomplaintservice.getChecklistList();
    }

    @GetMapping("/getfeedbackquestions")
    public List<Map<String, Object>> getFeedbackQuestions(
            @RequestParam(value = "complaint_id", required = true) String complaint_id,
            @RequestParam(value = "loginId", required = false) String loginId) {
        return iecomplaintservice.getParentQuestionsList(complaint_id, loginId);
    }

    // updated

    @PostMapping("/savecomplaint")
    public List<?> saveComplaint(
            @RequestParam("complaint_id") Integer complaint_id,
            @RequestParam("zone") String zone,
            @RequestParam("ward") String ward,
            @RequestParam(value = "street_name") String street_name,
            @RequestParam("street_id") Integer street_id,
            @RequestParam("latitude") String latitude,
            @RequestParam("longitude") String longitude,
            @RequestParam("cby") String cby,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "questionAnswers", required = false) String questionAnswers,
            @RequestParam(value = "remarks") String remarks) {

        return iecomplaintservice.saveComplaint(
                complaint_id,
                zone,
                ward,
                street_name,
                street_id,
                latitude,
                longitude,
                cby,
                image,
                questionAnswers,
                remarks);
    }

    // si/so
    /*
     * @GetMapping("/getZoneComplaintList")
     * public ResponseEntity<List<Map<String, Object>>>
     * getZoneComplaintList(@RequestParam("loginId") String loginId) {
     * 
     * List<Map<String, Object>> complaintListforFeedback = iecomplaintservice
     * .getZoneComplaintListforFeedback(loginId);
     * 
     * return ResponseEntity.ok(complaintListforFeedback);
     * }
     */
    @GetMapping("/getWardComplaintList")
    public ResponseEntity<Map<String, Object>> getZoneComplaintList(
            @RequestParam("loginId") String loginId) {

        Map<String, Object> result = iecomplaintservice.getZoneComplaintListforFeedback(loginId);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/saveCompletion")
    public ResponseEntity<Map<String, Object>> saveCompletion(
            @RequestParam("ref_id") String refId,
            @RequestParam("remarks") String remarks,
            @RequestParam("image") MultipartFile image,
            @RequestParam("zone") String zone,
            @RequestParam("ward") String ward,
            @RequestParam(value = "street_name") String street_name,
            @RequestParam("street_id") Integer street_id,
            @RequestParam("latitude") String latitude,
            @RequestParam("longitude") String longitude,
            @RequestParam("cby") String cby) {

        Map<String, Object> response = iecomplaintservice.saveCompletion(refId, remarks, image, cby, zone, ward,
                street_name, street_id, latitude, longitude);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getAEEComplaintList")
    public ResponseEntity<Map<String, Object>> getAEEComplaintList(
            @RequestParam("loginId") String loginId) {

        return ResponseEntity.ok(
                iecomplaintservice.getZoneComplaintListForAEE(loginId));
    }

    @GetMapping("/zoneComplaintReport")
    public ResponseEntity<Map<String, Object>> getZoneComplaintReport(
            @RequestParam("complaintId") Integer complaintId,
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate) {

        return ResponseEntity.ok(
                iecomplaintservice.getZoneComplaintReport(
                        complaintId,
                        fromDate,
                        toDate));
    }

    @GetMapping("/wardComplaintReport")
    public ResponseEntity<Map<String, Object>> getWardComplaintReport(
            @RequestParam("zone") String zone,
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate,
            @RequestParam("complaintId") Integer complaintId) {

        return ResponseEntity.ok(
                iecomplaintservice.getWardComplaintReport(
                        zone,
                        fromDate,
                        toDate, complaintId));
    }

    @GetMapping("/wardComplaintReportDetails")
    public ResponseEntity<Map<String, Object>> getWardComplaintReportDetails(

            @RequestParam("ward") String ward,
            @RequestParam("status") String status,
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate,
            @RequestParam("complaintId") Integer complaintId) {

        return ResponseEntity.ok(
                iecomplaintservice.getWardComplaintReportDetails(
                        ward, status, fromDate, toDate, complaintId));
    }

    @GetMapping("/getComplaintMaster")
    public ResponseEntity<Map<String, Object>> getComplaintMaster() {

        return ResponseEntity.ok(
                iecomplaintservice.getComplaintMaster());
    }

}
