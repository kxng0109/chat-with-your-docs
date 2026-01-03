package io.github.kxng0109.chatwithdocs;

import io.github.kxng0109.chatwithdocs.model.*;
import io.github.kxng0109.chatwithdocs.repository.ChatMessageRepository;
import io.github.kxng0109.chatwithdocs.repository.ChatSessionRepository;
import io.github.kxng0109.chatwithdocs.repository.SessionDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration test for the RAG (Retrieval-Augmented Generation) workflow.
 * Tests the complete flow: Session creation -> Document upload -> Chat query.
 */
@Disabled("Wondering why it's having issues with Docker")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class RAGIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg18")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("init-db-test.sql");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private SessionDocumentRepository documentRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.ai.vectorstore.pgvector.initialize-schema", () -> "true");

        registry.add("spring.ai.provider", () -> "lmstudio");
        registry.add("spring.ai.ollama.base-url", () -> "http://localhost:1234");
    }

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        documentRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    @Test
    void completeRAGWorkflow_shouldWork() throws Exception {
        SessionCreateRequest createRequest = new SessionCreateRequest(
                "Integration Test Session",
                "Testing RAG workflow"
        );

        ResponseEntity<SessionResponse> sessionResponse = restTemplate.postForEntity(
                "/api/sessions",
                createRequest,
                SessionResponse.class
        );

        assertEquals(HttpStatus.CREATED, sessionResponse.getStatusCode());
        assertNotNull(sessionResponse.getBody());
        String sessionId = sessionResponse.getBody().sessionId();
        assertNotNull(sessionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ClassPathResource("test-document.pdf"));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<DocumentUploadResponse> uploadResponse = restTemplate.exchange(
                "/api/sessions/{sessionId}/documents/single",
                HttpMethod.POST,
                requestEntity,
                DocumentUploadResponse.class,
                sessionId
        );

        assertEquals(HttpStatus.CREATED, uploadResponse.getStatusCode());
        assertNotNull(uploadResponse.getBody());
        assertTrue(uploadResponse.getBody().chunksCreated() > 0);

        Thread.sleep(2000);

        ChatRequest chatRequest = new ChatRequest(
                sessionId,
                "What is this document about?",
                5,
                false,
                10
        );

        ResponseEntity<ChatResponse> chatResponse = restTemplate.postForEntity(
                "/api/chat",
                chatRequest,
                ChatResponse.class
        );

        assertEquals(HttpStatus.OK, chatResponse.getStatusCode());
        assertNotNull(chatResponse.getBody());
        assertNotNull(chatResponse.getBody().answer());
        assertFalse(chatResponse.getBody().sources().isEmpty());

        ResponseEntity<SessionResponse> finalSessionState = restTemplate.getForEntity(
                "/api/sessions/{sessionId}",
                SessionResponse.class,
                sessionId
        );

        assertEquals(HttpStatus.OK, finalSessionState.getStatusCode());
        assertNotNull(finalSessionState.getBody());
        assertTrue(finalSessionState.getBody().messageCount() >= 2);

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/sessions/{sessionId}",
                HttpMethod.DELETE,
                null,
                Void.class,
                sessionId
        );

        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
    }

    @Test
    void sessionIsolation_documentsShouldNotCrossSession() {
        SessionResponse session1 = restTemplate.postForObject(
                "/api/sessions",
                new SessionCreateRequest("Session 1", null),
                SessionResponse.class
        );

        SessionResponse session2 = restTemplate.postForObject(
                "/api/sessions",
                new SessionCreateRequest("Session 2", null),
                SessionResponse.class
        );

        assertNotNull(session1);
        assertNotNull(session2);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ClassPathResource("test-document.pdf"));

        restTemplate.exchange(
                "/api/sessions/{sessionId}/documents/single",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                DocumentUploadResponse.class,
                session1.sessionId()
        );

        ChatRequest chatRequest = new ChatRequest(
                session2.sessionId(),
                "What is this document about?",
                5,
                false,
                10
        );

        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "/api/chat",
                chatRequest,
                ChatResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().answer().contains("No relevant information"));
    }

    @Test
    void multipleDocuments_shouldAllBeSearchable() throws Exception {
        SessionResponse session = restTemplate.postForObject(
                "/api/sessions",
                new SessionCreateRequest("Multi-doc session", null),
                SessionResponse.class
        );
        assertNotNull(session);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", new ClassPathResource("test-document.pdf"));
        body.add("files", new ClassPathResource("test-document.pdf"));

        ResponseEntity<MultiDocumentUploadResponse> uploadResponse = restTemplate.exchange(
                "/api/sessions/{sessionId}/documents",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                MultiDocumentUploadResponse.class,
                session.sessionId()
        );

        assertEquals(HttpStatus.CREATED, uploadResponse.getStatusCode());
        assertNotNull(uploadResponse.getBody());
        assertEquals(2, uploadResponse.getBody().totalFiles());
        assertEquals(2, uploadResponse.getBody().successfulUploads());

        Thread.sleep(2000);

        ChatRequest chatRequest = new ChatRequest(
                session.sessionId(),
                "Summarize all documents",
                10,
                false,
                10
        );

        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "/api/chat",
                chatRequest,
                ChatResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().sources().isEmpty());
    }

    @Test
    void conversationHistory_shouldProvideContext() {
        SessionResponse session = restTemplate.postForObject(
                "/api/sessions",
                new SessionCreateRequest("History test", null),
                SessionResponse.class
        );
        assertNotNull(session);

        ChatRequest firstQuery = new ChatRequest(
                session.sessionId(),
                "What is the main topic?",
                5,
                false,
                10
        );

        restTemplate.postForEntity("/api/chat", firstQuery, ChatResponse.class);

        ChatRequest secondQuery = new ChatRequest(
                session.sessionId(),
                "Can you elaborate on that?",
                5,
                true,
                10
        );

        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                "/api/chat",
                secondQuery,
                ChatResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}