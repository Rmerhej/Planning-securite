package com.plansecu.service;

import com.plansecu.config.AppConfig;
import com.plansecu.domain.Creneau;
import com.plansecu.domain.Mission;
import com.plansecu.domain.Site;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Génère un calendrier iCalendar (.ics) importable dans Google Agenda, Apple Calendrier, Outlook. */
@Service
public class IcsService {

    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    public String generer(String nomCalendrier, List<Mission> missions) {
        StringBuilder sb = new StringBuilder();
        ligne(sb, "BEGIN:VCALENDAR");
        ligne(sb, "VERSION:2.0");
        ligne(sb, "PRODID:-//PlanSecu//Planning//FR");
        ligne(sb, "CALSCALE:GREGORIAN");
        ligne(sb, "METHOD:PUBLISH");
        ligne(sb, "X-WR-CALNAME:" + echapper(nomCalendrier));
        ligne(sb, "X-WR-TIMEZONE:Europe/Paris");
        String maintenant = UTC.format(Instant.now());

        for (Mission m : missions) {
            if (!m.occupeAgent()) continue;
            Site s = m.getSite();
            int i = 0;
            for (Creneau c : m.getCreneauxTries()) {
                ligne(sb, "BEGIN:VEVENT");
                ligne(sb, "UID:mission-" + m.getId() + "-" + (i++) + "@plansecu");
                ligne(sb, "DTSTAMP:" + maintenant);
                ligne(sb, "DTSTART:" + utc(c.debut(m.getDateMission())));
                ligne(sb, "DTEND:" + utc(c.fin(m.getDateMission())));
                ligne(sb, "SUMMARY:" + echapper("Mission " + s.getNom()));
                ligne(sb, "LOCATION:" + echapper(s.getAdresseComplete()));
                StringBuilder desc = new StringBuilder("Horaires : " + m.getHorairesLibelle());
                if (s.getContactNom() != null) desc.append("\nContact : ").append(s.getContactNom())
                        .append(s.getContactTelephone() == null ? "" : " " + s.getContactTelephone());
                if (s.getConsignes() != null) desc.append("\nConsignes : ").append(s.getConsignes());
                if (m.getCommentaire() != null) desc.append("\n").append(m.getCommentaire());
                ligne(sb, "DESCRIPTION:" + echapper(desc.toString()));
                ligne(sb, "END:VEVENT");
            }
        }
        ligne(sb, "END:VCALENDAR");
        return sb.toString();
    }

    private static String utc(LocalDateTime local) {
        return UTC.format(local.atZone(AppConfig.ZONE).toInstant());
    }

    private static String echapper(String s) {
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
                .replace("\r\n", "\\n").replace("\n", "\\n");
    }

    /** Ajoute une ligne en la repliant à 73 caractères (RFC 5545). */
    private static void ligne(StringBuilder sb, String contenu) {
        int max = 73;
        while (contenu.length() > max) {
            sb.append(contenu, 0, max).append("\r\n ");
            contenu = contenu.substring(max);
        }
        sb.append(contenu).append("\r\n");
    }
}
