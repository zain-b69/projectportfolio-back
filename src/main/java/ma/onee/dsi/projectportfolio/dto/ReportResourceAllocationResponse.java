package ma.onee.dsi.projectportfolio.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportResourceAllocationResponse {

    private final String natureIntervention;
    private final BigDecimal totalChargeJH;
    private final long count;
}
