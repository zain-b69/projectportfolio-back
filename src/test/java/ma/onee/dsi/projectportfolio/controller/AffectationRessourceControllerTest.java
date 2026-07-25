package ma.onee.dsi.projectportfolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import ma.onee.dsi.projectportfolio.dto.AffectationRessourceResponse;
import ma.onee.dsi.projectportfolio.dto.CreateAffectationRessourceRequest;
import ma.onee.dsi.projectportfolio.dto.UpdateAffectationRessourceRequest;
import ma.onee.dsi.projectportfolio.enums.NatureIntervention;
import ma.onee.dsi.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dsi.projectportfolio.security.CustomUserDetailsService;
import ma.onee.dsi.projectportfolio.security.JwtAuthenticationFilter;
import ma.onee.dsi.projectportfolio.security.JwtService;
import ma.onee.dsi.projectportfolio.security.SecurityConfig;
import ma.onee.dsi.projectportfolio.service.AffectationRessourceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AffectationRessourceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AffectationRessourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AffectationRessourceService affectationRessourceService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getByProjetAllowsAuthenticatedUser() throws Exception {
        when(affectationRessourceService.getAffectationsByProjet(10L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/projets/10/affectations-ressources")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idAffectationRessource").value(30L));
    }

    @Test
    void getByRessourceAllowsAuthenticatedUser() throws Exception {
        when(affectationRessourceService.getAffectationsByRessource(20L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/ressources/20/affectations")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idRessource").value(20L));
    }

    @Test
    void postAllowsAdminAndPassesAuthenticatedEmail() throws Exception {
        when(affectationRessourceService.createAffectation(
                any(CreateAffectationRessourceRequest.class),
                eq("admin@example.com")
        )).thenReturn(response());

        mockMvc.perform(post("/projets/10/affectations-ressources")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idAffectationRessource").value(30L));

        ArgumentCaptor<CreateAffectationRessourceRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateAffectationRessourceRequest.class);
        verify(affectationRessourceService).createAffectation(requestCaptor.capture(), eq("admin@example.com"));
        assertThat(requestCaptor.getValue().getIdProjet()).isEqualTo(10L);
    }

    @Test
    void postAllowsProjectManager() throws Exception {
        when(affectationRessourceService.createAffectation(
                any(CreateAffectationRessourceRequest.class),
                eq("pm@example.com")
        )).thenReturn(response());

        mockMvc.perform(post("/projets/10/affectations-ressources")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void writeEndpointsRejectSimpleUser() throws Exception {
        mockMvc.perform(post("/projets/10/affectations-ressources")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/projets/10/affectations-ressources/30")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/projets/10/affectations-ressources/30")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidPostRequestReturnsBadRequest() throws Exception {
        CreateAffectationRessourceRequest request = new CreateAffectationRessourceRequest();
        request.setIdProjet(10L);

        mockMvc.perform(post("/projets/10/affectations-ressources")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation echouee"));
    }

    @Test
    void invalidPutRequestReturnsBadRequest() throws Exception {
        UpdateAffectationRessourceRequest request = new UpdateAffectationRessourceRequest();

        mockMvc.perform(put("/projets/10/affectations-ressources/30")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation echouee"));
    }

    @Test
    void putPassesProjectIdAffectationIdAndAuthenticatedEmail() throws Exception {
        when(affectationRessourceService.updateAffectation(
                eq(10L),
                eq(30L),
                any(UpdateAffectationRessourceRequest.class),
                eq("pm@example.com")
        )).thenReturn(response());

        mockMvc.perform(put("/projets/10/affectations-ressources/30")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAffectationRessource").value(30L));

        verify(affectationRessourceService).updateAffectation(
                eq(10L),
                eq(30L),
                any(UpdateAffectationRessourceRequest.class),
                eq("pm@example.com")
        );
    }

    @Test
    void deletePassesProjectIdAffectationIdAndAuthenticatedEmail() throws Exception {
        mockMvc.perform(delete("/projets/10/affectations-ressources/30")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(affectationRessourceService).deleteAffectation(10L, 30L, "admin@example.com");
    }

    @Test
    void resourceNotFoundExceptionUsesGlobalHandler() throws Exception {
        when(affectationRessourceService.getAffectationsByProjet(99L))
                .thenThrow(new ResourceNotFoundException("Projet introuvable"));

        mockMvc.perform(get("/projets/99/affectations-ressources")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Projet introuvable"));
    }

    @Test
    void accessDeniedExceptionUsesGlobalHandler() throws Exception {
        when(affectationRessourceService.updateAffectation(
                eq(10L),
                eq(30L),
                any(UpdateAffectationRessourceRequest.class),
                eq("pm@example.com")
        )).thenThrow(new AccessDeniedException("Acces refuse"));

        mockMvc.perform(put("/projets/10/affectations-ressources/30")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acces refuse"));
    }

    private CreateAffectationRessourceRequest createRequest() {
        CreateAffectationRessourceRequest request = new CreateAffectationRessourceRequest();
        request.setIdProjet(10L);
        request.setIdRessource(20L);
        request.setNatureIntervention(NatureIntervention.DEVELOPPEMENT);
        request.setChargeJH(new BigDecimal("5.50"));
        return request;
    }

    private UpdateAffectationRessourceRequest updateRequest() {
        UpdateAffectationRessourceRequest request = new UpdateAffectationRessourceRequest();
        request.setNatureIntervention(NatureIntervention.TEST);
        request.setChargeJH(new BigDecimal("3.00"));
        return request;
    }

    private AffectationRessourceResponse response() {
        return AffectationRessourceResponse.builder()
                .idAffectationRessource(30L)
                .natureIntervention("DEVELOPPEMENT")
                .chargeJH(new BigDecimal("5.50"))
                .idProjet(10L)
                .codeProjet("PRJ-001")
                .intituleProjet("Projet portefeuille")
                .idRessource(20L)
                .nomRessource("Karim Bennani")
                .fonctionRessource("Developpeur")
                .natureRessource("INTERNE")
                .build();
    }
}
