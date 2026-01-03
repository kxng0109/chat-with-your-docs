package io.github.kxng0109.chatwithdocs.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Represents a chat request containing a question and session context.
 * The sessionId links the question to a specific document context.
 *
 * <p>Changes from original: </p>
 * <ul>
 *   <li>Added sessionId to scope the query to a specific session's documents</li>
 *   <li>Added includeHistory flag to control conversation context</li>
 *   <li>Added historyLimit to control how much history to include</li>
 * </ul>
 *
 * @param sessionId      The session to query (required for session-scoped queries)
 * @param question       The user's query (required)
 * @param topK           Number of relevant chunks to retrieve (1-20, default 5)
 * @param includeHistory Whether to include conversation history in context
 * @param historyLimit   Maximum number of previous messages to include (default 10)
 */
public record ChatRequest(
        @NotBlank(message = "Session ID is required")
        String sessionId,

        @NotBlank(message = "Question can not be empty")
        String question,

        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 20, message = "topK cannot exceed 20")
        Integer topK,

        Boolean includeHistory,

        @Min(value = 1, message = "historyLimit must be at least 1")
        @Max(value = 50, message = "historyLimit cannot exceed 50")
        Integer historyLimit
) {
    public ChatRequest {
        if (topK == null) topK = 5;
        if (includeHistory == null) includeHistory = true;
        if (historyLimit == null) historyLimit = 10;
    }
}
