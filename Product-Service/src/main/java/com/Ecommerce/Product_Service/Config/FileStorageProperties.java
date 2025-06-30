package com.Ecommerce.Product_Service.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "file")
@Data
public class FileStorageProperties {

    private String uploadDir = "./uploads/images";
    private long maxSize = 10485760; // 10MB default
    private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp");
}