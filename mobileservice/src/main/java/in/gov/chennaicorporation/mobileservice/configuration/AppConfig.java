package in.gov.chennaicorporation.mobileservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
	
	public String qrAssetFeedback = "http://117.232.67.158:8063";
	public String mtm = "http://117.232.67.158:8060";
	public String petRegistration = "http://117.232.67.158:8061";
	public String garbageCollection = "http://117.232.67.158:8067"; //117.232.67.158
	public String otpurl = "https://tmegov.onex-aura.com/api/sms?";
	
	public String gccappsMysqlPassword = "";
	
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }  
}