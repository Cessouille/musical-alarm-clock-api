package fr.cours.musical.alarm.clock.api.domain.model;

public record AlarmResult(String userId, Track track, ChannelType channel, boolean degraded) {
}
