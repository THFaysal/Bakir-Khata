package com.example.bakir_khata.service;


import com.example.bakir_khata.dto.RegistrationDTO;
import com.example.bakir_khata.model.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    User register(RegistrationDTO dto);
    User getUserByEmail(String email);
    boolean emailExists(String email);
    boolean phoneExists(String phone);
    void updateProfilePicture(User user, MultipartFile file);
    void deleteAccount(User user);
}
