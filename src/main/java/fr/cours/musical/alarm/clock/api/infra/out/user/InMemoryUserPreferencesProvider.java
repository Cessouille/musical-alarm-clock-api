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

@Component
public class InMemoryUserPreferencesProvider implements UserPreferencesProvider {

    private final Map<String, UserPreferences> users = Map.of(
            "alice", new UserPreferences("alice",
                    Map.of(new AlarmSlot(DayOfWeek.MONDAY, WeatherType.SUN), "Walking on Sunshine",
                            new AlarmSlot(DayOfWeek.MONDAY, WeatherType.RAIN), "Singin' in the Rain",
                            new AlarmSlot(DayOfWeek.TUESDAY, WeatherType.SUN), "Good Day Sunshine",
                            new AlarmSlot(DayOfWeek.TUESDAY, WeatherType.RAIN), "Purple Rain"),
                    "Here Comes the Sun", ChannelType.EMAIL),
            "bob", new UserPreferences("bob",
                    Map.of(new AlarmSlot(DayOfWeek.TUESDAY, WeatherType.SNOW), "Let It Snow",
                            new AlarmSlot(DayOfWeek.SATURDAY, WeatherType.SNOW), "Frozen"),
                    "Imagine", ChannelType.SMS),
            "carol", new UserPreferences("carol", Map.of(), "Dancing Queen", ChannelType.PUSH));

    @Override
    public Optional<UserPreferences> findByUserId(String userId) {
        return userId == null ? Optional.empty() : Optional.ofNullable(users.get(userId));
    }
}
