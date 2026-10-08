package fr.cours.musical.alarm.clock.api.infra.out.user;

import fr.cours.musical.alarm.clock.api.domain.model.AlarmSlot;
import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.UserPreferences;
import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;
import fr.cours.musical.alarm.clock.api.domain.port.out.UserPreferencesProvider;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.Map;
import java.util.Optional;

/** Mock of the internal user service: fixed demo users, one track per (day, weather) they chose. */
@Component
public class InMemoryUserPreferencesProvider implements UserPreferencesProvider {

    private final Map<String, UserPreferences> users = Map.of(
            "alice", new UserPreferences("alice",
                    Map.of(new AlarmSlot(DayOfWeek.MONDAY, WeatherType.SOLEIL), "Walking on Sunshine",
                            new AlarmSlot(DayOfWeek.MONDAY, WeatherType.PLUIE), "Singin' in the Rain",
                            new AlarmSlot(DayOfWeek.TUESDAY, WeatherType.SOLEIL), "Good Day Sunshine",
                            new AlarmSlot(DayOfWeek.TUESDAY, WeatherType.PLUIE), "Purple Rain"),
                    "Here Comes the Sun", ChannelType.EMAIL),
            "bob", new UserPreferences("bob",
                    Map.of(new AlarmSlot(DayOfWeek.TUESDAY, WeatherType.NEIGE), "Let It Snow",
                            new AlarmSlot(DayOfWeek.SATURDAY, WeatherType.NEIGE), "Frozen"),
                    "Imagine", ChannelType.SMS),
            "carol", new UserPreferences("carol", Map.of(), "Dancing Queen", ChannelType.PUSH));

    @Override
    public Optional<UserPreferences> findByUserId(String userId) {
        return userId == null ? Optional.empty() : Optional.ofNullable(users.get(userId));
    }
}
