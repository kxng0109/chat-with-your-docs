package io.github.kxng0109.chatwithdocs.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "Question can not be empty")
        String question,

        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 20, message = "topK cannot exceed 20")
        Integer topK
) {
    public ChatRequest{
        if(topK == null) topK = 5;
    }
}
