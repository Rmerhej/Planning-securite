package com.plansecu.web.admin;

import com.plansecu.domain.Absence;
import com.plansecu.domain.Mission;
import com.plansecu.domain.TypeAbsence;
import com.plansecu.repository.AbsenceRepository;
import com.plansecu.repository.AgentRepository;
import com.plansecu.service.PlanningService;
import com.plansecu.web.form.AbsenceForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/absences")
@RequiredArgsConstructor
public class AbsenceController {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM");

    private final AbsenceRepository absenceRepository;
    private final AgentRepository agentRepository;
    private final PlanningService planningService;
    private final Clock clock;

    @ModelAttribute
    public void referentiels(Model model) {
        model.addAttribute("agents", agentRepository.findAllByOrderByNomAscPrenomAsc());
        model.addAttribute("types", TypeAbsence.values());
    }

    @GetMapping
    public String liste(Model model) {
        LocalDate aujourdhui = LocalDate.now(clock);
        model.addAttribute("absences", absenceRepository.chevauchant(aujourdhui.minusMonths(2), aujourdhui.plusYears(1)));
        model.addAttribute("aujourdhui", aujourdhui);
        return "admin/absences/liste";
    }

    @GetMapping("/nouvelle")
    public String nouvelle(@RequestParam(required = false) Long agentId, Model model) {
        AbsenceForm f = new AbsenceForm();
        f.setAgentId(agentId);
        f.setDateDebut(LocalDate.now(clock));
        f.setDateFin(LocalDate.now(clock));
        model.addAttribute("form", f);
        return "admin/absences/form";
    }

    @GetMapping("/{id}")
    public String editer(@PathVariable Long id, Model model) {
        model.addAttribute("form", AbsenceForm.depuis(absenceRepository.findById(id).orElseThrow()));
        return "admin/absences/form";
    }

    @PostMapping("/enregistrer")
    @Transactional
    public String enregistrer(@Valid @ModelAttribute("form") AbsenceForm form, BindingResult br, RedirectAttributes ra) {
        if (br.hasErrors()) return "admin/absences/form";
        Absence a = form.getId() == null ? new Absence() : absenceRepository.findById(form.getId()).orElseThrow();
        a.setAgent(agentRepository.findById(form.getAgentId()).orElseThrow());
        a.setDateDebut(form.getDateDebut());
        a.setDateFin(form.getDateFin());
        a.setType(form.getType());
        a.setMotif(form.getMotif() == null || form.getMotif().isBlank() ? null : form.getMotif().trim());
        absenceRepository.save(a);
        ra.addFlashAttribute("succes", "Absence enregistrée.");

        // Missions déjà planifiées pendant l'absence : à réaffecter
        List<Mission> aReaffecter = planningService.missionsAgent(form.getAgentId(), form.getDateDebut(), form.getDateFin())
                .stream().filter(Mission::occupeAgent).toList();
        if (!aReaffecter.isEmpty()) {
            ra.addFlashAttribute("avertissements", aReaffecter.stream()
                    .map(m -> "Mission à réaffecter le " + DATE.format(m.getDateMission()) + " : "
                            + m.getSite().getNom() + " (" + m.getHorairesLibelle() + ")")
                    .toList());
            ra.addFlashAttribute("missionsAReaffecter", aReaffecter.stream().map(Mission::getId).toList());
        }
        return "redirect:/admin/absences";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        absenceRepository.deleteById(id);
        ra.addFlashAttribute("succes", "Absence supprimée.");
        return "redirect:/admin/absences";
    }
}
