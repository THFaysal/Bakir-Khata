package com.example.bakir_khata.dto;


import com.example.bakir_khata.model.enums.LoanTag;
import com.example.bakir_khata.model.enums.PaymentType;
import com.example.bakir_khata.model.enums.Priority;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LoanDTO {

    private Long id;

    @NotNull(message = "Lender is required")
    private Long lenderId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Enter a valid amount")
    private BigDecimal amount;

    @NotNull(message = "Borrow date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate borrowDate;

    @NotNull(message = "Due date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    @Size(max = 150, message = "Purpose cannot exceed 150 characters")
    private String purpose;

    @NotNull(message = "Priority is required")
    private Priority priority;

    private LoanTag tag;

    @NotNull(message = "Payment type is required")
    private PaymentType paymentType = PaymentType.ONE_TIME;

    @Min(value = 2, message = "Installment count must be at least 2")
    private Integer installmentCount;

    @DecimalMin(value = "0.01", message = "Installment amount must be greater than zero")
    private BigDecimal expectedInstallmentAmount;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    /** Cross-field check used by the controller: due date cannot precede borrow date. */
    public boolean isDueDateValid() {
        return borrowDate == null || dueDate == null || !dueDate.isBefore(borrowDate);
    }

    /** When paymentType is INSTALLMENT, count and amount are required. */
    public boolean isInstallmentDataValid() {
        if (paymentType != PaymentType.INSTALLMENT) {
            return true;
        }
        return installmentCount != null && expectedInstallmentAmount != null;
    }
}
