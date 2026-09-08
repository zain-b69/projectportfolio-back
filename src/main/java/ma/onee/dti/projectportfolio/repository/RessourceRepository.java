package ma.onee.dti.projectportfolio.repository;

import ma.onee.dti.projectportfolio.entity.Ressource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RessourceRepository extends JpaRepository<Ressource, Long>, JpaSpecificationExecutor<Ressource> {
}
