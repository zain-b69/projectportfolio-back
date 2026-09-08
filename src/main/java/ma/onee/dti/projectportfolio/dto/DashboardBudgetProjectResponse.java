package ma.onee.dti.projectportfolio.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardBudgetProjectResponse {

    private final Long projectId;
    private final String code;
    private final String intitule;
    private final BigDecimal plannedBudget;
    private final BigDecimal consumedCost;
    private final BigDecimal variance;
}
