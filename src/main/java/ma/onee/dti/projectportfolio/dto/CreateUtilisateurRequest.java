package ma.onee.dti.projectportfolio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.onee.dti.projectportfolio.enums.RoleLibelle;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUtilisateurRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prenom est obligatoire")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email est invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caracteres")
    private String motDePasse;

    @NotNull(message = "Le role est obligatoire")
    private RoleLibelle role;
}
