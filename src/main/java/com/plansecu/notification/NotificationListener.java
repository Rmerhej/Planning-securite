package com.plansecu.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Les notifications partent en arrière-plan, uniquement si l'enregistrement a réussi. */
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void surMission(EvenementMission evenement) {
        notificationService.notifierMission(evenement);
    }
}
