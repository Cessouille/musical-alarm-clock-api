package fr.cours.musical.alarm.clock.api.application;

import fr.cours.musical.alarm.clock.api.domain.exception.UserNotFoundException;
import fr.cours.musical.alarm.clock.api.domain.model.AlarmResult;
import fr.cours.musical.alarm.clock.api.domain.model.UserPreferences;
import fr.cours.musical.alarm.clock.api.domain.model.WakeUpMessage;
import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;
import fr.cours.musical.alarm.clock.api.domain.port.in.AlarmUseCase;
import fr.cours.musical.alarm.clock.api.domain.port.out.UserPreferencesProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService implements AlarmUseCase {

    private final UserPreferencesProvider userPreferencesProvider;
    private final TrackSelector trackSelector;
    private final NotificationDispatcher notificationDispatcher;

    @Override
    public AlarmResult triggerAlarm(String userId, DayOfWeek dayOfWeek, WeatherType weather) {
        UserPreferences preferences = userPreferencesProvider.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        TrackSelector.Selection selection = trackSelector.select(preferences.queryFor(weather), dayOfWeek);
        WakeUpMessage message = new WakeUpMessage(userId, dayOfWeek, weather, selection.track());
        NotificationDispatcher.Delivery delivery =
                notificationDispatcher.dispatch(message, preferences.preferredChannel());

        boolean degraded = selection.degraded() || delivery.degraded();
        log.info("Alarm sent to user {} via {} (track='{}', degraded={})",
                userId, delivery.channel(), selection.track().title(), degraded);
        return new AlarmResult(userId, selection.track(), delivery.channel(), degraded);
    }
}
