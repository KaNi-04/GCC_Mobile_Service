package in.gov.chennaicorporation.mobileservice.works.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.works.service.WorksService;

@RequestMapping("/gccofficialapp/works/api")
@RestController("worksRestapi")
public class WorksController {
 
	@Autowired
	private WorksService worksService;
	
	/*
	@GetMapping("/getESTList")
    public List<?> getTaskList(
    		@RequestParam(value = "typeofworkid", required = true) String typeofworkid
			) {
        return worksService.getTaskList(typeofworkid);
    }
	*/
	
	@GetMapping("/getESTList")
    public List<?> getESTList(
    		@RequestParam(value = "typeofworkid", required = false) String typeofworkid,
    		@RequestParam(value = "loginId", required = true) String loginId
			) {
        return worksService.getESTList(loginId);
    }
	
	@GetMapping("/getESTWorkListData")
    public List<?> getESTWorkListData(
    		@RequestParam(value = "estid", required = true) String estid,
    		@RequestParam(value = "loginId", required = false) String cby
			) {
        return worksService.getESTWorkListData(estid);
    }
	
	@GetMapping("/getTaskDataListWithData")
    public List<?> getTaskDataListWithData(
    		@RequestParam(value = "worktasktid", required = true) String wtid,
    		@RequestParam(value = "estid", required = true) String estid,
    		@RequestParam(value = "typeofworkid", required = true) String typeofworkid
			) {
        return worksService.getTaskDataListWithData(wtid, estid,typeofworkid);
    }
	
	@PostMapping(value="/addTaskStageActivityData") 
	public List<?> addTaskStageActivityData(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "estid", required = true) String estid,
			@RequestParam(value = "tmid", required = true) String tmid,
			@RequestParam(value = "worktasktid", required = true) String wtid,
			@RequestParam(value = "activityfile", required = true) MultipartFile activityfile,
			@RequestParam(value = "percentage", required = true) String percentage,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestParam(value = "loginId", required = true) String cby,
			@RequestParam(value = "zone", required = true) String zone,
			@RequestParam(value = "ward", required = true) String ward,
			@RequestParam(value = "latitude", required = true) String latitude, 
			@RequestParam(value = "longitude", required = true) String longitude
			) {
		return worksService.addTaskStageActivityData(estid, tmid, wtid, activityfile, percentage, remarks, cby, zone, ward, latitude, longitude, "open");
	}
	
	@GetMapping("/getZoneReport")
    public List<?> getZoneReport(
    		@RequestParam(value = "loginId", required = false) String cby
			) {
        return worksService.getZoneReport();
    }
	
	@GetMapping("/getWardReport")
    public List<?> getWardReport(
    		@RequestParam(value = "zone", required = true) String zone,
    		@RequestParam(value = "loginId", required = false) String cby
			) {
        return worksService.getWardReport(zone);
    }
	
	@GetMapping("/getListReport")
    public List<?> getListReport(
    		@RequestParam(value = "ward", required = true) String ward,
    		@RequestParam(value = "statusFilter", required = true) String statusFilter,
    		@RequestParam(value = "loginId", required = false) String cby
			) {
        return worksService.getListReport(ward, statusFilter);
    }
	
}
