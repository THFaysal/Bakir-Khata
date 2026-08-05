package com.example.bakir_khata.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Phone must be numeric")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    /** Optional: lets the user also log in later with phone + PIN instead of email + password. */
    @Pattern(regexp = "^$|^[0-9]{4,6}$", message = "PIN must be 4 to 6 digits")
    private String pin;

    private String confirmPin;

    public boolean isPinProvided() {
        return pin != null && !pin.isBlank();
    }

    public boolean isPinConfirmed() {
        return !isPinProvided() || pin.equals(confirmPin);
    }
}
