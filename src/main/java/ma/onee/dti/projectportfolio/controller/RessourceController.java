package ma.onee.dti.projectportfolio.controller;

import jakarta.validation.Valid;
import java.util.List;
import ma.onee.dti.projectportfolio.dto.CreateRessourceRequest;
import ma.onee.dti.projectportfolio.dto.RessourceResponse;
import ma.onee.dti.projectportfolio.dto.UpdateRessourceRequest;
import ma.onee.dti.projectportfolio.enums.NatureRessource;
import ma.onee.dti.projectportfolio.service.RessourceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ressources")
public class RessourceController {

    private final RessourceService ressourceService;

    public RessourceController(RessourceService ressourceService) {
        this.ressourceService = ressourceService;
    }

    @GetMapping
    public ResponseEntity<List<RessourceResponse>> getRessources(
            @RequestParam(required = false) String texte,
            @RequestParam(required = false) NatureRessource nature
    ) {
        if (!hasText(texte) && nature == null) {
            return ResponseEntity.ok(ressourceService.getAllRessources());
        }

        return ResponseEntity.ok(ressourceService.searchRessources(texte, nature));
    }

    @GetMapping("/{ressourceId}")
    public ResponseEntity<RessourceResponse> getRessourceById(@PathVariable Long ressourceId) {
        return ResponseEntity.ok(ressourceService.getRessourceById(ressourceId));
    }

    @PostMapping
    public ResponseEntity<RessourceResponse> createRessource(@Valid @RequestBody CreateRessourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ressourceService.createRessource(request));
    }

    @PutMapping("/{ressourceId}")
    public ResponseEntity<RessourceResponse> updateRessource(
            @PathVariable Long ressourceId,
            @Valid @RequestBody UpdateRessourceRequest request
    ) {
        return ResponseEntity.ok(ressourceService.updateRessource(ressourceId, request));
    }

    @DeleteMapping("/{ressourceId}")
    public ResponseEntity<Void> deleteRessource(@PathVariable Long ressourceId) {
        ressourceService.deleteRessource(ressourceId);
        return ResponseEntity.noContent().build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
