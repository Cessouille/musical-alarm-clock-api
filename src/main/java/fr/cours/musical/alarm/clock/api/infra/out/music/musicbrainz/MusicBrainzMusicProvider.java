package fr.cours.musical.alarm.clock.api.infra.out.music.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.cours.musical.alarm.clock.api.domain.model.Track;
import fr.cours.musical.alarm.clock.api.domain.port.out.MusicProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component("musicbrainz")
@Qualifier("source")
@RequiredArgsConstructor
public class MusicBrainzMusicProvider implements MusicProvider {

    private static final int MAX_RESULTS = 5;

    private final RestClient musicBrainzApiHttpClient;

    @Override
    public Optional<Track> findTrack(String query) {
        MusicBrainzResponse response = musicBrainzApiHttpClient.get()
                .uri("/recording?query={query}&fmt=json&limit={limit}", query, MAX_RESULTS)
                .retrieve()
                .body(MusicBrainzResponse.class);
        if (response == null || response.recordings() == null) {
            return Optional.empty();
        }
        return response.recordings().stream()
                .filter(recording -> recording.title() != null && !recording.title().isBlank())
                .findFirst()
                .map(recording -> new Track(recording.title(), artistName(recording.artistCredit())));
    }

    private String artistName(List<ArtistCredit> credits) {
        if (credits == null) {
            return null;
        }
        StringBuilder name = new StringBuilder();
        for (ArtistCredit credit : credits) {
            if (credit.name() != null) {
                name.append(credit.name());
            }
            if (credit.joinphrase() != null) {
                name.append(credit.joinphrase());
            }
        }
        return name.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MusicBrainzResponse(List<Recording> recordings) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Recording(String title, @JsonProperty("artist-credit") List<ArtistCredit> artistCredit) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ArtistCredit(String name, String joinphrase) {
    }
}
