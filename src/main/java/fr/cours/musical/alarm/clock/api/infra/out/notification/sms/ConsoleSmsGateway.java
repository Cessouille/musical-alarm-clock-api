package fr.cours.musical.alarm.clock.api.infra.out.notification.sms;

import fr.cours.musical.alarm.clock.api.infra.out.notification.Recipients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@ConditionalOnProperty(name = "app.notification.mock", havingValue = "true", matchIfMissing = true)
@Component
public class ConsoleSmsGateway implements SmsGateway {

    @Override
    public String sendText(String phoneNumber, String text) {
        String messageId = "sms-" + UUID.randomUUID();
        log.info("[MOCK SMS] id={} to={} text={}", messageId, Recipients.mask(phoneNumber), text);
        return messageId;
    }
}
