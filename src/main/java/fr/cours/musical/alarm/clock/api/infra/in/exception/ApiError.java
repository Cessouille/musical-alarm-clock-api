package fr.cours.musical.alarm.clock.api.infra.in.exception;

public record ApiError(int status, String message) {
}
