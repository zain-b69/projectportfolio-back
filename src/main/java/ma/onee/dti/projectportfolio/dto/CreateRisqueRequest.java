package ma.onee.dti.projectportfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import ma.onee.dti.projectportfolio.enums.NiveauCriticite;

@Getter
@Setter
public class CreateRisqueRequest {

    @NotBlank(message = "La description est obligatoire")
    @Size(max = 1000, message = "La description ne peut pas depasser 1000 caracteres")
    private String description;

    @NotNull(message = "Le niveau de criticite est obligatoire")
    private NiveauCriticite niveauCriticite;
}
