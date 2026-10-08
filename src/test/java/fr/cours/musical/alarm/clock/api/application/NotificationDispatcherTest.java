package fr.cours.musical.alarm.clock.api.application;

import fr.cours.musical.alarm.clock.api.domain.exception.AlarmDeliveryException;
import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.NotificationPolicy;
import fr.cours.musical.alarm.clock.api.domain.model.Contact;
import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.model.WakeUpMessage;
import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;
import fr.cours.musical.alarm.clock.api.domain.port.out.Notifier;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Map;
import java.util.List;

import static fr.cours.musical.alarm.clock.api.domain.model.ChannelType.EMAIL;
import static fr.cours.musical.alarm.clock.api.domain.model.ChannelType.PUSH;
import static fr.cours.musical.alarm.clock.api.domain.model.ChannelType.SMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDispatcherTest {

    private static final WakeUpMessage MESSAGE = new WakeUpMessage("alice", DayOfWeek.MONDAY,
            WeatherType.SUN, new Track("Walking on Sunshine", "Katrina & The Waves"),
            new Contact(Map.of(ChannelType.EMAIL, "alice@example.invalid", ChannelType.SMS, "+33600000001",
            ChannelType.PUSH, "device-alice")));
    private static final NotificationPolicy POLICY = new NotificationPolicy(List.of(PUSH, SMS, EMAIL));

    private final Notifier email = notifier(EMAIL);
    private final Notifier sms = notifier(SMS);
    private final Notifier push = notifier(PUSH);

    private static Notifier notifier(ChannelType channel) {
        Notifier notifier = mock(Notifier.class);
        when(notifier.channel()).thenReturn(channel);
        return notifier;
    }

    @Test
    void dispatch_usesPreferredChannel_notDegraded() {
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(email, sms, push), POLICY);

        NotificationDispatcher.Delivery delivery = dispatcher.dispatch(MESSAGE, EMAIL);

        assertThat(delivery).isEqualTo(new NotificationDispatcher.Delivery(EMAIL, false));
        verify(email).send(MESSAGE);
        verify(sms, never()).send(any());
        verify(push, never()).send(any());
    }

    @Test
    void dispatch_fallsBackToNextConfiguredChannel_degraded_whenPreferredFails() {
        doThrow(new IllegalStateException("smtp down")).when(email).send(MESSAGE);
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(email, sms, push), POLICY);

        NotificationDispatcher.Delivery delivery = dispatcher.dispatch(MESSAGE, EMAIL);

        assertThat(delivery).isEqualTo(new NotificationDispatcher.Delivery(PUSH, true));
        verify(sms, never()).send(any());
    }

    @Test
    void dispatch_skipsChannelsWithoutRegisteredNotifier() {
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(push), POLICY);

        NotificationDispatcher.Delivery delivery = dispatcher.dispatch(MESSAGE, EMAIL);

        assertThat(delivery).isEqualTo(new NotificationDispatcher.Delivery(PUSH, true));
    }

    @Test
    void dispatch_throwsDeliveryException_listingAttemptedChannels_whenAllFail() {
        doThrow(new IllegalStateException("down")).when(email).send(MESSAGE);
        doThrow(new IllegalStateException("down")).when(sms).send(MESSAGE);
        doThrow(new IllegalStateException("down")).when(push).send(MESSAGE);
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(email, sms, push), POLICY);

        assertThatThrownBy(() -> dispatcher.dispatch(MESSAGE, EMAIL))
                .isInstanceOf(AlarmDeliveryException.class)
                .hasMessageContaining("alice")
                .hasMessageContaining("EMAIL")
                .hasMessageContaining("PUSH")
                .hasMessageContaining("SMS");
    }

    @Test
    void dispatch_throwsDeliveryException_whenNoNotifierIsRegisteredAtAll() {
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(), POLICY);

        assertThatThrownBy(() -> dispatcher.dispatch(MESSAGE, EMAIL))
                .isInstanceOf(AlarmDeliveryException.class);
    }

    @Test
    void constructor_rejectsTwoNotifiersForTheSameChannel() {
        assertThatThrownBy(() -> new NotificationDispatcher(List.of(email, notifier(EMAIL)), POLICY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EMAIL");
    }
}
