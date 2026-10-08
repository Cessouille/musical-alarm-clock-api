package fr.cours.musical.alarm.clock.api.infra.out.cache;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.port.out.TrackCache;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryTrackCache implements TrackCache {

    private final Map<String, Track> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<Track> get(String key) {
        return Optional.ofNullable(entries.get(key));
    }

    @Override
    public void put(String key, Track track) {
        entries.put(key, track);
    }
}
