package com.example.bakir_khata.dto;


import com.example.bakir_khata.model.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentDTO {

    private Long id;

    @NotNull(message = "Loan is required")
    private Long loanId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Enter a valid amount")
    private BigDecimal amount;

    @NotNull(message = "Payment date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    private MultipartFile proofFile;
}
