package com.plansecu.config;

import com.plansecu.domain.Role;
import com.plansecu.domain.Utilisateur;
import com.plansecu.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crée le compte administrateur au premier démarrage. */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UtilisateurRepository repo;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    @Override
    public void run(ApplicationArguments args) {
        if (repo.existsByRole(Role.ADMIN)) return;
        Utilisateur admin = new Utilisateur();
        admin.setUsername(props.getAdmin().getUsername());
        admin.setPassword(encoder.encode(props.getAdmin().getPassword()));
        admin.setRole(Role.ADMIN);
        repo.save(admin);
        log.warn("Compte administrateur « {} » créé. Pensez à changer le mot de passe (variable ADMIN_PASSWORD).",
                admin.getUsername());
    }
}
