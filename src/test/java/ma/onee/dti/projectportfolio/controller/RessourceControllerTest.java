package ma.onee.dti.projectportfolio.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import ma.onee.dti.projectportfolio.dto.CreateRessourceRequest;
import ma.onee.dti.projectportfolio.dto.RessourceResponse;
import ma.onee.dti.projectportfolio.dto.UpdateRessourceRequest;
import ma.onee.dti.projectportfolio.enums.NatureRessource;
import ma.onee.dti.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dti.projectportfolio.security.CustomUserDetailsService;
import ma.onee.dti.projectportfolio.security.JwtAuthenticationFilter;
import ma.onee.dti.projectportfolio.security.JwtService;
import ma.onee.dti.projectportfolio.security.SecurityConfig;
import ma.onee.dti.projectportfolio.service.RessourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RessourceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RessourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RessourceService ressourceService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getRessourcesAllowsAuthenticatedUsers() throws Exception {
        when(ressourceService.getAllRessources()).thenReturn(List.of(response()));

        mockMvc.perform(get("/ressources")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idRessource").value(1L));

        mockMvc.perform(get("/ressources?texte=dev&nature=INTERNE")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/ressources")
                        .with(user("user@example.com").roles("UTILISATEUR_SIMPLE")))
                .andExpect(status().isOk());
    }

    @Test
    void writeEndpointsAreReservedToAdmin() throws Exception {
        when(ressourceService.createRessource(any(CreateRessourceRequest.class))).thenReturn(response());
        when(ressourceService.updateRessource(any(Long.class), any(UpdateRessourceRequest.class))).thenReturn(response());

        mockMvc.perform(post("/ressources")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/ressources/1")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/ressources/1")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/ressources")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/ressources/1")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/ressources/1")
                        .with(user("pm@example.com").roles("RESPONSABLE_PROJET")))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRessourceValidatesRequestBody() throws Exception {
        CreateRessourceRequest request = new CreateRessourceRequest();
        request.setNom("");
        request.setFonction("");
        request.setNature(null);

        mockMvc.perform(post("/ressources")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation echouee"));
    }

    @Test
    void getRessourceByIdReturnsNotFoundForUnknownResource() throws Exception {
        when(ressourceService.getRessourceById(99L))
                .thenThrow(new ResourceNotFoundException("Ressource introuvable"));

        mockMvc.perform(get("/ressources/99")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ressource introuvable"));
    }

    @Test
    void updateRessourceReturnsNotFoundForUnknownResource() throws Exception {
        when(ressourceService.updateRessource(any(Long.class), any(UpdateRessourceRequest.class)))
                .thenThrow(new ResourceNotFoundException("Ressource introuvable"));

        mockMvc.perform(put("/ressources/99")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ressource introuvable"));
    }

    @Test
    void deleteRessourceReturnsNotFoundForUnknownResource() throws Exception {
        doThrow(new ResourceNotFoundException("Ressource introuvable"))
                .when(ressourceService).deleteRessource(99L);

        mockMvc.perform(delete("/ressources/99")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ressource introuvable"));
    }

    private CreateRessourceRequest createRequest() {
        CreateRessourceRequest request = new CreateRessourceRequest();
        request.setNom("Karim Bennani");
        request.setFonction("Developpeur");
        request.setNature(NatureRessource.INTERNE);
        return request;
    }

    private UpdateRessourceRequest updateRequest() {
        UpdateRessourceRequest request = new UpdateRessourceRequest();
        request.setNom("Karim Bennani");
        request.setFonction("Architecte");
        request.setNature(NatureRessource.PRESTATAIRE);
        return request;
    }

    private RessourceResponse response() {
        return RessourceResponse.builder()
                .idRessource(1L)
                .nom("Karim Bennani")
                .fonction("Developpeur")
                .nature("INTERNE")
                .build();
    }
}
