package fr.cours.musical.alarm.clock.api.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ITunesConfig {

    @Value("${app.itunes.base-url}")
    private String baseUrl;

    @Bean("itunesApiHttpClient")
    public RestClient itunesApiHttpClient(ClientHttpRequestFactory outboundRequestFactory) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(outboundRequestFactory)
                .build();
    }
}
