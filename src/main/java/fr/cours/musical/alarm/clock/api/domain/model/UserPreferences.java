package fr.cours.musical.alarm.clock.api.domain.model;

import java.time.DayOfWeek;
import java.util.Map;
import java.util.Objects;

public record UserPreferences(
        String userId,
        Map<AlarmSlot, String> trackBySlot,
        String fallbackTrack,
        ChannelType preferredChannel,
        Contact contact
) {

    public UserPreferences {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(fallbackTrack, "fallbackTrack is required");
        Objects.requireNonNull(preferredChannel, "preferredChannel is required");
        Objects.requireNonNull(contact, "contact is required");
        trackBySlot = Map.copyOf(trackBySlot);
    }

    public String queryFor(DayOfWeek dayOfWeek, WeatherType weather) {
        return trackBySlot.getOrDefault(new AlarmSlot(dayOfWeek, weather), fallbackTrack);
    }
}
