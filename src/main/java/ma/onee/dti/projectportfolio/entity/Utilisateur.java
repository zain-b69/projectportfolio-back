package ma.onee.dti.projectportfolio.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUtilisateur;

    private String nom;

    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    private String motDePasse;

    private LocalDateTime dateDerniereConnexion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_role")
    private Role role;

    @OneToMany(mappedBy = "utilisateur")
    private List<HistoriqueModification> historiqueModifications = new ArrayList<>();

    @OneToMany(mappedBy = "utilisateur")
    private List<Projet> projets = new ArrayList<>();

}
