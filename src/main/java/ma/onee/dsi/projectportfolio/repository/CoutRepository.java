package ma.onee.dsi.projectportfolio.repository;

import ma.onee.dsi.projectportfolio.entity.Cout;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoutRepository extends JpaRepository<Cout, Long> {

    long countByProjet_IdProjet(Long idProjet);
}
