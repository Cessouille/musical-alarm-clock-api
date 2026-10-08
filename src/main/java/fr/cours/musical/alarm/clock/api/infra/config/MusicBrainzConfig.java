package fr.cours.musical.alarm.clock.api.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MusicBrainzConfig {

    @Value("${app.musicbrainz.base-url}")
    private String baseUrl;

    @Value("${app.musicbrainz.user-agent}")
    private String userAgent;

    @Bean("musicBrainzApiHttpClient")
    public RestClient musicBrainzApiHttpClient(ClientHttpRequestFactory outboundRequestFactory) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(outboundRequestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
