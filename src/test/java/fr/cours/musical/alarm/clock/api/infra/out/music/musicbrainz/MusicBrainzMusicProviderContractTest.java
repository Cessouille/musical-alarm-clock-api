package fr.cours.musical.alarm.clock.api.infra.out.music.musicbrainz;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.port.out.MusicProvider;
import fr.cours.musical.alarm.clock.api.domain.port.out.MusicProviderContractTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MusicBrainzMusicProviderContractTest extends MusicProviderContractTest {

    private MockRestServiceServer server;
    private MusicBrainzMusicProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://musicbrainz.org/ws/2");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new MusicBrainzMusicProvider(builder.build());
    }

    @Override
    protected MusicProvider provider() {
        return provider;
    }

    @Override
    protected void givenTrackFoundResponse() {
        server.expect(requestTo(containsString("/recording")))
                .andRespond(withSuccess("""
                        {"recordings":[{"title":"Here Comes the Sun",
                         "artist-credit":[{"name":"The Beatles","joinphrase":""}]}]}
                        """, MediaType.APPLICATION_JSON));
    }

    @Override
    protected void givenNoMatchResponse() {
        server.expect(requestTo(containsString("/recording")))
                .andRespond(withSuccess("{\"recordings\":[]}", MediaType.APPLICATION_JSON));
    }

    @Override
    protected void givenEmptyBodyResponse() {
        server.expect(requestTo(containsString("/recording")))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));
    }

    @Override
    protected void givenAccentedTrackResponse() {
        server.expect(requestTo(containsString("/recording")))
                .andRespond(withSuccess("""
                        {"recordings":[{"title":"Désenchantée","artist-credit":[{"name":"Mylène Farmer"}]}]}
                        """, MediaType.APPLICATION_JSON));
    }

    @Override
    protected void givenOnlyUnusableResultResponse() {
        server.expect(requestTo(containsString("/recording")))
                .andRespond(withSuccess("""
                        {"recordings":[{"artist-credit":[{"name":"Someone"}]}]}
                        """, MediaType.APPLICATION_JSON));
    }

    @Override
    protected void givenServerErrorResponse() {
        server.expect(requestTo(containsString("/recording"))).andRespond(withServerError());
    }

    @Override
    protected void givenNoMatchForEncodedRequestAcDcAndCo() {
        server.expect(requestTo(containsString("query=AC%2FDC%20%26%20Co")))
                .andRespond(withSuccess("{\"recordings\":[]}", MediaType.APPLICATION_JSON));
    }

    @Override
    protected void verifyExpectedRequestWasReceived() {
        server.verify();
    }

    @Test
    void findTrack_joinsSeveralArtistCredits() {
        server.expect(requestTo(containsString("/recording")))
                .andRespond(withSuccess("""
                        {"recordings":[{"title":"Under Pressure",
                         "artist-credit":[{"name":"Queen","joinphrase":" & "},{"name":"David Bowie"}]}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.findTrack("Under Pressure")).contains(new Track("Under Pressure", "Queen & David Bowie"));
    }

    @Test
    void findTrack_usesUnknownArtist_whenArtistCreditIsMissing() {
        server.expect(requestTo(containsString("/recording")))
                .andRespond(withSuccess("{\"recordings\":[{\"title\":\"Imagine\"}]}", MediaType.APPLICATION_JSON));

        assertThat(provider.findTrack("Imagine")).contains(new Track("Imagine", "Unknown artist"));
    }
}
