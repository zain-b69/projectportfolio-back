package ma.onee.dsi.projectportfolio.controller;

import jakarta.validation.Valid;
import java.util.List;
import ma.onee.dsi.projectportfolio.dto.CreateRisqueRequest;
import ma.onee.dsi.projectportfolio.dto.RisqueResponse;
import ma.onee.dsi.projectportfolio.dto.UpdateRisqueRequest;
import ma.onee.dsi.projectportfolio.service.RisqueService;
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
public class RisqueController {

    private final RisqueService risqueService;

    public RisqueController(RisqueService risqueService) {
        this.risqueService = risqueService;
    }

    @GetMapping("/projets/{idProjet}/risques")
    public ResponseEntity<List<RisqueResponse>> getRisquesByProjet(@PathVariable Long idProjet) {
        return ResponseEntity.ok(risqueService.getRisquesByProjet(idProjet));
    }

    @GetMapping("/risques/{idRisque}")
    public ResponseEntity<RisqueResponse> getRisqueById(@PathVariable Long idRisque) {
        return ResponseEntity.ok(risqueService.getRisqueById(idRisque));
    }

    @PostMapping("/projets/{idProjet}/risques")
    public ResponseEntity<RisqueResponse> createRisque(
            @PathVariable Long idProjet,
            @Valid @RequestBody CreateRisqueRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(risqueService.createRisque(idProjet, request, authentication.getName()));
    }

    @PutMapping("/risques/{idRisque}")
    public ResponseEntity<RisqueResponse> updateRisque(
            @PathVariable Long idRisque,
            @Valid @RequestBody UpdateRisqueRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(risqueService.updateRisque(idRisque, request, authentication.getName()));
    }

    @DeleteMapping("/risques/{idRisque}")
    public ResponseEntity<Void> deleteRisque(
            @PathVariable Long idRisque,
            Authentication authentication
    ) {
        risqueService.deleteRisque(idRisque, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
