package ma.onee.dti.projectportfolio.repository.projection;

import ma.onee.dti.projectportfolio.enums.StatutProjet;

public interface ProjectStatusCountProjection {

    StatutProjet getStatut();

    long getCount();
}
