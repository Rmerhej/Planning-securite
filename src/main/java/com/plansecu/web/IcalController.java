package com.plansecu.web;

import com.plansecu.config.AppProperties;
import com.plansecu.domain.Agent;
import com.plansecu.repository.AgentRepository;
import com.plansecu.service.IcsService;
import com.plansecu.service.PlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Abonnement calendrier : l'agent ajoute cette URL secrète dans Google Agenda / Apple Calendrier,
 * et son planning se met à jour tout seul sur son téléphone.
 */
@RestController
@RequiredArgsConstructor
public class IcalController {

    private final AgentRepository agentRepository;
    private final PlanningService planningService;
    private final IcsService icsService;
    private final AppProperties props;
    private final Clock clock;

    @GetMapping("/ical/{token}.ics")
    public ResponseEntity<String> abonnement(@PathVariable String token) {
        Agent agent = agentRepository.findByCalendrierToken(token).filter(Agent::isActif).orElse(null);
        if (agent == null) return ResponseEntity.notFound().build();
        LocalDate aujourdhui = LocalDate.now(clock);
        String ics = icsService.generer(props.getNom() + " – " + agent.getPrenom(),
                planningService.missionsAgent(agent.getId(), aujourdhui.minusDays(30), aujourdhui.plusMonths(3)));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(ics);
    }
}
