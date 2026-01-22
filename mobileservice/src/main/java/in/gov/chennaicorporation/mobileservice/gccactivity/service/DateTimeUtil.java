package in.gov.chennaicorporation.mobileservice.gccactivity.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DateTimeUtil {
	public static String getCurrentDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyyHHmmss");
        LocalDateTime now = LocalDateTime.now();
        return now.format(formatter);
    }
	public static String getCurrentYear() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");
        LocalDateTime now = LocalDateTime.now();
        return now.format(formatter);
    }
	public static String getCurrentMonth() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM");
        LocalDateTime now = LocalDateTime.now();
        return now.format(formatter);
    }
	public static String getCurrentDay() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd");
        LocalDateTime now = LocalDateTime.now();
        return now.format(formatter);
    }
	public static String getCurrentDateTimeMysql() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		return now.format(formatter);
    }
}
