package in.gov.chennaicorporation.mobileservice.gccactivity.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.MassClearn;

@RequestMapping("/gccofficialapp/api/massClearn/")
@RestController("massActivityRest")
public class APIMassActivityController {
	@Autowired
	private MassClearn massClearn;
	
	@GetMapping(value="/getBrrRoadType")
	public List<?> getBrrRoadType(@RequestParam(value="loginId", required = false) String loginId){
		return massClearn.getBrrRoadType(loginId);
	}
	
	@GetMapping(value="/getAllTaskList")
	public List<?> getAllTaskList(@RequestParam(value="loginId", required = false) String loginId){
		return massClearn.getAllTaskList(loginId);
	}
	
	@GetMapping(value="/getTaskListByZone")
	public List<?> getAllTaskListByZone(
			@RequestParam("zone") String zone,
			@RequestParam(value="loginId", required = false) String loginId){
		return massClearn.getTaskListByZone(zone,loginId);
	}
	
	@GetMapping(value="/getTaskListByWard")
	public List<?> getTaskListByWard(
			@RequestParam("ward") String ward,
			@RequestParam(value="loginId", required = false) String loginId){
		return massClearn.getTaskListByWard(ward,loginId);
	}
	
	@GetMapping(value="/getTaskListByStreetId")
	public List<?> getAllTaskListByStreetId(
			@RequestParam("streeId") String streeId,
			@RequestParam(value="loginId", required = false) String loginId){
		return massClearn.getAllTaskListByStreetId(streeId,loginId);
	}
	
	@GetMapping(value="/getTaskDatesInMonth")
	public List<?> getTaskDatesInMonth(
			@RequestParam("month") String month,
			@RequestParam("year") String year,
			@RequestParam(value="loginId", required = false) String loginId){
		return massClearn.getTaskDatesInMonth(month,year,loginId);
	}
	
	@GetMapping(value="/getAllTaskListByMonth")
	public List<?> getAllTaskListByMonth(
			@RequestParam("month") String month,
			@RequestParam("year") String year,
			@RequestParam(value="loginId", required = false) String loginId){
		return massClearn.getAllTaskListByMonth(month,year,loginId);
	}
	
	@GetMapping(value="/getAllTaskListByDate")
	public List<?> getAllTaskListByDate(
			@RequestParam("workdate") String workdate,
			@RequestParam(value="loginId", required = false) String loginId){
		return massClearn.getAllTaskListByDate(workdate,loginId);
	}
	
	@GetMapping(value="/getTaskInfoBytaskId")
	public List<?> getTaskInfoBytaskId(
			@RequestParam("taskId") String taskId,
			@RequestParam(value="loginId", required = false) String loginId){
		return massClearn.getTaskInfoBytaskId(taskId,loginId);
	}
	
	@GetMapping(value="/getTaskHistoryBytaskId")
	public List<?> getTaskHistoryBytaskId(
			@RequestParam("taskId") String taskId,
			@RequestParam(value="loginId", required = false) String loginId){
		return massClearn.getTaskHistoryBytaskId(taskId,loginId);
	}
	
	@PostMapping(value="/saveTask")
	public List<?> saveTask(
			@RequestParam("taskname") String taskname, 
			@RequestParam("roadtype") String roadtype, 
			@RequestParam("fromdate") String fromdate, 
			@RequestParam("todate") String todate,
			@RequestParam("activitytype") String activitytype,
			@RequestParam("zone") String zone,
			@RequestParam("ward") String ward,
			@RequestParam("streeId") String streeId,
			@RequestParam("streetName") String streetName,
			@RequestParam("taskremarks") String taskremarks,
			@RequestParam("loginId") String loginId,
			@RequestParam("streetlength") String streetlength
			) {
		return massClearn.saveTask(taskname, roadtype, fromdate, todate, activitytype, zone, ward, streeId, streetName, taskremarks, loginId, streetlength);
	}
	
	@GetMapping(value="/DeleteTask")
	public List<?> DeleteTask(
			@RequestParam("taskId") String taskId,
			@RequestParam("loginId") String loginId){
		return massClearn.DeleteTask(taskId,loginId);
	}
	
	@PostMapping(value="/saveTaskActivity")
	public List<?> saveTaskActivity(
			@RequestParam("taskid") String taskid, 
			@RequestParam("before_after") String before_after, 
			@RequestParam(value="activitystatus", required = false) String activitystatus,
			@RequestParam("activity_id") String activity_id,
			@RequestParam("remarks") String remarks,
			@RequestParam("loginId") String loginId,
			@RequestParam(value = "file", required = false) MultipartFile file) {
		return massClearn.saveTaskActivity(taskid, before_after, activitystatus, activity_id, remarks, loginId, file);
	}
	
	@GetMapping(value="/reportByDate")
	public List<?> reportByDate(
			@RequestParam("fromdate") String fromdate,
			@RequestParam("todate") String todate,
			@RequestParam("roadtype") String roadtype,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streetid", required = false) String streetid,
			@RequestParam(value="loginId", required = false) String loginId,
			@RequestParam(value="status", required = false) String status){
		return massClearn.reportByDate(fromdate, todate, roadtype, zone, ward, streetid,loginId,status);
	}
	
	@GetMapping(value="/reportByStatus")
	public List<?> reportByStatus(
			@RequestParam("fromdate") String fromdate,
			@RequestParam("todate") String todate,
			@RequestParam("status") String status,
			@RequestParam("roadtype") String roadtype,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streetid", required = false) String streetid,
			@RequestParam(value="loginId", required = false) String loginId){
		return massClearn.reportByStatus(fromdate, todate, status, roadtype, zone, ward, streetid,loginId);
	}
	
}
