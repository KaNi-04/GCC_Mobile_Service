package in.gov.chennaicorporation.mobileservice.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.gov.chennaicorporation.mobileservice.constructionGuidelines.service.GuidelinesService;
import in.gov.chennaicorporation.mobileservice.service.PdfGeneratorService;

@RestController
@RequestMapping("/gccofficialapp/api/pdf")
public class PdfTemplateController {

    @Autowired
    private PdfGeneratorService pdfGeneratorService;
    
    @Autowired
    private GuidelinesService guidelinesService;
/*
    @GetMapping("/template")
    public ResponseEntity<byte[]> generateFromTemplate(
    		@RequestParam(value = "cdid", required = false) String cdid,
    		@RequestParam(value = "ciid", required = false) String ciid
    ) {
    	//Map<String, Object> data = new HashMap<>();
    	
        //data.put("siteLocation", siteLocation);
        //data.put("violations", violations);
        //data.put("violationLevel", violationLevel);
        //data.put("clause", clause);
        //data.put("fineAmount", fineAmount);
        //data.put("date", LocalDate.now().toString());
        
        
        String attachmentFileName="UNDER_CLEAN_AND_SAFE_CONSTRUCTION_GUIDELINES_NOTICE_"+LocalDate.now().toString()+"pdf";

        byte[] pdfBytes = pdfGeneratorService.generatePdf(guidelinesService.getNoticeData(cdid, ciid));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", attachmentFileName);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
    */
    @GetMapping("/template")
    public ResponseEntity<byte[]> generateFromTemplate(
            @RequestParam(value = "cdid", required = false) String cdid,
            @RequestParam(value = "ciid", required = false) String ciid
    ) {
        String attachmentFileName = "UNDER_CLEAN_AND_SAFE_CONSTRUCTION_GUIDELINES_NOTICE_" + LocalDate.now() + ".pdf";

        // Generate PDF content
        byte[] pdfBytes = pdfGeneratorService.generatePdf(
                guidelinesService.getNoticeData(cdid, ciid)
        );

        // Prepare response headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", attachmentFileName);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
