package fr.cours.musical.alarm.clock.api.infra.out.notification.push;

import fr.cours.musical.alarm.clock.api.infra.out.notification.Recipients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@ConditionalOnProperty(name = "app.notification.mock", havingValue = "true", matchIfMissing = true)
@Component
public class ConsolePushService implements PushService {

    @Override
    public void pushNotification(PushPayload payload) {
        log.info("[MOCK PUSH] token={} title={} body={} data={}",
                Recipients.mask(payload.deviceToken()), payload.title(), payload.body(), payload.data());
    }
}
