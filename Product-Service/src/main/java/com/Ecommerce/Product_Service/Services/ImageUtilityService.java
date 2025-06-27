package com.Ecommerce.Product_Service.Services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
@Slf4j
public class ImageUtilityService {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Resize image to specified dimensions
     */
    public byte[] resizeImage(byte[] originalImage, int targetWidth, int targetHeight, String format) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalImage);
        BufferedImage originalBufferedImage = ImageIO.read(inputStream);

        // Calculate dimensions maintaining aspect ratio
        Dimension newDimension = calculateDimensions(
                originalBufferedImage.getWidth(),
                originalBufferedImage.getHeight(),
                targetWidth,
                targetHeight
        );

        BufferedImage resizedImage = new BufferedImage(
                newDimension.width,
                newDimension.height,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(originalBufferedImage, 0, 0, newDimension.width, newDimension.height, null);
        graphics2D.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, format, outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Create thumbnail from image
     */
    public byte[] createThumbnail(byte[] originalImage, String format) throws IOException {
        return resizeImage(originalImage, 200, 200, format);
    }

    /**
     * Convert image to Base64 string
     */
    public String convertToBase64(byte[] imageBytes) {
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * Convert Base64 string to image bytes
     */
    public byte[] convertFromBase64(String base64Image) {
        return Base64.getDecoder().decode(base64Image);
    }

    /**
     * Calculate new dimensions maintaining aspect ratio
     */
    private Dimension calculateDimensions(int originalWidth, int originalHeight, int targetWidth, int targetHeight) {
        double aspectRatio = (double) originalWidth / originalHeight;

        int newWidth = targetWidth;
        int newHeight = (int) (targetWidth / aspectRatio);

        if (newHeight > targetHeight) {
            newHeight = targetHeight;
            newWidth = (int) (targetHeight * aspectRatio);
        }

        return new Dimension(newWidth, newHeight);
    }

    /**
     * Get image dimensions
     */
    public Dimension getImageDimensions(byte[] imageBytes) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        return new Dimension(bufferedImage.getWidth(), bufferedImage.getHeight());
    }

    /**
     * Validate image format
     */
    public boolean isValidImageFormat(byte[] imageBytes) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            return bufferedImage != null;
        } catch (IOException e) {
            log.error("Error validating image format", e);
            return false;
        }
    }

    /**
     * Extract format from image bytes
     */
    public String getImageFormat(byte[] imageBytes) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);

        // Read the first few bytes to determine format
        byte[] header = new byte[10];
        inputStream.read(header);

        // Check for common image formats
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8) {
            return "jpg";
        } else if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
            return "png";
        } else if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46) {
            return "gif";
        } else if (header[0] == 0x42 && header[1] == 0x4D) {
            return "bmp";
        }

        return "unknown";
    }

    /**
     * Compress image quality
     */
    public byte[] compressImage(byte[] originalImage, float quality, String format) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalImage);
        BufferedImage bufferedImage = ImageIO.read(inputStream);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        if ("jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format)) {
            // For JPEG, we can set compression quality
            // This is a simplified version - in practice, you'd use ImageWriter
            ImageIO.write(bufferedImage, "jpg", outputStream);
        } else {
            ImageIO.write(bufferedImage, format, outputStream);
        }

        return outputStream.toByteArray();
    }

    /**
     * Create image variants (thumbnail, medium, large)
     */
    public ImageVariants createImageVariants(byte[] originalImage, String format) throws IOException {
        byte[] thumbnail = resizeImage(originalImage, 150, 150, format);
        byte[] medium = resizeImage(originalImage, 400, 400, format);
        byte[] large = resizeImage(originalImage, 800, 800, format);

        return new ImageVariants(thumbnail, medium, large, originalImage);
    }

    /**
     * Inner class to hold image variants
     */
    public static class ImageVariants {
        private final byte[] thumbnail;
        private final byte[] medium;
        private final byte[] large;
        private final byte[] original;

        public ImageVariants(byte[] thumbnail, byte[] medium, byte[] large, byte[] original) {
            this.thumbnail = thumbnail;
            this.medium = medium;
            this.large = large;
            this.original = original;
        }

        public byte[] getThumbnail() { return thumbnail; }
        public byte[] getMedium() { return medium; }
        public byte[] getLarge() { return large; }
        public byte[] getOriginal() { return original; }
    }

    /**
     * Clean up image metadata (EXIF data)
     */
    public byte[] stripMetadata(byte[] originalImage, String format) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalImage);
        BufferedImage bufferedImage = ImageIO.read(inputStream);

        // Create new image without metadata
        BufferedImage cleanImage = new BufferedImage(
                bufferedImage.getWidth(),
                bufferedImage.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = cleanImage.createGraphics();
        graphics.drawImage(bufferedImage, 0, 0, null);
        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(cleanImage, format, outputStream);
        return outputStream.toByteArray();
    }
}