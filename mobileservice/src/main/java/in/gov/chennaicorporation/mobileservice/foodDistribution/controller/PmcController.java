package in.gov.chennaicorporation.mobileservice.foodDistribution.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.foodDistribution.service.PmcService;

@RequestMapping("/gccofficialapp/api/fooddistribution/pmc")
@RestController("gccofficialappsfooddistributionpmc")
public class PmcController {

    @Autowired
    private PmcService pmcservice;

    @GetMapping("/getConfig")
    public List<?> getConfig(@RequestParam("loginId") String loginId) {
        return pmcservice.getConfig(loginId);
    }

    @GetMapping("/getFinalFoodCount")
    public List<Map<String, Object>> getZoneCount(
            @RequestParam int shiftid,
            @RequestParam int loginid,
            @RequestParam String date) {

        return pmcservice.getFinalFoodCount(shiftid, loginid, date);
    }

    @GetMapping("/getfeedbackquestions")
    public List<Map<String, Object>> getFeedbackQuestions(
            @RequestParam(value = "loginId", required = false) String loginId) {
        return pmcservice.getParentQuestionsList(loginId);
    }

    @PostMapping("/savefeedback")
    public List<?> savefeedback(
            @RequestParam(value = "loginId", required = true) String loginId,
            @RequestParam(value = "auditdate", required = true) String auditdate,
            @RequestParam(value = "shiftid", required = true) String shiftid,
            @RequestParam(value = "latitude", required = true) String latitude,
            @RequestParam(value = "longitude", required = true) String longitude,
            @RequestParam(value = "zone", required = true) String zone,
            @RequestParam(value = "ward", required = true) String ward,
            @RequestParam(value = "address", required = true) String address,
            @RequestParam(value = "final_food_count", required = true) String final_food_count,
            @RequestParam(value = "foodid", required = true) String foodid,
            @RequestParam(value = "food_others", required = false) String food_others,
            @RequestParam(value = "hub_id", required = true) String hub_id,

            @RequestParam(value = "q1", required = true) String q1,
            @RequestParam(value = "q2", required = true) String q2,
            @RequestParam(value = "q3", required = true) String q3,
            @RequestParam(value = "q4", required = true) String q4,
            @RequestParam(value = "q5", required = true) String q5,
            @RequestParam(value = "q6", required = true) String q6,
            @RequestParam(value = "q7", required = true) String q7,
            @RequestParam(value = "q8", required = true) String q8,
            @RequestParam(value = "q9", required = true) String q9,
            @RequestParam(value = "q10", required = true) String q10,
            @RequestParam(value = "q11", required = true) String q11,
            @RequestParam(value = "q12", required = true) String q12,
            @RequestParam(value = "q13", required = true) String q13,
            @RequestParam(value = "q14", required = true) String q14,
            @RequestParam(value = "q15", required = true) String q15,
            @RequestParam(value = "q16", required = true) String q16,
            @RequestParam(value = "q17", required = true) String q17,
            @RequestParam(value = "q18", required = true) String q18,

            @RequestParam(value = "image1", required = true) MultipartFile image1,
            @RequestParam(value = "image2", required = true) MultipartFile image2,
            @RequestParam(value = "image3", required = true) MultipartFile image3,
            @RequestParam(value = "image4", required = true) MultipartFile image4,
            @RequestParam(value = "image5", required = true) MultipartFile image5) {

        return pmcservice.saveFeedback(
                loginId, auditdate, shiftid, latitude, longitude, zone, ward, address,
                final_food_count, foodid, food_others, hub_id,
                Arrays.asList(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10,
                        q11, q12, q13, q14, q15, q16, q17, q18),
                image1, image2, image3, image4, image5);
    }

}
