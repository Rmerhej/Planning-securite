package com.plansecu.web.agent;

import com.plansecu.domain.TypePointage;
import com.plansecu.service.PointageService;
import com.plansecu.service.PointageService.EtatPointage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Page ouverte quand l'agent scanne le QR code du lieu avec l'appareil photo de son téléphone.
 * Le scan affiche la mission ; un appui sur le bouton enregistre l'arrivée ou le départ.
 */
@Controller
@RequestMapping("/pointage")
@RequiredArgsConstructor
public class PointageController {

    private final PointageService pointageService;
    private final Clock clock;

    @GetMapping("/{token}")
    public String afficher(@PathVariable String token, Authentication auth, Model model) {
        EtatPointage etat = pointageService.preparer(token, auth.getName());
        model.addAttribute("etat", etat);
        model.addAttribute("token", token);
        model.addAttribute("maintenant", LocalDateTime.now(clock));
        return "agent/pointage";
    }

    @PostMapping("/{token}")
    public String pointer(@PathVariable String token, @RequestParam TypePointage type,
                          Authentication auth, RedirectAttributes ra) {
        EtatPointage etat = pointageService.pointer(token, auth.getName(), type);
        if (etat.possible() && etat.message() != null) {
            ra.addFlashAttribute("succes", etat.message());
        }
        return "redirect:/pointage/" + token;
    }
}
