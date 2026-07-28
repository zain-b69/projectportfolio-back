package ma.onee.dsi.projectportfolio.repository;

import java.math.BigDecimal;
import java.util.List;
import ma.onee.dsi.projectportfolio.entity.Cout;
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
}
