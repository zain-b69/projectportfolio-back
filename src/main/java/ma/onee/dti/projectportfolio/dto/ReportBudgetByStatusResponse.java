package ma.onee.dti.projectportfolio.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportBudgetByStatusResponse {

    private final String statut;
    private final BigDecimal totalPlannedBudget;
    private final long projectCount;
}
