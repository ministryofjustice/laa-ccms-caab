package uk.gov.laa.ccms.caab.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;

@Service
@RequiredArgsConstructor
public class CaabApiService {

    private final WebClient caabApiWebClient;

    private final CaabApiServiceErrorHandler caabApiServiceErrorHandler;

    public Mono<Void> createApplication(String loginId, ApplicationDetail application){

        return caabApiWebClient
                .post()
                .uri("/applications")
                .header("Caab-User-Login-Id", loginId)
                .contentType(MediaType.APPLICATION_JSON) // Set the content type to JSON
                .bodyValue(application) // Add the application details to the request body
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> caabApiServiceErrorHandler.handleCreateApplicationError(e));

    }
}
