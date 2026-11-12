package fr.quinquennat.backend.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.quinquennat.backend.catalog.Catalog;
import fr.quinquennat.backend.engine.FakeEngineClient;
import tools.jackson.databind.json.JsonMapper;

class SimulationServiceTest {

    private static final String IR_RATE_1 = "impot_revenu.bareme_ir_depuis_1945.bareme.brackets[1].rate";

    private final Catalog catalog = new Catalog(JsonMapper.builder().build());
    private final FakeEngineClient engine = new FakeEngineClient();
    private final Map<String, Simulation> stored = new HashMap<>();
    private final SimulationStore store = new SimulationStore() {
        @Override
        public void save(Simulation simulation) {
            stored.put(simulation.id(), simulation);
        }

        @Override
        public Optional<Simulation> find(String id) {
            return Optional.ofNullable(stored.get(id));
        }
    };
    private final SimulationService service = new SimulationService(catalog, engine, store,
            Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void catalogueHasTheExpectedShape() {
        assertThat(catalog.levers()).hasSizeBetween(5, 10);
        assertThat(catalog.households()).hasSizeGreaterThanOrEqualTo(20);
        assertThat(catalog.year()).isEqualTo(2025);
    }

    @Test
    void emptyProgramSendsNoReformAndYieldsNoDelta() {
        Simulation simulation = service.create(new SimulationRequest(Map.of()));

        assertThat(engine.lastReform).isEmpty();
        assertThat(simulation.program()).isEmpty();
        assertThat(simulation.households()).hasSameSizeAs(catalog.households());
        assertThat(simulation.households()).allSatisfy(h -> assertThat(h.disposableIncomeDelta()).isZero());
    }

    @Test
    void convertsPercentagesToRatesForTheEngine() {
        Simulation simulation = service.create(new SimulationRequest(Map.of("ir-taux-tranche-1", 14.5)));

        assertThat(engine.lastReform).containsExactly(Map.entry(IR_RATE_1, 0.145));
        assertThat(simulation.program()).singleElement().satisfies(item -> {
            assertThat(item.leverId()).isEqualTo("ir-taux-tranche-1");
            assertThat(item.currentValue()).isEqualTo(11);
            assertThat(item.value()).isEqualTo(14.5);
            assertThat(item.parameter()).isEqualTo(IR_RATE_1);
        });
    }

    @Test
    void leversLeftAtTheirCurrentValueAreNotPartOfTheReform() {
        Simulation simulation = service.create(new SimulationRequest(Map.of("ir-taux-tranche-1", 11.0)));

        assertThat(engine.lastReform).isEmpty();
        assertThat(simulation.program()).isEmpty();
    }

    @Test
    void reportsDeltaAndKeepsHouseholdDescriptions() {
        engine.delta = -350.25;

        Simulation simulation = service.create(new SimulationRequest(Map.of("rsa-montant-forfaitaire", 700.0)));

        Simulation.HouseholdResult first = simulation.households().getFirst();
        assertThat(first.id()).isEqualTo(catalog.households().getFirst().id());
        assertThat(first.name()).isEqualTo(catalog.households().getFirst().name());
        assertThat(first.disposableIncomeDelta()).isEqualTo(-350.25);
        assertThat(simulation.versions()).isEqualTo(new Simulation.Versions("44.7.1", "176.1.10"));
        assertThat(simulation.createdAt()).isEqualTo(Instant.parse("2026-11-12T10:00:00Z"));
    }

    @Test
    void storesTheSimulationUnderAShortId() {
        Simulation simulation = service.create(new SimulationRequest(Map.of()));

        assertThat(simulation.id()).matches("[a-z2-9]{8}");
        assertThat(service.find(simulation.id())).contains(simulation);
        assertThat(service.find("inconnu")).isEmpty();
    }

    @Test
    void rejectsUnknownLevers() {
        assertThatThrownBy(() -> service.create(new SimulationRequest(Map.of("smic", 2000.0))))
                .isInstanceOf(InvalidProgramException.class)
                .hasMessageContaining("smic");
        assertThat(engine.lastReform).isNull();
    }

    @Test
    void rejectsRawParameterPaths() {
        assertThatThrownBy(() -> service.create(new SimulationRequest(Map.of(IR_RATE_1, 0.5))))
                .isInstanceOf(InvalidProgramException.class);
    }

    @Test
    void rejectsValuesOutOfBounds() {
        assertThatThrownBy(() -> service.create(new SimulationRequest(Map.of("ir-taux-tranche-1", 90.0))))
                .isInstanceOf(InvalidProgramException.class);
        assertThatThrownBy(() -> service.create(new SimulationRequest(Map.of("ir-taux-tranche-1", -1.0))))
                .isInstanceOf(InvalidProgramException.class);
        assertThat(stored).isEmpty();
    }
}
