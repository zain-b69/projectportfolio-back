package ma.onee.dsi.projectportfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import ma.onee.dsi.projectportfolio.dto.PieceJointeDownloadResponse;
import ma.onee.dsi.projectportfolio.dto.PieceJointeResponse;
import ma.onee.dsi.projectportfolio.entity.PieceJointe;
import ma.onee.dsi.projectportfolio.entity.Projet;
import ma.onee.dsi.projectportfolio.entity.Role;
import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;
import ma.onee.dsi.projectportfolio.enums.RoleLibelle;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;
import ma.onee.dsi.projectportfolio.enums.TypeAction;
import ma.onee.dsi.projectportfolio.exception.BusinessRuleException;
import ma.onee.dsi.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dsi.projectportfolio.repository.PieceJointeRepository;
import ma.onee.dsi.projectportfolio.repository.ProjetRepository;
import ma.onee.dsi.projectportfolio.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class PieceJointeServiceTest {

    @Mock
    private PieceJointeRepository pieceJointeRepository;

    @Mock
    private ProjetRepository projetRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private HistoriqueService historiqueService;

    @Mock
    private FileStorageService fileStorageService;

    private PieceJointeService pieceJointeService;

    @BeforeEach
    void setUp() {
        pieceJointeService = new PieceJointeService(
                pieceJointeRepository,
                projetRepository,
                utilisateurRepository,
                historiqueService,
                fileStorageService
        );
    }

    @Test
    void ownerProjectManagerCanUploadAttachment() throws IOException {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, projectManager);
        MultipartFile file = file("planning.pdf", "contenu");

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(fileStorageService.save(10L, file)).thenReturn("uploads/projets/10/generated.pdf");
        when(pieceJointeRepository.save(any(PieceJointe.class))).thenAnswer(invocation -> {
            PieceJointe savedPieceJointe = invocation.getArgument(0);
            savedPieceJointe.setIdPieceJointe(20L);
            return savedPieceJointe;
        });

        PieceJointeResponse response = pieceJointeService.upload(10L, file, "pm@example.com");

        assertThat(response.getIdPieceJointe()).isEqualTo(20L);
        assertThat(response.getNomFichier()).isEqualTo("planning.pdf");
        assertThat(response.getIdProjet()).isEqualTo(10L);
        assertThat(response.getDateAjout()).isNotNull();

        ArgumentCaptor<PieceJointe> pieceJointeCaptor = ArgumentCaptor.forClass(PieceJointe.class);
        verify(pieceJointeRepository).save(pieceJointeCaptor.capture());
        assertThat(pieceJointeCaptor.getValue().getNomFichier()).isEqualTo("planning.pdf");
        assertThat(pieceJointeCaptor.getValue().getCheminFichier()).isEqualTo("uploads/projets/10/generated.pdf");
        assertThat(pieceJointeCaptor.getValue().getProjet()).isSameAs(projet);

        ArgumentCaptor<Projet> historiqueProjetCaptor = ArgumentCaptor.forClass(Projet.class);
        ArgumentCaptor<Utilisateur> historiqueUserCaptor = ArgumentCaptor.forClass(Utilisateur.class);
        ArgumentCaptor<TypeAction> historiqueActionCaptor = ArgumentCaptor.forClass(TypeAction.class);
        ArgumentCaptor<String> historiqueDescriptionCaptor = ArgumentCaptor.forClass(String.class);
        verify(historiqueService).record(
                historiqueProjetCaptor.capture(),
                historiqueUserCaptor.capture(),
                historiqueActionCaptor.capture(),
                historiqueDescriptionCaptor.capture());
        assertThat(historiqueActionCaptor.getValue()).isEqualTo(TypeAction.AJOUT_PIECE_JOINTE);
        assertThat(historiqueProjetCaptor.getValue()).isSameAs(projet);
        assertThat(historiqueUserCaptor.getValue()).isSameAs(projectManager);
        assertThat(historiqueDescriptionCaptor.getValue()).contains("Ajout de la piece jointe planning.pdf");
    }

    @Test
    void uploadRejectsMissingProject() throws IOException {
        when(projetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pieceJointeService.upload(99L, file("planning.pdf", "contenu"), "pm@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Projet introuvable");

        verify(fileStorageService, never()).save(any(Long.class), any(MultipartFile.class));
        verify(pieceJointeRepository, never()).save(any(PieceJointe.class));
    }

    @Test
    void uploadRejectsUnauthorizedUser() throws IOException {
        Utilisateur simpleUser = utilisateur(2L, "user@example.com", RoleLibelle.ROLE_UTILISATEUR_SIMPLE);
        Projet projet = projet(10L, utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(simpleUser));

        assertThatThrownBy(() -> pieceJointeService.upload(10L, file("planning.pdf", "contenu"), "user@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(fileStorageService, never()).save(any(Long.class), any(MultipartFile.class));
        verify(pieceJointeRepository, never()).save(any(PieceJointe.class));
        verify(historiqueService, never()).record(any(), any(), any(), any());
    }

    @Test
    void uploadRejectsEmptyFile() throws IOException {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, projectManager);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));

        assertThatThrownBy(() -> pieceJointeService.upload(10L, file("empty.pdf", ""), "pm@example.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("fichier est obligatoire");

        verify(fileStorageService, never()).save(any(Long.class), any(MultipartFile.class));
        verify(pieceJointeRepository, never()).save(any(PieceJointe.class));
    }

    @Test
    void listByProjetReturnsAttachmentMetadata() {
        Projet projet = projet(10L, utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));
        PieceJointe pieceJointe = pieceJointe(20L, "planning.pdf", "uploads/projets/10/generated.pdf", projet);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(pieceJointeRepository.findByProjet_IdProjet(10L)).thenReturn(List.of(pieceJointe));

        List<PieceJointeResponse> responses = pieceJointeService.listByProjet(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getIdPieceJointe()).isEqualTo(20L);
        assertThat(responses.get(0).getNomFichier()).isEqualTo("planning.pdf");
        assertThat(responses.get(0).getIdProjet()).isEqualTo(10L);
    }

    @Test
    void downloadReturnsResourceAndFilename() {
        Projet projet = projet(10L, utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));
        PieceJointe pieceJointe = pieceJointe(20L, "planning.pdf", "uploads/projets/10/generated.pdf", projet);
        Resource resource = new ByteArrayResource("contenu".getBytes());

        when(pieceJointeRepository.findById(20L)).thenReturn(Optional.of(pieceJointe));
        when(fileStorageService.load("uploads/projets/10/generated.pdf")).thenReturn(resource);

        PieceJointeDownloadResponse response = pieceJointeService.download(20L);

        assertThat(response.getNomFichier()).isEqualTo("planning.pdf");
        assertThat(response.getResource()).isSameAs(resource);
    }

    @Test
    void ownerProjectManagerCanDeleteAttachment() throws IOException {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, projectManager);
        PieceJointe pieceJointe = pieceJointe(20L, "planning.pdf", "uploads/projets/10/generated.pdf", projet);

        when(pieceJointeRepository.findById(20L)).thenReturn(Optional.of(pieceJointe));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));

        pieceJointeService.delete(20L, "pm@example.com");

        verify(fileStorageService).delete("uploads/projets/10/generated.pdf");
        verify(pieceJointeRepository).delete(pieceJointe);

        ArgumentCaptor<Projet> historiqueProjetCaptor = ArgumentCaptor.forClass(Projet.class);
        ArgumentCaptor<Utilisateur> historiqueUserCaptor = ArgumentCaptor.forClass(Utilisateur.class);
        ArgumentCaptor<TypeAction> historiqueActionCaptor = ArgumentCaptor.forClass(TypeAction.class);
        ArgumentCaptor<String> historiqueDescriptionCaptor = ArgumentCaptor.forClass(String.class);
        verify(historiqueService).record(
                historiqueProjetCaptor.capture(),
                historiqueUserCaptor.capture(),
                historiqueActionCaptor.capture(),
                historiqueDescriptionCaptor.capture());
        assertThat(historiqueActionCaptor.getValue()).isEqualTo(TypeAction.SUPPRESSION);
        assertThat(historiqueProjetCaptor.getValue()).isSameAs(projet);
        assertThat(historiqueUserCaptor.getValue()).isSameAs(projectManager);
        assertThat(historiqueDescriptionCaptor.getValue()).contains("Suppression de la piece jointe 20");
    }

    @Test
    void deleteRejectsNonOwnerProjectManager() throws IOException {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, utilisateur(2L, "other@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));
        PieceJointe pieceJointe = pieceJointe(20L, "planning.pdf", "uploads/projets/10/generated.pdf", projet);

        when(pieceJointeRepository.findById(20L)).thenReturn(Optional.of(pieceJointe));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));

        assertThatThrownBy(() -> pieceJointeService.delete(20L, "pm@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(fileStorageService, never()).delete(any(String.class));
        verify(pieceJointeRepository, never()).delete(any(PieceJointe.class));
        verify(historiqueService, never()).record(any(), any(), any(), any());
    }

    private MockMultipartFile file(String filename, String content) {
        return new MockMultipartFile("file", filename, "application/pdf", content.getBytes());
    }

    private PieceJointe pieceJointe(Long idPieceJointe, String nomFichier, String cheminFichier, Projet projet) {
        PieceJointe pieceJointe = new PieceJointe();
        pieceJointe.setIdPieceJointe(idPieceJointe);
        pieceJointe.setNomFichier(nomFichier);
        pieceJointe.setCheminFichier(cheminFichier);
        pieceJointe.setDateAjout(LocalDateTime.of(2026, 8, 5, 13, 0));
        pieceJointe.setProjet(projet);
        return pieceJointe;
    }

    private Projet projet(Long idProjet, Utilisateur responsable) {
        Projet projet = new Projet();
        projet.setIdProjet(idProjet);
        projet.setCode("PRJ-001");
        projet.setIntitule("Projet portefeuille");
        projet.setDateDebutPrevue(LocalDate.of(2026, 1, 1));
        projet.setDateFinPrevue(LocalDate.of(2026, 12, 31));
        projet.setStatut(StatutProjet.PLANIFIE);
        projet.setBudgetPrevisionnel(new BigDecimal("10000.00"));
        projet.setPriorite(PrioriteProjet.MOYENNE);
        projet.setPourcentageAvancement(0);
        projet.setUtilisateur(responsable);
        return projet;
    }

    private Utilisateur utilisateur(Long idUtilisateur, String email, RoleLibelle roleLibelle) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setIdUtilisateur(idUtilisateur);
        utilisateur.setEmail(email);
        utilisateur.setNom("Nom");
        utilisateur.setPrenom("Prenom");
        utilisateur.setRole(role(roleLibelle));
        return utilisateur;
    }

    private Role role(RoleLibelle roleLibelle) {
        Role role = new Role();
        role.setLibelle(roleLibelle);
        return role;
    }
}
