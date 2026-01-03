package io.github.kxng0109.chatwithdocs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.NoopApiKey;
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
@ConditionalOnProperty(name = "spring.ai.provider", havingValue = "lmstudio")
public class LMStudioConfig {

    @Value("${spring.ai.openai.base-url:http://localhost:1234}")
    private String lmStudioBaseUrl;

    @Value("${spring.ai.openai.chat.options.model:}")
    private String lmStudioChatModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double lmStudioTemperature;

    @Value("${spring.ai.openai.embedding.options.model:text-embedding-nomic-embed-text-v2-moe}")
    private String lmStudioEmbeddingModel;

    // Using Duration instead of long because Spring Boot automatically converts string like
    // "30s", "1m", "2m30s" to a Duration object which allows for greater flexibility
    // Instead of us having to use Duration.ofSeconds() which limits it to seconds and it must be a long
    @Value("${spring.ai.model.timeout}")
    private Duration modelTimeout;

    @Bean
    @Primary
    public ChatModel chatModel() {
        log.info("Configuring LMStudio chat model with model: {}", lmStudioChatModel);
        log.debug("LMStudio base url: {}. LMStudio chat model temperature: {}",
                  lmStudioBaseUrl,
                  lmStudioTemperature
        );

        OpenAiApi lmStudioApi = OpenAiApi.builder()
                                         .baseUrl(lmStudioBaseUrl)
                                         .apiKey(new NoopApiKey())
                                         .restClientBuilder(restClientBuilder())
                                         .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                                                     .model(lmStudioChatModel)
                                                     .temperature(lmStudioTemperature)
                                                     .build();

        return OpenAiChatModel.builder()
                              .openAiApi(lmStudioApi)
                              .defaultOptions(options)
                              .build();
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        log.info("Configuring LMStudio embedding model with model: {}", lmStudioEmbeddingModel);
        log.debug("LMStudio base url: {}", lmStudioBaseUrl);

        OpenAiApi lmStudioApi = OpenAiApi.builder()
                                         .baseUrl(lmStudioBaseUrl)
                                         .apiKey(new NoopApiKey())
                                         .restClientBuilder(restClientBuilder())
                                         .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                                                               .model(lmStudioEmbeddingModel)
                                                               .build();

        return new OpenAiEmbeddingModel(lmStudioApi, MetadataMode.EMBED, options);
    }

    private RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                                          // Force HTTP/1.1 for non-streaming
                                          .version(HttpClient.Version.HTTP_1_1)
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
