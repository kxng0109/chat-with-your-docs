package io.github.kxng0109.chatwithdocs.controller;

import io.github.kxng0109.chatwithdocs.TestUtils;
import io.github.kxng0109.chatwithdocs.model.DocumentResult;
import io.github.kxng0109.chatwithdocs.model.DocumentUploadResponse;
import io.github.kxng0109.chatwithdocs.model.MultiDocumentUploadResponse;
import io.github.kxng0109.chatwithdocs.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
public class DocumentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @Test
    void uploadSingleDocument_shouldReturn201Created_whenFileIsValid() throws Exception {
        String testSessionId = UUID.randomUUID().toString();
        MockMultipartFile validFile = TestUtils.createValidPdfMockFile();

        DocumentUploadResponse sampleResponse = DocumentUploadResponse.builder()
                                                                      .sessionId(testSessionId)
                                                                      .chunksStored(10)
                                                                      .chunksCreated(10)
                                                                      .processingTimeMs(1500L)
                                                                      .message("Document processed successfully")
                                                                      .filename(validFile.getOriginalFilename())
                                                                      .build();

        when(documentService.processDocument(any(), eq(testSessionId)))
                .thenReturn(sampleResponse);

        mockMvc.perform(multipart("/api/sessions/{sessionId}/documents/single", testSessionId)
                                .file(validFile))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.sessionId").value(testSessionId))
               .andExpect(jsonPath("$.filename").value(validFile.getOriginalFilename()))
               .andExpect(jsonPath("$.chunksStored").value(sampleResponse.chunksStored()))
               .andExpect(jsonPath("$.chunksCreated").value(sampleResponse.chunksCreated()))
               .andExpect(jsonPath("$.message").value(sampleResponse.message()));
    }

    @Test
    void uploadSingleDocument_shouldReturn400_whenFileIsMissing() throws Exception {
        String testSessionId = UUID.randomUUID().toString();

        mockMvc.perform(multipart("/api/sessions/{sessionId}/documents/single", testSessionId))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").value("File required."));
    }

    @Test
    void uploadMultipleDocuments_shouldReturn201Created_whenAllFilesValid() throws Exception {
        String testSessionId = UUID.randomUUID().toString();
        MockMultipartFile file1 = TestUtils.createValidPdfMockFile();
        MockMultipartFile file2 = TestUtils.createValidPdfMockFile();

        List<DocumentResult> documentResults = List.of(
                DocumentResult.builder()
                              .filename(file1.getOriginalFilename())
                              .success(true)
                              .chunksCreated(10)
                              .processingTimeMs(1000L)
                              .build(),
                DocumentResult.builder()
                              .filename(file2.getOriginalFilename())
                              .success(true)
                              .chunksCreated(8)
                              .processingTimeMs(800L)
                              .build()
        );

        MultiDocumentUploadResponse sampleResponse = MultiDocumentUploadResponse.builder()
                                                                                .sessionId(testSessionId)
                                                                                .totalFiles(2)
                                                                                .successfulUploads(2)
                                                                                .failedUploads(0)
                                                                                .totalChunksCreated(18)
                                                                                .totalProcessingTimeMs(1800L)
                                                                                .documents(documentResults)
                                                                                .message(
                                                                                        "All documents have been processed successfully")
                                                                                .build();

        when(documentService.processDocuments(eq(testSessionId), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(multipart("/api/sessions/{sessionId}/documents", testSessionId)
                                .file("files", file1.getBytes())
                                .file("files", file2.getBytes()))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.sessionId").value(testSessionId))
               .andExpect(jsonPath("$.totalFiles").value(2))
               .andExpect(jsonPath("$.successfulUploads").value(2))
               .andExpect(jsonPath("$.failedUploads").value(0))
               .andExpect(jsonPath("$.totalChunksCreated").value(18));
    }

    @Test
    void uploadMultipleDocuments_shouldReturn207MultiStatus_whenSomeFilesFail() throws Exception {
        String testSessionId = UUID.randomUUID().toString();
        MockMultipartFile file1 = TestUtils.createValidPdfMockFile();
        MockMultipartFile file2 = TestUtils.createValidPdfMockFile();

        List<DocumentResult> documentResults = List.of(
                DocumentResult.builder()
                              .filename(file1.getOriginalFilename())
                              .success(true)
                              .chunksCreated(10)
                              .processingTimeMs(1000L)
                              .build(),
                DocumentResult.builder()
                              .filename(file2.getOriginalFilename())
                              .success(false)
                              .errorMessage("Failed to process document")
                              .build()
        );

        MultiDocumentUploadResponse sampleResponse = MultiDocumentUploadResponse.builder()
                                                                                .sessionId(testSessionId)
                                                                                .totalFiles(2)
                                                                                .successfulUploads(1)
                                                                                .failedUploads(1)
                                                                                .totalChunksCreated(10)
                                                                                .totalProcessingTimeMs(1000L)
                                                                                .documents(documentResults)
                                                                                .message(
                                                                                        "1 of 2 documents processed successfully")
                                                                                .build();

        when(documentService.processDocuments(eq(testSessionId), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(multipart("/api/sessions/{sessionId}/documents", testSessionId)
                                .file("files", file1.getBytes())
                                .file("files", file2.getBytes()))
               .andExpect(status().isMultiStatus())
               .andExpect(jsonPath("$.successfulUploads").value(1))
               .andExpect(jsonPath("$.failedUploads").value(1));
    }

    @Test
    void uploadMultipleDocuments_shouldReturn400_whenNoFilesProvided() throws Exception {
        String testSessionId = UUID.randomUUID().toString();

        mockMvc.perform(multipart("/api/sessions/{sessionId}/documents", testSessionId))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").value("No files provided"));
    }
}
