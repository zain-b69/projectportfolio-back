package ma.onee.dsi.projectportfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import ma.onee.dsi.projectportfolio.dto.CreateRisqueRequest;
import ma.onee.dsi.projectportfolio.dto.RisqueResponse;
import ma.onee.dsi.projectportfolio.dto.UpdateRisqueRequest;
import ma.onee.dsi.projectportfolio.entity.Projet;
import ma.onee.dsi.projectportfolio.entity.Risque;
import ma.onee.dsi.projectportfolio.entity.Role;
import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import ma.onee.dsi.projectportfolio.enums.NiveauCriticite;
import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;
import ma.onee.dsi.projectportfolio.enums.RoleLibelle;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;
import ma.onee.dsi.projectportfolio.enums.TypeAction;
import ma.onee.dsi.projectportfolio.exception.ResourceNotFoundException;
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
class RisqueServiceTest {

    @Mock
    private RisqueRepository risqueRepository;

    @Mock
    private ProjetRepository projetRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private HistoriqueService historiqueService;

    private RisqueService risqueService;

    @BeforeEach
    void setUp() {
        risqueService = new RisqueService(
                risqueRepository,
                projetRepository,
                utilisateurRepository,
                historiqueService
        );
    }

    @Test
    void authenticatedUsersCanListRisksForProject() {
        Projet projet = projet(10L, utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));
        Risque risque = risque(20L, "Risque budget", NiveauCriticite.ELEVE, projet);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(risqueRepository.findByProjet_IdProjet(10L)).thenReturn(List.of(risque));

        List<RisqueResponse> responses = risqueService.getRisquesByProjet(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getIdRisque()).isEqualTo(20L);
        assertThat(responses.get(0).getDescription()).isEqualTo("Risque budget");
        assertThat(responses.get(0).getNiveauCriticite()).isEqualTo(NiveauCriticite.ELEVE);
        assertThat(responses.get(0).getIdProjet()).isEqualTo(10L);
    }

    @Test
    void existingRiskCanBeConsulted() {
        Projet projet = projet(10L, utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));
        Risque risque = risque(20L, "Risque planning", NiveauCriticite.MOYEN, projet);

        when(risqueRepository.findById(20L)).thenReturn(Optional.of(risque));

        RisqueResponse response = risqueService.getRisqueById(20L);

        assertThat(response.getIdRisque()).isEqualTo(20L);
        assertThat(response.getDescription()).isEqualTo("Risque planning");
        assertThat(response.getNiveauCriticite()).isEqualTo(NiveauCriticite.MOYEN);
        assertThat(response.getIdProjet()).isEqualTo(10L);
    }

    @Test
    void ownerProjectManagerCanCreateRisk() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, projectManager);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(risqueRepository.save(any(Risque.class))).thenAnswer(invocation -> {
            Risque savedRisque = invocation.getArgument(0);
            savedRisque.setIdRisque(20L);
            return savedRisque;
        });

        RisqueResponse response = risqueService.createRisque(10L, createRequest("  Risque budget  "), "pm@example.com");

        assertThat(response.getIdRisque()).isEqualTo(20L);
        assertThat(response.getDescription()).isEqualTo("Risque budget");
        assertThat(response.getNiveauCriticite()).isEqualTo(NiveauCriticite.ELEVE);
        assertThat(response.getIdProjet()).isEqualTo(10L);

        ArgumentCaptor<Risque> risqueCaptor = ArgumentCaptor.forClass(Risque.class);
        verify(risqueRepository).save(risqueCaptor.capture());
        assertThat(risqueCaptor.getValue().getDescription()).isEqualTo("Risque budget");
        assertThat(risqueCaptor.getValue().getProjet()).isSameAs(projet);

        ArgumentCaptor<Projet> historiqueProjetCaptor = ArgumentCaptor.forClass(Projet.class);
        ArgumentCaptor<Utilisateur> historiqueUserCaptor = ArgumentCaptor.forClass(Utilisateur.class);
        ArgumentCaptor<TypeAction> historiqueActionCaptor = ArgumentCaptor.forClass(TypeAction.class);
        ArgumentCaptor<String> historiqueDescriptionCaptor = ArgumentCaptor.forClass(String.class);
        verify(historiqueService).record(
                historiqueProjetCaptor.capture(),
                historiqueUserCaptor.capture(),
                historiqueActionCaptor.capture(),
                historiqueDescriptionCaptor.capture());
        assertThat(historiqueActionCaptor.getValue()).isEqualTo(TypeAction.AJOUT_RISQUE);
        assertThat(historiqueProjetCaptor.getValue()).isSameAs(projet);
        assertThat(historiqueUserCaptor.getValue()).isSameAs(projectManager);
    }

    @Test
    void nonOwnerProjectManagerCannotCreateRisk() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Utilisateur otherProjectManager = utilisateur(2L, "other@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, otherProjectManager);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));

        assertThatThrownBy(() -> risqueService.createRisque(10L, createRequest("Risque budget"), "pm@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(risqueRepository, never()).save(any(Risque.class));
        verify(historiqueService, never()).record(any(), any(), any(), any());
    }

    @Test
    void adminCannotCreateRisk() {
        Utilisateur admin = utilisateur(1L, "admin@example.com", RoleLibelle.ROLE_ADMIN);
        Projet projet = projet(10L, utilisateur(2L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> risqueService.createRisque(10L, createRequest("Risque budget"), "admin@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(risqueRepository, never()).save(any(Risque.class));
        verify(historiqueService, never()).record(any(), any(), any(), any());
    }

    @Test
    void simpleUserCannotCreateRisk() {
        Utilisateur simpleUser = utilisateur(1L, "user@example.com", RoleLibelle.ROLE_UTILISATEUR_SIMPLE);
        Projet projet = projet(10L, utilisateur(2L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(simpleUser));

        assertThatThrownBy(() -> risqueService.createRisque(10L, createRequest("Risque budget"), "user@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(risqueRepository, never()).save(any(Risque.class));
        verify(historiqueService, never()).record(any(), any(), any(), any());
    }

    @Test
    void ownerProjectManagerCanUpdateRisk() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, projectManager);
        Risque risque = risque(20L, "Ancien risque", NiveauCriticite.FAIBLE, projet);

        when(risqueRepository.findById(20L)).thenReturn(Optional.of(risque));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(risqueRepository.save(risque)).thenReturn(risque);

        RisqueResponse response = risqueService.updateRisque(20L, updateRequest("  Risque corrige  "), "pm@example.com");

        assertThat(response.getDescription()).isEqualTo("Risque corrige");
        assertThat(response.getNiveauCriticite()).isEqualTo(NiveauCriticite.CRITIQUE);
        assertThat(response.getIdProjet()).isEqualTo(10L);
        assertThat(risque.getProjet()).isSameAs(projet);

        verify(risqueRepository).save(risque);

        ArgumentCaptor<Projet> historiqueProjetCaptor = ArgumentCaptor.forClass(Projet.class);
        ArgumentCaptor<Utilisateur> historiqueUserCaptor = ArgumentCaptor.forClass(Utilisateur.class);
        ArgumentCaptor<TypeAction> historiqueActionCaptor = ArgumentCaptor.forClass(TypeAction.class);
        ArgumentCaptor<String> historiqueDescriptionCaptor = ArgumentCaptor.forClass(String.class);
        verify(historiqueService).record(
                historiqueProjetCaptor.capture(),
                historiqueUserCaptor.capture(),
                historiqueActionCaptor.capture(),
                historiqueDescriptionCaptor.capture());
        assertThat(historiqueActionCaptor.getValue()).isEqualTo(TypeAction.MODIFICATION);
    }

    @Test
    void nonOwnerProjectManagerCannotUpdateRisk() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, utilisateur(2L, "other@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));
        Risque risque = risque(20L, "Risque budget", NiveauCriticite.ELEVE, projet);

        when(risqueRepository.findById(20L)).thenReturn(Optional.of(risque));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));

        assertThatThrownBy(() -> risqueService.updateRisque(20L, updateRequest("Risque corrige"), "pm@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(risqueRepository, never()).save(any(Risque.class));
        verify(historiqueService, never()).record(any(), any(), any(), any());
    }

    @Test
    void adminCannotUpdateRisk() {
        Utilisateur admin = utilisateur(1L, "admin@example.com", RoleLibelle.ROLE_ADMIN);
        Projet projet = projet(10L, utilisateur(2L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));
        Risque risque = risque(20L, "Risque budget", NiveauCriticite.ELEVE, projet);

        when(risqueRepository.findById(20L)).thenReturn(Optional.of(risque));
        when(utilisateurRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> risqueService.updateRisque(20L, updateRequest("Risque corrige"), "admin@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(risqueRepository, never()).save(any(Risque.class));
        verify(historiqueService, never()).record(any(), any(), any(), any());
    }

    @Test
    void ownerProjectManagerCanDeleteRisk() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, projectManager);
        Risque risque = risque(20L, "Risque budget", NiveauCriticite.ELEVE, projet);

        when(risqueRepository.findById(20L)).thenReturn(Optional.of(risque));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));

        risqueService.deleteRisque(20L, "pm@example.com");

        verify(risqueRepository).delete(risque);

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
    }

    @Test
    void nonOwnerProjectManagerCannotDeleteRisk() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, utilisateur(2L, "other@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));
        Risque risque = risque(20L, "Risque budget", NiveauCriticite.ELEVE, projet);

        when(risqueRepository.findById(20L)).thenReturn(Optional.of(risque));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));

        assertThatThrownBy(() -> risqueService.deleteRisque(20L, "pm@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(risqueRepository, never()).delete(any(Risque.class));
        verify(historiqueService, never()).record(any(), any(), any(), any());
    }

    @Test
    void adminCannotDeleteRisk() {
        Utilisateur admin = utilisateur(1L, "admin@example.com", RoleLibelle.ROLE_ADMIN);
        Projet projet = projet(10L, utilisateur(2L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));
        Risque risque = risque(20L, "Risque budget", NiveauCriticite.ELEVE, projet);

        when(risqueRepository.findById(20L)).thenReturn(Optional.of(risque));
        when(utilisateurRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> risqueService.deleteRisque(20L, "admin@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(risqueRepository, never()).delete(any(Risque.class));
        verify(historiqueService, never()).record(any(), any(), any(), any());
    }

    @Test
    void missingProjectProducesNotFoundException() {
        when(projetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> risqueService.getRisquesByProjet(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Projet introuvable");
    }

    @Test
    void missingRiskProducesNotFoundException() {
        when(risqueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> risqueService.getRisqueById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Risque introuvable");
    }

    @Test
    void entityToResponseMappingReturnsCorrectProjectId() {
        Projet projet = projet(42L, utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));
        Risque risque = risque(20L, "Risque budget", NiveauCriticite.ELEVE, projet);

        when(risqueRepository.findById(20L)).thenReturn(Optional.of(risque));

        RisqueResponse response = risqueService.getRisqueById(20L);

        assertThat(response.getIdProjet()).isEqualTo(42L);
    }

    @Test
    void descriptionIsTrimmedBeforeSavingOnUpdate() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, projectManager);
        Risque risque = risque(20L, "Ancien risque", NiveauCriticite.FAIBLE, projet);

        when(risqueRepository.findById(20L)).thenReturn(Optional.of(risque));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(risqueRepository.save(risque)).thenReturn(risque);

        risqueService.updateRisque(20L, updateRequest("  Nouvelle description  "), "pm@example.com");

        ArgumentCaptor<Risque> risqueCaptor = ArgumentCaptor.forClass(Risque.class);
        verify(risqueRepository).save(risqueCaptor.capture());
        assertThat(risqueCaptor.getValue().getDescription()).isEqualTo("Nouvelle description");
    }

    private CreateRisqueRequest createRequest(String description) {
        CreateRisqueRequest request = new CreateRisqueRequest();
        request.setDescription(description);
        request.setNiveauCriticite(NiveauCriticite.ELEVE);
        return request;
    }

    private UpdateRisqueRequest updateRequest(String description) {
        UpdateRisqueRequest request = new UpdateRisqueRequest();
        request.setDescription(description);
        request.setNiveauCriticite(NiveauCriticite.CRITIQUE);
        return request;
    }

    private Risque risque(Long idRisque, String description, NiveauCriticite niveauCriticite, Projet projet) {
        Risque risque = new Risque();
        risque.setIdRisque(idRisque);
        risque.setDescription(description);
        risque.setNiveauCriticite(niveauCriticite);
        risque.setProjet(projet);
        return risque;
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
