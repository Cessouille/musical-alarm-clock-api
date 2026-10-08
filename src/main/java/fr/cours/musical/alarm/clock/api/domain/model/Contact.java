package fr.cours.musical.alarm.clock.api.domain.model;

import java.util.Map;
import java.util.Optional;

public record Contact(Map<ChannelType, String> addresses) {

    public Contact {
        addresses = Map.copyOf(addresses);
    }

    public Optional<String> addressFor(ChannelType channel) {
        return Optional.ofNullable(addresses.get(channel)).filter(address -> !address.isBlank());
    }
}
