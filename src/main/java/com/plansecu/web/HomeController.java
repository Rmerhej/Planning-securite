package com.plansecu.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** Redirige vers le dashboard admin ou l'espace agent selon le rôle. */
    @GetMapping("/")
    public String accueil(Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return admin ? "redirect:/admin" : "redirect:/agent";
    }
}
