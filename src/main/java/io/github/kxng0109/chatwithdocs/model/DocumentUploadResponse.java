package io.github.kxng0109.chatwithdocs.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * Represents the response after processing a document upload.
 * This record encapsulates information regarding the uploaded document,
 * including metadata about chunks and processing details.
 * <p>
 * Fields:
 * - filename: The name of the file that was uploaded.
 * - chunksCreated: The total number of chunks that were created as part of the upload process.
 * - chunksStored: The total number of chunks successfully stored in the system.
 * - message: A descriptive message providing additional information or status of the upload.
 * - processingTimeMs: The time, in milliseconds, taken to process the upload operation.
 */
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
