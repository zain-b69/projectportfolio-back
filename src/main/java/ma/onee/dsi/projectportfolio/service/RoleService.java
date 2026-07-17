package ma.onee.dsi.projectportfolio.service;

import java.util.List;
import ma.onee.dsi.projectportfolio.dto.RoleResponse;
import ma.onee.dsi.projectportfolio.dto.UtilisateurRoleResponse;
import ma.onee.dsi.projectportfolio.entity.Role;
import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import ma.onee.dsi.projectportfolio.enums.RoleLibelle;
import ma.onee.dsi.projectportfolio.exception.BusinessRuleException;
import ma.onee.dsi.projectportfolio.exception.ResourceNotFoundException;
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
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Role role = roleRepository.findByLibelle(roleLibelle)
                .orElseThrow(() -> new ResourceNotFoundException("Role introuvable"));

        if (isLastAdminRoleRemoval(utilisateur, roleLibelle)) {
            throw new BusinessRuleException("Modification impossible: dernier administrateur du systeme");
        }

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

    private boolean isLastAdminRoleRemoval(Utilisateur utilisateur, RoleLibelle targetRole) {
        return utilisateur.getRole() != null
                && utilisateur.getRole().getLibelle() == RoleLibelle.ROLE_ADMIN
                && targetRole != RoleLibelle.ROLE_ADMIN
                && utilisateurRepository.countByRole_Libelle(RoleLibelle.ROLE_ADMIN) <= 1;
    }
}
