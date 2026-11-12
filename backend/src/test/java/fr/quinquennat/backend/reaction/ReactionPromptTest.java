package fr.quinquennat.backend.reaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.quinquennat.backend.simulation.Simulation;

class ReactionPromptTest {

    private final Simulation.HouseholdResult household = new Simulation.HouseholdResult(
            "seul-smic", "Personne seule au SMIC", "Salariée à temps plein, locataire.",
            Map.of("revenu_disponible", 20_000.0, "impot_revenu", 0.0, "rsa", 0.0),
            Map.of("revenu_disponible", 19_700.0, "impot_revenu", 300.0, "rsa", 0.0),
            -300);

    @Test
    void userMessageDescribesTheHouseholdAndItsEuros() {
        String message = ReactionPrompt.userMessage(household).replace(' ', ' ');

        assertThat(message).contains("Personne seule au SMIC. Salariée à temps plein, locataire.");
        assertThat(message).contains("20 000 € avant, 19 700 € après, soit −300 € par an");
        assertThat(message).contains("- Impôt sur le revenu : 0 € → 300 € par an");
        assertThat(message).doesNotContain("RSA"); // unchanged amounts are left out
    }

    @Test
    void userMessageCarriesNothingAboutTheProgrammeOrPolitics() {
        String message = ReactionPrompt.userMessage(household).toLowerCase();

        assertThat(message).doesNotContain("programme", "levier", "tranche", "parti", "candidat", "vote");
    }

    @Test
    void systemPromptStatesTheNeutralityRulesAndTheOutputContract() {
        assertThat(ReactionPrompt.SYSTEM)
                .contains("aucun parti", "aucun candidat", "aucune opinion politique", "aucune personne réelle")
                .contains("40 mots", "ressenti", "temoignage", "raison");
    }
}
