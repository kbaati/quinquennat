package fr.quinquennat.backend.engine;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpEngineClient implements EngineClient {

    private final RestClient restClient;

    public HttpEngineClient(
            @Value("${engine.url}") String baseUrl,
            @Value("${engine.connect-timeout}") Duration connectTimeout,
            @Value("${engine.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public EngineHealth health() {
        try {
            EngineHealth health = restClient.get().uri("/sante").retrieve().body(EngineHealth.class);
            if (health == null) {
                throw new EngineUnavailableException("Engine returned an empty health response", null);
            }
            return health;
        } catch (RestClientException e) {
            throw new EngineUnavailableException("Engine health check failed", e);
        }
    }
}
