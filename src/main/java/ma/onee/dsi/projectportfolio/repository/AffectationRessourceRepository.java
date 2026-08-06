package ma.onee.dsi.projectportfolio.repository;

import java.util.List;
import java.util.Optional;
import ma.onee.dsi.projectportfolio.entity.AffectationRessource;
import ma.onee.dsi.projectportfolio.repository.projection.ChargeByNatureInterventionProjection;
import ma.onee.dsi.projectportfolio.repository.projection.ResourceCountByProjectProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    @Query("""
            select ar.projet.idProjet as projectId, count(ar) as resourceCount
            from AffectationRessource ar
            group by ar.projet.idProjet
            """)
    List<ResourceCountByProjectProjection> countByProject();

    @Query("""
            select ar.natureIntervention as natureIntervention,
                   sum(ar.chargeJH) as totalChargeJH,
                   count(ar) as count
            from AffectationRessource ar
            group by ar.natureIntervention
            """)
    List<ChargeByNatureInterventionProjection> sumChargeJHByNature();
}
