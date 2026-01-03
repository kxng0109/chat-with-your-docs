package io.github.kxng0109.chatwithdocs.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * Response model for multi-document upload results.
 *
 * @param sessionId             Unique identifier for the upload session.
 * @param totalFiles            Total number of files attempted to upload.
 * @param successfulUploads     Number of files successfully uploaded.
 * @param failedUploads         Number of files that failed to upload.
 * @param totalChunksCreated    Total number of chunks created across all documents.
 * @param totalProcessingTimeMs Total time taken to process the uploads in milliseconds.
 * @param documents             List of individual document upload results.
 * @param message               Additional message or status information.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MultiDocumentUploadResponse(
        String sessionId,
        int totalFiles,
        int successfulUploads,
        int failedUploads,
        int totalChunksCreated,
        Long totalProcessingTimeMs,
        List<DocumentResult> documents,
        String message
) {
}
