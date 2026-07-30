package ma.onee.dsi.projectportfolio.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.onee.dsi.projectportfolio.dto.ProjetResponse;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

class ProjetExportServiceTest {

    private final ProjetExportService projetExportService = new ProjetExportService();

    @Test
    void exportProjectsCreatesReadableWorkbookWithExpectedHeadersAndProjectRow() throws Exception {
        ProjetResponse projet = ProjetResponse.builder()
                .code("PRJ-001")
                .intitule("Refonte portefeuille")
                .prenomResponsable("Sara")
                .nomResponsable("Alami")
                .emailResponsable("sara.alami@example.com")
                .statut("EN_COURS")
                .priorite("ELEVEE")
                .budgetPrevisionnel(new BigDecimal("125000.50"))
                .pourcentageAvancement(45)
                .dateDebutPrevue(LocalDate.of(2026, 1, 15))
                .dateFinPrevue(LocalDate.of(2026, 6, 30))
                .build();

        byte[] content = projetExportService.exportProjects(List.of(projet));

        assertThat(content).isNotEmpty();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet("Projets");
            assertThat(sheet).isNotNull();

            assertHeaders(sheet.getRow(0));

            Row row = sheet.getRow(1);
            assertThat(row.getCell(0).getStringCellValue()).isEqualTo("PRJ-001");
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("Refonte portefeuille");
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("Sara Alami");
            assertThat(row.getCell(3).getStringCellValue()).isEqualTo("EN_COURS");
            assertThat(row.getCell(4).getStringCellValue()).isEqualTo("ELEVEE");
            assertThat(row.getCell(5).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(row.getCell(5).getNumericCellValue()).isEqualTo(125000.50);
            assertThat(row.getCell(6).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(row.getCell(6).getNumericCellValue()).isEqualTo(45);
            assertThat(row.getCell(7).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(2026, 1, 15));
            assertThat(row.getCell(8).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(2026, 6, 30));
        }
    }

    @Test
    void exportProjectsUsesEmailWhenResponsableNameIsUnavailable() throws Exception {
        ProjetResponse projet = ProjetResponse.builder()
                .code("PRJ-002")
                .emailResponsable("chef.projet@example.com")
                .build();

        byte[] content = projetExportService.exportProjects(List.of(projet));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Row row = workbook.getSheet("Projets").getRow(1);

            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("chef.projet@example.com");
        }
    }

    @Test
    void exportProjectsHandlesNullValuesWithoutError() throws Exception {
        ProjetResponse projet = ProjetResponse.builder().build();

        byte[] content = projetExportService.exportProjects(List.of(projet));

        assertThat(content).isNotEmpty();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Row row = workbook.getSheet("Projets").getRow(1);

            assertThat(row).isNotNull();
            for (int columnIndex = 0; columnIndex < 9; columnIndex++) {
                assertThat(row.getCell(columnIndex)).isNull();
            }
        }
    }

    @Test
    void exportProjectsWithEmptyListCreatesOnlyHeaderRow() throws Exception {
        byte[] content = projetExportService.exportProjects(List.of());

        assertThat(content).isNotEmpty();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet("Projets");

            assertThat(sheet).isNotNull();
            assertHeaders(sheet.getRow(0));
            assertThat(sheet.getLastRowNum()).isZero();
        }
    }

    private void assertHeaders(Row headerRow) {
        assertThat(headerRow).isNotNull();
        assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Code");
        assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Intitulé");
        assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Responsable");
        assertThat(headerRow.getCell(3).getStringCellValue()).isEqualTo("Statut");
        assertThat(headerRow.getCell(4).getStringCellValue()).isEqualTo("Priorité");
        assertThat(headerRow.getCell(5).getStringCellValue()).isEqualTo("Budget prévisionnel");
        assertThat(headerRow.getCell(6).getStringCellValue()).isEqualTo("Avancement (%)");
        assertThat(headerRow.getCell(7).getStringCellValue()).isEqualTo("Date début prévue");
        assertThat(headerRow.getCell(8).getStringCellValue()).isEqualTo("Date fin prévue");
    }
}
