package com.example.bakir_khata.controller;


import com.example.bakir_khata.dto.RegistrationDTO;
import com.example.bakir_khata.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registrationDTO")) {
            model.addAttribute("registrationDTO", new RegistrationDTO());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationDTO") RegistrationDTO dto,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {

        if (dto.email() != null && userService.emailExists(dto.email())) {
            bindingResult.rejectValue("email", "duplicate", "An account with this email already exists.");
        }
        if (dto.phone() != null && userService.phoneExists(dto.phone())) {
            bindingResult.rejectValue("phone", "duplicate", "An account with this phone number already exists.");
        }
        if (!dto.password().equals(dto.confirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (!dto.isPinConfirmed()) {
            bindingResult.rejectValue("confirmPin", "mismatch", "PIN and confirm PIN do not match");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        userService.register(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Account created successfully. Please log in.");
        return "redirect:/login";
    }
}