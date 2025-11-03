package io.github.kxng0109.chatwithdocs.exception;

/**
 * DocumentProcessingException is a custom unchecked exception that is used to
 * indicate errors during document processing within the application.
 * This exception can be thrown with an error message or combined with a cause
 * for enhanced error context.
 *
 * The exception is handled specifically by the application to provide appropriate
 * error responses, allowing for clean separation of processing errors and
 * custom error handling through mechanisms such as global exception handlers.
 */
public class DocumentProcessingException extends RuntimeException {
    public DocumentProcessingException(String message) {
        super(message);
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
