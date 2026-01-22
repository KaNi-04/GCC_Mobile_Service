package in.gov.chennaicorporation.mobileservice.gccflagpole.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccflagpole.service.FlagPoleService;

@RequestMapping("/gccofficialapp/api/flagpole")
@RestController("gccofficialappflagpole")
public class FlagPoleController {

    @Autowired
    FlagPoleService flagPoleService;

    @GetMapping("/getEventDetails")
    public List<Map<String, Object>> getEventDetails() {

        return flagPoleService.getEventDetails();

    }

    @GetMapping("/getAllFlagPoleRequest")
    public List<Map<String, Object>> getAllFlagPoleRequest(@RequestParam("userid") String userid) {

        return flagPoleService.getAllFlagPoleRequest(userid);

    }

    @GetMapping("/getRequestDetails")
    public Map<String, Object> getRequestDetailsById(@RequestParam String reqId) {

        return flagPoleService.getRequestDetailsById(reqId);
    }

    @PostMapping("/approveFlagPoleRequest")
    public Map<String, Object> approveFlagPoleRequest(@RequestParam String reqId, @RequestParam String approvedBy) {

        return flagPoleService.approveFlagPoleRequestByAE(reqId, approvedBy);
    }

    @GetMapping("/getRDOApprovedRequests")
    public List<Map<String, Object>> getRDOApprovedRequests(@RequestParam("userid") String userid) {

        return flagPoleService.getRDOApprovedRequests(userid);

    }

    @GetMapping("/getRDOApprovedRequestsDetails")
    public Map<String, Object> getRDOApprovedRequestsDetails(@RequestParam("reqId") String reqId) {

        return flagPoleService.getRDOApprovedRequestsDetails(reqId);

    }

    @PostMapping("/submitRestorationDetails")
    public String submitRestorationDetails(@RequestParam("refid") String refid,
            @RequestParam("amount") String amount,
            @RequestParam("restorimg") MultipartFile restorImg,
            @RequestParam("remarks") String remarks,
            @RequestParam("userid") String userid) {

        String restorImgPath = flagPoleService.fileUpload(restorImg, "restorationImages");

        return flagPoleService.submitRestorationDetails(refid, amount, restorImgPath, remarks, userid);

    }

    @GetMapping("/getRestorationList")
    public List<Map<String, Object>> getRestorationList(@RequestParam("userid") String userid) {

        return flagPoleService.getRestorationList(userid);

    }

    @GetMapping("/getRestorationDetails")
    public Map<String, Object> getRestorationDetails(@RequestParam("reqId") String  reqId) {

        return flagPoleService.getRestorationDetailsById(reqId);

    }

    @PostMapping("/saveFineDetails")
    public String saveFineDetails(@RequestParam MultipartFile fineImg,
            @RequestParam String address,
            @RequestParam String latitude,
            @RequestParam String longitude,
            @RequestParam String zone,
            @RequestParam String ward,
            @RequestParam String vName,
            @RequestParam String mobNo,
            @RequestParam String noofflagpoles,
            @RequestParam String fineAmount,
            @RequestParam String remarks,
            @RequestParam String userid,
            @RequestParam String eventType) {

        String fineImgPath = flagPoleService.fileUpload(fineImg, "fineImages");

        return flagPoleService.saveFineDetails(vName, mobNo, fineImgPath, address, zone, ward, latitude, longitude, noofflagpoles, fineAmount, remarks, userid, eventType);

    }
    
    @GetMapping("/getFineList")
    public List<Map<String, Object>> getFineList(@RequestParam("userid") String userid) {

        return flagPoleService.getFineList(userid);

    }

    @PostMapping("/submitFIRDetails")
    public String submitFIRDetails(@RequestParam("refid") String refid,
            @RequestParam("firimg") MultipartFile firImg,
            @RequestParam("userid") String userid,
            @RequestParam("fir_remarks") String fir_remarks) {

        String firImgPath = flagPoleService.fileUpload(firImg, "firImages");

        return flagPoleService.submitFIRDetails(refid, firImgPath, userid, fir_remarks);

    }

}
