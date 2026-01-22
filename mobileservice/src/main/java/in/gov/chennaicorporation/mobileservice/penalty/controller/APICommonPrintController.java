package in.gov.chennaicorporation.mobileservice.penalty.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.gov.chennaicorporation.mobileservice.penalty.service.PenaltyPrintService;

@RequestMapping("/gccofficialapp/api/penalty")
@RestController("apiPenaltyprintRest")
public class APICommonPrintController {
	@Autowired
	private PenaltyPrintService penaltyPrintService;
	
	@GetMapping(value="/getSuccessList")
	public List<Map<String, Object>> getPenaltyTypes(
			@RequestParam(value = "loginId", required = true) String loginId,
			@RequestParam(value = "type", required = true) int type,
			@RequestParam(value = "fromDate", required = true) String fromDate,
			@RequestParam(value = "toDate", required = true) String toDate
			) {
        return penaltyPrintService.getSuccessList(loginId, type,fromDate,toDate);
    }
}
