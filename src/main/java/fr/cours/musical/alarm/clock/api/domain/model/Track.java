package fr.cours.musical.alarm.clock.api.domain.model;

public record Track(String title, String artist) {

    private static final String UNKNOWN_ARTIST = "Unknown artist";

    public Track {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("A track must have a title");
        }
        title = title.trim();
        artist = (artist == null || artist.isBlank()) ? UNKNOWN_ARTIST : artist.trim();
    }
}
