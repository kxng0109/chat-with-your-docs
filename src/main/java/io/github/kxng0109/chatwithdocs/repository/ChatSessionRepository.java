package io.github.kxng0109.chatwithdocs.repository;

import io.github.kxng0109.chatwithdocs.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    /**
     * Find a chat session by using its unique session ID (UUID).
     * Needed because .findById() uses the internal database ID (Long), but we need to use the UUID which it won't help with.
     *
     * @param sessionId the UUID of the chat session
     * @return Optional containing the chat session if found, otherwise empty
     */
    Optional<ChatSession> findBySessionId(String sessionId);

    /**
     * Checks if a chat session exists by its session ID.
     *
     * @param sessionId the UUID of the chat session
     * @return true if the chat session exists, false otherwise
     */
    boolean existsBySessionId(String sessionId);

    /**
     * Retrieves all chat session but orders by creation date (newest first)
     *
     * @return list of all chat sessions in descending order of creation date
     */
    List<ChatSession> findAllByOrderByCreatedAtDescUpdatedAtDesc();

    /**
     * Retrieves all chat sessions that have at least one associated document.
     *
     * @return list of chat sessions with documents
     */
    @Query("SELECT DISTINCT s FROM ChatSession s JOIN s.documents d")
    List<ChatSession> findSessionsWithDocuments();

    /**
     * Deletes a chat session by its session ID.
     *
     * @param sessionId the UUID of the chat session to be deleted
     */
    void deleteBySessionId(String sessionId);
}