package ma.onee.dti.projectportfolio.service;

import java.util.List;
import ma.onee.dti.projectportfolio.dto.CreateUtilisateurRequest;
import ma.onee.dti.projectportfolio.dto.UpdateUtilisateurRequest;
import ma.onee.dti.projectportfolio.dto.UtilisateurResponse;
import ma.onee.dti.projectportfolio.entity.Role;
import ma.onee.dti.projectportfolio.entity.Utilisateur;
import ma.onee.dti.projectportfolio.enums.RoleLibelle;
import ma.onee.dti.projectportfolio.exception.BusinessRuleException;
import ma.onee.dti.projectportfolio.exception.DuplicateResourceException;
import ma.onee.dti.projectportfolio.exception.ResourceNotFoundException;
import ma.onee.dti.projectportfolio.repository.HistoriqueModificationRepository;
import ma.onee.dti.projectportfolio.repository.ProjetRepository;
import ma.onee.dti.projectportfolio.repository.RoleRepository;
import ma.onee.dti.projectportfolio.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final ProjetRepository projetRepository;
    private final HistoriqueModificationRepository historiqueModificationRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurService(
            UtilisateurRepository utilisateurRepository,
            RoleRepository roleRepository,
            ProjetRepository projetRepository,
            HistoriqueModificationRepository historiqueModificationRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.projetRepository = projetRepository;
        this.historiqueModificationRepository = historiqueModificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UtilisateurResponse> getAllUtilisateurs() {
        return utilisateurRepository.findAll().stream()
                .map(this::mapUtilisateur)
                .toList();
    }

    @Transactional(readOnly = true)
    public UtilisateurResponse getUtilisateurById(Long userId) {
        return mapUtilisateur(findUtilisateurById(userId));
    }

    @Transactional
    public UtilisateurResponse createUtilisateur(CreateUtilisateurRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (utilisateurRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Un utilisateur avec cet email existe deja");
        }

        Role role = findRoleByLibelle(request.getRole());

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(request.getNom().trim());
        utilisateur.setPrenom(request.getPrenom().trim());
        utilisateur.setEmail(normalizedEmail);
        utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        utilisateur.setRole(role);

        return mapUtilisateur(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public UtilisateurResponse updateUtilisateur(Long userId, UpdateUtilisateurRequest request) {
        Utilisateur utilisateur = findUtilisateurById(userId);
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        utilisateurRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(existingUser -> {
            if (!existingUser.getIdUtilisateur().equals(userId)) {
                throw new DuplicateResourceException("Un utilisateur avec cet email existe deja");
            }
        });

        if (isLastAdminRoleRemoval(utilisateur, request.getRole())) {
            throw new BusinessRuleException("Modification impossible: dernier administrateur du systeme");
        }

        utilisateur.setNom(request.getNom().trim());
        utilisateur.setPrenom(request.getPrenom().trim());
        utilisateur.setEmail(normalizedEmail);
        utilisateur.setRole(findRoleByLibelle(request.getRole()));

        if (request.getMotDePasse() != null && !request.getMotDePasse().isBlank()) {
            utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        }

        return mapUtilisateur(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public void deleteUtilisateur(Long userId, String currentUserEmail) {
        Utilisateur utilisateur = findUtilisateurById(userId);

        if (utilisateur.getEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new BusinessRuleException("Un administrateur ne peut pas supprimer son propre compte");
        }

        if (isAdmin(utilisateur) && utilisateurRepository.countByRole_Libelle(RoleLibelle.ROLE_ADMIN) <= 1) {
            throw new BusinessRuleException("Suppression impossible: dernier administrateur du systeme");
        }

        long projectCount = projetRepository.countByUtilisateur_IdUtilisateur(userId);
        long historyCount = historiqueModificationRepository.countByUtilisateur_IdUtilisateur(userId);

        if (projectCount > 0 || historyCount > 0) {
            throw new BusinessRuleException("Suppression impossible: utilisateur lie a des projets ou a un historique");
        }

        utilisateurRepository.delete(utilisateur);
    }

    private Utilisateur findUtilisateurById(Long userId) {
        return utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private Role findRoleByLibelle(RoleLibelle roleLibelle) {
        return roleRepository.findByLibelle(roleLibelle)
                .orElseThrow(() -> new ResourceNotFoundException("Role introuvable"));
    }

    private boolean isAdmin(Utilisateur utilisateur) {
        return utilisateur.getRole() != null
                && utilisateur.getRole().getLibelle() == RoleLibelle.ROLE_ADMIN;
    }

    private boolean isLastAdminRoleRemoval(Utilisateur utilisateur, RoleLibelle targetRole) {
        return isAdmin(utilisateur)
                && targetRole != RoleLibelle.ROLE_ADMIN
                && utilisateurRepository.countByRole_Libelle(RoleLibelle.ROLE_ADMIN) <= 1;
    }

    private UtilisateurResponse mapUtilisateur(Utilisateur utilisateur) {
        return UtilisateurResponse.builder()
                .idUtilisateur(utilisateur.getIdUtilisateur())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .dateDerniereConnexion(utilisateur.getDateDerniereConnexion())
                .role(utilisateur.getRole() != null && utilisateur.getRole().getLibelle() != null
                        ? utilisateur.getRole().getLibelle().name()
                        : null)
                .build();
    }
}
