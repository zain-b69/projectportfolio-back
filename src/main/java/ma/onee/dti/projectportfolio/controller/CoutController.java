package ma.onee.dti.projectportfolio.controller;

import jakarta.validation.Valid;
import java.util.List;
import ma.onee.dti.projectportfolio.dto.CoutResponse;
import ma.onee.dti.projectportfolio.dto.CreateCoutRequest;
import ma.onee.dti.projectportfolio.dto.UpdateCoutRequest;
import ma.onee.dti.projectportfolio.service.CoutService;
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
public class CoutController {

    private final CoutService coutService;

    public CoutController(CoutService coutService) {
        this.coutService = coutService;
    }

    @GetMapping("/projets/{idProjet}/couts")
    public ResponseEntity<List<CoutResponse>> getCoutsByProjet(@PathVariable Long idProjet) {
        return ResponseEntity.ok(coutService.getCoutsByProjet(idProjet));
    }

    @GetMapping("/couts/{idCout}")
    public ResponseEntity<CoutResponse> getCoutById(@PathVariable Long idCout) {
        return ResponseEntity.ok(coutService.getCoutById(idCout));
    }

    @PostMapping("/projets/{idProjet}/couts")
    public ResponseEntity<CoutResponse> createCout(
            @PathVariable Long idProjet,
            @Valid @RequestBody CreateCoutRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(coutService.createCout(idProjet, request, authentication.getName()));
    }

    @PutMapping("/couts/{idCout}")
    public ResponseEntity<CoutResponse> updateCout(
            @PathVariable Long idCout,
            @Valid @RequestBody UpdateCoutRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(coutService.updateCout(idCout, request, authentication.getName()));
    }

    @DeleteMapping("/couts/{idCout}")
    public ResponseEntity<Void> deleteCout(@PathVariable Long idCout, Authentication authentication) {
        coutService.deleteCout(idCout, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
