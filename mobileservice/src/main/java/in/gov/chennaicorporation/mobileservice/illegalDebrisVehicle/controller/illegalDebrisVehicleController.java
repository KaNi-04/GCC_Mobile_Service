package in.gov.chennaicorporation.mobileservice.illegalDebrisVehicle.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.illegalDebrisVehicle.service.illegalDebrisVehicleService;

@RequestMapping("/gccofficialapp/api/illegaldebrisvehicle")
@RestController("gccofficialappillegaldebrisvehicle")
public class illegalDebrisVehicleController {

    @Autowired
    illegalDebrisVehicleService illegaldebrisvehicleService;

    @RequestMapping("/getVehicleTypeList")
    public List<Map<String, Object>> getVehicleTypeList() {
        return illegaldebrisvehicleService.getVehicleTypeList();
    }

    @RequestMapping("/getWasteTypeList")
    public List<Map<String, Object>> getWasteTypeList() {
        return illegaldebrisvehicleService.getWasteTypeList();
    }

    @PostMapping("/saveCatchVehicleData")
    public List<Map<String, Object>> saveCatchVehicleData(@RequestParam String zone, @RequestParam String ward,
            @RequestParam String LoginId, @RequestParam String latitude, @RequestParam String longitude,
            @RequestParam String vehicle_no, @RequestParam String place_name, @RequestParam String tonage,
            @RequestParam int vehicle_type, @RequestParam int waste_type,
            @RequestParam String remarks, @RequestParam MultipartFile Image) {
        return illegaldebrisvehicleService.saveCatchVehicleData(zone, ward, LoginId, latitude, longitude, vehicle_no,
                place_name, tonage, vehicle_type, waste_type, remarks, Image);
    }

    @GetMapping("/getCaughtVehicleList")
    public ResponseEntity<List<Map<String, Object>>> getCaughtVehicleList(@RequestParam String LoginId) {
        List<Map<String, Object>> caughtVehicleList = illegaldebrisvehicleService.getComplaintList(LoginId);
        return ResponseEntity.ok(caughtVehicleList);
    }

    @PostMapping("/createChallanStep3")
    public ResponseEntity<List<Map<String, Object>>> step3(
            @RequestParam String vclid,
            @RequestParam(required = false) String latitude,
            @RequestParam(required = false) String longitude,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String streeId,
            @RequestParam(required = false) String streetName,
            @RequestParam String loginId,
            @RequestParam(required = false) String remarks,
            @RequestParam String violatorName,
            @RequestParam String violatorPhone,
            @RequestParam(required = false) String vehicle_no,
            @RequestParam(required = false) MultipartFile file) {

        List<Map<String, Object>> result = illegaldebrisvehicleService.step3(
                vclid,
                latitude,
                longitude,
                zone,
                ward,
                streeId,
                streetName,
                loginId,
                remarks,
                violatorName,
                violatorPhone,
                vehicle_no,
                file);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/getChallanPOSData")
    public ResponseEntity<List<Map<String, Object>>> getChallanPOSData(
            @RequestParam(required = false) String orderid,
            @RequestParam(required = false) String loginid,
            @RequestParam(required = false) String vehicleNo) {
        List<Map<String, Object>> result = illegaldebrisvehicleService.getChallanPOSData( orderid, loginid, vehicleNo);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/updateSerialNumber")
    public ResponseEntity<List<Map<String, Object>>> updateSerialNumber(
            @RequestParam String tid,
            @RequestParam String mid,
            @RequestParam String serialNumber,
            @RequestParam String orderid) {
        List<Map<String, Object>> result = illegaldebrisvehicleService.updateSerialNumber(null, tid, mid, serialNumber,
                orderid);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/storeBankTransaction")
    public ResponseEntity<List<Map<String, Object>>> storeBankTransaction(
            @RequestBody Map<String, Object> transactionData) {
        List<Map<String, Object>> result = illegaldebrisvehicleService.storeBankTransaction(transactionData);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/storeFailedBankTransaction")
    public ResponseEntity<List<Map<String, Object>>> storeFailedBankTransaction(
            @RequestBody Map<String, Object> transactionData) {
        List<Map<String, Object>> result = illegaldebrisvehicleService.storeFailedBankTransaction(transactionData);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getChallanPaidList")
    public ResponseEntity<List<Map<String, Object>>> step3list(@RequestParam String loginid) {
        List<Map<String, Object>> result = illegaldebrisvehicleService.getChallanPaidList( loginid);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/saveClose")
    public ResponseEntity<List<Map<String, Object>>> saveClose(
            @RequestParam String vclid,
            @RequestParam String zone,
            @RequestParam String ward,
            @RequestParam String latitude,
            @RequestParam String longitude,
            @RequestParam MultipartFile file,
            @RequestParam String remarks,
            @RequestParam String cby) {
        List<Map<String, Object>> result = illegaldebrisvehicleService.saveClose(vclid, zone, ward, latitude, longitude,
                file, remarks, cby);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getZoneSummary")
    public ResponseEntity<List<Map<String, Object>>> getZoneSummary(
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        List<Map<String, Object>> result = illegaldebrisvehicleService.getZoneSummary(fromDate, toDate);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getWardSummary")
    public ResponseEntity<List<Map<String, Object>>> getWardSummary(
            @RequestParam String zone,
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        List<Map<String, Object>> result = illegaldebrisvehicleService.getWardSummary(zone, fromDate, toDate);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getPendingSummary")
    public ResponseEntity<List<Map<String, Object>>> getPendingSummary(
            @RequestParam String ward,
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        List<Map<String, Object>> result = illegaldebrisvehicleService.getPendingSummary(ward, fromDate, toDate);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getCloseSummary")
    public ResponseEntity<List<Map<String, Object>>> getCloseSummary(
            @RequestParam String ward,
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        List<Map<String, Object>> result = illegaldebrisvehicleService.getCloseSummary(ward, fromDate, toDate);
        return ResponseEntity.ok(result);
    }

    // to fetch vehicleDetails based on zone, ward and vehicle no
    @GetMapping("/getVehicleData")
    public ResponseEntity<Map<String, Object>> getVehicleData(
            @RequestParam String zone,
            @RequestParam String ward,
            @RequestParam String vehicle_no) {
        Map<String, Object> result = illegaldebrisvehicleService.getVehicleData(zone, ward, vehicle_no);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/submitDumpDetails")
    public List<Map<String, Object>> submitDumpDetails(
            @RequestParam String vclid,
            @RequestParam String zone,
            @RequestParam String ward,
            @RequestParam String latitude,
            @RequestParam String longitude,
            @RequestParam MultipartFile dumpImage,
            @RequestParam String remarks,
            @RequestParam String login) {
        return illegaldebrisvehicleService.submitDumpDetails(vclid, zone, ward, latitude, longitude, dumpImage, remarks, login);
    }

    @GetMapping("/getFineAmount")
    public Map<String, Object> getFineAmountDetails(
            @RequestParam String vehicletype,
            @RequestParam String wastetype,
            @RequestParam String tonage) {
        return illegaldebrisvehicleService.getFineAmountDetails(vehicletype, wastetype, tonage);
    }

}
