package io.github.kxng0109.chatwithdocs.model;

import lombok.Builder;

/**
 * Result of processing an individual document upload.
 *
 * @param filename         Name of the uploaded file
 * @param success          Whether the upload and processing were successful
 * @param chunksCreated    Number of chunks created from the document
 * @param processingTimeMs Time taken to process the document in milliseconds
 * @param errorMessage     Error message if the processing failed, null otherwise
 */

@Builder
public record DocumentResult(
        String filename,
        boolean success,
        Integer chunksCreated,
        Long processingTimeMs,
        String errorMessage
) {
}
