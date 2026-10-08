package fr.cours.musical.alarm.clock.api.infra.out.notification.email;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.WakeUpMessage;
import fr.cours.musical.alarm.clock.api.domain.port.out.Notifier;
import fr.cours.musical.alarm.clock.api.infra.out.notification.Recipients;
import fr.cours.musical.alarm.clock.api.infra.out.notification.WakeUpTexts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailNotifier implements Notifier {

    private final EmailClient emailClient;

    @Override
    public ChannelType channel() {
        return ChannelType.EMAIL;
    }

    @Override
    public void send(WakeUpMessage message) {
        emailClient.sendEmail(Recipients.require(message, ChannelType.EMAIL),
                WakeUpTexts.subject(message), WakeUpTexts.body(message));
    }
}
