package ma.onee.dti.projectportfolio.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportProjectRowResponse {

    private final Long projectId;
    private final String code;
    private final String intitule;
    private final String responsable;
    private final String statut;
    private final String priorite;
    private final BigDecimal plannedBudget;
    private final BigDecimal consumedCost;
    private final BigDecimal variance;
    private final Integer progress;
    private final long criticalRiskCount;
    private final long resourceCount;
}
