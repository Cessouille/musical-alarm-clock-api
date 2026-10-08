package fr.cours.musical.alarm.clock.api.domain.port.out;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.WakeUpMessage;

public interface Notifier {

    ChannelType channel();

    void send(WakeUpMessage message);
}
