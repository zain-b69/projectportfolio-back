package ma.onee.dti.projectportfolio.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import ma.onee.dti.projectportfolio.dto.ProjetResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ProjetExportService {

    private static final String SHEET_NAME = "Projets";
    private static final String[] HEADERS = {
            "Code",
            "Intitulé",
            "Responsable",
            "Statut",
            "Priorité",
            "Budget prévisionnel",
            "Avancement (%)",
            "Date début prévue",
            "Date fin prévue"
    };

    public byte[] exportProjects(List<ProjetResponse> projets) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            writeHeader(sheet, headerStyle);
            writeRows(sheet, projets, dateStyle);

            for (int columnIndex = 0; columnIndex < HEADERS.length; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de generer l'export Excel des projets", exception);
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle createDateStyle(XSSFWorkbook workbook) {
        CreationHelper creationHelper = workbook.getCreationHelper();
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(creationHelper.createDataFormat().getFormat("dd/mm/yyyy"));
        return style;
    }

    private void writeHeader(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);

        for (int columnIndex = 0; columnIndex < HEADERS.length; columnIndex++) {
            Cell cell = headerRow.createCell(columnIndex);
            cell.setCellValue(HEADERS[columnIndex]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeRows(Sheet sheet, List<ProjetResponse> projets, CellStyle dateStyle) {
        if (projets == null) {
            return;
        }

        for (int rowIndex = 0; rowIndex < projets.size(); rowIndex++) {
            ProjetResponse projet = projets.get(rowIndex);
            Row row = sheet.createRow(rowIndex + 1);

            writeStringCell(row, 0, projet.getCode());
            writeStringCell(row, 1, projet.getIntitule());
            writeStringCell(row, 2, formatResponsable(projet));
            writeStringCell(row, 3, projet.getStatut());
            writeStringCell(row, 4, projet.getPriorite());
            if (projet.getBudgetPrevisionnel() != null) {
                row.createCell(5).setCellValue(projet.getBudgetPrevisionnel().doubleValue());
            }
            if (projet.getPourcentageAvancement() != null) {
                row.createCell(6).setCellValue(projet.getPourcentageAvancement());
            }
            writeDateCell(row, 7, projet.getDateDebutPrevue(), dateStyle);
            writeDateCell(row, 8, projet.getDateFinPrevue(), dateStyle);
        }
    }

    private void writeStringCell(Row row, int columnIndex, String value) {
        if (value != null) {
            row.createCell(columnIndex).setCellValue(value);
        }
    }

    private void writeDateCell(Row row, int columnIndex, LocalDate value, CellStyle dateStyle) {
        if (value != null) {
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(value);
            cell.setCellStyle(dateStyle);
        }
    }

    private String formatResponsable(ProjetResponse projet) {
        String fullName = joinNames(projet.getPrenomResponsable(), projet.getNomResponsable());

        if (!fullName.isBlank()) {
            return fullName;
        }

        return projet.getEmailResponsable();
    }

    private String joinNames(String firstName, String lastName) {
        String normalizedFirstName = firstName != null ? firstName.trim() : "";
        String normalizedLastName = lastName != null ? lastName.trim() : "";

        return (normalizedFirstName + " " + normalizedLastName).trim();
    }
}
