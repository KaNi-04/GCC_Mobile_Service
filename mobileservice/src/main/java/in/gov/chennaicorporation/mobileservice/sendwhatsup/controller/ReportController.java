package in.gov.chennaicorporation.mobileservice.sendwhatsup.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.chennaicorporation.mobileservice.sendwhatsup.service.HtmlToImageService;

@RestController("gccofficialappshtmltopng")
@RequestMapping("/gccofficialapp/api/htmltopng/")
public class ReportController {

    @Autowired
    private HtmlToImageService service;
    
   
    @GetMapping("/html-to-png")
    public Map<String, Object> convertHtmlToImage(@RequestParam("url") String url, @RequestParam("templateid") String tempId) {

        String htmlPath = url;//"https://gccservices.in/commissionerreview/sofacollectiondetails";
        String pngPath  = "gcc_mobile_service_uploads/whatsappreports/report_" +tempId+"_"+ System.currentTimeMillis() + ".png";

        Map<String, Object> result = service.generatePng(htmlPath, pngPath);
        /*
        String todayDate = LocalDate.now().toString(); // yyyy-MM-dd
        
        // Save only if PNG generation is successful
        if ((int) result.get("status") == 200) {
            //String imageUrl = (String) result.get("imageUrl");
            String imagePath = "/" + pngPath; // same path you saved
            
            int msgId=service.saveMessageDetails(todayDate, tempId, imagePath);
            
            if(msgId>0) {
            	String status = service.sendMessage(String.valueOf(msgId),"","");
            	service.disablemessage(String.valueOf(msgId));
            }
        }
        */
        return result;
    }
    
}
