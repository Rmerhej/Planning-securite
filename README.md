# PlanSécu — planning des agents de sécurité

Application Spring Boot (Java 21) pour planifier des agents de sécurité sur des lieux de mission
(magasins…), prévenir les agents et produire le récapitulatif mensuel.

Stack : Spring Boot 3.5 · Thymeleaf + Layout Dialect · Bootstrap 5 (thème Bootswatch **Zephyr**) ·
Bootstrap Icons · Google Fonts (Inter) · FullCalendar 6 · Spring Security · Spring Data JPA ·
**MySQL 8** + Flyway · Spring Mail · Apache POI (Excel).

## Demo
https://demo-planning-securite.duckdns.org/


## Fonctionnalités

**Administrateur**
- Dashboard : calendrier jour / semaine / mois / liste (FullCalendar), filtres par agent et par lieu,
  missions du jour avec nom + téléphone de l'agent, absents du jour, alertes cartes pro.
- Missions : date, agent, lieu, **un ou plusieurs créneaux** (horaires discontinus 9h-12h + 15h-18h,
  créneaux de nuit 21h-6h), modèles « Coupure / Journée / Nuit », répétition sur plusieurs jours.
- Contrôles à l'enregistrement :
  - bloquants : double affectation (y compris nuit qui déborde sur le lendemain), agent absent,
    carte pro CNAPS expirée, agent inactif, créneaux qui se chevauchent ;
  - avertissements : > 12h/jour, < 11h de repos entre deux journées, > 48h/semaine, carte pro bientôt expirée.
- Ajout / modification / suppression → **notification automatique de l'agent** (SMS et/ou email) après
  l'enregistrement. Changement d'agent = annulation à l'ancien + nouvelle mission au nouveau.
- Planning de chaque agent par mois, envoi manuel du planning (semaine, semaine prochaine, mois, mois prochain),
  export .ics, impression.
- Agents (nom, prénom, **téléphone**, email, carte pro, couleur, préférences de notification, compte de connexion).
- Lieux de mission (adresse, client, contact sur place, consignes de poste, lien Google Maps).
- Absences (congés, maladie, injustifiée, formation…) avec signalement des missions à réaffecter.
- **Récap mensuel** par agent : jours travaillés, heures effectuées, dont nuit (21h-6h) / dimanche / férié,
  heures encore planifiées, jours d'absence (détail par type) + **export Excel**.
- Journal de toutes les notifications envoyées.

**Agent** (pensé mobile)
- Planning de la semaine ou du mois, adresse cliquable (Maps), téléphone du contact, consignes.
- Lien d'**abonnement calendrier** (Google Agenda / Apple Calendrier) mis à jour automatiquement.

**Pointage par QR code**
- Chaque lieu a son QR code (Lieux → icône QR → Imprimer), à afficher sur place.
- L'agent le scanne avec l'appareil photo de son téléphone, se connecte (une seule fois grâce à « Rester connecté »)
  et appuie sur « Pointer mon arrivée » puis « Pointer mon départ » (à chaque créneau pour une coupure).
- Refus si l'agent n'a pas de mission à ce lieu à ce moment (fenêtre : 2h avant le début → 2h après la fin).
- Page **Pointages** : prévu / réel, retards, arrivées ou départs manquants, départs anticipés, ajout manuel
  (tracé avec le nom de l'admin), suppression. Le dashboard affiche l'état du pointage des missions du jour.
- Récap mensuel et export Excel : heures pointées, écart avec les heures effectuées, missions non pointées.
- Si un code a pu être photographié et diffusé : « Nouveau code » (l'ancien cesse aussitôt de marcher).
- ⚠ `APP_BASE_URL` doit être une adresse joignable par les téléphones (pas `localhost`).

**Automatique**
- Dimanche 18h : planning de la semaine suivante aux agents « hebdo ».
- Le 25 à 18h : planning du mois suivant aux agents « mensuel ».
- Chaque nuit : missions passées « planifiées » → « effectuées » (l'admin marque « Agent absent » si besoin).

## Lancer l'application

### Option 1 — Docker (MySQL + appli)
```bash
docker compose up --build
```
Puis http://localhost:8080 — compte `admin` / `changez-moi` (défini dans `docker-compose.yml`).
Pour des données d'exemple, décommentez `SPRING_PROFILES_ACTIVE: demo`.

### Option 2 — MySQL installé localement
```sql
CREATE DATABASE plansecu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'plansecu'@'%' IDENTIFIED BY 'plansecu';
GRANT ALL ON plansecu.* TO 'plansecu'@'%';
```
```bash
mvn spring-boot:run                                         # base vide
mvn spring-boot:run -Dspring-boot.run.profiles=demo         # avec données d'exemple
```
Le schéma est créé par Flyway (`src/main/resources/db/migration`).

### Option 3 — Essai rapide sans MySQL (H2 en mémoire)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev,demo
```

Comptes de démo : `admin` / `admin123`, agents `jdupont`, `smartin`, `kbenali`, `lpetit`, `mrobert` / `agent1234`.

### Tests
```bash
mvn test
```

## Configuration (variables d'environnement)

| Variable | Rôle | Défaut |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | MySQL | localhost / 3306 / plansecu |
| `ADMIN_USER`, `ADMIN_PASSWORD` | compte admin créé au 1er démarrage | admin / admin123 |
| `APP_BASE_URL` | URL publique (liens dans les SMS / emails / .ics) | http://localhost:8080 |
| `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_FROM` | envoi des emails | désactivé (simulé) |
| `SMS_PROVIDER` | `log` (simulé) ou `brevo` | log |
| `BREVO_API_KEY`, `SMS_SENDER` | SMS via Brevo (expéditeur : 11 caractères max) | – |

Tant que l'email et le SMS ne sont pas configurés, les notifications sont **simulées** : elles apparaissent dans
les logs et dans *Notifications* (statut « Simulé »). Pour un autre fournisseur SMS (OVH, Twilio…), créer une
classe qui implémente `SmsSender`.

Les règles (12h/jour, 48h/semaine, 11h de repos, plage de nuit, délai d'alerte carte pro) et les horaires
d'envoi automatique se règlent dans `application.yml` (section `app`).

## Structure

```
config/        sécurité, propriétés, compte admin, données de démo
domain/        Agent, Site, Mission, Creneau, Absence, Utilisateur, Notification
repository/    Spring Data JPA
service/       PlanningService, ControleService (conflits/règles), CalculHeures, RecapService,
               IcsService, ExportExcelService, AgentService
notification/  SMS / email, envoi périodique, tâches planifiées
web/           contrôleurs admin, espace agent, abonnement iCal
templates/     vues Thymeleaf (layout commun, admin/, agent/, email/)
```

## Pistes pour la suite
- Historique des modifications (Hibernate Envers).
- Planning PDF joint aux emails (OpenPDF).
- Duplication d'une semaine type, gestion des remplaçants.
- Multi-utilisateurs admin (chefs de poste) avec droits par client.

## License and Copyright

Copyright © 2026 Roland Merhej.
All rights reserved.

Source code is publicly available for review.
See COPYRIGHT.md for usage restrictions.
