package ma.onee.dsi.projectportfolio.entity;

import ma.onee.dsi.projectportfolio.enums.NatureIntervention;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class AffectationRessource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAffectationRessource;

    @Enumerated(EnumType.STRING)
    private NatureIntervention natureIntervention;

    private BigDecimal chargeJH;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projet")
    private Projet projet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ressource")
    private Ressource ressource;
}
