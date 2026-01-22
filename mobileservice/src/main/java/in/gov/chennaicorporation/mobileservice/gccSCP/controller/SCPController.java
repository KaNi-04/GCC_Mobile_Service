package in.gov.chennaicorporation.mobileservice.gccSCP.controller;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccSCP.service.SCPService;

@RequestMapping("/gccofficialapp/api/scp/")
@RestController("gccofficialappsscp")
public class SCPController {
	@Autowired
    private SCPService scpService;
	
	@GetMapping(value="/getSCPListForMap")
	public List<?> getSCPListForMap(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value="streetid", required = true) String streetid
			){
		return scpService.getSCPListForMap(cby,streetid);
	}
	
	@PostMapping("/saveActivity")
	public List<?> saveActivity(
            @RequestParam(value = "asset_id", required = true) String asset_id,
            @RequestParam(value = "grating", required = true) String grating,
            @RequestParam(value = "file", required = true) MultipartFile file,
            @RequestParam(value = "zone", required = true) String zone,
            @RequestParam(value = "ward", required = true) String ward,
            @RequestParam(value = "street_name", required = false) String street_name,
            @RequestParam(value = "street_id", required = false) String street_id,
            @RequestParam(value = "latitude", required = true) String latitude,
            @RequestParam(value = "longitude", required = true) String longitude,
            @RequestParam(value = "loginId", required = true) String loginId,
            @RequestParam(value = "road_type", required = true) String road_type
    ) {
		return scpService.saveActivity(
				asset_id,
				grating,
				file,
                zone,
                ward,
                street_name,
                street_id,
                latitude,
                longitude,
                loginId,
                road_type
        );
    }
	
	@GetMapping(value="/getZoneReport")
	public List<?> getZoneReport(
			@RequestParam(value="loginId", required = true) String cby
			){
		return scpService.getZoneReport(cby);
	}
	
	@GetMapping(value="/getWardReport")
	public List<?> getWardReport(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value="zone", required = true) String zone
			){
		return scpService.getWardReport(cby,zone);
	}
	
	@GetMapping(value="/getStreetReport")
	public List<?> getStreetReport(
			@RequestParam(value="loginId", required = true) String cby,
			@RequestParam(value="ward", required = true) String ward
			){
		return scpService.getStreetReport(cby,ward);
	}
}
