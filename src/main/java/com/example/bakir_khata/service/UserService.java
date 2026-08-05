package com.example.bakir_khata.service;


import com.example.bakir_khata.dto.RegistrationDTO;
import com.example.bakir_khata.model.User;

public interface UserService {
    User register(RegistrationDTO dto);
    User getUserByEmail(String email);
}
