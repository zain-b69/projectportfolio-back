package ma.onee.dti.projectportfolio.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportKpisResponse {

    private final long totalProjects;
    private final BigDecimal totalPlannedBudget;
    private final BigDecimal totalConsumedCost;
    private final BigDecimal totalBudgetVariance;
    private final double averageProgress;
    private final long totalCriticalRisks;
    private final long totalResourceAllocations;
}
