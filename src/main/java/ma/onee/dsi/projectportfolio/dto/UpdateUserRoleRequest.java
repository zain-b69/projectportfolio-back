package ma.onee.dsi.projectportfolio.dto;

import ma.onee.dsi.projectportfolio.enums.RoleLibelle;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRoleRequest {

    private RoleLibelle libelle;
}
