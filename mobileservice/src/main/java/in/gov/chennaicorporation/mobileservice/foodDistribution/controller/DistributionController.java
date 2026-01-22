package in.gov.chennaicorporation.mobileservice.foodDistribution.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.foodDistribution.service.DistributionService;

@RequestMapping("/gccofficialapp/api/fooddistribution/")
@RestController("gccofficialappsfooddistribution")
public class DistributionController {

	@Autowired
	private DistributionService distributionService;
	

    // -------------------- GET CONFIG --------------------
    @GetMapping("/getConfig")
    public List<?> getConfig(@RequestParam("loginId") String loginId) {
        return distributionService.getConfig(loginId);
    }


    // -------------------- GET SHIFT COUNT --------------------
    @GetMapping("/getShiftCount")
    public List<?> getShiftCount(
            @RequestParam("loginId") String loginId,
            @RequestParam("shiftid") String shiftid
    ) {
        return distributionService.getShiftCount(loginId, shiftid);
    }


    // -------------------- SAVE DAILY REQUEST --------------------
    @PostMapping("/saveRequest")
    public List<?> saveRequest(
            @RequestParam String ward,
            @RequestParam String required_date,
            @RequestParam String permanent,
            @RequestParam String nulm,
            @RequestParam String private_,
            @RequestParam String others,
            @RequestParam(value = "nmr", required = false) String nmr,
            @RequestParam String weeklyoff_permanent,
            @RequestParam String weeklyoff_nulm,
            @RequestParam String weeklyoff_private,
            @RequestParam String weeklyoff_others,
            @RequestParam(value = "weeklyoff_nmr", required = false) String weeklyoff_nmr,
            @RequestParam String absentees_permanent,
            @RequestParam String absentees_nulm,
            @RequestParam String absentees_private,
            @RequestParam String absentees_others,
            @RequestParam(value = "absentees_nmr", required = false) String absentees_nmr,
            @RequestParam String shiftid,
            @RequestParam String request_by
    ) {
        return distributionService.saveRequest(
                ward,
                required_date,
                permanent,
                nulm,
                private_,
                others,
                nmr,
                weeklyoff_permanent,
                weeklyoff_nulm,
                weeklyoff_private,
                weeklyoff_others,
                weeklyoff_nmr,
                absentees_permanent,
                absentees_nulm,
                absentees_private,
                absentees_others,
                absentees_nmr,
                shiftid,
                request_by
        );
    }


    // -------------------- GET PENDING LIST --------------------
    @GetMapping("/getPending")
    public List<?> getPending(@RequestParam("loginId") String loginId) {
        return distributionService.getPending(loginId);
    }


    // -------------------- GET PENDING DETAILS --------------------
    @GetMapping("/getPendingDetails")
    public List<?> getPendingDetails(
            @RequestParam("loginId") String loginId,
            @RequestParam("requestid") String requestid,
            @RequestParam("shiftid") String shiftid
    ) {
        return distributionService.getPendingDetails(loginId, requestid, shiftid);
    }
    
    // -------------------- SAVE FEEDBACK AND RECEVIED INFORMATION --------------------
    @PostMapping("/saveFeedbackAndReceived")
    public List<?> saveFeedbackAndReceived(
            @RequestParam String requestid,
            @RequestParam String shiftid,

            // Feedback
            @RequestParam String q1,
            @RequestParam(value = "q1_img", required = false) MultipartFile q1_img,
            @RequestParam String q2,
            @RequestParam(value = "q2_img", required = false) MultipartFile q2_img,
            @RequestParam String q3,
            @RequestParam(value = "q3_img", required = false) MultipartFile q3_img,
            @RequestParam String feedback_by,

            // Received Update
            @RequestParam String total_box_shortfall,
            @RequestParam String return_today,
            @RequestParam String total_food_requested,
            @RequestParam String total_food_received,
            @RequestParam String today_absentees_permanent,
            @RequestParam String today_absentees_nulm,
            @RequestParam String today_absentees_private,
            @RequestParam String today_absentees_others,
            @RequestParam String received_by,
            @RequestParam(value = "today_absentees_nmr", required = false) String today_absentees_nmr,
            @RequestParam(value = "remarks", required = false) String remarks
    ) {
        return distributionService.saveFullFeedbackAndReceived(
                requestid, shiftid,
                q1, q1_img, q2, q2_img, q3, q3_img, feedback_by,
                total_box_shortfall, return_today, total_food_requested, total_food_received,
                today_absentees_permanent, today_absentees_nulm,
                today_absentees_private, today_absentees_others,
                received_by,today_absentees_nmr,remarks
                
        );
    }
    
    // For Reports
    @GetMapping("/getZoneSummary")
    public List<?> getZoneSummary(
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate
    ) {
        return distributionService.getZoneSummary(fromDate, toDate);
    }
    
    @GetMapping("/getWardSummary")
    public List<?> getWardSummary(
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate,
            @RequestParam("zone") String zone
    ) {
        return distributionService.getWardSummary(fromDate, toDate, zone);
    }
}
