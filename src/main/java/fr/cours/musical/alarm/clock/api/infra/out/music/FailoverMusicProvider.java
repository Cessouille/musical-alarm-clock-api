package fr.cours.musical.alarm.clock.api.infra.out.music;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.port.out.MusicProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Qualifier("live")
public class FailoverMusicProvider implements MusicProvider {

    private final List<MusicProvider> orderedSources;
    private final Clock clock;
    private final Duration cooldown;
    private final Map<MusicProvider, Instant> skippedUntil = new ConcurrentHashMap<>();

    public FailoverMusicProvider(@MusicSource Map<String, MusicProvider> sources,
                                 @Value("${app.music.providers}") List<String> order,
                                 @Value("${app.music.failure-cooldown}") Duration cooldown,
                                 Clock clock) {
        this.cooldown = cooldown;
        this.clock = clock;
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
            if (isCoolingDown(source)) {
                continue;
            }
            try {
                Optional<Track> result = source.findTrack(query);
                skippedUntil.remove(source);
                if (result.isPresent()) {
                    return result;
                }
            } catch (RuntimeException e) {
                skippedUntil.put(source, clock.instant().plus(cooldown));
                log.warn("Music provider {} failed for '{}' ({}), skipped for {}",
                        source.getClass().getSimpleName(), query, e.toString(), cooldown);
            }
        }
        return Optional.empty();
    }

    private boolean isCoolingDown(MusicProvider source) {
        Instant until = skippedUntil.get(source);
        return until != null && clock.instant().isBefore(until);
    }
}
