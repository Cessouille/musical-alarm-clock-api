package fr.cours.musical.alarm.clock.api.domain.port.out;

import fr.cours.musical.alarm.clock.api.domain.model.Track;

import java.time.DayOfWeek;

public interface FallbackTrackSource {

    Track trackFor(DayOfWeek day);
}
