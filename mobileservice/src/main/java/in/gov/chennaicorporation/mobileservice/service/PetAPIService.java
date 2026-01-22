package in.gov.chennaicorporation.mobileservice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import in.gov.chennaicorporation.mobileservice.configuration.AppConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
@Service
public class PetAPIService {
	private final RestTemplate restTemplate;
	private final AppConfig appConfig;
	
	@Autowired
    public PetAPIService(
    		RestTemplate restTemplate,
    		AppConfig appConfig) {
        this.restTemplate = restTemplate;
        this.appConfig = appConfig;
    }

    public List<Map<String, Object>> fetchApiData(String fromDate, String toDate) {
        String url = appConfig.petRegistration+"/api/admin/paymentInfo?";  // Replace with the actual URL
        url = url + "fromDate="+fromDate+"&toDate="+toDate;
        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        return response.getBody();
    }
}
