package ma.onee.dti.projectportfolio.repository.projection;

import java.math.BigDecimal;
import ma.onee.dti.projectportfolio.enums.TypeCout;

public interface CostByTypeProjection {

    TypeCout getType();

    BigDecimal getTotalMontant();

    Long getCount();
}
