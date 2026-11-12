package fr.quinquennat.backend.reaction;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import fr.quinquennat.backend.simulation.Simulation;

/**
 * The whole prompt sent to the model. It describes the household and its change in euros, and
 * nothing else: the programme itself, its levers and any political context are deliberately absent.
 */
final class ReactionPrompt {

    static final String SYSTEM = """
            Tu écris de courts témoignages fictifs pour un outil pédagogique qui montre l'effet de \
            changements fiscaux et sociaux sur le budget de foyers types. Les lecteurs savent que ces \
            textes sont fictifs et générés par IA.

            On te décrit un foyer type et l'évolution de son budget annuel en euros. Écris ce qu'une \
            personne de ce foyer pourrait dire de cet écart : à la première personne, en français \
            courant, en 40 mots au maximum. Le témoignage parle uniquement du budget du foyer, de ce \
            que cette somme représente concrètement dans son quotidien, à la mesure de ses revenus.

            Cet outil est strictement neutre. Le témoignage ne mentionne donc aucun parti, aucun \
            candidat, aucune personnalité, aucun gouvernement, aucune élection ; il n'exprime aucune \
            opinion politique ni intention de vote, ne juge pas la mesure elle-même, et ne nomme \
            aucune personne réelle.

            Champs attendus :
            - ressenti : entier de -2 à 2. -2 = très défavorable pour le budget du foyer, \
            2 = très favorable, 0 = effet négligeable.
            - temoignage : le témoignage, 40 mots au maximum.
            - raison : une phrase factuelle, à la troisième personne, qui relie les montants au ressenti.
            """;

    private static final Map<String, String> AMOUNT_LABELS = Map.of(
            "impot_revenu", "Impôt sur le revenu",
            "csg", "CSG",
            "rsa", "RSA",
            "prime_activite", "Prime d'activité",
            "allocations_familiales", "Allocations familiales",
            "aides_logement", "Aides au logement");

    private ReactionPrompt() {
    }

    static String userMessage(Simulation.HouseholdResult household) {
        double before = household.before().getOrDefault("revenu_disponible", 0.0);
        double after = household.after().getOrDefault("revenu_disponible", 0.0);
        double delta = household.disposableIncomeDelta();
        StringBuilder message = new StringBuilder();
        message.append("Foyer type : ").append(household.name()).append(". ").append(household.description())
                .append('\n');
        message.append("Revenu disponible annuel : ").append(euros(before)).append(" avant, ").append(euros(after))
                .append(" après, soit ").append(delta > 0 ? "+" : "−").append(euros(Math.abs(delta)))
                .append(" par an");
        if (before > 0) {
            message.append(" (").append(percent(Math.abs(delta) / before)).append(" du revenu disponible)");
        }
        message.append(".\n");
        StringBuilder changes = new StringBuilder();
        household.before().keySet().stream().sorted().forEach(name -> {
            Double from = household.before().get(name);
            Double to = household.after().get(name);
            String label = AMOUNT_LABELS.get(name);
            if (label != null && from != null && to != null && Math.round(from) != Math.round(to)) {
                changes.append("- ").append(label).append(" : ").append(euros(from)).append(" → ").append(euros(to))
                        .append(" par an\n");
            }
        });
        if (!changes.isEmpty()) {
            message.append("Montants annuels qui changent pour ce foyer :\n").append(changes);
        }
        return message.toString();
    }

    private static String euros(double amount) {
        return NumberFormat.getIntegerInstance(Locale.FRANCE).format(Math.round(amount)) + " €";
    }

    private static String percent(double ratio) {
        NumberFormat format = NumberFormat.getPercentInstance(Locale.FRANCE);
        format.setMaximumFractionDigits(1);
        return format.format(ratio);
    }
}
