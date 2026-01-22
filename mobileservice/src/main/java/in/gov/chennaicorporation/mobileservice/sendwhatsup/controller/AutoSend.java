package in.gov.chennaicorporation.mobileservice.sendwhatsup.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.gov.chennaicorporation.mobileservice.sendwhatsup.service.HtmlToImageService;

@RequestMapping("/gccofficialapp/api/sendwhatsapp/")
@RestController("gccofficialappsautosend")
public class AutoSend {

	@Autowired
    private HtmlToImageService service;
	
    // Runs every 2 hours
    @Scheduled(cron = "0 0 */1 * * *")
    public void autoSendReports() {

        // POS Category Collection
        service.processHtmlToPng(
            "https://gccservices.in/commissionerreview/pos/categoryCollectionDetails",
            "19"
        );

        // POS Zone Collection
        service.processHtmlToPng(
            "https://gccservices.in/commissionerreview/pos/zoneCollectionDetails",
            "18"
        );

        // Vehicle Details
        service.processHtmlToPng(
            "https://gccservices.in/commissionerreview/pos/vehicleDetails",
            "20"
        );
    }
    
    @GetMapping("/sendreport")
    public void sendreport() {
    	// POS Category Collection
        service.processHtmlToPng(
            "https://gccservices.in/commissionerreview/pos/categoryCollectionDetails",
            "19"
        );
    }
}
