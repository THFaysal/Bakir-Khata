package com.example.bakir_khata.service.impl;

import com.example.bakir_khata.dto.PaymentInitiationDTO;
import com.example.bakir_khata.dto.TransactionDTO;
import com.example.bakir_khata.dto.TransactionSubmitDTO;
import com.example.bakir_khata.exception.BusinessRuleException;
import com.example.bakir_khata.exception.CounterpartyNotRegisteredException;
import com.example.bakir_khata.exception.InvalidTransactionException;
import com.example.bakir_khata.exception.UnauthorizedActionException;
import com.example.bakir_khata.model.*;
import com.example.bakir_khata.model.enums.*;
import com.example.bakir_khata.repository.*;
import com.example.bakir_khata.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final RevenueLedgerRepository revenueLedgerRepository;
    private final NotificationService notificationService;
    private final FinancialPolicyService financialPolicyService;
    private final LoanService loanService;

    @Override
    @Transactional
    public TransactionDTO initiatePayment(PaymentInitiationDTO dto, User borrower) {
        if (!dto.holdConfirmed()) {
            throw new BusinessRuleException("Hold the payment button for 3 seconds to confirm the transaction.");
        }
        Loan loan = ownedLoan(dto.loanId(), borrower);
        validateAmount(loan, dto.amount());
        User counterparty = requireLinkedLender(loan, borrower);
        List<TransactionStatus> activeStatuses = List.of(
                TransactionStatus.PENDING,
                TransactionStatus.AWAITING_LENDER_CONFIRMATION);
        if (transactionRepository.existsByLoan_IdAndInitiatedBy_IdAndStatusIn(loan.getId(), borrower.getId(), activeStatuses)) {
            throw new BusinessRuleException("This loan already has a payment waiting for confirmation or verification. Finish or reject it before starting another.");
        }

        if (dto.method() != TransactionMethod.CASH) {
            validatePaymentAccount(dto.paymentAccountId(), counterparty, dto.method());
            throw new BusinessRuleException(
                    "This payment system is not available in the student project demo. Please use Cash for the complete confirmation workflow.");
        }

        BigDecimal serviceFee = financialPolicyService.calculateServiceFee(dto.amount());
        BigDecimal penalty = financialPolicyService.calculateOverduePenalty(loan, LocalDate.now());
        loan.setOverduePenalty(penalty);
        loan.setOverdueDays(financialPolicyService.overdueDays(loan, LocalDate.now()));
        loanRepository.save(loan);
        BigDecimal platformPenalty = financialPolicyService.platformPenaltyShare(penalty);
        BigDecimal lenderPenalty = financialPolicyService.lenderPenaltyShare(penalty);
        BigDecimal totalPayable = money(dto.amount().add(serviceFee).add(penalty));

        Transaction.TransactionBuilder builder = Transaction.builder()
                .loan(loan)
                .initiatedBy(borrower)
                .counterpartyUser(counterparty)
                .amount(money(dto.amount()))
                .serviceFee(serviceFee)
                .overduePenaltyAmount(penalty)
                .penaltyPlatformShare(platformPenalty)
                .penaltyLenderShare(lenderPenalty)
                .totalPayable(totalPayable)
                .method(dto.method())
                .note(dto.note());

        builder.status(TransactionStatus.AWAITING_LENDER_CONFIRMATION)
                .paymentProvider("Cash")
                .recipientMasked(counterparty.getName());

        Transaction transaction = transactionRepository.save(builder.build());

        notificationService.notify(counterparty, "CASH_PAYMENT_REQUEST",
                borrower.getName() + " says they paid ৳" + transaction.getAmount() + " in cash for loan #" + loan.getId() + ". Please accept or reject it.",
                transaction.getId());

        return toDTO(transaction);
    }
    
    @Override
    @Transactional
    public TransactionDTO submit(TransactionSubmitDTO dto, User borrower) {
        PaymentInitiationDTO modern =
                new PaymentInitiationDTO(
                        dto.loanId(),
                        dto.amount(),
                        dto.method(),
                        null,
                        null,
                        dto.note(),
                        true
                );

        return initiatePayment(modern, borrower);
    }
    @Override
    @Transactional
    public TransactionDTO accept(Long transactionId, User lenderUser) {
        Transaction transaction = loadOwnedByCounterparty(transactionId, lenderUser);
        if (transaction.getStatus() != TransactionStatus.AWAITING_LENDER_CONFIRMATION
                && transaction.getStatus() != TransactionStatus.PENDING) {
            throw new InvalidTransactionException("This cash payment has already been handled.");
        }
        if (transaction.getMethod() != TransactionMethod.CASH) {
            throw new InvalidTransactionException("Only cash payments require lender confirmation.");
        }
        return finalizeSuccessfulTransaction(transaction, true);
    }

    @Override
    @Transactional
    public TransactionDTO reject(Long transactionId, User lenderUser) {
        Transaction transaction = loadOwnedByCounterparty(transactionId, lenderUser);
        if (transaction.getStatus() != TransactionStatus.AWAITING_LENDER_CONFIRMATION
                && transaction.getStatus() != TransactionStatus.PENDING) {
            throw new InvalidTransactionException("This transaction has already been handled.");
        }
        transaction.setStatus(TransactionStatus.REJECTED);
        transaction.setRespondedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
        notificationService.notify(transaction.getInitiatedBy(), "TRANSACTION_REJECTED",
                "Your cash payment request of ৳" + transaction.getAmount() + " for loan #" + transaction.getLoan().getId() + " was rejected.",
                transaction.getId());
        return toDTO(transaction);
    }

    @Override
    @Transactional
    public TransactionDTO cancel(Long transactionId, User borrower) {
        Transaction transaction = loadOwnedByInitiator(transactionId, borrower);
        if (transaction.getStatus() != TransactionStatus.OTP_REQUIRED
                && transaction.getStatus() != TransactionStatus.AWAITING_LENDER_CONFIRMATION
                && transaction.getStatus() != TransactionStatus.PENDING) {
            throw new InvalidTransactionException("Only a payment still waiting for verification/confirmation can be cancelled.");
        }
        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setRespondedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
        if (transaction.getCounterpartyUser() != null) {
            notificationService.notify(transaction.getCounterpartyUser(), "PAYMENT_CANCELLED",
                    borrower.getName() + " cancelled payment request #" + transaction.getId() + ".", transaction.getId());
        }
        return toDTO(transaction);
    }

    @Override
    public List<TransactionDTO> getPendingForCurrentUser(User lenderUser) {
        return transactionRepository.findForCounterpartyByStatuses(lenderUser.getId(),
                        List.of(TransactionStatus.AWAITING_LENDER_CONFIRMATION, TransactionStatus.PENDING))
                .stream().map(this::toDTO).toList();
    }

    @Override
    public List<TransactionDTO> getSubmittedByCurrentUser(User borrower) {
        return transactionRepository.findAllInitiatedBy(borrower.getId()).stream().map(this::toDTO).toList();
    }

    @Override
    public List<TransactionDTO> getAllForReview() {
        return transactionRepository.findAllForReview().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public void setReviewFlag(Long transactionId, boolean flagged, String reviewNote, User reviewer) {
        if (reviewer.getRole() != Role.ADMIN && reviewer.getRole() != Role.COORDINATOR) {
            throw new UnauthorizedActionException("Only admin or coordinator can review system transactions.");
        }
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found."));
        transaction.setFlagged(flagged);
        transaction.setReviewNote(reviewNote == null || reviewNote.isBlank() ? null : reviewNote.trim());
        transaction.setReviewedBy(reviewer);
        transaction.setReviewedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    private TransactionDTO finalizeSuccessfulTransaction(Transaction transaction, boolean lenderConfirmed) {
        Loan loan = transaction.getLoan();
        if (transaction.getAmount().compareTo(loan.getRemainingAmount()) > 0) {
            throw new BusinessRuleException("The loan balance changed and this payment now exceeds the remaining principal.");
        }

        Payment payment = Payment.builder()
                .loan(loan)
                .amount(transaction.getAmount())
                .serviceFee(transaction.getServiceFee())
                .penaltyAmount(transaction.getOverduePenaltyAmount())
                .totalCharged(transaction.getTotalPayable())
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.valueOf(transaction.getMethod().name()))
                .notes(transaction.getNote())
                .build();

        loanService.applyPayment(loan, transaction.getAmount());
        BigDecimal previouslyPaidPenalty = loan.getTotalPenaltyPaid() == null ? BigDecimal.ZERO : loan.getTotalPenaltyPaid();
        loan.setTotalPenaltyPaid(money(previouslyPaidPenalty.add(transaction.getOverduePenaltyAmount())));
        if (transaction.getOverduePenaltyAmount() != null && transaction.getOverduePenaltyAmount().signum() > 0) {
            loan.setPenaltySettledThrough(transaction.getCreatedAt().toLocalDate());
        }
        loan.setOverdueDays(financialPolicyService.overdueDays(loan, LocalDate.now()));
        loan.setOverduePenalty(financialPolicyService.calculateOverduePenalty(loan, LocalDate.now()));
        loanRepository.save(loan);
        payment = paymentRepository.save(payment);

        transaction.setPayment(payment);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setRespondedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        if (!revenueLedgerRepository.existsByTransaction_Id(transaction.getId())) {
            revenueLedgerRepository.save(RevenueLedger.builder()
                    .transaction(transaction)
                    .serviceFeeRevenue(transaction.getServiceFee())
                    .penaltyRevenue(transaction.getPenaltyPlatformShare())
                    .grossRevenue(transaction.getServiceFee().add(transaction.getPenaltyPlatformShare()))
                    .build());
        }

        String verification = lenderConfirmed ? "confirmed by the lender" : "processed by the digital payment gateway";
        notificationService.notify(transaction.getInitiatedBy(), "PAYMENT_SUCCESS",
                "Payment of ৳" + transaction.getAmount() + " for loan #" + loan.getId() + " succeeded (" + verification + ").",
                transaction.getId());
        notificationService.notify(transaction.getCounterpartyUser(), "PAYMENT_RECEIVED",
                "A payment of ৳" + transaction.getAmount() + " for loan #" + loan.getId() + " was completed.",
                transaction.getId());
        return toDTO(transaction);
    }

    private Loan ownedLoan(Long loanId, User borrower) {
        if (loanId == null) throw new BusinessRuleException("Loan is required.");
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new InvalidTransactionException("Loan not found."));
        if (!loan.getUser().getId().equals(borrower.getId())) throw new UnauthorizedActionException("You do not own this loan.");
        return loan;
    }

    private User requireLinkedLender(Loan loan, User borrower) {
        User counterparty = loan.getLender().getLinkedUser();
        if (counterparty == null) throw new CounterpartyNotRegisteredException("This lender must have a linked Bakir Khata account before using confirmed payments.");
        if (counterparty.getId().equals(borrower.getId())) throw new InvalidTransactionException("You cannot pay yourself.");
        if (counterparty.getAccountStatus() != AccountStatus.ACTIVE) throw new BusinessRuleException("The lender account is not active.");
        return counterparty;
    }

    private void validateAmount(Loan loan, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new BusinessRuleException("Amount must be greater than zero.");
        if (amount.compareTo(loan.getRemainingAmount()) > 0) throw new BusinessRuleException("Payment cannot exceed the remaining principal of ৳" + loan.getRemainingAmount() + ".");
    }

    private PaymentAccount validatePaymentAccount(Long accountId, User counterparty, TransactionMethod method) {
        if (accountId == null) throw new BusinessRuleException("Select one of the lender's verified payment accounts.");
        PaymentAccount account = paymentAccountRepository.findById(accountId).orElseThrow(() -> new BusinessRuleException("Payment account not found."));
        if (!account.getUser().getId().equals(counterparty.getId()) || !account.isActive()) throw new BusinessRuleException("That payment destination is not available for this lender.");
        if (method == TransactionMethod.BANK && account.getType() != PaymentAccountType.BANK) throw new BusinessRuleException("Select a bank account for bank payment.");
        if (method == TransactionMethod.MOBILE_BANKING && account.getType() != PaymentAccountType.MOBILE) throw new BusinessRuleException("Select a mobile payment account.");
        return account;
    }

    private String providerName(PaymentAccount account) {
        if (account.getType() == PaymentAccountType.BANK) return account.getBankName();
        return account.getMobileProvider() == null ? "Mobile Banking" : account.getMobileProvider().getDisplayName();
    }

    private Transaction loadOwnedByCounterparty(Long transactionId, User lenderUser) {
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() -> new InvalidTransactionException("Transaction not found."));
        if (!transaction.getCounterpartyUser().getId().equals(lenderUser.getId())) throw new UnauthorizedActionException("This transaction was not sent to you.");
        return transaction;
    }

    private Transaction loadOwnedByInitiator(Long transactionId, User borrower) {
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() -> new InvalidTransactionException("Transaction not found."));
        if (!transaction.getInitiatedBy().getId().equals(borrower.getId())) throw new UnauthorizedActionException("This transaction does not belong to you.");
        return transaction;
    }

    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String clean = phone.trim();
        if (clean.length() <= 4) return "****";
        return "*".repeat(Math.max(4, clean.length() - 4)) + clean.substring(clean.length() - 4);
    }

    private TransactionDTO toDTO(Transaction t) {
        return new TransactionDTO(t.getId(), t.getLoan().getId(), t.getInitiatedBy().getName(), t.getCounterpartyUser().getName(),
                t.getAmount(), t.getServiceFee(), t.getOverduePenaltyAmount(), t.getTotalPayable(), t.getMethod(),
                t.getPaymentProvider(), t.getRecipientMasked(), t.getTransactionReferenceId(), t.getGatewayReference(),
                maskPhone(t.getVerificationPhone()), t.getStatus(), t.isOtpVerified(), t.isFlagged(), t.getReviewNote(), t.getCreatedAt(), t.getRespondedAt());
    }
}
