package fr.cours.musical.alarm.clock.api.infra.out.notification.push;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.port.out.Notifier;
import fr.cours.musical.alarm.clock.api.domain.port.out.NotifierContractTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PushNotifierContractTest extends NotifierContractTest {

    private final PushService pushService = mock(PushService.class);
    private final PushNotifier notifier = new PushNotifier(pushService);

    @Override
    protected Notifier notifier() {
        return notifier;
    }

    @Override
    protected ChannelType expectedChannel() {
        return ChannelType.PUSH;
    }

    @Override
    protected String textReceivedByVendor() {
        return lastPayload().title() + " " + lastPayload().body();
    }

    @Override
    protected void makeVendorFail() {
        doThrow(new IllegalStateException("push provider down")).when(pushService).pushNotification(any());
    }

    @Test
    void send_targetsTheDeviceOfTheUser_andCarriesContextData() {
        notifier.send(MESSAGE);

        assertThat(lastPayload().deviceToken()).isEqualTo("device-alice");
        assertThat(lastPayload().data()).containsEntry("weather", "SUN").containsEntry("day", "MONDAY");
    }

    private PushPayload lastPayload() {
        ArgumentCaptor<PushPayload> payload = ArgumentCaptor.forClass(PushPayload.class);
        verify(pushService).pushNotification(payload.capture());
        return payload.getValue();
    }
}
