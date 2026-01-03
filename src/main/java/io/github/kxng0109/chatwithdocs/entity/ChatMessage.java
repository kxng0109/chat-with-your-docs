package io.github.kxng0109.chatwithdocs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a chat message within a chat session.
 * Each message is associated with a specific chat session and contains information about the sender's role and content.
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The session the message belongs to.
     * Read as "many messages belong to one session, don't fetch the session when I ask for the message".
     * "Map each message to its session using the session_id foreign key column."
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;


    /**
     * The role of the message sender (e.g., USER or ASSISTANT).
     * User asks the questions, assistant(LLM) provides the answers.
     */
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageRole role;

    /**
     * The actual content of the message.
     * It'll be a questions for the user role and generated answer for the assistant role.
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * The sources referenced in the assistant's response.
     * This field is only applicable for messages with the ASSISTANT role.
     * It contains information about the documents or data used to generate the response.
     * For messages with the USER role, this field will be null.
     */
    @Column(name = "sources", columnDefinition = "TEXT")
    private String sources;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Enum representing the role of the message sender.
     */
    public enum MessageRole {
        USER,
        ASSISTANT
    }
}
