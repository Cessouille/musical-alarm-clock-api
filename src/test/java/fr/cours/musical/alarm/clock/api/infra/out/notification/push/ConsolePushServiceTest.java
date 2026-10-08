package fr.cours.musical.alarm.clock.api.infra.out.notification.push;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

class ConsolePushServiceTest {

    @Test
    void pushNotification_doesNotThrow() {
        assertThatCode(() -> new ConsolePushService().pushNotification(
                new PushPayload("device-alice", "title", "body", Map.of("k", "v"))))
                .doesNotThrowAnyException();
    }
}
