package ma.onee.dsi.projectportfolio.controller;

import jakarta.validation.Valid;
import java.util.List;
import ma.onee.dsi.projectportfolio.dto.CreateUtilisateurRequest;
import ma.onee.dsi.projectportfolio.dto.UpdateUtilisateurRequest;
import ma.onee.dsi.projectportfolio.dto.UtilisateurResponse;
import ma.onee.dsi.projectportfolio.service.UtilisateurService;
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
@RequestMapping("/utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    public ResponseEntity<List<UtilisateurResponse>> getAllUtilisateurs() {
        return ResponseEntity.ok(utilisateurService.getAllUtilisateurs());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UtilisateurResponse> getUtilisateurById(@PathVariable Long userId) {
        return ResponseEntity.ok(utilisateurService.getUtilisateurById(userId));
    }

    @PostMapping
    public ResponseEntity<UtilisateurResponse> createUtilisateur(@Valid @RequestBody CreateUtilisateurRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(utilisateurService.createUtilisateur(request));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UtilisateurResponse> updateUtilisateur(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUtilisateurRequest request
    ) {
        return ResponseEntity.ok(utilisateurService.updateUtilisateur(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUtilisateur(@PathVariable Long userId, Authentication authentication) {
        utilisateurService.deleteUtilisateur(userId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
