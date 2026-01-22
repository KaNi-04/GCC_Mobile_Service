package in.gov.chennaicorporation.mobileservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.file.Path;
import java.nio.file.Paths;

@RequestMapping("/gccofficialapp")
@Controller("FileController")
public class FileController {

    @Value("${file.upload.directory}")
    private String uploadDirectory;
    
    @GetMapping("/files/{directoryPath}/**")
    public ResponseEntity<Resource> serveFile(@PathVariable String directoryPath, HttpServletRequest request) {
    	String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    	//System.out.println(fullPath.toString()+"\n"+directoryPath.length()+"\n directoryPath:"+directoryPath);
        //String relativePath = fullPath.substring(directoryPath.length() + 1); // Adding 1 for the trailing '/'
    	String relativePath = fullPath.replace("/gccofficialapp/files", "");
        String filePath = uploadDirectory + "/" + relativePath;
        Path path = Paths.get(filePath).normalize();
        
        //System.out.println(path.toString());
        
        Resource resource;
        try { 
            resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
            	MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
                // Determine content type based on file extension
                String fileName = resource.getFilename();
                if (fileName != null) {
                    String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
                    switch (fileExtension.toLowerCase()) {
                        case "jpg":
                        case "jpeg":
                            mediaType = MediaType.IMAGE_JPEG;
                            break;
                        case "png":
                            mediaType = MediaType.IMAGE_PNG;
                            break;
                        case "gif":
                            mediaType = MediaType.IMAGE_GIF;
                            break;
                        case "pdf":
                            mediaType = MediaType.APPLICATION_PDF;
                            break;
                        // Add more cases for other file types if needed
                    }
                }
                return ResponseEntity.ok()
                		.contentType(mediaType)
                		.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        //.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}

