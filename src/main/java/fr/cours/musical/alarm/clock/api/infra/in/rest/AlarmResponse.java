package fr.cours.musical.alarm.clock.api.infra.in.rest;

import fr.cours.musical.alarm.clock.api.domain.model.AlarmResult;
import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;

public record AlarmResponse(String userId, TrackResponse track, ChannelType channel, boolean degraded) {

    public static AlarmResponse from(AlarmResult result) {
        return new AlarmResponse(result.userId(),
                new TrackResponse(result.track().title(), result.track().artist()),
                result.channel(), result.degraded());
    }

    public record TrackResponse(String title, String artist) {
    }
}
