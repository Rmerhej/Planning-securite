package com.plansecu.config;

import com.plansecu.service.CalculHeures;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/** Utilitaires de format accessibles dans les vues : ${@fmt.heures(minutes)}. */
@Component("fmt")
public class FormatHelper {

    private static final DateTimeFormatter JOUR_LONG = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);
    private static final DateTimeFormatter JOUR_COURT = DateTimeFormatter.ofPattern("EEE dd/MM", Locale.FRENCH);
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    public String heures(long minutes) {
        return CalculHeures.format(minutes);
    }

    /** "Lundi 3 novembre" */
    public String jourLong(LocalDate d) {
        return majuscule(JOUR_LONG.format(d));
    }

    /** "Lun. 03/11" */
    public String jourCourt(LocalDate d) {
        return majuscule(JOUR_COURT.format(d));
    }

    /** "Novembre 2026" */
    public String mois(TemporalAccessor t) {
        return majuscule(MOIS.format(t));
    }

    /** Valeur pour un champ HTML datetime-local : "2026-11-10T09:00" */
    public String dateTimeLocal(java.time.LocalDateTime dt) {
        return dt.withSecond(0).withNano(0).toString().substring(0, 16);
    }

    /** "+0h15" / "-1h30" */
    public String ecart(long minutes) {
        return (minutes < 0 ? "-" : "+") + CalculHeures.format(Math.abs(minutes));
    }

    public String telLien(String telephone) {
        return "tel:" + (telephone == null ? "" : telephone.replaceAll("[^\\d+]", ""));
    }

    private static String majuscule(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
