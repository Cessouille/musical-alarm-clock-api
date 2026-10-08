package fr.cours.musical.alarm.clock.api.domain.port.out;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.model.WakeUpMessage;
import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Contract every Notifier must satisfy, whatever the vendor interface behind it. */
public abstract class NotifierContractTest {

    protected static final WakeUpMessage MESSAGE = new WakeUpMessage("alice", DayOfWeek.MONDAY,
            WeatherType.SOLEIL, new Track("Walking on Sunshine", "Katrina & The Waves"));

    protected abstract Notifier notifier();

    protected abstract ChannelType expectedChannel();

    /** Everything textual the vendor received for the last send, concatenated. */
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
}
