package com.example.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    @Value("${BREVO_API_KEY}")
    private String brevoApiKey  ;

    @Value("${brevo.from.email}")
    private String fromEmail;

    @Value("${brevo.from.name}")
    private String fromName;


    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();
    private static final int OTP_EXPIRY_MINUTES = 10;

    private static class OtpRecord {
        final String otp;
        final LocalDateTime createdAt;
        OtpRecord(String otp) { this.otp = otp; this.createdAt = LocalDateTime.now(); }
        boolean isExpired() {
            return LocalDateTime.now().isAfter(createdAt.plusMinutes(OTP_EXPIRY_MINUTES));
        }
    }

    public void sendOtp(String toEmail) throws Exception {
        String otp = generateOtp();
        otpStore.put(toEmail.toLowerCase(), new OtpRecord(otp));

        String html = buildEmailHtml(otp)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");

        String json = "{"
                + "\"sender\":{"
                + "\"email\":\"" + fromEmail + "\","
                + "\"name\":\"" + fromName + "\""
                + "},"
                + "\"to\":[{\"email\":\"" + toEmail + "\"}],"
                + "\"subject\":\"Your Kisan Seva Verification Code\","
                + "\"htmlContent\":\"" + html + "\""
                + "}";

        URL url = new URL("https://api.brevo.com/v3/smtp/email");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("api-key", brevoApiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();

        if (code != 200 && code != 201 && code != 202) {
            String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("Brevo API error " + code + ": " + err);
        }

        System.out.println("[OtpService] OTP sent via Brevo to " + toEmail);
    }

    public OtpVerifyResult verifyOtp(String email, String otp) {
        String key = email.toLowerCase();
        OtpRecord record = otpStore.get(key);
        if (record == null)           return OtpVerifyResult.NOT_FOUND;
        if (record.isExpired()) { otpStore.remove(key); return OtpVerifyResult.EXPIRED; }
        if (!record.otp.equals(otp))  return OtpVerifyResult.INVALID;
        otpStore.remove(key);
        return OtpVerifyResult.SUCCESS;
    }

    private String buildEmailHtml(String otp) {
        return "<div style='font-family:Arial,sans-serif;max-width:480px;margin:auto;"
             + "border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;'>"
             + "<div style='background:#2f6df6;padding:28px;text-align:center;'>"
             + "<h1 style='color:#fff;margin:0;font-size:26px;'>🌾 Kisan Seva</h1>"
             + "<p style='color:rgba(255,255,255,0.85);margin:6px 0 0;font-size:13px;'>Farm to Doorstep</p>"
             + "</div>"
             + "<div style='padding:32px;background:#fff;'>"
             + "<p style='color:#333;font-size:15px;margin:0 0 20px;'>Use the code below to verify your email:</p>"
             + "<div style='text-align:center;margin:24px 0;'>"
             + "<span style='display:inline-block;background:#f0f4ff;border:2px dashed #2f6df6;"
             + "border-radius:12px;padding:18px 40px;font-size:38px;font-weight:800;"
             + "color:#2f6df6;letter-spacing:10px;'>" + otp + "</span>"
             + "</div>"
             + "<p style='color:#666;font-size:13px;text-align:center;'>Expires in <strong>10 minutes</strong>.</p>"
             + "<hr style='border:none;border-top:1px solid #eee;margin:16px 0;'/>"
             + "<p style='color:#999;font-size:12px;margin:0;'>Do not share this code with anyone.</p>"
             + "</div>"
             + "<div style='background:#f7f7f7;padding:16px;text-align:center;'>"
             + "<p style='color:#aaa;font-size:11px;margin:0;'>© 2025 Kisan Seva</p>"
             + "</div></div>";
    }

    private String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    public enum OtpVerifyResult {
        SUCCESS, INVALID, EXPIRED, NOT_FOUND
    }
}
