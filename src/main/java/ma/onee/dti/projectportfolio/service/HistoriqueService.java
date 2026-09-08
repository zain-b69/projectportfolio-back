package ma.onee.dti.projectportfolio.service;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ma.onee.dti.projectportfolio.dto.HistoriqueModificationResponse;
import ma.onee.dti.projectportfolio.dto.HistoriquePageResponse;
import ma.onee.dti.projectportfolio.dto.HistoriqueSearchCriteria;
import ma.onee.dti.projectportfolio.entity.HistoriqueModification;
import ma.onee.dti.projectportfolio.entity.Projet;
import ma.onee.dti.projectportfolio.entity.Utilisateur;
import ma.onee.dti.projectportfolio.enums.TypeAction;
import ma.onee.dti.projectportfolio.repository.HistoriqueModificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoriqueService {

    private static final int MAX_PAGE_SIZE = 100;

    private final HistoriqueModificationRepository historiqueModificationRepository;

    public HistoriqueService(HistoriqueModificationRepository historiqueModificationRepository) {
        this.historiqueModificationRepository = historiqueModificationRepository;
    }

    /**
     * Records an audit-trail entry. Called from within existing service transactions.
     * {@code projet} may be null (e.g. connection events or after a project deletion).
     */
    public void record(Projet projet, Utilisateur utilisateur, TypeAction typeAction, String description) {
        HistoriqueModification historiqueModification = new HistoriqueModification();
        historiqueModification.setDateModification(LocalDateTime.now());
        historiqueModification.setUtilisateur(utilisateur);
        historiqueModification.setProjet(projet);
        historiqueModification.setTypeAction(typeAction);
        historiqueModification.setDescription(description);

        historiqueModificationRepository.save(historiqueModification);
    }

    @Transactional(readOnly = true)
    public HistoriquePageResponse search(HistoriqueSearchCriteria criteria, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "dateModification")
                        .and(Sort.by(Sort.Direction.DESC, "idHistoriqueModification"))
        );

        Page<HistoriqueModification> result =
                historiqueModificationRepository.findAll(buildSpecification(criteria), pageable);

        return HistoriquePageResponse.builder()
                .content(result.getContent().stream().map(this::mapToResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<HistoriqueModificationResponse> getByProjet(Long idProjet) {
        return historiqueModificationRepository
                .findByProjet_IdProjetOrderByDateModificationDescIdHistoriqueModificationDesc(idProjet)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Specification<HistoriqueModification> buildSpecification(HistoriqueSearchCriteria criteria) {
        if (criteria == null) {
            return null;
        }

        List<Specification<HistoriqueModification>> specifications = new ArrayList<>();

        if (criteria.getTypeAction() != null) {
            specifications.add((root, query, cb) ->
                    cb.equal(root.get("typeAction"), criteria.getTypeAction()));
        }

        if (criteria.getProjetId() != null) {
            specifications.add((root, query, cb) ->
                    cb.equal(root.get("projet").get("idProjet"), criteria.getProjetId()));
        }

        if (criteria.getUtilisateurId() != null) {
            specifications.add((root, query, cb) ->
                    cb.equal(root.get("utilisateur").get("idUtilisateur"), criteria.getUtilisateurId()));
        }

        if (criteria.getDateFrom() != null) {
            LocalDateTime from = criteria.getDateFrom().atStartOfDay();
            specifications.add((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("dateModification"), from));
        }

        if (criteria.getDateTo() != null) {
            LocalDateTime toExclusive = criteria.getDateTo().plusDays(1).atStartOfDay();
            specifications.add((root, query, cb) ->
                    cb.lessThan(root.get("dateModification"), toExclusive));
        }

        if (hasText(criteria.getSearch())) {
            String pattern = "%" + criteria.getSearch().trim().toLowerCase() + "%";
            specifications.add((root, query, cb) -> {
                Join<HistoriqueModification, Utilisateur> utilisateur = root.join("utilisateur", JoinType.LEFT);
                Expression<String> fullName = cb.concat(
                        cb.concat(cb.coalesce(utilisateur.get("prenom"), ""), " "),
                        cb.coalesce(utilisateur.get("nom"), "")
                );

                return cb.or(
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(cb.coalesce(utilisateur.get("nom"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(utilisateur.get("prenom"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(utilisateur.get("email"), "")), pattern),
                        cb.like(cb.lower(fullName), pattern)
                );
            });
        }

        return specifications.stream()
                .reduce(Specification::and)
                .orElse(null);
    }

    private HistoriqueModificationResponse mapToResponse(HistoriqueModification historique) {
        Utilisateur utilisateur = historique.getUtilisateur();
        Projet projet = historique.getProjet();

        return HistoriqueModificationResponse.builder()
                .idHistoriqueModification(historique.getIdHistoriqueModification())
                .dateModification(historique.getDateModification())
                .typeAction(historique.getTypeAction() != null ? historique.getTypeAction().name() : null)
                .description(historique.getDescription())
                .idUtilisateur(utilisateur != null ? utilisateur.getIdUtilisateur() : null)
                .nomUtilisateur(utilisateur != null ? utilisateur.getNom() : null)
                .prenomUtilisateur(utilisateur != null ? utilisateur.getPrenom() : null)
                .emailUtilisateur(utilisateur != null ? utilisateur.getEmail() : null)
                .nomCompletUtilisateur(formatFullName(utilisateur))
                .idProjet(projet != null ? projet.getIdProjet() : null)
                .codeProjet(projet != null ? projet.getCode() : null)
                .intituleProjet(projet != null ? projet.getIntitule() : null)
                .build();
    }

    private String formatFullName(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }

        String prenom = utilisateur.getPrenom() != null ? utilisateur.getPrenom().trim() : "";
        String nom = utilisateur.getNom() != null ? utilisateur.getNom().trim() : "";
        String fullName = (prenom + " " + nom).trim();

        if (!fullName.isBlank()) {
            return fullName;
        }

        return utilisateur.getEmail();
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
