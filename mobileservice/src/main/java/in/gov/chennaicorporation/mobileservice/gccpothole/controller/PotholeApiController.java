package in.gov.chennaicorporation.mobileservice.gccpothole.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccpothole.service.PotholeService;


@RequestMapping("/gccofficialapp/api/pothole")
@RestController("gccofficialapppothole")

public class PotholeApiController {


    @Autowired
    private PotholeService potholeService;


    @PostMapping("/save-complaint")
    public ResponseEntity<Map<String, Object>> saveNewComplaints(

            @RequestParam("zone") String zone,
            @RequestParam("ward") String ward,
            @RequestParam(required = false, name = "latitude") String latitude,
            @RequestParam(required = false, name = "longitude") String longitude,
            @RequestParam(required = false, name = "length") String length,
            @RequestParam(required = false, name = "width") String width,
            @RequestParam(required = false, name = "area") String area,
            @RequestParam("case_id") String caseId,
            @RequestParam("street_id") String streetId,
            @RequestParam("street_name") String streetName,
            @RequestParam("status_id") String statusId,
            @RequestParam(required = true, name = "complaint_photo") MultipartFile complaintPhoto,
            @RequestParam("login_id") String loginId,
            @RequestParam(required = false, name = "user_length") String userLength,
            @RequestParam(required = false, name = "user_width") String userWidth,
            @RequestParam(required = false, name = "user_height") String userHeight,
            @RequestParam("risk_level") String riskLevel,
            @RequestParam(required = false, name = "damage_type") String damage_type) {

        // File upload paths
        String complaintPhotoPath = (complaintPhoto != null) ?
                potholeService.fileUpload(complaintPhoto, "complaint_photo") : "";

        // Save to DB
        String saveStatus = potholeService.saveComplaintDetails(
                zone, ward, latitude, longitude, length, width, area, streetId,
                streetName, statusId, complaintPhotoPath, loginId, caseId, userLength, userWidth, userHeight, riskLevel, damage_type);

        Map<String, Object> response = new HashMap<>();
        if (saveStatus == null || saveStatus.isEmpty() || saveStatus.equals("error")) {
            response.put("status", false);
            response.put("message", "failed");
            response.put("description", "Failed to save the details");
            return ResponseEntity.ok(response);
        } else if (saveStatus.equals("duplicate")) {
            response.put("status", false);
            response.put("message", "failed");
            response.put("description", "Complaints already exist");
            return ResponseEntity.ok(response);
        }

        response.put("status", true);
        response.put("message", "success");
        response.put("description", "Details were saved successfully");
        return ResponseEntity.ok(response);
    }

    // Status Master
    @GetMapping("/status")
    public List<Map<String, Object>> getAllStatus() {
        return potholeService.getAllStatus();
    }

    // AE Complaint List against ward
    @GetMapping("/aecomplaintlist")
    public ResponseEntity<Map<String, Object>> getComplaintsByWard(@RequestParam("aeUserId") String aeUserId) {
        List<Map<String, Object>> result = potholeService.getComplaintsByWard(aeUserId);


        Map<String, Object> response = new HashMap<>();
        if (result.isEmpty()) {
            response.put("status", false);
            response.put("message", "No data found");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", true);
            response.put("message", "Data found");
            response.put("data", result);
        }

        //System.out.println("result: " + result);
        return ResponseEntity.ok(response);
    }

    // Vendor Complaint List against ward
    @GetMapping("/vendor")
    public ResponseEntity<Map<String, Object>> getComplaintsByVendor(@RequestParam("vendorUserId") String vendorUserId) {
        List<Map<String, Object>> result = potholeService.getComplaintsByVendorId(vendorUserId);

        Map<String, Object> response = new HashMap<>();
        if (result.isEmpty()) {
            response.put("status", false);
            response.put("message", "No data found");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", true);
            response.put("message", "Data found");
            response.put("data", result);
        }

        return ResponseEntity.ok(response);
    }

    // My Complaint List
    @GetMapping("/viewRequestList")
    public ResponseEntity<Map<String, Object>> viewRequestList(@RequestParam("loginId") String loginId) {

        List<Map<String, Object>> result = potholeService.getRequestList(loginId);

        Map<String, Object> response = new HashMap<>();
        if (result.isEmpty()) {
            response.put("status", false);
            response.put("message", "No data found");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", true);
            response.put("message", "Data found");
            response.put("data", result);
        }

        return ResponseEntity.ok(response);

    }

    // Vendor Update

    @PostMapping("/updateComplaint")
    public ResponseEntity<Map<String, Object>> updateComplaint(@RequestParam("zone") String zone,
                                                               @RequestParam("ward") String ward, @RequestParam(name = "latitude") String latitude,
                                                               @RequestParam(name = "longitude") String longitude,
                                                               @RequestParam(required = false, name = "length") String length,
                                                               @RequestParam(required = false, name = "width") String width,
                                                               @RequestParam(required = false, name = "area") String area, 
                                                               @RequestParam("street_id") String streetId,
                                                               @RequestParam("street_name") String streetName, 
                                                               @RequestParam("status_id") String statusId,
                                                               @RequestParam("complaint_photo") MultipartFile vendorPhoto, 
                                                               @RequestParam("login_id") String loginId,
                                                               @RequestParam("complaint_no") String complaint_no, 
                                                               @RequestParam(required = false, name = "vendor_length") String vendorLength,
                                                               @RequestParam(required = false, name = "vendor_width") String vendorWidth, 
                                                               @RequestParam(required = false, name = "vendor_height") String vendorHeight,
                                                               @RequestParam(required = false, name = "ae_length") String aeLength, 
                                                               @RequestParam(required = false, name = "ae_width") String aeWidth,
                                                               @RequestParam(required = false, name = "ae_height") String aeHeight) {

        Map<String, Object> response = new HashMap<>();

        String vendorPhotoPath = null;
        //LocalDate updatedDate = LocalDate.now();
        LocalDateTime updatedDate = LocalDateTime.now();

        if (statusId.equals("2")) {
            vendorPhotoPath = (vendorPhoto != null) ? potholeService.fileUpload(vendorPhoto, "vendor_photo") : "";

        } else if (statusId.equals("4")) {
            vendorPhotoPath = (vendorPhoto != null) ? potholeService.fileUpload(vendorPhoto, "rectified_photo") : "";

        } else if (statusId.equals("3")) {
            vendorPhotoPath = (vendorPhoto != null) ? potholeService.fileUpload(vendorPhoto, "reopen_photo") : "";

        } else if (statusId.equals("5")) {
            vendorPhotoPath = (vendorPhoto != null) ? potholeService.fileUpload(vendorPhoto, "completed_photo") : "";

        } else {
            vendorPhotoPath = null;

        }

        String updationStatus = potholeService.updateComplaintbyVendor(statusId, loginId, updatedDate, vendorLength, vendorWidth, vendorHeight,
                vendorPhotoPath, aeLength, aeWidth, aeHeight, complaint_no);

        if (updationStatus.equals("Success")) {
            response.put("status", true);
            response.put("message", "success");
            response.put("description", "Details were saved successfully");
            System.out.println(response);
            return ResponseEntity.ok(response);
        }

        response.put("status", false);
        response.put("message", "Failed to update complaint Details");
        //System.err.println("Pot Hole : "+ updationStatus);
        return ResponseEntity
        	    .status(HttpStatus.INTERNAL_SERVER_ERROR)
        	    .body(response);

    }

    @GetMapping("/validateLocation")
    public ResponseEntity<Map<String, Object>> validateLocation(@RequestParam(name = "latitude") String latitude,
                                                                @RequestParam(name = "longitude") String longitude, @RequestParam("complaint_no") String complaint_no) {

        boolean locationStatus = false;

        Map<String, Object> response = new HashMap<>();

        if (!latitude.isEmpty() && !longitude.isEmpty() && latitude != null && longitude != null) {
            locationStatus = potholeService.checkLocation(latitude, longitude);
        }

        if (locationStatus) {
            response.put("status", true);
            response.put("message", "Location Matched");
            return ResponseEntity.ok(response);
        }

        response.put("status", false);
        response.put("message", "Location Issue");
        response.put("description", "Please go to nearest location");
        return ResponseEntity.ok(response);

    }

    // report related api

    //  zone wise AE pending and completed

    @GetMapping("/ae-zone-wise")
    public ResponseEntity<Map<String, Object>> getZoneWiseAEReport(
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate,
            @RequestParam(required = false, name = "damage_type") String damage_type) {

        List<Map<String, Object>> report = potholeService.getZoneWiseAEReport(fromDate, toDate, damage_type);


        Map<String, Object> response = new HashMap<>();
        if (report.isEmpty()) {
            response.put("status", false);
            response.put("message", "No data found");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", true);
            response.put("message", "Data found");
            response.put("data", report);
        }

        return ResponseEntity.ok(response);

    }


    @GetMapping("/ae-ward-wise")
    public ResponseEntity<Map<String, Object>> getWardWiseAEReport(
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate,
            @RequestParam("zone") String zone,
            @RequestParam(required = false, name = "damage_type") String damage_type) {

        List<Map<String, Object>> report = potholeService.getWardWiseAEReport(fromDate, toDate, zone, damage_type);
        Map<String, Object> response = new HashMap<>();
        if (report.isEmpty()) {
            response.put("status", false);
            response.put("message", "No data found");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", true);
            response.put("message", "Data found");
            response.put("data", report);
        }

        return ResponseEntity.ok(response);

    }


    // report api for zone wise Vendor pending and completed


    @GetMapping("/vendor-zone-wise")
    public ResponseEntity<Map<String, Object>> getZoneVendorWiseReport(
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate,
            @RequestParam(required = false, name = "damage_type") String damage_type) {

        List<Map<String, Object>> report = potholeService.getZoneWiseVendorReport(fromDate, toDate, damage_type);
        Map<String, Object> response = new HashMap<>();
        if (report.isEmpty()) {
            response.put("status", false);
            response.put("message", "No data found");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", true);
            response.put("message", "Data found");
            response.put("data", report);
        }

        return ResponseEntity.ok(response);
    }


    @GetMapping("/vendor-ward-wise")
    public ResponseEntity<Map<String, Object>> getWardWiseVendorReport(
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate,
            @RequestParam("zone") String zone,
            @RequestParam(required = false, name = "damage_type") String damage_type) {

        List<Map<String, Object>> report = potholeService.getWardWiseVendorReport(fromDate, toDate, zone, damage_type);
        Map<String, Object> response = new HashMap<>();
        if (report.isEmpty()) {
            response.put("status", false);
            response.put("message", "No data found");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", true);
            response.put("message", "Data found");
            response.put("data", report);
        }

        return ResponseEntity.ok(response);
    }


    @GetMapping("/getdetailsbycomplaintno")
    public ResponseEntity<Map<String, Object>> getDetailsByComplaintNo(@RequestParam("complaintNo") String complaintNo) {

        List<Map<String, Object>> result = potholeService.getDetailsByComplaintNo(complaintNo);

        Map<String, Object> response = new HashMap<>();
        if (result.isEmpty()) {
            response.put("status", false);
            response.put("message", "No data found");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", true);
            response.put("message", "Data found");
            response.put("data", result);
        }

        return ResponseEntity.ok(response);

    }


    // get the details by between date,zone and ward
    @GetMapping("/getcomplaintdetails")
    public ResponseEntity<Map<String, Object>> getComplaintDetails(

            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate,
            @RequestParam("zone") String zone,
            @RequestParam("ward") String ward,
            @RequestParam("streetId") String streetId,
            @RequestParam(required = false, name = "damage_type") String damage_type,
            @RequestParam(required = false, name = "status") String status
            ) {

        List<Map<String, Object>> result = potholeService.getAllComplaintNo(fromDate, toDate, zone, ward, streetId, damage_type, status);

        Map<String, Object> response = new HashMap<>();
        if (result.isEmpty()) {
            response.put("status", false);
            response.put("message", "No data found");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", true);
            response.put("message", "Data found");
            response.put("data", result);
        }

        return ResponseEntity.ok(response);

    }

    @GetMapping("/getstreetdetails-ae")
    public ResponseEntity<Map<String, Object>> getStreetDetails(

            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate,
            @RequestParam("zone") String zone,
            @RequestParam("ward") String ward,
            @RequestParam(required = false, name = "damage_type") String damage_type) {

        List<Map<String, Object>> result = potholeService.getStreetDetailsAE(fromDate, toDate, zone, ward, damage_type);

        Map<String, Object> response = new HashMap<>();
        if (result.isEmpty()) {
            response.put("status", false);
            response.put("message", "No data found");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", true);
            response.put("message", "Data found");
            response.put("data", result);
        }

        return ResponseEntity.ok(response);

    }

    @GetMapping("/getstreetdetails-vendor")
    public ResponseEntity<Map<String, Object>> getStreetDetailsVendor(
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate,
            @RequestParam("zone") String zone,
            @RequestParam("ward") String ward,
            @RequestParam(required = false, name = "damage_type") String damage_type) {

        List<Map<String, Object>> result = potholeService.getStreetDetailsVE(fromDate, toDate, zone, ward, damage_type);

        Map<String, Object> response = new HashMap<>();
        if (result.isEmpty()) {
            response.put("status", false);
            response.put("message", "No data found");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", true);
            response.put("message", "Data found");
            response.put("data", result);
        }

        return ResponseEntity.ok(response);

    }

    // to save user complaints - new API

    @PostMapping("/save-user-complaint")
    public ResponseEntity<Map<String, Object>> saveUserComplaints(
            @RequestParam("zone") String zone,
            @RequestParam("ward") String ward,
            @RequestParam(required = false, name = "latitude") String latitude,
            @RequestParam(required = false, name = "longitude") String longitude,
            @RequestParam(required = false, name = "length") String length,
            @RequestParam(required = false, name = "width") String width,
            @RequestParam(required = false, name = "area") String area,
            @RequestParam("case_id") String caseId,
            @RequestParam("street_id") String streetId,
            @RequestParam("street_name") String streetName,
            @RequestParam("status_id") String statusId,
            @RequestParam(required = true, name = "complaint_photo") MultipartFile complaintPhoto,
            @RequestParam(required = false, name = "user_length") String userLength,
            @RequestParam(required = false, name = "user_width") String userWidth,
            @RequestParam(required = false, name = "user_height") String userHeight,
            @RequestParam("risk_level") String riskLevel) {

        // File upload paths
        String complaintPhotoPath = (complaintPhoto != null) ?
                potholeService.fileUpload(complaintPhoto, "user_complaint_photo") : "";

        // Save to DB
        String saveStatus = potholeService.saveUserComplaintDetails(
                zone, ward, latitude, longitude, length, width, area, streetId,
                streetName, statusId, complaintPhotoPath, caseId, userLength, userWidth, userHeight, riskLevel);

        Map<String, Object> response = new HashMap<>();
        if (saveStatus == null || saveStatus.isEmpty() || saveStatus.equals("error")) {
            response.put("status", false);
            response.put("message", "failed");
            response.put("description", "Failed to save the details");
            return ResponseEntity.ok(response);
        } else if (saveStatus.equals("duplicate")) {
            response.put("status", false);
            response.put("message", "failed");
            response.put("description", "Complaints already exist");
            return ResponseEntity.ok(response);
        }

        response.put("status", true);
        response.put("message", "success");
        response.put("description", "Details were saved successfully");
        return ResponseEntity.ok(response);
    }

    // get Pending user request for Approval
    @GetMapping("/getPendingApprovalList")
    public ResponseEntity<Map<String, Object>> getPendingUserRequest(@RequestParam String loginId,
                                                                     @RequestParam(required = false, name="fromDate") String fromDate,
                                                                     @RequestParam(required = false, name="toDate") String toDate){

        List<Map<String, Object>> result = potholeService.getPendingUserRequest(loginId, fromDate, toDate);

        Map<String, Object> response = new HashMap<>();
        if (result.isEmpty()) {
            response.put("status", false);
            response.put("message", "No data found");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", true);
            response.put("message", "Data found");
            response.put("data", result);
        }

        return ResponseEntity.ok(response);
    }

    // save AE approval of pending user request
    @PostMapping("/approveUserRequest")
    public ResponseEntity<Map<String, Object>> approveUserRequest(@RequestParam String loginId, @RequestParam String complaintNo,
                                                                  @RequestParam(required = false, name="remarks") String remarks,
                                                                  @RequestParam String status, @RequestParam String locationStatus){

        Map<String,Object> result = potholeService.approveUserRequest(loginId, complaintNo, remarks, status, locationStatus);

        Map<String, Object> response = new HashMap<>();
        if (result.isEmpty()) {
            response.put("status", false);
            response.put("message", "No data found");
            response.put("data", Collections.emptyList());
        } else {
            response.put("status", true);
            response.put("message", "Data found");
            response.put("data", result);
        }

        return ResponseEntity.ok(response);
    }
}
