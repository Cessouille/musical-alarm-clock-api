package fr.cours.musical.alarm.clock.api.infra.out.notification.sms;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.WakeUpMessage;
import fr.cours.musical.alarm.clock.api.domain.port.out.Notifier;
import fr.cours.musical.alarm.clock.api.infra.out.notification.Recipients;
import fr.cours.musical.alarm.clock.api.infra.out.notification.WakeUpTexts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmsNotifier implements Notifier {

    private static final int SMS_MAX_LENGTH = 160;

    private final SmsGateway smsGateway;

    @Override
    public ChannelType channel() {
        return ChannelType.SMS;
    }

    @Override
    public void send(WakeUpMessage message) {
        String text = WakeUpTexts.subject(message) + " : " + message.track().title() + " – " + message.track().artist();
        smsGateway.sendText(Recipients.require(message, ChannelType.SMS), truncate(text));
    }

    private static String truncate(String text) {
        if (text.length() <= SMS_MAX_LENGTH) {
            return text;
        }
        int end = Character.isHighSurrogate(text.charAt(SMS_MAX_LENGTH - 1)) ? SMS_MAX_LENGTH - 1 : SMS_MAX_LENGTH;
        return text.substring(0, end);
    }
}
