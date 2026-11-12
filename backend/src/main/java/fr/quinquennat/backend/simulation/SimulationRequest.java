package fr.quinquennat.backend.simulation;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Lever id to chosen value, in the lever's display unit. Levers left out keep their current value. */
public record SimulationRequest(@JsonProperty("leviers") Map<String, Double> levers) {
}
