package io.github.kxng0109.chatwithdocs.service;

import io.github.kxng0109.chatwithdocs.entity.ChatSession;
import io.github.kxng0109.chatwithdocs.entity.SessionDocument;
import io.github.kxng0109.chatwithdocs.exception.SessionNotFoundException;
import io.github.kxng0109.chatwithdocs.model.DocumentSummary;
import io.github.kxng0109.chatwithdocs.model.SessionCreateRequest;
import io.github.kxng0109.chatwithdocs.model.SessionResponse;
import io.github.kxng0109.chatwithdocs.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing chat sessions. This class provides methods to create,
 * retrieve, list, and delete chat sessions, as well as mapping entities to
 * response DTOs.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionService {
    private final ChatSessionRepository sessionRepository;
    private final VectorStore vectorStore;

    /**
     * Creates a new chat session based on the provided request.
     * Since it's a new session, it won't have any documents or messages yet.
     *
     * @param request the session creation request containing optional name and description
     * @return the created session's response DTO
     */
    @Transactional
    public SessionResponse createSession(SessionCreateRequest request) {
        if (request == null) {
            throw new NullPointerException("Error occurred creating chat session: Request object is empty");
        }

        String sessionName = request.name() != null ? request.name() : "New Chat Session";
        log.info("Creating new chat session with name: {}", sessionName);

        ChatSession session = ChatSession.builder()
                                         .name(sessionName)
                                         .description(request.description())
                                         .build();

        session = sessionRepository.save(session);
        log.info("Created session with ID: {}", session.getSessionId());

        return mapToResponse(session, false);
    }

    /**
     * Retrieves session information by session ID.
     *
     * @param sessionId        the UUID of the chat session
     * @param includeDocuments whether to include document summaries in the response
     * @return the session response DTO
     * @throws SessionNotFoundException if the session with the given ID does not exist
     */

    @Transactional(readOnly = true)
    public SessionResponse getSessionInfo(String sessionId, boolean includeDocuments) {
        log.debug("Fetching session info for sessionId: {}, includeDocuments: {}", sessionId, includeDocuments);

        ChatSession session = sessionRepository.findBySessionId(sessionId)
                                               .orElseThrow(
                                                       () -> new SessionNotFoundException(sessionId)
                                               );

        return mapToResponse(session, includeDocuments);
    }

    /**
     * Retrieves the ChatSession entity by session ID.
     *
     * @param sessionId the UUID of the chat session
     * @return the ChatSession entity
     * @throws SessionNotFoundException if the session with the given ID does not exist
     */
    @Transactional(readOnly = true)
    public ChatSession getSessionEntity(String sessionId) {
        log.debug("Fetching session entity for sessionId: {}", sessionId);

        return sessionRepository.findBySessionId(sessionId)
                                .orElseThrow(
                                        () -> new SessionNotFoundException(sessionId)
                                );
    }

    /**
     * Lists all chat sessions.
     *
     * @param includeDocuments whether to include document summaries in the response
     * @return list of session response DTOs
     */
    @Transactional(readOnly = true)
    public List<SessionResponse> listAllSessions(boolean includeDocuments) {
        log.debug("Fetching all chat sessions, includeDocuments: {}", includeDocuments);

        List<ChatSession> sessions = sessionRepository.findAllByOrderByCreatedAtDescUpdatedAtDesc();

        return sessions.stream()
                       .map(session -> mapToResponse(session, includeDocuments))
                       .toList();
    }

    /**
     * Deletes a chat session by its session ID.
     * This also removes all associated documents and messages.
     *
     * @param sessionId the UUID of the chat session to be deleted
     * @throws SessionNotFoundException if the session with the given ID does not exist
     */
    @Transactional
    public void deleteSession(String sessionId) {
        log.info("Deleting session with sessionId: {}", sessionId);

        ChatSession session = sessionRepository.findBySessionId(sessionId)
                                               .orElseThrow(() -> new SessionNotFoundException(sessionId));

        deleteSessionVectors(sessionId);

        sessionRepository.delete(session);
        log.info("Deleted session with sessionId: {}", sessionId);
    }

    /**
     * Deletes all vector embeddings associated with a session.
     * Uses metadata filtering to find and remove all vectors with matching sessionId.
     *
     * @param sessionId The session UUID
     */
    private void deleteSessionVectors(String sessionId) {
        log.debug("Deleting vectors for session: {}", sessionId);

        try {
            // Build filter expression to match sessionId in metadata
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            var filterExpression = builder.eq("sessionId", sessionId).build();

            // Delete using filter expression
            vectorStore.delete(filterExpression);
            log.info("Successfully deleted vector embeddings for session: {}", sessionId);
        } catch (UnsupportedOperationException e) {
            // Fallback:  Some vector store implementations may not support filter delete
            log.warn("Filter based delete not supported, attempting alternative method for session:  {}", sessionId);
            deleteSessionVectorsFallback(sessionId);
        } catch (Exception e) {
            log.error("Failed to delete vectors for session {}: {}", sessionId, e.getMessage(), e);
            // Continue with session deletion even if vector deletion fails
            // The orphaned vectors won't be searchable since session metadata won't match
        }
    }

    /**
     * Fallback method for deleting session vectors when filter delete is not supported.
     * Uses string based filter expression.
     *
     * @param sessionId The session UUID
     */
    private void deleteSessionVectorsFallback(String sessionId) {
        try {
            // Try string based filter expression
            String filterExpression = "sessionId == '" + sessionId + "'";
            vectorStore.delete(filterExpression);
            log.info("Successfully deleted vectors using fallback method for session: {}", sessionId);
        } catch (Exception e) {
            log.warn("Fallback vector deletion also failed for session {}: {}", sessionId, e.getMessage());
        }
    }


    /**
     * Maps a SessionDocument entity to a DocumentSummary DTO.
     *
     * @param document the SessionDocument entity
     * @return the corresponding DocumentSummary DTO
     */
    private DocumentSummary mapToDocumentSummary(SessionDocument document) {
        return DocumentSummary.builder()
                              .id(document.getId())
                              .fileName(document.getOriginalFilename())
                              .contentType(document.getContentType())
                              .fileSize(document.getFileSize())
                              .chunkCount(document.getChunkCount())
                              .status(document.getStatus().name())
                              .uploadedAt(document.getCreatedAt())
                              .build();
    }

    /**
     * Maps a ChatSession entity to a SessionResponse DTO.
     *
     * @param session          the ChatSession entity
     * @param includeDocuments whether to include document summaries in the response. Sometimes we might just want to return the session metadata without the potentially large list of documents.
     * @return the corresponding SessionResponse DTO
     */

    private SessionResponse mapToResponse(ChatSession session, boolean includeDocuments) {
        List<DocumentSummary> documentSummaries = null;

        if (includeDocuments && session.getDocuments() != null) {
            documentSummaries = session.getDocuments().stream()
                                       .map(this::mapToDocumentSummary)
                                       .toList();
        }

        return SessionResponse.builder()
                              .sessionId(session.getSessionId())
                              .name(session.getName())
                              .description(session.getDescription())
                              .documents(documentSummaries)
                              .documentCount(session.getDocuments().size())
                              .messageCount(session.getMessages().size())
                              .createdAt(session.getCreatedAt())
                              .updatedAt(session.getUpdatedAt())
                              .build();
    }
}
