package io.github.kxng0109.chatwithdocs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kxng0109.chatwithdocs.model.SessionCreateRequest;
import io.github.kxng0109.chatwithdocs.model.SessionResponse;
import io.github.kxng0109.chatwithdocs.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
public class SessionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SessionService sessionService;

    @Test
    void createSession_shouldReturn201Created() throws Exception {
        SessionCreateRequest request = new SessionCreateRequest(
                "Test Session",
                "A test session description"
        );

        String sessionId = UUID.randomUUID().toString();
        SessionResponse response = SessionResponse.builder()
                                                  .sessionId(sessionId)
                                                  .name(request.name())
                                                  .description(request.description())
                                                  .documentCount(0)
                                                  .messageCount(0)
                                                  .createdAt(LocalDateTime.now())
                                                  .updatedAt(LocalDateTime.now())
                                                  .build();

        when(sessionService.createSession(any(SessionCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/sessions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.sessionId").value(sessionId))
               .andExpect(jsonPath("$.name").value(request.name()))
               .andExpect(jsonPath("$.description").value(request.description()))
               .andExpect(jsonPath("$.documentCount").value(0))
               .andExpect(jsonPath("$.messageCount").value(0));
    }

    @Test
    void createSession_shouldCreateWithDefaultName_whenRequestIsNull() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        SessionResponse response = SessionResponse.builder()
                                                  .sessionId(sessionId)
                                                  .name("New Chat Session")
                                                  .documentCount(0)
                                                  .messageCount(0)
                                                  .createdAt(LocalDateTime.now())
                                                  .updatedAt(LocalDateTime.now())
                                                  .build();

        when(sessionService.createSession(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/sessions")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.name").value("New Chat Session"));
    }

    @Test
    void getSession_shouldReturn200Ok() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        SessionResponse response = SessionResponse.builder()
                                                  .sessionId(sessionId)
                                                  .name("Existing Session")
                                                  .documentCount(2)
                                                  .messageCount(5)
                                                  .createdAt(LocalDateTime.now())
                                                  .updatedAt(LocalDateTime.now())
                                                  .build();

        when(sessionService.getSessionInfo(eq(sessionId), eq(false)))
                .thenReturn(response);

        mockMvc.perform(get("/api/sessions/{sessionId}", sessionId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.sessionId").value(sessionId))
               .andExpect(jsonPath("$.name").value("Existing Session"))
               .andExpect(jsonPath("$.documentCount").value(2))
               .andExpect(jsonPath("$.messageCount").value(5));
    }

    @Test
    void getSession_shouldIncludeDocuments_whenRequested() throws Exception {
        String sessionId = UUID.randomUUID().toString();

        when(sessionService.getSessionInfo(eq(sessionId), eq(true)))
                .thenReturn(SessionResponse.builder()
                                           .sessionId(sessionId)
                                           .name("Session with docs")
                                           .documents(List.of())
                                           .build());

        mockMvc.perform(get("/api/sessions/{sessionId}", sessionId)
                                .param("includeDocuments", "true"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.documents").isArray());
    }

    @Test
    void listAllSessions_shouldReturn200Ok() throws Exception {
        List<SessionResponse> sessions = List.of(
                SessionResponse.builder()
                               .sessionId(UUID.randomUUID().toString())
                               .name("Session 1")
                               .documentCount(1)
                               .messageCount(3)
                               .createdAt(LocalDateTime.now())
                               .build(),
                SessionResponse.builder()
                               .sessionId(UUID.randomUUID().toString())
                               .name("Session 2")
                               .documentCount(0)
                               .messageCount(0)
                               .createdAt(LocalDateTime.now())
                               .build()
        );

        when(sessionService.listAllSessions(false))
                .thenReturn(sessions);

        mockMvc.perform(get("/api/sessions"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(2))
               .andExpect(jsonPath("$[0].name").value("Session 1"))
               .andExpect(jsonPath("$[1].name").value("Session 2"));
    }

    @Test
    void deleteSession_shouldReturn204NoContent() throws Exception {
        String sessionId = UUID.randomUUID().toString();

        doNothing().when(sessionService).deleteSession(sessionId);

        mockMvc.perform(delete("/api/sessions/{sessionId}", sessionId))
               .andExpect(status().isNoContent());

        verify(sessionService, times(1)).deleteSession(sessionId);
    }
}
