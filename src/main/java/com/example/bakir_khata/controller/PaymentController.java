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

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final LoanService loanService;

    @GetMapping("/new")
    public String newForm(@RequestParam(value = "loanId", required = false) Long loanId,
                           @AuthenticationPrincipal User user, Model model) {
        PaymentDTO dto = new PaymentDTO();
        dto.setLoanId(loanId);
        model.addAttribute("paymentDTO", dto);
        model.addAttribute("loans", loanService.getAllLoans(user));
        return "payments/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("paymentDTO") PaymentDTO dto,
                        BindingResult bindingResult,
                        @AuthenticationPrincipal User user,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (dto.getLoanId() != null) {
            var loan = loanService.getLoanById(dto.getLoanId(), user);
            if (dto.getAmount() != null && loan.getRemainingAmount().compareTo(dto.getAmount()) < 0) {
                bindingResult.rejectValue("amount", "exceeds", "Payment cannot exceed the remaining balance of " + loan.getRemainingAmount());
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
