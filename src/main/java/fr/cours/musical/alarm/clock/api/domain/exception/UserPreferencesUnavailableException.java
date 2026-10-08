package fr.cours.musical.alarm.clock.api.domain.exception;

public class UserPreferencesUnavailableException extends RuntimeException {

    public UserPreferencesUnavailableException(String userId, Throwable cause) {
        super("User preferences are currently unavailable for user '" + userId + "'", cause);
    }
}
