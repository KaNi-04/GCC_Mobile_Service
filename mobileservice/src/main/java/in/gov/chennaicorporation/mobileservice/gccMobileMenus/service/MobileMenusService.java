package in.gov.chennaicorporation.mobileservice.gccMobileMenus.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class MobileMenusService {

	@Autowired
    JdbcTemplate jdbcTemplate;
	
	@Autowired
    public void setDataSource(@Qualifier("mysqlGccMobileMenuDataSource") DataSource gccMobiledenusDataSource) {
        this.jdbcTemplate = new JdbcTemplate(gccMobiledenusDataSource);
    }
	
	@Transactional
    public List<Map<String, Object>> versionCheck() {
        String sqlQuery = " SELECT * FROM app_version WHERE isactive=1 LIMIT 1 ";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sqlQuery);
        return result;
    }

	public List<Map<String,Object>> getMenus(Integer loginId){

	    String sql = " SELECT DISTINCT "
	    		+ "	            mm.menu_id, "
	    		+ "	            mm.menu_name, "
	    		+ "	            mm.menu_icon, "
	    		+ "	            mm.orderby "
	    		+ "	        FROM menu_master mm "
	    		+ "	        INNER JOIN usergroup_menu_mapping ugmm "
	    		+ "	            ON mm.menu_id = ugmm.menu_id "
	    		+ "	        INNER JOIN usergroup_login_mapping uglm "
	    		+ "	            ON FIND_IN_SET(ugmm.usergroup_id, uglm.usergroup_id) "
	    		+ "	        WHERE uglm.login_id = ? "
	    		+ "	            AND uglm.isactive = 1 "
	    		+ "	            AND uglm.isdelete = 0 "
	    		+ "	            AND ugmm.isactive = 1 "
	    		+ "	            AND ugmm.isdelete = 0 "
	    		+ "	            AND mm.isactive = 1 "
	    		+ "	            AND mm.isdelete = 0 "
	    		+ "	        ORDER BY mm.orderby ";

	    return jdbcTemplate.queryForList(sql, loginId);
	}
	
	public List<Map<String,Object>> getSubMenus(
	        Integer loginId,
	        Integer menuId){

	    String sql = " SELECT DISTINCT "
	    		+ "	            sm.submenu_id, "
	    		+ "	            sm.submenu_name, "
	    		+ "	            sm.submenu_icon, "
	    		+ "	            sm.menu_id, "
	    		+ "	            sm.orderby "
	    		+ "	        FROM submenu_master sm "
	    		+ "	        INNER JOIN usergroup_submenu_mapping ugsm "
	    		+ "	            ON sm.submenu_id = ugsm.submenu_id "
	    		+ "	        INNER JOIN usergroup_login_mapping uglm "
	    		+ "	            ON FIND_IN_SET(ugsm.usergroup_id, uglm.usergroup_id) "
	    		+ "	        WHERE uglm.login_id = ? "
	    		+ "	            AND sm.menu_id = ? "
	    		+ "	            AND uglm.isactive = 1 "
	    		+ "	            AND uglm.isdelete = 0 "
	    		+ "	            AND ugsm.isactive = 1 "
	    		+ "	            AND ugsm.isdelete = 0 "
	    		+ "	            AND sm.isactive = 1 "
	    		+ "	            AND sm.isdelete = 0 "
	    		+ "	        ORDER BY sm.orderby ";

	    return jdbcTemplate.queryForList(sql, loginId, menuId);
	}

	
}
