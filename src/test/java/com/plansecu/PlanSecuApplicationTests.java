package com.plansecu;

import com.plansecu.domain.Agent;
import com.plansecu.domain.Mission;
import com.plansecu.domain.Pointage;
import com.plansecu.domain.Site;
import com.plansecu.domain.TypePointage;
import com.plansecu.service.AgentService;
import com.plansecu.service.PointageService;
import com.plansecu.web.form.AgentForm;
import com.plansecu.repository.AgentRepository;
import com.plansecu.repository.SiteRepository;
import com.plansecu.service.PlanningService;
import com.plansecu.service.ResultatEnregistrement;
import com.plansecu.web.form.CreneauForm;
import com.plansecu.web.form.MissionForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Démarre l'application sur H2 avec les données de démo et vérifie le rendu des pages. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "demo"})
class PlanSecuApplicationTests {

    @Autowired MockMvc mvc;
    @Autowired PlanningService planningService;
    @Autowired AgentRepository agentRepository;
    @Autowired SiteRepository siteRepository;
    @Autowired AgentService agentService;
    @Autowired PointageService pointageService;

    @Test
    void pageDeConnexion() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void pagesAdmin() throws Exception {
        for (String url : new String[]{"/admin", "/admin/agents", "/admin/agents/nouveau", "/admin/agents/1",
                "/admin/agents/1/modifier", "/admin/sites", "/admin/sites/nouveau", "/admin/sites/1",
                "/admin/absences", "/admin/absences/nouvelle", "/admin/recap", "/admin/notifications",
                "/admin/missions/nouvelle", "/admin/missions/1", "/admin/pointages", "/admin/pointages?anomalies=true",
                "/admin/sites/1/qr", "/admin/sites/tous/qr"}) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
        mvc.perform(get("/admin/api/evenements").param("start", "2026-10-26T00:00:00+01:00").param("end", "2026-11-30"))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/recap/export").param("mois", LocalDate.now().toString().substring(0, 7)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("jdupont")
    void espaceAgent() throws Exception {
        mvc.perform(get("/agent")).andExpect(status().isOk());
        mvc.perform(get("/agent").param("vue", "mois")).andExpect(status().isOk());
        mvc.perform(get("/compte")).andExpect(status().isOk());
        mvc.perform(get("/agent/planning.ics")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BEGIN:VCALENDAR")));
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    void conflitDetecte() {
        Agent agent = agentRepository.findAll().get(0);
        Site site = siteRepository.findAll().get(0);
        LocalDate date = LocalDate.now().plusYears(1);

        MissionForm f = form(agent, site, date, 9, 12);
        assertThat(planningService.creer(f).missions()).hasSize(1);

        ResultatEnregistrement conflit = planningService.creer(form(agent, site, date, 11, 14));
        assertThat(conflit.aucuneEnregistree()).isTrue();
        assertThat(conflit.toutesLesErreurs()).anyMatch(e -> e.startsWith("Conflit"));

        // Créneau de nuit la veille qui déborde sur le matin
        ResultatEnregistrement nuit = planningService.creer(form(agent, site, date.minusDays(1), 22, 10));
        assertThat(nuit.aucuneEnregistree()).isTrue();
    }

    @Test
    void pointageParQrCode() throws Exception {
        // Agent dédié au test, avec un compte de connexion
        AgentForm af = new AgentForm();
        af.setNom("Test");
        af.setPrenom("Pointage");
        af.setTelephone("0700000000");
        af.setIdentifiant("tpointage");
        af.setMotDePasse("motdepasse123");
        Agent agent = agentService.enregistrer(af);
        Site site = siteRepository.findAll().get(1);

        // Mission qui a commencé il y a 30 minutes
        LocalDateTime debut = LocalDateTime.now().minusMinutes(30).withSecond(0).withNano(0);
        MissionForm f = new MissionForm();
        f.setAgentId(agent.getId());
        f.setSiteId(site.getId());
        f.setDate(debut.toLocalDate());
        f.getCreneaux().add(new CreneauForm(debut.toLocalTime(), debut.toLocalTime().plusHours(3)));
        assertThat(planningService.creer(f).missions()).hasSize(1);

        PointageService.EtatPointage etat = pointageService.preparer(site.getQrToken(), "tpointage");
        assertThat(etat.possible()).isTrue();
        assertThat(etat.prochain()).isEqualTo(TypePointage.ARRIVEE);

        etat = pointageService.pointer(site.getQrToken(), "tpointage", TypePointage.ARRIVEE);
        assertThat(etat.prochain()).isEqualTo(TypePointage.DEPART);
        assertThat(etat.message()).contains("Retard");

        // Double appui : ignoré
        etat = pointageService.pointer(site.getQrToken(), "tpointage", TypePointage.ARRIVEE);
        assertThat(etat.pointages()).hasSize(1);

        // Mauvais lieu : refusé
        Site autre = siteRepository.findAll().get(2);
        assertThat(pointageService.preparer(autre.getQrToken(), "tpointage").possible()).isFalse();

        // Code inconnu
        assertThat(pointageService.preparer("inconnu", "tpointage").possible()).isFalse();

        // Page vue par l'agent
        mvc.perform(get("/pointage/" + site.getQrToken()).with(user("tpointage").roles("AGENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Pointer mon départ")));
    }

    @Test
    void minutesPointeesAvecPause() {
        Mission m = new Mission();
        LocalDate d = LocalDate.of(2026, 11, 10);
        List<Pointage> ps = List.of(
                new Pointage(m, TypePointage.ARRIVEE, d.atTime(9, 2)),
                new Pointage(m, TypePointage.DEPART, d.atTime(12, 0)),
                new Pointage(m, TypePointage.ARRIVEE, d.atTime(15, 0)),
                new Pointage(m, TypePointage.DEPART, d.atTime(18, 5)));
        assertThat(PointageService.minutesPointees(ps)).isEqualTo(178 + 185);
    }

    private static MissionForm form(Agent agent, Site site, LocalDate date, int debut, int fin) {
        MissionForm f = new MissionForm();
        f.setAgentId(agent.getId());
        f.setSiteId(site.getId());
        f.setDate(date);
        f.getCreneaux().add(new CreneauForm(LocalTime.of(debut, 0), LocalTime.of(fin, 0)));
        return f;
    }
}
