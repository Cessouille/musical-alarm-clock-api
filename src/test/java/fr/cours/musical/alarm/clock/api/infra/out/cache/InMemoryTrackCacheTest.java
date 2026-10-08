package fr.cours.musical.alarm.clock.api.infra.out.cache;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTrackCacheTest {

    private final MutableClock clock = new MutableClock();
    private final InMemoryTrackCache cache = new InMemoryTrackCache(clock, Duration.ofHours(1), 2);

    @Test
    void get_returnsEmpty_whenKeyWasNeverStored() {
        assertThat(cache.get("imagine")).isEmpty();
    }

    @Test
    void get_returnsStoredTrack_afterPut() {
        Track track = new Track("Imagine", "John Lennon");

        cache.put("imagine", track);

        assertThat(cache.get("imagine")).contains(track);
    }

    @Test
    void put_overwritesPreviousValueForTheSameKey() {
        cache.put("imagine", new Track("Imagine", "Old"));
        Track newer = new Track("Imagine", "New");

        cache.put("imagine", newer);

        assertThat(cache.get("imagine")).contains(newer);
    }

    @Test
    void get_keepsKeysIndependent() {
        cache.put("imagine", new Track("Imagine", "John Lennon"));

        assertThat(cache.get("yesterday")).isEmpty();
    }

    @Test
    void get_returnsEmpty_onceTheTtlHasElapsed() {
        cache.put("imagine", new Track("Imagine", "John Lennon"));

        clock.advance(Duration.ofMinutes(59));
        assertThat(cache.get("imagine")).isPresent();

        clock.advance(Duration.ofMinutes(1));
        assertThat(cache.get("imagine")).isEmpty();
    }

    @Test
    void put_evictsTheLeastRecentlyUsedEntry_whenFull() {
        cache.put("a", new Track("A", "x"));
        cache.put("b", new Track("B", "x"));
        cache.get("a");

        cache.put("c", new Track("C", "x"));

        assertThat(cache.get("a")).isPresent();
        assertThat(cache.get("b")).isEmpty();
        assertThat(cache.get("c")).isPresent();
    }
}
