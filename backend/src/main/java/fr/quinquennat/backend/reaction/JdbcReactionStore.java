package fr.quinquennat.backend.reaction;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcReactionStore implements ReactionStore {

    private final JdbcClient jdbc;
    private final JsonMapper mapper;

    public JdbcReactionStore(JdbcClient jdbc, JsonMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public Optional<Reaction> find(String fingerprint) {
        return jdbc.sql("select payload from reactions where fingerprint = ?")
                .param(fingerprint)
                .query(String.class)
                .optional()
                .map(payload -> mapper.readValue(payload, Reaction.class));
    }

    @Override
    public void save(String fingerprint, Reaction reaction) {
        try {
            jdbc.sql("insert into reactions (fingerprint, created_at, payload) values (?, ?, ?)")
                    .params(fingerprint, Timestamp.from(Instant.now()), mapper.writeValueAsString(reaction))
                    .update();
        } catch (DuplicateKeyException alreadyCached) {
            // Another request generated the same reaction first: keep that one.
        }
    }
}
