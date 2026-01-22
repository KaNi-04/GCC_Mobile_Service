package in.gov.chennaicorporation.mobileservice.gccsos.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccsos.service.UserActivity;

@RequestMapping("/gccofficialapp/api/gccsos/user/")
@RestController("gccofficialappUserSOSRest")
public class APIUserController {
	@Autowired
	private UserActivity userActivity;
	
	@PostMapping(value="/saveRequest")
	public List<?> saveRequest(
			@RequestParam(value="contact_name", required = false) String contact_name,
			@RequestParam(value="contact_number", required = false) String contact_number,
			@RequestParam(value="latitude", required = false) String latitude,
			@RequestParam(value="longitude", required = false) String longitude,
			@RequestParam(value="zone", required = false) String zone,
			@RequestParam(value="ward", required = false) String ward,
			@RequestParam(value="streetid", required = false) String streetid,
			@RequestParam(value="streetname", required = false) String streetname,
			@RequestParam(value="location_details", required = false) String location_details,
			@RequestParam(value="request_type", required = false) String request_type,
			@RequestParam(value="no_of_count", required = false) String no_of_count,
			@RequestParam(value="if_any", required = false) String if_any,
			@RequestParam(value="land_mark", required = false) String land_mark,
			@RequestParam(value="remarks", required = false) String remarks,
			@RequestParam(value="loginId", required = false) String loginId,
			@RequestParam(value="mode", required = false) String mode
			){
		return userActivity.saveRequest(contact_name, contact_number, 
				latitude, longitude, zone, ward, streetid, streetname, 
				location_details, request_type, no_of_count, if_any, 
				land_mark, remarks, loginId,mode);
	}
	
	@GetMapping(value="/getMyRequest")
	public List<?> getMyRequest(
			@RequestParam(value="loginId", required = true) String loginId,
			@RequestParam(value="status", required = true) String status
			){
		return userActivity.getMyRequest(loginId,status);
	}
	
	@GetMapping(value="/getRequestDataById")
	public List<?> getRequestDataById(
			@RequestParam(value="rescue_id", required = true) String rescue_id
			){
		return userActivity.getRequestDataById(rescue_id);
	}
	
	@GetMapping(value="/getAlert")
	public List<?> getAlert(){
		return userActivity.getAlert();
	}
}
