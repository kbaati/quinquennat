package fr.quinquennat.backend.reaction;

import fr.quinquennat.backend.simulation.Simulation;

/**
 * Writes a fictional testimonial from a household and its change in euros, and nothing else: no
 * party, no candidate, no real person, no political opinion.
 */
public interface ReactionGenerator {

    /** Shown to the user so the label says how the text was produced: {@code "factice"} or {@code "ia"}. */
    String kind();

    /** Distinguishes cached reactions of different generators or models. */
    String cacheKey();

    /** May throw: the caller then shows no testimonial for this household. */
    Reaction generate(Simulation.HouseholdResult household);
}
