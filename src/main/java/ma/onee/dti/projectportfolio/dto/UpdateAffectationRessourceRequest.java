package ma.onee.dti.projectportfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import ma.onee.dti.projectportfolio.enums.NatureIntervention;

@Getter
@Setter
public class UpdateAffectationRessourceRequest {

    @NotNull(message = "La nature d'intervention est obligatoire")
    private NatureIntervention natureIntervention;

    @NotNull(message = "La charge JH est obligatoire")
    @DecimalMin(value = "0.00", inclusive = false, message = "La charge JH doit etre strictement positive")
    private BigDecimal chargeJH;
}
