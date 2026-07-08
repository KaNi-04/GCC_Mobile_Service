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

    // kitchen

    @GetMapping("/getConfig")
    public List<?> getConfig(@RequestParam("loginId") String loginId) {
        return pmcservice.getConfig(loginId);
    }

//    @GetMapping("/getFinalFoodCount")
//    public List<Map<String, Object>> getZoneCount(
//            @RequestParam int shiftid,
//            @RequestParam int loginid,
//            @RequestParam String date) {
//
//        return pmcservice.getFinalFoodCount(shiftid, loginid, date);
//    }

//    @GetMapping("/getfeedbackquestions")
//    public List<Map<String, Object>> getFeedbackQuestions(
//            @RequestParam(value = "loginId", required = false) String loginId) {
//        return pmcservice.getParentQuestionsList(loginId);
//    }

//    @PostMapping("/savefeedback")
//    public List<?> savefeedback(
//            @RequestParam(value = "loginId", required = true) String loginId,
//            @RequestParam(value = "auditdate", required = true) String auditdate,
//            @RequestParam(value = "shiftid", required = true) String shiftid,
//            @RequestParam(value = "latitude", required = true) String latitude,
//            @RequestParam(value = "longitude", required = true) String longitude,
//            @RequestParam(value = "zone", required = true) String zone,
//            @RequestParam(value = "ward", required = true) String ward,
//            @RequestParam(value = "address", required = true) String address,
//            @RequestParam(value = "final_food_count", required = true) String final_food_count,
//            @RequestParam(value = "foodid", required = true) String foodid,
//            @RequestParam(value = "food_others", required = false) String food_others,
//            @RequestParam(value = "hub_id", required = true) String hub_id,
//
//            @RequestParam(value = "q1", required = true) String q1,
//            @RequestParam(value = "q2", required = true) String q2,
//            @RequestParam(value = "q3", required = true) String q3,
//            @RequestParam(value = "q4", required = true) String q4,
//            @RequestParam(value = "q5", required = true) String q5,
//            @RequestParam(value = "q6", required = true) String q6,
//            @RequestParam(value = "q7", required = true) String q7,
//            @RequestParam(value = "q8", required = true) String q8,
//            @RequestParam(value = "q9", required = true) String q9,
//            @RequestParam(value = "q10", required = true) String q10,
//            @RequestParam(value = "q11", required = true) String q11,
//            @RequestParam(value = "q12", required = true) String q12,
//            @RequestParam(value = "q13", required = true) String q13,
//            @RequestParam(value = "q14", required = true) String q14,
//            @RequestParam(value = "q15", required = true) String q15,
//            @RequestParam(value = "q16", required = true) String q16,
//            @RequestParam(value = "q17", required = true) String q17,
//            @RequestParam(value = "q18", required = true) String q18,
//
//            @RequestParam(value = "image1", required = true) MultipartFile image1,
//            @RequestParam(value = "image2", required = true) MultipartFile image2,
//            @RequestParam(value = "image3", required = true) MultipartFile image3,
//            @RequestParam(value = "image4", required = true) MultipartFile image4,
//            @RequestParam(value = "image5", required = true) MultipartFile image5) {
//
//        return pmcservice.saveFeedback(
//                loginId, auditdate, shiftid, latitude, longitude, zone, ward, address,
//                final_food_count, foodid, food_others, hub_id,
//                Arrays.asList(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10,
//                        q11, q12, q13, q14, q15, q16, q17, q18),
//                image1, image2, image3, image4, image5);
//    }
   
    //pmc
    
    @GetMapping("/getNotFilledCategories")
    public List<Map<String, Object>> getNotFilledCategories(
            @RequestParam int shiftid,
            @RequestParam int loginid,
            @RequestParam String date) {

        return pmcservice.getNotFilledCategories(shiftid, loginid, date);
    }
    
    
    @GetMapping("/getquestionsbycat")
    public List<Map<String, Object>> getquestionsbycat(
            @RequestParam int qcm_id,
            @RequestParam int loginid,
            @RequestParam int shiftid,
            @RequestParam String latitude,
    		@RequestParam String longitude) {

        return pmcservice.getquestionsbycat(qcm_id,loginid,shiftid,latitude,longitude);
    }
    
    @GetMapping("/getselectedmenu")
    public List<Map<String, Object>> getselectedmenu(
            @RequestParam int shiftid,
            @RequestParam int loginid,
            @RequestParam String date) {

        return pmcservice.getselectedmenu(shiftid, loginid, date);
    }
    
    @PostMapping("/savecatfeedback")
    public List<?> savefeedback(
    		@RequestParam(value = "loginId", required = true) String loginId,
    		@RequestParam(value = "auditdate", required = true) String auditdate,
    		@RequestParam(value = "shiftid", required = true) String shiftid,
    		@RequestParam(value = "latitude", required = true) String latitude,
    		@RequestParam(value = "longitude", required = true) String longitude,
    		@RequestParam(value = "zone", required = true) String zone,
    		@RequestParam(value = "ward", required = true) String ward,
    		@RequestParam(value = "address", required = true) String address,
    		@RequestParam(value = "final_food_count", required = false) String final_food_count,
    		@RequestParam(value = "foodid", required = true) String foodid,
    		@RequestParam(value = "food_others", required = false) String food_others,
    		@RequestParam(value = "hub_id", required = true) String hub_id,
    		@RequestParam(value = "qcm_id", required = true) String qcm_id,
    		@RequestParam(value = "questionAnswers", required = true) String questionAnswers,
    		@RequestParam(value = "images", required = false) MultipartFile[] images
    		) 
    { 
    	return pmcservice.saveFeedbackbycat(
    		    loginId, auditdate, shiftid, latitude, longitude, zone, ward, address,
    		    final_food_count, foodid, food_others, hub_id, qcm_id,
    		    questionAnswers, images
    		);
    }
    
    //foodswing
    
    @GetMapping("/issuefoundhubs")
    public List<Map<String, Object>> getfeedbackhubdetails(
    		@RequestParam (value = "loginid", required = false) Integer loginid,
    		@RequestParam String date
    		){
    	return pmcservice.getfeedbackhubdetails(loginid, date);
    }
    
    
    @GetMapping("/getfoodswingCategory")
    public List<Map<String, Object>> getfoodswingCategory(
            @RequestParam int shiftid,
            @RequestParam int hub_id,
            @RequestParam String date) {

        return pmcservice.getfoodswingCategory(shiftid, hub_id, date);
    }
    
    @GetMapping("/getfoodswingDataNoCategory")
    public List<Map<String, Object>> getfoodswingDataNoCategory(
            @RequestParam int shiftid,
            @RequestParam int hub_id,
            @RequestParam String date,
            @RequestParam(value = "latitude", required = false) String latitude,
    		@RequestParam(value = "longitude", required = false) String longitude,
            @RequestParam(value = "qcm_id", required = false) Integer qcm_id) {

        return pmcservice.getfoodswingDataNoCategory(shiftid, hub_id, date,latitude,longitude,qcm_id);
    }
    
    
    @PostMapping("/savefoodswingdata")
    public List<?> savefoodswingdata(
       @RequestParam(value = "questionAnswers", required = true) String questionAnswers,
       @RequestParam(value = "loginId", required = true) String loginId,
       @RequestParam(value = "latitude", required = false) String latitude,
		@RequestParam(value = "longitude", required = false) String longitude,
		@RequestParam(value = "images", required = false) MultipartFile[] images
    		){
    	
    	return pmcservice.savefoodswingdata(questionAnswers,loginId,latitude,longitude,images);  
    }
    
  
    //ce login data
    
    @GetMapping("/celoginhubs")
    public List<Map<String, Object>> getceloginhubs(
    		@RequestParam (value = "date", required = false) String date
    		){
    	return pmcservice.getceloginhubs();
    }
    
    @GetMapping("/gethubforcelogin")
    public List<Map<String, Object>> gethubsforreport(
    		 @RequestParam int hub_id) {

        return pmcservice.getHubDateShiftDetails(hub_id);
    }
    
    
    @GetMapping("/getCEDataNoCategory")
    public List<Map<String, Object>> getCEDataNoCategory(
            @RequestParam int shiftid,
            @RequestParam int hub_id,
            @RequestParam String date) {

        return pmcservice.getCEDataNoCategory(shiftid, hub_id, date);
    }
    
    @PostMapping("/savecelogindata")
    public List<?> savecelogindata(
       @RequestParam(value = "questionAnswers", required = true) String questionAnswers,
       @RequestParam(value = "loginId", required = true) String loginId
    		){
    	
    	return pmcservice.savecelogindata(questionAnswers,loginId);  
    }
    
    //
    
    
    //General Common Report
    @GetMapping("/gethubsforreport")
    public List<Map<String, Object>> gethubsforreport(
            @RequestParam String date,
            @RequestParam(required = false) Integer loginId) {

        return pmcservice.gethubsforreport(date, loginId);
    }
    
    
    
    @GetMapping("/getcatsforhubreport")
    public List<Map<String, Object>> getcatsforhubreport(
            @RequestParam int shiftid,
            @RequestParam int hub_id,
            @RequestParam String date) {

        return pmcservice.getcatsforhubreport(shiftid, hub_id, date);
    }
    
    @GetMapping("/getreportdata")
    public List<Map<String, Object>> getreportdata(
            @RequestParam int shiftid,
            @RequestParam int hub_id,
            @RequestParam String date,
            @RequestParam int qcm_id) {

        return pmcservice.getreportdata(shiftid, hub_id, date,qcm_id);
    }
    
    //penalty Report
    
    @GetMapping("/penalty/hubs")
    public List<Map<String, Object>> getPenaltyHubSummary(@RequestParam int loginid) {
        return pmcservice.getPenaltyHubSummary(loginid);
    }
    
    @GetMapping("/penalty/hubdetails")
    public List<Map<String, Object>> getHubDetails(
            @RequestParam int hub_id,
            @RequestParam String type) {

        return pmcservice.getHubDateShiftDetails(hub_id, type);
    }
    
    @GetMapping("/penalty/category")
    public List<Map<String, Object>> getCategoryReport(
            @RequestParam int hub_id,
            @RequestParam int shiftid,
            @RequestParam String date,
            @RequestParam String type) {

        return pmcservice.getCategoryReport(hub_id, shiftid, date, type);
    }
    
    @GetMapping("/penalty/questions")
    public List<Map<String, Object>> getQuestionReport(
            @RequestParam int hub_id,
            @RequestParam int shiftid,
            @RequestParam String date,
            @RequestParam String type,
            @RequestParam int qcm_id) {

        return pmcservice.getQuestionReport(hub_id, shiftid, date, type, qcm_id);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    @GetMapping("/getfeedbackreport")
    public List<Map<String, Object>> getfeedbackreport(
            @RequestParam int shiftid,
            @RequestParam int loginid,
            @RequestParam String date) {

        return pmcservice.getfeedbackreport(shiftid, loginid, date);
    }
    
    
    
 
    @GetMapping("/getFinalFoodCount")
    public List<Map<String, Object>> getFinalFoodCount(
            @RequestParam int shiftid,
            @RequestParam int loginid,
            @RequestParam String date) {

        return pmcservice.getFinalFoodCountPerHubId(shiftid, loginid, date);
    }
    
    
    @GetMapping("/getfoodswingData")
    public List<Map<String, Object>> getfoodswingData(
            @RequestParam int shiftid,
            @RequestParam int loginid,
            @RequestParam String date){
    	
    	return pmcservice.getfoodswingData(shiftid, loginid, date);
    	
    }
    
    
    

    
    
   
    
    
    

    // dispatch
//    @GetMapping("/getDispatchFoodCount")
//    public List<Map<String, Object>> getZoneCountForDispatch(
//            @RequestParam int shiftid,
//            @RequestParam int loginid,
//            @RequestParam String date) {
//
//        return pmcservice.getFinalFoodCountForDispatch(shiftid, loginid, date);
//    }
    
    @GetMapping("/getDispatchFoodCount")
    public List<Map<String, Object>> getZoneCountForDispatch(
            @RequestParam int shiftid,
            @RequestParam int loginid,
            @RequestParam String date) {

        return pmcservice.getFoodCountForDispatch(shiftid, loginid, date);
    }

//    @PostMapping("/savedispatch")
//    public List<?> savedispatch(
//            @RequestParam(value = "pmc_audit_id", required = true) int pmc_audit_id,
//            @RequestParam(value = "driver_name", required = true) String driver_name,
//            @RequestParam(value = "driver_mob_num", required = true) String driver_mob_num,
//            @RequestParam(value = "vehicle_number", required = true) String vehicle_number,
//            @RequestParam(value = "packedfoodphoto", required = true) MultipartFile packedfoodphoto,
//            @RequestParam(value = "vehiclephoto", required = true) MultipartFile vehiclephoto,
//            @RequestParam(value = "loginId", required = true) String loginId,
//            @RequestParam(value = "yet_dispatch_count", required = true) int yet_dispatch_count,
//            @RequestParam(value = "dispatch_food_list") String dispatch_food_list) {
//
//        return pmcservice.savedispatch(pmc_audit_id, driver_name, driver_mob_num, vehicle_number, packedfoodphoto,
//                vehiclephoto, loginId, yet_dispatch_count, dispatch_food_list);
//    }
    
    
    @PostMapping("/savedispatch")
    public List<?> savedispatch(
    		@RequestParam(value = "auditdate", required = true) String auditdate,
    		@RequestParam(value = "shiftid", required = true) String shiftid,
    		@RequestParam(value = "hub_id", required = true) String hub_id,
            @RequestParam(value = "driver_name", required = true) String driver_name,
            @RequestParam(value = "driver_mob_num", required = true) String driver_mob_num,
            @RequestParam(value = "vehicle_number", required = true) String vehicle_number,
            @RequestParam(value = "packedfoodphoto", required = true) MultipartFile packedfoodphoto,
            @RequestParam(value = "vehiclephoto", required = true) MultipartFile vehiclephoto,
            @RequestParam(value = "loginId", required = true) String loginId,
            @RequestParam(value = "yet_dispatch_count", required = true) int yet_dispatch_count,
            @RequestParam(value = "dispatch_food_list") String dispatch_food_list) {

        return pmcservice.savefordispatch(auditdate,shiftid,hub_id, driver_name, driver_mob_num, vehicle_number, packedfoodphoto,
                vehiclephoto, loginId, yet_dispatch_count, dispatch_food_list);
    }

    @GetMapping("/getvehicleallocated")
    public List<Map<String, Object>> getvehicleallocated(
            @RequestParam int shiftid,
            @RequestParam int loginid,
            @RequestParam String date) {

        return pmcservice.getvehicleallocated(shiftid, loginid, date);
    }

}
