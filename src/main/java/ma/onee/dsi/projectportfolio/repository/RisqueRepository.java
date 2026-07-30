package ma.onee.dsi.projectportfolio.repository;

import java.util.List;
import ma.onee.dsi.projectportfolio.entity.Risque;
import ma.onee.dsi.projectportfolio.enums.NiveauCriticite;
import ma.onee.dsi.projectportfolio.repository.projection.CriticalRiskCountByProjectProjection;
import ma.onee.dsi.projectportfolio.repository.projection.RiskCriticalityCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RisqueRepository extends JpaRepository<Risque, Long> {

    long countByProjet_IdProjet(Long idProjet);

    List<Risque> findByProjet_IdProjet(Long idProjet);

    long countByNiveauCriticite(NiveauCriticite niveauCriticite);

    @Query("""
            select r.niveauCriticite as niveauCriticite, count(r) as count
            from Risque r
            group by r.niveauCriticite
            """)
    List<RiskCriticalityCountProjection> countRisksByCriticality();

    @Query("""
            select r.projet.idProjet as projectId, count(r) as criticalRiskCount
            from Risque r
            where r.niveauCriticite = :niveauCriticite
            group by r.projet.idProjet
            """)
    List<CriticalRiskCountByProjectProjection> countByCriticalityGroupByProject(
            @Param("niveauCriticite") NiveauCriticite niveauCriticite
    );
}
