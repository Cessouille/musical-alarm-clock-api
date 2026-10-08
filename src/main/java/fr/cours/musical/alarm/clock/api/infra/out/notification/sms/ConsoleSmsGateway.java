package fr.cours.musical.alarm.clock.api.infra.out.notification.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class ConsoleSmsGateway implements SmsGateway {

    @Override
    public String sendText(String phoneNumber, String text) {
        String messageId = "sms-" + UUID.randomUUID();
        log.info("[MOCK SMS] id={} to={} text={}", messageId, phoneNumber, text);
        return messageId;
    }
}
