package ma.onee.dti.projectportfolio.entity;

import jakarta.validation.constraints.Digits;
import ma.onee.dti.projectportfolio.enums.TypeCout;
import jakarta.persistence.Column;
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
    @Column(nullable = false, length = 20)
    private TypeCout type;

    @Column(nullable = false, precision = 19, scale = 2)
    @Digits(integer = 17, fraction = 2)
    private BigDecimal montant;

    @Column(nullable = false)
    private LocalDate dateCout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_projet", nullable = false)
    private Projet projet;
}
