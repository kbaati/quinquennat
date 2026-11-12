package fr.quinquennat.backend.reaction;

import java.util.Optional;

/** Cache of generated reactions, keyed by the fingerprint of (generator, programme, household). */
public interface ReactionStore {

    Optional<Reaction> find(String fingerprint);

    void save(String fingerprint, Reaction reaction);
}
