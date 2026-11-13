package com.plansecu.notification;

import com.plansecu.config.AppProperties;
import com.plansecu.domain.Agent;
import com.plansecu.service.PlanningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanningScheduler {

    private final PlanningEnvoiService envoiService;
    private final PlanningService planningService;
    private final AppProperties props;
    private final Clock clock;

    /** Par défaut le dimanche à 18h : planning de la semaine suivante. */
    @Scheduled(cron = "${app.planning.cron-hebdo}", zone = "Europe/Paris")
    public void envoyerPlanningsHebdo() {
        LocalDate lundi = LocalDate.now(clock).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        envoiService.envoyerATous(lundi, lundi.plusDays(6), "de la semaine", Agent::isRecoitHebdo);
    }

    /** Par défaut le 25 à 18h : planning du mois suivant. */
    @Scheduled(cron = "${app.planning.cron-mensuel}", zone = "Europe/Paris")
    public void envoyerPlanningsMensuels() {
        YearMonth mois = YearMonth.now(clock).plusMonths(1);
        String libelle = "de " + mois.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));
        envoiService.envoyerATous(mois.atDay(1), mois.atEndOfMonth(), libelle, Agent::isRecoitMensuel);
    }

    @Scheduled(cron = "${app.planning.cron-cloture}", zone = "Europe/Paris")
    public void cloturer() {
        if (!props.getPlanning().isClotureAuto()) return;
        int n = planningService.cloturerMissionsPassees();
        if (n > 0) log.info("{} mission(s) passée(s) marquée(s) comme effectuée(s)", n);
    }
}
