package ma.onee.dsi.projectportfolio.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Ressource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRessource;

    private String nom;

    private String fonction;

    @OneToMany(mappedBy = "ressource")
    private List<AffectationRessource> affectationRessources = new ArrayList<>();
}
