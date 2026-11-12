package fr.quinquennat.backend.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.json.JsonMapper;

/** Versioned catalogue of levers and typical households, read once from {@code donnees/}. */
@Component
public class Catalog {

    private final int year;
    private final List<Lever> levers;
    private final List<Household> households;

    public Catalog(JsonMapper mapper) {
        LeverFile leverFile = read(mapper, "donnees/leviers.json", LeverFile.class);
        HouseholdFile householdFile = read(mapper, "donnees/foyers.json", HouseholdFile.class);
        if (leverFile.year() != householdFile.year()) {
            throw new IllegalStateException("leviers.json and foyers.json target different years");
        }
        requireUnique(leverFile.levers().stream().map(Lever::id).toList(), "lever");
        requireUnique(householdFile.households().stream().map(Household::id).toList(), "household");
        for (Lever lever : leverFile.levers()) {
            if (!(lever.min() <= lever.currentValue() && lever.currentValue() <= lever.max())) {
                throw new IllegalStateException("Current value out of bounds for lever " + lever.id());
            }
        }
        this.year = leverFile.year();
        this.levers = List.copyOf(leverFile.levers());
        this.households = List.copyOf(householdFile.households());
    }

    public int year() {
        return year;
    }

    public List<Lever> levers() {
        return levers;
    }

    public List<Household> households() {
        return households;
    }

    public Optional<Lever> lever(String id) {
        return levers.stream().filter(lever -> lever.id().equals(id)).findFirst();
    }

    private static <T> T read(JsonMapper mapper, String path, Class<T> type) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(input, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + path, e);
        }
    }

    private static void requireUnique(List<String> ids, String kind) {
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) {
                throw new IllegalStateException("Duplicate " + kind + " id: " + id);
            }
        }
    }

    private record LeverFile(@JsonProperty("annee") int year, @JsonProperty("leviers") List<Lever> levers) {
    }

    private record HouseholdFile(@JsonProperty("annee") int year, @JsonProperty("foyers") List<Household> households) {
    }
}
