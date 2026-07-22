package ma.onee.dsi.projectportfolio.repository;

import ma.onee.dsi.projectportfolio.entity.HistoriqueModification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistoriqueModificationRepository extends JpaRepository<HistoriqueModification, Long> {

    long countByUtilisateur_IdUtilisateur(Long idUtilisateur);

    @Modifying(flushAutomatically = true)
    @Query("update HistoriqueModification h set h.projet = null where h.projet.idProjet = :idProjet")
    int detachProjet(@Param("idProjet") Long idProjet);
}
