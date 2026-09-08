package ma.onee.dti.projectportfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import ma.onee.dti.projectportfolio.enums.TypeCout;

@Getter
@Setter
public class UpdateCoutRequest {

    @NotNull(message = "Le type de cout est obligatoire")
    private TypeCout type;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(
            value = "0.00",
            inclusive = false,
            message = "Le montant doit etre strictement superieur a zero"
    )
    @Digits(
            integer = 17,
            fraction = 2,
            message = "Le montant doit contenir au maximum 17 chiffres entiers et 2 decimales"
    )
    private BigDecimal montant;

    @NotNull(message = "La date du cout est obligatoire")
    private LocalDate dateCout;
}
