package in.gov.chennaicorporation.mobileservice.gccSpecialVehicle.controller;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccSpecialVehicle.service.VehicleService;

@RequestMapping("/gccofficialapp/api/specialvehicle/")
@RestController("gccofficialappsspecialvehicle")
public class VehicleController {
	
	@Autowired
    private VehicleService vehicleService;

	@GetMapping("/getVehicleList")
	public List<?> getVehicleList(@RequestParam("loginId") String loginId){
		return vehicleService.getVehicleList(loginId);
	}
	
	@PostMapping("/saveActivity")
	public List<?> saveActivity(
            @RequestParam("vehicle_id") String vehicle_id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("video")MultipartFile video,
            @RequestParam("zone") String zone,
            @RequestParam("ward") String ward,
            @RequestParam(value = "street_name", required = false) String street_name,
            @RequestParam(value = "street_id", required = false) String street_id,
            @RequestParam("latitude") String latitude,
            @RequestParam("longitude") String longitude,
            @RequestParam("loginId") String loginId,
            @RequestParam("action") String action,
            @RequestParam(value = "beforeActivityId", required = false) String beforeActivityId,
            @RequestParam(value = "clean_weight", required = false) String clean_weight
            
    ) {
		return vehicleService.saveActivity(
                vehicle_id,
                file,
                video,
                zone,
                ward,
                street_name,
                street_id,
                latitude,
                longitude,
                loginId,
                action,
                beforeActivityId,
                clean_weight
        );
    }
	
	@GetMapping("/getVehicleReport")
	public List<?> getVehicleReport(@RequestParam("loginId") String loginId){
		return vehicleService.getVehicleReport(loginId);
	}
	
	@GetMapping("/getVehicleSummaryReport")
	public List<?> getVehicleSummaryReport(
			@RequestParam("loginId") String loginId,
			@RequestParam("vehicle_id") String vehicle_id
			){
		return vehicleService.getVehicleSummaryReport(loginId,vehicle_id);
	}
}
