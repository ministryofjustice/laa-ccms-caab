package uk.gov.laa.ccms.caab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ApplicationConfig {

    private final String dataApiUrl;

    private final String soaGatewayApiUrl;

    public ApplicationConfig(@Value("${laa.ccms.data-api.url}") String dataApiUrl,
                             @Value("${laa.ccms.soa-gateway-api.url}") String soaGatewayApiUrl) {
        this.dataApiUrl = dataApiUrl;
        this.soaGatewayApiUrl = soaGatewayApiUrl;
    }

    @Bean("dataWebClient")
    WebClient dataWebClient(){
        return WebClient.create(dataApiUrl);
    }

    @Bean("soaGatewayWebClient")
    WebClient soaGatewayWebClient() {return WebClient.create(soaGatewayApiUrl);}
}
