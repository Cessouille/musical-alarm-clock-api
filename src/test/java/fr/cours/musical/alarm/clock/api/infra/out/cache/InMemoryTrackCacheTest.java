package fr.cours.musical.alarm.clock.api.infra.out.cache;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTrackCacheTest {

    private final InMemoryTrackCache cache = new InMemoryTrackCache();

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
}
