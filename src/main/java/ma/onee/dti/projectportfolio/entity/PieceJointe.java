package ma.onee.dti.projectportfolio.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class PieceJointe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPieceJointe;

    private String nomFichier;

    private String cheminFichier;

    private LocalDateTime dateAjout;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projet")
    private Projet projet;
}
