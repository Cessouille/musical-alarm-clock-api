package fr.cours.musical.alarm.clock.api.application;

import fr.cours.musical.alarm.clock.api.domain.exception.AlarmDeliveryException;
import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.NotificationPolicy;
import fr.cours.musical.alarm.clock.api.domain.model.WakeUpMessage;
import fr.cours.musical.alarm.clock.api.domain.port.out.Notifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotificationDispatcher {

    private final Map<ChannelType, Notifier> notifiers = new EnumMap<>(ChannelType.class);
    private final NotificationPolicy policy;

    public NotificationDispatcher(List<Notifier> availableNotifiers, NotificationPolicy policy) {
        this.policy = policy;
        for (Notifier notifier : availableNotifiers) {
            if (notifiers.put(notifier.channel(), notifier) != null) {
                throw new IllegalStateException("Several notifiers registered for channel " + notifier.channel());
            }
        }
    }

    public Delivery dispatch(WakeUpMessage message, ChannelType preferred) {
        List<ChannelType> attempted = new ArrayList<>();
        for (ChannelType channel : policy.channelsToTry(preferred)) {
            Notifier notifier = notifiers.get(channel);
            if (notifier == null) {
                log.warn("No notifier registered for channel {}, skipping", channel);
                continue;
            }
            attempted.add(channel);
            try {
                notifier.send(message);
                return new Delivery(channel, channel != preferred);
            } catch (RuntimeException e) {
                log.warn("Channel {} failed for user {}, trying next channel", channel, message.userId(), e);
            }
        }
        log.error("Alarm for user {} could not be delivered on any channel {}", message.userId(), attempted);
        throw new AlarmDeliveryException(message.userId(), attempted);
    }

    public record Delivery(ChannelType channel, boolean degraded) {
    }
}
