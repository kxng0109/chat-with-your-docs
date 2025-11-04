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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
        ChatResponse sampleResponse = ChatResponse.builder()
                                                  .processingTimeMs(1000L)
                                                  .question("What is WiFi 7?")
                                                  .answer("WiFi 7 is 802.11be")
                                                  .sources(List.of("Source 1", "Source 2"))
                                                  .build();

        ChatRequest sampleRequest = new ChatRequest(sampleResponse.question(), 5);

        when(chatService.chat(any(ChatRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleRequest)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.processingTimeMs").value(sampleResponse.processingTimeMs()))
               .andExpect(jsonPath("$.question").value(sampleResponse.question()))
               .andExpect(jsonPath("$.answer").value(sampleResponse.answer()))
               .andExpect(jsonPath("$.sources").isArray());
    }

    @Test
    void chat_shouldReturn400BadRequest_whenRequestIsInvalid() throws Exception {
        ChatRequest sampleRequest = new ChatRequest(null, 5);

        mockMvc.perform(
                       post("/api/chat")
                               .contentType(MediaType.APPLICATION_JSON)
                               .content(objectMapper.writeValueAsString(sampleRequest)))
               .andExpect(status().isBadRequest());
    }
}
