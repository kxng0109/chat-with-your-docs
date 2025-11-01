package io.github.kxng0109.chatwithdocs.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentUploadResponse(
        String filename,

        int chunksCreated,

        int chunksStored,

        String message,

        Long processingTimeMs
) {
}
