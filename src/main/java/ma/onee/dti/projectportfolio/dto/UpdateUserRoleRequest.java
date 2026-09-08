package ma.onee.dti.projectportfolio.dto;

import jakarta.validation.constraints.NotNull;
import ma.onee.dti.projectportfolio.enums.RoleLibelle;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRoleRequest {

    @NotNull(message = "Le libelle du role est obligatoire")
    private RoleLibelle libelle;
}
