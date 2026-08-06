package ma.onee.dsi.projectportfolio.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import ma.onee.dsi.projectportfolio.dto.PieceJointeResponse;
import ma.onee.dsi.projectportfolio.entity.Projet;
import ma.onee.dsi.projectportfolio.entity.Role;
import ma.onee.dsi.projectportfolio.entity.Utilisateur;
import ma.onee.dsi.projectportfolio.enums.PrioriteProjet;
import ma.onee.dsi.projectportfolio.enums.RoleLibelle;
import ma.onee.dsi.projectportfolio.enums.StatutProjet;
import ma.onee.dsi.projectportfolio.repository.HistoriqueModificationRepository;
import ma.onee.dsi.projectportfolio.repository.PieceJointeRepository;
import ma.onee.dsi.projectportfolio.repository.ProjetRepository;
import ma.onee.dsi.projectportfolio.repository.RoleRepository;
import ma.onee.dsi.projectportfolio.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@Import({PieceJointeService.class, FileStorageService.class, HistoriqueService.class})
class PieceJointeServiceIntegrationTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private PieceJointeService pieceJointeService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private PieceJointeRepository pieceJointeRepository;

    @Autowired
    private HistoriqueModificationRepository historiqueModificationRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.upload-dir", () -> tempDir.resolve("uploads").toString());
    }

    @Test
    void uploadCreatesFileUnderUploadsProjectsAndDeleteRemovesFileAndDatabaseRecord() throws Exception {
        Utilisateur projectManager = utilisateurRepository.saveAndFlush(utilisateur());
        Projet projet = projetRepository.saveAndFlush(projet(projectManager));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "planning.pdf",
                "application/pdf",
                "contenu du fichier".getBytes()
        );

        PieceJointeResponse uploaded = pieceJointeService.upload(projet.getIdProjet(), file, "pm@example.com");

        assertThat(uploaded.getIdPieceJointe()).isNotNull();
        assertThat(pieceJointeRepository.findById(uploaded.getIdPieceJointe())).isPresent();

        Path projectUploadsFolder = tempDir.resolve("uploads")
                .resolve("projets")
                .resolve(projet.getIdProjet().toString());
        assertThat(projectUploadsFolder).exists().isDirectory();

        Path storedFile = Files.list(projectUploadsFolder)
                .findFirst()
                .orElseThrow();
        assertThat(storedFile).exists().isRegularFile();
        assertThat(Files.readString(storedFile)).isEqualTo("contenu du fichier");

        pieceJointeService.delete(uploaded.getIdPieceJointe(), "pm@example.com");

        assertThat(storedFile).doesNotExist();
        assertThat(pieceJointeRepository.findById(uploaded.getIdPieceJointe())).isEmpty();
        assertThat(pieceJointeRepository.countByProjet_IdProjet(projet.getIdProjet())).isZero();
        assertThat(historiqueModificationRepository.findAll()).hasSize(2);
    }

    private Utilisateur utilisateur() {
        Role role = new Role();
        role.setLibelle(RoleLibelle.ROLE_RESPONSABLE_PROJET);
        Role savedRole = roleRepository.saveAndFlush(role);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom("Nom");
        utilisateur.setPrenom("Prenom");
        utilisateur.setEmail("pm@example.com");
        utilisateur.setMotDePasse("secret");
        utilisateur.setRole(savedRole);
        return utilisateur;
    }

    private Projet projet(Utilisateur responsable) {
        Projet projet = new Projet();
        projet.setCode("PRJ-PIECE");
        projet.setIntitule("Projet pieces jointes");
        projet.setDateDebutPrevue(LocalDate.of(2026, 1, 1));
        projet.setDateFinPrevue(LocalDate.of(2026, 12, 31));
        projet.setStatut(StatutProjet.PLANIFIE);
        projet.setBudgetPrevisionnel(new BigDecimal("10000.00"));
        projet.setPriorite(PrioriteProjet.MOYENNE);
        projet.setPourcentageAvancement(0);
        projet.setUtilisateur(responsable);
        return projet;
    }
}
