package ma.onee.dsi.projectportfolio.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.onee.dsi.projectportfolio.entity.Cout;
import ma.onee.dsi.projectportfolio.entity.Projet;
import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;
import ma.onee.dsi.projectportfolio.enums.TypeCout;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class CoutRepositoryTest {

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private CoutRepository coutRepository;

    @Test
    void savingValidCostLinkedToProjectSucceeds() {
        Projet projet = projet("PRJ-001");
        projetRepository.saveAndFlush(projet);

        Cout cout = cout(TypeCout.MATERIEL, new BigDecimal("1200.50"), LocalDate.of(2026, 2, 1), projet);

        Cout savedCout = coutRepository.saveAndFlush(cout);

        assertThat(savedCout.getIdCout()).isNotNull();
        assertThat(savedCout.getType()).isEqualTo(TypeCout.MATERIEL);
        assertThat(savedCout.getMontant()).isEqualByComparingTo("1200.50");
        assertThat(savedCout.getDateCout()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(savedCout.getProjet().getIdProjet()).isEqualTo(projet.getIdProjet());
    }

    @Test
    void findByProjetIdReturnsOnlyCostsBelongingToProject() {
        Projet firstProjet = projetRepository.saveAndFlush(projet("PRJ-001"));
        Projet secondProjet = projetRepository.saveAndFlush(projet("PRJ-002"));
        Cout firstCout = coutRepository.save(cout(
                TypeCout.MATERIEL,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 1, 10),
                firstProjet
        ));
        Cout secondCout = coutRepository.save(cout(
                TypeCout.LOGICIEL,
                new BigDecimal("200.00"),
                LocalDate.of(2026, 1, 11),
                firstProjet
        ));
        coutRepository.save(cout(
                TypeCout.AUTRE,
                new BigDecimal("300.00"),
                LocalDate.of(2026, 1, 12),
                secondProjet
        ));
        coutRepository.flush();

        List<Cout> couts = coutRepository.findByProjet_IdProjet(firstProjet.getIdProjet());

        assertThat(couts)
                .extracting(Cout::getIdCout)
                .containsExactlyInAnyOrder(firstCout.getIdCout(), secondCout.getIdCout());
    }

    @Test
    void countByProjetIdReturnsCorrectCount() {
        Projet firstProjet = projetRepository.saveAndFlush(projet("PRJ-001"));
        Projet secondProjet = projetRepository.saveAndFlush(projet("PRJ-002"));
        coutRepository.save(cout(TypeCout.MATERIEL, new BigDecimal("100.00"), LocalDate.of(2026, 1, 10), firstProjet));
        coutRepository.save(cout(TypeCout.LOGICIEL, new BigDecimal("200.00"), LocalDate.of(2026, 1, 11), firstProjet));
        coutRepository.save(cout(TypeCout.AUTRE, new BigDecimal("300.00"), LocalDate.of(2026, 1, 12), secondProjet));
        coutRepository.flush();

        long count = coutRepository.countByProjet_IdProjet(firstProjet.getIdProjet());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void sumMontantByProjetIdReturnsCorrectTotal() {
        Projet firstProjet = projetRepository.saveAndFlush(projet("PRJ-001"));
        Projet secondProjet = projetRepository.saveAndFlush(projet("PRJ-002"));
        coutRepository.save(cout(TypeCout.MATERIEL, new BigDecimal("100.50"), LocalDate.of(2026, 1, 10), firstProjet));
        coutRepository.save(cout(TypeCout.LOGICIEL, new BigDecimal("200.25"), LocalDate.of(2026, 1, 11), firstProjet));
        coutRepository.save(cout(TypeCout.AUTRE, new BigDecimal("300.00"), LocalDate.of(2026, 1, 12), secondProjet));
        coutRepository.flush();

        BigDecimal total = coutRepository.sumMontantByProjetId(firstProjet.getIdProjet());

        assertThat(total).isEqualByComparingTo("300.75");
    }

    @Test
    void sumMontantByProjetIdReturnsNullWhenProjectHasNoCosts() {
        Projet projet = projetRepository.saveAndFlush(projet("PRJ-001"));

        BigDecimal total = coutRepository.sumMontantByProjetId(projet.getIdProjet());

        assertThat(total).isNull();
    }

    @Test
    void savingCostWithoutProjectViolatesDatabaseConstraint() {
        Cout cout = cout(TypeCout.MATERIEL, new BigDecimal("100.00"), LocalDate.of(2026, 1, 10), null);

        assertThatThrownBy(() -> coutRepository.saveAndFlush(cout))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savingCostWithoutTypeViolatesDatabaseConstraint() {
        Projet projet = projetRepository.saveAndFlush(projet("PRJ-001"));
        Cout cout = cout(null, new BigDecimal("100.00"), LocalDate.of(2026, 1, 10), projet);

        assertThatThrownBy(() -> coutRepository.saveAndFlush(cout))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savingCostWithoutMontantViolatesDatabaseConstraint() {
        Projet projet = projetRepository.saveAndFlush(projet("PRJ-001"));
        Cout cout = cout(TypeCout.MATERIEL, null, LocalDate.of(2026, 1, 10), projet);

        assertThatThrownBy(() -> coutRepository.saveAndFlush(cout))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savingCostWithoutDateCoutViolatesDatabaseConstraint() {
        Projet projet = projetRepository.saveAndFlush(projet("PRJ-001"));
        Cout cout = cout(TypeCout.MATERIEL, new BigDecimal("100.00"), null, projet);

        assertThatThrownBy(() -> coutRepository.saveAndFlush(cout))
                .isInstanceOf(DataIntegrityViolationException.class);
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

    private Cout cout(TypeCout type, BigDecimal montant, LocalDate dateCout, Projet projet) {
        Cout cout = new Cout();
        cout.setType(type);
        cout.setMontant(montant);
        cout.setDateCout(dateCout);
        cout.setProjet(projet);
        return cout;
    }
}
