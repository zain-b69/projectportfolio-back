package ma.onee.dsi.projectportfolio.repository.projection;

import ma.onee.dsi.projectportfolio.enums.NiveauCriticite;

public interface RiskCriticalityCountProjection {

    NiveauCriticite getNiveauCriticite();

    long getCount();
}
