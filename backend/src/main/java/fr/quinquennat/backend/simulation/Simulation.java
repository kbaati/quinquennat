package fr.quinquennat.backend.simulation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A stored, shareable simulation: the programme, the engine versions and the result per household. */
public record Simulation(
        @JsonProperty("id") String id,
        @JsonProperty("creeLe") Instant createdAt,
        @JsonProperty("annee") int year,
        @JsonProperty("programme") List<ProgramItem> program,
        @JsonProperty("versions") Versions versions,
        @JsonProperty("foyers") List<HouseholdResult> households) {

    /** A lever moved away from its current value. */
    public record ProgramItem(
            @JsonProperty("levier") String leverId,
            @JsonProperty("groupe") String group,
            @JsonProperty("libelle") String label,
            @JsonProperty("unite") String unit,
            @JsonProperty("valeurActuelle") double currentValue,
            @JsonProperty("valeur") double value,
            @JsonProperty("parametre") String parameter) {
    }

    public record Versions(
            @JsonProperty("openfiscaCore") String openfiscaCore,
            @JsonProperty("openfiscaFrance") String openfiscaFrance) {
    }

    /** Yearly amounts in euros; {@code disposableIncomeDelta} is after minus before. */
    public record HouseholdResult(
            @JsonProperty("id") String id,
            @JsonProperty("nom") String name,
            @JsonProperty("description") String description,
            @JsonProperty("avant") Map<String, Double> before,
            @JsonProperty("apres") Map<String, Double> after,
            @JsonProperty("ecartRevenuDisponible") double disposableIncomeDelta) {
    }
}
