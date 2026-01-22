package in.gov.chennaicorporation.mobileservice.gccparks.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccparks.service.NoDataException;
import in.gov.chennaicorporation.mobileservice.gccparks.service.ParksService;

@RequestMapping("/gccofficialapp/api/parks")
@RestController("gccofficalapparksrest")
public class ParksController {

	@Autowired
	private ParksService parksService;

	@GetMapping("/saveAssets")
	public List<?> saveAssets(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "categoryId", required = false) String categoryId,
			@RequestParam(value = "assetTypeId", required = false) String assetTypeId,
			@RequestParam(value = "latitude", required = false) String latitude,
			@RequestParam(value = "longitude", required = false) String longitude,
			@RequestParam(value = "zone", required = false) String zone,
			@RequestParam(value = "ward", required = false) String ward,
			@RequestParam(value = "streeId", required = false) String streeId,
			@RequestParam(value = "streetName", required = false) String streetName,
			@RequestParam(value = "loginId", required = false) String loginId,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "file", required = false) MultipartFile file) {
		return parksService.saveAsset(formData, categoryId, assetTypeId, latitude, longitude, zone, ward, streeId,
				streetName, loginId, name, file);
	}

	@GetMapping("/filterAssets")
	public List<?> getAssets(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "categoryId", required = false) String categoryId,
			@RequestParam(value = "assetTypeId", required = false) String assetTypeId,
			@RequestParam(value = "latitude", required = false) String latitude,
			@RequestParam(value = "longitude", required = false) String longitude) {
		return parksService.loadAssetByFilter(categoryId, assetTypeId, latitude, longitude);
	}

	// ----------------------------------------------Enumeration
	// API------------------------------------------------
	@PostMapping("/centerMedianEnumeration")
	public ResponseEntity<String> centerMedianEnumeration(@RequestParam("EnterParkName") String EnterParkName,
			@RequestParam("maintained_By") int maintained_By, @RequestParam("Enu_Area_Meters") Double Enu_Area_Meters,
			@RequestParam("TypeofCenterMedian") int TypeofCenterMedian, // ask
			@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude,
			@RequestParam("zone") String zone, @RequestParam("ward") String ward,
			@RequestParam(value = "location", required = false) String location, @RequestParam("UserId") String UserId,
			@RequestParam("Enumeration_Image") MultipartFile photo,
			@RequestParam("enumeration_type") String enumeration_type

	) throws Exception {
		
		int centerMedianEnumeration = parksService.centerMedianEnumeration(EnterParkName, maintained_By,
				Enu_Area_Meters, TypeofCenterMedian, latitude, longitude, zone, ward, location, UserId, photo,
				enumeration_type);

		if (centerMedianEnumeration == 0 || centerMedianEnumeration == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}

		return ResponseEntity.ok("Details were saved successfully");
	}

	@PostMapping("/trafficIslandEnumeration")
	public ResponseEntity<String> trafficIslandEnumeration(
			@RequestParam("maintained_By") int maintained_By, @RequestParam("Enu_Area_Meters") Double Enu_Area_Meters,
			@RequestParam("TypeofCenterMedian") int Type_of_Traffic_Island, // ask
			@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude,
			@RequestParam("zone") String zone, @RequestParam("ward") String ward,
			@RequestParam(value = "location", required = false) String location, @RequestParam("UserId") String UserId,
			@RequestParam("Enumeration_Image") MultipartFile photo,
			@RequestParam("enumeration_type") String enumeration_type) throws Exception {
		
		int trafficIslandEnumeration = parksService.trafficIslandEnumeration(maintained_By, Enu_Area_Meters,
				Type_of_Traffic_Island, latitude, longitude, zone, ward, location, UserId, photo, enumeration_type);
		if (trafficIslandEnumeration == 0 || trafficIslandEnumeration == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}

		return ResponseEntity.ok("Details were saved successfully");
	}

	@PostMapping("/parkEnumeration")
	public ResponseEntity<String> parkEnumeration(@RequestParam("EnterParkName") String EnterParkName,
			@RequestParam("maintained_By") int maintained_By, @RequestParam("latitude") String latitude,
			@RequestParam("longitude") String longitude, @RequestParam("zone") String zone,
			@RequestParam("ward") String ward, @RequestParam(value = "location", required = false) String location,
			@RequestParam("UserId") String UserId, @RequestParam("Zone_Available") String Zone_Available, // reading zone
			@RequestParam("Enumeration_Image") MultipartFile photo,
			@RequestParam("enumeration_type") String enumeration_type) throws Exception {

		int parkEnumeration = parksService.parkEnumeration(EnterParkName, maintained_By, latitude, longitude, zone,
				ward, location, UserId, photo, Zone_Available, enumeration_type);

		if (parkEnumeration == 0 || parkEnumeration == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}

		return ResponseEntity.ok("Details were saved successfully");
	}

	@PostMapping("/beautificationRoadEnumeration")
	public ResponseEntity<String> beautificationRoad(
//			@RequestParam("Area") String Area,//location
			@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude,
			@RequestParam("zone") String zone, @RequestParam("ward") String ward,
			@RequestParam(value = "location", required = false) String location, @RequestParam("UserId") String UserId,
			@RequestParam("Enumeration_Image") MultipartFile photo,
			@RequestParam("enumeration_type") String enumeration_type) throws Exception {

		int beautificationRoad = parksService.beautificationRoad(latitude, longitude, zone, ward, location, UserId,
				photo, enumeration_type);

		if (beautificationRoad == 0 || beautificationRoad == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}

		return ResponseEntity.ok("Details were saved successfully");
	}

	@PostMapping("/readingZoneEnumeration")
	public ResponseEntity<String> readingZone(
			@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude,
			@RequestParam("zone") String zone, @RequestParam("ward") String ward,
			@RequestParam(value = "location", required = false) String location, @RequestParam("UserId") String UserId,
			// @RequestParam("Zone_Available_name") String Zone_Available_name,//reading zone
			@RequestParam("Enumeration_Image") MultipartFile photo,
			@RequestParam("enumeration_type") String enumeration_type, @RequestParam("park_name") String park_name // name
	) throws Exception {
		
		int readingZone = parksService.readingZone(latitude, longitude, zone, ward, location, UserId, photo,
				enumeration_type, park_name);

		if (readingZone == 0 || readingZone == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}

		return ResponseEntity.ok("Details were saved successfully");
	}

	@PostMapping("/miyawakiNewEnumeration")
	public ResponseEntity<String> miyawakiNew(@RequestParam("latitude") String latitude,
			@RequestParam("longitude") String longitude, @RequestParam("zone") String zone,
			@RequestParam("ward") String ward, @RequestParam(value = "location", required = false) String location,
			@RequestParam("UserId") String UserId, @RequestParam("Type_of_place") String Type_of_place, // reading zone
			@RequestParam("Enumeration_Image") MultipartFile photo,
			@RequestParam("enumeration_type") String enumeration_type) throws Exception {
		
		int miyawakiNew = parksService.miyawakiNew(Type_of_place, latitude, longitude, zone, ward, location, UserId,
				photo, enumeration_type);

		if (miyawakiNew == 0 || miyawakiNew == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}

		return ResponseEntity.ok("Details were saved successfully");
	}

	@PostMapping("/miyawakiExistingEnumeration")
	public ResponseEntity<String> miyawakiExisting(@RequestParam("latitude") String latitude,
			@RequestParam("longitude") String longitude, @RequestParam("zone") String zone,
			@RequestParam("ward") String ward, @RequestParam(value = "location", required = false) String location,
			@RequestParam("UserId") String UserId, @RequestParam("maintained_By") int maintained_By,
			@RequestParam("Enumeration_Image") MultipartFile photo,
			@RequestParam("enumeration_type") String enumeration_type) throws Exception {
		
		int miyawakiExisting = parksService.miyawakiExisting(maintained_By, latitude, longitude, zone, ward, location,
				UserId, photo, enumeration_type);

		if (miyawakiExisting == 0 || miyawakiExisting == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}

		return ResponseEntity.ok("Details were saved successfully");
	}

	// To fetch park name within 500 meter radius
	@GetMapping("/location")
	public ResponseEntity<List<Map<String, Object>>> location(@RequestParam("latitude") String latitude,
			@RequestParam("longitude") String longitude, @RequestParam("enumeration_type") String enumeration_type) throws Exception {
		List<Map<String, Object>> dataByLocation = parksService.getDateByLocation(latitude, longitude,enumeration_type);
		
		if (dataByLocation == null || dataByLocation.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(dataByLocation);		

	}

	/*
	 * // To fetch park name with reading zone available
	 * 
	 * @PostMapping("/parkname") public ResponseEntity<List<Map<String, Object>>>
	 * parkname() throws Exception { List<Map<String, Object>> parkName =
	 * parksService.parkName();
	 * 
	 * return ResponseEntity.ok(parkName); }
	 */

	@GetMapping("/parkname")
	public ResponseEntity<List<Map<String, Object>>> parkname(@RequestParam("latitude") String latitude,
			@RequestParam("longitude") String longitude) throws Exception {

		List<Map<String, Object>> dataByLocation = parksService.getReadingZoneParkByLocation(latitude, longitude,
				latitude);

		return ResponseEntity.ok(dataByLocation);

	}

	// -----------------------------------------------Feedback
	// API-----------------------------------------------------------------

	@PostMapping("/centerMedianFeedback")
	public ResponseEntity<String> centerMedian(@RequestParam("irrigationProperlyQa") String irrigationProperlyQa,
			@RequestParam("irrigationProperlyAns") String irrigationProperlyAns,
			@RequestParam("trimmingProperlyQa") String trimmingProperlyQa,
			@RequestParam("trimmingProperlyAns") String trimmingProperlyAns,
			@RequestParam("weedingQa") String weedingQa, @RequestParam("weedingans") String weedingAns,
			@RequestParam("HandRailingQa") String HandRailingQa, @RequestParam("HandRailingAns") String HandRailingAns,
			@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude,
			@RequestParam("zone") String zone, @RequestParam("ward") String ward,
			@RequestParam("location") String location, @RequestParam("enumeration_id") String enumeration_id,
			@RequestParam("enumeration_type") String enumeration_type, @RequestParam("userId") String userId,
			@RequestParam("centerMedian_Image") MultipartFile photo) throws Exception {
		
		int centerMedian = parksService.centerMedian(irrigationProperlyQa, irrigationProperlyAns, trimmingProperlyQa,
				trimmingProperlyAns, weedingQa, weedingAns, HandRailingQa, HandRailingAns, latitude, longitude, zone,
				ward, location, enumeration_type, userId, enumeration_id, photo);

		if (centerMedian == 0 || centerMedian == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}

		return ResponseEntity.ok("Details were saved successfully");
	}
/*// Patch 23-11-2024
	@PostMapping("/trafficIslandFeedback")
	public ResponseEntity<String> trafficIsland(
			// gap filling meter
			@RequestParam("irrigationProperlyQa") String irrigationProperlyQa,
			@RequestParam("irrigationProperlyAns") String irrigationProperlyAns,
			@RequestParam("trimmingProperlyQa") String trimmingProperlyQa,
			@RequestParam("trimmingProperlyAns") String trimmingProperlyAns,
			@RequestParam("weedingQa") String weedingQa, @RequestParam("weedingans") String weedingans,
			@RequestParam("HandRailingQa") String HandRailingQa, @RequestParam("HandRailingAns") String HandRailingAns,
			@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude,
			@RequestParam("zone") String zone, @RequestParam("ward") String ward,
			@RequestParam("location") String location, @RequestParam("userId") String userId,
			@RequestParam("enumerationi_id") String enumerationi_id,
			@RequestParam("enumeration_type") String enumeration_type, @RequestParam("gapFilingQa") String gapFillingQa,
			@RequestParam("gapFilingAns") String gapFilingAns, @RequestParam("improvementQa") String improvementQa, // ask
			@RequestParam("improvementAns") String improvementAns, // ask
			@RequestParam("trafficIsland_Image") MultipartFile photo

	) throws Exception {
		
		int trafficIsland = parksService.trafficIsland(irrigationProperlyQa, irrigationProperlyAns, trimmingProperlyQa,
				trimmingProperlyAns, weedingQa, weedingans, HandRailingQa, HandRailingAns, latitude, longitude, zone,
				ward, location, userId, enumeration_type, enumerationi_id, gapFillingQa, gapFilingAns, improvementQa,
				improvementAns, photo);

		if (trafficIsland == 0 || trafficIsland == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}
		return ResponseEntity.ok("Details were saved successfully");
	}
*/
	@PostMapping("/trafficIslandFeedback")
	public ResponseEntity<String> trafficIsland(
			// gap filling meter
			@RequestParam("irrigationProperlyQa") String irrigationProperlyQa,
			@RequestParam("irrigationProperlyAns") String irrigationProperlyAns,
			@RequestParam("trimmingProperlyQa") String trimmingProperlyQa,
			@RequestParam("trimmingProperlyAns") String trimmingProperlyAns,
			@RequestParam("weedingQa") String weedingQa, @RequestParam("weedingans") String weedingans,
			@RequestParam("HandRailingQa") String HandRailingQa, @RequestParam("HandRailingAns") String HandRailingAns,
			@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude,
			@RequestParam("zone") String zone, @RequestParam("ward") String ward,
			@RequestParam("location") String location, @RequestParam("userId") String userId,
			@RequestParam("enumerationi_id") String enumerationi_id,
			@RequestParam("enumeration_type") String enumeration_type, @RequestParam("gapFilingQa") String gapFillingQa,
			@RequestParam("gapFilingAns") String gapFilingAns, @RequestParam("improvementQa") String improvementQa, // ask
			@RequestParam("improvementAns") String improvementAns, // ask
			@RequestParam("fountainAvailableQa") String fountainAvailableQa,
			@RequestParam("fountainAvailableAns") String fountainAvailableAns,
			@RequestParam("fountainWorkingQa") String fountainWorkingQa, // ask
			@RequestParam("fountainWorkingAns") String fountainWorkingAns,
			@RequestParam("trafficIsland_Image") MultipartFile photo

	) throws Exception {
		
		int trafficIsland = parksService.trafficIsland(irrigationProperlyQa, irrigationProperlyAns, trimmingProperlyQa,
				trimmingProperlyAns, weedingQa, weedingans, HandRailingQa, HandRailingAns, latitude, longitude, zone,
				ward, location, userId, enumeration_type, enumerationi_id, gapFillingQa, gapFilingAns, improvementQa,
				improvementAns, fountainAvailableQa, fountainAvailableAns,fountainWorkingQa,fountainWorkingAns, photo);

		if (trafficIsland == 0 || trafficIsland == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}
		return ResponseEntity.ok("Details were saved successfully");
	}

	@PostMapping("/beautificationFeedback")
	public ResponseEntity<String> beautification(
			@RequestParam("irrigationProperlyQa") String irrigationProperlyQa,
			@RequestParam("irrigationProperlyAns") String irrigationProperlyAns,
			@RequestParam("trimmingProperlyQa") String trimmingProperlyQa,
			@RequestParam("trimmingProperlyAns") String trimmingProperlyAns,
			@RequestParam("weedingQa") String weedingQa, @RequestParam("weedingans") String weedingans,
			@RequestParam("gapFillingQa") String gapFillingQa, @RequestParam("gapFillingAns") String gapFillingAns,
			@RequestParam("improvementQa") String improvementQa, @RequestParam("improvementAns") String improvementAns,
			@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude,
			@RequestParam("zone") String zone, @RequestParam("ward") String ward,
			@RequestParam("location") String location, @RequestParam("beautification_Image") MultipartFile photo, // image
			@RequestParam("enumerationi_id") String enumerationi_id, @RequestParam("userId") String userId,
			@RequestParam("enumeration_type") String enumeration_type) throws Exception {
		
		int beautification = parksService.beautification(irrigationProperlyQa, irrigationProperlyAns,
				trimmingProperlyQa, trimmingProperlyAns, weedingQa, weedingans, gapFillingQa, gapFillingAns,
				improvementQa, improvementAns, latitude, longitude, zone, ward, location, enumerationi_id, userId,
				enumeration_type, photo);

		if (beautification == 0 || beautification == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}

		return ResponseEntity.ok("Details were saved successfully");
	}

	@PostMapping("/parkInspectionFeedback")
	public ResponseEntity<String> prakInspection(@RequestParam("openedQa") String openedQa,
			@RequestParam("openedAns") String openedAns,
			@RequestParam("irrigationProperlyQa") String irrigationProperlyQa,
			@RequestParam("irrigationProperlyAns") String irrigationProperlyAns,
			@RequestParam("trimmingProperlyQa") String trimmingProperlyQa,
			@RequestParam("trimmingProperlyAns") String trimmingProperlyAns,
			@RequestParam("weedingQa") String weedingQa, @RequestParam("weedingans") String weedingans,
			@RequestParam("gapFillingQa") String gapFillingQa, @RequestParam("gapFillingAns") String gapFillingAns,
			@RequestParam("playandGymQa") String playandGymQa, @RequestParam("playandGymAns") String playandGymAns,
			@RequestParam("overAllQa") String overAllQa, @RequestParam("overAllans") String overAllAns,
			@RequestParam("properLightQa") String properLightQa, @RequestParam("properLightAns") String properLightAns,
			@RequestParam("debrisRemovalQa") String debrisRemovalQa,
			@RequestParam("debrisRemovalAns") String debrisRemovalAns, @RequestParam("ToiletQA") String ToiletAQA,
			@RequestParam("ToiletAns") String ToiletAns, @RequestParam("latitude") String latitude,
			@RequestParam("longitude") String longitude, @RequestParam("zone") String zone,
			@RequestParam("ward") String ward, @RequestParam("location") String location,
			@RequestParam("enumerationi_id") String enumerationi_id, @RequestParam("userId") String userId,
			@RequestParam("enumeration_type") String enumeration_type,
			@RequestParam("prakInspection_Image") MultipartFile photo, @RequestParam("CivilWorkQa") String CivilWorkQa,
			@RequestParam("CivilWorkAns") String CivilWorkAns, // type
			@RequestParam("NoWatchmenQa") String NoWatchmenQa, @RequestParam("NoWatchmenAns") String NoWatchmenAns, // type
			@RequestParam("NoSweeperQa") String NoSweeperQa, @RequestParam("NoSweeperAns") String NoSweeperAns

	) throws Exception {
		
		int prakInspection = parksService.parkInspection(openedQa, openedAns, irrigationProperlyQa,
				irrigationProperlyAns, trimmingProperlyQa, trimmingProperlyAns, weedingQa, weedingans, gapFillingQa,
				gapFillingAns, playandGymQa, playandGymAns, overAllQa, overAllAns, properLightQa, properLightAns,
				debrisRemovalQa, debrisRemovalAns, ToiletAQA, ToiletAns, latitude, longitude, zone, ward, location,
				userId, enumeration_type, enumerationi_id, CivilWorkQa, CivilWorkAns, NoWatchmenQa, NoWatchmenAns,
				NoSweeperQa, NoSweeperAns, photo);
		
		if (prakInspection == 0 || prakInspection == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}

		return ResponseEntity.ok("Details were saved successfully");
	}

	@PostMapping("/readingZoneFeedback")
	public ResponseEntity<String> readingZone(
			@RequestParam("openedOrNotQa") String openedOrNotQa, @RequestParam("openedOrNotAns") String openedOrNotAns,
			@RequestParam("bookAvailableQa") String bookAvailableQa,
			@RequestParam("bookAvailableAns") String bookAvailableAns,
			@RequestParam("NewsapaperAvailableQa") String NewsapaperAvailableQa,
			@RequestParam("NewsapaperAvailableAns") String NewsapaperAvailableAns,
			@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude,
			@RequestParam("zone") String zone, @RequestParam("ward") String ward,
			@RequestParam("location") String location, @RequestParam("enumerationi_id") String enumerationi_id,
			@RequestParam("userId") String userId, @RequestParam("enumeration_type") String enumeration_type,
			@RequestParam("readingZone_Image") MultipartFile photo,
			@RequestParam("improvementQa") String improvementQa, // type
			@RequestParam("improvementAns") String improvementAns// type

	) throws Exception {
		
		int readingZone = parksService.readingZone(openedOrNotQa, openedOrNotAns, bookAvailableQa, bookAvailableAns,
				NewsapaperAvailableQa, NewsapaperAvailableAns, latitude, longitude, zone, ward, location, userId,
				enumeration_type, enumerationi_id, improvementQa, improvementAns, photo);
		
		if (readingZone == 0 || readingZone == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}

		return ResponseEntity.ok("Details were saved successfully");
	}

	@PostMapping("/miyawakiNewFeedback")
	public ResponseEntity<String> miyawakiNew(
			@RequestParam("debrisCleaningQa") String debrisCleaningQa,
			@RequestParam("debrisCleaningAns") String debrisCleaningAns,
			@RequestParam("landpreprarationQa") String landpreprarationQa,
			@RequestParam("landpreprarationAns") String landpreprarationAns,
			@RequestParam("plantingDoneQa") String plantingDoneQa,
			@RequestParam("plantingDoneAns") String plantingDoneAns, @RequestParam("cctvQa") String cctvQa,
			@RequestParam("cctvAns") String cctvAns, @RequestParam("latitude") String latitude,
			@RequestParam("longitude") String longitude, @RequestParam("zone") String zone,
			@RequestParam("ward") String ward, @RequestParam("location") String location,
			@RequestParam("enumerationi_id") String enumerationi_id, @RequestParam("userId") String userId,
			@RequestParam("enumeration_type") String enumeration_type,
			@RequestParam("SaplingAvailbilityQa") String saplingAvailQa,
			@RequestParam("SaplingAvailbilityAns") String saplingAvailAns,
			@RequestParam("fencingAvailbilityQa") String fencingAvailbilityQa,
			@RequestParam("fencingAvailbilityAns") String fencingAvailbilityAns,
			@RequestParam("sourcaOfIrrigationQa") String sourcaOfIrrigationQa, // type
			@RequestParam("sourcaOfIrrigationAns") String sourcaOfIrrigationAns, // type

			@RequestParam("miyawakiNew_Image") MultipartFile photo) throws Exception {
		
		int miyawakiNew = parksService.miyawakiNew(debrisCleaningQa, debrisCleaningAns, landpreprarationQa,
				landpreprarationAns, plantingDoneQa, plantingDoneAns, cctvQa, cctvAns, latitude, longitude, zone, ward,
				location, userId, enumeration_type, enumerationi_id, saplingAvailQa, saplingAvailAns,
				fencingAvailbilityQa, fencingAvailbilityAns, sourcaOfIrrigationQa, sourcaOfIrrigationAns, photo);
		
		if (miyawakiNew == 0 || miyawakiNew == -1) {
			return ResponseEntity.ok("Failed to save the detailse");
		}

		return ResponseEntity.ok("Details were saved successfully");
	}

	@PostMapping("/miyawakiExisitingFeedback")
	public ResponseEntity<String> miyawakiExisiting(
			@RequestParam("weedingQa") String weedingQa, @RequestParam("weedingAns") String weedingAns,
			@RequestParam("purningQa") String purningQa, @RequestParam("purningans") String purningans,
			@RequestParam("gapFillingQa") String gapFillingQa, @RequestParam("gapFillingAns") String gapFillingAns,
			@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude,
			@RequestParam("zone") String zone, @RequestParam("ward") String ward,
			@RequestParam("location") String location, @RequestParam("enumerationi_id") String enumerationi_id,
			@RequestParam("userId") String userId, @RequestParam("enumeration_type") String enumeration_type,
			@RequestParam("improvmetReqQa") String improvmetReqQa,
			@RequestParam("improvmetReqAns") String improvmetReqAns,
			@RequestParam("sourceOfirrigationQa") String sourceOfirrigationQa,
			@RequestParam("sourceOfirrigationAns") String sourceOfirrigationAns,
			@RequestParam("miyawakiExisiting_Image") MultipartFile photo) throws Exception {

		int miyawakiExisiting = parksService.miyawakiExisiting(weedingQa, weedingAns, purningQa, purningans,
				gapFillingQa, gapFillingAns, latitude, longitude, zone, ward, location, userId, enumeration_type,
				enumerationi_id, improvmetReqQa, improvmetReqAns, sourceOfirrigationQa, sourceOfirrigationAns, photo);
		
		if (miyawakiExisiting == 0 || miyawakiExisiting == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}

		return ResponseEntity.ok("Details were saved successfully");
	}

	// To fetch Category ID and Name for reports

	@GetMapping("/enuumerationType")
	public ResponseEntity<List<Map<String, Object>>> enuumerationType() {
		List<Map<String, Object>> parkName = parksService.categoryType();
		return ResponseEntity.ok(parkName);
	}

	// To fetch master category type based on enumeration type
	@GetMapping("/masterCategoryType")
	public ResponseEntity<List<Map<String, Object>>> masterCategoryType(
			@RequestParam("enumeration_type_id") String Cat_enumeration_id) {
		List<Map<String, Object>> parkName = parksService.masterCategoryType(Cat_enumeration_id);
		return ResponseEntity.ok(parkName);
	}

	// To fetch sub category type based on master category type
	@GetMapping("/subCategoryType")
	public ResponseEntity<List<Map<String, Object>>> subCategoryType(
			@RequestParam("master_category_id") String masterCategory) {

		List<Map<String, Object>> parkName = parksService.subCategoryType(masterCategory);
		return ResponseEntity.ok(parkName);
	}

	// ------------------------------------------Tree Feedback
	// API-----------------------------------------------------

	@PostMapping("/treeDetails")
	public ResponseEntity<Map<String, Object>> SaveTreeDetailes(
			@RequestParam("No_of_trees_required") int No_of_trees_required,
			@RequestParam("enumeration_type") String enumeration_type, @RequestParam("tree_Image") MultipartFile tree_Image,
			@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude,
			@RequestParam("zone") String zone, @RequestParam("ward") String ward,
			@RequestParam("location") String location, @RequestParam("parkname") String parkname,
			// @RequestParam(value = "painting_status", required = false) String painting_status,
			@RequestParam("userId") String userId) throws IOException {

		Map<String, Object> saveTreeDetailes = parksService.SaveTreeDetailes(No_of_trees_required, enumeration_type,
				latitude, longitude, zone, ward, location, parkname, userId, tree_Image);
		return ResponseEntity.ok(saveTreeDetailes);

	}

	@PostMapping("/SaveTreeimage")
	public ResponseEntity<Map<String, Object>> SaveTreeimage(@RequestParam("tree_id") int tree_id,
			@RequestParam("enumeration_type") String enumeration_type, @RequestParam("tree_Image") MultipartFile tree_Image,
			@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude,
			// @RequestParam("enumeration_type") String enumeration_type,
			@RequestParam("userId") String userId) throws Exception {
		
		Map<String, Object> saveTreeDetailes = parksService.SaveimageDetailes(enumeration_type, latitude, longitude,
				userId, tree_Image, tree_id);
		return ResponseEntity.ok(saveTreeDetailes);

	}

	// ------------------------------------------Enumeration Report
	// API-----------------------------------------------------

	@GetMapping("/zoneCount") // UserId added
	public ResponseEntity<List<Map<String, Object>>> getZoneCount(@RequestParam("fromdate") String fromdate,
			@RequestParam("todate") String todate, @RequestParam("enumeration_type") String enumeration_type,
			@RequestParam(value = "userId", required = false) String userId) throws Exception {
		
		List<Map<String, Object>> zoneCountData = parksService.getZoneCount(fromdate, todate, enumeration_type, userId);
		if (zoneCountData == null || zoneCountData.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(zoneCountData);

	}

	@GetMapping("/wardCount") // Zone added
	public ResponseEntity<List<Map<String, Object>>> getwardCount(@RequestParam("zone") String zone,
			@RequestParam(value = "userId", required = false) String userId, @RequestParam("fromdate") String fromdate,
			@RequestParam("todate") String todate, @RequestParam("enumeration_type") String enumeration_type)
			throws Exception {	
		
		List<Map<String, Object>> wardCountData = parksService.getwardCount(zone, fromdate, todate, enumeration_type, userId);
		if (wardCountData == null || wardCountData.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(wardCountData);

	}

	@GetMapping("/activityCount") // userId and Zone added
	public ResponseEntity<List<Map<String, Object>>> getActivityCount(@RequestParam("ward") String ward,
			@RequestParam("fromdate") String fromdate, @RequestParam("todate") String todate,
			@RequestParam("enumeration_type") String enumeration_type, @RequestParam(value="userId", required = false) String userId,
			@RequestParam("zone") String zone) throws Exception {
		
		List<Map<String, Object>> activityByWard = parksService.getActivityCount(ward, fromdate, todate,
				enumeration_type, zone, userId);
		if (activityByWard == null || activityByWard.isEmpty()) {
			throw new NoDataException("No Activity is performed ");
		}
		return ResponseEntity.ok(activityByWard);

	}

	@GetMapping("/activityDetails") // userId and Zone added
	public ResponseEntity<List<Map<String, Object>>> getActivityDetails(@RequestParam("ward") String ward,
			@RequestParam("fromdate") String fromdate, @RequestParam("todate") String todate,
			@RequestParam("enumeration_type") String enumeration_type,@RequestParam("enumerationId") String enumerationId,
			@RequestParam(value="userId", required= false) String userId, @RequestParam("zone") String zone) throws Exception {
		
		List<Map<String, Object>> activityDetailsByWard = parksService.getActivityDetails(ward, fromdate, todate,
				enumeration_type, enumerationId, zone, userId);
		if (activityDetailsByWard == null || activityDetailsByWard.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(activityDetailsByWard);

	}

	@GetMapping("/getInspectionDetails") // zone, ward and userId added check it in last
	public ResponseEntity<List<List<Map<String, Object>>>> getInspectionDetails(
			@RequestParam("activityId") String activityId, @RequestParam("fromdate") String fromdate,
			@RequestParam("todate") String todate, @RequestParam("enumerationId") String enumerationId, 
			@RequestParam(value="userId", required =  false) String userId )
			throws Exception {
		
		List<List<Map<String, Object>>> activityDetailsByWard = parksService.getInspectionDetails(activityId, fromdate,
				todate, enumerationId);
		if (activityDetailsByWard == null || activityDetailsByWard.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(activityDetailsByWard);
		
	}

	// tofxcvbn
	@GetMapping("/getParkCount")
	public ResponseEntity<List<Map<String, Object>>> getParkCount(@RequestParam("fromdate") String fromdate,
			@RequestParam("todate") String todate, @RequestParam("ward") String ward,
			@RequestParam("userId") String userId, @RequestParam("enumeration_type") String enumeration_type)
			throws Exception {
		
		try {
			List<Map<String, Object>> dateByLocation = parksService.getParkCount(ward, fromdate, todate,
					enumeration_type);

			if (dateByLocation == null || dateByLocation.isEmpty()) {
				throw new NoDataException("no data ");
			} else {
				return ResponseEntity.ok(dateByLocation);
			}
		} catch (NoDataException e) {
			// TODO Auto-generated catch block
			throw new NoDataException("no data");
		}

	}

	@GetMapping("/getParkDetails")
	public ResponseEntity<List<List<Map<String, Object>>>> getParkDetails(
			@RequestParam(value = "enumeration_type", required = false) String enumeration_type,
			@RequestParam("userId") String userId, @RequestParam("fromdate") String fromdate,
			@RequestParam("todate") String todate) throws Exception {
		
		List<List<Map<String, Object>>> dateByLocation = parksService.getParkDetails(enumeration_type, fromdate, todate, userId);
		if (dateByLocation == null || dateByLocation.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(dateByLocation);

	}

	//////////////////// ------------------------------tree-------------------///////////////////////////

	@GetMapping("/treeWishZoneCount")
	public ResponseEntity<List<Map<String, Object>>> getWishtreeZoneCount(@RequestParam("fromdate") String fromdate,
			@RequestParam("todate") String todate, @RequestParam("enumeration_type") String enumeration_type,
			@RequestParam("userId") String userId) throws Exception {
		
		List<Map<String, Object>> dateByLocation = parksService.getWishtreeZoneCount(fromdate, todate,
				enumeration_type);
		if (dateByLocation == null || dateByLocation.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(dateByLocation);

	}

	// to
	@GetMapping("/treeWardCount")
	public ResponseEntity<List<Map<String, Object>>> getTreewardCount(@RequestParam("fromdate") String fromdate,
			@RequestParam("todate") String todate, @RequestParam("zone") String zone,
			@RequestParam("userId") String userId, @RequestParam("enumeration_type") String enumeration_type)
			throws Exception {
		
		List<Map<String, Object>> dateByLocation = parksService.getTreewardCount(zone, fromdate, todate,
				enumeration_type);
		if (dateByLocation == null || dateByLocation.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(dateByLocation);

	}

	@GetMapping("/wardWishTreedata")
	public ResponseEntity<List<Map<String, Object>>> getwardWishTreedata(@RequestParam("ward") String ward,
			@RequestParam("userId") String userId) throws Exception {
		
		List<Map<String, Object>> dateByLocation = parksService.getwardWishTreedata(ward);
		if (dateByLocation == null || dateByLocation.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(dateByLocation);

	}

	@GetMapping("/getTreeWardWiseCountDetails")
	public ResponseEntity<List<Map<String, Object>>> getTreeWardWiseCountDetails(@RequestParam("ward") String ward,
			@RequestParam("enumeration_type") String enumeration_type,@RequestParam("fromdate") String fromdate,
			@RequestParam("todate") String todate) throws Exception {
		
		List<Map<String, Object>> treeCountByWard = parksService.getTreeWardWiseCountDetails(ward,enumeration_type,fromdate,todate);
		if (treeCountByWard == null || treeCountByWard.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(treeCountByWard);

	}
	
	@GetMapping("/getTreeImageDetails")
	public ResponseEntity<List<Map<String, Object>>> getTreeImageDetails(@RequestParam("tree_Id") String tree_Id,
			@RequestParam("enumeration_type") String enumeration_type, @RequestParam("fromdate") String fromdate,
			@RequestParam("todate") String todate) throws Exception {
		
		List<Map<String, Object>> treeImageDetails = parksService.getTreeImageDetails(tree_Id,enumeration_type, fromdate, todate);
		if (treeImageDetails == null || treeImageDetails.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(treeImageDetails);

	}
	
	@GetMapping("/getTreeDetails")
	public ResponseEntity<List<Map<String, Object>>> getTreeDetails(@RequestParam("tree_Id") String tree_Id,
			@RequestParam("userId") String userId) throws Exception {
		
		List<Map<String, Object>> dateByLocation = parksService.getTreeDetails(tree_Id);
		if (dateByLocation == null || dateByLocation.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(dateByLocation);

	}

	// feedback

	@GetMapping("/enumerationFeedback")
	public ResponseEntity<List<Map<String, Object>>> getEnumerationFeedback(
			@RequestParam("enumerationId") String enumerationId,
			@RequestParam("enumeration_type") String enumeration_type) throws Exception {

		List<Map<String, Object>> feedbackData = parksService.getEnumeration_feedback(enumerationId, enumeration_type);

		if (feedbackData == null || feedbackData.isEmpty()) {
			throw new NoDataException("No feedback data found.");
		}

		return ResponseEntity.ok(feedbackData);
	}

	// To Get Brief Report
	@GetMapping("/getBriefReport")
	public ResponseEntity<List<Map<String, Object>>> getBriefReport(@RequestParam("fromdate") String fromdate,
			@RequestParam("todate") String todate, @RequestParam("enumeration_type") String category,
			@RequestParam("division") String divison, @RequestParam("zone") String zone,
			@RequestParam("park_name") String park_name) throws Exception {
		
		List<Map<String, Object>> briefReport = parksService.getBriefReport(fromdate, todate, category, divison, zone,
				park_name);
		if (briefReport == null || briefReport.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(briefReport);

	}

	@GetMapping("/getUserBasedEnumAndStatus")
	public ResponseEntity<List<Map<String, Object>>> getUserBasedEnum(@RequestParam("userId") String tree_user_id,
			@RequestParam("latitude") String tree_latitude, @RequestParam("longitude") String tree_longitude,
			@RequestParam("Status") String tree_Status, @RequestParam("enumeration_type") String enumeration_type) throws Exception {
		
		List<Map<String, Object>> userBasedEnum = parksService.getUserBasedEnum(tree_user_id, tree_latitude,
				tree_longitude, tree_Status, enumeration_type);
		if (userBasedEnum == null || userBasedEnum.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(userBasedEnum);
	}

//	@GetMapping("/getEnumByRadiusAndStatus")
//	public ResponseEntity<List<Map<String, Object>>> getUserBasedEnum(
//			@RequestParam("latitude") String tree_latitude,@RequestParam("longitude") String tree_longitude,@RequestParam("Status") String tree_Status
//		)
//			{
//				
//		List<Map<String, Object>> userBasedEnum = parksService.getUserBasedEnum(tree_latitude,tree_longitude,tree_Status);
//		
//		if (userBasedEnum == null || userBasedEnum.isEmpty()) {
//			throw new NoDataException("no data ");
//		}
//		return 	 ResponseEntity.ok(userBasedEnum);
//			}

	@GetMapping("/getparkNameByRadius")
	public ResponseEntity<List<Map<String, Object>>> getparkNameByRadius(@RequestParam("latitude") String tree_latitude,
			@RequestParam("longitude") String tree_longitude) {

		List<Map<String, Object>> getparkNameByRadius = parksService.getparkNameByRadius(tree_latitude, tree_longitude);
		if (getparkNameByRadius == null || getparkNameByRadius.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(getparkNameByRadius);

	}

	@GetMapping("/getTreeDetailsBasedonLocation")
	public ResponseEntity<List<Map<String, Object>>> getTreeDetailsBasedonLocation(
			@RequestParam("parkName") String parkName, @RequestParam("location") String location,
			@RequestParam("ward") String ward) throws Exception {
		
		List<Map<String, Object>> treeDataByLocation = parksService.getTreeDetailsBasedonLocation(parkName, location,
				ward);
		if (treeDataByLocation == null || treeDataByLocation.isEmpty()) {
			throw new NoDataException("no data ");
		}
		return ResponseEntity.ok(treeDataByLocation);

	}
	
	// flowering and Non Flowering API
	
	@GetMapping("/getparkNameByEnumType")
	public ResponseEntity<List<Map<String,Object>>> getparkNameByEnumType(@RequestParam("enum_Type") int enum_Type,
			@RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude){
		
		List<Map<String,Object>> parkNameByEnum = parksService.getparkNameByEnumType(enum_Type,latitude,longitude);
		
		if(parkNameByEnum == null || parkNameByEnum.isEmpty())
		{
			throw new NoDataException("No Data found");
		}else {
			return ResponseEntity.ok(parkNameByEnum);
		}
		
	}

	
	@PostMapping("/saveFloweringandNonFlowering")
	public ResponseEntity<String> saveFloweringandNonFlowering(
			@RequestParam("enumType") String enumType,@RequestParam("parkname") String parkname, @RequestParam("userId") String userId,
			@RequestParam("enumerationId") int enumerationId, @RequestParam(value="floweringPlantCount", required = false) int floweringPlantCount,
			@RequestParam(value="nonFloweringPlantCount", required = false) int nonFloweringPlantCount, 
			@RequestParam(value="floweringImage", required = false) MultipartFile floweringImg,
			@RequestParam(value="nonFloweringImage", required = false) MultipartFile nonFloweringImg) throws Exception {
		
		int flowringData = parksService.saveFlowringandNonFlowringDetails(enumerationId, enumType,parkname, floweringPlantCount, floweringImg, 
				nonFloweringPlantCount, nonFloweringImg, userId);
		
		if (flowringData == 0 || flowringData == -1) {
			return ResponseEntity.ok("Failed to save the details");
		}

		return ResponseEntity.ok("Details were saved successfully");		

	}

	
	@GetMapping("/getFlowringZoneCount")
	public ResponseEntity<List<Map<String,Object>>> getFlowringZoneData(@RequestParam("enumeration_type") String enumeration_type,
			@RequestParam("fromdate") String fromdate, @RequestParam("todate") String todate, 
			@RequestParam(value="userId", required = false) String userId){
		
		List<Map<String, Object>> floweringZoneCount = parksService.getFloweringZoneCount(enumeration_type,fromdate,todate,userId);
		
		if(floweringZoneCount == null || floweringZoneCount.isEmpty()) {
			throw new NoDataException("No data found");
		}
		
		return ResponseEntity.ok(floweringZoneCount);
	}

	
	@GetMapping("/getFlowringWardCount")
	public ResponseEntity<List<Map<String,Object>>> getFlowringWardData(@RequestParam("enumeration_type") String enumeration_type,
			@RequestParam("fromdate") String fromdate, @RequestParam("todate") String todate, 
			@RequestParam(value="userId", required = false) String userId, @RequestParam("zone") String zone){
		
		List<Map<String, Object>> floweringWardCount = parksService.getFloweringWardCount(zone, fromdate, todate, enumeration_type, userId);
		
		if (floweringWardCount == null || floweringWardCount.isEmpty()) {
			throw new NoDataException("No data found");
		}
		return ResponseEntity.ok(floweringWardCount);
	}

	
	@GetMapping("/getFloweringDetails")
	public ResponseEntity<List<Map<String,Object>>> getFloweringDetails(@RequestParam("enumeration_type") String enumeration_type,
			@RequestParam("zone") String zone, @RequestParam("ward") String ward, @RequestParam("fromdate") String fromdate,
			@RequestParam("todate") String todate, @RequestParam(value="userId" , required = false) String userId, 
			@RequestParam("parkname") String parkname){
		
		List<Map<String,Object>> floweringDetails = parksService.getFloweringDetails(enumeration_type, zone, ward, fromdate, todate, userId, parkname);
		if(floweringDetails == null || floweringDetails.isEmpty())
		{
			throw new NoDataException( "No data found");
		}
		return ResponseEntity.ok(floweringDetails);
		
	}
	
	@GetMapping("/getParkwiseFloweringCount")
	public ResponseEntity<List<Map<String,Object>>> getParkwiseFloweringCount(@RequestParam("enumeration_type") String enumeration_type,
			@RequestParam("zone") String zone, @RequestParam("ward") String ward, @RequestParam("fromdate") String fromdate,
			@RequestParam("todate") String todate, @RequestParam(value="userId", required = false) String userId){
		
				
		List<Map<String,Object>> parkwiseCount = parksService.getParkwiseFloweringCount(enumeration_type, zone, ward, fromdate, todate, userId);
		
		if(parkwiseCount == null || parkwiseCount.isEmpty())
		{
			throw new NoDataException( "No data found");
		}
		return ResponseEntity.ok(parkwiseCount);
		
	}
	
	@GetMapping("/getComplaintList")
    public ResponseEntity<List<Map<String, Object>>> getComplaintList(@RequestParam("userid") String userid) {

        List<Map<String, Object>> complaintListforFeedback = parksService.getComplaintListforFeedback(userid);

        if (complaintListforFeedback == null || complaintListforFeedback.isEmpty()) {
            throw new NoDataException("No data found");

        }
        return ResponseEntity.ok(complaintListforFeedback);
    }

	@GetMapping("/confirmLocation")
    public ResponseEntity<Map<String, Object>> confirmLocation(@RequestParam("compId") String compId,
                                                               @RequestParam("latitude") String latitude,
                                                               @RequestParam("longitude") String longitude) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean locationStatus = parksService.confirmLocationforComplaint(compId, latitude, longitude);

            response.put("statusCode", HttpStatus.OK.value());
            response.put("message", locationStatus ? "true" : "false");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.put("message", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
	
	@PostMapping("/updateComplaint")
    public ResponseEntity<Map<String, Object>> updateComplaint(@RequestParam("compId") String compId, @RequestParam(value = "remarks", required = false) String remarks,
                                                               @RequestParam("compImg") MultipartFile compImg, @RequestParam("userid") String userid,
                                                               @RequestParam("latitude") String latitude, @RequestParam("longitude") String longitude) throws Exception {

        Map<String, Object> response = new HashMap<>();

        if (remarks == null || remarks.isEmpty()) {
            remarks = "N/A";
        }
        try {

            List<Map<String, Object>> updateComplaint = parksService.updateComplaintDetails(compId, remarks, compImg, latitude, longitude, userid);

            response.put("statusCode", HttpStatus.OK.value());
            response.put("message", updateComplaint.size() > 0 ? "Details were saved successfully" : "Error in saving details");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.put("message", "Error in saving details");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
	
	@GetMapping("/getZoneComplaintList")
    public ResponseEntity<List<Map<String, Object>>> getZoneComplaintList(@RequestParam("loginId") String loginId) {

        List<Map<String, Object>> complaintListforFeedback = parksService.getZoneComplaintListforFeedback(loginId);

        if (complaintListforFeedback == null || complaintListforFeedback.isEmpty()) {
            throw new NoDataException("No data found");

        }
        return ResponseEntity.ok(complaintListforFeedback);
    }

}
