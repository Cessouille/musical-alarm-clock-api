package fr.cours.musical.alarm.clock.api.infra.out.cache;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.port.out.TrackCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class InMemoryTrackCache implements TrackCache {

    private final Clock clock;
    private final Duration ttl;
    private final Map<String, Entry> entries;

    public InMemoryTrackCache(Clock clock,
                              @Value("${app.cache.ttl}") Duration ttl,
                              @Value("${app.cache.max-entries}") int maxEntries) {
        this.clock = clock;
        this.ttl = ttl;
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
                return size() > maxEntries;
            }
        };
    }

    @Override
    public synchronized Optional<Track> get(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (!clock.instant().isBefore(entry.expiresAt())) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.track());
    }

    @Override
    public synchronized void put(String key, Track track) {
        entries.put(key, new Entry(track, clock.instant().plus(ttl)));
    }

    private record Entry(Track track, Instant expiresAt) {
    }
}
