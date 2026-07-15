package ma.onee.dsi.projectportfolio.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoleResponse {

    private final Long idRole;
    private final String libelle;
}
