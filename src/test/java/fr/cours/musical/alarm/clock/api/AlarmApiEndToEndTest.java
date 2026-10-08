package fr.cours.musical.alarm.clock.api;

import fr.cours.musical.alarm.clock.api.infra.out.notification.email.EmailClient;
import fr.cours.musical.alarm.clock.api.infra.out.notification.push.PushPayload;
import fr.cours.musical.alarm.clock.api.infra.out.notification.push.PushService;
import fr.cours.musical.alarm.clock.api.infra.out.notification.sms.SmsGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "app.music.providers=itunes,musicbrainz",
                "app.notification.fallback-order=PUSH,SMS,EMAIL"
        })
@AutoConfigureMockMvc
@Import(AlarmApiEndToEndTest.MockRestClientsConfig.class)
class AlarmApiEndToEndTest {

    private static final MediaType ITUNES_CONTENT_TYPE = new MediaType("text", "javascript", StandardCharsets.UTF_8);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRestServiceServer itunesServer;

    @Autowired
    private MockRestServiceServer musicBrainzServer;

    @MockitoBean
    private EmailClient emailClient;

    @MockitoBean
    private SmsGateway smsGateway;

    @MockitoBean
    private PushService pushService;

    @AfterEach
    void resetServers() {
        itunesServer.reset();
        musicBrainzServer.reset();
    }

    private ResultActions trigger(String user, String day, String weather) throws Exception {
        return mockMvc.perform(post("/alarms/trigger").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"%s\",\"dayOfWeek\":\"%s\",\"weather\":\"%s\"}".formatted(user, day, weather)));
    }

    private void itunesFinds(String encodedTerm, String title, String artist) {
        itunesServer.expect(requestTo(containsString("/search?term=" + encodedTerm + "&"))).andRespond(withSuccess(
                "{\"resultCount\":1,\"results\":[{\"trackName\":\"%s\",\"artistName\":\"%s\",\"trackViewUrl\":\"https://music.apple.com/x\"}]}"
                        .formatted(title, artist), ITUNES_CONTENT_TYPE));
    }

    @Test
    void trigger_sendsWeatherTrackByEmail_andDoesNotLeakIntoTheResponse() throws Exception {
        itunesFinds("Walking%20on%20Sunshine", "Walking on Sunshine", "Katrina & The Waves");

        trigger("alice", "MONDAY", "SUN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.track.title").value("Walking on Sunshine"))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.degraded").value(false))
                .andExpect(jsonPath("$.track.trackViewUrl").doesNotExist());

        verify(emailClient).sendEmail(eq("alice@mock.invalid"), any(), contains("Walking on Sunshine"));
        itunesServer.verify();
    }

    @Test
    void trigger_choosesTheTrackOfTheDayAndWeather_notOnlyTheWeather() throws Exception {
        itunesFinds("Good%20Day%20Sunshine", "Good Day Sunshine", "The Beatles");

        trigger("alice", "TUESDAY", "SUN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.track.title").value("Good Day Sunshine"));

        itunesServer.verify();
    }

    @Test
    void trigger_fallsBackToMusicBrainz_whenITunesIsDown() throws Exception {
        itunesServer.expect(requestTo(containsString("/search"))).andRespond(withServerError());
        musicBrainzServer.expect(requestTo(containsString("query=Purple%20Rain"))).andRespond(withSuccess("""
                {"recordings":[{"title":"Purple Rain","artist-credit":[{"name":"Prince"}]}]}
                """, MediaType.APPLICATION_JSON));

        trigger("alice", "TUESDAY", "RAIN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.track.artist").value("Prince"))
                .andExpect(jsonPath("$.degraded").value(false));
    }

    @Test
    void trigger_usesLocalFallbackTrackAndStillSends_whenEveryProviderIsDown() throws Exception {
        itunesServer.expect(requestTo(containsString("/search"))).andRespond(withServerError());
        musicBrainzServer.expect(requestTo(containsString("/recording"))).andRespond(withServerError());

        trigger("bob", "TUESDAY", "SNOW")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.track.title").value("Good Vibrations"))
                .andExpect(jsonPath("$.channel").value("SMS"))
                .andExpect(jsonPath("$.degraded").value(true));

        verify(smsGateway).sendText(any(), contains("Good Vibrations"));
    }

    @Test
    void trigger_fallsBackToAnotherChannel_whenPreferredChannelFails() throws Exception {
        itunesFinds("Dancing%20Queen", "Dancing Queen", "ABBA");
        doThrow(new IllegalStateException("push provider down")).when(pushService).pushNotification(any(PushPayload.class));

        trigger("carol", "FRIDAY", "CLOUDY")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("SMS"))
                .andExpect(jsonPath("$.degraded").value(true));
    }

    @Test
    void trigger_returns503_whenEveryChannelFails() throws Exception {
        itunesFinds("Here%20Comes%20the%20Sun", "Here Comes the Sun", "The Beatles");
        doThrow(new IllegalStateException("down")).when(emailClient).sendEmail(any(), any(), any());
        doThrow(new IllegalStateException("down")).when(smsGateway).sendText(any(), any());
        doThrow(new IllegalStateException("down")).when(pushService).pushNotification(any(PushPayload.class));

        trigger("alice", "SUNDAY", "CLOUDY")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void trigger_queriesTheProviderOnlyOnce_forTwoIdenticalRequests() throws Exception {
        itunesFinds("Imagine", "Imagine", "John Lennon");

        trigger("bob", "WEDNESDAY", "CLOUDY").andExpect(status().isOk());
        trigger("bob", "THURSDAY", "CLOUDY").andExpect(status().isOk());

        itunesServer.verify();
    }

    @Test
    void trigger_returns404_forUnknownUser() throws Exception {
        trigger("ghost", "MONDAY", "SUN").andExpect(status().isNotFound());
    }

    @Test
    void trigger_returns400_forUnknownWeather() throws Exception {
        trigger("alice", "MONDAY", "FOG").andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class MockRestClientsConfig {

        record ITunesStub(MockRestServiceServer server, RestClient client) {
        }

        record MusicBrainzStub(MockRestServiceServer server, RestClient client) {
        }

        @Bean
        ITunesStub itunesStub() {
            RestClient.Builder builder = RestClient.builder().baseUrl("https://itunes.apple.com");
            return new ITunesStub(MockRestServiceServer.bindTo(builder).build(), builder.build());
        }

        @Bean
        MockRestServiceServer itunesServer(ITunesStub itunesStub) {
            return itunesStub.server();
        }

        @Bean("itunesApiHttpClient")
        RestClient itunesApiHttpClient(ITunesStub itunesStub) {
            return itunesStub.client();
        }

        @Bean
        MusicBrainzStub musicBrainzStub() {
            RestClient.Builder builder = RestClient.builder().baseUrl("https://musicbrainz.org/ws/2");
            return new MusicBrainzStub(MockRestServiceServer.bindTo(builder).build(), builder.build());
        }

        @Bean
        MockRestServiceServer musicBrainzServer(MusicBrainzStub musicBrainzStub) {
            return musicBrainzStub.server();
        }

        @Bean("musicBrainzApiHttpClient")
        RestClient musicBrainzApiHttpClient(MusicBrainzStub musicBrainzStub) {
            return musicBrainzStub.client();
        }
    }
}
