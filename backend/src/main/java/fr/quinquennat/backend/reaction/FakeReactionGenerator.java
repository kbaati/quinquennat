package fr.quinquennat.backend.reaction;

import java.text.NumberFormat;
import java.util.Locale;

import fr.quinquennat.backend.simulation.Simulation;

/** Deterministic template sentences, without any AI call. Used when no API key is configured. */
public class FakeReactionGenerator implements ReactionGenerator {

    /** Share of the disposable income from which a change counts as strong. */
    private static final double STRONG_CHANGE = 0.03;

    @Override
    public String kind() {
        return "factice";
    }

    @Override
    public String cacheKey() {
        return "factice-v1";
    }

    @Override
    public Reaction generate(Simulation.HouseholdResult household) {
        double before = household.before().getOrDefault("revenu_disponible", 0.0);
        double after = household.after().getOrDefault("revenu_disponible", 0.0);
        double delta = household.disposableIncomeDelta();
        int feeling = feeling(delta, before);
        String amount = euros(Math.abs(delta));
        String testimony = switch (feeling) {
            case 2 -> "Avec " + amount + " de plus par an, le budget respirerait nettement : on ferait face aux"
                    + " imprévus plus sereinement.";
            case 1 -> amount + " de plus par an, ce n'est pas énorme, mais ça aiderait à boucler certains mois.";
            case -1 -> amount + " de moins par an, on s'adapterait, mais il faudrait surveiller quelques dépenses.";
            case -2 -> "Avec " + amount + " de moins par an, il faudrait revoir sérieusement le budget et renoncer"
                    + " à certaines dépenses.";
            default -> "Pour nous, cela ne changerait presque rien au budget de l'année.";
        };
        String sign = delta > 0 ? "+" : "−";
        String reason = feeling == 0
                ? "Revenu disponible inchangé : " + euros(before) + " par an."
                : "Revenu disponible : " + euros(before) + " → " + euros(after) + " par an, soit " + sign + amount
                        + (before > 0 ? " (" + sign + percent(Math.abs(delta) / before) + ")" : "") + ".";
        return new Reaction(household.id(), feeling, testimony, reason);
    }

    static int feeling(double delta, double before) {
        if (Math.abs(delta) < 1) {
            return 0;
        }
        boolean strong = before <= 0 || Math.abs(delta) / before >= STRONG_CHANGE;
        return (int) Math.signum(delta) * (strong ? 2 : 1);
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
