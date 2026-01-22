package in.gov.chennaicorporation.mobileservice;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MobileServiceApplication {

	public static void main(String[] args) {
		// Set the timezone to Asia/Kolkata
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(MobileServiceApplication.class, args);
	}

}
