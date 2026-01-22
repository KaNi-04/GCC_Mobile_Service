package in.gov.chennaicorporation.mobileservice.manhole.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.manhole.service.ManHoleActivity;

@RequestMapping("/gccofficialapp/api/manhole/")
@RestController("gccofficialappsmanhole")
public class ManHoleController {
	@Autowired
    private ManHoleActivity manHoleActivity;
	
	@GetMapping(value="/duplicateDrainCheck")
	public String duplicateDrainCheck(
			@RequestParam(value="street_id", required = true) String street_id,
			@RequestParam(value="drain_side", required = true) String drain_side
			){
		return manHoleActivity.duplicateDrainCheck(street_id,drain_side);
	}
	
	@PostMapping("/saveDrain")
	public List<?> saveDrain(
			@RequestParam("drain_side") String drain_side,
			@RequestParam("drain_type") String drain_type,
			@RequestParam("drain_length") String drain_length,
			@RequestParam("drain_width") String drain_width,
			@RequestParam("drain_depth") String drain_depth,
			@RequestParam(value="manhole_count", required = false) String manhole_count,
			@RequestParam("zone") String zone,
			@RequestParam("ward") String ward,
			@RequestParam("street_name") String street_name,
			@RequestParam("street_id") String street_id,
			@RequestParam("road_type") String road_type,
			@RequestParam("latitude") String latitude,
			@RequestParam("longitude") String longitude,
			@RequestParam("loginId") String inby,
			@RequestParam("file") MultipartFile file
			)
	{
		return manHoleActivity.saveDrain(
				drain_side,
				drain_type,
				drain_length,
				drain_width,
				drain_depth,
				manhole_count,
				zone,
				ward,
				street_name,
				street_id,
				road_type,
				latitude,
				longitude,
				inby,
				file
        );
    }
	
	@GetMapping(value="/getDrainList")
	public List<?> getDrainList(@RequestParam(value="loginId", required = true) String cby){
		return manHoleActivity.getDrainList(cby);
	}
	
	@PostMapping("/saveManHole")
	public List<?> saveManHole(
			@RequestParam("drain_id") String drain_id,
			@RequestParam("zone") String zone,
			@RequestParam("ward") String ward,
			@RequestParam("street_name") String street_name,
			@RequestParam("street_id") String street_id,
			@RequestParam("latitude") String latitude,
			@RequestParam("longitude") String longitude,
			@RequestParam("loginId") String inby
	)
	{
		return manHoleActivity.saveManHole(
				drain_id,
				zone,
				ward,
				street_name,
				street_id,
				latitude,
				longitude,
				inby
        );
    }
	
	@GetMapping(value="/getManHoleList")
	public List<?> getManHoleList(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value="drain_id", required = true) String drain_id
			){
		return manHoleActivity.getManHoleList(cby, drain_id);
	}
	
	@GetMapping(value="/manHoleLocationCheck")
	public String manHoleLocationCheck(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam("manhole_id") String manhole_id,
			@RequestParam("latitude") String latitude,
            @RequestParam("longitude") String longitude
			){
		return manHoleActivity.manHoleLocationCheck(cby, manhole_id,latitude, longitude);
	}
	
	@PostMapping("/saveActivity")
	public List<?> saveActivity(
            @RequestParam("manhole_id") String manhole_id,
            @RequestParam("drain_id") String drain_id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("zone") String zone,
            @RequestParam("ward") String ward,
            @RequestParam("street_name") String street_name,
            @RequestParam("street_id") String street_id,
            @RequestParam("latitude") String latitude,
            @RequestParam("longitude") String longitude,
            @RequestParam("loginId") String loginId,
            @RequestParam("action") String action,
            @RequestParam(value = "beforeActivityId", required = false) String beforeActivityId,
            @RequestParam(value = "clean_type", required = false) String clean_type,
            @RequestParam(value = "case_id", required = true) String case_id
            
    ) {
		return manHoleActivity.saveActivity(
                manhole_id,
                drain_id,
                file,
                zone,
                ward,
                street_name,
                street_id,
                latitude,
                longitude,
                loginId,
                action,
                beforeActivityId,
                clean_type,
                case_id
        );
    }
	
	@GetMapping(value="/getZoneReport")
	public List<?> getZoneReport(){
		return manHoleActivity.getZoneReport();
	}
	
	@GetMapping(value="/getWardReport")
	public List<?> getWardReport(
			@RequestParam(value="zone", required = true) String zone
			){
		return manHoleActivity.getWardReport(zone);
	}
	@GetMapping(value="/getDrainReport")
	public List<?> getDrainReport(
			@RequestParam(value="ward", required = true) String ward
			){
		return manHoleActivity.getDrainReport(ward);
	}
}
