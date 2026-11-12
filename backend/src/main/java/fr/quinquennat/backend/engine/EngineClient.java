package fr.quinquennat.backend.engine;

import java.util.List;
import java.util.Map;

import fr.quinquennat.backend.catalog.Household;

/** Access to the Python tax-benefit engine (OpenFisca-France). */
public interface EngineClient {

    /**
     * @throws EngineUnavailableException if the engine cannot be reached or answers incorrectly
     */
    EngineHealth health();

    /**
     * @param reform OpenFisca parameter path to new value; empty for the current legislation
     * @throws EngineUnavailableException if the engine cannot be reached or rejects the request
     */
    EngineSimulation simulate(int year, Map<String, Double> reform, List<Household> households);
}
