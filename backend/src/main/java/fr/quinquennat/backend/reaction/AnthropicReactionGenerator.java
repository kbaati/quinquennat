package fr.quinquennat.backend.reaction;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import fr.quinquennat.backend.simulation.Simulation;

/** Asks Claude for one testimonial, as structured output. Model and key come from the environment. */
public class AnthropicReactionGenerator implements ReactionGenerator {

    /** Room for the model's reasoning; the answer itself is a few dozen words. */
    private static final long MAX_TOKENS = 4096;

    /** Shape of the structured output; field names are part of the prompt, hence in French. */
    public record Output(
            @JsonPropertyDescription("Entier de -2 (très défavorable pour le budget du foyer) à 2 (très favorable)")
            int ressenti,
            @JsonPropertyDescription("Témoignage fictif à la première personne, 40 mots au maximum")
            String temoignage,
            @JsonPropertyDescription("Une phrase factuelle reliant les montants au ressenti")
            String raison) {
    }

    private final AnthropicClient client;
    private final String model;

    public AnthropicReactionGenerator(AnthropicClient client, String model) {
        this.client = client;
        this.model = model;
    }

    @Override
    public String kind() {
        return "ia";
    }

    @Override
    public String cacheKey() {
        return "ia-v1:" + model;
    }

    @Override
    public Reaction generate(Simulation.HouseholdResult household) {
        StructuredMessageCreateParams<Output> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(ReactionPrompt.SYSTEM)
                .addUserMessage(ReactionPrompt.userMessage(household))
                .outputConfig(Output.class)
                .build();

        StructuredMessage<Output> message = client.messages().create(params);
        StopReason stopReason = message.stopReason().orElse(null);
        if (!StopReason.END_TURN.equals(stopReason)) {
            // Refusal or truncated output: the JSON may be missing or incomplete.
            throw new IllegalStateException("No usable testimonial, stop reason: " + stopReason);
        }
        Output output = message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No text block in the model answer"));
        return new Reaction(household.id(), output.ressenti(), output.temoignage(), output.raison());
    }
}
