package fr.quinquennat.backend.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import fr.quinquennat.backend.catalog.Household;

class HttpEngineClientTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private HttpServer server;
    private final AtomicReference<String> receivedBody = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private HttpEngineClient client() {
        return new HttpEngineClient("http://127.0.0.1:" + server.getAddress().getPort(), TIMEOUT, TIMEOUT);
    }

    private void respond(String path, int status, String body) {
        server.createContext(path, exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    @Test
    void parsesEngineHealth() {
        respond("/sante", 200,
                "{\"status\": \"ok\", \"openfisca_core\": \"44.7.1\", \"openfisca_france\": \"176.1.10\"}");

        assertThat(client().health()).isEqualTo(new EngineHealth("ok", "44.7.1", "176.1.10"));
    }

    @Test
    void wrapsHttpErrors() {
        respond("/sante", 500, "{}");

        assertThatThrownBy(() -> client().health()).isInstanceOf(EngineUnavailableException.class);
    }

    @Test
    void wrapsConnectionFailures() {
        HttpEngineClient client = client();
        server.stop(0);

        assertThatThrownBy(client::health).isInstanceOf(EngineUnavailableException.class);
    }

    @Test
    void sendsReformAndHouseholdsThenParsesAmounts() {
        respond("/simuler", 200, """
                {"annee": 2025,
                 "versions": {"openfisca_core": "44.7.1", "openfisca_france": "176.1.10"},
                 "foyers": [{"id": "a", "avant": {"revenu_disponible": 100.5}, "apres": {"revenu_disponible": 90.0}}]}
                """);
        Household household = new Household("a", "A", "desc", Map.of("individus", Map.of("x", Map.of())));

        EngineSimulation result = client().simulate(2025, Map.of("some.path", 0.2), List.of(household));

        assertThat(receivedBody.get())
                .contains("\"annee\":2025")
                .contains("\"some.path\":0.2")
                .contains("\"id\":\"a\"")
                .contains("\"individus\"")
                .doesNotContain("\"nom\"");
        assertThat(result.year()).isEqualTo(2025);
        assertThat(result.versions().openfiscaFrance()).isEqualTo("176.1.10");
        assertThat(result.households()).containsExactly(new EngineSimulation.HouseholdAmounts(
                "a", Map.of("revenu_disponible", 100.5), Map.of("revenu_disponible", 90.0)));
    }

    @Test
    void wrapsEngineRejections() {
        respond("/simuler", 400, "{\"detail\": \"Paramètre inconnu\"}");

        assertThatThrownBy(() -> client().simulate(2025, Map.of(), List.of()))
                .isInstanceOf(EngineUnavailableException.class);
    }
}
