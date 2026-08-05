package com.example.bakir_khata.service.impl;

import com.example.bakir_khata.dto.PaymentDTO;
import com.example.bakir_khata.model.Loan;
import com.example.bakir_khata.model.Payment;
import com.example.bakir_khata.model.User;
import com.example.bakir_khata.repository.PaymentRepository;
import com.example.bakir_khata.service.FileStorageService;
import com.example.bakir_khata.service.LoanService;
import com.example.bakir_khata.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final LoanService loanService;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public Payment recordPayment(PaymentDTO dto, User user) {
        Loan loan = loanService.getLoanById(dto.getLoanId(), user);

        Payment payment = new Payment();
        payment.setLoan(loan);
        payment.setAmount(dto.getAmount());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setNotes(dto.getNotes());

        if (dto.getProofFile() != null && !dto.getProofFile().isEmpty()) {
            String path = fileStorageService.store(dto.getProofFile(), "payments");
            payment.setProofFilePath(path);
        }

        // Deduct from the loan first so an over-payment is rejected before the payment row is persisted.
        loanService.applyPayment(loan, dto.getAmount());

        return paymentRepository.save(payment);
    }

    @Override
    public List<Payment> getPaymentsForLoan(Long loanId, User user) {
        Loan loan = loanService.getLoanById(loanId, user);
        return paymentRepository.findByLoanOrderByPaymentDateDescCreatedAtDesc(loan);
    }
}
