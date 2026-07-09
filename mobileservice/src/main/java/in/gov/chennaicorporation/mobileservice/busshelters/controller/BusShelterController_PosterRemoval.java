package in.gov.chennaicorporation.mobileservice.busshelters.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.busshelters.service.BusShelterPosterRemovalActivity;

@RequestMapping("/gccofficialapp/api/busshelter/posterremoval/")
@RestController("busshelterPosterRemovalRest")
public class BusShelterController_PosterRemoval {
    @Autowired
    private JdbcTemplate jdbcBusShelterTemplate;
    @Autowired
    private BusShelterPosterRemovalActivity BusShelterPosterRemovalActivity;

    @GetMapping(value = "/getBusShelterList")
    public List<?> getBusShelterList(@RequestParam MultiValueMap<String, String> formData,
            @RequestParam(value = "latitude", required = true) String latitude,
            @RequestParam(value = "longitude", required = true) String longitude,
            @RequestParam(value = "loginId", required = false) String loginId) {
        return BusShelterPosterRemovalActivity.getBusShelterList(formData, latitude, longitude, loginId);
    }

    @GetMapping(value = "/getParentQuestionsList")
    public List<?> getParentQuestionsList(
            @RequestParam(value = "loginId", required = false) String cby,
            @RequestParam(value = "qtype", required = true) String qtype) {
        return BusShelterPosterRemovalActivity.getParentQuestionsList(qtype);
    }

    @GetMapping(value = "/getChildQuestionsList")
    public List<?> getChildQuestionsList(
            @RequestParam(value = "loginId", required = false) String cby,
            @RequestParam(value = "pid", required = true) String pid) {
        return BusShelterPosterRemovalActivity.getChildQuestionsList(pid);
    }

    @PostMapping(value = "/saveFeedback")
    public List<?> saveFeedback(
            @RequestParam(value = "shelter_id", required = true) String shelter_id,
            @RequestParam(value = "loginId", required = true) String cby,
            @RequestParam(value = "latitude", required = true) String latitude,
            @RequestParam(value = "longitude", required = true) String longitude,
            @RequestParam(value = "action", required = true) String type,
            @RequestParam(value = "zone", required = true) String zone,
            @RequestParam(value = "ward", required = true) String ward,
            @RequestParam(value = "q_1", required = false) String q_1,
            @RequestParam(value = "q_2", required = false) String q_2,
            @RequestParam(value = "q_3", required = false) String q_3,
            @RequestParam(value = "q_4", required = false) String q_4,
            @RequestParam(value = "q_5", required = false) String q_5,
            @RequestParam(value = "q_6", required = false) String q_6,
            @RequestParam(value = "q_7", required = false) String q_7,
            @RequestParam(value = "q_8", required = false) String q_8,
            @RequestParam(value = "q_9", required = false) String q_9,
            @RequestParam(value = "q_10", required = false) String q_10,
            @RequestParam(value = "q_11", required = false) String q_11,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "file", required = false) MultipartFile filedata,

            @RequestParam(value = "file_1", required = false) MultipartFile file_1,
            @RequestParam(value = "file_2", required = false) MultipartFile file_2,
            @RequestParam(value = "file_3", required = false) MultipartFile file_3,
            @RequestParam(value = "file_4", required = false) MultipartFile file_4,
            @RequestParam(value = "file_5", required = false) MultipartFile file_5,
            @RequestParam(value = "file_6", required = false) MultipartFile file_6,
            @RequestParam(value = "file_7", required = false) MultipartFile file_7,
            @RequestParam(value = "file_8", required = false) MultipartFile file_8,
            @RequestParam(value = "file_9", required = false) MultipartFile file_9,
            @RequestParam(value = "file_10", required = false) MultipartFile file_10,
            @RequestParam(value = "file_11", required = false) MultipartFile file_11) {
        return BusShelterPosterRemovalActivity.saveFeedback(shelter_id, cby, latitude, longitude, zone, ward,
                q_1, q_2, q_3, q_4, q_5, q_6, q_7, q_8, q_9, q_10, q_11, remarks, filedata,
                file_1, file_2, file_3, file_4, file_5, file_6, file_7, file_8, file_9, file_10, file_11, type);
    }

    @GetMapping(value = "/getZoneReport")
    public List<?> getZoneReport(
            @RequestParam(value = "loginId", required = true) String cby) {
        return BusShelterPosterRemovalActivity.getZoneReport(cby);
    }

    @GetMapping(value = "/getWardReport")
    public List<?> getWardReport(
            @RequestParam(value = "loginId", required = true) String cby,
            @RequestParam(value = "zone", required = true) String zone) {
        return BusShelterPosterRemovalActivity.getWardReport(cby, zone);
    }

    @GetMapping(value = "/getCompletedReport")
    public List<?> getCompletedReport(
            @RequestParam(value = "loginId", required = true) String cby,
            @RequestParam(value = "ward", required = true) String ward) {
        return BusShelterPosterRemovalActivity.getCompletedReport(cby, ward);
    }

    @GetMapping(value = "/getPendingReport")
    public List<?> getPendingReport(
            @RequestParam(value = "loginId", required = true) String cby,
            @RequestParam(value = "ward", required = true) String ward) {
        return BusShelterPosterRemovalActivity.getPendingReport(cby, ward);
    }

}
