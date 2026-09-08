package ma.onee.dti.projectportfolio.repository.projection;

import java.math.BigDecimal;

public interface ConsumedCostByProjectProjection {

    Long getProjectId();

    BigDecimal getConsumedCost();
}
