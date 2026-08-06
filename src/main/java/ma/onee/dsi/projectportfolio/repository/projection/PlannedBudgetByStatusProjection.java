package ma.onee.dsi.projectportfolio.repository.projection;

import java.math.BigDecimal;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;

public interface PlannedBudgetByStatusProjection {

    StatutProjet getStatut();

    BigDecimal getTotalPlannedBudget();

    Long getProjectCount();
}
