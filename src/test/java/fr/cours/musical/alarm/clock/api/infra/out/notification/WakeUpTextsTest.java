package fr.cours.musical.alarm.clock.api.infra.out.notification;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.model.WakeUpMessage;
import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WakeUpTextsTest {

    private static final WakeUpMessage MESSAGE = new WakeUpMessage("alice", DayOfWeek.MONDAY,
            WeatherType.SUN, new Track("Walking on Sunshine", "Katrina & The Waves"));

    @Test
    void dayName_isInFrench() {
        assertThat(WakeUpTexts.dayName(DayOfWeek.MONDAY)).isEqualTo("lundi");
    }

    @Test
    void subject_mentionsTheDay() {
        assertThat(WakeUpTexts.subject(MESSAGE)).contains("lundi");
    }

    @Test
    void body_mentionsTrackArtistDayAndWeather() {
        assertThat(WakeUpTexts.body(MESSAGE))
                .contains("Walking on Sunshine", "Katrina & The Waves", "lundi", "soleil");
    }

    @Test
    void body_describesEveryWeatherInFrench() {
        Map<WeatherType, String> expected = Map.of(
                WeatherType.SUN, "soleil", WeatherType.RAIN, "pluie",
                WeatherType.SNOW, "neige", WeatherType.CLOUDY, "nuageux");

        expected.forEach((weather, label) -> assertThat(WakeUpTexts.body(
                new WakeUpMessage("alice", DayOfWeek.MONDAY, weather, MESSAGE.track()))).contains("météo : " + label));
    }
}
