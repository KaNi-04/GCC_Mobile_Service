package in.gov.chennaicorporation.mobileservice.nulm.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
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

import in.gov.chennaicorporation.mobileservice.nulm.service.ParksApiService;

@RequestMapping("/gccofficialapp/api/nulm/parks")
@RestController("gccofficialappNULMParksRest")
public class ParksApiController {

    @Autowired
    private ParksApiService parksApiService;

    @GetMapping(value = "/getParkDetails")
    public List<?> getParkDetails(@RequestParam(value = "division", required = false) String division) {
        return parksApiService.getParkDetails(division);
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
            @RequestParam(required = false) String parkid,
            @RequestParam(required = false) MultipartFile photoUrl,
            @RequestParam(required = false) String latitude,
            @RequestParam(required = false) String longitude,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String verifiedStatus) {

        Map<String, Object> response = new HashMap<>();

        try {

            // ✅ userid validation
            if (userid == null || userid.isBlank()) {
                response.put("status", "Failed");
                response.put("message", "userid is mandatory");
                return ResponseEntity.badRequest().body(response); // 400
            }

            // ✅ photo validation
            if (photoUrl == null || photoUrl.isEmpty()) {
                response.put("status", "Failed");
                response.put("message", "Please upload image");
                return ResponseEntity.badRequest().body(response); // 400
            }

            String photoUrlPath = parksApiService.fileUpload(photoUrl, "photoUrl");

            Map<String, Object> serviceResponse = parksApiService.saveStaffVerificationDetails(
                    userid,
                    enrollmentId,
                    parkid,
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

    // @PostMapping("/saveStaffVerificationDetails")
    // public Map<String, Object> saveStaffVerificationDetails(
    // @RequestParam String userid,
    // @RequestParam String enrollmentId,
    // @RequestParam MultipartFile photoUrl,
    // @RequestParam String latitude,
    // @RequestParam String longitude,
    // @RequestParam String address,
    // @RequestParam(required = false) String verifiedStatus) {

    // if (userid == null || userid.isBlank() ||
    // enrollmentId == null || enrollmentId.isBlank()) {

    // Map<String, Object> response = new HashMap<>();
    // response.put("status", "Failed");
    // response.put("message", "userid and enrollmentId are mandatory");
    // return response;
    // }

    // String photoUrlPath = parksApiService.fileUpload(photoUrl, "photoUrl");

    // return parksApiService.saveStaffVerificationDetails(
    // userid,
    // enrollmentId,
    // photoUrlPath,
    // latitude,
    // longitude,
    // address,
    // verifiedStatus);
    // }

    // @GetMapping(value = "/getStaffListCount_Loc_Park")
    // public List<?> getStaffListForAttendance_Loc_Park(@RequestParam(value =
    // "parkid", required = false) String parkid) {
    // return parksApiService.getStaffListForAttendance_Loc_Park(parkid);
    // }

}
