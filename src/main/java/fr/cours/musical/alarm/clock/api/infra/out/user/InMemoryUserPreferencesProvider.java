package fr.cours.musical.alarm.clock.api.infra.out.user;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.UserPreferences;
import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;
import fr.cours.musical.alarm.clock.api.domain.port.out.UserPreferencesProvider;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/** Mock of the internal user service: fixed demo users. */
@Component
public class InMemoryUserPreferencesProvider implements UserPreferencesProvider {

    private final Map<String, UserPreferences> users = Map.of(
            "alice", new UserPreferences("alice",
                    Map.of(WeatherType.SOLEIL, "Walking on Sunshine", WeatherType.PLUIE, "Singin' in the Rain"),
                    "Here Comes the Sun", ChannelType.EMAIL),
            "bob", new UserPreferences("bob",
                    Map.of(WeatherType.NEIGE, "Let It Snow"), "Imagine", ChannelType.SMS),
            "carol", new UserPreferences("carol", Map.of(), "Dancing Queen", ChannelType.PUSH));

    @Override
    public Optional<UserPreferences> findByUserId(String userId) {
        return userId == null ? Optional.empty() : Optional.ofNullable(users.get(userId));
    }
}
