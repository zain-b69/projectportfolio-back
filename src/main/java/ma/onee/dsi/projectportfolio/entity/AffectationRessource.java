package ma.onee.dsi.projectportfolio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ma.onee.dsi.projectportfolio.enums.NatureIntervention;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(
        name = "affectation_ressource",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_affectation_projet_ressource",
                        columnNames = {"id_projet", "id_ressource"}
                )
        }
)
@Check(constraints = "charge_jh > 0")
public class AffectationRessource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAffectationRessource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NatureIntervention natureIntervention;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal chargeJH;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_projet", nullable = false)
    private Projet projet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ressource", nullable = false)
    private Ressource ressource;
}
