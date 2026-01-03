package io.github.kxng0109.chatwithdocs.model;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Summary information about an uploaded document.
 *
 * @param id          Unique identifier of the document
 * @param fileName    Original filename of the uploaded document
 * @param contentType MIME type of the document
 * @param fileSize    Size of the document in bytes
 * @param chunkCount  Number of chunks the document was split into for processing
 * @param status      Current processing status of the document
 * @param uploadedAt  Timestamp when the document was uploaded
 */
@Builder
public record DocumentSummary(
        Long id,
        String fileName,
        String contentType,
        Long fileSize,
        Integer chunkCount,
        String status,
        LocalDateTime uploadedAt
) {
}
