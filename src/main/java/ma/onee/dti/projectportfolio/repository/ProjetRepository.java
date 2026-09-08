package ma.onee.dti.projectportfolio.repository;

import java.util.Optional;
import ma.onee.dti.projectportfolio.entity.Projet;
import ma.onee.dti.projectportfolio.repository.projection.DashboardProjectProjection;
import ma.onee.dti.projectportfolio.repository.projection.PlannedBudgetByStatusProjection;
import ma.onee.dti.projectportfolio.repository.projection.ProjectPriorityCountProjection;
import ma.onee.dti.projectportfolio.repository.projection.ProjectStatusCountProjection;
import ma.onee.dti.projectportfolio.repository.projection.ReportProjectProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ProjetRepository extends JpaRepository<Projet, Long>, JpaSpecificationExecutor<Projet> {

    long countByUtilisateur_IdUtilisateur(Long idUtilisateur);

    boolean existsByCodeIgnoreCase(String code);

    Optional<Projet> findByCodeIgnoreCase(String code);

    @Query("""
            select p.statut as statut, count(p) as count
            from Projet p
            group by p.statut
            """)
    java.util.List<ProjectStatusCountProjection> countProjectsByStatus();

    @Query("""
            select p.priorite as priorite, count(p) as count
            from Projet p
            group by p.priorite
            """)
    java.util.List<ProjectPriorityCountProjection> countProjectsByPriority();

    @Query("""
            select sum(p.budgetPrevisionnel)
            from Projet p
            """)
    java.math.BigDecimal sumBudgetPrevisionnel();

    @Query("""
            select p.idProjet as projectId,
                   p.code as code,
                   p.intitule as intitule,
                   p.statut as statut,
                   p.priorite as priorite,
                   p.pourcentageAvancement as pourcentageAvancement,
                   p.dateFinPrevue as dateFinPrevue,
                   p.budgetPrevisionnel as plannedBudget
            from Projet p
            """)
    java.util.List<DashboardProjectProjection> findDashboardProjects();

    @Query("""
            select p.idProjet as projectId,
                   p.code as code,
                   p.intitule as intitule,
                   p.statut as statut,
                   p.priorite as priorite,
                   p.pourcentageAvancement as pourcentageAvancement,
                   p.budgetPrevisionnel as plannedBudget,
                   u.prenom as prenomResponsable,
                   u.nom as nomResponsable
            from Projet p left join p.utilisateur u
            """)
    java.util.List<ReportProjectProjection> findReportProjects();

    @Query("""
            select p.statut as statut,
                   sum(p.budgetPrevisionnel) as totalPlannedBudget,
                   count(p) as projectCount
            from Projet p
            group by p.statut
            """)
    java.util.List<PlannedBudgetByStatusProjection> budgetByStatus();
}
