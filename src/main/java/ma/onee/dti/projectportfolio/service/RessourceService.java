package ma.onee.dti.projectportfolio.service;

import java.util.ArrayList;
import java.util.List;
import ma.onee.dti.projectportfolio.dto.CreateRessourceRequest;
import ma.onee.dti.projectportfolio.dto.RessourceResponse;
import ma.onee.dti.projectportfolio.dto.UpdateRessourceRequest;
import ma.onee.dti.projectportfolio.entity.Ressource;
import ma.onee.dti.projectportfolio.enums.NatureRessource;
import ma.onee.dti.projectportfolio.exception.BusinessRuleException;
import ma.onee.dti.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dti.projectportfolio.repository.AffectationRessourceRepository;
import ma.onee.dti.projectportfolio.repository.RessourceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RessourceService {

    private final RessourceRepository ressourceRepository;
    private final AffectationRessourceRepository affectationRessourceRepository;

    public RessourceService(
            RessourceRepository ressourceRepository,
            AffectationRessourceRepository affectationRessourceRepository
    ) {
        this.ressourceRepository = ressourceRepository;
        this.affectationRessourceRepository = affectationRessourceRepository;
    }

    @Transactional(readOnly = true)
    public List<RessourceResponse> getAllRessources() {
        return ressourceRepository.findAll(Sort.by(Sort.Direction.DESC, "idRessource")).stream()
                .map(this::mapRessource)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RessourceResponse> searchRessources(String texte, NatureRessource nature) {
        return ressourceRepository.findAll(
                        buildSpecification(texte, nature),
                        Sort.by(Sort.Direction.DESC, "idRessource")
                ).stream()
                .map(this::mapRessource)
                .toList();
    }

    @Transactional(readOnly = true)
    public RessourceResponse getRessourceById(Long ressourceId) {
        return mapRessource(findRessourceById(ressourceId));
    }

    @Transactional
    public RessourceResponse createRessource(CreateRessourceRequest request) {
        Ressource ressource = new Ressource();
        applyRequestToRessource(ressource, request);

        return mapRessource(ressourceRepository.save(ressource));
    }

    @Transactional
    public RessourceResponse updateRessource(Long ressourceId, UpdateRessourceRequest request) {
        Ressource ressource = findRessourceById(ressourceId);
        applyRequestToRessource(ressource, request);

        return mapRessource(ressourceRepository.save(ressource));
    }

    @Transactional
    public void deleteRessource(Long ressourceId) {
        Ressource ressource = findRessourceById(ressourceId);

        if (affectationRessourceRepository.existsByRessource_IdRessource(ressourceId)) {
            throw new BusinessRuleException("Suppression impossible: ressource affectee a un projet");
        }

        ressourceRepository.delete(ressource);
    }

    private Specification<Ressource> buildSpecification(String texte, NatureRessource nature) {
        List<Specification<Ressource>> specifications = new ArrayList<>();

        if (hasText(texte)) {
            specifications.add((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("nom")), contains(texte)),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("fonction")), contains(texte))
            ));
        }

        if (nature != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("nature"), nature));
        }

        return specifications.stream()
                .reduce(Specification::and)
                .orElse(null);
    }

    private Ressource findRessourceById(Long ressourceId) {
        return ressourceRepository.findById(ressourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Ressource introuvable"));
    }

    private void applyRequestToRessource(Ressource ressource, CreateRessourceRequest request) {
        ressource.setNom(normalizeRequiredText(request.getNom()));
        ressource.setFonction(normalizeRequiredText(request.getFonction()));
        ressource.setNature(request.getNature());
    }

    private void applyRequestToRessource(Ressource ressource, UpdateRessourceRequest request) {
        ressource.setNom(normalizeRequiredText(request.getNom()));
        ressource.setFonction(normalizeRequiredText(request.getFonction()));
        ressource.setNature(request.getNature());
    }

    private RessourceResponse mapRessource(Ressource ressource) {
        return RessourceResponse.builder()
                .idRessource(ressource.getIdRessource())
                .nom(ressource.getNom())
                .fonction(ressource.getFonction())
                .nature(ressource.getNature() != null ? ressource.getNature().name() : null)
                .build();
    }

    private String normalizeRequiredText(String value) {
        return value.trim();
    }

    private String contains(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
