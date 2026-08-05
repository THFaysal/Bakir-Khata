package com.example.bakir_khata.controller;


import com.example.bakir_khata.dto.LoanDTO;
import com.example.bakir_khata.model.Loan;
import com.example.bakir_khata.model.User;
import com.example.bakir_khata.model.enums.LoanStatus;
import com.example.bakir_khata.model.enums.Priority;
import com.example.bakir_khata.service.LenderService;
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

import java.util.List;

@Controller
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final LenderService lenderService;
    private final PaymentService paymentService;

    @GetMapping
    public String list(@AuthenticationPrincipal User user,
                        @RequestParam(value = "q", required = false) String q,
                        @RequestParam(value = "status", required = false) LoanStatus status,
                        @RequestParam(value = "priority", required = false) Priority priority,
                        Model model) {
        List<Loan> loans;
        if (q != null && !q.isBlank()) {
            loans = loanService.searchLoans(user, q);
        } else if (status != null) {
            loans = loanService.filterByStatus(user, status);
        } else if (priority != null) {
            loans = loanService.filterByPriority(user, priority);
        } else {
            loans = loanService.getAllLoans(user);
        }
        model.addAttribute("loans", loans);
        model.addAttribute("query", q);
        model.addAttribute("statusFilter", status);
        model.addAttribute("priorityFilter", priority);
        model.addAttribute("statuses", LoanStatus.values());
        model.addAttribute("priorities", Priority.values());
        return "loans/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal User user, Model model) {
        if (!model.containsAttribute("loanDTO")) {
            model.addAttribute("loanDTO", new LoanDTO());
        }
        model.addAttribute("lenders", lenderService.getAllLenders(user));
        return "loans/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {
        Loan loan = loanService.getLoanById(id, user);
        LoanDTO dto = new LoanDTO();
        dto.setId(loan.getId());
        dto.setLenderId(loan.getLender().getId());
        dto.setAmount(loan.getAmount());
        dto.setBorrowDate(loan.getBorrowDate());
        dto.setDueDate(loan.getDueDate());
        dto.setPurpose(loan.getPurpose());
        dto.setPriority(loan.getPriority());
        dto.setTag(loan.getTag());
        dto.setPaymentType(loan.getPaymentType());
        dto.setInstallmentCount(loan.getInstallmentCount());
        dto.setExpectedInstallmentAmount(loan.getExpectedInstallmentAmount());
        dto.setNotes(loan.getNotes());
        model.addAttribute("loanDTO", dto);
        model.addAttribute("lenders", lenderService.getAllLenders(user));
        return "loans/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("loanDTO") LoanDTO dto,
                        BindingResult bindingResult,
                        @AuthenticationPrincipal User user,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("lenders", lenderService.getAllLenders(user));
            return "loans/form";
        }
        Loan loan = loanService.saveLoan(dto, user);
        redirectAttributes.addFlashAttribute("successMessage",
                dto.getId() == null ? "Loan added successfully." : "Loan updated successfully.");
        return "redirect:/loans/" + loan.getId();
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {
        Loan loan = loanService.getLoanById(id, user);
        model.addAttribute("loan", loan);
        model.addAttribute("payments", paymentService.getPaymentsForLoan(id, user));
        return "loans/details";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal User user,
                          RedirectAttributes redirectAttributes) {
        loanService.deleteLoan(id, user);
        redirectAttributes.addFlashAttribute("successMessage", "Loan deleted.");
        return "redirect:/loans";
    }
}
