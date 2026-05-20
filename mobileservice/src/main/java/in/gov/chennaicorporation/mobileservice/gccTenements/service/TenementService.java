package in.gov.chennaicorporation.mobileservice.gccTenements.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TenementService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    public void setDataSource(@Qualifier("mysqlTenementDataSource") DataSource tenementDataSource) {
        this.jdbcTemplate = new JdbcTemplate(tenementDataSource);
    }

    public Map<String, Object> getTenementsListByWard(String loginid, String type) {

        Map<String, Object> response = new HashMap<>();

        String sqlQuery = "SELECT `ward` FROM `officer_login_mapping` WHERE `userid` = ? AND `type` = ? AND isactive=1 LIMIT 1";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery, loginid, type);

        String ward = results.get(0).get("ward").toString();

        String sqlQuery2 = "SELECT * FROM asset_master WHERE ward = ?";

        List<Map<String, Object>> results2 = jdbcTemplate.queryForList(sqlQuery2, ward);

        response.put("ward", ward);
        response.put("tenements", results2);

        return response;
    }

}
