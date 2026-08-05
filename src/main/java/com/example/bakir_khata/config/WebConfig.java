package com.example.bakir_khata.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/** Serves user-uploaded files (lender photos, payment proofs) from an external directory
 *  so they survive application redeploys instead of living on the classpath. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + new File(uploadRoot).getAbsolutePath() + File.separator;
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
