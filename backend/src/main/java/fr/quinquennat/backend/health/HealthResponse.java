package fr.quinquennat.backend.health;

public record HealthResponse(String backend, Engine engine) {

    public record Engine(String status, String openfiscaCore, String openfiscaFrance) {
    }
}
