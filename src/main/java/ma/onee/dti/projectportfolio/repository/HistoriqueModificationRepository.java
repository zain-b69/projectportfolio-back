package ma.onee.dti.projectportfolio.repository;

import java.util.List;
import ma.onee.dti.projectportfolio.entity.HistoriqueModification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistoriqueModificationRepository extends JpaRepository<HistoriqueModification, Long>,
        JpaSpecificationExecutor<HistoriqueModification> {

    long countByUtilisateur_IdUtilisateur(Long idUtilisateur);

    List<HistoriqueModification> findByProjet_IdProjetOrderByDateModificationDescIdHistoriqueModificationDesc(
            Long idProjet
    );

    @Modifying(flushAutomatically = true)
    @Query("update HistoriqueModification h set h.projet = null where h.projet.idProjet = :idProjet")
    int detachProjet(@Param("idProjet") Long idProjet);
}
