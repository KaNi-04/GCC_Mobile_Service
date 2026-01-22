package in.gov.chennaicorporation.mobileservice.abandonedVehicle.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.abandonedVehicle.service.VehicleService;

@RequestMapping("/gccofficialapp/api/abandonedvehicle")
@RestController("gccofficialappabandonedvehicle")
public class VehicleController {
	
	@Autowired
	VehicleService vehicleService;
	
	@PostMapping("/saveIdentfiedVehicle")
 	public List<?> saveIdentfiedVehicle(
             @RequestParam(value = "zone", required = true) String zone,
             @RequestParam(value = "ward", required = true) String ward,
             @RequestParam(value = "street_name", required = false) String street_name,
             @RequestParam(value = "street_id", required = false) String street_id,
             @RequestParam(value = "latitude", required = true) String latitude,
             @RequestParam(value = "longitude", required = true) String longitude,
             @RequestParam(value = "loginId", required = true) String loginId,
             @RequestParam(value = "frontFile", required = true) MultipartFile frontFile,
             @RequestParam(value = "backFile", required = true) MultipartFile backFile,
             @RequestParam(value = "sideFile", required = true) MultipartFile sideFile
     ) {
 		return vehicleService.saveIdentfiedVehicle(
                 zone,
                 ward,
                 street_name,
                 street_id,
                 latitude,
                 longitude,
                 loginId,
                 frontFile,
                 backFile,
                 sideFile
         );
    }
	
	@GetMapping(value="/getRemovePendingList")
	public List<?> getRemovePendingList(
			@RequestParam(value="loginId", required = true) String loginId
			){
		return vehicleService.getRemovePendingList(loginId);
	}
	
	@PostMapping("/saveRemovedVehicle")
 	public List<?> saveRemovedVehicle(
 			 @RequestParam(value = "vid", required = true) String vid,
             @RequestParam(value = "zone", required = true) String zone,
             @RequestParam(value = "ward", required = true) String ward,
             @RequestParam(value = "street_name", required = false) String street_name,
             @RequestParam(value = "street_id", required = false) String street_id,
             @RequestParam(value = "latitude", required = true) String latitude,
             @RequestParam(value = "longitude", required = true) String longitude,
             @RequestParam(value = "loginId", required = true) String loginId,
             @RequestParam(value = "removeFile", required = true) MultipartFile removeFile
     ) {
 		return vehicleService.saveRemovedVehicle(
 				 vid,
                 zone,
                 ward,
                 street_name,
                 street_id,
                 latitude,
                 longitude,
                 loginId,
                 removeFile
         );
    }
	
	@GetMapping(value="/getYardPendingList")
	public List<?> getYardPendingList(
			@RequestParam(value="loginId", required = true) String loginId
			){
		return vehicleService.getYardPendingList(loginId);
	}
	
	@PostMapping("/saveYardVehicle")
 	public List<?> saveYardVehicle(
 			 @RequestParam(value = "vid", required = true) String vid,
 			 @RequestParam(value = "yardid", required = true) String yardid,
             @RequestParam(value = "zone", required = true) String zone,
             @RequestParam(value = "ward", required = true) String ward,
             @RequestParam(value = "street_name", required = false) String street_name,
             @RequestParam(value = "street_id", required = false) String street_id,
             @RequestParam(value = "latitude", required = true) String latitude,
             @RequestParam(value = "longitude", required = true) String longitude,
             @RequestParam(value = "loginId", required = true) String loginId,
             @RequestParam(value = "removeFile", required = true) MultipartFile removeFile
     ) {
 		return vehicleService.saveYardVehicle(
 				 vid,
 				 yardid,
                 zone,
                 ward,
                 street_name,
                 street_id,
                 latitude,
                 longitude,
                 loginId,
                 removeFile
         );
    }
	
	// Reports
	@GetMapping(value="/getZoneSummary")
	public List<?> getZoneSummary(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate
			){
		return vehicleService.getZoneSummary(fromDate, toDate);
	}
	
	@GetMapping(value="/getWardSummary")
	public List<?> getWardSummary(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="zone", required = true) String zone
			){
		return vehicleService.getWardSummary(fromDate, toDate, zone);
	}
	
	@GetMapping(value="/getVehicleSummary")
	public List<?> getVehicleSummary(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="ward", required = true) String ward
			){
		return vehicleService.getVehicleSummary(fromDate, toDate, ward);
	}
	
	// New
	@GetMapping(value="/getZoneSummary_")
	public List<?> getZoneSummary_(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate
			){
		return vehicleService.getZoneSummary_(fromDate, toDate);
	}
	
	@GetMapping(value="/getWardSummary_")
	public List<?> getWardSummary_(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="zone", required = true) String zone
			){
		return vehicleService.getWardSummary_(fromDate, toDate, zone);
	}
	
	@GetMapping(value="/getVehicleSummary_")
	public List<?> getVehicleSummary_(
			@RequestParam(value="fromDate", required = true) String fromDate,
			@RequestParam(value="toDate", required = true) String toDate,
			@RequestParam(value="ward", required = true) String ward,
			@RequestParam(value="filterType", required = true) String filterType
			){
		// filterType : IDENTIFIED | REMOVED | YARD | REMOVED_PENDING | YARD_PENDING
		return vehicleService.getVehicleSummary_(fromDate, toDate, ward, filterType);
	}

}
