package fr.quinquennat.backend.engine;

import java.util.List;
import java.util.Map;

import fr.quinquennat.backend.catalog.Household;

/** Engine double: every household has a disposable income of 20 000 before, plus {@code delta} after. */
public class FakeEngineClient implements EngineClient {

    public boolean available = true;
    public double delta = 0;
    public Map<String, Double> lastReform;

    @Override
    public EngineHealth health() {
        requireAvailable();
        return new EngineHealth("ok", "44.7.1", "176.1.10");
    }

    @Override
    public EngineSimulation simulate(int year, Map<String, Double> reform, List<Household> households) {
        requireAvailable();
        lastReform = reform;
        List<EngineSimulation.HouseholdAmounts> amounts = households.stream()
                .map(household -> new EngineSimulation.HouseholdAmounts(household.id(),
                        Map.of("revenu_disponible", 20_000.0, "impot_revenu", 1_000.0),
                        Map.of("revenu_disponible", 20_000.0 + delta, "impot_revenu", 1_000.0 - delta)))
                .toList();
        return new EngineSimulation(year, new EngineSimulation.Versions("44.7.1", "176.1.10"), amounts);
    }

    private void requireAvailable() {
        if (!available) {
            throw new EngineUnavailableException("down", null);
        }
    }
}
