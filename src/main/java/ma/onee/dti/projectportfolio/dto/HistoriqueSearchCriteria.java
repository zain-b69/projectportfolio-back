package ma.onee.dti.projectportfolio.dto;

import java.time.LocalDate;
import ma.onee.dti.projectportfolio.enums.TypeAction;
import org.springframework.format.annotation.DateTimeFormat;

public class HistoriqueSearchCriteria {

    private String search;
    private TypeAction typeAction;
    private Long projetId;
    private Long utilisateurId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public TypeAction getTypeAction() {
        return typeAction;
    }

    public void setTypeAction(TypeAction typeAction) {
        this.typeAction = typeAction;
    }

    public Long getProjetId() {
        return projetId;
    }

    public void setProjetId(Long projetId) {
        this.projetId = projetId;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }
}
