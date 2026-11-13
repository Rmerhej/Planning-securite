package com.plansecu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Paramètres de l'application (préfixe "app" dans application.yml). */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String nom = "PlanSécu";
    private String baseUrl = "http://localhost:8080";
    private Admin admin = new Admin();
    private Notifications notifications = new Notifications();
    private Regles regles = new Regles();
    private Planning planning = new Planning();

    @Data
    public static class Admin {
        private String username = "admin";
        private String password = "admin123";
    }

    @Data
    public static class Notifications {
        private boolean emailEnabled = false;
        private String emailFrom = "planning@plansecu.fr";
        private String smsProvider = "log";
        private String brevoApiKey;
        private String smsSender = "PlanSecu";
    }

    /** Règles légales / conventionnelles contrôlées à la saisie (avertissements). */
    @Data
    public static class Regles {
        private int maxHeuresJour = 12;
        private int maxHeuresSemaine = 48;
        private int reposMinHeures = 11;
        private int alerteCarteProJours = 60;
        /** Plage du travail de nuit (21h-6h par défaut). */
        private int nuitDebut = 21;
        private int nuitFin = 6;
        /** Retard toléré à l'arrivée avant de le signaler (minutes). */
        private int toleranceRetardMinutes = 5;
        /** Combien de temps avant le début de la mission on peut pointer son arrivée (minutes). */
        private int pointageAvanceMinutes = 120;
    }

    @Data
    public static class Planning {
        /** Passe automatiquement les missions passées "planifiées" en "effectuées" chaque nuit. */
        private boolean clotureAuto = true;
    }
}
