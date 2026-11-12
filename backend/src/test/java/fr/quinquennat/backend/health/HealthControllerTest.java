package fr.quinquennat.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import fr.quinquennat.backend.engine.EngineHealth;
import fr.quinquennat.backend.engine.EngineUnavailableException;

class HealthControllerTest {

    @Test
    void reportsEngineVersionsWhenEngineAnswers() {
        HealthController controller = new HealthController(() -> new EngineHealth("ok", "46.0.1", "176.1.10"));

        HealthResponse response = controller.health();

        assertThat(response.backend()).isEqualTo("ok");
        assertThat(response.engine()).isEqualTo(new HealthResponse.Engine("ok", "46.0.1", "176.1.10"));
    }

    @Test
    void reportsEngineUnavailableWithoutFailing() {
        HealthController controller = new HealthController(() -> {
            throw new EngineUnavailableException("down", null);
        });

        HealthResponse response = controller.health();

        assertThat(response.backend()).isEqualTo("ok");
        assertThat(response.engine()).isEqualTo(new HealthResponse.Engine("unavailable", null, null));
    }
}
