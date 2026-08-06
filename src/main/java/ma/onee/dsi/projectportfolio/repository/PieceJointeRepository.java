package ma.onee.dsi.projectportfolio.repository;

import java.util.List;
import ma.onee.dsi.projectportfolio.entity.PieceJointe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PieceJointeRepository extends JpaRepository<PieceJointe, Long> {

    long countByProjet_IdProjet(Long idProjet);

    List<PieceJointe> findByProjet_IdProjet(Long idProjet);
}
