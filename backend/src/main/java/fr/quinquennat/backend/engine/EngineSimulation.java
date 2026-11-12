package fr.quinquennat.backend.engine;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Answer of the engine's {@code POST /simuler}: yearly amounts in euros, before and after the reform. */
public record EngineSimulation(
        @JsonProperty("annee") int year,
        @JsonProperty("versions") Versions versions,
        @JsonProperty("foyers") List<HouseholdAmounts> households) {

    public record Versions(
            @JsonProperty("openfisca_core") String openfiscaCore,
            @JsonProperty("openfisca_france") String openfiscaFrance) {
    }

    public record HouseholdAmounts(
            @JsonProperty("id") String id,
            @JsonProperty("avant") Map<String, Double> before,
            @JsonProperty("apres") Map<String, Double> after) {
    }
}
