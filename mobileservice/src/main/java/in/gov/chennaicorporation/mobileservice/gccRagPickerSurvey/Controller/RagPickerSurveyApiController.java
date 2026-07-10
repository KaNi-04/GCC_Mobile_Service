package in.gov.chennaicorporation.mobileservice.gccRagPickerSurvey.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import jakarta.servlet.http.HttpServletRequest;

import in.gov.chennaicorporation.mobileservice.gccRagPickerSurvey.Service.RagPickerSurveyService;

@RequestMapping("/gccofficialapp/api/ragpickersurvey")
@RestController("gccofficialappragpickersurvey")
public class RagPickerSurveyApiController {

    @Autowired
    private RagPickerSurveyService ragPickerSurveyService;

    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions(@RequestParam Integer cid) {

        List<Map<String, Object>> data = ragPickerSurveyService.getSurveyQuestions(cid);

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/login")
    public ResponseEntity<?> getLogin(@RequestParam String mobileNo, @RequestParam String password) {

        Map<String, Object> response = ragPickerSurveyService.getLoginDetails(mobileNo, password);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/states")
    public ResponseEntity<?> getStates() {

        List<Map<String, Object>> data = ragPickerSurveyService.getStates();

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts(@RequestParam(required = false) String sid) {

        List<Map<String, Object>> data = ragPickerSurveyService.getDistricts(sid);

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/questions_category")
    public ResponseEntity<?> getQuestionsCategory() {
        List<Map<String, Object>> data = ragPickerSurveyService.getQuestionsCategory();
        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    // @PostMapping("/save")
    // public ResponseEntity<?> saveResponses(
    // @RequestParam Map<String, String> params,
    // HttpServletRequest request) {
    // MultipartHttpServletRequest multipartRequest = null;
    // if (request instanceof MultipartHttpServletRequest) {
    // multipartRequest = (MultipartHttpServletRequest) request;
    // }

    // Map<String, Object> result =
    // ragPickerSurveyService.saveSurveyResponses(params, multipartRequest);
    // System.out.println("params ----------------------------------------->" +
    // params);
    // System.out.println("request-------------" + request);
    // System.out.println("multipartRequest --------------------------------->" +
    // multipartRequest);
    // System.out.println("result ----------------------------------------->" +
    // result);
    // return ResponseEntity.ok(result);
    // }

    @PostMapping("/save")
    public ResponseEntity<?> saveResponses(
            @RequestParam Map<String, String> params,
            HttpServletRequest request,
            MultipartHttpServletRequest multipartRequest) {

        Map<String, MultipartFile> files = multipartRequest.getFileMap();

        // System.out.println("===== FORM PARAMETERS =====");

        request.getParameterMap().forEach((key, values) -> {

            for (String value : values) {
                // System.out.println(key + " = " + value);
            }
        });

        // System.out.println("\n===== FILES ====");

        // multipartRequest.getFileMap().forEach((key, file) -> {

        // System.out.println("Field Name : " + key);

        // System.out.println("File Name : " + file.getOriginalFilename());

        // System.out.println("Size : " + file.getSize());

        // System.out.println("ContentType: " + file.getContentType());

        // System.out.println("---------------------------");

        // });

        // System.out.println("\n===== FILES FINAL ====");
        for (Map.Entry<String, MultipartFile> entry : files.entrySet()) {

            String fieldName = entry.getKey();
            MultipartFile file = entry.getValue();
            // System.out.println("params :" + params);
            // System.out.println("request :" + request);
            // System.out.println("multipartRequest :" + multipartRequest);
            // System.out.println("Field Name : " + fieldName);
            // System.out.println("File Name : " + file.getOriginalFilename());
            // System.out.println("Size : " + file.getSize());
        }

        // Existing service call
        Map<String, Object> result = ragPickerSurveyService.saveSurveyResponses(params, multipartRequest);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/category_status")
    public ResponseEntity<?> getCategoryStatus(@RequestParam("survey_id") String surveyId) {
        Map<String, Object> response = ragPickerSurveyService.getCategoryStatus(surveyId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile_list")
    public ResponseEntity<?> getProfileList(@RequestParam(required = false) String cby) {
        List<Map<String, Object>> data = ragPickerSurveyService.getProfileCreatedList(cby);
        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
    
//    @PostMapping(value = "/save", consumes = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<?> saveSurveyJson(@RequestBody Map<String, Object> jsonParams) {
//        Map<String, String> params = new HashMap<>();
//        if (jsonParams != null) {
//            for (Map.Entry<String, Object> entry : jsonParams.entrySet()) {
//                params.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString());
//            }
//        }
//        String result = ragPickerSurveyService.saveSurveyFromParams(params);
//
//        return ResponseEntity.ok(Map.of(
//                "status", true,
//                "message", result));
//    }
    
    @PostMapping(value= "/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveResponsesJson(
            @RequestBody Map<String, Object> jsonparams,
            HttpServletRequest request,
            MultipartHttpServletRequest multipartRequest) {
    	

    	 Map<String, String> params = new HashMap<>();
       if (jsonparams != null) {
           for (Map.Entry<String, Object> entry : jsonparams.entrySet()) {
               params.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString());
           }
       }
        Map<String, MultipartFile> files = multipartRequest.getFileMap();

        // System.out.println("===== FORM PARAMETERS =====");

        request.getParameterMap().forEach((key, values) -> {

            for (String value : values) {
                // System.out.println(key + " = " + value);
            }
        });

        // System.out.println("\n===== FILES ====");

        // multipartRequest.getFileMap().forEach((key, file) -> {

        // System.out.println("Field Name : " + key);

        // System.out.println("File Name : " + file.getOriginalFilename());

        // System.out.println("Size : " + file.getSize());

        // System.out.println("ContentType: " + file.getContentType());

        // System.out.println("---------------------------");

        // });

        // System.out.println("\n===== FILES FINAL ====");
        for (Map.Entry<String, MultipartFile> entry : files.entrySet()) {

            String fieldName = entry.getKey();
            MultipartFile file = entry.getValue();
            // System.out.println("params :" + params);
            // System.out.println("request :" + request);
            // System.out.println("multipartRequest :" + multipartRequest);
            // System.out.println("Field Name : " + fieldName);
            // System.out.println("File Name : " + file.getOriginalFilename());
            // System.out.println("Size : " + file.getSize());
        }

        // Existing service call
        Map<String, Object> result = ragPickerSurveyService.saveSurveyResponses(params, multipartRequest);

        return ResponseEntity.ok(result);
    }

}
