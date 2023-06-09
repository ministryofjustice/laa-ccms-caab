package uk.gov.laa.ccms.caab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ApplicationConfig {

    private final String dataApiUrl;

    public ApplicationConfig(@Value("${laa.ccms.data-api.url}") String dataApiUrl) {
        this.dataApiUrl = dataApiUrl;
    }

    @Bean
    WebClient dataWebClient(){
        return WebClient.create(dataApiUrl);
    }
}
