package ma.onee.dti.projectportfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import ma.onee.dti.projectportfolio.enums.TypeCout;

@Getter
@Builder
public class CoutResponse {

    private final Long idCout;
    private final TypeCout type;
    private final BigDecimal montant;
    private final LocalDate dateCout;
    private final Long idProjet;
}
