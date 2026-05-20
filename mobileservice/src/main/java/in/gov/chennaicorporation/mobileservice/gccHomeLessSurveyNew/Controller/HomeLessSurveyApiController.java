package in.gov.chennaicorporation.mobileservice.gccHomeLessSurveyNew.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

}
