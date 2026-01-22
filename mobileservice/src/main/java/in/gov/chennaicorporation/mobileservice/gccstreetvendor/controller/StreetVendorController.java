package in.gov.chennaicorporation.mobileservice.gccstreetvendor.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.chennaicorporation.mobileservice.configuration.AppConfig;
import in.gov.chennaicorporation.mobileservice.gccstreetvendor.service.StreetVendorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.bytedeco.opencv.presets.opencv_core;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequestMapping("/gccofficialapp/api/streetvendor")
@RestController("gccofficialappstreetvendor")

public class StreetVendorController {

    @Autowired
    StreetVendorService streetVendorService;

    @Autowired
    AppConfig  appConfig;

    //To fetch all social category
    @GetMapping("/social-categories")
    public List<Map<String,Object>> getSocialCategories() {
        return streetVendorService.getAllSocialCategories();
    }

    //To fetch all vending type
    @GetMapping("/vending-type")
    public List<Map<String,Object>> getVendingType() {
        return streetVendorService.getAllVendingType();
    }
    
    //To fetch all vending type
    @GetMapping("/vending-type-sub")
    public List<Map<String,Object>> getVendingTypeSub(@RequestParam("typeid") String typeid) {
        return streetVendorService.getAllVendingTypeSub(typeid);
    }
    
    //To fetch all vending category
    @GetMapping("/vending-categories")
    public List<Map<String,Object>> getVendingCategories() {
        return streetVendorService.getAllVendingCategories();
    }

    // To save Vendor details
    @PostMapping("/save-street-vendor")
    public ResponseEntity<String> saveStreetVendor(
            @RequestParam("name") String name,
            @RequestParam("fh_name") String fhname,
            @RequestParam("gender") String gender,
            @RequestParam("dob") String dob,
            @RequestParam("social_category") String socialCategory,
            @RequestParam("diff_abled") boolean diffAbled,
            @RequestParam("mob_no") String mobileNo,
            @RequestParam("aadhar_no") String aadharNo,
            @RequestParam("present_address") String presentAddress,
            @RequestParam("present_district") String presentDistrict,
            @RequestParam("present_pincode") String presentPincode,
            @RequestParam("vending_address") String vendingAddress,
            @RequestParam("vending_district") String vendingDistrict,
            @RequestParam("vending_pincode") String vendingPincode,
            @RequestParam("vending_category") String vendingCategory,
            @RequestParam("vending_space") String vendingSpace,
            @RequestParam("bank_acc_status") boolean bankAccStatus,
            @RequestParam("zone") String zone,
            @RequestParam("ward") String ward,
            @RequestParam("pm_svanidhi_loan") boolean pmSvanidhiLoan,
            @RequestParam(required = false, name = "bank_passbook") MultipartFile bankPassbook,
            @RequestParam("ration_card") MultipartFile rationCard,
            @RequestParam("education_status") int education_status,
            @RequestParam("marital_status") int marital_status,
            @RequestParam("no_of_fam_mem") int no_of_fam_mem,
            @RequestParam("fam_mem_invol_str_ven") boolean are_fam_mem_invol_str_ven,
            @RequestParam("street_vendor_photo") MultipartFile streetVendorPhoto,
            @RequestParam("aadhar_front_photo") MultipartFile aadharFrontPhoto,
            @RequestParam("aadhar_back_photo") MultipartFile aadharBackPhoto) {

        // File upload paths
        String bankPassbookPath = (bankPassbook != null) ? 
            streetVendorService.fileUpload(bankPassbook, "street_vendor_bank") : "";

        String rationCardPath = (rationCard != null) ? 
            streetVendorService.fileUpload(rationCard, "street_vendor_ration") : "";

        String streetVendorPhotoPath = (streetVendorPhoto != null) ? 
            streetVendorService.fileUpload(streetVendorPhoto, "street_vendor_photo") : "";

        String aadharFrontPhotoPath = (aadharFrontPhoto != null) ? 
            streetVendorService.fileUpload(aadharFrontPhoto, "aadhar_front") : "";

        String aadharBackPhotoPath = (aadharBackPhoto != null) ? 
            streetVendorService.fileUpload(aadharBackPhoto, "aadhar_back") : "";

        // Save to DB
        String saveStatus = streetVendorService.saveStreetVendorDetails(
            name, fhname, gender, dob, socialCategory, diffAbled, mobileNo, aadharNo,
            presentAddress, presentDistrict, presentPincode,
            vendingAddress, vendingDistrict, vendingPincode,
            vendingCategory, vendingSpace, bankAccStatus, zone, ward, pmSvanidhiLoan,
            bankPassbookPath, rationCardPath, streetVendorPhotoPath,
            aadharFrontPhotoPath, aadharBackPhotoPath,
            education_status,marital_status,no_of_fam_mem,are_fam_mem_invol_str_ven
        );

        if (saveStatus == null || saveStatus.isEmpty() || saveStatus.equals("error") || saveStatus.equals("mobile_error")) {
        	String error_desc ="Failed to save the details";
        	if(saveStatus.equals("mobile_error")) {
        		error_desc ="Duplicate mobile number. This mobile number is already registered.";
        	}
            return ResponseEntity.ok(Map.of(
                "status", false,
                "message", "failed",
                "description", error_desc
            ).toString());
        }

        return ResponseEntity.ok(Map.of(
            "status", true,
            "message", "success",
            "description", "Details were saved successfully"
        ).toString());
    }

    @GetMapping(value = "/sendOTP")
    @ResponseBody
    public Map<String, Object> sendOtp(HttpServletRequest request, @RequestParam String mobile) {
        Map<String, Object> response = new HashMap<>();
        String MobileNo = mobile;
        Random rand = new Random();
        int otp = rand.nextInt(9000) + 1000;;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy%20HH:mm");
        LocalDateTime now = LocalDateTime.now();

        String generatedToken = "Welcome to GCC! Your OTP for GCC Street Vendor Application is " + otp;
        String key = "pfTEYN6H";
        String urlString = appConfig.otpurl + "from=GCCCRP&key=" + key + "&sender=GCCCRP&to=" + MobileNo +
                "&body=" + generatedToken + "&entityid=1401572690000011081&templateid=1407174410515874750";

        try {
            RestTemplate restTemplate = new RestTemplate();
            String apiResponse = restTemplate.getForObject(urlString, String.class);

            // Parse the response using ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(apiResponse);

            int statusCode = jsonNode.get("status").asInt();
            String description = jsonNode.get("description").asText();

            if (statusCode == 100) {
                HttpSession session = request.getSession();
                session.setAttribute("otp", otp);
                response.put("status", true);
            } else {
                response.put("status", false);
            }

            response.put("description", description);
            response.put("otp", otp); // Optional: for debugging only (don't include in production)
        } catch (Exception e) {
            response.put("status", false);
            response.put("description", "Error sending OTP: " + e.getMessage());
        }

        return response;
    }

    @GetMapping(value = "/verifyOTP")
    @ResponseBody
    public Map<String, Object> verifyOtp(HttpServletRequest request, @RequestParam String otp) {
        Map<String, Object> response = new HashMap<>();
        HttpSession session = request.getSession();

        try {
            Integer session_otp = (Integer) session.getAttribute("otp");
            Integer entered_otp = Integer.parseInt(otp);

            //System.out.println("session_otp---:" + session_otp);
            //System.out.println("entered_otp---:" + entered_otp);

            if (session_otp != null && session_otp.equals(entered_otp)) {
                session.setAttribute("verifiedotp", "success");
                response.put("status", true);
                response.put("message", "Success");
            } else {
                session.setAttribute("verifiedotp", "error");
                response.put("status", false);
                response.put("message", "Incorrect OTP");
            }
        } catch (NumberFormatException e) {
            //System.out.println("Invalid OTP format: " + otp);
            response.put("status", false);
            response.put("message", "Invalid OTP format");
        } catch (Exception e) {
            //System.out.println("Error verifying OTP: " + e.getMessage());
            response.put("status", false);
            response.put("message", "Error verifying OTP");
        }

        return response;
    }
    
    @GetMapping("/getvendorlist")
    public List<Map<String, Object>> getVendorList(
    		@RequestParam(value = "loginId", required = false) String loginid
    		) {
        return streetVendorService.getAllVendorDetails_l1(loginid); 
    }

    @GetMapping("/getvendingtimes")
    public List<Map<String, Object>> getVendingTimes() {
        return streetVendorService.getAllVendingTimes();
    }

    @GetMapping("/getmaritalstatus")
    public List<Map<String, Object>> getMaritalStatus() {
        return streetVendorService.getAllMaritalStatus();
    }

    @GetMapping("/gettransactionmodes")
    public List<Map<String, Object>> getTransactionMode() {
        return streetVendorService.getAllTransactionModes();
    }

    @GetMapping("/geteducationstatus")
    public List<Map<String, Object>> getEducationSatus() {
        return streetVendorService.getAllEducationSatus();
    }

    // save the entered li details
    @PostMapping("/saveFirstApproval")
    public ResponseEntity<String> saveFirstApproval(
    		@RequestParam(required = true, name = "loginId") String loginid,
    		@RequestParam(required = true, name = "vdid") String vdid,
			@RequestParam(required = true, name = "vendor_type") String vendor_type,
			@RequestParam(required = true, name = "vendor_sub_type") String vendor_sub_type,
			@RequestParam(required = true, name = "latitude") String latitude,
	        @RequestParam(required = true, name = "longitude") String longitude,
	        @RequestParam(required = false, name = "vendor_id_card") boolean vendor_id_card,
	        @RequestParam(required = false, name = "vending_nature") String vending_nature,
	        @RequestParam(required = false, name = "vending_time") String vending_time,
	        //@RequestParam(required = false, name = "education_status") String education_status,
	        //@RequestParam(required = false, name = "marital_status") String marital_status,
	        //@RequestParam(required = false, name = "no_of_family_mem") String no_of_family_mem,
	        //@RequestParam(required = false, name = "fam_mem_street_vendor") boolean fam_mem_street_vendor,
	        @RequestParam(required = false, name = "transaction_mode") String transaction_mode,
	        @RequestParam(required = false, name = "svanidhi_loan_amount") String svanidhi_loan_amount,
	        @RequestParam(required = false, name = "vendor_space_photo") MultipartFile vendor_space_photo,
	        @RequestParam(required = true, name = "status") String status,
	        @RequestParam(required = true, name = "remarks") String remarks){
    	

        String vendor_space_path = "";
        if ( vendor_space_photo != null) {
            vendor_space_path = streetVendorService.fileUpload(vendor_space_photo, "vendor_space_photo");
        }
        //String status = "Approved";

        String saveStatus = streetVendorService.saveFirstApproval(loginid,vdid,vendor_type, vendor_sub_type, latitude, longitude, vendor_id_card, vending_nature,
                vending_time, transaction_mode,
                svanidhi_loan_amount, status, remarks, vendor_space_path);

        if (saveStatus ==null || saveStatus.isEmpty()) {
            return ResponseEntity.ok(Map
                    .of("status", false, "message", "failed", "description", "Failed to save the details").toString());
        }

        return ResponseEntity.ok(Map
                .of("status", true, "message", "success", "description", "Details were saved successfully").toString());

    }
    
    @GetMapping("/getAllVendorDetails_l2")
    public List<Map<String, Object>> getAllVendorDetails_l2(
    		@RequestParam(value = "loginId", required = false) String loginid
    		) {
        return streetVendorService.getAllVendorDetails_l2(loginid); 
    }
    
    // save the entered aro details
    @PostMapping("/saveSecondApproval")
    public ResponseEntity<String> saveSecondApproval(
    		@RequestParam(required = true, name = "loginId") String loginid,
    		@RequestParam(required = true, name = "vdid") String vdid,
	        @RequestParam(required = true, name = "status") String status,
	        @RequestParam(required = true, name = "remarks") String remarks){
    	

        String saveStatus = streetVendorService.saveSecondApproval(loginid,vdid, status, remarks);

        if (saveStatus ==null || saveStatus.isEmpty()) {
            return ResponseEntity.ok(Map
                    .of("status", false, "message", "failed", "description", "Failed to save the details").toString());
        }

        return ResponseEntity.ok(Map
                .of("status", true, "message", "success", "description", "Details were saved successfully").toString());

    }
    
    @GetMapping("/getAllVendorDetails_l3")
    public List<Map<String, Object>> getAllVendorDetails_l3(
    		@RequestParam(value = "loginId", required = false) String loginid
    		) {
        return streetVendorService.getAllVendorDetails_l3(loginid); 
    }
    
    // save the entered exeeng details
    @PostMapping("/savethirdApproval")
    public ResponseEntity<String> savethirdApproval(
    		@RequestParam(required = true, name = "loginId") String loginid,
    		@RequestParam(required = true, name = "vdid") String vdid,
	        @RequestParam(required = true, name = "status") String status,
	        @RequestParam(required = true, name = "remarks") String remarks){
    	

        String saveStatus = streetVendorService.savethirdApproval(loginid,vdid, status, remarks);

        if (saveStatus ==null || saveStatus.isEmpty()) {
            return ResponseEntity.ok(Map
                    .of("status", false, "message", "failed", "description", "Failed to save the details").toString());
        }

        return ResponseEntity.ok(Map
                .of("status", true, "message", "success", "description", "Details were saved successfully").toString());

    }
}
