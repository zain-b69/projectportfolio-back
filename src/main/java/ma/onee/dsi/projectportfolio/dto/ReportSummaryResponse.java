package ma.onee.dsi.projectportfolio.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportSummaryResponse {

    private final ReportKpisResponse kpis;
    private final List<ReportProjectRowResponse> projects;
    private final List<DashboardCountItemResponse> projectsByStatus;
    private final List<DashboardCountItemResponse> projectsByPriority;
    private final List<ReportBudgetByStatusResponse> budgetByStatus;
    private final List<ReportCostByTypeResponse> costByType;
    private final List<ReportResourceAllocationResponse> resourceAllocationByNature;
}
