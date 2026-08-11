package com.example.bakir_khata.service.impl;


import com.example.bakir_khata.dto.RegistrationDTO;
import com.example.bakir_khata.exception.BusinessRuleException;
import com.example.bakir_khata.exception.ResourceNotFoundException;
import com.example.bakir_khata.model.Lender;
import com.example.bakir_khata.model.Loan;
import com.example.bakir_khata.model.User;
import com.example.bakir_khata.repository.LenderRepository;
import com.example.bakir_khata.repository.LoanRepository;
import com.example.bakir_khata.repository.UserRepository;
import com.example.bakir_khata.service.FileStorageService;
import com.example.bakir_khata.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final LenderRepository lenderRepository;
    private final LoanRepository loanRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(RegistrationDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new BusinessRuleException("An account with this email already exists.");
        }
        if (userRepository.existsByPhone(dto.phone())) {
            throw new BusinessRuleException("An account with this phone number already exists.");
        }
        if (!dto.password().equals(dto.confirmPassword())) {
            throw new BusinessRuleException("Passwords do not match.");
        }
        if (!dto.isPinConfirmed()) {
            throw new BusinessRuleException("PIN and confirm PIN do not match.");
        }
        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPhone(dto.phone());
        user.setPassword(passwordEncoder.encode(dto.password()));
        if (dto.isPinProvided()) {
            user.setPin(passwordEncoder.encode(dto.pin()));
        }
        return userRepository.save(user);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Override
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean phoneExists(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Override
    public void updateProfilePicture(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Please choose an image to upload.");
        }
        String path = fileStorageService.store(file, "users");
        user.setProfileImagePath(path);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteAccount(User user) {
        List<Loan> loans = loanRepository.findByUserOrderByDueDateAsc(user);
        BigDecimal outstanding = loans.stream()
                .map(Loan::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException(
                    "You still owe ৳ " + outstanding.setScale(2, java.math.RoundingMode.HALF_UP)
                            + " across your loans. Please clear all outstanding dues before deleting your profile.");
        }

        List<Lender> lenders = lenderRepository.findByUserOrderByNameAsc(user);
        lenderRepository.deleteAll(lenders);
        userRepository.delete(user);
    }
}