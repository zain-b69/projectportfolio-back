package ma.onee.dti.projectportfolio.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ma.onee.dti.projectportfolio.dto.DashboardCountItemResponse;
import ma.onee.dti.projectportfolio.dto.ReportBudgetByStatusResponse;
import ma.onee.dti.projectportfolio.dto.ReportCostByTypeResponse;
import ma.onee.dti.projectportfolio.dto.ReportKpisResponse;
import ma.onee.dti.projectportfolio.dto.ReportProjectRowResponse;
import ma.onee.dti.projectportfolio.dto.ReportResourceAllocationResponse;
import ma.onee.dti.projectportfolio.dto.ReportSummaryResponse;
import ma.onee.dti.projectportfolio.enums.NiveauCriticite;
import ma.onee.dti.projectportfolio.enums.PrioriteProjet;
import ma.onee.dti.projectportfolio.enums.StatutProjet;
import ma.onee.dti.projectportfolio.repository.AffectationRessourceRepository;
import ma.onee.dti.projectportfolio.repository.CoutRepository;
import ma.onee.dti.projectportfolio.repository.ProjetRepository;
import ma.onee.dti.projectportfolio.repository.RisqueRepository;
import ma.onee.dti.projectportfolio.repository.projection.ConsumedCostByProjectProjection;
import ma.onee.dti.projectportfolio.repository.projection.CriticalRiskCountByProjectProjection;
import ma.onee.dti.projectportfolio.repository.projection.PlannedBudgetByStatusProjection;
import ma.onee.dti.projectportfolio.repository.projection.ProjectPriorityCountProjection;
import ma.onee.dti.projectportfolio.repository.projection.ProjectStatusCountProjection;
import ma.onee.dti.projectportfolio.repository.projection.ReportProjectProjection;
import ma.onee.dti.projectportfolio.repository.projection.ResourceCountByProjectProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final ProjetRepository projetRepository;
    private final CoutRepository coutRepository;
    private final RisqueRepository risqueRepository;
    private final AffectationRessourceRepository affectationRessourceRepository;

    public ReportService(
            ProjetRepository projetRepository,
            CoutRepository coutRepository,
            RisqueRepository risqueRepository,
            AffectationRessourceRepository affectationRessourceRepository
    ) {
        this.projetRepository = projetRepository;
        this.coutRepository = coutRepository;
        this.risqueRepository = risqueRepository;
        this.affectationRessourceRepository = affectationRessourceRepository;
    }

    @Transactional(readOnly = true)
    public ReportSummaryResponse getReport() {
        List<ReportProjectProjection> rawProjects = projetRepository.findReportProjects();

        Map<Long, BigDecimal> consumedCostByProject = coutRepository.sumMontantGroupByProject().stream()
                .collect(Collectors.toMap(
                        ConsumedCostByProjectProjection::getProjectId,
                        p -> zeroIfNull(p.getConsumedCost())
                ));

        Map<Long, Long> criticalRiskByProject = risqueRepository.countByCriticalityGroupByProject(NiveauCriticite.CRITIQUE).stream()
                .collect(Collectors.toMap(
                        CriticalRiskCountByProjectProjection::getProjectId,
                        CriticalRiskCountByProjectProjection::getCriticalRiskCount
                ));

        Map<Long, Long> resourceCountByProject = affectationRessourceRepository.countByProject().stream()
                .collect(Collectors.toMap(
                        ResourceCountByProjectProjection::getProjectId,
                        ResourceCountByProjectProjection::getResourceCount
                ));

        List<ReportProjectRowResponse> projectRows = rawProjects.stream()
                .map(p -> mapProjectRow(p, consumedCostByProject, criticalRiskByProject, resourceCountByProject))
                .sorted(Comparator.comparing(ReportProjectRowResponse::getCode, Comparator.nullsLast(String::compareTo)))
                .toList();

        BigDecimal totalPlannedBudget = projectRows.stream()
                .map(p -> p.getPlannedBudget() != null ? p.getPlannedBudget() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalConsumedCost = zeroIfNull(coutRepository.sumMontant());
        double averageProgress = rawProjects.isEmpty() ? 0.0 :
                rawProjects.stream()
                        .mapToInt(p -> p.getPourcentageAvancement() != null ? p.getPourcentageAvancement() : 0)
                        .average()
                        .orElse(0.0);
        long totalCriticalRisks = criticalRiskByProject.values().stream().mapToLong(Long::longValue).sum();
        long totalResourceAllocations = resourceCountByProject.values().stream().mapToLong(Long::longValue).sum();

        ReportKpisResponse kpis = ReportKpisResponse.builder()
                .totalProjects(rawProjects.size())
                .totalPlannedBudget(totalPlannedBudget)
                .totalConsumedCost(totalConsumedCost)
                .totalBudgetVariance(totalPlannedBudget.subtract(totalConsumedCost))
                .averageProgress(Math.round(averageProgress * 10.0) / 10.0)
                .totalCriticalRisks(totalCriticalRisks)
                .totalResourceAllocations(totalResourceAllocations)
                .build();

        return ReportSummaryResponse.builder()
                .kpis(kpis)
                .projects(projectRows)
                .projectsByStatus(buildStatusDistribution())
                .projectsByPriority(buildPriorityDistribution())
                .budgetByStatus(buildBudgetByStatus())
                .costByType(buildCostByType())
                .resourceAllocationByNature(buildResourceAllocationByNature())
                .build();
    }

    private ReportProjectRowResponse mapProjectRow(
            ReportProjectProjection project,
            Map<Long, BigDecimal> consumedCostByProject,
            Map<Long, Long> criticalRiskByProject,
            Map<Long, Long> resourceCountByProject
    ) {
        BigDecimal plannedBudget = zeroIfNull(project.getPlannedBudget());
        BigDecimal consumedCost = consumedCostByProject.getOrDefault(project.getProjectId(), BigDecimal.ZERO);

        return ReportProjectRowResponse.builder()
                .projectId(project.getProjectId())
                .code(project.getCode())
                .intitule(project.getIntitule())
                .responsable(formatResponsable(project.getPrenomResponsable(), project.getNomResponsable()))
                .statut(project.getStatut() != null ? project.getStatut().name() : null)
                .priorite(project.getPriorite() != null ? project.getPriorite().name() : null)
                .plannedBudget(plannedBudget)
                .consumedCost(consumedCost)
                .variance(plannedBudget.subtract(consumedCost))
                .progress(project.getPourcentageAvancement())
                .criticalRiskCount(criticalRiskByProject.getOrDefault(project.getProjectId(), 0L))
                .resourceCount(resourceCountByProject.getOrDefault(project.getProjectId(), 0L))
                .build();
    }

    private List<DashboardCountItemResponse> buildStatusDistribution() {
        Map<StatutProjet, Long> counts = new EnumMap<>(StatutProjet.class);
        for (ProjectStatusCountProjection p : projetRepository.countProjectsByStatus()) {
            if (p.getStatut() != null) {
                counts.put(p.getStatut(), p.getCount());
            }
        }
        return Arrays.stream(StatutProjet.values())
                .map(s -> DashboardCountItemResponse.builder().key(s.name()).count(counts.getOrDefault(s, 0L)).build())
                .toList();
    }

    private List<DashboardCountItemResponse> buildPriorityDistribution() {
        Map<PrioriteProjet, Long> counts = new EnumMap<>(PrioriteProjet.class);
        for (ProjectPriorityCountProjection p : projetRepository.countProjectsByPriority()) {
            if (p.getPriorite() != null) {
                counts.put(p.getPriorite(), p.getCount());
            }
        }
        return Arrays.stream(PrioriteProjet.values())
                .map(p -> DashboardCountItemResponse.builder().key(p.name()).count(counts.getOrDefault(p, 0L)).build())
                .toList();
    }

    private List<ReportBudgetByStatusResponse> buildBudgetByStatus() {
        Map<StatutProjet, BigDecimal> budgetMap = new EnumMap<>(StatutProjet.class);
        Map<StatutProjet, Long> countMap = new EnumMap<>(StatutProjet.class);

        for (PlannedBudgetByStatusProjection p : projetRepository.budgetByStatus()) {
            if (p.getStatut() != null) {
                budgetMap.put(p.getStatut(), zeroIfNull(p.getTotalPlannedBudget()));
                countMap.put(p.getStatut(), p.getProjectCount() != null ? p.getProjectCount() : 0L);
            }
        }

        return Arrays.stream(StatutProjet.values())
                .filter(budgetMap::containsKey)
                .map(s -> ReportBudgetByStatusResponse.builder()
                        .statut(s.name())
                        .totalPlannedBudget(budgetMap.get(s))
                        .projectCount(countMap.getOrDefault(s, 0L))
                        .build())
                .toList();
    }

    private List<ReportCostByTypeResponse> buildCostByType() {
        return coutRepository.sumMontantGroupByType().stream()
                .filter(p -> p.getType() != null)
                .map(p -> ReportCostByTypeResponse.builder()
                        .type(p.getType().name())
                        .totalMontant(zeroIfNull(p.getTotalMontant()))
                        .count(p.getCount() != null ? p.getCount() : 0L)
                        .build())
                .sorted(Comparator.comparing(ReportCostByTypeResponse::getTotalMontant, Comparator.reverseOrder()))
                .toList();
    }

    private List<ReportResourceAllocationResponse> buildResourceAllocationByNature() {
        return affectationRessourceRepository.sumChargeJHByNature().stream()
                .filter(p -> p.getNatureIntervention() != null)
                .map(p -> ReportResourceAllocationResponse.builder()
                        .natureIntervention(p.getNatureIntervention().name())
                        .totalChargeJH(zeroIfNull(p.getTotalChargeJH()))
                        .count(p.getCount() != null ? p.getCount() : 0L)
                        .build())
                .sorted(Comparator.comparing(ReportResourceAllocationResponse::getTotalChargeJH, Comparator.reverseOrder()))
                .toList();
    }

    private String formatResponsable(String prenom, String nom) {
        String normalizedPrenom = prenom != null ? prenom.trim() : "";
        String normalizedNom = nom != null ? nom.trim() : "";
        return (normalizedPrenom + " " + normalizedNom).trim();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
