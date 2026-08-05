package com.example.bakir_khata.service.impl;


import com.example.bakir_khata.dto.LoanDTO;
import com.example.bakir_khata.exception.BusinessRuleException;
import com.example.bakir_khata.exception.ResourceNotFoundException;
import com.example.bakir_khata.model.Lender;
import com.example.bakir_khata.model.Loan;
import com.example.bakir_khata.model.User;
import com.example.bakir_khata.model.enums.LoanStatus;
import com.example.bakir_khata.model.enums.Priority;
import com.example.bakir_khata.repository.LenderRepository;
import com.example.bakir_khata.repository.LoanRepository;
import com.example.bakir_khata.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LenderRepository lenderRepository;

    @Override
    public Loan saveLoan(LoanDTO dto, User user) {
        if (!dto.isDueDateValid()) {
            throw new BusinessRuleException("Due date cannot be before borrow date.");
        }
        if (!dto.isInstallmentDataValid()) {
            throw new BusinessRuleException("Installment count and expected installment amount are required for installment loans.");
        }

        Lender lender = lenderRepository.findById(dto.getLenderId())
                .orElseThrow(() -> new ResourceNotFoundException("Lender not found"));
        if (!lender.getUser().equals(user)) {
            throw new ResourceNotFoundException("Lender not found");
        }

        boolean isNew = dto.getId() == null;
        Loan loan = isNew ? new Loan() : getLoanById(dto.getId(), user);

        loan.setLender(lender);
        loan.setUser(user);
        loan.setAmount(dto.getAmount());
        loan.setBorrowDate(dto.getBorrowDate());
        loan.setDueDate(dto.getDueDate());
        loan.setPurpose(dto.getPurpose());
        loan.setPriority(dto.getPriority());
        loan.setTag(dto.getTag());
        loan.setPaymentType(dto.getPaymentType());
        loan.setInstallmentCount(dto.getInstallmentCount());
        loan.setExpectedInstallmentAmount(dto.getExpectedInstallmentAmount());
        loan.setNotes(dto.getNotes());

        if (isNew) {
            loan.setRemainingAmount(dto.getAmount());
            loan.setStatus(LoanStatus.PENDING);
        } else {
            // Amount may have been edited; keep remaining consistent with amount minus what's already paid.
            BigDecimal alreadyPaid = loan.getAmount().subtract(loan.getRemainingAmount());
            BigDecimal newRemaining = dto.getAmount().subtract(alreadyPaid);
            if (newRemaining.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("New loan amount is less than the amount already paid.");
            }
            loan.setRemainingAmount(newRemaining);
            recalculateStatus(loan);
        }

        return loanRepository.save(loan);
    }

    @Override
    public List<Loan> getAllLoans(User user) {
        return loanRepository.findByUserOrderByDueDateAsc(user);
    }

    @Override
    public List<Loan> searchLoans(User user, String term) {
        if (!StringUtils.hasText(term)) {
            return getAllLoans(user);
        }
        return loanRepository.search(user, term.trim());
    }

    @Override
    public List<Loan> filterByStatus(User user, LoanStatus status) {
        return loanRepository.findByUserAndStatusOrderByDueDateAsc(user, status);
    }

    @Override
    public List<Loan> filterByPriority(User user, Priority priority) {
        return loanRepository.findByUserAndPriorityOrderByDueDateAsc(user, priority);
    }

    @Override
    public List<Loan> getDueToday(User user) {
        return loanRepository.findByUserAndDueDateOrderByDueDateAsc(user, LocalDate.now());
    }

    @Override
    public List<Loan> getDueTomorrow(User user) {
        return loanRepository.findByUserAndDueDateOrderByDueDateAsc(user, LocalDate.now().plusDays(1));
    }

    @Override
    public List<Loan> getDueThisWeek(User user) {
        LocalDate today = LocalDate.now();
        return loanRepository.findByUserAndDueDateBetweenOrderByDueDateAsc(user, today, today.plusDays(7));
    }

    @Override
    public List<Loan> getOverdueLoans(User user) {
        return loanRepository.findByUserAndStatusOrderByDueDateAsc(user, LoanStatus.OVERDUE);
    }

    @Override
    public Loan getLoanById(Long id, User user) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        if (!loan.getUser().equals(user)) {
            throw new ResourceNotFoundException("Loan not found");
        }
        return loan;
    }

    @Override
    public void deleteLoan(Long id, User user) {
        Loan loan = getLoanById(id, user);
        loanRepository.delete(loan);
    }


    @Override
    public void refreshOverdueStatuses() {
        LocalDate today = LocalDate.now();
        List<Loan> candidates = loanRepository.findAllUnpaidForScheduler();
        for (Loan loan : candidates) {
            if (loan.getDueDate().isBefore(today) && loan.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
                loan.setStatus(LoanStatus.OVERDUE);
                loanRepository.save(loan);
            }
        }
    }

    @Override
    public void applyPayment(Loan loan, BigDecimal paymentAmount) {
        BigDecimal newRemaining = loan.getRemainingAmount().subtract(paymentAmount);
        if (newRemaining.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Payment cannot exceed the remaining balance.");
        }
        loan.setRemainingAmount(newRemaining);
        recalculateStatus(loan);
        loanRepository.save(loan);
    }

    /** Recomputes status from remaining balance vs. due date; called after a loan or payment change. */
    private void recalculateStatus(Loan loan) {
        if (loan.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(LoanStatus.PAID);
            loan.setRemainingAmount(BigDecimal.ZERO);
        } else if (loan.getDueDate().isBefore(LocalDate.now())) {
            loan.setStatus(LoanStatus.OVERDUE);
        } else if (loan.getRemainingAmount().compareTo(loan.getAmount()) < 0) {
            loan.setStatus(LoanStatus.PARTIALLY_PAID);
        } else {
            loan.setStatus(LoanStatus.PENDING);
        }
    }
}
