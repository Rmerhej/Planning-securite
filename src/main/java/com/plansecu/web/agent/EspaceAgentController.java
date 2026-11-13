package com.plansecu.web.agent;

import com.plansecu.config.AppProperties;
import com.plansecu.domain.Agent;
import com.plansecu.domain.Mission;
import com.plansecu.repository.AbsenceRepository;
import com.plansecu.repository.UtilisateurRepository;
import com.plansecu.service.IcsService;
import com.plansecu.service.PlanningService;
import com.plansecu.service.PointageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Espace agent (pensé pour le mobile) : planning de la semaine ou du mois. */
@Controller
@RequestMapping("/agent")
@RequiredArgsConstructor
public class EspaceAgentController {

    private final UtilisateurRepository utilisateurRepository;
    private final PlanningService planningService;
    private final AbsenceRepository absenceRepository;
    private final IcsService icsService;
    private final PointageService pointageService;
    private final AppProperties props;
    private final Clock clock;

    private Agent agentConnecte(Authentication auth) {
        return utilisateurRepository.findByUsername(auth.getName())
                .map(u -> u.getAgent())
                .orElseThrow(() -> new IllegalStateException("Compte non rattaché à un agent"));
    }

    @GetMapping
    public String planning(Authentication auth,
                           @RequestParam(defaultValue = "semaine") String vue,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate d,
                           Model model) {
        Agent agent = agentConnecte(auth);
        LocalDate aujourdhui = LocalDate.now(clock);
        LocalDate ref = d != null ? d : aujourdhui;
        boolean mois = vue.equals("mois");

        LocalDate debut, fin, precedent, suivant;
        if (mois) {
            YearMonth ym = YearMonth.from(ref);
            debut = ym.atDay(1);
            fin = ym.atEndOfMonth();
            precedent = debut.minusMonths(1);
            suivant = debut.plusMonths(1);
        } else {
            debut = ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            fin = debut.plusDays(6);
            precedent = debut.minusWeeks(1);
            suivant = debut.plusWeeks(1);
        }

        List<Mission> missions = planningService.missionsAgent(agent.getId(), debut, fin);
        Map<LocalDate, List<Mission>> parJour = missions.stream()
                .collect(Collectors.groupingBy(Mission::getDateMission, TreeMap::new, Collectors.toList()));

        model.addAttribute("agent", agent);
        model.addAttribute("pointages", pointageService.pointagesParMission(missions.stream().map(Mission::getId).toList()));
        model.addAttribute("vue", mois ? "mois" : "semaine");
        model.addAttribute("debut", debut);
        model.addAttribute("fin", fin);
        model.addAttribute("precedent", precedent);
        model.addAttribute("suivant", suivant);
        model.addAttribute("aujourdhui", aujourdhui);
        model.addAttribute("parJour", parJour);
        model.addAttribute("totalMinutes", missions.stream().filter(Mission::occupeAgent).mapToLong(Mission::getDureeMinutes).sum());
        model.addAttribute("nbMissions", missions.stream().filter(Mission::occupeAgent).count());
        model.addAttribute("absences", absenceRepository.chevauchantAgent(agent.getId(), debut, fin));
        model.addAttribute("lienAbonnement", props.getBaseUrl() + "/ical/" + agent.getCalendrierToken() + ".ics");
        return "agent/planning";
    }

    @GetMapping("/planning.ics")
    public ResponseEntity<String> ics(Authentication auth) {
        Agent agent = agentConnecte(auth);
        LocalDate aujourdhui = LocalDate.now(clock);
        String ics = icsService.generer("Mon planning " + props.getNom(),
                planningService.missionsAgent(agent.getId(), aujourdhui.minusDays(7), aujourdhui.plusMonths(3)));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mon-planning.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(ics);
    }
}
