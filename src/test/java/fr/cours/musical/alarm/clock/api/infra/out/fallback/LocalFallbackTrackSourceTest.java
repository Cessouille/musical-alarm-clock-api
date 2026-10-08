package fr.cours.musical.alarm.clock.api.infra.out.fallback;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFallbackTrackSourceTest {

    private final LocalFallbackTrackSource source = new LocalFallbackTrackSource();

    @Test
    void trackFor_returnsATrackForEveryDayOfTheWeek() {
        for (DayOfWeek day : DayOfWeek.values()) {
            assertThat(source.trackFor(day)).isNotNull();
        }
    }

    @Test
    void trackFor_isDeterministic() {
        assertThat(source.trackFor(DayOfWeek.MONDAY)).isEqualTo(source.trackFor(DayOfWeek.MONDAY));
    }

    @Test
    void trackFor_givesADifferentTrackEachDay() {
        Set<Track> tracks = Arrays.stream(DayOfWeek.values()).map(source::trackFor).collect(Collectors.toSet());

        assertThat(tracks).hasSize(7);
    }
}
