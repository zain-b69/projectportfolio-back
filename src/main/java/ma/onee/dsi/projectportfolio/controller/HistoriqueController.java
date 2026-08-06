package ma.onee.dsi.projectportfolio.controller;

import java.util.List;
import ma.onee.dsi.projectportfolio.dto.HistoriqueModificationResponse;
import ma.onee.dsi.projectportfolio.dto.HistoriquePageResponse;
import ma.onee.dsi.projectportfolio.dto.HistoriqueSearchCriteria;
import ma.onee.dsi.projectportfolio.service.HistoriqueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HistoriqueController {

    private final HistoriqueService historiqueService;

    public HistoriqueController(HistoriqueService historiqueService) {
        this.historiqueService = historiqueService;
    }

    @GetMapping("/historique")
    public ResponseEntity<HistoriquePageResponse> searchHistorique(
            @ModelAttribute HistoriqueSearchCriteria criteria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(historiqueService.search(criteria, page, size));
    }

    @GetMapping("/projets/{projectId}/historique")
    public ResponseEntity<List<HistoriqueModificationResponse>> getHistoriqueByProjet(
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(historiqueService.getByProjet(projectId));
    }
}
