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
import java.util.stream.Stream;
import ma.onee.dsi.projectportfolio.dto.CreateProjetRequest;
import ma.onee.dsi.projectportfolio.dto.ProjetResponse;
import ma.onee.dsi.projectportfolio.dto.ProjetSearchCriteria;
import ma.onee.dsi.projectportfolio.dto.UpdateProjetRequest;
import ma.onee.dsi.projectportfolio.entity.Projet;
import ma.onee.dsi.projectportfolio.entity.Risque;
import ma.onee.dsi.projectportfolio.entity.Role;
import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import ma.onee.dsi.projectportfolio.enums.NiveauCriticite;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
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
    private HistoriqueService historiqueService;

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
                historiqueService,
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

        ArgumentCaptor<Projet> historiqueProjetCaptor = ArgumentCaptor.forClass(Projet.class);
        ArgumentCaptor<Utilisateur> historiqueUserCaptor = ArgumentCaptor.forClass(Utilisateur.class);
        ArgumentCaptor<TypeAction> historiqueActionCaptor = ArgumentCaptor.forClass(TypeAction.class);
        ArgumentCaptor<String> historiqueDescriptionCaptor = ArgumentCaptor.forClass(String.class);
        verify(historiqueService).record(
                historiqueProjetCaptor.capture(),
                historiqueUserCaptor.capture(),
                historiqueActionCaptor.capture(),
                historiqueDescriptionCaptor.capture());

        assertThat(historiqueActionCaptor.getValue()).isEqualTo(TypeAction.CREATION);
        assertThat(historiqueUserCaptor.getValue()).isSameAs(projectManager);
        assertThat(historiqueProjetCaptor.getValue().getCode()).isEqualTo("PRJ-001");
        assertThat(historiqueDescriptionCaptor.getValue()).contains("Creation du projet PRJ-001");
    }

    @Test
    void createProjetRejectsSimpleUser() {
        Utilisateur simpleUser = utilisateur(2L, "user@example.com", RoleLibelle.ROLE_UTILISATEUR_SIMPLE);

        when(utilisateurRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(simpleUser));

        assertThatThrownBy(() -> projetService.createProjet(plannedProjectRequest(), "user@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(projetRepository, never()).save(any(Projet.class));
        verify(historiqueService, never()).record(any(), any(), any(), any());
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
        assertThat(historiqueUserCaptor.getValue()).isSameAs(projectManager);
        assertThat(historiqueProjetCaptor.getValue()).isNull();
        assertThat(historiqueDescriptionCaptor.getValue()).contains("Suppression du projet PRJ-001");
    }

    @Test
    void deleteProjetRejectsSimpleUser() {
        Utilisateur simpleUser = utilisateur(2L, "user@example.com", RoleLibelle.ROLE_UTILISATEUR_SIMPLE);

        when(utilisateurRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(simpleUser));

        assertThatThrownBy(() -> projetService.deleteProjet(20L, "user@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        verify(projetRepository, never()).delete(any(Projet.class));
        verify(historiqueModificationRepository, never()).detachProjet(any(Long.class));
        verify(historiqueService, never()).record(any(), any(), any(), any());
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
        verify(historiqueService, never()).record(any(), any(), any(), any());
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
        verify(historiqueService, never()).record(any(), any(), any(), any());
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
        verify(historiqueService, never()).record(any(), any(), any(), any());
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
        verify(historiqueService, never()).record(any(), any(), any(), any());
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
        verify(historiqueService, never()).record(any(), any(), any(), any());
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

@DataJpaTest
class ProjetServiceSearchSpecificationTest {

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ProjetService projetService;

    @BeforeEach
    void setUp() {
        projetService = new ProjetService(
                projetRepository,
                Mockito.mock(UtilisateurRepository.class),
                Mockito.mock(HistoriqueModificationRepository.class),
                Mockito.mock(HistoriqueService.class),
                Mockito.mock(CoutRepository.class),
                Mockito.mock(RisqueRepository.class),
                Mockito.mock(PieceJointeRepository.class),
                Mockito.mock(AffectationRessourceRepository.class)
        );

        Utilisateur sara = utilisateur("Alami", "Sara", "sara.alami@example.com");
        Utilisateur yassine = utilisateur("Bennis", "Yassine", "yassine.bennis@example.com");
        entityManager.persist(sara);
        entityManager.persist(yassine);
        entityManager.persist(projet(
                "PRJ-RESEAU",
                "Migration fibre",
                "Modernisation de l'infrastructure LAN",
                StatutProjet.EN_COURS,
                sara
        ));
        entityManager.persist(projet(
                "PRJ-BUDGET",
                "Suivi financier",
                "Controle des enveloppes budgetaires",
                StatutProjet.PLANIFIE,
                yassine
        ));
        entityManager.flush();
        entityManager.clear();
    }

    @ParameterizedTest
    @MethodSource("globalSearchCases")
    void searchProjetsFindsProjectsByGlobalSearch(String search, String expectedCode) {
        ProjetSearchCriteria criteria = new ProjetSearchCriteria();
        criteria.setSearch(search);

        List<ProjetResponse> results = projetService.searchProjets(criteria);

        assertThat(results)
                .extracting(ProjetResponse::getCode)
                .containsExactly(expectedCode);
    }

    @Test
    void searchProjetsIgnoresCaseAndTrimsSearchValue() {
        ProjetSearchCriteria criteria = new ProjetSearchCriteria();
        criteria.setSearch("  MIGRATION  ");

        List<ProjetResponse> results = projetService.searchProjets(criteria);

        assertThat(results)
                .extracting(ProjetResponse::getCode)
                .containsExactly("PRJ-RESEAU");
    }

    @Test
    void searchProjetsIgnoresBlankSearchValue() {
        ProjetSearchCriteria criteria = new ProjetSearchCriteria();
        criteria.setSearch("   ");

        List<ProjetResponse> results = projetService.searchProjets(criteria);

        assertThat(results)
                .extracting(ProjetResponse::getCode)
                .containsExactly("PRJ-BUDGET", "PRJ-RESEAU");
    }

    @Test
    void searchProjetsCombinesGlobalSearchWithExistingFilters() {
        ProjetSearchCriteria criteria = new ProjetSearchCriteria();
        criteria.setSearch("prj");
        criteria.setStatut(StatutProjet.EN_COURS);

        List<ProjetResponse> results = projetService.searchProjets(criteria);

        assertThat(results)
                .extracting(ProjetResponse::getCode)
                .containsExactly("PRJ-RESEAU");

        criteria.setStatut(StatutProjet.TERMINE);

        assertThat(projetService.searchProjets(criteria)).isEmpty();
    }

    @Test
    void searchProjetsFiltersWeakRiskWhenWeakIsMaximumLevel() {
        persistProjectWithRisks("PRJ-RISK-FAIBLE", StatutProjet.EN_COURS, NiveauCriticite.FAIBLE);

        ProjetSearchCriteria criteria = new ProjetSearchCriteria();
        criteria.setNiveauRisque(NiveauCriticite.FAIBLE);

        List<ProjetResponse> results = projetService.searchProjets(criteria);

        assertThat(results)
                .extracting(ProjetResponse::getCode)
                .containsExactly("PRJ-RISK-FAIBLE");
    }

    @Test
    void searchProjetsFiltersMediumRiskWhenMediumIsMaximumLevel() {
        persistProjectWithRisks(
                "PRJ-RISK-MOYEN",
                StatutProjet.EN_COURS,
                NiveauCriticite.FAIBLE,
                NiveauCriticite.MOYEN
        );

        ProjetSearchCriteria mediumCriteria = new ProjetSearchCriteria();
        mediumCriteria.setNiveauRisque(NiveauCriticite.MOYEN);

        assertThat(projetService.searchProjets(mediumCriteria))
                .extracting(ProjetResponse::getCode)
                .containsExactly("PRJ-RISK-MOYEN");

        ProjetSearchCriteria weakCriteria = new ProjetSearchCriteria();
        weakCriteria.setNiveauRisque(NiveauCriticite.FAIBLE);

        assertThat(projetService.searchProjets(weakCriteria))
                .extracting(ProjetResponse::getCode)
                .doesNotContain("PRJ-RISK-MOYEN");
    }

    @Test
    void searchProjetsFiltersHighRiskWhenHighIsMaximumLevel() {
        persistProjectWithRisks(
                "PRJ-RISK-ELEVE",
                StatutProjet.EN_COURS,
                NiveauCriticite.MOYEN,
                NiveauCriticite.ELEVE
        );

        ProjetSearchCriteria highCriteria = new ProjetSearchCriteria();
        highCriteria.setNiveauRisque(NiveauCriticite.ELEVE);

        assertThat(projetService.searchProjets(highCriteria))
                .extracting(ProjetResponse::getCode)
                .containsExactly("PRJ-RISK-ELEVE");

        ProjetSearchCriteria mediumCriteria = new ProjetSearchCriteria();
        mediumCriteria.setNiveauRisque(NiveauCriticite.MOYEN);

        assertThat(projetService.searchProjets(mediumCriteria))
                .extracting(ProjetResponse::getCode)
                .doesNotContain("PRJ-RISK-ELEVE");
    }

    @Test
    void searchProjetsFiltersCriticalRiskWhenCriticalIsMaximumLevel() {
        persistProjectWithRisks(
                "PRJ-RISK-CRITIQUE",
                StatutProjet.EN_COURS,
                NiveauCriticite.ELEVE,
                NiveauCriticite.CRITIQUE
        );

        ProjetSearchCriteria criticalCriteria = new ProjetSearchCriteria();
        criticalCriteria.setNiveauRisque(NiveauCriticite.CRITIQUE);

        assertThat(projetService.searchProjets(criticalCriteria))
                .extracting(ProjetResponse::getCode)
                .containsExactly("PRJ-RISK-CRITIQUE");

        ProjetSearchCriteria highCriteria = new ProjetSearchCriteria();
        highCriteria.setNiveauRisque(NiveauCriticite.ELEVE);

        assertThat(projetService.searchProjets(highCriteria))
                .extracting(ProjetResponse::getCode)
                .doesNotContain("PRJ-RISK-CRITIQUE");
    }

    @Test
    void searchProjetsDoesNotMatchProjectsWithoutRiskForAnyRiskLevelFilter() {
        for (NiveauCriticite niveauCriticite : NiveauCriticite.values()) {
            ProjetSearchCriteria criteria = new ProjetSearchCriteria();
            criteria.setNiveauRisque(niveauCriticite);

            assertThat(projetService.searchProjets(criteria))
                    .extracting(ProjetResponse::getCode)
                    .doesNotContain("PRJ-RESEAU", "PRJ-BUDGET");
        }
    }

    @Test
    void searchProjetsCombinesRiskLevelWithExistingStatusFilter() {
        persistProjectWithRisks("PRJ-RISK-ELEVE-ACTIF", StatutProjet.EN_COURS, NiveauCriticite.ELEVE);
        persistProjectWithRisks("PRJ-RISK-ELEVE-PLANIFIE", StatutProjet.PLANIFIE, NiveauCriticite.ELEVE);

        ProjetSearchCriteria criteria = new ProjetSearchCriteria();
        criteria.setNiveauRisque(NiveauCriticite.ELEVE);
        criteria.setStatut(StatutProjet.EN_COURS);

        assertThat(projetService.searchProjets(criteria))
                .extracting(ProjetResponse::getCode)
                .containsExactly("PRJ-RISK-ELEVE-ACTIF");
    }

    @Test
    void searchProjetsMapsNullRiskLevelWhenProjectHasNoRisk() {
        ProjetSearchCriteria criteria = new ProjetSearchCriteria();
        criteria.setCode("PRJ-RESEAU");

        List<ProjetResponse> results = projetService.searchProjets(criteria);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNiveauRisque()).isNull();
    }

    @Test
    void searchProjetsMapsWeakRiskLevelWhenProjectHasOnlyWeakRisk() {
        persistProjectWithRisks("PRJ-MAP-FAIBLE", StatutProjet.EN_COURS, NiveauCriticite.FAIBLE);

        ProjetResponse response = findProjectByCode("PRJ-MAP-FAIBLE");

        assertThat(response.getNiveauRisque()).isEqualTo(NiveauCriticite.FAIBLE);
    }

    @Test
    void searchProjetsMapsMediumRiskLevelWhenProjectHasWeakAndMediumRisks() {
        persistProjectWithRisks(
                "PRJ-MAP-MOYEN",
                StatutProjet.EN_COURS,
                NiveauCriticite.FAIBLE,
                NiveauCriticite.MOYEN
        );

        ProjetResponse response = findProjectByCode("PRJ-MAP-MOYEN");

        assertThat(response.getNiveauRisque()).isEqualTo(NiveauCriticite.MOYEN);
    }

    @Test
    void searchProjetsMapsCriticalRiskLevelWhenProjectHasHighAndCriticalRisks() {
        persistProjectWithRisks(
                "PRJ-MAP-CRITIQUE",
                StatutProjet.EN_COURS,
                NiveauCriticite.ELEVE,
                NiveauCriticite.CRITIQUE
        );

        ProjetResponse response = findProjectByCode("PRJ-MAP-CRITIQUE");

        assertThat(response.getNiveauRisque()).isEqualTo(NiveauCriticite.CRITIQUE);
    }

    @Test
    void searchProjetsKeepsOtherMappedFieldsWhenAddingRiskLevel() {
        persistProjectWithRisks("PRJ-MAP-FIELDS", StatutProjet.EN_COURS, NiveauCriticite.ELEVE);

        ProjetResponse response = findProjectByCode("PRJ-MAP-FIELDS");

        assertThat(response.getCode()).isEqualTo("PRJ-MAP-FIELDS");
        assertThat(response.getIntitule()).isEqualTo("PRJ-MAP-FIELDS");
        assertThat(response.getDescriptif()).isEqualTo("Projet avec risques");
        assertThat(response.getStatut()).isEqualTo(StatutProjet.EN_COURS.name());
        assertThat(response.getPriorite()).isEqualTo(PrioriteProjet.MOYENNE.name());
        assertThat(response.getBudgetPrevisionnel()).isEqualByComparingTo("10000.00");
        assertThat(response.getPourcentageAvancement()).isZero();
        assertThat(response.getNomResponsable()).isEqualTo("Responsable");
        assertThat(response.getPrenomResponsable()).isEqualTo("PRJ-MAP-FIELDS");
        assertThat(response.getEmailResponsable()).isEqualTo("prj-map-fields@example.com");
        assertThat(response.getNiveauRisque()).isEqualTo(NiveauCriticite.ELEVE);
    }

    private static Stream<Arguments> globalSearchCases() {
        return Stream.of(
                Arguments.of("reseau", "PRJ-RESEAU"),
                Arguments.of("migration", "PRJ-RESEAU"),
                Arguments.of("infrastructure", "PRJ-RESEAU"),
                Arguments.of("alami", "PRJ-RESEAU"),
                Arguments.of("sara", "PRJ-RESEAU"),
                Arguments.of("sara.alami@example.com", "PRJ-RESEAU"),
                Arguments.of("sara alami", "PRJ-RESEAU"),
                Arguments.of("alami sara", "PRJ-RESEAU")
        );
    }

    private Projet projet(
            String code,
            String intitule,
            String descriptif,
            StatutProjet statut,
            Utilisateur utilisateur
    ) {
        Projet projet = new Projet();
        projet.setCode(code);
        projet.setIntitule(intitule);
        projet.setDescriptif(descriptif);
        projet.setDateDebutPrevue(LocalDate.of(2026, 1, 1));
        projet.setDateFinPrevue(LocalDate.of(2026, 12, 31));
        projet.setStatut(statut);
        projet.setBudgetPrevisionnel(new BigDecimal("10000.00"));
        projet.setPriorite(PrioriteProjet.MOYENNE);
        projet.setPourcentageAvancement(0);
        projet.setUtilisateur(utilisateur);
        return projet;
    }

    private Projet persistProjectWithRisks(
            String code,
            StatutProjet statut,
            NiveauCriticite... niveauxCriticite
    ) {
        Utilisateur utilisateur = utilisateur("Responsable", code, code.toLowerCase() + "@example.com");
        entityManager.persist(utilisateur);
        Projet projet = projet(code, code, "Projet avec risques", statut, utilisateur);
        entityManager.persist(projet);

        for (NiveauCriticite niveauCriticite : niveauxCriticite) {
            Risque risque = new Risque();
            risque.setDescription("Risque " + niveauCriticite);
            risque.setNiveauCriticite(niveauCriticite);
            risque.setProjet(projet);
            entityManager.persist(risque);
        }

        entityManager.flush();
        entityManager.clear();
        return projet;
    }

    private ProjetResponse findProjectByCode(String code) {
        ProjetSearchCriteria criteria = new ProjetSearchCriteria();
        criteria.setCode(code);

        List<ProjetResponse> results = projetService.searchProjets(criteria);

        assertThat(results).hasSize(1);
        return results.get(0);
    }

    private Utilisateur utilisateur(String nom, String prenom, String email) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setEmail(email);
        utilisateur.setMotDePasse("secret");
        return utilisateur;
    }
}
