package ma.onee.dsi.projectportfolio.entity;

import ma.onee.dsi.projectportfolio.enums.TypeAction;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class HistoriqueModification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idHistoriqueModification;

    private LocalDate dateModification;

    private String description;

    @Enumerated(EnumType.STRING)
    private TypeAction typeAction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projet")
    private Projet projet;
}
