package fr.quinquennat.backend.reaction;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;

/** Picks the generator from {@code reactions.generator}: {@code fake} (default) or {@code anthropic}. */
@Configuration
public class ReactionConfiguration {

    @Bean
    @ConditionalOnProperty(name = "reactions.generator", havingValue = "fake", matchIfMissing = true)
    ReactionGenerator fakeReactionGenerator() {
        return new FakeReactionGenerator();
    }

    @Bean
    @ConditionalOnProperty(name = "reactions.generator", havingValue = "anthropic")
    ReactionGenerator anthropicReactionGenerator(
            @Value("${reactions.anthropic.api-key:}") String apiKey,
            @Value("${reactions.anthropic.model:}") String model) {
        if (apiKey.isBlank() || model.isBlank()) {
            throw new IllegalStateException(
                    "REACTIONS_GENERATOR=anthropic requires both ANTHROPIC_API_KEY and ANTHROPIC_MODEL");
        }
        return new AnthropicReactionGenerator(
                AnthropicOkHttpClient.builder().apiKey(apiKey).timeout(Duration.ofSeconds(60)).build(), model);
    }
}
