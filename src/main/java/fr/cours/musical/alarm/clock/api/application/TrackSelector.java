package fr.cours.musical.alarm.clock.api.application;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.port.out.FallbackTrackSource;
import fr.cours.musical.alarm.clock.api.domain.port.out.MusicProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackSelector {

    private final MusicProvider musicProvider;
    private final FallbackTrackSource fallbackTrackSource;

    public Selection select(String query, DayOfWeek day) {
        try {
            Optional<Track> found = musicProvider.findTrack(query);
            if (found.isPresent()) {
                return new Selection(found.get(), false);
            }
            log.warn("No music provider found a track for '{}', using local fallback", query);
        } catch (RuntimeException e) {
            log.warn("Music lookup failed for '{}', using local fallback", query, e);
        }
        return new Selection(fallbackTrackSource.trackFor(day), true);
    }

    public record Selection(Track track, boolean degraded) {
    }
}
