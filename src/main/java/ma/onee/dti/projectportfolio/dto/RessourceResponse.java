package ma.onee.dti.projectportfolio.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RessourceResponse {

    private final Long idRessource;
    private final String nom;
    private final String fonction;
    private final String nature;
}
