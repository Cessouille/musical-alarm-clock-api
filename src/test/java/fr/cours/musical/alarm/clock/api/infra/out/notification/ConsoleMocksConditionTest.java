package fr.cours.musical.alarm.clock.api.infra.out.notification;

import fr.cours.musical.alarm.clock.api.infra.out.notification.email.ConsoleEmailClient;
import fr.cours.musical.alarm.clock.api.infra.out.notification.push.ConsolePushService;
import fr.cours.musical.alarm.clock.api.infra.out.notification.sms.ConsoleSmsGateway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleMocksConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ConsoleEmailClient.class, ConsoleSmsGateway.class, ConsolePushService.class);

    @Test
    void consoleMocks_areRegistered_byDefault() {
        runner.run(context -> assertThat(context).hasSingleBean(ConsoleEmailClient.class)
                .hasSingleBean(ConsoleSmsGateway.class).hasSingleBean(ConsolePushService.class));
    }

    @Test
    void consoleMocks_areNotRegistered_whenMockingIsDisabled() {
        runner.withPropertyValues("app.notification.mock=false").run(context -> assertThat(context)
                .doesNotHaveBean(ConsoleEmailClient.class)
                .doesNotHaveBean(ConsoleSmsGateway.class)
                .doesNotHaveBean(ConsolePushService.class));
    }
}
