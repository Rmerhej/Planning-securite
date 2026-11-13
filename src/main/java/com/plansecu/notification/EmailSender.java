package com.plansecu.notification;

import com.plansecu.config.AppProperties;
import com.plansecu.domain.StatutNotification;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final AppProperties props;

    public StatutNotification envoyer(String destinataire, String sujet, String corps, boolean html) throws Exception {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!props.getNotifications().isEmailEnabled() || sender == null) {
            log.info("[Email simulé] -> {} : {}", destinataire, sujet);
            return StatutNotification.SIMULE;
        }
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setFrom(props.getNotifications().getEmailFrom(), props.getNom());
        helper.setTo(destinataire);
        helper.setSubject(sujet);
        helper.setText(corps, html);
        sender.send(message);
        return StatutNotification.ENVOYE;
    }
}
