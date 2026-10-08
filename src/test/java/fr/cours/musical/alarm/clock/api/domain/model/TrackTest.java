package fr.cours.musical.alarm.clock.api.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrackTest {

    @Test
    void constructor_trimsTitleAndArtist() {
        Track track = new Track("  Walking on Sunshine ", " Katrina & The Waves ");

        assertThat(track.title()).isEqualTo("Walking on Sunshine");
        assertThat(track.artist()).isEqualTo("Katrina & The Waves");
    }

    @Test
    void constructor_usesUnknownArtist_whenArtistIsMissingOrBlank() {
        assertThat(new Track("Imagine", null).artist()).isEqualTo("Unknown artist");
        assertThat(new Track("Imagine", "  ").artist()).isEqualTo("Unknown artist");
    }

    @Test
    void constructor_rejectsMissingOrBlankTitle() {
        assertThatThrownBy(() -> new Track(null, "X")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Track("  ", "X")).isInstanceOf(IllegalArgumentException.class);
    }
}
