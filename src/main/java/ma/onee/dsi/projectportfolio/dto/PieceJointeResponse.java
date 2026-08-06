package ma.onee.dsi.projectportfolio.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PieceJointeResponse {

    private final Long idPieceJointe;
    private final String nomFichier;
    private final LocalDateTime dateAjout;
    private final Long idProjet;
}
