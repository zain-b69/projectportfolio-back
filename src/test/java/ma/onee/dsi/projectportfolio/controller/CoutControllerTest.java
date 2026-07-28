package ma.onee.dsi.projectportfolio.controller;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.onee.dsi.projectportfolio.dto.CoutResponse;
import ma.onee.dsi.projectportfolio.dto.CreateCoutRequest;
import ma.onee.dsi.projectportfolio.dto.UpdateCoutRequest;
import ma.onee.dsi.projectportfolio.enums.TypeCout;
import ma.onee.dsi.projectportfolio.exception.BusinessRuleException;
import ma.onee.dsi.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dsi.projectportfolio.security.CustomUserDetailsService;
import ma.onee.dsi.projectportfolio.security.JwtAuthenticationFilter;
import ma.onee.dsi.projectportfolio.security.JwtService;
import ma.onee.dsi.projectportfolio.security.SecurityConfig;
import ma.onee.dsi.projectportfolio.service.CoutService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CoutController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CoutService coutService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getCoutsByProjetReturnsCostDataAndPassesProjectId() throws Exception {
        when(coutService.getCoutsByProjet(10L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/projets/10/couts")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idCout").value(20L))
                .andExpect(jsonPath("$[0].type").value("MATERIEL"))
                .andExpect(jsonPath("$[0].montant").value(1200.50))
                .andExpect(jsonPath("$[0].dateCout").value("2026-02-01"))
                .andExpect(jsonPath("$[0].idProjet").value(10L));

        verify(coutService).getCoutsByProjet(10L);
    }

    @Test
    void getCoutByIdReturnsCostDataPassesCostIdAndDoesNotExposeProjectEntity() throws Exception {
        when(coutService.getCoutById(20L)).thenReturn(response());

        mockMvc.perform(get("/couts/20")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCout").value(20L))
                .andExpect(jsonPath("$.type").value("MATERIEL"))
                .andExpect(jsonPath("$.montant").value(1200.50))
                .andExpect(jsonPath("$.dateCout").value("2026-02-01"))
                .andExpect(jsonPath("$.idProjet").value(10L))
                .andExpect(jsonPath("$.projet").doesNotExist());

        verify(coutService).getCoutById(20L);
    }

    @Test
    void postCoutValidRequestReturnsCreatedAndPassesProjectIdRequestAndAuthenticatedEmail() throws Exception {
        when(coutService.createCout(
                eq(10L),
                any(CreateCoutRequest.class),
                eq("pm@example.com")
        )).thenReturn(response());

        mockMvc.perform(post("/projets/10/couts")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCout").value(20L))
                .andExpect(jsonPath("$.type").value("MATERIEL"))
                .andExpect(jsonPath("$.montant").value(1200.50))
                .andExpect(jsonPath("$.dateCout").value("2026-02-01"))
                .andExpect(jsonPath("$.idProjet").value(10L));

        ArgumentCaptor<CreateCoutRequest> requestCaptor = ArgumentCaptor.forClass(CreateCoutRequest.class);
        verify(coutService).createCout(eq(10L), requestCaptor.capture(), eq("pm@example.com"));
        assertThat(requestCaptor.getValue().getType()).isEqualTo(TypeCout.MATERIEL);
        assertThat(requestCaptor.getValue().getMontant()).isEqualByComparingTo("1200.50");
        assertThat(requestCaptor.getValue().getDateCout()).isEqualTo(LocalDate.of(2026, 2, 1));
    }

    @Test
    void postCoutMissingTypeReturnsBadRequest() throws Exception {
        CreateCoutRequest request = createRequest();
        request.setType(null);

        assertInvalidPostRequest(request);
    }

    @Test
    void postCoutInvalidTypeEnumReturnsBadRequest() throws Exception {
        String request = """
                {
                  "type": "VOYAGE",
                  "montant": 1200.50,
                  "dateCout": "2026-02-01"
                }
                """;

        mockMvc.perform(post("/projets/10/couts")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Requete invalide"));

        verifyNoInteractions(coutService);
    }

    @Test
    void postCoutMissingMontantReturnsBadRequest() throws Exception {
        CreateCoutRequest request = createRequest();
        request.setMontant(null);

        assertInvalidPostRequest(request);
    }

    @Test
    void postCoutZeroMontantReturnsBadRequest() throws Exception {
        CreateCoutRequest request = createRequest();
        request.setMontant(BigDecimal.ZERO);

        assertInvalidPostRequest(request);
    }

    @Test
    void postCoutNegativeMontantReturnsBadRequest() throws Exception {
        CreateCoutRequest request = createRequest();
        request.setMontant(new BigDecimal("-1.00"));

        assertInvalidPostRequest(request);
    }

    @Test
    void postCoutMontantWithMoreThanTwoEffectiveDecimalPlacesReturnsBadRequest() throws Exception {
        CreateCoutRequest request = createRequest();
        request.setMontant(new BigDecimal("100.001"));

        assertInvalidPostRequest(request);
    }

    @Test
    void postCoutMissingDateCoutReturnsBadRequest() throws Exception {
        CreateCoutRequest request = createRequest();
        request.setDateCout(null);

        assertInvalidPostRequest(request);
    }

    @Test
    void putCoutValidRequestReturnsOkAndPassesCostIdRequestAndAuthenticatedEmail() throws Exception {
        when(coutService.updateCout(
                eq(20L),
                any(UpdateCoutRequest.class),
                eq("pm@example.com")
        )).thenReturn(updatedResponse());

        mockMvc.perform(put("/couts/20")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCout").value(20L))
                .andExpect(jsonPath("$.type").value("LOGICIEL"))
                .andExpect(jsonPath("$.montant").value(900.25))
                .andExpect(jsonPath("$.dateCout").value("2026-03-01"))
                .andExpect(jsonPath("$.idProjet").value(10L));

        ArgumentCaptor<UpdateCoutRequest> requestCaptor = ArgumentCaptor.forClass(UpdateCoutRequest.class);
        verify(coutService).updateCout(eq(20L), requestCaptor.capture(), eq("pm@example.com"));
        assertThat(requestCaptor.getValue().getType()).isEqualTo(TypeCout.LOGICIEL);
        assertThat(requestCaptor.getValue().getMontant()).isEqualByComparingTo("900.25");
        assertThat(requestCaptor.getValue().getDateCout()).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    void putCoutInvalidRequestReturnsBadRequest() throws Exception {
        UpdateCoutRequest request = new UpdateCoutRequest();

        mockMvc.perform(put("/couts/20")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation echouee"));

        verifyNoInteractions(coutService);
    }

    @Test
    void deleteCoutReturnsNoContentPassesCostIdAndAuthenticatedEmailAndHasEmptyBody() throws Exception {
        mockMvc.perform(delete("/couts/20")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(coutService).deleteCout(20L, "pm@example.com");
    }

    @Test
    void authenticatedRolesCanReadCosts() throws Exception {
        when(coutService.getCoutsByProjet(10L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/projets/10/couts")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/projets/10/couts")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/projets/10/couts")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET")))
                .andExpect(status().isOk());
    }

    @Test
    void adminAndSimpleUserCannotWriteCosts() throws Exception {
        mockMvc.perform(post("/projets/10/couts")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/couts/20")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/couts/20")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/projets/10/couts")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/couts/20")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/couts/20")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(coutService);
    }

    @Test
    void unauthenticatedRequestsReturnForbidden() throws Exception {
        mockMvc.perform(get("/projets/10/couts"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/couts/20"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/projets/10/couts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void resourceNotFoundExceptionUsesGlobalHandler() throws Exception {
        when(coutService.getCoutsByProjet(99L))
                .thenThrow(new ResourceNotFoundException("Projet introuvable"));
        when(coutService.getCoutById(99L))
                .thenThrow(new ResourceNotFoundException("Cout introuvable"));

        mockMvc.perform(get("/projets/99/couts")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Projet introuvable"));

        mockMvc.perform(get("/couts/99")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cout introuvable"));
    }

    @Test
    void accessDeniedExceptionUsesGlobalHandler() throws Exception {
        when(coutService.updateCout(
                eq(20L),
                any(UpdateCoutRequest.class),
                eq("pm@example.com")
        )).thenThrow(new AccessDeniedException("Le responsable projet ne peut gerer que ses propres couts"));

        mockMvc.perform(put("/couts/20")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acces refuse"));
    }

    @Test
    void businessRuleExceptionUsesGlobalHandler() throws Exception {
        when(coutService.createCout(
                eq(10L),
                any(CreateCoutRequest.class),
                eq("pm@example.com")
        )).thenThrow(new BusinessRuleException("Le montant doit etre strictement superieur a zero"));

        mockMvc.perform(post("/projets/10/couts")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Le montant doit etre strictement superieur a zero"));
    }

    @Test
    void invalidJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/projets/10/couts")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Requete invalide"));

        verifyNoInteractions(coutService);
    }

    @Test
    void deleteCoutNotFoundUsesGlobalHandler() throws Exception {
        doThrow(new ResourceNotFoundException("Cout introuvable"))
                .when(coutService).deleteCout(99L, "pm@example.com");

        mockMvc.perform(delete("/couts/99")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cout introuvable"));
    }

    private void assertInvalidPostRequest(CreateCoutRequest request) throws Exception {
        mockMvc.perform(post("/projets/10/couts")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation echouee"));

        verifyNoInteractions(coutService);
    }

    private CreateCoutRequest createRequest() {
        CreateCoutRequest request = new CreateCoutRequest();
        request.setType(TypeCout.MATERIEL);
        request.setMontant(new BigDecimal("1200.50"));
        request.setDateCout(LocalDate.of(2026, 2, 1));
        return request;
    }

    private UpdateCoutRequest updateRequest() {
        UpdateCoutRequest request = new UpdateCoutRequest();
        request.setType(TypeCout.LOGICIEL);
        request.setMontant(new BigDecimal("900.25"));
        request.setDateCout(LocalDate.of(2026, 3, 1));
        return request;
    }

    private CoutResponse response() {
        return CoutResponse.builder()
                .idCout(20L)
                .type(TypeCout.MATERIEL)
                .montant(new BigDecimal("1200.50"))
                .dateCout(LocalDate.of(2026, 2, 1))
                .idProjet(10L)
                .build();
    }

    private CoutResponse updatedResponse() {
        return CoutResponse.builder()
                .idCout(20L)
                .type(TypeCout.LOGICIEL)
                .montant(new BigDecimal("900.25"))
                .dateCout(LocalDate.of(2026, 3, 1))
                .idProjet(10L)
                .build();
    }
}
