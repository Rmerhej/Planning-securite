package com.plansecu.web.admin;

import com.plansecu.config.AppProperties;
import com.plansecu.domain.Agent;
import com.plansecu.domain.Mission;
import com.plansecu.notification.PlanningEnvoiService;
import com.plansecu.repository.AbsenceRepository;
import com.plansecu.repository.AgentRepository;
import com.plansecu.service.AgentService;
import com.plansecu.service.IcsService;
import com.plansecu.service.PlanningService;
import com.plansecu.service.RecapService;
import com.plansecu.web.form.AgentForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentRepository agentRepository;
    private final AbsenceRepository absenceRepository;
    private final AgentService agentService;
    private final PlanningService planningService;
    private final PlanningEnvoiService envoiService;
    private final RecapService recapService;
    private final IcsService icsService;
    private final AppProperties props;
    private final Clock clock;

    @GetMapping
    public String liste(Model model) {
        List<Agent> agents = agentRepository.findAllByOrderByNomAscPrenomAsc();
        Map<Long, String> comptes = new HashMap<>();
        agents.forEach(a -> agentService.identifiant(a.getId()).ifPresent(u -> comptes.put(a.getId(), u)));
        model.addAttribute("agents", agents);
        model.addAttribute("comptes", comptes);
        model.addAttribute("aujourdhui", LocalDate.now(clock));
        model.addAttribute("alerteJours", props.getRegles().getAlerteCarteProJours());
        return "admin/agents/liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("form", new AgentForm());
        return "admin/agents/form";
    }

    @GetMapping("/{id}/modifier")
    public String editer(@PathVariable Long id, Model model) {
        Agent a = agentRepository.findById(id).orElseThrow();
        model.addAttribute("form", AgentForm.depuis(a, agentService.identifiant(id).orElse(null)));
        model.addAttribute("agent", a);
        model.addAttribute("lienIcs", props.getBaseUrl() + "/ical/" + a.getCalendrierToken() + ".ics");
        return "admin/agents/form";
    }

    @PostMapping("/enregistrer")
    public String enregistrer(@Valid @ModelAttribute("form") AgentForm form, BindingResult br,
                              Model model, RedirectAttributes ra) {
        String erreurCompte = agentService.verifierCompte(form);
        if (erreurCompte != null) br.rejectValue("identifiant", "compte", erreurCompte);
        if (br.hasErrors()) {
            if (form.getId() != null) agentRepository.findById(form.getId()).ifPresent(a -> model.addAttribute("agent", a));
            return "admin/agents/form";
        }
        Agent a = agentService.enregistrer(form);
        ra.addFlashAttribute("succes", "Agent " + a.getNomComplet() + " enregistré.");
        return "redirect:/admin/agents/" + a.getId();
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        boolean supprime = agentService.supprimerOuDesactiver(id);
        ra.addFlashAttribute("succes", supprime ? "Agent supprimé."
                : "L'agent a des missions dans l'historique : il a été désactivé (son historique est conservé).");
        return "redirect:/admin/agents";
    }

    @PostMapping("/{id}/nouveau-lien-calendrier")
    public String nouveauLien(@PathVariable Long id, RedirectAttributes ra) {
        agentService.regenererLienCalendrier(id);
        ra.addFlashAttribute("succes", "Nouveau lien d'abonnement généré. L'ancien ne fonctionne plus.");
        return "redirect:/admin/agents/" + id + "/modifier";
    }

    // ---------------------------------------------------------------- planning d'un agent

    @GetMapping("/{id}")
    public String planning(@PathVariable Long id,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois,
                           Model model) {
        Agent agent = agentRepository.findById(id).orElseThrow();
        YearMonth m = mois != null ? mois : YearMonth.now(clock);
        List<Mission> missions = planningService.missionsAgent(id, m.atDay(1), m.atEndOfMonth());
        Map<LocalDate, List<Mission>> parJour = missions.stream()
                .collect(Collectors.groupingBy(Mission::getDateMission, TreeMap::new, Collectors.toList()));

        model.addAttribute("agent", agent);
        model.addAttribute("mois", m);
        model.addAttribute("moisPrecedent", m.minusMonths(1));
        model.addAttribute("moisSuivant", m.plusMonths(1));
        model.addAttribute("parJour", parJour);
        model.addAttribute("recap", recapService.recapAgent(agent, m));
        model.addAttribute("absences", absenceRepository.chevauchantAgent(id, m.atDay(1), m.atEndOfMonth()));
        model.addAttribute("aujourdhui", LocalDate.now(clock));
        model.addAttribute("identifiant", agentService.identifiant(id).orElse(null));
        return "admin/agents/planning";
    }

    @PostMapping("/{id}/envoyer-planning")
    public String envoyerPlanning(@PathVariable Long id, @RequestParam String periode, RedirectAttributes ra) {
        LocalDate aujourdhui = LocalDate.now(clock);
        switch (periode) {
            case "semaine" -> {
                LocalDate lundi = aujourdhui.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                envoiService.envoyerAgent(id, lundi, lundi.plusDays(6), "de la semaine");
            }
            case "semaine-suivante" -> {
                LocalDate lundi = aujourdhui.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
                envoiService.envoyerAgent(id, lundi, lundi.plusDays(6), "de la semaine prochaine");
            }
            default -> {
                YearMonth m = periode.equals("mois-suivant") ? YearMonth.from(aujourdhui).plusMonths(1) : YearMonth.from(aujourdhui);
                envoiService.envoyerAgent(id, m.atDay(1), m.atEndOfMonth(),
                        "de " + m.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)));
            }
        }
        ra.addFlashAttribute("succes", "Planning envoyé à l'agent (voir le journal des notifications).");
        return "redirect:/admin/agents/" + id;
    }

    @GetMapping(value = "/{id}/planning.ics")
    public ResponseEntity<String> ics(@PathVariable Long id) {
        Agent agent = agentRepository.findById(id).orElseThrow();
        LocalDate aujourdhui = LocalDate.now(clock);
        String ics = icsService.generer("Planning " + agent.getNomComplet(),
                planningService.missionsAgent(id, aujourdhui.minusDays(7), aujourdhui.plusMonths(3)));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"planning-" + agent.getNom().toLowerCase() + ".ics\"")
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(ics);
    }
}
