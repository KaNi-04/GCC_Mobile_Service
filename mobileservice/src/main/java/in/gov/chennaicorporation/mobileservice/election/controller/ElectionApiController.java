package in.gov.chennaicorporation.mobileservice.election.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import in.gov.chennaicorporation.mobileservice.election.service.ElectionService;

@RequestMapping("/gccofficialapp/api/election")
@RestController("gccofficialappelection")
public class ElectionApiController {

    @Autowired
    ElectionService electionService;

    @GetMapping("/getPollPersonDetailsByMobile")
    public ResponseEntity<Map<String, Object>> getPollPersonDetails(
            @RequestParam(required = false) String slno) {

        Map<String, Object> response = new HashMap<>();

        // Validation
        if (slno == null || slno.trim().isEmpty()) {
            response.put("status", HttpStatus.BAD_REQUEST.value());
            response.put("message", "slno number is required");
            response.put("data", null);
            return ResponseEntity.ok(response);
        }

        // if (!slno.matches("^[1-9][0-9]{9}$")) {
        // response.put("status", HttpStatus.BAD_REQUEST.value());
        // response.put("message", "Mobile number must be 10 digits and should not start
        // with 0");
        // response.put("data", null);
        // return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        // }

        // 🔹 Fetch data
        List<Map<String, Object>> applicants = electionService.getApplicantsByMobile(slno);

        if (applicants == null || applicants.isEmpty()) {
            response.put("status", HttpStatus.NOT_FOUND.value());
            response.put("message", "No records found for given mobile number");
            return ResponseEntity.ok(response);
        }

        // 🔹 Success response
        response.put("status", HttpStatus.OK.value());
        response.put("message", "Poll person details fetched successfully");
        response.put("data", applicants);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getLoginSuccess")
    public ResponseEntity<Map<String, Object>> getLoginSuccess(
            @RequestParam String mobileNo) {

        Map<String, Object> response = new HashMap<>();

        try {

            // Validation FIRST
            if (mobileNo == null || mobileNo.trim().isEmpty()) {
                response.put("status", false);
                response.put("message", "Mobile number is required");
                response.put("data", null);
                return ResponseEntity.ok(response);
            }

            if (!mobileNo.matches("^[1-9][0-9]{9}$")) {
                response.put("status", false);
                response.put("message", "Mobile number must be 10 digits and should not start with 0");
                response.put("data", null);
                return ResponseEntity.ok(response);
            }

            List<Map<String, Object>> result = electionService.getBatchDetails(mobileNo);

            // No records
            if (result.isEmpty()) {
                response.put("status", false);
                response.put("message", "No records found");
                response.put("data", new ArrayList<>());
                return ResponseEntity.ok(response);
            }

            // Multiple records
            if (result.size() > 1) {
                response.put("status", false);
                response.put("message",
                        "You have multiple records. Please contact IT Cell.");
                response.put("data", null);
                return ResponseEntity.ok(response);
            }

            // 🔹 Single record success
            response.put("status", true);
            response.put("message", "Batch details fetched successfully");
            response.put("data", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();

            response.put("status", false);
            response.put("message", "Something went wrong");
            response.put("data", null);

            return ResponseEntity.ok(response);
        }
    }

}
