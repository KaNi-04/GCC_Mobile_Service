package in.gov.chennaicorporation.mobileservice.homelessSurvey.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.gov.chennaicorporation.mobileservice.homelessSurvey.service.HomelessService;


@RequestMapping("/gccofficialapp/api/homeless/")
@RestController("gccofficialappshomeless")
public class HomelessController {
	@Autowired
	 private HomelessService homelessService;
	
	@GetMapping({"/versionCheck"}) // Template
	public List <?> versionCheck() {
		return homelessService.versionCheck();
	}
 
 	@GetMapping(value="login")
	public List login(@RequestParam("username") String username, @RequestParam("password") String Password) {
		return homelessService.getloginList(username, Password);
	}
 	
 	@GetMapping("/fields/{formId}")
    public List<?> getFormFields(@PathVariable int formId) {
        return homelessService.getFormFields(formId);
    }

    @GetMapping("/options/{fieldId}")
    public List<?> getOptions(@PathVariable int fieldId) {
        return homelessService.getFieldOptions(fieldId);
    }

    @PostMapping("/submit")
    public List<?> submitForm(@RequestBody Map<String, Object> request) {

        int formId = (int) request.get("formId");
        String submittedBy = (String) request.get("submittedBy");
        String latitude = (String) request.get("latitude");
        String longitude = (String) request.get("longitude");

        Map<Integer, String> fieldValues =
                (Map<Integer, String>) request.get("fieldValues");

        return homelessService.submitForm(
                formId, submittedBy, latitude, longitude, fieldValues);
    }

    @GetMapping("/submission/{id}")
    public List<?> getSubmission(@PathVariable int id) {
        return homelessService.getSubmission(id);
    }
}
