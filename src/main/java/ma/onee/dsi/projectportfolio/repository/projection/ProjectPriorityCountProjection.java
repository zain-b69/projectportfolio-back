package ma.onee.dsi.projectportfolio.repository.projection;

import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;

public interface ProjectPriorityCountProjection {

    PrioriteProjet getPriorite();

    long getCount();
}
