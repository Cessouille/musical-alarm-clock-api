package fr.cours.musical.alarm.clock.api.domain.model;

import java.time.DayOfWeek;

public record WakeUpMessage(String userId, DayOfWeek dayOfWeek, WeatherType weather, Track track, Contact contact) {
}
