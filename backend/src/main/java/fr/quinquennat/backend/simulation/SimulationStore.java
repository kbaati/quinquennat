package fr.quinquennat.backend.simulation;

import java.util.Optional;

public interface SimulationStore {

    void save(Simulation simulation);

    Optional<Simulation> find(String id);
}
