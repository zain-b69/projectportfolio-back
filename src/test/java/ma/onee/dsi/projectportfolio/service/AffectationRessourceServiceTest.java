package ma.onee.dsi.projectportfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import ma.onee.dsi.projectportfolio.dto.AffectationRessourceResponse;
import ma.onee.dsi.projectportfolio.dto.CreateAffectationRessourceRequest;
import ma.onee.dsi.projectportfolio.dto.UpdateAffectationRessourceRequest;
import ma.onee.dsi.projectportfolio.entity.AffectationRessource;
import ma.onee.dsi.projectportfolio.entity.Projet;
import ma.onee.dsi.projectportfolio.entity.Ressource;
import ma.onee.dsi.projectportfolio.entity.Role;
import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import ma.onee.dsi.projectportfolio.enums.NatureIntervention;
import ma.onee.dsi.projectportfolio.enums.NatureRessource;
import ma.onee.dsi.projectportfolio.enums.RoleLibelle;
import ma.onee.dsi.projectportfolio.exception.BusinessRuleException;
import ma.onee.dsi.projectportfolio.exception.DuplicateResourceException;
import ma.onee.dsi.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dsi.projectportfolio.repository.AffectationRessourceRepository;
import ma.onee.dsi.projectportfolio.repository.ProjetRepository;
import ma.onee.dsi.projectportfolio.repository.RessourceRepository;
import ma.onee.dsi.projectportfolio.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AffectationRessourceServiceTest {

    @Mock
    private AffectationRessourceRepository affectationRessourceRepository;

    @Mock
    private ProjetRepository projetRepository;

    @Mock
    private RessourceRepository ressourceRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private HistoriqueService historiqueService;

    private AffectationRessourceService affectationRessourceService;

    @BeforeEach
    void setUp() {
        affectationRessourceService = new AffectationRessourceService(
                affectationRessourceRepository,
                projetRepository,
                ressourceRepository,
                utilisateurRepository,
                historiqueService
        );
    }

    @Test
    void getAffectationsByProjetReturnsProjectAffectations() {
        Projet projet = projet(10L, projectManager(1L, "pm@example.com"));
        Ressource ressource = ressource(20L);
        AffectationRessource affectation = affectation(30L, projet, ressource);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(affectationRessourceRepository.findByProjet_IdProjet(10L)).thenReturn(List.of(affectation));

        List<AffectationRessourceResponse> responses = affectationRessourceService.getAffectationsByProjet(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getIdAffectationRessource()).isEqualTo(30L);
        assertThat(responses.get(0).getIdProjet()).isEqualTo(10L);
        assertThat(responses.get(0).getIdRessource()).isEqualTo(20L);
    }

    @Test
    void getAffectationsByRessourceReturnsResourceAffectations() {
        Projet projet = projet(10L, projectManager(1L, "pm@example.com"));
        Ressource ressource = ressource(20L);
        AffectationRessource affectation = affectation(30L, projet, ressource);

        when(ressourceRepository.findById(20L)).thenReturn(Optional.of(ressource));
        when(affectationRessourceRepository.findByRessource_IdRessource(20L)).thenReturn(List.of(affectation));

        List<AffectationRessourceResponse> responses = affectationRessourceService.getAffectationsByRessource(20L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getIdAffectationRessource()).isEqualTo(30L);
        assertThat(responses.get(0).getNomRessource()).isEqualTo("Karim Bennani");
    }

    @Test
    void getAffectationsByProjetRejectsUnknownProject() {
        when(projetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> affectationRessourceService.getAffectationsByProjet(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Projet introuvable");
    }

    @Test
    void getAffectationsByRessourceRejectsUnknownResource() {
        when(ressourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> affectationRessourceService.getAffectationsByRessource(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ressource introuvable");
    }

    @Test
    void createAffectationAllowsResponsibleProjectManagerForOwnProject() {
        Utilisateur projectManager = projectManager(1L, "pm@example.com");
        Projet projet = projet(10L, projectManager);
        Ressource ressource = ressource(20L);
        CreateAffectationRessourceRequest request = createRequest();

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(ressourceRepository.findById(20L)).thenReturn(Optional.of(ressource));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(affectationRessourceRepository.existsByProjet_IdProjetAndRessource_IdRessource(10L, 20L))
                .thenReturn(false);
        when(affectationRessourceRepository.save(any(AffectationRessource.class))).thenAnswer(invocation -> {
            AffectationRessource savedAffectation = invocation.getArgument(0);
            savedAffectation.setIdAffectationRessource(30L);
            return savedAffectation;
        });

        AffectationRessourceResponse response =
                affectationRessourceService.createAffectation(request, "pm@example.com");

        assertThat(response.getIdAffectationRessource()).isEqualTo(30L);
        assertThat(response.getIdProjet()).isEqualTo(10L);
        assertThat(response.getIdRessource()).isEqualTo(20L);
        assertThat(response.getNatureIntervention()).isEqualTo("DEVELOPPEMENT");
        assertThat(response.getChargeJH()).isEqualByComparingTo("5.50");

        ArgumentCaptor<AffectationRessource> captor = ArgumentCaptor.forClass(AffectationRessource.class);
        verify(affectationRessourceRepository).save(captor.capture());
        assertThat(captor.getValue().getProjet()).isSameAs(projet);
        assertThat(captor.getValue().getRessource()).isSameAs(ressource);
    }

    @Test
    void createAffectationAllowsAdminForAnyProject() {
        Utilisateur admin = utilisateur(2L, "admin@example.com", RoleLibelle.ROLE_ADMIN);
        Utilisateur projectManager = projectManager(1L, "pm@example.com");
        Projet projet = projet(10L, projectManager);
        Ressource ressource = ressource(20L);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(ressourceRepository.findById(20L)).thenReturn(Optional.of(ressource));
        when(utilisateurRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        when(affectationRessourceRepository.existsByProjet_IdProjetAndRessource_IdRessource(10L, 20L))
                .thenReturn(false);
        when(affectationRessourceRepository.save(any(AffectationRessource.class))).thenAnswer(invocation -> {
            AffectationRessource savedAffectation = invocation.getArgument(0);
            savedAffectation.setIdAffectationRessource(30L);
            return savedAffectation;
        });

        AffectationRessourceResponse response =
                affectationRessourceService.createAffectation(createRequest(), "admin@example.com");

        assertThat(response.getIdAffectationRessource()).isEqualTo(30L);
        verify(affectationRessourceRepository).save(any(AffectationRessource.class));
    }

    @Test
    void createAffectationRejectsNonResponsibleProjectManager() {
        Utilisateur currentUser = projectManager(2L, "other@example.com");
        Utilisateur owner = projectManager(1L, "pm@example.com");
        Projet projet = projet(10L, owner);
        Ressource ressource = ressource(20L);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(ressourceRepository.findById(20L)).thenReturn(Optional.of(ressource));
        when(utilisateurRepository.findByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> affectationRessourceService.createAffectation(createRequest(), "other@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(affectationRessourceRepository, never()).save(any(AffectationRessource.class));
    }

    @Test
    void createAffectationRejectsSimpleUser() {
        Utilisateur simpleUser = utilisateur(3L, "user@example.com", RoleLibelle.ROLE_UTILISATEUR_SIMPLE);
        Projet projet = projet(10L, projectManager(1L, "pm@example.com"));
        Ressource ressource = ressource(20L);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(ressourceRepository.findById(20L)).thenReturn(Optional.of(ressource));
        when(utilisateurRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(simpleUser));

        assertThatThrownBy(() -> affectationRessourceService.createAffectation(createRequest(), "user@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(affectationRessourceRepository, never()).save(any(AffectationRessource.class));
    }

    @Test
    void createAffectationRejectsDuplicateProjectResource() {
        Utilisateur projectManager = projectManager(1L, "pm@example.com");
        Projet projet = projet(10L, projectManager);
        Ressource ressource = ressource(20L);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(ressourceRepository.findById(20L)).thenReturn(Optional.of(ressource));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(affectationRessourceRepository.existsByProjet_IdProjetAndRessource_IdRessource(10L, 20L))
                .thenReturn(true);

        assertThatThrownBy(() -> affectationRessourceService.createAffectation(createRequest(), "pm@example.com"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Ressource deja affectee au projet");

        verify(affectationRessourceRepository, never()).save(any(AffectationRessource.class));
    }

    @Test
    void updateAffectationUpdatesOnlyNatureAndCharge() {
        Utilisateur projectManager = projectManager(1L, "pm@example.com");
        Projet projet = projet(10L, projectManager);
        Ressource ressource = ressource(20L);
        AffectationRessource affectation = affectation(30L, projet, ressource);
        UpdateAffectationRessourceRequest request = updateRequest();

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(affectationRessourceRepository.findByIdAffectationRessourceAndProjet_IdProjet(30L, 10L))
                .thenReturn(Optional.of(affectation));
        when(affectationRessourceRepository.save(affectation)).thenReturn(affectation);

        AffectationRessourceResponse response =
                affectationRessourceService.updateAffectation(10L, 30L, request, "pm@example.com");

        assertThat(response.getNatureIntervention()).isEqualTo("TEST");
        assertThat(response.getChargeJH()).isEqualByComparingTo("3.00");
        assertThat(affectation.getRessource()).isSameAs(ressource);
        verify(affectationRessourceRepository).save(affectation);
    }

    @Test
    void updateAffectationRejectsAffectationFromAnotherProject() {
        Utilisateur projectManager = projectManager(1L, "pm@example.com");
        Projet projet = projet(10L, projectManager);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(affectationRessourceRepository.findByIdAffectationRessourceAndProjet_IdProjet(30L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                affectationRessourceService.updateAffectation(10L, 30L, updateRequest(), "pm@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Affectation introuvable");

        verify(affectationRessourceRepository, never()).save(any(AffectationRessource.class));
    }

    @Test
    void updateAffectationRejectsNullOrNegativeCharge() {
        Utilisateur projectManager = projectManager(1L, "pm@example.com");
        Projet projet = projet(10L, projectManager);
        AffectationRessource affectation = affectation(30L, projet, ressource(20L));
        UpdateAffectationRessourceRequest request = updateRequest();
        request.setChargeJH(BigDecimal.ZERO);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(affectationRessourceRepository.findByIdAffectationRessourceAndProjet_IdProjet(30L, 10L))
                .thenReturn(Optional.of(affectation));

        assertThatThrownBy(() ->
                affectationRessourceService.updateAffectation(10L, 30L, request, "pm@example.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La charge JH doit etre strictement positive");

        request.setChargeJH(new BigDecimal("-1.00"));

        assertThatThrownBy(() ->
                affectationRessourceService.updateAffectation(10L, 30L, request, "pm@example.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La charge JH doit etre strictement positive");

        verify(affectationRessourceRepository, never()).save(any(AffectationRessource.class));
    }

    @Test
    void deleteAffectationDeletesExistingProjectAffectation() {
        Utilisateur projectManager = projectManager(1L, "pm@example.com");
        Projet projet = projet(10L, projectManager);
        AffectationRessource affectation = affectation(30L, projet, ressource(20L));

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(affectationRessourceRepository.findByIdAffectationRessourceAndProjet_IdProjet(30L, 10L))
                .thenReturn(Optional.of(affectation));

        affectationRessourceService.deleteAffectation(10L, 30L, "pm@example.com");

        verify(affectationRessourceRepository).delete(affectation);
    }

    @Test
    void deleteAffectationRejectsNonResponsibleProjectManager() {
        Utilisateur currentUser = projectManager(2L, "other@example.com");
        Utilisateur owner = projectManager(1L, "pm@example.com");
        Projet projet = projet(10L, owner);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> affectationRessourceService.deleteAffectation(10L, 30L, "other@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(affectationRessourceRepository, never()).delete(any(AffectationRessource.class));
    }

    private CreateAffectationRessourceRequest createRequest() {
        CreateAffectationRessourceRequest request = new CreateAffectationRessourceRequest();
        request.setIdProjet(10L);
        request.setIdRessource(20L);
        request.setNatureIntervention(NatureIntervention.DEVELOPPEMENT);
        request.setChargeJH(new BigDecimal("5.50"));
        return request;
    }

    private UpdateAffectationRessourceRequest updateRequest() {
        UpdateAffectationRessourceRequest request = new UpdateAffectationRessourceRequest();
        request.setNatureIntervention(NatureIntervention.TEST);
        request.setChargeJH(new BigDecimal("3.00"));
        return request;
    }

    private AffectationRessource affectation(Long idAffectation, Projet projet, Ressource ressource) {
        AffectationRessource affectation = new AffectationRessource();
        affectation.setIdAffectationRessource(idAffectation);
        affectation.setProjet(projet);
        affectation.setRessource(ressource);
        affectation.setNatureIntervention(NatureIntervention.DEVELOPPEMENT);
        affectation.setChargeJH(new BigDecimal("5.50"));
        return affectation;
    }

    private Projet projet(Long idProjet, Utilisateur responsable) {
        Projet projet = new Projet();
        projet.setIdProjet(idProjet);
        projet.setCode("PRJ-001");
        projet.setIntitule("Projet portefeuille");
        projet.setUtilisateur(responsable);
        return projet;
    }

    private Ressource ressource(Long idRessource) {
        Ressource ressource = new Ressource();
        ressource.setIdRessource(idRessource);
        ressource.setNom("Karim Bennani");
        ressource.setFonction("Developpeur");
        ressource.setNature(NatureRessource.INTERNE);
        return ressource;
    }

    private Utilisateur projectManager(Long idUtilisateur, String email) {
        return utilisateur(idUtilisateur, email, RoleLibelle.ROLE_RESPONSABLE_PROJET);
    }

    private Utilisateur utilisateur(Long idUtilisateur, String email, RoleLibelle roleLibelle) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setIdUtilisateur(idUtilisateur);
        utilisateur.setEmail(email);
        utilisateur.setRole(role(roleLibelle));
        return utilisateur;
    }

    private Role role(RoleLibelle roleLibelle) {
        Role role = new Role();
        role.setLibelle(roleLibelle);
        return role;
    }
}
