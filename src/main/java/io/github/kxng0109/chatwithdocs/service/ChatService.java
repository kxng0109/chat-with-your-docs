package io.github.kxng0109.chatwithdocs.service;

import io.github.kxng0109.chatwithdocs.model.ChatRequest;
import io.github.kxng0109.chatwithdocs.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Provides the core functionality for processing chat requests using a language model.
 * The service integrates with a vector store to retrieve relevant documents and uses
 * these documents to build a context that helps generate a meaningful response.
 * <p>
 * ChatService orchestrates the workflow of retrieving relevant documents,
 * constructing a prompt, and interacting with a Large Language Model (LLM) to generate
 * an answer tailored to the input question.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {
    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    /**
     * Processes a chat request by searching for relevant documents, building a context,
     * and generating an answer using a language model.
     *
     * @param chatRequest The input chat request containing the question and optional parameters such as topK.
     * @return A ChatResponse containing the generated answer, relevant source documents,
     * the question, and processing time in milliseconds.
     */
    public ChatResponse chat(ChatRequest chatRequest) {
        long startTime = System.currentTimeMillis();

        String question = chatRequest.question();
        int topK = chatRequest.topK() != null ? chatRequest.topK() : 5;

        log.info("Processing chat request: Question \"{}\", topK:{}", question, topK);

        /*
         * An example of a response for this would be:
         * Chunk 1: "Spring AI is a framework for building AI applications..."
         * Chunk 2: "The framework provides abstractions for LLMs..."
         * Chunk 3: "Spring AI supports RAG patterns out of the box..."
         * */
        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                             .query(question)
                             .topK(topK)
                             .build()
        );

        if (relevantDocs.isEmpty()) {
            log.warn("No relevant documents found for question \"{}\"", question);
            return ChatResponse.builder()
                               .processingTimeMs(System.currentTimeMillis() - startTime)
                               .answer("No relevant information found to answer this question. Make sure you've uploaded documents related to your query.")
                               .sources(List.of())
                               .question(question)
                               .build();
        }

        List<String> sources = relevantDocs.stream()
                                           .map(Document::getText)
                                           .toList();

        String context = buildContext(sources);
        log.debug("Built context from {} chunks ({} chars)", sources.size(), context.length());

        String prompt = buildPrompt(context, question);
        log.debug("Prompt length: {} chars", prompt.length());

        log.debug("Sending prompt to LLM.");
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        String answer = chatClient.prompt()
                                  .user(prompt)
                                  .call()
                                  .content();

        if (answer == null) {
            log.info("No answer received for the question, \"{}\"", question);
            return ChatResponse.builder()
                               .processingTimeMs(System.currentTimeMillis() - startTime)
                               .answer("No answer received for the question.")
                               .sources(List.of())
                               .question(question)
                               .build();
        }

        log.info("LLM responded with answer ({} chars)", answer.length());

        long processingTime = System.currentTimeMillis() - startTime;

        return ChatResponse.builder()
                           .answer(answer)
                           .sources(sources)
                           .question(question)
                           .processingTimeMs(processingTime)
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
     *
     * @param context  The textual context to be used for answering the question.
     * @param question The question to be answered based on the provided context.
     * @return The formatted prompt string containing the context and the question.
     */
    private String buildPrompt(String context, String question) {
        return """
                You are a helpful AI assistant. Use the following pieces of context to answer the question.
                If you don't know the answer based on the context, just say that you don't know.
                Don't try to make up an answer.
                
                Context:
                %s
                
                Question: %s
                
                Answer:
                """.formatted(context, question);
    }

}
