package fr.quinquennat.backend.reaction;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Reactions of one simulation. {@code generator} is {@code "factice"} or {@code "ia"}, so the
 * interface can label the texts truthfully. Households without a change have no reaction.
 */
public record Reactions(
        @JsonProperty("actif") boolean enabled,
        @JsonProperty("generateur") String generator,
        @JsonProperty("reactions") List<Reaction> reactions) {
}
