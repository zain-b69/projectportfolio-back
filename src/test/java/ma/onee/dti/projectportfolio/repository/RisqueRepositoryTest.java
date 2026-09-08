package ma.onee.dti.projectportfolio.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.onee.dti.projectportfolio.entity.Projet;
import ma.onee.dti.projectportfolio.entity.Risque;
import ma.onee.dti.projectportfolio.enums.NiveauCriticite;
import ma.onee.dti.projectportfolio.enums.PrioriteProjet;
import ma.onee.dti.projectportfolio.enums.StatutProjet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class RisqueRepositoryTest {

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private RisqueRepository risqueRepository;

    @Test
    void savingValidRiskLinkedToProjectSucceeds() {
        Projet projet = projet("PRJ-001");
        projetRepository.saveAndFlush(projet);

        Risque risque = risque("Risque planning", NiveauCriticite.MOYEN, projet);

        Risque savedRisque = risqueRepository.saveAndFlush(risque);

        assertThat(savedRisque.getIdRisque()).isNotNull();
        assertThat(savedRisque.getDescription()).isEqualTo("Risque planning");
        assertThat(savedRisque.getNiveauCriticite()).isEqualTo(NiveauCriticite.MOYEN);
        assertThat(savedRisque.getProjet().getIdProjet()).isEqualTo(projet.getIdProjet());
    }

    @Test
    void findByProjetIdReturnsOnlyRisksBelongingToProject() {
        Projet firstProjet = projetRepository.saveAndFlush(projet("PRJ-001"));
        Projet secondProjet = projetRepository.saveAndFlush(projet("PRJ-002"));
        Risque firstRisque = risqueRepository.save(risque("Risque budget", NiveauCriticite.ELEVE, firstProjet));
        Risque secondRisque = risqueRepository.save(risque("Risque delai", NiveauCriticite.CRITIQUE, firstProjet));
        risqueRepository.save(risque("Risque hors projet", NiveauCriticite.FAIBLE, secondProjet));
        risqueRepository.flush();

        List<Risque> risques = risqueRepository.findByProjet_IdProjet(firstProjet.getIdProjet());

        assertThat(risques)
                .extracting(Risque::getIdRisque)
                .containsExactlyInAnyOrder(firstRisque.getIdRisque(), secondRisque.getIdRisque());
    }

    @Test
    void savingRiskWithoutProjectViolatesDatabaseConstraint() {
        Risque risque = new Risque();
        risque.setDescription("Risque sans projet");
        risque.setNiveauCriticite(NiveauCriticite.MOYEN);

        assertThatThrownBy(() -> risqueRepository.saveAndFlush(risque))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void countByProjetIdReturnsCorrectCount() {
        Projet firstProjet = projetRepository.saveAndFlush(projet("PRJ-001"));
        Projet secondProjet = projetRepository.saveAndFlush(projet("PRJ-002"));
        risqueRepository.save(risque("Risque budget", NiveauCriticite.ELEVE, firstProjet));
        risqueRepository.save(risque("Risque delai", NiveauCriticite.CRITIQUE, firstProjet));
        risqueRepository.save(risque("Risque hors projet", NiveauCriticite.FAIBLE, secondProjet));
        risqueRepository.flush();

        long count = risqueRepository.countByProjet_IdProjet(firstProjet.getIdProjet());

        assertThat(count).isEqualTo(2);
    }

    private Projet projet(String code) {
        Projet projet = new Projet();
        projet.setCode(code);
        projet.setIntitule("Projet portefeuille");
        projet.setDateDebutPrevue(LocalDate.of(2026, 1, 1));
        projet.setDateFinPrevue(LocalDate.of(2026, 12, 31));
        projet.setStatut(StatutProjet.PLANIFIE);
        projet.setBudgetPrevisionnel(new BigDecimal("10000.00"));
        projet.setPriorite(PrioriteProjet.MOYENNE);
        projet.setPourcentageAvancement(0);
        return projet;
    }

    private Risque risque(String description, NiveauCriticite niveauCriticite, Projet projet) {
        Risque risque = new Risque();
        risque.setDescription(description);
        risque.setNiveauCriticite(niveauCriticite);
        risque.setProjet(projet);
        return risque;
    }
}
