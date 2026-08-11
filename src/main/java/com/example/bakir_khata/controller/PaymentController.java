package com.example.bakir_khata.controller;


import com.example.bakir_khata.dto.PaymentDTO;
import com.example.bakir_khata.model.User;
import com.example.bakir_khata.service.LoanService;
import com.example.bakir_khata.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final LoanService loanService;

    @GetMapping("/new")
    public String newForm(@RequestParam(value = "loanId", required = false) Long loanId,
                          @AuthenticationPrincipal User user, Model model) {
        model.addAttribute("paymentDTO", new PaymentDTO(loanId));
        model.addAttribute("loans", loanService.getAllLoans(user));
        return "payments/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("paymentDTO") PaymentDTO dto,
                       BindingResult bindingResult,
                       @AuthenticationPrincipal User user,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (dto.loanId() != null) {
            var loan = loanService.getLoanById(dto.loanId(), user);
            if (dto.amount() != null && loan.getRemainingAmount().compareTo(dto.amount()) < 0) {
                bindingResult.rejectValue("amount", "exceeds", "Payment cannot exceed the remaining balance of " + loan.getRemainingAmount());
            }
            if (dto.paymentDate() != null) {
                if (dto.paymentDate().isBefore(loan.getBorrowDate())) {
                    bindingResult.rejectValue("paymentDate", "beforeIssue",
                            "Payment date cannot be before the loan's issue date (" + loan.getBorrowDate() + ").");
                } else if (dto.paymentDate().isAfter(LocalDate.now())) {
                    bindingResult.rejectValue("paymentDate", "future",
                            "Payment date cannot be in the future.");
                }
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("loans", loanService.getAllLoans(user));
            return "payments/form";
        }
        var payment = paymentService.recordPayment(dto, user);
        redirectAttributes.addFlashAttribute("successMessage", "Payment recorded successfully.");
        return "redirect:/loans/" + payment.getLoan().getId();
    }
}