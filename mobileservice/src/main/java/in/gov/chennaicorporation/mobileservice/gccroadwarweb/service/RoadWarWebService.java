package in.gov.chennaicorporation.mobileservice.gccroadwarweb.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class RoadWarWebService {

    @Autowired
    private JdbcTemplate jdbcRoadWarWebTemplate;

    private final Environment environment;

    private String fileBaseUrl;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final int STRING_LENGTH = 15;

    private static final Random RANDOM = new SecureRandom();

    @Autowired
    public void setDataSource(@Qualifier("mysqlroadwarwebGccDataSource") DataSource roadwarwebDataSource) {
        this.jdbcRoadWarWebTemplate = new JdbcTemplate(roadwarwebDataSource);
    }

    public RoadWarWebService(Environment environment) {
        this.environment = environment;
        this.fileBaseUrl = environment.getProperty("fileBaseUrl");
    }

    public static String generateRandomString() {
        StringBuilder result = new StringBuilder(STRING_LENGTH);
        for (int i = 0; i < STRING_LENGTH; i++) {
            result.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return result.toString();
    }

    private byte[] compressImage(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(byteArrayOutputStream);

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        writer.setOutput(imageOutputStream);

        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }

        writer.write(null, new javax.imageio.IIOImage(image, null, null), param);

        writer.dispose();
        imageOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    public String fileUpload(MultipartFile file, String name) {

        // Set the file path where you want to save it
        String uploadDirectory = environment.getProperty("file.upload.directory");
        String serviceFolderName = environment.getProperty("roadwarweb_foldername");
        var year = DateTimeUtil.getCurrentYear();
        var month = DateTimeUtil.getCurrentMonth();

        uploadDirectory = uploadDirectory + serviceFolderName + year + "/" + month;

        try {
            // Create directory if it doesn't exist
            Path directoryPath = Paths.get(uploadDirectory);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Datetime string
            String datetimetxt = DateTimeUtil.getCurrentDateTime();
            // File name
            String fileName = name + "_" + datetimetxt + "_" + file.getOriginalFilename();
            fileName = fileName.replaceAll("\\s+", ""); // Remove space on filename

            String filePath = uploadDirectory + "/" + fileName;

            String filepath_txt = "/" + serviceFolderName + year + "/" + month + "/" + fileName;

            // Create a new Path object
            Path path = Paths.get(filePath);

            // Detect file type using extension and MIME content type
            String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase()
                    : "";
            String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";

            boolean isImage = originalFilename.endsWith(".jpg") || originalFilename.endsWith(".jpeg")
                    || contentType.equals("image/jpeg");
            boolean isPdf = originalFilename.endsWith(".pdf") || contentType.equals("application/pdf");

            // Get the bytes of the file
            // byte[] bytes = file.getBytes();

            // // Compress the image
            // BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            // byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50%
            // quality

            // // Write the bytes to the file
            // Files.write(path, compressedBytes);

            if (isImage) {
                // Get the bytes of the file
                byte[] bytes = file.getBytes();

                // Compress the image with 50% quality and write to file
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                byte[] compressedBytes = compressImage(image, 0.5f);
                Files.write(path, compressedBytes);

            } else if (isPdf) {
                // Directly save the PDF without any compression
                Files.write(path, file.getBytes());

            } else {
                // Unsupported file type — reject the upload
                return "Unsupported file type: " + file.getOriginalFilename();
            }

            System.out.println(filePath);
            return filepath_txt;

        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to save file " + file.getOriginalFilename();
        }
    }

    // public List<Map<String, Object>> getRoadMasterDetails(String loginId) {

    // String zone = getZoneFromLoginId(loginId);

    // String sqlQuery = "SELECT " +
    // " rm.ref_id, " +
    // " rm.zone, " +
    // " rm.ward, " +
    // " rm.road_name, " +
    // " rt.road_type, " +
    // " rm.road_length, " +
    // " rm.road_avg_width, " +
    // " rm.road_area, " +
    // " rm.footpath_material, " +
    // " rm.footpath_length, " +
    // " rm.footpath_width, " +
    // " rm.footpath_from_location, " +
    // " rm.footpath_to_location, " +
    // " rm.is_swd, " +
    // " ifnull(rm.swd_from,'') as swd_from_location, " +
    // " ifnull(rm.swd_to,'') as swd_to_location, " +
    // " ifnull(rm.swd_length,'') as swd_length, " +
    // " ifnull(rm.is_scp,'') as scp_availability, " +
    // " ifnull(rm.scp_count,'') as scp_count, " +
    // " ifnull(rm.is_manhole,'') as manhole_availability, " +
    // " ifnull(rm.manhole_count,'') as manhole_count, " +
    // " ifnull(rm.manhole_location,'') as manhole_location, " +
    // " ifnull(rm.is_cpipe,'') as chutpipe_availability, " +
    // " ifnull(rm.cpipe_count,'') as chutpipe_count, " +
    // " ifnull(rm.is_rwh,'') as rain_water_harvesting_availability, " +
    // " ifnull(rm.rwh_count,'') as rain_water_harvesting_count, " +
    // " ifnull(rm.sewer_length,'') as sewer_length, " +
    // " ifnull(rm.sewer_size,'') as sewer_size, " +
    // " ifnull(rm.is_electric_poles,'') as electric_poles_availability, " +
    // " ifnull(rm.ep_count,'') as electric_pole_count, " +
    // " ifnull(rm.is_hml,'') as high_mass_light_availability, " +
    // " ifnull(rm.culvert_bridge_count,'') as culvert_bridge_count, " +
    // " ifnull(rm.details,'') as culvert_bridge_details, " +
    // " ifnull(rm.is_busshelter,'') as bus_shelter_availability , " +
    // " ifnull(rm.busshelter_count,'') as bus_shelter_count, " +
    // " ifnull(rm.is_centermedian,'') as center_median_availability, " +
    // " ifnull(rm.erp_asset_code,'') as erp_asset_code, " +
    // " DATE_FORMAT(rm.cdate,'%d-%m-%Y %r') AS cdate, " +
    // " rm.cby, " +
    // " rd.relaying_history " +
    // "FROM road_details_master rm " +
    // "LEFT JOIN road_type_master rt " +
    // " ON rm.road_type = rt.id " +
    // " AND rt.is_active = 1 " +
    // "LEFT JOIN ( " +
    // " SELECT " +
    // " road_ref_id, " +
    // " JSON_ARRAYAGG( " +
    // " JSON_OBJECT( " +
    // " 'relayMonth', relay_month, " +
    // " 'relayYear', relay_year, " +
    // " 'crValue', cr_value, " +
    // " 'roadType', road_type, " +
    // " 'remarks', remarks, " +
    // " 'cdate', DATE_FORMAT(cdate, '%d-%m-%Y %r') " +
    // " ) " +
    // " ) AS relaying_history " +
    // " FROM relaying_details " +
    // " WHERE is_active = 1 " +
    // " GROUP BY road_ref_id " +
    // ") rd " +
    // "ON rm.ref_id = rd.road_ref_id " +
    // "WHERE NOT EXISTS ( SELECT ref_id " +
    // "FROM road_master_files rmf " +
    // "WHERE rmf.road_ref_id = rm.ref_id " +
    // "AND rmf.is_active = 1 " +
    // "AND rmf.is_delete = 0 ) and rm.zone = ? ";

    // List<Map<String, Object>> results =
    // jdbcRoadWarWebTemplate.queryForList(sqlQuery, zone);
    // return results;

    // }

    public Map<String, Object> getRoadMasterDetails(String road_id) {

        Map<String, Object> result = new HashMap<>();

        // String checkSql = "SELECT COUNT(*)as count, ifnull(group_concat( ' " +
        // fileBaseUrl
        // + "gccofficialapp/files',rmf.file_path),'File not uploaded') as filePaths " +
        // "FROM road_master_files rmf " +
        // "left join road_details_master rdm on rmf.road_ref_id = rdm.ref_id and
        // rdm.is_active = 1 and rdm.is_delete = 0 "
        // +
        // "WHERE rdm.road_id = ? AND rmf.is_active = 1 AND rmf.is_delete = 0";

        // result = jdbcRoadWarWebTemplate.queryForMap(checkSql, road_id);

        // long existingCount = result.get("count") != null ? ((Number)
        // result.get("count")).longValue() : 0L;

        // if (existingCount > 0) {
        // return result;
        // }
        // String zone = getZoneFromLoginId(loginId);

        try {
            String sqlQuery = "SELECT  " +
                    "    rm.ref_id,  " +
                    "    rm.zone,  " +
                    "    rm.ward,  " +
                    "    ifnull(rm.road_name,'') as road_name,  " +
                    "    ifnull(rt.road_type,'') as road_type,  " +
                    "    ifnull(rm.road_length,'') as road_length,  " +
                    "    ifnull(rm.road_avg_width,'') as road_avg_width,  " +
                    "    ifnull(rm.road_area,'') as road_area,  " +
                    "    ifnull(rm.footpath_material,'') as footpath_material,  " +
                    "    ifnull(rm.footpath_length,'') as footpath_length,  " +
                    "    ifnull(rm.footpath_width,'') as footpath_width,  " +
                    "    ifnull(rm.footpath_from_location,'') as footpath_from_location,  " +
                    "    ifnull(rm.footpath_to_location,'') as footpath_to_location,  " +
                    "    ifnull(rm.is_swd,'') as is_swd,  " +
                    "    ifnull(rm.swd_from,'') as swd_from_location,  " +
                    "    ifnull(rm.swd_to,'') as swd_to_location,  " +
                    "    ifnull(rm.swd_length,'') as swd_length,  " +
                    "    ifnull(rm.is_scp,'') as scp_availability,  " +
                    "    ifnull(rm.scp_count,'') as scp_count,  " +
                    "    ifnull(rm.is_manhole,'') as manhole_availability,  " +
                    "    ifnull(rm.manhole_count,'') as manhole_count,  " +
                    "    ifnull(rm.manhole_location,'') as manhole_location,  " +
                    "    ifnull(rm.is_cpipe,'') as chutpipe_availability,  " +
                    "    ifnull(rm.cpipe_count,'') as chutpipe_count,  " +
                    "    ifnull(rm.is_rwh,'') as rain_water_harvesting_availability,  " +
                    "    ifnull(rm.rwh_count,'') as rain_water_harvesting_count,  " +
                    "    ifnull(rm.sewer_length,'') as sewer_length,  " +
                    "    ifnull(rm.sewer_size,'') as sewer_size,  " +
                    "    ifnull(rm.is_electric_poles,'') as electric_poles_availability,  " +
                    "    ifnull(rm.ep_count,'') as electric_pole_count,  " +
                    "    ifnull(rm.is_hml,'') as high_mass_light_availability,  " +
                    "    ifnull(rm.culvert_bridge_count,'') as culvert_bridge_count,  " +
                    "    ifnull(rm.details,'') as culvert_bridge_details,  " +
                    "    ifnull(rm.is_busshelter,'') as bus_shelter_availability ,  " +
                    "    ifnull(rm.busshelter_count,'') as bus_shelter_count,  " +
                    "    ifnull(rm.is_centermedian,'') as center_median_availability,  " +
                    "    ifnull(rm.erp_asset_code,'') as erp_asset_code,  " +
                    "    DATE_FORMAT(rm.cdate,'%d-%m-%Y %r') AS cdate,  " +
                    "    rm.cby,  " +
                    "    rd.relaying_history,  " +
                    "CASE WHEN EXISTS ( " +
                    "            SELECT 1 " +
                    "            FROM road_master_files rmf " +
                    "            WHERE rmf.road_ref_id = rm.ref_id " +
                    "              AND rmf.file_path IS NOT NULL " +
                    "              AND rmf.is_active = 1 " +
                    "              AND rmf.is_delete = 0 ) THEN FALSE ELSE TRUE END AS fileFlag, " +
                    "    (SELECT JSON_ARRAYAGG( CONCAT('https://gccservices.in/gccofficialapp/files', file_path) ) " +
                    "    FROM road_master_files rmf WHERE rmf.road_ref_id = rm.ref_id " +
                    "    AND rmf.is_active = 1 AND rmf.is_delete = 0 ) AS filePaths " +
                    "FROM road_details_master rm  " +
                    "LEFT JOIN road_type_master rt ON rm.road_type = rt.id AND rt.is_active = 1  " +
                    "LEFT JOIN ( SELECT road_ref_id, JSON_ARRAYAGG( JSON_OBJECT(  " +
                    "                'relayMonth', relay_month,  " +
                    "                'relayYear', relay_year,  " +
                    "                'crValue', cr_value,  " +
                    "                'roadType', road_type,  " +
                    "                'remarks', remarks,  " +
                    "                'cdate', DATE_FORMAT(cdate, '%d-%m-%Y %r')  " +
                    "            )  " +
                    "        ) AS relaying_history  " +
                    "    FROM relaying_details  " +
                    "    WHERE is_active = 1  " +
                    "    GROUP BY road_ref_id  " +
                    ") rd  " +
                    "ON rm.ref_id = rd.road_ref_id  " +
                    "WHERE rm.is_active=1 and rm.is_delete=0 and rm.road_id = ? ";

            result = jdbcRoadWarWebTemplate.queryForMap(sqlQuery, road_id);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private String getZoneFromLoginId(String loginId) {
        String sqlQuery = "SELECT `zone` FROM gcc_penalty_hoardings.hoading_user_list WHERE userid = ? LIMIT 1";

        // Query the database using queryForList
        List<Map<String, Object>> results = jdbcRoadWarWebTemplate.queryForList(sqlQuery, loginId);

        // Check if results is not empty and extract the ward value
        if (!results.isEmpty()) {
            System.out.println("zone....." + results);
            // Extract the ward value from the first result
            return (String) results.get(0).get("zone");
        }

        // Handle the case where no result is found
        return "00"; // or return null based on your needs

    }

    // public String saveRoadMasterFiles(String ref_id, List<MultipartFile> files,
    // String loginId) {

    // try {

    // String checkSql = "SELECT COUNT(*) FROM road_master_files WHERE road_ref_id =
    // ? AND is_active = 1 AND is_delete = 0";
    // Integer existingCount = jdbcRoadWarWebTemplate.queryForObject(checkSql,
    // Integer.class, ref_id);

    // if (existingCount != null && existingCount > 0) {
    // return "already_exists";
    // }

    // String insertSql = "INSERT INTO road_master_files (road_ref_id, file_path,
    // file_flag, cby, is_active, is_delete) "
    // + "VALUES (?, ?, ?, ?, 1, 0)";

    // if (files != null) {
    // for (int i = 0; i < files.size(); i++) {
    // MultipartFile file = files.get(i);
    // String fileFlag = "file" + (i + 1);

    // if (file == null || file.isEmpty()) {
    // continue;
    // }

    // String filepath_txt = fileUpload(file, fileFlag);

    // if (filepath_txt == null || filepath_txt.startsWith("Failed")
    // || filepath_txt.startsWith("Unsupported")) {
    // System.err.println("File upload failed for " + fileFlag + ": " +
    // filepath_txt);
    // continue;
    // }

    // jdbcRoadWarWebTemplate.update(insertSql, ref_id, filepath_txt, fileFlag,
    // loginId);
    // }
    // }

    // return "Success";

    // } catch (Exception e) {
    // e.printStackTrace();
    // return "Failed to upload image " + e.getMessage();
    // }
    // }

    public String saveRoadMasterFiles(String ref_id, MultipartFile file1, MultipartFile file2, MultipartFile file3,
            MultipartFile file4,
            String loginId) {

        try {

            String checkSql = "SELECT COUNT(*) FROM road_master_files WHERE road_ref_id = ? AND is_active = 1 AND is_delete = 0";
            Integer existingCount = jdbcRoadWarWebTemplate.queryForObject(checkSql, Integer.class, ref_id);

            if (existingCount != null && existingCount > 0) {
                return "already_exists";
            }

            MultipartFile[] files = { file1, file2, file3, file4 };
            String[] fileFlags = { "file1", "file2", "file3", "file4" };

            String insertSql = "INSERT INTO road_master_files (road_ref_id, file_path, file_flag, cby, is_active, is_delete) "
                    + "VALUES (?, ?, ?, ?, 1, 0)";

            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];

                if (file == null || file.isEmpty()) {
                    break;
                }
                String filepath_txt = fileUpload(file, fileFlags[i]);

                if (filepath_txt == null || filepath_txt.startsWith("Failed")
                        || filepath_txt.startsWith("Unsupported")) {
                    System.err.println("File upload failed for " + fileFlags[i] + ": " + filepath_txt);
                    break;
                }

                jdbcRoadWarWebTemplate.update(insertSql, ref_id, filepath_txt, fileFlags[i], loginId);
            }

            return "Success";

        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to upload image " + e.getMessage();
        }
    }

    public Map<String, Object> getRoadMasterFileDetails(String ref_id) {
        Map<String, Object> result = new HashMap<>();

        String checkSql = "SELECT COUNT(*)as count, ifnull(group_concat( ' " + fileBaseUrl
                + "gccofficialapp/files',rmf.file_path),'File not uploaded') as filePaths " +
                "FROM road_master_files rmf " +
                "left join road_details_master rdm on rmf.road_ref_id = rdm.ref_id and rdm.is_active = 1 and rdm.is_delete = 0 "
                +
                "WHERE rdm.ref_id = ? AND rmf.is_active = 1 AND rmf.is_delete = 0 ";

        result = jdbcRoadWarWebTemplate.queryForMap(checkSql, ref_id);

        long existingCount = result.get("count") != null ? ((Number) result.get("count")).longValue() : 0L;

        if (existingCount > 0) {
            return result;
        } else {
            result.put("status", "200");
            result.put("message", "File not uploaded");
            result.put("data", null);
        }
        return result;
    }

}
