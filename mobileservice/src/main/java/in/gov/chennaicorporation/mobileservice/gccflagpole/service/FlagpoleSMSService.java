package in.gov.chennaicorporation.mobileservice.gccflagpole.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.gov.chennaicorporation.mobileservice.configuration.AppConfig;
import in.gov.chennaicorporation.mobileservice.gccflagpole.util.MessageUtility;

@Service
public class FlagpoleSMSService {

        @Autowired
        private AppConfig appConfig;

        @Autowired
        private MessageUtility messageUtility;

        private final String key = "pfTEYN6H";

        public String restorationflagpoleSms(String mobileNo,
                        String streetName,
                        String amount,
                        String requestId) {

                try {

                        String message = "Your flagpole installation at " + streetName
                                        + " caused damage to the public property. "
                                        + "Restoration charges of RS." + amount
                                        + " to be paid at https://gccservices.in/flagpolesregistration/paymentform?requestId="
                                        + requestId
                                        + " to avoid further action.\r\n"
                                        + "By GCC";

                        String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);

                        String urlString = appConfig.otpurl
                                        + "from=GCCCRP"
                                        + "&key=" + key
                                        + "&sender=GCCCRP"
                                        + "&to=" + mobileNo
                                        + "&body=" + encodedMsg
                                        + "&entityid=1401572690000011081"
                                        + "&templateid=1407176984612580095";

                        System.out.println("FINAL SMS URL:- " + urlString);

                        URL apiUrl = new URL(urlString);
                        HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(15000);

                        BufferedReader in = new BufferedReader(
                                        new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder content = new StringBuilder();

                        while ((inputLine = in.readLine()) != null) {
                                content.append(inputLine);
                        }

                        in.close();
                        conn.disconnect();

                        System.out.println("DIRECT SMS RESPONSE:- " + content.toString());

                        return content.toString();

                } catch (Exception e) {
                        e.printStackTrace();
                        return "FAILED";
                }
        }

        public String unauthorizedFlagPoleSms(String mobileNo,
                        String streetName,
                        String fineAmount,
                        String requestId) {

                try {

                        String message = "An unauthorized flag pole installation was found at " + streetName + ". "
                                        + "Fine amount: Rs." + fineAmount
                                        + " to be paid at https://gccservices.in/flagpolesregistration/paymentform?requestId="
                                        + requestId
                                        + " to avoid further action.\r\n"
                                        + "By GCC";

                        String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);

                        String urlString = appConfig.otpurl
                                        + "from=GCCCRP"
                                        + "&key=" + key
                                        + "&sender=GCCCRP"
                                        + "&to=" + mobileNo
                                        + "&body=" + encodedMsg
                                        + "&entityid=1401572690000011081"
                                        + "&templateid=1407176984622316740";

                        System.out.println("FINAL SMS URL:- " + urlString);

                        URL apiUrl = new URL(urlString);
                        HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(15000);

                        BufferedReader in = new BufferedReader(
                                        new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder content = new StringBuilder();

                        while ((inputLine = in.readLine()) != null) {
                                content.append(inputLine);
                        }

                        in.close();
                        conn.disconnect();

                        System.out.println("DIRECT SMS RESPONSE:- " + content.toString());

                        return content.toString();

                } catch (Exception e) {
                        e.printStackTrace();
                        return "FAILED";
                }
        }

}
