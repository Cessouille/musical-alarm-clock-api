package fr.cours.musical.alarm.clock.api.domain.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPreferencesTest {

    private static final Contact CONTACT = new Contact(Map.of(ChannelType.EMAIL, "alice@example.invalid", ChannelType.SMS, "+33600000001",
            ChannelType.PUSH, "device-alice"));

    private final UserPreferences withMapping = new UserPreferences("alice",
            Map.of(new AlarmSlot(DayOfWeek.MONDAY, WeatherType.SUN), "Walking on Sunshine",
                    new AlarmSlot(DayOfWeek.MONDAY, WeatherType.RAIN), "Singin' in the Rain",
                    new AlarmSlot(DayOfWeek.TUESDAY, WeatherType.SUN), "Good Day Sunshine"),
            "Here Comes the Sun", ChannelType.EMAIL, CONTACT);

    private final UserPreferences withoutMapping = new UserPreferences("carol",
            Map.of(), "Dancing Queen", ChannelType.PUSH, CONTACT);

    @Test
    void queryFor_returnsTheTrackOfTheDayAndWeather() {
        assertThat(withMapping.queryFor(DayOfWeek.MONDAY, WeatherType.SUN)).isEqualTo("Walking on Sunshine");
    }

    @Test
    void queryFor_differsByWeather_forTheSameDay() {
        assertThat(withMapping.queryFor(DayOfWeek.MONDAY, WeatherType.RAIN)).isEqualTo("Singin' in the Rain");
    }

    @Test
    void queryFor_differsByDay_forTheSameWeather() {
        assertThat(withMapping.queryFor(DayOfWeek.TUESDAY, WeatherType.SUN)).isEqualTo("Good Day Sunshine");
    }

    @Test
    void queryFor_returnsFallbackTrack_whenTheCombinationIsNotMapped() {
        assertThat(withMapping.queryFor(DayOfWeek.WEDNESDAY, WeatherType.SUN)).isEqualTo("Here Comes the Sun");
        assertThat(withMapping.queryFor(DayOfWeek.TUESDAY, WeatherType.RAIN)).isEqualTo("Here Comes the Sun");
    }

    @Test
    void queryFor_returnsFallbackTrack_forEveryCombination_whenUserHasNoMapping() {
        for (DayOfWeek day : DayOfWeek.values()) {
            for (WeatherType weather : WeatherType.values()) {
                assertThat(withoutMapping.queryFor(day, weather)).isEqualTo("Dancing Queen");
            }
        }
    }

    @Test
    void constructor_rejectsMissingMandatoryFields() {
        assertThatThrownBy(() -> new UserPreferences(null, Map.of(), "x", ChannelType.EMAIL, CONTACT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UserPreferences("a", Map.of(), null, ChannelType.EMAIL, CONTACT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UserPreferences("a", Map.of(), "x", null, CONTACT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UserPreferences("a", Map.of(), "x", ChannelType.EMAIL, null))
                .isInstanceOf(NullPointerException.class);
    }
}
