package com.plansecu.notification;

import com.plansecu.config.AppProperties;
import com.plansecu.domain.StatutNotification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Envoi réel via l'API SMS transactionnels de Brevo (ex-Sendinblue).
 * Activer avec SMS_PROVIDER=brevo et BREVO_API_KEY=...
 * Pour un autre fournisseur (OVH, Twilio...), créer une autre implémentation de SmsSender.
 */
@Component
@ConditionalOnProperty(name = "app.notifications.sms-provider", havingValue = "brevo")
public class BrevoSmsSender implements SmsSender {

    private final RestClient client;
    private final String expediteur;

    public BrevoSmsSender(AppProperties props) {
        this.expediteur = props.getNotifications().getSmsSender();
        this.client = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", props.getNotifications().getBrevoApiKey())
                .build();
    }

    @Override
    public StatutNotification envoyer(String telephone, String message) {
        client.post()
                .uri("/transactionalSMS/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "sender", expediteur,
                        "recipient", SmsSender.formatInternational(telephone),
                        "content", message,
                        "type", "transactional"))
                .retrieve()
                .toBodilessEntity();
        return StatutNotification.ENVOYE;
    }
}
