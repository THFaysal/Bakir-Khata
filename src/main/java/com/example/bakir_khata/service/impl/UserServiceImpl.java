package com.example.bakir_khata.service.impl;


import com.example.bakir_khata.dto.RegistrationDTO;
import com.example.bakir_khata.exception.BusinessRuleException;
import com.example.bakir_khata.exception.ResourceNotFoundException;
import com.example.bakir_khata.model.User;
import com.example.bakir_khata.repository.UserRepository;
import com.example.bakir_khata.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(RegistrationDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessRuleException("An account with this email already exists.");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessRuleException("Passwords do not match.");
        }
        if (!dto.isPinConfirmed()) {
            throw new BusinessRuleException("PIN and confirm PIN do not match.");
        }
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (dto.isPinProvided()) {
            user.setPin(passwordEncoder.encode(dto.getPin()));
        }
        return userRepository.save(user);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
