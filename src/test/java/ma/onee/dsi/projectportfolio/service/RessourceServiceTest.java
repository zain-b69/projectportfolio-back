package ma.onee.dsi.projectportfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import ma.onee.dsi.projectportfolio.dto.CreateRessourceRequest;
import ma.onee.dsi.projectportfolio.dto.RessourceResponse;
import ma.onee.dsi.projectportfolio.dto.UpdateRessourceRequest;
import ma.onee.dsi.projectportfolio.entity.Ressource;
import ma.onee.dsi.projectportfolio.enums.NatureRessource;
import ma.onee.dsi.projectportfolio.exception.BusinessRuleException;
import ma.onee.dsi.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dsi.projectportfolio.repository.AffectationRessourceRepository;
import ma.onee.dsi.projectportfolio.repository.RessourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class RessourceServiceTest {

    @Mock
    private RessourceRepository ressourceRepository;

    @Mock
    private AffectationRessourceRepository affectationRessourceRepository;

    private RessourceService ressourceService;

    @BeforeEach
    void setUp() {
        ressourceService = new RessourceService(ressourceRepository, affectationRessourceRepository);
    }

    @Test
    void createRessourceNormalizesTextAndReturnsResponse() {
        CreateRessourceRequest request = createRequest();

        when(ressourceRepository.save(any(Ressource.class))).thenAnswer(invocation -> {
            Ressource savedRessource = invocation.getArgument(0);
            savedRessource.setIdRessource(10L);
            return savedRessource;
        });

        RessourceResponse response = ressourceService.createRessource(request);

        assertThat(response.getIdRessource()).isEqualTo(10L);
        assertThat(response.getNom()).isEqualTo("Karim Bennani");
        assertThat(response.getFonction()).isEqualTo("Developpeur");
        assertThat(response.getNature()).isEqualTo("INTERNE");

        ArgumentCaptor<Ressource> ressourceCaptor = ArgumentCaptor.forClass(Ressource.class);
        verify(ressourceRepository).save(ressourceCaptor.capture());

        Ressource savedRessource = ressourceCaptor.getValue();
        assertThat(savedRessource.getNom()).isEqualTo("Karim Bennani");
        assertThat(savedRessource.getFonction()).isEqualTo("Developpeur");
        assertThat(savedRessource.getNature()).isEqualTo(NatureRessource.INTERNE);
    }

    @Test
    void getRessourceByIdRejectsUnknownResource() {
        when(ressourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ressourceService.getRessourceById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ressource introuvable");
    }

    @Test
    void updateRessourceUpdatesExistingResource() {
        Ressource ressource = ressource(12L, "Ancien nom", "Ancienne fonction", NatureRessource.INTERNE);
        UpdateRessourceRequest request = updateRequest();

        when(ressourceRepository.findById(12L)).thenReturn(Optional.of(ressource));
        when(ressourceRepository.save(ressource)).thenReturn(ressource);

        RessourceResponse response = ressourceService.updateRessource(12L, request);

        assertThat(response.getIdRessource()).isEqualTo(12L);
        assertThat(response.getNom()).isEqualTo("Nouveau nom");
        assertThat(response.getFonction()).isEqualTo("Chef de projet");
        assertThat(response.getNature()).isEqualTo("PRESTATAIRE");

        verify(ressourceRepository).save(ressource);
    }

    @Test
    void deleteRessourceDeletesFreeResource() {
        Ressource ressource = ressource(7L, "Nom", "Fonction", NatureRessource.INTERNE);

        when(ressourceRepository.findById(7L)).thenReturn(Optional.of(ressource));
        when(affectationRessourceRepository.existsByRessource_IdRessource(7L)).thenReturn(false);

        ressourceService.deleteRessource(7L);

        verify(ressourceRepository).delete(ressource);
    }

    @Test
    void deleteRessourceRejectsAssignedResource() {
        Ressource ressource = ressource(7L, "Nom", "Fonction", NatureRessource.INTERNE);

        when(ressourceRepository.findById(7L)).thenReturn(Optional.of(ressource));
        when(affectationRessourceRepository.existsByRessource_IdRessource(7L)).thenReturn(true);

        assertThatThrownBy(() -> ressourceService.deleteRessource(7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ressource affectee");

        verify(ressourceRepository, never()).delete(any(Ressource.class));
    }

    @Test
    void searchRessourcesUsesTextAndNatureFilters() {
        Ressource ressource = ressource(3L, "Salma Alami", "Analyste", NatureRessource.PRESTATAIRE);

        when(ressourceRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(ressource));

        List<RessourceResponse> responses = ressourceService.searchRessources("  ana  ", NatureRessource.PRESTATAIRE);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getIdRessource()).isEqualTo(3L);
        assertThat(responses.get(0).getNom()).isEqualTo("Salma Alami");
        assertThat(responses.get(0).getNature()).isEqualTo("PRESTATAIRE");

        ArgumentCaptor<Specification<Ressource>> specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(ressourceRepository).findAll(specificationCaptor.capture(), sortCaptor.capture());

        assertThat(specificationCaptor.getValue()).isNotNull();
        assertThat(sortCaptor.getValue()).isEqualTo(Sort.by(Sort.Direction.DESC, "idRessource"));
    }

    private CreateRessourceRequest createRequest() {
        CreateRessourceRequest request = new CreateRessourceRequest();
        request.setNom(" Karim Bennani ");
        request.setFonction(" Developpeur ");
        request.setNature(NatureRessource.INTERNE);
        return request;
    }

    private UpdateRessourceRequest updateRequest() {
        UpdateRessourceRequest request = new UpdateRessourceRequest();
        request.setNom(" Nouveau nom ");
        request.setFonction(" Chef de projet ");
        request.setNature(NatureRessource.PRESTATAIRE);
        return request;
    }

    private Ressource ressource(Long idRessource, String nom, String fonction, NatureRessource nature) {
        Ressource ressource = new Ressource();
        ressource.setIdRessource(idRessource);
        ressource.setNom(nom);
        ressource.setFonction(fonction);
        ressource.setNature(nature);
        return ressource;
    }
}
