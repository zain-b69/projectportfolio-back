package ma.onee.dsi.projectportfolio.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportCostByTypeResponse {

    private final String type;
    private final BigDecimal totalMontant;
    private final long count;
}
