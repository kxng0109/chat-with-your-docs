package io.github.kxng0109.chatwithdocs.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Represents a chat request containing a question and an optional parameter for specifying
 * the number of relevant sources (topK) to be used when answering the question.
 * <p>
 * The chat request serves as input for processing by the chat service, which generates responses
 * based on the provided question and context derived from the relevant sources.
 * <p>
 * Validation is applied to ensure that:
 * - The question field is not blank.
 * - The topK parameter, when provided, is at least 1 and does not exceed 20.
 * <p>
 * If the topK parameter is not explicitly specified, it defaults to 5.
 * <p>
 * Fields:
 * - question: The user's query that the chat service is expected to answer.
 * - topK: An optional parameter specifying the number of top relevant sources to consider.
 */
public record ChatRequest(
        @NotBlank(message = "Question can not be empty")
        String question,

        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 20, message = "topK cannot exceed 20")
        Integer topK
) {
    public ChatRequest {
        if (topK == null) topK = 5;
    }
}
