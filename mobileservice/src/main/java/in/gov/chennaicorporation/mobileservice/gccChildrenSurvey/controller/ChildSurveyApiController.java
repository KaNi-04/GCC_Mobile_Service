package in.gov.chennaicorporation.mobileservice.gccChildrenSurvey.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.gov.chennaicorporation.mobileservice.gccChildrenSurvey.service.ChildSurveyService;

@RequestMapping("/gccofficialapp/api/childsurvey")
@RestController("gccofficialappchildsurvey")
public class ChildSurveyApiController {

    @Autowired
    private ChildSurveyService childSurveyService;

    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions() {

        List<Map<String, Object>> data = childSurveyService.getSurveyQuestions();

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/questions_1")
    public ResponseEntity<?> getQuestions1() {

        List<Map<String, Object>> data = childSurveyService.getParticipateSurveyQuestions();

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    // @PostMapping("/save")
    // public ResponseEntity<?> saveSurvey(@RequestBody Map<String, Object> request)
    // {

    // List<Map<String, Object>> responses = (List<Map<String, Object>>)
    // request.get("responses");

    // String result = childSurveyService.saveSurvey(responses);

    // Map<String, Object> response = new HashMap<>();
    // response.put("status", true);
    // response.put("message", result);

    // return ResponseEntity.ok(response);
    // }

    @PostMapping(value = "/save", consumes = { MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> saveSurvey(@RequestParam Map<String, String> params) {

        String result = childSurveyService.saveSurveyFromParams(params);

        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", result));
    }

    @PostMapping(value = "/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveSurveyJson(@RequestBody Map<String, Object> jsonParams) {
        Map<String, String> params = new HashMap<>();
        if (jsonParams != null) {
            for (Map.Entry<String, Object> entry : jsonParams.entrySet()) {
                params.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString());
            }
        }
        String result = childSurveyService.saveSurveyFromParams(params);

        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", result));
    }

    @PostMapping(value = "/save_1", consumes = { MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> saveParticipateSurvey(@RequestParam Map<String, String> params) {

        String result = childSurveyService.saveParticipateSurveyFromParams(params);

        return ResponseEntity.ok(Map.of(
                "status", true,
                "message", result));
    }

    @GetMapping("/login")
    public ResponseEntity<?> getLogin(@RequestParam String mobileNo, String password) {

        Map<String, Object> response = childSurveyService.getLoginDetails(mobileNo, password);

        return ResponseEntity.ok(response);
    }

}
