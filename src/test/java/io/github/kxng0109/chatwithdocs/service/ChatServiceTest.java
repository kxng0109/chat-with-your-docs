package io.github.kxng0109.chatwithdocs.service;

import io.github.kxng0109.chatwithdocs.model.ChatRequest;
import io.github.kxng0109.chatwithdocs.model.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {
    private final org.springframework.ai.chat.model.ChatResponse sampleResponse = new org.springframework.ai.chat.model.ChatResponse(
            List.of(
                    new Generation(
                            new AssistantMessage(
                                    "WiFi 7 (known as 802.11be) provides very high speed for wireless devices")
                    )
            )
    );

    @Mock
    private ChatModel chatModel;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private ChatService chatService;

    private ChatRequest sampleRequest;
    private List<Document> relevantDocuments;

    @BeforeEach
    void setUp() {
        sampleRequest = new ChatRequest(
                "What is WiFi 7?",
                5
        );

        relevantDocuments = List.of(
                new Document("WiFi 7 is also called 802.11be", Map.of("source", "doc1")),
                new Document("WiFi 7 introduces 4k QAM and 320MHz wide channels", Map.of("source", "doc2")),
                new Document("WiFI 7 has a theoretical max speed of 5.8gbps on consumer devices",
                             Map.of("source", "doc3")
                )
        );
    }

    @Test
    void chat_WithValidQuestion_shouldReturnResponse() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(relevantDocuments);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(sampleResponse);

        ChatResponse response = chatService.chat(sampleRequest);

        assertNotNull(response);
        assertEquals(sampleRequest.question(), response.question());
        assertEquals(relevantDocuments.size(), response.sources().size());

        verify(vectorStore).similaritySearch(any(SearchRequest.class));
        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    void chat_withNoRelevantDocuments_shouldReturnNoRelevantDocuments() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());

        ChatResponse response = chatService.chat(sampleRequest);

        assertNotNull(response);
        assertEquals(sampleRequest.question(), response.question());
        assertTrue(response.answer().contains("No relevant information found to answer this question."));

        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void chat_WithNullTopK_shouldUseDefaultValue() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(relevantDocuments);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(sampleResponse);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);

        ChatResponse response = chatService.chat(sampleRequest);

        assertNotNull(response);
        assertEquals(sampleRequest.question(), response.question());
        assertEquals(relevantDocuments.size(), response.sources().size());

        verify(vectorStore).similaritySearch(captor.capture());
        verify(chatModel).call(any(Prompt.class));

        assertEquals(5, captor.getValue().getTopK());
    }

    @Test
    void chat_whenVectorStoreThrowsException_shouldPropagateException() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> chatService.chat(sampleRequest));
    }
}
