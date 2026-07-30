package ma.onee.dsi.projectportfolio.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.onee.dsi.projectportfolio.dto.DashboardAttentionProjectResponse;
import ma.onee.dsi.projectportfolio.dto.DashboardAttentionReason;
import ma.onee.dsi.projectportfolio.dto.DashboardBudgetProjectResponse;
import ma.onee.dsi.projectportfolio.dto.DashboardCountItemResponse;
import ma.onee.dsi.projectportfolio.dto.DashboardKpisResponse;
import ma.onee.dsi.projectportfolio.dto.DashboardResponse;
import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;
import ma.onee.dsi.projectportfolio.security.CustomUserDetailsService;
import ma.onee.dsi.projectportfolio.security.JwtAuthenticationFilter;
import ma.onee.dsi.projectportfolio.security.JwtService;
import ma.onee.dsi.projectportfolio.security.SecurityConfig;
import ma.onee.dsi.projectportfolio.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void authenticatedAdminReceivesDashboard() throws Exception {
        when(dashboardService.getDashboard()).thenReturn(response());

        mockMvc.perform(get("/dashboard").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(dashboardService).getDashboard();
    }

    @Test
    void authenticatedProjectManagerReceivesDashboard() throws Exception {
        when(dashboardService.getDashboard()).thenReturn(response());

        mockMvc.perform(get("/dashboard").with(user("pm@example.com").roles("RESPONSABLE_PROJET")))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedSimpleUserReceivesDashboard() throws Exception {
        when(dashboardService.getDashboard()).thenReturn(response());

        mockMvc.perform(get("/dashboard").with(user("user@example.com").roles("UTILISATEUR_SIMPLE")))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnedJsonContainsDashboardSections() throws Exception {
        when(dashboardService.getDashboard()).thenReturn(response());

        mockMvc.perform(get("/dashboard").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalProjects").value(1))
                .andExpect(jsonPath("$.projectsByStatus[0].key").value("EN_RETARD"))
                .andExpect(jsonPath("$.projectsByPriority[0].key").value("CRITIQUE"))
                .andExpect(jsonPath("$.budgetVsConsumedByProject[0].projectId").value(10))
                .andExpect(jsonPath("$.risksByCriticality[0].key").value("CRITIQUE"))
                .andExpect(jsonPath("$.projectsRequiringAttention[0].reasons[0]").value("DELAYED"));

        verify(dashboardService).getDashboard();
    }

    private DashboardResponse response() {
        return DashboardResponse.builder()
                .kpis(DashboardKpisResponse.builder()
                        .totalProjects(1)
                        .plannedProjects(0)
                        .projectsInProgress(0)
                        .completedProjects(0)
                        .delayedProjects(1)
                        .totalPlannedBudget(new BigDecimal("1000.00"))
                        .totalConsumedCost(new BigDecimal("1200.00"))
                        .globalBudgetVariance(new BigDecimal("-200.00"))
                        .criticalRisks(1)
                        .build())
                .projectsByStatus(List.of(count("EN_RETARD", 1)))
                .projectsByPriority(List.of(count("CRITIQUE", 1)))
                .budgetVsConsumedByProject(List.of(DashboardBudgetProjectResponse.builder()
                        .projectId(10L)
                        .code("PRJ-001")
                        .intitule("Projet critique")
                        .plannedBudget(new BigDecimal("1000.00"))
                        .consumedCost(new BigDecimal("1200.00"))
                        .variance(new BigDecimal("-200.00"))
                        .build()))
                .risksByCriticality(List.of(count("CRITIQUE", 1)))
                .projectsRequiringAttention(List.of(DashboardAttentionProjectResponse.builder()
                        .projectId(10L)
                        .code("PRJ-001")
                        .intitule("Projet critique")
                        .statut(StatutProjet.EN_RETARD)
                        .priorite(PrioriteProjet.CRITIQUE)
                        .pourcentageAvancement(40)
                        .dateFinPrevue(LocalDate.of(2026, 12, 31))
                        .plannedBudget(new BigDecimal("1000.00"))
                        .consumedCost(new BigDecimal("1200.00"))
                        .budgetVariance(new BigDecimal("-200.00"))
                        .criticalRiskCount(1)
                        .reasons(List.of(DashboardAttentionReason.DELAYED))
                        .build()))
                .build();
    }

    private DashboardCountItemResponse count(String key, long count) {
        return DashboardCountItemResponse.builder()
                .key(key)
                .count(count)
                .build();
    }
}
