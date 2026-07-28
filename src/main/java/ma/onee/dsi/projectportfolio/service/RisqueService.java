package ma.onee.dsi.projectportfolio.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import ma.onee.dsi.projectportfolio.dto.CreateRisqueRequest;
import ma.onee.dsi.projectportfolio.dto.RisqueResponse;
import ma.onee.dsi.projectportfolio.dto.UpdateRisqueRequest;
import ma.onee.dsi.projectportfolio.entity.HistoriqueModification;
import ma.onee.dsi.projectportfolio.entity.Projet;
import ma.onee.dsi.projectportfolio.entity.Risque;
import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import ma.onee.dsi.projectportfolio.enums.RoleLibelle;
import ma.onee.dsi.projectportfolio.enums.TypeAction;
import ma.onee.dsi.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dsi.projectportfolio.repository.HistoriqueModificationRepository;
import ma.onee.dsi.projectportfolio.repository.ProjetRepository;
import ma.onee.dsi.projectportfolio.repository.RisqueRepository;
import ma.onee.dsi.projectportfolio.repository.UtilisateurRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RisqueService {

    private final RisqueRepository risqueRepository;
    private final ProjetRepository projetRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final HistoriqueModificationRepository historiqueModificationRepository;

    public RisqueService(
            RisqueRepository risqueRepository,
            ProjetRepository projetRepository,
            UtilisateurRepository utilisateurRepository,
            HistoriqueModificationRepository historiqueModificationRepository
    ) {
        this.risqueRepository = risqueRepository;
        this.projetRepository = projetRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.historiqueModificationRepository = historiqueModificationRepository;
    }

    @Transactional(readOnly = true)
    public List<RisqueResponse> getRisquesByProjet(Long idProjet) {
        findProjetById(idProjet);

        return risqueRepository.findByProjet_IdProjet(idProjet).stream()
                .map(this::mapRisque)
                .toList();
    }

    @Transactional(readOnly = true)
    public RisqueResponse getRisqueById(Long idRisque) {
        return mapRisque(findRisqueById(idRisque));
    }

    @Transactional
    public RisqueResponse createRisque(Long idProjet, CreateRisqueRequest request, String currentUserEmail) {
        Projet projet = findProjetById(idProjet);
        Utilisateur currentUser = findCurrentUser(currentUserEmail);
        assertResponsibleProjectManager(projet, currentUser);

        Risque risque = new Risque();
        risque.setDescription(normalizeRequiredText(request.getDescription()));
        risque.setNiveauCriticite(request.getNiveauCriticite());
        risque.setProjet(projet);

        Risque savedRisque = risqueRepository.save(risque);
        recordHistory(
                projet,
                currentUser,
                TypeAction.AJOUT_RISQUE,
                "Ajout du risque " + savedRisque.getIdRisque() + " au projet " + projet.getCode()
        );

        return mapRisque(savedRisque);
    }

    @Transactional
    public RisqueResponse updateRisque(Long idRisque, UpdateRisqueRequest request, String currentUserEmail) {
        Risque risque = findRisqueById(idRisque);
        Projet projet = risque.getProjet();
        Utilisateur currentUser = findCurrentUser(currentUserEmail);
        assertResponsibleProjectManager(projet, currentUser);

        risque.setDescription(normalizeRequiredText(request.getDescription()));
        risque.setNiveauCriticite(request.getNiveauCriticite());

        Risque savedRisque = risqueRepository.save(risque);
        recordHistory(
                projet,
                currentUser,
                TypeAction.MODIFICATION,
                "Modification du risque " + savedRisque.getIdRisque() + " du projet " + projet.getCode()
        );

        return mapRisque(savedRisque);
    }

    @Transactional
    public void deleteRisque(Long idRisque, String currentUserEmail) {
        Risque risque = findRisqueById(idRisque);
        Projet projet = risque.getProjet();
        Utilisateur currentUser = findCurrentUser(currentUserEmail);
        assertResponsibleProjectManager(projet, currentUser);

        risqueRepository.delete(risque);
        recordHistory(
                projet,
                currentUser,
                TypeAction.SUPPRESSION,
                "Suppression du risque " + idRisque + " du projet " + projet.getCode()
        );
    }

    private Projet findProjetById(Long idProjet) {
        return projetRepository.findById(idProjet)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
    }

    private Risque findRisqueById(Long idRisque) {
        return risqueRepository.findById(idRisque)
                .orElseThrow(() -> new ResourceNotFoundException("Risque introuvable"));
    }

    private Utilisateur findCurrentUser(String email) {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifie introuvable"));
    }

    private void assertResponsibleProjectManager(Projet projet, Utilisateur utilisateur) {
        if (utilisateur.getRole() == null
                || utilisateur.getRole().getLibelle() != RoleLibelle.ROLE_RESPONSABLE_PROJET) {
            throw new AccessDeniedException("Seul un responsable projet peut gerer les risques");
        }

        if (projet.getUtilisateur() == null
                || !Objects.equals(projet.getUtilisateur().getIdUtilisateur(), utilisateur.getIdUtilisateur())) {
            throw new AccessDeniedException("Le responsable projet ne peut gerer que les risques de ses propres projets");
        }
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

    private RisqueResponse mapRisque(Risque risque) {
        Projet projet = risque.getProjet();

        return RisqueResponse.builder()
                .idRisque(risque.getIdRisque())
                .description(risque.getDescription())
                .niveauCriticite(risque.getNiveauCriticite())
                .idProjet(projet != null ? projet.getIdProjet() : null)
                .build();
    }

    private String normalizeRequiredText(String value) {
        return value.trim();
    }
}
