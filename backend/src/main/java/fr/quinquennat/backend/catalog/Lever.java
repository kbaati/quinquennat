package fr.quinquennat.backend.catalog;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A policy lever the public can move. Values are expressed in the display unit; the OpenFisca
 * value is {@code value * factor} (e.g. 11 % is stored as 0.11).
 */
public record Lever(
        @JsonProperty("id") String id,
        @JsonProperty("groupe") String group,
        @JsonProperty("libelle") String label,
        @JsonProperty("description") String description,
        @JsonProperty("unite") String unit,
        @JsonProperty("valeurActuelle") double currentValue,
        @JsonProperty("min") double min,
        @JsonProperty("max") double max,
        @JsonProperty("pas") double step,
        @JsonProperty("facteur") double factor,
        @JsonProperty("parametre") String parameter) {

    public double toEngineValue(double value) {
        return BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(factor)).doubleValue();
    }
}
