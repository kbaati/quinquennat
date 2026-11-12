package fr.quinquennat.backend.reaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import fr.quinquennat.backend.simulation.Simulation;

class ReactionServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC);

    private final Map<String, Reaction> cache = new ConcurrentHashMap<>();
    private final ReactionStore store = new ReactionStore() {
        @Override
        public Optional<Reaction> find(String fingerprint) {
            return Optional.ofNullable(cache.get(fingerprint));
        }

        @Override
        public void save(String fingerprint, Reaction reaction) {
            cache.put(fingerprint, reaction);
        }
    };
    private final AtomicInteger calls = new AtomicInteger();

    private ReactionGenerator generator(Function<Simulation.HouseholdResult, Reaction> body) {
        return new ReactionGenerator() {
            @Override
            public String kind() {
                return "factice";
            }

            @Override
            public String cacheKey() {
                return "test";
            }

            @Override
            public Reaction generate(Simulation.HouseholdResult household) {
                calls.incrementAndGet();
                return body.apply(household);
            }
        };
    }

    private ReactionService service(ReactionGenerator generator) {
        return new ReactionService(true, 100, generator, store, CLOCK);
    }

    private static Simulation.HouseholdResult household(String id, double delta) {
        Map<String, Double> before = new HashMap<>(Map.of("revenu_disponible", 20_000.0));
        Map<String, Double> after = new HashMap<>(Map.of("revenu_disponible", 20_000.0 + delta));
        return new Simulation.HouseholdResult(id, "Foyer " + id, "Description.", before, after, delta);
    }

    private static Simulation simulation(double leverValue, Simulation.HouseholdResult... households) {
        return new Simulation("abcd2345", Instant.parse("2026-11-12T10:00:00Z"), 2025,
                List.of(new Simulation.ProgramItem("ir-taux-tranche-1", "Impôt", "Taux", "%", 11, leverValue, "x")),
                new Simulation.Versions("44.7.1", "176.1.10"), List.of(households));
    }

    @Test
    void generatesOneReactionPerChangedHousehold() {
        ReactionService service = service(new FakeReactionGenerator());

        Reactions reactions = service.forSimulation(
                simulation(15, household("a", -900), household("b", 0), household("c", 120)));

        assertThat(reactions.enabled()).isTrue();
        assertThat(reactions.generator()).isEqualTo("factice");
        assertThat(reactions.reactions()).extracting(Reaction::householdId).containsExactly("a", "c");
        assertThat(reactions.reactions()).extracting(Reaction::feeling).containsExactly(-2, 1);
    }

    @Test
    void sameProgrammeAndHouseholdIsServedFromTheCache() {
        ReactionService service = service(generator(h -> new Reaction(h.id(), 1, "Un peu mieux.", "Hausse.")));

        service.forSimulation(simulation(15, household("a", 100)));
        service.forSimulation(simulation(15, household("a", 100)));
        assertThat(calls).hasValue(1);

        service.forSimulation(simulation(16, household("a", 100)));
        assertThat(calls).hasValue(2);
    }

    @Test
    void disabledSwitchGeneratesNothing() {
        ReactionService service = new ReactionService(false, 100,
                generator(h -> new Reaction(h.id(), 1, "Texte.", "Raison.")), store, CLOCK);

        Reactions reactions = service.forSimulation(simulation(15, household("a", 100)));

        assertThat(reactions.enabled()).isFalse();
        assertThat(reactions.reactions()).isEmpty();
        assertThat(calls).hasValue(0);
    }

    @Test
    void failedOrInvalidGenerationOnlySkipsThatHousehold() {
        ReactionService service = service(generator(h -> switch (h.id()) {
            case "a" -> throw new IllegalStateException("model unavailable");
            case "b" -> new Reaction("b", 5, "Hors bornes.", "Raison.");
            default -> new Reaction(h.id(), -1, "Un peu moins.", "Baisse.");
        }));

        Reactions reactions = service.forSimulation(
                simulation(15, household("a", -100), household("b", -100), household("c", -100)));

        assertThat(reactions.reactions()).extracting(Reaction::householdId).containsExactly("c");
        assertThat(cache).hasSize(1); // failures are not cached, so they are retried later
    }

    @Test
    void testimonyIsCutAtFortyWords() {
        String tooLong = "mot ".repeat(60);
        ReactionService service = service(generator(h -> new Reaction(h.id(), 1, tooLong, "Raison.")));

        Reaction reaction = service.forSimulation(simulation(15, household("a", 100))).reactions().getFirst();

        assertThat(reaction.testimony().split(" ")).hasSize(ReactionService.MAX_WORDS);
        assertThat(reaction.testimony()).endsWith("…");
    }

    @Test
    void dailyBudgetCapsGenerations() {
        ReactionService service = new ReactionService(true, 2,
                generator(h -> new Reaction(h.id(), 1, "Texte.", "Raison.")), store, CLOCK);

        Reactions reactions = service.forSimulation(
                simulation(15, household("a", 100), household("b", 100), household("c", 100)));

        assertThat(reactions.reactions()).hasSize(2);
        assertThat(calls).hasValue(2);
    }
}
