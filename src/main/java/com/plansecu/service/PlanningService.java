package com.plansecu.service;

import com.plansecu.domain.Agent;
import com.plansecu.domain.Mission;
import com.plansecu.domain.Site;
import com.plansecu.domain.StatutMission;
import com.plansecu.notification.EvenementMission;
import com.plansecu.notification.MissionResume;
import com.plansecu.repository.AgentRepository;
import com.plansecu.repository.MissionRepository;
import com.plansecu.repository.SiteRepository;
import com.plansecu.web.form.CreneauForm;
import com.plansecu.web.form.MissionForm;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PlanningService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM");
    private static final Comparator<Mission> ORDRE = Comparator
            .comparing(Mission::getDateMission)
            .thenComparing(Mission::getPremiereHeure)
            .thenComparing(m -> m.getAgent().getNom());

    private final MissionRepository missionRepository;
    private final AgentRepository agentRepository;
    private final SiteRepository siteRepository;
    private final ControleService controleService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    // ---------------------------------------------------------------- lecture

    @Transactional(readOnly = true)
    public List<Mission> missionsEntre(LocalDate debut, LocalDate fin) {
        return trier(missionRepository.findByDateMissionBetweenOrderByDateMissionAsc(debut, fin));
    }

    @Transactional(readOnly = true)
    public List<Mission> missionsAgent(Long agentId, LocalDate debut, LocalDate fin) {
        return trier(missionRepository.findByAgentIdAndDateMissionBetweenOrderByDateMissionAsc(agentId, debut, fin));
    }

    @Transactional(readOnly = true)
    public Mission detail(Long id) {
        return missionRepository.findDetailById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mission introuvable : " + id));
    }

    private static List<Mission> trier(List<Mission> missions) {
        return missions.stream().sorted(ORDRE).toList();
    }

    // ---------------------------------------------------------------- écriture

    /** Crée une mission, ou une série si une répétition est demandée. */
    public ResultatEnregistrement creer(MissionForm form) {
        Agent agent = agentRepository.findById(form.getAgentId()).orElseThrow();
        Site site = siteRepository.findById(form.getSiteId()).orElseThrow();
        List<LocalDate> dates = form.datesCibles();
        boolean serie = dates.size() > 1;

        List<Mission> creees = new ArrayList<>();
        Map<LocalDate, List<String>> refus = new LinkedHashMap<>();
        Set<String> avertissements = new LinkedHashSet<>();

        for (LocalDate date : dates) {
            Mission m = nouvelleMission(form, date, agent, site);
            m.setStatut(StatutMission.PLANIFIEE);
            ResultatControle r = controleService.controler(m);
            if (!r.valide()) {
                refus.put(date, r.erreurs().stream().map(e -> serie ? DATE.format(date) + " : " + e : e).toList());
                continue;
            }
            missionRepository.save(m);
            creees.add(m);
            avertissements.addAll(r.avertissements());
        }

        if (!creees.isEmpty()) {
            events.publishEvent(EvenementMission.creees(creees.stream().map(MissionResume::de).toList()));
        }
        return new ResultatEnregistrement(creees, refus, new ArrayList<>(avertissements));
    }

    /** Modifie une mission existante et prévient le ou les agents concernés. */
    public ResultatEnregistrement modifier(Long id, MissionForm form) {
        Mission existante = detail(id);
        Agent agent = agentRepository.findById(form.getAgentId()).orElseThrow();
        Site site = siteRepository.findById(form.getSiteId()).orElseThrow();

        // Contrôle sur une copie : l'entité gérée n'est modifiée que si tout est valide.
        Mission candidat = nouvelleMission(form, form.getDate(), agent, site);
        candidat.setId(id);
        candidat.setStatut(form.getStatut() == null ? existante.getStatut() : form.getStatut());
        ResultatControle r = controleService.controler(candidat);
        if (!r.valide()) {
            return new ResultatEnregistrement(List.of(), Map.of(form.getDate(), r.erreurs()), r.avertissements());
        }

        MissionResume avant = MissionResume.de(existante);
        StatutMission ancienStatut = existante.getStatut();

        existante.setDateMission(candidat.getDateMission());
        existante.setAgent(agent);
        existante.setSite(site);
        existante.setStatut(candidat.getStatut());
        existante.setCommentaire(candidat.getCommentaire());
        existante.getCreneaux().clear();
        candidat.getCreneaux().forEach(c -> existante.ajouterCreneau(c.getHeureDebut(), c.getHeureFin()));
        missionRepository.saveAndFlush(existante);

        MissionResume apres = MissionResume.de(existante);
        publierChangement(avant, apres, ancienStatut, existante.getStatut());
        return new ResultatEnregistrement(List.of(existante), Map.of(), r.avertissements());
    }

    public void changerStatut(Long id, StatutMission statut) {
        Mission m = detail(id);
        StatutMission ancien = m.getStatut();
        if (ancien == statut) return;
        m.setStatut(statut);
        MissionResume resume = MissionResume.de(m);
        publierChangement(resume, resume, ancien, statut);
    }

    public void supprimer(Long id) {
        Mission m = detail(id);
        boolean aVenir = !m.getDateMission().isBefore(LocalDate.now(clock)) && m.getStatut() == StatutMission.PLANIFIEE;
        MissionResume resume = MissionResume.de(m);
        missionRepository.delete(m);
        if (aVenir) {
            events.publishEvent(EvenementMission.annulee(resume));
        }
    }

    /** Clôture automatique : les missions passées encore "planifiées" deviennent "effectuées". */
    public int cloturerMissionsPassees() {
        return missionRepository.changerStatutAvant(StatutMission.PLANIFIEE, StatutMission.EFFECTUEE, LocalDate.now(clock));
    }

    // ---------------------------------------------------------------- interne

    private Mission nouvelleMission(MissionForm form, LocalDate date, Agent agent, Site site) {
        Mission m = new Mission();
        m.setDateMission(date);
        m.setAgent(agent);
        m.setSite(site);
        m.setCommentaire(form.getCommentaire() == null || form.getCommentaire().isBlank() ? null : form.getCommentaire().trim());
        for (CreneauForm c : form.creneauxRemplis()) {
            m.ajouterCreneau(c.getHeureDebut(), c.getHeureFin());
        }
        return m;
    }

    private void publierChangement(MissionResume avant, MissionResume apres, StatutMission ancien, StatutMission nouveau) {
        boolean etaitActive = ancien == StatutMission.PLANIFIEE;
        boolean estActive = nouveau == StatutMission.PLANIFIEE;
        boolean futur = !apres.date().isBefore(LocalDate.now(clock)) || !avant.date().isBefore(LocalDate.now(clock));
        if (!futur) return; // pas de SMS pour une correction a posteriori

        if (etaitActive && !estActive) {
            if (nouveau == StatutMission.ANNULEE) events.publishEvent(EvenementMission.annulee(avant));
            return;
        }
        if (!etaitActive && estActive) {
            events.publishEvent(EvenementMission.creees(List.of(apres)));
            return;
        }
        if (!estActive) return;

        if (!Objects.equals(avant.agentId(), apres.agentId())) {
            events.publishEvent(EvenementMission.annulee(avant));
            events.publishEvent(EvenementMission.creees(List.of(apres)));
        } else if (apres.differeDe(avant)) {
            events.publishEvent(EvenementMission.modifiee(avant, apres));
        }
    }
}
