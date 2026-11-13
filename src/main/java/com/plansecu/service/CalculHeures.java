package com.plansecu.service;

import com.plansecu.config.AppProperties;
import com.plansecu.domain.Mission;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

/** Calculs d'heures : durées, heures de nuit, dimanche, jours fériés. */
@Component
public class CalculHeures {

    private final int nuitDebut;
    private final int nuitFin;

    public CalculHeures(AppProperties props) {
        this.nuitDebut = props.getRegles().getNuitDebut();
        this.nuitFin = props.getRegles().getNuitFin();
    }

    /** Intervalle de temps [debut, fin). */
    public record Plage(LocalDateTime debut, LocalDateTime fin) {
        public long minutes() {
            return Duration.between(debut, fin).toMinutes();
        }

        public boolean chevauche(Plage o) {
            return debut.isBefore(o.fin) && o.debut.isBefore(fin);
        }

        public long intersection(Plage o) {
            LocalDateTime d = debut.isAfter(o.debut) ? debut : o.debut;
            LocalDateTime f = fin.isBefore(o.fin) ? fin : o.fin;
            return d.isBefore(f) ? Duration.between(d, f).toMinutes() : 0;
        }
    }

    public static List<Plage> plages(Mission m) {
        LocalDate d = m.getDateMission();
        return m.getCreneaux().stream()
                .map(c -> new Plage(c.debut(d), c.fin(d)))
                .toList();
    }

    public long minutesNuit(Plage p) {
        long total = 0;
        for (LocalDate d = p.debut().toLocalDate().minusDays(1); !d.isAfter(p.fin().toLocalDate()); d = d.plusDays(1)) {
            LocalDateTime debutNuit = d.atTime(nuitDebut, 0);
            LocalDateTime finNuit = nuitFin <= nuitDebut ? d.plusDays(1).atTime(nuitFin, 0) : d.atTime(nuitFin, 0);
            total += p.intersection(new Plage(debutNuit, finNuit));
        }
        return total;
    }

    public long minutesDimanche(Plage p) {
        return minutesSelonJour(p, d -> d.getDayOfWeek() == DayOfWeek.SUNDAY);
    }

    public long minutesFerie(Plage p) {
        return minutesSelonJour(p, JoursFeries::estFerie);
    }

    private long minutesSelonJour(Plage p, Predicate<LocalDate> jourConcerne) {
        long total = 0;
        for (LocalDate d = p.debut().toLocalDate(); !d.isAfter(p.fin().toLocalDate()); d = d.plusDays(1)) {
            if (jourConcerne.test(d)) {
                total += p.intersection(new Plage(d.atStartOfDay(), d.plusDays(1).atStartOfDay()));
            }
        }
        return total;
    }

    public long minutesNuit(Mission m) {
        return plages(m).stream().mapToLong(this::minutesNuit).sum();
    }

    public long minutesDimanche(Mission m) {
        return plages(m).stream().mapToLong(this::minutesDimanche).sum();
    }

    public long minutesFerie(Mission m) {
        return plages(m).stream().mapToLong(this::minutesFerie).sum();
    }

    /** 2310 -> "38h30" */
    public static String format(long minutes) {
        return String.format("%dh%02d", minutes / 60, minutes % 60);
    }
}
