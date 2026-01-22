package in.gov.chennaicorporation.mobileservice.boatService.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.boatService.service.BoatService;

@RequestMapping("/gccofficialapp/api/boat/pos")
@RestController("apiBoatPOSRest")
public class BoatController {

	@Autowired
	private BoatService boatService;
	
	@PostMapping("/devicelogin")
    public List<?> devicelogin(
    		@RequestParam(value = "device_username", required = true) String device_username, 
			@RequestParam(value = "device_password", required = true) String device_password
			) {
        return boatService.devicelogin(device_username,device_password);
    }
	
	@PostMapping("/getuserinfo")
    public List<?> getuserinfo(
    		@RequestParam(value = "loginId", required = true) String loginId
			) {
        return boatService.getuserinfo(loginId);
    }
	
	@GetMapping("/getBoatServiceLocation")
    public List<?> getBoatServiceLocation(
    		@RequestParam(value = "lid", required = true) String lid
			) {
        return boatService.getBoatServiceLocation(lid);
    }
	
	@GetMapping("/getBoatConfigInfo")
    public List<?> getBoatConfigInfo(
    		@RequestParam(value = "lid", required = true) String lid
			) {
        return boatService.getBoatConfigInfo(lid);
    }
	
	@PostMapping(value="/generateBoatTicketPOS")
	public List<?> generateBoatTicketPOS(@RequestParam MultiValueMap<String, String> formData,
			@RequestParam(value = "loginId", required = true) String loginId, 
			@RequestParam(value = "lid", required = false) String lid, 
			@RequestParam(value = "bcid", required = false) String bcid,
			@RequestParam(value = "boat_name", required = false) String boat_name,
			@RequestParam(value = "visitorName", required = false) String visitorName,
			@RequestParam(value = "visitorPhone", required = false) String visitorPhone,
			@RequestParam(value = "adult_count", required = false) String adult_count,
			@RequestParam(value = "child_count", required = false) String child_count,
			@RequestParam(value = "adult_price", required = false) String adult_price,
			@RequestParam(value = "child_price", required = false) String child_price,
			@RequestParam(value = "raid_time", required = false) String raid_time,
			@RequestParam(value = "total_ticket", required = false) String total_ticket,
			@RequestParam(value = "ticket_amount", required = false) String ticket_amount,
			@RequestParam(value = "tid", required = false) String tid,
			@RequestParam(value = "mid", required = false) String mid,
			@RequestParam(value = "serialNumber", required = false) String serialNumber
			) {
		return boatService.generateBoatTicketPOS(formData, loginId, lid, bcid, boat_name, visitorName,
				visitorPhone, adult_count, child_count, adult_price, child_price, raid_time,total_ticket,ticket_amount, tid, mid, serialNumber);
	}
	
	@GetMapping("/getTicketData")
    public List<?> getTicketData(
    		@RequestParam MultiValueMap<String, String> formData,
    		@RequestParam(value = "orderid", required = false) String orderid,
    		@RequestParam(value = "ticketid", required = false) String ticketid
			) {
        return boatService.getTicketData(formData, orderid, ticketid);
    }
	
	@PostMapping("/storeBankTransaction")
    public List<?> storeBankTransaction(@RequestBody Map<String, Object> transactionData) {
        return boatService.storeBankTransaction(transactionData);
    }
	
	@PostMapping("/storeFailedBankTransaction")
    public List<?> storeFailedBankTransaction(@RequestBody Map<String, Object> transactionData) {
        return boatService.storeFailedBankTransaction(transactionData);
    }
}
