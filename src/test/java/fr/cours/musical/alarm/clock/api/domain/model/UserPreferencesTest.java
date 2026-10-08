package fr.cours.musical.alarm.clock.api.domain.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserPreferencesTest {

    private final UserPreferences withMapping = new UserPreferences("alice",
            Map.of(new AlarmSlot(DayOfWeek.MONDAY, WeatherType.SOLEIL), "Walking on Sunshine",
                    new AlarmSlot(DayOfWeek.MONDAY, WeatherType.PLUIE), "Singin' in the Rain",
                    new AlarmSlot(DayOfWeek.TUESDAY, WeatherType.SOLEIL), "Good Day Sunshine"),
            "Here Comes the Sun", ChannelType.EMAIL);

    private final UserPreferences withoutMapping = new UserPreferences("carol",
            Map.of(), "Dancing Queen", ChannelType.PUSH);

    @Test
    void queryFor_returnsTheTrackOfTheDayAndWeather() {
        assertThat(withMapping.queryFor(DayOfWeek.MONDAY, WeatherType.SOLEIL)).isEqualTo("Walking on Sunshine");
    }

    @Test
    void queryFor_differsByWeather_forTheSameDay() {
        assertThat(withMapping.queryFor(DayOfWeek.MONDAY, WeatherType.PLUIE)).isEqualTo("Singin' in the Rain");
    }

    @Test
    void queryFor_differsByDay_forTheSameWeather() {
        assertThat(withMapping.queryFor(DayOfWeek.TUESDAY, WeatherType.SOLEIL)).isEqualTo("Good Day Sunshine");
    }

    @Test
    void queryFor_returnsFallbackTrack_whenTheCombinationIsNotMapped() {
        assertThat(withMapping.queryFor(DayOfWeek.WEDNESDAY, WeatherType.SOLEIL)).isEqualTo("Here Comes the Sun");
        assertThat(withMapping.queryFor(DayOfWeek.TUESDAY, WeatherType.PLUIE)).isEqualTo("Here Comes the Sun");
    }

    @Test
    void queryFor_returnsFallbackTrack_forEveryCombination_whenUserHasNoMapping() {
        for (DayOfWeek day : DayOfWeek.values()) {
            for (WeatherType weather : WeatherType.values()) {
                assertThat(withoutMapping.queryFor(day, weather)).isEqualTo("Dancing Queen");
            }
        }
    }
}
