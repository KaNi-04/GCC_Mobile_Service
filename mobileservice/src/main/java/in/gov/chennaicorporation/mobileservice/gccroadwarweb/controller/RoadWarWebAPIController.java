package in.gov.chennaicorporation.mobileservice.gccroadwarweb.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccroadwarweb.service.RoadWarWebService;

@RestController("gccofficialapproadwarweb")
@RequestMapping("/gccofficialapp/api/roadwarweb")
public class RoadWarWebAPIController {

    @Autowired
    RoadWarWebService roadwarwebservice;

    // @GetMapping("/getRoadMasterDetails")
    // public Map<String, Object> getRoadMasterDetails(@RequestParam String loginId)
    // {
    // Map<String, Object> response = new HashMap<>();

    // List<Map<String, Object>> roadDetails =
    // roadwarwebservice.getRoadMasterDetails(loginId);

    // if (roadDetails != null) {
    // response.put("status", "200");
    // response.put("message", "Success");
    // response.put("data", roadDetails);
    // } else {
    // response.put("status", "200");
    // response.put("message", "No Data Found");
    // response.put("data", null);
    // }
    // return response;
    // }

    @GetMapping("/getRoadMasterDetails")
    public Map<String, Object> getRoadMasterDetails(@RequestParam String road_id) {
        Map<String, Object> response = new HashMap<>();

        Map<String, Object> roadDetails = roadwarwebservice.getRoadMasterDetails(road_id);

        if (roadDetails != null) {
            response.put("status", "200");
            response.put("message", "Success");
            response.put("data", roadDetails);
        } else {
            response.put("status", "200");
            response.put("message", "No Data Found");
            response.put("data", "");
        }
        return response;
    }

    @PostMapping("/saveRoadMasterFiles")
    public Map<String, Object> saveRoadMasterFiles(
            @RequestParam("ref_id") String ref_id,
            @RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2,
            @RequestParam("file3") MultipartFile file3,
            @RequestParam(value = "file4", required = false) MultipartFile file4,
            @RequestParam("loginId") String loginId) {
        Map<String, Object> response = new HashMap<>();

        String result = roadwarwebservice.saveRoadMasterFiles(ref_id, file1, file2, file3, file4, loginId);

        if ("already_exists".equalsIgnoreCase(result)) {
            Map<String, Object> existingData = roadwarwebservice.getRoadMasterFileDetails(ref_id);
            response.put("status", "200");
            response.put("message", "Files already uploaded for this road.");
            response.put("data", existingData);
        } else if ("Success".equalsIgnoreCase(result)) {
            response.put("status", "200");
            response.put("message", "Success");
            response.put("data", result);
        } else {
            response.put("status", "200");
            response.put("message", result != null ? result : "Failed to save files");
            response.put("data", null);
        }
        return response;
    }

}
