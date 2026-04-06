package com.example.user;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // In-memory store: email -> OtpRecord
    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    private static final int OTP_EXPIRY_MINUTES = 10;

    // ─── Inner record ─────────────────────────────────────────────────────────
    private static class OtpRecord {
        final String otp;
        final LocalDateTime createdAt;

        OtpRecord(String otp) {
            this.otp = otp;
            this.createdAt = LocalDateTime.now();
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(createdAt.plusMinutes(OTP_EXPIRY_MINUTES));
        }
    }

    // ─── Generate & Send OTP via Email ────────────────────────────────────────
    public void sendOtp(String toEmail) throws MessagingException {
        String otp = generateOtp();
        otpStore.put(toEmail.toLowerCase(), new OtpRecord(otp));

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("Your Kisan Seva Verification Code");
        helper.setText(buildEmailHtml(otp), true); // true = HTML

        mailSender.send(message);
        System.out.println("[OtpService] OTP sent to " + toEmail); // remove in production
    }

    // ─── Verify OTP ──────────────────────────────────────────────────────────
    public OtpVerifyResult verifyOtp(String email, String otp) {
        String key = email.toLowerCase();
        OtpRecord record = otpStore.get(key);

        if (record == null)        return OtpVerifyResult.NOT_FOUND;
        if (record.isExpired()) {
            otpStore.remove(key);  return OtpVerifyResult.EXPIRED;
        }
        if (!record.otp.equals(otp)) return OtpVerifyResult.INVALID;

        otpStore.remove(key);      // used — remove so it can't be reused
        return OtpVerifyResult.SUCCESS;
    }

    // ─── HTML Email Template ─────────────────────────────────────────────────
    private String buildEmailHtml(String otp) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto;
                        border: 1px solid #e0e0e0; border-radius: 12px; overflow: hidden;">

              <!-- Header -->
              <div style="background: #2f6df6; padding: 28px; text-align: center;">
                <h1 style="color: #ffffff; margin: 0; font-size: 26px; letter-spacing: 1px;">
                  🌾 Kisan Seva
                </h1>
                <p style="color: rgba(255,255,255,0.85); margin: 6px 0 0; font-size: 13px;">
                  Farm to Doorstep
                </p>
              </div>

              <!-- Body -->
              <div style="padding: 32px; background: #ffffff;">
                <p style="color: #333; font-size: 15px; margin: 0 0 20px;">
                  Hello! Use the verification code below to complete your registration.
                </p>

                <!-- OTP Box -->
                <div style="text-align: center; margin: 24px 0;">
                  <span style="display: inline-block; background: #f0f4ff;
                               border: 2px dashed #2f6df6; border-radius: 12px;
                               padding: 18px 40px; font-size: 38px; font-weight: 800;
                               color: #2f6df6; letter-spacing: 10px;">
                    %s
                  </span>
                </div>

                <p style="color: #666; font-size: 13px; text-align: center; margin: 0 0 24px;">
                  ⏰ This code expires in <strong>10 minutes</strong>.
                </p>

                <hr style="border: none; border-top: 1px solid #eee; margin: 0 0 20px;" />

                <p style="color: #999; font-size: 12px; margin: 0;">
                  If you did not request this code, you can safely ignore this email.
                  Do <strong>not</strong> share this code with anyone.
                </p>
              </div>

              <!-- Footer -->
              <div style="background: #f7f7f7; padding: 16px; text-align: center;">
                <p style="color: #aaa; font-size: 11px; margin: 0;">
                  © 2025 Kisan Seva · All rights reserved
                </p>
              </div>
            </div>
        """.formatted(otp);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────
    private String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    public enum OtpVerifyResult {
        SUCCESS, INVALID, EXPIRED, NOT_FOUND
    }
}
