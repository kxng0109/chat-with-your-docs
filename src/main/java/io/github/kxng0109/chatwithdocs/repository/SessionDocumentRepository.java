package io.github.kxng0109.chatwithdocs.repository;

import io.github.kxng0109.chatwithdocs.entity.SessionDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionDocumentRepository extends JpaRepository<SessionDocument, Long> {
//    /**
//     * Find all documents associated with a specific chat session.
//     *
//     * @param session the chat session
//     * @return list of documents in a chat session
//     */
//    List<SessionDocument> findBySession(ChatSession session);
//
//    /**
//     * Count documents in a specific chat session.
//     *
//     * @param session the chat session
//     * @return number of documents in the chat session
//     */
//    long countBySession(ChatSession session);
//
//    /**
//     * Find documents by their processing status.
//     *
//     * @param status the document status
//     * @return list of documents with the specified status
//     */
//    List<SessionDocument> findByStatus(SessionDocument.DocumentStatus status);
}
