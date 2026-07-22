package ma.onee.dsi.projectportfolio.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ma.onee.dsi.projectportfolio.dto.CreateProjetRequest;
import ma.onee.dsi.projectportfolio.dto.ProjetResponse;
import ma.onee.dsi.projectportfolio.dto.ProjetSearchCriteria;
import ma.onee.dsi.projectportfolio.dto.UpdateProjetRequest;
import ma.onee.dsi.projectportfolio.entity.HistoriqueModification;
import ma.onee.dsi.projectportfolio.entity.Projet;
import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import ma.onee.dsi.projectportfolio.enums.RoleLibelle;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;
import ma.onee.dsi.projectportfolio.enums.TypeAction;
import ma.onee.dsi.projectportfolio.exception.BusinessRuleException;
import ma.onee.dsi.projectportfolio.exception.DuplicateResourceException;
import ma.onee.dsi.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dsi.projectportfolio.repository.AffectationRessourceRepository;
import ma.onee.dsi.projectportfolio.repository.CoutRepository;
import ma.onee.dsi.projectportfolio.repository.HistoriqueModificationRepository;
import ma.onee.dsi.projectportfolio.repository.PieceJointeRepository;
import ma.onee.dsi.projectportfolio.repository.ProjetRepository;
import ma.onee.dsi.projectportfolio.repository.RisqueRepository;
import ma.onee.dsi.projectportfolio.repository.UtilisateurRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjetService {

    private final ProjetRepository projetRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final HistoriqueModificationRepository historiqueModificationRepository;
    private final CoutRepository coutRepository;
    private final RisqueRepository risqueRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final AffectationRessourceRepository affectationRessourceRepository;

    public ProjetService(
            ProjetRepository projetRepository,
            UtilisateurRepository utilisateurRepository,
            HistoriqueModificationRepository historiqueModificationRepository,
            CoutRepository coutRepository,
            RisqueRepository risqueRepository,
            PieceJointeRepository pieceJointeRepository,
            AffectationRessourceRepository affectationRessourceRepository
    ) {
        this.projetRepository = projetRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.historiqueModificationRepository = historiqueModificationRepository;
        this.coutRepository = coutRepository;
        this.risqueRepository = risqueRepository;
        this.pieceJointeRepository = pieceJointeRepository;
        this.affectationRessourceRepository = affectationRessourceRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjetResponse> searchProjets(ProjetSearchCriteria criteria) {
        validateSearchCriteria(criteria);

        return projetRepository.findAll(
                        buildSpecification(criteria),
                        Sort.by(Sort.Direction.DESC, "idProjet")
                ).stream()
                .map(this::mapProjet)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjetResponse getProjetById(Long projectId) {
        return mapProjet(findProjetById(projectId));
    }

    @Transactional
    public ProjetResponse createProjet(CreateProjetRequest request, String currentUserEmail) {
        Utilisateur currentUser = findCurrentUser(currentUserEmail);
        assertProjectManager(currentUser);

        String normalizedCode = normalizeRequiredText(request.getCode());
        if (projetRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new DuplicateResourceException("Un projet avec ce code existe deja");
        }

        validateProjectData(
                request.getDateDebutPrevue(),
                request.getDateFinPrevue(),
                request.getDateDebutReelle(),
                request.getDateFinReelle(),
                request.getStatut(),
                request.getBudgetPrevisionnel(),
                request.getPourcentageAvancement()
        );

        Projet projet = new Projet();
        applyRequestToProjet(projet, request, normalizedCode);
        projet.setUtilisateur(currentUser);

        Projet savedProjet = projetRepository.save(projet);
        recordHistory(savedProjet, currentUser, TypeAction.CREATION, "Creation du projet " + savedProjet.getCode());

        return mapProjet(savedProjet);
    }

    @Transactional
    public ProjetResponse updateProjet(Long projectId, UpdateProjetRequest request, String currentUserEmail) {
        Utilisateur currentUser = findCurrentUser(currentUserEmail);
        assertProjectManager(currentUser);

        Projet projet = findProjetById(projectId);
        assertResponsibleProjectManager(projet, currentUser);

        String normalizedCode = normalizeRequiredText(request.getCode());
        projetRepository.findByCodeIgnoreCase(normalizedCode).ifPresent(existingProjet -> {
            if (!existingProjet.getIdProjet().equals(projectId)) {
                throw new DuplicateResourceException("Un projet avec ce code existe deja");
            }
        });

        validateProjectData(
                request.getDateDebutPrevue(),
                request.getDateFinPrevue(),
                request.getDateDebutReelle(),
                request.getDateFinReelle(),
                request.getStatut(),
                request.getBudgetPrevisionnel(),
                request.getPourcentageAvancement()
        );

        applyRequestToProjet(projet, request, normalizedCode);

        Projet savedProjet = projetRepository.save(projet);
        recordHistory(savedProjet, currentUser, TypeAction.MODIFICATION, "Modification du projet " + savedProjet.getCode());

        return mapProjet(savedProjet);
    }

    @Transactional
    public void deleteProjet(Long projectId, String currentUserEmail) {
        Utilisateur currentUser = findCurrentUser(currentUserEmail);
        assertProjectManager(currentUser);

        Projet projet = findProjetById(projectId);
        assertResponsibleProjectManager(projet, currentUser);
        assertProjetCanBeDeleted(projectId);

        String projectCode = projet.getCode();
        historiqueModificationRepository.detachProjet(projectId);
        projetRepository.delete(projet);
        recordHistory(
                null,
                currentUser,
                TypeAction.SUPPRESSION,
                "Suppression du projet " + projectCode + " (id=" + projectId + ")"
        );
    }

    private Specification<Projet> buildSpecification(ProjetSearchCriteria criteria) {
        if (criteria == null) {
            return null;
        }

        List<Specification<Projet>> specifications = new ArrayList<>();

        if (hasText(criteria.getCode())) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), contains(criteria.getCode())));
        }

        if (hasText(criteria.getIntitule())) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("intitule")), contains(criteria.getIntitule())));
        }

        if (criteria.getStatut() != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("statut"), criteria.getStatut()));
        }

        if (criteria.getPriorite() != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("priorite"), criteria.getPriorite()));
        }

        if (criteria.getResponsableId() != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("utilisateur").get("idUtilisateur"), criteria.getResponsableId()));
        }

        if (hasText(criteria.getResponsableEmail())) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("utilisateur").get("email")),
                            contains(criteria.getResponsableEmail())
                    ));
        }

        if (criteria.getDateDebutPrevueFrom() != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("dateDebutPrevue"), criteria.getDateDebutPrevueFrom()));
        }

        if (criteria.getDateDebutPrevueTo() != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("dateDebutPrevue"), criteria.getDateDebutPrevueTo()));
        }

        if (criteria.getDateFinPrevueFrom() != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("dateFinPrevue"), criteria.getDateFinPrevueFrom()));
        }

        if (criteria.getDateFinPrevueTo() != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("dateFinPrevue"), criteria.getDateFinPrevueTo()));
        }

        if (criteria.getBudgetMin() != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("budgetPrevisionnel"), criteria.getBudgetMin()));
        }

        if (criteria.getBudgetMax() != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("budgetPrevisionnel"), criteria.getBudgetMax()));
        }

        if (criteria.getAvancementMin() != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("pourcentageAvancement"), criteria.getAvancementMin()));
        }

        if (criteria.getAvancementMax() != null) {
            specifications.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("pourcentageAvancement"), criteria.getAvancementMax()));
        }

        return specifications.stream()
                .reduce(Specification::and)
                .orElse(null);
    }

    private void validateSearchCriteria(ProjetSearchCriteria criteria) {
        if (criteria == null) {
            return;
        }

        validateDateRange(
                criteria.getDateDebutPrevueFrom(),
                criteria.getDateDebutPrevueTo(),
                "La date de debut prevue minimum ne peut pas etre apres la date maximum"
        );
        validateDateRange(
                criteria.getDateFinPrevueFrom(),
                criteria.getDateFinPrevueTo(),
                "La date de fin prevue minimum ne peut pas etre apres la date maximum"
        );

        if (criteria.getBudgetMin() != null && criteria.getBudgetMin().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le budget minimum ne peut pas etre negatif");
        }

        if (criteria.getBudgetMax() != null && criteria.getBudgetMax().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le budget maximum ne peut pas etre negatif");
        }

        if (criteria.getBudgetMin() != null
                && criteria.getBudgetMax() != null
                && criteria.getBudgetMin().compareTo(criteria.getBudgetMax()) > 0) {
            throw new IllegalArgumentException("Le budget minimum ne peut pas etre superieur au budget maximum");
        }

        validateAvancement(criteria.getAvancementMin(), "L'avancement minimum");
        validateAvancement(criteria.getAvancementMax(), "L'avancement maximum");

        if (criteria.getAvancementMin() != null
                && criteria.getAvancementMax() != null
                && criteria.getAvancementMin() > criteria.getAvancementMax()) {
            throw new IllegalArgumentException("L'avancement minimum ne peut pas etre superieur a l'avancement maximum");
        }
    }

    private void validateProjectData(
            LocalDate dateDebutPrevue,
            LocalDate dateFinPrevue,
            LocalDate dateDebutReelle,
            LocalDate dateFinReelle,
            StatutProjet statut,
            BigDecimal budgetPrevisionnel,
            Integer pourcentageAvancement
    ) {
        if (dateDebutPrevue == null || dateFinPrevue == null) {
            throw new BusinessRuleException("Les dates prevues sont obligatoires");
        }

        if (dateFinPrevue.isBefore(dateDebutPrevue)) {
            throw new BusinessRuleException("La date de fin prevue ne peut pas etre avant la date de debut prevue");
        }

        if (dateDebutReelle != null && dateFinReelle != null && dateFinReelle.isBefore(dateDebutReelle)) {
            throw new BusinessRuleException("La date de fin reelle ne peut pas etre avant la date de debut reelle");
        }

        if (budgetPrevisionnel == null || budgetPrevisionnel.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Le budget ne peut pas etre negatif");
        }

        if (pourcentageAvancement == null || pourcentageAvancement < 0 || pourcentageAvancement > 100) {
            throw new BusinessRuleException("Le pourcentage d'avancement doit etre compris entre 0 et 100");
        }

        validateStatusConsistency(statut, pourcentageAvancement, dateDebutReelle, dateFinReelle);
    }

    private void validateStatusConsistency(
            StatutProjet statut,
            Integer pourcentageAvancement,
            LocalDate dateDebutReelle,
            LocalDate dateFinReelle
    ) {
        if (statut == null) {
            throw new BusinessRuleException("Le statut est obligatoire");
        }

        if (statut == StatutProjet.PLANIFIE) {
            if (pourcentageAvancement != 0) {
                throw new BusinessRuleException("Un projet planifie doit avoir un avancement egal a 0");
            }
            if (dateDebutReelle != null || dateFinReelle != null) {
                throw new BusinessRuleException("Un projet planifie ne peut pas avoir de dates reelles");
            }
            return;
        }

        if (dateFinReelle != null && dateDebutReelle == null) {
            throw new BusinessRuleException("La date de debut reelle est obligatoire si la date de fin reelle est renseignee");
        }

        if (statut == StatutProjet.TERMINE) {
            if (pourcentageAvancement != 100) {
                throw new BusinessRuleException("Un projet termine doit avoir un avancement egal a 100");
            }
            if (dateDebutReelle == null || dateFinReelle == null) {
                throw new BusinessRuleException("Un projet termine doit avoir une date de debut reelle et une date de fin reelle");
            }
            return;
        }

        if (pourcentageAvancement == 100) {
            throw new BusinessRuleException("Seul un projet termine peut avoir un avancement egal a 100");
        }

        if (dateFinReelle != null) {
            throw new BusinessRuleException("Seul un projet termine peut avoir une date de fin reelle");
        }

        if (statut == StatutProjet.EN_COURS
                || statut == StatutProjet.EN_RETARD
                || statut == StatutProjet.SUSPENDU) {
            if (dateDebutReelle == null) {
                throw new BusinessRuleException("Un projet actif ou suspendu doit avoir une date de debut reelle");
            }
        }
    }

    private void assertProjetCanBeDeleted(Long projectId) {
        if (coutRepository.countByProjet_IdProjet(projectId) > 0
                || risqueRepository.countByProjet_IdProjet(projectId) > 0
                || pieceJointeRepository.countByProjet_IdProjet(projectId) > 0
                || affectationRessourceRepository.countByProjet_IdProjet(projectId) > 0) {
            throw new BusinessRuleException("Suppression impossible: projet lie a des couts, risques, pieces jointes ou affectations");
        }
    }

    private void validateDateRange(LocalDate start, LocalDate end, String message) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateAvancement(Integer value, String label) {
        if (value != null && (value < 0 || value > 100)) {
            throw new IllegalArgumentException(label + " doit etre compris entre 0 et 100");
        }
    }

    private void applyRequestToProjet(Projet projet, CreateProjetRequest request, String normalizedCode) {
        projet.setCode(normalizedCode);
        projet.setIntitule(normalizeRequiredText(request.getIntitule()));
        projet.setDescriptif(normalizeOptionalText(request.getDescriptif()));
        projet.setDateDebutPrevue(request.getDateDebutPrevue());
        projet.setDateFinPrevue(request.getDateFinPrevue());
        projet.setDateDebutReelle(request.getDateDebutReelle());
        projet.setDateFinReelle(request.getDateFinReelle());
        projet.setStatut(request.getStatut());
        projet.setBudgetPrevisionnel(request.getBudgetPrevisionnel());
        projet.setPriorite(request.getPriorite());
        projet.setPourcentageAvancement(request.getPourcentageAvancement());
    }

    private void applyRequestToProjet(Projet projet, UpdateProjetRequest request, String normalizedCode) {
        projet.setCode(normalizedCode);
        projet.setIntitule(normalizeRequiredText(request.getIntitule()));
        projet.setDescriptif(normalizeOptionalText(request.getDescriptif()));
        projet.setDateDebutPrevue(request.getDateDebutPrevue());
        projet.setDateFinPrevue(request.getDateFinPrevue());
        projet.setDateDebutReelle(request.getDateDebutReelle());
        projet.setDateFinReelle(request.getDateFinReelle());
        projet.setStatut(request.getStatut());
        projet.setBudgetPrevisionnel(request.getBudgetPrevisionnel());
        projet.setPriorite(request.getPriorite());
        projet.setPourcentageAvancement(request.getPourcentageAvancement());
    }

    private void recordHistory(Projet projet, Utilisateur utilisateur, TypeAction typeAction, String description) {
        HistoriqueModification historiqueModification = new HistoriqueModification();
        historiqueModification.setDateModification(LocalDate.now());
        historiqueModification.setUtilisateur(utilisateur);
        historiqueModification.setProjet(projet);
        historiqueModification.setTypeAction(typeAction);
        historiqueModification.setDescription(description);

        historiqueModificationRepository.save(historiqueModification);
    }

    private Projet findProjetById(Long projectId) {
        return projetRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
    }

    private Utilisateur findCurrentUser(String email) {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifie introuvable"));
    }

    private void assertProjectManager(Utilisateur utilisateur) {
        if (utilisateur.getRole() == null
                || utilisateur.getRole().getLibelle() != RoleLibelle.ROLE_RESPONSABLE_PROJET) {
            throw new AccessDeniedException("Seul un responsable projet peut effectuer cette operation");
        }
    }

    private void assertResponsibleProjectManager(Projet projet, Utilisateur utilisateur) {
        if (projet.getUtilisateur() == null
                || !Objects.equals(projet.getUtilisateur().getIdUtilisateur(), utilisateur.getIdUtilisateur())) {
            throw new AccessDeniedException("Le responsable projet ne peut modifier que ses propres projets");
        }
    }

    private ProjetResponse mapProjet(Projet projet) {
        Utilisateur responsable = projet.getUtilisateur();

        return ProjetResponse.builder()
                .idProjet(projet.getIdProjet())
                .code(projet.getCode())
                .intitule(projet.getIntitule())
                .descriptif(projet.getDescriptif())
                .dateDebutPrevue(projet.getDateDebutPrevue())
                .dateFinPrevue(projet.getDateFinPrevue())
                .dateDebutReelle(projet.getDateDebutReelle())
                .dateFinReelle(projet.getDateFinReelle())
                .statut(projet.getStatut() != null ? projet.getStatut().name() : null)
                .budgetPrevisionnel(projet.getBudgetPrevisionnel())
                .priorite(projet.getPriorite() != null ? projet.getPriorite().name() : null)
                .pourcentageAvancement(projet.getPourcentageAvancement())
                .idResponsable(responsable != null ? responsable.getIdUtilisateur() : null)
                .nomResponsable(responsable != null ? responsable.getNom() : null)
                .prenomResponsable(responsable != null ? responsable.getPrenom() : null)
                .emailResponsable(responsable != null ? responsable.getEmail() : null)
                .build();
    }

    private String normalizeRequiredText(String value) {
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String contains(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
