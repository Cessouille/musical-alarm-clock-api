package fr.cours.musical.alarm.clock.api.infra.out.music.itunes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.port.out.MusicProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

@Component("itunes")
@Qualifier("source")
@RequiredArgsConstructor
public class ITunesMusicProvider implements MusicProvider {

    private static final int MAX_RESULTS = 5;

    private final RestClient itunesApiHttpClient;
    private final JsonMapper jsonMapper;

    @Override
    public Optional<Track> findTrack(String query) {
        // iTunes answers JSON as text/javascript: read it as text, then parse it ourselves.
        String body = itunesApiHttpClient.get()
                .uri("/search?term={term}&media=music&limit={limit}", query, MAX_RESULTS)
                .retrieve()
                .body(String.class);
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        ITunesResponse response = jsonMapper.readValue(body, ITunesResponse.class);
        if (response == null || response.results() == null) {
            return Optional.empty();
        }
        return response.results().stream()
                .filter(result -> result.trackName() != null && !result.trackName().isBlank())
                .findFirst()
                .map(result -> new Track(result.trackName(), result.artistName()));
    }

    // trackViewUrl and every other iTunes field are deliberately not mapped: they never leave this adapter.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ITunesResponse(List<ITunesResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ITunesResult(String trackName, String artistName) {
    }
}
