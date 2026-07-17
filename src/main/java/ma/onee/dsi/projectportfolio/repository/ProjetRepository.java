package ma.onee.dsi.projectportfolio.repository;

import ma.onee.dsi.projectportfolio.entity.Projet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjetRepository extends JpaRepository<Projet, Long> {

    long countByUtilisateur_IdUtilisateur(Long idUtilisateur);
}
