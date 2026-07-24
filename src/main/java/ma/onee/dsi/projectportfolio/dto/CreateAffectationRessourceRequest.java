package ma.onee.dsi.projectportfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import ma.onee.dsi.projectportfolio.enums.NatureIntervention;

@Getter
@Setter
public class CreateAffectationRessourceRequest {

    @NotNull(message = "Le projet est obligatoire")
    private Long idProjet;

    @NotNull(message = "La ressource est obligatoire")
    private Long idRessource;

    @NotNull(message = "La nature d'intervention est obligatoire")
    private NatureIntervention natureIntervention;

    @NotNull(message = "La charge JH est obligatoire")
    @DecimalMin(value = "0.00", inclusive = false, message = "La charge JH doit etre strictement positive")
    private BigDecimal chargeJH;
}
