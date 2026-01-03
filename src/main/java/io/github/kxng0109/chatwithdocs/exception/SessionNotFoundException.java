package io.github.kxng0109.chatwithdocs.exception;

/**
 * Exception thrown when a chat session is not found.
 */
public class SessionNotFoundException extends RuntimeException {
    /**
     * Constructs a new SessionNotFoundException with the specified session ID.
     *
     * @param sessionId The ID of the session that was not found.
     */
    public SessionNotFoundException(String sessionId) {
        super("Session with ID " + sessionId + " not found.");
    }

    /**
     * Constructs a new SessionNotFoundException with the specified detail message and cause.
     *
     * @param message The detail message.
     * @param cause   The cause of the exception.
     */
    public SessionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
