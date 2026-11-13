package com.plansecu.notification;

import com.plansecu.domain.Agent;
import com.plansecu.domain.Mission;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Copie figée d'une mission, transmise aux notifications après la fin de la transaction. */
public record MissionResume(
        Long agentId,
        String agentNom,
        String telephone,
        String email,
        boolean notifSms,
        boolean notifEmail,
        LocalDate date,
        String siteNom,
        String siteAdresse,
        String horaires) {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEE dd/MM", Locale.FRENCH);

    public static MissionResume de(Mission m) {
        Agent a = m.getAgent();
        return new MissionResume(a.getId(), a.getNomComplet(), a.getTelephone(), a.getEmail(),
                a.isNotifSms(), a.isNotifEmail(), m.getDateMission(),
                m.getSite().getNom(), m.getSite().getAdresseComplete(), m.getHorairesLibelle());
    }

    /** "Lun. 03/11 · Carrefour Market · 09h00–12h00 / 15h00–18h00" */
    public String ligne() {
        String jour = JOUR.format(date);
        return Character.toUpperCase(jour.charAt(0)) + jour.substring(1) + " · " + siteNom + " · " + horaires;
    }

    /** Ce qui intéresse l'agent a-t-il changé ? */
    public boolean differeDe(MissionResume autre) {
        return !date.equals(autre.date) || !siteNom.equals(autre.siteNom)
                || !siteAdresse.equals(autre.siteAdresse) || !horaires.equals(autre.horaires);
    }
}
