package fr.cours.musical.alarm.clock.api.infra.out.user;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.UserPreferences;
import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryUserPreferencesProviderTest {

    private final InMemoryUserPreferencesProvider provider = new InMemoryUserPreferencesProvider();

    @Test
    void findByUserId_returnsPreferences_forKnownUser() {
        UserPreferences alice = provider.findByUserId("alice").orElseThrow();

        assertThat(alice.preferredChannel()).isEqualTo(ChannelType.EMAIL);
        assertThat(alice.queryFor(WeatherType.SOLEIL)).isEqualTo("Walking on Sunshine");
    }

    @Test
    void findByUserId_returnsFallbackForUnmappedWeather() {
        assertThat(provider.findByUserId("alice").orElseThrow().queryFor(WeatherType.NUAGEUX))
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
