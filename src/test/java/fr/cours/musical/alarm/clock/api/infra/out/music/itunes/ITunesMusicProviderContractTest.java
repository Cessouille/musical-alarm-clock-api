package fr.cours.musical.alarm.clock.api.infra.out.music.itunes;

import fr.cours.musical.alarm.clock.api.domain.port.out.MusicProvider;
import fr.cours.musical.alarm.clock.api.domain.port.out.MusicProviderContractTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ITunesMusicProviderContractTest extends MusicProviderContractTest {

    /** iTunes really answers JSON with this content type. */
    private static final MediaType ITUNES_CONTENT_TYPE = new MediaType("text", "javascript", StandardCharsets.UTF_8);

    private MockRestServiceServer server;
    private ITunesMusicProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://itunes.apple.com");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new ITunesMusicProvider(builder.build(), JsonMapper.builder().build());
    }

    @Override
    protected MusicProvider provider() {
        return provider;
    }

    @Override
    protected void givenTrackFoundResponse() {
        server.expect(requestTo(containsString("/search")))
                .andRespond(withSuccess("""
                        {"resultCount":1,"results":[{"wrapperType":"track","kind":"song",
                         "artistName":"The Beatles","trackName":"Here Comes the Sun",
                         "trackViewUrl":"https://music.apple.com/fr/album/here-comes-the-sun/1"}]}
                        """, ITUNES_CONTENT_TYPE));
    }

    @Override
    protected void givenNoMatchResponse() {
        server.expect(requestTo(containsString("/search")))
                .andRespond(withSuccess("{\"resultCount\":0,\"results\":[]}", ITUNES_CONTENT_TYPE));
    }

    @Override
    protected void givenEmptyBodyResponse() {
        server.expect(requestTo(containsString("/search")))
                .andRespond(withSuccess("", ITUNES_CONTENT_TYPE));
    }

    @Override
    protected void givenAccentedTrackResponse() {
        server.expect(requestTo(containsString("/search")))
                .andRespond(withSuccess("""
                        {"resultCount":1,"results":[{"artistName":"Mylène Farmer","trackName":"Désenchantée"}]}
                        """, ITUNES_CONTENT_TYPE));
    }

    @Override
    protected void givenOnlyUnusableResultResponse() {
        server.expect(requestTo(containsString("/search")))
                .andRespond(withSuccess("""
                        {"resultCount":1,"results":[{"artistName":"Someone","trackViewUrl":"https://x"}]}
                        """, ITUNES_CONTENT_TYPE));
    }

    @Override
    protected void givenServerErrorResponse() {
        server.expect(requestTo(containsString("/search"))).andRespond(withServerError());
    }

    @Override
    protected void givenNoMatchForEncodedRequestAcDcAndCo() {
        server.expect(requestTo(containsString("term=AC%2FDC%20%26%20Co")))
                .andRespond(withSuccess("{\"resultCount\":0,\"results\":[]}", ITUNES_CONTENT_TYPE));
    }

    @Override
    protected void verifyExpectedRequestWasReceived() {
        server.verify();
    }
}
