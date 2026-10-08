package fr.cours.musical.alarm.clock.api.infra.in.rest;

import fr.cours.musical.alarm.clock.api.domain.port.in.AlarmUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/alarms")
@RequiredArgsConstructor
public class AlarmRestController {

    private final AlarmUseCase alarmUseCase;

    @PostMapping("/trigger")
    public AlarmResponse trigger(@Valid @RequestBody TriggerAlarmRequest request) {
        log.info("REST request to trigger alarm for user {} ({}, {})",
                request.userId(), request.dayOfWeek(), request.weather());
        return AlarmResponse.from(alarmUseCase.triggerAlarm(request.userId(), request.dayOfWeek(), request.weather()));
    }
}
