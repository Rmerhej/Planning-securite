package com.plansecu.web.admin;

import com.plansecu.domain.Mission;
import com.plansecu.domain.StatutMission;
import com.plansecu.repository.AgentRepository;
import com.plansecu.repository.SiteRepository;
import com.plansecu.service.PlanningService;
import com.plansecu.service.ResultatEnregistrement;
import com.plansecu.web.form.CreneauForm;
import com.plansecu.web.form.MissionForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Controller
@RequestMapping("/admin/missions")
@RequiredArgsConstructor
public class MissionController {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM");

    private final PlanningService planningService;
    private final AgentRepository agentRepository;
    private final SiteRepository siteRepository;
    private final Clock clock;

    public record Jour(DayOfWeek valeur, String libelle) {}

    @ModelAttribute
    public void referentiels(Model model) {
        model.addAttribute("agents", agentRepository.findByActifTrueOrderByNomAscPrenomAsc());
        model.addAttribute("sites", siteRepository.findByActifTrueOrderByNomAsc());
        model.addAttribute("statuts", StatutMission.values());
        model.addAttribute("jours", Arrays.stream(DayOfWeek.values())
                .map(j -> new Jour(j, j.getDisplayName(TextStyle.SHORT, Locale.FRENCH)))
                .toList());
    }

    // ---------------------------------------------------------------- création

    @GetMapping("/nouvelle")
    public String nouvelle(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(required = false) Long agentId,
                           @RequestParam(required = false) Long siteId,
                           Model model) {
        MissionForm form = new MissionForm();
        form.setDate(date != null ? date : LocalDate.now(clock));
        form.setAgentId(agentId);
        form.setSiteId(siteId);
        form.getCreneaux().add(new CreneauForm(LocalTime.of(9, 0), LocalTime.of(12, 0)));
        form.getCreneaux().add(new CreneauForm(LocalTime.of(15, 0), LocalTime.of(18, 0)));
        model.addAttribute("form", form);
        return "admin/missions/form";
    }

    @PostMapping("/nouvelle")
    public String creer(@Valid @ModelAttribute("form") MissionForm form, BindingResult br,
                        Model model, RedirectAttributes ra) {
        if (form.creneauxRemplis().isEmpty()) {
            br.reject("creneaux", "Ajoutez au moins un créneau horaire complet.");
        }
        if (br.hasErrors()) {
            completerCreneaux(form);
            return "admin/missions/form";
        }

        ResultatEnregistrement r = planningService.creer(form);
        if (r.aucuneEnregistree()) {
            model.addAttribute("erreursMetier", r.toutesLesErreurs());
            completerCreneaux(form);
            return "admin/missions/form";
        }

        int n = r.missions().size();
        ra.addFlashAttribute("succes", n == 1
                ? "Mission enregistrée. L'agent a été prévenu."
                : n + " missions enregistrées. L'agent a été prévenu.");
        flashAvertissements(ra, r);
        return "redirect:/admin?d=" + form.getDate();
    }

    // ---------------------------------------------------------------- modification

    @GetMapping("/{id}")
    public String editer(@PathVariable Long id, Model model) {
        Mission m = planningService.detail(id);
        model.addAttribute("form", MissionForm.depuis(m));
        model.addAttribute("mission", m);
        return "admin/missions/form";
    }

    @PostMapping("/{id}")
    public String modifier(@PathVariable Long id, @Valid @ModelAttribute("form") MissionForm form, BindingResult br,
                           Model model, RedirectAttributes ra) {
        form.setId(id);
        form.setRepeterJusquau(null);
        if (form.creneauxRemplis().isEmpty()) {
            br.reject("creneaux", "Ajoutez au moins un créneau horaire complet.");
        }
        if (br.hasErrors()) {
            model.addAttribute("mission", planningService.detail(id));
            completerCreneaux(form);
            return "admin/missions/form";
        }
        ResultatEnregistrement r = planningService.modifier(id, form);
        if (r.aucuneEnregistree()) {
            model.addAttribute("erreursMetier", r.toutesLesErreurs());
            model.addAttribute("mission", planningService.detail(id));
            completerCreneaux(form);
            return "admin/missions/form";
        }
        ra.addFlashAttribute("succes", "Mission modifiée.");
        flashAvertissements(ra, r);
        return "redirect:/admin?d=" + form.getDate();
    }

    @PostMapping("/{id}/statut")
    public String statut(@PathVariable Long id, @RequestParam StatutMission statut,
                         @RequestParam(defaultValue = "/admin") String retour, RedirectAttributes ra) {
        planningService.changerStatut(id, statut);
        ra.addFlashAttribute("succes", "Statut mis à jour : " + statut.getLibelle() + ".");
        return "redirect:" + (retour.startsWith("/") && !retour.startsWith("//") ? retour : "/admin");
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        LocalDate date = planningService.detail(id).getDateMission();
        planningService.supprimer(id);
        ra.addFlashAttribute("succes", "Mission supprimée.");
        return "redirect:/admin?d=" + date;
    }

    // ---------------------------------------------------------------- utilitaires

    private void completerCreneaux(MissionForm form) {
        if (form.getCreneaux() == null) form.setCreneaux(new ArrayList<>());
        form.setCreneaux(new ArrayList<>(form.getCreneaux().stream()
                .filter(c -> c.getHeureDebut() != null || c.getHeureFin() != null).toList()));
        if (form.getCreneaux().isEmpty()) form.getCreneaux().add(new CreneauForm());
    }

    private void flashAvertissements(RedirectAttributes ra, ResultatEnregistrement r) {
        List<String> messages = new ArrayList<>(r.avertissements());
        r.erreursParDate().forEach((date, erreurs) -> messages.add("Non créée le " + DATE.format(date) + " : "
                + String.join(" ", erreurs).replaceFirst("^\\d{2}/\\d{2} : ", "")));
        if (!messages.isEmpty()) ra.addFlashAttribute("avertissements", messages);
    }
}
