package io.github.kxng0109.chatwithdocs.model;

import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Request DTO for creating a new chat session.
 * Both name and description are optional, sessions can be created with just defaults.
 *
 * @param name        Optional human-readable name for the session (max 255 characters)
 * @param description Optional longer description of the session's purpose (max 1000 characters)
 */
@Builder
public record SessionCreateRequest(
        @Size(max = 255, message = "Name cannot exceed 255 characters")
        String name,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description
) {
}
