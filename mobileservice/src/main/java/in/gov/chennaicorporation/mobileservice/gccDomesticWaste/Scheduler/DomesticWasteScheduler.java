package in.gov.chennaicorporation.mobileservice.gccDomesticWaste.Scheduler;

import in.gov.chennaicorporation.mobileservice.gccDomesticWaste.Service.DomesticWasteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@EnableScheduling
public class DomesticWasteScheduler {
    @Autowired
    private DomesticWasteService domesticWasteService;


    @Scheduled(cron = "0 0 1 * * WED", zone = "Asia/Kolkata")
    //@Scheduled(fixedRate = 3000)
    public void domesticWasteScheduler() {
        System.out.println("DomesticWasteScheduler Started......");
        Map<String,Object> result= domesticWasteService.updateOrderStatus("3");
        System.out.println("Scheduler Result: " + result);

    }


}
