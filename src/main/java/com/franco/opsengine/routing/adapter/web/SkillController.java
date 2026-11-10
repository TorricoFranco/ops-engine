package com.franco.opsengine.routing.adapter.web;

import com.franco.opsengine.routing.application.SkillCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/skills")
@Tag(name = "Áreas de soporte")
class SkillController {

    private final SkillCatalog catalog;

    SkillController(SkillCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * La consola muestra estos nombres en lugar de los códigos, para que nadie tenga que
     * conocer el código para leer la pantalla.
     */
    @GetMapping
    @Operation(summary = "Áreas de soporte con el nombre con el que se muestran")
    List<SkillView> all() {
        return catalog.displayNames().entrySet().stream()
                .map(entry -> new SkillView(entry.getKey().code(), entry.getValue()))
                .sorted(Comparator.comparing(SkillView::name))
                .toList();
    }

    record SkillView(String code, String name) {
    }
}
