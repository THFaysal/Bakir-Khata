package com.example.bakir_khata.service.impl;


import com.example.bakir_khata.exception.BusinessRuleException;
import com.example.bakir_khata.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadRoot;

    @Override
    public String store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot);
        }
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String filename = timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        try {
            Path dir = Paths.get(uploadRoot, subDirectory);
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to store uploaded file: " + e.getMessage());
        }
        return subDirectory + "/" + filename;
    }
}
