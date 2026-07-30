package ma.onee.dsi.projectportfolio.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import ma.onee.dsi.projectportfolio.entity.Cout;
import ma.onee.dsi.projectportfolio.entity.Projet;
import ma.onee.dsi.projectportfolio.entity.Risque;
import ma.onee.dsi.projectportfolio.enums.NiveauCriticite;
import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;
import ma.onee.dsi.projectportfolio.enums.TypeCout;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class DashboardRepositoryAggregateTest {

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private CoutRepository coutRepository;

    @Autowired
    private RisqueRepository risqueRepository;

    @Test
    void dashboardAggregatesReturnGroupedPortfolioData() {
        Projet firstProjet = projetRepository.saveAndFlush(projet(
                "PRJ-001",
                StatutProjet.EN_COURS,
                PrioriteProjet.ELEVEE,
                new BigDecimal("1000.00")
        ));
        Projet secondProjet = projetRepository.saveAndFlush(projet(
                "PRJ-002",
                StatutProjet.EN_RETARD,
                PrioriteProjet.CRITIQUE,
                new BigDecimal("2000.00")
        ));
        coutRepository.save(cout(new BigDecimal("250.00"), firstProjet));
        coutRepository.save(cout(new BigDecimal("300.00"), firstProjet));
        coutRepository.save(cout(new BigDecimal("100.00"), secondProjet));
        risqueRepository.save(risque(NiveauCriticite.CRITIQUE, firstProjet));
        risqueRepository.save(risque(NiveauCriticite.CRITIQUE, secondProjet));
        risqueRepository.save(risque(NiveauCriticite.MOYEN, secondProjet));
        risqueRepository.flush();
        coutRepository.flush();

        assertThat(projetRepository.sumBudgetPrevisionnel()).isEqualByComparingTo("3000.00");
        assertThat(coutRepository.sumMontant()).isEqualByComparingTo("650.00");
        assertThat(risqueRepository.countByNiveauCriticite(NiveauCriticite.CRITIQUE)).isEqualTo(2);

        assertThat(projetRepository.countProjectsByStatus())
                .anySatisfy(item -> {
                    assertThat(item.getStatut()).isEqualTo(StatutProjet.EN_COURS);
                    assertThat(item.getCount()).isEqualTo(1);
                })
                .anySatisfy(item -> {
                    assertThat(item.getStatut()).isEqualTo(StatutProjet.EN_RETARD);
                    assertThat(item.getCount()).isEqualTo(1);
                });
        assertThat(projetRepository.countProjectsByPriority())
                .anySatisfy(item -> {
                    assertThat(item.getPriorite()).isEqualTo(PrioriteProjet.ELEVEE);
                    assertThat(item.getCount()).isEqualTo(1);
                });
        assertThat(coutRepository.sumMontantGroupByProject())
                .anySatisfy(item -> {
                    assertThat(item.getProjectId()).isEqualTo(firstProjet.getIdProjet());
                    assertThat(item.getConsumedCost()).isEqualByComparingTo("550.00");
                });
        assertThat(risqueRepository.countRisksByCriticality())
                .anySatisfy(item -> {
                    assertThat(item.getNiveauCriticite()).isEqualTo(NiveauCriticite.CRITIQUE);
                    assertThat(item.getCount()).isEqualTo(2);
                });
        assertThat(risqueRepository.countByCriticalityGroupByProject(NiveauCriticite.CRITIQUE))
                .hasSize(2);
        assertThat(projetRepository.findDashboardProjects())
                .extracting("projectId")
                .containsExactlyInAnyOrder(firstProjet.getIdProjet(), secondProjet.getIdProjet());
    }

    @Test
    void monetarySumsReturnNullWhenNoRowsExist() {
        assertThat(projetRepository.sumBudgetPrevisionnel()).isNull();
        assertThat(coutRepository.sumMontant()).isNull();
    }

    private Projet projet(String code, StatutProjet statut, PrioriteProjet priorite, BigDecimal budget) {
        Projet projet = new Projet();
        projet.setCode(code);
        projet.setIntitule("Projet " + code);
        projet.setDateDebutPrevue(LocalDate.of(2026, 1, 1));
        projet.setDateFinPrevue(LocalDate.of(2026, 12, 31));
        projet.setStatut(statut);
        projet.setBudgetPrevisionnel(budget);
        projet.setPriorite(priorite);
        projet.setPourcentageAvancement(statut == StatutProjet.PLANIFIE ? 0 : 50);
        if (statut != StatutProjet.PLANIFIE) {
            projet.setDateDebutReelle(LocalDate.of(2026, 1, 2));
        }
        return projet;
    }

    private Cout cout(BigDecimal montant, Projet projet) {
        Cout cout = new Cout();
        cout.setType(TypeCout.MATERIEL);
        cout.setMontant(montant);
        cout.setDateCout(LocalDate.of(2026, 2, 1));
        cout.setProjet(projet);
        return cout;
    }

    private Risque risque(NiveauCriticite niveauCriticite, Projet projet) {
        Risque risque = new Risque();
        risque.setDescription("Risque");
        risque.setNiveauCriticite(niveauCriticite);
        risque.setProjet(projet);
        return risque;
    }
}
