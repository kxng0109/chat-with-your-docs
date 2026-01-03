package io.github.kxng0109.chatwithdocs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "spring.ai.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaConfig {
    @Value("${spring.ai.ollama.base-url: http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.options.model:llama3.2:1b}")
    private String ollamaChatModel;

    @Value("${spring.ai.ollama.chat.options.temperature:0.7}")
    private Double ollamaTemperature;

    @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text:v1.5}")
    private String ollamaEmbeddingModel;

    // Using Duration instead of long because Spring Boot automatically converts string like
    // "30s", "1m", "2m30s" to a Duration object which allows for greater flexibility
    // Instead of us having to use Duration.ofSeconds() which limits it to seconds and it must be a long
    @Value("${spring.ai.model.timeout}")
    private Duration modelTimeout;

    @Bean
    @Primary
    public ChatModel chatModel() {
        log.info("Configuring Ollama chat model with model: {}", ollamaChatModel);
        log.debug("Ollama base url: {}. Ollama chat model temperature: {}",
                  ollamaBaseUrl,
                  ollamaTemperature
        );
        OllamaApi ollamaApi = OllamaApi.builder()
                                       .baseUrl(ollamaBaseUrl)
                                       .restClientBuilder(restClientBuilder())
                                       .build();

        OllamaOptions ollamaOptions = OllamaOptions.builder()
                                                   .model(ollamaChatModel)
                                                   .temperature(ollamaTemperature)
                                                   .build();

        return OllamaChatModel.builder()
                              .ollamaApi(ollamaApi)
                              .defaultOptions(ollamaOptions)
                              .build();
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        log.info("Configuring Ollama embedding model with model: {}", ollamaEmbeddingModel);
        log.debug("Ollama base url: {}", ollamaBaseUrl);

        OllamaApi ollamaApi = OllamaApi.builder()
                                       .baseUrl(ollamaBaseUrl)
                                       .restClientBuilder(restClientBuilder())
                                       .build();

        OllamaOptions ollamaOptions = OllamaOptions.builder()
                                                   .model(ollamaEmbeddingModel)
                                                   .build();

        return OllamaEmbeddingModel.builder()
                                   .ollamaApi(ollamaApi)
                                   .defaultOptions(ollamaOptions)
                                   .build();
    }


    private RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                                          .connectTimeout(modelTimeout)
                                          .build();

        return RestClient.builder()
                         .requestFactory(
                                 new JdkClientHttpRequestFactory(
                                         httpClient
                                 )
                         );
    }
}
