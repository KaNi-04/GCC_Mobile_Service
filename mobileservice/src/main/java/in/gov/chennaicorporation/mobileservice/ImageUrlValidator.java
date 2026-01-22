package in.gov.chennaicorporation.mobileservice;
import java.sql.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.HttpURLConnection;
import java.net.URL;

@RequestMapping("/gccofficialapp/api/imgval/")
@RestController("imgval")
public class ImageUrlValidator {

    // JDBC connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/cmdashboard";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    
    @GetMapping("/imagecheck")
    public String imagecheck(){
        String query = "SELECT data_id, abs_est_number, regionname, typeofwork, imageurl, zonename, wardname FROM tbl_cmdashboard"
        		+ " WHERE abs_est_number LIKE '%2024-25%'";
        String updateQuery = "UPDATE tbl_cmdashboard SET image_status = ? WHERE data_id = ?";
        
        try (
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        		PreparedStatement stmt = conn.prepareStatement(query);
        		PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
            ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                int dataId = rs.getInt("data_id");
                String estNumber = rs.getString("abs_est_number");
                String region = rs.getString("regionname");
                String typeOfWork = rs.getString("typeofwork");
                String imageUrl = rs.getString("imageurl");
                String zone = rs.getString("zonename");
                String ward = rs.getString("wardname");

                boolean isValid = isImageUrlValid(imageUrl);
                updateStmt.setBoolean(1, isValid ? true : false);
                updateStmt.setInt(2, dataId);
                updateStmt.executeUpdate();
                
                System.out.printf("ID: %d | Estimate: %s | URL: %s | Valid: %s%n",
                        dataId, estNumber, imageUrl, isValid ? "✅" : "❌");
            }
            System.out.println("All image statuses updated.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return "";
    }

    // Checks if the image URL is valid and accessible
    private static boolean isImageUrlValid(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return false;

        try {
            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();

            int statusCode = conn.getResponseCode();
            String contentType = conn.getContentType();

            return statusCode == 200 && contentType != null && contentType.startsWith("image/");
        } catch (Exception e) {
            return false;
        }
    }
}