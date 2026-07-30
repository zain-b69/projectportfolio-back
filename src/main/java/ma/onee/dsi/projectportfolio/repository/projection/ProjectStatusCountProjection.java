package ma.onee.dsi.projectportfolio.repository.projection;

import ma.onee.dsi.projectportfolio.enums.StatutProjet;

public interface ProjectStatusCountProjection {

    StatutProjet getStatut();

    long getCount();
}
