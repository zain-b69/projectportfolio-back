package ma.onee.dti.projectportfolio.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import ma.onee.dti.projectportfolio.enums.PrioriteProjet;
import ma.onee.dti.projectportfolio.enums.StatutProjet;

public interface DashboardProjectProjection {

    Long getProjectId();

    String getCode();

    String getIntitule();

    StatutProjet getStatut();

    PrioriteProjet getPriorite();

    Integer getPourcentageAvancement();

    LocalDate getDateFinPrevue();

    BigDecimal getPlannedBudget();
}
