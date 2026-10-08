package fr.cours.musical.alarm.clock.api.infra.out.music;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.port.out.MusicProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Tries the configured providers in order; a failing or empty provider never blocks the next one. */
@Slf4j
@Component
@Qualifier("live")
public class FailoverMusicProvider implements MusicProvider {

    private final List<MusicProvider> orderedSources;

    public FailoverMusicProvider(@Qualifier("source") Map<String, MusicProvider> sources,
                                 @Value("${app.music.providers}") List<String> order) {
        this.orderedSources = order.stream()
                .map(name -> Optional.ofNullable(sources.get(name))
                        .orElseThrow(() -> new IllegalStateException(
                                "Unknown music provider '" + name + "' in app.music.providers; available: "
                                        + sources.keySet())))
                .toList();
    }

    @Override
    public Optional<Track> findTrack(String query) {
        for (MusicProvider source : orderedSources) {
            try {
                Optional<Track> result = source.findTrack(query);
                if (result.isPresent()) {
                    return result;
                }
            } catch (RuntimeException e) {
                log.warn("Music provider {} failed for '{}', trying next provider",
                        source.getClass().getSimpleName(), query, e);
            }
        }
        return Optional.empty();
    }
}
