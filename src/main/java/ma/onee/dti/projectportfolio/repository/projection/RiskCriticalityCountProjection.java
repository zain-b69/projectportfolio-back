package ma.onee.dti.projectportfolio.repository.projection;

import ma.onee.dti.projectportfolio.enums.NiveauCriticite;

public interface RiskCriticalityCountProjection {

    NiveauCriticite getNiveauCriticite();

    long getCount();
}
