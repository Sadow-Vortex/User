package com.example.user;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dtnzrl7px",
                "api_key", "835951476592934",
                "api_secret", "E_kiQd0_0LAaL3r3Kj9hbB2uhlU"
        ));
    }
}