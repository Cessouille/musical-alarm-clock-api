package fr.cours.musical.alarm.clock.api.domain.model;

import java.util.List;
import java.util.stream.Stream;

public record NotificationPolicy(List<ChannelType> fallbackOrder) {

    public NotificationPolicy {
        fallbackOrder = List.copyOf(fallbackOrder);
    }

    public List<ChannelType> channelsToTry(ChannelType preferred) {
        return Stream.concat(Stream.of(preferred), fallbackOrder.stream()).distinct().toList();
    }
}
