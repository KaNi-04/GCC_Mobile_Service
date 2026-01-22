package in.gov.chennaicorporation.mobileservice.privateToilets.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.privateToilets.service.ToiletService;

@RequestMapping("/gccofficialapp/api/privatetoilets/")
@RestController("gccofficialappsprivatetoiltes")
public class APIController {
	@Autowired
    private ToiletService toiletService;
	
	@GetMapping(value="/getToiletsList")
	public List<?> getToiletsList(@RequestParam(value="loginId", required = true) String cby){
		return toiletService.getToiletsList(cby);
	}
	
	@GetMapping("/toiletlistRest")
    public List<?> toiletlistRest(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude) {
        return toiletService.toiletlistRest(formData, latitude, longitude);
    }
	
	@GetMapping("/toiletlistbylatlong")
    public List<?> toiletlistbylatlong(@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			//@RequestParam(value = "assetid", required = true) String id,
			@RequestParam(value = "loginId", required = false) String loginId) {
        return toiletService.toiletlistbylatlong(formData, latitude, longitude,loginId);
    }
	
	@GetMapping(value="/getParentQuestionsList")
	public List<?> getParentQuestionsList(@RequestParam(value="loginId", required = false) String cby){
		return toiletService.getParentQuestionsList();
	}
	
	@GetMapping(value="/getChildQuestionsList")
	public List<?> getChildQuestionsList(
			@RequestParam(value="loginId", required = false) String cby,
			@RequestParam(value="pid", required = true) String pid
			){
		return toiletService.getChildQuestionsList(pid);
	}
	
	@PostMapping(value="/saveFeedback")
	public List<?> saveFeedback(
	    @RequestParam(value="toilet_id", required = true) String toilet_id,
	    @RequestParam(value="loginId", required = true) String cby,
	    @RequestParam(value="latitude", required = true) String latitude,
	    @RequestParam(value="longitude", required = true) String longitude,
	    @RequestParam(value="zone", required = true) String zone,
	    @RequestParam(value="ward", required = true) String ward,
	    @RequestParam(value = "file", required = true) MultipartFile filedata,
	    @RequestParam(value="remarks", required = false) String remarks,

	    @RequestParam(value="q1", required = false) String q1,
	    @RequestParam(value="q2", required = false) String q2,
	    @RequestParam(value="q3", required = false) String q3,
	    @RequestParam(value="q4", required = false) String q4,
	    @RequestParam(value="q5", required = false) String q5,
	    @RequestParam(value="q6", required = false) String q6,
	    @RequestParam(value="q7", required = false) String q7,
	    @RequestParam(value="q8", required = false) String q8,
	    @RequestParam(value="q9", required = false) String q9,
	    @RequestParam(value="q10", required = false) String q10,
	    @RequestParam(value="q11", required = false) String q11,
	    @RequestParam(value="q12", required = false) String q12,
	    @RequestParam(value="q13", required = false) String q13,
	    @RequestParam(value="q14", required = false) String q14,
	    @RequestParam(value="q15", required = false) String q15,
	    @RequestParam(value="q16", required = false) String q16,
	    @RequestParam(value="q17", required = false) String q17,
	    @RequestParam(value="q18", required = false) String q18,
	    @RequestParam(value="q19", required = false) String q19,
	    @RequestParam(value="q20", required = false) String q20,
	    @RequestParam(value="q21", required = false) String q21,
	    @RequestParam(value="q22", required = false) String q22,
	    @RequestParam(value="q23", required = false) String q23,
	    @RequestParam(value="q24", required = false) String q24,
	    @RequestParam(value="q25", required = false) String q25,
	    @RequestParam(value="q26", required = false) String q26,
	    @RequestParam(value="q27", required = false) String q27,
	    @RequestParam(value="q28", required = false) String q28,
	    @RequestParam(value="q29", required = false) String q29,
	    @RequestParam(value="q30", required = false) String q30,
	    @RequestParam(value="q31", required = false) String q31,
	    @RequestParam(value="q32", required = false) String q32,
	    @RequestParam(value="q33", required = false) String q33,
	    @RequestParam(value="q34", required = false) String q34,
	    @RequestParam(value="q35", required = false) String q35,
	    

	    @RequestParam(value="q1_image", required = false) MultipartFile q1_image,
	    @RequestParam(value="q2_image", required = false) MultipartFile q2_image,
	    @RequestParam(value="q3_image", required = false) MultipartFile q3_image,
	    @RequestParam(value="q4_image", required = false) MultipartFile q4_image,
	    @RequestParam(value="q5_image", required = false) MultipartFile q5_image,
	    @RequestParam(value="q6_image", required = false) MultipartFile q6_image,
	    @RequestParam(value="q7_image", required = false) MultipartFile q7_image,
	    @RequestParam(value="q8_image", required = false) MultipartFile q8_image,
	    @RequestParam(value="q9_image", required = false) MultipartFile q9_image,
	    @RequestParam(value="q10_image", required = false) MultipartFile q10_image,
	    @RequestParam(value="q11_image", required = false) MultipartFile q11_image,
	    @RequestParam(value="q12_image", required = false) MultipartFile q12_image,
	    @RequestParam(value="q13_image", required = false) MultipartFile q13_image,
	    @RequestParam(value="q14_image", required = false) MultipartFile q14_image,
	    @RequestParam(value="q15_image", required = false) MultipartFile q15_image,
	    @RequestParam(value="q16_image", required = false) MultipartFile q16_image,
	    @RequestParam(value="q17_image", required = false) MultipartFile q17_image,
	    @RequestParam(value="q18_image", required = false) MultipartFile q18_image,
	    @RequestParam(value="q19_image", required = false) MultipartFile q19_image,
	    @RequestParam(value="q20_image", required = false) MultipartFile q20_image,
	    @RequestParam(value="q21_image", required = false) MultipartFile q21_image,
	    @RequestParam(value="q22_image", required = false) MultipartFile q22_image,
	    @RequestParam(value="q23_image", required = false) MultipartFile q23_image,
	    @RequestParam(value="q24_image", required = false) MultipartFile q24_image,
	    @RequestParam(value="q25_image", required = false) MultipartFile q25_image,
	    @RequestParam(value="q26_image", required = false) MultipartFile q26_image,
	    @RequestParam(value="q27_image", required = false) MultipartFile q27_image,
	    @RequestParam(value="q28_image", required = false) MultipartFile q28_image,
	    @RequestParam(value="q29_image", required = false) MultipartFile q29_image,
	    @RequestParam(value="q30_image", required = false) MultipartFile q30_image,
	    @RequestParam(value="q31_image", required = false) MultipartFile q31_image,
	    @RequestParam(value="q32_image", required = false) MultipartFile q32_image,
	    @RequestParam(value="q33_image", required = false) MultipartFile q33_image,
	    @RequestParam(value="q34_image", required = false) MultipartFile q34_image,
	    @RequestParam(value="q35_image", required = false) MultipartFile q35_image
	) {
	    return toiletService.saveFeedback(
	        toilet_id, cby, latitude, longitude, zone, ward,
	        q1, q2, q3, q4, q5, q6, q7, q8, q9, q10,
	        q11, q12, q13, q14, q15, q16, q17, q18, q19, q20,
	        q21, q22, q23, q24, q25, q26, q27, q28, q29, q30, q31, q32, q33, q34, q35, 
	        remarks, filedata,
	        q1_image, q2_image, q3_image, q4_image, q5_image, q6_image, q7_image, q8_image, q9_image, q10_image,
	        q11_image, q12_image, q13_image, q14_image, q15_image, q16_image, q17_image, q18_image, q19_image, q20_image,
	        q21_image, q22_image, q23_image, q24_image, q25_image, q26_image, q27_image, q28_image, q29_image, q30_image, 
	        q31_image, q32_image, q33_image, q34_image, q35_image
	    );
	}
	
	@GetMapping(value="/zoneReport")
	public List<?> zoneReport(
			@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="loginId", required = false) String cby
			){
		return toiletService.zoneReport(formData,fromDate,toDate,cby);
	}
	
	@GetMapping(value="/wardReport")
	public List<?> wardReport(
			@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="zone", required = true) String zone,
			@RequestParam(value="loginId", required = false) String cby
			){
		return toiletService.wardReport(formData,fromDate,toDate,zone,cby);
	}
	
	@GetMapping(value="/toiletReport")
	public List<?> toiletReport(
			@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="ward", required = true) String ward,
			@RequestParam(value="loginId", required = false) String cby
			){
		return toiletService.toiletReport(formData,fromDate,toDate,ward,cby);
	}
	
	@GetMapping(value="/toiletDailyFeedbackReport")
	public List<?> toiletDailyFeedbackReport(
			@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="toilet_id", required = false) String toilet_id,
			@RequestParam(value="type", required = true) String type,
			@RequestParam(value="loginId", required = false) String cby
			){
		return toiletService.getToiletDailyFeedbackReport(formData,fromDate,toDate,toilet_id,type,cby);
	}
	
	@GetMapping(value="/feedbackReport")
	public List<?> feedbackReport(
			@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value="filterDate", required = true) String filterDate,
			//@RequestParam(value="toDate", required = true) String toDate,
			//@RequestParam(value="type", required = true) String type,
			@RequestParam(value="toilet_id", required = true) String id,
			@RequestParam(value="loginId", required = false) String cby
			){
		return toiletService.feedbackReport(formData,filterDate,id,cby);
	}
}
