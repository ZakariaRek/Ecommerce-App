package com.Ecommerce.Cart.Service.Exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return createJsonResponse(
                HttpStatus.NOT_FOUND,
                ApiResponse.error(ex.getMessage())
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequestException(BadRequestException ex) {
        return createJsonResponse(
                HttpStatus.BAD_REQUEST,
                ApiResponse.error(ex.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return createJsonResponse(
                HttpStatus.BAD_REQUEST,
                ApiResponse.error("Validation failed", errors)
        );
    }

    /**
     * Handle JSON parsing errors with a clearer error message
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleJsonParseException(HttpMessageNotReadableException ex) {
        String message = "Invalid JSON format. Please check your request payload.";

        // If it's a UTF-8 encoding issue, provide more specific help
        if (ex.getCause() instanceof JsonParseException && ex.getMessage().contains("Invalid UTF-8")) {
            message = "Invalid character encoding detected. Please ensure your JSON uses standard UTF-8 characters.";
        }
        // If it's an unrecognized property issue
        else if (ex.getCause() instanceof UnrecognizedPropertyException) {
            UnrecognizedPropertyException upe = (UnrecognizedPropertyException) ex.getCause();
            message = "Unrecognized field '" + upe.getPropertyName() + "' in JSON. Please check your request payload.";
        }
        // If it's an invalid format issue
        else if (ex.getCause() instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) ex.getCause();
            message = "Invalid format for field '" + ife.getPath().get(0).getFieldName() + "'. Expected "
                    + ife.getTargetType().getSimpleName() + " but got '" + ife.getValue() + "'.";
        }

        return createJsonResponse(
                HttpStatus.BAD_REQUEST,
                ApiResponse.error(message)
        );
    }

    /**
     * Handle Redis serialization issues
     */
    @ExceptionHandler(SerializationException.class)
    public ResponseEntity<ApiResponse<Object>> handleSerializationException(SerializationException ex) {
        String message = "Data serialization error.";

        if (ex.getCause() instanceof UnrecognizedPropertyException) {
            UnrecognizedPropertyException upe = (UnrecognizedPropertyException) ex.getCause();
            message = "Cache serialization issue with property '" + upe.getPropertyName() + "'. Please clear the cache and try again.";
        } else if (ex.getCause() instanceof JsonMappingException) {
            message = "Cache data mapping error. Please clear the cache and try again.";
        }

        return createJsonResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiResponse.error(message)
        );
    }

    /**
     * Handle media type issues - ensures we always return JSON even when Accept header is not compatible
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResponse<Object>> handleMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException ex) {
        return createJsonResponse(
                HttpStatus.NOT_ACCEPTABLE,
                ApiResponse.error("The requested media type is not supported. This API returns JSON.")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex, WebRequest request) {
        // Log the detailed exception for debugging
        ex.printStackTrace();

        return createJsonResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiResponse.error("An unexpected error occurred: " + ex.getMessage())
        );
    }

    /**
     * Helper method to create response with JSON content type
     */
    private <T> ResponseEntity<T> createJsonResponse(HttpStatus status, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(body, headers, status);
    }

    // Custom ApiResponse class
    public static class ApiResponse<T> {
        private final boolean success;
        private final String message;
        private final T data;

        private ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public T getData() {
            return data;
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }

        public static <T> ApiResponse<T> error(String message, T data) {
            return new ApiResponse<>(false, message, data);
        }
    }
}