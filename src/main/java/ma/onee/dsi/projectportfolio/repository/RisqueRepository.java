package ma.onee.dsi.projectportfolio.repository;

import java.util.List;
import ma.onee.dsi.projectportfolio.entity.Risque;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RisqueRepository extends JpaRepository<Risque, Long> {

    long countByProjet_IdProjet(Long idProjet);

    List<Risque> findByProjet_IdProjet(Long idProjet);
}
