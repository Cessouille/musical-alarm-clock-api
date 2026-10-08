package fr.cours.musical.alarm.clock.api.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserPreferencesTest {

    private final UserPreferences withMapping = new UserPreferences("alice",
            Map.of(WeatherType.SOLEIL, "Walking on Sunshine"), "Here Comes the Sun", ChannelType.EMAIL);

    private final UserPreferences withoutMapping = new UserPreferences("carol",
            Map.of(), "Dancing Queen", ChannelType.PUSH);

    @Test
    void queryFor_returnsWeatherSpecificTrack_whenWeatherIsMapped() {
        assertThat(withMapping.queryFor(WeatherType.SOLEIL)).isEqualTo("Walking on Sunshine");
    }

    @Test
    void queryFor_returnsFallbackTrack_whenWeatherIsNotMapped() {
        assertThat(withMapping.queryFor(WeatherType.NEIGE)).isEqualTo("Here Comes the Sun");
    }

    @Test
    void queryFor_returnsFallbackTrack_forEveryWeather_whenUserHasNoMapping() {
        for (WeatherType weather : WeatherType.values()) {
            assertThat(withoutMapping.queryFor(weather)).isEqualTo("Dancing Queen");
        }
    }
}
