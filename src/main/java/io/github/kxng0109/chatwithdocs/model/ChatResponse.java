package io.github.kxng0109.chatwithdocs.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * Response model for chat interactions.
 *
 * @param sessionId        Unique identifier for the chat session.
 * @param answer           The generated answer from the chat model.
 * @param sources          List of source identifiers used to generate the answer.
 * @param sourceDocuments  List of source document excerpts or references.
 * @param question         The original question asked by the user.
 * @param processingTimeMs Time taken to process the request in milliseconds.
 * @param messageId        Unique identifier for the chat message.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(
        String sessionId,

        String answer,

        List<String> sources,

        List<String> sourceDocuments,

        String question,

        Long processingTimeMs,

        Long messageId
) {
}
