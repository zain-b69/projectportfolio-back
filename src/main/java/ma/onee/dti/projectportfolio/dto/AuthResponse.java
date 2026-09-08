package ma.onee.dti.projectportfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private Long idUtilisateur;
    private String nom;
    private String prenom;
    private String email;
    private String role;
}
