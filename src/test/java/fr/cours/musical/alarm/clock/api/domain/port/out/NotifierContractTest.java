package fr.cours.musical.alarm.clock.api.domain.port.out;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.Contact;
import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.model.WakeUpMessage;
import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class NotifierContractTest {

    protected static final WakeUpMessage MESSAGE = new WakeUpMessage("alice", DayOfWeek.MONDAY,
            WeatherType.SUN, new Track("Walking on Sunshine", "Katrina & The Waves"),
            new Contact(Map.of(ChannelType.EMAIL, "alice@example.invalid", ChannelType.SMS, "+33600000001",
            ChannelType.PUSH, "device-alice")));

    protected abstract Notifier notifier();

    protected abstract ChannelType expectedChannel();

    protected abstract String textReceivedByVendor();

    protected abstract void makeVendorFail();

    @Test
    void channel_isTheOneThisAdapterServes() {
        assertThat(notifier().channel()).isEqualTo(expectedChannel());
    }

    @Test
    void send_deliversTrackArtistAndDayToTheVendor() {
        notifier().send(MESSAGE);

        assertThat(textReceivedByVendor())
                .contains("Walking on Sunshine")
                .contains("Katrina & The Waves")
                .containsIgnoringCase("lundi");
    }

    @Test
    void send_propagatesVendorFailure_soTheDispatcherCanFallBack() {
        makeVendorFail();

        assertThatThrownBy(() -> notifier().send(MESSAGE)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void send_failsExplicitly_whenTheUserHasNoAddressForThisChannel() {
        assertThatThrownBy(() -> notifier().send(withContact(new Contact(Map.of()))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("alice");
    }

    @Test
    void send_failsExplicitly_whenTheAddressForThisChannelIsBlank() {
        assertThatThrownBy(() -> notifier().send(withContact(new Contact(Map.of(notifier().channel(), " ")))))
                .isInstanceOf(IllegalStateException.class);
    }

    private static WakeUpMessage withContact(Contact contact) {
        return new WakeUpMessage(MESSAGE.userId(), MESSAGE.dayOfWeek(), MESSAGE.weather(), MESSAGE.track(), contact);
    }
}
