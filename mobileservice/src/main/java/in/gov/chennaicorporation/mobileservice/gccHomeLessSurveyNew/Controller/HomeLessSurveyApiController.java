package in.gov.chennaicorporation.mobileservice.gccHomeLessSurveyNew.Controller;

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
import org.springframework.web.multipart.MultipartHttpServletRequest;

import jakarta.servlet.http.HttpServletRequest;

import in.gov.chennaicorporation.mobileservice.gccHomeLessSurveyNew.Service.HomeLessSurveyService;

@RequestMapping("/gccofficialapp/api/homelesssurvey")
@RestController("gccofficialapphomelesssurvey")
public class HomeLessSurveyApiController {

    @Autowired
    private HomeLessSurveyService homeLessSurveyService;

    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions(@RequestParam Integer cid) {

        List<Map<String, Object>> data = homeLessSurveyService.getSurveyQuestions(cid);

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/login")
    public ResponseEntity<?> getLogin(@RequestParam String mobileNo, @RequestParam String password) {

        Map<String, Object> response = homeLessSurveyService.getLoginDetails(mobileNo, password);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/states")
    public ResponseEntity<?> getStates() {

        List<Map<String, Object>> data = homeLessSurveyService.getStates();

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts(@RequestParam(required = false) String sid) {

        List<Map<String, Object>> data = homeLessSurveyService.getDistricts(sid);

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/questions_category")
    public ResponseEntity<?> getQuestionsCategory() {
        List<Map<String, Object>> data = homeLessSurveyService.getQuestionsCategory();
        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveResponses(
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {
        MultipartHttpServletRequest multipartRequest = null;
        if (request instanceof MultipartHttpServletRequest) {
            multipartRequest = (MultipartHttpServletRequest) request;
        }

        Map<String, Object> result = homeLessSurveyService.saveSurveyResponses(params, multipartRequest);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/category_status")
    public ResponseEntity<?> getCategoryStatus(@RequestParam("survey_id") String surveyId) {
        Map<String, Object> response = homeLessSurveyService.getCategoryStatus(surveyId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile_list")
    public ResponseEntity<?> getProfileList(@RequestParam(required = false) String cby) {
        List<Map<String, Object>> data = homeLessSurveyService.getProfileCreatedList(cby);
        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

}
