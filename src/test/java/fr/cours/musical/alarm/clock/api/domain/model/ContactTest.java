package fr.cours.musical.alarm.clock.api.domain.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContactTest {

    @Test
    void addressFor_returnsTheAddressOfThatChannelOnly() {
        Contact contact = new Contact(Map.of(ChannelType.EMAIL, "a@example.invalid"));

        assertThat(contact.addressFor(ChannelType.EMAIL)).contains("a@example.invalid");
        assertThat(contact.addressFor(ChannelType.SMS)).isEmpty();
    }

    @Test
    void addressFor_treatsABlankAddressAsMissing() {
        assertThat(new Contact(Map.of(ChannelType.SMS, "  ")).addressFor(ChannelType.SMS)).isEmpty();
    }

    @Test
    void constructor_keepsAnImmutableCopy() {
        Map<ChannelType, String> source = new HashMap<>(Map.of(ChannelType.EMAIL, "a@example.invalid"));
        Contact contact = new Contact(source);

        source.clear();

        assertThat(contact.addressFor(ChannelType.EMAIL)).isPresent();
    }
}
