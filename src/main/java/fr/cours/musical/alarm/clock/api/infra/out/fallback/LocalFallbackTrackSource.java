package fr.cours.musical.alarm.clock.api.infra.out.fallback;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.port.out.FallbackTrackSource;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;

@Component
public class LocalFallbackTrackSource implements FallbackTrackSource {

    /** One track per day, Monday first: index = DayOfWeek.ordinal(). */
    private static final List<Track> TRACKS = List.of(
            new Track("Here Comes the Sun", "The Beatles"),
            new Track("Good Vibrations", "The Beach Boys"),
            new Track("Walking on Sunshine", "Katrina & The Waves"),
            new Track("Three Little Birds", "Bob Marley & The Wailers"),
            new Track("Dancing Queen", "ABBA"),
            new Track("Wake Me Up", "Avicii"),
            new Track("Don't Stop Me Now", "Queen"));

    @Override
    public Track trackFor(DayOfWeek day) {
        return TRACKS.get(day.ordinal());
    }
}
