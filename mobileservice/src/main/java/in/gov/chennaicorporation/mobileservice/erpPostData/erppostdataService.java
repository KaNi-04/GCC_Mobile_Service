package in.gov.chennaicorporation.mobileservice.erpPostData;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class erppostdataService {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	public void setDataSource(@Qualifier("mysqlPenaltyDataSource") DataSource penaltyDataSource) {
		this.jdbcTemplate = new JdbcTemplate(penaltyDataSource);
	}
	// This method runs every 1 hour (3600000 ms)
    @Scheduled(fixedRate = 60 * 60 * 1000)
    //@Scheduled(cron = "0 0 * * * *") // every hour at 0th minute
    public void postDataToERP() {
        // Call the logic you currently expose through controller
       // System.out.println("Posting data to ERP at " + java.time.LocalDateTime.now());
        postPOStoERP(); // FOR PENALTY POS (gcc_penalty_pos)
    }
    
    private String postPOStoERP() {
    	// System.out.println("ERP txn: Started");
        String sql = "SELECT "
        		+ "	   bt.id, "
        		+ "    bt.txnDate, "
        		+ "    DATE_FORMAT(bt.txnDate, '%Y') AS TRANSACTIONYEAR, "
        		+ "    DATE_FORMAT(bt.txnDate, '%d/%m/%Y') AS TRANSACTIONDATE, "
        		+ "    bt.txnId AS TRANSACTIONNO, "
        		+ "    bt.transactionAmount AS TOTALTRANSACTIONAMOUNT, "
        		+ "    bt.txnRefNo AS TRANSACTIONREFNO, "
        		+ "    '450310000' AS TOTALTRANSACTIONCODE, "
        		+ "    pcat.erpcode AS MSTRCODEONE, "
        		+ "    0 AS SPLITAMOUNTONE, "
        		+ "    pc.violator_name AS PARTYNAME, "
        		+ "    pc.violator_company AS PARTYADDRESS, "
        		+ "    pc.violator_phone AS PARTYMOBILE, "
        		+ "    pc.id AS UNIQUERECEIPTID "
        		+ "FROM  "
        		+ "    `bank_transactions` bt "
        		+ "LEFT JOIN  "
        		+ "    `penalty_challan` pc ON bt.txnId = pc.orderid  "
        		+ "    AND pc.isactive = 1  "
        		+ "    AND pc.isdelete = 0 "
        		+ "    AND bt.txnMode NOT IN ('Hand cash','Cheque','DD') "
        		+ "LEFT JOIN  "
        		+ "    `penalty_category` pcat ON pcat.id = pc.category_id   "
        		+ "WHERE  bt.erp_post_status IS NULL OR bt.erp_post_status != 'posted' "
        		+ " AND bt.erp_post_message != 'Already Updated this collection' "
        		+ "ORDER BY `bt`.`id` ASC";
        try {
        	
        	// System.out.println("Running ERP sync...");
           //  System.out.println("Using SQL:\n" + sql);
            // System.out.println("Connecting to: " + jdbcTemplate.getDataSource().getConnection().getMetaData().getURL());
            
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            // System.out.println("ERP txt Total records: : "+result.size());
            for (Map<String, Object> row : result) {
            	String ID = String.valueOf(row.get("id"));
            	//System.out.println("ERP TXT DATA : "+ID);
                String ASSESSMENTNO = row.get("TRANSACTIONYEAR")+"/Fine/"+row.get("id");
                String UNIQUERECEIPTID = String.valueOf(row.get("UNIQUERECEIPTID"));
                String TRANSACTIONNO = String.valueOf(row.get("TRANSACTIONNO"));
                String TRANSACTIONREFNO = String.valueOf(row.get("TRANSACTIONREFNO")); // optional mapping
                String TRANSACTIONDATE = String.valueOf(row.get("TRANSACTIONDATE"));
                String TOTALTRANSACTIONAMOUNT = String.valueOf(row.get("TOTALTRANSACTIONAMOUNT"));
                String TOTALTRANSACTIONCODE = String.valueOf(row.get("TOTALTRANSACTIONCODE"));
                String MSTRCODEONE = String.valueOf(row.get("MSTRCODEONE"));
                String SPLITAMOUNTONE = String.valueOf(row.get("SPLITAMOUNTONE"));
                String PARTYNAME = String.valueOf(row.get("PARTYNAME"));
                String PARTYADDRESS = String.valueOf(row.get("PARTYADDRESS"));
                String PARTYMOBILE = String.valueOf(row.get("PARTYMOBILE"));

                String response = postToErp(
                		ID,
                        ASSESSMENTNO,
                        UNIQUERECEIPTID,
                        TRANSACTIONNO,
                        TRANSACTIONREFNO,
                        TRANSACTIONDATE,
                        TOTALTRANSACTIONAMOUNT,
                        TOTALTRANSACTIONCODE,
                        MSTRCODEONE,
                        SPLITAMOUNTONE,
                        PARTYNAME,
                        PARTYADDRESS,
                        PARTYMOBILE
                );
                
                // ✅ Exit loop immediately if ERP server down or response is not "success"
                if ("server_down".equalsIgnoreCase(response)) {
                    System.out.println("ERP server is down or unreachable. Halting current loop...");
                    break; // stop further processing in this cycle
                }
                
                // Optionally log response
                // System.out.println("ERP Response for txn: " + TRANSACTIONNO + " → " + response);
            }

            return "success";

        } catch (Exception e) {
            e.printStackTrace();
            return "erp error: " + e.getMessage();
        }
    }
    
    private String postToErp(
    		String id,
            String ASSESSMENTNO,
            String UNIQUERECEIPTID,
            String TRANSACTIONNO,
            String TRANSACTIONREFNO,
            String TRANSACTIONDATE,
            String TOTALTRANSACTIONAMOUNT,
            String TOTALTRANSACTIONCODE,
            String MSTRCODEONE,
            String SPLITAMOUNTONE,
            String PARTYNAME,
            String PARTYADDRESS,
            String PARTYMOBILE) {
    	// System.out.println("ERP txn request: ERP");
        String erpURL = "http://coc-staging.egovernments.org:9980/pgr/external/mobileservice?serviceId=getOtherCollection"
                + "&COLLECTION_TYPE=Fine Collection"
                + "&ASSESSMENTNO=" + ASSESSMENTNO
                + "&UNIQUERECEIPTID=" + UNIQUERECEIPTID
                + "&TRANSACTIONNO=" + TRANSACTIONNO
                + "&TRANSACTIONREFNO=" + TRANSACTIONREFNO
                + "&TRANSACTIONDATE=" + TRANSACTIONDATE
                + "&TOTALTRANSACTIONAMOUNT=" + TOTALTRANSACTIONAMOUNT
                + "&TOTALTRANSACTIONCODE=" + TOTALTRANSACTIONCODE
                + "&MSTRCODEONE=" + MSTRCODEONE
                + "&SPLITAMOUNTONE=" + SPLITAMOUNTONE
                + "&PARTYNAME=" + PARTYNAME
                + "&PARTYADDRESS=" + PARTYADDRESS
                + "&PARTYMOBILE=" + PARTYMOBILE;

        RestTemplate restTemplate = new RestTemplate();
        //System.out.println("ERP URL:" + erpURL);
        try {
            ResponseEntity<String> response = restTemplate.exchange(erpURL, HttpMethod.GET, null, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(response.getBody());

                boolean resultStatus = jsonNode.path("ResultStatus").asBoolean(); // this is already a boolean
                String message = jsonNode.path("Message").asText(); // capture the message string

                if (resultStatus) {
                    String updateSql = "UPDATE bank_transactions SET erp_post_status = 'posted', erp_post_message = ?, erp_post_date = ? WHERE id = ?";
                    jdbcTemplate.update(updateSql, message, Timestamp.valueOf(LocalDateTime.now()),  id);
                } else {
                    String updateSql = "UPDATE bank_transactions SET erp_post_status = 'failed', erp_post_message = ?, erp_post_date = ? WHERE id = ?";
                    jdbcTemplate.update(updateSql, message, Timestamp.valueOf(LocalDateTime.now()), id);
                }

                return resultStatus ? "success" : "failed";

            } else {
                return "failed"; // Non-200 response from server
            }

        } catch (Exception ex) {
            System.out.println("Error contacting ERP server: " + ex.getMessage());
            return "server_down";
        }
    }
}
