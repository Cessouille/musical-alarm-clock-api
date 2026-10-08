package fr.cours.musical.alarm.clock.api.infra.in.rest;

import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;

public record TriggerAlarmRequest(
        @NotBlank(message = "userId must not be blank") String userId,
        @NotNull(message = "dayOfWeek is required") DayOfWeek dayOfWeek,
        @NotNull(message = "weather is required") WeatherType weather
) {
}
