package com.Ecommerce.Product_Service.Services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir:./uploads/images}")
    private String uploadDir;

    @Value("${file.max-size:10485760}") // 10MB default
    private long maxFileSize;

    private Path fileStorageLocation;

    // Allowed image types
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
    );

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("File storage directory created/verified at: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            log.error("Could not create the directory where the uploaded files will be stored.", ex);
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    /**
     * Store a single file and return the filename
     */
    public String storeFile(MultipartFile file) {
        validateFile(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);

        // Generate unique filename with timestamp
        String fileName = generateUniqueFileName(fileExtension);

        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence " + fileName);
            }

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored successfully: {}", fileName);
            return fileName;
        } catch (IOException ex) {
            log.error("Could not store file {}. Please try again!", fileName, ex);
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    /**
     * Store multiple files and return list of filenames
     */
    public List<String> storeFiles(MultipartFile[] files) {
        return Arrays.stream(files)
                .map(this::storeFile)
                .toList();
    }

    /**
     * Load file as Resource
     */
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            log.error("File not found {}", fileName, ex);
            throw new RuntimeException("File not found " + fileName, ex);
        }
    }

    /**
     * Delete a file
     */
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                log.info("File deleted successfully: {}", fileName);
            } else {
                log.warn("File not found for deletion: {}", fileName);
            }

            return deleted;
        } catch (IOException ex) {
            log.error("Could not delete file {}", fileName, ex);
            return false;
        }
    }

    /**
     * Delete multiple files
     */
    public void deleteFiles(List<String> fileNames) {
        fileNames.forEach(this::deleteFile);
    }

    /**
     * Get file URL for web access
     */
    public String getFileUrl(String fileName) {
        return "/api/products/images/" + fileName;
    }

    /**
     * Get multiple file URLs
     */
    public List<String> getFileUrls(List<String> fileNames) {
        return fileNames.stream()
                .map(this::getFileUrl)
                .toList();
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file");
        }

        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Invalid file type. Only image files are allowed: " + ALLOWED_EXTENSIONS);
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new RuntimeException("Invalid file name");
        }

        String fileExtension = getFileExtension(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase())) {
            throw new RuntimeException("Invalid file extension. Allowed extensions: " + ALLOWED_EXTENSIONS);
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    /**
     * Generate unique filename with timestamp and UUID
     */
    private String generateUniqueFileName(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("product_%s_%s.%s", timestamp, uuid, extension);
    }

    /**
     * Extract filename from URL
     */
    public String extractFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // Extract filename from URL like "/api/products/images/filename.jpg"
        String[] parts = url.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            return Files.exists(filePath);
        } catch (Exception ex) {
            log.error("Error checking file existence: {}", fileName, ex);
            return false;
        }
    }

    /**
     * Get file size
     */
    public long getFileSize(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            return Files.size(filePath);
        } catch (IOException ex) {
            log.error("Error getting file size: {}", fileName, ex);
            return 0;
        }
    }
}