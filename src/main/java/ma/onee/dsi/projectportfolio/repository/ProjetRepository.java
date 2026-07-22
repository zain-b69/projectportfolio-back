package ma.onee.dsi.projectportfolio.repository;

import java.util.Optional;
import ma.onee.dsi.projectportfolio.entity.Projet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProjetRepository extends JpaRepository<Projet, Long>, JpaSpecificationExecutor<Projet> {

    long countByUtilisateur_IdUtilisateur(Long idUtilisateur);

    boolean existsByCodeIgnoreCase(String code);

    Optional<Projet> findByCodeIgnoreCase(String code);
}
