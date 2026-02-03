package in.gov.chennaicorporation.mobileservice.foodDistribution.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.foodDistribution.service.DriverService;

@RequestMapping("/gccofficialapp/api/fooddistribution/driver")
@RestController("gccofficialappsfooddistributiondriver")
public class DriverController {
 @Autowired
 private DriverService driverService;
 
 	@GetMapping({"/versionCheck"}) // Template
	public List <?> versionCheck() {
		return driverService.versionCheck();
	}
 
 	@GetMapping(value="login")
	public List login(@RequestParam("username") String username, @RequestParam("password") String Password, @RequestParam("usertype") String usertype) {
		return driverService.getloginList(username, Password, usertype);
	}
 	// ---------------------------------------------------------
    //️⃣ GET LIVE TRACK
    // ---------------------------------------------------------
    @GetMapping("/getDeliveryLocation")
    public List<?> getDeliveryLocation(
            @RequestParam String loginid
    ) {
        return driverService.getDeliveryLocation(loginid);
    }
    
    // ---------------------------------------------------------
    //  GET LIVE TRACK OF Driver BY SI Login
    // ---------------------------------------------------------
    @GetMapping("/getDriverStatus")
    public List<?> getDriverStatus(
            @RequestParam String siloginid
    ) {
        return driverService.getDriverStatus(siloginid);
    }
    
    // ---------------------------------------------------------
    // 1️⃣ GET LIVE TRACK BY SI Login
    // ---------------------------------------------------------
    @GetMapping("/getLiveTrack")
    public List<?> getLiveTrack(
            @RequestParam String siloginid,
            @RequestParam String date
    ) {
        return driverService.getLiveTrack(siloginid, date);
    }


    // ---------------------------------------------------------
    // 2️⃣ GET DRIVER LAST START/STOP STATUS
    // ---------------------------------------------------------
    @GetMapping("/getDriverLastStartStop")
    public List<?> getDriverLastStartStop(
            @RequestParam String loginid
    ) {
        return driverService.getDriverLastStartStop(loginid);
    }


    // ---------------------------------------------------------
    // 3️⃣ SAVE DRIVER START / STOP WITH IMAGE
    // ---------------------------------------------------------
    @PostMapping(value = "/saveStartStop", consumes = {"multipart/form-data"})
    public List<?> saveStartStop(
            @RequestParam String loginid,
            @RequestParam String latitude,
            @RequestParam String longitude,
            @RequestParam String action,
            @RequestParam String startid,
            @RequestParam String driver_live_mobile,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String driver_name
    ) {
        return driverService.saveStartStop(
                loginid, latitude, longitude,
                action, startid, driver_live_mobile, image, address, driver_name
        );
    }
    
    // ---------------------------------------------------------
    // 4️⃣ SAVE LIVE LOCATION POINT
    // ---------------------------------------------------------
    @PostMapping("/saveLiveLocation")
    public List<?> saveLiveLocation(
            @RequestParam String loginid,
            @RequestParam String latitude,
            @RequestParam String longitude,
            @RequestParam(required = false) String address
    ) {
        return driverService.saveLiveLocation(loginid, latitude, longitude, address);
    }
    
    // Vendor
    @GetMapping("/getMenuDetails")
    public List<?> getMenuDetails(
    		@RequestParam String loginid
    ) {
        return driverService.getMenuDetails(loginid);
    }
    
    @GetMapping("/getPendingDetails")
    public List<?> getPendingDetails(
    		@RequestParam String loginid,
            @RequestParam String shiftid,
            @RequestParam String date,
            @RequestParam String hub_id
    ) {
        return driverService.getPendingDetails(loginid, shiftid, date, hub_id);
    }
    
    @GetMapping("/getRequestUpdated")
    public List<?> getRequestUpdated(
    		@RequestParam String loginid,
            @RequestParam String shiftid,
            @RequestParam String date
    ) {
        return driverService.getRequestUpdated(loginid, shiftid, date);
    }
    
    @PostMapping("/saveRequestUpadte")
    public List<?> saveRequestUpadte(
            @RequestParam String loginid,
            @RequestParam String shiftid,
            @RequestParam String total_food_request_recived,
            @RequestParam String total_food_transported,
            @RequestParam String foodid
    ) {
        return driverService.saveRequestUpadte(loginid, shiftid, total_food_request_recived, total_food_transported, foodid);
    }
}
