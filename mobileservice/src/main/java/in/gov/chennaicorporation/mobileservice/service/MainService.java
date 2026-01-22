package in.gov.chennaicorporation.mobileservice.service;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MainService {
	private JdbcTemplate jdbcMainTemplate;
	@Autowired
	public void setDataSource(
			@Qualifier("mysqlActivityDataSource") DataSource activityDataSource) {
		this.jdbcMainTemplate = new JdbcTemplate(activityDataSource);
	}
	/*
	@Transactional
	public List<Map<String, Object>> versionCheck() {
		String sqlQuery = "SELECT * FROM `app_version` WHERE `isactive`=1 LIMIT 1"; 
		List<Map<String, Object>> result = jdbcMainTemplate.queryForList(sqlQuery);
		return result;
	}
	*/
}
