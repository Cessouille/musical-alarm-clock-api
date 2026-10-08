package fr.cours.musical.alarm.clock.api.domain.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String userId) {
        super("No user found with id: " + userId);
    }
}
