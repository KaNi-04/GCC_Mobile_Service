package in.gov.chennaicorporation.mobileservice.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@Service
public class PdfGeneratorService {
	@Autowired
    private TemplateEngine templateEngine;

    public byte[] generatePdf(Map<String, Object> data) {
        Context context = new Context();
        context.setVariables(data);

        String htmlContent = templateEngine.process("pdf-template", context);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(htmlContent, null);
        builder.toStream(os);
        try {
            builder.run();
        } catch (IOException e) {
            e.printStackTrace(); // or log it properly
            throw new RuntimeException("PDF generation failed", e);
        }

        return os.toByteArray();
    }
}
