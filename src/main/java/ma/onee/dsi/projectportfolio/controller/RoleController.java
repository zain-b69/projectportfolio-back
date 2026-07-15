package ma.onee.dsi.projectportfolio.controller;

import java.util.List;
import ma.onee.dsi.projectportfolio.dto.RoleResponse;
import ma.onee.dsi.projectportfolio.dto.UpdateUserRoleRequest;
import ma.onee.dsi.projectportfolio.dto.UtilisateurRoleResponse;
import ma.onee.dsi.projectportfolio.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @PutMapping("/utilisateurs/{userId}")
    public ResponseEntity<UtilisateurRoleResponse> updateUserRole(
            @PathVariable Long userId,
            @RequestBody UpdateUserRoleRequest request
    ) {
        return ResponseEntity.ok(roleService.updateUserRole(userId, request.getLibelle()));
    }
}
