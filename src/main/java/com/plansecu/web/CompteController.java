package com.plansecu.web;

import com.plansecu.config.AppProperties;
import com.plansecu.domain.Utilisateur;
import com.plansecu.repository.UtilisateurRepository;
import com.plansecu.web.form.MotDePasseForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Page « Mon compte » : chaque utilisateur (admin ou agent) change son propre mot de passe. */
@Controller
@RequestMapping("/compte")
@RequiredArgsConstructor
public class CompteController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    @GetMapping
    public String compte(Authentication auth, Model model) {
        model.addAttribute("utilisateur", utilisateur(auth));
        model.addAttribute("form", new MotDePasseForm());
        return "compte";
    }

    @PostMapping("/mot-de-passe")
    @Transactional
    public String changerMotDePasse(Authentication auth,
                                    @Valid @ModelAttribute("form") MotDePasseForm form, BindingResult br,
                                    Model model, RedirectAttributes ra) {
        if (props.getDemo().isEnabled()) {
            ra.addFlashAttribute("erreur", "Le changement de mot de passe est désactivé dans la démo.");
            return "redirect:/compte";
        }
        Utilisateur u = utilisateur(auth);
        if (form.getActuel() != null && !form.getActuel().isBlank()
                && !passwordEncoder.matches(form.getActuel(), u.getPassword())) {
            br.rejectValue("actuel", "incorrect", "Mot de passe actuel incorrect");
        }
        if (form.getNouveau() != null && form.getNouveau().equals(form.getActuel())) {
            br.rejectValue("nouveau", "identique", "Le nouveau mot de passe doit être différent de l'actuel");
        }
        if (br.hasErrors()) {
            model.addAttribute("utilisateur", u);
            return "compte";
        }
        u.setPassword(passwordEncoder.encode(form.getNouveau()));
        utilisateurRepository.save(u);
        ra.addFlashAttribute("succes", "Mot de passe modifié.");
        return "redirect:/compte";
    }

    private Utilisateur utilisateur(Authentication auth) {
        return utilisateurRepository.findByUsername(auth.getName()).orElseThrow();
    }
}
