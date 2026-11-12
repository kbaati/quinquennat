package fr.quinquennat.backend.reaction;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.quinquennat.backend.simulation.Simulation;

/**
 * Fictional testimonials of a simulation, generated once per (generator, programme, household) and
 * then served from the cache. A failed generation only leaves that household without a testimonial.
 */
@Service
public class ReactionService {

    static final int MAX_WORDS = 40;

    private static final Logger log = LoggerFactory.getLogger(ReactionService.class);
    private static final int CONCURRENT_GENERATIONS = 4;

    private final boolean enabled;
    private final int maxGenerationsPerDay;
    private final ReactionGenerator generator;
    private final ReactionStore store;
    private final Clock clock;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore slots = new Semaphore(CONCURRENT_GENERATIONS);
    private final ConcurrentHashMap<String, CompletableFuture<Optional<Reaction>>> inFlight =
            new ConcurrentHashMap<>();

    private LocalDate budgetDay;
    private int generatedToday;

    @Autowired
    public ReactionService(
            @Value("${reactions.enabled:true}") boolean enabled,
            @Value("${reactions.max-generations-per-day:2000}") int maxGenerationsPerDay,
            ReactionGenerator generator,
            ReactionStore store) {
        this(enabled, maxGenerationsPerDay, generator, store, Clock.systemUTC());
    }

    ReactionService(boolean enabled, int maxGenerationsPerDay, ReactionGenerator generator, ReactionStore store,
            Clock clock) {
        this.enabled = enabled;
        this.maxGenerationsPerDay = maxGenerationsPerDay;
        this.generator = generator;
        this.store = store;
        this.clock = clock;
    }

    public Reactions forSimulation(Simulation simulation) {
        if (!enabled) {
            return new Reactions(false, generator.kind(), List.of());
        }
        List<CompletableFuture<Optional<Reaction>>> pending = new ArrayList<>();
        for (Simulation.HouseholdResult household : simulation.households()) {
            if (Math.abs(household.disposableIncomeDelta()) < 1) {
                continue; // nothing changes for this household: nothing to react to
            }
            String fingerprint = fingerprint(simulation, household.id());
            CompletableFuture<Optional<Reaction>> future = inFlight.computeIfAbsent(fingerprint,
                    key -> CompletableFuture.supplyAsync(() -> cachedOrGenerated(key, household), executor));
            future.whenComplete((result, failure) -> inFlight.remove(fingerprint, future));
            pending.add(future);
        }
        List<Reaction> reactions = pending.stream()
                .map(CompletableFuture::join)
                .flatMap(Optional::stream)
                .toList();
        return new Reactions(true, generator.kind(), reactions);
    }

    private Optional<Reaction> cachedOrGenerated(String fingerprint, Simulation.HouseholdResult household) {
        try {
            Optional<Reaction> cached = store.find(fingerprint);
            if (cached.isPresent()) {
                return cached;
            }
            if (!takeFromDailyBudget()) {
                log.warn("Daily budget of {} generated reactions is spent", maxGenerationsPerDay);
                return Optional.empty();
            }
            slots.acquire();
            Reaction reaction;
            try {
                reaction = checked(generator.generate(household), household.id());
            } finally {
                slots.release();
            }
            store.save(fingerprint, reaction);
            return Optional.of(reaction);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("No reaction for household {}: {}", household.id(), e.toString());
            return Optional.empty();
        }
    }

    /** Enforces the contract whatever the generator returned. */
    private static Reaction checked(Reaction reaction, String householdId) {
        if (reaction == null || reaction.feeling() < -2 || reaction.feeling() > 2
                || reaction.testimony() == null || reaction.testimony().isBlank()) {
            throw new IllegalStateException("Generator returned an invalid reaction");
        }
        String[] words = reaction.testimony().strip().split("\\s+");
        String testimony = words.length <= MAX_WORDS
                ? String.join(" ", words)
                : String.join(" ", List.of(words).subList(0, MAX_WORDS)) + "…";
        String reason = reaction.reason() == null ? "" : reaction.reason().strip();
        return new Reaction(householdId, reaction.feeling(), testimony, reason);
    }

    private synchronized boolean takeFromDailyBudget() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(budgetDay)) {
            budgetDay = today;
            generatedToday = 0;
        }
        if (generatedToday >= maxGenerationsPerDay) {
            return false;
        }
        generatedToday++;
        return true;
    }

    /** Same generator, same programme, same legislation and same household: same reaction. */
    String fingerprint(Simulation simulation, String householdId) {
        StringBuilder key = new StringBuilder(generator.cacheKey())
                .append('|').append(simulation.year())
                .append('|').append(simulation.versions().openfiscaFrance())
                .append('|').append(householdId);
        for (Simulation.ProgramItem item : simulation.program()) {
            key.append('|').append(item.leverId()).append('=').append(item.value());
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(key.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
