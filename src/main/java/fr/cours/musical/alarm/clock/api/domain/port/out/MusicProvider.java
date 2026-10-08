package fr.cours.musical.alarm.clock.api.domain.port.out;

import fr.cours.musical.alarm.clock.api.domain.model.Track;

import java.util.Optional;

public interface MusicProvider {

    Optional<Track> findTrack(String query);
}
