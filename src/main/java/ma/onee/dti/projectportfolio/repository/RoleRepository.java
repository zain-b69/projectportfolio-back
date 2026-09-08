package ma.onee.dti.projectportfolio.repository;

import java.util.Optional;
import ma.onee.dti.projectportfolio.entity.Role;
import ma.onee.dti.projectportfolio.enums.RoleLibelle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByLibelle(RoleLibelle libelle);
}
