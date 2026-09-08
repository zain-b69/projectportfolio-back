package ma.onee.dti.projectportfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import ma.onee.dti.projectportfolio.enums.NatureRessource;

@Getter
@Setter
public class CreateRessourceRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 255, message = "Le nom ne peut pas depasser 255 caracteres")
    private String nom;

    @NotBlank(message = "La fonction est obligatoire")
    @Size(max = 255, message = "La fonction ne peut pas depasser 255 caracteres")
    private String fonction;

    @NotNull(message = "La nature est obligatoire")
    private NatureRessource nature;
}
