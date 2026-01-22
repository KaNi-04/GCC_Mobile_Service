package in.gov.chennaicorporation.mobileservice.swmbwg.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.swmbwg.service.activity;

@RequestMapping("/gccofficialapp/api/swmbwg/")
@RestController("gccofficialappswmbwg")
public class APIController {
	@Autowired
    private activity activity;
	
	@GetMapping(value="/getClass")
	public List<?> getAllClass(){
		return activity.getAllClass();
	}
	
	@GetMapping(value="/getBWGTypes")
	public List<?> getBWGTypes(){
		return activity.getBWGTypes();
	}
	
	@GetMapping(value="/getServiceProviderList")
	public List<?> getServiceProviderList(){
		return activity.getServiceProviderList();
	}
	
	@PostMapping(value="/saveRequest")
	public List<?> saveRequest(
			@RequestParam(value="latitude", required = false) String latitude,
			@RequestParam(value="longitude", required = false) String longitude,
			@RequestParam(value="zone", required = false) String zone,
			@RequestParam(value="ward", required = false) String ward,
			@RequestParam(value="streetid", required = false) String streetid,
			@RequestParam(value="streetname", required = false) String streetname,
			@RequestParam(value="location", required = false) String location,
			@RequestParam(value="classid", required = false) String classid,
			@RequestParam(value="typeid", required = false) String typeid,
			@RequestParam(value="qtyofwaste", required = false) String qtyofwaste,
			@RequestParam(value="onsiteprocessing", required = false) String onsiteprocessing,
			@RequestParam(value="serviceprovider", required = false) String serviceprovider,
			@RequestParam(value="serviceproviderid", required = false) String serviceproviderid,
			@RequestParam(value="loginId", required = false) String cby,
			@RequestParam(value = "image", required = false) MultipartFile filedata,
			@RequestParam(value = "file", required = false) MultipartFile filedata2
			){
		return activity.saveRequest(latitude, longitude, 
				zone, ward, streetid, streetname, location, classid, 
				typeid, qtyofwaste, onsiteprocessing, serviceprovider, 
				serviceproviderid, cby, filedata, filedata2);
	}
	
	@GetMapping(value="/getServiceProviderPendingList")
	public List<?> getServiceProviderPendingList(@RequestParam(value="loginId", required = false) String cby){
		return activity.getServiceProviderPendingList(cby);
	}
	
	@PostMapping(value="/updateServiceProviderImg")
	public List<?> updateServiceProviderImg(
			@RequestParam(value="did", required = true) String did,
			@RequestParam(value = "file", required = true) MultipartFile filedata2
			){
		return activity.updateServiceProviderImg(did,filedata2);
	}
	
	@GetMapping(value="/getZoneWiseCount")
	public List<?> getZoneWiseCount(){
		return activity.getZoneWiseCount();
	}
	
	@GetMapping(value="/getWardWiseCount")
	public List<?> getWardWiseCount(@RequestParam(value="zone", required = true) String zone){
		return activity.getWardWiseCount(zone);
	}
	
	@GetMapping(value="/getReport")
	public List<?> getReport(@RequestParam(value="ward", required = true) String ward){
		return activity.getReport(ward);
	}

}
