package com.example.bakir_khata.dto;


import com.example.bakir_khata.model.enums.Relationship;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class LenderDTO {

    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Phone must be numeric")
    private String phone;

    @Email(message = "Enter a valid email address")
    private String email;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @NotNull(message = "Relationship is required")
    private Relationship relationship;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    private MultipartFile profileImage;

    private String existingProfileImagePath;
}
