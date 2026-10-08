package fr.cours.musical.alarm.clock.api.infra.config;

import fr.cours.musical.alarm.clock.api.domain.model.ChannelType;
import fr.cours.musical.alarm.clock.api.domain.model.NotificationPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class NotificationConfig {

    @Bean
    public NotificationPolicy notificationPolicy(
            @Value("${app.notification.fallback-order}") List<ChannelType> fallbackOrder) {
        return new NotificationPolicy(fallbackOrder);
    }
}
