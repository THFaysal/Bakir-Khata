package com.example.bakir_khata.service;

import com.example.bakir_khata.model.User;

public interface OtpService {
    OtpDelivery send(User user, String purpose);
    OtpDelivery sendToPhone(User user, String phone, String purpose);
    boolean verify(User user, String purpose, String code);
    String normalizePhone(String phone);

    record OtpDelivery(String maskedPhone, String mode, String demoCode) {}
}
