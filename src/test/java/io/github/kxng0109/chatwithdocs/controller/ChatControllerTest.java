package io.github.kxng0109.chatwithdocs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kxng0109.chatwithdocs.model.ChatRequest;
import io.github.kxng0109.chatwithdocs.model.ChatResponse;
import io.github.kxng0109.chatwithdocs.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
public class ChatControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    @Test
    void chat_shouldReturn200Ok() throws Exception {
        String testSessionId = UUID.randomUUID().toString();

        ChatResponse sampleResponse = ChatResponse.builder()
                                                  .sessionId(testSessionId)
                                                  .processingTimeMs(1000L)
                                                  .question("What is WiFi 7?")
                                                  .answer("WiFi 7 is 802.11be")
                                                  .sources(List.of("Source 1", "Source 2"))
                                                  .sourceDocuments(List.of("doc1.pdf", "doc2.pdf"))
                                                  .messageId(1L)
                                                  .build();

        ChatRequest sampleRequest = new ChatRequest(
                testSessionId,
                "What is WiFi 7?",
                5,
                false,
                10
        );

        when(chatService.chat(any(ChatRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleRequest)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.sessionId").value(testSessionId))
               .andExpect(jsonPath("$.processingTimeMs").value(sampleResponse.processingTimeMs()))
               .andExpect(jsonPath("$.question").value(sampleResponse.question()))
               .andExpect(jsonPath("$.answer").value(sampleResponse.answer()))
               .andExpect(jsonPath("$.sources").isArray())
               .andExpect(jsonPath("$.sourceDocuments").isArray())
               .andExpect(jsonPath("$.messageId").value(1));
    }

    @Test
    void chat_shouldReturn400BadRequest_whenSessionIdIsNull() throws Exception {
        ChatRequest invalidRequest = new ChatRequest(
                null,
                "What is WiFi 7?",
                5,
                false,
                10
        );

        mockMvc.perform(post("/api/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void chat_shouldReturn400BadRequest_whenQuestionIsNull() throws Exception {
        ChatRequest invalidRequest = new ChatRequest(
                UUID.randomUUID().toString(),
                null,
                5,
                false,
                10
        );

        mockMvc.perform(post("/api/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void chat_shouldReturn400BadRequest_whenQuestionIsBlank() throws Exception {
        ChatRequest invalidRequest = new ChatRequest(
                UUID.randomUUID().toString(),
                "   ",
                5,
                false,
                10
        );

        mockMvc.perform(post("/api/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void health_shouldReturn200Ok() throws Exception {
        mockMvc.perform(get("/api/chat/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$").value("Chat service is running"));
    }
}
