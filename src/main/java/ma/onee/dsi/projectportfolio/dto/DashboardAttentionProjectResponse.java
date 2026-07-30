package ma.onee.dsi.projectportfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;

@Getter
@Builder
public class DashboardAttentionProjectResponse {

    private final Long projectId;
    private final String code;
    private final String intitule;
    private final StatutProjet statut;
    private final PrioriteProjet priorite;
    private final Integer pourcentageAvancement;
    private final LocalDate dateFinPrevue;
    private final BigDecimal plannedBudget;
    private final BigDecimal consumedCost;
    private final BigDecimal budgetVariance;
    private final long criticalRiskCount;
    private final List<DashboardAttentionReason> reasons;
}
