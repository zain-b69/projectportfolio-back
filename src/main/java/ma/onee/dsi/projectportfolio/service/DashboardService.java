package ma.onee.dsi.projectportfolio.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ma.onee.dsi.projectportfolio.dto.DashboardAttentionProjectResponse;
import ma.onee.dsi.projectportfolio.dto.DashboardAttentionReason;
import ma.onee.dsi.projectportfolio.dto.DashboardBudgetProjectResponse;
import ma.onee.dsi.projectportfolio.dto.DashboardCountItemResponse;
import ma.onee.dsi.projectportfolio.dto.DashboardKpisResponse;
import ma.onee.dsi.projectportfolio.dto.DashboardResponse;
import ma.onee.dsi.projectportfolio.enums.NiveauCriticite;
import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;
import ma.onee.dsi.projectportfolio.repository.CoutRepository;
import ma.onee.dsi.projectportfolio.repository.ProjetRepository;
import ma.onee.dsi.projectportfolio.repository.RisqueRepository;
import ma.onee.dsi.projectportfolio.repository.projection.ConsumedCostByProjectProjection;
import ma.onee.dsi.projectportfolio.repository.projection.CriticalRiskCountByProjectProjection;
import ma.onee.dsi.projectportfolio.repository.projection.DashboardProjectProjection;
import ma.onee.dsi.projectportfolio.repository.projection.ProjectPriorityCountProjection;
import ma.onee.dsi.projectportfolio.repository.projection.ProjectStatusCountProjection;
import ma.onee.dsi.projectportfolio.repository.projection.RiskCriticalityCountProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final ProjetRepository projetRepository;
    private final CoutRepository coutRepository;
    private final RisqueRepository risqueRepository;

    public DashboardService(
            ProjetRepository projetRepository,
            CoutRepository coutRepository,
            RisqueRepository risqueRepository
    ) {
        this.projetRepository = projetRepository;
        this.coutRepository = coutRepository;
        this.risqueRepository = risqueRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        List<DashboardCountItemResponse> projectsByStatus = buildStatusDistribution();
        List<DashboardCountItemResponse> projectsByPriority = buildPriorityDistribution();
        List<DashboardCountItemResponse> risksByCriticality = buildRiskDistribution();

        BigDecimal totalPlannedBudget = zeroIfNull(projetRepository.sumBudgetPrevisionnel());
        BigDecimal totalConsumedCost = zeroIfNull(coutRepository.sumMontant());

        DashboardKpisResponse kpis = DashboardKpisResponse.builder()
                .totalProjects(projetRepository.count())
                .plannedProjects(countForKey(projectsByStatus, StatutProjet.PLANIFIE.name()))
                .projectsInProgress(countForKey(projectsByStatus, StatutProjet.EN_COURS.name()))
                .completedProjects(countForKey(projectsByStatus, StatutProjet.TERMINE.name()))
                .delayedProjects(countForKey(projectsByStatus, StatutProjet.EN_RETARD.name()))
                .totalPlannedBudget(totalPlannedBudget)
                .totalConsumedCost(totalConsumedCost)
                .globalBudgetVariance(totalPlannedBudget.subtract(totalConsumedCost))
                .criticalRisks(risqueRepository.countByNiveauCriticite(NiveauCriticite.CRITIQUE))
                .build();

        List<DashboardProjectProjection> projects = projetRepository.findDashboardProjects();
        Map<Long, BigDecimal> consumedCostByProject = consumedCostByProject();
        Map<Long, Long> criticalRiskCountByProject = criticalRiskCountByProject();

        List<DashboardBudgetProjectResponse> budgetVsConsumedByProject = projects.stream()
                .map(project -> mapBudgetProject(project, consumedCostByProject))
                .sorted(Comparator
                        .comparing(DashboardBudgetProjectResponse::getConsumedCost, Comparator.reverseOrder())
                        .thenComparing(DashboardBudgetProjectResponse::getCode, Comparator.nullsLast(String::compareTo)))
                .toList();

        List<DashboardAttentionProjectResponse> projectsRequiringAttention = projects.stream()
                .map(project -> mapAttentionProject(project, consumedCostByProject, criticalRiskCountByProject))
                .filter(project -> !project.getReasons().isEmpty())
                .sorted(Comparator
                        .comparing((DashboardAttentionProjectResponse project) -> project.getReasons().size()).reversed()
                        .thenComparing(DashboardAttentionProjectResponse::getCode, Comparator.nullsLast(String::compareTo)))
                .toList();

        return DashboardResponse.builder()
                .kpis(kpis)
                .projectsByStatus(projectsByStatus)
                .projectsByPriority(projectsByPriority)
                .budgetVsConsumedByProject(budgetVsConsumedByProject)
                .risksByCriticality(risksByCriticality)
                .projectsRequiringAttention(projectsRequiringAttention)
                .build();
    }

    private List<DashboardCountItemResponse> buildStatusDistribution() {
        Map<StatutProjet, Long> counts = new EnumMap<>(StatutProjet.class);
        for (ProjectStatusCountProjection projection : projetRepository.countProjectsByStatus()) {
            if (projection.getStatut() != null) {
                counts.put(projection.getStatut(), projection.getCount());
            }
        }

        return java.util.Arrays.stream(StatutProjet.values())
                .map(statut -> countItem(statut.name(), counts.getOrDefault(statut, 0L)))
                .toList();
    }

    private List<DashboardCountItemResponse> buildPriorityDistribution() {
        Map<PrioriteProjet, Long> counts = new EnumMap<>(PrioriteProjet.class);
        for (ProjectPriorityCountProjection projection : projetRepository.countProjectsByPriority()) {
            if (projection.getPriorite() != null) {
                counts.put(projection.getPriorite(), projection.getCount());
            }
        }

        return java.util.Arrays.stream(PrioriteProjet.values())
                .map(priorite -> countItem(priorite.name(), counts.getOrDefault(priorite, 0L)))
                .toList();
    }

    private List<DashboardCountItemResponse> buildRiskDistribution() {
        Map<NiveauCriticite, Long> counts = new EnumMap<>(NiveauCriticite.class);
        for (RiskCriticalityCountProjection projection : risqueRepository.countRisksByCriticality()) {
            if (projection.getNiveauCriticite() != null) {
                counts.put(projection.getNiveauCriticite(), projection.getCount());
            }
        }

        return java.util.Arrays.stream(NiveauCriticite.values())
                .map(niveauCriticite -> countItem(niveauCriticite.name(), counts.getOrDefault(niveauCriticite, 0L)))
                .toList();
    }

    private Map<Long, BigDecimal> consumedCostByProject() {
        return coutRepository.sumMontantGroupByProject().stream()
                .collect(Collectors.toMap(
                        ConsumedCostByProjectProjection::getProjectId,
                        projection -> zeroIfNull(projection.getConsumedCost())
                ));
    }

    private Map<Long, Long> criticalRiskCountByProject() {
        return risqueRepository.countByCriticalityGroupByProject(NiveauCriticite.CRITIQUE).stream()
                .collect(Collectors.toMap(
                        CriticalRiskCountByProjectProjection::getProjectId,
                        CriticalRiskCountByProjectProjection::getCriticalRiskCount
                ));
    }

    private DashboardBudgetProjectResponse mapBudgetProject(
            DashboardProjectProjection project,
            Map<Long, BigDecimal> consumedCostByProject
    ) {
        BigDecimal plannedBudget = zeroIfNull(project.getPlannedBudget());
        BigDecimal consumedCost = consumedCostByProject.getOrDefault(project.getProjectId(), BigDecimal.ZERO);

        return DashboardBudgetProjectResponse.builder()
                .projectId(project.getProjectId())
                .code(project.getCode())
                .intitule(project.getIntitule())
                .plannedBudget(plannedBudget)
                .consumedCost(consumedCost)
                .variance(plannedBudget.subtract(consumedCost))
                .build();
    }

    private DashboardAttentionProjectResponse mapAttentionProject(
            DashboardProjectProjection project,
            Map<Long, BigDecimal> consumedCostByProject,
            Map<Long, Long> criticalRiskCountByProject
    ) {
        BigDecimal plannedBudget = zeroIfNull(project.getPlannedBudget());
        BigDecimal consumedCost = consumedCostByProject.getOrDefault(project.getProjectId(), BigDecimal.ZERO);
        BigDecimal budgetVariance = plannedBudget.subtract(consumedCost);
        long criticalRiskCount = criticalRiskCountByProject.getOrDefault(project.getProjectId(), 0L);
        List<DashboardAttentionReason> reasons = new ArrayList<>();

        if (project.getStatut() == StatutProjet.EN_RETARD) {
            reasons.add(DashboardAttentionReason.DELAYED);
        }

        if (consumedCost.compareTo(plannedBudget) > 0) {
            reasons.add(DashboardAttentionReason.BUDGET_OVERRUN);
        }

        if (criticalRiskCount > 0) {
            reasons.add(DashboardAttentionReason.CRITICAL_RISK);
        }

        return DashboardAttentionProjectResponse.builder()
                .projectId(project.getProjectId())
                .code(project.getCode())
                .intitule(project.getIntitule())
                .statut(project.getStatut())
                .priorite(project.getPriorite())
                .pourcentageAvancement(project.getPourcentageAvancement())
                .dateFinPrevue(project.getDateFinPrevue())
                .plannedBudget(plannedBudget)
                .consumedCost(consumedCost)
                .budgetVariance(budgetVariance)
                .criticalRiskCount(criticalRiskCount)
                .reasons(reasons)
                .build();
    }

    private long countForKey(List<DashboardCountItemResponse> items, String key) {
        return items.stream()
                .filter(item -> item.getKey().equals(key))
                .findFirst()
                .map(DashboardCountItemResponse::getCount)
                .orElse(0L);
    }

    private DashboardCountItemResponse countItem(String key, long count) {
        return DashboardCountItemResponse.builder()
                .key(key)
                .count(count)
                .build();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
