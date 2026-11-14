package com.plansecu.notification;

import com.plansecu.config.AppProperties;
import com.plansecu.domain.*;
import com.plansecu.repository.AgentRepository;
import com.plansecu.repository.NotificationRepository;
import com.plansecu.service.CalculHeures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Compose et envoie les SMS / emails aux agents, et journalise chaque envoi. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_LIGNES_SMS = 6;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM");

    private final SmsSender smsSender;
    private final EmailSender emailSender;
    private final NotificationRepository notificationRepository;
    private final AgentRepository agentRepository;
    private final TemplateEngine templateEngine;
    private final AppProperties props;

    // ------------------------------------------------------------ missions

    public void notifierMission(EvenementMission e) {
        if (e.missions().isEmpty()) return;
        MissionResume ref = e.missions().get(0);
        String titre;
        StringBuilder corps = new StringBuilder();

        switch (e.type()) {
            case CREEE -> {
                titre = e.missions().size() == 1 ? "Nouvelle mission" : e.missions().size() + " nouvelles missions";
                if (e.missions().size() == 1) {
                    corps.append(ref.ligne()).append('\n').append(ref.siteAdresse());
                } else {
                    corps.append(lignes(e.missions()));
                }
            }
            case MODIFIEE -> {
                titre = "Mission modifiée";
                corps.append("Avant : ").append(e.avant().ligne()).append('\n')
                        .append("Maintenant : ").append(ref.ligne()).append('\n').append(ref.siteAdresse());
            }
            case ANNULEE -> {
                titre = "Mission annulée";
                corps.append(ref.ligne());
            }
            default -> throw new IllegalStateException();
        }
        corps.append("\nPlanning : ").append(props.getBaseUrl()).append("/agent");

        String message = props.getNom() + " – " + titre + "\n" + corps;
        envoyer(ref.agentId(), ref.telephone(), ref.email(), ref.notifSms(), ref.notifEmail(),
                props.getNom() + " : " + titre.toLowerCase(), message, null);
    }

    private String lignes(List<MissionResume> missions) {
        List<String> l = missions.stream().map(m -> "• " + m.ligne()).collect(Collectors.toCollection(ArrayList::new));
        if (l.size() > MAX_LIGNES_SMS) {
            int reste = l.size() - (MAX_LIGNES_SMS - 1);
            l = new ArrayList<>(l.subList(0, MAX_LIGNES_SMS - 1));
            l.add("… et " + reste + " autres");
        }
        return String.join("\n", l);
    }

    // ------------------------------------------------------------ planning périodique

    /** Envoie à l'agent son planning sur la période (SMS court + email détaillé). */
    public void envoyerPlanning(Agent agent, List<Mission> missions, LocalDate debut, LocalDate fin, String periode) {
        long minutes = missions.stream().filter(Mission::occupeAgent).mapToLong(Mission::getDureeMinutes).sum();
        long nb = missions.stream().filter(Mission::occupeAgent).count();

        String sms = props.getNom() + " – Planning " + periode + " (" + DATE.format(debut) + " au " + DATE.format(fin) + ")\n"
                + (nb == 0 ? "Aucune mission prévue." : nb + " mission(s), " + CalculHeures.format(minutes) + ".")
                + (agent.isNotifEmail() && agent.getEmail() != null && !agent.getEmail().isBlank() ? " Détail envoyé par email." : "")
                + "\n" + props.getBaseUrl() + "/agent";

        String html = null;
        if (agent.isNotifEmail() && agent.getEmail() != null && !agent.getEmail().isBlank()) {
            Context ctx = new Context(Locale.FRANCE);
            ctx.setVariable("agent", agent);
            ctx.setVariable("missions", missions);
            ctx.setVariable("debut", debut);
            ctx.setVariable("fin", fin);
            ctx.setVariable("periode", periode);
            ctx.setVariable("total", CalculHeures.format(minutes));
            ctx.setVariable("appNom", props.getNom());
            ctx.setVariable("baseUrl", props.getBaseUrl());
            ctx.setVariable("lienIcs", props.getBaseUrl() + "/ical/" + agent.getCalendrierToken() + ".ics");
            html = templateEngine.process("email/planning", ctx);
        }
        envoyer(agent.getId(), agent.getTelephone(), agent.getEmail(), agent.isNotifSms(), agent.isNotifEmail(),
                props.getNom() + " : votre planning " + periode, sms, html);
    }

    // ------------------------------------------------------------ envoi + journal

    private void envoyer(Long agentId, String telephone, String email, boolean viaSms, boolean viaEmail,
                         String sujet, String texte, String html) {
        if (viaSms && telephone != null && !telephone.isBlank()) {
            journaliser(agentId, telephone, CanalNotification.SMS, null, texte,
                    () -> smsSender.envoyer(telephone, texte));
        }
        if (viaEmail && email != null && !email.isBlank()) {
            String corps = html != null ? html : texte;
            journaliser(agentId, email, CanalNotification.EMAIL, sujet, html != null ? texte : corps,
                    () -> emailSender.envoyer(email, sujet, corps, html != null));
        }
    }

    @FunctionalInterface
    private interface Envoi {
        StatutNotification executer() throws Exception;
    }

    private void journaliser(Long agentId, String destinataire, CanalNotification canal, String sujet,
                             String message, Envoi envoi) {
        Notification n = new Notification();
        n.setAgent(agentId == null ? null : agentRepository.getReferenceById(agentId));
        n.setDestinataire(destinataire);
        n.setCanal(canal);
        n.setSujet(sujet);
        n.setMessage(message.length() > 4000 ? message.substring(0, 4000) : message);
        n.setEnvoyeLe(LocalDateTime.now());
        if (props.getDemo().isEnabled()) {
            // Démo publique : jamais d'envoi réel, quelle que soit la configuration
            n.setStatut(StatutNotification.SIMULE);
            notificationRepository.save(n);
            return;
        }
        try {
            n.setStatut(envoi.executer());
        } catch (Exception ex) {
            log.warn("Échec d'envoi {} à {} : {}", canal, destinataire, ex.getMessage());
            n.setStatut(StatutNotification.ECHEC);
            String err = String.valueOf(ex.getMessage());
            n.setErreur(err.length() > 500 ? err.substring(0, 500) : err);
        }
        notificationRepository.save(n);
    }
}
