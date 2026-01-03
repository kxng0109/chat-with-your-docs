package io.github.kxng0109.chatwithdocs.repository;

import io.github.kxng0109.chatwithdocs.entity.ChatMessage;
import io.github.kxng0109.chatwithdocs.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    /**
     * Finds all messages for a session ordered by creation time.
     *
     * @param session the chat session
     * @return list of messages in chronological order
     */
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);

    /**
     * Finds the last N messages for a session (for context window).
     *
     * @param sessionId the session's UUID
     * @param limit     maximum number of messages to retrieve
     * @return list of recent messages
     */
    @Query(value = """
            SELECT * FROM chat_messages 
            WHERE session_id = (SELECT id FROM chat_sessions WHERE session_id = :sessionId)
            ORDER BY created_at DESC 
            LIMIT :limit
            """, nativeQuery = true)
    List<ChatMessage> findRecentMessages(@Param("sessionId") String sessionId, @Param("limit") int limit);

    /**
     * Counts messages in a session.
     *
     * @param session the chat session
     * @return number of messages
     */
    long countBySession(ChatSession session);
}
