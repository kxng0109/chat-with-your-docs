package io.github.kxng0109.chatwithdocs.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for chat session details.
 *
 * @param sessionId     Unique identifier (UUID) of the chat session
 * @param name          Human-readable name of the session
 * @param description   Description of the session's purpose
 * @param documentCount Number of documents associated with the session
 * @param messageCount  Number of messages exchanged in the session
 * @param documents     List of document summaries associated with the session
 * @param createdAt     Timestamp when the session was created
 * @param updatedAt     Timestamp when the session was last updated
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record SessionResponse(
        String sessionId,
        String name,
        String description,
        int documentCount,
        int messageCount,
        List<DocumentSummary> documents,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
