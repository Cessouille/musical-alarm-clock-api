package fr.cours.musical.alarm.clock.api.infra.out.notification.email;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ConsoleEmailClientTest {

    @Test
    void sendEmail_doesNotThrow() {
        assertThatCode(() -> new ConsoleEmailClient().sendEmail("alice@example.invalid", "subject", "body"))
                .doesNotThrowAnyException();
    }
}
