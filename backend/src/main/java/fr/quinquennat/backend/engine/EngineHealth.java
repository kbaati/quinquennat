package fr.quinquennat.backend.engine;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Versions reported by the Python engine on {@code GET /sante}. */
public record EngineHealth(
        @JsonProperty("status") String status,
        @JsonProperty("openfisca_core") String openfiscaCore,
        @JsonProperty("openfisca_france") String openfiscaFrance) {
}
