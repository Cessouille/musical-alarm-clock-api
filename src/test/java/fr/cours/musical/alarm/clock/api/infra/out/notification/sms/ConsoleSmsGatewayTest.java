package fr.cours.musical.alarm.clock.api.infra.out.notification.sms;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleSmsGatewayTest {

    @Test
    void sendText_returnsAMessageId() {
        assertThat(new ConsoleSmsGateway().sendText("+000-alice", "hello")).startsWith("sms-");
    }
}
