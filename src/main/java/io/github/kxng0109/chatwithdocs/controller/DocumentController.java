package io.github.kxng0109.chatwithdocs.controller;

import io.github.kxng0109.chatwithdocs.model.DocumentUploadResponse;
import io.github.kxng0109.chatwithdocs.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * A REST controller that provides endpoints for managing document-related operations.
 * This controller handles functionalities such as uploading and processing documents.
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    /**
     * Handles the upload of a document file, processes it, and returns a response with processing details.
     * Validates the file input and processes it to generate a structured response.
     *
     * @param file the file to be uploaded, represented as a {@code MultipartFile}; may be null or empty
     *             to allow customized exception handling.
     * @return a {@code ResponseEntity} containing a {@code DocumentUploadResponse} object with details
     *         about the uploaded document or an error message if the validation fails.
     */
    @PostMapping(value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam(required = false) MultipartFile file /*Making it not required so that I can throw an exception of my own instead of Spring's 500*/) {
        if (file == null || file.isEmpty()) {
            DocumentUploadResponse response = DocumentUploadResponse.builder()
                                                                    .message("File required.")
                                                                    .build();
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        log.info("Received document for upload: {}", file.getOriginalFilename());

        DocumentUploadResponse response = documentService.processDocument(file);
        log.info("Document processed successfully: {}", response);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
