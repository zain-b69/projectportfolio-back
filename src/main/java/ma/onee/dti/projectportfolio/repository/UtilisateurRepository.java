package ma.onee.dti.projectportfolio.repository;

import ma.onee.dti.projectportfolio.entity.Utilisateur;
import ma.onee.dti.projectportfolio.enums.RoleLibelle;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    @EntityGraph(attributePaths = "role")
    Optional<Utilisateur> findByEmail(String email);

    @EntityGraph(attributePaths = "role")
    Optional<Utilisateur> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole_Libelle(RoleLibelle libelle);
}
