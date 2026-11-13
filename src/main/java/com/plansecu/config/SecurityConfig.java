package com.plansecu.config;

import com.plansecu.repository.UtilisateurRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/ical/**").permitAll()          // abonnement calendrier par lien secret
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/agent/**", "/pointage/**").hasRole("AGENT")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)   // après connexion, retour à la page demandée (ex. QR scanné)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?deconnecte")
                        .permitAll())
                .rememberMe(rm -> rm
                        .key("plansecu-remember-me")
                        .tokenValiditySeconds(30 * 24 * 3600));
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UtilisateurRepository repo) {
        return username -> repo.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPassword())
                        .roles(u.getRole().name())
                        .disabled(!u.isActif())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
