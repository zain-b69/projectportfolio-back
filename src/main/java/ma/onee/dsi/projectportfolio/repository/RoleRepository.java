package ma.onee.dsi.projectportfolio.repository;

import java.util.Optional;
import ma.onee.dsi.projectportfolio.entity.Role;
import ma.onee.dsi.projectportfolio.enums.RoleLibelle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByLibelle(RoleLibelle libelle);
}
