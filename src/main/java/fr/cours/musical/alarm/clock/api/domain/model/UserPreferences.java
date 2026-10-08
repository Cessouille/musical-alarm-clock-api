package fr.cours.musical.alarm.clock.api.domain.model;

import java.time.DayOfWeek;
import java.util.Map;

public record UserPreferences(
        String userId,
        Map<AlarmSlot, String> trackBySlot,
        String fallbackTrack,
        ChannelType preferredChannel
) {

    public UserPreferences {
        trackBySlot = Map.copyOf(trackBySlot);
    }

    /** The track chosen for this exact day and weather, else the user's fallback track. */
    public String queryFor(DayOfWeek dayOfWeek, WeatherType weather) {
        return trackBySlot.getOrDefault(new AlarmSlot(dayOfWeek, weather), fallbackTrack);
    }
}
