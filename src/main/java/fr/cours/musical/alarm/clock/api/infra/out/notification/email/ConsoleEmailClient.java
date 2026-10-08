package fr.cours.musical.alarm.clock.api.infra.out.notification.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConsoleEmailClient implements EmailClient {

    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        log.info("[MOCK EMAIL] to={} subject={} body={}", to, subject, htmlBody);
    }
}
