package ma.onee.dti.projectportfolio.repository.projection;

import ma.onee.dti.projectportfolio.enums.PrioriteProjet;

public interface ProjectPriorityCountProjection {

    PrioriteProjet getPriorite();

    long getCount();
}
