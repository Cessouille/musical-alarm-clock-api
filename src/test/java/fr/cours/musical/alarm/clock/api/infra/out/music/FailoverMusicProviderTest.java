package fr.cours.musical.alarm.clock.api.infra.out.music;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.port.out.MusicProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FailoverMusicProviderTest {

    private static final Track FROM_FIRST = new Track("Song", "First");
    private static final Track FROM_SECOND = new Track("Song", "Second");

    private final MusicProvider itunes = mock(MusicProvider.class);
    private final MusicProvider musicBrainz = mock(MusicProvider.class);
    private final Map<String, MusicProvider> sources = Map.of("itunes", itunes, "musicbrainz", musicBrainz);

    @Test
    void findTrack_returnsFirstProviderResult_withoutCallingTheNext() {
        when(itunes.findTrack("Song")).thenReturn(Optional.of(FROM_FIRST));
        FailoverMusicProvider failover = new FailoverMusicProvider(sources, List.of("itunes", "musicbrainz"));

        assertThat(failover.findTrack("Song")).contains(FROM_FIRST);
        verify(musicBrainz, never()).findTrack(any());
    }

    @Test
    void findTrack_triesNextProvider_whenFirstHasNoMatch() {
        when(itunes.findTrack("Song")).thenReturn(Optional.empty());
        when(musicBrainz.findTrack("Song")).thenReturn(Optional.of(FROM_SECOND));
        FailoverMusicProvider failover = new FailoverMusicProvider(sources, List.of("itunes", "musicbrainz"));

        assertThat(failover.findTrack("Song")).contains(FROM_SECOND);
    }

    @Test
    void findTrack_triesNextProvider_whenFirstThrows() {
        when(itunes.findTrack("Song")).thenThrow(new IllegalStateException("429 Too Many Requests"));
        when(musicBrainz.findTrack("Song")).thenReturn(Optional.of(FROM_SECOND));
        FailoverMusicProvider failover = new FailoverMusicProvider(sources, List.of("itunes", "musicbrainz"));

        assertThat(failover.findTrack("Song")).contains(FROM_SECOND);
    }

    @Test
    void findTrack_returnsEmpty_whenEveryProviderFailsOrHasNoMatch() {
        when(itunes.findTrack("Song")).thenThrow(new IllegalStateException("down"));
        when(musicBrainz.findTrack("Song")).thenReturn(Optional.empty());
        FailoverMusicProvider failover = new FailoverMusicProvider(sources, List.of("itunes", "musicbrainz"));

        assertThat(failover.findTrack("Song")).isEmpty();
    }

    @Test
    void findTrack_followsConfiguredOrder() {
        when(musicBrainz.findTrack("Song")).thenReturn(Optional.of(FROM_SECOND));
        FailoverMusicProvider failover = new FailoverMusicProvider(sources, List.of("musicbrainz", "itunes"));

        assertThat(failover.findTrack("Song")).contains(FROM_SECOND);
        verify(itunes, never()).findTrack(any());
    }

    @Test
    void findTrack_returnsEmptyWithoutCallingAnyone_whenNoProviderIsConfigured() {
        FailoverMusicProvider failover = new FailoverMusicProvider(sources, List.of());

        assertThat(failover.findTrack("Song")).isEmpty();
        verifyNoInteractions(itunes, musicBrainz);
    }

    @Test
    void constructor_failsFast_onUnknownProviderName() {
        assertThatThrownBy(() -> new FailoverMusicProvider(sources, List.of("itunes", "spotify")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spotify");
    }
}
