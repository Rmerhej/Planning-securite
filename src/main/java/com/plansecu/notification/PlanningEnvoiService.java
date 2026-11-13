package com.plansecu.notification;

import com.plansecu.domain.Agent;
import com.plansecu.domain.Mission;
import com.plansecu.repository.AgentRepository;
import com.plansecu.service.PlanningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Envoi des plannings hebdomadaires / mensuels (automatique ou depuis le dashboard). */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlanningEnvoiService {

    private final AgentRepository agentRepository;
    private final PlanningService planningService;
    private final NotificationService notificationService;

    /** @return nombre d'agents prévenus */
    public int envoyerATous(LocalDate debut, LocalDate fin, String periode, Predicate<Agent> filtre) {
        Map<Long, List<Mission>> parAgent = planningService.missionsEntre(debut, fin).stream()
                .collect(Collectors.groupingBy(m -> m.getAgent().getId()));
        int n = 0;
        for (Agent agent : agentRepository.findByActifTrueOrderByNomAscPrenomAsc()) {
            List<Mission> missions = parAgent.getOrDefault(agent.getId(), List.of());
            if (!filtre.test(agent) || missions.isEmpty()) continue;
            notificationService.envoyerPlanning(agent, missions, debut, fin, periode);
            n++;
        }
        log.info("Planning {} envoyé à {} agent(s)", periode, n);
        return n;
    }

    public void envoyerAgent(Long agentId, LocalDate debut, LocalDate fin, String periode) {
        Agent agent = agentRepository.findById(agentId).orElseThrow();
        List<Mission> missions = planningService.missionsAgent(agentId, debut, fin);
        notificationService.envoyerPlanning(agent, missions, debut, fin, periode);
    }
}
