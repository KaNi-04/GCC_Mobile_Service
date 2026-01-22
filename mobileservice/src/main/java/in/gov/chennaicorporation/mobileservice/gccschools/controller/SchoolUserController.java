package in.gov.chennaicorporation.mobileservice.gccschools.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccschools.service.SchoolUserActivity;
//import in.gov.chennaicorporation.mobileservice.gccsos.service.UserActivity;

@RequestMapping("/gccofficialapp/api/gccschools/user/")
@RestController("gccofficialappUserSchoolsRest")
public class SchoolUserController {
	   @Autowired
	    private JdbcTemplate jdbcSchoolsTemplate;
	@Autowired
	private SchoolUserActivity schoolUserActivity;
	
	@GetMapping("/getSchoolList")
	public ResponseEntity<List<Map<String, Object>>> getSchoolList(@RequestParam String ward, @RequestParam String category_type) {
	    List<Map<String, Object>> schoolList = schoolUserActivity.getSchoolList(ward,category_type);
	    
	    if (schoolList.isEmpty()) {
	        // Return a "no data" message when the list is empty
	        return ResponseEntity.status(404).body(Collections.singletonList(Collections.singletonMap("message", "No data available")));
	    }
	    
	    return ResponseEntity.ok(schoolList);
	}
	
	@PostMapping("/saveschool")
	public ResponseEntity<String> saveSchool(
	    @RequestParam String latitude,
	    @RequestParam String longitude,
	    @RequestParam(required = false) MultipartFile photo,
	    @RequestParam Integer schoolType,
	    @RequestParam String schoolName,
	    @RequestParam String earmark,
	    @RequestParam String zone,
	    @RequestParam String division,
	    @RequestParam String user_id
	) {
	    try {
	        // Save school details if type is valid
	        schoolUserActivity.saveSchool(latitude, longitude, photo, schoolType, schoolName, earmark, zone, division, user_id);
	        return new ResponseEntity<>("School details saved successfully.", HttpStatus.OK);
	    } catch (IllegalArgumentException e) {
	        return new ResponseEntity<>("Failed to save school details: " + e.getMessage(), HttpStatus.BAD_REQUEST);
	    } catch (Exception e) {
	        return new ResponseEntity<>("Failed to save school details: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@PostMapping("/savetype")
	public ResponseEntity<Integer> saveSchoolType(@RequestParam String schoolType) {
	    Integer schoolTypeId = schoolUserActivity.saveSchoolType(schoolType);
	    return ResponseEntity.ok(schoolTypeId);
	}

    @GetMapping("/getCategoryWithQuestions")
    public ResponseEntity<Map<String, Object>> getCategoryWithQuestions(
            @RequestParam int categoryId
    ) {
        try {
            Map<String, Object> categoryDetails = schoolUserActivity.getCategoryWithQuestions(categoryId);
            return ResponseEntity.ok(categoryDetails);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to retrieve category details: " + e.getMessage()));
       }
    }
    
    @GetMapping("/getFeedback")
    public ResponseEntity<?> getFeedback(@RequestParam int catQuestionId) {
        try {
            // Call the service method to fetch feedback
            List<Map<String, Object>> feedbackDetails = schoolUserActivity.getFeedbackByQuestionId(catQuestionId);

            // Check if feedback exists for the given catQuestionId
            if (feedbackDetails.isEmpty()) {
                return new ResponseEntity<>("No feedback found for the provided category question ID.", HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(feedbackDetails, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to retrieve feedback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/getSchools")
    public ResponseEntity<?> getSchools(
            @RequestParam String latitude,
            @RequestParam String longitude 
    ) {
        try {
            // Validate latitude and longitude
            double lat = Double.parseDouble(latitude);
            double lon = Double.parseDouble(longitude);
            
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                return ResponseEntity.badRequest()
                                     .body(Collections.singletonList("Latitude must be between -90 and 90, and longitude between -180 and 180."));
            }
            //String latitude1=latitude;

            // Get schools based on location
            List<Map<String, Object>> schools = schoolUserActivity.getSchoolsByLocationAndType(latitude, longitude);

            if (schools.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .body(Collections.singletonList("No schools found for the specified location."));
            }

            return ResponseEntity.ok(schools);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                                 .body(Collections.singletonList("Invalid latitude or longitude format."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonList("Failed to retrieve schools: " + e.getMessage()));
        }
    }


    @PostMapping("/savechennaiboard")
    public ResponseEntity<?> saveChennaiBoard(
        @RequestParam int categoryId,
        @RequestParam String newboardconstructQa,
        @RequestParam String newboardconstructAns,
        @RequestParam String SchoolunderConstructQa,
        @RequestParam String SchoolunderConstructAns,
        @RequestParam(required = false) MultipartFile feedPhoto, // file is optional
        @RequestParam(required = false) int school_details_id, // school_details_id is optional
        @RequestParam String zone,
        @RequestParam String division,
        @RequestParam double latitude,
        @RequestParam double longitude,  
        @RequestParam String user_id ) {

        try {
            // Save feedback for both questions
            schoolUserActivity.saveChennaiSchoolBoard(categoryId, newboardconstructQa, newboardconstructAns,
                    SchoolunderConstructQa, SchoolunderConstructAns, feedPhoto, school_details_id, zone, division,latitude,longitude,user_id);

            return new ResponseEntity<>("Feedback saved successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to save feedback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/saveDrinkingWater")
    public ResponseEntity<?> saveDrinkingWater(
        @RequestParam int categoryId,
        @RequestParam String MetroWaterFacilitiesQa,
        @RequestParam String MetroWaterFacilitiesAns,
        @RequestParam String SumpFacilitiesAvailableQa,
        @RequestParam String SumpFacilitiesAvailableAns,
        @RequestParam String SumpGoodConditionQa,
        @RequestParam String SumpGoodConditionAns,
        @RequestParam String ROFacilitiesAvailableQa,
        @RequestParam String ROFacilitiesAvailableAns,
        @RequestParam String ROGoodConditionQa,
        @RequestParam String ROGoodConditionAns,//Drinking water facilities available or not
        @RequestParam String DrinkingwaterfacilitiesQa,
        @RequestParam String DrinkingwaterfacilitiesAns,
        @RequestParam String typeoffacilitiesQa,
        @RequestParam String typeoffacilitiesAns,
        
        @RequestParam(required = false) MultipartFile feedPhoto,
        @RequestParam(required = false) MultipartFile storageofdrinkingPhoto,
        @RequestParam(required = false) MultipartFile roPhoto,
        
        @RequestParam(required = false) int school_details_id,
        @RequestParam String zone,
        @RequestParam String division,
        @RequestParam double latitude,
        @RequestParam double longitude,
        @RequestParam String user_id
  ) {

        try {
            // Call the service method to save feedback
            schoolUserActivity.savewaterfd(categoryId, MetroWaterFacilitiesQa, MetroWaterFacilitiesAns,
                    SumpFacilitiesAvailableQa, SumpFacilitiesAvailableAns,
                    SumpGoodConditionQa, SumpGoodConditionAns,
                    ROFacilitiesAvailableQa, ROFacilitiesAvailableAns,
                    ROGoodConditionQa, ROGoodConditionAns,DrinkingwaterfacilitiesQa,DrinkingwaterfacilitiesAns,
                    typeoffacilitiesQa,typeoffacilitiesAns,
                    feedPhoto, storageofdrinkingPhoto, roPhoto, 
                    school_details_id, zone, division,latitude,longitude,user_id);

            return new ResponseEntity<>("Feedback saved successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to save feedback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/FirstAidKit")
    public ResponseEntity<?> FirstAidKit(
        @RequestParam int categoryId,
        @RequestParam String FirstAidKitAvailableQa,
        @RequestParam String FirstAidKitAvailableAns,
        @RequestParam(required = false) MultipartFile feedPhoto,
        @RequestParam(required = false) int school_details_id,
        @RequestParam String zone,
        @RequestParam String division  ,
        @RequestParam double latitude,
        @RequestParam double  longitude,
        @RequestParam String user_id) {

        try {
            // Call the service method to save feedback
            schoolUserActivity.saveFirstAidKitFeedback(categoryId, FirstAidKitAvailableQa, FirstAidKitAvailableAns, 
                    feedPhoto, school_details_id, zone, division, latitude,longitude,user_id);

            return new ResponseEntity<>("Feedback saved successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to save feedback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/Cleaning")
    public ResponseEntity<?> Cleaning(
        @RequestParam int categoryId,
        @RequestParam String ToiletsAvailableQa,
        @RequestParam String ToiletsAvailableAns,
        @RequestParam String UrinariesAvailableQa,
        @RequestParam String UrinariesAvailableAns,
        @RequestParam String SweepersAvailableQa,
        @RequestParam String SweepersAvailableAns,
        @RequestParam String SewagesAvailableQa,
        @RequestParam String SewagesAvailableAns,
        @RequestParam String RoomsAvailableQa,
        @RequestParam String RoomsAvailableAns,
        @RequestParam String PlaygroundAvailableQa,
        @RequestParam String PlaygroundAvailableAns,
        @RequestParam String DrainageAvailableQa,
        @RequestParam String DrainageAvailableAns,
        @RequestParam(required = false) MultipartFile toiletfeedPhoto,
        @RequestParam(required = false) MultipartFile RoomsfeedPhoto,
        @RequestParam(required = false) MultipartFile PlaygroundfeedPhoto,
        @RequestParam(required = false) int school_details_id,
        @RequestParam String zone,
        @RequestParam String division,
        @RequestParam double latitude,
        @RequestParam double longitude,
        @RequestParam String   user_id) {

        try {
            // Call the service method to save feedback
            schoolUserActivity.saveCleaningFeedback(
                categoryId,
                ToiletsAvailableQa, ToiletsAvailableAns,
                UrinariesAvailableQa, UrinariesAvailableAns,
                SweepersAvailableQa, SweepersAvailableAns,
                SewagesAvailableQa, SewagesAvailableAns,
                RoomsAvailableQa, RoomsAvailableAns,
                PlaygroundAvailableQa, PlaygroundAvailableAns,
                DrainageAvailableQa, DrainageAvailableAns,
                toiletfeedPhoto, RoomsfeedPhoto, PlaygroundfeedPhoto,
                school_details_id, zone, division, latitude, longitude,user_id
            );

            return new ResponseEntity<>("Feedback saved successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to save feedback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @PostMapping("/NoonMealKitchenCenter")
    public ResponseEntity<?> NoonMealKitchenCenter(
        @RequestParam int categoryId,
        @RequestParam String NoonMealKitchenAvailableQa,
        @RequestParam String NoonMealKitchenAvailableAns,
        @RequestParam String GasStoveQa,
        @RequestParam String GasStoveAns,
        @RequestParam String StoreRoomAvailableQa,
        @RequestParam String StoreRoomAvailableAns,
        @RequestParam String UtensilsAvailableQa,
        @RequestParam String UtensilsAvailableAns,
        @RequestParam String SufficientNumberQa,
        @RequestParam String SufficientNumberAns,
        @RequestParam String BlockageInWashAreaQa,
        @RequestParam String BlockageInWashAreaAns,
        @RequestParam String SeepageQa,
        @RequestParam String SeepageAns,
        @RequestParam(required = false) MultipartFile feedPhoto,
        @RequestParam(required = false) MultipartFile utensilsPhoto,
        @RequestParam(required = false) MultipartFile blockagePhoto,
        @RequestParam(required = false) MultipartFile seepagePhoto,
        @RequestParam(required = false) int school_details_id,
        @RequestParam String zone,
        @RequestParam String division,
        @RequestParam Double latitude,
        @RequestParam Double longitude,
        @RequestParam String   user_id) {

        try {
            // Call the service method to save feedback
            schoolUserActivity.saveNoonMealFeedback(
                categoryId,
                NoonMealKitchenAvailableQa, NoonMealKitchenAvailableAns,
                GasStoveQa, GasStoveAns,
                StoreRoomAvailableQa, StoreRoomAvailableAns,
                UtensilsAvailableQa, UtensilsAvailableAns,
                SufficientNumberQa, SufficientNumberAns,
                BlockageInWashAreaQa, BlockageInWashAreaAns,
                SeepageQa, SeepageAns,
                feedPhoto, utensilsPhoto, blockagePhoto, seepagePhoto, school_details_id, zone, division,latitude,longitude,user_id
            );

            return new ResponseEntity<>("Feedback saved successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to save feedback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/Electricity")
    public ResponseEntity<?> Electricity(
        @RequestParam int categoryId,
        @RequestParam String FansAvailableQa,
        @RequestParam String FansAvailableAns,
        @RequestParam String FansNotAvailableQa,
        @RequestParam String FansNotAvailableAns,
        @RequestParam String LightsAvailableQa,
        @RequestParam String LightsAvailableAns,
        @RequestParam String LightsNotAvailableQa,
        @RequestParam String LightsNotAvailableAns,
        @RequestParam String SwitchBoardQa,
        @RequestParam String SwitchBoardAns,
        @RequestParam String GenSetQa,
        @RequestParam String GenSetAns,
        @RequestParam String GenSetconditionQa,
        @RequestParam String GenSetconditionAns,
        @RequestParam(required = false) MultipartFile feedPhoto, 
        @RequestParam(required = false) int school_details_id,
        @RequestParam String zone,
        @RequestParam String division,
        @RequestParam   double latitude ,
        @RequestParam double longitude,
        @RequestParam String user_id) {

        try {
            // Call the service method to save feedback
            schoolUserActivity.saveElectricityFeedback(
                categoryId,
                FansAvailableQa, FansAvailableAns, FansNotAvailableQa, FansNotAvailableAns,
                LightsAvailableQa, LightsAvailableAns, LightsNotAvailableQa, LightsNotAvailableAns,
                SwitchBoardQa, SwitchBoardAns,
                GenSetQa, GenSetAns,GenSetconditionQa,GenSetconditionAns,
                feedPhoto, school_details_id, zone, division,latitude,longitude,user_id
            );

            return new ResponseEntity<>("Feedback saved successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to save feedback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/ITAssets")
    public ResponseEntity<?> ITAssets(
        @RequestParam int categoryId,
        @RequestParam String SystemAvailableQa,
        @RequestParam String SystemAvailableAns,
        @RequestParam String SystemNotAvailableQa,
        @RequestParam String SystemNotAvailableAns,
        @RequestParam String PrinterAvailableQa,
        @RequestParam String PrinterAvailableAns,
        @RequestParam String PrinterNotAvailableQa,
        @RequestParam String PrinterNotAvailableAns,
        @RequestParam String UPSAvailableQa,
        @RequestParam String UPSAvailableAns,
        @RequestParam String UPSNotAvailableQa,
        @RequestParam String UPSNotAvailableAns,
        @RequestParam(required = false) String CCTVAvailableQa,
        @RequestParam(required = false) String CCTVAvailableAns,
        @RequestParam(required = false) String CCTVNotAvailableQa,
        @RequestParam(required = false) String CCTVNotAvailableAns,
        @RequestParam String NetworkAvailableQa,
        @RequestParam String NetworkAvailableAns,
        @RequestParam String projectorgoodconditionQa,
        @RequestParam String projectorgoodconditionAns,
        @RequestParam String smartboardgoodconditionQa,
        @RequestParam String smartboardgoodconditionAns,
        @RequestParam(required = false) MultipartFile feedPhoto,
        @RequestParam(required = false) int school_details_id,
        @RequestParam String zone,
        @RequestParam String division,
        @RequestParam Double latitude,
        @RequestParam double longitude,
        @RequestParam String user_id) {

        try {
            // Call the service method to save feedback
            schoolUserActivity.saveITAssetsFeedback(
                categoryId,
                SystemAvailableQa, SystemAvailableAns, SystemNotAvailableQa, SystemNotAvailableAns,
                PrinterAvailableQa, PrinterAvailableAns, PrinterNotAvailableQa, PrinterNotAvailableAns,
                UPSAvailableQa, UPSAvailableAns, UPSNotAvailableQa, UPSNotAvailableAns,
                CCTVAvailableQa, CCTVAvailableAns, CCTVNotAvailableQa, CCTVNotAvailableAns,
                NetworkAvailableQa, NetworkAvailableAns,
                projectorgoodconditionQa, projectorgoodconditionAns,
                smartboardgoodconditionQa, smartboardgoodconditionAns,
                feedPhoto, school_details_id, zone, division,latitude,longitude,user_id
            );

            return new ResponseEntity<>("Feedback saved successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to save feedback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/SeepageSchool")
    public ResponseEntity<?> SeepageSchool(
        @RequestParam int categoryId,
        @RequestParam String SeepageinSchoolQa,
        @RequestParam String SeepageinSchoolAns,
        @RequestParam String HowmanyLocationsQa,
        @RequestParam String HowmanyLocationsAns,
        @RequestParam(required = false) MultipartFile seepageinPhoto,
        @RequestParam(required = false) int school_details_id,
        @RequestParam String zone,
        @RequestParam String division,
        @RequestParam(required = false) Double latitude,
        @RequestParam Double longitude,
      @RequestParam String  user_id) { // Add latitude parameter

        try {
            // Call the service method to save feedback
            schoolUserActivity.saveSeepageSchoolFeedback(
                categoryId,
                SeepageinSchoolQa, SeepageinSchoolAns,
                HowmanyLocationsQa, HowmanyLocationsAns,
                seepageinPhoto, 
                school_details_id, zone, division,
                latitude,longitude,user_id
            );

            return new ResponseEntity<>("Feedback saved successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to save feedback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
 
    @GetMapping("/subCategories")
    public ResponseEntity<List<Map<String, Object>>> getAllSubCategories() {
        List<Map<String, Object>> subCategories = schoolUserActivity.getAllSubCategories();
        
        if (subCategories.isEmpty()) {
            return ResponseEntity.status(404).body(null); // or use a different response if needed
        }
        
        return ResponseEntity.ok(subCategories);
    }

    @GetMapping("/getschooltype")
    public ResponseEntity<Map<String, Object>> getSchoolType(@RequestParam int id) {
        Map<String, Object> schoolType = schoolUserActivity.getSchoolTypeById(id);
        return ResponseEntity.ok(schoolType);
    }
    @GetMapping("/get")
    public ResponseEntity<List<Map<String, Object>>> getSchoolTypes() {
        List<Map<String, Object>> schoolTypes = schoolUserActivity.getAllSchoolTypes();
        return ResponseEntity.ok(schoolTypes);
    }
  
    @GetMapping("/getcountsdivision")
    public ResponseEntity<?> getFeedbackCountByDivision(@RequestParam String division) {
        try {
            List<Map<String, Object>> feedbackCount = schoolUserActivity.getFeedbackCountByDivision(division);
            return ResponseEntity.ok(feedbackCount);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonList("Failed to retrieve feedback count: " + e.getMessage()));
        }
    }
    
    @GetMapping("/overallreport")
    public Map<String, Object> getFeedbackBySchoolId(
            @RequestParam int schoolId,
            @RequestParam String division,
            @RequestParam String zone,
            @RequestParam int categoryId,
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam String user_id) {
        
        return schoolUserActivity.getFeedbackBySchoolId(schoolId, division, zone, categoryId, fromDate, toDate, user_id);
    }
    
    @GetMapping("/feedbackdetails")
    public Map<String, Object> getFeedbackByFeedRef(
            @RequestParam String feedref,
            @RequestParam String user_id) {
        
        return schoolUserActivity.getFeedbackByFeedRef(feedref, user_id);
    }
    
    /*
     * @GetMapping("/overallreport")
    public ResponseEntity<?> getFeedbackBySchoolId(
            @RequestParam int schoolId, 
            @RequestParam String division, 
            @RequestParam String zone,
            @RequestParam int categoryId,
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam (required = false) String user_id
           ) {
        try {
            List<Map<String, Object>> feedbackList = schoolUserActivity.getFeedbackBySchoolId(schoolId, division, zone, categoryId, fromDate, toDate, user_id);

            if (feedbackList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .body(Collections.singletonList("No feedback found for the specified parameters."));
            }

            // Initialize response structure
            List<Map<String, Object>> responseList = new ArrayList<>();

            // Create a map to store the last inserted data for each question
            Map<String, Map<String, Object>> lastInsertedData = new HashMap<>();

            // Create a map to store the common data
            Map<String, Object> commonData = feedbackList.get(0);

            // Retrieve the schoolId from the feedbackList
            Integer schoolIdValue = (Integer) feedbackList.get(0).get("schoolId");

            // Create a map to store the grouped feedbacks
            Map<String, List<Map<String, Object>>> groupedFeedbacks = new HashMap<>();

            // Create a set to store the common photos
            Set<String> commonPhotos = new HashSet<>();

            for (Map<String, Object> feedback : feedbackList) {
                String[] questions = ((String) feedback.get("category_questions")).split(",");
                String feedbackResponse = (String) feedback.get("feedback");
                String feedbackPhotos = (String) feedback.get("feedbackphotos");

                if (feedbackPhotos != null && !feedbackPhotos.isEmpty()) {
                    String[] photos = feedbackPhotos.split(",");
                    for (String photo : photos) {
                        commonPhotos.add(photo.trim());
                    }
                }

                for (String question : questions) {
                    question = question.trim();

                    // Check if the question already exists in the last inserted data map
                    if (lastInsertedData.containsKey(question)) {
                        Map<String, Object> existingEntry = lastInsertedData.get(question);
                        existingEntry.put("feedback", feedbackResponse);
                        existingEntry.put("feedback_photos", new ArrayList<>(commonPhotos));
                    } else {
                        Map<String, Object> newEntry = new HashMap<>();
                        newEntry.put("feedback", feedbackResponse);
                        newEntry.put("category_question", question);
                        newEntry.put("feedback_photos", new ArrayList<>(commonPhotos));
                        lastInsertedData.put(question, newEntry);
                    }
                }
            }

            // Create a key for the common data
            String key = commonData.get("division") + "_" + commonData.get("cdate") + "_" + commonData.get("zone") + "_" + commonData.get("schoolId") + "_" + commonData.get("school_name") + "_" + commonData.get("category");

            // Add the feedbacks to the grouped feedbacks map
            if (!groupedFeedbacks.containsKey(key)) {
                groupedFeedbacks.put(key, new ArrayList<>());
            }

            for (Map<String, Object> entry : lastInsertedData.values()) {
                groupedFeedbacks.get(key).add(entry);
            }

            // Create the response list with the grouped feedbacks
            for (Map.Entry<String, List<Map<String, Object>>> entry : groupedFeedbacks.entrySet()) {
                Map<String, Object> response = new HashMap<>();
                String[] parts = entry.getKey().split("_");
                response.put("division", parts[0]);
                response.put("cdate", parts[1]);
                response.put("zone", parts[2]);
                response.put("school_details_id", schoolId);
                response.put("school_name", parts[4]);
                response.put("category", parts[5]);
                response.put("feedbacks", entry.getValue());

                // Add the common photos to the response
                for (Map<String, Object> feedback : (List<Map<String, Object>>) response.get("feedbacks")) {
                    feedback.put("feedback_photos", new ArrayList<>(commonPhotos));
                }

                responseList.add(response);
            }

            // Find which photo belongs to which question
            List<String> questions = new ArrayList<>();
            for (Map<String, Object> feedback : feedbackList) {
                String[] questionArray = ((String) feedback.get("category_questions")).split(",");
                questions.addAll(Arrays.asList(questionArray));
            }

            Map<String, List<String>> questionPhotos = new HashMap<>();
            for (String question : questions) {
                List<String> photos = getPhotosForQuestion(question, lastInsertedData);
                questionPhotos.put(question, photos);
            }

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonList(" Failed to retrieve feedback: " + e.getMessage()));
        }
    }
*/
    private List<String> getPhotosForQuestion(String question, Map<String, Map<String, Object>> lastInsertedData) {
        List<String> photos = new ArrayList<>();
        for (Map<String, Object> entry : lastInsertedData.values()) {
            if (entry.get("category_question").equals(question)) {
                photos.addAll((List<String>) entry.get("feedback_photos"));
            }
        }
        return photos;
    }
	
	@GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        try {
            List<Map<String, Object>> categories = schoolUserActivity.getActiveCategories();

            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonList("Failed to retrieve categories: " + e.getMessage()));
        }
    }
	

	   @GetMapping("/categorydates")
	    public ResponseEntity<?> getFeedbackCountByCategoryAndDate(
	            @RequestParam int categoryId,
	            @RequestParam String startDate, // Change to String
	            @RequestParam String endDate,
	            @RequestParam (required = false) String user_id) { // Change to String
	        try {
	            // Define the date formatter
	            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	            LocalDate start = LocalDate.parse(startDate, formatter);
	            LocalDate end = LocalDate.parse(endDate, formatter);
	            
	            List<Map<String, Object>> feedbackCounts = schoolUserActivity.getFeedbackCountByCategoryAndDate(categoryId, start, end, user_id);
	            if (feedbackCounts.isEmpty()) {
	                return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                                     .body(Collections.singletonList("No feedback found for the specified category and date range."));
	            }
	            return ResponseEntity.ok(feedbackCounts);
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                                 .body(Collections.singletonList("Failed to retrieve feedback count: " + e.getMessage()));
	        }
	    }
	
	@GetMapping("/divisionschools")
    public ResponseEntity<?> getFeedbackCountByDivisionAndSchool(
            @RequestParam String division,
            @RequestParam String categoryId, // Add categoryId parameter
            @RequestParam String fromDate, 
            @RequestParam String toDate,
            @RequestParam (required = false) String user_id
            ) { 
        try {
            // Define the date formatter
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate from = LocalDate.parse(fromDate, formatter);
            LocalDate to = LocalDate.parse(toDate, formatter);
            
            List<Map<String, Object>> feedbackCounts = schoolUserActivity.getFeedbackCountByDivisionAndSchool(division, categoryId, from, to, user_id);
            
            if (feedbackCounts.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .body(Collections.singletonList("No feedback found for the specified division and date range."));
            }

            return ResponseEntity.ok(feedbackCounts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonList("Failed to retrieve feedback count: " + e.getMessage()));
        }
    }
	
	@GetMapping("/divisionschoolslist")
    public ResponseEntity<?> getFeedbackCountByDivisionAndSchoolList(
            @RequestParam String schoolId,
            @RequestParam String division,
            @RequestParam String categoryId, // Add categoryId parameter
            @RequestParam String fromDate, 
            @RequestParam String toDate,
            @RequestParam (required = false) String user_id
            ) { 
        try {
            // Define the date formatter
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate from = LocalDate.parse(fromDate, formatter);
            LocalDate to = LocalDate.parse(toDate, formatter);
            
            List<Map<String, Object>> feedbackCounts = schoolUserActivity.getFeedbackCountByDivisionAndSchoolList(division, schoolId, categoryId, from, to, user_id);
            
            if (feedbackCounts.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .body(Collections.singletonList("No feedback found for the specified division and date range."));
            }

            return ResponseEntity.ok(feedbackCounts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonList("Failed to retrieve feedback count: " + e.getMessage()));
        }
    }
	
	@GetMapping("/myreportlist")
    public ResponseEntity<?> getMyReportList(
            @RequestParam String categoryId, // Add categoryId parameter
            @RequestParam String fromDate, 
            @RequestParam String toDate,
            @RequestParam (required = true) String user_id
            ) { 
        try {
            // Define the date formatter
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate from = LocalDate.parse(fromDate, formatter);
            LocalDate to = LocalDate.parse(toDate, formatter);
            
            List<Map<String, Object>> feedbackCounts = schoolUserActivity.getFeedbackCountSchoolListByUserId(categoryId, from, to, user_id);
            
            if (feedbackCounts.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .body(Collections.singletonList("No feedback found for the specified division and date range."));
            }

            return ResponseEntity.ok(feedbackCounts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonList("Failed to retrieve feedback count: " + e.getMessage()));
        }
    }
	
	@GetMapping("/zonecatschool")
	public ResponseEntity<List<Map<String, Object>>> getFeedbackCountByZoneCat(
            @RequestParam String zone,
            @RequestParam int categoryId,
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam(required = false) String user_id) {
        
        // Parse the date strings to LocalDate
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate from = LocalDate.parse(fromDate, formatter);
        LocalDate to = LocalDate.parse(toDate, formatter);
        
        // Call the service method
        List<Map<String, Object>> feedbackCounts = schoolUserActivity.getFeedbackCountByZoneCat(zone, categoryId, from, to, user_id);

        return ResponseEntity.ok(feedbackCounts);
    }
	
	/*
	@GetMapping("/zonecatschool")
    public ResponseEntity<?> getFeedbackCountByZoneCat(
            @RequestParam String zone, 
            @RequestParam int categoryId,
            @RequestParam String fromDate, // Change to String
            @RequestParam String toDate,
            @RequestParam (required = false) String user_id) { // Change to String
        try {
            // Define the date formatter for dd-MM-yyyy 
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate from = LocalDate.parse(fromDate, formatter);
            LocalDate to = LocalDate.parse(toDate, formatter);
            
            List<Map<String, Object>> feedbackCounts = schoolUserActivity.getFeedbackCountByZoneCat(zone, categoryId, from, to, user_id);
            
            if (feedbackCounts.isEmpty()) {
                 return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .body(Collections.singletonList("No feedback found for the specified zone and date range."));
            }

            // Assuming all feedback counts have the same division, extract it
            String division = feedbackCounts.get(0).get("division").toString();

            // Build the response
            Map<String, Object> response = new HashMap<>();
            response.put("division", division);
            
            // Sum up feedback counts as Long
            long totalFeedbackCount = feedbackCounts.stream()
                .mapToLong(feedback -> ((Number) feedback.get("feedback_count")).longValue())
                .sum();
            
            response.put("feedback_count", totalFeedbackCount);

            // Wrap the response in a list
            return ResponseEntity.ok(Collections.singletonList(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonList("Failed to retrieve feedback count: " + e.getMessage()));
        }
    }
    */
	
	@GetMapping("/divisioncatschool")
    public ResponseEntity<?> getFeedbackCountByDivCat(
            @RequestParam String division, 
            @RequestParam int categoryId,
            @RequestParam String fromDate, // Change to String
            @RequestParam String toDate,
            @RequestParam (required = false) String user_id) { // Change to String
        try {
            // Define the date formatter for dd-MM-yyyy
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate from = LocalDate.parse(fromDate, formatter);
            LocalDate to = LocalDate.parse(toDate, formatter);
            
            List<Map<String, Object>> feedbackCounts = schoolUserActivity.getFeedbackCountByDivCat(division, categoryId, from, to);
            
            if (feedbackCounts.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .body(Collections.singletonList("No feedback found for the specified division."));
            }

            // Create a map to aggregate feedback counts by division
            Map<String, Long> aggregatedCounts = new HashMap<>();
            
            for (Map<String, Object> feedback : feedbackCounts) {
                String div = (String) feedback.get("division");
                Long count = (Long) feedback.get("feedback_count");

                // Aggregate feedback counts
                aggregatedCounts.put(div, aggregatedCounts.getOrDefault(div, 0L) + count);
            }

            // Build the response
            List<Map<String, Object>> response = new ArrayList<>();
            for (Map.Entry<String, Long> entry : aggregatedCounts.entrySet()) {
                Map<String, Object> divisionData = new HashMap<>();
                divisionData.put("division", entry.getKey());
                divisionData.put("feedback_count", entry.getValue());
                response.add(divisionData);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonList("Failed to retrieve feedback count: " + e.getMessage()));
        }
    }

	@PostMapping("/saveCmSheme")
    public ResponseEntity<?> CmSheme(
        @RequestParam int categoryId,
        @RequestParam String BreakFastQualityQa,
        @RequestParam String BreakFastQualityAns,
        @RequestParam String CMBFhelpersPresentSchoolQa,
        @RequestParam String CMBFhelpersPresentSchoolAns,
        @RequestParam String DinningAreaKeptQa,
        @RequestParam String DinningAreaKeptAns,
        @RequestParam String IsschoolhygienicallyQa,
        @RequestParam String IsschoolhygienicallyAns,
        @RequestParam(required = false) MultipartFile feedPhoto,
        @RequestParam(required = false) int school_details_id,
        @RequestParam String zone,
        @RequestParam String division,
        @RequestParam Double latitude,
        @RequestParam double longitude,
        @RequestParam String user_id,
        @RequestParam (required = false) String question_id
    		) {

        try {
            // Call the service method to save feedback
            schoolUserActivity.saveCmSheme(
                categoryId,
                BreakFastQualityQa, BreakFastQualityAns,
                CMBFhelpersPresentSchoolQa, CMBFhelpersPresentSchoolAns,
                DinningAreaKeptQa, DinningAreaKeptAns,
                IsschoolhygienicallyQa, IsschoolhygienicallyAns,
                feedPhoto, school_details_id, zone, division,latitude,longitude,user_id,question_id
            );

            return new ResponseEntity<>("Feedback saved successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to save feedback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
	
	@PostMapping("/saveClusterKitchen")
    public ResponseEntity<?> ClusterKitchen(
	        @RequestParam int categoryId,
	        @RequestParam String ClusterKitchencleanandneatQa,
	        @RequestParam String ClusterKitchencleanandneatAns,
	        @RequestParam String SampleoffoodmaintainedQa,
	        @RequestParam String SampleoffoodmaintainedAns,
	        @RequestParam String cookingstaffwearingQa,
	        @RequestParam String cookingstaffwearingAns,
	        @RequestParam String fooditemsmatchwiththestockQa ,
	        @RequestParam String fooditemsmatchwiththestockAns,
	        @RequestParam String FooditemsusedofgoodqualityQa  ,
	        @RequestParam String FooditemsusedofgoodqualityAns,
	        @RequestParam(required = false) MultipartFile feedPhoto,
	        @RequestParam(required = false) int school_details_id,
	        @RequestParam String zone,
	        @RequestParam String division,
	        @RequestParam double latitude,
	        @RequestParam double longitude,
	        @RequestParam String user_id,
	        @RequestParam (required = false) String question_id
	    	) {

        try {
            // Call the service method to save feedback
            schoolUserActivity.saveClusterKitchen(
                categoryId,
                ClusterKitchencleanandneatQa, ClusterKitchencleanandneatAns,
                SampleoffoodmaintainedQa, SampleoffoodmaintainedAns,
                cookingstaffwearingQa, cookingstaffwearingAns,
                fooditemsmatchwiththestockQa, fooditemsmatchwiththestockAns,
                FooditemsusedofgoodqualityQa,FooditemsusedofgoodqualityAns,
                feedPhoto, school_details_id, zone, division,latitude,longitude,user_id,question_id
            );

            return new ResponseEntity<>("Feedback saved successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to save feedback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
	
	@PostMapping("/savePlayFields")
    public ResponseEntity<?> PlayFields(
    		@RequestParam int categoryId,
    		@RequestParam String playfieldavailableQa,
    		@RequestParam String playfieldavailableAns,
    		@RequestParam String civilworkQa,
    		@RequestParam String civilworkAns,
	        @RequestParam String electricalworkQa,
	        @RequestParam String electricalworkAns,
	        @RequestParam String sportsmaterialkQa ,
	        @RequestParam String sportsmaterialAns,
	        @RequestParam(required = false) MultipartFile playfieldPhoto,
	        @RequestParam(required = false) MultipartFile civilworkPhoto,
	        @RequestParam(required = false) int school_details_id,
	        @RequestParam String zone,
	        @RequestParam String division,
	        @RequestParam double latitude,
	        @RequestParam double longitude,
	        @RequestParam String user_id,
	        @RequestParam (required = false) String question_id
	        ) {
		
		try {
            // Call the service method to save feedback
			
			
            schoolUserActivity.savePlayFields(
            		categoryId,
            		playfieldavailableQa, playfieldavailableAns,
            		civilworkQa, civilworkAns,
            		electricalworkQa, electricalworkAns,
            		sportsmaterialkQa, sportsmaterialAns,
            		playfieldPhoto, civilworkPhoto,
            		school_details_id, zone, division,latitude,longitude,user_id,question_id
            );

            return new ResponseEntity<>("Feedback saved successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to save feedback: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
	
	//Complaints
	
	//saveComplaintReplay(String complaintId, String user_id, String replay, MultipartFile cPhoto);
	//getComplaints(userId);
	//getComplaintsReplay(comid);
	
	@GetMapping("/getComplaints")
    public ResponseEntity<List<Map<String, Object>>> Complaints(@RequestParam String user_id) {
		List<Map<String, Object>> ComplaintsList = schoolUserActivity.getComplaints(user_id);
        return ResponseEntity.ok(ComplaintsList);
    }
	
	@PostMapping("/saveComplaintReplay")
    public ResponseEntity<?> complaintReplay(
        @RequestParam String complaintId,
        @RequestParam String replay,
        @RequestParam(required = true) MultipartFile file,
        @RequestParam String user_id
) {

        try {
            // Call the service method to save feedback
            schoolUserActivity.saveComplaintReplay(
            	complaintId,user_id,replay,file
            );

            return new ResponseEntity<>("Complaint replay saved successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to save Complaint replay: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
	
	@GetMapping("/getComplaintsReplay")
    public ResponseEntity<List<Map<String, Object>>> ComplaintsReplay(@RequestParam String comid) {
		List<Map<String, Object>> ComplaintReplayList = schoolUserActivity.getComplaintsReplay(comid);
        return ResponseEntity.ok(ComplaintReplayList);
    }
	
}
