package fr.cours.musical.alarm.clock.api.domain.model;

import java.util.Map;

public record UserPreferences(
        String userId,
        Map<WeatherType, String> trackByWeather,
        String fallbackTrack,
        ChannelType preferredChannel
) {

    public UserPreferences {
        trackByWeather = Map.copyOf(trackByWeather);
    }

    public String queryFor(WeatherType weather) {
        return trackByWeather.getOrDefault(weather, fallbackTrack);
    }
}
