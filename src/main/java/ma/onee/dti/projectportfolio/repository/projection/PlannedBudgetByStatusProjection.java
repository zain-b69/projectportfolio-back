package ma.onee.dti.projectportfolio.repository.projection;

import java.math.BigDecimal;
import ma.onee.dti.projectportfolio.enums.StatutProjet;

public interface PlannedBudgetByStatusProjection {

    StatutProjet getStatut();

    BigDecimal getTotalPlannedBudget();

    Long getProjectCount();
}
