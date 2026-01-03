package io.github.kxng0109.chatwithdocs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kxng0109.chatwithdocs.entity.ChatMessage;
import io.github.kxng0109.chatwithdocs.entity.ChatSession;
import io.github.kxng0109.chatwithdocs.model.ChatRequest;
import io.github.kxng0109.chatwithdocs.model.ChatResponse;
import io.github.kxng0109.chatwithdocs.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the core functionality for processing chat requests using a language model.
 * The service integrates with a vector store to retrieve relevant documents and uses
 * these documents to build a context that helps generate a meaningful response.
 * <p>
 * ChatService orchestrates the workflow of retrieving relevant documents,
 * constructing a prompt, and interacting with a Large Language Model (LLM) to generate
 * an answer tailored to the input question.
 * <p>Key changes from original:</p>
 * <ul>
 *   <li>Vector search is filtered by sessionId - only session documents are searched</li>
 *   <li>Conversation history is included in prompts for context</li>
 *   <li>Messages are persisted for conversation continuity</li>
 *   <li>Source documents are identified in responses</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {
    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final ChatMessageRepository chatMessageRepository;
    private final SessionService sessionService;
    private final ObjectMapper objectMapper;
    private final MarkdownService markdownService;

    /**
     * Processes a chat request by searching for relevant documents, building a context,
     * and generating an answer using a language model.
     *
     * @param chatRequest The input chat request containing the question and optional parameters such as topK.
     * @return A ChatResponse containing the generated answer, relevant source documents,
     * the question, and processing time in milliseconds.
     */
    @Transactional
    public ChatResponse chat(ChatRequest chatRequest) {
        long startTime = System.currentTimeMillis();

        String sessionId = chatRequest.sessionId();
        String question = chatRequest.question();
        int topK = chatRequest.topK() != null ? chatRequest.topK() : 5;
        boolean includeHistory = chatRequest.includeHistory();
        int historyLimit = chatRequest.historyLimit();

        log.info("Processing chat request for session: {}. Question \"{}\", topK:{}",
                 sessionId,
                 question,
                 topK
        );

        //Verify session exists and get it
        ChatSession session = sessionService.getSessionEntity(sessionId);

        //Get a certain number chat history if indicated (limited by historyLimit)
        List<ChatMessage> history = new ArrayList<>();
        if (includeHistory) {
            history = chatMessageRepository.findRecentMessages(sessionId, historyLimit).reversed();
            log.debug("Retrieved {} messages from history.", history.size());
        }

        //Save users message
        saveChatMessage(
                session,
                ChatMessage.MessageRole.USER,
                question,
                null
        );

        // Get relevant documents that are related to the question.
        // These documents are restricted to documents that are connected to this sessions ID
        List<Document> relevantDocs = searchForRelevantDocuments(sessionId, question, topK);
        //If there are no relevant documents, then we can't answer the question
        //So just return a response and save the assistant message
        if (relevantDocs.isEmpty()) {
            log.warn("No relevant documents found in session {} for question \"{}\"", sessionId, question);

            ChatResponse response = ChatResponse.builder()
                                                .sessionId(sessionId)
                                                .processingTimeMs(System.currentTimeMillis() - startTime)
                                                .answer("No relevant information found to answer this question. Make sure you've uploaded documents related to your query.")
                                                .sources(List.of())
                                                .sourceDocuments(List.of())
                                                .question(question)
                                                .build();

            saveChatMessage(session, ChatMessage.MessageRole.ASSISTANT, response.answer(), null);
            return response;
        }

        //Get the content(the text) in each relevant source/doc
        List<String> sources = relevantDocs.stream()
                                           .map(Document::getText)
                                           .toList();

        //Get the relevant docs filename
        List<String> sourceDocumentsFileName = relevantDocs.stream()
                                                           .map(document -> document.getMetadata()
                                                                                    .getOrDefault("filename", "unknown")
                                                                                    .toString())
                                                           .distinct()
                                                           .toList();

        //Basically "prettifies" the sources we got
        String context = buildContext(sources);
        log.debug("Built context from {} chunks ({} chars)", sources.size(), context.length());

        //Build a prompt for the LLM, using the context, question and previous chat history if any or if the user allows us to
        String prompt = buildPromptWithHistory(context, question, history);
        log.debug("Prompt for this request: {}", prompt);
        log.debug("Prompt length: {} chars", prompt.length());

        //Send prompt to llm and get the answer
        log.debug("Sending prompt to LLM.");
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        String answer = chatClient.prompt()
                                  .user(prompt)
                                  .call()
                                  .content();

        if (answer == null || answer.isBlank()) {
            answer = "LLM was not able to generate an answer, no answer received. Try again or rephrase your question.";
        }

        try {
            answer = markdownService.stripMarkdown(answer);
        } catch (Exception e) {
            log.error("An error occurred while stripping markdown: {}", e.getMessage(), e);
        }

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("LLM responded with an answer ({} chars) in {}ms", answer.length(), processingTime);

        //Serializing to sources list into JSON for storage
        String serializedSources;
        try {
            serializedSources = objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize sources: {}", e.getMessage(), e);
            serializedSources = "[]";
        }

        //Save the assistant's message
        ChatMessage assistantMessage = saveChatMessage(
                session,
                ChatMessage.MessageRole.ASSISTANT,
                answer,
                serializedSources
        );

        return ChatResponse.builder()
                           .sessionId(sessionId)
                           .answer(answer)
                           .sources(sources)
                           .sourceDocuments(sourceDocumentsFileName)
                           .question(question)
                           .processingTimeMs(processingTime)
                           .messageId(assistantMessage.getId())
                           .build();

    }

    /**
     * Constructs a formatted context string by iterating over the provided list of sources.
     * Each source is labeled as a separate document with its corresponding index.
     * The method appends content from each source into a single string, separated by line breaks.
     * <p>
     * Example of a response:
     * <pre>
     * Document 1:
     * Spring AI is a framework for building AI applications...
     *
     * Document 2:
     * The framework provides abstractions for LLMs...
     *
     * Document 3:
     * Spring AI supports RAG patterns out of the box...
     * </pre>
     *
     * @param sources A list of strings, each representing the content of a document to be included in the context.
     * @return A single string that combines all sources, with each source labeled and separated appropriately.
     */
    private String buildContext(List<String> sources) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < sources.size(); i++) {
            sb.append("Document ").append(i).append(":\n");
            sb.append(sources.get(i));
            sb.append("\n\n");
        }

        return sb.toString();
    }

    /**
     * Builds a prompt string by embedding the provided context and question into a predefined template.
     * It includes conversation history for context.
     *
     * @param context             The textual context to be used for answering the question.
     * @param question            The question to be answered based on the provided context.
     * @param conversationHistory The chat history of the chat session
     * @return The formatted prompt string containing the context, question and chat history, if any.
     */
    private String buildPromptWithHistory(
            String context,
            String question,
            List<ChatMessage> conversationHistory
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                              You are a helpful AI assistant. Use the following pieces of context from the user's documents to answer the question.
                              
                              If you don't know the answer based on the context, just say that you don't know.
                              Don't try to make up an answer or information.
                              
                              CRITICAL FORMATTING INSTRUCTIONS:
                              - You must answer in PLAIN TEXT only.
                              - DO NOT use Markdown formatting.
                              - DO NOT use asterisks (**) for bolding or italics.
                              - DO NOT use bullet points or lists.
                              - Write in standard paragraphs.
                              - If you need to list items, use comma separation or numbered sentences (e.g., "1. First item. 2. Second item.") instead of bullet points.
                              - Provide the answer as a SINGLE, flowing paragraph of plain text.
                              - DO NOT use tables.
                              - DO NOT use bullet points or lists.
                              - DO NOT use Markdown formatting (no asterisks, no hashes).
                              - If comparing items, write them out in full sentences (e.g., "Mask ROM is permanent, whereas PROM is programmable once.").
                              """
        );

        if (!conversationHistory.isEmpty()) {
            prompt.append("Previous conversation:\n");
            for (ChatMessage message : conversationHistory) {
                String role = message.getRole() == ChatMessage.MessageRole.USER ? "User" : "Assistant";
                prompt.append(role).append(":").append(message.getContent()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("Context from the documents:\n");
        prompt.append(context).append("\n");

        prompt.append("The question to be answered: ").append(question).append("\n\n");

        prompt.append("Answer: ");

        return prompt.toString();
    }

    /**
     * Saves a message to the database
     *
     * @param chatSession current chat session
     * @param role        role of the sender of the message (e.g User or Assistant)
     * @param content     the message itself
     * @param sources     the sources which were used to answer the question
     * @return returns a {@code ChatMessage} containing the session, role, content and sources
     */
    private ChatMessage saveChatMessage(
            ChatSession chatSession,
            ChatMessage.MessageRole role,
            String content,
            String sources
    ) {
        ChatMessage message = ChatMessage.builder()
                                         .session(chatSession)
                                         .role(role)
                                         .content(content)
                                         .sources(sources)
                                         .build();
        return chatMessageRepository.save(message);
    }

    /**
     * Searches for relevant documents within a specific session.
     * Uses metadata filtering to only search documents belonging to the session.
     *
     * @param sessionId The session to search within
     * @param query     The search query
     * @param topK      Number of results to return
     * @return List of relevant documents
     */
    private List<Document> searchForRelevantDocuments(String sessionId, String query, int topK) {
        log.debug("Searching session {} for:  \"{}\"", sessionId, query);

        // Build filter expression to scope search to this session
        Filter.Expression filterExpression = new FilterExpressionBuilder()
                .eq("sessionId", sessionId)
                .build();

        SearchRequest searchRequest = SearchRequest.builder()
                                                   .query(query)
                                                   .topK(topK)
                                                   .filterExpression(filterExpression)
                                                   .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);
        log.debug("Found {} relevant documents", results.size());

        return results;
    }

}
