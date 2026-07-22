package ma.onee.dsi.projectportfolio.controller;

import jakarta.validation.Valid;
import java.util.List;
import ma.onee.dsi.projectportfolio.dto.CreateProjetRequest;
import ma.onee.dsi.projectportfolio.dto.ProjetResponse;
import ma.onee.dsi.projectportfolio.dto.ProjetSearchCriteria;
import ma.onee.dsi.projectportfolio.dto.UpdateProjetRequest;
import ma.onee.dsi.projectportfolio.service.ProjetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projets")
public class ProjetController {

    private final ProjetService projetService;

    public ProjetController(ProjetService projetService) {
        this.projetService = projetService;
    }

    @GetMapping
    public ResponseEntity<List<ProjetResponse>> searchProjets(@Valid @ModelAttribute ProjetSearchCriteria criteria) {
        return ResponseEntity.ok(projetService.searchProjets(criteria));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjetResponse> getProjetById(@PathVariable Long projectId) {
        return ResponseEntity.ok(projetService.getProjetById(projectId));
    }

    @PostMapping
    public ResponseEntity<ProjetResponse> createProjet(
            @Valid @RequestBody CreateProjetRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projetService.createProjet(request, authentication.getName()));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjetResponse> updateProjet(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjetRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(projetService.updateProjet(projectId, request, authentication.getName()));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProjet(@PathVariable Long projectId, Authentication authentication) {
        projetService.deleteProjet(projectId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
