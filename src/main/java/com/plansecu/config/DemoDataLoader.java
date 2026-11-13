package com.plansecu.config;

import com.plansecu.domain.*;
import com.plansecu.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Données d'exemple (profil "demo") : 5 agents, 4 magasins, 3 mois de planning.
 * Comptes agents : jdupont, smartin, kbenali, lpetit, mrobert — mot de passe "agent1234".
 */
@Slf4j
@Component
@Profile("demo")
@Order(2)
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    private final AgentRepository agentRepository;
    private final SiteRepository siteRepository;
    private final MissionRepository missionRepository;
    private final AbsenceRepository absenceRepository;
    private final PointageRepository pointageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder encoder;
    private final Clock clock;

    private record Horaire(LocalTime debut, LocalTime fin) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (agentRepository.count() > 0) return;
        LocalDate aujourdhui = LocalDate.now(clock);

        List<Site> sites = List.of(
                site("Carrefour Market Bellecour", "12 rue de la République", "69002", "Lyon", "Carrefour", "M. Garnier", "0478000001",
                        "Ronde parking toutes les heures. Contrôle des sorties caisses."),
                site("Monoprix Part-Dieu", "17 rue du Docteur Bouchut", "69003", "Lyon", "Monoprix", "Mme Leroy", "0478000002",
                        "Poste fixe à l'entrée principale."),
                site("Leroy Merlin Vénissieux", "2 avenue de la Division Leclerc", "69200", "Vénissieux", "Leroy Merlin", null, null,
                        "Surveillance de la zone extérieure (matériaux)."),
                site("Entrepôt Lidl Corbas", "Rue des Frères Lumière", "69960", "Corbas", "Lidl", "Accueil logistique", "0478000004",
                        "Gardiennage de nuit, rondes toutes les 2h, main courante."));

        Agent a1 = agent("Dupont", "Jean", "0612345678", "jean.dupont@example.com", "#2563eb", aujourdhui.plusYears(2));
        Agent a2 = agent("Martin", "Sophie", "0623456789", "sophie.martin@example.com", "#db2777", aujourdhui.plusYears(1));
        Agent a3 = agent("Benali", "Karim", "0634567890", "karim.benali@example.com", "#7c3aed", aujourdhui.plusMonths(18));
        Agent a4 = agent("Petit", "Laura", "0645678901", null, "#059669", aujourdhui.plusDays(30));
        Agent a5 = agent("Robert", "Marc", "0656789012", "marc.robert@example.com", "#ea580c", aujourdhui.plusYears(3));
        List<Agent> agents = List.of(a1, a2, a3, a4, a5);

        compte("jdupont", a1);
        compte("smartin", a2);
        compte("kbenali", a3);
        compte("lpetit", a4);
        compte("mrobert", a5);

        // Horaires types : coupure, journée, nuit, coupure tardive, matin
        List<List<Horaire>> horaires = List.of(
                List.of(h(9, 12), h(15, 18)),
                List.of(h(8, 16)),
                List.of(h(21, 6)),
                List.of(h(10, 14), h(16, 20)),
                List.of(h(6, 14)));
        int[] siteParAgent = {0, 1, 2, 3, 0};

        // Congés de Marc la semaine prochaine
        LocalDate debutConges = aujourdhui.plusDays(7);
        Absence conges = new Absence();
        conges.setAgent(a5);
        conges.setDateDebut(debutConges);
        conges.setDateFin(debutConges.plusDays(4));
        conges.setType(TypeAbsence.CONGE);
        conges.setMotif("Congés payés");
        absenceRepository.save(conges);

        YearMonth moisPrecedent = YearMonth.from(aujourdhui).minusMonths(1);
        LocalDate debut = moisPrecedent.atDay(1);
        LocalDate fin = YearMonth.from(aujourdhui).plusMonths(1).atEndOfMonth();
        int compteur = 0;
        for (LocalDate d = debut; !d.isAfter(fin); d = d.plusDays(1)) {
            for (int i = 0; i < agents.size(); i++) {
                int jour = (int) (d.toEpochDay() % 7);
                if ((jour + i) % 7 >= 5) continue;                 // 2 jours de repos par semaine
                if (i == 4 && conges.couvre(d)) continue;
                Mission m = new Mission();
                m.setDateMission(d);
                m.setAgent(agents.get(i));
                // Les agents de jour alternent entre 2 magasins
                Site s = i == 2 ? sites.get(3) : sites.get((siteParAgent[i] + (jour % 2)) % 3);
                m.setSite(s);
                horaires.get(i).forEach(hh -> m.ajouterCreneau(hh.debut(), hh.fin()));
                m.setStatut(d.isBefore(aujourdhui) ? StatutMission.EFFECTUEE : StatutMission.PLANIFIEE);
                if (d.isBefore(aujourdhui) && ++compteur % 37 == 0) m.setStatut(StatutMission.ABSENT);
                missionRepository.save(m);

                // Pointages des missions passées : petits retards réalistes, quelques oublis
                if (m.getStatut() == StatutMission.EFFECTUEE && compteur % 11 != 0) {
                    int retard = (compteur * 7) % 13;   // 0 à 12 min
                    for (Creneau c : m.getCreneauxTries()) {
                        pointageRepository.save(new Pointage(m, TypePointage.ARRIVEE, c.debut(d).plusMinutes(retard)));
                        pointageRepository.save(new Pointage(m, TypePointage.DEPART, c.fin(d).plusMinutes(compteur % 4)));
                    }
                }
            }
        }
        log.info("Données de démonstration chargées ({} missions). Comptes agents : jdupont / agent1234",
                missionRepository.count());
    }

    private static Horaire h(int debut, int fin) {
        return new Horaire(LocalTime.of(debut, 0), LocalTime.of(fin, 0));
    }

    private Site site(String nom, String adresse, String cp, String ville, String client,
                      String contact, String telContact, String consignes) {
        Site s = new Site();
        s.setNom(nom);
        s.setAdresse(adresse);
        s.setCodePostal(cp);
        s.setVille(ville);
        s.setClient(client);
        s.setContactNom(contact);
        s.setContactTelephone(telContact);
        s.setConsignes(consignes);
        return siteRepository.save(s);
    }

    private Agent agent(String nom, String prenom, String tel, String email, String couleur, LocalDate carte) {
        Agent a = new Agent();
        a.setNom(nom);
        a.setPrenom(prenom);
        a.setTelephone(tel);
        a.setEmail(email);
        a.setCouleur(couleur);
        a.setCarteProNumero("CAR-069-2025-" + (1000 + nom.length() * 37));
        a.setCarteProExpiration(carte);
        a.setNotifEmail(email != null);
        return agentRepository.save(a);
    }

    private void compte(String username, Agent agent) {
        Utilisateur u = new Utilisateur();
        u.setUsername(username);
        u.setPassword(encoder.encode("agent1234"));
        u.setRole(Role.AGENT);
        u.setAgent(agent);
        utilisateurRepository.save(u);
    }
}
