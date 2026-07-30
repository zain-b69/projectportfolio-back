package ma.onee.dsi.projectportfolio.repository;

import java.math.BigDecimal;
import java.util.List;
import ma.onee.dsi.projectportfolio.entity.Cout;
import ma.onee.dsi.projectportfolio.repository.projection.ConsumedCostByProjectProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoutRepository extends JpaRepository<Cout, Long> {

    long countByProjet_IdProjet(Long idProjet);

    List<Cout> findByProjet_IdProjet(Long idProjet);

    @Query("""
            select sum(c.montant)
            from Cout c
            where c.projet.idProjet = :idProjet
            """)
    BigDecimal sumMontantByProjetId(@Param("idProjet") Long idProjet);

    @Query("""
            select sum(c.montant)
            from Cout c
            """)
    BigDecimal sumMontant();

    @Query("""
            select c.projet.idProjet as projectId, sum(c.montant) as consumedCost
            from Cout c
            group by c.projet.idProjet
            """)
    List<ConsumedCostByProjectProjection> sumMontantGroupByProject();
}
