package in.gov.chennaicorporation.mobileservice.penalty.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PenaltyPrintService {
	private JdbcTemplate jdbcPenaltyTemplate;
	private final Environment environment;
	private String fileBaseUrl;
	
	@Autowired
	public void setDataSource(@Qualifier("mysqlActivityDataSource") DataSource penaltyDataSource) {
		this.jdbcPenaltyTemplate = new JdbcTemplate(penaltyDataSource);
	}
	
	@Autowired
	public PenaltyPrintService(Environment environment) {
		this.environment = environment;
		this.fileBaseUrl=environment.getProperty("fileBaseUrl");
	}
	
	@Transactional
	public List<Map<String, Object>> getSuccessList(String loginId, int type, String fromDate, String toDate) {
	    List<Map<String, Object>> result = new ArrayList<>();
	    Map<String, Object> response = new HashMap<>();
	    switch (type) {
	        case 1:
	            result = getPenalty_SuccessList(loginId, fromDate, toDate);
	            break;
	        case 2:
	            result = getCattle_SuccessList(loginId, fromDate, toDate);
	            break;
	        case 3:
	            result = getHoarding_SuccessList(loginId, fromDate, toDate);
	            break;
	        default:
	            // Optional: throw exception if type is invalid
	            //throw new IllegalArgumentException("Invalid type: " + type);
	        	response.put("status", 400);
	            response.put("message", "Invalid type: " + type);
	            response.put("data", Collections.emptyList());
	            return Collections.singletonList(response);
	    }

	    response.put("status", 200);
        response.put("message", "Fine Collected List");
        response.put("data", result);
        
	    //return Collections.singletonList(response);
        return result;
	}
	
	@Transactional
	public List<Map<String, Object>> getPenalty_SuccessList(String loginId, String fromDate, String toDate) {
	    String sqlQuery =
	        "SELECT " +
	        "    bt.id, " +
	        "    bt.mid, " +
	        "    bt.tid, " +
	        "    bt.transactionResponseStatus, " +
	        "    bt.txnCode, " +
	        "    bt.txnId, " +
	        "    bt.bankName, " +
	        "    bt.batchNumber, " +
	        "    bt.transactionAmount, " +
	        "    bt.transactionTitle, " +
	        "    bt.txnAID, " +
	        "    bt.txnApprCode, " +
	        "    bt.txnCardNo, " +
	        "    bt.txnCardType, " +
	        "    DATE_FORMAT(bt.txnDate, '%d-%m-%Y') AS txnDate, " +
	        "    bt.txnInvoice, " +
	        "    bt.txnMode, " +
	        "    bt.txnRefNo, " +
	        "    bt.txnTC, " +
	        "    bt.txnTSI, " +
	        "    bt.txnTVR, " +
	        "    bt.txnTime, " +
	        "    bt.branchname, " +
	        "    bt.extraInfo, " +
	        "    bt.extraInfo2, " +
	        "    bt.extraInfo3, " +
	        "    bt.file, " +
	        "    bt.loginId, " +
	        "    c3.violator_name, " +
	        "    c3.violator_phone, " +
	        "    c3.orderid, " +
	        "    pc.name AS penaltyType " + 
	        "FROM gcc_penalty_pos.bank_transactions bt " +
	        "LEFT JOIN gcc_penalty_pos.penalty_challan c3 ON bt.txnId = c3.orderid " +
	        "LEFT JOIN gcc_penalty_pos.penalty_category pc ON c3.category_id = pc.id " +
	        "WHERE bt.txnDate BETWEEN ? AND ? " +
	        "AND c3.challan_by = ? " +
	        "ORDER BY bt.txnDate DESC";

	    return jdbcPenaltyTemplate.queryForList(sqlQuery, fromDate, toDate, loginId);
	}
	
	@Transactional
	public List<Map<String, Object>> getCattle_SuccessList(String loginId, String fromDate, String toDate) {
	    String sqlQuery = "SELECT  "
	    		+ "    bt.id, "
	    		+ "    bt.mid, "
	    		+ "    bt.tid, "
	    		+ "    bt.transactionResponseStatus, "
	    		+ "    bt.txnCode, "
	    		+ "    bt.txnId, "
	    		+ "    bt.bankName, "
	    		+ "    bt.batchNumber, "
	    		+ "    bt.transactionAmount, "
	    		+ "    bt.transactionTitle, "
	    		+ "    bt.txnAID, "
	    		+ "    bt.txnApprCode, "
	    		+ "    bt.txnCardNo, "
	    		+ "    bt.txnCardType, "
	    		+ "    DATE_FORMAT(bt.txnDate, '%d-%m-%Y') AS txnDate, "
	    		+ "    bt.txnInvoice, "
	    		+ "    bt.txnMode, "
	    		+ "    bt.txnRefNo, "
	    		+ "    bt.txnTC, "
	    		+ "    bt.txnTSI, "
	    		+ "    bt.txnTVR, "
	    		+ "    bt.txnTime, "
	    		+ "    bt.branchname, "
	    		+ "    bt.extraInfo, "
	    		+ "    bt.extraInfo2, "
	    		+ "    bt.extraInfo3, "
	    		+ "    bt.file, "
	    		+ "    bt.loginId, "
	    		+ "    c3.violator_name, "
	    		+ "    c3.violator_phone, "
	    		+ "    c3.orderid, "
	    		+ "    'Cattel Catch' AS penaltyType "
	    		+ "FROM gcc_penalty.bank_transactions bt "
	    		+ "LEFT JOIN gcc_penalty.cc_step3 c3  "
	    		+ "    ON bt.txnId = c3.orderid "
	    		+ "WHERE bt.txnDate BETWEEN ? AND ? "
	    		+ "AND c3.challan_by = ? "
	    		+ "ORDER BY bt.txnDate DESC"; 
	    //System.out.println("SQL : " + sqlQuery);
	    //List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
	    return jdbcPenaltyTemplate.queryForList(sqlQuery, fromDate, toDate, loginId);
	}
	
	@Transactional
	public List<Map<String, Object>> getHoarding_SuccessList(String loginId, String fromDate, String toDate) {
	    String sqlQuery = "SELECT  "
	    		+ "    bt.id, "
	    		+ "    bt.mid, "
	    		+ "    bt.tid, "
	    		+ "    bt.transactionResponseStatus, "
	    		+ "    bt.txnCode, "
	    		+ "    bt.txnId, "
	    		+ "    bt.bankName, "
	    		+ "    bt.batchNumber, "
	    		+ "    bt.transactionAmount, "
	    		+ "    bt.transactionTitle, "
	    		+ "    bt.txnAID, "
	    		+ "    bt.txnApprCode, "
	    		+ "    bt.txnCardNo, "
	    		+ "    bt.txnCardType, "
	    		+ "    DATE_FORMAT(bt.txnDate, '%d-%m-%Y') AS txnDate, "
	    		+ "    bt.txnInvoice, "
	    		+ "    bt.txnMode, "
	    		+ "    bt.txnRefNo, "
	    		+ "    bt.txnTC, "
	    		+ "    bt.txnTSI, "
	    		+ "    bt.txnTVR, "
	    		+ "    bt.txnTime, "
	    		+ "    bt.branchname, "
	    		+ "    bt.extraInfo, "
	    		+ "    bt.extraInfo2, "
	    		+ "    bt.extraInfo3, "
	    		+ "    bt.file, "
	    		+ "    bt.loginId, "
	    		+ "    c3.agency_name As violator_name, "
	    		+ "    c3.agency_mobile As violator_phone, "
	    		+ "    c3.orderid, "
	    		+ "    'Hoarding' AS penaltyType "
	    		+ "FROM gcc_penalty_hoardings.bank_transactions bt "
	    		+ "LEFT JOIN gcc_penalty_hoardings.hoardings_info c3  "
	    		+ "    ON bt.txnId = c3.orderid "
	    		+ "WHERE bt.txnDate BETWEEN ? AND ? "
	    		+ "AND c3.cby = ? "
	    		+ "ORDER BY bt.txnDate DESC"; 
	    //System.out.println("SQL : " + sqlQuery);
	    //List<Map<String, Object>> result = jdbcPenaltyTemplate.queryForList(sqlQuery);
	    return jdbcPenaltyTemplate.queryForList(sqlQuery, fromDate, toDate, loginId);
	}
	
}
