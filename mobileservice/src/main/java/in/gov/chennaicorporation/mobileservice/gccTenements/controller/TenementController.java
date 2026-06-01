package in.gov.chennaicorporation.mobileservice.gccTenements.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccTenements.service.TenementService;

@RequestMapping("/gccofficialapp/api/tenement")
@RestController("gccofficialapptenement")
public class TenementController {

        @Autowired
        private TenementService tenementService;

        @GetMapping("/getTenementsListByWard")
        public Map<String, Object> getTenementsListByWard(@RequestParam(value = "loginid") String loginid,
                        @RequestParam(value = "type", required = false) String type) {
                return tenementService.getTenementsListByWard(loginid, type);
        }

        @PostMapping("/saveAsset")
        public ResponseEntity<Map<String, Object>> saveAsset(

                        @RequestParam String zone,
                        @RequestParam String ward,
                        @RequestParam String latitude,
                        @RequestParam String longitude,
                        @RequestParam String address,

                        @RequestParam String am_id,
                        @RequestParam String name,
                        @RequestParam String cby,

                        @RequestParam(required = false) MultipartFile image) {

                return ResponseEntity.ok(
                                tenementService.saveAsset(zone, ward, latitude, longitude,
                                                address, am_id, name, cby, image));
        }

        /*
         * @GetMapping("/getAssetListByRadius")
         * public ResponseEntity<List<Map<String, Object>>> getAssetListByRadius(
         * 
         * @RequestParam String latitude,
         * 
         * @RequestParam String longitude) {
         * 
         * return ResponseEntity.ok(
         * tenementService.getAssetListByRadius(
         * latitude,
         * longitude));
         * }
         */
        @GetMapping("/getAssetListByRadius")
        public Map<String, Object> getAssetListByRadius(

                        @RequestParam("latitude") String latitude,
                        @RequestParam("longitude") String longitude) {

                return tenementService.getAssetListByRadius(
                                latitude,
                                longitude);
        }

        @PostMapping("/saveIssue")
        public ResponseEntity<Map<String, Object>> saveIssue(

                        @RequestParam String zone,
                        @RequestParam String ward,
                        @RequestParam String latitude,
                        @RequestParam String longitude,
                        @RequestParam String assetlist_id,
                        @RequestParam MultipartFile before_image,
                        @RequestParam String radius,
                        @RequestParam String am_id,
                        @RequestParam String cby,
                        @RequestParam String remarks) {

                return ResponseEntity.ok(
                                tenementService.saveIssue(
                                                zone,
                                                ward,
                                                latitude,
                                                longitude,
                                                assetlist_id,
                                                before_image,
                                                radius,
                                                am_id,
                                                cby,
                                                remarks));
        }

        @PostMapping("/saveIssueCompletion")
        public Map<String, Object> saveIssueCompletion(

                        @RequestParam String issuelist1_id,
                        @RequestParam String zone,
                        @RequestParam String ward,
                        @RequestParam String latitude,
                        @RequestParam String longitude,
                        @RequestParam String assetlist_id,
                        @RequestParam MultipartFile after_image,
                        @RequestParam String remarks,
                        @RequestParam String radius,
                        @RequestParam String am_id,
                        @RequestParam String cby) {

                return tenementService.saveIssueCompletion(
                                issuelist1_id,
                                zone,
                                ward,
                                latitude,
                                longitude,
                                assetlist_id,
                                after_image,

                                remarks,
                                radius,
                                am_id,
                                cby);
        }

        @PostMapping("/saveIssueVerification")
        public ResponseEntity<Map<String, Object>> saveIssueVerification(

                        @RequestParam String issuelist1_id,
                        @RequestParam String issuelist2_id,
                        @RequestParam String zone,
                        @RequestParam String ward,
                        @RequestParam String latitude,
                        @RequestParam String longitude,
                        @RequestParam String assetlist_id,
                        @RequestParam String remarks,
                        @RequestParam String radius,
                        @RequestParam String am_id,
                        @RequestParam(required = false) MultipartFile verify_image,

                        @RequestParam String cby,
                        @RequestParam(required = false) String status) {

                return ResponseEntity.ok(
                                tenementService.saveIssueVerification(
                                                issuelist1_id,
                                                issuelist2_id,
                                                zone,
                                                ward,
                                                latitude,
                                                longitude,
                                                assetlist_id,
                                                remarks,

                                                radius,
                                                am_id,
                                                verify_image,
                                                cby,status));
        }

        @GetMapping("/getCreatedIssueList")
        public Map<String, Object> getCreatedIssueList(
                        @RequestParam(required = false) String loginid, String ward) {

                return tenementService.getCreatedIssueList(loginid, ward);
        }

        @GetMapping("/getIssueVerificationList")
        public Map<String, Object> getIssueVerificationList(
                        @RequestParam(required = false) String loginid, @RequestParam(required = false) String ward) {

                return tenementService.getIssueVerificationList(loginid, ward);
        }

        @GetMapping("/getZoneWiseReport")
        public Map<String, Object> getZoneWiseReport(

                        @RequestParam String fromDate,
                        @RequestParam String toDate) {

                // ✅ convert dd-MM-yyyy → yyyy-MM-dd
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                String formattedFromDate = LocalDate.parse(fromDate, inputFormatter)
                                .format(outputFormatter);

                String formattedToDate = LocalDate.parse(toDate, inputFormatter)
                                .format(outputFormatter);

                return tenementService.getZoneWiseReport(
                                formattedFromDate,
                                formattedToDate);
        }

        @GetMapping("/getWardWiseReport")
        public Map<String, Object> getWardWiseReport(

                        @RequestParam String zone,
                        @RequestParam String fromDate,
                        @RequestParam String toDate) {

                // ✅ convert dd-MM-yyyy to yyyy-MM-dd
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                String formattedFromDate = LocalDate.parse(fromDate, inputFormatter)
                                .format(outputFormatter);

                String formattedToDate = LocalDate.parse(toDate, inputFormatter)
                                .format(outputFormatter);

                return tenementService.getWardWiseReport(
                                zone,
                                formattedFromDate,
                                formattedToDate);
        }

        @GetMapping("/getSchemeWiseReport")
        public Map<String, Object> getSchemeWiseReport(

                        @RequestParam String ward,
                        @RequestParam String fromDate,
                        @RequestParam String toDate) {

                // ✅ dd-MM-yyyy → yyyy-MM-dd
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                String formattedFromDate = LocalDate.parse(fromDate, inputFormatter)
                                .format(outputFormatter);

                String formattedToDate = LocalDate.parse(toDate, inputFormatter)
                                .format(outputFormatter);

                return tenementService.getSchemeWiseReport(
                                ward,
                                formattedFromDate,
                                formattedToDate);
        }

        /*
         * @GetMapping("/getComplaintDetails")
         * public Map<String, Object> getComplaintDetails(
         * 
         * @RequestParam String ward,
         * 
         * @RequestParam String status,
         * 
         * @RequestParam String fromDate,
         * 
         * @RequestParam String toDate) {
         * 
         * // ✅ convert dd-MM-yyyy → yyyy-MM-dd
         * DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
         * 
         * DateTimeFormatter outputFormatter =
         * DateTimeFormatter.ofPattern("yyyy-MM-dd");
         * 
         * String formattedFromDate = LocalDate.parse(fromDate, inputFormatter)
         * .format(outputFormatter);
         * 
         * String formattedToDate = LocalDate.parse(toDate, inputFormatter)
         * .format(outputFormatter);
         * 
         * return tenementService.getComplaintDetails(
         * ward,
         * status,
         * formattedFromDate,
         * formattedToDate);
         * }
         */
        @GetMapping("/getComplaintDetails")
        public Map<String, Object> getComplaintDetails(

                        @RequestParam String ward,
                        @RequestParam String schemeid,
                        @RequestParam String status,
                        @RequestParam String fromDate,
                        @RequestParam String toDate) {

                // ✅ convert dd-MM-yyyy → yyyy-MM-dd
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                String formattedFromDate = LocalDate.parse(fromDate, inputFormatter)
                                .format(outputFormatter);

                String formattedToDate = LocalDate.parse(toDate, inputFormatter)
                                .format(outputFormatter);

                return tenementService.getComplaintDetails(
                                ward,
                                schemeid,
                                status,
                                formattedFromDate,
                                formattedToDate);
        }

}
