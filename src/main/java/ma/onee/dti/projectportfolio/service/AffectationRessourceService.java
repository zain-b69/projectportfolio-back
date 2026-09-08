package ma.onee.dti.projectportfolio.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import ma.onee.dti.projectportfolio.dto.AffectationRessourceResponse;
import ma.onee.dti.projectportfolio.dto.CreateAffectationRessourceRequest;
import ma.onee.dti.projectportfolio.dto.UpdateAffectationRessourceRequest;
import ma.onee.dti.projectportfolio.entity.AffectationRessource;
import ma.onee.dti.projectportfolio.entity.Projet;
import ma.onee.dti.projectportfolio.entity.Ressource;
import ma.onee.dti.projectportfolio.entity.Utilisateur;
import ma.onee.dti.projectportfolio.enums.NatureIntervention;
import ma.onee.dti.projectportfolio.enums.RoleLibelle;
import ma.onee.dti.projectportfolio.enums.TypeAction;
import ma.onee.dti.projectportfolio.exception.BusinessRuleException;
import ma.onee.dti.projectportfolio.exception.DuplicateResourceException;
import ma.onee.dti.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dti.projectportfolio.repository.AffectationRessourceRepository;
import ma.onee.dti.projectportfolio.repository.ProjetRepository;
import ma.onee.dti.projectportfolio.repository.RessourceRepository;
import ma.onee.dti.projectportfolio.repository.UtilisateurRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AffectationRessourceService {

    private final AffectationRessourceRepository affectationRessourceRepository;
    private final ProjetRepository projetRepository;
    private final RessourceRepository ressourceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final HistoriqueService historiqueService;

    public AffectationRessourceService(
            AffectationRessourceRepository affectationRessourceRepository,
            ProjetRepository projetRepository,
            RessourceRepository ressourceRepository,
            UtilisateurRepository utilisateurRepository,
            HistoriqueService historiqueService
    ) {
        this.affectationRessourceRepository = affectationRessourceRepository;
        this.projetRepository = projetRepository;
        this.ressourceRepository = ressourceRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.historiqueService = historiqueService;
    }

    @Transactional(readOnly = true)
    public List<AffectationRessourceResponse> getAffectationsByProjet(Long projectId) {
        findProjetById(projectId);

        return affectationRessourceRepository.findByProjet_IdProjet(projectId).stream()
                .map(this::mapAffectationRessource)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AffectationRessourceResponse> getAffectationsByRessource(Long ressourceId) {
        findRessourceById(ressourceId);

        return affectationRessourceRepository.findByRessource_IdRessource(ressourceId).stream()
                .map(this::mapAffectationRessource)
                .toList();
    }

    @Transactional
    public AffectationRessourceResponse createAffectation(
            CreateAffectationRessourceRequest request,
            String currentUserEmail
    ) {
        Projet projet = findProjetById(request.getIdProjet());
        Ressource ressource = findRessourceById(request.getIdRessource());
        Utilisateur currentUser = findCurrentUser(currentUserEmail);

        assertCanManageProjectAffectations(currentUser, projet);
        validateAffectationData(request.getNatureIntervention(), request.getChargeJH());

        if (affectationRessourceRepository.existsByProjet_IdProjetAndRessource_IdRessource(
                projet.getIdProjet(),
                ressource.getIdRessource()
        )) {
            throw new DuplicateResourceException("Ressource deja affectee au projet");
        }

        AffectationRessource affectationRessource = new AffectationRessource();
        affectationRessource.setProjet(projet);
        affectationRessource.setRessource(ressource);
        affectationRessource.setNatureIntervention(request.getNatureIntervention());
        affectationRessource.setChargeJH(request.getChargeJH());

        AffectationRessource savedAffectation = affectationRessourceRepository.save(affectationRessource);
        historiqueService.record(
                projet,
                currentUser,
                TypeAction.AFFECTATION_RESSOURCE,
                "Affectation de la ressource " + ressource.getNom() + " au projet " + projet.getCode()
        );

        return mapAffectationRessource(savedAffectation);
    }

    @Transactional
    public AffectationRessourceResponse updateAffectation(
            Long projectId,
            Long affectationId,
            UpdateAffectationRessourceRequest request,
            String currentUserEmail
    ) {
        Projet projet = findProjetById(projectId);
        Utilisateur currentUser = findCurrentUser(currentUserEmail);
        assertCanManageProjectAffectations(currentUser, projet);

        AffectationRessource affectationRessource = findAffectationByIdAndProjectId(affectationId, projectId);
        validateAffectationData(request.getNatureIntervention(), request.getChargeJH());

        affectationRessource.setNatureIntervention(request.getNatureIntervention());
        affectationRessource.setChargeJH(request.getChargeJH());

        AffectationRessource savedAffectation = affectationRessourceRepository.save(affectationRessource);
        historiqueService.record(
                projet,
                currentUser,
                TypeAction.MODIFICATION,
                "Modification de l'affectation " + affectationId + " du projet " + projet.getCode()
        );

        return mapAffectationRessource(savedAffectation);
    }

    @Transactional
    public void deleteAffectation(Long projectId, Long affectationId, String currentUserEmail) {
        Projet projet = findProjetById(projectId);
        Utilisateur currentUser = findCurrentUser(currentUserEmail);
        assertCanManageProjectAffectations(currentUser, projet);

        AffectationRessource affectationRessource = findAffectationByIdAndProjectId(affectationId, projectId);
        affectationRessourceRepository.delete(affectationRessource);
        historiqueService.record(
                projet,
                currentUser,
                TypeAction.SUPPRESSION,
                "Retrait de l'affectation " + affectationId + " du projet " + projet.getCode()
        );
    }

    private AffectationRessource findAffectationByIdAndProjectId(Long affectationId, Long projectId) {
        return affectationRessourceRepository.findByIdAffectationRessourceAndProjet_IdProjet(affectationId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Affectation introuvable"));
    }

    private Projet findProjetById(Long projectId) {
        return projetRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
    }

    private Ressource findRessourceById(Long ressourceId) {
        return ressourceRepository.findById(ressourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Ressource introuvable"));
    }

    private Utilisateur findCurrentUser(String email) {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifie introuvable"));
    }

    private void assertCanManageProjectAffectations(Utilisateur utilisateur, Projet projet) {
        if (utilisateur.getRole() == null || utilisateur.getRole().getLibelle() == null) {
            throw new AccessDeniedException("Utilisateur non autorise a gerer les affectations");
        }

        RoleLibelle role = utilisateur.getRole().getLibelle();

        if (role == RoleLibelle.ROLE_ADMIN) {
            return;
        }

        if (role == RoleLibelle.ROLE_RESPONSABLE_PROJET) {
            if (projet.getUtilisateur() != null
                    && Objects.equals(projet.getUtilisateur().getIdUtilisateur(), utilisateur.getIdUtilisateur())) {
                return;
            }
            throw new AccessDeniedException("Le responsable projet ne peut gerer que les affectations de ses propres projets");
        }

        throw new AccessDeniedException("Utilisateur non autorise a gerer les affectations");
    }

    private void validateAffectationData(NatureIntervention natureIntervention, BigDecimal chargeJH) {
        if (natureIntervention == null) {
            throw new BusinessRuleException("La nature d'intervention est obligatoire");
        }

        if (chargeJH == null || chargeJH.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("La charge JH doit etre strictement positive");
        }
    }

    private AffectationRessourceResponse mapAffectationRessource(AffectationRessource affectationRessource) {
        Projet projet = affectationRessource.getProjet();
        Ressource ressource = affectationRessource.getRessource();

        return AffectationRessourceResponse.builder()
                .idAffectationRessource(affectationRessource.getIdAffectationRessource())
                .natureIntervention(affectationRessource.getNatureIntervention() != null
                        ? affectationRessource.getNatureIntervention().name()
                        : null)
                .chargeJH(affectationRessource.getChargeJH())
                .idProjet(projet != null ? projet.getIdProjet() : null)
                .codeProjet(projet != null ? projet.getCode() : null)
                .intituleProjet(projet != null ? projet.getIntitule() : null)
                .idRessource(ressource != null ? ressource.getIdRessource() : null)
                .nomRessource(ressource != null ? ressource.getNom() : null)
                .fonctionRessource(ressource != null ? ressource.getFonction() : null)
                .natureRessource(ressource != null && ressource.getNature() != null
                        ? ressource.getNature().name()
                        : null)
                .build();
    }
}
