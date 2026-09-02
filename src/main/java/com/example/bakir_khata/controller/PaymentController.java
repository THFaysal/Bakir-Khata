package com.example.bakir_khata.controller;

import com.example.bakir_khata.dto.PaymentInitiationDTO;
import com.example.bakir_khata.exception.BusinessRuleException;
import com.example.bakir_khata.model.Loan;
import com.example.bakir_khata.model.PaymentAccount;
import com.example.bakir_khata.model.User;
import com.example.bakir_khata.model.enums.TransactionMethod;
import com.example.bakir_khata.repository.PaymentAccountRepository;
import com.example.bakir_khata.service.FinancialPolicyService;
import com.example.bakir_khata.service.LoanService;
import com.example.bakir_khata.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final TransactionService transactionService;
    private final LoanService loanService;
    private final PaymentAccountRepository paymentAccountRepository;
    private final FinancialPolicyService financialPolicyService;

    @GetMapping("/new")
    public String newForm(@RequestParam(value = "loanId", required = false) Long loanId,
                          @AuthenticationPrincipal User user, Model model) {
        if (!model.containsAttribute("paymentDTO")) {
            model.addAttribute("paymentDTO",
                    new PaymentInitiationDTO(loanId, null, TransactionMethod.CASH, null, null, null, false));
        }
        addPaymentModel(user, model);
        return "payments/form";
    }

    @PostMapping("/start")
    public String start(@Valid @ModelAttribute("paymentDTO") PaymentInitiationDTO dto,
                        BindingResult bindingResult,
                        @AuthenticationPrincipal User user,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (dto.loanId() != null && dto.amount() != null) {
            Loan loan = loanService.getLoanById(dto.loanId(), user);
            if (dto.amount().compareTo(loan.getRemainingAmount()) > 0) {
                bindingResult.rejectValue("amount", "exceeds",
                        "Payment cannot exceed the remaining principal of " + loan.getRemainingAmount());
            }
        }

        if (dto.method() != TransactionMethod.CASH && dto.paymentAccountId() == null) {
            bindingResult.rejectValue("paymentAccountId", "required",
                    "No lender payment destination is available for the selected method.");
        }

        if (bindingResult.hasErrors()) {
            addPaymentModel(user, model);
            return "payments/form";
        }

        // Student-project rule: show the lender's bank/mobile destination,
        // but do not pretend to process a real digital payment.
        if (dto.method() != TransactionMethod.CASH) {
            throw new BusinessRuleException(
                    "This payment system is not available in the student project demo. Please use Cash for the complete confirmation workflow.");
        }

        transactionService.initiatePayment(dto, user);
        redirectAttributes.addFlashAttribute("successMessage",
                "Cash payment request sent. The lender must accept it before the payment becomes successful.");
        return "redirect:/transactions";
    }

    private void addPaymentModel(User user, Model model) {
        List<Loan> loans = loanService.getAllLoans(user).stream()
                .filter(l -> l.getRemainingAmount().signum() > 0)
                .toList();

        for (Loan loan : loans) {
            loan.setOverdueDays(financialPolicyService.overdueDays(loan, LocalDate.now()));
            loan.setOverduePenalty(financialPolicyService.calculateOverduePenalty(loan, LocalDate.now()));
        }

        Set<Long> lenderUserIds = new HashSet<>();
        for (Loan loan : loans) {
            if (loan.getLender().getLinkedUser() != null) {
                lenderUserIds.add(loan.getLender().getLinkedUser().getId());
            }
        }

        List<PaymentAccount> accounts = lenderUserIds.stream()
                .flatMap(id -> paymentAccountRepository
                        .findByUserIdAndActiveTrueOrderByPrimaryAccountDescCreatedAtAsc(id)
                        .stream())
                .toList();

        model.addAttribute("loans", loans);
        model.addAttribute("paymentAccounts", accounts);
        model.addAttribute("serviceFeePercent", financialPolicyService.getServiceFeePercent());
    }
}
