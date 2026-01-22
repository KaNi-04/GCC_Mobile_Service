package in.gov.chennaicorporation.mobileservice.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class OTPGenerator {

    private static final String NUMERIC_CHARACTERS = "0123456789";
    private static final int OTP_LENGTH = 6;

    public static String generateOTP(int OTP_LENGTH) {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder(OTP_LENGTH);

        for (int i = 0; i < OTP_LENGTH; i++) {
            int index = random.nextInt(NUMERIC_CHARACTERS.length());
            otp.append(NUMERIC_CHARACTERS.charAt(index));
        }

        return otp.toString();
    }
}
