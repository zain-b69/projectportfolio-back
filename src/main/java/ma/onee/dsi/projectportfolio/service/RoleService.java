package ma.onee.dsi.projectportfolio.service;

import java.util.List;
import ma.onee.dsi.projectportfolio.dto.RoleResponse;
import ma.onee.dsi.projectportfolio.dto.UtilisateurRoleResponse;
import ma.onee.dsi.projectportfolio.entity.Role;
import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import ma.onee.dsi.projectportfolio.enums.RoleLibelle;
import ma.onee.dsi.projectportfolio.repository.RoleRepository;
import ma.onee.dsi.projectportfolio.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final UtilisateurRepository utilisateurRepository;

    public RoleService(RoleRepository roleRepository, UtilisateurRepository utilisateurRepository) {
        this.roleRepository = roleRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapRole)
                .toList();
    }

    @Transactional
    public UtilisateurRoleResponse updateUserRole(Long userId, RoleLibelle roleLibelle) {
        if (roleLibelle == null) {
            throw new IllegalArgumentException("Le libelle du role est obligatoire");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        Role role = roleRepository.findByLibelle(roleLibelle)
                .orElseThrow(() -> new IllegalStateException("Role introuvable"));

        utilisateur.setRole(role);
        Utilisateur savedUser = utilisateurRepository.save(utilisateur);

        return mapUtilisateur(savedUser);
    }

    private RoleResponse mapRole(Role role) {
        return RoleResponse.builder()
                .idRole(role.getIdRole())
                .libelle(role.getLibelle().name())
                .build();
    }

    private UtilisateurRoleResponse mapUtilisateur(Utilisateur utilisateur) {
        return UtilisateurRoleResponse.builder()
                .idUtilisateur(utilisateur.getIdUtilisateur())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .role(utilisateur.getRole() != null && utilisateur.getRole().getLibelle() != null
                        ? utilisateur.getRole().getLibelle().name()
                        : null)
                .build();
    }
}
