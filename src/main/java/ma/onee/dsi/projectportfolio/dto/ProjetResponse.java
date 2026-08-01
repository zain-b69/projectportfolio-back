package ma.onee.dsi.projectportfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import ma.onee.dsi.projectportfolio.enums.NiveauCriticite;

@Getter
@Builder
public class ProjetResponse {

    private final Long idProjet;
    private final String code;
    private final String intitule;
    private final String descriptif;
    private final LocalDate dateDebutPrevue;
    private final LocalDate dateFinPrevue;
    private final LocalDate dateDebutReelle;
    private final LocalDate dateFinReelle;
    private final String statut;
    private final BigDecimal budgetPrevisionnel;
    private final String priorite;
    private final NiveauCriticite niveauRisque;
    private final Integer pourcentageAvancement;
    private final Long idResponsable;
    private final String nomResponsable;
    private final String prenomResponsable;
    private final String emailResponsable;
}
