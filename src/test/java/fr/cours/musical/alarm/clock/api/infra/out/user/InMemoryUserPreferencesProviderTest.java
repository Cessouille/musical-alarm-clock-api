package fr.cours.musical.alarm.clock.api.infra.out.user;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.UserPreferences;
import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryUserPreferencesProviderTest {

    private final InMemoryUserPreferencesProvider provider = new InMemoryUserPreferencesProvider();

    @Test
    void findByUserId_returnsPreferences_forKnownUser() {
        UserPreferences alice = provider.findByUserId("alice").orElseThrow();

        assertThat(alice.preferredChannel()).isEqualTo(ChannelType.EMAIL);
        assertThat(alice.queryFor(DayOfWeek.MONDAY, WeatherType.SUN)).isEqualTo("Walking on Sunshine");
    }

    @Test
    void findByUserId_givesDifferentTracksForDifferentDaysAndWeathers() {
        UserPreferences alice = provider.findByUserId("alice").orElseThrow();

        assertThat(alice.queryFor(DayOfWeek.MONDAY, WeatherType.SUN)).isEqualTo("Walking on Sunshine");
        assertThat(alice.queryFor(DayOfWeek.MONDAY, WeatherType.RAIN)).isEqualTo("Singin' in the Rain");
        assertThat(alice.queryFor(DayOfWeek.TUESDAY, WeatherType.SUN)).isEqualTo("Good Day Sunshine");
        assertThat(alice.queryFor(DayOfWeek.TUESDAY, WeatherType.RAIN)).isEqualTo("Purple Rain");
    }

    @Test
    void findByUserId_returnsFallbackForUnmappedCombination() {
        assertThat(provider.findByUserId("alice").orElseThrow().queryFor(DayOfWeek.SUNDAY, WeatherType.CLOUDY))
                .isEqualTo("Here Comes the Sun");
    }

    @Test
    void findByUserId_coversEachChannel() {
        assertThat(provider.findByUserId("bob").orElseThrow().preferredChannel()).isEqualTo(ChannelType.SMS);
        assertThat(provider.findByUserId("carol").orElseThrow().preferredChannel()).isEqualTo(ChannelType.PUSH);
    }

    @Test
    void findByUserId_returnsEmpty_forUnknownOrNullUser() {
        assertThat(provider.findByUserId("ghost")).isEmpty();
        assertThat(provider.findByUserId(null)).isEmpty();
    }
}
