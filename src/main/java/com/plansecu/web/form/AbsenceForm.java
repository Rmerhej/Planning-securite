package com.plansecu.web.form;

import com.plansecu.domain.Absence;
import com.plansecu.domain.TypeAbsence;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class AbsenceForm {

    private Long id;

    @NotNull(message = "Choisissez un agent")
    private Long agentId;

    @NotNull(message = "Date de début obligatoire")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateDebut;

    @NotNull(message = "Date de fin obligatoire")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFin;

    @NotNull
    private TypeAbsence type = TypeAbsence.CONGE;

    @Size(max = 500)
    private String motif;

    @AssertTrue(message = "La date de fin doit être après la date de début")
    public boolean isPeriodeValide() {
        return dateDebut == null || dateFin == null || !dateFin.isBefore(dateDebut);
    }

    public static AbsenceForm depuis(Absence a) {
        AbsenceForm f = new AbsenceForm();
        f.setId(a.getId());
        f.setAgentId(a.getAgent().getId());
        f.setDateDebut(a.getDateDebut());
        f.setDateFin(a.getDateFin());
        f.setType(a.getType());
        f.setMotif(a.getMotif());
        return f;
    }
}
