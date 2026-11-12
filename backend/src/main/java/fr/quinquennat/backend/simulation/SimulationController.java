package fr.quinquennat.backend.simulation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class SimulationController {

    private final SimulationService service;
    private final RateLimiter rateLimiter;

    public SimulationController(SimulationService service, RateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/api/simulations")
    @ResponseStatus(HttpStatus.CREATED)
    public Simulation create(@RequestBody SimulationRequest request, HttpServletRequest httpRequest) {
        if (!rateLimiter.tryAcquire(httpRequest.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de simulations en peu de temps. Réessayez dans une minute.");
        }
        return service.create(request);
    }

    @GetMapping("/api/simulations/{id}")
    public Simulation find(@PathVariable String id) {
        return service.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulation introuvable."));
    }
}
