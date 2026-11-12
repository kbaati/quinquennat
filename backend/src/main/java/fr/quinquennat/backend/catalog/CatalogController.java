package fr.quinquennat.backend.catalog;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogController {

    private final Catalog catalog;

    public CatalogController(Catalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/api/leviers")
    public List<Lever> levers() {
        return catalog.levers();
    }

    @GetMapping("/api/foyers")
    public List<Household> households() {
        return catalog.households();
    }
}
