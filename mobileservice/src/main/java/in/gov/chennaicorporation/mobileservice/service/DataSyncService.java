package in.gov.chennaicorporation.mobileservice.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DataSyncService {

    private JdbcTemplate oracleERPJdbcTemplate;
    private JdbcTemplate mysqlJdbcTemplate;

    @Autowired
    @Qualifier("oracleERPDataSource")
    private DataSource oracleERPDataSource;

    @Autowired
    public void setDataSource(@Qualifier("oracleERPDataSource") DataSource oracleERPDataSource,
                              @Qualifier("mysqlPGRMasterDataSource") DataSource mysqlDataSource) {
        this.oracleERPJdbcTemplate = new JdbcTemplate(oracleERPDataSource);
        this.mysqlJdbcTemplate = new JdbcTemplate(mysqlDataSource);
    }
   
    private int getTotalRecordCount(String tablename) {
        String countQuery = "SELECT COUNT(*) FROM "+tablename;
        if(tablename.equals("EG_USER@erp")) {
    		countQuery = "SELECT COUNT(*) FROM "+tablename+" WHERE ISPORTALUSER!=1";
    	}
        if(tablename.equals("EGGR_COMPLAINTDETAILS@erp")) {
        	countQuery = "SELECT COUNT(*) FROM "+tablename+" WHERE YEAR(COMPLAINTDATE) = '2023'";
    	}
    	if(tablename.equals("EGGR_REDRESSALDETAILS@erp")) {
    		countQuery = "SELECT COUNT(*) FROM "+tablename+" WHERE YEAR(RESPONSEDATE) = '2023'";
    	}
        return oracleERPJdbcTemplate.queryForObject(countQuery, Integer.class);
    }

    private List<Map<String, Object>> fetchBatch(int startRow, int endRow, String tablename) {
    	String fetchQuery = "";
    	fetchQuery = "SELECT * FROM (SELECT a.*, ROWNUM rnum FROM (SELECT * FROM "+tablename+") a " +
                "WHERE ROWNUM <= ?) WHERE rnum > ?";
    	if(tablename.equals("EG_USER@erp")) {
    		fetchQuery = "SELECT * FROM (SELECT a.*, ROWNUM rnum FROM (SELECT * FROM "+tablename+" WHERE ISPORTALUSER!=1) a " +
                    "WHERE ROWNUM <= ?) WHERE rnum > ?";
    	}
    	if(tablename.equals("EGGR_COMPLAINTDETAILS@erp")) {
    		fetchQuery = "SELECT * FROM (SELECT a.*, ROWNUM rnum FROM (SELECT * FROM "+tablename+" WHERE YEAR(COMPLAINTDATE) = '2023') a " +
                    "WHERE ROWNUM <= ?) WHERE rnum > ?";
    	}
    	if(tablename.equals("EGGR_REDRESSALDETAILS@erp")) {
    		fetchQuery = "SELECT * FROM (SELECT a.*, ROWNUM rnum FROM (SELECT * FROM "+tablename+" WHERE AND YEAR(RESPONSEDATE) = '2023') a " +
    				"WHERE ROWNUM <= ?) WHERE rnum > ?";
    	}
        return oracleERPJdbcTemplate.queryForList(fetchQuery, endRow, startRow);
    }
    
    private List<Map<String, Object>> fetchExistingData(List<Object> ids, String tablename, String primaryKeyColumn) {
        String idList = ids.stream().map(Objects::toString).collect(Collectors.joining(", "));
        String fetchQuery = "SELECT * FROM "+tablename+" WHERE "+primaryKeyColumn+" IN (" + idList + ")";
        System.out.println(fetchQuery);
        return mysqlJdbcTemplate.queryForList(fetchQuery);
    }
    
    private boolean compareAndPrint(Object existingPrimaryKeyValue, Object primaryKeyValue) {
        Integer existingKey = (existingPrimaryKeyValue instanceof Integer) ? (Integer) existingPrimaryKeyValue : null;
        Integer keyToCompare = (primaryKeyValue instanceof BigDecimal) ? ((BigDecimal) primaryKeyValue).intValue() : null;

        //System.out.println("Comparing existingPrimaryKeyValue: " + existingKey + " (" + (existingKey != null ? existingKey.getClass().getName() : "null") + ") with primaryKeyValue: " + keyToCompare + " (" + (keyToCompare != null ? keyToCompare.getClass().getName() : "null") + ")");

        return Objects.equals(existingKey, keyToCompare);
    }
    
    public void syncEgUserData() {
        int batchSize = 10000;
        int totalRecords = getTotalRecordCount("EG_USER@erp");
        int totalBatches = (totalRecords + batchSize - 1) / batchSize; // Round up

        System.out.println("totalRecords : " + totalRecords + "\n"
                + "totalBatches : " + totalBatches);

        String autoIncrementColumn = "id"; // Replace 'id' with the actual auto-increment column name if different
        String primaryKeyColumn = "id_user"; // Replace 'id_user' with the actual primary key column name if different

        for (int batch = 0; batch < totalBatches; batch++) {
            int startRow = batch * batchSize;
            int endRow = startRow + batchSize;

            long startTime = System.currentTimeMillis();
            List<Map<String, Object>> rows = fetchBatch(startRow, endRow,"EG_USER@erp");
            long endTime = System.currentTimeMillis();
            long queryTime = endTime - startTime;
            System.out.println("Fetch Batch Time Taken: " + queryTime + " ms");

            if (!rows.isEmpty()) {
                // Extract primary keys from the fetched rows
                List<Object> ids = rows.stream()
                        .map(row -> row.get(primaryKeyColumn))
                        .collect(Collectors.toList());

                // Fetch existing data from MySQL
                startTime = System.currentTimeMillis();
                List<Map<String, Object>> existingRows = fetchExistingData(ids,"EG_USER",primaryKeyColumn);
                endTime = System.currentTimeMillis();
                queryTime = endTime - startTime;
                System.out.println("Fetch Existing Data Time Taken: " + queryTime + " ms");

                StringBuilder insertQuery = new StringBuilder("INSERT INTO EG_USER (");
                StringBuilder updateQuery = new StringBuilder(" ON DUPLICATE KEY UPDATE ");
                boolean first = true;

                for (String key : rows.get(0).keySet()) {
                    if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
                        continue; // Skip the auto-increment column and RNUM
                    }
                    if (!first) {
                        insertQuery.append(", ");
                        updateQuery.append(", ");
                    }
                    first = false;
                    insertQuery.append(key);
                    updateQuery.append(key).append("=VALUES(").append(key).append(")");
                }
                insertQuery.append(") VALUES (");
                first = true;
                for (String key : rows.get(0).keySet()) {
                    if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
                        continue; // Skip the auto-increment column and RNUM
                    }
                    if (!first) {
                        insertQuery.append(", ");
                    }
                    first = false;
                    insertQuery.append("?");
                }
                insertQuery.append(")").append(updateQuery.toString());

                startTime = System.currentTimeMillis();
                for (Map<String, Object> row : rows) {
                    Object primaryKeyValue = row.get(primaryKeyColumn);

                    // Filter to find if there is any existing row with the same primary key value
                    Map<String, Object> existingRow = existingRows.stream()
                            .filter(eRow -> compareAndPrint(eRow.get(primaryKeyColumn), primaryKeyValue))
                            .findFirst()
                            .orElse(null);

                    if (existingRow != null) {
                        System.out.println("Match found: primaryKeyValue: " + primaryKeyValue + " == existingPrimaryKeyValue: " + existingRow.get(primaryKeyColumn));
                        boolean hasChanges = !row.equals(existingRow);
                        if (hasChanges) {
                            Object[] values = row.entrySet().stream()
                                    .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                    .map(Map.Entry::getValue)
                                    .toArray();
                            mysqlJdbcTemplate.update(insertQuery.toString(), values);
                            System.out.println("Updated: " + primaryKeyValue);
                        }
                    } else {
                        System.out.println("No match found: primaryKeyValue: " + primaryKeyValue);
                        Object[] values = row.entrySet().stream()
                                .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                .map(Map.Entry::getValue)
                                .toArray();
                        mysqlJdbcTemplate.update(insertQuery.toString(), values);
                        System.out.println("Inserted: " + primaryKeyValue);
                    }
                }
                endTime = System.currentTimeMillis();
                queryTime = endTime - startTime;
                System.out.println("Insert/Update Time Taken: " + queryTime + " ms");

                System.out.println("Batch " + (batch+1) + " Completed\n");
            }
        }
    }
    
    public void syncEgDepartmentData() {
        
    	int batchSize = 10000;
        int totalRecords = getTotalRecordCount("EG_DEPARTMENT@erp");
        int totalBatches = (totalRecords + batchSize - 1) / batchSize; // Round up

        System.out.println("totalRecords : " + totalRecords + "\n"
                + "totalBatches : " + totalBatches);

        String autoIncrementColumn = "id"; // Replace 'id' with the actual auto-increment column name if different
        String primaryKeyColumn = "ID_DEPT"; // Replace 'id_user' with the actual primary key column name if different

        for (int batch = 0; batch < totalBatches; batch++) {
            int startRow = batch * batchSize;
            int endRow = startRow + batchSize;

            long startTime = System.currentTimeMillis();
            List<Map<String, Object>> rows = fetchBatch(startRow, endRow,"EG_DEPARTMENT@erp");
            long endTime = System.currentTimeMillis();
            long queryTime = endTime - startTime;
            System.out.println("Fetch Batch Time Taken: " + queryTime + " ms");

            if (!rows.isEmpty()) {
                // Extract primary keys from the fetched rows
                List<Object> ids = rows.stream()
                        .map(row -> row.get(primaryKeyColumn))
                        .collect(Collectors.toList());

                // Fetch existing data from MySQL
                startTime = System.currentTimeMillis();
                List<Map<String, Object>> existingRows = fetchExistingData(ids,"EG_DEPARTMENT",primaryKeyColumn);
                endTime = System.currentTimeMillis();
                queryTime = endTime - startTime;
                System.out.println("Fetch Existing Data Time Taken: " + queryTime + " ms");

                StringBuilder insertQuery = new StringBuilder("INSERT INTO EG_DEPARTMENT (");
                StringBuilder updateQuery = new StringBuilder(" ON DUPLICATE KEY UPDATE ");
                boolean first = true;

                for (String key : rows.get(0).keySet()) {
                    if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
                        continue; // Skip the auto-increment column and RNUM
                    }
                    if (!first) {
                        insertQuery.append(", ");
                        updateQuery.append(", ");
                    }
                    first = false;
                    insertQuery.append(key);
                    updateQuery.append(key).append("=VALUES(").append(key).append(")");
                }
                insertQuery.append(") VALUES (");
                first = true;
                for (String key : rows.get(0).keySet()) {
                    if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
                        continue; // Skip the auto-increment column and RNUM
                    }
                    if (!first) {
                        insertQuery.append(", ");
                    }
                    first = false;
                    insertQuery.append("?");
                }
                insertQuery.append(")").append(updateQuery.toString());

                startTime = System.currentTimeMillis();
                for (Map<String, Object> row : rows) {
                    Object primaryKeyValue = row.get(primaryKeyColumn);

                    // Filter to find if there is any existing row with the same primary key value
                    Map<String, Object> existingRow = existingRows.stream()
                            .filter(eRow -> compareAndPrint(eRow.get(primaryKeyColumn), primaryKeyValue))
                            .findFirst()
                            .orElse(null);

                    if (existingRow != null) {
                        System.out.println("Match found: primaryKeyValue: " + primaryKeyValue + " == existingPrimaryKeyValue: " + existingRow.get(primaryKeyColumn));
                        boolean hasChanges = !row.equals(existingRow);
                        if (hasChanges) {
                            Object[] values = row.entrySet().stream()
                                    .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                    .map(Map.Entry::getValue)
                                    .toArray();
                            mysqlJdbcTemplate.update(insertQuery.toString(), values);
                            System.out.println("Updated: " + primaryKeyValue);
                        }
                    } else {
                        System.out.println("No match found: primaryKeyValue: " + primaryKeyValue);
                        Object[] values = row.entrySet().stream()
                                .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                .map(Map.Entry::getValue)
                                .toArray();
                        mysqlJdbcTemplate.update(insertQuery.toString(), values);
                        System.out.println("Inserted: " + primaryKeyValue);
                    }
                }
                endTime = System.currentTimeMillis();
                queryTime = endTime - startTime;
                System.out.println("Insert/Update Time Taken: " + queryTime + " ms");

                System.out.println("Batch " + (batch+1) + " Completed\n");
            }
        }
    }
    
    public void syncEgBoundaryData() {
        
    	int batchSize = 10000;
        int totalRecords = getTotalRecordCount("EG_BOUNDARY@erp");
        int totalBatches = (totalRecords + batchSize - 1) / batchSize; // Round up

        System.out.println("totalRecords : " + totalRecords + "\n"
                + "totalBatches : " + totalBatches);

        String autoIncrementColumn = "id"; // Replace 'id' with the actual auto-increment column name if different
        String primaryKeyColumn = "ID_BNDRY"; // Replace 'id_user' with the actual primary key column name if different

        for (int batch = 0; batch < totalBatches; batch++) {
            int startRow = batch * batchSize;
            int endRow = startRow + batchSize;

            long startTime = System.currentTimeMillis();
            List<Map<String, Object>> rows = fetchBatch(startRow, endRow,"EG_BOUNDARY@erp");
            long endTime = System.currentTimeMillis();
            long queryTime = endTime - startTime;
            System.out.println("Fetch Batch Time Taken: " + queryTime + " ms");

            if (!rows.isEmpty()) {
                // Extract primary keys from the fetched rows
                List<Object> ids = rows.stream()
                        .map(row -> row.get(primaryKeyColumn))
                        .collect(Collectors.toList());

                // Fetch existing data from MySQL
                startTime = System.currentTimeMillis();
                List<Map<String, Object>> existingRows = fetchExistingData(ids,"EG_BOUNDARY",primaryKeyColumn);
                endTime = System.currentTimeMillis();
                queryTime = endTime - startTime;
                System.out.println("Fetch Existing Data Time Taken: " + queryTime + " ms");

                StringBuilder insertQuery = new StringBuilder("INSERT INTO EG_BOUNDARY (");
                StringBuilder updateQuery = new StringBuilder(" ON DUPLICATE KEY UPDATE ");
                boolean first = true;

                for (String key : rows.get(0).keySet()) {
                    if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
                        continue; // Skip the auto-increment column and RNUM
                    }
                    if (!first) {
                        insertQuery.append(", ");
                        updateQuery.append(", ");
                    }
                    first = false;
                    insertQuery.append(key);
                    updateQuery.append(key).append("=VALUES(").append(key).append(")");
                }
                insertQuery.append(") VALUES (");
                first = true;
                for (String key : rows.get(0).keySet()) {
                    if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
                        continue; // Skip the auto-increment column and RNUM
                    }
                    if (!first) {
                        insertQuery.append(", ");
                    }
                    first = false;
                    insertQuery.append("?");
                }
                insertQuery.append(")").append(updateQuery.toString());

                startTime = System.currentTimeMillis();
                for (Map<String, Object> row : rows) {
                    Object primaryKeyValue = row.get(primaryKeyColumn);

                    // Filter to find if there is any existing row with the same primary key value
                    Map<String, Object> existingRow = existingRows.stream()
                            .filter(eRow -> compareAndPrint(eRow.get(primaryKeyColumn), primaryKeyValue))
                            .findFirst()
                            .orElse(null);

                    if (existingRow != null) {
                        System.out.println("Match found: primaryKeyValue: " + primaryKeyValue + " == existingPrimaryKeyValue: " + existingRow.get(primaryKeyColumn));
                        boolean hasChanges = !row.equals(existingRow);
                        if (hasChanges) {
                            Object[] values = row.entrySet().stream()
                                    .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                    .map(Map.Entry::getValue)
                                    .toArray();
                            mysqlJdbcTemplate.update(insertQuery.toString(), values);
                            System.out.println("Updated: " + primaryKeyValue);
                        }
                    } else {
                        System.out.println("No match found: primaryKeyValue: " + primaryKeyValue);
                        Object[] values = row.entrySet().stream()
                                .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                .map(Map.Entry::getValue)
                                .toArray();
                        mysqlJdbcTemplate.update(insertQuery.toString(), values);
                        System.out.println("Inserted: " + primaryKeyValue);
                    }
                }
                endTime = System.currentTimeMillis();
                queryTime = endTime - startTime;
                System.out.println("Insert/Update Time Taken: " + queryTime + " ms");

                System.out.println("Batch " + (batch+1) + " Completed\n");
            }
        }
    }

    public void syncEggrComplaintStatusData() {
    
		int batchSize = 10000;
	    int totalRecords = getTotalRecordCount("EGGR_COMPLAINTSTATUS@erp");
	    int totalBatches = (totalRecords + batchSize - 1) / batchSize; // Round up
	
	    System.out.println("totalRecords : " + totalRecords + "\n"
	            + "totalBatches : " + totalBatches);
	
	    String autoIncrementColumn = "id"; // Replace 'id' with the actual auto-increment column name if different
	    String primaryKeyColumn = "COMPLAINTSTATUSID"; // Replace 'id_user' with the actual primary key column name if different
	
	    for (int batch = 0; batch < totalBatches; batch++) {
	        int startRow = batch * batchSize;
	        int endRow = startRow + batchSize;
	
	        long startTime = System.currentTimeMillis();
	        List<Map<String, Object>> rows = fetchBatch(startRow, endRow,"EGGR_COMPLAINTSTATUS@erp");
	        long endTime = System.currentTimeMillis();
	        long queryTime = endTime - startTime;
	        System.out.println("Fetch Batch Time Taken: " + queryTime + " ms");
	
	        if (!rows.isEmpty()) {
	            // Extract primary keys from the fetched rows
	            List<Object> ids = rows.stream()
	                    .map(row -> row.get(primaryKeyColumn))
	                    .collect(Collectors.toList());
	
	            // Fetch existing data from MySQL
	            startTime = System.currentTimeMillis();
	            List<Map<String, Object>> existingRows = fetchExistingData(ids,"EGGR_COMPLAINTSTATUS",primaryKeyColumn);
	            endTime = System.currentTimeMillis();
	            queryTime = endTime - startTime;
	            System.out.println("Fetch Existing Data Time Taken: " + queryTime + " ms");
	
	            StringBuilder insertQuery = new StringBuilder("INSERT INTO EGGR_COMPLAINTSTATUS (");
	            StringBuilder updateQuery = new StringBuilder(" ON DUPLICATE KEY UPDATE ");
	            boolean first = true;
	
	            for (String key : rows.get(0).keySet()) {
	                if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
	                    continue; // Skip the auto-increment column and RNUM
	                }
	                if (!first) {
	                    insertQuery.append(", ");
	                    updateQuery.append(", ");
	                }
	                first = false;
	                insertQuery.append(key);
	                updateQuery.append(key).append("=VALUES(").append(key).append(")");
	            }
	            insertQuery.append(") VALUES (");
	            first = true;
	            for (String key : rows.get(0).keySet()) {
	                if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
	                    continue; // Skip the auto-increment column and RNUM
	                }
	                if (!first) {
	                    insertQuery.append(", ");
	                }
	                first = false;
	                insertQuery.append("?");
	            }
	            insertQuery.append(")").append(updateQuery.toString());
	
	            startTime = System.currentTimeMillis();
                for (Map<String, Object> row : rows) {
                    Object primaryKeyValue = row.get(primaryKeyColumn);

                    // Filter to find if there is any existing row with the same primary key value
                    Map<String, Object> existingRow = existingRows.stream()
                            .filter(eRow -> compareAndPrint(eRow.get(primaryKeyColumn), primaryKeyValue))
                            .findFirst()
                            .orElse(null);

                    if (existingRow != null) {
                        System.out.println("Match found: primaryKeyValue: " + primaryKeyValue + " == existingPrimaryKeyValue: " + existingRow.get(primaryKeyColumn));
                        boolean hasChanges = !row.equals(existingRow);
                        if (hasChanges) {
                            Object[] values = row.entrySet().stream()
                                    .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                    .map(Map.Entry::getValue)
                                    .toArray();
                            mysqlJdbcTemplate.update(insertQuery.toString(), values);
                            System.out.println("Updated: " + primaryKeyValue);
                        }
                    } else {
                        System.out.println("No match found: primaryKeyValue: " + primaryKeyValue);
                        Object[] values = row.entrySet().stream()
                                .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                .map(Map.Entry::getValue)
                                .toArray();
                        mysqlJdbcTemplate.update(insertQuery.toString(), values);
                        System.out.println("Inserted: " + primaryKeyValue);
                    }
                }
                endTime = System.currentTimeMillis();
	            queryTime = endTime - startTime;
	            System.out.println("Insert/Update Time Taken: " + queryTime + " ms");
	
	            System.out.println("Batch " + (batch+1) + " Completed\n");
	        }
	    }
	}
    
    public void syncEggrComplaintTypesData() {
        
		int batchSize = 10000;
	    int totalRecords = getTotalRecordCount("EGGR_COMPLAINTTYPES@erp");
	    int totalBatches = (totalRecords + batchSize - 1) / batchSize; // Round up
	
	    System.out.println("totalRecords : " + totalRecords + "\n"
	            + "totalBatches : " + totalBatches);
	
	    String autoIncrementColumn = "id"; // Replace 'id' with the actual auto-increment column name if different
	    String primaryKeyColumn = "COMPLAINTTYPEID"; // Replace 'id_user' with the actual primary key column name if different
	
	    for (int batch = 0; batch < totalBatches; batch++) {
	        int startRow = batch * batchSize;
	        int endRow = startRow + batchSize;
	
	        long startTime = System.currentTimeMillis();
	        List<Map<String, Object>> rows = fetchBatch(startRow, endRow,"EGGR_COMPLAINTTYPES@erp");
	        long endTime = System.currentTimeMillis();
	        long queryTime = endTime - startTime;
	        System.out.println("Fetch Batch Time Taken: " + queryTime + " ms");
	
	        if (!rows.isEmpty()) {
	            // Extract primary keys from the fetched rows
	            List<Object> ids = rows.stream()
	                    .map(row -> row.get(primaryKeyColumn))
	                    .collect(Collectors.toList());
	
	            // Fetch existing data from MySQL
	            startTime = System.currentTimeMillis();
	            List<Map<String, Object>> existingRows = fetchExistingData(ids,"EGGR_COMPLAINTTYPES",primaryKeyColumn);
	            endTime = System.currentTimeMillis();
	            queryTime = endTime - startTime;
	            System.out.println("Fetch Existing Data Time Taken: " + queryTime + " ms");
	
	            StringBuilder insertQuery = new StringBuilder("INSERT INTO EGGR_COMPLAINTTYPES (");
	            StringBuilder updateQuery = new StringBuilder(" ON DUPLICATE KEY UPDATE ");
	            boolean first = true;
	
	            for (String key : rows.get(0).keySet()) {
	                if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
	                    continue; // Skip the auto-increment column and RNUM
	                }
	                if (!first) {
	                    insertQuery.append(", ");
	                    updateQuery.append(", ");
	                }
	                first = false;
	                insertQuery.append(key);
	                updateQuery.append(key).append("=VALUES(").append(key).append(")");
	            }
	            insertQuery.append(") VALUES (");
	            first = true;
	            for (String key : rows.get(0).keySet()) {
	                if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
	                    continue; // Skip the auto-increment column and RNUM
	                }
	                if (!first) {
	                    insertQuery.append(", ");
	                }
	                first = false;
	                insertQuery.append("?");
	            }
	            insertQuery.append(")").append(updateQuery.toString());
	
	            startTime = System.currentTimeMillis();
                for (Map<String, Object> row : rows) {
                    Object primaryKeyValue = row.get(primaryKeyColumn);

                    // Filter to find if there is any existing row with the same primary key value
                    Map<String, Object> existingRow = existingRows.stream()
                            .filter(eRow -> compareAndPrint(eRow.get(primaryKeyColumn), primaryKeyValue))
                            .findFirst()
                            .orElse(null);

                    if (existingRow != null) {
                        System.out.println("Match found: primaryKeyValue: " + primaryKeyValue + " == existingPrimaryKeyValue: " + existingRow.get(primaryKeyColumn));
                        boolean hasChanges = !row.equals(existingRow);
                        if (hasChanges) {
                            Object[] values = row.entrySet().stream()
                                    .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                    .map(Map.Entry::getValue)
                                    .toArray();
                            mysqlJdbcTemplate.update(insertQuery.toString(), values);
                            System.out.println("Updated: " + primaryKeyValue);
                        }
                    } else {
                        System.out.println("No match found: primaryKeyValue: " + primaryKeyValue);
                        Object[] values = row.entrySet().stream()
                                .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                .map(Map.Entry::getValue)
                                .toArray();
                        mysqlJdbcTemplate.update(insertQuery.toString(), values);
                        System.out.println("Inserted: " + primaryKeyValue);
                    }
                }
                endTime = System.currentTimeMillis();
	            queryTime = endTime - startTime;
	            System.out.println("Insert/Update Time Taken: " + queryTime + " ms");
	
	            System.out.println("Batch " + (batch+1) + " Completed\n");
	        }
	    }
	}
    
    public void syncEggrComplaintGroupData() {
        
		int batchSize = 10000;
	    int totalRecords = getTotalRecordCount("EGGR_COMPLAINTGROUP@erp");
	    int totalBatches = (totalRecords + batchSize - 1) / batchSize; // Round up
	
	    System.out.println("totalRecords : " + totalRecords + "\n"
	            + "totalBatches : " + totalBatches);
	
	    String autoIncrementColumn = "id"; // Replace 'id' with the actual auto-increment column name if different
	    String primaryKeyColumn = "ID_COMPLAINTGROUP"; // Replace 'id_user' with the actual primary key column name if different
	
	    for (int batch = 0; batch < totalBatches; batch++) {
	        int startRow = batch * batchSize;
	        int endRow = startRow + batchSize;
	
	        long startTime = System.currentTimeMillis();
	        List<Map<String, Object>> rows = fetchBatch(startRow, endRow,"EGGR_COMPLAINTGROUP@erp");
	        long endTime = System.currentTimeMillis();
	        long queryTime = endTime - startTime;
	        System.out.println("Fetch Batch Time Taken: " + queryTime + " ms");
	
	        if (!rows.isEmpty()) {
	            // Extract primary keys from the fetched rows
	            List<Object> ids = rows.stream()
	                    .map(row -> row.get(primaryKeyColumn))
	                    .collect(Collectors.toList());
	
	            // Fetch existing data from MySQL
	            startTime = System.currentTimeMillis();
	            List<Map<String, Object>> existingRows = fetchExistingData(ids,"EGGR_COMPLAINTGROUP",primaryKeyColumn);
	            endTime = System.currentTimeMillis();
	            queryTime = endTime - startTime;
	            System.out.println("Fetch Existing Data Time Taken: " + queryTime + " ms");
	
	            StringBuilder insertQuery = new StringBuilder("INSERT INTO EGGR_COMPLAINTGROUP (");
	            StringBuilder updateQuery = new StringBuilder(" ON DUPLICATE KEY UPDATE ");
	            boolean first = true;
	
	            for (String key : rows.get(0).keySet()) {
	                if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
	                    continue; // Skip the auto-increment column and RNUM
	                }
	                if (!first) {
	                    insertQuery.append(", ");
	                    updateQuery.append(", ");
	                }
	                first = false;
	                insertQuery.append(key);
	                updateQuery.append(key).append("=VALUES(").append(key).append(")");
	            }
	            insertQuery.append(") VALUES (");
	            first = true;
	            for (String key : rows.get(0).keySet()) {
	                if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
	                    continue; // Skip the auto-increment column and RNUM
	                }
	                if (!first) {
	                    insertQuery.append(", ");
	                }
	                first = false;
	                insertQuery.append("?");
	            }
	            insertQuery.append(")").append(updateQuery.toString());
	
	            startTime = System.currentTimeMillis();
                for (Map<String, Object> row : rows) {
                    Object primaryKeyValue = row.get(primaryKeyColumn);

                    // Filter to find if there is any existing row with the same primary key value
                    Map<String, Object> existingRow = existingRows.stream()
                            .filter(eRow -> compareAndPrint(eRow.get(primaryKeyColumn), primaryKeyValue))
                            .findFirst()
                            .orElse(null);

                    if (existingRow != null) {
                        System.out.println("Match found: primaryKeyValue: " + primaryKeyValue + " == existingPrimaryKeyValue: " + existingRow.get(primaryKeyColumn));
                        boolean hasChanges = !row.equals(existingRow);
                        if (hasChanges) {
                            Object[] values = row.entrySet().stream()
                                    .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                    .map(Map.Entry::getValue)
                                    .toArray();
                            mysqlJdbcTemplate.update(insertQuery.toString(), values);
                            System.out.println("Updated: " + primaryKeyValue);
                        }
                    } else {
                        System.out.println("No match found: primaryKeyValue: " + primaryKeyValue);
                        Object[] values = row.entrySet().stream()
                                .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                .map(Map.Entry::getValue)
                                .toArray();
                        mysqlJdbcTemplate.update(insertQuery.toString(), values);
                        System.out.println("Inserted: " + primaryKeyValue);
                    }
                }
                endTime = System.currentTimeMillis();
	            queryTime = endTime - startTime;
	            System.out.println("Insert/Update Time Taken: " + queryTime + " ms");
	
	            System.out.println("Batch " + (batch+1) + " Completed\n");
	        }
	    }
	}

    public void syncEggrComplaintDetailsData() {
        
		int batchSize = 10000;
	    int totalRecords = getTotalRecordCount("EGGR_COMPLAINTDETAILS@erp");
	    int totalBatches = (totalRecords + batchSize - 1) / batchSize; // Round up
	
	    System.out.println("totalRecords : " + totalRecords + "\n"
	            + "totalBatches : " + totalBatches);
	
	    String autoIncrementColumn = "id"; // Replace 'id' with the actual auto-increment column name if different
	    String primaryKeyColumn = "COMPLAINTID"; // Replace 'id_user' with the actual primary key column name if different
	
	    for (int batch = 0; batch < totalBatches; batch++) {
	        int startRow = batch * batchSize;
	        int endRow = startRow + batchSize;
	
	        long startTime = System.currentTimeMillis();
	        List<Map<String, Object>> rows = fetchBatch(startRow, endRow,"EGGR_COMPLAINTDETAILS@erp");
	        long endTime = System.currentTimeMillis();
	        long queryTime = endTime - startTime;
	        System.out.println("Fetch Batch Time Taken: " + queryTime + " ms");
	
	        if (!rows.isEmpty()) {
	            // Extract primary keys from the fetched rows
	            List<Object> ids = rows.stream()
	                    .map(row -> row.get(primaryKeyColumn))
	                    .collect(Collectors.toList());
	
	            // Fetch existing data from MySQL
	            startTime = System.currentTimeMillis();
	            List<Map<String, Object>> existingRows = fetchExistingData(ids,"EGGR_COMPLAINTDETAILS",primaryKeyColumn);
	            endTime = System.currentTimeMillis();
	            queryTime = endTime - startTime;
	            System.out.println("Fetch Existing Data Time Taken: " + queryTime + " ms");
	
	            StringBuilder insertQuery = new StringBuilder("INSERT INTO EGGR_COMPLAINTDETAILS (");
	            StringBuilder updateQuery = new StringBuilder(" ON DUPLICATE KEY UPDATE ");
	            boolean first = true;
	
	            for (String key : rows.get(0).keySet()) {
	                if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
	                    continue; // Skip the auto-increment column and RNUM
	                }
	                if (!first) {
	                    insertQuery.append(", ");
	                    updateQuery.append(", ");
	                }
	                first = false;
	                insertQuery.append(key);
	                updateQuery.append(key).append("=VALUES(").append(key).append(")");
	            }
	            insertQuery.append(") VALUES (");
	            first = true;
	            for (String key : rows.get(0).keySet()) {
	                if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
	                    continue; // Skip the auto-increment column and RNUM
	                }
	                if (!first) {
	                    insertQuery.append(", ");
	                }
	                first = false;
	                insertQuery.append("?");
	            }
	            insertQuery.append(")").append(updateQuery.toString());
	
	            startTime = System.currentTimeMillis();
                for (Map<String, Object> row : rows) {
                    Object primaryKeyValue = row.get(primaryKeyColumn);

                    // Filter to find if there is any existing row with the same primary key value
                    Map<String, Object> existingRow = existingRows.stream()
                            .filter(eRow -> compareAndPrint(eRow.get(primaryKeyColumn), primaryKeyValue))
                            .findFirst()
                            .orElse(null);

                    if (existingRow != null) {
                        System.out.println("Match found: primaryKeyValue: " + primaryKeyValue + " == existingPrimaryKeyValue: " + existingRow.get(primaryKeyColumn));
                        boolean hasChanges = !row.equals(existingRow);
                        if (hasChanges) {
                            Object[] values = row.entrySet().stream()
                                    .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                    .map(Map.Entry::getValue)
                                    .toArray();
                            mysqlJdbcTemplate.update(insertQuery.toString(), values);
                            System.out.println("Updated: " + primaryKeyValue);
                        }
                    } else {
                        System.out.println("No match found: primaryKeyValue: " + primaryKeyValue);
                        Object[] values = row.entrySet().stream()
                                .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                .map(Map.Entry::getValue)
                                .toArray();
                        mysqlJdbcTemplate.update(insertQuery.toString(), values);
                        System.out.println("Inserted: " + primaryKeyValue);
                    }
                }
                endTime = System.currentTimeMillis();
	            queryTime = endTime - startTime;
	            System.out.println("Insert/Update Time Taken: " + queryTime + " ms");
	
	            System.out.println("Batch " + (batch+1) + " Completed\n");
	        }
	    }
	}
    
public void syncEggrRedressalDetailsData() {
        
		int batchSize = 10000;
	    int totalRecords = getTotalRecordCount("EGGR_REDRESSALDETAILS@erp");
	    int totalBatches = (totalRecords + batchSize - 1) / batchSize; // Round up
	
	    System.out.println("totalRecords : " + totalRecords + "\n"
	            + "totalBatches : " + totalBatches);
	
	    String autoIncrementColumn = "id"; // Replace 'id' with the actual auto-increment column name if different
	    String primaryKeyColumn = "REDRESSALID"; // Replace 'id_user' with the actual primary key column name if different
	
	    for (int batch = 0; batch < totalBatches; batch++) {
	        int startRow = batch * batchSize;
	        int endRow = startRow + batchSize;
	
	        long startTime = System.currentTimeMillis();
	        List<Map<String, Object>> rows = fetchBatch(startRow, endRow,"EGGR_REDRESSALDETAILS@erp");
	        long endTime = System.currentTimeMillis();
	        long queryTime = endTime - startTime;
	        System.out.println("Fetch Batch Time Taken: " + queryTime + " ms");
	
	        if (!rows.isEmpty()) {
	            // Extract primary keys from the fetched rows
	            List<Object> ids = rows.stream()
	                    .map(row -> row.get(primaryKeyColumn))
	                    .collect(Collectors.toList());
	
	            // Fetch existing data from MySQL
	            startTime = System.currentTimeMillis();
	            List<Map<String, Object>> existingRows = fetchExistingData(ids,"EGGR_REDRESSALDETAILS",primaryKeyColumn);
	            endTime = System.currentTimeMillis();
	            queryTime = endTime - startTime;
	            System.out.println("Fetch Existing Data Time Taken: " + queryTime + " ms");
	
	            StringBuilder insertQuery = new StringBuilder("INSERT INTO EGGR_REDRESSALDETAILS (");
	            StringBuilder updateQuery = new StringBuilder(" ON DUPLICATE KEY UPDATE ");
	            boolean first = true;
	
	            for (String key : rows.get(0).keySet()) {
	                if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
	                    continue; // Skip the auto-increment column and RNUM
	                }
	                if (!first) {
	                    insertQuery.append(", ");
	                    updateQuery.append(", ");
	                }
	                first = false;
	                insertQuery.append(key);
	                updateQuery.append(key).append("=VALUES(").append(key).append(")");
	            }
	            insertQuery.append(") VALUES (");
	            first = true;
	            for (String key : rows.get(0).keySet()) {
	                if (key.equalsIgnoreCase(autoIncrementColumn) || key.equalsIgnoreCase("RNUM")) {
	                    continue; // Skip the auto-increment column and RNUM
	                }
	                if (!first) {
	                    insertQuery.append(", ");
	                }
	                first = false;
	                insertQuery.append("?");
	            }
	            insertQuery.append(")").append(updateQuery.toString());
	
	            startTime = System.currentTimeMillis();
                for (Map<String, Object> row : rows) {
                    Object primaryKeyValue = row.get(primaryKeyColumn);

                    // Filter to find if there is any existing row with the same primary key value
                    Map<String, Object> existingRow = existingRows.stream()
                            .filter(eRow -> compareAndPrint(eRow.get(primaryKeyColumn), primaryKeyValue))
                            .findFirst()
                            .orElse(null);

                    if (existingRow != null) {
                        System.out.println("Match found: primaryKeyValue: " + primaryKeyValue + " == existingPrimaryKeyValue: " + existingRow.get(primaryKeyColumn));
                        boolean hasChanges = !row.equals(existingRow);
                        if (hasChanges) {
                            Object[] values = row.entrySet().stream()
                                    .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                    .map(Map.Entry::getValue)
                                    .toArray();
                            mysqlJdbcTemplate.update(insertQuery.toString(), values);
                            System.out.println("Updated: " + primaryKeyValue);
                        }
                    } else {
                        System.out.println("No match found: primaryKeyValue: " + primaryKeyValue);
                        Object[] values = row.entrySet().stream()
                                .filter(entry -> !entry.getKey().equalsIgnoreCase(autoIncrementColumn) && !entry.getKey().equalsIgnoreCase("RNUM"))
                                .map(Map.Entry::getValue)
                                .toArray();
                        mysqlJdbcTemplate.update(insertQuery.toString(), values);
                        System.out.println("Inserted: " + primaryKeyValue);
                    }
                }
                endTime = System.currentTimeMillis();
	            queryTime = endTime - startTime;
	            System.out.println("Insert/Update Time Taken: " + queryTime + " ms");
	
	            System.out.println("Batch " + (batch+1) + " Completed\n");
	        }
	    }
	}
}
