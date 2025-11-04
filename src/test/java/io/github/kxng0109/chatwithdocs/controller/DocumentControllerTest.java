package io.github.kxng0109.chatwithdocs.controller;

import io.github.kxng0109.chatwithdocs.TestUtils;
import io.github.kxng0109.chatwithdocs.model.DocumentUploadResponse;
import io.github.kxng0109.chatwithdocs.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
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
    void uploadDocument_shouldReturn200Ok_whenFileIsValid() throws Exception {
        MockMultipartFile validFile = TestUtils.createValidPdfMockFile();

        DocumentUploadResponse sampleResponse = DocumentUploadResponse.builder()
                                                                      .chunksStored(10)
                                                                      .chunksCreated(10)
                                                                      .message("Document processed succesfully")
                                                                      .filename(validFile.getOriginalFilename())
                                                                      .build();

        when(documentService.processDocument(any(MultipartFile.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(multipart("/api/documents/upload")
                                .file(validFile))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.filename").value(validFile.getOriginalFilename()))
               .andExpect(jsonPath("$.chunksStored").value(sampleResponse.chunksStored()))
               .andExpect(jsonPath("$.chunksCreated").value(sampleResponse.chunksCreated()))
               .andExpect(jsonPath("$.message").value(sampleResponse.message()));
    }

    @Test
    void uploadDocument_shouldReturn400_whenFileIsNotValid() throws Exception {
        mockMvc.perform(multipart("/api/documents/upload"))
               .andExpect(status().isBadRequest());
    }
}
