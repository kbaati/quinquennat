package fr.quinquennat.backend.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.quinquennat.backend.engine.EngineClient;
import fr.quinquennat.backend.engine.EngineHealth;
import fr.quinquennat.backend.engine.EngineUnavailableException;

@RestController
public class HealthController {

    static final String OK = "ok";
    static final String UNAVAILABLE = "unavailable";

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final EngineClient engineClient;

    public HealthController(EngineClient engineClient) {
        this.engineClient = engineClient;
    }

    /** Always answers 200: an unreachable engine is reported in the body, not as an error. */
    @GetMapping("/api/sante")
    public HealthResponse health() {
        try {
            EngineHealth engine = engineClient.health();
            return new HealthResponse(OK,
                    new HealthResponse.Engine(OK, engine.openfiscaCore(), engine.openfiscaFrance()));
        } catch (EngineUnavailableException e) {
            log.warn("Engine unavailable: {}", e.getMessage());
            return new HealthResponse(OK, new HealthResponse.Engine(UNAVAILABLE, null, null));
        }
    }
}
