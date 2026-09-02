package com.example.bakir_khata.service.impl;

import com.example.bakir_khata.exception.BusinessRuleException;
import com.example.bakir_khata.model.OtpVerification;
import com.example.bakir_khata.model.User;
import com.example.bakir_khata.repository.OtpVerificationRepository;
import com.example.bakir_khata.repository.UserRepository;
import com.example.bakir_khata.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    private final OtpVerificationRepository repository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.otp.mode:demo}")
    private String otpMode;
    @Value("${app.otp.fallback-to-demo:true}")
    private boolean fallbackToDemo;
    @Value("${app.otp.expiry-minutes:2}")
    private int expiryMinutes;
    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;
    @Value("${app.otp.resend-cooldown-seconds:30}")
    private int resendCooldownSeconds;
    @Value("${TWILIO_ACCOUNT_SID:}")
    private String accountSid;
    @Value("${TWILIO_AUTH_TOKEN:}")
    private String authToken;
    @Value("${TWILIO_VERIFY_SERVICE_SID:}")
    private String verifyServiceSid;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    @Override
    @Transactional
    public OtpDelivery send(User user, String purpose) {
        return sendToPhone(user, user.getPhone(), purpose);
    }

    @Override
    @Transactional
    public OtpDelivery sendToPhone(User user, String rawPhone, String purpose) {
        String phone = normalizePhone(rawPhone);
        repository.findFirstByUserIdAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(user.getId(), purpose)
                .filter(previous -> previous.getCreatedAt() != null
                        && previous.getCreatedAt().plusSeconds(resendCooldownSeconds).isAfter(LocalDateTime.now()))
                .ifPresent(previous -> {
                    long seconds = Math.max(1, java.time.Duration.between(LocalDateTime.now(), previous.getCreatedAt().plusSeconds(resendCooldownSeconds)).toSeconds());
                    throw new BusinessRuleException("Please wait " + seconds + " seconds before requesting another OTP.");
                });
        if ("twilio".equalsIgnoreCase(otpMode)) {
            try {
                requireTwilioConfig();
                twilioSend(phone);
                repository.save(OtpVerification.builder()
                        .user(user).purpose(purpose).phone(phone).mode("TWILIO")
                        .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes)).build());
                return new OtpDelivery(mask(phone), "TWILIO", null);
            } catch (Exception ex) {
                if (!fallbackToDemo) throw new BusinessRuleException("OTP provider is unavailable. Please try again.");
                log.warn("Twilio OTP unavailable; falling back to demo OTP: {}", ex.getMessage());
            }
        }
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        repository.save(OtpVerification.builder()
                .user(user).purpose(purpose).phone(phone).mode("DEMO")
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes)).build());
        return new OtpDelivery(mask(phone), "DEMO", code);
    }

    @Override
    @Transactional
    public boolean verify(User user, String purpose, String code) {
        OtpVerification verification = repository
                .findFirstByUserIdAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseThrow(() -> new BusinessRuleException("No active OTP verification was found."));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("OTP expired. Request a new code.");
        }
        if (verification.getAttempts() >= maxAttempts) {
            throw new BusinessRuleException("Maximum OTP attempts reached. Request a new code.");
        }
        verification.setAttempts(verification.getAttempts() + 1);

        boolean ok;
        if ("TWILIO".equalsIgnoreCase(verification.getMode())) {
            try {
                ok = twilioVerify(verification.getPhone(), code);
            } catch (Exception ex) {
                throw new BusinessRuleException("Could not verify OTP with the SMS provider.");
            }
        } else {
            ok = code != null && verification.getCodeHash() != null && passwordEncoder.matches(code, verification.getCodeHash());
        }

        if (ok) {
            verification.setVerified(true);
            verification.setVerifiedAt(LocalDateTime.now());
            if (user.getPhone() != null && normalizePhone(user.getPhone()).equals(verification.getPhone())) {
                user.setPhoneVerified(true);
                userRepository.save(user);
            }
        }
        repository.save(verification);
        return ok;
    }

    private void twilioSend(String phone) throws Exception {
        String body = form("To", phone) + "&" + form("Channel", "sms");
        HttpRequest request = twilioRequest("https://verify.twilio.com/v2/Services/" + verifyServiceSid + "/Verifications", body);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) throw new IllegalStateException("Twilio send failed: " + response.statusCode());
    }

    private boolean twilioVerify(String phone, String code) throws Exception {
        String body = form("To", phone) + "&" + form("Code", code == null ? "" : code.trim());
        HttpRequest request = twilioRequest("https://verify.twilio.com/v2/Services/" + verifyServiceSid + "/VerificationCheck", body);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() / 100 == 2 && response.body().contains("\"approved\"");
    }

    private HttpRequest twilioRequest(String url, String body) {
        String basic = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private String form(String key, String value) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void requireTwilioConfig() {
        if (accountSid.isBlank() || authToken.isBlank() || verifyServiceSid.isBlank()) {
            throw new IllegalStateException("Twilio Verify environment variables are not configured");
        }
    }

    @Override
    public String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) throw new BusinessRuleException("Your profile needs a phone number before OTP verification.");
        String value = phone.replaceAll("[^0-9+]", "");
        if (value.startsWith("01") && value.length() == 11) value = "+880" + value.substring(1);
        else if (value.startsWith("880")) value = "+" + value;
        if (!value.startsWith("+")) throw new BusinessRuleException("Phone number must include a valid country code.");
        return value;
    }

    private String mask(String phone) {
        if (phone.length() <= 4) return "****";
        return "*".repeat(Math.max(4, phone.length() - 4)) + phone.substring(phone.length() - 4);
    }
}
