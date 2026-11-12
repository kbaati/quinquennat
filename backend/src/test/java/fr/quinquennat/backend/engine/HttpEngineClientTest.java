package fr.quinquennat.backend.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class HttpEngineClientTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private HttpServer server;

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

    private void respond(int status, String body) {
        server.createContext("/sante", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    @Test
    void parsesEngineHealth() {
        respond(200, "{\"status\": \"ok\", \"openfisca_core\": \"46.0.1\", \"openfisca_france\": \"176.1.10\"}");

        assertThat(client().health()).isEqualTo(new EngineHealth("ok", "46.0.1", "176.1.10"));
    }

    @Test
    void wrapsHttpErrors() {
        respond(500, "{}");

        assertThatThrownBy(() -> client().health()).isInstanceOf(EngineUnavailableException.class);
    }

    @Test
    void wrapsConnectionFailures() {
        HttpEngineClient client = client();
        server.stop(0);

        assertThatThrownBy(client::health).isInstanceOf(EngineUnavailableException.class);
    }
}
