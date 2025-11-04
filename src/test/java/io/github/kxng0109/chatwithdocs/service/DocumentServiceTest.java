package io.github.kxng0109.chatwithdocs.service;

import io.github.kxng0109.chatwithdocs.TestUtils;
import io.github.kxng0109.chatwithdocs.exception.DocumentProcessingException;
import io.github.kxng0109.chatwithdocs.model.DocumentUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {
    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private DocumentService documentService;

    private MultipartFile validFile;

    @BeforeEach
    public void setUp() {
        validFile = TestUtils.createValidPdfMockFile();
    }

    @Test
    void processDocument_WithValidPdf_ShouldReturnSuccessResponse() {
        doNothing().when(vectorStore).add(anyList());

        DocumentUploadResponse result = documentService.processDocument(validFile);

        assertNotNull(result);

        verify(vectorStore).add(anyList());
    }

    @Test
    void processDocument_WithNullFile_ShouldThrowException() {
        assertThrows(NullPointerException.class, () -> {
                         documentService.processDocument(null);
                     }
        );
    }

    @Test
    void processDocument_WithEmptyFile_ShouldThrowException() {
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThrows(DocumentProcessingException.class, () -> {
                         documentService.processDocument(emptyFile);
                     }
        );
    }

    @Test
    void processDocument_WhenVectorStoreThrowsException_ShouldPropagateException() {
        doThrow(new RuntimeException("Vector store error"))
                .when(vectorStore).add(anyList());

        assertThrows(DocumentProcessingException.class, () -> {
                         documentService.processDocument(validFile);
                     }
        );
    }
}
