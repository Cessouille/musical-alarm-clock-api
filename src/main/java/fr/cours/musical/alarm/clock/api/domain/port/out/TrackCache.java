package fr.cours.musical.alarm.clock.api.domain.port.out;

import fr.cours.musical.alarm.clock.api.domain.model.Track;

import java.util.Optional;

public interface TrackCache {

    Optional<Track> get(String key);

    void put(String key, Track track);
}
