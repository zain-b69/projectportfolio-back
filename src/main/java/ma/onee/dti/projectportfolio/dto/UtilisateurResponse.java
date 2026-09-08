package ma.onee.dti.projectportfolio.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UtilisateurResponse {

    private final Long idUtilisateur;
    private final String nom;
    private final String prenom;
    private final String email;
    private final LocalDateTime dateDerniereConnexion;
    private final String role;
}
