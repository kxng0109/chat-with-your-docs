package io.github.kxng0109.chatwithdocs.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler is a centralized exception handling component
 * using @RestControllerAdvice to catch and process exceptions thrown
 * by controllers across the application.
 * <p>
 * This class defines specific handling for exceptions including validation errors,
 * custom document processing errors, file upload size limit breaches, and generic
 * unexpected errors, providing consistent response structures to the client.
 * <p>
 * Key exception handling methods:
 * 1. handleValidationExceptions: Processes MethodArgumentNotValidException errors
 * to extract and return detailed field error information for client inputs.
 * 2. handleDocumentProcessingException: Handles the application-specific
 * DocumentProcessingException to provide customized error messages related
 * to document processing failures.
 * 3. handleMaxUploadSizeExceededException: Handles MaxUploadSizeExceededException
 * to respond with appropriate messages when file size exceeds defined limits.
 * 4. handleGenericException: A catch-all handler for other unhandled general exceptions,
 * returns a generic internal server error response.
 * <p>
 * Each handler constructs a detailed error response with information such as
 * timestamp, HTTP status, error type, and a user-friendly message, ensuring
 * consistent and informative error responses to the client.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException e) {
        log.error("Validation error: {}", e.getMessage(), e);

        Map<String, Object> fieldErrors = new HashMap<>();

        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMsg = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMsg);
        });

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Validation Failed");
        errorResponse.put("message", "Invalid input data");
        errorResponse.put("fieldErrors", fieldErrors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentProcessingException(DocumentProcessingException e) {
        log.error("Document processing error: {}", e.getMessage(), e);

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                e.getMessage()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("Max upload size exceeded: {}", e.getMessage(), e);

        return buildErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                e.getMessage()
        );
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSessionNotFoundException(SessionNotFoundException e) {
        log.error("Session not found: {}", e.getMessage(), e);

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                e.getMessage()
        );
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(NullPointerException e) {
        log.error(e.getMessage());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                e.getMessage()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error(e.getMessage());

        return buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                e.getMessage()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.error(e.getMessage());

        return buildErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                e.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                e.getMessage()
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
