package com.plansecu.notification;

import com.plansecu.domain.StatutNotification;

/** Fournisseur d'envoi de SMS. Choisi par app.notifications.sms-provider (log | brevo). */
public interface SmsSender {

    StatutNotification envoyer(String telephone, String message) throws Exception;

    /** "06 12 34 56 78" -> "33612345678" (format international sans +). */
    static String formatInternational(String telephone) {
        String t = telephone == null ? "" : telephone.replaceAll("[^\\d+]", "");
        if (t.startsWith("+")) return t.substring(1);
        if (t.startsWith("00")) return t.substring(2);
        if (t.startsWith("0") && t.length() == 10) return "33" + t.substring(1);
        return t;
    }
}
