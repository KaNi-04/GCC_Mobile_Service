package in.gov.chennaicorporation.mobileservice.foodDistribution.Scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import in.gov.chennaicorporation.mobileservice.foodDistribution.service.DistributionService;

@Component
@EnableScheduling
public class DailyRequestScheduler {

	@Autowired
	private DistributionService distributionService;
	
	//@Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata") // for minute
	
	
	@Scheduled(cron = "0 0 14 * * ?", zone = "Asia/Kolkata") //  2:00 PM daily	
    public void runAutoInsert() {
		System.out.println("Food Daily Request Scheduler Started......");
		distributionService.autoInsertNextDayRequest();
		System.out.println("Food Daily Request Scheduler Ended......");
    }
}
