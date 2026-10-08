package fr.cours.musical.alarm.clock.api.infra.out.notification.push;

public interface PushService {

    void pushNotification(PushPayload payload);
}
