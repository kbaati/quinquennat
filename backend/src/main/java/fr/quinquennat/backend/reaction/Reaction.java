package fr.quinquennat.backend.reaction;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A fictional testimonial for one household. {@code feeling} goes from -2 (very unfavourable for the
 * household budget) to +2 (very favourable). It is never a voting intention nor an opinion poll.
 */
public record Reaction(
        @JsonProperty("foyer") String householdId,
        @JsonProperty("ressenti") int feeling,
        @JsonProperty("temoignage") String testimony,
        @JsonProperty("raison") String reason) {
}
