package fr.quinquennat.backend.reaction;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.quinquennat.backend.simulation.SimulationService;

@RestController
public class ReactionController {

    private final SimulationService simulations;
    private final ReactionService reactions;

    public ReactionController(SimulationService simulations, ReactionService reactions) {
        this.simulations = simulations;
        this.reactions = reactions;
    }

    /** Fictional testimonials of a stored simulation; generated on first call, cached afterwards. */
    @GetMapping("/api/simulations/{id}/reactions")
    public Reactions find(@PathVariable String id) {
        return simulations.find(id)
                .map(reactions::forSimulation)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulation introuvable."));
    }
}
