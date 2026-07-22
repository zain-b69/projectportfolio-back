package ma.onee.dsi.projectportfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import ma.onee.dsi.projectportfolio.dto.CreateProjetRequest;
import ma.onee.dsi.projectportfolio.dto.ProjetResponse;
import ma.onee.dsi.projectportfolio.dto.UpdateProjetRequest;
import ma.onee.dsi.projectportfolio.entity.HistoriqueModification;
import ma.onee.dsi.projectportfolio.entity.Projet;
import ma.onee.dsi.projectportfolio.entity.Role;
import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;
import ma.onee.dsi.projectportfolio.enums.RoleLibelle;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;
import ma.onee.dsi.projectportfolio.enums.TypeAction;
import ma.onee.dsi.projectportfolio.exception.BusinessRuleException;
import ma.onee.dsi.projectportfolio.repository.AffectationRessourceRepository;
import ma.onee.dsi.projectportfolio.repository.CoutRepository;
import ma.onee.dsi.projectportfolio.repository.HistoriqueModificationRepository;
import ma.onee.dsi.projectportfolio.repository.PieceJointeRepository;
import ma.onee.dsi.projectportfolio.repository.ProjetRepository;
import ma.onee.dsi.projectportfolio.repository.RisqueRepository;
import ma.onee.dsi.projectportfolio.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ProjetServiceTest {

    @Mock
    private ProjetRepository projetRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private HistoriqueModificationRepository historiqueModificationRepository;

    @Mock
    private CoutRepository coutRepository;

    @Mock
    private RisqueRepository risqueRepository;

    @Mock
    private PieceJointeRepository pieceJointeRepository;

    @Mock
    private AffectationRessourceRepository affectationRessourceRepository;

    private ProjetService projetService;

    @BeforeEach
    void setUp() {
        projetService = new ProjetService(
                projetRepository,
                utilisateurRepository,
                historiqueModificationRepository,
                coutRepository,
                risqueRepository,
                pieceJointeRepository,
                affectationRessourceRepository
        );
    }

    @Test
    void createProjetAssignsAuthenticatedProjectManagerAndRecordsHistory() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        CreateProjetRequest request = plannedProjectRequest();

        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(projetRepository.existsByCodeIgnoreCase("PRJ-001")).thenReturn(false);
        when(projetRepository.save(any(Projet.class))).thenAnswer(invocation -> {
            Projet savedProjet = invocation.getArgument(0);
            savedProjet.setIdProjet(10L);
            return savedProjet;
        });

        ProjetResponse response = projetService.createProjet(request, "pm@example.com");

        assertThat(response.getIdProjet()).isEqualTo(10L);
        assertThat(response.getIdResponsable()).isEqualTo(1L);
        assertThat(response.getEmailResponsable()).isEqualTo("pm@example.com");

        ArgumentCaptor<HistoriqueModification> historiqueCaptor =
                ArgumentCaptor.forClass(HistoriqueModification.class);
        verify(historiqueModificationRepository).save(historiqueCaptor.capture());

        HistoriqueModification historique = historiqueCaptor.getValue();
        assertThat(historique.getTypeAction()).isEqualTo(TypeAction.CREATION);
        assertThat(historique.getUtilisateur()).isSameAs(projectManager);
        assertThat(historique.getProjet().getCode()).isEqualTo("PRJ-001");
        assertThat(historique.getDescription()).contains("Creation du projet PRJ-001");
    }

    @Test
    void createProjetRejectsSimpleUser() {
        Utilisateur simpleUser = utilisateur(2L, "user@example.com", RoleLibelle.ROLE_UTILISATEUR_SIMPLE);

        when(utilisateurRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(simpleUser));

        assertThatThrownBy(() -> projetService.createProjet(plannedProjectRequest(), "user@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(projetRepository, never()).save(any(Projet.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    @Test
    void deleteProjetByResponsibleProjectManagerDetachesHistoryDeletesProjectAndRecordsSuppression() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(20L, projectManager);

        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(projetRepository.findById(20L)).thenReturn(Optional.of(projet));

        projetService.deleteProjet(20L, "pm@example.com");

        verify(historiqueModificationRepository).detachProjet(20L);
        verify(projetRepository).delete(projet);

        ArgumentCaptor<HistoriqueModification> historiqueCaptor =
                ArgumentCaptor.forClass(HistoriqueModification.class);
        verify(historiqueModificationRepository).save(historiqueCaptor.capture());

        HistoriqueModification historique = historiqueCaptor.getValue();
        assertThat(historique.getTypeAction()).isEqualTo(TypeAction.SUPPRESSION);
        assertThat(historique.getUtilisateur()).isSameAs(projectManager);
        assertThat(historique.getProjet()).isNull();
        assertThat(historique.getDescription()).contains("Suppression du projet PRJ-001");
    }

    @Test
    void deleteProjetRejectsSimpleUser() {
        Utilisateur simpleUser = utilisateur(2L, "user@example.com", RoleLibelle.ROLE_UTILISATEUR_SIMPLE);

        when(utilisateurRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(simpleUser));

        assertThatThrownBy(() -> projetService.deleteProjet(20L, "user@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(projetRepository, never()).delete(any(Projet.class));
        verify(historiqueModificationRepository, never()).detachProjet(any(Long.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    @Test
    void deleteProjetRejectsNonResponsibleProjectManager() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Utilisateur otherProjectManager = utilisateur(2L, "other@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(20L, otherProjectManager);

        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(projetRepository.findById(20L)).thenReturn(Optional.of(projet));

        assertThatThrownBy(() -> projetService.deleteProjet(20L, "pm@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(projetRepository, never()).delete(any(Projet.class));
        verify(historiqueModificationRepository, never()).detachProjet(any(Long.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    @Test
    void deleteProjetRejectsProjectLinkedToOtherModuleData() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(20L, projectManager);

        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(projetRepository.findById(20L)).thenReturn(Optional.of(projet));
        when(coutRepository.countByProjet_IdProjet(20L)).thenReturn(1L);

        assertThatThrownBy(() -> projetService.deleteProjet(20L, "pm@example.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Suppression impossible");

        verify(projetRepository, never()).delete(any(Projet.class));
        verify(historiqueModificationRepository, never()).detachProjet(any(Long.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    @Test
    void updateProjetRejectsNonResponsibleProjectManager() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Utilisateur otherProjectManager = utilisateur(2L, "other@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(20L, otherProjectManager);

        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(projetRepository.findById(20L)).thenReturn(Optional.of(projet));

        assertThatThrownBy(() -> projetService.updateProjet(20L, updateProjectRequest(), "pm@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(projetRepository, never()).save(any(Projet.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    @Test
    void createProjetRejectsInvalidPlannedDateRange() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        CreateProjetRequest request = plannedProjectRequest();
        request.setDateFinPrevue(request.getDateDebutPrevue().minusDays(1));

        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(projetRepository.existsByCodeIgnoreCase("PRJ-001")).thenReturn(false);

        assertThatThrownBy(() -> projetService.createProjet(request, "pm@example.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("fin prevue");

        verify(projetRepository, never()).save(any(Projet.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    @Test
    void createProjetRejectsFinishedStatusWithoutFullProgress() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        CreateProjetRequest request = plannedProjectRequest();
        request.setStatut(StatutProjet.TERMINE);
        request.setPourcentageAvancement(90);
        request.setDateDebutReelle(LocalDate.of(2026, 1, 2));
        request.setDateFinReelle(LocalDate.of(2026, 2, 1));

        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(projetRepository.existsByCodeIgnoreCase("PRJ-001")).thenReturn(false);

        assertThatThrownBy(() -> projetService.createProjet(request, "pm@example.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("avancement egal a 100");

        verify(projetRepository, never()).save(any(Projet.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    private CreateProjetRequest plannedProjectRequest() {
        CreateProjetRequest request = new CreateProjetRequest();
        request.setCode("PRJ-001");
        request.setIntitule("Projet portefeuille");
        request.setDescriptif("Descriptif projet");
        request.setDateDebutPrevue(LocalDate.of(2026, 1, 1));
        request.setDateFinPrevue(LocalDate.of(2026, 12, 31));
        request.setStatut(StatutProjet.PLANIFIE);
        request.setBudgetPrevisionnel(new BigDecimal("10000.00"));
        request.setPriorite(PrioriteProjet.MOYENNE);
        request.setPourcentageAvancement(0);
        return request;
    }

    private UpdateProjetRequest updateProjectRequest() {
        UpdateProjetRequest request = new UpdateProjetRequest();
        request.setCode("PRJ-001");
        request.setIntitule("Projet portefeuille");
        request.setDescriptif("Descriptif projet");
        request.setDateDebutPrevue(LocalDate.of(2026, 1, 1));
        request.setDateFinPrevue(LocalDate.of(2026, 12, 31));
        request.setStatut(StatutProjet.PLANIFIE);
        request.setBudgetPrevisionnel(new BigDecimal("10000.00"));
        request.setPriorite(PrioriteProjet.MOYENNE);
        request.setPourcentageAvancement(0);
        return request;
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
