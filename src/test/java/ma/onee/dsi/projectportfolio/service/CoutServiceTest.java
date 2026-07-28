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
import ma.onee.dsi.projectportfolio.dto.CoutResponse;
import ma.onee.dsi.projectportfolio.dto.CreateCoutRequest;
import ma.onee.dsi.projectportfolio.dto.UpdateCoutRequest;
import ma.onee.dsi.projectportfolio.entity.Cout;
import ma.onee.dsi.projectportfolio.entity.HistoriqueModification;
import ma.onee.dsi.projectportfolio.entity.Projet;
import ma.onee.dsi.projectportfolio.entity.Role;
import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;
import ma.onee.dsi.projectportfolio.enums.RoleLibelle;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;
import ma.onee.dsi.projectportfolio.enums.TypeAction;
import ma.onee.dsi.projectportfolio.enums.TypeCout;
import ma.onee.dsi.projectportfolio.exception.BusinessRuleException;
import ma.onee.dsi.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dsi.projectportfolio.repository.CoutRepository;
import ma.onee.dsi.projectportfolio.repository.HistoriqueModificationRepository;
import ma.onee.dsi.projectportfolio.repository.ProjetRepository;
import ma.onee.dsi.projectportfolio.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CoutServiceTest {

    @Mock
    private CoutRepository coutRepository;

    @Mock
    private ProjetRepository projetRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private HistoriqueModificationRepository historiqueModificationRepository;

    private CoutService coutService;

    @BeforeEach
    void setUp() {
        coutService = new CoutService(
                coutRepository,
                projetRepository,
                utilisateurRepository,
                historiqueModificationRepository
        );
    }

    @Test
    void authenticatedUsersCanListCostsForProject() {
        Projet projet = projet(10L, utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));
        Cout cout = cout(20L, TypeCout.MATERIEL, new BigDecimal("1200.50"), LocalDate.of(2026, 2, 1), projet);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(coutRepository.findByProjet_IdProjet(10L)).thenReturn(List.of(cout));

        List<CoutResponse> responses = coutService.getCoutsByProjet(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getIdCout()).isEqualTo(20L);
        assertThat(responses.get(0).getType()).isEqualTo(TypeCout.MATERIEL);
        assertThat(responses.get(0).getMontant()).isEqualByComparingTo("1200.50");
        assertThat(responses.get(0).getDateCout()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(responses.get(0).getIdProjet()).isEqualTo(10L);
    }

    @Test
    void getCoutByIdReturnsExpectedResponse() {
        Projet projet = projet(10L, utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));
        Cout cout = cout(20L, TypeCout.LOGICIEL, new BigDecimal("500.00"), LocalDate.of(2026, 3, 1), projet);

        when(coutRepository.findById(20L)).thenReturn(Optional.of(cout));

        CoutResponse response = coutService.getCoutById(20L);

        assertThat(response.getIdCout()).isEqualTo(20L);
        assertThat(response.getType()).isEqualTo(TypeCout.LOGICIEL);
        assertThat(response.getMontant()).isEqualByComparingTo("500.00");
        assertThat(response.getDateCout()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(response.getIdProjet()).isEqualTo(10L);
    }

    @Test
    void entityToResponseMappingReturnsCorrectProjectId() {
        Projet projet = projet(42L, utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));
        Cout cout = cout(20L, TypeCout.FORMATION, new BigDecimal("750.00"), LocalDate.of(2026, 4, 1), projet);

        when(coutRepository.findById(20L)).thenReturn(Optional.of(cout));

        CoutResponse response = coutService.getCoutById(20L);

        assertThat(response.getIdProjet()).isEqualTo(42L);
    }

    @Test
    void missingProjectProducesNotFoundException() {
        when(projetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coutService.getCoutsByProjet(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Projet introuvable");
    }

    @Test
    void missingCostProducesNotFoundException() {
        when(coutRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coutService.getCoutById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cout introuvable");
    }

    @Test
    void ownerProjectManagerCanCreateCost() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, projectManager);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(coutRepository.save(any(Cout.class))).thenAnswer(invocation -> {
            Cout savedCout = invocation.getArgument(0);
            savedCout.setIdCout(20L);
            return savedCout;
        });

        CoutResponse response = coutService.createCout(10L, createRequest(), "pm@example.com");

        assertThat(response.getIdCout()).isEqualTo(20L);
        assertThat(response.getType()).isEqualTo(TypeCout.MATERIEL);
        assertThat(response.getMontant()).isEqualByComparingTo("1200.50");
        assertThat(response.getDateCout()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(response.getIdProjet()).isEqualTo(10L);

        ArgumentCaptor<Cout> coutCaptor = ArgumentCaptor.forClass(Cout.class);
        verify(coutRepository).save(coutCaptor.capture());
        assertThat(coutCaptor.getValue().getType()).isEqualTo(TypeCout.MATERIEL);
        assertThat(coutCaptor.getValue().getMontant()).isEqualByComparingTo("1200.50");
        assertThat(coutCaptor.getValue().getDateCout()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(coutCaptor.getValue().getProjet()).isSameAs(projet);

        ArgumentCaptor<HistoriqueModification> historiqueCaptor =
                ArgumentCaptor.forClass(HistoriqueModification.class);
        verify(historiqueModificationRepository).save(historiqueCaptor.capture());
        assertThat(historiqueCaptor.getValue().getTypeAction()).isEqualTo(TypeAction.AJOUT_COUT);
        assertThat(historiqueCaptor.getValue().getProjet()).isSameAs(projet);
        assertThat(historiqueCaptor.getValue().getUtilisateur()).isSameAs(projectManager);
        assertThat(historiqueCaptor.getValue().getDescription()).contains("Ajout du cout 20 au projet PRJ-001");
    }

    @Test
    void nonOwnerProjectManagerCannotCreateCost() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, utilisateur(2L, "other@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));

        assertThatThrownBy(() -> coutService.createCout(10L, createRequest(), "pm@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(coutRepository, never()).save(any(Cout.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    @Test
    void adminCannotCreateCost() {
        Utilisateur admin = utilisateur(1L, "admin@example.com", RoleLibelle.ROLE_ADMIN);
        Projet projet = projet(10L, utilisateur(2L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> coutService.createCout(10L, createRequest(), "admin@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(coutRepository, never()).save(any(Cout.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    @Test
    void simpleUserCannotCreateCost() {
        Utilisateur simpleUser = utilisateur(1L, "user@example.com", RoleLibelle.ROLE_UTILISATEUR_SIMPLE);
        Projet projet = projet(10L, utilisateur(2L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(simpleUser));

        assertThatThrownBy(() -> coutService.createCout(10L, createRequest(), "user@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(coutRepository, never()).save(any(Cout.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    @Test
    void missingAuthenticatedUserProducesNotFoundExceptionOnCreate() {
        Projet projet = projet(10L, utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET));

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coutService.createCout(10L, createRequest(), "missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Utilisateur authentifie introuvable");

        verify(coutRepository, never()).save(any(Cout.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    @Test
    void nullTypeIsRejectedOnCreate() {
        CreateCoutRequest request = createRequest();
        request.setType(null);

        assertInvalidCreateRequest(request, "type de cout");
    }

    @Test
    void nullAmountIsRejectedOnCreate() {
        CreateCoutRequest request = createRequest();
        request.setMontant(null);

        assertInvalidCreateRequest(request, "montant");
    }

    @Test
    void zeroAmountIsRejectedOnCreate() {
        CreateCoutRequest request = createRequest();
        request.setMontant(BigDecimal.ZERO);

        assertInvalidCreateRequest(request, "strictement superieur");
    }

    @Test
    void negativeAmountIsRejectedOnCreate() {
        CreateCoutRequest request = createRequest();
        request.setMontant(new BigDecimal("-1.00"));

        assertInvalidCreateRequest(request, "strictement superieur");
    }

    @Test
    void amountWithMoreThanTwoDecimalPlacesIsRejectedOnCreate() {
        CreateCoutRequest request = createRequest();
        request.setMontant(new BigDecimal("100.001"));

        assertInvalidCreateRequest(request, "2 decimales");
    }

    @Test
    void amountWithTrailingZeroDecimalPlacesIsAcceptedOnCreate() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, projectManager);
        CreateCoutRequest request = createRequest();
        request.setMontant(new BigDecimal("100.000"));

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(coutRepository.save(any(Cout.class))).thenAnswer(invocation -> {
            Cout savedCout = invocation.getArgument(0);
            savedCout.setIdCout(20L);
            return savedCout;
        });

        CoutResponse response = coutService.createCout(10L, request, "pm@example.com");

        assertThat(response.getMontant()).isEqualByComparingTo("100.000");

        ArgumentCaptor<Cout> coutCaptor = ArgumentCaptor.forClass(Cout.class);
        verify(coutRepository).save(coutCaptor.capture());
        assertThat(coutCaptor.getValue().getMontant()).isEqualByComparingTo("100.000");
        verify(historiqueModificationRepository).save(any(HistoriqueModification.class));
    }

    @Test
    void nullDateIsRejectedOnCreate() {
        CreateCoutRequest request = createRequest();
        request.setDateCout(null);

        assertInvalidCreateRequest(request, "date du cout");
    }

    @Test
    void ownerProjectManagerCanUpdateCost() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, projectManager);
        Cout cout = cout(20L, TypeCout.MATERIEL, new BigDecimal("1200.50"), LocalDate.of(2026, 2, 1), projet);

        when(coutRepository.findById(20L)).thenReturn(Optional.of(cout));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));
        when(coutRepository.save(cout)).thenReturn(cout);

        CoutResponse response = coutService.updateCout(20L, updateRequest(), "pm@example.com");

        assertThat(response.getType()).isEqualTo(TypeCout.LOGICIEL);
        assertThat(response.getMontant()).isEqualByComparingTo("900.25");
        assertThat(response.getDateCout()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(response.getIdProjet()).isEqualTo(10L);
        assertThat(cout.getProjet()).isSameAs(projet);

        ArgumentCaptor<Cout> coutCaptor = ArgumentCaptor.forClass(Cout.class);
        verify(coutRepository).save(coutCaptor.capture());
        assertThat(coutCaptor.getValue().getType()).isEqualTo(TypeCout.LOGICIEL);
        assertThat(coutCaptor.getValue().getMontant()).isEqualByComparingTo("900.25");
        assertThat(coutCaptor.getValue().getDateCout()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(coutCaptor.getValue().getProjet()).isSameAs(projet);

        ArgumentCaptor<HistoriqueModification> historiqueCaptor =
                ArgumentCaptor.forClass(HistoriqueModification.class);
        verify(historiqueModificationRepository).save(historiqueCaptor.capture());
        assertThat(historiqueCaptor.getValue().getTypeAction()).isEqualTo(TypeAction.MODIFICATION);
        assertThat(historiqueCaptor.getValue().getProjet()).isSameAs(projet);
        assertThat(historiqueCaptor.getValue().getUtilisateur()).isSameAs(projectManager);
        assertThat(historiqueCaptor.getValue().getDescription()).contains("Modification du cout 20 du projet PRJ-001");
    }

    @Test
    void nonOwnerProjectManagerCannotUpdateCost() {
        assertDeniedUpdate(
                utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET),
                projet(10L, utilisateur(2L, "other@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET)),
                "pm@example.com"
        );
    }

    @Test
    void adminCannotUpdateCost() {
        assertDeniedUpdate(
                utilisateur(1L, "admin@example.com", RoleLibelle.ROLE_ADMIN),
                projet(10L, utilisateur(2L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET)),
                "admin@example.com"
        );
    }

    @Test
    void simpleUserCannotUpdateCost() {
        assertDeniedUpdate(
                utilisateur(1L, "user@example.com", RoleLibelle.ROLE_UTILISATEUR_SIMPLE),
                projet(10L, utilisateur(2L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET)),
                "user@example.com"
        );
    }

    @Test
    void invalidAmountIsRejectedOnUpdate() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, projectManager);
        Cout cout = cout(20L, TypeCout.MATERIEL, new BigDecimal("1200.50"), LocalDate.of(2026, 2, 1), projet);
        UpdateCoutRequest request = updateRequest();
        request.setMontant(new BigDecimal("100.001"));

        when(coutRepository.findById(20L)).thenReturn(Optional.of(cout));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));

        assertThatThrownBy(() -> coutService.updateCout(20L, request, "pm@example.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("2 decimales");

        verify(coutRepository, never()).save(any(Cout.class));
        verify(coutRepository, never()).delete(any(Cout.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    @Test
    void ownerProjectManagerCanDeleteCost() {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, projectManager);
        Cout cout = cout(20L, TypeCout.MATERIEL, new BigDecimal("1200.50"), LocalDate.of(2026, 2, 1), projet);

        when(coutRepository.findById(20L)).thenReturn(Optional.of(cout));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));

        coutService.deleteCout(20L, "pm@example.com");

        verify(coutRepository).delete(cout);

        ArgumentCaptor<HistoriqueModification> historiqueCaptor =
                ArgumentCaptor.forClass(HistoriqueModification.class);
        verify(historiqueModificationRepository).save(historiqueCaptor.capture());
        assertThat(historiqueCaptor.getValue().getTypeAction()).isEqualTo(TypeAction.SUPPRESSION);
        assertThat(historiqueCaptor.getValue().getProjet()).isSameAs(projet);
        assertThat(historiqueCaptor.getValue().getUtilisateur()).isSameAs(projectManager);
        assertThat(historiqueCaptor.getValue().getDescription()).contains("Suppression du cout 20 du projet PRJ-001");
    }

    @Test
    void nonOwnerProjectManagerCannotDeleteCost() {
        assertDeniedDelete(
                utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET),
                projet(10L, utilisateur(2L, "other@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET)),
                "pm@example.com"
        );
    }

    @Test
    void adminCannotDeleteCost() {
        assertDeniedDelete(
                utilisateur(1L, "admin@example.com", RoleLibelle.ROLE_ADMIN),
                projet(10L, utilisateur(2L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET)),
                "admin@example.com"
        );
    }

    @Test
    void simpleUserCannotDeleteCost() {
        assertDeniedDelete(
                utilisateur(1L, "user@example.com", RoleLibelle.ROLE_UTILISATEUR_SIMPLE),
                projet(10L, utilisateur(2L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET)),
                "user@example.com"
        );
    }

    private void assertInvalidCreateRequest(CreateCoutRequest request, String expectedMessage) {
        Utilisateur projectManager = utilisateur(1L, "pm@example.com", RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Projet projet = projet(10L, projectManager);

        when(projetRepository.findById(10L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findByEmailIgnoreCase("pm@example.com")).thenReturn(Optional.of(projectManager));

        assertThatThrownBy(() -> coutService.createCout(10L, request, "pm@example.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(expectedMessage);

        verify(coutRepository, never()).save(any(Cout.class));
        verify(coutRepository, never()).delete(any(Cout.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    private void assertDeniedUpdate(Utilisateur currentUser, Projet projet, String email) {
        Cout cout = cout(20L, TypeCout.MATERIEL, new BigDecimal("1200.50"), LocalDate.of(2026, 2, 1), projet);

        when(coutRepository.findById(20L)).thenReturn(Optional.of(cout));
        when(utilisateurRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> coutService.updateCout(20L, updateRequest(), email))
                .isInstanceOf(AccessDeniedException.class);

        verify(coutRepository, never()).save(any(Cout.class));
        verify(coutRepository, never()).delete(any(Cout.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    private void assertDeniedDelete(Utilisateur currentUser, Projet projet, String email) {
        Cout cout = cout(20L, TypeCout.MATERIEL, new BigDecimal("1200.50"), LocalDate.of(2026, 2, 1), projet);

        when(coutRepository.findById(20L)).thenReturn(Optional.of(cout));
        when(utilisateurRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> coutService.deleteCout(20L, email))
                .isInstanceOf(AccessDeniedException.class);

        verify(coutRepository, never()).save(any(Cout.class));
        verify(coutRepository, never()).delete(any(Cout.class));
        verify(historiqueModificationRepository, never()).save(any(HistoriqueModification.class));
    }

    private CreateCoutRequest createRequest() {
        CreateCoutRequest request = new CreateCoutRequest();
        request.setType(TypeCout.MATERIEL);
        request.setMontant(new BigDecimal("1200.50"));
        request.setDateCout(LocalDate.of(2026, 2, 1));
        return request;
    }

    private UpdateCoutRequest updateRequest() {
        UpdateCoutRequest request = new UpdateCoutRequest();
        request.setType(TypeCout.LOGICIEL);
        request.setMontant(new BigDecimal("900.25"));
        request.setDateCout(LocalDate.of(2026, 3, 1));
        return request;
    }

    private Cout cout(Long idCout, TypeCout type, BigDecimal montant, LocalDate dateCout, Projet projet) {
        Cout cout = new Cout();
        cout.setIdCout(idCout);
        cout.setType(type);
        cout.setMontant(montant);
        cout.setDateCout(dateCout);
        cout.setProjet(projet);
        return cout;
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
