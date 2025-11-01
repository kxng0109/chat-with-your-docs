package io.github.kxng0109.chatwithdocs.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(
        String answer,

        List<String> sources,

        String question,

        Long processingTimeMs
) {
}
