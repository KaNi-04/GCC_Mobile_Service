package in.gov.chennaicorporation.mobileservice.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import in.gov.chennaicorporation.mobileservice.component.ApiLoggingFilter;

@Configuration
public class WebConfig {

    @Autowired
    private ApiLoggingFilter apiLoggingFilter;

    // Register the ApiLoggingFilter using FilterRegistrationBean
    @Bean
    public FilterRegistrationBean<ApiLoggingFilter> loggingFilter() {
        FilterRegistrationBean<ApiLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(apiLoggingFilter); // Set the filter
        registrationBean.addUrlPatterns("/**/api/*"); // Apply to all API endpoints
        registrationBean.setOrder(1); // Optional: Set order if needed
        System.out.println("API Logging Filter Registered for URL patterns: /api/*");
        return registrationBean;
    }
}