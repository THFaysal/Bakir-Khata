package com.example.bakir_khata.controller;

import com.example.bakir_khata.model.Loan;
import com.example.bakir_khata.model.User;
import com.example.bakir_khata.repository.LoanRepository;
import com.example.bakir_khata.service.RatingService;
import com.example.bakir_khata.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final UserService userService;
    private final RatingService ratingService;
    private final LoanRepository loanRepository;

    @GetMapping
    public String profile(@AuthenticationPrincipal User user, Model model) {
        List<Loan> loans = loanRepository.findByUserOrderByDueDateAsc(user);

        BigDecimal totalBorrowed = loans.stream()
                .map(Loan::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = loans.stream()
                .map(Loan::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = totalBorrowed.subtract(remaining);
        boolean hasDue = remaining.compareTo(BigDecimal.ZERO) > 0;

        model.addAttribute("rating", ratingService.calculateRating(user));
        model.addAttribute("totalBorrowed", totalBorrowed);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("remaining", remaining);
        model.addAttribute("hasDue", hasDue);
        model.addAttribute("loanCount", loans.size());
        return "profile/index";
    }

    @PostMapping("/picture")
    public String updatePicture(@AuthenticationPrincipal User user,
                                @RequestParam("profileImage") MultipartFile file,
                                RedirectAttributes redirectAttributes) {
        userService.updateProfilePicture(user, file);
        redirectAttributes.addFlashAttribute("successMessage", "Profile picture updated.");
        return "redirect:/profile";
    }

    @PostMapping("/delete")
    public String deleteAccount(@AuthenticationPrincipal User user,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        userService.deleteAccount(user);
        new SecurityContextLogoutHandler().logout(request, null, null);
        redirectAttributes.addFlashAttribute("successMessage", "Your account has been permanently deleted.");
        return "redirect:/login";
    }
}
