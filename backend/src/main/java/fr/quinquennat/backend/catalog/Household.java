package fr.quinquennat.backend.catalog;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A typical household: a neutral name, a one-sentence description and its OpenFisca situation. */
public record Household(
        @JsonProperty("id") String id,
        @JsonProperty("nom") String name,
        @JsonProperty("description") String description,
        @JsonProperty("situation") Map<String, Object> situation) {
}
