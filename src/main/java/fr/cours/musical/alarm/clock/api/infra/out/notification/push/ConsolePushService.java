package fr.cours.musical.alarm.clock.api.infra.out.notification.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConsolePushService implements PushService {

    @Override
    public void pushNotification(PushPayload payload) {
        log.info("[MOCK PUSH] token={} title={} body={} data={}",
                payload.deviceToken(), payload.title(), payload.body(), payload.data());
    }
}
