package fr.cours.musical.alarm.clock.api.infra.out.notification.email;

import fr.cours.musical.alarm.clock.api.infra.out.notification.Recipients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@ConditionalOnProperty(name = "app.notification.mock", havingValue = "true", matchIfMissing = true)
@Component
public class ConsoleEmailClient implements EmailClient {

    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        log.info("[MOCK EMAIL] to={} subject={} body={}", Recipients.mask(to), subject, htmlBody);
    }
}
