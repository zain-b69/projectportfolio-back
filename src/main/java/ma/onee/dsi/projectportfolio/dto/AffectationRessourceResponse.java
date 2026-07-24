package ma.onee.dsi.projectportfolio.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AffectationRessourceResponse {

    private final Long idAffectationRessource;
    private final String natureIntervention;
    private final BigDecimal chargeJH;
    private final Long idProjet;
    private final String codeProjet;
    private final String intituleProjet;
    private final Long idRessource;
    private final String nomRessource;
    private final String fonctionRessource;
    private final String natureRessource;
}
