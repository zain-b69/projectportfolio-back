package ma.onee.dti.projectportfolio.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import ma.onee.dti.projectportfolio.dto.PieceJointeDownloadResponse;
import ma.onee.dti.projectportfolio.dto.PieceJointeResponse;
import ma.onee.dti.projectportfolio.entity.PieceJointe;
import ma.onee.dti.projectportfolio.entity.Projet;
import ma.onee.dti.projectportfolio.entity.Utilisateur;
import ma.onee.dti.projectportfolio.enums.RoleLibelle;
import ma.onee.dti.projectportfolio.enums.TypeAction;
import ma.onee.dti.projectportfolio.exception.BusinessRuleException;
import ma.onee.dti.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dti.projectportfolio.repository.PieceJointeRepository;
import ma.onee.dti.projectportfolio.repository.ProjetRepository;
import ma.onee.dti.projectportfolio.repository.UtilisateurRepository;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PieceJointeService {

    private final PieceJointeRepository pieceJointeRepository;
    private final ProjetRepository projetRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final HistoriqueService historiqueService;
    private final FileStorageService fileStorageService;

    public PieceJointeService(
            PieceJointeRepository pieceJointeRepository,
            ProjetRepository projetRepository,
            UtilisateurRepository utilisateurRepository,
            HistoriqueService historiqueService,
            FileStorageService fileStorageService
    ) {
        this.pieceJointeRepository = pieceJointeRepository;
        this.projetRepository = projetRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.historiqueService = historiqueService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public PieceJointeResponse upload(Long idProjet, MultipartFile file, String currentUserEmail) throws IOException {
        Projet projet = findProjetById(idProjet);
        Utilisateur currentUser = findCurrentUser(currentUserEmail);
        assertResponsibleProjectManager(projet, currentUser);
        validateFile(file);

        String cheminFichier = fileStorageService.save(idProjet, file);

        PieceJointe pieceJointe = new PieceJointe();
        pieceJointe.setNomFichier(normalizeOriginalFilename(file.getOriginalFilename()));
        pieceJointe.setCheminFichier(cheminFichier);
        pieceJointe.setDateAjout(LocalDateTime.now());
        pieceJointe.setProjet(projet);

        PieceJointe savedPieceJointe = pieceJointeRepository.save(pieceJointe);
        historiqueService.record(
                projet,
                currentUser,
                TypeAction.AJOUT_PIECE_JOINTE,
                "Ajout de la piece jointe " + savedPieceJointe.getNomFichier() + " au projet " + projet.getCode()
        );

        return mapPieceJointe(savedPieceJointe);
    }

    @Transactional(readOnly = true)
    public List<PieceJointeResponse> listByProjet(Long idProjet) {
        findProjetById(idProjet);

        return pieceJointeRepository.findByProjet_IdProjet(idProjet).stream()
                .map(this::mapPieceJointe)
                .toList();
    }

    @Transactional(readOnly = true)
    public PieceJointeDownloadResponse download(Long idPieceJointe) {
        PieceJointe pieceJointe = findPieceJointeById(idPieceJointe);
        Resource resource = fileStorageService.load(pieceJointe.getCheminFichier());

        return PieceJointeDownloadResponse.builder()
                .nomFichier(pieceJointe.getNomFichier())
                .resource(resource)
                .build();
    }

    @Transactional
    public void delete(Long idPieceJointe, String currentUserEmail) throws IOException {
        PieceJointe pieceJointe = findPieceJointeById(idPieceJointe);
        Projet projet = pieceJointe.getProjet();
        Utilisateur currentUser = findCurrentUser(currentUserEmail);
        assertResponsibleProjectManager(projet, currentUser);

        fileStorageService.delete(pieceJointe.getCheminFichier());
        pieceJointeRepository.delete(pieceJointe);
        historiqueService.record(
                projet,
                currentUser,
                TypeAction.SUPPRESSION,
                "Suppression de la piece jointe " + idPieceJointe + " du projet " + projet.getCode()
        );
    }

    private Projet findProjetById(Long idProjet) {
        return projetRepository.findById(idProjet)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
    }

    private PieceJointe findPieceJointeById(Long idPieceJointe) {
        return pieceJointeRepository.findById(idPieceJointe)
                .orElseThrow(() -> new ResourceNotFoundException("Piece jointe introuvable"));
    }

    private Utilisateur findCurrentUser(String email) {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifie introuvable"));
    }

    private void assertResponsibleProjectManager(Projet projet, Utilisateur utilisateur) {
        if (utilisateur.getRole() == null
                || utilisateur.getRole().getLibelle() != RoleLibelle.ROLE_RESPONSABLE_PROJET) {
            throw new AccessDeniedException("Seul un responsable projet peut gerer les pieces jointes");
        }

        if (projet.getUtilisateur() == null
                || !Objects.equals(projet.getUtilisateur().getIdUtilisateur(), utilisateur.getIdUtilisateur())) {
            throw new AccessDeniedException("Le responsable projet ne peut gerer que les pieces jointes de ses propres projets");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Le fichier est obligatoire");
        }

        if (!hasText(file.getOriginalFilename())) {
            throw new BusinessRuleException("Le nom du fichier est obligatoire");
        }
    }

    private PieceJointeResponse mapPieceJointe(PieceJointe pieceJointe) {
        Projet projet = pieceJointe.getProjet();

        return PieceJointeResponse.builder()
                .idPieceJointe(pieceJointe.getIdPieceJointe())
                .nomFichier(pieceJointe.getNomFichier())
                .dateAjout(pieceJointe.getDateAjout())
                .idProjet(projet != null ? projet.getIdProjet() : null)
                .build();
    }

    private String normalizeOriginalFilename(String originalFilename) {
        return originalFilename.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
