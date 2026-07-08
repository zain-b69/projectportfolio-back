package ma.onee.dsi.projectportfolio.entity;

import ma.onee.dsi.projectportfolio.enums.TypeCout;
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
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Cout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCout;

    @Enumerated(EnumType.STRING)
    private TypeCout type;

    private BigDecimal montant;

    private LocalDate dateCout;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projet")
    private Projet projet;
}
