package ma.onee.dsi.projectportfolio.repository.projection;

import java.math.BigDecimal;
import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;

public interface ReportProjectProjection {

    Long getProjectId();

    String getCode();

    String getIntitule();

    StatutProjet getStatut();

    PrioriteProjet getPriorite();

    Integer getPourcentageAvancement();

    BigDecimal getPlannedBudget();

    String getPrenomResponsable();

    String getNomResponsable();
}
