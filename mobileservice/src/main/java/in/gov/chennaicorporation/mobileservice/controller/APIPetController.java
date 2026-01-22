package in.gov.chennaicorporation.mobileservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import in.gov.chennaicorporation.mobileservice.service.PetAPIService;

@RequestMapping("/gccofficialapp/pet/api")
@RestController("petapicontroller")
public class APIPetController {
	 @Autowired
	    private PetAPIService petAPIService;
	@GetMapping(value="/paymentinfo")
	public List<Map<String, Object>> getApiData(@RequestParam("fromDate") String fromDate, @RequestParam("toDate") String toDate ) {
        return petAPIService.fetchApiData(fromDate, toDate);
    }

}