package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.data.model.CommonValueListDetails;
import uk.gov.laa.ccms.data.model.UserDetails;

import java.net.URI;

@Service
@Slf4j
public class DataService {
    private final WebClient dataWebClient;

    private final DataServiceErrorHandler dataServiceErrorHandler;

    public DataService(@Qualifier("dataWebClient") WebClient dataWebClient,
        DataServiceErrorHandler dataServiceErrorHandler) {
        this.dataWebClient = dataWebClient;
        this.dataServiceErrorHandler = dataServiceErrorHandler;
    }

    public Mono<UserDetails> getUser(String loginId){

        return dataWebClient
                .get()
                .uri("/users/{loginId}", loginId)
                .retrieve()
                .bodyToMono(UserDetails.class)
                .onErrorResume(e -> dataServiceErrorHandler.handleUserError(loginId, e));
    }

    public Mono<CommonValueListDetails> getCommonValues(String type, String code, String sort) {

        return dataWebClient
                .get()
                .uri(builder -> builder.path("/common-values")
                        .queryParam("type", type)
                        .queryParam("sort", sort)
                        .build())
                .retrieve()
                .bodyToMono(CommonValueListDetails.class)
                .onErrorResume(e -> dataServiceErrorHandler.handleCommonValuesError(e));
    }

}

