package ma.onee.dsi.projectportfolio.repository;

import java.util.List;
import java.util.Optional;
import ma.onee.dsi.projectportfolio.entity.AffectationRessource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AffectationRessourceRepository extends JpaRepository<AffectationRessource, Long> {

    long countByProjet_IdProjet(Long idProjet);

    List<AffectationRessource> findByProjet_IdProjet(Long idProjet);

    List<AffectationRessource> findByRessource_IdRessource(Long idRessource);

    Optional<AffectationRessource> findByIdAffectationRessourceAndProjet_IdProjet(
            Long idAffectationRessource,
            Long idProjet
    );

    boolean existsByProjet_IdProjetAndRessource_IdRessource(Long idProjet, Long idRessource);

    boolean existsByRessource_IdRessource(Long idRessource);
}
