package ma.onee.dsi.projectportfolio.repository;

import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
}
