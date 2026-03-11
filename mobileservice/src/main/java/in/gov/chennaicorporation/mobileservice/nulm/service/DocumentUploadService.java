package in.gov.chennaicorporation.mobileservice.nulm.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sql.DataSource;
import java.awt.image.BufferedImage;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.gccactivity.service.DateTimeUtil;

@Service
public class DocumentUploadService {

    private JdbcTemplate jdbcTemplate;
    @Autowired
    private Environment environment;
    private String fileBaseUrl;

    private static final Logger logger = LoggerFactory.getLogger(DocumentUploadService.class);

    @Autowired
    public void setDataSource(@Qualifier("mysqlNulmDataSource") DataSource DataSource) {
        this.jdbcTemplate = new JdbcTemplate(DataSource);
    }

    public String fileUpload(String name, String id, MultipartFile file) {

        int lastInsertId = 0;
        // Set the file path where you want to save it
        String uploadDirectory = environment.getProperty("file.upload.directory.web");
        String serviceFolderName = environment.getProperty("nulm_foldername");
        var year = DateTimeUtil.getCurrentYear();
        var month = DateTimeUtil.getCurrentMonth();
        var date = DateTimeUtil.getCurrentDay();

        uploadDirectory = uploadDirectory + serviceFolderName + "/" + year + "/" + month + "/" + date;

        try {
            // Create directory if it doesn't exist
            Path directoryPath = Paths.get(uploadDirectory);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Datetime string
            String datetimetxt = DateTimeUtil.getCurrentDateTime();
            // File name
            System.out.println(file.getOriginalFilename());
            String fileName = name + "_" + id + "_" + datetimetxt + "_"
                    + file.getOriginalFilename();
            fileName = fileName.replaceAll("\\s+", ""); // Remove space on filename

            String filePath = uploadDirectory + "/" + fileName;

            String filepath_txt = "/" + serviceFolderName + "/" + year + "/" + month + "/" + date + "/" + fileName;

            // Create a new Path object
            Path path = Paths.get(filePath);

            // Get the bytes of the file
            byte[] bytes = file.getBytes();

            // Compress the image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            // byte[] compressedBytes = compressImage(image, 0.5f); // Compress with 50%
            // quality

            // Write the bytes to the file
            Files.write(path, bytes);

            System.out.println(filePath);
            return filepath_txt;

        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to save file " + file.getOriginalFilename();
        }
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

    public String uploadDocumentDetails(String empId, MultipartFile aadharPhoto, MultipartFile bankPhoto,
            MultipartFile formPhoto) {

        String formPath = "";
        String aadharPath = "";
        String bankPath = "";

        try {

            String sql = "SELECT COUNT(*) FROM enrollment_table WHERE emp_id=? and emp_type = 'Park' ";
            int count = jdbcTemplate.queryForObject(sql, new Object[] { empId }, Integer.class);

            if (count == 0) {
                return "Employee ID not found";
            }

            if (formPhoto != null && !formPhoto.isEmpty()) {
                formPath = fileUpload("uploadform", String.valueOf(empId), formPhoto);
                if (formPath.contains("Failed")) {
                    return "Failed to upload form photo";
                }
            }

            if (aadharPhoto != null && !aadharPhoto.isEmpty()) {
                aadharPath = fileUpload("aadharcard", String.valueOf(empId), aadharPhoto);
                if (aadharPath.contains("Failed")) {
                    return "Failed to upload aadhar photo";
                }
            }

            if (bankPhoto != null && !bankPhoto.isEmpty()) {
                bankPath = fileUpload("bankpassbook", String.valueOf(empId), bankPhoto);
                if (bankPath.contains("Failed")) {
                    return "Failed to upload bank passbook photo";
                }
            }

            String sql1 = "UPDATE enrollment_table SET form_url=?, aadhar_card_url=?, bank_passbook_url=? WHERE emp_id=? ";
            jdbcTemplate.update(sql1, formPath, aadharPath, bankPath, empId);

            return "Documents uploaded successfully";

        } catch (Exception e) {
            logger.error(
                    "Method: uploadDocumentDetails | Parameters: empId={}, formPath={}, aadharPath={}, bankPath={} | Date: {} | Exception: {} - {}",
                    empId, formPath, aadharPath, bankPath, LocalDateTime.now(),
                    e.getClass().getName(), e.getMessage(), e);
            return "Failed to upload documents  " + e;

        }
    }

}
