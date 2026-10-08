package fr.cours.musical.alarm.clock.api.infra.out.notification.sms;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.model.WakeUpMessage;
import fr.cours.musical.alarm.clock.api.domain.model.WeatherType;
import fr.cours.musical.alarm.clock.api.domain.port.out.Notifier;
import fr.cours.musical.alarm.clock.api.domain.port.out.NotifierContractTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.DayOfWeek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsNotifierContractTest extends NotifierContractTest {

    private final SmsGateway smsGateway = mock(SmsGateway.class);
    private final SmsNotifier notifier = new SmsNotifier(smsGateway);

    @Override
    protected Notifier notifier() {
        return notifier;
    }

    @Override
    protected ChannelType expectedChannel() {
        return ChannelType.SMS;
    }

    @Override
    protected String textReceivedByVendor() {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(smsGateway).sendText(eq("+33600000001"), text.capture());
        return text.getValue();
    }

    @Override
    protected void makeVendorFail() {
        when(smsGateway.sendText(any(), any())).thenThrow(new IllegalStateException("sms gateway down"));
    }

    @Test
    void send_truncatesTextToOneSmsSegment() {
        WakeUpMessage longMessage = new WakeUpMessage("alice", DayOfWeek.MONDAY, WeatherType.SUN,
                new Track("A".repeat(300), "B"), MESSAGE.contact());

        notifier.send(longMessage);

        assertThat(textReceivedByVendor()).hasSize(160);
    }

    @Test
    void send_doesNotSplitASurrogatePairWhenTruncating() {
        WakeUpMessage emojiMessage = new WakeUpMessage("alice", DayOfWeek.MONDAY, WeatherType.SUN,
                new Track("😀".repeat(200), "B"), MESSAGE.contact());

        notifier.send(emojiMessage);

        String text = textReceivedByVendor();
        assertThat(text.length()).isLessThanOrEqualTo(160);
        assertThat(Character.isHighSurrogate(text.charAt(text.length() - 1))).isFalse();
    }
}
