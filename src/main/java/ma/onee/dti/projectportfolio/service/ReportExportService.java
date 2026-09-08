package ma.onee.dti.projectportfolio.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import ma.onee.dti.projectportfolio.dto.ReportCostByTypeResponse;
import ma.onee.dti.projectportfolio.dto.ReportProjectRowResponse;
import ma.onee.dti.projectportfolio.dto.ReportResourceAllocationResponse;
import ma.onee.dti.projectportfolio.dto.ReportSummaryResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ReportExportService {

    private static final String[] PROJECT_HEADERS = {
            "Code",
            "Intitulé",
            "Responsable",
            "Statut",
            "Priorité",
            "Budget prévisionnel (MAD)",
            "Coût consommé (MAD)",
            "Écart budgétaire (MAD)",
            "Avancement (%)",
            "Risques critiques",
            "Ressources affectées"
    };

    private static final String[] COST_HEADERS = {
            "Type de coût",
            "Total (MAD)",
            "Nombre d'entrées"
    };

    private static final String[] RESOURCE_HEADERS = {
            "Nature d'intervention",
            "Charge totale (JH)",
            "Nombre d'affectations"
    };

    public byte[] exportReport(ReportSummaryResponse report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);

            writeProjectSheet(workbook, report.getProjects(), headerStyle);
            writeCostByTypeSheet(workbook, report.getCostByType(), headerStyle);
            writeResourceAllocationSheet(workbook, report.getResourceAllocationByNature(), headerStyle);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de generer l'export Excel du rapport portefeuille", e);
        }
    }

    private void writeProjectSheet(XSSFWorkbook workbook, List<ReportProjectRowResponse> projects, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Synthese projets");
        writeHeader(sheet, PROJECT_HEADERS, headerStyle);

        if (projects == null) {
            return;
        }

        for (int i = 0; i < projects.size(); i++) {
            ReportProjectRowResponse project = projects.get(i);
            Row row = sheet.createRow(i + 1);

            writeStringCell(row, 0, project.getCode());
            writeStringCell(row, 1, project.getIntitule());
            writeStringCell(row, 2, project.getResponsable());
            writeStringCell(row, 3, project.getStatut());
            writeStringCell(row, 4, project.getPriorite());
            writeDecimalCell(row, 5, project.getPlannedBudget());
            writeDecimalCell(row, 6, project.getConsumedCost());
            writeDecimalCell(row, 7, project.getVariance());
            if (project.getProgress() != null) {
                row.createCell(8).setCellValue(project.getProgress());
            }
            row.createCell(9).setCellValue(project.getCriticalRiskCount());
            row.createCell(10).setCellValue(project.getResourceCount());
        }

        autoSize(sheet, PROJECT_HEADERS.length);
    }

    private void writeCostByTypeSheet(XSSFWorkbook workbook, List<ReportCostByTypeResponse> costByType, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Couts par type");
        writeHeader(sheet, COST_HEADERS, headerStyle);

        if (costByType == null) {
            return;
        }

        for (int i = 0; i < costByType.size(); i++) {
            ReportCostByTypeResponse item = costByType.get(i);
            Row row = sheet.createRow(i + 1);

            writeStringCell(row, 0, item.getType());
            writeDecimalCell(row, 1, item.getTotalMontant());
            row.createCell(2).setCellValue(item.getCount());
        }

        autoSize(sheet, COST_HEADERS.length);
    }

    private void writeResourceAllocationSheet(XSSFWorkbook workbook, List<ReportResourceAllocationResponse> allocations, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Affectations ressources");
        writeHeader(sheet, RESOURCE_HEADERS, headerStyle);

        if (allocations == null) {
            return;
        }

        for (int i = 0; i < allocations.size(); i++) {
            ReportResourceAllocationResponse item = allocations.get(i);
            Row row = sheet.createRow(i + 1);

            writeStringCell(row, 0, item.getNatureIntervention());
            writeDecimalCell(row, 1, item.getTotalChargeJH());
            row.createCell(2).setCellValue(item.getCount());
        }

        autoSize(sheet, RESOURCE_HEADERS.length);
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void writeHeader(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeStringCell(Row row, int columnIndex, String value) {
        if (value != null && !value.isBlank()) {
            row.createCell(columnIndex).setCellValue(value);
        }
    }

    private void writeDecimalCell(Row row, int columnIndex, BigDecimal value) {
        if (value != null) {
            row.createCell(columnIndex).setCellValue(value.doubleValue());
        }
    }

    private void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
