package fr.cours.musical.alarm.clock.api.infra.in.rest;

import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;

public record TriggerAlarmRequest(
        @NotBlank(message = "userId must not be blank")
        @Size(max = 64, message = "userId must not exceed 64 characters")
        @Pattern(regexp = "[A-Za-z0-9._-]*", message = "userId may only contain letters, digits, dots, dashes and underscores")
        String userId,
        @NotNull(message = "dayOfWeek is required") DayOfWeek dayOfWeek,
        @NotNull(message = "weather is required") WeatherType weather
) {
}
