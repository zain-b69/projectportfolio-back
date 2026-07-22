package ma.onee.dsi.projectportfolio.entity;

import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;
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
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProjet;

    @Column(nullable = false, unique = true)
    private String code;

    private String intitule;

    private String descriptif;

    private LocalDate dateDebutPrevue;

    private LocalDate dateFinPrevue;

    private LocalDate dateDebutReelle;

    private LocalDate dateFinReelle;

    @Enumerated(EnumType.STRING)
    private StatutProjet statut;

    private BigDecimal budgetPrevisionnel;

    @Enumerated(EnumType.STRING)
    private PrioriteProjet priorite;

    private Integer pourcentageAvancement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;

    @OneToMany(mappedBy = "projet")
    private List<HistoriqueModification> historiqueModifications = new ArrayList<>();

    @OneToMany(mappedBy = "projet")
    private List<PieceJointe> piecesJointes = new ArrayList<>();

    @OneToMany(mappedBy = "projet")
    private List<Cout> couts = new ArrayList<>();

    @OneToMany(mappedBy = "projet")
    private List<Risque> risques = new ArrayList<>();

    @OneToMany(mappedBy = "projet")
    private List<AffectationRessource> affectationRessources = new ArrayList<>();
}
