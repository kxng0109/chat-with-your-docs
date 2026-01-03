package io.github.kxng0109.chatwithdocs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kxng0109.chatwithdocs.entity.ChatMessage;
import io.github.kxng0109.chatwithdocs.entity.ChatSession;
import io.github.kxng0109.chatwithdocs.model.ChatRequest;
import io.github.kxng0109.chatwithdocs.model.ChatResponse;
import io.github.kxng0109.chatwithdocs.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {
    @Mock
    private ChatModel chatModel;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private SessionService sessionService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ChatService chatService;

    private ChatRequest sampleRequest;
    private List<Document> relevantDocuments;
    private String testSessionId;
    private ChatSession testSession;

    @BeforeEach
    void setUp() {
        testSessionId = UUID.randomUUID().toString();
        testSession = ChatSession.builder()
                                 .id(1L)
                                 .sessionId(testSessionId)
                                 .name("Test Session")
                                 .messages(new ArrayList<>())
                                 .documents(new ArrayList<>())
                                 .build();

        sampleRequest = new ChatRequest(
                testSessionId,
                "What is WiFi 7?",
                5,
                false,
                10
        );

        relevantDocuments = List.of(
                new Document("WiFi 7 is also called 802.11be",
                             Map.of("sessionId", testSessionId, "filename", "doc1.pdf")
                ),
                new Document("WiFi 7 introduces 4k QAM and 320MHz wide channels",
                             Map.of("sessionId", testSessionId, "filename", "doc2.pdf")
                ),
                new Document("WiFI 7 has a theoretical max speed of 5.8gbps on consumer devices",
                             Map.of("sessionId", testSessionId, "filename", "doc3.pdf")
                )
        );
    }

    @Test
    void chat_WithValidQuestion_shouldReturnResponse() {
        // Setup mocks
        when(sessionService.getSessionEntity(testSessionId)).thenReturn(testSession);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(relevantDocuments);
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock ChatClient chain
        ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
        ChatClient mockChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockCallSpec = mock(ChatClient.CallResponseSpec.class);

        // Mock ObjectMapper for sources serialization
        try {
            when(objectMapper.writeValueAsString(anyList())).thenReturn("[]");
        } catch (Exception e) {
            fail("ObjectMapper mock setup failed");
        }

        ChatResponse response = chatService.chat(sampleRequest);

        assertNotNull(response);
        assertEquals(testSessionId, response.sessionId());
        assertEquals(sampleRequest.question(), response.question());
        assertEquals(relevantDocuments.size(), response.sources().size());
        assertNotNull(response.answer());
        assertTrue(response.processingTimeMs() >= 0);

        // Verify interactions
        verify(sessionService).getSessionEntity(testSessionId);
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class)); // User + Assistant
    }

    @Test
    void chat_withNoRelevantDocuments_shouldReturnNoRelevantInfoMessage() {
        when(sessionService.getSessionEntity(testSessionId)).thenReturn(testSession);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = chatService.chat(sampleRequest);

        assertNotNull(response);
        assertEquals(testSessionId, response.sessionId());
        assertEquals(sampleRequest.question(), response.question());
        assertTrue(response.answer().contains("No relevant information found"));
        assertTrue(response.sources().isEmpty());

        verify(vectorStore).similaritySearch(any(SearchRequest.class));
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    void chat_WithNullTopK_shouldUseDefaultValue() {
        ChatRequest requestWithNullTopK = new ChatRequest(
                testSessionId,
                "What is WiFi 7?",
                null,
                false,
                10
        );

        when(sessionService.getSessionEntity(testSessionId)).thenReturn(testSession);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(relevantDocuments);
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);

        chatService.chat(requestWithNullTopK);

        verify(vectorStore).similaritySearch(captor.capture());
        assertEquals(5, captor.getValue().getTopK()); // Default value
    }

    @Test
    void chat_WithHistoryEnabled_shouldIncludeConversationHistory() {
        ChatRequest requestWithHistory = new ChatRequest(
                testSessionId,
                "What else about WiFi 7?",
                5,
                true,
                10
        );

        List<ChatMessage> historyMessages = List.of(
                ChatMessage.builder()
                           .session(testSession)
                           .role(ChatMessage.MessageRole.USER)
                           .content("What is WiFi 7?")
                           .build(),
                ChatMessage.builder()
                           .session(testSession)
                           .role(ChatMessage.MessageRole.ASSISTANT)
                           .content("WiFi 7 is the next generation standard.")
                           .build()
        );

        when(sessionService.getSessionEntity(testSessionId)).thenReturn(testSession);
        when(chatMessageRepository.findRecentMessages(testSessionId, 10)).thenReturn(historyMessages);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(relevantDocuments);
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        chatService.chat(requestWithHistory);

        verify(chatMessageRepository).findRecentMessages(testSessionId, 10);
    }

    @Test
    void chat_whenSessionNotFound_shouldThrowException() {
        when(sessionService.getSessionEntity(testSessionId))
                .thenThrow(new io.github.kxng0109.chatwithdocs.exception.SessionNotFoundException(testSessionId));

        assertThrows(
                io.github.kxng0109.chatwithdocs.exception.SessionNotFoundException.class,
                () -> chatService.chat(sampleRequest)
        );
    }

    @Test
    void chat_whenVectorStoreThrowsException_shouldPropagateException() {
        when(sessionService.getSessionEntity(testSessionId)).thenReturn(testSession);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("Vector store connection failed"));

        assertThrows(RuntimeException.class, () -> chatService.chat(sampleRequest));
    }

    @Test
    void chat_shouldFilterDocumentsBySessionId() {
        when(sessionService.getSessionEntity(testSessionId)).thenReturn(testSession);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(relevantDocuments);
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);

        chatService.chat(sampleRequest);

        verify(vectorStore).similaritySearch(captor.capture());

        SearchRequest capturedRequest = captor.getValue();
        assertNotNull(capturedRequest.getFilterExpression());
    }
}
