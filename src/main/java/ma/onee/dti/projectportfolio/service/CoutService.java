package ma.onee.dti.projectportfolio.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import ma.onee.dti.projectportfolio.dto.CoutResponse;
import ma.onee.dti.projectportfolio.dto.CreateCoutRequest;
import ma.onee.dti.projectportfolio.dto.UpdateCoutRequest;
import ma.onee.dti.projectportfolio.entity.Cout;
import ma.onee.dti.projectportfolio.entity.Projet;
import ma.onee.dti.projectportfolio.entity.Utilisateur;
import ma.onee.dti.projectportfolio.enums.RoleLibelle;
import ma.onee.dti.projectportfolio.enums.TypeAction;
import ma.onee.dti.projectportfolio.enums.TypeCout;
import ma.onee.dti.projectportfolio.exception.BusinessRuleException;
import ma.onee.dti.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dti.projectportfolio.repository.CoutRepository;
import ma.onee.dti.projectportfolio.repository.ProjetRepository;
import ma.onee.dti.projectportfolio.repository.UtilisateurRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoutService {

    private final CoutRepository coutRepository;
    private final ProjetRepository projetRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final HistoriqueService historiqueService;

    public CoutService(
            CoutRepository coutRepository,
            ProjetRepository projetRepository,
            UtilisateurRepository utilisateurRepository,
            HistoriqueService historiqueService
    ) {
        this.coutRepository = coutRepository;
        this.projetRepository = projetRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.historiqueService = historiqueService;
    }

    @Transactional(readOnly = true)
    public List<CoutResponse> getCoutsByProjet(Long idProjet) {
        findProjetById(idProjet);

        return coutRepository.findByProjet_IdProjet(idProjet).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CoutResponse getCoutById(Long idCout) {
        return mapToResponse(findCoutById(idCout));
    }

    @Transactional
    public CoutResponse createCout(Long idProjet, CreateCoutRequest request, String currentUserEmail) {
        Projet projet = findProjetById(idProjet);
        Utilisateur currentUser = findCurrentUser(currentUserEmail);
        assertResponsibleProjectManager(projet, currentUser);
        validateCoutData(request.getType(), request.getMontant(), request.getDateCout());

        Cout cout = new Cout();
        cout.setType(request.getType());
        cout.setMontant(request.getMontant());
        cout.setDateCout(request.getDateCout());
        cout.setProjet(projet);

        Cout savedCout = coutRepository.save(cout);
        historiqueService.record(
                projet,
                currentUser,
                TypeAction.AJOUT_COUT,
                "Ajout du cout " + savedCout.getIdCout() + " au projet " + projet.getCode()
        );

        return mapToResponse(savedCout);
    }

    @Transactional
    public CoutResponse updateCout(Long idCout, UpdateCoutRequest request, String currentUserEmail) {
        Cout cout = findCoutById(idCout);
        Projet projet = cout.getProjet();
        Utilisateur currentUser = findCurrentUser(currentUserEmail);
        assertResponsibleProjectManager(projet, currentUser);
        validateCoutData(request.getType(), request.getMontant(), request.getDateCout());

        cout.setType(request.getType());
        cout.setMontant(request.getMontant());
        cout.setDateCout(request.getDateCout());

        Cout savedCout = coutRepository.save(cout);
        historiqueService.record(
                projet,
                currentUser,
                TypeAction.MODIFICATION,
                "Modification du cout " + savedCout.getIdCout() + " du projet " + projet.getCode()
        );

        return mapToResponse(savedCout);
    }

    @Transactional
    public void deleteCout(Long idCout, String currentUserEmail) {
        Cout cout = findCoutById(idCout);
        Projet projet = cout.getProjet();
        Utilisateur currentUser = findCurrentUser(currentUserEmail);
        assertResponsibleProjectManager(projet, currentUser);

        coutRepository.delete(cout);
        historiqueService.record(
                projet,
                currentUser,
                TypeAction.SUPPRESSION,
                "Suppression du cout " + idCout + " du projet " + projet.getCode()
        );
    }

    private Projet findProjetById(Long idProjet) {
        return projetRepository.findById(idProjet)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
    }

    private Cout findCoutById(Long idCout) {
        return coutRepository.findById(idCout)
                .orElseThrow(() -> new ResourceNotFoundException("Cout introuvable"));
    }

    private Utilisateur findCurrentUser(String email) {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifie introuvable"));
    }

    private void assertResponsibleProjectManager(Projet projet, Utilisateur utilisateur) {
        if (utilisateur.getRole() == null
                || utilisateur.getRole().getLibelle() != RoleLibelle.ROLE_RESPONSABLE_PROJET) {
            throw new AccessDeniedException("Seul un responsable projet peut gerer les couts");
        }

        if (projet.getUtilisateur() == null
                || !Objects.equals(projet.getUtilisateur().getIdUtilisateur(), utilisateur.getIdUtilisateur())) {
            throw new AccessDeniedException("Le responsable projet ne peut gerer que les couts de ses propres projets");
        }
    }

    private void validateCoutData(TypeCout type, BigDecimal montant, LocalDate dateCout) {
        if (type == null) {
            throw new BusinessRuleException("Le type de cout est obligatoire");
        }

        if (montant == null) {
            throw new BusinessRuleException("Le montant est obligatoire");
        }

        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Le montant doit etre strictement superieur a zero");
        }

        if (montant.stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleException("Le montant doit contenir au maximum 2 decimales");
        }

        if (dateCout == null) {
            throw new BusinessRuleException("La date du cout est obligatoire");
        }
    }

    private CoutResponse mapToResponse(Cout cout) {
        Projet projet = cout.getProjet();

        return CoutResponse.builder()
                .idCout(cout.getIdCout())
                .type(cout.getType())
                .montant(cout.getMontant())
                .dateCout(cout.getDateCout())
                .idProjet(projet != null ? projet.getIdProjet() : null)
                .build();
    }
}
