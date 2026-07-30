package ma.onee.dsi.projectportfolio.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardCountItemResponse {

    private final String key;
    private final long count;
}
