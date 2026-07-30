package ma.onee.dsi.projectportfolio.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardKpisResponse {

    private final long totalProjects;
    private final long plannedProjects;
    private final long projectsInProgress;
    private final long completedProjects;
    private final long delayedProjects;
    private final BigDecimal totalPlannedBudget;
    private final BigDecimal totalConsumedCost;
    private final BigDecimal globalBudgetVariance;
    private final long criticalRisks;
}
