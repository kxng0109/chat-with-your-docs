package io.github.kxng0109.chatwithdocs.service;

import io.github.kxng0109.chatwithdocs.exception.DocumentProcessingException;
import io.github.kxng0109.chatwithdocs.model.DocumentUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    /**
     * Processes the given document file by validating, reading, splitting, enriching,
     * and storing it, and returns a response containing processing details.
     *
     * @param file the input file to be processed, represented as a {@code MultipartFile}.
     * @return a {@code DocumentUploadResponse} containing details about the processed file,
     * including the number of chunks created and stored, the processing time,
     * and a message indicating success.
     */
    public DocumentUploadResponse processDocument(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        log.info("Starting document processing for file: {}", file.getOriginalFilename());

        validateFile(file);

        List<Document> documents = readDocument(file);
        log.info("Read {} documents from file: {}",
                 documents.size(),
                 file.getOriginalFilename()
        );

        List<Document> chunks = splitDocuments(documents);
        log.info("Document was split into {} chunks", chunks.size());

        enrichChunksWithMetadata(chunks, file.getName());

        storeChunks(chunks);
        log.info("Stored {} chunks in vector database", chunks.size());

        long processingTime = System.currentTimeMillis() - startTime;
        return DocumentUploadResponse.builder()
                                     .processingTimeMs(processingTime)
                                     .chunksStored(chunks.size())
                                     .filename(file.getName())
                                     .chunksCreated(chunks.size())
                                     .message("Documents processed successfully")
                                     .build();
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
    private void enrichChunksWithMetadata(List<Document> chunks, String fileName) {
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
            metadata.put("filename", fileName);
            metadata.put("chunk_index", i);
            metadata.put("total_chunks", chunks.size());
            metadata.put("upload_timestamp", System.currentTimeMillis());

            chunk.getMetadata().putAll(metadata);
        }

        log.debug("Successfully enriched {} chunks with metadata for file: {}",
                  chunks.size(),
                  fileName
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
