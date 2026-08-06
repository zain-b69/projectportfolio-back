package ma.onee.dsi.projectportfolio.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.core.io.Resource;

@Getter
@Builder
public class PieceJointeDownloadResponse {

    private final String nomFichier;
    private final Resource resource;
}
