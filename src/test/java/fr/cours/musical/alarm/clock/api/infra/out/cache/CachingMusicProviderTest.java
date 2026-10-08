package fr.cours.musical.alarm.clock.api.infra.out.cache;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.port.out.MusicProvider;
import fr.cours.musical.alarm.clock.api.domain.port.out.TrackCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachingMusicProviderTest {

    private static final Track TRACK = new Track("Walking on Sunshine", "Katrina & The Waves");

    @Mock
    private MusicProvider delegate;

    @Mock
    private TrackCache cache;

    private CachingMusicProvider cachingProvider;

    @BeforeEach
    void setUp() {
        cachingProvider = new CachingMusicProvider(delegate, cache);
    }

    @Test
    void findTrack_callsDelegateAndStoresResult_whenCacheIsEmpty() {
        when(cache.get("walking on sunshine")).thenReturn(Optional.empty());
        when(delegate.findTrack("Walking on Sunshine")).thenReturn(Optional.of(TRACK));

        Optional<Track> result = cachingProvider.findTrack("Walking on Sunshine");

        assertThat(result).contains(TRACK);
        verify(cache).put("walking on sunshine", TRACK);
    }

    @Test
    void findTrack_returnsCachedValueWithoutCallingDelegate_whenCacheHasEntry() {
        when(cache.get("walking on sunshine")).thenReturn(Optional.of(TRACK));

        Optional<Track> result = cachingProvider.findTrack("Walking on Sunshine");

        assertThat(result).contains(TRACK);
        verifyNoInteractions(delegate);
    }

    @Test
    void findTrack_normalizesTheKey_ignoringCaseAndSurroundingSpaces() {
        when(cache.get("walking on sunshine")).thenReturn(Optional.of(TRACK));

        assertThat(cachingProvider.findTrack("  Walking On SUNSHINE ")).contains(TRACK);
    }

    @Test
    void findTrack_doesNotCacheEmptyResult() {
        when(cache.get("unknown song")).thenReturn(Optional.empty());
        when(delegate.findTrack("Unknown song")).thenReturn(Optional.empty());

        assertThat(cachingProvider.findTrack("Unknown song")).isEmpty();
        verify(cache, never()).put(any(), any());
    }

    @Test
    void findTrack_propagatesDelegateFailure_andCachesNothing() {
        when(cache.get("walking on sunshine")).thenReturn(Optional.empty());
        when(delegate.findTrack("Walking on Sunshine")).thenThrow(new IllegalStateException("down"));

        assertThatThrownBy(() -> cachingProvider.findTrack("Walking on Sunshine"))
                .isInstanceOf(IllegalStateException.class);
        verify(cache, never()).put(any(), any());
    }
}
