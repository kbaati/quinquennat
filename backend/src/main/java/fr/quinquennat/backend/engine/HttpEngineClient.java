package fr.quinquennat.backend.engine;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import fr.quinquennat.backend.catalog.Household;

@Component
public class HttpEngineClient implements EngineClient {

    private final RestClient restClient;

    public HttpEngineClient(
            @Value("${engine.url}") String baseUrl,
            @Value("${engine.connect-timeout}") Duration connectTimeout,
            @Value("${engine.read-timeout}") Duration readTimeout) {
        // HTTP/1.1: uvicorn does not speak h2c, and the JDK client would otherwise try to upgrade.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public EngineHealth health() {
        try {
            return requireBody(restClient.get().uri("/sante").retrieve().body(EngineHealth.class));
        } catch (RestClientException e) {
            throw new EngineUnavailableException("Engine health check failed", e);
        }
    }

    @Override
    public EngineSimulation simulate(int year, Map<String, Double> reform, List<Household> households) {
        List<Map<String, Object>> engineHouseholds = households.stream()
                .map(household -> Map.<String, Object>of("id", household.id(), "situation", household.situation()))
                .toList();
        Map<String, Object> body = Map.of("annee", year, "reforme", reform, "foyers", engineHouseholds);
        try {
            return requireBody(restClient.post().uri("/simuler")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(EngineSimulation.class));
        } catch (RestClientException e) {
            throw new EngineUnavailableException("Engine simulation failed: " + e.getMessage(), e);
        }
    }

    private static <T> T requireBody(T body) {
        if (body == null) {
            throw new EngineUnavailableException("Engine returned an empty response", null);
        }
        return body;
    }
}
