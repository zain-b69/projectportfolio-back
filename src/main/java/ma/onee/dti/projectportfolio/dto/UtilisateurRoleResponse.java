package ma.onee.dti.projectportfolio.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UtilisateurRoleResponse {

    private final Long idUtilisateur;
    private final String nom;
    private final String prenom;
    private final String email;
    private final String role;
}
