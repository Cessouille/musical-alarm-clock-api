package fr.cours.musical.alarm.clock.api.infra.out.notification.push;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.WakeUpMessage;
import fr.cours.musical.alarm.clock.api.domain.port.out.Notifier;
import fr.cours.musical.alarm.clock.api.infra.out.notification.WakeUpTexts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PushNotifier implements Notifier {

    private final PushService pushService;

    @Override
    public ChannelType channel() {
        return ChannelType.PUSH;
    }

    @Override
    public void send(WakeUpMessage message) {
        pushService.pushNotification(new PushPayload("device-" + message.userId(),
                WakeUpTexts.subject(message),
                message.track().title() + " – " + message.track().artist(),
                Map.of("weather", message.weather().name(), "day", message.dayOfWeek().name())));
    }
}
