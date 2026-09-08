package ma.onee.dti.projectportfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.onee.dti.projectportfolio.dto.DashboardAttentionReason;
import ma.onee.dti.projectportfolio.dto.DashboardResponse;
import ma.onee.dti.projectportfolio.enums.NiveauCriticite;
import ma.onee.dti.projectportfolio.enums.PrioriteProjet;
import ma.onee.dti.projectportfolio.enums.StatutProjet;
import ma.onee.dti.projectportfolio.repository.CoutRepository;
import ma.onee.dti.projectportfolio.repository.ProjetRepository;
import ma.onee.dti.projectportfolio.repository.RisqueRepository;
import ma.onee.dti.projectportfolio.repository.projection.ConsumedCostByProjectProjection;
import ma.onee.dti.projectportfolio.repository.projection.CriticalRiskCountByProjectProjection;
import ma.onee.dti.projectportfolio.repository.projection.DashboardProjectProjection;
import ma.onee.dti.projectportfolio.repository.projection.ProjectPriorityCountProjection;
import ma.onee.dti.projectportfolio.repository.projection.ProjectStatusCountProjection;
import ma.onee.dti.projectportfolio.repository.projection.RiskCriticalityCountProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ProjetRepository projetRepository;

    @Mock
    private CoutRepository coutRepository;

    @Mock
    private RisqueRepository risqueRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(projetRepository, coutRepository, risqueRepository);
    }

    @Test
    void emptyPortfolioReturnsZeroKpisAndAllEnumDistributions() {
        when(projetRepository.count()).thenReturn(0L);
        when(projetRepository.countProjectsByStatus()).thenReturn(List.of());
        when(projetRepository.countProjectsByPriority()).thenReturn(List.of());
        when(projetRepository.sumBudgetPrevisionnel()).thenReturn(null);
        when(projetRepository.findDashboardProjects()).thenReturn(List.of());
        when(coutRepository.sumMontant()).thenReturn(null);
        when(coutRepository.sumMontantGroupByProject()).thenReturn(List.of());
        when(risqueRepository.countByNiveauCriticite(NiveauCriticite.CRITIQUE)).thenReturn(0L);
        when(risqueRepository.countRisksByCriticality()).thenReturn(List.of());
        when(risqueRepository.countByCriticalityGroupByProject(NiveauCriticite.CRITIQUE)).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard();

        assertThat(response.getKpis().getTotalProjects()).isZero();
        assertThat(response.getKpis().getTotalPlannedBudget()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getKpis().getTotalConsumedCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getKpis().getGlobalBudgetVariance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getProjectsByStatus()).hasSize(StatutProjet.values().length);
        assertThat(response.getProjectsByPriority()).hasSize(PrioriteProjet.values().length);
        assertThat(response.getRisksByCriticality()).hasSize(NiveauCriticite.values().length);
        assertThat(response.getBudgetVsConsumedByProject()).isEmpty();
        assertThat(response.getProjectsRequiringAttention()).isEmpty();
    }

    @Test
    void dashboardCalculatesKpisDistributionsBudgetsAndAttentionProjects() {
        when(projetRepository.count()).thenReturn(5L);
        when(projetRepository.countProjectsByStatus()).thenReturn(List.of(
                statusCount(StatutProjet.PLANIFIE, 1),
                statusCount(StatutProjet.EN_COURS, 2),
                statusCount(StatutProjet.TERMINE, 1),
                statusCount(StatutProjet.EN_RETARD, 1)
        ));
        when(projetRepository.countProjectsByPriority()).thenReturn(List.of(
                priorityCount(PrioriteProjet.FAIBLE, 1),
                priorityCount(PrioriteProjet.MOYENNE, 1),
                priorityCount(PrioriteProjet.ELEVEE, 1),
                priorityCount(PrioriteProjet.CRITIQUE, 2)
        ));
        when(risqueRepository.countRisksByCriticality()).thenReturn(List.of(
                riskCount(NiveauCriticite.CRITIQUE, 3),
                riskCount(NiveauCriticite.MOYEN, 1)
        ));
        when(projetRepository.sumBudgetPrevisionnel()).thenReturn(new BigDecimal("6000.00"));
        when(coutRepository.sumMontant()).thenReturn(new BigDecimal("2300.00"));
        when(risqueRepository.countByNiveauCriticite(NiveauCriticite.CRITIQUE)).thenReturn(3L);
        when(projetRepository.findDashboardProjects()).thenReturn(List.of(
                project(1L, "PRJ-001", StatutProjet.EN_RETARD, PrioriteProjet.CRITIQUE, new BigDecimal("1000.00")),
                project(2L, "PRJ-002", StatutProjet.EN_COURS, PrioriteProjet.ELEVEE, new BigDecimal("500.00")),
                project(3L, "PRJ-003", StatutProjet.EN_COURS, PrioriteProjet.MOYENNE, new BigDecimal("1000.00")),
                project(4L, "PRJ-004", StatutProjet.PLANIFIE, PrioriteProjet.FAIBLE, new BigDecimal("1500.00"))
        ));
        when(coutRepository.sumMontantGroupByProject()).thenReturn(List.of(
                consumed(1L, new BigDecimal("1200.00")),
                consumed(2L, new BigDecimal("800.00")),
                consumed(3L, new BigDecimal("300.00"))
        ));
        when(risqueRepository.countByCriticalityGroupByProject(NiveauCriticite.CRITIQUE)).thenReturn(List.of(
                criticalRisks(1L, 2),
                criticalRisks(3L, 1)
        ));

        DashboardResponse response = dashboardService.getDashboard();

        assertThat(response.getKpis().getPlannedProjects()).isEqualTo(1);
        assertThat(response.getKpis().getProjectsInProgress()).isEqualTo(2);
        assertThat(response.getKpis().getCompletedProjects()).isEqualTo(1);
        assertThat(response.getKpis().getDelayedProjects()).isEqualTo(1);
        assertThat(response.getKpis().getGlobalBudgetVariance()).isEqualByComparingTo("3700.00");
        assertThat(response.getKpis().getCriticalRisks()).isEqualTo(3);
        assertThat(response.getProjectsByStatus()).extracting("key")
                .containsExactly("PLANIFIE", "EN_COURS", "TERMINE", "EN_RETARD", "SUSPENDU", "ANNULE");
        assertThat(response.getProjectsByPriority()).extracting("key")
                .containsExactly("FAIBLE", "MOYENNE", "ELEVEE", "CRITIQUE");
        assertThat(response.getRisksByCriticality()).extracting("key")
                .containsExactly("FAIBLE", "MOYEN", "ELEVE", "CRITIQUE");
        assertThat(response.getBudgetVsConsumedByProject())
                .anySatisfy(project -> {
                    assertThat(project.getProjectId()).isEqualTo(4L);
                    assertThat(project.getConsumedCost()).isEqualByComparingTo(BigDecimal.ZERO);
                });
        assertThat(response.getProjectsRequiringAttention())
                .anySatisfy(project -> {
                    assertThat(project.getProjectId()).isEqualTo(1L);
                    assertThat(project.getReasons()).containsExactly(
                            DashboardAttentionReason.DELAYED,
                            DashboardAttentionReason.BUDGET_OVERRUN,
                            DashboardAttentionReason.CRITICAL_RISK
                    );
                })
                .anySatisfy(project -> {
                    assertThat(project.getProjectId()).isEqualTo(2L);
                    assertThat(project.getReasons()).containsExactly(DashboardAttentionReason.BUDGET_OVERRUN);
                })
                .anySatisfy(project -> {
                    assertThat(project.getProjectId()).isEqualTo(3L);
                    assertThat(project.getReasons()).containsExactly(DashboardAttentionReason.CRITICAL_RISK);
                });
        assertThat(response.getProjectsRequiringAttention())
                .extracting("projectId")
                .doesNotContain(4L);
    }

    private ProjectStatusCountProjection statusCount(StatutProjet statut, long count) {
        return new ProjectStatusCountProjection() {
            public StatutProjet getStatut() {
                return statut;
            }

            public long getCount() {
                return count;
            }
        };
    }

    private ProjectPriorityCountProjection priorityCount(PrioriteProjet priorite, long count) {
        return new ProjectPriorityCountProjection() {
            public PrioriteProjet getPriorite() {
                return priorite;
            }

            public long getCount() {
                return count;
            }
        };
    }

    private RiskCriticalityCountProjection riskCount(NiveauCriticite niveauCriticite, long count) {
        return new RiskCriticalityCountProjection() {
            public NiveauCriticite getNiveauCriticite() {
                return niveauCriticite;
            }

            public long getCount() {
                return count;
            }
        };
    }

    private DashboardProjectProjection project(
            Long projectId,
            String code,
            StatutProjet statut,
            PrioriteProjet priorite,
            BigDecimal plannedBudget
    ) {
        return new DashboardProjectProjection() {
            public Long getProjectId() {
                return projectId;
            }

            public String getCode() {
                return code;
            }

            public String getIntitule() {
                return "Projet " + code;
            }

            public StatutProjet getStatut() {
                return statut;
            }

            public PrioriteProjet getPriorite() {
                return priorite;
            }

            public Integer getPourcentageAvancement() {
                return 50;
            }

            public LocalDate getDateFinPrevue() {
                return LocalDate.of(2026, 12, 31);
            }

            public BigDecimal getPlannedBudget() {
                return plannedBudget;
            }
        };
    }

    private ConsumedCostByProjectProjection consumed(Long projectId, BigDecimal consumedCost) {
        return new ConsumedCostByProjectProjection() {
            public Long getProjectId() {
                return projectId;
            }

            public BigDecimal getConsumedCost() {
                return consumedCost;
            }
        };
    }

    private CriticalRiskCountByProjectProjection criticalRisks(Long projectId, long count) {
        return new CriticalRiskCountByProjectProjection() {
            public Long getProjectId() {
                return projectId;
            }

            public long getCriticalRiskCount() {
                return count;
            }
        };
    }
}
