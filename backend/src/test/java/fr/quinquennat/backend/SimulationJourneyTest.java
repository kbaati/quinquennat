package fr.quinquennat.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import fr.quinquennat.backend.engine.EngineClient;
import fr.quinquennat.backend.engine.FakeEngineClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** End-to-end journey over real HTTP, the real database layer (H2) and a fake engine. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SimulationJourneyTest {

    @TestConfiguration
    static class FakeEngineConfiguration {
        @Bean
        @Primary
        EngineClient fakeEngineClient() {
            FakeEngineClient engine = new FakeEngineClient();
            engine.delta = -120;
            return engine;
        }
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JsonMapper mapper;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    @Test
    void composeSimulateAndShare() throws Exception {
        JsonNode levers = mapper.readTree(get("/api/leviers").body());
        assertThat(levers.size()).isGreaterThanOrEqualTo(5);
        String leverId = levers.get(1).get("id").asString();
        assertThat(leverId).isEqualTo("ir-taux-tranche-1");
        assertThat(levers.get(1).get("valeurActuelle").asDouble()).isEqualTo(11);

        JsonNode households = mapper.readTree(get("/api/foyers").body());
        assertThat(households.size()).isGreaterThanOrEqualTo(20);

        HttpResponse<String> created = post("/api/simulations", "{\"leviers\": {\"" + leverId + "\": 15}}");
        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode simulation = mapper.readTree(created.body());
        String id = simulation.get("id").asString();
        assertThat(simulation.get("programme").get(0).get("valeur").asDouble()).isEqualTo(15);
        assertThat(simulation.get("foyers").size()).isEqualTo(households.size());
        assertThat(simulation.get("foyers").get(0).get("ecartRevenuDisponible").asDouble()).isEqualTo(-120);
        assertThat(simulation.get("versions").get("openfiscaFrance").asString()).isEqualTo("176.1.10");

        HttpResponse<String> shared = get("/api/simulations/" + id);
        assertThat(shared.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(shared.body())).isEqualTo(simulation);
    }

    @Test
    void rejectsInvalidProgrammesWithAReadableMessage() throws Exception {
        HttpResponse<String> response = post("/api/simulations", "{\"leviers\": {\"ir-taux-tranche-1\": 99}}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(mapper.readTree(response.body()).get("detail").asString()).contains("hors bornes");
    }

    @Test
    void unknownSimulationIsNotFound() throws Exception {
        assertThat(get("/api/simulations/inconnue").statusCode()).isEqualTo(404);
    }

    @Test
    void publishesAnOpenApiDescription() throws Exception {
        HttpResponse<String> response = get("/api/openapi");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("/api/simulations").contains("/api/leviers");
    }
}
