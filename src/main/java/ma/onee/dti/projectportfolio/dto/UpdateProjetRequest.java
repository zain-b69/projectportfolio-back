package ma.onee.dti.projectportfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import ma.onee.dti.projectportfolio.enums.PrioriteProjet;
import ma.onee.dti.projectportfolio.enums.StatutProjet;

@Getter
@Setter
public class UpdateProjetRequest {

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 100, message = "Le code ne peut pas depasser 100 caracteres")
    private String code;

    @NotBlank(message = "L'intitule est obligatoire")
    @Size(max = 255, message = "L'intitule ne peut pas depasser 255 caracteres")
    private String intitule;

    @Size(max = 2000, message = "Le descriptif ne peut pas depasser 2000 caracteres")
    private String descriptif;

    @NotNull(message = "La date de debut prevue est obligatoire")
    private LocalDate dateDebutPrevue;

    @NotNull(message = "La date de fin prevue est obligatoire")
    private LocalDate dateFinPrevue;

    private LocalDate dateDebutReelle;

    private LocalDate dateFinReelle;

    @NotNull(message = "Le statut est obligatoire")
    private StatutProjet statut;

    @NotNull(message = "Le budget previsionnel est obligatoire")
    @DecimalMin(value = "0.00", message = "Le budget ne peut pas etre negatif")
    private BigDecimal budgetPrevisionnel;

    @NotNull(message = "La priorite est obligatoire")
    private PrioriteProjet priorite;

    @NotNull(message = "Le pourcentage d'avancement est obligatoire")
    @Min(value = 0, message = "Le pourcentage d'avancement doit etre superieur ou egal a 0")
    @Max(value = 100, message = "Le pourcentage d'avancement doit etre inferieur ou egal a 100")
    private Integer pourcentageAvancement;
}
