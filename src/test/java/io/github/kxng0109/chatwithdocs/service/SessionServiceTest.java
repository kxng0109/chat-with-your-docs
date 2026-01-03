package io.github.kxng0109.chatwithdocs.service;

import io.github.kxng0109.chatwithdocs.entity.ChatSession;
import io.github.kxng0109.chatwithdocs.entity.SessionDocument;
import io.github.kxng0109.chatwithdocs.exception.SessionNotFoundException;
import io.github.kxng0109.chatwithdocs.model.SessionCreateRequest;
import io.github.kxng0109.chatwithdocs.model.SessionResponse;
import io.github.kxng0109.chatwithdocs.repository.ChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionServiceTest {

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private SessionService sessionService;

    private String testSessionId;
    private ChatSession testSession;

    @BeforeEach
    void setUp() {
        testSessionId = UUID.randomUUID().toString();
        testSession = ChatSession.builder()
                                 .id(1L)
                                 .sessionId(testSessionId)
                                 .name("Test Session")
                                 .description("Test Description")
                                 .documents(new ArrayList<>())
                                 .messages(new ArrayList<>())
                                 .build();
    }

    @Test
    void createSession_withValidRequest_shouldCreateSession() {
        SessionCreateRequest request = new SessionCreateRequest(
                "New Session",
                "Session description"
        );

        when(sessionRepository.save(any(ChatSession.class)))
                .thenAnswer(invocation -> {
                    ChatSession session = invocation.getArgument(0);
                    session.setId(1L);
                    if (session.getSessionId() == null || session.getSessionId().isEmpty()) {
                        session.setSessionId(UUID.randomUUID().toString());
                    }
                    return session;
                });

        SessionResponse response = sessionService.createSession(request);

        assertNotNull(response);
        assertEquals(request.name(), response.name());
        assertEquals(request.description(), response.description());
        assertNotNull(response.sessionId());
        assertEquals(0, response.documentCount());
        assertEquals(0, response.messageCount());

        verify(sessionRepository).save(any(ChatSession.class));
    }

    @Test
    void createSession_withNullRequest_shouldUseDefaults() {
        when(sessionRepository.save(any(ChatSession.class)))
                .thenAnswer(invocation -> {
                    ChatSession session = invocation.getArgument(0);
                    session.setId(1L);
                    if (session.getSessionId() == null || session.getSessionId().isEmpty()) {
                        session.setSessionId(UUID.randomUUID().toString());
                    }
                    return session;
                });

        SessionCreateRequest nullRequest = new SessionCreateRequest(null, null);
        SessionResponse response = sessionService.createSession(nullRequest);

        assertNotNull(response);
        assertEquals("New Chat Session", response.name());
        assertNull(response.description());
        assertNotNull(response.sessionId());
    }

    @Test
    void getSessionInfo_withValidId_shouldReturnSession() {
        when(sessionRepository.findBySessionId(testSessionId))
                .thenReturn(Optional.of(testSession));

        SessionResponse response = sessionService.getSessionInfo(testSessionId, false);

        assertNotNull(response);
        assertEquals(testSessionId, response.sessionId());
        assertEquals(testSession.getName(), response.name());
        assertNull(response.documents());

        verify(sessionRepository).findBySessionId(testSessionId);
    }

    @Test
    void getSessionInfo_withIncludeDocuments_shouldReturnDocuments() {
        SessionDocument doc = SessionDocument.builder()
                                             .id(1L)
                                             .session(testSession)
                                             .originalFilename("test.pdf")
                                             .status(SessionDocument.DocumentStatus.COMPLETED)
                                             .chunkCount(10)
                                             .build();
        testSession.getDocuments().add(doc);

        when(sessionRepository.findBySessionId(testSessionId))
                .thenReturn(Optional.of(testSession));

        SessionResponse response = sessionService.getSessionInfo(testSessionId, true);

        assertNotNull(response);
        assertNotNull(response.documents());
        assertEquals(1, response.documents().size());
        assertEquals("test.pdf", response.documents().getFirst().fileName());
    }

    @Test
    void getSessionInfo_withInvalidId_shouldThrowException() {
        when(sessionRepository.findBySessionId(testSessionId))
                .thenReturn(Optional.empty());

        assertThrows(SessionNotFoundException.class,
                     () -> sessionService.getSessionInfo(testSessionId, false)
        );
    }

    @Test
    void getSessionEntity_withValidId_shouldReturnEntity() {
        when(sessionRepository.findBySessionId(testSessionId))
                .thenReturn(Optional.of(testSession));

        ChatSession result = sessionService.getSessionEntity(testSessionId);

        assertNotNull(result);
        assertEquals(testSessionId, result.getSessionId());
        assertEquals(testSession.getName(), result.getName());
    }

    @Test
    void listAllSessions_shouldReturnAllSessions() {
        List<ChatSession> sessions = List.of(
                ChatSession.builder()
                           .sessionId(UUID.randomUUID().toString())
                           .name("Session 1")
                           .documents(new ArrayList<>())
                           .messages(new ArrayList<>())
                           .build(),
                ChatSession.builder()
                           .sessionId(UUID.randomUUID().toString())
                           .name("Session 2")
                           .documents(new ArrayList<>())
                           .messages(new ArrayList<>())
                           .build()
        );

        when(sessionRepository.findAllByOrderByCreatedAtDescUpdatedAtDesc())
                .thenReturn(sessions);

        List<SessionResponse> responses = sessionService.listAllSessions(false);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("Session 1", responses.get(0).name());
        assertEquals("Session 2", responses.get(1).name());
    }

    @Test
    void deleteSession_withValidId_shouldDeleteSessionAndVectors() {
        when(sessionRepository.findBySessionId(testSessionId))
                .thenReturn(Optional.of(testSession));
        doNothing().when(vectorStore).delete(Collections.singletonList(any()));
        doNothing().when(sessionRepository).delete(testSession);

        sessionService.deleteSession(testSessionId);

        verify(sessionRepository).findBySessionId(testSessionId);
        verify(vectorStore).delete(Collections.singletonList(any()));
        verify(sessionRepository).delete(testSession);
    }

    @Test
    void deleteSession_withInvalidId_shouldThrowException() {
        when(sessionRepository.findBySessionId(testSessionId))
                .thenReturn(Optional.empty());

        assertThrows(SessionNotFoundException.class,
                     () -> sessionService.deleteSession(testSessionId)
        );

        verify(sessionRepository, never()).delete(any());
    }
}