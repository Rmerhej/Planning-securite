package com.plansecu.notification;

import com.plansecu.domain.StatutNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Mode développement : le SMS est seulement écrit dans les logs et le journal des notifications. */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.notifications.sms-provider", havingValue = "log", matchIfMissing = true)
public class LogSmsSender implements SmsSender {

    @Override
    public StatutNotification envoyer(String telephone, String message) {
        log.info("[SMS simulé] -> {} :\n{}", telephone, message);
        return StatutNotification.SIMULE;
    }
}
