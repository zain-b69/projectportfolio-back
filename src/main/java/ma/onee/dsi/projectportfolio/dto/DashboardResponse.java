package ma.onee.dsi.projectportfolio.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardResponse {

    private final DashboardKpisResponse kpis;
    private final List<DashboardCountItemResponse> projectsByStatus;
    private final List<DashboardCountItemResponse> projectsByPriority;
    private final List<DashboardBudgetProjectResponse> budgetVsConsumedByProject;
    private final List<DashboardCountItemResponse> risksByCriticality;
    private final List<DashboardAttentionProjectResponse> projectsRequiringAttention;
}
