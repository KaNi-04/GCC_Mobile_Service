package in.gov.chennaicorporation.mobileservice.gccsos.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccsos.service.OfficerActivity;

@RequestMapping("/gccofficialapp/api/gccsos/officer/")
@RestController("gccofficialappOffcierSOSRest")
public class APIOfficerController {
	@Autowired
	private OfficerActivity officerActivity;
	
	@GetMapping(value="/getAllRequest")
	public List<?> getAllRequest(
			@RequestParam(value="zone", required = false) String zone,
			@RequestParam(value="ward", required = false) String ward,
			@RequestParam(value="streetid", required = false) String streetid,
			@RequestParam(value="fromdate", required = false) String fromdate,
			@RequestParam(value="todate", required = false) String todate,
			@RequestParam(value="request_type", required = false) String request_type,
			@RequestParam(value="loginId", required = false) String loginId,
			@RequestParam(value="status", required = false) String status
			){
		return officerActivity.getAllRequest(request_type, zone, ward, streetid, fromdate, todate, loginId,status);
	}

	@GetMapping(value="/dashboard")
	public List<?> dashboard(
			@RequestParam(value="request_type", required = false) String request_type,
			@RequestParam(value="loginId", required = false) String loginId
			){
		return officerActivity.dashboard(loginId, request_type);
	}
	
	@GetMapping(value="/dashboardByType")
	public List<?> dashboardByType(
			@RequestParam(value="request_type", required = false) String request_type,
			@RequestParam(value="loginId", required = false) String loginId,
			@RequestParam(value="status", required = false) String status
			){
		return officerActivity.dashboardByType(loginId, request_type, status);
	}
	
	@GetMapping(value="/dashboardByZone")
	public List<?> dashboardByZone(
			@RequestParam(value="request_type", required = false) String request_type,
			@RequestParam(value="loginId", required = false) String loginId,
			@RequestParam(value="status", required = false) String status,
			@RequestParam(value="zone", required = false) String zone,
			@RequestParam(value="ward", required = false) String ward
			){
		return officerActivity.dashboardByZone(loginId, request_type, status, zone ,ward);
	}
	
	@GetMapping(value="/dashboardByWard")
	public List<?> dashboardByWard(
			@RequestParam(value="request_type", required = false) String request_type,
			@RequestParam(value="loginId", required = false) String loginId,
			@RequestParam(value="status", required = false) String status,
			@RequestParam(value="zone", required = false) String zone,
			@RequestParam(value="ward", required = false) String ward
			){
		return officerActivity.dashboardByWard(loginId, request_type, status, zone ,ward);
	}
	
	@GetMapping(value="/dashboardByList")
	public List<?> dashboardByList(
			@RequestParam(value="request_type", required = false) String request_type,
			@RequestParam(value="loginId", required = false) String loginId,
			@RequestParam(value="status", required = false) String status,
			@RequestParam(value="zone", required = false) String zone,
			@RequestParam(value="ward", required = false) String ward
			){
		return officerActivity.dashboardByList(loginId, request_type, status, zone ,ward);
	}
	
	@GetMapping(value="/reportByDate")
	public List<?> reportByDate(
			@RequestParam(value = "fromdate", required = false) String fromdate,
			@RequestParam(value = "todate", required = false) String todate,
			@RequestParam("request_type") String request_type,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streetid", required = false) String streetid,
			@RequestParam(value="loginId", required = false) String loginId,
			@RequestParam(value="status", required = false) String status){
		return officerActivity.reportByDate(fromdate, todate, request_type, zone, ward, streetid,loginId,status);
	}
	
	@PostMapping(value="/saveRequestUpdate")
	public List<?> saveRequestUpdate(
			@RequestParam(value="rescue_id", required = true) String rescue_id,
			@RequestParam(value="latitude", required = false) String latitude,
			@RequestParam(value="longitude", required = false) String longitude,
			@RequestParam(value="zone", required = false) String zone,
			@RequestParam(value="ward", required = false) String ward,
			@RequestParam(value="streetid", required = false) String streetid,
			@RequestParam(value="streetname", required = false) String streetname,
			@RequestParam(value="remarks", required = false) String remarks,
			@RequestParam(value="loginId", required = false) String loginId,
			@RequestParam(value = "file", required = false) MultipartFile file
			){
		return officerActivity.saveRequestUpdate(rescue_id, latitude, longitude, zone, ward, streetid, streetname, remarks, loginId, file);
	}

	@GetMapping(value="/getRequestDataById")
	public List<?> getRequestDataById(
			@RequestParam(value="rescue_id", required = true) String rescue_id
			){
		return officerActivity.getRequestDataById(rescue_id);
	}
	
	@GetMapping(value="/getFloodGrandData")
	public List<?> getFloodGrandData(
			@RequestParam(value="", required = true) String fromDate,
			@RequestParam(value="", required = true) String toDate
			){
		return officerActivity.getFloodGrandData(fromDate,toDate);
	}
	
	@GetMapping(value="/getFloodAbsData")
	public List<?> getFloodAbsData(
			@RequestParam(value="", required = true) String fromDate,
			@RequestParam(value="", required = true) String toDate
			){
		return officerActivity.getFloodAbsData(fromDate,toDate);
	}
	
	@GetMapping(value="/getFloodZoneData")
	public List<?> getFloodZoneData(
			@RequestParam(value="", required = true) String fromDate,
			@RequestParam(value="", required = true) String toDate,
			@RequestParam(value="", required = true) String categorie,
			@RequestParam(value="", required = true) String mode,
			@RequestParam(value="", required = false) String status
			){
		return officerActivity.getFloodZoneData(fromDate,toDate,categorie,mode,status);
	}
	
	@GetMapping(value="/getFloodWardData")
	public List<?> getFloodWardData(
			@RequestParam(value="", required = true) String fromDate,
			@RequestParam(value="", required = true) String toDate,
			@RequestParam(value="", required = true) String categorie,
			@RequestParam(value="", required = true) String mode,
			@RequestParam(value="", required = true) String zone,
			@RequestParam(value="", required = false) String status
			){
		return officerActivity.getFloodWardData(fromDate,toDate,categorie,mode,zone,status);
	}
	
	@GetMapping(value="/getFloodData")
	public List<?> getFloodData(
			@RequestParam(value="", required = true) String fromDate,
			@RequestParam(value="", required = true) String toDate,
			@RequestParam(value="", required = true) String categorie,
			@RequestParam(value="", required = true) String mode,
			@RequestParam(value="", required = true) String zone,
			@RequestParam(value="", required = true) String ward,
			@RequestParam(value="", required = false) String status
			){
		return officerActivity.getFloodData(fromDate,toDate,categorie,mode,zone,ward,status);
	}
}
