package ma.onee.dsi.projectportfolio.controller;

import java.time.LocalDate;
import ma.onee.dsi.projectportfolio.dto.ReportSummaryResponse;
import ma.onee.dsi.projectportfolio.service.ReportExportService;
import ma.onee.dsi.projectportfolio.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rapports")
public class ReportController {

    private final ReportService reportService;
    private final ReportExportService reportExportService;

    public ReportController(ReportService reportService, ReportExportService reportExportService) {
        this.reportService = reportService;
        this.reportExportService = reportExportService;
    }

    @GetMapping
    public ResponseEntity<ReportSummaryResponse> getReport() {
        return ResponseEntity.ok(reportService.getReport());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport() {
        ReportSummaryResponse report = reportService.getReport();
        byte[] content = reportExportService.exportReport(report);
        String filename = "rapport_portefeuille_" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString()
                )
                .body(content);
    }
}
