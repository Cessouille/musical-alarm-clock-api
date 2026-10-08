package fr.cours.musical.alarm.clock.api.domain.exception;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;

import java.util.List;

public class AlarmDeliveryException extends RuntimeException {

    public AlarmDeliveryException(String userId, List<ChannelType> attemptedChannels) {
        super("Alarm for user '" + userId + "' could not be delivered; channels attempted: " + attemptedChannels);
    }
}
