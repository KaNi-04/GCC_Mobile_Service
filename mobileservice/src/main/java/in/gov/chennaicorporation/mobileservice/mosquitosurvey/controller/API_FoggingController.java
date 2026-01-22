package in.gov.chennaicorporation.mobileservice.mosquitosurvey.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.mosquitosurvey.service.FoggingService;

@RequestMapping("/gccofficialapp/mosquito/api")
@RestController("apiFoggingRest")
public class API_FoggingController {
	@Autowired
	private FoggingService foggingService;
	
	@GetMapping("/getMaintainByList")
    public List<?> getMaintainByList() {
        return foggingService.getMaintainByList();
    }
	
	@GetMapping("/getCanalsList")
    public List<?> getCanalsList(
    		@RequestParam(value = "maintainedby", required = true) String maintainedby,
    		@RequestParam(value = "zone", required = true) String zone
			) {
        return foggingService.getCanalsList(maintainedby, zone);
    }
	
	@PostMapping(value="/saveCanalFlow") 
	public List<?> saveCanalFlow(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "category", required = true) String cid,
			@RequestParam(value = "typeofsite", required = true) String scid,
			@RequestParam(value = "maintainedby", required = true) String maintainedby,
			@RequestParam(value = "canalid", required = true) String canalid,
			@RequestParam(value = "sprayingmedium", required = true) String q4,
			@RequestParam(value = "sprayingtype", required = true) String q5,
			@RequestParam(value = "file", required = true) MultipartFile file,
			@RequestParam(value = "remarks", required = true) String remarks,
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "address", required = false) String address
			) {
		return foggingService.saveCanalFlow(cid,scid,maintainedby,canalid,q4,q5,file,remarks,cby,zone,ward,latitude,longitude,"pending",address);
	}
	
	@GetMapping("/checkHasCanalPending")
    public List<?> checkHasPending(
    		@RequestParam(value = "loginId", required = true) String cby
			) {
        return foggingService.checkHasCanalPending(cby);
    }
	
	@GetMapping("/getCanalPendingList")
    public List<?> getCanalPendingList(
    		@RequestParam(value = "loginId", required = true) String cby
			) {
        return foggingService.getCanalPendingList(cby);
    }
	
	@GetMapping("/checkCanalPendingUpdateLocation")
    public List<?> checkPendingUpdateLocation(
    		@RequestParam(value = "activityid", required = true) String canalactivityid, 
    		@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude
			) {
        return foggingService.checkCanalPendingUpdateLocation(canalactivityid,latitude,longitude);
    }
	
	@PostMapping(value="/saveCanalData") 
	public List<?> saveCanalFlow(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "activityid", required = true) String canalactivityid,
			@RequestParam(value = "isfinal", required = true) String isfinal,
			@RequestParam(value = "file", required = true) MultipartFile file,
			@RequestParam(value = "remarks", required = true) String remarks,
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude
			) {
		return foggingService.saveCanalActivityData(canalactivityid,isfinal,file,remarks,cby,zone,ward,latitude,longitude);
	}
	
	
	// ************************************************************************************************************************* //
	// For SWD Activity
	
	@PostMapping(value="/saveSWDFlow") 
	public List<?> saveSWDFlow(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "category", required = true) String cid,
			@RequestParam(value = "typeofsite", required = true) String scid,
			@RequestParam(value = "streetname", required = true) String streetname,
			@RequestParam(value = "streetid", required = true) String streetid,
			@RequestParam(value = "breedfound", required = true) String q2,
			@RequestParam(value = "sprayingmedium", required = true) String q4,
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "remarks", required = true) String remarks,
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "address", required = false) String address
			) {
		return foggingService.saveSWDFlow(cid,scid,streetname,streetid,q2,q4,file,remarks,cby,zone,ward,latitude,longitude,"pending",address);
	}
	
	@GetMapping("/checkHasSWDPending")
    public List<?> checkHasSWDPending(
    		@RequestParam(value = "loginId", required = true) String cby
			) {
        return foggingService.checkHasSWDPending(cby);
    }
	
	@GetMapping("/getSWDPendingList")
    public List<?> getSWDPendingList(
    		@RequestParam(value = "loginId", required = true) String cby
			) {
        return foggingService.getSWDPendingList(cby);
    }
	
	@GetMapping("/checkSWDPendingUpdateLocation")
    public List<?> checkSWDPendingUpdateLocation(
    		@RequestParam(value = "activityid", required = true) String swdactivityid, 
    		@RequestParam(value = "streetid", required = true) String streetid
			) {
        return foggingService.checkSWDPendingUpdateLocation(swdactivityid,streetid);
    }
	
	@PostMapping(value="/updateSWDPendingStatus")
	public List<?> updatePendingStatus(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "activityid", required = true) String swdactivityid,
			@RequestParam(value = "file", required = true) MultipartFile file,
			@RequestParam(value = "streetname", required = true) String streetname,
			@RequestParam(value = "streetid", required = true) String streetid,
			@RequestParam(value = "q2", required = true) String q2,
			@RequestParam(value = "q4", required = true) String q4,
			@RequestParam(value = "remarks", required = true) String remarks,
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "did", required = true) String did
			) {
		return foggingService.updateSWDPendingStatus(swdactivityid, file, streetname, streetid, 
				q2, q4, remarks, cby, zone, ward, latitude, longitude, "close", did);
	}
	
	@PostMapping(value="/saveStreetData") 
	public List<?> saveStreetData(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "category", required = true) String cid,
			@RequestParam(value = "typeofsite", required = true) String scid,
			@RequestParam(value = "streetid", required = true) String streetid,
			@RequestParam(value = "streetname", required = true) String streetname,
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "address", required = false) String address
			) {
		return foggingService.saveStreetData(cid, scid, streetid, streetname, file, remarks, cby, zone, ward, latitude, longitude, "close",address);
	}
	
	@PostMapping(value="/saveVacantLandData") 
	public List<?> saveVacantLandData(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "category", required = true) String cid,
			@RequestParam(value = "typeofsite", required = true) String scid,
			@RequestParam(value = "name", required = true) String name,
			@RequestParam(value = "breedfound", required = true) String q2,
			@RequestParam(value = "action", required = false) String q3,
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "address", required = false) String address
			) {
		return foggingService.saveVacantLandData(cid, scid, name, q2, q3, file, remarks, cby, zone, ward, latitude, longitude, "close",address);
	}
	
	// SAMPLE COLLECTION 
	@PostMapping(value="/saveLarvalAdultCollectionFlow") 
	public List<?> saveLarvalAdultCollectionFlow(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "category", required = true) String cid,
			@RequestParam(value = "maintainedby", required = true) String maintainedby,
			@RequestParam(value = "canalid", required = true) String canalid,
			@RequestParam(value = "breedfound", required = true) String q2,
			@RequestParam(value = "lavasample", required = false) String q6,
			@RequestParam(value = "action", required = false) String q7,
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude,
			@RequestParam(value = "address", required = false) String address
			) {
		return foggingService.saveLarvalAdultCollectionFlow(cid,maintainedby,canalid,q2,q6,q7,
				file,remarks,cby,zone,ward,latitude,longitude,"close",address);
	}
	
}
