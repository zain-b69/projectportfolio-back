package ma.onee.dti.projectportfolio.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import ma.onee.dti.projectportfolio.dto.CreateProjetRequest;
import ma.onee.dti.projectportfolio.dto.ProjetResponse;
import ma.onee.dti.projectportfolio.dto.ProjetSearchCriteria;
import ma.onee.dti.projectportfolio.dto.UpdateProjetRequest;
import ma.onee.dti.projectportfolio.service.ProjetExportService;
import ma.onee.dti.projectportfolio.service.ProjetService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final ProjetExportService projetExportService;

    public ProjetController(ProjetService projetService, ProjetExportService projetExportService) {
        this.projetService = projetService;
        this.projetExportService = projetExportService;
    }

    @GetMapping
    public ResponseEntity<List<ProjetResponse>> searchProjets(@Valid @ModelAttribute ProjetSearchCriteria criteria) {
        return ResponseEntity.ok(projetService.searchProjets(criteria));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportProjets(@Valid @ModelAttribute ProjetSearchCriteria criteria) {
        List<ProjetResponse> projets = projetService.searchProjets(criteria);
        byte[] content = projetExportService.exportProjects(projets);
        String filename = "projets_" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString()
                )
                .body(content);
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
