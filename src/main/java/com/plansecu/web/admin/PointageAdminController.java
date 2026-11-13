package com.plansecu.web.admin;

import com.plansecu.domain.Mission;
import com.plansecu.domain.TypePointage;
import com.plansecu.service.PlanningService;
import com.plansecu.service.PointageService;
import com.plansecu.service.SuiviPointage;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Suivi des pointages d'une journée : prévu / réel, retards, oublis, saisie manuelle. */
@Controller
@RequestMapping("/admin/pointages")
@RequiredArgsConstructor
public class PointageAdminController {

    private final PlanningService planningService;
    private final PointageService pointageService;
    private final Clock clock;

    @GetMapping
    public String jour(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate d,
                       @RequestParam(defaultValue = "false") boolean anomalies,
                       Model model) {
        LocalDate date = d != null ? d : LocalDate.now(clock);
        List<Mission> missions = planningService.missionsEntre(date, date);
        List<SuiviPointage> suivis = pointageService.suivre(missions);
        List<SuiviPointage> affiches = anomalies
                ? suivis.stream().filter(s -> s.etat() == SuiviPointage.Etat.ANOMALIE || s.etat() == SuiviPointage.Etat.NON_POINTEE).toList()
                : suivis;

        model.addAttribute("date", date);
        model.addAttribute("veille", date.minusDays(1));
        model.addAttribute("lendemain", date.plusDays(1));
        model.addAttribute("suivis", affiches);
        model.addAttribute("seulementAnomalies", anomalies);
        model.addAttribute("nbAnomalies", suivis.stream()
                .filter(s -> s.etat() == SuiviPointage.Etat.ANOMALIE || s.etat() == SuiviPointage.Etat.NON_POINTEE).count());
        model.addAttribute("nbEnPoste", suivis.stream().filter(s -> s.etat() == SuiviPointage.Etat.EN_COURS).count());
        model.addAttribute("types", TypePointage.values());
        return "admin/pointages";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam Long missionId,
                          @RequestParam TypePointage type,
                          @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime horodatage,
                          @RequestParam(required = false) String commentaire,
                          Authentication auth, RedirectAttributes ra) {
        pointageService.ajouterManuel(missionId, type, horodatage, auth.getName(), commentaire);
        ra.addFlashAttribute("succes", type.getLibelle() + " ajouté(e) manuellement.");
        return "redirect:/admin/pointages?d=" + planningService.detail(missionId).getDateMission();
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        LocalDate date = pointageService.supprimer(id);
        ra.addFlashAttribute("succes", "Pointage supprimé.");
        return "redirect:/admin/pointages?d=" + date;
    }
}
