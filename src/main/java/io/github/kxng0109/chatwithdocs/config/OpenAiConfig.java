package io.github.kxng0109.chatwithdocs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.ai.provider", havingValue = "openai")
public class OpenAiConfig {
    @Value("${spring.ai.openai.base-url:https://api.openai.com/}")
    private String openAiBaseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
    private String openAiChatModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double openAiTemperature;

    @Value("${spring.ai.openai.embedding.options.model:text-embedding-3-small}")
    private String openAiEmbeddingModel;

    // Using Duration instead of long because Spring Boot automatically converts string like
    // "30s", "1m", "2m30s" to a Duration object which allows for greater flexibility
    // Instead of us having to use Duration.ofSeconds() which limits it to seconds and it must be a long
    @Value("${spring.ai.model.timeout}")
    private Duration modelTimeout;

    @Bean
    @Primary
    public ChatModel chatModel() {
        log.info("Configuring OpenAI chat model with model: {}", openAiChatModel);
        log.debug("OpenAI base url: {}. OpenAI chat model temperature: {}",
                  openAiBaseUrl,
                  openAiTemperature
        );

        OpenAiApi openAiApi = OpenAiApi.builder()
                                       .baseUrl(openAiBaseUrl)
                                       .apiKey(openAiApiKey)
                                       .restClientBuilder(restClientBuilder())
                                       .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                                                     .model(openAiChatModel)
                                                     .temperature(openAiTemperature)
                                                     .build();

        return OpenAiChatModel.builder()
                              .openAiApi(openAiApi)
                              .defaultOptions(options)
                              .build();
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        log.info("Configuring OpenAI embedding model with model: {}", openAiEmbeddingModel);
        log.debug("OpenAI base url: {}", openAiBaseUrl);

        OpenAiApi openAiApi = OpenAiApi.builder()
                                       .baseUrl(openAiBaseUrl)
                                       .apiKey(openAiApiKey)
                                       .restClientBuilder(restClientBuilder())
                                       .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                                                               .model(openAiEmbeddingModel)
                                                               .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
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
