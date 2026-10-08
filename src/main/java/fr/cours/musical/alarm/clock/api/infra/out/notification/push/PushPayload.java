package fr.cours.musical.alarm.clock.api.infra.out.notification.push;

import java.util.Map;

public record PushPayload(String deviceToken, String title, String body, Map<String, String> data) {
}
