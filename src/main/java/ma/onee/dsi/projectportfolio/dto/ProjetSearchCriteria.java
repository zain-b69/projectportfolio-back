package ma.onee.dsi.projectportfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class ProjetSearchCriteria {

    private String code;

    private String intitule;

    private StatutProjet statut;

    private PrioriteProjet priorite;

    private Long responsableId;

    private String responsableEmail;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateDebutPrevueFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateDebutPrevueTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFinPrevueFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFinPrevueTo;

    @DecimalMin(value = "0.00", message = "Le budget minimum ne peut pas etre negatif")
    private BigDecimal budgetMin;

    @DecimalMin(value = "0.00", message = "Le budget maximum ne peut pas etre negatif")
    private BigDecimal budgetMax;

    @Min(value = 0, message = "L'avancement minimum doit etre superieur ou egal a 0")
    @Max(value = 100, message = "L'avancement minimum doit etre inferieur ou egal a 100")
    private Integer avancementMin;

    @Min(value = 0, message = "L'avancement maximum doit etre superieur ou egal a 0")
    @Max(value = 100, message = "L'avancement maximum doit etre inferieur ou egal a 100")
    private Integer avancementMax;
}
