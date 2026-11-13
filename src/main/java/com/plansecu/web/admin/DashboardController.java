package com.plansecu.web.admin;

import com.plansecu.config.AppProperties;
import com.plansecu.domain.Mission;
import com.plansecu.repository.AbsenceRepository;
import com.plansecu.repository.AgentRepository;
import com.plansecu.repository.SiteRepository;
import com.plansecu.service.PlanningService;
import com.plansecu.service.PointageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final PlanningService planningService;
    private final PointageService pointageService;
    private final AgentRepository agentRepository;
    private final SiteRepository siteRepository;
    private final AbsenceRepository absenceRepository;
    private final AppProperties props;
    private final Clock clock;

    @GetMapping
    public String dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate d,
                            @RequestParam(required = false) Long agentId,
                            @RequestParam(required = false) Long siteId,
                            @RequestParam(required = false) String vue,
                            Model model) {
        LocalDate aujourdhui = LocalDate.now(clock);
        List<Mission> duJour = planningService.missionsEntre(aujourdhui, aujourdhui);
        LocalDate lundi = aujourdhui.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long minutesSemaine = planningService.missionsEntre(lundi, lundi.plusDays(6)).stream()
                .filter(Mission::occupeAgent).mapToLong(Mission::getDureeMinutes).sum();

        model.addAttribute("aujourdhui", aujourdhui);
        model.addAttribute("missionsDuJour", duJour);
        model.addAttribute("suivis", pointageService.suivre(duJour).stream()
                .collect(Collectors.toMap(s -> s.mission().getId(), s -> s)));
        model.addAttribute("nbAgentsDuJour", duJour.stream().filter(Mission::occupeAgent)
                .map(m -> m.getAgent().getId()).distinct().count());
        model.addAttribute("minutesSemaine", minutesSemaine);
        model.addAttribute("absencesDuJour", absenceRepository.chevauchant(aujourdhui, aujourdhui));
        model.addAttribute("cartesAExpirer",
                agentRepository.cartesProAExpirer(aujourdhui.plusDays(props.getRegles().getAlerteCarteProJours())));
        model.addAttribute("agents", agentRepository.findByActifTrueOrderByNomAscPrenomAsc());
        model.addAttribute("sites", siteRepository.findByActifTrueOrderByNomAsc());
        model.addAttribute("dateInitiale", d == null ? aujourdhui : d);
        model.addAttribute("agentId", agentId);
        model.addAttribute("siteId", siteId);
        model.addAttribute("vue", vue);
        return "admin/dashboard";
    }
}
