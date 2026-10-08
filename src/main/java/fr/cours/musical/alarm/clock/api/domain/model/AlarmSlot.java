package fr.cours.musical.alarm.clock.api.domain.model;

import java.time.DayOfWeek;

/** A day of the week combined with the day's weather: the key under which a user picks a track. */
public record AlarmSlot(DayOfWeek dayOfWeek, WeatherType weather) {
}
