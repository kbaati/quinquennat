package fr.quinquennat.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import fr.quinquennat.backend.engine.FakeEngineClient;

class HealthControllerTest {

    private final FakeEngineClient engine = new FakeEngineClient();
    private final HealthController controller = new HealthController(engine);

    @Test
    void reportsEngineVersionsWhenEngineAnswers() {
        HealthResponse response = controller.health();

        assertThat(response.backend()).isEqualTo("ok");
        assertThat(response.engine()).isEqualTo(new HealthResponse.Engine("ok", "44.7.1", "176.1.10"));
    }

    @Test
    void reportsEngineUnavailableWithoutFailing() {
        engine.available = false;

        HealthResponse response = controller.health();

        assertThat(response.backend()).isEqualTo("ok");
        assertThat(response.engine()).isEqualTo(new HealthResponse.Engine("unavailable", null, null));
    }
}
