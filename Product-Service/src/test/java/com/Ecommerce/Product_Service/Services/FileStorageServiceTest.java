package com.Ecommerce.Product_Service.Services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("File Storage Service Tests")
class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();

        // Set test values using reflection
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileStorageService, "maxFileSize", 10485760L); // 10MB

        // Initialize the service
        fileStorageService.init();
    }

    @Test
    @DisplayName("Should store file successfully")
    void storeFile_WithValidImage_ShouldStoreFile() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "test-image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // When
        String fileName = fileStorageService.storeFile(file);

        // Then
        assertThat(fileName).isNotNull();
        assertThat(fileName).contains("product_");
        assertThat(fileName).endsWith(".jpg");

        // Verify file exists
        Path storedFile = tempDir.resolve(fileName);
        assertTrue(Files.exists(storedFile));

        // Verify file content
        String content = Files.readString(storedFile);
        assertThat(content).isEqualTo("test image content");
    }

    @Test
    @DisplayName("Should store multiple files successfully")
    void storeFiles_WithValidImages_ShouldStoreAllFiles() {
        // Given
        MockMultipartFile[] files = {
                new MockMultipartFile("file1", "test1.jpg", "image/jpeg", "content1".getBytes()),
                new MockMultipartFile("file2", "test2.png", "image/png", "content2".getBytes())
        };

        // When
        List<String> fileNames = fileStorageService.storeFiles(files);

        // Then
        assertThat(fileNames).hasSize(2);
        assertThat(fileNames.get(0)).endsWith(".jpg");
        assertThat(fileNames.get(1)).endsWith(".png");

        // Verify both files exist
        for (String fileName : fileNames) {
            Path storedFile = tempDir.resolve(fileName);
            assertTrue(Files.exists(storedFile));
        }
    }

    @Test
    @DisplayName("Should throw exception for empty file")
    void storeFile_WithEmptyFile_ShouldThrowException() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "empty", "empty.jpg", "image/jpeg", new byte[0]
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileStorageService.storeFile(emptyFile);
        });

        assertThat(exception.getMessage()).contains("Failed to store empty file");
    }

    @Test
    @DisplayName("Should throw exception for invalid file type")
    void storeFile_WithInvalidFileType_ShouldThrowException() {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
                "invalid", "test.txt", "text/plain", "text content".getBytes()
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileStorageService.storeFile(invalidFile);
        });

        assertThat(exception.getMessage()).contains("Invalid file type");
    }

    @Test
    @DisplayName("Should throw exception for file too large")
    void storeFile_WithTooLargeFile_ShouldThrowException() {
        // Given
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB, exceeds 10MB limit
        MockMultipartFile largeFile = new MockMultipartFile(
                "large", "large.jpg", "image/jpeg", largeContent
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileStorageService.storeFile(largeFile);
        });

        assertThat(exception.getMessage()).contains("File size exceeds maximum allowed size");
    }

    @Test
    @DisplayName("Should load file as resource")
    void loadFileAsResource_WhenFileExists_ShouldReturnResource() throws IOException {
        // Given
        String fileName = "test-resource.jpg";
        Path testFile = tempDir.resolve(fileName);
        Files.write(testFile, "test content".getBytes());

        // When
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // Then
        assertThat(resource).isNotNull();
        assertTrue(resource.exists());
        assertThat(resource.getFilename()).isEqualTo(fileName);
    }

    @Test
    @DisplayName("Should throw exception when loading non-existent file")
    void loadFileAsResource_WhenFileNotExists_ShouldThrowException() {
        // Given
        String nonExistentFile = "non-existent.jpg";

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileStorageService.loadFileAsResource(nonExistentFile);
        });

        assertThat(exception.getMessage()).contains("File not found " + nonExistentFile);
    }

    @Test
    @DisplayName("Should delete file successfully")
    void deleteFile_WhenFileExists_ShouldDeleteFile() throws IOException {
        // Given
        String fileName = "to-delete.jpg";
        Path testFile = tempDir.resolve(fileName);
        Files.write(testFile, "content to delete".getBytes());
        assertTrue(Files.exists(testFile));

        // When
        boolean deleted = fileStorageService.deleteFile(fileName);

        // Then
        assertTrue(deleted);
        assertFalse(Files.exists(testFile));
    }

    @Test
    @DisplayName("Should return false when deleting non-existent file")
    void deleteFile_WhenFileNotExists_ShouldReturnFalse() {
        // Given
        String nonExistentFile = "non-existent.jpg";

        // When
        boolean deleted = fileStorageService.deleteFile(nonExistentFile);

        // Then
        assertFalse(deleted);
    }

    @Test
    @DisplayName("Should generate correct file URL")
    void getFileUrl_ShouldReturnCorrectUrl() {
        // Given
        String fileName = "test-image.jpg";

        // When
        String url = fileStorageService.getFileUrl(fileName);

        // Then
        assertThat(url).isEqualTo("/api/products/images/" + fileName);
    }

    @Test
    @DisplayName("Should generate multiple file URLs")
    void getFileUrls_ShouldReturnCorrectUrls() {
        // Given
        List<String> fileNames = List.of("image1.jpg", "image2.png");

        // When
        List<String> urls = fileStorageService.getFileUrls(fileNames);

        // Then
        assertThat(urls).hasSize(2);
        assertThat(urls.get(0)).isEqualTo("/api/products/images/image1.jpg");
        assertThat(urls.get(1)).isEqualTo("/api/products/images/image2.png");
    }

    @Test
    @DisplayName("Should extract filename from URL correctly")
    void extractFileNameFromUrl_ShouldReturnFileName() {
        // Given
        String url = "/api/products/images/test-image.jpg";

        // When
        String fileName = fileStorageService.extractFileNameFromUrl(url);

        // Then
        assertThat(fileName).isEqualTo("test-image.jpg");
    }

    @Test
    @DisplayName("Should return null for invalid URL")
    void extractFileNameFromUrl_WithInvalidUrl_ShouldReturnNull() {
        // When & Then
        assertThat(fileStorageService.extractFileNameFromUrl(null)).isNull();
        assertThat(fileStorageService.extractFileNameFromUrl("")).isNull();
    }

    @Test
    @DisplayName("Should check if file exists")
    void fileExists_ShouldReturnCorrectResult() throws IOException {
        // Given
        String existingFile = "existing.jpg";
        String nonExistentFile = "non-existent.jpg";

        Path testFile = tempDir.resolve(existingFile);
        Files.write(testFile, "content".getBytes());

        // When & Then
        assertTrue(fileStorageService.fileExists(existingFile));
        assertFalse(fileStorageService.fileExists(nonExistentFile));
    }

    @Test
    @DisplayName("Should get correct file size")
    void getFileSize_ShouldReturnCorrectSize() throws IOException {
        // Given
        String fileName = "size-test.jpg";
        String content = "test content for size";

        Path testFile = tempDir.resolve(fileName);
        Files.write(testFile, content.getBytes());

        // When
        long size = fileStorageService.getFileSize(fileName);

        // Then
        assertThat(size).isEqualTo(content.getBytes().length);
    }

    @Test
    @DisplayName("Should return 0 for non-existent file size")
    void getFileSize_WhenFileNotExists_ShouldReturnZero() {
        // Given
        String nonExistentFile = "non-existent.jpg";

        // When
        long size = fileStorageService.getFileSize(nonExistentFile);

        // Then
        assertThat(size).isEqualTo(0);
    }

    @Test
    @DisplayName("Should validate file extension correctly")
    void storeFile_WithValidExtensions_ShouldStoreFile() {
        // Given
        String[] validExtensions = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};
        String[] validMimeTypes = {"image/jpeg", "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"};

        // When & Then
        for (int i = 0; i < validExtensions.length; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "test",
                    "test." + validExtensions[i],
                    validMimeTypes[i],
                    "content".getBytes()
            );

            int finalI = i;
            assertDoesNotThrow(() -> {
                String fileName = fileStorageService.storeFile(file);
                assertThat(fileName).endsWith("." + validExtensions[finalI]);
            });
        }
    }
}