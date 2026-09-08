package ma.onee.dti.projectportfolio.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HistoriqueModificationResponse {

    private final Long idHistoriqueModification;
    private final LocalDateTime dateModification;
    private final String typeAction;
    private final String description;
    private final Long idUtilisateur;
    private final String nomUtilisateur;
    private final String prenomUtilisateur;
    private final String emailUtilisateur;
    private final String nomCompletUtilisateur;
    private final Long idProjet;
    private final String codeProjet;
    private final String intituleProjet;
}
