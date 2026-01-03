package io.github.kxng0109.chatwithdocs.controller;

import io.github.kxng0109.chatwithdocs.model.DocumentUploadResponse;
import io.github.kxng0109.chatwithdocs.model.MultiDocumentUploadResponse;
import io.github.kxng0109.chatwithdocs.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * A REST controller that provides endpoints for managing document-related operations.
 * This controller handles functionalities such as uploading and processing documents.
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions/{sessionId}/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    /**
     * Handles the upload of a document file, processes it, and returns a response with processing details.
     * Validates the file input and processes it to generate a structured response.
     *
     * @param file      the file to be uploaded, represented as a {@code MultipartFile}; may be null or empty
     *                  to allow customized exception handling.
     * @param sessionId UUID of the current chat session
     * @return a {@code ResponseEntity} containing a {@code DocumentUploadResponse} object with details
     * about the uploaded document or an error message if the validation fails.
     */
    @PostMapping(
            value = "/single",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DocumentUploadResponse> uploadSingleDocument(
            @RequestParam(required = false) MultipartFile file /*Making it not required so that I can throw an exception of my own instead of Spring's 500*/,
            @PathVariable String sessionId
    ) {
        if (file == null || file.isEmpty()) {
            DocumentUploadResponse response = DocumentUploadResponse.builder()
                                                                    .sessionId(sessionId)
                                                                    .message("File required.")
                                                                    .build();
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        log.info("Received document for upload: {}", file.getOriginalFilename());

        DocumentUploadResponse response = documentService.processDocument(file, sessionId);
        log.info("Document processed successfully: {}", response);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Uploads one or more documents to a session.
     * Documents are processed, chunked, embedded, and stored in the vector database.
     * Each document is processed independently, failures don't affect other documents.
     *
     * @param sessionId The session to upload documents to
     * @param files     Array of files to upload (use 'files' as the form field name)
     * @return Response with status for each document
     */
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<MultiDocumentUploadResponse> uploadDocuments(
            @PathVariable String sessionId,
            @RequestParam(value = "files", required = false) MultipartFile[] files
    ) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(
                    MultiDocumentUploadResponse.builder()
                                               .sessionId(sessionId)
                                               .message("No files provided")
                                               .totalFiles(0)
                                               .build()
            );
        }

        log.info("Received {} files for session:  {}", files.length, sessionId);

        MultiDocumentUploadResponse response = documentService.processDocuments(sessionId, files);

        HttpStatus status = response.failedUploads() == 0
                ? HttpStatus.CREATED
                : HttpStatus.MULTI_STATUS;  // 207 if some failed

        return new ResponseEntity<>(response, status);
    }

    /**
     * Delete all documents in the session
     *
     * @param sessionId UUID of the chat session
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteAllDocuments(@PathVariable String sessionId) {
        log.info("Deleting documents");
        documentService.deleteAllDocuments(sessionId);
        return ResponseEntity.noContent().build();
    }
}
