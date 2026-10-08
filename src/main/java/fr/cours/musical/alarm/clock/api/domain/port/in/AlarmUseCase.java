package fr.cours.musical.alarm.clock.api.domain.port.in;

import fr.cours.musical.alarm.clock.api.domain.model.AlarmResult;
import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;

import java.time.DayOfWeek;

public interface AlarmUseCase {

    AlarmResult triggerAlarm(String userId, DayOfWeek dayOfWeek, WeatherType weather);
}
