package in.gov.chennaicorporation.mobileservice.ie_complaints.Scheduler;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import in.gov.chennaicorporation.mobileservice.ie_complaints.service.ieComplaintService;

@Component
@EnableScheduling
public class I_E_Scheduler {

	@Autowired
	ieComplaintService iecomplaintservice;
	// @Scheduled(cron = "0 0 * * * *", zone = "Asia/Kolkata") for hour

	// @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata") for minute

	// @Scheduled(cron = "0 0 * * * *", zone = "Asia/Kolkata")

	// @Scheduled(cron = "0 0 * * * *", zone = "Asia/Kolkata") // for hour
	@Scheduled(cron = "0 0 * * * *", zone = "Asia/Kolkata")
	public void IE_Scheduler() {
		System.out.println("IE_Scheduler Started......");
		Map<String, Object> result = iecomplaintservice.updateIEEscalationStatus("1");
		System.out.println("Scheduler Result: " + result);
	}

	/*
	 * @Scheduled(cron = "0 0 * * * *", zone = "Asia/Kolkata")
	 * public void cd_userwasteScheduler() {
	 * System.out.println("CD_userwasteScheduler Started......");
	 * Map<String, Object> result =
	 * c_d_WasteUserService.updateCDWasteUserEscalationStatus("1");
	 * System.out.println("Scheduler Result: " + result);
	 * }
	 */

}
