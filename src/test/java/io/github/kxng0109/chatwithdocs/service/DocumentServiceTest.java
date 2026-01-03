package io.github.kxng0109.chatwithdocs.service;

import io.github.kxng0109.chatwithdocs.TestUtils;
import io.github.kxng0109.chatwithdocs.entity.ChatSession;
import io.github.kxng0109.chatwithdocs.entity.SessionDocument;
import io.github.kxng0109.chatwithdocs.exception.DocumentProcessingException;
import io.github.kxng0109.chatwithdocs.model.DocumentUploadResponse;
import io.github.kxng0109.chatwithdocs.model.MultiDocumentUploadResponse;
import io.github.kxng0109.chatwithdocs.repository.SessionDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {
    @Mock
    private VectorStore vectorStore;

    @Mock
    private SessionService sessionService;

    @Mock
    private SessionDocumentRepository documentRepository;

    @InjectMocks
    private DocumentService documentService;

    private String testSessionId;
    private ChatSession testSession;
    private MultipartFile validFile;

    @BeforeEach
    public void setUp() {
        testSessionId = UUID.randomUUID().toString();
        testSession = ChatSession.builder()
                                 .id(1L)
                                 .sessionId(testSessionId)
                                 .name("Test Session")
                                 .documents(new ArrayList<>())
                                 .messages(new ArrayList<>())
                                 .build();

        validFile = TestUtils.createValidPdfMockFile();
    }

    @Test
    void processDocument_WithValidPdf_ShouldReturnSuccessResponse() {
        when(sessionService.getSessionEntity(testSessionId)).thenReturn(testSession);
        when(documentRepository.save(any(SessionDocument.class)))
                .thenAnswer(invocation -> {
                    SessionDocument doc = invocation.getArgument(0);
                    doc.setId(1L);
                    return doc;
                });
        doNothing().when(vectorStore).add(anyList());

        DocumentUploadResponse result = documentService.processDocument(validFile, testSessionId);

        assertNotNull(result);
        assertEquals(validFile.getOriginalFilename(), result.filename());
        assertTrue(result.processingTimeMs() >= 0);

        verify(sessionService).getSessionEntity(testSessionId);
        verify(documentRepository, atLeast(2)).save(any(SessionDocument.class));
        verify(vectorStore).add(anyList());
    }

    @Test
    void processDocuments_WithMultipleFiles_ShouldProcessAll() {
        MultipartFile file1 = TestUtils.createValidPdfMockFile();
        MultipartFile file2 = TestUtils.createValidPdfMockFile();
        MultipartFile[] files = {file1, file2};

        when(sessionService.getSessionEntity(testSessionId)).thenReturn(testSession);
        when(documentRepository.save(any(SessionDocument.class)))
                .thenAnswer(invocation -> {
                    SessionDocument doc = invocation.getArgument(0);
                    doc.setId((long) (Math.random() * 1000));
                    return doc;
                });
        doNothing().when(vectorStore).add(anyList());

        MultiDocumentUploadResponse result = documentService.processDocuments(testSessionId, files);

        assertNotNull(result);
        assertEquals(testSessionId, result.sessionId());
        assertEquals(2, result.totalFiles());
        assertEquals(2, result.successfulUploads());
        assertEquals(0, result.failedUploads());

        verify(vectorStore, times(2)).add(anyList());
    }

    @Test
    void processDocuments_WithSomeFailures_ShouldContinueProcessing() {
        MockMultipartFile validFile1 = TestUtils.createValidPdfMockFile();
        MockMultipartFile corruptFile = new MockMultipartFile(
                "file",
                "corrupt.pdf",
                "application/pdf",
                new byte[]{1, 2, 3}
        );
        MultipartFile[] files = {validFile1, corruptFile};

        when(sessionService.getSessionEntity(testSessionId)).thenReturn(testSession);
        when(documentRepository.save(any(SessionDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(vectorStore).add(anyList());

        MultiDocumentUploadResponse result = documentService.processDocuments(testSessionId, files);

        assertNotNull(result);
        assertEquals(2, result.totalFiles());
        assertTrue(result.successfulUploads() >= 0);
        assertTrue(result.failedUploads() >= 0);
        assertEquals(2, result.successfulUploads() + result.failedUploads());
    }

    @Test
    void processDocument_WithEmptyFile_ShouldThrowException() {
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        when(sessionService.getSessionEntity(testSessionId)).thenReturn(testSession);
        when(documentRepository.save(any(SessionDocument.class)))
                .thenAnswer(invocation -> {
                    SessionDocument doc = invocation.getArgument(0);
                    doc.setId(1L);
                    return doc;
                });

        assertThrows(DocumentProcessingException.class,
                     () -> documentService.processDocument(emptyFile, testSessionId)
        );
    }

    @Test
    void processDocument_WithInvalidSession_ShouldThrowException() {
        String invalidSessionId = UUID.randomUUID().toString();

        when(sessionService.getSessionEntity(invalidSessionId))
                .thenThrow(new io.github.kxng0109.chatwithdocs.exception.SessionNotFoundException(invalidSessionId));

        assertThrows(
                io.github.kxng0109.chatwithdocs.exception.SessionNotFoundException.class,
                () -> documentService.processDocument(validFile, invalidSessionId)
        );
    }

    @Test
    void processDocument_WhenVectorStoreThrowsException_ShouldPropagateException() {
        when(sessionService.getSessionEntity(testSessionId)).thenReturn(testSession);
        when(documentRepository.save(any(SessionDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("Vector store error"))
                .when(vectorStore).add(anyList());

        assertThrows(DocumentProcessingException.class,
                     () -> documentService.processDocument(validFile, testSessionId)
        );

        verify(documentRepository, atLeastOnce()).save(
                argThat(
                        doc ->
                                doc.getStatus() == SessionDocument.DocumentStatus.FAILED
                )
        );
    }

    @Test
    void processDocument_ShouldEnrichChunksWithSessionMetadata() {
        when(sessionService.getSessionEntity(testSessionId)).thenReturn(testSession);
        when(documentRepository.save(any(SessionDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doAnswer(invocation -> {
            java.util.List<org.springframework.ai.document.Document> docs = invocation.getArgument(0);
            for (org.springframework.ai.document.Document doc : docs) {
                assertTrue(doc.getMetadata().containsKey("sessionId"));
                assertEquals(testSessionId, doc.getMetadata().get("sessionId"));
                assertTrue(doc.getMetadata().containsKey("filename"));
                assertTrue(doc.getMetadata().containsKey("chunk_index"));
                assertTrue(doc.getMetadata().containsKey("total_chunks"));
            }
            return null;
        }).when(vectorStore).add(anyList());

        documentService.processDocument(validFile, testSessionId);

        verify(vectorStore).add(anyList());
    }
}
