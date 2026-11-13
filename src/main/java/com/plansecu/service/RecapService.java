package com.plansecu.service;

import com.plansecu.domain.*;
import com.plansecu.repository.AbsenceRepository;
import com.plansecu.repository.AgentRepository;
import com.plansecu.repository.MissionRepository;
import com.plansecu.repository.PointageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Récapitulatif mensuel par agent :
 * - heures effectuées (missions au statut "effectuée"), dont nuit / dimanche / férié ;
 * - heures encore planifiées dans le mois ;
 * - jours d'absence (absences déclarées + jours où l'agent était absent sur mission).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecapService {

    private final MissionRepository missionRepository;
    private final AbsenceRepository absenceRepository;
    private final AgentRepository agentRepository;
    private final PointageRepository pointageRepository;
    private final CalculHeures calcul;

    public List<Mission> missionsDuMois(YearMonth mois) {
        return missionRepository.findByDateMissionBetweenOrderByDateMissionAsc(mois.atDay(1), mois.atEndOfMonth());
    }

    public List<RecapAgent> recapMensuel(YearMonth mois) {
        LocalDate debut = mois.atDay(1);
        LocalDate fin = mois.atEndOfMonth();
        Map<Long, List<Mission>> missions = missionsDuMois(mois).stream()
                .collect(Collectors.groupingBy(m -> m.getAgent().getId()));
        Map<Long, List<Absence>> absences = absenceRepository.chevauchant(debut, fin).stream()
                .collect(Collectors.groupingBy(a -> a.getAgent().getId()));
        Map<Long, List<Pointage>> pointages = pointageRepository.entre(debut, fin).stream()
                .collect(Collectors.groupingBy(p -> p.getMission().getId()));

        List<RecapAgent> recap = new ArrayList<>();
        for (Agent agent : agentRepository.findAllByOrderByNomAscPrenomAsc()) {
            List<Mission> ms = missions.getOrDefault(agent.getId(), List.of());
            List<Absence> abs = absences.getOrDefault(agent.getId(), List.of());
            if (!agent.isActif() && ms.isEmpty() && abs.isEmpty()) continue;
            recap.add(calculer(agent, ms, abs, pointages, debut, fin));
        }
        return recap;
    }

    public RecapAgent recapAgent(Agent agent, YearMonth mois) {
        LocalDate debut = mois.atDay(1);
        LocalDate fin = mois.atEndOfMonth();
        List<Mission> missions = missionRepository.findByAgentIdAndDateMissionBetweenOrderByDateMissionAsc(agent.getId(), debut, fin);
        Map<Long, List<Pointage>> pointages = missions.isEmpty() ? Map.of()
                : pointageRepository.deMissions(missions.stream().map(Mission::getId).toList()).stream()
                .collect(Collectors.groupingBy(p -> p.getMission().getId()));
        return calculer(agent, missions, absenceRepository.chevauchantAgent(agent.getId(), debut, fin), pointages, debut, fin);
    }

    private RecapAgent calculer(Agent agent, List<Mission> missions, List<Absence> absences,
                                Map<Long, List<Pointage>> pointages, LocalDate debut, LocalDate fin) {
        long pointees = 0;
        int nonPointees = 0;
        for (Mission m : missions) {
            List<Pointage> ps = pointages.getOrDefault(m.getId(), List.of());
            pointees += PointageService.minutesPointees(ps.stream().sorted(Comparator.comparing(Pointage::getHorodatage)).toList());
            if (m.getStatut() == StatutMission.EFFECTUEE && ps.isEmpty()) nonPointees++;
        }

        long effectuees = 0, nuit = 0, dimanche = 0, ferie = 0, planifiees = 0;
        int nbEffectuees = 0;
        Set<LocalDate> joursTravailles = new HashSet<>();
        Map<String, Long> parSite = new TreeMap<>();

        for (Mission m : missions) {
            switch (m.getStatut()) {
                case EFFECTUEE -> {
                    nbEffectuees++;
                    effectuees += m.getDureeMinutes();
                    nuit += calcul.minutesNuit(m);
                    dimanche += calcul.minutesDimanche(m);
                    ferie += calcul.minutesFerie(m);
                    joursTravailles.add(m.getDateMission());
                    parSite.merge(m.getSite().getNom(), m.getDureeMinutes(), Long::sum);
                }
                case PLANIFIEE -> planifiees += m.getDureeMinutes();
                default -> { }
            }
        }

        // Jours d'absence, sans double comptage
        Map<LocalDate, TypeAbsence> joursAbsence = new TreeMap<>();
        for (Absence a : absences) {
            LocalDate d1 = a.getDateDebut().isBefore(debut) ? debut : a.getDateDebut();
            LocalDate d2 = a.getDateFin().isAfter(fin) ? fin : a.getDateFin();
            for (LocalDate d = d1; !d.isAfter(d2); d = d.plusDays(1)) {
                joursAbsence.putIfAbsent(d, a.getType());
            }
        }
        for (Mission m : missions) {
            if (m.getStatut() == StatutMission.ABSENT) {
                joursAbsence.putIfAbsent(m.getDateMission(), TypeAbsence.INJUSTIFIEE);
            }
        }
        Map<TypeAbsence, Integer> parType = new EnumMap<>(TypeAbsence.class);
        joursAbsence.values().forEach(t -> parType.merge(t, 1, Integer::sum));

        return new RecapAgent(agent, joursTravailles.size(), nbEffectuees, effectuees, nuit, dimanche, ferie,
                planifiees, joursAbsence.size(), parType, parSite, pointees, nonPointees);
    }
}
