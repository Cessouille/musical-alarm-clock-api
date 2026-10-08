package fr.cours.musical.alarm.clock.api.domain.port.out;

import fr.cours.musical.alarm.clock.api.domain.model.Track;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract every MusicProvider implementation must satisfy. Each adapter's test
 * subclass stubs its own HTTP responses via the given* hooks below.
 */
public abstract class MusicProviderContractTest {

    protected abstract MusicProvider provider();

    /** Answers one match: "Here Comes the Sun" by "The Beatles". */
    protected abstract void givenTrackFoundResponse();

    protected abstract void givenNoMatchResponse();

    protected abstract void givenEmptyBodyResponse();

    /** Answers one match: "Désenchantée" by "Mylène Farmer". */
    protected abstract void givenAccentedTrackResponse();

    /** HTTP 200 whose only result has no title. */
    protected abstract void givenOnlyUnusableResultResponse();

    protected abstract void givenServerErrorResponse();

    /** Expects a request carrying the URL-encoded form {@code AC%2FDC%20%26%20Co}; answers "no match". */
    protected abstract void givenNoMatchForEncodedRequestAcDcAndCo();

    protected abstract void verifyExpectedRequestWasReceived();

    @Test
    void findTrack_returnsTrack_whenProviderHasAMatch() {
        givenTrackFoundResponse();

        Optional<Track> result = provider().findTrack("Here Comes the Sun");

        assertThat(result).contains(new Track("Here Comes the Sun", "The Beatles"));
    }

    @Test
    void findTrack_returnsEmpty_whenNoMatch() {
        givenNoMatchResponse();

        assertThat(provider().findTrack("zzzz")).isEmpty();
    }

    @Test
    void findTrack_returnsEmpty_whenBodyIsEmpty() {
        givenEmptyBodyResponse();

        assertThat(provider().findTrack("zzzz")).isEmpty();
    }

    @Test
    void findTrack_handlesAccentedCharacters() {
        givenAccentedTrackResponse();

        Optional<Track> result = provider().findTrack("Désenchantée");

        assertThat(result).contains(new Track("Désenchantée", "Mylène Farmer"));
    }

    @Test
    void findTrack_ignoresResultsWithoutTitle() {
        givenOnlyUnusableResultResponse();

        assertThat(provider().findTrack("whatever")).isEmpty();
    }

    @Test
    void findTrack_throws_whenProviderFails() {
        givenServerErrorResponse();

        assertThatThrownBy(() -> provider().findTrack("anything")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void findTrack_urlEncodesSpecialCharactersInTheQuery() {
        givenNoMatchForEncodedRequestAcDcAndCo();

        provider().findTrack("AC/DC & Co");

        verifyExpectedRequestWasReceived();
    }
}
