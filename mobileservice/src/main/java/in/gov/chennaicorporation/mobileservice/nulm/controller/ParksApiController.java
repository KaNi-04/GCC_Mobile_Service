package in.gov.chennaicorporation.mobileservice.nulm.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.nulm.service.ParksApiService;

@RequestMapping("/gccofficialapp/api/nulm/parks")
@RestController("gccofficialappNULMParksRest")
public class ParksApiController {

    @Autowired
    private ParksApiService parksApiService;

    // @GetMapping(value = "/getParkDetails")
    // public List<?> getParkDetails(@RequestParam(value = "division", required =
    // false) String division) {
    // return parksApiService.getParkDetails(division);
    // }

    @GetMapping("/getParkDetails")
    public List<Map<String, Object>> getParkDetails(
            @RequestParam(required = false) String division,
            @RequestParam(required = false) String latitude,
            @RequestParam(required = false) String longitude) {

        return parksApiService.getParkDetails(division, latitude, longitude);
    }

    @GetMapping(value = "/getStaffListForAttendance")
    public List<?> getStaffListForAttendance(
            @RequestParam(value = "parkid", required = false) String parkid,
            @RequestParam(value = "date", required = false) String date) {

        Map<String, Object> response = new HashMap<>();

        if (parkid == null || parkid.isBlank() || date == null || date.isBlank()) {
            response.put("status", "Failed");
            response.put("message", "parkid and date are mandatory");
            response.put("Data", Collections.emptyList());
            return Collections.singletonList(response);
        }

        return parksApiService.getStaffListForAttendance(parkid, date);
    }

    @PostMapping("/saveStaffVerificationDetails")
    public ResponseEntity<Map<String, Object>> saveStaffVerificationDetails(

            @RequestParam(required = false) String userid,
            @RequestParam(required = false) String enrollmentId,
            @RequestParam String park_id,
            @RequestParam MultipartFile photoUrl,
            @RequestParam(required = false) String latitude,
            @RequestParam(required = false) String longitude,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String verifiedStatus) {

        Map<String, Object> response = new HashMap<>();

        try {

            // userid validation
            if (userid == null || userid.isBlank()) {
                response.put("status", "Failed");
                response.put("message", "userid is mandatory");
                return ResponseEntity.badRequest().body(response); // 400
            }

            // photo validation
            if (photoUrl == null || photoUrl.isEmpty()) {
                response.put("status", "Failed");
                response.put("message", "Please upload image");
                return ResponseEntity.badRequest().body(response); // 400
            }

            if (park_id == null || park_id.isEmpty()) {
                response.put("status", "Failed");
                response.put("message", "Park id empty");
                return ResponseEntity.badRequest().body(response); // 400
            }

            String photoUrlPath = parksApiService.fileUpload(photoUrl, "photoUrl");

            Map<String, Object> serviceResponse = parksApiService.saveStaffVerificationDetails(
                    userid,
                    enrollmentId,
                    park_id,
                    photoUrlPath,
                    latitude,
                    longitude,
                    address,
                    verifiedStatus);

            return ResponseEntity.ok(serviceResponse); // 200

        } catch (Exception ex) {

            response.put("status", "Failed");
            response.put("message", "Internal Server Error: " + ex.getMessage());

            return ResponseEntity.internalServerError().body(response); // 500
        }
    }

    @GetMapping(value = "/getParksInspectionQuestionsList")
    public List<?> getParksInspectionQuestionsList() {
        return parksApiService.getParksInspectionQuestionsList();
    }

    // @PostMapping(value = "/saveParksInspection", consumes =
    // MediaType.MULTIPART_FORM_DATA_VALUE)
    // public ResponseEntity<?> saveParksInspection(
    // @RequestParam Integer userid,
    // @RequestParam Integer park_id,
    // @RequestParam String responses,
    // @RequestParam String latitude,
    // @RequestParam String longitude,
    // @RequestParam String location,
    // @RequestParam String ai_verified_count,
    // @RequestParam String ai_not_verified_count,
    // @RequestParam(required = false) MultipartFile photoUrl) {

    // String photoUrlPath = "";

    // try {
    // if (photoUrl != null && !photoUrl.isEmpty()) {
    // photoUrlPath = parksApiService.fileUpload(photoUrl, "photoUrl");
    // }
    // } catch (Exception e) {
    // return ResponseEntity.ok(Map.of(
    // "status", "Error",
    // "message", "File upload failed: " + e.getMessage()));
    // }

    // return ResponseEntity.ok(
    // parksApiService.saveParksInspection(
    // userid,
    // park_id,
    // responses,
    // latitude,
    // longitude,
    // location,
    // ai_verified_count,
    // ai_not_verified_count,
    // photoUrlPath));
    // }

    @PostMapping(value = "/saveParksInspection", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveParksInspection(

            @RequestParam(required = false) Integer userid,
            @RequestParam(required = false) Integer park_id,

            @RequestParam String responses, // ✅ REQUIRED

            @RequestParam(required = false) String verificationData,
            @RequestParam(required = false) String latitude,
            @RequestParam(required = false) String longitude,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String ai_verified_count,
            @RequestParam(required = false) String ai_not_verified_count,

            @RequestParam MultipartFile photoUrl // ✅ REQUIRED
    ) {

        String photoUrlPath = "";

        try {

            // image mandatory check
            if (photoUrl == null || photoUrl.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "status", "Error",
                        "message", "Image is required"));
            }

            photoUrlPath = parksApiService.fileUpload(photoUrl, "photoUrl");

            Map<String, Object> result = parksApiService.saveParksInspection(
                    userid,
                    park_id,
                    responses,
                    verificationData,
                    latitude,
                    longitude,
                    location,
                    ai_verified_count,
                    ai_not_verified_count,
                    photoUrlPath);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status", "Error",
                    "message", "Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/zoneWardReport")
    public ResponseEntity<Map<String, Object>> getZoneWardReport(
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        return ResponseEntity.ok(
                parksApiService.getZoneWardReport(zone, ward, fromDate, toDate));
    }

    @PostMapping("/saveStaffDeviceDetails")
    public ResponseEntity<Map<String, Object>> saveStaffDeviceDetails(

            @RequestParam(required = false) String userid,
            @RequestParam(required = false) String supervisor_id,
            @RequestParam(required = false) String park_id,
            @RequestParam(required = false) String device_id,
            @RequestParam(required = false) String latitude,
            @RequestParam(required = false) String longitude,
            @RequestParam(required = false) String address) {

        Map<String, Object> response = new HashMap<>();

        try {

            // VALIDATION START

            if (park_id == null || park_id.trim().isEmpty()) {
                response.put("status", "Failed");
                response.put("message", "park_id is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (userid == null || userid.trim().isEmpty()) {
                response.put("status", "Failed");
                response.put("message", "userid is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (device_id == null || device_id.trim().isEmpty()) {
                response.put("status", "Failed");
                response.put("message", "device_id is required");
                return ResponseEntity.badRequest().body(response);
            }

            // CALL SERVICE
            response = parksApiService.saveStaffDeviceDetails(
                    userid,
                    supervisor_id,
                    park_id,
                    device_id,
                    latitude,
                    longitude,
                    address);

            return ResponseEntity.ok(response);

        } catch (Exception ex) {

            response.put("status", "Failed");
            response.put("message", "Internal Server Error: " + ex.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/checkSupervisorDevice")
    public ResponseEntity<Map<String, Object>> checkSupervisorDevice(
            @RequestParam(required = false) String park_id,
            @RequestParam(required = false) Integer supervisor_id,
            @RequestParam(required = false) String device_id) {

        Map<String, Object> response = new HashMap<>();

        try {

            // supervisor validation
            if (supervisor_id == null) {
                response.put("status", "Failed");
                response.put("message", "supervisor_id is required");
                return ResponseEntity.badRequest().body(response);
            }

            // device validation
            if (device_id == null || device_id.trim().isEmpty()) {
                response.put("status", "Failed");
                response.put("message", "device_id is required");
                return ResponseEntity.badRequest().body(response);
            }

            // optional park_id parsing
            Integer parsedParkId = null;
            if (park_id != null && !park_id.trim().isEmpty()) {
                try {
                    parsedParkId = Integer.parseInt(park_id);
                } catch (NumberFormatException e) {
                    response.put("status", "Failed");
                    response.put("message", "park_id must be a valid number");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            response = parksApiService.checkSupervisorDevice(parsedParkId, supervisor_id, device_id);

            return ResponseEntity.ok(response);

        } catch (Exception ex) {

            response.put("status", "Failed");
            response.put("message", "Internal Server Error");

            return ResponseEntity.internalServerError().body(response);
        }
    }
}
