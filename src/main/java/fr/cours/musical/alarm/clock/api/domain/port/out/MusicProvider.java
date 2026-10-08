package fr.cours.musical.alarm.clock.api.domain.port.out;

import fr.cours.musical.alarm.clock.api.domain.model.Track;

import java.util.Optional;

public interface MusicProvider {

    /** Empty = no match. A provider failure is signalled by an unchecked exception. */
    Optional<Track> findTrack(String query);
}
