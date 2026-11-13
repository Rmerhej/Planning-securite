package com.plansecu.web.admin;

import com.plansecu.config.AppProperties;
import com.plansecu.domain.Site;
import com.plansecu.repository.MissionRepository;
import com.plansecu.repository.SiteRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteRepository siteRepository;
    private final MissionRepository missionRepository;
    private final AppProperties props;

    /** Le code QR n'est jamais modifiable par le formulaire. */
    @InitBinder("site")
    void binder(WebDataBinder binder) {
        binder.setDisallowedFields("qrToken");
    }

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("sites", siteRepository.findAllByOrderByNomAsc());
        return "admin/sites/liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("site", new Site());
        return "admin/sites/form";
    }

    @GetMapping("/{id}")
    public String editer(@PathVariable Long id, Model model) {
        model.addAttribute("site", siteRepository.findById(id).orElseThrow());
        return "admin/sites/form";
    }

    @PostMapping("/enregistrer")
    public String enregistrer(@Valid @ModelAttribute("site") Site site, BindingResult br, RedirectAttributes ra) {
        if (br.hasErrors()) return "admin/sites/form";
        if (site.getId() != null) {
            // conserver le QR code déjà imprimé
            siteRepository.findById(site.getId()).ifPresent(existant -> site.setQrToken(existant.getQrToken()));
        }
        siteRepository.save(site);
        ra.addFlashAttribute("succes", "Lieu « " + site.getNom() + " » enregistré.");
        return "redirect:/admin/sites";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        Site site = siteRepository.findById(id).orElseThrow();
        if (missionRepository.existsBySiteId(id)) {
            site.setActif(false);
            siteRepository.save(site);
            ra.addFlashAttribute("succes", "Ce lieu a des missions dans l'historique : il a été désactivé.");
        } else {
            siteRepository.delete(site);
            ra.addFlashAttribute("succes", "Lieu supprimé.");
        }
        return "redirect:/admin/sites";
    }

    // ---------------------------------------------------------------- QR code de pointage

    /** Affiche à imprimer pour un lieu, ou pour tous les lieux actifs (id = "tous"). */
    @GetMapping("/{id}/qr")
    public String qr(@PathVariable String id, Model model) {
        List<Site> sites = id.equals("tous")
                ? siteRepository.findByActifTrueOrderByNomAsc()
                : List.of(siteRepository.findById(Long.valueOf(id)).orElseThrow());
        model.addAttribute("sites", sites);
        model.addAttribute("baseUrl", props.getBaseUrl());
        model.addAttribute("baseUrlLocale", props.getBaseUrl().contains("localhost") || props.getBaseUrl().contains("127.0.0.1"));
        return "admin/sites/qr";
    }

    @PostMapping("/{id}/nouveau-qr")
    @Transactional
    public String nouveauQr(@PathVariable Long id, RedirectAttributes ra) {
        siteRepository.findById(id).orElseThrow().regenererQr();
        ra.addFlashAttribute("succes", "Nouveau QR code généré : l'ancien ne fonctionne plus, imprimez et affichez le nouveau.");
        return "redirect:/admin/sites/" + id + "/qr";
    }
}
