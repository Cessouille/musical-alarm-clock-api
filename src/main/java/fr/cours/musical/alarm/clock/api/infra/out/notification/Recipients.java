package fr.cours.musical.alarm.clock.api.infra.out.notification;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.WakeUpMessage;

public final class Recipients {

    private Recipients() {
    }

    public static String require(WakeUpMessage message, ChannelType channel) {
        return message.contact().addressFor(channel).orElseThrow(() -> new IllegalStateException(
                "No " + channel + " address known for user '" + message.userId() + "'"));
    }

    public static String mask(String address) {
        if (address == null || address.length() <= 6) {
            return "***";
        }
        return address.substring(0, 2) + "***" + address.substring(address.length() - 2);
    }
}
