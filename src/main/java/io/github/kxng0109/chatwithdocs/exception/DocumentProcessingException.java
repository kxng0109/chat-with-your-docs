package io.github.kxng0109.chatwithdocs.exception;

public class DocumentProcessingException extends RuntimeException {
    public DocumentProcessingException(String message) {
        super(message);
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
