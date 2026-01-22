package in.gov.chennaicorporation.mobileservice.gccstraydogs.controller;

import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.hibernate.internal.build.AllowSysOut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import in.gov.chennaicorporation.mobileservice.gccstraydogs.service.UserService;

@RequestMapping("/gccofficialapp/api/gccstraydogs/user/")
@RestController("gccofficialappUserStraydogRest")
public class UserApiController {

	@Autowired
	private UserService userService;

	@PostMapping("/step1")
	public ResponseEntity<Map<String, Object>> saveStep1(@RequestParam("qr_id") String qr_id,
			@RequestParam("userid_1") String userid_1, @RequestParam("date1") String date1,
			@RequestParam("lat_1") String lat_1, @RequestParam("long_1") String long_1,
			@RequestParam("location_1") String location_1, @RequestParam("zone_1") String zone_1,
			@RequestParam("ward_1") String ward_1, @RequestParam("streetid_1") String streetid_1,
			@RequestParam(value = "streetname_1", required = false, defaultValue = "-") String streetname_1,
			@RequestParam("photo1") MultipartFile photo1, @RequestParam("sex_1") String sex_1,
			@RequestParam("contraception_1") boolean contraception_1,
			@RequestParam("flow_location") boolean flow_location) {

		try {

			int stage = 1;
			Timestamp date_1 = Timestamp.valueOf(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.format(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(date1)));
			Map<String, Object> response = new HashMap<>();

			String result = userService.checkQrId(qr_id);

			if (result.equalsIgnoreCase("false")) {

				String step = userService.saveStep1(qr_id, userid_1, date_1, lat_1, long_1, location_1, zone_1, ward_1,
						streetid_1, streetname_1, photo1, sex_1, contraception_1, stage, flow_location);
				response.put("status", "OK");
				response.put("QR_id", qr_id);
				response.put("Step", step);
			} else {
				response.put("Message", "Provided QRID = " + qr_id + " already exists");
				response.put("QR_id", qr_id);

			}

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("status", "ERROR", "message", "Error saving Step_1: " + e.getMessage()));
		}
	}

	@GetMapping(value = "/getABCCenter")
	public List<?> getAlert(@RequestParam(value = "latitude", required = false) String latitude,
			@RequestParam(value = "longitude", required = false) String longitude) {
		return userService.getABCCenter(latitude, longitude);
	}

	@GetMapping(value = "/getABCCenterList")
	public List<?> getABCCenterList() {
		return userService.getABCCenterList();
	}

	@PostMapping("/step2")
	public ResponseEntity<Map<String, Object>> saveStep2(@RequestParam("qr_id") String qr_id,
			@RequestParam("userid_2") String userid_2, @RequestParam("date2") String date2,
			@RequestParam("lat_2") String lat_2, @RequestParam("long_2") String long_2,
			@RequestParam("location_2") String location_2,
			@RequestParam(value = "zone_2", required = false, defaultValue = "-") String zone_2,
			@RequestParam(value = "ward_2", required = false, defaultValue = "-") String ward_2,
			@RequestParam(value = "streetid_2", required = false, defaultValue = "-") String streetid_2,
			@RequestParam(value = "streetname_2", required = false, defaultValue = "-") String streetname_2,
			@RequestParam("abc_center_2") String abc_center_2, @RequestParam("photo2") MultipartFile photo2,
			@RequestParam("sex_2") String sex_2, @RequestParam("contraception_2") boolean contraception_2) {

		try {
			int stage = 2;
			Timestamp date_2 = Timestamp.valueOf(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.format(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(date2)));

			// String photo_2 = photo2.getOriginalFilename();

			String step = userService.saveStep2(qr_id, userid_2, date_2, lat_2, long_2, location_2, zone_2, ward_2,
					streetid_2, streetname_2, abc_center_2, photo2, sex_2, contraception_2, stage);

			// Prepare and return the response
			Map<String, Object> response = new HashMap<>();
			response.put("status", "OK");
			response.put("QR_id", qr_id);
			response.put("Step", step);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("status", "ERROR", "message", "Error saving Step_2: " + e.getMessage()));
		}
	}

	@PostMapping("/step3")
	public ResponseEntity<Map<String, Object>> saveStep3(@RequestParam("qr_id") String qr_id,
			@RequestParam("rf_id") String rf_id, @RequestParam("userid_3") String userid_3,
			@RequestParam("sex_3") String sex_3, @RequestParam("body_weight_3") String body_weight_3,
			@RequestParam("temperature_3") String temperature_3,
			@RequestParam("surgery_fitness_3") boolean surgery_fitness_3,
			@RequestParam("surgery_type_3") String surgery_type_3) {

		int stage = 0;
		String step = "";
		String qrid = "";
		Timestamp surgery_date = new Timestamp(new Date().getTime()); // Current timestamp

		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String formattedDate = sdf.format(surgery_date);

			// Fetch logs using rf_id
			List<Map<String, Object>> data = userService.getlogsrfid(rf_id);
			Map<String, Object> response = new HashMap<>();

			if (data != null && !data.isEmpty()) {
				for (Map<String, Object> map : data) {
					if (map.get("date_5") != null) {
						int days = userService.getVaccineDue(rf_id, formattedDate);
						if (days == 99999) {
						} else if (days <= 10) {

							stage = 5;
							step = userService.updateStage(rf_id, qr_id, stage);
						} else {
							stage = 6;
							step = userService.updateStage(rf_id, qr_id, stage);
						}

//	        	            response.put("ARV_Expiry", Integer.toString(days));
//	        	            response.put("DHPPI_Expiry", Integer.toString(days));
					}
				}
				String qrIDD = userService.updateStage3(rf_id, qr_id);

			} else {
				if (surgery_fitness_3) {
					stage = 4;
				} else {

					stage = 5;
				}
				step = userService.updateStage(rf_id, qr_id, stage);
				qrid = userService.saveStep3(qr_id, rf_id, userid_3, sex_3, body_weight_3, temperature_3,
						surgery_fitness_3, surgery_type_3, surgery_date);
			}

			// Create response
			response.put("QR_id", qr_id);
			response.put("RF_id", rf_id);
			response.put("Step", step);
			response.put("status", "OK");

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("status", "ERROR", "message", "Error saving Step_3: " + e.getMessage()));
		}
	}

	@PostMapping("/step4")
	public ResponseEntity<Map<String, Object>> saveStep4(@RequestParam("rf_id") String rf_id,
			@RequestParam("qr_id") String qr_id, @RequestParam("userid_4") String userid_4,
			@RequestParam("surgery_complication_4") boolean surgery_complication_4,
			@RequestParam("complication_type_4") String complication_type_4,
			@RequestParam("ready_to_release_4") boolean ready_to_release_4) {
		try {
			int stage = 0;
			if (ready_to_release_4) {
				stage = 5; // Assuming stage is hardcoded to 5 for Step 4
			} else {
				stage = 0;
			}

			String qrid = userService.saveStep4(qr_id, rf_id, userid_4, surgery_complication_4, complication_type_4,
					ready_to_release_4);
			String step = userService.updateStage(rf_id, qr_id, stage);

			// Prepare response
			Map<String, Object> response = new HashMap<>();
			response.put("QR_id", qrid);
			response.put("RF_id", rf_id);
			response.put("Step", step);
			response.put("status", "OK");

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("status", "ERROR", "message", "Error saving Step 4: " + e.getMessage()));
		}
	}

	@PostMapping("/step5")
	public ResponseEntity<Map<String, Object>> saveStep5(@RequestParam("rf_id") String rf_id,
			@RequestParam("qr_id") String qr_id, @RequestParam("userid_5") String userid_5,
			@RequestParam("arv_5") boolean arv_5, @RequestParam("dhppi_5") boolean dhppi_5,
			@RequestParam("photo_5") MultipartFile photo_5, @RequestParam("lat_5") String lat_5,
			@RequestParam("long_5") String long_5, @RequestParam("location_5") String location_5,
			@RequestParam("zone_5") String zone_5, @RequestParam("ward_5") String ward_5,
			@RequestParam(value = "streetid_5", required = false, defaultValue = "-") String streetid_5,
			@RequestParam(value = "streetname_5", required = false, defaultValue = "-") String streetname_5,
			@RequestParam("date_5") String date_5) {
		try {
			int stage = 6; // Assuming stage is set to 6 for Step 5
			Timestamp date5 = parseDate(date_5);
			int days = userService.getVaccineDue(rf_id, date5.toString());
			Map<String, Object> response = new HashMap<>();

			String qrid;
			String step;

			if (days <= 10 && days != 99999) {
				qrid = userService.saveNewStep5(rf_id, qr_id, userid_5, arv_5, dhppi_5, photo_5, lat_5, long_5,
						location_5, zone_5, ward_5, streetid_5, date5);
				step = userService.updateStage(rf_id, qr_id, stage);
				response.put("status", "OK");
				response.put("Qr_id", qrid);
				response.put("Step", step);
			} else if (days > 10 && days != 99999) {
				qrid = userService.saveUpdateStep5(rf_id, qr_id, userid_5, arv_5, dhppi_5, photo_5, lat_5, long_5,
						location_5, zone_5, ward_5, streetid_5, streetname_5, date5);
				step = userService.updateStage(rf_id, qr_id, stage);
				response.put("status", "OK");
				response.put("Qr_id", qrid);
				response.put("Step", step);
			} else if (days == 99999) {
				response.put("status", "Provided RFID = " + rf_id + " is wrong");
				response.put("Rf_id", rf_id);
			}

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("status", "ERROR", "message", "Error saving Step 5: " + e.getMessage()));
		}
	}

	private Timestamp parseDate(String dateStr) throws ParseException {
		SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = inputFormat.parse(dateStr);
		return Timestamp.valueOf(outputFormat.format(date));
	}

	@PostMapping("/step6")
	public ResponseEntity<Map<String, Object>> saveStep6(@RequestParam("qr_id") String qr_id,
			@RequestParam("userid_6") String userid_6, @RequestParam("lat_6") String lat_6,
			@RequestParam("long_6") String long_6, @RequestParam("location_6") String location_6,
			@RequestParam("zone_6") String zone_6, @RequestParam("ward_6") String ward_6,
			@RequestParam("streetid_6") String streetid_6,
			@RequestParam(value = "streetname_6", required = false, defaultValue = "-") String streetname_6,
			@RequestParam("date_6") String date_6, @RequestParam("photo_6") MultipartFile photo_6) {
		try {
			int stage = 0; // Assuming stage 0 is a valid value, adjust as needed

			Timestamp date6 = parseDate(date_6);
			// String photoFilename = photo_6.getOriginalFilename(); // Retrieve the
			// filename of the uploaded photo

			String step = userService.saveStep6(qr_id, userid_6, lat_6, long_6, location_6, zone_6, ward_6, streetid_6,
					streetname_6, date6, photo_6, stage);

			Map<String, Object> response = new HashMap<>();
			response.put("status", "OK");
			response.put("Qr_id", qr_id);
			response.put("Step", step);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("status", "ERROR", "message", "Error saving Step 6: " + e.getMessage()));
		}
	}

	private Timestamp parseDate1(String dateStr) throws ParseException {
		SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = inputFormat.parse(dateStr);
		return Timestamp.valueOf(outputFormat.format(date));
	}

	@GetMapping("/getMasterData")
	public ResponseEntity<Map<String, Object>> getMasterData(@RequestParam String rf_id) {
		try {

			List<Map<String, Object>> data = userService.getMasterData(rf_id);
			Map<String, Object> response = new HashMap<>();

			Timestamp surgery_date = new Timestamp(new Date().getTime()); // Current timestamp
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String formattedDate = sdf.format(surgery_date);

			int days = userService.getVaccineDue(rf_id, formattedDate);

			if (!data.isEmpty() && days != 99999) {
				for (Map<String, Object> map : data) {
					map.put("ARV_Expiry", days);
					map.put("DHPPI_Expiry", days);
				}
				response.put("status", "OK");
				response.put("data", data);
			} else {
				response.put("status", "Failed");
				response.put("message", "No Data Found");
			}

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			// Log the exception details for debugging
			e.printStackTrace();

			// Provide a user-friendly error message
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("status", "Error", "message", "Error getting data: " + e.getMessage()));
		}
	}

	@GetMapping("/getLogData")
	public ResponseEntity<Map<String, Object>> getLogData(@RequestParam String qr_id) {
		try {
			List<Map<String, Object>> data = userService.getLogData(qr_id);
			Map<String, Object> response = new HashMap<>();
			if (data.size() != 0) {
				response.put("status", "OK");
				response.put("data", data);
			} else {
				// response.put("status", "Failed");
				// response.put("data", "No Data Found");
			}
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			// Log the exception details for debugging
			e.printStackTrace();

			// Provide a user-friendly error message
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("status", "Error", "message", "Error getting data: " + e.getMessage()));
		}
	}

//  Get Overall Data	    
	@GetMapping("getoveralldata")
	public ResponseEntity<Map<String, Object>> getOverallData(@RequestParam String fromDate,
			@RequestParam String toDate) {

		Map<String, Object> response = new HashMap<>();
		List<Map<String, Object>> result = userService.getOverallData(fromDate, toDate);
		if (!result.isEmpty()) {

			response.put("Message", "success");
			response.put("Data", result);
		} else {

			response.put("Message", "Failed");
		}

		return ResponseEntity.ok().body(response);
	}

// Get Overall Catched Data	    
	@GetMapping("getoverallcatcheddata")
	public List<Map<String, Object>> getoverallcatcheddata(@RequestParam String fromDate, @RequestParam String toDate) {
		return userService.getoverallcatcheddata(fromDate, toDate);
	}

// Get Overall Released Data		    
	@GetMapping("getoverallreleaseddata")
	public List<Map<String, Object>> getoverallreleaseddata(@RequestParam String fromDate,
			@RequestParam String toDate) {
		return userService.getoverallreleaseddata(fromDate, toDate);
	}

// Get AbcCenterwiseData
	@GetMapping("getAbcCenterwiseData")
	public ResponseEntity<Map<String, Object>> getAbcCenterwiseData(@RequestParam String fromDate,
			@RequestParam String toDate, @RequestParam String abcCenter) {
		Map<String, Object> response = new HashMap<>();
		List<Map<String, Object>> result = userService.getAbcCenterwiseData(fromDate, toDate, abcCenter);
		if (!result.isEmpty()) {

			response.put("Message", "success");
			response.put("Data", result);
		} else {

			response.put("Message", "Failed");
		}

		return ResponseEntity.ok().body(response);

	}

// Unstrelized 
//Get Unstrelized zone breakup  
	@GetMapping("getUnstrelizedZoneBreakup")
	public List<Map<String, Object>> getUnstrelizedZoneBreakup(@RequestParam String fromDate,
			@RequestParam String toDate, @RequestParam String abcCenter) {
		return userService.getUnstrelizedZoneBreakup(fromDate, toDate, abcCenter);
	}

// Get Unstrelized division breakup  
	@GetMapping("getUnstrelizedDivisionBreakup")
	public List<Map<String, Object>> getUnstrelizedDivisionBreakup(@RequestParam String fromDate,
			@RequestParam String toDate, @RequestParam String abcCenter, @RequestParam String zone) {
		return userService.getUnstrelizedDivisionBreakup(fromDate, toDate, abcCenter, zone);
	}

// Get Unstrelized street breakup  
	@GetMapping("getUnstrelizedStreetBreakup")
	public List<Map<String, Object>> getUnstrelizedStreetBreakup(@RequestParam String fromDate,
			@RequestParam String toDate, @RequestParam String abcCenter, @RequestParam String ward,
			@RequestParam String zone) {
		return userService.getUnstrelizedStreetBreakup(ward, fromDate, toDate, abcCenter, zone);
	}

	// Get Unstrelized dogs breakup
	@GetMapping("getUnstrelizedDogsBreakup")
	public Map<String, Object> getUnstrelizedDogsBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter, @RequestParam String ward, @RequestParam String zone,
			@RequestParam String streetID, @RequestParam String sex) {
		Map<String, Object> response = new LinkedHashMap<>();

		try {
			List<Map<String, Object>> data = userService.getUnstrelizedDogsBreakup(ward, fromDate, toDate, abcCenter,
					zone, streetID, sex);

			Map<String, List<Map<String, Object>>> groupedData = new LinkedHashMap<>();
			for (Map<String, Object> record : data) {
				String rfId = (String) record.get("rf_id");
				groupedData.putIfAbsent(rfId, new ArrayList<>());
				groupedData.get(rfId).add(record);
			}

			List<Map<String, Object>> list1 = new ArrayList<>();
			for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
				Map<String, Object> object1 = new LinkedHashMap<>();
				object1.put("rfid", entry.getKey());
				object1.put("rfid_data", entry.getValue());
				list1.add(object1);
			}

			response.put("message", "success");
			response.put("data", list1);
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			response.put("message", "Error");
			response.put("data", "No Data Found");
			return response;
		}
	}

//  Strelized 
// Get Strelized zone breakup  
	@GetMapping("getStrelizedZoneBreakup")
	public List<Map<String, Object>> geStrelizedZoneBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter) {
		return userService.getStrelizedZoneBreakup(fromDate, toDate, abcCenter);
	}

// Get Strelized division breakup  
	@GetMapping("getStrelizedDivisionBreakup")
	public List<Map<String, Object>> getStrelizedDivisionBreakup(@RequestParam String fromDate,
			@RequestParam String toDate, @RequestParam String abcCenter, @RequestParam String zone) {
		return userService.getStrelizedDivisionBreakup(fromDate, toDate, abcCenter, zone);
	}

// Get Strelized street breakup  
	@GetMapping("getStrelizedStreetBreakup")
	public List<Map<String, Object>> getStrelizedStreetBreakup(@RequestParam String fromDate,
			@RequestParam String toDate, @RequestParam String abcCenter, @RequestParam String ward,
			@RequestParam String zone) {
		return userService.getStrelizedStreetBreakup(ward, fromDate, toDate, abcCenter, zone);
	}

	// Get Strelized dogs breakup
	@GetMapping("getStrelizedDogsBreakup")
	public Map<String, Object> getStrelizedDogsBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter, @RequestParam String ward, @RequestParam String zone,
			@RequestParam String streetID, @RequestParam String sex) {
		Map<String, Object> response = new LinkedHashMap<>();

		try {
			List<Map<String, Object>> data = userService.getStrelizedDogsBreakup(ward, fromDate, toDate, abcCenter,
					zone, streetID, sex);

			Map<String, List<Map<String, Object>>> groupedData = new LinkedHashMap<>();
			for (Map<String, Object> record : data) {
				String rfId = (String) record.get("rf_id");
				groupedData.putIfAbsent(rfId, new ArrayList<>());
				groupedData.get(rfId).add(record);
			}

			List<Map<String, Object>> list1 = new ArrayList<>();
			for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
				Map<String, Object> object1 = new LinkedHashMap<>();
				object1.put("rfid", entry.getKey());
				object1.put("rfid_data", entry.getValue());
				list1.add(object1);
			}

			response.put("message", "success");
			response.put("data", list1);
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			response.put("message", "Error");
			response.put("data", "No Data Found");
			return response;
		}
	}

//Unfit
// Get Unfit zone breakup  
	@GetMapping("getUnfitZoneBreakup")
	public List<Map<String, Object>> getUnfitZoneBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter) {
		return userService.getUnfitZoneBreakup(fromDate, toDate, abcCenter);
	}

// Get Unfit division breakup  
	@GetMapping("getUnfitDivisionBreakup")
	public List<Map<String, Object>> getUnfitDivisionBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter, @RequestParam String zone) {
		return userService.getUnfitDivisionBreakup(fromDate, toDate, abcCenter, zone);
	}

// Get Unfit street breakup  
	@GetMapping("getUnfitStreetBreakup")
	public List<Map<String, Object>> getUnfitStreetBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter, @RequestParam String ward, @RequestParam String zone) {
		return userService.getUnfitStreetBreakup(ward, zone, fromDate, toDate, abcCenter);
	}

	// Get Unfit dogs breakup
	@GetMapping("getUnfitDogsBreakup")
	public Map<String, Object> getUnfitDogsBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter, @RequestParam String ward, @RequestParam String zone,
			@RequestParam String streetID, @RequestParam String sex) {
		Map<String, Object> response = new LinkedHashMap<>();

		try {
			List<Map<String, Object>> data = userService.getUnfitDogsBreakup(ward, fromDate, toDate, abcCenter, zone,
					streetID, sex);

			Map<String, List<Map<String, Object>>> groupedData = new LinkedHashMap<>();
			for (Map<String, Object> record : data) {
				String rfId = (String) record.get("rf_id");
				groupedData.putIfAbsent(rfId, new ArrayList<>());
				groupedData.get(rfId).add(record);
			}

			List<Map<String, Object>> list1 = new ArrayList<>();
			for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
				Map<String, Object> object1 = new LinkedHashMap<>();
				object1.put("rfid", entry.getKey());
				object1.put("rfid_data", entry.getValue());
				list1.add(object1);
			}

			response.put("message", "success");
			response.put("data", list1);
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			response.put("message", "Error");
			response.put("data", "No Data Found");
			return response;
		}
	}

//Surgery
//Get Surgery zone breakup  
	@GetMapping("getSurgeryZoneBreakup")
	public List<Map<String, Object>> getSurgeryZoneBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter) {
		return userService.getSurgeryZoneBreakup(fromDate, toDate, abcCenter);
	}

//Get Surgery division breakup  
	@GetMapping("getSurgeryDivisionBreakup")
	public List<Map<String, Object>> getSurgeryDivisionBreakup(@RequestParam String fromDate,
			@RequestParam String toDate, @RequestParam String abcCenter, @RequestParam String zone) {
		return userService.getSurgeryDivisionBreakup(fromDate, toDate, abcCenter, zone);
	}

//Get Surgery street breakup  
	@GetMapping("getSurgeryStreetBreakup")
	public List<Map<String, Object>> getSurgeryStreetBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter, @RequestParam String ward, @RequestParam String zone) {
		return userService.getSurgeryStreetBreakup(ward, zone, fromDate, toDate, abcCenter);
	}

// Get Surgery dogs breakup
	@GetMapping("getSurgeryDogsBreakup")
	public Map<String, Object> getSurgeryDogsBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter, @RequestParam String ward, @RequestParam String zone,
			@RequestParam String streetID, @RequestParam String sex) {
		Map<String, Object> response = new LinkedHashMap<>();

		try {
			List<Map<String, Object>> data = userService.getSurgeryDogsBreakup(ward, fromDate, toDate, abcCenter, zone,
					streetID, sex);

			Map<String, List<Map<String, Object>>> groupedData = new LinkedHashMap<>();
			for (Map<String, Object> record : data) {
				String rfId = (String) record.get("rf_id");
				groupedData.putIfAbsent(rfId, new ArrayList<>());
				groupedData.get(rfId).add(record);
			}

			List<Map<String, Object>> list1 = new ArrayList<>();
			for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
				Map<String, Object> object1 = new LinkedHashMap<>();
				object1.put("rfid", entry.getKey());
				object1.put("rfid_data", entry.getValue());
				list1.add(object1);
			}

			response.put("message", "success");
			response.put("data", list1);
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			response.put("message", "Error");
			response.put("data", "No Data Found");
			return response;
		}
	}

//Died
//Get Died zone breakup  
	@GetMapping("getDiedZoneBreakup")
	public List<Map<String, Object>> getDiedZoneBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter) {
		return userService.getDiedZoneBreakup(fromDate, toDate, abcCenter);
	}

//Get Died division breakup  
	@GetMapping("getDiedDivisionBreakup")
	public List<Map<String, Object>> getDiedDivisionBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter, @RequestParam String zone) {
		return userService.getDiedDivisionBreakup(fromDate, toDate, abcCenter, zone);
	}

//Get Died street breakup  
	@GetMapping("getDiedStreetBreakup")
	public List<Map<String, Object>> getDiedStreetBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter, @RequestParam String ward, @RequestParam String zone) {
		return userService.getDiedStreetBreakup(ward, zone, fromDate, toDate, abcCenter);
	}

// Get Died dogs breakup
	@GetMapping("getDiedDogsBreakup")
	public Map<String, Object> getDiedDogsBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter, @RequestParam String ward, @RequestParam String zone,
			@RequestParam String streetID, @RequestParam String sex) {
		Map<String, Object> response = new LinkedHashMap<>();

		try {
			List<Map<String, Object>> data = userService.getDiedDogsBreakup(ward, fromDate, toDate, abcCenter, zone,
					streetID, sex);

			Map<String, List<Map<String, Object>>> groupedData = new LinkedHashMap<>();
			for (Map<String, Object> record : data) {
				String rfId = (String) record.get("rf_id");
				groupedData.putIfAbsent(rfId, new ArrayList<>());
				groupedData.get(rfId).add(record);
			}

			List<Map<String, Object>> list1 = new ArrayList<>();
			for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
				Map<String, Object> object1 = new LinkedHashMap<>();
				object1.put("rfid", entry.getKey());
				object1.put("rfid_data", entry.getValue());
				list1.add(object1);
			}

			response.put("message", "success");
			response.put("data", list1);
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			response.put("message", "Error");
			response.put("data", "No Data Found");
			return response;
		}
	}

//Released
//Get Released zone breakup  
	@GetMapping("getReleasedZoneBreakup")
	public List<Map<String, Object>> getReleasedZoneBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter) {
		return userService.getReleasedZoneBreakup(fromDate, toDate, abcCenter);
	}

//Get Released division breakup  
	@GetMapping("getReleasedDivisionBreakup")
	public List<Map<String, Object>> getReleasedDivisionBreakup(@RequestParam String fromDate,
			@RequestParam String toDate, @RequestParam String abcCenter, @RequestParam String zone) {
		return userService.getReleasedDivisionBreakup(fromDate, toDate, abcCenter, zone);
	}

//Get Released street breakup  
	@GetMapping("getReleasedStreetBreakup")
	public List<Map<String, Object>> getReleasedStreetBreakup(@RequestParam String fromDate,
			@RequestParam String toDate, @RequestParam String abcCenter, @RequestParam String ward,
			@RequestParam String zone) {
		return userService.getReleasedStreetBreakup(ward, zone, fromDate, toDate, abcCenter);
	}

	// Get Released dogs breakup
	@GetMapping("getReleasedDogsBreakup")
	public Map<String, Object> getReleasedDogsBreakup(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String abcCenter, @RequestParam String ward, @RequestParam String zone,
			@RequestParam String streetID, @RequestParam String sex) {
		Map<String, Object> response = new LinkedHashMap<>();

		try {
			List<Map<String, Object>> data = userService.getReleasedDogsBreakup(ward, fromDate, toDate, abcCenter, zone,
					streetID, sex);

			Map<String, List<Map<String, Object>>> groupedData = new LinkedHashMap<>();
			for (Map<String, Object> record : data) {
				String rfId = (String) record.get("rf_id");
				groupedData.putIfAbsent(rfId, new ArrayList<>());
				groupedData.get(rfId).add(record);
			}

			List<Map<String, Object>> list1 = new ArrayList<>();
			for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
				Map<String, Object> object1 = new LinkedHashMap<>();
				object1.put("rfid", entry.getKey());
				object1.put("rfid_data", entry.getValue());
				list1.add(object1);
			}

			response.put("message", "success");
			response.put("data", list1);
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			response.put("message", "Error");
			response.put("data", "No Data Found");
			return response;
		}
	}

// Get List of Zone
	@GetMapping("getZoneList")
	public ResponseEntity<Map<String, Object>> getZoneList() {
		Map<String, Object> response = new HashMap<>();
		List<Map<String, Object>> result = userService.getZoneList();
		if (!result.isEmpty()) {

			response.put("Message", "success");
			response.put("Data", result);
		} else {

			response.put("Message", "Failed");
		}

		return ResponseEntity.ok().body(response);

	}

	@GetMapping("/overallExcelReport")
	public ResponseEntity<InputStreamResource> overallExcelReport(@RequestParam String fromDate,
			@RequestParam String toDate) {

		ByteArrayInputStream reportStream = userService.overallExcelReport(fromDate, toDate);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Disposition", "attachment; filename=Zonewise_Overall_Report.xlsx");

		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new InputStreamResource(reportStream));
	}

	@GetMapping("/abcCenterExcelReport")
	public ResponseEntity<InputStreamResource> abcCenterExcelReport(@RequestParam String fromDate,
			@RequestParam String toDate, @RequestParam String abcCenter) {

		ByteArrayInputStream reportStream = userService.abcCenterExcelReport(fromDate, toDate, abcCenter);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Disposition", "attachment; filename=ABC_Centerwise_Report.xlsx");

		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new InputStreamResource(reportStream));
	}

	@PostMapping("/saveBuildings")
	public ResponseEntity<Map<String, Object>> saveBuildings(@RequestParam("location") String location,
			@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude,
			@RequestParam(value = "street_id", required = false) String street_id,
			@RequestParam(value = "street_name", required = false) String street_name) {

		Map<String, Object> response = new HashMap<>();
		try {
			if (street_id.equals("") || street_id == null && street_name.equals("") || street_name == null) {

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
				String dateTime = LocalDateTime.now().format(formatter);
				street_id = "L_" + dateTime;
				street_name = location;
				String data = userService.saveBuildings(location, latitude, longitude, street_id, street_name);
			} else {

				String data = userService.saveBuildings(location, latitude, longitude, street_id, street_name);
			}

			response.put("Status", "Sucess");
			response.put("Message", "Saved Sucessfuly");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			e.printStackTrace();
			response.put("Status", "Error");
			response.put("Message", "Failed to Save");
			return ResponseEntity.ok(response);
		}
	}

	@GetMapping(value = "/getBuildings")
	public List<?> getBuildings(@RequestParam(value = "latitude", required = false) String latitude,
			@RequestParam(value = "longitude", required = false) String longitude) {
		return userService.getBuildings(latitude, longitude);
	}

}