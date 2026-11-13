package com.plansecu.service;

import com.plansecu.config.AppProperties;
import com.plansecu.domain.Absence;
import com.plansecu.domain.Agent;
import com.plansecu.domain.Creneau;
import com.plansecu.domain.Mission;
import com.plansecu.repository.AbsenceRepository;
import com.plansecu.repository.MissionRepository;
import com.plansecu.service.CalculHeures.Plage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Contrôles avant enregistrement d'une mission :
 * - bloquants : créneaux invalides, double affectation, absence, carte pro expirée, agent inactif ;
 * - avertissements : amplitude journalière, repos quotidien, durée hebdomadaire, carte pro bientôt expirée.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ControleService {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEE dd/MM", Locale.FRENCH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH'h'mm");

    private final MissionRepository missionRepository;
    private final AbsenceRepository absenceRepository;
    private final AppProperties props;

    public ResultatControle controler(Mission m) {
        List<String> erreurs = new ArrayList<>();
        List<String> avert = new ArrayList<>();
        LocalDate date = m.getDateMission();
        Agent agent = m.getAgent();

        // 1. Créneaux
        if (m.getCreneaux().isEmpty()) {
            erreurs.add("Ajoutez au moins un créneau horaire.");
            return new ResultatControle(erreurs, avert);
        }
        for (Creneau c : m.getCreneaux()) {
            if (c.getHeureDebut() == null || c.getHeureFin() == null) {
                erreurs.add("Chaque créneau doit avoir une heure de début et de fin.");
                return new ResultatControle(erreurs, avert);
            }
            if (c.getHeureDebut().equals(c.getHeureFin())) {
                erreurs.add("Le créneau " + c.getLibelle() + " a la même heure de début et de fin.");
            }
        }
        if (!erreurs.isEmpty()) return new ResultatControle(erreurs, avert);

        List<Plage> mesPlages = CalculHeures.plages(m);
        for (int i = 0; i < mesPlages.size(); i++) {
            for (int j = i + 1; j < mesPlages.size(); j++) {
                if (mesPlages.get(i).chevauche(mesPlages.get(j))) {
                    erreurs.add("Deux créneaux de la mission se chevauchent.");
                }
            }
        }
        if (!erreurs.isEmpty() || !m.occupeAgent()) return new ResultatControle(erreurs, avert);

        // 2. Agent
        if (!agent.isActif()) {
            erreurs.add(agent.getNomComplet() + " est désactivé(e).");
        }
        if (!agent.carteProValideLe(date)) {
            erreurs.add("La carte professionnelle de " + agent.getNomComplet() + " a expiré le "
                    + DATE.format(agent.getCarteProExpiration()) + ".");
        } else if (agent.getCarteProExpiration() != null
                && agent.getCarteProExpiration().isBefore(date.plusDays(props.getRegles().getAlerteCarteProJours()))) {
            avert.add("La carte professionnelle de " + agent.getNomComplet() + " expire le "
                    + DATE.format(agent.getCarteProExpiration()) + ".");
        }
        for (Absence a : absenceRepository.chevauchantAgent(agent.getId(), date, date)) {
            erreurs.add(agent.getNomComplet() + " est absent(e) (" + a.getType().getLibelle() + ") du "
                    + DATE.format(a.getDateDebut()) + " au " + DATE.format(a.getDateFin()) + ".");
        }

        // 3. Double affectation (veille, jour, lendemain pour les créneaux de nuit)
        List<Mission> autres = missionRepository
                .findByAgentIdAndDateMissionBetweenOrderByDateMissionAsc(agent.getId(), date.minusDays(1), date.plusDays(1))
                .stream()
                .filter(o -> !Objects.equals(o.getId(), m.getId()) && o.occupeAgent())
                .toList();
        Set<String> conflits = new LinkedHashSet<>();
        for (Mission o : autres) {
            for (Plage p : CalculHeures.plages(o)) {
                for (Plage q : mesPlages) {
                    if (p.chevauche(q)) {
                        conflits.add("Conflit : " + agent.getNomComplet() + " est déjà sur « " + o.getSite().getNom()
                                + " » le " + JOUR.format(p.debut()) + " de " + HEURE.format(p.debut())
                                + " à " + HEURE.format(p.fin()) + ".");
                    }
                }
            }
        }
        erreurs.addAll(conflits);
        if (!erreurs.isEmpty()) return new ResultatControle(erreurs, avert);

        // 4. Amplitude journalière
        AppProperties.Regles r = props.getRegles();
        long minutesJour = m.getDureeMinutes() + autres.stream()
                .filter(o -> o.getDateMission().equals(date))
                .mapToLong(Mission::getDureeMinutes).sum();
        if (minutesJour > r.getMaxHeuresJour() * 60L) {
            avert.add("Durée de travail le " + JOUR.format(date) + " : " + CalculHeures.format(minutesJour)
                    + " (maximum " + r.getMaxHeuresJour() + "h).");
        }

        // 5. Repos quotidien entre deux journées
        List<Mission> toutes = new ArrayList<>(autres);
        toutes.add(m);
        verifierRepos(toutes, date.minusDays(1), date, avert);
        verifierRepos(toutes, date, date.plusDays(1), avert);

        // 6. Durée hebdomadaire
        LocalDate lundi = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long minutesSemaine = m.getDureeMinutes() + missionRepository
                .findByAgentIdAndDateMissionBetweenOrderByDateMissionAsc(agent.getId(), lundi, lundi.plusDays(6))
                .stream()
                .filter(o -> !Objects.equals(o.getId(), m.getId()) && o.occupeAgent())
                .mapToLong(Mission::getDureeMinutes).sum();
        if (minutesSemaine > r.getMaxHeuresSemaine() * 60L) {
            avert.add("Durée sur la semaine du " + JOUR.format(lundi) + " : " + CalculHeures.format(minutesSemaine)
                    + " (maximum " + r.getMaxHeuresSemaine() + "h).");
        }

        return new ResultatControle(erreurs, avert);
    }

    private void verifierRepos(List<Mission> missions, LocalDate jour1, LocalDate jour2, List<String> avert) {
        Optional<LocalDateTime> finJour1 = missions.stream()
                .filter(x -> x.getDateMission().equals(jour1))
                .flatMap(x -> CalculHeures.plages(x).stream())
                .map(Plage::fin).max(Comparator.naturalOrder());
        Optional<LocalDateTime> debutJour2 = missions.stream()
                .filter(x -> x.getDateMission().equals(jour2))
                .flatMap(x -> CalculHeures.plages(x).stream())
                .map(Plage::debut).min(Comparator.naturalOrder());
        if (finJour1.isPresent() && debutJour2.isPresent()) {
            long repos = Duration.between(finJour1.get(), debutJour2.get()).toMinutes();
            int min = props.getRegles().getReposMinHeures();
            if (repos >= 0 && repos < min * 60L) {
                avert.add("Repos de seulement " + CalculHeures.format(repos) + " entre le " + JOUR.format(jour1)
                        + " et le " + JOUR.format(jour2) + " (minimum " + min + "h).");
            }
        }
    }
}
