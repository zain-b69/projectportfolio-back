package ma.onee.dti.projectportfolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import ma.onee.dti.projectportfolio.dto.CreateRisqueRequest;
import ma.onee.dti.projectportfolio.dto.RisqueResponse;
import ma.onee.dti.projectportfolio.dto.UpdateRisqueRequest;
import ma.onee.dti.projectportfolio.enums.NiveauCriticite;
import ma.onee.dti.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dti.projectportfolio.security.CustomUserDetailsService;
import ma.onee.dti.projectportfolio.security.JwtAuthenticationFilter;
import ma.onee.dti.projectportfolio.security.JwtService;
import ma.onee.dti.projectportfolio.security.SecurityConfig;
import ma.onee.dti.projectportfolio.service.RisqueService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RisqueController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RisqueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RisqueService risqueService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getRisquesByProjetReturnsRiskDataAndPassesProjectId() throws Exception {
        when(risqueService.getRisquesByProjet(10L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/projets/10/risques")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idRisque").value(20L))
                .andExpect(jsonPath("$[0].description").value("Risque budget"))
                .andExpect(jsonPath("$[0].niveauCriticite").value("ELEVE"))
                .andExpect(jsonPath("$[0].idProjet").value(10L));

        verify(risqueService).getRisquesByProjet(10L);
    }

    @Test
    void getRisqueByIdReturnsRiskDataAndPassesRiskId() throws Exception {
        when(risqueService.getRisqueById(20L)).thenReturn(response());

        mockMvc.perform(get("/risques/20")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idRisque").value(20L))
                .andExpect(jsonPath("$.description").value("Risque budget"))
                .andExpect(jsonPath("$.niveauCriticite").value("ELEVE"))
                .andExpect(jsonPath("$.idProjet").value(10L));

        verify(risqueService).getRisqueById(20L);
    }

    @Test
    void postRisqueValidRequestReturnsCreatedAndPassesAuthenticatedEmail() throws Exception {
        when(risqueService.createRisque(
                eq(10L),
                any(CreateRisqueRequest.class),
                eq("pm@example.com")
        )).thenReturn(response());

        mockMvc.perform(post("/projets/10/risques")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idRisque").value(20L))
                .andExpect(jsonPath("$.idProjet").value(10L));

        ArgumentCaptor<CreateRisqueRequest> requestCaptor = ArgumentCaptor.forClass(CreateRisqueRequest.class);
        verify(risqueService).createRisque(eq(10L), requestCaptor.capture(), eq("pm@example.com"));
        assertThat(requestCaptor.getValue().getDescription()).isEqualTo("Risque budget");
        assertThat(requestCaptor.getValue().getNiveauCriticite()).isEqualTo(NiveauCriticite.ELEVE);
    }

    @Test
    void postRisqueMissingDescriptionReturnsBadRequest() throws Exception {
        CreateRisqueRequest request = createRequest();
        request.setDescription(null);

        mockMvc.perform(post("/projets/10/risques")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation echouee"));

        verifyNoInteractions(risqueService);
    }

    @Test
    void postRisqueBlankDescriptionReturnsBadRequest() throws Exception {
        CreateRisqueRequest request = createRequest();
        request.setDescription(" ");

        mockMvc.perform(post("/projets/10/risques")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation echouee"));

        verifyNoInteractions(risqueService);
    }

    @Test
    void postRisqueDescriptionLongerThanLimitReturnsBadRequest() throws Exception {
        CreateRisqueRequest request = createRequest();
        request.setDescription("a".repeat(1001));

        mockMvc.perform(post("/projets/10/risques")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation echouee"));

        verifyNoInteractions(risqueService);
    }

    @Test
    void postRisqueMissingNiveauCriticiteReturnsBadRequest() throws Exception {
        CreateRisqueRequest request = createRequest();
        request.setNiveauCriticite(null);

        mockMvc.perform(post("/projets/10/risques")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation echouee"));

        verifyNoInteractions(risqueService);
    }

    @Test
    void postRisqueInvalidEnumValueReturnsBadRequest() throws Exception {
        String request = """
                {
                  "description": "Risque budget",
                  "niveauCriticite": "IMPORTANT"
                }
                """;

        mockMvc.perform(post("/projets/10/risques")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Requete invalide"));

        verifyNoInteractions(risqueService);
    }

    @Test
    void putRisqueValidRequestReturnsOkAndPassesRiskIdAndAuthenticatedEmail() throws Exception {
        when(risqueService.updateRisque(
                eq(20L),
                any(UpdateRisqueRequest.class),
                eq("pm@example.com")
        )).thenReturn(response());

        mockMvc.perform(put("/risques/20")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idRisque").value(20L));

        ArgumentCaptor<UpdateRisqueRequest> requestCaptor = ArgumentCaptor.forClass(UpdateRisqueRequest.class);
        verify(risqueService).updateRisque(eq(20L), requestCaptor.capture(), eq("pm@example.com"));
        assertThat(requestCaptor.getValue().getDescription()).isEqualTo("Risque corrige");
        assertThat(requestCaptor.getValue().getNiveauCriticite()).isEqualTo(NiveauCriticite.CRITIQUE);
    }

    @Test
    void putRisqueInvalidRequestReturnsBadRequest() throws Exception {
        UpdateRisqueRequest request = new UpdateRisqueRequest();

        mockMvc.perform(put("/risques/20")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation echouee"));

        verifyNoInteractions(risqueService);
    }

    @Test
    void deleteRisqueReturnsNoContentPassesRiskIdAndAuthenticatedEmailAndHasEmptyBody() throws Exception {
        mockMvc.perform(delete("/risques/20")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(risqueService).deleteRisque(20L, "pm@example.com");
    }

    @Test
    void unauthenticatedRequestsReturnForbidden() throws Exception {
        mockMvc.perform(get("/projets/10/risques"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/risques/20"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/projets/10/risques")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAndSimpleUserCannotWriteRisks() throws Exception {
        mockMvc.perform(post("/projets/10/risques")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/risques/20")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/risques/20")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/projets/10/risques")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/risques/20")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/risques/20")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void resourceNotFoundExceptionUsesGlobalHandler() throws Exception {
        when(risqueService.getRisquesByProjet(99L))
                .thenThrow(new ResourceNotFoundException("Projet introuvable"));
        when(risqueService.getRisqueById(99L))
                .thenThrow(new ResourceNotFoundException("Risque introuvable"));

        mockMvc.perform(get("/projets/99/risques")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Projet introuvable"));

        mockMvc.perform(get("/risques/99")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Risque introuvable"));
    }

    @Test
    void accessDeniedExceptionUsesGlobalHandler() throws Exception {
        when(risqueService.updateRisque(
                eq(20L),
                any(UpdateRisqueRequest.class),
                eq("pm@example.com")
        )).thenThrow(new AccessDeniedException("Le responsable projet ne peut gerer que ses propres risques"));

        mockMvc.perform(put("/risques/20")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acces refuse"));
    }

    @Test
    void deleteRisqueNotFoundUsesGlobalHandler() throws Exception {
        doThrow(new ResourceNotFoundException("Risque introuvable"))
                .when(risqueService).deleteRisque(99L, "pm@example.com");

        mockMvc.perform(delete("/risques/99")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Risque introuvable"));
    }

    private CreateRisqueRequest createRequest() {
        CreateRisqueRequest request = new CreateRisqueRequest();
        request.setDescription("Risque budget");
        request.setNiveauCriticite(NiveauCriticite.ELEVE);
        return request;
    }

    private UpdateRisqueRequest updateRequest() {
        UpdateRisqueRequest request = new UpdateRisqueRequest();
        request.setDescription("Risque corrige");
        request.setNiveauCriticite(NiveauCriticite.CRITIQUE);
        return request;
    }

    private RisqueResponse response() {
        return RisqueResponse.builder()
                .idRisque(20L)
                .description("Risque budget")
                .niveauCriticite(NiveauCriticite.ELEVE)
                .idProjet(10L)
                .build();
    }
}
