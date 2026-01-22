package in.gov.chennaicorporation.mobileservice.component;

import java.io.IOException;
import java.time.LocalDateTime;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ApiLoggingFilter implements Filter {

    @Autowired
    private JdbcTemplate jdbcAPITemplate;
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlAPIDataSource") DataSource apiDataSource) {
		this.jdbcAPITemplate = new JdbcTemplate(apiDataSource);
	}
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
    	System.out.println("API Logging Filter triggered");
    	
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String ipAddress = request.getRemoteAddr();
        LocalDateTime accessTime = LocalDateTime.now();
        String endpoint = httpRequest.getRequestURI();
        String userAgent = httpRequest.getHeader("User-Agent");

        // Log to check if filter is being triggered
        System.out.println("Request Details: IP Address = " + ipAddress + ", Endpoint = " + endpoint);

        
        // Insert log into the database
        String sql = "INSERT INTO api_access_log (ip_address, access_time, endpoint, user_agent) VALUES (?, ?, ?, ?)";
        jdbcAPITemplate.update(sql, ipAddress, accessTime, endpoint, userAgent);

        // Continue with the request-response cycle
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization logic if needed
    }

    @Override
    public void destroy() {
        // Cleanup logic if needed
    }
}
