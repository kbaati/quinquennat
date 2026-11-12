package fr.quinquennat.backend.simulation;

import java.sql.Timestamp;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import tools.jackson.databind.json.JsonMapper;

/** Stores each simulation as one JSON document: results are immutable and only read by id. */
@Repository
public class JdbcSimulationStore implements SimulationStore {

    private final JdbcClient jdbc;
    private final JsonMapper mapper;

    public JdbcSimulationStore(JdbcClient jdbc, JsonMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public void save(Simulation simulation) {
        jdbc.sql("insert into simulations (id, created_at, payload) values (?, ?, ?)")
                .params(simulation.id(), Timestamp.from(simulation.createdAt()), mapper.writeValueAsString(simulation))
                .update();
    }

    @Override
    public Optional<Simulation> find(String id) {
        return jdbc.sql("select payload from simulations where id = ?")
                .param(id)
                .query(String.class)
                .optional()
                .map(payload -> mapper.readValue(payload, Simulation.class));
    }
}
