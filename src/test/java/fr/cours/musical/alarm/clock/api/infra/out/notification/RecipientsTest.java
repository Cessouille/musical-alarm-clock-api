package fr.cours.musical.alarm.clock.api.infra.out.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecipientsTest {

    @Test
    void mask_keepsOnlyTheEdgesOfTheAddress() {
        assertThat(Recipients.mask("alice@example.invalid")).isEqualTo("al***id");
        assertThat(Recipients.mask("+33600000001")).isEqualTo("+3***01");
    }

    @Test
    void mask_hidesShortOrMissingValuesEntirely() {
        assertThat(Recipients.mask("abc")).isEqualTo("***");
        assertThat(Recipients.mask(null)).isEqualTo("***");
    }
}
