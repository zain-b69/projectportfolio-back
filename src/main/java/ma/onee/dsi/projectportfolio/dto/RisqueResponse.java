package ma.onee.dsi.projectportfolio.dto;

import lombok.Builder;
import lombok.Getter;
import ma.onee.dsi.projectportfolio.enums.NiveauCriticite;

@Getter
@Builder
public class RisqueResponse {

    private final Long idRisque;
    private final String description;
    private final NiveauCriticite niveauCriticite;
    private final Long idProjet;
}
