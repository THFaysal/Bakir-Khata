package com.example.bakir_khata.controller;


import com.example.bakir_khata.model.User;
import com.example.bakir_khata.repository.LoanRepository;
import com.example.bakir_khata.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;

/** Injects the current user and a live notification count into every view's model. */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final UserService userService;
    private final LoanRepository loanRepository;

    @ModelAttribute("currentUser")
    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        try {
            return userService.getUserByEmail(auth.getName());
        } catch (Exception e) {
            return null;
        }
    }

    @ModelAttribute("notificationCount")
    public long notificationCount() {
        User user = currentUser();
        if (user == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        long dueToday = loanRepository.findByUserAndDueDateOrderByDueDateAsc(user, today).size();
        long overdue = loanRepository.findByUserAndStatusOrderByDueDateAsc(user, com.example.bakir_khata.model.enums.LoanStatus.OVERDUE).size();
        return dueToday + overdue;
    }
}
