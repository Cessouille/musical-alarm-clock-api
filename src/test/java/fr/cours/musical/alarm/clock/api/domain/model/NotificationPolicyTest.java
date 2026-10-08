package fr.cours.musical.alarm.clock.api.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static fr.cours.musical.alarm.clock.api.domain.model.ChannelType.EMAIL;
import static fr.cours.musical.alarm.clock.api.domain.model.ChannelType.PUSH;
import static fr.cours.musical.alarm.clock.api.domain.model.ChannelType.SMS;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationPolicyTest {

    @Test
    void channelsToTry_putsPreferredFirst_thenFallbackOrderWithoutDuplicates() {
        NotificationPolicy policy = new NotificationPolicy(List.of(PUSH, SMS, EMAIL));

        assertThat(policy.channelsToTry(SMS)).containsExactly(SMS, PUSH, EMAIL);
    }

    @Test
    void channelsToTry_returnsOnlyPreferred_whenFallbackOrderIsEmpty() {
        assertThat(new NotificationPolicy(List.of()).channelsToTry(EMAIL)).containsExactly(EMAIL);
    }

    @Test
    void channelsToTry_removesDuplicatesInConfiguredOrder() {
        NotificationPolicy policy = new NotificationPolicy(List.of(PUSH, PUSH, EMAIL));

        assertThat(policy.channelsToTry(EMAIL)).containsExactly(EMAIL, PUSH);
    }
}
