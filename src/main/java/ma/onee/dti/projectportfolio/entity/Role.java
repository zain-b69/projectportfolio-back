package ma.onee.dti.projectportfolio.entity;

import ma.onee.dti.projectportfolio.enums.RoleLibelle;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRole;

    @Enumerated(EnumType.STRING)
    private RoleLibelle libelle;

    @OneToMany(mappedBy = "role")
    private List<Utilisateur> utilisateurs = new ArrayList<>();

}
