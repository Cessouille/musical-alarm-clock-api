package fr.cours.musical.alarm.clock.api.infra.out.cache;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.port.out.MusicProvider;
import fr.cours.musical.alarm.clock.api.domain.port.out.TrackCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Component
@Primary
public class CachingMusicProvider implements MusicProvider {

    private final MusicProvider delegate;
    private final TrackCache cache;

    public CachingMusicProvider(@Qualifier("live") MusicProvider delegate, TrackCache cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    @Override
    public Optional<Track> findTrack(String query) {
        String key = query.trim().toLowerCase(Locale.ROOT);
        Optional<Track> cached = cache.get(key);
        if (cached.isPresent()) {
            log.debug("Track cache hit for '{}'", key);
            return cached;
        }
        Optional<Track> result = delegate.findTrack(query);
        result.ifPresent(track -> cache.put(key, track));
        return result;
    }
}
