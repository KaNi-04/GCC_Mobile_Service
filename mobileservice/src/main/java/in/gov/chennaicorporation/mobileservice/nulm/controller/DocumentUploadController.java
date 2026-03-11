package in.gov.chennaicorporation.mobileservice.nulm.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.nulm.service.DocumentUploadService;

@RestController
@RequestMapping("/gccofficialapp/api/park/document/")
public class DocumentUploadController {

    @Autowired
    DocumentUploadService documentUploadService;

    @PostMapping("/uploadDocuments")
    public ResponseEntity<Map<String, Object>> uploadDocumentDetails(@RequestParam String empId,
            @RequestParam MultipartFile aadharPhoto,
            @RequestParam MultipartFile bankPhoto,
            @RequestParam MultipartFile formPhoto) {

        Map<String, Object> response = new HashMap<>();

        String docStatus = documentUploadService.uploadDocumentDetails(empId, aadharPhoto, bankPhoto, formPhoto);

        if (docStatus.contains("success")) {
            response.put("status", "success");
            response.put("message", empId + " : " + "Document Uploaded successfully");
        } else if (docStatus.contains("not found")) {
            response.put("status", "Failed");
            response.put("message", "Please Enter Correct Employee Id");
        } else {
            response.put("status", "Failed");
            response.put("message", empId + " : " + "Document Upload failed");
        }

        return ResponseEntity.ok(response);
    }

}
