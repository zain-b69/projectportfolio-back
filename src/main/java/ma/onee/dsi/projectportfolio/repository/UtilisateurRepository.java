package ma.onee.dsi.projectportfolio.repository;

import java.util.Optional;
import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);
}
