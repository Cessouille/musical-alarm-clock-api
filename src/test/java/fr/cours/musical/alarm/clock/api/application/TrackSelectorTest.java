package fr.cours.musical.alarm.clock.api.application;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.port.out.FallbackTrackSource;
import fr.cours.musical.alarm.clock.api.domain.port.out.MusicProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackSelectorTest {

    private static final Track FOUND = new Track("Walking on Sunshine", "Katrina & The Waves");
    private static final Track LOCAL = new Track("Here Comes the Sun", "The Beatles");

    @Mock
    private MusicProvider musicProvider;

    @Mock
    private FallbackTrackSource fallbackTrackSource;

    private TrackSelector trackSelector;

    @BeforeEach
    void setUp() {
        trackSelector = new TrackSelector(musicProvider, fallbackTrackSource);
    }

    @Test
    void select_returnsProviderTrackNotDegraded_andNeverTouchesLocalFallback() {
        when(musicProvider.findTrack("Walking on Sunshine")).thenReturn(Optional.of(FOUND));

        TrackSelector.Selection selection = trackSelector.select("Walking on Sunshine", DayOfWeek.MONDAY);

        assertThat(selection).isEqualTo(new TrackSelector.Selection(FOUND, false));
        verifyNoInteractions(fallbackTrackSource);
    }

    @Test
    void select_usesLocalFallbackDegraded_whenNoProviderFindsTheTrack() {
        when(musicProvider.findTrack("Unknown song")).thenReturn(Optional.empty());
        when(fallbackTrackSource.trackFor(DayOfWeek.TUESDAY)).thenReturn(LOCAL);

        TrackSelector.Selection selection = trackSelector.select("Unknown song", DayOfWeek.TUESDAY);

        assertThat(selection).isEqualTo(new TrackSelector.Selection(LOCAL, true));
    }

    @Test
    void select_usesLocalFallbackDegraded_whenProviderThrows() {
        when(musicProvider.findTrack("Walking on Sunshine")).thenThrow(new IllegalStateException("boom"));
        when(fallbackTrackSource.trackFor(DayOfWeek.FRIDAY)).thenReturn(LOCAL);

        TrackSelector.Selection selection = trackSelector.select("Walking on Sunshine", DayOfWeek.FRIDAY);

        assertThat(selection).isEqualTo(new TrackSelector.Selection(LOCAL, true));
    }
}
