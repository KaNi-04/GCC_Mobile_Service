package in.gov.chennaicorporation.mobileservice.boatService.service;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BoatService {
	
	private JdbcTemplate jdbcTemplate;

	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int STRING_LENGTH = 15;
    private static final Random RANDOM = new SecureRandom();
    
    @Autowired
	public void setDataSource(@Qualifier("mysqlBoatSource") DataSource BoatDataSource) {
		this.jdbcTemplate = new JdbcTemplate(BoatDataSource);
	}
    
    public static String generateRandomString() {
		StringBuilder result = new StringBuilder(STRING_LENGTH);
		for (int i = 0; i < STRING_LENGTH; i++) {
			result.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
		}
		return result.toString();
	}
    
    public List<Map<String, Object>> devicelogin(String device_username, String device_password) {
        String sql = "SELECT `uid`, `lid`, `device_username` FROM `device_user` "
        		+ "WHERE (`isactive`=1 AND `isdelete`=0) "
        		+ "AND (`device_username` = ? AND `device_password` = ?) "
        		+ "LIMIT 1";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql,device_username, device_password);
        
        Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
        response.put("message", "Login Info");
        response.put("data", result);
        
		return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getuserinfo(String loginId) {
        String sql = "SELECT `userid`, `lid`, `device_username` FROM `device_user` "
        		+ "WHERE (`isactive`=1 AND `isdelete`=0) "
        		+ "AND (`userid` = ? ) "
        		+ "LIMIT 1";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql,loginId);
        
        Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
        response.put("message", "Login Info");
        response.put("data", result);
        
		return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getBoatServiceLocation(String lid) {
        String sql = "SELECT `lid`, `name`, `zone`, `ward`, `incharge_name`, `incharge_mobile`, `open_time`, `close_time` "
        		+ " FROM `service_location` "
        		+ "WHERE "
        		+ "`isactive` = 1 "
        		+ " AND `isdelete` = 0 "
        		+ " AND `lid` = ?";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql,lid);
        
        Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
        response.put("message", "Boat Service Loaction");
        response.put("data", result);
        
		return Collections.singletonList(response);
    }
    
    public List<Map<String, Object>> getBoatConfigInfo(String lid) {
        String sql = "SELECT `bcid`, `name`, `type`, `noofseats`, "
        		+ "`adult_price`, `child_price`, "
        		+ "`raid_time`, `holiday`, "
        		+ "`lid` "
        		+ "FROM `boat_config` "
        		+ "WHERE "
        		+ "`isactive` = 1 "
        		+ "AND `isdelete` = 0 "
        		+ "AND `lid` = ?";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql,lid);
        
        Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
        response.put("message", "Boat details");
        response.put("data", result);
        
		return Collections.singletonList(response);
    }
    
    @Transactional
    public void updateTicketNo(int ticketId, String lid) {

        String sql = """
            SELECT COALESCE(MAX(ticketno), 0) + 1
            FROM tickets
            WHERE lid = ?
            AND DATE(ticketdatetime) = CURDATE()
        """;

        Integer nextTicketNo = jdbcTemplate.queryForObject(sql, Integer.class, lid);

        String updateSql = """
            UPDATE tickets
            SET ticketno = ?
            WHERE ticketid = ?
        """;

        jdbcTemplate.update(updateSql, nextTicketNo, ticketId);
    }
    
    @Transactional
	public List<Map<String, Object>> generateBoatTicketPOS(
			MultiValueMap<String, String> formData,
			String loginId,
			String lid,
			String bcid,
			String boat_name,
			String visitorName,
			String visitorPhone,
			String adult_count,
			String child_count,
			String adult_price,
			String child_price,
			String raid_time,
			String total_ticket,
			String ticket_amount,
			String tid,
			String mid,
			String serialNumber) {
		
		Map<String, Object> response = new HashMap<>();
		int lastInsertId = 0;
		String orderid = generateRandomString();
		
		String sqlQuery = "INSERT INTO `tickets`(`lid`, `bcid`, `boat_name`, `visitorName`, "
				+ "`visitorPhone`, `adult_count`, `child_count`, "
				+ "`adult_price`, `child_price`, `raid_time`, `total_ticket`,`ticket_amount`,`orderid`,"
				+ "`tid`, `mid`, `serialNumber`,`ticket_by`) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		KeyHolder keyHolder = new GeneratedKeyHolder();
		try {
            int affectedRows = jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"ticketid"});
                    int i = 1;
                    
                    ps.setString(i++, lid);
                    ps.setString(i++, bcid);
                    ps.setString(i++, boat_name);
                    ps.setString(i++, visitorName);
                    ps.setString(i++, visitorPhone);
                    ps.setString(i++, adult_count);
                    ps.setString(i++, child_count);
                    ps.setString(i++, adult_price);
                    ps.setString(i++, child_price);
                    ps.setString(i++, raid_time);
                    ps.setString(i++, total_ticket);
                    ps.setString(i++, ticket_amount);
                    ps.setString(i++, orderid);
                    ps.setString(i++, tid);
                    ps.setString(i++, mid);
                    ps.setString(i++, serialNumber);
                    ps.setString(i++, loginId);
                    return ps;
                }
            }, keyHolder);

            if (affectedRows > 0) {
                Number generatedId = keyHolder.getKey();
                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
                
                updateTicketNo(lastInsertId, lid);
                
                String sqlQuery_txt = "SELECT * From `tickets` WHERE ticketid='"+lastInsertId+"' LIMIT 1";
                List<Map<String, Object>> result = jdbcTemplate.queryForList(sqlQuery_txt);
              
                response.put("status", 200);
                response.put("message", "A new ticket (Boat) was generated successfully!");
                response.put("data", result);
              
                System.out.println("A new Ticket (Boat) was generated successfully! Insert ID: " + generatedId);
            } else {
                response.put("status", 201);
                response.put("message", "Failed to insert a new ticket (Boat).");
                response.put("data", "");
            }
        } catch (DataAccessException e) {
            System.out.println("Data Access Exception:");
            Throwable rootCause = e.getMostSpecificCause();
            if (rootCause instanceof SQLException) {
                SQLException sqlException = (SQLException) rootCause;
                System.out.println("SQL State: " + sqlException.getSQLState());
                System.out.println("Error Code: " + sqlException.getErrorCode());
                System.out.println("Message: " + sqlException.getMessage());
                response.put("status", 500);
	            response.put("message", "Database error while storing new ticket (Boat).");
	            response.put("error", e.getMessage());
            } else {
                System.out.println("Message: " + rootCause.getMessage());
                response.put("status", 400);
    	        response.put("message", "Failed to insert a new ticket (Boat).");
            }
        }
		
        return Collections.singletonList(response);
    }
    
    @Transactional
	public List<Map<String, Object>> getTicketData(
			MultiValueMap<String, String> formData,
			String orderid,
			String ticketid) {
		
		String sqlQuery = "SELECT * From `tickets` WHERE (`isactive`=1 AND `isdelete`=0) ";
		
		if(orderid!=null && !orderid.isEmpty() && !orderid.isBlank()) {
			sqlQuery= sqlQuery +" AND `orderid`='"+orderid+"'";
		}
		if(ticketid!=null && !ticketid.isEmpty() && !ticketid.isBlank()) {
			sqlQuery= sqlQuery +" AND `ticketid`='"+ticketid+"'";
		}
		
		sqlQuery = sqlQuery + " LIMIT 1";
		
		//System.out.println(sqlQuery);
		List<Map<String, Object>> result = jdbcTemplate.queryForList(sqlQuery);
		Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Ticket Details");
        response.put("data", result);
		
        return result;
    }

	@Transactional
	public List<Map<String, Object>> storeBankTransaction(Map<String, Object> transactionData) {

	    Map<String, Object> response = new HashMap<>();
	    int lastInsertId = 0;
	    
	    // Extract data from the JSON object
	    String transactionResponseStatus = (String) transactionData.get("transactionResponseStatus");

	    // Check if the transaction was successful
	    if ("Transaction success".equalsIgnoreCase(transactionResponseStatus)) {
	    	
	    	String mid = (String) transactionData.get("mid");
	        String tid = (String) transactionData.get("tid");
	        String txnId = (String) transactionData.get("txnId");

	        // Fetch nested transaction response data
	        Map<String, Object> txnResponseData = (Map<String, Object>) transactionData.get("txnResponseData");

	        String bankName = (String) txnResponseData.get("bankName");
	        String batchNumber = (String) txnResponseData.get("batchNumber");
	        String transactionTitle = (String) txnResponseData.get("transactionTitle");
	        String txnAID = (String) txnResponseData.get("txnAID");
	        String txnApprCode = (String) txnResponseData.get("txnApprCode");
	        String txnCardNo = (String) txnResponseData.get("txnCardNo");
	        String txnCardType = (String) txnResponseData.get("txnCardType");
	        String txnDate = (String) txnResponseData.get("txnDate");
	        String txnInvoice = (String) txnResponseData.get("txnInvoice");
	        String txnMode = (String) txnResponseData.get("txnMode");
	        String txnRefNo = (String) txnResponseData.get("txnRefNo");
	        String txnTC = (String) txnResponseData.get("txnTC");
	        String txnTSI = (String) txnResponseData.get("txnTSI");
	        String txnTVR = (String) txnResponseData.get("txnTVR");
	        String txnTime = (String) txnResponseData.get("txnTime");
            
	        // Convert transaction amount properly
	        Double transactionAmount = Double.valueOf((String) txnResponseData.get("transactionAmount"));

	        String sqlQuery = "INSERT INTO bank_transactions (mid, tid, txnId, bankName, batchNumber, transactionAmount, "
	                + "transactionTitle, txnAID, txnApprCode, txnCardNo, txnCardType, txnDate, txnInvoice, txnMode, txnRefNo, "
	                + "txnTC, txnTSI, txnTVR, txnTime) "
	                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	        
	        KeyHolder keyHolder = new GeneratedKeyHolder();

	        try {
	            int affectedRows = jdbcTemplate.update(new PreparedStatementCreator() {
	                @Override
	                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
	                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
	                    ps.setString(1, mid);
	                    ps.setString(2, tid);
	                    ps.setString(3, txnId);
	                    ps.setString(4, bankName);
	                    ps.setString(5, batchNumber);
	                    ps.setDouble(6, transactionAmount);
	                    ps.setString(7, transactionTitle);
	                    ps.setString(8, txnAID);
	                    ps.setString(9, txnApprCode);
	                    ps.setString(10, txnCardNo);
	                    ps.setString(11, txnCardType);
	                    ps.setString(12, txnDate);
	                    ps.setString(13, txnInvoice);
	                    ps.setString(14, txnMode);
	                    ps.setString(15, txnRefNo);
	                    ps.setString(16, txnTC);
	                    ps.setString(17, txnTSI);
	                    ps.setString(18, txnTVR);
	                    ps.setString(19, txnTime);
	                    return ps;
	                }
	            }, keyHolder);

	            if (affectedRows > 0) {
	                Number generatedId = keyHolder.getKey();
	                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
	                
	                updatePaymentStatus(txnId, transactionResponseStatus);
	                
	                // Fetch inserted data for response, joining `penalty_challan` and `bank_transactions`
	                String sqlQuery_joined = "SELECT ts.*, "
	                        + "bt.mid, bt.tid, bt.txnId, bt.bankName, bt.batchNumber, bt.transactionAmount, bt.transactionTitle, "
	                        + "bt.txnCardNo, bt.txnDate, bt.txnInvoice, bt.txnMode, bt.txnRefNo "
	                        + "FROM tickets ts "
	                        + "LEFT JOIN bank_transactions bt ON ts.orderid = bt.txnId "
	                        + "WHERE ts.isactive = 1 AND ts.isdelete = 0 AND bt.id = ? LIMIT 1";
	                System.out.println(sqlQuery_joined);
	                // Execute the query
	                List<Map<String, Object>> result = jdbcTemplate.queryForList(sqlQuery_joined, lastInsertId);
	                response.put("status", 200);
	                response.put("message", "Transaction stored successfully!");
	                response.put("data", result);
	            } else {
	                response.put("status", 201);
	                response.put("message", "Failed to store the transaction.");
	                response.put("data", "");
	            }
	        } catch (DataAccessException e) {
	            response.put("status", 500);
	            response.put("message", "Database error while storing transaction.");
	            response.put("error", e.getMessage());
	            e.printStackTrace(); // This will give you more detail in logs
	        }
	    } else {
	        response.put("status", 400);
	        response.put("message", "Transaction failed or invalid status.");
	    }

	    return Collections.singletonList(response);
	}
	
	@Transactional
	public List<Map<String, Object>> storeFailedBankTransaction(Map<String, Object> transactionData) {

	    Map<String, Object> response = new HashMap<>();
	    int lastInsertId = 0;
	    
	    // Extract data from the JSON object
	    String transactionResponseStatus = (String) transactionData.get("transactionResponseStatus");

	    // Check if the transaction was successful
	    if ("Transaction cancelled".equalsIgnoreCase(transactionResponseStatus)) {
	    	
	    	String mid = (String) transactionData.get("mid");
	        String tid = (String) transactionData.get("tid");
	        String txnId = (String) transactionData.get("txnId");

	        // Fetch nested transaction response data
	        Map<String, Object> txnResponseData = (Map<String, Object>) transactionData.get("txnResponseData");

	        String bankName = (String) txnResponseData.get("bankName");
	        String batchNumber = (String) txnResponseData.get("batchNumber");
	        String transactionTitle = (String) txnResponseData.get("transactionTitle");
	        String txnAID = (String) txnResponseData.get("txnAID");
	        String txnApprCode = (String) txnResponseData.get("txnApprCode");
	        String txnCardNo = (String) txnResponseData.get("txnCardNo");
	        String txnCardType = (String) txnResponseData.get("txnCardType");
	        String txnDate = (String) txnResponseData.get("txnDate");
	        String txnInvoice = (String) txnResponseData.get("txnInvoice");
	        String txnMode = (String) txnResponseData.get("txnMode");
	        String txnRefNo = (String) txnResponseData.get("txnRefNo");
	        String txnTC = (String) txnResponseData.get("txnTC");
	        String txnTSI = (String) txnResponseData.get("txnTSI");
	        String txnTVR = (String) txnResponseData.get("txnTVR");
	        String txnTime = (String) txnResponseData.get("txnTime");
            
	        // Convert transaction amount properly
	        //Double transactionAmount = Double.valueOf((String) txnResponseData.get("transactionAmount"));

	     // Get the transactionAmount from txnResponseData
	        String transactionAmountStr = (String) txnResponseData.get("transactionAmount");

	        // Check if the transactionAmount is not null and not empty before converting
	        Double transactionAmountTemp = null;

	        if (transactionAmountStr != null && !transactionAmountStr.trim().isEmpty()) {
	            try {
	                // Convert the valid string to a Double
	                transactionAmountTemp = Double.valueOf(transactionAmountStr);
	            } catch (NumberFormatException e) {
	                // Handle the case where the string cannot be parsed to a Double // Log the error or throw a custom exception
	                System.out.println("Error: Unable to parse transactionAmount to Double. " + e.getMessage());
	                transactionAmountTemp = 0.0; // Set to a default value or handle as needed
	            }
	        } else {
	            // Handle the case where transactionAmount is empty or null
	            transactionAmountTemp = 0.0; // Default value or handle as needed
	        }
	        
	        Double transactionAmount = transactionAmountTemp;
	        
	        String sqlQuery = "INSERT INTO bank_failed_transactions (mid, tid, txnId, bankName, batchNumber, transactionAmount, "
	                + "transactionTitle, txnAID, txnApprCode, txnCardNo, txnCardType, txnDate, txnInvoice, txnMode, txnRefNo, "
	                + "txnTC, txnTSI, txnTVR, txnTime) "
	                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	        
	        KeyHolder keyHolder = new GeneratedKeyHolder();

	        try {
	            int affectedRows = jdbcTemplate.update(new PreparedStatementCreator() {
	                @Override
	                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
	                    PreparedStatement ps = connection.prepareStatement(sqlQuery, new String[]{"id"});
	                    ps.setString(1, mid);
	                    ps.setString(2, tid);
	                    ps.setString(3, txnId);
	                    ps.setString(4, bankName);
	                    ps.setString(5, batchNumber);
	                    ps.setDouble(6, transactionAmount);
	                    ps.setString(7, transactionTitle);
	                    ps.setString(8, txnAID);
	                    ps.setString(9, txnApprCode);
	                    ps.setString(10, txnCardNo);
	                    ps.setString(11, txnCardType);
	                    ps.setString(12, txnDate);
	                    ps.setString(13, txnInvoice);
	                    ps.setString(14, txnMode);
	                    ps.setString(15, txnRefNo);
	                    ps.setString(16, txnTC);
	                    ps.setString(17, txnTSI);
	                    ps.setString(18, txnTVR);
	                    ps.setString(19, txnTime);
	                    return ps;
	                }
	            }, keyHolder);

	            if (affectedRows > 0) {
	                Number generatedId = keyHolder.getKey();
	                lastInsertId = (generatedId != null) ? generatedId.intValue() : 0;
	                
	                updatePaymentStatus(txnId, transactionResponseStatus);
	                
	             // Fetch inserted data for response, joining `penalty_challan` and `bank_transactions`
	                String sqlQuery_joined = "SELECT ts.*, "
	                        + "bt.mid, bt.tid, bt.txnId, bt.bankName, bt.batchNumber, bt.transactionAmount, bt.transactionTitle, "
	                        + "bt.txnCardNo, bt.txnDate, bt.txnInvoice, bt.txnMode, bt.txnRefNo "
	                        + "FROM tickets ts "
	                        + "LEFT JOIN bank_transactions bt ON ts.orderid = bt.txnId "
	                        + "WHERE ts.isactive = 1 AND ts.isdelete = 0 AND bt.txnId = ? LIMIT 1";
	                System.out.println(sqlQuery_joined);
	                // Execute the query
	                List<Map<String, Object>> result = jdbcTemplate.queryForList(sqlQuery_joined, txnId);
	                response.put("status", 200);
	                response.put("message", "Transaction stored successfully!");
	                response.put("data", result);
	            } else {
	                response.put("status", 201);
	                response.put("message", "Failed to store the transaction.");
	                response.put("data", "");
	            }
	        } catch (DataAccessException e) {
	            response.put("status", 500);
	            response.put("message", "Database error while storing transaction.");
	            response.put("error", e.getMessage());
	            e.printStackTrace(); // This will give you more detail in logs
	        }
	    } else {
	        response.put("status", 400);
	        response.put("message", "Transaction failed or invalid status.");
	    }

	    return Collections.singletonList(response);
	}
	
	public String updatePaymentStatus(String orderid, String status) {
	    String sqlQuery = "UPDATE tickets SET status = ? WHERE orderid = ? LIMIT 1";
	    jdbcTemplate.update(sqlQuery, status, orderid);
	    return "success";
	}
	
}
