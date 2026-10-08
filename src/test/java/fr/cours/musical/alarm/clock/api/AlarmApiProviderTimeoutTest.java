package fr.cours.musical.alarm.clock.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real RestClients against a local server that never answers in time: the alarm must still go out. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "app.music.providers=itunes,musicbrainz",
                "app.http.connect-timeout=300ms",
                "app.http.read-timeout=300ms"
        })
@AutoConfigureMockMvc
class AlarmApiProviderTimeoutTest {

    private static HttpServer slowServer;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void startSlowServer() throws IOException {
        slowServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        slowServer.createContext("/", exchange -> {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        slowServer.setExecutor(Executors.newCachedThreadPool());
        slowServer.start();
    }

    @AfterAll
    static void stopSlowServer() {
        slowServer.stop(0);
    }

    @DynamicPropertySource
    static void providerUrls(DynamicPropertyRegistry registry) {
        String baseUrl = "http://127.0.0.1:" + slowServer.getAddress().getPort();
        registry.add("app.itunes.base-url", () -> baseUrl);
        registry.add("app.musicbrainz.base-url", () -> baseUrl);
    }

    @Test
    void trigger_sendsDegradedAlarmQuickly_whenProvidersHang() throws Exception {
        long start = System.nanoTime();

        mockMvc.perform(post("/alarms/trigger").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"carol\",\"dayOfWeek\":\"MONDAY\",\"weather\":\"SOLEIL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.track.title").value("Here Comes the Sun"))
                .andExpect(jsonPath("$.degraded").value(true));

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
    }
}
