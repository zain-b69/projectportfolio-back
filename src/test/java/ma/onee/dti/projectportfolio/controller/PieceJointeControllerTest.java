package ma.onee.dti.projectportfolio.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import ma.onee.dti.projectportfolio.dto.PieceJointeDownloadResponse;
import ma.onee.dti.projectportfolio.dto.PieceJointeResponse;
import ma.onee.dti.projectportfolio.exception.BusinessRuleException;
import ma.onee.dti.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dti.projectportfolio.security.CustomUserDetailsService;
import ma.onee.dti.projectportfolio.security.JwtAuthenticationFilter;
import ma.onee.dti.projectportfolio.security.JwtService;
import ma.onee.dti.projectportfolio.security.SecurityConfig;
import ma.onee.dti.projectportfolio.service.PieceJointeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(PieceJointeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PieceJointeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PieceJointeService pieceJointeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void uploadValidFileReturnsCreatedAndPassesProjectFileAndAuthenticatedEmail() throws Exception {
        when(pieceJointeService.upload(eq(10L), any(MultipartFile.class), eq("pm@example.com")))
                .thenReturn(response());

        mockMvc.perform(multipart("/api/pieces-jointes/projets/10")
                        .file(file("planning.pdf", "contenu"))
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPieceJointe").value(20L))
                .andExpect(jsonPath("$.nomFichier").value("planning.pdf"))
                .andExpect(jsonPath("$.idProjet").value(10L));

        verify(pieceJointeService).upload(eq(10L), any(MultipartFile.class), eq("pm@example.com"));
    }

    @Test
    void uploadEmptyFileBusinessRuleUsesGlobalHandler() throws Exception {
        when(pieceJointeService.upload(eq(10L), any(MultipartFile.class), eq("pm@example.com")))
                .thenThrow(new BusinessRuleException("Le fichier est obligatoire"));

        mockMvc.perform(multipart("/api/pieces-jointes/projets/10")
                        .file(file("empty.pdf", ""))
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Le fichier est obligatoire"));
    }

    @Test
    void listByProjetReturnsAttachmentMetadataAndPassesProjectId() throws Exception {
        when(pieceJointeService.listByProjet(10L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/pieces-jointes/projets/10")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPieceJointe").value(20L))
                .andExpect(jsonPath("$[0].nomFichier").value("planning.pdf"))
                .andExpect(jsonPath("$[0].idProjet").value(10L));

        verify(pieceJointeService).listByProjet(10L);
    }

    @Test
    void listByProjetNotFoundUsesGlobalHandler() throws Exception {
        when(pieceJointeService.listByProjet(99L))
                .thenThrow(new ResourceNotFoundException("Projet introuvable"));

        mockMvc.perform(get("/api/pieces-jointes/projets/99")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Projet introuvable"));
    }

    @Test
    void downloadReturnsResourceWithAttachmentHeader() throws Exception {
        ByteArrayResource resource = new ByteArrayResource("contenu".getBytes());
        when(pieceJointeService.download(20L)).thenReturn(PieceJointeDownloadResponse.builder()
                .nomFichier("planning.pdf")
                .resource(resource)
                .build());

        mockMvc.perform(get("/api/pieces-jointes/20/download")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE")))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("attachment"),
                                org.hamcrest.Matchers.containsString("planning.pdf")
                        )
                ))
                .andExpect(content().bytes("contenu".getBytes()));

        verify(pieceJointeService).download(20L);
    }

    @Test
    void deleteReturnsNoContentAndPassesAttachmentIdAndAuthenticatedEmail() throws Exception {
        mockMvc.perform(delete("/api/pieces-jointes/20")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(pieceJointeService).delete(20L, "pm@example.com");
    }

    @Test
    void deleteAccessDeniedUsesGlobalHandler() throws Exception {
        doThrow(new AccessDeniedException("Le responsable projet ne peut gerer que ses propres pieces jointes"))
                .when(pieceJointeService).delete(20L, "pm@example.com");

        mockMvc.perform(delete("/api/pieces-jointes/20")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acces refuse"));
    }

    @Test
    void unauthenticatedRequestsReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/pieces-jointes/projets/10"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/pieces-jointes/20/download"))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/pieces-jointes/projets/10")
                        .file(file("planning.pdf", "contenu")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pieceJointeService);
    }

    private MockMultipartFile file(String filename, String content) {
        return new MockMultipartFile("file", filename, "application/pdf", content.getBytes());
    }

    private PieceJointeResponse response() {
        return PieceJointeResponse.builder()
                .idPieceJointe(20L)
                .nomFichier("planning.pdf")
                .dateAjout(LocalDateTime.of(2026, 8, 5, 13, 0))
                .idProjet(10L)
                .build();
    }
}
