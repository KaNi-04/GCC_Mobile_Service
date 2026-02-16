package in.gov.chennaicorporation.mobileservice.c_d_waste.Scheduler;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import in.gov.chennaicorporation.mobileservice.c_d_waste.service.c_d_WasteService;
import in.gov.chennaicorporation.mobileservice.c_d_waste.service.c_d_WasteUserService;

@Component
@EnableScheduling
public class C_D_WasteScheduler {
	
	@Autowired
    private c_d_WasteService c_d_WasteService;
	
	@Autowired
	private c_d_WasteUserService c_d_WasteUserService;
	
	//@Scheduled(cron = "0 0 * * * *", zone = "Asia/Kolkata") for hour
	
	//	@Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata") for minute
	
	
	@Scheduled(cron = "0 0 * * * *", zone = "Asia/Kolkata")
	public void cd_wasteScheduler() {
		System.out.println("CD_wasteScheduler Started......");
		 Map<String,Object> result= c_d_WasteService.updateCDWasteEscalationStatus("1");
		System.out.println("Scheduler Result: " + result);
	}
	
	@Scheduled(cron = "0 0 * * * *", zone = "Asia/Kolkata")
	public void cd_userwasteScheduler() {
		System.out.println("CD_userwasteScheduler Started......");
		 Map<String,Object> result= c_d_WasteUserService.updateCDWasteUserEscalationStatus("1");
		System.out.println("Scheduler Result: " + result);
	}

}
