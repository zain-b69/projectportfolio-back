package ma.onee.dsi.projectportfolio.repository;

import ma.onee.dsi.projectportfolio.entity.Ressource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RessourceRepository extends JpaRepository<Ressource, Long>, JpaSpecificationExecutor<Ressource> {
}
