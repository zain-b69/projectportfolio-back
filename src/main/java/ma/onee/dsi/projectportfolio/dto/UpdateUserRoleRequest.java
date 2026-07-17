package ma.onee.dsi.projectportfolio.dto;

import jakarta.validation.constraints.NotNull;
import ma.onee.dsi.projectportfolio.enums.RoleLibelle;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRoleRequest {

    @NotNull(message = "Le libelle du role est obligatoire")
    private RoleLibelle libelle;
}
