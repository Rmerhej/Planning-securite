package com.plansecu.web.form;

import com.plansecu.domain.Creneau;
import com.plansecu.domain.Mission;
import com.plansecu.domain.StatutMission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Data
public class MissionForm {

    /** Nombre maximum de dates créées en une fois par la répétition. */
    public static final int MAX_REPETITIONS = 93;

    private Long id;

    @NotNull(message = "La date est obligatoire")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @NotNull(message = "Choisissez un agent")
    private Long agentId;

    @NotNull(message = "Choisissez un lieu de mission")
    private Long siteId;

    private StatutMission statut = StatutMission.PLANIFIEE;

    @Size(max = 500)
    private String commentaire;

    @Valid
    private List<CreneauForm> creneaux = new ArrayList<>();

    /** Répétition (création uniquement) : jusqu'à cette date, sur les jours cochés. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate repeterJusquau;

    private Set<DayOfWeek> joursRepetition = new HashSet<>();

    public List<CreneauForm> creneauxRemplis() {
        return creneaux == null ? List.of() : creneaux.stream().filter(CreneauForm::estRempli).toList();
    }

    public List<LocalDate> datesCibles() {
        if (repeterJusquau == null || !repeterJusquau.isAfter(date)) {
            return List.of(date);
        }
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = date; !d.isAfter(repeterJusquau) && dates.size() < MAX_REPETITIONS; d = d.plusDays(1)) {
            if (d.equals(date) || joursRepetition.isEmpty() || joursRepetition.contains(d.getDayOfWeek())) {
                dates.add(d);
            }
        }
        return dates;
    }

    public static MissionForm depuis(Mission m) {
        MissionForm f = new MissionForm();
        f.setId(m.getId());
        f.setDate(m.getDateMission());
        f.setAgentId(m.getAgent().getId());
        f.setSiteId(m.getSite().getId());
        f.setStatut(m.getStatut());
        f.setCommentaire(m.getCommentaire());
        for (Creneau c : m.getCreneauxTries()) {
            f.getCreneaux().add(new CreneauForm(c.getHeureDebut(), c.getHeureFin()));
        }
        return f;
    }
}
