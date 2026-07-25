package ma.onee.dsi.projectportfolio.controller;

import jakarta.validation.Valid;
import java.util.List;
import ma.onee.dsi.projectportfolio.dto.AffectationRessourceResponse;
import ma.onee.dsi.projectportfolio.dto.CreateAffectationRessourceRequest;
import ma.onee.dsi.projectportfolio.dto.UpdateAffectationRessourceRequest;
import ma.onee.dsi.projectportfolio.service.AffectationRessourceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AffectationRessourceController {

    private final AffectationRessourceService affectationRessourceService;

    public AffectationRessourceController(AffectationRessourceService affectationRessourceService) {
        this.affectationRessourceService = affectationRessourceService;
    }

    @GetMapping("/projets/{projectId}/affectations-ressources")
    public ResponseEntity<List<AffectationRessourceResponse>> getAffectationsByProjet(
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(affectationRessourceService.getAffectationsByProjet(projectId));
    }

    @GetMapping("/ressources/{ressourceId}/affectations")
    public ResponseEntity<List<AffectationRessourceResponse>> getAffectationsByRessource(
            @PathVariable Long ressourceId
    ) {
        return ResponseEntity.ok(affectationRessourceService.getAffectationsByRessource(ressourceId));
    }

    @PostMapping("/projets/{projectId}/affectations-ressources")
    public ResponseEntity<AffectationRessourceResponse> createAffectation(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateAffectationRessourceRequest request,
            Authentication authentication
    ) {
        assertProjectIdConsistency(projectId, request);
        request.setIdProjet(projectId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(affectationRessourceService.createAffectation(request, authentication.getName()));
    }

    @PutMapping("/projets/{projectId}/affectations-ressources/{affectationId}")
    public ResponseEntity<AffectationRessourceResponse> updateAffectation(
            @PathVariable Long projectId,
            @PathVariable Long affectationId,
            @Valid @RequestBody UpdateAffectationRessourceRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(affectationRessourceService.updateAffectation(
                projectId,
                affectationId,
                request,
                authentication.getName()
        ));
    }

    @DeleteMapping("/projets/{projectId}/affectations-ressources/{affectationId}")
    public ResponseEntity<Void> deleteAffectation(
            @PathVariable Long projectId,
            @PathVariable Long affectationId,
            Authentication authentication
    ) {
        affectationRessourceService.deleteAffectation(projectId, affectationId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    private void assertProjectIdConsistency(Long projectId, CreateAffectationRessourceRequest request) {
        if (!projectId.equals(request.getIdProjet())) {
            throw new IllegalArgumentException("Le projet du chemin doit correspondre au projet de la requete");
        }
    }
}
