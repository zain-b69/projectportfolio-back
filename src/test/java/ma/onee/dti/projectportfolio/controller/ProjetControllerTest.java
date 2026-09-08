package ma.onee.dti.projectportfolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.onee.dti.projectportfolio.dto.ProjetResponse;
import ma.onee.dti.projectportfolio.dto.ProjetSearchCriteria;
import ma.onee.dti.projectportfolio.enums.NiveauCriticite;
import ma.onee.dti.projectportfolio.enums.PrioriteProjet;
import ma.onee.dti.projectportfolio.enums.StatutProjet;
import ma.onee.dti.projectportfolio.security.CustomUserDetailsService;
import ma.onee.dti.projectportfolio.security.JwtAuthenticationFilter;
import ma.onee.dti.projectportfolio.security.JwtService;
import ma.onee.dti.projectportfolio.security.SecurityConfig;
import ma.onee.dti.projectportfolio.service.ProjetExportService;
import ma.onee.dti.projectportfolio.service.ProjetService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjetController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ProjetControllerTest {

    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjetService projetService;

    @MockitoBean
    private ProjetExportService projetExportService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void searchProjetsPassesSearchCriteria() throws Exception {
        when(projetService.searchProjets(org.mockito.ArgumentMatchers.any(ProjetSearchCriteria.class)))
                .thenReturn(List.of(response()));

        mockMvc.perform(get("/projets")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE"))
                        .param("search", "reseau")
                        .param("niveauRisque", "ELEVE"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProjetSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ProjetSearchCriteria.class);
        verify(projetService).searchProjets(criteriaCaptor.capture());

        assertThat(criteriaCaptor.getValue().getSearch()).isEqualTo("reseau");
        assertThat(criteriaCaptor.getValue().getNiveauRisque()).isEqualTo(NiveauCriticite.ELEVE);
    }

    @Test
    void exportProjetsAuthenticatedUserReturnsExcelFileAndPassesCriteriaAndResults() throws Exception {
        List<ProjetResponse> projets = List.of(response());
        byte[] exportContent = new byte[] {1, 2, 3, 4};

        when(projetService.searchProjets(org.mockito.ArgumentMatchers.any(ProjetSearchCriteria.class)))
                .thenReturn(projets);
        when(projetExportService.exportProjects(same(projets))).thenReturn(exportContent);

        mockMvc.perform(get("/projets/export")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE"))
                        .param("search", "reseau")
                        .param("code", "PRJ")
                        .param("statut", "EN_COURS")
                        .param("priorite", "ELEVEE")
                        .param("niveauRisque", "ELEVE")
                        .param("budgetMin", "1000.00")
                        .param("avancementMax", "80"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(EXCEL_CONTENT_TYPE))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("attachment"),
                                org.hamcrest.Matchers.containsString("filename=\"projets_"),
                                org.hamcrest.Matchers.containsString(".xlsx\"")
                        )
                ))
                .andExpect(content().bytes(exportContent));

        ArgumentCaptor<ProjetSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ProjetSearchCriteria.class);
        verify(projetService).searchProjets(criteriaCaptor.capture());
        verify(projetExportService).exportProjects(same(projets));

        ProjetSearchCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.getSearch()).isEqualTo("reseau");
        assertThat(criteria.getCode()).isEqualTo("PRJ");
        assertThat(criteria.getStatut()).isEqualTo(StatutProjet.EN_COURS);
        assertThat(criteria.getPriorite()).isEqualTo(PrioriteProjet.ELEVEE);
        assertThat(criteria.getNiveauRisque()).isEqualTo(NiveauCriticite.ELEVE);
        assertThat(criteria.getBudgetMin()).isEqualByComparingTo("1000.00");
        assertThat(criteria.getAvancementMax()).isEqualTo(80);
    }

    @Test
    void searchProjetsInvalidRiskLevelReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/projets")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE"))
                        .param("niveauRisque", "INCONNU"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(projetService, projetExportService);
    }

    @Test
    void exportProjetsUnauthenticatedUserIsRejected() throws Exception {
        mockMvc.perform(get("/projets/export"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(projetService, projetExportService);
    }

    private ProjetResponse response() {
        return ProjetResponse.builder()
                .idProjet(10L)
                .code("PRJ-001")
                .intitule("Projet export")
                .statut("EN_COURS")
                .priorite("ELEVEE")
                .budgetPrevisionnel(new BigDecimal("1000.00"))
                .pourcentageAvancement(40)
                .dateDebutPrevue(LocalDate.of(2026, 1, 1))
                .dateFinPrevue(LocalDate.of(2026, 12, 31))
                .build();
    }
}
