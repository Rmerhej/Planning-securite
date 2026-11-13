package com.plansecu.web.admin;

import com.plansecu.domain.Absence;
import com.plansecu.domain.Creneau;
import com.plansecu.domain.Mission;
import com.plansecu.domain.StatutMission;
import com.plansecu.repository.AbsenceRepository;
import com.plansecu.service.PlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;

/** Flux JSON pour FullCalendar : un événement par créneau, plus les absences en journée entière. */
@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class CalendrierApiController {

    private final PlanningService planningService;
    private final AbsenceRepository absenceRepository;

    @GetMapping("/evenements")
    public List<Map<String, Object>> evenements(@RequestParam String start,
                                                @RequestParam String end,
                                                @RequestParam(required = false) Long agentId,
                                                @RequestParam(required = false) Long siteId) {
        // FullCalendar envoie "2026-10-26T00:00:00+01:00" : seule la date nous intéresse
        LocalDate debut = LocalDate.parse(start.substring(0, 10)).minusDays(1);
        LocalDate fin = LocalDate.parse(end.substring(0, 10));

        List<Mission> missions = agentId != null
                ? planningService.missionsAgent(agentId, debut, fin)
                : planningService.missionsEntre(debut, fin);

        List<Map<String, Object>> evts = new ArrayList<>();
        for (Mission m : missions) {
            if (siteId != null && !m.getSite().getId().equals(siteId)) continue;
            int i = 0;
            for (Creneau c : m.getCreneauxTries()) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("id", m.getId() + "-" + i++);
                e.put("groupId", "m" + m.getId());
                e.put("title", m.getAgent().getNomCourt() + " · " + m.getSite().getNom());
                e.put("start", c.debut(m.getDateMission()));
                e.put("end", c.fin(m.getDateMission()));
                e.put("color", m.getStatut() == StatutMission.PLANIFIEE || m.getStatut() == StatutMission.EFFECTUEE
                        ? m.getAgent().getCouleur() : "#94a3b8");
                e.put("url", "/admin/missions/" + m.getId());
                e.put("classNames", List.of("statut-" + m.getStatut().name().toLowerCase()));
                e.put("extendedProps", Map.of(
                        "tooltip", m.getAgent().getNomEtTelephone() + "\n" + m.getSite().getNom() + " – "
                                + m.getSite().getAdresseComplete() + "\n" + m.getHorairesLibelle()
                                + "\n" + m.getStatut().getLibelle(),
                        "statut", m.getStatut().name()));
                evts.add(e);
            }
        }

        if (siteId == null) {
            List<Absence> absences = agentId != null
                    ? absenceRepository.chevauchantAgent(agentId, debut, fin)
                    : absenceRepository.chevauchant(debut, fin);
            for (Absence a : absences) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("id", "abs-" + a.getId());
                e.put("title", a.getType().getLibelle() + " · " + a.getAgent().getNomCourt());
                e.put("start", a.getDateDebut());
                e.put("end", a.getDateFin().plusDays(1)); // fin exclusive
                e.put("allDay", true);
                e.put("color", "#e2e8f0");
                e.put("textColor", "#334155");
                e.put("url", "/admin/absences/" + a.getId());
                e.put("extendedProps", Map.of("tooltip", a.getAgent().getNomEtTelephone() + "\n" + a.getType().getLibelle()));
                evts.add(e);
            }
        }
        return evts;
    }
}
