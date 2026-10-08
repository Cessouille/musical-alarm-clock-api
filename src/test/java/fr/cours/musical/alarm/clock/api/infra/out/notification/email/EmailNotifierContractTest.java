package fr.cours.musical.alarm.clock.api.infra.out.notification.email;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.port.out.Notifier;
import fr.cours.musical.alarm.clock.api.domain.port.out.NotifierContractTest;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailNotifierContractTest extends NotifierContractTest {

    private final EmailClient emailClient = mock(EmailClient.class);
    private final EmailNotifier notifier = new EmailNotifier(emailClient);

    @Override
    protected Notifier notifier() {
        return notifier;
    }

    @Override
    protected ChannelType expectedChannel() {
        return ChannelType.EMAIL;
    }

    @Override
    protected String textReceivedByVendor() {
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailClient).sendEmail(eq("alice@example.invalid"), subject.capture(), body.capture());
        return subject.getValue() + " " + body.getValue();
    }

    @Override
    protected void makeVendorFail() {
        doThrow(new IllegalStateException("smtp down")).when(emailClient).sendEmail(any(), any(), any());
    }
}
