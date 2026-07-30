package ma.onee.dsi.projectportfolio.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;

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
