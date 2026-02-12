package in.gov.chennaicorporation.mobileservice.nulm.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.gov.chennaicorporation.mobileservice.nulm.service.NULMOfficerActivity;

@RequestMapping("/gccofficialapp/api/nulm/officer/")
@RestController("gccofficialappOffcierNULMRest")
public class APINULMOfficerController {
	@Autowired
	private NULMOfficerActivity nulmOfficerActivity;

	@GetMapping(value = "/getAllRequest") // Template
	public List<?> getAllRequest(
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streetid", required = false) String streetid,
			@RequestParam(value = "fromdate", required = false) String fromdate,
			@RequestParam(value = "todate", required = false) String todate,
			@RequestParam(value = "request_type", required = false) String request_type,
			@RequestParam(value = "loginId", required = false) String loginId,
			@RequestParam(value = "status", required = false) String status) {
		return nulmOfficerActivity.getAllRequest(request_type, zone, ward, streetid, fromdate, todate, loginId, status);
	}

	@GetMapping(value = "/getStaffListToRegister") // Template
	public List<?> getStaffListToRegister(
			@RequestParam(value = "reporterId", required = false) String reporterId) {
		return nulmOfficerActivity.getStaffListToRegister(reporterId);
	}

	@GetMapping(value = "/updateStaffFaceRegister") // Template
	public List<?> updateStaffFaceRegister(
			@RequestParam(value = "reporterId", required = false) String reporterId,
			@RequestParam(value = "enrollId", required = false) String enrollId,
			@RequestParam(value = "status", required = false) String status) {
		return nulmOfficerActivity.updateStaffFaceRegister(reporterId, enrollId, status);
	}

//	@GetMapping(value = "/getStaffListForAttendance") // Template
//	public List<?> getStaffListForAttendance(
//			@RequestParam(value = "reporterId", required = false) String reporterId) {
//		return nulmOfficerActivity.getStaffListForAttendance(reporterId);
//	}
	
	@GetMapping(value = "/getStaffListForAttendance_New") // Template
	public List<?> getStaffListForAttendance_New(
			@RequestParam(value = "reporterId", required = false) String reporterId) {
		return nulmOfficerActivity.getStaffListForAttendance(reporterId);
	}

	// @GetMapping(value = "/getStaffListForAttendanceMultipleIncharge") // Template
	// public List<?> getStaffListForAttendance(
	// @RequestParam(value = "reporterId", required = false) String reporterId) {
	// return nulmOfficerActivity.getStaffListForAttendance(reporterId);
	// }
//	@GetMapping(value = "/markAttendance") // Template
//	public List<?> markAttendance(
//			@RequestParam(value = "reporterId", required = false) String reporterId,
//			@RequestParam(value = "enrollId", required = false) String enrollId,
//			@RequestParam(value = "action", required = false) String status,
//			@RequestParam(value = "photourl", required = false) String photourl) {
//		return nulmOfficerActivity.markAttendance(reporterId, enrollId, status, photourl);
//	}

	@GetMapping(value = "/markAttendance_New") // Template
	public List<?> markAttendance_New(
			@RequestParam(value = "reporterId", required = false) String reporterId,
			@RequestParam(value = "enrollId", required = false) String enrollId,
			@RequestParam(value = "action", required = false) String status,
			@RequestParam(value = "photourl", required = false) String photourl,
	        @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "versionName", required = false) String versionName,
            @RequestParam(value = "mobilePlatform", required = false) String mobilePlatform) {
		return nulmOfficerActivity.markAttendance_New(reporterId, enrollId, status, photourl, address, versionName, mobilePlatform);
	}

	@GetMapping(value = "/getStaffAttendanceListByDate") // Template
	public List<?> getStaffAttendanceListByDate(
			@RequestParam(value = "reporterId", required = false) String reporterId,
			@RequestParam(value = "specificDate", required = false) String specificDate) {
		return nulmOfficerActivity.getStaffAttendanceListByDate(reporterId, specificDate);
	}

	@GetMapping(value = "/getAttendanceListByStaff")
	public List<?> getAttendanceListByStaff(@RequestParam(value = "Year") String year,
			@RequestParam(value = "Month") String month, @RequestParam(value = "EmpId") int empId,
			@RequestParam(value = "AttendanceType") String attendanceType) {

		return nulmOfficerActivity.getAttendanceListByStaff(year, month, empId, attendanceType);

	}


	@GetMapping(value = "/getStaffListForAttendanceMultipleIncharge")
	public List<?> getStaffListForAttendanceMultipleIncharge(
	@RequestParam(value = "reporterId", required = false) String reporterId) {
	return nulmOfficerActivity.getStaffListForAttendanceMultipleIncharge(reporterId);
	}
	

	

}
