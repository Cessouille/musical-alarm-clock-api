package fr.cours.musical.alarm.clock.api.domain.port.out;

import fr.cours.musical.alarm.clock.api.domain.model.UserPreferences;

import java.util.Optional;

public interface UserPreferencesProvider {

    Optional<UserPreferences> findByUserId(String userId);
}
