package fr.quinquennat.backend.engine;

/** Access to the Python tax-benefit engine (OpenFisca-France). */
public interface EngineClient {

    /**
     * @throws EngineUnavailableException if the engine cannot be reached or answers incorrectly
     */
    EngineHealth health();
}
