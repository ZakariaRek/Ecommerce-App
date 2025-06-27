//package com.Ecommerce.Product_Service.Config;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.multipart.MaxUploadSizeExceededException;
//import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
//
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//
//@ControllerAdvice
//@Slf4j
//public class FileExceptionHandler extends ResponseEntityExceptionHandler {
//
//    /**
//     * Handle file size exceeded exception
//     */
//    @ExceptionHandler(MaxUploadSizeExceededException.class)
//    public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
//        log.error("File size exceeded", ex);
//
//        Map<String, Object> error = createErrorResponse(
//                "FILE_SIZE_EXCEEDED",
//                "File size exceeds the maximum allowed size",
//                HttpStatus.PAYLOAD_TOO_LARGE
//        );
//
//        return new ResponseEntity<>(error, HttpStatus.PAYLOAD_TOO_LARGE);
//    }
//
//    /**
//     * Handle IO exceptions during file operations
//     */
//    @ExceptionHandler(IOException.class)
//    public ResponseEntity<Map<String, Object>> handleIOException(IOException ex) {
//        log.error("File I/O error", ex);
//
//        Map<String, Object> error = createErrorResponse(
//                "FILE_IO_ERROR",
//                "Error processing file: " + ex.getMessage(),
//                HttpStatus.INTERNAL_SERVER_ERROR
//        );
//
//        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//
//    /**
//     * Handle general runtime exceptions related to file storage
//     */
//    @ExceptionHandler(RuntimeException.class)
//    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
//        log.error("Runtime error during file operation", ex);
//
//        HttpStatus status = determineStatusFromMessage(ex.getMessage());
//
//        Map<String, Object> error = createErrorResponse(
//                "FILE_OPERATION_ERROR",
//                ex.getMessage(),
//                status
//        );
//
//        return new ResponseEntity<>(error, status);
//    }
//
//    /**
//     * Create standardized error response
//     */
//    private Map<String, Object> createErrorResponse(String errorCode, String message, HttpStatus status) {
//        Map<String, Object> error = new HashMap<>();
//        error.put("timestamp", LocalDateTime.now());
//        error.put("status", status.value());
//        error.put("error", status.getReasonPhrase());
//        error.put("errorCode", errorCode);
//        error.put("message", message);
//        error.put("path", "file-storage");
//
//        return error;
//    }
//
//    /**
//     * Determine HTTP status based on exception message
//     */
//    private HttpStatus determineStatusFromMessage(String message) {
//        if (message == null) {
//            return HttpStatus.INTERNAL_SERVER_ERROR;
//        }
//
//        String lowerMessage = message.toLowerCase();
//
//        if (lowerMessage.contains("file size") || lowerMessage.contains("too large")) {
//            return HttpStatus.PAYLOAD_TOO_LARGE;
//        } else if (lowerMessage.contains("invalid") || lowerMessage.contains("not allowed")) {
//            return HttpStatus.BAD_REQUEST;
//        } else if (lowerMessage.contains("not found")) {
//            return HttpStatus.NOT_FOUND;
//        } else if (lowerMessage.contains("permission") || lowerMessage.contains("access denied")) {
//            return HttpStatus.FORBIDDEN;
//        }
//
//        return HttpStatus.INTERNAL_SERVER_ERROR;
//    }
//}