package in.gov.chennaicorporation.mobileservice.roadwarregister.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.roadwarregister.service.RoadWarAPIService;

import java.util.List;
import java.util.Map;

@RequestMapping("/gccofficialapp/api/roadwar/")
@RestController("gccofficialappsroadwar")
public class APIController {
	@Autowired
	private RoadWarAPIService apiService;
	
	
	@GetMapping(value="/road-types")
	public List<?> getRoadTypes(){
		return apiService.getRoadTypes();
	}
	
	@GetMapping(value="/lay-types")
	public List<?> getRoadLayTypes(){
		return apiService.getRoadLayTypes();
	}
	
	/**
     * Save Start Street details
     */
    @PostMapping("/street/start")
    public ResponseEntity<Map<String, Object>> saveStartStreet(
            @RequestParam String roadName,
            @RequestParam String roadZone,
            @RequestParam String roadWard,
            @RequestParam String roadId,
            @RequestParam String roadType,
            @RequestParam String manualZone,
            @RequestParam String manualWard,
            @RequestParam String manualroadType,
            @RequestParam String roadLayType,
            @RequestParam(required = false) String lastLayOn,
            @RequestParam String roadLength,
            @RequestParam String carriagewayWidth,
            @RequestParam String walltowallWidth,
            @RequestParam String footpath,
            @RequestParam String median,
            @RequestParam String swd,
            @RequestParam String inby,
            @RequestParam String latitude,
            @RequestParam String longitude,
            @RequestParam String streetboard,
            @RequestParam(required = false) MultipartFile roadImage
    ) {
        Map<String, Object> result = apiService.saveStreetDetails(
                "START", roadName, roadZone, roadWard, roadId, roadType,
                manualZone, manualWard, manualroadType,
                roadLayType, lastLayOn, roadLength, carriagewayWidth,
                walltowallWidth, footpath, median, swd, inby, latitude,
                longitude, streetboard, roadImage, null
        );
        return ResponseEntity.ok(result);
    }
    
    @GetMapping(value="/street/startlist")
	public List<?> getStartRoadList(@RequestParam String inby){
		return apiService.getStartRoadList(inby);
	}

    /**
     * Save End Street details (linked with Start Street)
     */
    @PostMapping("/street/end")
    public ResponseEntity<Map<String, Object>> saveEndStreet(
            @RequestParam String roadName,
            @RequestParam String roadZone,
            @RequestParam String roadWard,
            @RequestParam String roadId,
            @RequestParam String roadType,
            @RequestParam String manualZone,
            @RequestParam String manualWard,
            @RequestParam String manualroadType,
            @RequestParam String roadLayType,
            @RequestParam(required = false) String lastLayOn,
            @RequestParam String roadLength,
            @RequestParam String carriagewayWidth,
            @RequestParam String walltowallWidth,
            @RequestParam String footpath,
            @RequestParam String median,
            @RequestParam String swd,
            @RequestParam String inby,
            @RequestParam String latitude,
            @RequestParam String longitude,
            @RequestParam String streetboard,
            @RequestParam(required = false) MultipartFile roadImage,
            @RequestParam Integer startId   // required for END street
    ) {
        Map<String, Object> result = apiService.saveStreetDetails(
                "END", roadName, roadZone, roadWard, roadId, roadType,
                manualZone, manualWard, manualroadType,
                roadLayType, lastLayOn, roadLength, carriagewayWidth,
                walltowallWidth, footpath, median, swd, inby, latitude,
                longitude, streetboard, roadImage, startId
        );
        return ResponseEntity.ok(result);
    }
    
    
    @GetMapping(value="/getCompletedRoadLists")
	public List<?> getCompletedRoadLists(){
		return apiService.getCompletedRoadLists();
	}
}
