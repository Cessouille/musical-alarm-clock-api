package fr.cours.musical.alarm.clock.api.domain.model;

import java.time.DayOfWeek;

public record AlarmSlot(DayOfWeek dayOfWeek, WeatherType weather) {
}
