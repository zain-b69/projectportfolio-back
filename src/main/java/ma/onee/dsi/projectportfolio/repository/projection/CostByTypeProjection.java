package ma.onee.dsi.projectportfolio.repository.projection;

import java.math.BigDecimal;
import ma.onee.dsi.projectportfolio.enums.TypeCout;

public interface CostByTypeProjection {

    TypeCout getType();

    BigDecimal getTotalMontant();

    Long getCount();
}
