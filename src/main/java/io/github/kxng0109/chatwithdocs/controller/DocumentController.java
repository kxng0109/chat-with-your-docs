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
     * Handles the upload of a document file, processes it, and returns a response containing
     * details of the upload and processing results.
     *
     * @param file the document file to be uploaded, represented as a {@code MultipartFile}
     *             containing the file's binary content.
     * @return a {@code ResponseEntity} containing a {@code DocumentUploadResponse} object
     * with detailed information about the uploaded and processed document, including
     * the file name, number of chunks created and stored, processing time, and a
     * success message.
     */
    @PostMapping(value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam MultipartFile file) {
        log.info("Received document for upload: {}", file.getOriginalFilename());

        DocumentUploadResponse response = documentService.processDocument(file);
        log.info("Document processed successfully: {}", response);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
