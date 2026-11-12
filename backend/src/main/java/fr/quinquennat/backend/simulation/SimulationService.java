package fr.quinquennat.backend.simulation;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.quinquennat.backend.catalog.Catalog;
import fr.quinquennat.backend.catalog.Household;
import fr.quinquennat.backend.catalog.Lever;
import fr.quinquennat.backend.engine.EngineClient;
import fr.quinquennat.backend.engine.EngineSimulation;
import fr.quinquennat.backend.engine.EngineUnavailableException;

@Service
public class SimulationService {

    static final String DISPOSABLE_INCOME = "revenu_disponible";

    private static final String ID_ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789"; // no 0/o/1/l look-alikes
    private static final int ID_LENGTH = 8;

    private final Catalog catalog;
    private final EngineClient engine;
    private final SimulationStore store;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public SimulationService(Catalog catalog, EngineClient engine, SimulationStore store) {
        this(catalog, engine, store, Clock.systemUTC());
    }

    SimulationService(Catalog catalog, EngineClient engine, SimulationStore store, Clock clock) {
        this.catalog = catalog;
        this.engine = engine;
        this.store = store;
        this.clock = clock;
    }

    public Simulation create(SimulationRequest request) {
        List<Simulation.ProgramItem> program = toProgram(request);
        Map<String, Double> reform = new LinkedHashMap<>();
        for (Simulation.ProgramItem item : program) {
            Lever lever = catalog.lever(item.leverId()).orElseThrow();
            reform.put(lever.parameter(), lever.toEngineValue(item.value()));
        }

        EngineSimulation result = engine.simulate(catalog.year(), reform, catalog.households());

        Simulation simulation = new Simulation(
                newId(),
                clock.instant().truncatedTo(ChronoUnit.SECONDS),
                result.year(),
                program,
                new Simulation.Versions(result.versions().openfiscaCore(), result.versions().openfiscaFrance()),
                toHouseholdResults(result));
        store.save(simulation);
        return simulation;
    }

    public Optional<Simulation> find(String id) {
        return store.find(id);
    }

    /** Keeps only the levers that differ from the current legislation, in catalogue order. */
    private List<Simulation.ProgramItem> toProgram(SimulationRequest request) {
        Map<String, Double> chosen = request == null || request.levers() == null ? Map.of() : request.levers();
        for (String id : chosen.keySet()) {
            if (catalog.lever(id).isEmpty()) {
                throw new InvalidProgramException("Levier inconnu : " + id);
            }
        }
        List<Simulation.ProgramItem> program = new ArrayList<>();
        for (Lever lever : catalog.levers()) {
            Double value = chosen.get(lever.id());
            if (!chosen.containsKey(lever.id())) {
                continue;
            }
            if (value == null || value.isNaN() || value < lever.min() || value > lever.max()) {
                throw new InvalidProgramException("Valeur hors bornes pour le levier « " + lever.label()
                        + " » : attendue entre " + lever.min() + " et " + lever.max());
            }
            if (value != lever.currentValue()) {
                program.add(new Simulation.ProgramItem(lever.id(), lever.group(), lever.label(), lever.unit(),
                        lever.currentValue(), value, lever.parameter()));
            }
        }
        return program;
    }

    private List<Simulation.HouseholdResult> toHouseholdResults(EngineSimulation result) {
        Map<String, EngineSimulation.HouseholdAmounts> amountsById = result.households().stream()
                .collect(Collectors.toMap(EngineSimulation.HouseholdAmounts::id, Function.identity()));
        List<Simulation.HouseholdResult> households = new ArrayList<>();
        for (Household household : catalog.households()) {
            EngineSimulation.HouseholdAmounts amounts = amountsById.get(household.id());
            if (amounts == null || amounts.before().get(DISPOSABLE_INCOME) == null
                    || amounts.after().get(DISPOSABLE_INCOME) == null) {
                throw new EngineUnavailableException("Engine answer is missing household " + household.id(), null);
            }
            double delta = amounts.after().get(DISPOSABLE_INCOME) - amounts.before().get(DISPOSABLE_INCOME);
            households.add(new Simulation.HouseholdResult(household.id(), household.name(), household.description(),
                    amounts.before(), amounts.after(), Math.round(delta * 100) / 100.0));
        }
        return households;
    }

    private String newId() {
        StringBuilder id = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            id.append(ID_ALPHABET.charAt(random.nextInt(ID_ALPHABET.length())));
        }
        return id.toString();
    }
}
