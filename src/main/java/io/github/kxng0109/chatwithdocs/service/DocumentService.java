package io.github.kxng0109.chatwithdocs.service;

import io.github.kxng0109.chatwithdocs.entity.ChatSession;
import io.github.kxng0109.chatwithdocs.entity.SessionDocument;
import io.github.kxng0109.chatwithdocs.exception.DocumentProcessingException;
import io.github.kxng0109.chatwithdocs.model.DocumentResult;
import io.github.kxng0109.chatwithdocs.model.DocumentUploadResponse;
import io.github.kxng0109.chatwithdocs.model.MultiDocumentUploadResponse;
import io.github.kxng0109.chatwithdocs.repository.ChatSessionRepository;
import io.github.kxng0109.chatwithdocs.repository.SessionDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for processing and managing document files. This class provides
 * methods to validate, read, split, enrich, and store documents. It facilitates
 * seamless handling of document chunks, metadata enrichment, and integration with
 * a vector store for embedding-based storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    private final VectorStore vectorStore;
    private final SessionService sessionService;
    private final SessionDocumentRepository documentRepository;
    private final ChatSessionRepository chatSessionRepository;

    /**
     * Processes multiple documents and adds them to a session.
     * Each document is processed independently - failures don't affect other documents.
     *
     * @param sessionId the id (UUID) of the session to add the documents to
     * @param files     array of files to process
     * @return {@code MultiDocumentUploadResponse} containing status for each document
     * @throws io.github.kxng0109.chatwithdocs.exception.SessionNotFoundException if session does not exist
     */
    @Transactional
    public MultiDocumentUploadResponse processDocuments(String sessionId, MultipartFile[] files) {
        long startTime = System.currentTimeMillis();
        log.info("Starting processing of {} documents for session: {}", files.length, sessionId);

        ChatSession session = sessionService.getSessionEntity(sessionId);

        List<DocumentResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        int totalChunksCreated = 0;

        for (MultipartFile file : files) {
            try {
                DocumentUploadResponse result = processDocument(file, session.getSessionId());
                DocumentResult successfulFile = DocumentResult.builder()
                                                              .filename(file.getOriginalFilename())
                                                              .success(true)
                                                              .chunksCreated(result.chunksCreated())
                                                              .processingTimeMs(result.processingTimeMs())
                                                              .build();

                results.add(successfulFile);
                successCount++;
                totalChunksCreated += result.chunksCreated();
            } catch (Exception e) {
                log.error(
                        "Failed to process document {}: {}",
                        file.getOriginalFilename(),
                        e.getMessage(),
                        e
                );
                DocumentResult failedFile = DocumentResult.builder()
                                                          .filename(file.getOriginalFilename())
                                                          .success(false)
                                                          .chunksCreated(null)
                                                          .errorMessage(e.getMessage())
                                                          .build();
                results.add(failedFile);
                failureCount++;
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        String message = failureCount == 0
                ? "All documents have been processed successfully"
                : String.format("%d of %d documents processed successfully", successCount, files.length);
        return MultiDocumentUploadResponse.builder()
                                          .sessionId(sessionId)
                                          .totalFiles(files.length)
                                          .successfulUploads(successCount)
                                          .failedUploads(failureCount)
                                          .totalChunksCreated(totalChunksCreated)
                                          .totalProcessingTimeMs(totalTime)
                                          .documents(results)
                                          .message(message)
                                          .build();
    }

    /**
     * Processes the given document file by validating, reading, splitting, enriching,
     * and storing it, and returns a response containing processing details.
     *
     * @param file      the input file to be processed, represented as a {@code MultipartFile}.
     * @param sessionId the chat session to which the document belongs.
     * @return a {@code DocumentUploadResponse} containing details about the processed file,
     * including the number of chunks created and stored, the processing time,
     * and a message indicating success.
     */
    public DocumentUploadResponse processDocument(MultipartFile file, String sessionId) {
        long startTime = System.currentTimeMillis();
        String filename = file.getOriginalFilename();
        log.info("Starting documentRecord processing for file: {} for session with id: {}", filename,
                 sessionId
        );

        ChatSession session = sessionService.getSessionEntity(sessionId);

        SessionDocument documentRecord = SessionDocument.builder()
                                                        .session(session)
                                                        .originalFilename(filename)
                                                        .contentType(file.getContentType())
                                                        .fileSize(file.getSize())
                                                        .status(SessionDocument.DocumentStatus.PROCESSING)
                                                        .build();
        documentRecord = documentRepository.save(documentRecord);

        try {
            validateFile(file);

            List<Document> documents = readDocument(file);
            log.info("Read {} documents from file: {}",
                     documents.size(),
                     file.getOriginalFilename()
            );

            List<Document> chunks = splitDocuments(documents);
            log.debug("Document was split into {} chunks", chunks.size());

            enrichChunksWithMetadata(chunks, filename, session.getSessionId());

            storeChunks(chunks);
            log.info("Stored {} chunks in vector database", chunks.size());

            long processingTime = System.currentTimeMillis() - startTime;

            documentRecord.setChunkCount(chunks.size());
            documentRecord.setStatus(SessionDocument.DocumentStatus.COMPLETED);
            documentRecord.setProcessingTimeMs(processingTime);
            documentRepository.save(documentRecord);

            return DocumentUploadResponse.builder()
                                         .processingTimeMs(processingTime)
                                         .chunksStored(chunks.size())
                                         .filename(filename)
                                         .chunksCreated(chunks.size())
                                         .message("Documents processed successfully")
                                         .build();
        } catch (Exception e) {
            documentRecord.setStatus(SessionDocument.DocumentStatus.FAILED);
            documentRecord.setErrorMessage(e.getMessage());
            documentRepository.save(documentRecord);
            throw new DocumentProcessingException("Failed to process document: " + filename, e);
        }
    }

    /**
     * Deletes all documents in the session
     *
     * @param sessionId the UUID of the chat session
     * @throws io.github.kxng0109.chatwithdocs.exception.SessionNotFoundException if {@code sessionId} is invalid
     */
    public void deleteAllDocuments(String sessionId) {
        ChatSession session = sessionService.getSessionEntity(sessionId);
        int documentCount = session.getDocumentCount();

        try {
            Filter.Expression filterExpression = new FilterExpressionBuilder()
                    .eq("sessionId", sessionId)
                    .build();

            vectorStore.delete(filterExpression);
            log.info("Deleted vectors for session: {}", sessionId);

            session.getDocuments().clear();
            chatSessionRepository.save(session);

            log.info("Deleted {} documents from session with ID: {}",
                     documentCount,
                     sessionId
            );
        } catch (Exception e) {
            log.error("Failed to delete vectors for session {}: {}", sessionId, e.getMessage(), e);
            throw new DocumentProcessingException("Failed to delete documents from session: " + sessionId, e);
        }
    }

    /**
     * Validates the given file to ensure it is not null or empty and has a valid name.
     *
     * @param file the MultipartFile to be validated; must not be null or empty and must have a valid filename
     * @throws DocumentProcessingException if the file is null, empty, or has an invalid filename
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentProcessingException("File is empty or null.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new DocumentProcessingException("File name is invalid.");
        }

        log.debug("File validation passed: {}", fileName);
    }

    /**
     * Reads and processes the specified multipart file to extract its content as a list of Documents.
     * Depending on the file type, it either uses a PDF reader for PDF files or a Tika reader for other file types.
     *
     * @param file the multipart file to be read and processed; must not be null or empty
     * @return a list of Document objects representing the content of the file
     * @throws DocumentProcessingException if an error occurs while reading or processing the file
     */
    private List<Document> readDocument(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();

            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };

            if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
                log.debug("PDF file found using PDF reader for: {}", fileName);
                PagePdfDocumentReader pdfDocumentReader = new PagePdfDocumentReader(resource);
                return pdfDocumentReader.get();
            } else {
                log.debug("Using Tika reader for: {}", fileName);
                TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);
                return tikaDocumentReader.get();
            }
        } catch (IOException e) {
            log.error("Error occurred reading document: {}", e.getMessage(), e);
            throw new DocumentProcessingException("Error occurred reading document.", e);
        }
    }

    /**
     * Splits a list of documents into smaller, manageable chunks based on specific token-based rules.
     * The splitting process ensures optimal size for further processing or embedding while preserving
     * necessary formatting and avoiding excessively small chunks.
     *
     * @param documents the list of documents to be split; must not be null
     * @return a list of smaller documents obtained by splitting the provided documents
     */
    private List<Document> splitDocuments(List<Document> documents) {
        /**
         * The default config can be found here:
         * https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_tokentextsplitter
         * But basically, here is it:
         * - defaultChunkSize: 800 tokens per chunk (adjust based on your needs)
         * - minChunkSizeChars: 350 characters minimum
         * - minChunkLengthToEmbed: 5 characters (skip tiny chunks)
         * - maxNumChunks: 10000 (safety limit)
         * - keepSeparator: true (preserve paragraph breaks, maintain formatting)
         */
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

    /**
     * Enriches a list of document chunks with metadata information, including filename,
     * chunk index, total number of chunks, and upload timestamp.
     *
     * @param chunks   the list of document chunks that need to be enriched with metadata
     * @param fileName the name of the file associated with the chunks
     */
    private void enrichChunksWithMetadata(List<Document> chunks, String fileName, String sessionId) {
        Instant uploadTimestamp = Instant.now();

        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
            metadata.put("sessionId", sessionId);
            metadata.put("filename", fileName);
            metadata.put("chunk_index", i);
            metadata.put("total_chunks", chunks.size());
            metadata.put("upload_timestamp", uploadTimestamp.toString());

            chunk.getMetadata().putAll(metadata);
        }

        log.debug("Successfully enriched {} chunks with metadata for file: {} with sessionId: {}",
                  chunks.size(),
                  fileName,
                  sessionId
        );
    }

    /**
     * Stores a list of document chunks into the vector store. Each chunk is processed
     * with associated embeddings to enable fast retrieval and further analysis.
     *
     * @param chunks the list of document chunks to be stored; must not be null
     * @throws DocumentProcessingException if an error occurs while storing the chunks
     */
    private void storeChunks(List<Document> chunks) {
        try {
            vectorStore.add(chunks);
            log.info("Successfully stored all chunks with embeddings");
        } catch (Exception e) {
            log.error("Failed to store document chunks: {}", e.getMessage(), e);
            throw new DocumentProcessingException("Failed to store document chunks.", e);
        }
    }
}
