package io.github.kxng0109.chatwithdocs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a chat session.
 * A chat session encapsulates a series of interactions (messages) and associated documents.
 */
@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", unique = true, nullable = false, updatable = false)
    private String sessionId;

    /**
     * Optional human-readable name for the session.
     * Example: "Research on Climate Change" or "Product Documentation Q&A"
     */
    @Column(name = "name")
    private String name;

    /**
     * An optional description providing more context about the session.
     * Example: "This session focuses on discussing the impacts of climate change based on recent studies."
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * All the documents uploaded to this session.
     * FYI, documents are isolated to this session and won't appear in other sessions' searches.
     * Read as "one session has many documents".
     */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SessionDocument> documents = new ArrayList<>();

    /**
     * All chat messages in this session.
     * This is just to keep track of the conversation history and maintain context for the session.
     * Read as "one session has many chat messages".
     */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * Timestamp when the session was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the session was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Generate a UUID for sessionId before persisting(basically before saving it) if it's not already set.
     * This ensures that every ChatSession has a unique sessionId.
     */
    @PrePersist
    public void prePersist() {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
    }

    /**
     * Convenience method to get the count of documents in this session.
     *
     * @return the number of documents associated with this session
     */
    public int getDocumentCount() {
        return documents != null ? documents.size() : 0;
    }

    /**
     * Convenience method to get the count of messages in this session.
     *
     * @return the number of messages associated with this session
     */
    public int getMessageCount() {
        return messages != null ? messages.size() : 0;
    }
}
