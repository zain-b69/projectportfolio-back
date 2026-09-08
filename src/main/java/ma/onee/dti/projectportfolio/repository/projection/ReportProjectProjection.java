package ma.onee.dti.projectportfolio.repository.projection;

import java.math.BigDecimal;
import ma.onee.dti.projectportfolio.enums.PrioriteProjet;
import ma.onee.dti.projectportfolio.enums.StatutProjet;

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
