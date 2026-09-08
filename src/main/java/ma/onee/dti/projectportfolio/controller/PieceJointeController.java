package ma.onee.dti.projectportfolio.controller;

import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import ma.onee.dti.projectportfolio.dto.PieceJointeDownloadResponse;
import ma.onee.dti.projectportfolio.dto.PieceJointeResponse;
import ma.onee.dti.projectportfolio.service.PieceJointeService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pieces-jointes")
@RequiredArgsConstructor
public class PieceJointeController {

    private final PieceJointeService pieceJointeService;

    @PostMapping(value = "/projets/{projetId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PieceJointeResponse> upload(
            @PathVariable Long projetId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pieceJointeService.upload(projetId, file, authentication.getName()));
    }

    @GetMapping("/projets/{projetId}")
    public ResponseEntity<List<PieceJointeResponse>> list(@PathVariable Long projetId) {
        return ResponseEntity.ok(pieceJointeService.listByProjet(projetId));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        PieceJointeDownloadResponse download = pieceJointeService.download(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.getNomFichier()).build().toString()
                )
                .body(download.getResource());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) throws IOException {
        pieceJointeService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
