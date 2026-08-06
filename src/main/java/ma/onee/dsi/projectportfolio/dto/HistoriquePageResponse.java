package ma.onee.dsi.projectportfolio.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HistoriquePageResponse {

    private final List<HistoriqueModificationResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;
}
